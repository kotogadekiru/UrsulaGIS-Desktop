package com.ursulagis.desktop.gui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.api.data.FileDataStore;

import com.ursulagis.desktop.api.OrdenPulverizacion;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import gov.nasa.worldwind.WorldWindow;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.PulverizacionConfigDialogController;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.gui.nww.LayerAction;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import com.ursulagis.desktop.tasks.CompartirPulverizacionLaborTask;
import com.ursulagis.desktop.tasks.importar.ProcessPulvMapTask;
import com.ursulagis.desktop.tasks.procesar.ExportarPrescripcionPulverizacionTask;
import com.ursulagis.desktop.tasks.procesar.ExportarPrescripcionPulverizacionJSONTask;
import com.ursulagis.desktop.tasks.procesar.UnirPulverizacionesMapTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.FileHelper;

public class PulverizacionGUIController {
	//private static final String DD_MM_YYYY = "dd/MM/yyyy";
	JFXMain main=null;
	private Pane progressBox;

	public PulverizacionGUIController(JFXMain _main) {
		this.main=_main;		
		this.progressBox=main.progressBox;
	}
	
	public void addPulverizacionesRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();	
		rootNodeP.add(
				new LayerAction(					
						(layer)->{
							doOpenPulvMap(null);
							return "opened";	
						},	Messages.getString("JFXMain.importar")
						));
		main.getLayerPanel().addAccionesClase(rootNodeP,PulverizacionLabor.class);
	}
	
	public List<LayerAction> addAccionesPulverizaciones(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> pulverizacionesP = new ArrayList<LayerAction>();
		predicates.put(PulverizacionLabor.class, pulverizacionesP);
		
		/**
		 * Accion que permite clonar la pulverizacion
		 */
//		pulverizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.clonar"),(layer)->{
//			doUnirPulverizaciones((PulverizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "pulverizacion clonada" + layer.getName(); 
//		}));
		
		/**
		 *Accion que permite editar una pulverizacion
		 */
		pulverizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editPulvAction"),(layer)->{		
			doEditPulverizacion((PulverizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "pulverizacion editada" + layer.getName(); //$NON-NLS-1$
		}));

		/**
		 *Accion que permite exportar prescripcion de una pulverizacion
		 */
		pulverizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportarFertPAction"),(layer)->{		
			doExportarPrescPulverizacion((PulverizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "pulverizacion prescripcion exportada" + layer.getName(); //$NON-NLS-1$
		}));

		/**
		 *Accion que permite exportar prescripcion de una pulverizacion en formato JSON
		 */
		pulverizacionesP.add(LayerAction.constructPredicate("Exportar Prescripción JSON",(layer)->{		
			doExportarPrescPulverizacionJSON((PulverizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "pulverizacion prescripcion JSON exportada" + layer.getName(); //$NON-NLS-1$
		}));

		/**
		 *Accion que permite compartir prescripcion de una pulverizacion
		 */
		pulverizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.compartir"),(layer)->{		
			doCompartirPulverizacion((PulverizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "pulverizacion compartida" + layer.getName(); //$NON-NLS-1$
		}));


		return pulverizacionesP;
	}
	
	/**
	 *  updload recorrida to server and show url to access
	 * @param recorrida
	 */
	public void doCompartirPulverizacion(PulverizacionLabor value) {
		OrdenPulverizacion op = CompartirPulverizacionLaborTask.constructOrdenPulverizacion(value);
		if(op==null)return;
		DAH.save(op);
		CompartirPulverizacionLaborTask task = new CompartirPulverizacionLaborTask(value,op);			
			task.installProgressBar(main.progressBox);
			task.setOnSucceeded(handler -> {
				String ret = (String)handler.getSource().getValue();

				if(ret!=null) {
					main.configGUIController.showQR(ret);
				}
				task.uninstallProgressBar();			
			});
			System.out.println("ejecutando Compartir Recorrida");
			JFXMain.executorPool.submit(task);		
	}
	
	public void doEditPulverizacion(PulverizacionLabor cConfigured ) {
		Optional<PulverizacionLabor> cosechaConfigured=PulverizacionConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			//cConfigured.getLayer().removeAllRenderables();
			ProcessPulvMapTask umTask = new ProcessPulvMapTask(cConfigured);
			umTask.installProgressBar(main.progressBox);

			umTask.setOnSucceeded(handler -> {
				//CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
				main.getLayerPanel().update(main.getWwd());
				umTask.uninstallProgressBar();
				//	viewGoTo(ret);
				main.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.281")); //$NON-NLS-1$
				main.playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			JFXMain.executorPool.execute(umTask);
		}
	}
	
	//en la linea, al costado de la linea, siembra
		public void doExportarPrescPulverizacion(PulverizacionLabor laborToExport) {
			String nombre = laborToExport.getNombre();
			File shapeFile =  FileHelper.getNewShapeFile(nombre);

			Alert a = new Alert(Alert.AlertType.WARNING);
			a.setTitle("Advertencia");
			a.setContentText("Antes de aplicar consulte a un Ing. Agronomo!");
			a.initOwner(JFXMain.stage);
			a.show();

			ExportarPrescripcionPulverizacionTask ept = new ExportarPrescripcionPulverizacionTask(laborToExport, shapeFile); 
			ept.installProgressBar(main.progressBox);

			ept.setOnSucceeded(handler -> {
				File ret = (File)handler.getSource().getValue();
				main.playSound();
				ept.uninstallProgressBar();
				doOpenPulvMap(Collections.singletonList(ret));
			});
			JFXMain.executorPool.execute(ept);		
		}
		
		/**
		 * Exporta una prescripción de pulverización en formato JSON compatible con drones XAG
		 */
		public void doExportarPrescPulverizacionJSON(PulverizacionLabor laborToExport) {
			String nombre = laborToExport.getNombre();
			File jsonFile = FileHelper.getNewFile(nombre, "json");

			if(jsonFile == null) {
				return; // Usuario canceló
			}

			// Asegurar que tenga extensión .json
			if(!jsonFile.getName().toLowerCase().endsWith(".json")) {
				jsonFile = new File(jsonFile.getAbsolutePath() + ".json");
			}

			Alert a = new Alert(Alert.AlertType.WARNING);
			a.setTitle("Advertencia");
			a.setContentText("Antes de aplicar consulte a un Ing. Agronomo!");
			a.initOwner(JFXMain.stage);
			a.show();

			ExportarPrescripcionPulverizacionJSONTask ept = new ExportarPrescripcionPulverizacionJSONTask(laborToExport, jsonFile); 
			ept.installProgressBar(main.progressBox);

			ept.setOnSucceeded(handler -> {
				File ret = (File)handler.getSource().getValue();
				main.playSound();
				ept.uninstallProgressBar();
				
				Alert success = new Alert(Alert.AlertType.INFORMATION);
				success.setTitle("Exportación completada");
				success.setContentText("Prescripción JSON exportada exitosamente: " + ret.getName());
				success.initOwner(JFXMain.stage);
				success.show();
			});
			
			ept.setOnFailed(handler -> {
				ept.uninstallProgressBar();
				Alert error = new Alert(Alert.AlertType.ERROR);
				error.setTitle("Error");
				error.setContentText("Error al exportar prescripción JSON: " + handler.getSource().getException().getMessage());
				error.initOwner(JFXMain.stage);
				error.show();
			});
			
			JFXMain.executorPool.execute(ept);		
		}
		
		public void doOpenPulvMap(List<File> files) {
			List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(files);
			if (stores != null) {
				//	harvestMap.getChildren().clear();
				for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
					PulverizacionLabor labor = new PulverizacionLabor(store);
					//	SiembraLabor labor = new SiembraLabor(store);
					labor.setLayer(new LaborLayer());
					Optional<PulverizacionLabor> cosechaConfigured= PulverizacionConfigDialogController.config(labor);
					if(!cosechaConfigured.isPresent()){//
						System.out.println(Messages.getString("JFXMain.313")); //$NON-NLS-1$
						continue;
					}							

					ProcessPulvMapTask umTask = new ProcessPulvMapTask(labor);
					umTask.installProgressBar(main.progressBox);

					//	testLayer();
					umTask.setOnSucceeded(handler -> {
						PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
						//	pulverizaciones.add(ret);
						JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
						main.getLayerPanel().update(main.getWwd());
						umTask.uninstallProgressBar();
						main.viewGoTo(ret);

						System.out.println(Messages.getString("JFXMain.314")); //$NON-NLS-1$
						main.playSound();
					});//fin del OnSucceeded
					//umTask.start();
					JFXMain.executorPool.execute(umTask);
				}//fin del for stores

			}//if stores != null
		}
		
		private void doUnirPulverizaciones(PulverizacionLabor pulverizacionLabor) {
			List<PulverizacionLabor> pulverizacionesAUnir = new ArrayList<PulverizacionLabor>();
			if(pulverizacionLabor == null){
				List<PulverizacionLabor> pulverizacionesEnabled = main.getPulverizacionesSeleccionadas();
				pulverizacionesAUnir.addAll(pulverizacionesEnabled);
			} else {
				pulverizacionesAUnir.add(pulverizacionLabor);
			}
			
			UnirPulverizacionesMapTask umTask = new UnirPulverizacionesMapTask(pulverizacionesAUnir);
			umTask.installProgressBar(progressBox);
			umTask.setOnSucceeded(handler -> {
				PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
				if(ret.getLayer()!=null){
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
				}
				umTask.uninstallProgressBar();
				viewGoTo(ret);
				System.out.println(Messages.getString("JFXMain.287")); 
				playSound();
			});//fin del OnSucceeded						
			JFXMain.executorPool.execute(umTask);
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

		private void viewGoTo(PulverizacionLabor ret) {
			main.viewGoTo(ret);		
		}

		private void playSound() {
			main.playSound();
			
		}
}
