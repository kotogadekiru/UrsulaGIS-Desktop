package gui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.data.FileDataStore;

import api.OrdenPulverizacion;
import dao.Labor;
import dao.pulverizacion.PulverizacionLabor;
import gui.JFXMain;
import gui.Messages;
import gui.PulverizacionConfigDialogController;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import javafx.scene.control.Alert;
import tasks.CompartirPulverizacionLaborTask;
import tasks.importar.ProcessPulvMapTask;
import tasks.procesar.ExportarPrescripcionPulverizacionTask;
import utils.DAH;
import utils.FileHelper;

public class PulverizacionGUIController {
	//private static final String DD_MM_YYYY = "dd/MM/yyyy";
	JFXMain main=null;

	public PulverizacionGUIController(JFXMain _main) {
		this.main=_main;		
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
		 *Accion que permite compartir prescripcion de una pulverizacion
		 */
		pulverizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.compartirPulverizacionAction"),(layer)->{		
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
}
