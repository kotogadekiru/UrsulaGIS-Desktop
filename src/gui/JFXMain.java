package gui;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;

import dao.Labor;
import dao.LaborItem;
import dao.Ndvi;
import dao.Poligono;
import dao.config.Campania;
import dao.config.Configuracion;

import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Fertilizante;
import dao.config.Lote;
import dao.config.Semilla;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraLabor;
import dao.suelo.Suelo;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.data.BufferedImageRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.event.RenderingExceptionListener;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.UnitsFormat;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwind.util.measure.MeasureToolController;
import gov.nasa.worldwindx.examples.util.ExampleUtil;

import gui.nww.LaborLayer;
import gui.nww.LayerPanel;
import gui.nww.WWPanel;

import gui.utils.DateConverter;
import gui.utils.SmartTableView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tasks.ExportLaborMapTask;
import tasks.GetNDVIForLaborTask;
import tasks.GetNDVI2ForLaborTask;
import tasks.GoogleGeocodingHelper;
import tasks.ProcessMapTask;
import tasks.ShowNDVITifFileTask;
import tasks.UpdateTask;
import tasks.crear.ConvertirNdviACosechaTask;
import tasks.crear.CrearCosechaMapTask;
import tasks.crear.CrearFertilizacionMapTask;
import tasks.crear.CrearPulverizacionMapTask;
import tasks.crear.CrearSiembraMapTask;
import tasks.crear.CrearSueloMapTask;
import tasks.importar.OpenMargenMapTask;
import tasks.importar.OpenSoilMapTask;
import tasks.importar.ProcessFertMapTask;
import tasks.importar.ProcessHarvestMapTask;
import tasks.importar.ProcessPulvMapTask;
import tasks.importar.ProcessSiembraMapTask;
import tasks.procesar.ExtraerPoligonosDeCosechaTask;
import tasks.procesar.GrillarCosechasMapTask;
import tasks.procesar.JuntarShapefilesTask;
import tasks.procesar.ProcessMarginMapTask;
import tasks.procesar.ProcessNewSoilMapTask;
import tasks.procesar.RecomendFertNFromHarvestMapTask;
import tasks.procesar.RecomendFertPFromHarvestMapTask;
import tasks.procesar.UnirCosechasMapTask;
import utils.DAH;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class JFXMain extends Application {


	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE = "gov.nasa.worldwind.avkey.InitialAltitude";
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE = "gov.nasa.worldwind.avkey.InitialLongitude";
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE = "gov.nasa.worldwind.avkey.InitialLatitude";
	//	private static final double MAX_VALUE = 1.0;
	//	private static final double MIN_VALUE = 0.2;
	public static final String VERSION = "0.2.22 dev";
	private static final String TITLE_VERSION = "Ursula GIS "+VERSION;
	private static final String BUILD_INFO="Esta aplicacion fue compilada el 01 de Agosto de 2017."
			+"<br>Ursula GIS es de uso libre y gratutito provista sin ninguna garantia."
			+"<br>Se encuentra en estado de desarrollo y se provee a modo de prueba."
			+"<br>Desarrollada por Tomas Lund Petersen, cualquier consulta enviar un mail a kotogadekiru@gmail.com"
			+"<br>Twitter: @redbaron_ar";
	public static final String ICON = "gui/1-512.png";
	private static final String SOUND_FILENAME = "Alarm08.wav";//TODO cortar este wav porque suena 2 veces
	private Stage stage=null;
	private Scene scene=null;

	private Dimension canvasSize = new Dimension(1500, 800);

	protected WWPanel wwjPanel=null;
	protected LayerPanel layerPanel=null;
	private VBox progressBox = new VBox();

	//guardo referencia a las capas importadas o generadas
	private List<CosechaLabor> cosechas = new ArrayList<CosechaLabor>();
	private List<FertilizacionLabor> fertilizaciones = new ArrayList<FertilizacionLabor>();
	private List<SiembraLabor> siembras = new ArrayList<SiembraLabor>();
	private List<PulverizacionLabor> pulverizaciones = new ArrayList<PulverizacionLabor>();

	private List<Suelo> suelos = new ArrayList<Suelo>();

	private List<Margen> margenes= new ArrayList<Margen>();

	private ExecutorService executorPool = Executors.newCachedThreadPool();
	private Node wwNode=null;//contiene el arbol con los layers y el swingnode con el world wind


	@Override
	public void start(Stage primaryStage) throws Exception {
		this.stage = primaryStage;

		primaryStage.setTitle(TITLE_VERSION);
		primaryStage.getIcons().add(new Image(ICON));

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

		double ratio = primaryScreenBounds.getHeight()/primaryScreenBounds.getWidth();

		int canvasWidth = (int) (primaryScreenBounds.getWidth()*0.8);
		int canvasHeight = (int) (canvasWidth/ratio);

		canvasSize=new Dimension(canvasWidth,canvasHeight);
		StackPane pane = new StackPane();
		scene = new Scene(pane,canvasSize.getWidth()*1,canvasSize.getHeight()*0.3);//, Color.White);
		primaryStage.setScene(scene);

		addDragAndDropSupport();
		// scene.getStylesheets().add("gisUI/style.css");//esto funciona

		MenuBar menuBar = constructMenuBar();

		setInitialPosition();
		VBox vBox1 = new VBox();
		vBox1.getChildren().add(menuBar);
		createSwingNode(vBox1);
		pane.getChildren().add(vBox1);



		primaryStage.setOnHiding((e)-> {
			Platform.runLater(()->{
				DAH.em().close();
				System.out.println("em Closed");
				System.out.println("Application Closed by click to Close Button(X)");
				System.exit(0); 
			});
		});
		primaryStage.show();





	}

	private static void setInitialPosition() {
		Configuracion config =Configuracion.getInstance();
		double initLat = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, "-35"));
		double initLong = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE, "-62"));
		double initAltitude = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, "19.07e5"));
		//   <Property name="gov.nasa.worldwind.avkey.InitialLatitude" value="-35"/>
		//<Property name="gov.nasa.worldwind.avkey.InitialLongitude" value="-62"/>
		//<Property name="gov.nasa.worldwind.avkey.InitialAltitude" value="19.07e5"/>
		Configuration.setValue(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, initLat);
		Configuration.setValue(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE,initLong);
		Configuration.setValue(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, initAltitude);
	}

	public void createSwingNode(VBox vBox1) {
		//		if (Configuration.isMacOS()) {
		//			System.setProperty(
		//					"com.apple.mrj.application.apple.menu.about.name",
		//					JFXMain.TITLE_VERSION);
		//		}

		Task<Node> pfMapTask = new Task<Node>(){
			@Override
			protected Node call() throws Exception {
				try{
					return initializeWorldWind();//esto tiene que estar antes de initialize
					//return wwdNode;
				}catch(Throwable t){
					t.printStackTrace();				
					return null;
				}
			}			
		};

		pfMapTask.setOnSucceeded(handler -> {			
			wwNode = (Node) handler.getSource().getValue();
			vBox1.getChildren().add( wwNode);
			this.wwjPanel.repaint();	


		});
		executorPool.execute(pfMapTask);
	}

	protected Node initializeWorldWind() {
		try {//com.sun.java.swing.plaf.windows.WindowsLookAndFeel
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");//UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		}

		//	setDefaultSize(50);//esto funciona para la barra de abajo pero no para los placemarks

		// Create the WorldWindow.
		this.wwjPanel =	new WWPanel(canvasSize, true);
		//una vez que se establecio el tamaño inicial ese es el tamaño maximo
		//this.wwjPanel.setPreferredSize(canvasSize);
		final SwingNode wwSwingNode = new SwingNode();
		wwSwingNode.setContent(wwjPanel);
		// Put the pieces together.
		//wwSwingNode.autosize();
		this.layerPanel = new LayerPanel(this.wwjPanel.getWwd(),stage.widthProperty(),stage.heightProperty());
		this.layerPanel.addToScrollPaneBottom(progressBox);

		setAccionesTreePanel();

		this.stage.widthProperty().addListener((o,old,nu)->{
			this.wwjPanel.setPreferredSize(new Dimension(nu.intValue(),(int)stage.getWidth()));
			this.wwjPanel.repaint();
		});

		this.stage.heightProperty().addListener((o,old,nu)->{
			this.wwjPanel.setPreferredSize(new Dimension((int)stage.getHeight(),nu.intValue()));
			this.wwjPanel.repaint();
		});

		//ok
		this.stage.maximizedProperty().addListener((o,ov,nu)->{
			this.wwjPanel.repaint();	
		});

		SplitPane sp = new SplitPane();
		sp.getItems().addAll(layerPanel, wwSwingNode);
		sp.setDividerPositions(0.15f);
		//TODO modificar esto para que cuando sea full screen mantenga el tamanio del arbol en vez de estirarlo
		//hbox.set(layerPanel);
		//		wwSwingNode.setScaleX(1.5);
		//		wwSwingNode.setScaleY(1.5);
		//	hbox.setCenter(wwSwingNode);

		// Create and install the view controls layer and register a controller
		// for it with the World Window.
		ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
		insertBeforeCompass(getWwd(), viewControlsLayer);
		this.getWwd()
		.addSelectListener(
				new ViewControlsSelectListener(this.getWwd(),
						viewControlsLayer));

		// Register a rendering exception listener that's notified when
		// exceptions occur during rendering.
		this.wwjPanel.getWwd().addRenderingExceptionListener(
				new RenderingExceptionListener() {
					public void exceptionThrown(Throwable t) {
						if (t instanceof WWAbsentRequirementException) {
							String message = "Computer does not meet minimum graphics requirements.\n";
							message += "Please install up-to-date graphics driver and try again.\n";
							message += "Reason: " + t.getMessage() + "\n";
							message += "This program will end when you press OK.";
							System.err.println(message);
							t.printStackTrace();
							Alert alert = new Alert(AlertType.ERROR,message,ButtonType.OK);
							alert.initOwner(stage);
							alert.showAndWait();
							// JOptionPane.showMessageDialog(JFXMain.this,
							// message, "Unable to Start Program",
							// JOptionPane.ERROR_MESSAGE);
							System.exit(-1);
						}
					}
				});

		// Search the layer list for layers that are also select listeners and
		// register them with the World
		// Window. This enables interactive layers to be included without
		// specific knowledge of them here.
		for (Layer layer : this.wwjPanel.getWwd().getModel().getLayers()) {
			if (layer instanceof SelectListener) {
				this.getWwd().addSelectListener((SelectListener) layer);
			}
		}

		importElevations();
		// Center the application on the screen.
		// WWUtil.alignComponent(null, this, AVKey.CENTER);
		stage.setResizable(true);

		//XXX descomentar esto para cargar los poligonos de la base de datos. bloquea la interface
		executorPool.execute(()->{loadPoligonos();});
		//loadPoligonos();

		return sp;
	}

	public static void setDefaultSize(int size) {
		Set<Object> keySet = UIManager.getLookAndFeelDefaults().keySet();
		Object[] keys = keySet.toArray(new Object[keySet.size()]);

		for (Object key : keys) {
			if (key != null && key.toString().toLowerCase().contains("font")) {
				//System.out.println(key);
				Font font = UIManager.getDefaults().getFont(key);
				if (font != null) {
					font = font.deriveFont((float)size);
					UIManager.put(key, font);
				}
			}
		}
	}

	/**
	 * aca se configuran los menues contextuales del arbol de capas
	 //XXX agregar nuevas funcionalidades aca!!! 
	 */
	private void setAccionesTreePanel() {
		Map<Class<?>,List<Function<Layer, String>>> predicates = new HashMap<Class<?>,List<Function<Layer,String>>>();
		List<Function<Layer, String>> cosechasP = new ArrayList<Function<Layer,String>>();
		predicates.put(CosechaLabor.class, cosechasP);
		List<Function<Layer, String>> fertilizacionesP = new ArrayList<Function<Layer,String>>();
		predicates.put(FertilizacionLabor.class, fertilizacionesP);
		List<Function<Layer, String>> siembrasP = new ArrayList<Function<Layer,String>>();
		predicates.put(SiembraLabor.class, siembrasP);
		List<Function<Layer, String>> pulverizacionesP = new ArrayList<Function<Layer,String>>();
		predicates.put(PulverizacionLabor.class, pulverizacionesP);
		List<Function<Layer, String>> margenesP = new ArrayList<Function<Layer,String>>();
		predicates.put(Margen.class, margenesP);
		List<Function<Layer, String>> ndviP = new ArrayList<Function<Layer,String>>();
		predicates.put(Ndvi.class, ndviP);

		List<Function<Layer, String>> poligonosP = new ArrayList<Function<Layer,String>>();
		predicates.put(Poligono.class, poligonosP);

		List<Function<Layer, String>> laboresP = new ArrayList<Function<Layer,String>>();
		predicates.put(Labor.class, laboresP);
		List<Function<Layer, String>> todosP = new ArrayList<Function<Layer,String>>();
		predicates.put(Object.class, todosP);

		poligonosP.add((layer)->{
			if(layer==null){
				return "Convertir a Siembra"; 
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					//doConvertirASiembra((Polygon) layerObject);
					doCrearSiembra((Poligono) layerObject);

				}
				return "converti a Siembra";
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Convertir a Fertilizacion"; 
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){	
					doCrearFertilizacion((Poligono) layerObject);

				}
				return "converti a Fertilizacion";
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Convertir a Pulverizacion"; 
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doCrearPulverizacion((Poligono) layerObject);

				}
				return "converti a Pulverizacion";
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Convertir a Cosecha"; 
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doCrearCosecha((Poligono) layerObject);
					//TODO deshabilitar layer para que el polygono no rompa la cosecha nueva

				}
				return "converti a Cosecha";
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Convertir a Mapa de Suelo"; 
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doCrearSuelo((Poligono) layerObject);
					//TODO deshabilitar layer para que el polygono no rompa la cosecha nueva

				}
				return "converti a Cosecha";
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Guardar"; 
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doGuardarPoligono((Poligono) layerObject);
					//TODO deshabilitar layer para que el polygono no rompa la cosecha nueva

				}
				return "converti a Cosecha";
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Ir a"; 
			} else{
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (layerObject==null){
				}else if(Poligono.class.isAssignableFrom(layerObject.getClass())){
					Poligono poli = (Poligono)layerObject;
					Position pos =poli.getPositions().get(0);
					viewGoTo(pos);
				}
				return "went to " + layer.getName();
			}});

		
		laboresP.add((layer)->{
			if(layer==null){
				return "Ir a"; 
			} else{
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (layerObject==null){
				}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
					viewGoTo((Labor<?>) layerObject);
				}
				return "went to " + layer.getName();
			}});



		/**
		 *Accion que permite editar un mapa de rentabilidad
		 */
		margenesP.add((layer)->{ 
			if(layer==null){
				return "Editar"; 
			} else{
				doEditMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "margen editada" + layer.getName();

			}});


		/**
		 *Accion que permite editar una siembra
		 */
		pulverizacionesP.add((layer)-> {
			if(layer==null){
				return "Editar"; 
			} else{
				doEditPulverizacion((PulverizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "pulverizacion editada" + layer.getName();
			}});


		/**
		 *Accion que permite editar una siembra
		 */
		siembrasP.add((layer)-> {
			if(layer==null){
				return "Editar"; 
			} else{
				doEditSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "siembra editada" + layer.getName();
			}});

		/**
		 *Accion que permite editar una cosecha
		 */
		fertilizacionesP.add((layer)-> {
			if(layer==null){
				return "Editar"; 
			} else{
				doEditFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "fertilizacion editada" + layer.getName();
			}});

		/**
		 *Accion que permite editar una cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return "Editar"; 
			} else{
				doEditCosecha((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha editada" + layer.getName();

			}});
		/**
		 * Accion que permite pasar una grilla sobre la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return "Grillar cosecha"; 
			} else{
				doGrillarCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha editada" + layer.getName();
			}});

		/**
		 * Accion que permite pasar una grilla sobre la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return "Clonar cosecha"; 
			} else{
				doUnirCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha clonada" + layer.getName();
			}});

		/**
		 * Accion que muesta el histograma
		 */
		laboresP.add((layer)->applyHistogramaCosecha(layer));


		/**
		 * Accion que muesta el la relacion entre el rinde y la elevacion
		 */
		cosechasP.add((layer)-> {
			if(layer==null){
				return "Elevacion vs Cantidad"; 
			} else{
				showAmountVsElevacionChart((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "grafico mostrado " + layer.getName();
			}});
		
//		/**
//		 * Accion que permite extraer los poligonos de una cosecha para guardar
//		 */
//		cosechasP.add((layer)-> {
//			if(layer==null){
//				return "Extraer poligonos"; 
//			} else{
//				doExtraerPoligonos((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//				return "poligonos Extraidos " + layer.getName();
//			}});


		/**
		 * Accion permite generar una ferilizacion a partir de los poligonos de una cosecha
		 */
		//		predicates.add(new Function<Layer,String>(){			
		//			@Override
		//			public String apply(Layer layer) {
		//				if(layer==null){
		//					return "Exportar Fertilizacion"; 
		//				} else{
		//					doRecomendFertFromHarvestPotential((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
		//					return "histograma mostrado" + layer.getName();
		//				}
		//			}
		//
		//		});

		/**
		 * Accion permite exportar la labor como shp
		 */
		laboresP.add((layer)->{
			if(layer==null){
				return "Exportar"; 
			} else{
				doExportLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "labor Exportada" + layer.getName();
			}});
		
		/**
		 * Accion permite exportar la labor como shp
		 */
		fertilizacionesP.add((layer)->{
			if(layer==null){
				return "Exportar Prescripcion"; 
			} else{
				doExportPrescripcion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "labor Exportada" + layer.getName();
			}});

		/**
		 * Accion permite crear una fertilizacion P para reponer lo extraido por la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return "Recomendar Fertilizacion P"; 
			} else{
				doRecomendFertPFromHarvest((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "Fertilizacion P Creada" + layer.getName();
			}});
		
		/**
		 * Accion permite crear una fertilizacion P para reponer lo extraido por la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return "Recomendar Fertilizacion N"; 
			} else{
				doRecomendFertNFromHarvest((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "Fertilizacion N Creada" + layer.getName();
			}});

		/**
		 * Accion muestra una tabla con los datos de la cosecha
		 */
		laboresP.add((layer)->{
			if(layer==null){
				return "Ver Tabla"; 
			} else{
				doShowDataTable((Labor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "Tabla mostrada" + layer.getName();
			}});

		/**
		 * Accion permite exportar la cosecha como shp de puntos
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return "Exportar a puntos"; 
			} else{
				doExportHarvestDePuntos((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha exportada como puntos: " + layer.getName();
			}});


		ndviP.add((layer)->{
			if(layer==null){//TODO implementar estimar Rinde desde ndvi
				return "Convertir a cosecha"; 
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(o instanceof Ndvi){
					doConvertirNdviACosecha((Ndvi) o);
				}

				return "rinde estimado desde ndvi" + layer.getName();
			}});

		ndviP.add((layer)->{
			if(layer==null){
				return "Histograma"; 
			} else{//TODO implementar histograma ndvi
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(o instanceof Ndvi){
					showHistoNDVI((Ndvi)o);
				}

				return "histograma ndvi mostrado" + layer.getName();
			}});
		
		ndviP.add((layer)->{
			if(layer==null){
				return "Ir a"; 
			} else{
				Object zoomPosition = layer.getValue(ProcessMapTask.ZOOM_TO_KEY);		
				
				//Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (zoomPosition==null){
				}else if(zoomPosition instanceof Position){
					Position pos =(Position)zoomPosition;
					viewGoTo(pos);
				}
				return "went to " + layer.getName();
			}});

		/**
		 * Accion permite obtener ndvi
		 */
		laboresP.add((layer)->{
			if(layer==null){
				return "Obtener NDVI"; 
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
				if(o instanceof Labor){
					doGetNdviTiffFile(o);
				}
				return "ndvi obtenido" + layer.getName();	
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Obtener NDVI"; 
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
				if(o instanceof Poligono){
					doGetNdviTiffFile(o);
				}
				return "ndvi obtenido" + layer.getName();	
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Obtener NDVI2"; 
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
				if(o instanceof Poligono){
					doGetNdwiTiffFile(o);
				}
				return "ndwi obtenido" + layer.getName();	
			}});


		/**
		 * Accion que permite eliminar una cosecha
		 */
		todosP.add((layer)->{
			if(layer==null){
				return "Quitar"; 
			} else{
				getWwd().getModel().getLayers().remove(layer);
				Object layerObject =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Labor.class.isAssignableFrom(layerObject.getClass())){
					Labor<?> l = (Labor<?>)layerObject;
					fertilizaciones.remove(l);
					pulverizaciones.remove(l);
					siembras.remove(l);
					cosechas.remove(l);
					suelos.remove(l);
					l.dispose();
				}
				//				if(layerObject instanceof Poligono){
				//					DAH.remove(layerObject);
				//				}
				layer.dispose();
				getLayerPanel().update(getWwd());
				return "layer removido" + layer.getName();
			}});


		layerPanel.setMenuItems(predicates);
	}





	private void enDesarrollo() {
		Alert enDesarrollo = new Alert(AlertType.INFORMATION,"funcionalidad en desarrollo");
		enDesarrollo.show();
	}


	private String applyHistogramaCosecha(Layer layer){
		if(layer==null){
			return "Histograma"; 
		} else{
			showHistoLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "histograma mostrado" + layer.getName();
		}
	}

	private void importElevations(){
		try{
			// Download the data and save it in a temp file.
			//  File sourceFile = ExampleUtil.saveResourceToTempFile(ELEVATIONS_PATH, ".tif");

			// Create a local elevation model from the data.
			final ElevationModel elevationModel = new ZeroElevationModel(){
				//            	 @Override
				//            	    public double getElevation(Angle latitude, Angle longitude){
				//            		 return 0;
				//            	    	
				//            	    }
			};
			//  elevationModel.addElevations(sourceFile);

			executorPool.execute(()->{
				// Get the WorldWindow's current elevation model.
				Globe globe = getWwd().getModel().getGlobe();
				ElevationModel currentElevationModel = globe.getElevationModel();

				// Add the new elevation model to the globe.
				//				if (currentElevationModel instanceof CompoundElevationModel)
				//					((CompoundElevationModel) currentElevationModel).addElevationModel(elevationModel);
				//				else
				globe.setElevationModel(elevationModel);

				// Set the view to look at the imported elevations, although they might be hard to detect. To
				// make them easier to detect, replace the globe's CompoundElevationModel with the new elevation
				// model rather than adding it.
				//     Sector modelSector = elevationModel.getSector();
				//   ExampleUtil.goTo(getWwd(), modelSector);
			});
		}catch (Exception e){
			e.printStackTrace();
		}
	}


	public WorldWindow getWwd() {
		return this.wwjPanel.getWwd();
	}

	public StatusBar getStatusBar() {
		return this.wwjPanel.getStatusBar();
	}

	public LayerPanel getLayerPanel() {
		return layerPanel;
	}

	//	public StatisticsPanel getStatsPanel() {
	//		return statsPanel;
	//	}

	//	public void setToolTipController(ToolTipController controller) {
	//		if (this.wwjPanel.toolTipController != null)
	//			this.wwjPanel.toolTipController.dispose();
	//
	//		this.wwjPanel.toolTipController = controller;
	//	}

	//	public void setHighlightController(HighlightController controller) {
	//		if (this.wwjPanel.highlightController != null)
	//			this.wwjPanel.highlightController.dispose();
	//
	//		this.wwjPanel.highlightController = controller;
	//	}

	public static void insertBeforeCompass(WorldWindow wwd, Layer layer) {
		// Insert the layer into the layer list just before the compass.
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l instanceof CompassLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition, layer);
	}

	//	public static void insertBeforePlacenames(WorldWindow wwd, Layer layer) {
	//		// Insert the layer into the layer list just before the placenames.
	//		int compassPosition = 0;
	//		LayerList layers = wwd.getModel().getLayers();
	//		for (Layer l : layers) {
	//			if (l instanceof PlaceNameLayer)
	//				compassPosition = layers.indexOf(l);
	//		}
	//		layers.add(compassPosition, layer);
	//	}

	//	public static void insertAfterPlacenames(WorldWindow wwd, Layer layer) {
	//		// Insert the layer into the layer list just after the placenames.
	//		int compassPosition = 0;
	//		LayerList layers = wwd.getModel().getLayers();
	//		for (Layer l : layers) {
	//			if (l instanceof PlaceNameLayer)
	//				compassPosition = layers.indexOf(l);
	//		}
	//		layers.add(compassPosition + 1, layer);
	//	}

	//	public static void insertBeforeLayerName(WorldWindow wwd, Layer layer,
	//			String targetName) {
	//		// Insert the layer into the layer list just before the target layer.
	//		int targetPosition = 0;
	//		LayerList layers = wwd.getModel().getLayers();
	//		for (Layer l : layers) {
	//			if (l.getName().indexOf(targetName) != -1) {
	//				targetPosition = layers.indexOf(l);
	//				break;
	//			}
	//		}
	//		layers.add(targetPosition, layer);
	//	}

	static {
		System.setProperty("java.net.useSystemProxies", "true");
		if (Configuration.isMacOS()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name",
					"World Wind Application");
			System.setProperty("com.apple.mrj.application.growbox.intrudes",
					"false");
			System.setProperty("apple.awt.brushMetalLook", "true");
		} else if (Configuration.isWindowsOS()) {
			System.setProperty("sun.awt.noerasebackground", "true"); 
			// prevents
			// flashing
			// during
			// window
			// resizing
		}
	}


	private MenuBar constructMenuBar() {
		/*Menu Importar*/
		final Menu menuImportar = new Menu("Importar");
		//addMenuItem("Suelo",(a)->doOpenSoilMap(),menuImportar);		
		addMenuItem("Fertilizacion",(a)->doOpenFertMap(null),menuImportar);
		addMenuItem("Siembra",(a)->doOpenSiembraMap(null),menuImportar);
		addMenuItem("Pulverizacion",(a)->doOpenPulvMap(null),menuImportar);
		addMenuItem("Cosecha",(a)->doOpenCosecha(null),menuImportar);		
		addMenuItem("NDVI",(a)->doOpenNDVITiffFiles(),menuImportar);
		addMenuItem("Imagen",(a)->importImagery(),menuImportar);
		addMenuItem("Suelo",(a)->doOpenSoilMap(null),menuImportar);
		addMenuItem("Margen",(a)->doOpenMarginlMap(),menuImportar);

		final Menu menuCalcular = new Menu("Calcular");
		//insertMenuItem(menuCalcular,"Retabilidades",a->doProcessMargin());
		addMenuItem("Distancia",(a)->doMedirDistancia(),menuCalcular);
		addMenuItem("Superficie",(a)->doMedirSuperfice(),menuCalcular);
		addMenuItem("Unir Shapefiles",(a)->JuntarShapefilesTask.process(),menuCalcular);
		addMenuItem("Rentabilidades",(a)->doProcessMargin(),menuCalcular);
		addMenuItem("Unir Cosechas",(a)->doUnirCosechas(null),menuCalcular);
		addMenuItem("Balance De Fosforo",(a)->doProcesarBalanceFosforo(),menuCalcular);
		addMenuItem("Ir a",(a)->{
			TextInputDialog anchoDialog = new TextInputDialog("Pehuajo");
			anchoDialog.setTitle("Buscar Ubicacion");
			anchoDialog.setHeaderText("Buscar Ubicacion");
			anchoDialog.initOwner(this.stage);
			Optional<String> anchoOptional = anchoDialog.showAndWait();
			if(anchoOptional.isPresent()){
				Position pos = GoogleGeocodingHelper.obtenerPositionDirect(anchoOptional.get());
				if(pos!=null){
					viewGoTo(pos);
				}				
			} else{
				return;
			}
			
		
		},menuCalcular);
		addMenuItem("Evolucion NDVI",(a)->{
			doShowNDVIEvolution();
		},menuCalcular);


		/*Menu Exportar*/
		final Menu menuExportar = new Menu("Exportar");		
		//	addMenuItem("Suelo",(a)->doExportSuelo(),menuExportar);
		addMenuItem("Pantalla",(a)->doSnapshot(),menuExportar);

		/*Menu Configuracion*/
		final Menu menuConfiguracion = new Menu("Configuracion");
		addMenuItem("Cultivos",(a)->doConfigCultivo(),menuConfiguracion);
		addMenuItem("Fertilizantes",(a)->doConfigFertilizantes(),menuConfiguracion);
		addMenuItem("Semillas",(a)->doConfigSemillas(),menuConfiguracion);

		addMenuItem("Empresa",(a)->doConfigEmpresa(),menuConfiguracion);
		addMenuItem("Establecimiento",(a)->doConfigEstablecimiento(),menuConfiguracion);
		addMenuItem("Lote",(a)->doConfigLote(),menuConfiguracion);
		addMenuItem("Campaña",(a)->doConfigCampania(),menuConfiguracion);
		addMenuItem("Poligonos",(a)->doConfigPoligonos(),menuConfiguracion);

		MenuItem actualizarMI=addMenuItem("Actualizar !",null,menuConfiguracion);
		actualizarMI.setOnAction((a)->doUpdate(actualizarMI));
		actualizarMI.setVisible(false);
		
		executorPool.submit(()->{
			if(UpdateTask.isUpdateAvailable()){
				actualizarMI.setVisible(true);
				//actualizarMI.getStyleClass().clear();

				actualizarMI.getStyleClass().add("menu-item:focused");
				actualizarMI.setStyle("-fx-background: -fx-accent;"
						+"-fx-background-color: -fx-selection-bar;"
						+ "-fx-text-fill: -fx-selection-bar-text;"
						+ "fx-text-fill: white;");
				//menuConfiguracion.show();
				menuConfiguracion.setStyle("-fx-background: -fx-accent;"
						+"-fx-background-color: -fx-selection-bar;"
						+ "-fx-text-fill: -fx-selection-bar-text;"
						+ "fx-text-fill: white;");

			}
		});
		
		addMenuItem("Acerca De",(a)->doShowAcercaDe(),menuConfiguracion);

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuImportar,menuCalcular, menuExportar,menuConfiguracion);
		menuBar.setPrefWidth(scene.getWidth());
		return menuBar;
	}

	private void doShowNDVIEvolution() {
		executorPool.execute(()->{
		List<Layer> ndviLayers = new ArrayList<Layer>();
		LayerList layers = getWwd().getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Ndvi){
				l.setEnabled(false);
				ndviLayers.add(l);
			}
		}	
		ndviLayers.sort((c1,c2)->{
			String l1Name =c1.getName();
			String l2Name =c2.getName();
			//TODO comparar por el valor del layer en vez del nombre del layer
			DateFormat df =new  SimpleDateFormat("dd-MM-yyyy");
			
			try{
				Date d1 = df.parse(l1Name);
				Date d2 = df.parse(l2Name);
				return d1.compareTo(d2);
			} catch(Exception e){
				//no se pudo parsear como fecha entonces lo interpreto como string.
				//e.printStackTrace();
			}
			return l1Name.compareTo(l2Name);
		});
		Platform.runLater(()->{
			this.layerPanel.update(getWwd());
			getWwd().redraw();
			});


		for(Layer l: ndviLayers){
			l.setEnabled(true);
			Platform.runLater(()->{
			this.layerPanel.update(getWwd());
			getWwd().redraw();
			});

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			
				e.printStackTrace();
			}

			l.setEnabled(false);

			Platform.runLater(()->{
				this.layerPanel.update(getWwd());
				getWwd().redraw();
				});

		}		
		});
	}




	private void doUpdate(MenuItem actualizarMI) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setContentText("Se descargara la nueva version");
		alert.initOwner(this.stage);
		alert.showAndWait();
		if(ButtonType.OK.equals(alert.getResult())){
			UpdateTask uTask = new UpdateTask();
			uTask.installProgressBar(progressBox);
			uTask.setOnSucceeded(handler -> {
				File newVersion = (File) handler.getSource().getValue();	
				if(newVersion==null){
					Alert error = new Alert(Alert.AlertType.ERROR);
					error.setContentText("No se pudo descargar la nueva version. Intente de nuevo o acceda a www.ursulagis.com");
					error.initOwner(this.stage);
					error.showAndWait();
				} else{
					Alert error = new Alert(Alert.AlertType.CONFIRMATION);
					error.setContentText("Se instalo la nueva version con exito! cerrando la version actual. vuelva a iniciar la aplicacion para seguir trabajando");
					error.initOwner(this.stage);
					error.showAndWait();

					//					playSound();	
					//					actualizarMI.getParentMenu().setStyle("");
					//					actualizarMI.setVisible(false);
					//borrar estilos del menuConfiguracion

					//TODO cerrar esta app y abrir la nueva?
				}
				//insertBeforeCompass(getWwd(), ndviLayer);
				//this.getLayerPanel().update(this.getWwd());
				//viewGoTo(ndviLayer);
				uTask.uninstallProgressBar();

			});
			//	Platform.runLater(uTask);

			executorPool.submit(uTask);


		}//fin del if OK	
	}

	
	private void loadPoligonos(){
		List<Poligono> poligonos = DAH.getAllPoligonos();
		showPoligonos(poligonos);
	}

	private void showPoligonos(List<Poligono> poligonos) {
		//boolean armed = false;
		BooleanProperty armed = new SimpleBooleanProperty(false);
		for(Poligono poli : poligonos){
			RenderableLayer surfaceLayer = new RenderableLayer();
			
			MeasureTool measureTool = new MeasureTool(getWwd(),surfaceLayer);
			measureTool.setController(new MeasureToolController());
			measureTool.setMeasureShapeType(MeasureTool.SHAPE_POLYGON);
			measureTool.setPathType(AVKey.GREAT_CIRCLE);
			measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.METERS);
			measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.HECTARE);
			measureTool.getUnitsFormat().setShowDMS(false);
			measureTool.setFollowTerrain(true);
			measureTool.setShowControlPoints(true);
			measureTool.setPositions((ArrayList<? extends Position>) poli.getPositions());
			measureTool.setArmed(false);

			poli.setLayer(surfaceLayer);		
			surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
			
			DoubleProperty valueProperty= new SimpleDoubleProperty();
			valueProperty.setValue( poli.getArea());
	

			measureTool.addPropertyChangeListener((event)->{
				//System.out.println(event.getPropertyName());
				// Add, remove or change positions
				if(event.getPropertyName().equals(MeasureTool.EVENT_ARMED)){
					if (measureTool.isArmed()) {
						((Component) getWwd()).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
					} else {                    //al cerrar el alert se hace un setArmed(false);
						((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
					}
				}
				// Metric changed - sent after each render frame
				else if(event.getPropertyName().equals(MeasureTool.EVENT_METRIC_CHANGED)){
					double value2 = measureTool.getArea();
					if(valueProperty.get()!=value2 && value2 > 0){
						double area = value2/ProyectionConstants.METROS2_POR_HA;
						poli.setPositions( (List<Position>) measureTool.getPositions());
						//TODO construir una geometria de la lista de posiciones y validarla con PolygonValidator
						poli.setArea(area);					
						surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
						valueProperty.setValue(value2);
						if(armed.get()){
							this.getLayerPanel().update(this.getWwd());
						}
					}                	                  
				}
			});
			armed.set(true);
			Platform.runLater(()->{
			insertBeforeCompass(this.getWwd(), surfaceLayer);
			this.getLayerPanel().update(this.getWwd());
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void doMedirSuperfice() {
		RenderableLayer surfaceLayer = new RenderableLayer();

		MeasureTool measureTool = new MeasureTool(getWwd(),surfaceLayer);
		measureTool.setController(new MeasureToolController());
		// MeasureToolPanel  measurePanel=  new MeasureToolPanel(getWwd(), measureTool);

		measureTool.setMeasureShapeType(MeasureTool.SHAPE_POLYGON);
		measureTool.setPathType(AVKey.GREAT_CIRCLE);
		measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.METERS);
		measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.HECTARE);
		measureTool.getUnitsFormat().setShowDMS(false);
		measureTool.setFollowTerrain(true);
		measureTool.setShowControlPoints(true);
		// measureTool.setMeasureShape(new SurfacePolygon());
		measureTool.clear();
		measureTool.setArmed(true);

		insertBeforeCompass(this.getWwd(), surfaceLayer);
		Poligono poli =new Poligono();

		Alert supDialog = new Alert(Alert.AlertType.INFORMATION);
		supDialog.initOwner(this.stage);
		supDialog.setTitle("Medir Superficie");
		supDialog.setHeaderText("Superficie");
		Text t = new Text();
		TextField nombreTF = new TextField();
		nombreTF.setPromptText("Nombre");
		VBox vb = new VBox();
		vb.getChildren().addAll(nombreTF,t);
		supDialog.setGraphic(vb);
		supDialog.initModality(Modality.NONE);
		nombreTF.textProperty().addListener((o,old,n)->{
			poli.setNombre(n);
			//surfaceLayer.setName(n);
		});

		DoubleProperty valueProperty = new SimpleDoubleProperty();
		measureTool.addPropertyChangeListener((event)->{
			//System.out.println(event.getPropertyName());
			// Add, remove or change positions
			if(event.getPropertyName().equals(MeasureTool.EVENT_ARMED)){
				if (measureTool.isArmed()) {
					((Component) getWwd()).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				} else {                    //al cerrar el alert se hace un setArmed(false);
					((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
				}
			}
			// Metric changed - sent after each render frame
			else if(event.getPropertyName().equals(MeasureTool.EVENT_METRIC_CHANGED)){
				DecimalFormat dc = new DecimalFormat("0.00");
				dc.setGroupingSize(3);
				dc.setGroupingUsed(true);
				double	value = measureTool.getArea()/ProyectionConstants.METROS2_POR_HA;
				if(value != valueProperty.doubleValue() && value > 0){
					String formated = dc.format(value)+" Ha";
					t.textProperty().set(formated);


					poli.setPositions( (List<Position>) measureTool.getPositions());
					poli.setArea(value);
					//poli.setNombre(dc.format(value/ProyectionConstants.METROS2_POR_HA));

					//System.out.println("nombre poli :"+poli.getNombre());
					poli.setLayer(surfaceLayer);
					surfaceLayer.setName(poli.getNombre()+" "+formated);
					//ArrayList<? extends Position> positions = measureTool.getPositions();
					surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
					//surfaceLayer.setValue("POSITIONS", poli.getPositions());

					valueProperty.setValue(value);
					this.getLayerPanel().update(this.getWwd());
					//  System.out.println("lengh="+measureTool.getLength());
				}                	                  
				//updateMetric();
			}
		});


		supDialog.show();
		supDialog.setOnHidden((event)->{			
			measureTool.setArmed(false);
			//surfaceLayer.setName(poli.getNombre()+" "+	t.textProperty().get());
			//((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
			this.getLayerPanel().update(this.getWwd());
		});

		//supDialog.getResult();      

	}

	private void doMedirDistancia() {		 
		RenderableLayer layer = new RenderableLayer();
		layer.setName("Distancia");
		MeasureTool measureTool = new MeasureTool(getWwd());
		measureTool.setController(new MeasureToolController());
		// MeasureToolPanel  measurePanel=  new MeasureToolPanel(getWwd(), measureTool);

		measureTool.setMeasureShapeType(MeasureTool.SHAPE_PATH);
		measureTool.setPathType(AVKey.GREAT_CIRCLE);
		measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.METERS);
		measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.HECTARE);
		measureTool.getUnitsFormat().setShowDMS(false);
		measureTool.setFollowTerrain(true);
		measureTool.setShowControlPoints(true);
		// measureTool.setMeasureShape(new SurfacePolygon());
		measureTool.clear();
		measureTool.setArmed(true);

		Alert distanciaDialog = new Alert(Alert.AlertType.INFORMATION);
		distanciaDialog.initOwner(this.stage);
		Text t = new Text();
		distanciaDialog.setTitle("Medir Distancia");
		distanciaDialog.setHeaderText("Distancia");
		distanciaDialog.setGraphic(t);
		distanciaDialog.initModality(Modality.NONE);

		DoubleProperty valueProperty = new SimpleDoubleProperty();
		measureTool.addPropertyChangeListener((event)->{
			//System.out.println(event.getPropertyName());
			// Add, remove or change positions
			if(event.getPropertyName().equals(MeasureTool.EVENT_ARMED)){
				if (measureTool.isArmed()) {
					((Component) getWwd()).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				} else {                    
					((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
				}
			}
			// Metric changed - sent after each render frame
			else if(event.getPropertyName().equals(MeasureTool.EVENT_METRIC_CHANGED)){
				double	value = measureTool.getLength();
				DecimalFormat dc = new DecimalFormat("0.00");
				dc.setGroupingSize(3);
				dc.setGroupingUsed(true);
				if(value != valueProperty.doubleValue() && value > 0){
					String formated = dc.format(value)+" mts";
					t.textProperty().set(formated);
					measureTool.getLayer().setName(formated);
					measureTool.getLayer().setValue(Labor.LABOR_LAYER_IDENTIFICATOR, "Medicion");
					valueProperty.setValue(value);
					this.getLayerPanel().update(this.getWwd());


					//                		t.textProperty().set(dc.format(value));
					//                		measureTool.getLayer().setName("Distancia "+dc.format(value));
					//                		measureTool.getLayer().setValue(Labor.LABOR_LAYER_IDENTIFICATOR, "Medicion");
					//                		valueProperty.setValue(value);
					//                		this.getLayerPanel().update(this.getWwd());

					//  System.out.println("lengh="+measureTool.getLength());
				}                	                  
				//updateMetric();
			}
		});


		distanciaDialog.show();
		distanciaDialog.setOnHidden((event)->{
			measureTool.setArmed(false);
			((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
			this.getLayerPanel().update(this.getWwd());
		});

		distanciaDialog.getResult();               
	}

	/**
	 * Funcion util para vincular un metodo con un item en un menu
	 * @param name nombre para mostrar en el menu
	 * @param action metodo a vincular con el item
	 * @param parent menu en el que se va a insertar el menuItem
	 */
	private static MenuItem addMenuItem(String name, EventHandler<ActionEvent> action, Menu parent){
		MenuItem menuItemProductos = new MenuItem(name);
		menuItemProductos.setOnAction(action);
		parent.getItems().addAll(menuItemProductos);
		return menuItemProductos;
	}



	public void viewGoTo(Labor<?> labor) {
		viewGoTo(labor.getLayer());
	}

	public void viewGoTo(Layer layer) {
		try {
			Position position=(Position) layer.getValue(ProcessMapTask.ZOOM_TO_KEY);
			viewGoTo(position);
		} catch (Exception e) {
			System.err.println("fallo hacer zoom a la cosecha nueva");
			e.printStackTrace();
		}
	}
	public void viewGoTo(Position position) {

		Configuracion config =Configuracion.getInstance();
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, String.valueOf(position.getLatitude().degrees));
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE,String.valueOf(position.getLongitude().degrees));
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, "64000");
		config.save();
		View view =getWwd().getView();
		view.goTo(position, 3000d);
	}

	private void showAmountVsElevacionChart(Labor<?> cosechaLabor) {
		TextInputDialog anchoDialog = new TextInputDialog("20");
		anchoDialog.setTitle("Configure la cantidad de grupos deseados");
		anchoDialog.setContentText("Max grupos");
		Optional<String> oGrupos = anchoDialog.showAndWait();
		int grupos=Integer.parseInt(oGrupos.get());
		Labor<?>[] cosechasAux = new Labor[]{cosechaLabor};
		if(cosechaLabor==null){
			Optional<CosechaLabor> optional = HarvestSelectDialogController.select(this.cosechas);
			if(!optional.isPresent()){
				return;
			}else{
				cosechasAux[0] =optional.get();
			}
		}

		Task<AmountVsElevacionChart> pfMapTask = new Task<AmountVsElevacionChart>(){
			@Override
			protected AmountVsElevacionChart call() throws Exception {
				try{
					//	Labor labor = optional.get();		
					AmountVsElevacionChart histoChart = new AmountVsElevacionChart(cosechasAux[0],grupos);
					return histoChart;
				}catch(Throwable t){
					t.printStackTrace();
					System.out.println("no hay ninguna labor para mostrar");
					System.out.print(t.getMessage());
					return null;
				}
			}			
		};


		pfMapTask.setOnSucceeded(handler -> {
			AmountVsElevacionChart	histoChart = (AmountVsElevacionChart) handler.getSource().getValue();	
			if(histoChart!=null){
				Stage histoStage = new Stage();
				histoStage.setTitle("Correlacion Rinde Vs Altura");
				histoStage.getIcons().add(new Image(ICON));

				Scene scene = new Scene(histoChart, 800,450);
				histoStage.setScene(scene);
				System.out.println("termine de crear el grafico rinde vs altura");
				histoStage.initOwner(this.stage);
				histoStage.show();
				System.out.println("histoChart.show();");
			}else{
				Alert error = new Alert(AlertType.ERROR);
				error.setTitle("error");
				error.setContentText("no se pudo crear el grafico de elevacion versus cantidad");
				error.show();
			}
		});
		executorPool.execute(pfMapTask);

		//		Thread currentTaskThread = new Thread(pfMapTask);
		//		currentTaskThread.setDaemon(true);
		//		currentTaskThread.start();
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
					System.out.println("no hay ninguna ndvi para mostrar");
					return new VBox(new Label("Upps!!"));
				}
			}			
		};


		pfMapTask.setOnSucceeded(handler -> {
			Parent	histoChart = (Parent) handler.getSource().getValue();	
			Stage histoStage = new Stage();
			//	histoStage.setTitle("Histograma Cosecha");
			histoStage.getIcons().add(new Image(ICON));

			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			//	System.out.println("termine de crear el histo chart");
			histoStage.initOwner(this.stage);
			histoStage.show();
			//	System.out.println("histoChart.show();");
		});
		executorPool.submit(pfMapTask);
	}

	private void showHistoLabor(Labor<?> cosechaLabor) {	

		//		Task<Parent> pfMapTask = new Task<Parent>(){
		//			@Override
		//			protected Parent call() throws Exception {
		//				try{	
		//					CosechaHistoChart histoChart = new CosechaHistoChart(cosechaLabor);
		//					return histoChart;
		//				}catch(Throwable t){
		//					t.printStackTrace();
		//					System.out.println("no hay ninguna labor para mostrar");
		//					return new VBox(new Label("Upps!!"));
		//				}
		//			}			
		//		};
		//
		//
		//		pfMapTask.setOnSucceeded(handler -> {
		//			Parent	histoChart = (Parent) handler.getSource().getValue();	
		//			Stage histoStage = new Stage();
		//			//	histoStage.setTitle("Histograma Cosecha");
		//			histoStage.getIcons().add(new Image(ICON));
		//
		//			Scene scene = new Scene(histoChart, 800,450);
		//			histoStage.setScene(scene);
		//			//	System.out.println("termine de crear el histo chart");
		//			histoStage.initOwner(this.stage);
		//			histoStage.show();
		//			//	System.out.println("histoChart.show();");
		//		});
		//		executorPool.submit(pfMapTask);

		Platform.runLater(()->{
			CosechaHistoChart histoChart = new CosechaHistoChart(cosechaLabor);
			Stage histoStage = new Stage();
			histoStage.getIcons().add(new Image(ICON));
			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(this.stage);
			histoStage.show();
		});
	}


	private LocalDate dateChooser(LocalDate ini){
		SimpleObjectProperty<LocalDate> ldp = new SimpleObjectProperty<LocalDate>();
		LocalDate initialDate = LocalDate.now();
		DateConverter dc = new DateConverter();
		Configuracion config = Configuracion.getInstance();
		String configDate = config.getPropertyOrDefault("LAST_DATE", dc.toString(initialDate));
		initialDate = dc.fromString(configDate);
		if(ini!=null){
			initialDate= ini;
		}
		ldp.set(initialDate);

		Alert dateDialog = new Alert(AlertType.CONFIRMATION);//dc.toString(initialDate));
		DatePicker datePickerFecha=new DatePicker();
		datePickerFecha.setConverter(new DateConverter());
		datePickerFecha.valueProperty().bindBidirectional(ldp);
	
		dateDialog.setGraphic(datePickerFecha);
		dateDialog.setTitle("Configure la fecha requerida");
		dateDialog.setHeaderText("Fecha");
		dateDialog.initOwner(this.stage);
		Optional<ButtonType> res = dateDialog.showAndWait();
		if(res.get().equals(ButtonType.OK)){
			 config.setProperty("LAST_DATE", dc.toString(ldp.get()));
				config.save();
			return ldp.get();
		} else {
			return null;
		}
	}

	private void doGetNdviTiffFile(Object labor) {
		LocalDate ini =null;
		if(labor instanceof Labor){
			ini= (LocalDate)((Labor<?>)labor).fechaProperty.getValue();
		} 
		 ini = dateChooser(ini);
		
		if(ini!=null){
			File downloadLocation = directoryChooser();
			if(downloadLocation==null)return;
			ObservableList<File> observableList = FXCollections.observableArrayList(new ArrayList<File>());
			observableList.addListener((ListChangeListener<File>) c -> {
				System.out.println("mostrando los archivos agregados");
				if(c.next()){
					c.getAddedSubList().forEach((file)->{
						showNdviTiffFile(file);
					});//fin del foreach
				}			
			});

			GetNDVIForLaborTask task = new GetNDVIForLaborTask(labor,downloadLocation,observableList);
			task.setDate(ini);
			task.installProgressBar(progressBox);
			task.setOnSucceeded(handler -> {
				if(labor instanceof Poligono){
					((Poligono)labor).getLayer().setEnabled(false);
				}
				task.uninstallProgressBar();
			});
			executorPool.submit(task);
		}
	}

	private void doGetNdwiTiffFile(Object labor) {
		LocalDate ini =null;
		if(labor instanceof Labor){
			ini= (LocalDate)((Labor<?>)labor).fechaProperty.getValue();
		} 
		 ini = dateChooser(ini);
		
		if(ini!=null){
			File downloadLocation = directoryChooser();
			if(downloadLocation==null)return;
			ObservableList<File> observableList = FXCollections.observableArrayList(new ArrayList<File>());
			observableList.addListener((ListChangeListener<File>) c -> {
				System.out.println("mostrando los archivos agregados");
				if(c.next()){
					c.getAddedSubList().forEach((file)->{
						showNdviTiffFile(file);
					});//fin del foreach
				}			
			});

			GetNDVI2ForLaborTask task = new GetNDVI2ForLaborTask(labor,downloadLocation,observableList);
			task.setDate(ini);
			task.installProgressBar(progressBox);
			task.setOnSucceeded(handler -> {
				if(labor instanceof Poligono){
					((Poligono)labor).getLayer().setEnabled(false);
				}
				task.uninstallProgressBar();
			});
			executorPool.submit(task);
		}
	}
	
	private void doOpenNDVITiffFiles() {
		List<File>	files =chooseFiles("TIF", "*.tif");
		if(files!=null)	files.forEach((file)->{
			showNdviTiffFile(file);
			});//fin del foreach
	}

	private void showNdviTiffFile(File file) {
		ShowNDVITifFileTask task = new ShowNDVITifFileTask(file);
		task.setOnSucceeded(handler -> {
			Layer ndviLayer = (Layer) handler.getSource().getValue();	
			insertBeforeCompass(getWwd(), ndviLayer);
			this.getLayerPanel().update(this.getWwd());
			viewGoTo(ndviLayer);
			playSound();	
		});
		executorPool.submit(task);

	}

	private boolean isLocalPath(String path){
		return new File(path).exists();
		//	//	 path = "http://example.com/something.pdf";
		//		if (path.matches("(?!file\\b)\\w+?:\\/\\/.*")) {
		//		    // Not a local file
		//			return false;
		//		}
		//		return true;
	}


	protected void importImagery()  {
		List<File>	files =chooseFiles("TIF", "*.tif");
		files.forEach((file)->{
			executorPool.execute(()->{	
				try {
					// Read the data and save it in a temp file.
					File sourceFile = file;

					// Create a raster reader to read this type of file. The reader is created from the currently
					// configured factory. The factory class is specified in the Configuration, and a different one can be
					// specified there.
					DataRasterReaderFactory readerFactory
					= (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
							AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
					DataRasterReader reader = readerFactory.findReaderFor(sourceFile, null);

					// Before reading the raster, verify that the file contains imagery.
					AVList metadata = reader.readMetadata(sourceFile, null);
					if (metadata == null || !AVKey.IMAGE.equals(metadata.getStringValue(AVKey.PIXEL_FORMAT)))
						throw new Exception("Not an image file.");

					// Read the file into the raster. read() returns potentially several rasters if there are multiple
					// files, but in this case there is only one so just use the first element of the returned array.
					DataRaster[] rasters = reader.read(sourceFile, null);
					if (rasters == null || rasters.length == 0)
						throw new Exception("Can't read the image file.");

					DataRaster raster = rasters[0];

					// Determine the sector covered by the image. This information is in the GeoTIFF file or auxiliary
					// files associated with the image file.
					final Sector sector = (Sector) raster.getValue(AVKey.SECTOR);
					if (sector == null)
						throw new Exception("No location specified with image.");

					// Request a sub-raster that contains the whole image. This step is necessary because only sub-rasters
					// are reprojected (if necessary); primary rasters are not.
					int width = raster.getWidth();
					int height = raster.getHeight();


					// getSubRaster() returns a sub-raster of the size specified by width and height for the area indicated
					// by a sector. The width, height and sector need not be the full width, height and sector of the data,
					// but we use the full values of those here because we know the full size isn't huge. If it were huge
					// it would be best to get only sub-regions as needed or install it as a tiled image layer rather than
					// merely import it.
					DataRaster subRaster = raster.getSubRaster(width, height, sector, null);

					// Tne primary raster can be disposed now that we have a sub-raster. Disposal won't affect the
					// sub-raster.
					raster.dispose();

					// Verify that the sub-raster can create a BufferedImage, then create one.
					if (!(subRaster instanceof BufferedImageRaster))
						throw new Exception("Cannot get BufferedImage.");
					BufferedImage image = ((BufferedImageRaster) subRaster).getBufferedImage();


					DataBuffer buffer = image.getData().getDataBuffer();


					double [][] doubleArray = new double[width*height][3];
					for(int x=0;x<width;x++){
						for(int y=0;y<height;y++){
							int index = y*width+x;
							double r = buffer.getElemDouble(0,index);
							double g = buffer.getElemDouble(1,index);
							double v = buffer.getElemDouble(2,index);
							doubleArray[index][0]=r;
							doubleArray[index][1]=g;
							doubleArray[index][2]=v;

						}
					}
					// BarnesHutTSne  tsne = new ParallelBHTsne();
					//  double [][] Y = tsne.tsne(doubleArray, 1, 3, 20);   
					// System.out.println(MatrixOps.doubleArrayToPrintString(Y, ", ", 50,10));

					// The sub-raster can now be disposed. Disposal won't affect the BufferedImage.
					subRaster.dispose();


					// Create a SurfaceImage to display the image over the specified sector.
					final SurfaceImage si1 = new SurfaceImage(image, sector);

					// On the event-dispatch thread, add the imported data as an SurfaceImageLayer.
					Platform.runLater(()->{
						// Add the SurfaceImage to a layer.
						SurfaceImageLayer layer = new SurfaceImageLayer();
						layer.setName("Imported Surface Image");
						layer.setPickEnabled(false);
						layer.addRenderable(si1);

						// Add the layer to the model and update the application's layer panel.
						insertBeforeCompass(getWwd(), layer);
						this.getLayerPanel().update(this.getWwd());


						// Set the view to look at the imported image.
						ExampleUtil.goTo(getWwd(), sector);

					});
				}
				catch (Exception e){
					e.printStackTrace();
				}
			});//execute
		});//foreach
	}

	private BufferWrapperRaster loadRasterFile(File file){
		if(!file.exists()){	
			//TODO si el recurso es web podemos bajarlo a 
			// Download the data and save it in a temp file.
			String path = file.getAbsolutePath();
			file = ExampleUtil.saveResourceToTempFile(path, "." + WWIO.getSuffix(path));
		}



		// Create a raster reader for the file type.
		DataRasterReaderFactory readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
				AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
		DataRasterReader reader = readerFactory.findReaderFor(file, null);

		try{
			// Before reading the raster, verify that the file contains elevations.
			AVList metadata = reader.readMetadata(file, null);
			if (metadata == null || !AVKey.ELEVATION.equals(metadata.getStringValue(AVKey.PIXEL_FORMAT)))
			{
				Platform.runLater(()->{
					Alert imagenAlert = new Alert(Alert.AlertType.ERROR);
					imagenAlert.initOwner(stage);
					imagenAlert.initModality(Modality.NONE);
					imagenAlert.setTitle("Archivo no compatible");
					imagenAlert.setContentText("El archivo no continen informacion ndvi. Por favor seleccione un archivo con solo una capa");
					imagenAlert.show();
				});
				String msg = Logging.getMessage("ElevationModel.SourceNotElevations", file.getAbsolutePath());
				Logging.logger().severe(msg);
				throw new IllegalArgumentException(msg);
			}

			// Read the file into the raster.
			DataRaster[] rasters = reader.read(file, null);
			if (rasters == null || rasters.length == 0)	{
				String msg = Logging.getMessage("ElevationModel.CannotReadElevations", file.getAbsolutePath());
				Logging.logger().severe(msg);
				throw new WWRuntimeException(msg);
			}

			// Determine the sector covered by the elevations. This information is in the GeoTIFF file or auxiliary
			// files associated with the elevations file.
			Sector sector = (Sector) rasters[0].getValue(AVKey.SECTOR);
			if (sector == null)
			{
				String msg = Logging.getMessage("DataRaster.MissingMetadata", AVKey.SECTOR);
				Logging.logger().severe(msg);
				throw new IllegalArgumentException(msg);
			}

			// Request a sub-raster that contains the whole file. This step is necessary because only sub-rasters
			// are reprojected (if necessary); primary rasters are not.
			int width = rasters[0].getWidth();
			int height = rasters[0].getHeight();

			DataRaster subRaster = rasters[0].getSubRaster(width, height, sector, rasters[0]);

			// Verify that the sub-raster can create a ByteBuffer, then create one.
			if (!(subRaster instanceof BufferWrapperRaster))
			{
				String msg = Logging.getMessage("ElevationModel.CannotCreateElevationBuffer", file.getName());
				Logging.logger().severe(msg);
				throw new WWRuntimeException(msg);
			}

			return (BufferWrapperRaster) subRaster;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private void doCrearSuelo(Poligono poli) {
		Suelo labor = new Suelo();
		labor.nombreProperty.set("suelo");
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);

		//		Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(labor);
		//		if(!cosechaConfigured.isPresent()){//
		//			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
		//			labor.dispose();//libero los recursos reservados
		//			return;
		//		}							

		TextInputDialog ppmPDialog = new TextInputDialog("PpmP");
		ppmPDialog.setTitle("Configure partes por millon de P");
		ppmPDialog.setContentText("Ppm P");
		Optional<String> ppmPOptional = ppmPDialog.showAndWait();
		Double ppmP = Double.valueOf(ppmPOptional.get());

		TextInputDialog ppmNDialog = new TextInputDialog("PpmN");
		ppmNDialog.setTitle("Configure partes por millon de N");
		ppmNDialog.setContentText("Ppm N");
		Optional<String> ppmNOptional = ppmNDialog.showAndWait();
		Double ppmN = Double.valueOf(ppmNOptional.get());


		CrearSueloMapTask umTask = new CrearSueloMapTask(labor,poli,ppmP,ppmN);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			suelos.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("OpenHarvestMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		this.executorPool.execute(umTask);		
	}

	private void doCrearPulverizacion(Poligono poli) {
		PulverizacionLabor labor = new PulverizacionLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<PulverizacionLabor> cosechaConfigured= PulverizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog("Dosis");
		anchoDialog.setTitle("Configure Dosis");
		anchoDialog.setContentText("dosis");
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearPulverizacionMapTask umTask = new CrearPulverizacionMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
			pulverizaciones.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("OpenHarvestMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		this.executorPool.execute(umTask);		
	}


	private void doCrearSiembra(Poligono poli) {
		SiembraLabor labor = new SiembraLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<SiembraLabor> cosechaConfigured= SiembraConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog("Cantidad por Ha");
		anchoDialog.setTitle("Cantidad por Ha");
		anchoDialog.setContentText("Cantidad por Ha");
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearSiembraMapTask umTask = new CrearSiembraMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			siembras.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("CrearSiembraMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		this.executorPool.execute(umTask);		
	}

	private void doCrearFertilizacion(Poligono poli) {
		FertilizacionLabor labor = new FertilizacionLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog("Dosis");
		anchoDialog.setTitle("Configure Dosis");
		anchoDialog.setContentText("Dosis");
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearFertilizacionMapTask umTask = new CrearFertilizacionMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			fertilizaciones.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("OpenHarvestMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		this.executorPool.execute(umTask);		
	}

	private void doCrearCosecha(Poligono poli) {
		CosechaLabor labor = new CosechaLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog("Rinde");
		anchoDialog.setTitle("Configure Rinde");
		anchoDialog.setContentText("Rinde");
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearCosechaMapTask umTask = new CrearCosechaMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			cosechas.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("OpenHarvestMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		this.executorPool.execute(umTask);		
	}



	private void doConvertirNdviACosecha(Ndvi ndvi) {
		CosechaLabor labor = new CosechaLabor();
		labor.nombreProperty.set(ndvi.getNombre());
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog("Rinde");
		anchoDialog.setTitle("Configure Rinde");
		anchoDialog.setContentText("Rinde");
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		ConvertirNdviACosechaTask umTask = new ConvertirNdviACosechaTask(labor,ndvi,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			cosechas.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("OpenHarvestMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		this.executorPool.execute(umTask);		
	}

	private void doEditCosecha(CosechaLabor cConfigured ) {
		if(cConfigured==null){
			Optional<CosechaLabor> cosechaSelected = HarvestSelectDialogController.select(this.cosechas);
			if(cosechaSelected.isPresent()){
				cConfigured= cosechaSelected.get();
			} else {
				return;
			}
		}
		//if(cosechaSelected.isPresent()){		
		Optional<CosechaLabor> cosechaConfigured=HarvestConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			//cConfigured.getLayer().removeAllRenderables();
			ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				//CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
				//insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				//	viewGoTo(ret);
				this.wwjPanel.repaint();
				System.out.println("EditHarvestMapTask succeded");
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			this.executorPool.execute(umTask);
		}
		//}
	}

	private void doExtraerPoligonos(Labor<?> labor ) {		
			ExtraerPoligonosDeCosechaTask umTask = new ExtraerPoligonosDeCosechaTask(labor);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
			
				List<Poligono> poligonos = (List<Poligono>)handler.getSource().getValue();
				this.showPoligonos(poligonos);
				umTask.uninstallProgressBar();
			
				this.wwjPanel.repaint();
				System.out.println("poligonos Extraidos succeded");
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			this.executorPool.execute(umTask);
	
	}

	private void doEditPulverizacion(PulverizacionLabor cConfigured ) {
		Optional<PulverizacionLabor> cosechaConfigured=PulverizacionConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			//cConfigured.getLayer().removeAllRenderables();
			ProcessPulvMapTask umTask = new ProcessPulvMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				//CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				//	viewGoTo(ret);
				this.wwjPanel.repaint();
				System.out.println("EditHarvestMapTask succeded");
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			this.executorPool.execute(umTask);
		}
	}

	private void doEditSiembra(SiembraLabor cConfigured ) {
		Optional<SiembraLabor> cosechaConfigured=SiembraConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			//cConfigured.getLayer().removeAllRenderables();
			ProcessSiembraMapTask umTask = new ProcessSiembraMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				//CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				//	viewGoTo(ret);
				this.wwjPanel.repaint();
				System.out.println("EditHarvestMapTask succeded");
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			this.executorPool.execute(umTask);
		}
	}

	private void doEditFertilizacion(FertilizacionLabor cConfigured ) {
		Optional<FertilizacionLabor> cosechaConfigured=FertilizacionConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			//cConfigured.getLayer().removeAllRenderables();
			ProcessFertMapTask umTask = new ProcessFertMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				//CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				//	viewGoTo(ret);
				this.wwjPanel.repaint();
				System.out.println("EditHarvestMapTask succeded");
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			this.executorPool.execute(umTask);
		}
	}

	// junta 2 o mas cosechas en una 
	private void doUnirCosechas(CosechaLabor cosechaLabor) {
		List<CosechaLabor> cosechasAUnir = new ArrayList<CosechaLabor>();
		if(cosechaLabor == null){
			List<CosechaLabor> cosechasEnabled = cosechas.stream().filter((l)->{
				Layer layer =l.getLayer();
				return layer!=null&&layer.isEnabled();}).collect(Collectors.toList());
			cosechasAUnir.addAll( cosechasEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {

			cosechasAUnir.add(cosechaLabor);

		}
		UnirCosechasMapTask umTask = new UnirCosechasMapTask(cosechasAUnir);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			if(ret.getLayer()!=null){
				cosechas.add(ret);
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
			}
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			System.out.println("ProcessUniteHarvestMapsTask succeded");
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();					
		this.executorPool.execute(umTask);
		//			}
		//		}
	}

	private void doGrillarCosechas(CosechaLabor cosechaAGrillar) {
		List<CosechaLabor> cosechasAUnir = new ArrayList<CosechaLabor>();
		if(cosechaAGrillar == null){
			List<CosechaLabor> cosechasEnabled = cosechas.stream().filter((l)->
			{
				Layer layer =l.getLayer();
				return layer!=null&&layer.isEnabled();}
					).collect(Collectors.toList());
			cosechasAUnir.addAll( cosechasEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {
			cosechasAUnir.add(cosechaAGrillar);

		}
		Configuracion config = Configuracion.getInstance();
		TextInputDialog anchoDialog = new TextInputDialog(config.getPropertyOrDefault(CosechaConfig.ANCHO_GRILLA_KEY,"10"));
		anchoDialog.setTitle("Configure el ancho de la grilla");
		anchoDialog.setContentText("Ancho grilla");
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		if(anchoOptional.isPresent()){
			//System.out.println("optional is present con valor "+anchoOptional.get());

			config.setProperty(CosechaConfig.ANCHO_GRILLA_KEY,anchoOptional.get());
			config.save();

		} else{
			return;
		}

		GrillarCosechasMapTask umTask = new GrillarCosechasMapTask(cosechasAUnir);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			if(ret.getLayer()!=null){
				cosechas.add(ret);
				insertBeforeCompass(getWwd(), ret.getLayer());
				cosechaAGrillar.getLayer().setEnabled(false);
				this.getLayerPanel().update(this.getWwd());
			}
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			System.out.println("GrillarCosechasMapTask succeded");
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();					
		this.executorPool.execute(umTask);
	}

	private void doGuardarPoligono(Poligono layerObject){
		DAH.save(layerObject);
	}

	private void doRecomendFertPFromHarvest(CosechaLabor value) {
		// TODO generar un layer de fertilizacion a partir de una cosecha
		//el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
		//que producto aplico y en que densidad por hectarea


		FertilizacionLabor labor = new FertilizacionLabor();
		labor.setLayer(new LaborLayer());

		labor.getNombreProperty().setValue(value.getNombreProperty().get()+" Prescripcion P");
		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			return;
		}							

		RecomendFertPFromHarvestMapTask umTask = new RecomendFertPFromHarvestMapTask(labor,value);
		umTask.installProgressBar(progressBox);

		//	testLayer();
		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			fertilizaciones.add(ret);//TODO cambiar esto cuando cambie las acciones a un menu contextual en layerPanel
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();

			viewGoTo(ret);

			System.out.println("RecomendFertFromHarvestPotentialMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		umTask.start();
	}
	
	private void doRecomendFertNFromHarvest(CosechaLabor cosecha) {
		// TODO generar un layer de fertilizacion a partir de una cosecha
		//el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
		//que producto aplico y en que densidad por hectarea


		List<Suelo> suelosEnabled = suelos.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<FertilizacionLabor> fertEnabled = fertilizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
	
		

		FertilizacionLabor fertN = new FertilizacionLabor();
		fertN.setLayer(new LaborLayer());

		fertN.getNombreProperty().setValue(cosecha.getNombreProperty().get()+" Prescripcion N");
		Optional<FertilizacionLabor> fertConfigured= FertilizacionConfigDialogController.config(fertN);
		if(!fertConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			return;
		}							

		
		RecomendFertNFromHarvestMapTask umTask = new RecomendFertNFromHarvestMapTask(fertN,cosecha, suelosEnabled,
				 fertEnabled);
		
		//RecomendFertNFromHarvestMapTask umTask = new RecomendFertNFromHarvestMapTask(labor,value);
		umTask.installProgressBar(progressBox);

		//	testLayer();
		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			fertilizaciones.add(ret);//TODO cambiar esto cuando cambie las acciones a un menu contextual en layerPanel
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();

			viewGoTo(ret);

			System.out.println("RecomendFertNFromHarvestPotentialMapTask succeded");
			playSound();
		});//fin del OnSucceeded
		umTask.start();
	}

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	private void doOpenCosecha(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				CosechaLabor labor = new CosechaLabor(store);
				LaborLayer layer = new LaborLayer();
				labor.setLayer(layer);
				Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
					labor.dispose();//libero los recursos reservados
					continue;
				}							

				ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(labor);
				umTask.installProgressBar(progressBox);


				umTask.setOnSucceeded(handler -> {
					CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
					cosechas.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);
					umTask.uninstallProgressBar();
					System.out.println("OpenHarvestMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				this.executorPool.execute(umTask);

				//	umTask.start();//crea un nuevo thread y ejecuta el task (ProcessMapTask)
				//Platform.runLater(umTask);
			}//fin del for stores

		}//if stores != null

	}

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	private void doOpenFertMap(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			//	harvestMap.getChildren().clear();
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				FertilizacionLabor labor = new FertilizacionLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la fertilizacion");
					continue;
				}							

				ProcessFertMapTask umTask = new ProcessFertMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
					fertilizaciones.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("OpenFertMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				this.executorPool.execute(umTask);
			}//fin del for stores

		}//if stores != null

	}


	private void doOpenSiembraMap(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			//	harvestMap.getChildren().clear();
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				SiembraLabor labor = new SiembraLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<SiembraLabor> cosechaConfigured= SiembraConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la fertilizacion");
					continue;
				}							

				ProcessSiembraMapTask umTask = new ProcessSiembraMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
					siembras.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("OpenFertMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				this.executorPool.execute(umTask);
			}//fin del for stores

		}//if stores != null

	}

	//	private void doOpenSiembraMap() {
	//		FileDataStore store = chooseShapeFileAndGetStore();
	//		if (store != null) {
	//			/*
	//			 * miro el archivo y pregunto cuales son las columnas
	//			 * correspondientes
	//			 */
	//			List<String> availableColumns = getAvailableColumns(store);
	//
	//			ColumnSelectDialog csd = new ColumnSelectDialog(
	//					SiembraLabor.getRequieredColumns(), availableColumns);
	//
	//			Optional<Map<String, String>> result = csd.showAndWait();
	//
	//			SiembraLabor siembra = new SiembraLabor(store);
	//
	//			Map<String, String> columns = null;
	//			if (result.isPresent()) {
	//				columns = result.get();
	//
	//				siembra.setColumnsMap(columns);
	//				System.out.println("columns map: " + columns);
	//			} else {
	//				System.out.println("columns names not set");
	//			}
	//
	//			//			Double precioLabor = Costos.getInstance().precioSiembraProperty.getValue(); 
	//			//			Double precioInsumo = Costos.getInstance().precioSemillaProperty.getValue();
	//
	//
	//
	//			siembra.setLayer(new RenderableLayer());
	//			//			siembra.precioInsumoProperty.set(precioInsumo);
	//			//			siembra.precioLaborProperty.set(precioLabor);
	//
	//			//			Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
	//			//			if(!cosechaConfigured.isPresent()){//
	//			//				System.out.println("el dialogo termino con cancel asi que no continuo con la fertilizacion");
	//			//				continue;
	//			//			}							
	//
	//
	//			ProcessSiembraMapTask psMapTask = 
	//					new ProcessSiembraMapTask(siembra);
	//			//		sgroup, precioLabor,  precioInsumo, store);
	//			psMapTask.installProgressBar(progressBox);
	//			//			ProgressBar progressBarTask = new ProgressBar();
	//			//			progressBox.getChildren().add(progressBarTask);
	//			//			progressBarTask.setProgress(0);
	//			//			progressBarTask.progressProperty().bind(
	//			//					psMapTask.progressProperty());
	//			//			Thread currentTaskThread = new Thread(psMapTask);
	//			//			currentTaskThread.setDaemon(true);
	//			//			currentTaskThread.start();
	//
	//			psMapTask.setOnSucceeded(handler -> {
	//				SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
	//				siembras.add(ret);
	//				insertBeforeCompass(getWwd(), ret.getLayer());
	//				this.getLayerPanel().update(this.getWwd());
	//				psMapTask.uninstallProgressBar();
	//				viewGoTo(ret);
	//
	//				System.out.println("OpenSiembraMap succeded");
	//				playSound();
	//			});
	//			this.executorPool.execute(psMapTask);
	//		}
	//	}

	// leer mapa de pulverizaciones y calcular costos
	//	private void doOpenPulvMap() {
	//		FileDataStore store = chooseShapeFileAndGetStore();
	//		if (store != null) {
	//			/*
	//			 * miro el archivo y pregunto cuales son las columnas
	//			 * correspondientes
	//			 */
	//			List<String> availableColumns = getAvailableColumns(store);
	//
	//			ColumnSelectDialog csd = new ColumnSelectDialog(
	//					PulverizacionItem.getRequieredColumns(), availableColumns);
	//
	//			Optional<Map<String, String>> result = csd.showAndWait();
	//
	//			PulverizacionLabor labor = new PulverizacionLabor(store);
	//			Map<String, String> columns = null;
	//			if (result.isPresent()) {
	//				columns = result.get();
	//
	//				labor.setColumnsMap(columns);
	//				System.out.println("columns map: " + columns);
	//			} else {
	//				System.out.println("columns names not set");
	//			}
	//
	//			//Double precioLabor = Costos.getInstance().precioPulvProperty.getValue(); 
	//
	//			//Group pGroup = new Group();
	//			//PulverizacionLabor pl = new PulverizacionLabor(store);
	//			ProcessPulvMapTask pulvmTask = new ProcessPulvMapTask(labor);
	//			pulvmTask.installProgressBar(progressBox);
	//			//			Thread currentTaskThread = new Thread(pulvmTask);
	//			//			currentTaskThread.setDaemon(true);
	//			//			currentTaskThread.start();
	//
	//			pulvmTask.setOnSucceeded(handler -> {
	//				PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
	//				pulverizaciones.add(ret);
	//				insertBeforeCompass(getWwd(), ret.getLayer());
	//				this.getLayerPanel().update(this.getWwd());
	//				pulvmTask.uninstallProgressBar();
	//				viewGoTo(ret);
	//
	//				System.out.println("OpenPulvMap succeded");
	//				playSound();
	//			});
	//			executorPool.execute(pulvmTask);
	//		}
	//	}

	private void doOpenPulvMap(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			//	harvestMap.getChildren().clear();
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				PulverizacionLabor labor = new PulverizacionLabor(store);
				//	SiembraLabor labor = new SiembraLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<PulverizacionLabor> cosechaConfigured= PulverizacionConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la fertilizacion");
					continue;
				}							

				ProcessPulvMapTask umTask = new ProcessPulvMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
					pulverizaciones.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("ProcessPulvMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				this.executorPool.execute(umTask);
			}//fin del for stores

		}//if stores != null

	}

	private void doOpenSoilMap(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			//	harvestMap.getChildren().clear();
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Suelo labor = new Suelo(store);
				//	SiembraLabor labor = new SiembraLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la fertilizacion");
					continue;
				}							

				OpenSoilMapTask umTask = new OpenSoilMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					Suelo ret = (Suelo)handler.getSource().getValue();
					suelos.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("OpenSoilMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				this.executorPool.execute(umTask);
			}//fin del for stores

		}//if stores != null

	}

	private void doOpenMarginlMap() {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(null);
		if (stores != null) {
			//	harvestMap.getChildren().clear();
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Margen labor = new Margen(store);
				//	SiembraLabor labor = new SiembraLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<Margen> cosechaConfigured= MargenConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la fertilizacion");
					continue;
				}							

				OpenMargenMapTask umTask = new OpenMargenMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					Margen ret = (Margen)handler.getSource().getValue();
					margenes.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("OpenSoilMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				this.executorPool.execute(umTask);
			}//fin del for stores

		}//if stores != null

	}

	private void doProcessMargin() {		
		System.out.println("processingMargins");

		Margen margen = new Margen();
		margen.setLayer(new LaborLayer());

		//todo pasar el filtrado por visibles aca y pasar nuevas listas solo con las visibles
		List<PulverizacionLabor> pulvEnabled = pulverizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<FertilizacionLabor> fertEnabled = fertilizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<SiembraLabor> siemEnabled = siembras.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<CosechaLabor> cosechasEnabled = cosechas.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());


		margen.setFertilizaciones(fertEnabled);
		margen.setPulverizaciones(pulvEnabled);
		margen.setSiembras(siemEnabled);
		margen.setCosechas(cosechasEnabled);

		StringBuilder sb = new StringBuilder();
		sb.append("Renta ");
		cosechasEnabled.forEach((c)->sb.append(c.getNombreProperty().get()+" "));
		margen.getNombreProperty().setValue(sb.toString());

		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con el calculo de los margenes");
			return;
		}							


		ProcessMarginMapTask uMmTask = new ProcessMarginMapTask(margen);
		//	ProgressBar progressBarTask = new ProgressBar();
		uMmTask.installProgressBar(progressBox);
		//progressBox.getChildren().add(progressBarTask);
		//		progressBarTask.setProgress(0);
		//	progressBarTask.progressProperty().bind(uMmTask.progressProperty());
		//			Thread currentTaskThread = new Thread(uMmTask);
		//			currentTaskThread.setDaemon(true);
		//			currentTaskThread.start();

		uMmTask.setOnSucceeded(handler -> {
			Margen ret = (Margen)handler.getSource().getValue();
			uMmTask.uninstallProgressBar();

			this.margenes.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();

			viewGoTo(ret);

			System.out.println("ProcessMarginTask succeded");
		});
		executorPool.execute(uMmTask);
	}

	private void doEditMargin(Margen margen) {		
		System.out.println("editingMargins");
		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con el calculo de los margenes");
			return;
		}							
		OpenMargenMapTask uMmTask = new OpenMargenMapTask(margen);
		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			this.getLayerPanel().update(this.getWwd());
			uMmTask.uninstallProgressBar();
			this.wwjPanel.repaint();
			System.out.println("EditMarginTask succeded");
			playSound();
		});
		executorPool.execute(uMmTask);
		//}
	}

	private void doProcesarBalanceFosforo() {		
		System.out.println("processingBalanceDeFosforo");
		//todo pasar el filtrado por visibles aca y pasar nuevas listas solo con las visibles
		List<Suelo> suelosEnabled = suelos.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<FertilizacionLabor> fertEnabled = fertilizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		//List<SiembraLabor> siemEnabled = siembras.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<CosechaLabor> cosechasEnabled = cosechas.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());

		//		Suelo suelo = new Suelo();
		//		suelo.setLayer(new LaborLayer());
		//		StringBuilder sb = new StringBuilder();
		//		sb.append("Suelo ");
		//		cosechasEnabled.forEach((c)->sb.append(c.getNombreProperty().get()+" "));
		//		suelo.getNombreProperty().setValue(sb.toString());

		//		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		//		if(!margenConfigured.isPresent()){//
		//			System.out.println("el dialogo termino con cancel asi que no continuo con el calculo del balance de fosforo");
		//			return;
		//		}							


		ProcessNewSoilMapTask balanceFosforoTask = new ProcessNewSoilMapTask(suelosEnabled,
				cosechasEnabled, fertEnabled);

		balanceFosforoTask.installProgressBar(progressBox);

		balanceFosforoTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			balanceFosforoTask.uninstallProgressBar();

			this.suelos.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println("ProcessBalanceDeFosforoTask succeded");
		});
		executorPool.execute(balanceFosforoTask);
	}

	//	private void doExportMargins() {
	//		@SuppressWarnings("unchecked")
	//		List<Margen> rentas = this.rentaTree.queryAll();
	//		//	System.out.println("construyendo el shp para las rentas "+rentas.size());
	//		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
	//				RentabilidadItem.getType());
	//		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
	//
	//		for (RentabilidadItem renta : rentas) {
	//			SimpleFeature rentaFeature = renta.getFeature(featureBuilder);
	//			features.add(rentaFeature);
	//			//	System.out.println("agregando a features "+rentaFeature);
	//
	//		}
	//
	//		File shapeFile =  getNewShapeFile();
	//
	//		Map<String, Serializable> params = new HashMap<String, Serializable>();
	//		try {
	//			params.put("url", shapeFile.toURI().toURL());
	//		} catch (MalformedURLException e) {
	//			e.printStackTrace();
	//		}
	//		params.put("create spatial index", Boolean.TRUE);
	//
	//
	//		ShapefileDataStore newDataStore=null;
	//		try {
	//			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
	//			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
	//			newDataStore.createSchema(RentabilidadItem.getType());
	//
	//			//		System.out.println("antes de forzar wgs 84");
	//
	//			/*
	//			 * You can comment out this line if you are using the createFeatureType
	//			 * method (at end of class file) rather than DataUtilities.createType
	//			 */
	//			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
	//			//		System.out.println("forzando dataStore WGS84");
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//
	//		String typeName = newDataStore.getTypeNames()[0];
	//		//	System.out.println("typeName 0 del newDataStore es "+typeName);
	//		SimpleFeatureSource featureSource = null;
	//		try {
	//			featureSource = newDataStore.getFeatureSource(typeName);
	//			//	System.out.println("cree new featureSource "+featureSource.getInfo());
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//
	//		if (featureSource instanceof SimpleFeatureStore) {			
	//
	//			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
	//			Transaction transaction = new DefaultTransaction("create");
	//			featureStore.setTransaction(transaction);
	//
	//			/*
	//			 * SimpleFeatureStore has a method to add features from a
	//			 * SimpleFeatureCollection object, so we use the
	//			 * ListFeatureCollection class to wrap our list of features.
	//			 */
	//			SimpleFeatureCollection collection = new ListFeatureCollection(RentabilidadItem.getType(), features);
	//			//	System.out.println("agregando features al store " +collection.size());
	//			try {
	//				featureStore.addFeatures(collection);
	//				transaction.commit();
	//				//	System.out.println("commiting transaction "+ featureStore.getCount(new Query()));
	//			} catch (Exception problem) {
	//				problem.printStackTrace();
	//				try {
	//					transaction.rollback();
	//					//	System.out.println("transaction rolledback");
	//				} catch (IOException e) {
	//
	//					e.printStackTrace();
	//				}
	//
	//			} finally {
	//				try {
	//					transaction.close();
	//					//System.out.println("closing transaction");
	//				} catch (IOException e) {
	//
	//					e.printStackTrace();
	//				}
	//			}
	//
	//		}		
	//	}

	private void doExportLabor(Labor<?> cosechaLabor) {
		if(cosechaLabor==null){
			Optional<CosechaLabor> cosechaSelected = HarvestSelectDialogController.select(this.cosechas);
			if(cosechaSelected.isPresent()){
				cosechaLabor= cosechaSelected.get();
			} else {
				return;
			}
		}

		final Labor<?> laborToExport = cosechaLabor;

		String nombre = laborToExport.getNombreProperty().get();
		File shapeFile =  getNewShapeFile(nombre);

		ExportLaborMapTask ehTask = new ExportLaborMapTask(cosechaLabor,shapeFile);
		executorPool.execute(ehTask);
	}
	
	private void doExportPrescripcion(FertilizacionLabor laborToExport) {
		String nombre = laborToExport.getNombreProperty().get();
		File shapeFile =  getNewShapeFile(nombre);
		executorPool.execute(()->{//esto me introduce un error al grabar en el que se pierderon features

			
			
			SimpleFeatureType type = null;
			String typeDescriptor = "the_geom:MultiPolygon:srid=4326,"//"*geom:Polygon,"the_geom
					+ "Rate" + ":Integer";

			try {
				type = DataUtilities.createType("PrescripcionFertilizacion", typeDescriptor);
			} catch (SchemaException e) {

				e.printStackTrace();
			}
		

			ShapefileDataStore newDataStore = createShapefileDataStore(shapeFile,type);

			SimpleFeatureIterator it = laborToExport.outCollection.features();
			DefaultFeatureCollection pointFeatureCollection =  new DefaultFeatureCollection("internal",type);
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
			while(it.hasNext()){
				FertilizacionItem fi = laborToExport.constructFeatureContainer(it.next());
				fb.add(fi.getGeometry());
				fb.add(fi.getDosistHa().intValue());
			
				SimpleFeature pointFeature = fb.buildFeature(fi.getId().toString());
				pointFeatureCollection.add(pointFeature);

			}
			it.close();

			SimpleFeatureSource featureSource = null;
			try {
				String typeName = newDataStore.getTypeNames()[0];
				featureSource = newDataStore.getFeatureSource(typeName);
			} catch (IOException e) {

				e.printStackTrace();
			}


			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				Transaction transaction = new DefaultTransaction("create");
				featureStore.setTransaction(transaction);

				/*
				 * SimpleFeatureStore has a method to add features from a
				 * SimpleFeatureCollection object, so we use the
				 * ListFeatureCollection class to wrap our list of features.
				 */

				try {
					featureStore.setFeatures(pointFeatureCollection.reader());
					try {
						transaction.commit();
					} catch (Exception e1) {
						e1.printStackTrace();
					}finally {
						try {
							transaction.close();
							//System.out.println("closing transaction");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}		

			Configuracion config = Configuracion.getInstance();
			config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
			config.save();
		});//fin del run later
		
	}

	private void doExportHarvestDePuntos(CosechaLabor cosechaLabor) {
		if(cosechaLabor==null){
			Optional<CosechaLabor> cosechaSelected = HarvestSelectDialogController.select(this.cosechas);
			if(cosechaSelected.isPresent()){
				cosechaLabor= cosechaSelected.get();
			} else {
				return;
			}
		}

		CosechaLabor laborToExport = cosechaLabor;

		String nombre = laborToExport.getNombreProperty().get();
		File shapeFile =  getNewShapeFile(nombre);
		Platform.runLater(()->{//esto me introduce un error al grabar en el que se pierderon features

			SimpleFeatureType type = laborToExport.getPointType();

			ShapefileDataStore newDataStore = createShapefileDataStore(shapeFile,type);

			SimpleFeatureIterator it = laborToExport.outCollection.features();
			DefaultFeatureCollection pointFeatureCollection =  new DefaultFeatureCollection("internal",type);
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
			while(it.hasNext()){
				SimpleFeature sf = it.next();		
				List<Object> attributes = sf.getAttributes();
				for(Object o : attributes){
					if(o instanceof MultiPolygon){
						int index =attributes.indexOf(o);
						attributes.set(index, ((MultiPolygon)sf.getDefaultGeometry()).getCentroid());

					}
				}
				fb.addAll(sf.getAttributes());


				SimpleFeature pointFeature = fb.buildFeature(LaborItem.getID(sf));
				pointFeatureCollection.add(pointFeature);

			}
			it.close();

			SimpleFeatureSource featureSource = null;
			try {
				String typeName = newDataStore.getTypeNames()[0];
				featureSource = newDataStore.getFeatureSource(typeName);
			} catch (IOException e) {

				e.printStackTrace();
			}


			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				Transaction transaction = new DefaultTransaction("create");
				featureStore.setTransaction(transaction);

				/*
				 * SimpleFeatureStore has a method to add features from a
				 * SimpleFeatureCollection object, so we use the
				 * ListFeatureCollection class to wrap our list of features.
				 */

				try {
					featureStore.setFeatures(pointFeatureCollection.reader());
					try {
						transaction.commit();
					} catch (Exception e1) {
						e1.printStackTrace();
					}finally {
						try {
							transaction.close();
							//System.out.println("closing transaction");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}		

			Configuracion config = Configuracion.getInstance();
			config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
			config.save();
		});//fin del run later
	}


	private ShapefileDataStore createShapefileDataStore(File shapeFile,
			SimpleFeatureType type) {
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put("url", shapeFile.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put("create spatial index", Boolean.TRUE);


		ShapefileDataStore newDataStore=null;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(type);
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
			//java.io.FileNotFoundException: D:\Dropbox\hackatonAgro\EmengareGis\MapasCrudos\shp\sup\out\grid\amb\Girszol_lote_19_s0limano_-_Harvesting.shp (Access is denied)
		}
		return newDataStore;
	}







	//	public void exportarSueloAShp(Quadtree newSoilTree) {
	//		@SuppressWarnings("unchecked")
	//		List<SueloItem> cosechas = newSoilTree.queryAll();
	//		//	System.out.println("construyendo el shp para las rentas "+rentas.size());
	//		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
	//				SueloItem.getType());
	//		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
	//
	//		for (SueloItem cosecha : cosechas) {
	//			SimpleFeature cosechaFeature = cosecha.getFeature(featureBuilder);
	//			features.add(cosechaFeature);
	//			//	System.out.println("agregando a features "+rentaFeature);
	//		}
	//
	//		File shapeFile =  getNewShapeFile();
	//		Map<String, Serializable> params = new HashMap<String, Serializable>();
	//		try {
	//			params.put("url", shapeFile.toURI().toURL());
	//		} catch (MalformedURLException e) {
	//			e.printStackTrace();
	//		}
	//		params.put("create spatial index", Boolean.TRUE);
	//
	//		ShapefileDataStore newDataStore=null;
	//		try {
	//			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
	//			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
	//			newDataStore.createSchema(SueloItem.getType());
	//
	//			//		System.out.println("antes de forzar wgs 84");
	//
	//			/*
	//			 * You can comment out this line if you are using the createFeatureType
	//			 * method (at end of class file) rather than DataUtilities.createType
	//			 */
	//			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
	//			//		System.out.println("forzando dataStore WGS84");
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//
	//		String typeName = newDataStore.getTypeNames()[0];
	//		//	System.out.println("typeName 0 del newDataStore es "+typeName);
	//		SimpleFeatureSource featureSource = null;
	//		try {
	//			featureSource = newDataStore.getFeatureSource(typeName);
	//			//	System.out.println("cree new featureSource "+featureSource.getInfo());
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//
	//		if (featureSource instanceof SimpleFeatureStore) {			
	//
	//			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
	//			Transaction transaction = new DefaultTransaction("create");
	//			featureStore.setTransaction(transaction);
	//
	//			/*
	//			 * SimpleFeatureStore has a method to add features from a
	//			 * SimpleFeatureCollection object, so we use the
	//			 * ListFeatureCollection class to wrap our list of features.
	//			 */
	//			SimpleFeatureCollection collection = new ListFeatureCollection(CosechaLabor.getFeatureType(), features);
	//			//	System.out.println("agregando features al store " +collection.size());
	//			try {
	//				featureStore.addFeatures(collection);
	//				transaction.commit();
	//				//	System.out.println("commiting transaction "+ featureStore.getCount(new Query()));
	//			} catch (Exception problem) {
	//				problem.printStackTrace();
	//				try {
	//					transaction.rollback();
	//					//	System.out.println("transaction rolledback");
	//				} catch (IOException e) {
	//
	//					e.printStackTrace();
	//				}
	//
	//			} finally {
	//				try {
	//					transaction.close();
	//					//System.out.println("closing transaction");
	//				} catch (IOException e) {
	//
	//					e.printStackTrace();
	//				}
	//			}
	//
	//		}
	//	}

	private void doShowDataTable(Labor<?> labor) {		   
		Platform.runLater(()->{

			ArrayList<LaborItem> ciLista = new ArrayList<LaborItem>();
			System.out.println("Comenzando a cargar la los datos de la tabla");
			Iterator<?> it = labor.outCollection.iterator();
			while(it.hasNext()){
				LaborItem ci = labor.constructFeatureContainerStandar((SimpleFeature)it.next(), false);
				ciLista.add(ci);
			}

			final ObservableList<LaborItem> dataLotes =
					FXCollections.observableArrayList(
							ciLista
							);

			SmartTableView<LaborItem> table = new SmartTableView<LaborItem>(dataLotes,dataLotes);
			table.setEditable(false);

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(labor.nombreProperty.get());
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	/**
	 * 
	 */
	private void doShowAcercaDe() {
		Alert acercaDe = new Alert(AlertType.INFORMATION);
		acercaDe.titleProperty().set("Acerca de "+JFXMain.TITLE_VERSION);
		acercaDe.initOwner(this.stage);
		//acercaDe.setHeaderText(this.TITLE_VERSION+"\n"+this.BUILD_INFO+"\nVisitar www.ursulagis.com");
		//acercaDe.contentTextProperty().set();
		String content =   "<b>"+JFXMain.TITLE_VERSION+"</b><br>"
				+JFXMain.BUILD_INFO
				+ "<br><b>"+"Visitar <a href=\"http://www.ursulagis.com\">www.ursulagis.com</a>"+"</b>";

		WebView webView = new WebView();
		webView.getEngine().loadContent("<html>"+content+"</html>");


		//   webView.setPrefSize(150, 60);
		acercaDe.setHeaderText("");
		acercaDe.setGraphic(null);

		acercaDe.getDialogPane().setContent(webView);;
		//  alert.showAndWait();
		acercaDe.setResizable(true);
		acercaDe.show();
		//TODO mostrar dialogo con informacion de la version, link a www.agrotoolbox.com y los creadores
	}

	private void doConfigCultivo() {
		Platform.runLater(()->{
			final ObservableList<Cultivo> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllCultivos()
							);

			SmartTableView<Cultivo> table = new SmartTableView<Cultivo>(dataLotes,dataLotes);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Cultivo("Nuevo Cultivo"));

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Cultivos");
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	private void doConfigFertilizantes() {
		Platform.runLater(()->{

			//			ArrayList<Fertilizante> ciLista = new ArrayList<Fertilizante>();
			//			System.out.println("Comenzando a cargar la los datos de la tabla");
			//
			//			ciLista.addAll(Fertilizante.fertilizantes.values());
			//			final ObservableList<Fertilizante> dataLotes =
			//					FXCollections.observableArrayList(
			//							ciLista
			//							);

			final ObservableList<Fertilizante> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllFertilizantes()
							);

			SmartTableView<Fertilizante> table = new SmartTableView<Fertilizante>(dataLotes,dataLotes);
			table.setEditable(true);
			
			table.setOnDoubleClick(()->new Fertilizante("Nuevo Fertilizante"));


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Fertilizantes");
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}


	private void doConfigCampania() {
		Platform.runLater(()->{
			final ObservableList<Campania> data =
					FXCollections.observableArrayList(
							DAH.getAllCampanias()
							);
			if(data.size()==0){
				data.add(new Campania("Nueva Campaña"));
			}
			SmartTableView<Campania> table = new SmartTableView<Campania>(data,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Campania("Nueva Campaña"));
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Campañas");
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}

	private void doConfigPoligonos() {
		Platform.runLater(()->{
			final ObservableList<Poligono> data =
					FXCollections.observableArrayList(
							DAH.getAllPoligonos()
							);

			SmartTableView<Poligono> table = new SmartTableView<Poligono>(data,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Poligono());

			
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Poligonos");
			tablaStage.setScene(scene);
			tablaStage.show();	 
			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				this.getLayerPanel().update(this.getWwd());
				//getWwd().redraw();
			});
		});	
	}

	private void doConfigEstablecimiento() {
		Platform.runLater(()->{

			final ObservableList<Establecimiento> data =
					FXCollections.observableArrayList(
							DAH.getAllEstablecimientos()
							);
			SmartTableView<Establecimiento> table = new SmartTableView<Establecimiento>(data,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Establecimiento("Nuevo Establecimiento"));

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Establecimientos");
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}


	private void doConfigLote() {
		Platform.runLater(()->{
			final ObservableList<Lote> data =
					FXCollections.observableArrayList(
							DAH.getAllLotes()
							);
			SmartTableView<Lote> table = new SmartTableView<Lote>(data,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Lote("Nuevo Lote"));

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Lotes");
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}


	private void doConfigEmpresa() {
		Platform.runLater(()->{
			final ObservableList<Empresa> data =
					FXCollections.observableArrayList(
							DAH.getAllEmpresas()
							);
			SmartTableView<Empresa> table = new SmartTableView<Empresa>(data,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Empresa("Nueva Empresa"));

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Empresas");
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}

	private void doConfigSemillas() {
		Platform.runLater(()->{

			//ArrayList<Semilla> ciLista = new ArrayList<Semilla>();
			//System.out.println("Comenzando a cargar la los datos de la tabla");

			//ciLista.addAll(Semilla.semillas.values());
			final ObservableList<Semilla> dataLotes =
					FXCollections.observableArrayList(	DAH.getAllSemillas());
			//System.out.println("mostrando la tabla de las semillas con "+dataLotes);
			SmartTableView<Semilla> table = new SmartTableView<Semilla>(dataLotes,dataLotes);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Semilla("Nueva Semilla",DAH.getAllCultivos().get(0)));
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Semillas");
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}
	private void doSnapshot(){
		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);

		WritableImage image = wwNode.snapshot(params, null);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Image");


		File lastFile = null;
		String lastFileName =  Configuracion.getInstance().getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile != null ){
			fileChooser.setInitialDirectory(lastFile.getParentFile());
			fileChooser.setInitialFileName(lastFile.getName());
		}

		//	if(file!=null)		fileChooser.setInitialDirectory(file.getParentFile());
		// Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				"PNG files (*.png)", "*.png");
		fileChooser.getExtensionFilters().add(extFilter);
		// Show save file dialog
		File snapsthotFile = fileChooser.showSaveDialog(this.stage);

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", snapsthotFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> getAvailableColumns(FileDataStore store) {
		List<String> availableColumns = new ArrayList<String>();

		SimpleFeatureType sch;
		try {
			sch = store.getSchema();
			List<AttributeType> types = sch.getTypes();
			for (AttributeType at : types) {
				availableColumns.add(at.getName().toString());
			}

		} catch (IOException e) {			
			e.printStackTrace();
		}
		return availableColumns;
	}

	private FileDataStore chooseShapeFileAndGetStore() {
		FileDataStore store = null;
		try{
			store = chooseShapeFileAndGetMultipleStores(null).get(0);
		}catch(Exception e ){
			e.printStackTrace();
		}
		return store;
	}

	private List<FileDataStore> chooseShapeFileAndGetMultipleStores(List<File> files) {
		if(files==null){
			//	List<File> 
			files =chooseFiles("SHP", "*.shp");;
		}
		List<FileDataStore> stores = new ArrayList<FileDataStore>();
		if (files != null) {
			for(File f : files){
				try {
					stores.add(FileDataStoreFinder.getDataStore(f));
				} catch (IOException e) {
					e.printStackTrace();
				}
				//stage.setTitle(TITLE_VERSION+" "+f.getName());
				//Configuracion.getInstance().setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());

			}


			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */

		}
		return stores;
	}

	private File directoryChooser(){
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setTitle("Seleccione un directorio");

		Configuracion config = Configuracion.getInstance();
		File lastFile = null;
		String lastFileName =config.getPropertyOrDefault(Configuracion.LAST_FILE,"");

		if(lastFileName != null){
			//LAST_FILE=F\:\\AgGPS\\Data\\Cliente_Predet\\Establecimiento_Predet\\030817_0001_EZ64952\\Swaths.shp
			//lastfile es valido pero ya no existe
			lastFile = new File(lastFileName);
		}

		if(lastFile != null && lastFile.exists()){

			if(!lastFile.isDirectory()){
				lastFile= lastFile.getParentFile();
			}
			
			fileChooser.setInitialDirectory(lastFile);
		}

		File selectedDirectory = fileChooser.showDialog(this.stage);

		if(selectedDirectory!=null){
			File f = selectedDirectory;
			config.setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());	
			config.save();
		}

		return selectedDirectory;
	}

	/**
	 * 
	 * @param f1 filter Title "JPG"
	 * @param f2 filter regex "*.jpg"
	 */
	private List<File> chooseFiles(String f1,String f2) {
		List<File> files =null;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Seleccione un archivo");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(f1, f2));

		Configuracion config = Configuracion.getInstance();
		File lastFile = null;

		String lastFileName =config.getPropertyOrDefault(Configuracion.LAST_FILE,"");
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile != null ){
			fileChooser.setInitialDirectory(lastFile.getParentFile());
			fileChooser.setInitialFileName(lastFile.getName());
		}
		try{
			files = fileChooser.showOpenMultipleDialog(this.stage);
			//		file = files.get(0);
		}catch(IllegalArgumentException e){
			fileChooser.setInitialDirectory(null);
			File file = fileChooser.showOpenDialog(this.stage);
			files = new LinkedList<File>();
			files.add(file);
		}

		if(files!=null && files.size()>0){
			File f = files.get(0);
			config.setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());	
			config.save();
		}

		return files;
	}

	/**
	 * este metodo se usa para crear archivos shp al momento de exportar mapas
	 * @param nombre es el nombre del archivo que se desea crear
	 * @return el archivo creado en la carpeta seleccionada por el usuario
	 */
	private File getNewShapeFile(String nombre) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Guardar ShapeFile");
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("SHP", "*.shp"));

		File lastFile = null;
		Configuracion config =Configuracion.getInstance();
		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile != null ){
			fileChooser.setInitialDirectory(lastFile.getParentFile());
			if(nombre == null){
				nombre = lastFile.getName();
			}
			fileChooser.setInitialFileName(nombre);
			config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
		}

		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());

		File file = fileChooser.showSaveDialog(this.stage);

		System.out.println("archivo seleccionado para guardar "+file);

		return file;
	}

	private void playSound() {
		executorPool.execute(()->{
			try	{
				AudioInputStream inputStream = AudioSystem
						.getAudioInputStream(getClass().getResourceAsStream(SOUND_FILENAME));
				Clip clip = AudioSystem.getClip();
				clip.open(inputStream);
				clip.start();
			}catch (Exception e){
				e.printStackTrace();
			}});
	}

	public void addDragAndDropSupport(){
		scene.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				//System.out.println("dragging");
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.ANY);
				} else {
					event.consume();
				}
			}
		});

		// Dropping over surface
		scene.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					//TODO agregar soporte para archivos tiff y preguntar si es una cosecha o siembra, etc
					//	String filePath = null;
					FileNameExtensionFilter filter = new FileNameExtensionFilter("shp only","shp");
					List<File> shpFiles = db.getFiles();
					shpFiles.removeIf(f->{
						return !filter.accept(f);
					});
					// update Configuracion.lasfFile
					if(shpFiles.size()>0){
						File lastFile = shpFiles.get(shpFiles.size()-1);
						Configuracion config = Configuracion.getInstance();
						config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
						config.save();
						doOpenCosecha(shpFiles);//ok!
					}



					FileNameExtensionFilter tifFilter = new FileNameExtensionFilter("tif only","tif");
					List<File> tifFiles = db.getFiles();
					tifFiles.removeIf(f->!tifFilter.accept(f));
					// update Configuracion.lasfFile
					if(tifFiles.size()>0){
						File lastFile = tifFiles.get(tifFiles.size()-1);
						Configuracion config = Configuracion.getInstance();
						config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
						config.save();
						tifFiles.stream().forEach((f)->showNdviTiffFile(f));
					}

					//ok!

				}
				event.setDropCompleted(success);
				event.consume();
			}
		});

	}

	public static void main(String[] args) {
		Application.launch(JFXMain.class, args);
	}
}
