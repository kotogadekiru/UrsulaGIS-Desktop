package com.ursulagis.desktop.gui.controller;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.api.data.FileDataStore;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.HarvestConfigDialogController;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.SueloConfigDialogController;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.gui.nww.LayerAction;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import com.ursulagis.desktop.tasks.crear.ConvertirSueloACosechaTask;
import com.ursulagis.desktop.tasks.importar.OpenSoilMapTask;
import com.ursulagis.desktop.tasks.procesar.ProcessBalanceDeNutrientes2;
import com.ursulagis.desktop.utils.FileHelper;

public class SueloGUIController extends AbstractGUIController{


	public SueloGUIController(JFXMain _main) {
		super(_main);
		
	}
	
	public void addAccionesSuelos(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> rootNodeSuelo = new ArrayList<LayerAction>();		
		rootNodeSuelo.add(LayerAction.constructPredicate(
				Messages.getString("JFXMain.balanceNutrientes"),
				(a)->{
					doProcesarBalanceNutrientes();
					return "";
				}));
		getLayerPanel().addAccionesClase(rootNodeSuelo,Suelo.class);
		
		List<LayerAction> suelosP = new ArrayList<LayerAction>();
		predicates.put(Suelo.class, suelosP);
		suelosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editSoilAction"),(layer)->{	
			doEditSuelo((Suelo) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "suelo editado" + layer.getName(); 
		}));

		suelosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.estimarPotencialRendimiento"),(layer)->{	
			doEstimarPotencialRendimiento((Suelo) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "potencial de rendimiento suelo estimado" + layer.getName(); 
		}));
		
		/**
		 *Accion que permite resumir por categoria un mapa de suelo
		 */
		//Comentado porque resumir ahora es una accion generica de labor
//		suelosP.add(LayerAction.constructPredicate(Messages.getString("ResumirMargenMapTask.resumirAction"),(layer)->{	
//			doResumirSuelo((Suelo) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "suelo resumido" + layer.getName(); 
//		}));
		
	}
	
	
	public void doProcesarBalanceNutrientes() {		
		//System.out.println(Messages.getString("JFXMain.327")); 
		//todo pasar el filtrado por visibles aca y pasar nuevas listas solo con las visibles
		List<Suelo> suelosEnabled = main.getSuelosSeleccionados();
		List<FertilizacionLabor> fertEnabled = main.getFertilizacionesSeleccionadas();		
		List<CosechaLabor> cosechasEnabled = main.getCosechasSeleccionadas();//cosechas.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());

		ProcessBalanceDeNutrientes2 balanceNutrientesTask = 
				new ProcessBalanceDeNutrientes2(suelosEnabled,
						cosechasEnabled, fertEnabled);

		balanceNutrientesTask.installProgressBar(progressBox);

		balanceNutrientesTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			balanceNutrientesTask.uninstallProgressBar();
			suelosEnabled.stream().forEach(l->l.getLayer().setEnabled(false));
			cosechasEnabled.stream().forEach(l->l.getLayer().setEnabled(false));
			fertEnabled.stream().forEach(l->l.getLayer().setEnabled(false));

			//this.suelos.add(ret);
			JFXMain.insertBeforeCompass(getWwd(), ret.getLayer());
			getLayerPanel().update(getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.328")); 
		});
		JFXMain.executorPool.execute(balanceNutrientesTask);
	}
	
	private void doEditSuelo(Suelo cConfigured) {			
		Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			OpenSoilMapTask umTask = new OpenSoilMapTask(cConfigured);
			umTask.installProgressBar(main.progressBox);

			umTask.setOnSucceeded(handler -> {
				main.getLayerPanel().update(main.getWwd());
				umTask.uninstallProgressBar();
				main.wwjPanel.repaint();
				main.playSound();
			});//fin del OnSucceeded
			JFXMain.executorPool.execute(umTask);
		}
	}
	
	//TODO tomar un suelo y una configuracion de cosecha 
	//y crear un mapa de potencial de rendimiento segun el agua en el perfil
	private void doEstimarPotencialRendimiento(Suelo suelo) {
		CosechaLabor cosecha = new CosechaLabor();
		LaborLayer layer = new LaborLayer();
		cosecha.setLayer(layer);
		cosecha.setNombre(suelo.getNombre());

		cosecha.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(cosecha);
		if(!cosechaConfigured.isPresent()){//
			//System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha"); 
			cosecha.dispose();//libero los recursos reservados
			return;
		}else {
			cosecha=cosechaConfigured.get();
		}
		Double mmLluvia = null;
		//TODO cambair a  NumberInputDialog
		try {
			Cultivo cultivo = cosecha.getCultivo();
			Double aguaPorCampania = cultivo.getAbsAgua()*cultivo.getRindeEsperado();
			TextInputDialog lluviaCampaniaDialog = new TextInputDialog(Messages.getNumberFormat().format(aguaPorCampania));//Messages.getString("JFXMain.272")); 
			lluviaCampaniaDialog.setTitle(Messages.getString("LluviaCampania")); 
			lluviaCampaniaDialog.setContentText(Messages.getString("LluviaCampania")); 
			lluviaCampaniaDialog.initOwner(JFXMain.stage);
			Optional<String> lluviaOP = lluviaCampaniaDialog.showAndWait();
			mmLluvia = PropertyHelper.parseDouble(lluviaOP.get()).doubleValue();//Double.valueOf(anchoOptional.get());
		}catch(java.lang.NumberFormatException e) {

			DecimalFormat format=PropertyHelper.getDoubleConverter();
			DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
			char sep=symbols.getDecimalSeparator();

			Alert a = new Alert(Alert.AlertType.ERROR);
			a.initOwner(JFXMain.stage);

			a.setTitle(Messages.getString("JFXMain.275")); 
			a.setHeaderText(Messages.getString("JFXMain.276")+sep+Messages.getString("JFXMain.277"));  
			a.setContentText(e.getMessage());
			a.show();
			return;
		}

		ConvertirSueloACosechaTask umTask = new ConvertirSueloACosechaTask(cosecha,suelo,mmLluvia);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			
			insertBeforeCompass(getWwd(), ret.getLayer());
			
			getLayerPanel().update(getWwd());
			suelo.getLayer().setEnabled(false);
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			
			playSound();
			
			//wwjPanel.repaint();		


//			ProcessHarvestMapTask pmtask = new ProcessHarvestMapTask(ret);
//			pmtask.installProgressBar(main.progressBox);
//			pmtask.setOnSucceeded(handler2 -> {
//				main.getLayerPanel().update(main.getWwd());
//				pmtask.uninstallProgressBar();
//
//				main.wwjPanel.repaint();
//				System.out.println(Messages.getString("JFXMain.279")); 
//				main.playSound();
//				main.viewGoTo(ret);
//			});
//			pmtask.run();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}
	
	public void doOpenSoilMap(List<File> files) {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Suelo labor = new Suelo(store);
				labor.setLayer(new LaborLayer());
				Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.315")); 
					continue;
				}							

				OpenSoilMapTask umTask = new OpenSoilMapTask(labor);
				umTask.installProgressBar(main.progressBox);

				umTask.setOnSucceeded(handler -> {
					Suelo ret = (Suelo)handler.getSource().getValue();
					JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
					main.getLayerPanel().update(main.getWwd());
					umTask.uninstallProgressBar();
					main.viewGoTo(ret);

					System.out.println(Messages.getString("JFXMain.316")); 
					main.playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}


}
