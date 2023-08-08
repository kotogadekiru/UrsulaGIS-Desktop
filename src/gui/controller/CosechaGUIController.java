package gui.controller;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.geotools.data.FileDataStore;

import dao.Labor;
import dao.Poligono;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.pulverizacion.PulverizacionLabor;
import dao.recorrida.Muestra;
import dao.recorrida.Recorrida;
import dao.siembra.SiembraLabor;
import dao.suelo.Suelo;
import dao.utils.PropertyHelper;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.RenderableLayer;
import gui.FertilizacionConfigDialogController;
import gui.HarvestConfigDialogController;
import gui.HarvestSelectDialogController;
import gui.JFXMain;
import gui.Messages;
import gui.PulverizacionConfigDialogController;
import gui.SiembraConfigDialogController;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import gui.nww.LayerPanel;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import tasks.crear.ConvertirAFertilizacionTask;
import tasks.crear.ConvertirAPulverizacionTask;
import tasks.crear.ConvertirASiembraTask;
import tasks.crear.ConvertirASueloTask;
import tasks.importar.ProcessHarvestMapTask;
import tasks.importar.ProcessSiembraMapTask;
import tasks.procesar.CortarCosechaMapTask;
import tasks.procesar.ExportarCosechaDePuntosTask;
import tasks.procesar.ExportarPrescripcionSiembraTask;
import tasks.procesar.GenerarRecorridaDirigidaTask;
import tasks.procesar.GrillarCosechasMapTask;
import tasks.procesar.RecomendFertNFromHarvestMapTask;
import tasks.procesar.RecomendFertPFromHarvestMapTask;
import tasks.procesar.SiembraFertTask;
import tasks.procesar.UnirCosechasMapTask;
import tasks.procesar.UnirSiembrasMapTask;
import utils.FileHelper;

public class CosechaGUIController {
	private JFXMain main=null;
	private Pane progressBox;
	private Executor executorPool;

	public CosechaGUIController(JFXMain _main) {
		this.main=_main;		
		this.progressBox=main.progressBox;
		this.executorPool=JFXMain.executorPool;
	}

	public void addCosechasRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();
		rootNodeP.add(new LayerAction(
				(layer)->{	this.doOpenCosecha(null);
				return "opened";	
				},Messages.getString("JFXMain.importar")));

		rootNodeP.add(new LayerAction(
				Messages.getString("JFXMain.unirCosechas"),
				(layer)->{
					this.doUnirCosechas(null);
					return "joined";	
				},
				2));

