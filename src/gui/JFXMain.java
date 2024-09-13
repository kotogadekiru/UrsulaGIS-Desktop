package gui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.data.FileDataStore;

import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import dao.config.Configuracion;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.pulverizacion.PulverizacionLabor;
import dao.recorrida.Recorrida;
import dao.siembra.SiembraLabor;
import dao.suelo.Suelo;
import dao.utils.PropertyHelper;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.data.BufferedImageRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.event.RenderingExceptionListener;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwindx.examples.util.ExampleUtil;
import gui.controller.ConfigGUI;
import gui.controller.CosechaGUIController;
import gui.controller.FertilizacionGUIController;
import gui.controller.NdviGUIController;
import gui.controller.PoligonoGUIController;
import gui.controller.PulverizacionGUIController;
import gui.controller.RecorridaGUIController;
import gui.controller.SiembraGUIController;
import gui.controller.SueloGUIController;
import gui.nww.LayerAction;
import gui.nww.LayerPanel;
import gui.nww.WWPanel;
import gui.utils.SmartTableView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tasks.ExportLaborMapTask;
import tasks.ProcessMapTask;
import tasks.importar.OpenMargenMapTask;
import tasks.procesar.ExtraerPoligonosDeLaborTask;
import tasks.procesar.JuntarShapefilesTask;
import utils.DAH;
import utils.FileHelper;
import utils.TarjetaHelper;

public class JFXMain extends Application {
	private static final String PREFERED_TREE_WIDTH_KEY = "PREFERED_TREE_WIDTH";
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE = "gov.nasa.worldwind.avkey.InitialAltitude"; 
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE = "gov.nasa.worldwind.avkey.InitialLongitude"; 
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE = "gov.nasa.worldwind.avkey.InitialLatitude"; 
	public static Configuracion config = Configuracion.getInstance();

	public static final String VERSION = "0.2.30"; 
	public static final String TITLE_VERSION = "Ursula GIS-"+VERSION; 
	public static final String buildDate = "28/06/2024";
	public static  final String ICON ="gui/ursula_logo_2020.png";//"gui/32x32-icon-earth.png";// "gui/1-512.png";//UrsulaGIS-Desktop/src/gui/32x32-icon-earth.png 
	private static final String SOUND_FILENAME = "gui/exito4.mp3";//"gui/Alarm08.wav";//"Alarm08.wav" funciona desde eclipse pero no desde el jar  

	public static Stage stage=null;
	private Scene scene=null;

	private Dimension canvasSize = new Dimension(1500, 800);

	public WWPanel wwjPanel=null;
	protected LayerPanel layerPanel=null;
	public VBox progressBox = new VBox();

	public static ExecutorService executorPool = Executors.newCachedThreadPool();
	private Node wwNode=null;//contiene el arbol con los layers y el swingnode con el world wind
	private boolean isPlayingSound=false;

	//GUI Controllers
	public ConfigGUI configGUIController = new ConfigGUI(this);
	public CosechaGUIController cosechaGUIController=new CosechaGUIController(this);
	public PoligonoGUIController poligonoGUIController= new PoligonoGUIController(this);;
	public PulverizacionGUIController pulverizacionGUIController = new PulverizacionGUIController(this);
	public SiembraGUIController siembraGUIController = new SiembraGUIController(this);
	public FertilizacionGUIController fertilizacionGUIController = new FertilizacionGUIController(this);
	public NdviGUIController ndviGUIController= new NdviGUIController(this);
	public RecorridaGUIController recorridaGUIController= new RecorridaGUIController(this);
	public SueloGUIController sueloGUIController= new SueloGUIController(this);

