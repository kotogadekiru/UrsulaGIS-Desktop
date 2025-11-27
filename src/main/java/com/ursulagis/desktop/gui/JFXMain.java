package com.ursulagis.desktop.gui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import java.time.LocalTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.event.Event;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Priority;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

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
import gov.nasa.worldwindx.examples.util.ExampleUtil;

import com.ursulagis.desktop.gui.controller.ConfigGUI;
import com.ursulagis.desktop.gui.controller.CosechaGUIController;
import com.ursulagis.desktop.gui.controller.FertilizacionGUIController;
import com.ursulagis.desktop.gui.controller.GenericLaborGUIController;
import com.ursulagis.desktop.gui.controller.MargenGUIController;
import com.ursulagis.desktop.gui.controller.NdviGUIController;
import com.ursulagis.desktop.gui.controller.PoligonoGUIController;
import com.ursulagis.desktop.gui.controller.PulverizacionGUIController;
import com.ursulagis.desktop.gui.controller.RecorridaGUIController;
import com.ursulagis.desktop.gui.controller.SiembraGUIController;
import com.ursulagis.desktop.gui.controller.SueloGUIController;
import com.ursulagis.desktop.gui.nww.LayerAction;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import com.ursulagis.desktop.gui.nww.WWPanel;
import com.ursulagis.desktop.gui.utils.LoggingOutputStream;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.TarjetaHelper;
import com.ursulagis.desktop.gui.UrsulaGISPreloader;

public class JFXMain extends Application {
	private static final String PREFERED_TREE_WIDTH_KEY = "PREFERED_TREE_WIDTH";
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE = "gov.nasa.worldwind.avkey.InitialAltitude"; 
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE = "gov.nasa.worldwind.avkey.InitialLongitude"; 
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE = "gov.nasa.worldwind.avkey.InitialLatitude"; 
	public static Configuracion config = Configuracion.getInstance();


	public static String buildDate = "12/09/2025";
	public static String VERSION = "1.0.1"; //XXX al cambiar la version cambiar el pom y el yml
	static {//init build date and version
		try {
			Properties props = new Properties();
			InputStream is = JFXMain.class.getClassLoader().getResourceAsStream("build-info.properties");
			if (is != null) {
				props.load(new InputStreamReader(is));
				String buildDate = props.getProperty("build.date");
				if (buildDate != null && !buildDate.startsWith("${")) {//not
					JFXMain.buildDate = buildDate;
				} else {
					// Try alternative property
					String buildTime = props.getProperty("build.time");
					if (buildTime != null && !buildTime.startsWith("${")) {//not
						JFXMain.buildDate = buildTime;
					} else {// entra por aca :(
						// Use current date as fallback
						JFXMain.buildDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
					}
				}
				String appVersion = props.getProperty("app.version");
				if (appVersion != null && !appVersion.startsWith("${")) {
					JFXMain.VERSION = appVersion;
				}
			}
		} catch (Exception e) {
			System.err.println("Could not load build date from properties: " + e.getMessage());
			// Use current date as fallback
			JFXMain.buildDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		}
	}

	
	public static final String TITLE_VERSION = "Ursula GIS Zulu-"+VERSION; 
	private static  final String ICON ="U_nueva_3_256x256_verde.png";//"gui/32x32-icon-earth.png";// "gui/1-512.png";//UrsulaGIS-Desktop/src/gui/32x32-icon-earth.png 
	private static final String SOUND_FILENAME = "exito4.mp3";//"gui/Alarm08.wav";//"Alarm08.wav" funciona desde eclipse pero no desde el jar
	//private static final String SOUND_FILENAME_WAV = "gui/Alarm08.wav"; // Fallback WAV file  

	public static Stage stage=null;
	private Scene scene=null;// usado en addDragAndDropSupport
	
	public WWPanel wwjPanel=null;//contiene el world wind
	protected LayerPanel layerPanel=null;//contriene el treeView con los layers
	private SplitPane sp=null;//contiene el layerPanel y el wwjPanel
	private Node wwNode=null;//contiene el arbol con los layers y el swingnode con el world wind
	public VBox progressBox = new VBox();

