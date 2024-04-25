package gui.controller;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.data.FileDataStore;

import dao.Labor;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.suelo.Suelo;
import dao.utils.PropertyHelper;
import gui.HarvestConfigDialogController;
import gui.JFXMain;
import gui.Messages;
import gui.SueloConfigDialogController;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import tasks.crear.ConvertirSueloACosechaTask;
import tasks.importar.OpenSoilMapTask;
import tasks.importar.ProcessHarvestMapTask;
import tasks.procesar.ProcessBalanceDeNutrientes;
import tasks.procesar.ProcessBalanceDeNutrientes2;
import tasks.procesar.ResumirMargenMapTask;
import tasks.procesar.ResumirSoilMapTask;
import utils.FileHelper;

public class SueloGUIController {
	JFXMain main=null;

	public SueloGUIController(JFXMain _main) {
		this.main=_main;		
	}
	
	public void addAccionesSuelos(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> rootNodeSuelo = new ArrayList<LayerAction>();		
		rootNodeSuelo.add(LayerAction.constructPredicate(
				Messages.getString("JFXMain.balanceNutrientes"),
				(a)->{
					doProcesarBalanceNutrientes();
					return "";
				}));
		main.getLayerPanel().addAccionesClase(rootNodeSuelo,Suelo.class);
		
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
		 *Accion que permite resumir por categoria un mapa de rentabilidad
		 */
		suelosP.add(LayerAction.constructPredicate(Messages.getString("ResumirMargenMapTask.resumirAction"),(layer)->{	
			doResumirSuelo((Suelo) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "margen resumido" + layer.getName(); 
		}));
		
	}
	
	public void doResumirSuelo(Suelo aResumir) {
		ResumirSoilMapTask uMmTask = new ResumirSoilMapTask(aResumir);

		uMmTask.installProgressBar(main.progressBox);
		uMmTask.setOnSucceeded(handler -> {
			
			aResumir.getLayer().setEnabled(false);
			
			
			Suelo ret = (Suelo)handler.getSource().getValue();
			uMmTask.uninstallProgressBar();			
			
			JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
			main.getLayerPanel().update(main.getWwd());

			main.playSound();
			main.viewGoTo(ret);
			
			System.out.println(Messages.getString("JFXMain.323")); 
		});
		JFXMain.executorPool.execute(uMmTask);
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

		balanceNutrientesTask.installProgressBar(main.progressBox);

		balanceNutrientesTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			balanceNutrientesTask.uninstallProgressBar();
			suelosEnabled.stream().forEach(l->l.getLayer().setEnabled(false));
			cosechasEnabled.stream().forEach(l->l.getLayer().setEnabled(false));
			fertEnabled.stream().forEach(l->l.getLayer().setEnabled(false));

			//this.suelos.add(ret);
			JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
			main.getLayerPanel().update(main.getWwd());

			main.playSound();
			main.viewGoTo(ret);
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
		CosechaLabor labor = new CosechaLabor();
		labor.setNombre(suelo.getNombre());

		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			//System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha"); 
			labor.dispose();//libero los recursos reservados
			return;
		}					
		Double mmLluvia = null;
		try {
			Double rindeEsperado = cosechaConfigured.get().getCultivo().getAbsAgua();
			TextInputDialog rindePromDialog = new TextInputDialog(Messages.getNumberFormat().format(rindeEsperado));//Messages.getString("JFXMain.272")); 
			rindePromDialog.setTitle(Messages.getString("LluviaCampania")); 
			rindePromDialog.setContentText(Messages.getString("LluviaCampania")); 
			rindePromDialog.initOwner(JFXMain.stage);
			Optional<String> rPromOptional = rindePromDialog.showAndWait();
			mmLluvia = PropertyHelper.parseDouble(rPromOptional.get()).doubleValue();//Double.valueOf(anchoOptional.get());
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

		ConvertirSueloACosechaTask umTask = new ConvertirSueloACosechaTask(labor,suelo,mmLluvia);
		umTask.installProgressBar(main.progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
			umTask.uninstallProgressBar();
			suelo.getLayer().setEnabled(false);

			ProcessHarvestMapTask pmtask = new ProcessHarvestMapTask(ret);
			pmtask.installProgressBar(main.progressBox);
			pmtask.setOnSucceeded(handler2 -> {
				main.getLayerPanel().update(main.getWwd());
				pmtask.uninstallProgressBar();

				main.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.279")); 
				main.playSound();
				main.viewGoTo(ret);
			});
			pmtask.run();
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