	@Override
	public void start(Stage primaryStage) {
		try {
			JFXMain.stage = primaryStage;
			primaryStage.setTitle(TITLE_VERSION);
			URL url = JFXMain.class.getClassLoader().getResource(ICON);
			primaryStage.getIcons().add(new Image(url.toURI().toString()));
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
			setInitialPosition();//pone init Lat y initLong en Configuracion
			VBox vBox1 = new VBox();
			vBox1.getChildren().add(menuBar);
			createSwingNode(vBox1);
			pane.getChildren().add(vBox1);			

			primaryStage.setOnHiding((e)-> {				
				//close swing node
				Platform.runLater(()->{
					JFXMain.config.save();
					DAH.closeEm();					
					System.out.println("Application Closed by click to Close Button(X)"); 
					getWwd().shutdown();
					System.exit(0); 
				});
			});
			configGUIController.startKeyBoardListener();
			primaryStage.show();

			//start clearCache cronJob
			startClearCacheCronJob();
		}catch(Exception e) {
			System.out.println("no se pudo hacer start de JFXMain.start(stage)");
			e.printStackTrace();
		}
	}
	


	private void startClearCacheCronJob() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		Property<LocalTime> lastClearCacheRun=new SimpleObjectProperty<LocalTime>(LocalTime.now());
		Property<Long> lagTimeProperty=new SimpleObjectProperty<Long>(new Long(600));//10min
		Runnable clearCaches = ()->{			
			Consumer<Labor<?>> checkCache = l->{
				synchronized(l){
					//Check against reads in the last minute
					if(l.cacheLastRead.isBefore(LocalTime.now().minusSeconds(60))) {
						l.clearCache();
					}}};
					getLaboresCargadas().forEach(checkCache);
					long lagTime = lagTimeProperty.getValue();
					LocalTime init = LocalTime.now();
					if(lastClearCacheRun.getValue().plusSeconds(lagTime).isBefore(init)) {//aseguro 30segundos entre gc y gc
						System.gc();//600ms aprox
						LocalTime end = LocalTime.now();
						lastClearCacheRun.setValue(end);
						long deltaMs = (end.toNanoOfDay()-init.toNanoOfDay())/(1000*1000);
						lagTimeProperty.setValue(600*deltaMs/1000);
						System.out.println("tarde "+(deltaMs)+"ms en hacer gc() en JFXMain.startClearCacheCronJob()");
					}
		};
		scheduler.scheduleAtFixedRate(clearCaches, 60, 30, TimeUnit.SECONDS);//si suspendi la pc hace todos los jobs juntos
	}

	private static void setInitialPosition() {
		double initLat = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, "-35")); 
		double initLong = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE, "-62")); 
		double initAltitude = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, "19.07e5")); 
		//<Property name="gov.nasa.worldwind.avkey.InitialLatitude" value="-35"/>
		//<Property name="gov.nasa.worldwind.avkey.InitialLongitude" value="-62"/>
		//<Property name="gov.nasa.worldwind.avkey.InitialAltitude" value="19.07e5"/>
		initLat = (initLat>-90&&initLat<90)?initLat:-35.0;
		initLong = (initLong>-180&&initLong<180)?initLong:-62.0;//Chequeo que este entre valores validos
		Configuration.setValue(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, initLat);
		Configuration.setValue(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE,initLong);
		Configuration.setValue(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, initAltitude);
	}

	public void createSwingNode(VBox vBox1) {
		Task<Node> pfMapTask = new Task<Node>(){
			@Override
			protected Node call() {
				try{
					return initializeWorldWind();//esto tiene que estar antes de initialize
				}catch(Throwable t){
					t.printStackTrace();				
					return null;
				}
			}			
		};

		pfMapTask.setOnSucceeded(handler -> {			
			wwNode = (Node) handler.getSource().getValue();
			if(wwNode!=null) {
				vBox1.getChildren().add( wwNode);
				this.wwjPanel.repaint();	
			}else {
				System.err.println("fallo la iniciacion del worldwind node");
			}
		});
		executorPool.execute(pfMapTask);		
	}

	protected Node initializeWorldWind() {
//		try {//com.sun.java.swing.plaf.windows.WindowsLookAndFeel
//			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");//UIManager.getSystemLookAndFeelClassName()); 
//		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
//			ex.printStackTrace();
//		}

		//setDefaultSize(50);//esto funciona para la barra de abajo pero no para los placemarks
		//		try {
		//			// Create the WorldWindow.
		//			this.wwjPanel =	new WWPanel(canvasSize, true);
		//		}catch(Exception e) {
		//			Platform.runLater(()->{
		//				Alert a = new Alert(Alert.AlertType.ERROR);
		//				a.setHeaderText("No se pudo crear WorldWindow");
		//				String stackTrace = Arrays.toString(e.getStackTrace());
		//				a.setContentText(stackTrace);
		//				a.show();
		//			});
		//		}
		//una vez que se establecio el tamaño inicial ese es el tamaño maximo
		//this.wwjPanel.setPreferredSize(canvasSize);
		final SwingNode wwSwingNode = new SwingNode();
		// SwingUtilities.invokeLater(()-> {			                	 
		try {
			// Create the WorldWindow.
			wwjPanel =	new WWPanel(canvasSize, true);
			wwSwingNode.setContent(wwjPanel);
		}catch(Exception e) {
			Platform.runLater(()->{
				Alert a = new Alert(Alert.AlertType.ERROR);
				a.setHeaderText("No se pudo crear WorldWindow");
				String stackTrace = Arrays.toString(e.getStackTrace());
				a.setContentText(stackTrace);
				a.show();
			});
		}
		//});

		this.layerPanel = new LayerPanel(this.wwjPanel.getWwd(),stage.widthProperty(),stage.heightProperty());
		this.layerPanel.addToScrollPaneBottom(progressBox);

		setAccionesTreePanel();

		JFXMain.stage.heightProperty().addListener((o,old,nu)->{
			this.wwjPanel.setPreferredSize(new Dimension((int)stage.getHeight(),nu.intValue()));
			this.wwjPanel.repaint();
		});

		//ok
		JFXMain.stage.maximizedProperty().addListener((o,ov,nu)->{
			this.wwjPanel.repaint();	
		});

		SplitPane sp = new SplitPane();
		sp.getItems().addAll(layerPanel, wwSwingNode);

		//PREFERED_TREE_WIDTH=219,546		
		double initSplitPaneWidth = PropertyHelper.parseDouble(
				config.getPropertyOrDefault(PREFERED_TREE_WIDTH_KEY,
						PropertyHelper.formatDouble(stage.getWidth()*0.15f))).doubleValue();

		//me permite agrandar la pantalla sin que se agrande el arbol
		sp.setDividerPositions(initSplitPaneWidth/stage.getWidth());//15% de la pantalla de 1245 es 186px ; 1552.0 es fullscreen
		sp.getDividers().get(0).positionProperty().addListener((o,ov,nu)->{
			double newPreferredSplitPaneWidth = stage.getWidth()*nu.doubleValue();
			config.loadProperties();
			JFXMain.config.setProperty(PREFERED_TREE_WIDTH_KEY, PropertyHelper.formatDouble(newPreferredSplitPaneWidth));
			config.save();
		});

		JFXMain.stage.widthProperty().addListener((o,old,nu)->{
			double splitPaneWidth = PropertyHelper.parseDouble(
					config.getPropertyOrDefault(PREFERED_TREE_WIDTH_KEY,
							PropertyHelper.formatDouble(initSplitPaneWidth))).doubleValue();
			sp.setDividerPositions(splitPaneWidth/nu.doubleValue());
			this.wwjPanel.setPreferredSize(new Dimension(nu.intValue(),(int)stage.getWidth()));
			this.wwjPanel.repaint();
		});

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

		// descomentar esto para cargar los poligonos de la base de datos. bloquea la interface
		executorPool.execute(()->loadActiveLayers());
		return sp;
	}

	//	public static void setDefaultSize(int size) {
	//		Set<Object> keySet = UIManager.getLookAndFeelDefaults().keySet();
	//		//Object[] keys = keySet.toArray(new Object[keySet.size()]);
	//		keySet.forEach(key->{
	//			if (key != null && key.toString().toLowerCase().contains("font")) {
	//				Font font = UIManager.getDefaults().getFont(key);
	//				if (font != null) {
	//					font = font.deriveFont((float)size);
	//					UIManager.put(key, font);
	//				}
	//			}
	//		});
	//	}	
	
	private MenuBar constructMenuBar() {	
		MenuBar menuBar = new MenuBar();
		configGUIController.addMenuesToMenuBar(menuBar);
		menuBar.setPrefWidth(scene.getWidth());

		Messages.registerLocaleChangeListener(loc->{
			menuBar.getMenus().clear();
			configGUIController.addMenuesToMenuBar(menuBar);
		});

		return menuBar;
	}

	/**
	 * aca se configuran los menues contextuales del arbol de capas
	 //XXX agregar nuevas funcionalidades aca!!! 
	 */
	private void setAccionesTreePanel() {		
		pulverizacionGUIController.addPulverizacionesRootNodeActions();
		fertilizacionGUIController.addFertilizacionesRootNodeActions();
		siembraGUIController.addSiembrasRootNodeActions();
		cosechaGUIController.addCosechasRootNodeActions();
		poligonoGUIController.addPoligonosRootNodeActions();
		ndviGUIController.addNdviRootNodeActions();

		Map<Class<?>,List<LayerAction>> predicates = new HashMap<Class<?>,List<LayerAction>>();

		cosechaGUIController.addAccionesCosecha(predicates);
		fertilizacionGUIController.addAccionesFertilizacion(predicates);
		siembraGUIController.addAccionesSiembras(predicates);	
		pulverizacionGUIController.addAccionesPulverizaciones(predicates);

		poligonoGUIController.addAccionesPoligonos(predicates);
		poligonoGUIController.addAccionesCaminos(predicates);

		recorridaGUIController.addAccionesRecorridas(predicates);
		ndviGUIController.addAccionesNdvi(predicates);

		addAccionesMargen(predicates);
		sueloGUIController.addAccionesSuelos(predicates);		

		addAccionesLabor(predicates);
		addAccionesGenericas(predicates);

		layerPanel.setMenuItems(predicates);
	}
	
	private void addAccionesGenericas(Map<Class<?>, List<LayerAction>> predicates) {
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
					DAH.save(ndvi);
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
			//TODO show stage with slider
	//		Object layerObject =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
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
			 
					 
			 

			return "layer transparente" + layer.getName()+" "+layer.getOpacity(); 
		}));
	}	

	private void addAccionesMargen(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> margenesP = new ArrayList<LayerAction>();
		predicates.put(Margen.class, margenesP);
		/**
		 *Accion que permite editar un mapa de rentabilidad
		 */
		margenesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editMargenAction"),(layer)->{	
			doEditMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "margen editado" + layer.getName(); 
		}));
		
		/**
		 *Accion que permite resumir por categoria un mapa de rentabilidad
		 */
		margenesP.add(LayerAction.constructPredicate(Messages.getString("ResumirMargenMapTask.resumirAction"),(layer)->{	
			configGUIController.doResumirMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "margen resumido" + layer.getName(); 
		}));
	}

	private void addAccionesLabor(Map<Class<?>, List<LayerAction>> predicates) {
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
			enDesarrollo();
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
				doGuardarLabor((Labor<?>) layerObject);
			}
			return "guarde labor " + layer.getName();
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
			poligonoGUIController.doExtraerPoligonos((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "poligonos Extraidos " + layer.getName(); 
		}));
		
		
		/**
		 * Accion que permite extraer el contorno de una cosecha
		 * es solo de prueba. se puede realizar extrayendo poligonos y uniendolos
		 */
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.extraerContornoAction"),(layer)->{
//			doExtraerContorno((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "poligonos Extraidos " + layer.getName(); 
//		}));
		
		/**
		 * Accion que permite cortar una labor por el poligono/s seleccionado
		 */
		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.cortarCosechaAction"),(layer)->{			
			poligonoGUIController.doCortarLaborPorPoligono((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
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
				ndviGUIController.doGetNdviTiffFile(o);
			}
			return "ndvi obtenido" + layer.getName();	 
		}));
	}


	public void enDesarrollo() {
		Alert enDesarrollo = new Alert(AlertType.INFORMATION,Messages.getString("JFXMain.workInProgressAction")); 
		enDesarrollo.showAndWait();
	}

	//TODO permitir actualizar el modelo de elevaciones con informacion de las labores
	private void importElevations(){
		executorPool.execute(()->{
			//try{
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

			//				}catch (Exception e){
			//					e.printStackTrace();
			//				}
			// Get the WorldWindow's current elevation model.
			Globe globe = getWwd().getModel().getGlobe();
			//ElevationModel currentElevationModel = globe.getElevationModel();

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

	/**
	 * Insert the layer into the layer list just before the compass.
	 */	
	public static void insertBeforeCompass(WorldWindow wwd, Layer layer) {
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l instanceof CompassLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition, layer);
	}

	static {
		System.setProperty("java.net.useSystemProxies", "true");  
		if (Configuration.isMacOS()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");  
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name", 
					"UrsulaGIS Application"); 
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


	//	private void doLeerCosechaJD() {
	//		//empezar por el fdl y leer el nombre del archivo y el formato del archivo fdd
	//		/**
	//		 <LogDataBlock logDataProcessor="EvenByteDelta">
	//			<EvenByteDelta filePathName="50b2706b-0000-1000-7fdb-e1e1e114c450.fdd"
	//				xmlns="urn:schemas-johndeere-com:LogDataBlock:EvenByteDelta"
	//				filePosition="0" />
	//		</LogDataBlock>
	//		 */
	//		List<File> files =FileHelper.chooseFiles("FDL", "*.fdl");  
	//		ReadJDHarvestLog task = new ReadJDHarvestLog(files.get(0));
	//		task.installProgressBar(progressBox);
	//
	//		task.setOnSucceeded(handler -> {
	//			//File ret = (File)handler.getSource().getValue();
	//
	//			task.uninstallProgressBar();
	//			System.out.println("ReadJDHarvestLog succeded"); 
	//			playSound();
	//		});//fin del OnSucceeded						
	//		//umTask.start();					
	//		//this.executorPool.execute(task);
	//
	//		executorPool.submit(task);
	//	}


	@SuppressWarnings("unchecked")
	public List<CosechaLabor> getCosechasSeleccionadas() {
		return (List<CosechaLabor>) getObjectFromEnabledLayersOfClass(CosechaLabor.class);	
	}

	@SuppressWarnings("unchecked")
	public List<FertilizacionLabor> getFertilizacionesSeleccionadas() {
		return (List<FertilizacionLabor>) getObjectFromEnabledLayersOfClass(FertilizacionLabor.class);
	}

	@SuppressWarnings("unchecked")
	public List<PulverizacionLabor> getPulverizacionesSeleccionadas() {
		return (List<PulverizacionLabor>) getObjectFromEnabledLayersOfClass(PulverizacionLabor.class);
	}
	
	@SuppressWarnings("unchecked")
	public List<Ndvi> getNdviSeleccionados() {
		return (List<Ndvi>) getObjectFromEnabledLayersOfClass(Ndvi.class);
	}

	@SuppressWarnings("unchecked")
	public List<SiembraLabor> getSiembrasSeleccionadas() {
		return (List<SiembraLabor>) getObjectFromEnabledLayersOfClass(SiembraLabor.class);
	}

	@SuppressWarnings("unchecked")
	public List<Suelo> getSuelosSeleccionados() {
		return (List<Suelo>) getObjectFromEnabledLayersOfClass(Suelo.class);
	}

	@SuppressWarnings("unchecked")
	public List<Recorrida> getRecorridasActivas() {
		return (List<Recorrida>) getObjectFromEnabledLayersOfClass(Recorrida.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<?> getObjectFromEnabledLayersOfClass(Class clazz){
		LayerList layers = this.getWwd().getModel().getLayers();
		Stream<Layer> layersOfClazz = layers.stream().filter(l->{
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			return l.isEnabled() && o!=null && clazz.isAssignableFrom(o.getClass());
		});
		return layersOfClazz.map(l->l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR)).collect(Collectors.toList());
	}	

	@SuppressWarnings({ "rawtypes", "unchecked" }) 
	public List<?> getObjectFromLayersOfClass(Class clazz){
		LayerList layers = this.getWwd().getModel().getLayers();
		Stream<Layer> layersOfClazz = layers.stream().filter(l->{
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			return o!=null && clazz.isAssignableFrom(o.getClass());
		});
		return layersOfClazz.map(l->l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR)).collect(Collectors.toList());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" }) 
	public List<?> getLayersOfClass(Class clazz){
		LayerList layers = this.getWwd().getModel().getLayers();
		Stream<Layer> layersOfClazz = layers.stream().filter(l->{
			
			return l!=null && clazz.isAssignableFrom(l.getClass());
		});
		return layersOfClazz.collect(Collectors.toList());
	}

	public List<Labor<?>> getLaboresCargadas() {
		List<Labor<?>> recorridasActivas =new ArrayList<Labor<?>>();
		LayerList layers = this.getWwd().getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if ( o instanceof Labor<?>){
				Labor<?> r = (Labor<?>)o;
				recorridasActivas.add(r);
			}
		}
		return recorridasActivas;
	}

	private void loadActiveLayers(){
		//		long now = System.currentTimeMillis();
		//		DAH.getAllAgroquimicos();
		//		DAH.getAllCultivos();
		//		DAH.getAllSemillas();
		//		DAH.getAllFertilizantes();
		//		System.out.println("tarde "+(System.currentTimeMillis()-now)+" en inicializar los defaults");
		//tarde 11148 en inicializar los defaults
		TarjetaHelper.initTarjeta();
		this.poligonoGUIController.showPoligonosActivos();

		this.ndviGUIController.showNdviActivos();
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
		if(position==null) return;
		config.loadProperties();//si viene de editar siembra se pisan los datos con los viejos
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, String.valueOf(position.getLatitude().degrees));		
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE,String.valueOf(position.getLongitude().degrees));	
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, "64000"); 
		config.save();
		View view =getWwd().getView();
		view.goTo(position, 3000d);
	}

	private void showHistoLabor(Labor<?> cosechaLabor) {	
		Platform.runLater(()->{
			CosechaHistoChart histoChart = new CosechaHistoChart(cosechaLabor);
			Stage histoStage = new Stage();
			histoStage.setTitle(Messages.getString("CosechaHistoChart.Title"));
			histoStage.getIcons().add(new Image(ICON));
			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(JFXMain.stage);
			histoStage.show();
		});
	}
	
	public void importImagery()  {
		List<File>	files =FileHelper.chooseFiles("TIF","*.tif");  
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
						throw new Exception(Messages.getString("JFXMain.222")); 

					// Read the file into the raster. read() returns potentially several rasters if there are multiple
					// files, but in this case there is only one so just use the first element of the returned array.
					DataRaster[] rasters = reader.read(sourceFile, null);
					if (rasters == null || rasters.length == 0)
						throw new Exception(Messages.getString("JFXMain.223")); 

					DataRaster raster = rasters[0];

					// Determine the sector covered by the image. This information is in the GeoTIFF file or auxiliary
					// files associated with the image file.
					final Sector sector = (Sector) raster.getValue(AVKey.SECTOR);
					if (sector == null)
						throw new Exception(Messages.getString("JFXMain.224")); 

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
						throw new Exception(Messages.getString("JFXMain.225")); 
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
						layer.setName(Messages.getString("JFXMain.226")); 
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
	
	
	
//	private void doExtraerPoligonos(Labor<?> labor ) {	
//		ExtraerPoligonosDeLaborTask umTask = new ExtraerPoligonosDeLaborTask(labor);
//		umTask.installProgressBar(progressBox);
//		umTask.setOnSucceeded(handler -> {
//			@SuppressWarnings("unchecked")
//			List<Poligono> poligonos = (List<Poligono>)handler.getSource().getValue();
//			this.poligonoGUIController.showPoligonos(poligonos);			
//			umTask.uninstallProgressBar();
//			this.wwjPanel.repaint();
//			System.out.println(Messages.getString("JFXMain.280")); 
//			playSound();
//		});//fin del OnSucceeded						
//		JFXMain.executorPool.execute(umTask);
//	}

	private void doGuardarLabor(Labor<?> labor) {
		File zipFile = FileHelper.zipLaborToTmpDir(labor);//ok funciona
		byte[] byteArray = FileHelper.fileToByteArray(zipFile);		
		labor.setContent(byteArray);
		DAH.save(labor);

	}

	


	public void doJuntarShapefiles() {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(null);
		File shapeFile = FileHelper.getNewShapeFile("union");
		executorPool.execute(()->JuntarShapefilesTask.process(stores,shapeFile));
	}

	private void doEditMargin(Margen margen) {		
		System.out.println(Messages.getString("JFXMain.324")); 
		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.325")); 
			return;
		}							
		OpenMargenMapTask uMmTask = new OpenMargenMapTask(margen);
		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			this.getLayerPanel().update(this.getWwd());
			uMmTask.uninstallProgressBar();
			this.wwjPanel.repaint();
			System.out.println(Messages.getString("JFXMain.326")); 
			playSound();
		});
		executorPool.execute(uMmTask);
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

	public void doSnapshot(){
		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);

		WritableImage image = wwNode.snapshot(params, null);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("JFXMain.395")); 

		File lastFile = null;
		String lastFileName =  config.getPropertyOrDefault(Configuracion.LAST_FILE,Messages.getString("JFXMain.396")); 
		if(lastFileName != Messages.getString("JFXMain.397")){ 
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 

		fileChooser.setInitialDirectory(lastFile.getParentFile());
		fileChooser.setInitialFileName(lastFile.getName());

		// Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				Messages.getString("JFXMain.398"), Messages.getString("JFXMain.399"));  
		fileChooser.getExtensionFilters().add(extFilter);
		// Show save file dialog
		File snapsthotFile = fileChooser.showSaveDialog(JFXMain.stage);

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), Messages.getString("JFXMain.400"), snapsthotFile); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Pane getProgressBox() {
		return this.progressBox;
	}

	public void playSound() {
		if(!this.isPlayingSound) {
			executorPool.execute(()->{
				URL url = JFXMain.class.getClassLoader().getResource(SOUND_FILENAME);

				try {					
					AudioClip plonkSound = new AudioClip(url.toURI().toString());
					this.isPlayingSound=true;
					plonkSound.setVolume(0.50);
					plonkSound.play();
					this.isPlayingSound=false;

				} catch (URISyntaxException e) {

					e.printStackTrace();
				}

			});
		}else {
			System.out.println("no reprodusco el sonido porque ya hay un player andando");
		}
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
			//Configuracion config = Configuracion.getInstance();
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;

					//	String filePath = null;
					FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString("JFXMain.421"),Messages.getString("JFXMain.422"));  
					List<File> shpFiles = db.getFiles();
					shpFiles.removeIf(f->{
						return !filter.accept(f);
					});
					// update Configuracion.lasfFile

					if(shpFiles.size()>0){
						File lastFile = shpFiles.get(shpFiles.size()-1);
						config.loadProperties();
						config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
						config.save();
						cosechaGUIController.doOpenCosecha(shpFiles);//ok!
					}

					FileNameExtensionFilter tifFilter = new FileNameExtensionFilter(Messages.getString("JFXMain.423"),Messages.getString("JFXMain.424"));  
					List<File> tifFiles = db.getFiles();
					tifFiles.removeIf(f->!tifFilter.accept(f));
					if(tifFiles.size()>0){
						File lastFile = tifFiles.get(tifFiles.size()-1);

						config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
						config.save();
						tifFiles.stream().forEach((f)->ndviGUIController.showNdviTiffFile(f,null));
					}
				}
				event.setDropCompleted(success);
				event.consume();
			}
		});
	}

	public static void main(String[] args) {
		try	{
			//System.setProperty("prism.order", "es2");
			Application.launch(JFXMain.class, args);
		}catch (Exception e){
			JOptionPane.showMessageDialog(null,"hola: " + e.getMessage());
			try
			{
				PrintWriter pw = new PrintWriter(new File("ursula_error.log"));
				e.printStackTrace(pw);
				pw.close();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}
}