	public static ExecutorService executorPool = Executors.newCachedThreadPool();
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
	public MargenGUIController margenGUIController = new MargenGUIController(this);
	public GenericLaborGUIController genericGUIController = new GenericLaborGUIController(this);

	@Override
    public void init() throws Exception {
		notifyPreloader(new Preloader.StateChangeNotification(Preloader.StateChangeNotification.Type.BEFORE_INIT));
        // Perform non-GUI initialization tasks here
        System.out.println("Application init() called. Loading configurations...");
        // Example: load a configuration file
        // config = loadConfiguration(); 
		setInitialPosition();//pone init Lat y initLong en Configuracion
		TarjetaHelper.initTarjeta();
		poligonoGUIController.poligonosActivos = DAH.getPoligonosActivos();
		ndviGUIController.ndviActivos = DAH.getNdviActivos();

    }

	@Override
	public void start(Stage primaryStage) {
		notifyPreloader(new Preloader.StateChangeNotification(Preloader.StateChangeNotification.Type.BEFORE_START));
		try {
			JFXMain.stage = primaryStage;
			primaryStage.setTitle(TITLE_VERSION);
			loadMainIcon(primaryStage); 

			Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
			//double ratio = primaryScreenBounds.getHeight()/primaryScreenBounds.getWidth();//screen ratio
			int canvasWidth = (int) (primaryScreenBounds.getWidth()*0.8);
			int canvasHeight = (int) (primaryScreenBounds.getHeight()*0.8);			
			
			MenuBar menuBar = constructMenuBar();			
			
			// Notify preloader of progress
			notifyPreloader(new Preloader.ProgressNotification(0.2));			
			
			Node layerTreeAndWWswingNodeSplitPane = createLayerTreeAndWWswingNodeSplitPane();			
			VBox.setVgrow(layerTreeAndWWswingNodeSplitPane, Priority.ALWAYS);
			VBox menuAndMainVBox = new VBox(menuBar,layerTreeAndWWswingNodeSplitPane);			
			scene = new Scene(menuAndMainVBox,canvasWidth,canvasHeight);//, Color.White);
			primaryStage.setScene(scene);
			
			// Notify preloader of progress
			notifyPreloader(new Preloader.ProgressNotification(0.3));			

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
			
			// Notify preloader of progress
			notifyPreloader(new Preloader.ProgressNotification(0.4));
			// primaryStage.setOnShowing((e)->{
			// 	System.out.println("primaryStage onShowing");
				
			// });
			// primaryStage.setOnShown((e)->{
			// 	System.out.println("primaryStage onShown");				
			// });

			//primaryStage.show();//el show lo hace el preloader
		
			addDragAndDropSupport();
			//start clearCache cronJob
			startClearCacheCronJob();
			notifyPreloader(new Preloader.ProgressNotification(0.5));
			loadActiveLayers();
			notifyPreloader(new Preloader.ProgressNotification(0.9));
	
		}catch(Exception e) {
			System.out.println("no se pudo hacer start de JFXMain.start(stage)");
			e.printStackTrace();
		}
	}



	private void loadMainIcon(Stage primaryStage) {
		InputStream iconIs = this.getClass().getResourceAsStream(ICON);			
		if (iconIs != null) {
			primaryStage.getIcons().add(new Image(iconIs));
		}
	}
	


