package gui.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.data.FileDataStore;

import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gui.CosechaHistoChart;
import gui.JFXMain;
import gui.Messages;
import gui.PoligonLayerFactory;
import gui.nww.LayerAction;
import gui.utils.SmartTableView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import tasks.ExportLaborMapTask;
import tasks.importar.OpenMargenMapTask;
import tasks.procesar.ClonarLaborMapTask;
import tasks.procesar.JuntarShapefilesTask;
import tasks.procesar.ResumirFertilizacionMapTask;
import tasks.procesar.ResumirMargenMapTask;
import utils.DAH;
import utils.FileHelper;

public class GenericLaborGUIController extends AbstractGUIController {


	public GenericLaborGUIController(JFXMain _main) {
		super(_main);
	}
	
	public void addAccionesLabor(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> laboresP = new ArrayList<LayerAction>();
		predicates.put(Labor.class, laboresP);
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.goToLayerAction"),(layer)->{	
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject!=null && Labor.class.isAssignableFrom(layerObject.getClass())){
				viewGoTo((Labor<?>) layerObject);
			}
			return "went to " + layer.getName(); 
		}));

		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.GuardarLabor"),(layer)->{
			main.enDesarrollo();
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doGuardarLabor((Labor<?>) layerObject);
			}
			return "guarde labor " + layer.getName();
		}));

		laboresP.add(LayerAction.constructPredicate(Messages.getString("GenericLaborGUIController.ClonarLabor"),(layer)->{
		
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doClonarLabor((Labor<?>) layerObject);
			}
			return "clone labor " + layer.getName();
		}));
		
		laboresP.add(LayerAction.constructPredicate(Messages.getString("GenericLaborGUIController.ResumirLabor"),(layer)->{
			
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doResumirLabor((Labor<?>) layerObject);
			}
			return "clone labor " + layer.getName();
		}));
		
		laboresP.add(LayerAction.constructPredicate(Messages.getString("GenericLaborGUIController.OutliersLabor"),(layer)->{
			
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doOutliersLabor((Labor<?>) layerObject);
			}
			return "filtre outliers labor " + layer.getName();
		}));


		
		
		/**
		 * Accion que muesta el histograma
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.showHistogramaLaborAction"),(layer)->{//	this::applyHistogramaCosecha);//(layer)->applyHistogramaCosecha(layer));
			showHistoLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "histograma mostrado" + layer.getName(); 
		}));

		/**
		 * Accion que permite extraer los poligonos de una cosecha para guardar
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.extraerPoligonoAction"),(layer)->{
			main.poligonoGUIController.doExtraerPoligonos((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "poligonos Extraidos " + layer.getName(); 
		}));
		
		
		/**
		 * Accion que permite extraer el contorno de una cosecha
		 * es solo de prueba. se puede realizar extrayendo poligonos y uniendolos
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("PoligonGUIController.extraerContornoAction"),(layer)->{
			main.poligonoGUIController.doExtraerContorno((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "poligonos Extraidos " + layer.getName(); 
		}));
		
		/**
		 * Accion que permite cortar una labor por el poligono/s seleccionado
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.cortarCosechaAction"),(layer)->{			
			main.poligonoGUIController.doCortarLaborPorPoligono((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "labor cortada" + layer.getName(); 
 
		}));

		/**
		 * Accion permite exportar la labor como shp
		 */
		laboresP.add(new LayerAction((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.exportLaborAction");  
			} else{
				doExportLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "labor Exportada" + layer.getName(); 
			}},Messages.getString("JFXMain.exportLaborAction")));

		/**
		 * Accion muestra una tabla con los datos de la cosecha
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.showTableLayerAction"),(layer)->{
			doShowDataTable((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "Tabla mostrada" + layer.getName(); 
		}));

		/**
		 * Accion permite obtener ndvi
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.downloadNDVI"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
			if(o instanceof Labor){
				main.ndviGUIController.doGetNdviTiffFile(o);
			}
			return "ndvi obtenido" + layer.getName();	 
		}));
	}
	
	public void addAccionesGenericas(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> todosP = new ArrayList<LayerAction>();
		predicates.put(Object.class, todosP);
		/**
		 * Accion que permite quitar un item del arbol
		 */
		todosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.removeLayerAction"),(layer)->{
			getWwd().getModel().getLayers().remove(layer);
			Object layerObject =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Labor.class.isAssignableFrom(layerObject.getClass())){
				Labor<?> l = (Labor<?>)layerObject;	
				l.dispose();
			}
			if(layerObject instanceof Poligono){
				Poligono poli = (Poligono) layerObject;
				poli.setActivo(false);
				if(poli.getId()!=null){
					DAH.save(poli);
				}
			}
			if(layerObject instanceof Ndvi){
				Ndvi ndvi = (Ndvi) layerObject;
				ndvi.setActivo(false);
				if(ndvi.getId()!=null){
					try {
						DAH.save(ndvi);
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			MeasureTool mt = (MeasureTool)layer.getValue(PoligonLayerFactory.MEASURE_TOOL);		
			if(mt!=null) {
				mt.setArmed(false);
				mt.dispose();
			}

			layer.dispose();
			getLayerPanel().update(getWwd());
			return "layer removido" + layer.getName(); 
		}));
		
		//editar opacidad
		//JFXMain.layerTransparencia=Transparencia
		todosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.layerTransparencia"),(layer)->{
			showTransparenciaSlider(layer);
			return "layer transparente" + layer.getName()+" "+layer.getOpacity(); 
		}));
	}

	public void showTransparenciaSlider(Layer layer) {
		double op = layer.getOpacity();
		//double newOp = op*0.5;
		
		 Slider slider = new Slider(0, 1, op);
		 slider.setShowTickMarks(true);
		 slider.setShowTickLabels(true);
		 slider.setMajorTickUnit(0.25f);
		 slider.setBlockIncrement(0.1f);
		 Scene sc = new Scene(slider,600,50);
		 //TODO fixme no se ve un layer a travez del otro
		 slider.valueProperty().addListener((obs,n,o)->{
			 
				layer.setOpacity(n.doubleValue());//newOp>0.1?newOp:1);
				this.getWwd().redraw();
				System.out.println("layer transparente" + layer.getName()+" "+layer.getOpacity());
		 });
		 Stage stage = new Stage();
		 stage.setScene(sc);
		 stage.initOwner(JFXMain.stage);
		 stage.getIcons().addAll(JFXMain.stage.getIcons());
		 stage.setTitle(Messages.getString("JFXMain.layerTransparencia")+" "+layer.getName());
		 stage.show();
	}	
	
	private void doClonarLabor(Labor<?> labor) {
		ClonarLaborMapTask umTask = new ClonarLaborMapTask(labor);
		umTask.installProgressBar(progressBox);

		//	testLayer();
		umTask.setOnSucceeded(handler -> {
			Labor ret = (Labor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);
	}
	
	private void doOutliersLabor(Labor<?> labor) {
		//TODO filtrar outliers
	}
	
	private void doResumirLabor(Labor<?> labor) {
		if(labor instanceof FertilizacionLabor) {
			ResumirFertilizacionMapTask uMmTask = new ResumirFertilizacionMapTask((FertilizacionLabor)labor);

			uMmTask.installProgressBar(progressBox);
			uMmTask.setOnSucceeded(handler -> {
				labor.getLayer().setEnabled(false);
				FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
				uMmTask.uninstallProgressBar();			
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
				playSound();
				viewGoTo(ret);
				System.out.println(Messages.getString("JFXMain.323")); 
			});
			executorPool.execute(uMmTask);
			
		}
	}
		
	private void doGuardarLabor(Labor<?> labor) {
		File zipFile = FileHelper.zipLaborToTmpDir(labor);//ok funciona
		byte[] byteArray = FileHelper.fileToByteArray(zipFile);		
		labor.setContent(byteArray);
		DAH.save(labor);//No se guardan las labores porque no extienden de entidad
	}

	public void doJuntarShapefiles() {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(null);
		File shapeFile = FileHelper.getNewShapeFile("union");
		//TODO agregar progress bar
		JuntarShapefilesTask task = new JuntarShapefilesTask(stores,shapeFile);
		task.installProgressBar(progressBox);

		task.setOnSucceeded(handler -> {
			playSound();
			task.uninstallProgressBar();
		});
		executorPool.execute(task);
	}
	
	private void showHistoLabor(Labor<?> cosechaLabor) {	
		Platform.runLater(()->{
			CosechaHistoChart histoChart = new CosechaHistoChart(cosechaLabor);
			Stage histoStage = new Stage();
			histoStage.setTitle(Messages.getString("CosechaHistoChart.Title"));
			histoStage.getIcons().add(new Image(JFXMain.ICON));
			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(JFXMain.stage);
			histoStage.show();
		});
	}
	
	private void doExportLabor(Labor<?> laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile =  FileHelper.getNewShapeFile(nombre);

		ExportLaborMapTask ehTask = new ExportLaborMapTask(laborToExport,shapeFile);
		ehTask.installProgressBar(progressBox);

		ehTask.setOnSucceeded(handler -> {
			playSound();
			ehTask.uninstallProgressBar();
		});
		executorPool.execute(ehTask);
	}
	
	/**
	 * metodo que toma una labor y muestra una tabla con los campos de la labor
	 * @param labor
	 */
	private void doShowDataTable(Labor<?> labor) {		   
		SmartTableView.showLaborTable(labor);
	}
	
}
