package com.ursulagis.desktop.gui.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import com.ursulagis.desktop.gui.BulkNdviDownloadGUI;
import com.ursulagis.desktop.gui.ExportNDVIToExcel;
import com.ursulagis.desktop.gui.ExportNDVIToKMZ;
import com.ursulagis.desktop.gui.FertilizacionConfigDialogController;
import com.ursulagis.desktop.gui.HarvestConfigDialogController;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.NDVIChart;
import com.ursulagis.desktop.gui.NDVIDatePickerDialog;
import com.ursulagis.desktop.gui.NDVIHistoChart;
import com.ursulagis.desktop.gui.ShowNDVIEvolution;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.gui.nww.LayerAction;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import com.ursulagis.desktop.gui.utils.DateConverter;
import com.ursulagis.desktop.gui.utils.DateRangeSlider;
import com.ursulagis.desktop.gui.utils.NumberInputDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.ursulagis.desktop.tasks.GetNdviForLaborTask4;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.ShowNDVITifFileTask;
import com.ursulagis.desktop.tasks.crear.ConvertirNdviACosechaTask;
import com.ursulagis.desktop.tasks.crear.ConvertirNdviAFertilizacionTask;
import com.ursulagis.desktop.tasks.crear.ConvertirNdviAcumuladoACosechaTask;
import com.ursulagis.desktop.tasks.importar.ProcessHarvestMapTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.GeometryHelper;

public class NdviGUIController extends AbstractGUIController{


	public List<Ndvi> ndviActivos=null;//secargan en el init de JFXMain
	public NdviGUIController(JFXMain _main) {
		super(_main);
	}