	private void startClearCacheCronJob() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		Property<LocalTime> lastClearCacheRun=new SimpleObjectProperty<LocalTime>(LocalTime.now());
		Property<Long> lagTimeProperty=new SimpleObjectProperty<Long>(Long.valueOf(600));//10min
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
		this.sp = (SplitPane) createLayerTreeAndWWswingNodeSplitPane();
		vBox1.getChildren().add(sp);
		//vBox1.getChildren().add( initializeWorldWind());
		//this.wwjPanel.repaint();
/* 
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
				
				// Notify preloader of progress
				notifyPreloader(new Preloader.ProgressNotification(0.6));
			}else {
				System.err.println("fallo la iniciacion del worldwind node");
			}
		});
		executorPool.execute(pfMapTask);		 */
	}

	/**
	 * devuelve un splitpane con el layerPanel y el wwjPanel
	 * @return
	 */
	protected Node createLayerTreeAndWWswingNodeSplitPane() {
		final SwingNode wwSwingNode = new SwingNode();
		JFXMain jfxMain = this;//referencia a this para que se pueda usar en el WWPanel
				try {
					// Create the WorldWindow.
					wwjPanel =	new WWPanel(true,jfxMain);					
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

		//DoubleProperty layerPanelWidthProperty = new SimpleDoubleProperty(stage.widthProperty().get()*0.15);
		
		this.layerPanel = new LayerPanel(this.wwjPanel.getWwd(),
							stage.widthProperty(),
							stage.heightProperty());
		//this.layerPanel.setPrefWidth(layerPanelWidthProperty.get());
		this.layerPanel.addToScrollPaneBottom(progressBox);
		setAccionesTreePanel();//inicializa los acciones del arbol de capas

		//ok
		JFXMain.stage.maximizedProperty().addListener((o,ov,nu)->{
			this.wwjPanel.repaint();	
		});

		sp = new SplitPane(layerPanel, wwSwingNode);
	
		

		

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

			
	
	

		return sp;
	}

	private void setSplitPaneDividerPosition() {
		//PREFERED_TREE_WIDTH=219,546		
		String defaultSplitPaneWidth = "200";//PropertyHelper.formatDouble(stage.getWidth()*0.15f);//Nan
		//System.out.println("defaultSplitPaneWidth: " + defaultSplitPaneWidth);
		String configSplitPaneWidth = config.getPropertyOrDefault(PREFERED_TREE_WIDTH_KEY, defaultSplitPaneWidth);
		//System.out.println("configSplitPaneWidth: " + configSplitPaneWidth);
		double initSplitPaneWidth = PropertyHelper.parseDouble(	configSplitPaneWidth).doubleValue();
		
		if(stage!=null && stage.getWidth()>200 && !Double.isNaN(stage.getWidth())) {
			double dividerPosition = initSplitPaneWidth/stage.getWidth();
			sp.getDividers().get(0).positionProperty().setValue(dividerPosition);
			//System.out.println("dividerPosition: " + dividerPosition);
		}		
		
		sp.getDividers().get(0).positionProperty().addListener((o,ov,nu)->{
			//System.out.println("divider position chaged from "+ov+" to "+nu);
			double newPreferredSplitPaneWidth = stage.getWidth()*nu.doubleValue();
			config.loadProperties();
			config.setProperty(PREFERED_TREE_WIDTH_KEY, PropertyHelper.formatDouble(newPreferredSplitPaneWidth));			
		});

		/* si se actualiza el tamaño */
		stage.widthProperty().addListener((o,old,nu)->{			
			if(nu!=null && nu.doubleValue()>0 && !Double.isNaN(nu.doubleValue())) {				
				String confSplitPaneWidth = config.getPropertyOrDefault(PREFERED_TREE_WIDTH_KEY,defaultSplitPaneWidth);
				double splitPaneWidth = PropertyHelper.parseDouble(	confSplitPaneWidth).doubleValue();
				sp.getDividers().get(0).positionProperty().setValue(splitPaneWidth/nu.doubleValue());
			//this.wwjPanel.setPreferredSize(new Dimension(nu.intValue(),(int)stage.getWidth()));
			//this.wwjPanel.repaint();
			}else{
				System.out.println("stage with chaged for the first time from "+old+" to "+nu);
			}
		});	
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
		//menuBar.setPrefWidth(scene.getWidth());

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
		margenGUIController.addMargenRootNodeActions();

		Map<Class<?>,List<LayerAction>> predicates = new HashMap<Class<?>,List<LayerAction>>();

		cosechaGUIController.addAccionesCosecha(predicates);
		fertilizacionGUIController.addAccionesFertilizacion(predicates);
		siembraGUIController.addAccionesSiembras(predicates);	
		pulverizacionGUIController.addAccionesPulverizaciones(predicates);

		poligonoGUIController.addAccionesPoligonos(predicates);
		poligonoGUIController.addAccionesCaminos(predicates);

		recorridaGUIController.addAccionesRecorridas(predicates);
		ndviGUIController.addAccionesNdvi(predicates);

		margenGUIController.addAccionesMargen(predicates);
		
		sueloGUIController.addAccionesSuelos(predicates);		
//TODO mover acciones a un nuevo controller
		genericGUIController.addAccionesLabor(predicates);
		genericGUIController.addAccionesGenericas(predicates);

		layerPanel.setMenuItems(predicates);
	}
	
//	private void addAccionesGenericas(Map<Class<?>, List<LayerAction>> predicates) {
//		List<LayerAction> todosP = new ArrayList<LayerAction>();
//		predicates.put(Object.class, todosP);
//		/**
//		 * Accion que permite quitar un item del arbol
//		 */
//		todosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.removeLayerAction"),(layer)->{
//			getWwd().getModel().getLayers().remove(layer);
//			Object layerObject =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//			if(layerObject!=null && Labor.class.isAssignableFrom(layerObject.getClass())){
//				Labor<?> l = (Labor<?>)layerObject;	
//				l.dispose();
//			}
//			if(layerObject instanceof Poligono){
//				Poligono poli = (Poligono) layerObject;
//				poli.setActivo(false);
//				if(poli.getId()!=null){
//					DAH.save(poli);
//				}
//			}
//			if(layerObject instanceof Ndvi){
//				Ndvi ndvi = (Ndvi) layerObject;
//				ndvi.setActivo(false);
//				if(ndvi.getId()!=null){
//					try {
//						DAH.save(ndvi);
//					}catch(Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}
//			MeasureTool mt = (MeasureTool)layer.getValue(PoligonLayerFactory.MEASURE_TOOL);		
//			if(mt!=null) {
//				mt.setArmed(false);
//				mt.dispose();
//			}
//
//			layer.dispose();
//			getLayerPanel().update(getWwd());
//			return "layer removido" + layer.getName(); 
//		}));
//		
//		//editar opacidad
//		//JFXMain.layerTransparencia=Transparencia
//		todosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.layerTransparencia"),(layer)->{
//			//TODO show stage with slider
//	//		Object layerObject =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//			double op = layer.getOpacity();
//			//double newOp = op*0.5;
//			
//			 Slider slider = new Slider(0, 1, op);
//			 slider.setShowTickMarks(true);
//			 slider.setShowTickLabels(true);
//			 slider.setMajorTickUnit(0.25f);
//			 slider.setBlockIncrement(0.1f);
//			 Scene sc = new Scene(slider,600,50);
//			 //TODO fixme no se ve un layer a travez del otro
//			 slider.valueProperty().addListener((obs,n,o)->{
//				 
//					layer.setOpacity(n.doubleValue());//newOp>0.1?newOp:1);
//					this.getWwd().redraw();
//					System.out.println("layer transparente" + layer.getName()+" "+layer.getOpacity());
//			 });
//			 Stage stage = new Stage();
//			 stage.setScene(sc);
//			 stage.initOwner(JFXMain.stage);
//			 stage.getIcons().addAll(JFXMain.stage.getIcons());
//			 stage.setTitle(Messages.getString("JFXMain.layerTransparencia")+" "+layer.getName());
//			 stage.show();
//			 
//					 
//			 
//
//			return "layer transparente" + layer.getName()+" "+layer.getOpacity(); 
//		}));
//	}	

//	private void addAccionesMargen(Map<Class<?>, List<LayerAction>> predicates) {
//		List<LayerAction> margenesP = new ArrayList<LayerAction>();
//		predicates.put(Margen.class, margenesP);
//		/**
//		 *Accion que permite editar un mapa de rentabilidad
//		 */
//		margenesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editMargenAction"),(layer)->{	
//			doEditMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "margen editado" + layer.getName(); 
//		}));
//		
//		/**
//		 *Accion que permite resumir por categoria un mapa de rentabilidad
//		 */
//		margenesP.add(LayerAction.constructPredicate(Messages.getString("ResumirMargenMapTask.resumirAction"),(layer)->{	
//			configGUIController.doResumirMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "margen resumido" + layer.getName(); 
//		}));
//	}

//	private void addAccionesLabor(Map<Class<?>, List<LayerAction>> predicates) {
//		List<LayerAction> laboresP = new ArrayList<LayerAction>();
//		predicates.put(Labor.class, laboresP);
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.goToLayerAction"),(layer)->{	
//			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//			if (layerObject!=null && Labor.class.isAssignableFrom(layerObject.getClass())){
//				viewGoTo((Labor<?>) layerObject);
//			}
//			return "went to " + layer.getName(); 
//		}));
//
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.GuardarLabor"),(layer)->{
//			enDesarrollo();
//			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//			if (layerObject==null){
//			}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
//				doGuardarLabor((Labor<?>) layerObject);
//			}
//			return "guarde labor " + layer.getName();
//		}));
//
//		/**
//		 * Accion que muesta el histograma
//		 */
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.showHistogramaLaborAction"),(layer)->{//	this::applyHistogramaCosecha);//(layer)->applyHistogramaCosecha(layer));
//			showHistoLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "histograma mostrado" + layer.getName(); 
//		}));
//
//		/**
//		 * Accion que permite extraer los poligonos de una cosecha para guardar
//		 */
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.extraerPoligonoAction"),(layer)->{
//			poligonoGUIController.doExtraerPoligonos((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "poligonos Extraidos " + layer.getName(); 
//		}));
//		
//		
//		/**
//		 * Accion que permite extraer el contorno de una cosecha
//		 * es solo de prueba. se puede realizar extrayendo poligonos y uniendolos
//		 */
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("PoligonGUIController.extraerContornoAction"),(layer)->{
//			poligonoGUIController.doExtraerContorno((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "poligonos Extraidos " + layer.getName(); 
//		}));
//		
//		/**
//		 * Accion que permite cortar una labor por el poligono/s seleccionado
//		 */
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.cortarCosechaAction"),(layer)->{			
//			poligonoGUIController.doCortarLaborPorPoligono((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "labor cortada" + layer.getName(); 
// 
//		}));
//
//		/**
//		 * Accion permite exportar la labor como shp
//		 */
//		laboresP.add(new LayerAction((layer)->{
//			if(layer==null){
//				return Messages.getString("JFXMain.exportLaborAction");  
//			} else{
//				doExportLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//				return "labor Exportada" + layer.getName(); 
//			}},Messages.getString("JFXMain.exportLaborAction")));
//
//		/**
//		 * Accion muestra una tabla con los datos de la cosecha
//		 */
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.showTableLayerAction"),(layer)->{
//			doShowDataTable((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
//			return "Tabla mostrada" + layer.getName(); 
//		}));
//
//		/**
//		 * Accion permite obtener ndvi
//		 */
//		laboresP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.downloadNDVI"),(layer)->{
//			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
//			if(o instanceof Labor){
//				ndviGUIController.doGetNdviTiffFile(o);
//			}
//			return "ndvi obtenido" + layer.getName();	 
//		}));
//	}


	public void enDesarrollo() {
		Alert enDesarrollo = new Alert(AlertType.INFORMATION,Messages.getString("JFXMain.workInProgressAction")); 
		enDesarrollo.showAndWait();
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
	public List<Margen> getMargenesSeleccionados() {
		return (List<Margen>) getObjectFromEnabledLayersOfClass(Margen.class);	
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
	public List<Poligono> getPoligonosSeleccionados() {
		return (List<Poligono>) getObjectFromEnabledLayersOfClass(Poligono.class);
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


	public List<?> getObjectFromEnabledLayersOfClass(Class<?> clazz){
		LayerList layers = this.getWwd().getModel().getLayers();
		Stream<Layer> layersOfClazz = layers.stream().filter(l->{
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			return l.isEnabled() && o!=null && clazz.isAssignableFrom(o.getClass());
		});
		return layersOfClazz.map(l->l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR)).collect(Collectors.toList());
	}	

/**
 * 
 * @param clazz
 * @return devuelve los objetos de clase clazz almacenado en los layers cargados
 */
	public List<?> getObjectFromLayersOfClass(Class<?> clazz){
		LayerList layers = this.getWwd().getModel().getLayers();
		Stream<Layer> layersOfClazz = layers.stream().filter(l->{
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			return o!=null && clazz.isAssignableFrom(o.getClass());
		});
		return layersOfClazz.map(l->l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR)).collect(Collectors.toList());
	}
	
/**
 * 
 * @param clazz
 * @return devuelve los layers cuyo object es de clase clazz
 */
	public List<?> getLayersOfClass(Class<?> clazz){
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
		Platform.runLater(() -> {
		
			
			this.poligonoGUIController.showPoligonosActivos();
			this.ndviGUIController.showNdviActivos();

		//Al agregar los poligonos y ndvi se resetea el divider position
	
		//todo enviar mensage al loader de que se termino de cargar la app
			setSplitPaneDividerPosition();
		//stage.show();		
		});
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

//	private void doEditMargin(Margen margen) {		
//		System.out.println(Messages.getString("JFXMain.324")); 
//		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
//		if(!margenConfigured.isPresent()){//
//			System.out.println(Messages.getString("JFXMain.325")); 
//			return;
//		}							
//		OpenMargenMapTask uMmTask = new OpenMargenMapTask(margen);
//		uMmTask.installProgressBar(progressBox);
//		uMmTask.setOnSucceeded(handler -> {
//			this.getLayerPanel().update(this.getWwd());
//			uMmTask.uninstallProgressBar();
//			this.wwjPanel.repaint();
//			System.out.println(Messages.getString("JFXMain.326")); 
//			playSound();
//		});
//		executorPool.execute(uMmTask);
//	}


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
			Platform.runLater(() -> {
				URL url = JFXMain.class.getClassLoader().getResource(SOUND_FILENAME);
				// System.out.println("=== SOUND DEBUG ===");
				// System.out.println("Looking for sound file: " + SOUND_FILENAME);
				// System.out.println("URL found: " + url);
				
				if (url != null) {
					try {
						String uriString = url.toURI().toString();
						// System.out.println("URI string: " + uriString);
						
						AudioClip plonkSound = new AudioClip(uriString);
						// System.out.println("AudioClip created successfully");
						
						this.isPlayingSound = true;
						plonkSound.setVolume(0.50);
						// System.out.println("Volume set to 0.50, attempting to play...");
						
						plonkSound.play();
						// System.out.println("play() method called");
						
						// Reset flag after a short delay to allow sound to finish
						executorPool.execute(() -> {
							try {
								Thread.sleep(2000); // 2 seconds
								this.isPlayingSound = false;
								// System.out.println("Sound playback flag reset");
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								this.isPlayingSound = false;
							}
						});

					} catch (URISyntaxException e) {
						System.out.println("URISyntaxException: " + e.getMessage());
						e.printStackTrace();
					} catch (Exception e) {
						System.out.println("Unexpected error: " + e.getMessage());
						e.printStackTrace();
					}
				}
			});
		}else {
			System.out.println("no reprodusco el sonido porque ya hay un player andando");
		}
	}

	// Test method to debug audio system
	// public void testAudioSystem() {
	// 	System.out.println("=== AUDIO SYSTEM TEST ===");
	// 	System.out.println("JavaFX Platform: " + javafx.application.Platform.isFxApplicationThread());
	// //	System.out.println("JavaFX Toolkit: " + javafx.application.Platform.isToolkitInitialized());
		
	// 	// Test with a simple beep sound
	// 	Platform.runLater(() -> {
	// 		try {
	// 			// Create a simple test sound using JavaFX built-in sound
	// 			AudioClip testSound = new AudioClip("data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIG2m98OScTgwOUarm7blmGgU7k9n1unEiBC13yO/eizEIHWq+8+OWT");
	// 			testSound.setVolume(0.3);
	// 			testSound.play();
	// 			System.out.println("Test beep sound played");
	// 		} catch (Exception e) {
	// 			System.out.println("Test sound failed: " + e.getMessage());
	// 			e.printStackTrace();
	// 		}
	// 	});
	// 	System.out.println("=== END AUDIO SYSTEM TEST ===");
	// }

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
		//LoggingOutputStream.installLogger();//redirige los System.out y System.err a los loggers
		
		try	{			
			//System.setProperty("prism.order", "es2");
			System.setProperty("javafx.preloader", UrsulaGISPreloader.class.getCanonicalName());
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
