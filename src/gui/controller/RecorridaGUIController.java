package gui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.data.FileDataStore;

import dao.Labor;
import dao.Ndvi;
import dao.fertilizacion.FertilizacionLabor;
import dao.recorrida.Recorrida;
import gov.nasa.worldwind.layers.RenderableLayer;
import gui.FertilizacionConfigDialogController;
import gui.JFXMain;
import gui.Messages;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import tasks.CompartirRecorridaTask;
import tasks.ShowRecorridaDirigidaTask;
import tasks.importar.ImportarRecorridaTask;
import tasks.importar.ProcessFertMapTask;
import tasks.procesar.ExportarRecorridaTask;
import utils.DAH;
import utils.FileHelper;

public class RecorridaGUIController extends AbstractGUIController {
	public RecorridaGUIController(JFXMain _main) {
		super(_main);
	}

	public void addAccionesRecorridas(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> recorridasP = new ArrayList<LayerAction>();
		predicates.put(Recorrida.class, recorridasP);

		//editar Recorrida
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editarLayer"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;				
				main.configGUIController.doShowRecorridaTable(Collections.singletonList(recorrida));
				layer.setName(recorrida.getNombre());
				this.getLayerPanel().update(this.getWwd());
			}
			return "edite recorrida"; 
		}));

		//Guardar Recorrida
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.guardarNDVIAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				DAH.save(recorrida);
			}
			return "guarde recorrida"; 
		}));

		//Tomar una recorrida y un poligono y convertir a mapa de suelo interpolando los datos con krigging
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("RecorridaGUIController.interpolarASuelo"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				doInterpolarRecorrida(recorrida);
			}
			return "interpole recorrida"; 
		}));	

		//Compartir Recorrida
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.compartir"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				//updload to server and show url to access
				doCompartirRecorrida(recorrida);
			}
			return "comparti una recorrida"; 
		}));

		/**
		 * metodo que permite asignar los resultados de los analisis a los muestreos con mismo nombre
		 */
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("Recorrida.asignarValores"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				doAsignarValoresRecorrida(recorrida);
			}
			return "asigne los valores de una recorrida"; 
		}));

		/**
		 * Accion permite exportar la recorrida como shp
		 */
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportLaborAction"),(layer)->{
			doExportRecorrida((Recorrida) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "recorrida Exportada" + layer.getName(); 
		}));
	}


	/**
	 * metodo que toma las muestras de una recorrida y interpola los puntos en 
	 * una grilla contenida dentro de un poligono seleccionado
	 * @param recorrida
	 */
	private void doInterpolarRecorrida(Recorrida recorrida) {
		main.enDesarrollo();
		List<Ndvi> seleccionados = main.getNdviSeleccionados();
		// TODO Auto-generated method stub

	}

	public void doShowRecorrida(Recorrida recorrida) {
		ShowRecorridaDirigidaTask umTask = new ShowRecorridaDirigidaTask(recorrida);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			RenderableLayer ret = (RenderableLayer)handler.getSource().getValue();
			JFXMain.insertBeforeCompass(getWwd(), ret);
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			main.viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);	
	}

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	public void doOpenRecorridaMap(List<File> files) {
		List<FileDataStore> stores =FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Recorrida labor = new Recorrida(store);

				//				Optional<Recorrida> configured= RecorridaConfigDialogController.config(labor);
				//				if(!configured.isPresent()){//
				//					System.out.println(Messages.getString("JFXMain.308")); 
				//					continue;
				//				}							

				ImportarRecorridaTask umTask = new ImportarRecorridaTask(labor,store);
				umTask.installProgressBar(progressBox);

				umTask.setOnSucceeded(handler -> {
					Recorrida ret = (Recorrida)handler.getSource().getValue();
					doShowRecorrida(ret);
					umTask.uninstallProgressBar();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}



	/**
	 *  updload recorrida to server and show url to access
	 * @param recorrida
	 */
	public void doCompartirRecorrida(Recorrida recorrida) {		
		if(recorrida.getUrl()!=null && recorrida.getUrl().length()>0) {			
			main.configGUIController.showQR(recorrida.getUrl());
			//XXX editar la recorrida remota con la informacion actualizada de la local?
			//XXX recupero la recorrida remota?
			return;
		}
		CompartirRecorridaTask task = new CompartirRecorridaTask(recorrida);
		//System.out.println("procesando los datos entre "+ndviDpDLG.initialDate+" y "+ ndviDpDLG.finalDate);//hasta aca ok!
		task.installProgressBar(progressBox);
		task.setOnSucceeded(handler -> {
			String ret = (String)handler.getSource().getValue();
			recorrida.setUrl(ret);
			DAH.save(recorrida);
			if(ret!=null) {
				main.configGUIController.showQR(ret);
			}
			//XXX agregar boton de actualizar desde la nube?
			task.uninstallProgressBar();			
		});
		System.out.println("ejecutando Compartir Recorrida"); 
		executorPool.execute(task);
	}

	// junta las muestras con mismo nombre y permite completar los datos de las objervaciones
	public void doAsignarValoresRecorrida(Recorrida recorrida) {
		main.configGUIController.doAsignarValoresRecorrida(recorrida);
	}	

	public void doExportRecorrida(Recorrida recorrida) {
		String nombre = recorrida.getNombre();
		File shapeFile = FileHelper.getNewShapeFile(nombre);
		ExportarRecorridaTask task = new ExportarRecorridaTask(recorrida,shapeFile);
		task.installProgressBar(progressBox);
		task.setOnSucceeded(handler -> {
			playSound();
			task.uninstallProgressBar();
		});
		executorPool.execute(task);
	}


}