		getLayerPanel().addAccionesClase(rootNodeP,CosechaLabor.class);
	}

	public List<LayerAction> addAccionesCosecha(
			Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> cosechasP = new ArrayList<LayerAction>();
		predicates.put(CosechaLabor.class, cosechasP);

		/**
		 *Accion que permite editar una cosecha
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editCosechaAction"),(layer)->{
			doEditCosecha((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "cosecha editada" + layer.getName(); 

		}));

		/**
		 * Accion que permite pasar una grilla sobre la cosecha
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.grillarCosechaAction"),(layer)->{
			doGrillarCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "cosecha grillada" + layer.getName(); 
		}));

		/**
		 * Accion que permite clonar la cosecha
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.clonarCosechaAction"),(layer)->{
			doUnirCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "cosecha clonada" + layer.getName(); 
		}));

		/**
		 * Accion que permite pasar una cortar la cosecha por poligono
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.cortarCosechaAction"),(layer)->{
			doCortarCosecha((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "cosecha cortada" + layer.getName(); 
		}));

		/**
		 * Accion que muesta el la relacion entre el rinde y la elevacion
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.showHeightVsAmountChart"),(layer)->{
			main.showAmountVsElevacionChart((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "grafico mostrado " + layer.getName(); 
		}));

		//		/**
		//		 * Accion que permite generar un muestreo dirigido para los poligonos de la cosecha
		//		 */
		//		cosechasP.add(constructPredicate(Messages.getString("JFXMain.generarMuestreoDirigido"),(layer)->{
		//			doGenerarMuestreoDirigido((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
		//			return "muestreo dirigido " + layer.getName(); 
		//		}));

		/**
		 * Accion que permite generar una recorrida dirigida para los poligonos de la cosecha
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.generarMuestreoDirigido"),(layer)->{
			doGenerarRecorridaDirigida((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "recorrida dirigida " + layer.getName(); 
		}));

		/**
		 * Accion permite crear una fertilizacion P para reponer lo extraido por la cosecha
		 */
		cosechasP.add(new LayerAction( (layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.recompendarFertP");  
			} else{
				doRecomendFertPFromHarvest((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "Fertilizacion P Creada" + layer.getName(); 
			}},Messages.getString("JFXMain.recompendarFertP")));

		/**
		 * Accion permite crear una fertilizacion P para reponer lo extraido por la cosecha
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.recomendarFertN"),(layer)->{
			doRecomendFertNFromHarvest((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "Fertilizacion N Creada" + layer.getName(); 
		}));

		/**
		 * Accion permite crear un mapa de suelo desde un mapa de potencial de rendimiento
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.doCrearSuelo"),(layer)->{
			doCrearSuelo((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "Suelo Creado" + layer.getName(); 
		}));

		/**
		 * Accion permite crear un mapa de suelo desde un mapa de potencial de rendimiento
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.doCrearSiembra"),(layer)->{
			main.siembraGUIController.doCrearSiembra((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "Siembra Creada" + layer.getName(); 
		}));

		/**
		 * Accion permite crear un mapa de suelo desde un mapa de potencial de rendimiento
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.doCrearFertilizacion"),(layer)->{
			doCrearFertilizacion((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "Fertilizacion Creada" + layer.getName(); 
		}));

		/**
		 * Accion permite crear un mapa de suelo desde un mapa de potencial de rendimiento
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.doCrearPulverizacion"),(layer)->{
			doCrearPulverizacion((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "Pulverizacion Creada" + layer.getName(); 
		}));


		/**
		 * Accion permite exportar la cosecha como shp de puntos
		 */
		cosechasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportarCosechaAPuntosAction"),(layer)->{
			doExportHarvestDePuntos((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "cosecha exportada como puntos: " + layer.getName(); 
		}));

		Collections.sort(cosechasP);
		return cosechasP;
	}

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	public void doOpenCosecha(List<File> files) {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				CosechaLabor labor = new CosechaLabor(store);
				LaborLayer layer = new LaborLayer();
				labor.setLayer(layer);
				Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.306")); 
					labor.dispose();//libero los recursos reservados
					continue;
				}							

				ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(labor);
				umTask.installProgressBar(progressBox);
				umTask.setOnSucceeded(handler -> {
					CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					viewGoTo(ret);
					umTask.uninstallProgressBar();
					System.out.println(Messages.getString("JFXMain.307")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}

	// junta 2 o mas cosechas en una 
	private void doUnirCosechas(CosechaLabor cosechaLabor) {
		List<CosechaLabor> cosechasAUnir = new ArrayList<CosechaLabor>();
		if(cosechaLabor == null){
			List<CosechaLabor> cosechasEnabled = main.getCosechasSeleccionadas();
			cosechasAUnir.addAll( cosechasEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {
			cosechasAUnir.add(cosechaLabor);
		}
		boolean calibrar =false;

		if(cosechasAUnir.size()>1) {
			Alert calibrarAlert = new Alert(Alert.AlertType.CONFIRMATION);
			calibrarAlert.setTitle(Messages.getString("JFXMain.284")); 
			calibrarAlert.setContentText(Messages.getString("JFXMain.285")); 
			calibrarAlert.getButtonTypes().setAll(ButtonType.YES,ButtonType.NO);
			Optional<ButtonType> calibrarButton = calibrarAlert.showAndWait();
			if(calibrarButton.isPresent()){
				if(calibrarButton.get().equals(ButtonType.YES)){
					calibrar=true;
				}
			} else{
				return;
			}
		}
		UnirCosechasMapTask umTask = new UnirCosechasMapTask(cosechasAUnir);
		umTask.setCalibrar(calibrar);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
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

	private void doEditCosecha(CosechaLabor cConfigured ) {
		if(cConfigured==null){
			Optional<CosechaLabor> cosechaSelected = HarvestSelectDialogController.select(main.getCosechasSeleccionadas());
			if(cosechaSelected.isPresent()){
				cConfigured= cosechaSelected.get();
			} else {
				return;
			}
		}

		Optional<CosechaLabor> cosechaConfigured=HarvestConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(cConfigured);
			umTask.installProgressBar(progressBox);
			umTask.setOnSucceeded(handler -> {
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				main.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.279")); 
				playSound();
			});//fin del OnSucceeded						
			JFXMain.executorPool.execute(umTask);
		}
	}

	private void doGrillarCosechas(CosechaLabor cosechaAGrillar) {
		List<CosechaLabor> cosechasAUnir = new ArrayList<CosechaLabor>();
		if(cosechaAGrillar == null){
			List<CosechaLabor> cosechasEnabled = main.getCosechasSeleccionadas();
			cosechasAUnir.addAll( cosechasEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {
			cosechasAUnir.add(cosechaAGrillar);
		}
		TextInputDialog anchoDialog = new TextInputDialog(JFXMain.config.getPropertyOrDefault(CosechaConfig.ANCHO_GRILLA_KEY,Messages.getString("JFXMain.288"))); 
		anchoDialog.initOwner(JFXMain.stage);
		anchoDialog.setTitle(Messages.getString("JFXMain.289")); 
		anchoDialog.setContentText(Messages.getString("JFXMain.290")); 
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		if(anchoOptional.isPresent()){
			JFXMain.config.loadProperties();
			JFXMain.config.setProperty(CosechaConfig.ANCHO_GRILLA_KEY,anchoOptional.get());
			JFXMain.config.save();
		} else{
			return;
		}

		Alert rellenarHuecosAlert= new Alert(Alert.AlertType.CONFIRMATION);
		rellenarHuecosAlert.initOwner(JFXMain.stage);
		rellenarHuecosAlert.setTitle(Messages.getString("JFXMain.rellenar_huecos")); 
		rellenarHuecosAlert.setContentText(Messages.getString("JFXMain.rellenar_huecos")); 
		boolean rellenarHuecos = false;
		rellenarHuecosAlert.getButtonTypes().setAll(ButtonType.YES,ButtonType.NO);
		Optional<ButtonType> rellenarHuecosButton = rellenarHuecosAlert.showAndWait();
		if(rellenarHuecosButton.isPresent()){
			if(rellenarHuecosButton.get().equals(ButtonType.YES)){
				rellenarHuecos=true;
			}
		}

		GrillarCosechasMapTask umTask = new GrillarCosechasMapTask(cosechasAUnir);
		umTask.setRellenarHuecos(rellenarHuecos);
		double anchoGrilla =PropertyHelper.parseDouble(anchoOptional.get()).doubleValue();
		umTask.setAncho(anchoGrilla);
		umTask.installProgressBar(progressBox);
		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			if(ret.getLayer()!=null){
				insertBeforeCompass(getWwd(), ret.getLayer());
				cosechaAGrillar.getLayer().setEnabled(false);
				this.getLayerPanel().update(this.getWwd());
			}
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			System.out.println(Messages.getString("JFXMain.291")); 
			playSound();
		});//fin del OnSucceeded		
		JFXMain.executorPool.execute(umTask);
	}

	private void doCortarCosecha(CosechaLabor cosechaAcortar) {
		List<Poligono> geometriasActivas = main.poligonoGUIController.getEnabledPoligonos();

		geometriasActivas.stream().forEach((geom)->{
			CortarCosechaMapTask umTask = new CortarCosechaMapTask(cosechaAcortar,Collections.singletonList(geom));
			umTask.installProgressBar(progressBox);
			umTask.setOnSucceeded(handler -> {
				CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
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
		});
	}

	/**
	 * metodo que toma los poligonos de la labor y genera un mapa de puntos con las densidades configuradas
	 * preguntar si desea generar el muestreo por cantidad de muestras por poligono o densidad de muestras por poligono
	 * permitir configurar cantidad max y min de muestras
	 * permitir configurar superficie minima relevante
	 * @param l una Labor
	 */
	private void doGenerarRecorridaDirigida(Labor<?> l) {
		double superficieMinimaAMuestrear=0;
		double densidadDeMuestrasDeseada=0;
		double cantidadMinimaDeMuestrasPoligonoAMuestrear=0;

		TextInputDialog supMinDialog = new TextInputDialog(Messages.getString("JFXMain.238")); 
		supMinDialog.initOwner(JFXMain.stage);
		supMinDialog.setTitle(Messages.getString("JFXMain.239")); 
		supMinDialog.setContentText(Messages.getString("JFXMain.240")); 
		Optional<String> supMinOpt = supMinDialog.showAndWait();
		superficieMinimaAMuestrear = PropertyHelper.parseDouble(supMinOpt.get()).doubleValue();

		TextInputDialog densidadDialog = new TextInputDialog(Messages.getString("JFXMain.241")); 
		densidadDialog.initOwner(JFXMain.stage);
		densidadDialog.setTitle(Messages.getString("JFXMain.242")); 
		densidadDialog.setContentText(Messages.getString("JFXMain.243")); 
		Optional<String> densidadOptional = densidadDialog.showAndWait();
		densidadDeMuestrasDeseada =PropertyHelper.parseDouble(densidadOptional.get()).doubleValue();// Double.valueOf(densidadOptional.get());

		TextInputDialog cMinDialog = new TextInputDialog(Messages.getString("JFXMain.244")); 
		cMinDialog.initOwner(JFXMain.stage);
		cMinDialog.setTitle(Messages.getString("JFXMain.245")); 
		cMinDialog.setContentText(Messages.getString("JFXMain.246")); 
		Optional<String> cantOptional = cMinDialog.showAndWait();
		cantidadMinimaDeMuestrasPoligonoAMuestrear = PropertyHelper.parseDouble(cantOptional.get()).doubleValue();//Double.valueOf(cantOptional.get());

		GenerarRecorridaDirigidaTask umTask = new GenerarRecorridaDirigidaTask(Collections.singletonList(l),superficieMinimaAMuestrear,densidadDeMuestrasDeseada,cantidadMinimaDeMuestrasPoligonoAMuestrear);
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
	 * genera un layer de fertilizacion a partir de una cosecha
	 * el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
	 * que producto aplico y en que densidad por hectarea
	 * @param cosecha
	 */
	private void doRecomendFertNFromHarvest(CosechaLabor cosecha) {
		List<Suelo> suelosEnabled = main.getSuelosSeleccionados();
		List<FertilizacionLabor> fertEnabled = main.getFertilizacionesSeleccionadas();

		FertilizacionLabor fertN = new FertilizacionLabor();
		fertN.setLayer(new LaborLayer());

		fertN.setNombre(cosecha.getNombre()+Messages.getString("JFXMain.299")); 
		Optional<FertilizacionLabor> fertConfigured= FertilizacionConfigDialogController.config(fertN);
		if(!fertConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.300")); 
			return;
		}							

		//Dialogo preguntar min y max a aplicar
		Alert minMaxDialog = new Alert(AlertType.CONFIRMATION);
		NumberFormat df=Messages.getNumberFormat();
		TextField min = new TextField(df.format(0));
		TextField max = new TextField(df.format(0));

		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.301")),min)); 
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.302")),max)); 

		minMaxDialog.setGraphic(vb);
		minMaxDialog.setTitle(Messages.getString("JFXMain.303")); 
		minMaxDialog.setContentText(Messages.getString("JFXMain.304")); 
		minMaxDialog.initOwner(JFXMain.stage);
		Optional<ButtonType> res = minMaxDialog.showAndWait();
		Double minFert =null,maxFert=null; 
		if(res.get().equals(ButtonType.OK)){
			try {
				minFert=df.parse(min.getText()).doubleValue();
				if(minFert==0)minFert=null;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			try {
				maxFert=df.parse(max.getText()).doubleValue();
				if(maxFert==0)maxFert=null;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

		RecomendFertNFromHarvestMapTask umTask = 
				new RecomendFertNFromHarvestMapTask(
						fertN, cosecha,
						suelosEnabled, fertEnabled);
		umTask.setMinFert(minFert);
		umTask.setMaxFert(maxFert);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.305")); 
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);
	}
	//generar un layer de fertilizacion a partir de una cosecha
	//el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
	//que producto aplico y en que densidad por hectarea
	private void doRecomendFertPFromHarvest(CosechaLabor value) {
		FertilizacionLabor labor = new FertilizacionLabor();
		labor.setLayer(new LaborLayer());

		labor.setNombre(value.getNombre()+Messages.getString("JFXMain.292")); 
		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.293")); 
			return;
		}					

		//Dialogo preguntar min y max a aplicar
		Alert minMaxDialog = new Alert(AlertType.CONFIRMATION);
		NumberFormat df=Messages.getNumberFormat();
		TextField min = new TextField(df.format(0));
		TextField max = new TextField(df.format(0));

		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.294")),min)); 
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.295")),max)); 

		minMaxDialog.setGraphic(vb);
		minMaxDialog.setTitle(Messages.getString("JFXMain.296")); 
		minMaxDialog.setContentText(Messages.getString("JFXMain.297")); 
		minMaxDialog.initOwner(JFXMain.stage);
		Optional<ButtonType> res = minMaxDialog.showAndWait();
		Double minFert =null,maxFert=null; 
		if(res.get().equals(ButtonType.OK)){
			try {
				minFert=df.parse(min.getText()).doubleValue();
				if(minFert==0)minFert=null;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			try {
				maxFert=df.parse(max.getText()).doubleValue();
				if(maxFert==0)maxFert=null;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

		RecomendFertPFromHarvestMapTask umTask = new RecomendFertPFromHarvestMapTask(labor,value);
		umTask.setMinFert(minFert);
		umTask.setMaxFert(maxFert);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.298")); 
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);
	}

	/**
	 * toma una cosecha, pregunta los resultados de los analisis para las clases de la cosecha
	 * y crea un mapa de suelo teniendo la informacion ingresada y la categoria a la que pertenece cada poligono
	 * @param cosecha
	 */
	private void doCrearSuelo(CosechaLabor cosecha) {
		Recorrida recorrida = null;
		List<Recorrida> recorridasActivas = main.getRecorridasActivas();
		if(recorridasActivas.size()>0) {
			recorrida=recorridasActivas.get(0);
		} else { // si no hay una recorrida seleccionada crear una nueva
			recorrida = new Recorrida();
			recorrida.setNombre(cosecha.getNombre());
			List<Muestra> muestras = new ArrayList<Muestra>();
			for(int i=0;i<cosecha.getClasificador().getNumClasses();i++) {
				Muestra m = new Muestra();
				m.initObservacionSuelo();
				m.setNombre(cosecha.getClasificador().getLetraCat(i));
				m.setRecorrida(recorrida);				
				muestras.add(m);
			}
			//si viene con recorridas seleccionadas permito editarlas?
			recorrida.setMuestras(muestras);
			main.configGUIController.doAsignarValoresRecorrida(recorrida);//esto guarda una recorrida nueva
		}
		ConvertirASueloTask csTask = new ConvertirASueloTask(cosecha,recorrida);
		csTask.installProgressBar(progressBox);
		csTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			csTask.uninstallProgressBar();
			viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(csTask);
	}



	/**
	 * toma una cosecha, pregunta las densidades deseadas para cada ambiente
	 * y crea una siembra teniendo la informacion ingresada y la categoria a la que pertenece cada poligono
	 * @param cosecha
	 */
	private void doCrearFertilizacion(CosechaLabor cosecha) {
		FertilizacionLabor labor = new FertilizacionLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		labor.setNombre(cosecha.getNombre()+" "+Messages.getString("JFXMain.255"));  
		Optional<FertilizacionLabor> siembraConfigured= FertilizacionConfigDialogController.config(labor);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); 
			labor.dispose();//libero los recursos reservados
			return;
		}		
		Map<String,Double[]> mapClaseValor = main.configGUIController.doAsignarValoresCosecha(cosecha,new String[] {Messages.getString("JFXMain.Dosis")});//"Densidad pl/m2"
		ConvertirAFertilizacionTask csTask = new ConvertirAFertilizacionTask(cosecha,labor,mapClaseValor);
		csTask.installProgressBar(progressBox);
		csTask.setOnSucceeded(handler -> {
			cosecha.getLayer().setEnabled(false);
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			csTask.uninstallProgressBar();
			viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(csTask);
	}

	/**
	 * toma una cosecha, pregunta las densidades deseadas para cada ambiente
	 * y crea una siembra teniendo la informacion ingresada y la categoria a la que pertenece cada poligono
	 * @param cosecha
	 */
	private void doCrearPulverizacion(CosechaLabor cosecha) {
		PulverizacionLabor labor = new PulverizacionLabor();
		labor.setNombre(cosecha.getNombre()+" "+Messages.getString("JFXMain.pulverizacion"));  
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);

		Optional<PulverizacionLabor> siembraConfigured= PulverizacionConfigDialogController.config(labor);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); 
			labor.dispose();//libero los recursos reservados
			return;
		}		
		Map<String,Double[]> mapClaseValor = main.configGUIController.doAsignarValoresCosecha(cosecha,new String[] {Messages.getString("JFXMain.Dosis")});//"Densidad pl/m2"
		ConvertirAPulverizacionTask csTask = new ConvertirAPulverizacionTask(cosecha,labor,mapClaseValor);
		csTask.installProgressBar(progressBox);

		csTask.setOnSucceeded(handler -> {
			cosecha.getLayer().setEnabled(false);
			PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			csTask.uninstallProgressBar();
			viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(csTask);
	}

	private void doExportHarvestDePuntos(CosechaLabor laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile = FileHelper.getNewShapeFile(nombre);
		executorPool.execute(()->ExportarCosechaDePuntosTask.run(laborToExport, shapeFile));
	}
	//XXX insert aqui

	/* metodos de conveniencia para el refactor
	 */
	private void insertBeforeCompass(WorldWindow wwd, LaborLayer layer) {
		JFXMain.insertBeforeCompass(wwd, layer);		
	}

	private LayerPanel getLayerPanel() {		
		return main.getLayerPanel();
	}

	private WorldWindow getWwd() {		
		return main.getWwd();
	}

	private void viewGoTo(Labor<?> ret) {
		main.viewGoTo(ret);		
	}

	private void playSound() {
		main.playSound();

	}
}