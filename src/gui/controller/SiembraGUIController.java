package gui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.geotools.data.FileDataStore;

import api.OrdenPulverizacion;
import api.OrdenSiembra;
import dao.Labor;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.WorldWindow;
import gui.JFXMain;
import gui.Messages;
import gui.SiembraConfigDialogController;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import gui.nww.LayerPanel;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.Pane;
import tasks.CompartirPulverizacionLaborTask;
import tasks.CompartirSiembraLaborTask;
import tasks.crear.ConvertirASiembraTask;
import tasks.importar.ProcessSiembraMapTask;
import tasks.procesar.ExportarPrescripcionSiembraTask;
import tasks.procesar.SiembraFertTask;
import tasks.procesar.UnirSiembrasMapTask;
import utils.DAH;
import utils.FileHelper;

public class SiembraGUIController {
	private JFXMain main=null;
	private Pane progressBox;
	private Executor executorPool;

	public SiembraGUIController(JFXMain _main) {
		this.main=_main;		
		this.progressBox=main.progressBox;
		this.executorPool=JFXMain.executorPool;
	}

	public void addSiembrasRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();
		rootNodeP.add(new LayerAction((layer)->{
			this.doOpenSiembraMap(null);
			return "opened";	
		},Messages.getString("JFXMain.importar")));

		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.unir"),(layer)->{
			this.doUnirSiembras(null);
			return "joined";	
		},2));

		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.generarSiembraFert"),(layer)->{
			this.doGenerarSiembraFertilizada();
			return "generated";	
		},1));

		main.getLayerPanel().addAccionesClase(rootNodeP,SiembraLabor.class);
	}
	
	public void addAccionesSiembras(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> siembrasP = new ArrayList<LayerAction>();
		predicates.put(SiembraLabor.class, siembrasP);
		/**
		 *Accion que permite editar una siembra
		 */
		siembrasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editSiembraAction"),(layer)->{
			doEditSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "siembra editada" + layer.getName(); 
		}));

		/**
		 * Accion permite exportar la prescripcion de siembra
		 */
		siembrasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportarSiembraAction"),(layer)->{
			doExportPrescripcionSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "prescripcion Exportada" + layer.getName(); 
		}));
		
		/**
		 *Accion que permite compartir prescripcion de una pulverizacion
		 */
		siembrasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.compartirPulverizacionAction"),(layer)->{		
			doCompartirSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "pulverizacion compartida" + layer.getName(); //$NON-NLS-1$
		}));
	}
	
	//	public void addSiembrasRootNodeActions() {
	//		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();	
	//		rootNodeP.add(
	//				new LayerAction(					
	//						(layer)->{
	//							doOpenPulvMap(null);
	//							return "opened";	
	//						},	Messages.getString("JFXMain.importar")
	//						));
	//		main.layerPanel.addAccionesClase(rootNodeP,PulverizacionLabor.class);
	//	}


	private void doEditSiembra(SiembraLabor cConfigured ) {
		Optional<SiembraLabor> cosechaConfigured=SiembraConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			ProcessSiembraMapTask umTask = new ProcessSiembraMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				main.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.282")); 
				playSound();
			});//fin del OnSucceeded						
			JFXMain.executorPool.execute(umTask);
		}
	}
	
	// junta 2 o mas cosechas en una 
	private void doUnirSiembras(SiembraLabor siembraLabor) {
		List<SiembraLabor> siemrbasAUnir = new ArrayList<SiembraLabor>();
		if(siembraLabor == null){
			List<SiembraLabor> cosechasEnabled = main.getSiembrasSeleccionadas();
			siemrbasAUnir.addAll( cosechasEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {
			siemrbasAUnir.add(siembraLabor);
		}

		UnirSiembrasMapTask umTask = new UnirSiembrasMapTask(siemrbasAUnir);
		umTask.installProgressBar(progressBox);
		umTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			if(ret.getLayer()!=null){
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
			}
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			System.out.println(Messages.getString("JFXMain.286")); 
			playSound();
		});//fin del OnSucceeded											
		JFXMain.executorPool.execute(umTask);
	}
	
	private void doGenerarSiembraFertilizada() {
		SiembraLabor siembraEnabled = main.getSiembrasSeleccionadas().get(0);
		FertilizacionLabor fertEnabled = main.getFertilizacionesSeleccionadas().get(0);

		boolean esFertLinea=true;
		Alert selectTipoFert = new Alert(Alert.AlertType.CONFIRMATION);
		selectTipoFert.initOwner(JFXMain.stage);
		selectTipoFert.setTitle("Seleccione tipo fertilizacion");
		selectTipoFert.setContentText("Seleccione OK si es fertilizacion en la linea");
		Optional<ButtonType> esFertLineaOP = selectTipoFert.showAndWait();
		if(!esFertLineaOP.isPresent()) {
			return;
		} else if(!esFertLineaOP.get().equals(ButtonType.OK)){
			esFertLinea=false;
		}		
		SiembraFertTask siembraFertTask = new SiembraFertTask(siembraEnabled, fertEnabled,esFertLinea);
		siembraFertTask.installProgressBar(main.progressBox);
		siembraFertTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			siembraFertTask.uninstallProgressBar();
			siembraEnabled.getLayer().setEnabled(false);
			fertEnabled.getLayer().setEnabled(false);
			this.insertBeforeCompass(main.getWwd(), ret.getLayer());
			main.getLayerPanel().update(main.getWwd());

			main.playSound();
			main.viewGoTo(ret);
			System.out.println("SiembraFertTask succeded"); 
		});
		this.executorPool.execute(siembraFertTask);
	}
	
	private void doOpenSiembraMap(List<File> files) {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				SiembraLabor labor = new SiembraLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<SiembraLabor> cosechaConfigured= SiembraConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.310")); 
					continue;
				}							

				ProcessSiembraMapTask umTask = new ProcessSiembraMapTask(labor);
				umTask.installProgressBar(progressBox);
				umTask.setOnSucceeded(handler -> {
					SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println(Messages.getString("JFXMain.311")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}

	private String selectUnidadSiembra() {
		//preguntar en que unidad exportar la dosis de semilla
				Dialog<String> d= new Dialog<String>();
				d.initOwner(JFXMain.stage);//exportarSiembraAction
				d.setTitle(Messages.getString("JFXMain.doExportPrescripcionSiembraTitle"));//"Seleccione unidad de dosis semilla");
				d.getDialogPane().getButtonTypes().add(ButtonType.OK);
				d.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
				d.setResizable(true);
				ComboBox<String> cb = new ComboBox<String>();
				Map<String, String> availableColums = getUnidadesPrescripcionSiembra();

				cb.setItems(FXCollections.observableArrayList(availableColums.keySet()));
				cb.getSelectionModel().select(0);
				d.getDialogPane().setContent(cb);
				//d.getDialogPane().getChildren().add(cb);

				d.setResultConverter((bt)->{
					if(ButtonType.OK.equals(bt)) {
						String unidad = cb.getSelectionModel().getSelectedItem();
						return availableColums.get(unidad);
					}else {
						return null;
					}
					//d.setResult(unidad);
				});
				d.showAndWait();
				String unidad = d.getResult();
				System.out.println("unidad seleccionada " + unidad);
				return unidad;
	}

	public static Map<String, String> getUnidadesPrescripcionSiembra() {
		Map<String,String> availableColums = new LinkedHashMap<String,String>();
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_10METROS"),SiembraLabor.COLUMNA_SEM_10METROS);//("Sem10ml");
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_DOSIS_SEMILLA"),SiembraLabor.COLUMNA_KG_SEMILLA);//("kgSemHa");
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_MILES_SEM_HA"),SiembraLabor.COLUMNA_MILES_SEM_HA);//("MilSemHa");
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_ML"),SiembraLabor.COLUMNA_SEM_ML);//("semML");
		return availableColums;
	}
	
	private void doExportPrescripcionSiembra(SiembraLabor laborToExport) {
		String unidad = this.selectUnidadSiembra();
		
		if(unidad!=null) {
			String nombre = laborToExport.getNombre();
			File shapeFile = FileHelper.getNewShapeFile(nombre);
			//executorPool.execute(()->ExportarPrescripcionSiembraTask.run(laborToExport, shapeFile,unidad));

			ExportarPrescripcionSiembraTask ept = new ExportarPrescripcionSiembraTask(laborToExport, shapeFile,unidad); 
			ept.installProgressBar(progressBox);

			ept.setOnSucceeded(handler -> {				
				File ret = (File)handler.getSource().getValue();
				playSound();
				ept.uninstallProgressBar();
				this.doOpenSiembraMap(Collections.singletonList(ret));
			});
			executorPool.execute(ept);	
		}
	}

	
	/**
	 *  updload siembra to server and show url to access
	 * @param recorrida
	 */
	public void doCompartirSiembra(SiembraLabor value) {
		OrdenSiembra op = CompartirSiembraLaborTask.constructOrdenSiembra(value);
		if(op==null)return;
		DAH.save(op);
		CompartirSiembraLaborTask task = new CompartirSiembraLaborTask(value,op);			
			task.installProgressBar(main.progressBox);
			task.setOnSucceeded(handler -> {
				String ret = (String)handler.getSource().getValue();

				if(ret!=null) {
					main.configGUIController.showQR(ret);
				}
				task.uninstallProgressBar();			
			});
			System.out.println("ejecutando Compartir Siembra");
			JFXMain.executorPool.submit(task);		
	}
	/**
	 * toma una cosecha, pregunta las densidades deseadas para cada ambiente
	 * y crea una siembra teniendo la informacion ingresada y la categoria a la que pertenece cada poligono
	 * @param cosecha
	 */
	//TODO mover a cosechaGUIController
	public void doCrearSiembra(CosechaLabor cosecha) {
		SiembraLabor siembra = new SiembraLabor();
		LaborLayer layer = new LaborLayer();
		siembra.setLayer(layer);
		siembra.setNombre(cosecha.getNombre()+" "+Messages.getString("JFXMain.255"));  
		Optional<SiembraLabor> siembraConfigured= SiembraConfigDialogController.config(siembra);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); 
			siembra.dispose();//libero los recursos reservados
			return;
		}		
		String[] columnas = new String[]{
				Messages.getString("JFXMain.Densidad"),
				Messages.getString("JFXMain.FertL"),
				Messages.getString("JFXMain.FertC")
		};
		Map<String,Double[]> mapClaseValor = main.configGUIController.doAsignarValoresCosecha(cosecha,columnas);//"Densidad pl/m2"
		ConvertirASiembraTask csTask = new ConvertirASiembraTask(cosecha,siembra,mapClaseValor);
		csTask.installProgressBar(progressBox);
		csTask.setOnSucceeded(handler -> {
			cosecha.getLayer().setEnabled(false);
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			csTask.uninstallProgressBar();
			viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(csTask);
	}

	private void insertBeforeCompass(WorldWindow wwd, LaborLayer layer) {
		JFXMain.insertBeforeCompass(wwd, layer);		
	}

	private LayerPanel getLayerPanel() {		
		return main.getLayerPanel();
	}

	private WorldWindow getWwd() {		
		return main.getWwd();
	}

	private void viewGoTo(SiembraLabor ret) {
		main.viewGoTo(ret);		
	}

	private void playSound() {
		main.playSound();
		
	}
}