	public void addNdviRootNodeActions() {
		List<LayerAction> rootNodeNDVI = new ArrayList<LayerAction>();

		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.evoNDVI"),(layer)->{ 
			ShowNDVIEvolution sEvo= new ShowNDVIEvolution(this.getWwd(),this.getLayerPanel());
			sEvo.doShowNDVIEvolution();
			return "mostre la evolucion del ndvi";
		}));

		rootNodeNDVI.add(LayerAction.constructPredicate(
				Messages.getString("JFXMain.show_ndvi_chart"),
				(layer)->{ 
					return doShowNdviChart();
				}));

		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.show_ndvi_acum_chart"),
				(layer)->{
					return doShowNdviAcumChart();
				}));

		//Exporta todos los ndvi cargados a un archivo excel donde las filas son las coordenadas y las columnas son los valores en esa fecha
		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("NdviGUIController.ConvertirNdviAcumuladoACosecha.title"),(layer)->{ 
			doConvertirNdviAcumuladoACosecha();

			return "converti a cosecha acumulando";
		}));
		
		//TODO agregar traduccion		
		rootNodeNDVI.add(LayerAction.constructPredicate("Filtrar Fecha",(layer)->{ 
			return doFiltrarFecha(null);
			//return "filtre por fecha";
		}));

		//Exporta todos los ndvi cargados a un archivo excel donde las filas son las coordenadas y las columnas son los valores en esa fecha
		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.expoNDVI"),(layer)->{ 
			ExportNDVIToExcel sEvo= new ExportNDVIToExcel(this.getWwd(),getLayerPanel());
			sEvo.exportToExcel();
			return "mostre la evolucion del ndvi";
		}));

		/**
		 * Save NDVI action
		 * guarda todos los ndvi activos en la rama de ndvi
		 */
		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.saveAction"),
				(layer)->{	executorPool.submit(()->{
					try {
						
						List<Ndvi> ndviToSave = main.getNdviSeleccionados();
						DAH.saveAll(ndviToSave);
						
//						LayerList layers = this.getWwd().getModel().getLayers();
//						for (Layer l : layers) {
//							Object o =  l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//							if(o instanceof Ndvi){									
//								Ndvi ndvi = (Ndvi)o;	
//								try {
//									DAH.save(ndvi);
//								}catch(Exception e) {
//									System.err.println("Error al guardar el ndvi "+ndvi.getNombre()); 
//									e.printStackTrace();
//								}
//							}
//						}
					}catch(Exception e) {
						System.err.println("Error al guardar los poligonos"); 
						e.printStackTrace();
					}
				});
				return "Guarde los ndvi"; 
				}));
		getLayerPanel().addAccionesClase(rootNodeNDVI,Ndvi.class);
	}

	public String doShowNdviAcumChart() {
		Platform.runLater(()->{
			NDVIChart sChart= new NDVIChart(this.getWwd());
			sChart.doShowNDVIChart(true);
			Stage histoStage = new Stage();
			histoStage.setTitle(Messages.getString("JFXMain.show_ndvi_acum_title"));
			histoStage.getIcons().addAll(JFXMain.stage.getIcons());
			VBox.setVgrow(sChart, Priority.ALWAYS);
			Scene scene = new Scene(sChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(JFXMain.stage);
			histoStage.show();
		});
		return "mostre el grafico del ndvi";
	}

	public String doShowNdviChart() {
		Platform.runLater(()->{
			NDVIChart sChart= new NDVIChart(this.getWwd());
			sChart.doShowNDVIChart(false);
			Stage histoStage = new Stage();
			histoStage.setTitle(Messages.getString("JFXMain.show_ndvi_title"));
			histoStage.getIcons().addAll(JFXMain.stage.getIcons());
			VBox.setVgrow(sChart, Priority.ALWAYS);
			Scene scene = new Scene(sChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(JFXMain.stage);
			histoStage.show();
		});
		return "mostre el grafico del ndvi";
	}


	public void addAccionesNdvi(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> ndviP = new ArrayList<LayerAction>();
		predicates.put(Ndvi.class, ndviP);

		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editarLayer"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Ndvi.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Ndvi ndvi =(Ndvi)layerObject;
				TextInputDialog nombreDialog = new TextInputDialog(ndvi.getNombre());
				nombreDialog.initOwner(main.stage);
				nombreDialog.setTitle(Messages.getString("JFXMain.editarLayerDialogTitle")); 
				nombreDialog.setContentText(Messages.getString("JFXMain.editarLayerNDVIName")); 

				Optional<String> nombreOptional = nombreDialog.showAndWait();
				if(nombreOptional.isPresent()){
					ndvi.setNombre(nombreOptional.get());
					NumberFormat df = Messages.getNumberFormat();
					layer.setName(ndvi.getNombre()+" "+df.format(ndvi.getPorcNubes()*100)+"% Nublado");
					this.getLayerPanel().update(this.getWwd());
				}
			}
			return "edite ndvi"; 
		}));

		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.convertirNDVIaCosechaAction"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(o instanceof Ndvi){
				doConvertirNdviACosecha((Ndvi) o);
			}
			return "rinde estimado desde ndvi" + layer.getName(); 
		}));

		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.convertirNDVIaFertInversaAction"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(o instanceof Ndvi){
				doConvertirNdviAFertilizacion((Ndvi) o);
			}
			return "rinde estimado desde ndvi" + layer.getName(); 
		}));

		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.mostrarNDVIChartAction"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(o instanceof Ndvi){
				showHistoNDVI((Ndvi)o);
			}
			return "histograma ndvi mostrado" + layer.getName(); 
		}));

		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.goToNDVIAction"),(layer)->{
			Object zoomPosition = layer.getValue(ProcessMapTask.ZOOM_TO_KEY);		
			if (zoomPosition==null){
			}else if(zoomPosition instanceof Position){
				Position pos =(Position)zoomPosition;
				viewGoTo(pos);
			}
			return "went to " + layer.getName(); 
		}));

		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.guardarNDVIAction"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(o instanceof Ndvi){
				Ndvi ndvi = (Ndvi)o;
				DAH.save(ndvi);
			}
			return "guarde" + layer.getName(); 
		}));

		/*
		 * funcionalidad que permite guardar el archivo tiff de este ndvi en una ubicacion definida por el usuario
		 */
		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportarNDVItoTIFFAction"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(o instanceof Ndvi){
				Ndvi ndvi = (Ndvi)o;
				doExportarTiffFile(ndvi);
			}
			return "exporte" + layer.getName(); 
		}));

		/*
		 * funcionalidad que permite guardar el archivo tiff de este ndvi en una ubicacion definida por el usuario
		 */
		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.expoNDVIToKML"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(o instanceof Ndvi){
				Ndvi ndvi = (Ndvi)o;
				ExportNDVIToKMZ toKMZ= new ExportNDVIToKMZ(this.getWwd(),this.getLayerPanel());				
				toKMZ.exportToKMZ(ndvi);
			}
			return "exporte" + layer.getName(); 
		}));
	}


	private void doExportarTiffFile(Ndvi ndvi) {
		File dir =FileHelper.getNewTiffFile(ndvi.getFileName());
		try{
			FileOutputStream fos = new FileOutputStream(dir);
			fos.write(ndvi.getContent());
			fos.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void showHistoNDVI(Ndvi  ndvi) {
		Task<Parent> pfMapTask = new Task<Parent>(){
			@Override
			protected Parent call() throws Exception {
				try{	
					NDVIHistoChart histoChart = new NDVIHistoChart(ndvi);
					return histoChart;
				}catch(Throwable t){
					t.printStackTrace();
					System.out.println("no hay ningun ndvi para mostrar"); 
					return new VBox(new Label(Messages.getString("JFXMain.207"))); 
				}
			}			
		};

		pfMapTask.setOnSucceeded(handler -> {
			Parent	histoChart = (Parent) handler.getSource().getValue();	
			Stage histoStage = new Stage();
			histoStage.getIcons().addAll(JFXMain.stage.getIcons());
			histoStage.setTitle(Messages.getString("NDVIHistoChart.Title"));
			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(JFXMain.stage);
			histoStage.show();
		});
		executorPool.submit(pfMapTask);
	}

	private void doConvertirNdviACosecha(Ndvi ndvi) {
		CosechaLabor labor = new CosechaLabor();
		labor.setNombre(ndvi.getNombre());

		Date date = new Date();
		try{
			date = java.util.Date.from(ndvi.getFecha().atStartOfDay()		
				.atZone(ZoneId.systemDefault())
				.toInstant());
		}catch(Exception e ){
			e.printStackTrace();
		}
		labor.setFecha(date);
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			//System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha"); 
			labor.dispose();//libero los recursos reservados
			return;
		}							

		Double rinde = null;
		try {
			Double rindeEsperado = cosechaConfigured.get().getCultivo().getRindeEsperado();
			TextInputDialog rindePromDialog = new TextInputDialog(Messages.getNumberFormat().format(rindeEsperado));//Messages.getString("JFXMain.272")); 
			rindePromDialog.setTitle(Messages.getString("JFXMain.273")); 
			rindePromDialog.setContentText(Messages.getString("JFXMain.274")); 
			rindePromDialog.initOwner(JFXMain.stage);
			Optional<String> rPromOptional = rindePromDialog.showAndWait();
			rinde = PropertyHelper.parseDouble(rPromOptional.get()).doubleValue();//Double.valueOf(anchoOptional.get());

			//XXX ingresar metodo de estimacion?
			//XXX ingresar min, max,amplitud, alfa, beta?<- indica la pendiente de la sigmoidea
			//XXX para la fecha y el cultivo tendria que haber coeficientes promedio alfa y beta que mejor ajusten.
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

		ConvertirNdviACosechaTask umTask = new ConvertirNdviACosechaTask(cosechaConfigured.get(),ndvi,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			umTask.uninstallProgressBar();
			ndvi.getLayer().setEnabled(false);

			ProcessHarvestMapTask pmtask = new ProcessHarvestMapTask(ret);
			pmtask.installProgressBar(progressBox);
			pmtask.setOnSucceeded(handler2 -> {
				this.getLayerPanel().update(this.getWwd());
				pmtask.uninstallProgressBar();
				main.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.279")); 
				playSound();
				main.viewGoTo(ret);
			});
			pmtask.run();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	private void doConvertirNdviAcumuladoACosecha() {
		List<Ndvi> ndvis = main.getNdviSeleccionados();
		ndvis.sort((n1,n2)->n1.compareTo(n2));
		Ndvi ndvi = ndvis.get(0);
		Ndvi lNdvi = ndvis.get(ndvis.size()-1);
		CosechaLabor labor = new CosechaLabor();
		//String YYYY_MM_DD = "yyyy-MM-dd";
		DateTimeFormatter format1 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		//LocalDate fecha = LocalDate.parse(v[0], format1);
		//ndvi.setFecha(fecha);
		labor.setNombre(ndvi.getNombre()+" -> "+format1.format(lNdvi.getFecha()));

		Date date = java.util.Date.from(ndvi.getFecha().atStartOfDay()
				.atZone(ZoneId.systemDefault())
				.toInstant());

		labor.setFecha(date);
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			//System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha"); 
			labor.dispose();//libero los recursos reservados
			return;
		}							

		Double diasNdviPorTn = null;

		Double diasNdviPorTnEsperado = 10.0;//cosechaConfigured.get().getCultivo().getRindeEsperado();
		diasNdviPorTn = NumberInputDialog.showAndWait(
				Messages.getString("NdviGUIController.ConvertirNdviAcumuladoACosecha.diasNdvi.title"), //titulo: "Ingrese dias por ndvi por Tn"
				Messages.getString("NdviGUIController.ConvertirNdviAcumuladoACosecha.diasNdvi.text"), //"Dias NDVI por TN"
				Messages.getString("NdviGUIController.ConvertirNdviAcumuladoACosecha.diasNdvi.var"),//"Dias NDVI por TN"
				PropertyHelper.formatDouble(diasNdviPorTnEsperado), 
				Messages.getString("JFXMain.SeparatorWarningTooltip"));
		if(diasNdviPorTn.isNaN())return;

		ConvertirNdviAcumuladoACosechaTask umTask = 
				new ConvertirNdviAcumuladoACosechaTask(
						cosechaConfigured.get(),
						ndvis,
						diasNdviPorTn);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			for(Ndvi n:ndvis) {
				n.getLayer().setEnabled(false);
			}
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			main.viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("crear ndvis a cosecha acumulada exito"); 
			playSound();
	
			
	
			
			/* se reprocesa el mapa para ejecutar los filtros si se configuro alguno*/
//			ProcessHarvestMapTask pmtask = new ProcessHarvestMapTask(ret);
//			pmtask.installProgressBar(progressBox);
//			pmtask.setOnSucceeded(handler2 -> {
//				this.getLayerPanel().update(this.getWwd());
//				pmtask.uninstallProgressBar();
//				main.wwjPanel.repaint();
//				System.out.println(Messages.getString("JFXMain.279")); 
//				playSound();
//				main.viewGoTo(ret);
//			});
//			pmtask.run();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		

	}

	//TODO obtener un rango de fechas a activar y desactivar los layers fuera de esas fechas
	private String doFiltrarFecha(Layer layer) {
		
		LocalDate min=null,//.MIN,
				max=null,//LocalDate.MAX,
				low = LocalDate.now().minusMonths(1),
				high=LocalDate.now().minusMonths(1);
		List<Ndvi> ndviCargados = (List<Ndvi>) main.getObjectFromLayersOfClass(Ndvi.class);
		System.out.println("ndvi cargados "+ndviCargados.size());
		for(Ndvi n:ndviCargados) {
			
			LocalDate fecha =n.getFecha();
			System.out.println("revisando ndvi con fecha "+fecha);
			if(max==null || max.isBefore(fecha)) {
				max=fecha;
			}
			if(min==null || min.isAfter(fecha)) {
				min=fecha;
			} 
		}
		System.out.println("creando filtro min "+min+" max "+max);
		low=min;
		high=max;
		DateRangeSlider slider = new DateRangeSlider(min,max, low,high);
		
		
		slider.setOnUpdate((Void)->{
			LocalDate nlow=slider.getLow();
			LocalDate nhigh=slider.getHigh();
			System.out.println("filtre por fecha low "+nlow+" high "+nhigh);
			for(Ndvi n:ndviCargados) {
				LocalDate fecha =n.getFecha();
				n.getLayer().setEnabled(true);
				if(nlow!=null && nlow.isAfter(fecha)) {
					n.getLayer().setEnabled(false);
				}
				if(nhigh!=null && nhigh.isBefore(fecha)) {
					n.getLayer().setEnabled(false);
				} 
			}
			this.getLayerPanel().update(this.getWwd());
		});
		
		slider.showDateSlider();
		return "filtre por fecha low "+low+" high "+high;
		
	}

	private void doConvertirNdviAFertilizacion(Ndvi ndvi) {
		FertilizacionLabor labor = new FertilizacionLabor();
		labor.setNombre(ndvi.getNombre());

		Date date = java.util.Date.from(ndvi.getFecha().atStartOfDay()
				.atZone(ZoneId.systemDefault())
				.toInstant());

		labor.setFecha(date);
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			//System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha"); 
			labor.dispose();//libero los recursos reservados
			return;
		}	
		//JFXMain.294=Fert Min
		//JFXMain.295=Fert Max
		DecimalFormat format=PropertyHelper.getDoubleConverter();//(DecimalFormat) Messages.getNumberFormat();
		Double dosisMax = null;
		try {
			TextInputDialog dMaxDialog = new TextInputDialog(Messages.getString("JFXMain.295")); //fertMax 
			dMaxDialog.setTitle(Messages.getString("JFXMain.295")); 
			dMaxDialog.setContentText(Messages.getString("JFXMain.295")); 
			dMaxDialog.initOwner(JFXMain.stage);
			Optional<String> dMaxOpt = dMaxDialog.showAndWait();
			System.out.println("opt max "+ dMaxOpt.get());
			dosisMax = format.parse(dMaxOpt.get()).doubleValue();
		}catch(java.lang.NumberFormatException | ParseException e) {
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

		Double dosisMin = null;
		try {
			TextInputDialog dMinDialog = new TextInputDialog(Messages.getString("JFXMain.294")); 
			dMinDialog.setTitle(Messages.getString("JFXMain.294")); 
			dMinDialog.setContentText(Messages.getString("JFXMain.294")); 
			dMinDialog.initOwner(JFXMain.stage);
			Optional<String> dMinOpt= dMinDialog.showAndWait();
			dosisMin = format.parse(dMinOpt.get()).doubleValue();// Double.valueOf(dMinOpt.get());
		}catch(java.lang.NumberFormatException | ParseException e) {
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

		Double ndviMin = null;
		try {
			TextInputDialog ndviMinDialog = new TextInputDialog("NDVI Min"); 
			ndviMinDialog.setTitle("NDVI Min"); 
			ndviMinDialog.setContentText("NDVI Min"); 
			ndviMinDialog.initOwner(JFXMain.stage);
			Optional<String> ndviMinOpt = ndviMinDialog.showAndWait();
			ndviMin = format.parse(ndviMinOpt.get()).doubleValue();//Double.valueOf(dMinOpt.get());
		}catch(java.lang.NumberFormatException | ParseException e) {
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

		Double ndviMax = null;
		try {
			TextInputDialog ndviMaxDialog = new TextInputDialog("NDVI Max"); 
			ndviMaxDialog.setTitle("NDVI Max"); 
			ndviMaxDialog.setContentText("NDVI Max"); 
			ndviMaxDialog.initOwner(JFXMain.stage);
			Optional<String> ndviMaxOpt= ndviMaxDialog.showAndWait();

			ndviMax = format.parse(ndviMaxOpt.get()).doubleValue();//Double.valueOf(dMinOpt.get());
		}catch(java.lang.NumberFormatException | ParseException e) {
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

		//con esto se decide si el mapa tiene correccion Outlayers
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Filtrado con Outlayers");
		alert.setHeaderText("Desea Suavizar el mapa con Outlayes");
		alert.setContentText("Seleccione OK para usar oulayers");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK){
			labor.getConfig().correccionOutlayersProperty().set(true);
		} else {
			labor.getConfig().correccionOutlayersProperty().set(false);		
		}

		ConvertirNdviAFertilizacionTask umTask = new ConvertirNdviAFertilizacionTask(labor,ndvi,dosisMax,dosisMin);
		umTask.ndviMin=ndviMin;
		umTask.ndviMax=ndviMax;
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			main.viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("convertir a fertiliacion tuvo exito"); 
			playSound();
			ndvi.getLayer().setEnabled(false);
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	public void showNdvi( Object placementObject,Ndvi _ndvi) {
		showNdvi(placementObject,_ndvi,true);
	}

	public void showNdvi( Object placementObject,Ndvi _ndvi,boolean goTo) {
		if(_ndvi!=null)System.out.println("showing ndvi "+_ndvi.getNombre());
		ShowNDVITifFileTask task = new ShowNDVITifFileTask(_ndvi);
		if( placementObject!=null && Poligono.class.isAssignableFrom(placementObject.getClass())){
			task.setPoligono((Poligono) placementObject);
		} else 	if( placementObject!=null && Labor.class.isAssignableFrom(placementObject.getClass())){
			Labor<?> l = (Labor<?>) placementObject;
			Geometry contornoG = GeometryHelper.extractContornoGeometry(l);
			Poligono contornoP = GeometryHelper.constructPoligono(contornoG);
			task.setPoligono(contornoP);
		}
		task.setOnSucceeded(handler -> {
			Layer ndviLayer = (Layer) handler.getSource().getValue();	
			if(ndviLayer != null) {
				insertBeforeCompass(getWwd(), ndviLayer);
				this.getLayerPanel().update(this.getWwd());
				if(goTo) {
					viewGoTo(ndviLayer);
					playSound();
				}
			}
		});
		executorPool.execute(task);
	}

	/**
	 * tomar un Ndvi y mostrarlo como layer
	 * @param ndvi
	 */
	public void doShowNDVI(Ndvi ndvi) {
		executorPool.submit(()->{			
			showNdvi(null,ndvi);
		});
	}

	public void showNdviActivos() {
		
		for(int i=0;i<ndviActivos.size();i++) {
			
			Ndvi ndvi = ndviActivos.get(i);
			boolean isLast = i==(ndviActivos.size()-1);

			if(ndvi!=null)System.out.println("showing ndvi "+ndvi.getNombre());
			Platform.runLater(()->{
			showNdvi(null,ndvi,isLast);
			});
		}
	}


	public void showNdviTiffFile(File file, Object placementObject) {
		//if(_ndvi!=null)System.out.println("showing ndvi "+_ndvi.getNombre());
		ShowNDVITifFileTask task = new ShowNDVITifFileTask(file);
		if( placementObject!=null && Poligono.class.isAssignableFrom(placementObject.getClass())){
			task.setPoligono((Poligono) placementObject);
		}
		task.setOnSucceeded(handler -> {
			Layer ndviLayer = (Layer) handler.getSource().getValue();	
			if(ndviLayer != null) {
				insertBeforeCompass(getWwd(), ndviLayer);
				this.getLayerPanel().update(this.getWwd());
				viewGoTo(ndviLayer);
				playSound();	
			}
		});
		executorPool.execute(task);
	}

	/**
	 * descargar los tiff correspondientes a un polygono y mostrarlos como ndvi
	 * @param placementObject
	 */
	@SuppressWarnings("unchecked")
	public void doGetNdviTiffFile(Object placementObject) {//ndvi2
		final Object plo=placementObject;
		LocalDate fin =null;
		if(placementObject !=null && Labor.class.isAssignableFrom(placementObject.getClass())){
			fin= DateConverter.asLocalDate((Date)((Labor<?>)placementObject).getFecha());
		} 

		NDVIDatePickerDialog ndviDpDLG = new NDVIDatePickerDialog(JFXMain.stage);
		LocalDate ret = ndviDpDLG.ndviDateChooser(fin);
		if(ret ==null)return;//seleccionar fecha termino en cancel.

		if(ndviDpDLG.finalDate != null){
			ObservableList<Ndvi> observableList = FXCollections.observableArrayList(new ArrayList<Ndvi>());
			observableList.addListener((ListChangeListener<Ndvi>) c -> {				
				if(c.next()){
					c.getAddedSubList().forEach((ndvi)->{
						doShowNDVI(ndvi);
					});//fin del foreach
				}			
			});
			if(placementObject !=null && Labor.class.isAssignableFrom(placementObject.getClass())){
				Labor<?> l =(Labor<?>)placementObject;


				Geometry contornoG = GeometryHelper.extractContornoGeometry(l);
				Poligono contornoP = GeometryHelper.constructPoligono(contornoG);

				placementObject =  contornoP;
				//				ReferencedEnvelope bounds =
				//				Polygon pol = GeometryHelper.constructPolygon(bounds);
				//				placementObject =GeometryHelper.constructPoligono(pol);

			} 
			GetNdviForLaborTask4 task = new GetNdviForLaborTask4((Poligono)placementObject, observableList);
			task.setBeginDate(ndviDpDLG.initialDate);
			task.setFinDate(ndviDpDLG.finalDate);
			task.setIgnoreNDVI((List<Ndvi>) main.getObjectFromLayersOfClass(Ndvi.class));

			System.out.println("procesando los datos entre "+ndviDpDLG.initialDate+" y "+ ndviDpDLG.finalDate);//hasta aca ok!
			task.installProgressBar(progressBox);
			task.setOnSucceeded(handler -> {
				if(plo instanceof Poligono){
					((Poligono)plo).getLayer().setEnabled(false);
				}
				task.uninstallProgressBar();
				System.out.println("termine de descargar todos los ndvi de "+plo);
			});
			executorPool.submit(task);
		}
	}



	/**
	 * seleccionar archivos .tif y mostrarlos como Ndvi
	 */
	public void doOpenNDVITiffFiles() {
		List<File>	files =FileHelper.chooseFiles("TIF","*.tif");  
		if(files!=null)	files.forEach((file)->{
			showNdviTiffFile(file,null);
		});//fin del foreach
	}
	/**
	 * metodo que muestra una tabla con poligonos que se pueden seleccionar para descargar el valor de los ndvi
	 * de cada uno dentro de un periodo determinado
	 */
	public void doBulkNDVIDownload() {
		BulkNdviDownloadGUI gui = new BulkNdviDownloadGUI();
		gui.show();
	}

	//metodos de conveniencia para el refactor


	//	private LayerPanel getLayerPanel() {		
	//		return main.getLayerPanel();
	//	}
	//
	//	private WorldWindow getWwd() {		
	//		return main.getWwd();
	//	}
	//	
	//	private void viewGoTo(Position position) {
	//		main.viewGoTo(position);
	//	}



	//	private void playSound() {
	//		main.playSound();
	//		
	//	}
}
