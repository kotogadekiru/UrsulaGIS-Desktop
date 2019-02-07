package gui;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.measure.unit.SystemOfUnits;
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
import org.geotools.filter.SortByImpl;
import org.geotools.xml.filter.FilterComplexTypes.SortByType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.sort.SortBy;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Labor;
import dao.LaborItem;
import dao.Ndvi;
import dao.Poligono;
import dao.config.Agroquimico;
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
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import dao.suelo.Suelo;
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
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.UnitsFormat;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwind.util.measure.MeasureToolController;
import gov.nasa.worldwindx.examples.util.ExampleUtil;
import gui.nww.LaborLayer;
import gui.nww.LayerPanel;
import gui.nww.WWPanel;
import gui.utils.DateConverter;
import gui.utils.SmartTableView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
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
import javafx.scene.control.ChoiceDialog;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import tasks.ExportLaborMapTask;
import tasks.GetNDVI2ForLaborTask;
import tasks.GoogleGeocodingHelper;
import tasks.ProcessMapTask;
import tasks.ReadJDHarvestLog;
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
import tasks.procesar.ExtraerPoligonosDeLaborTask;
import tasks.procesar.GenerarMuestreoDirigidoTask;
import tasks.procesar.GrillarCosechasMapTask;
import tasks.procesar.JuntarShapefilesTask;
import tasks.procesar.ProcessMarginMapTask;
import tasks.procesar.ProcessNewSoilMapTask;
import tasks.procesar.RecomendFertNFromHarvestMapTask;
import tasks.procesar.RecomendFertPFromHarvestMapTask;
import tasks.procesar.SiembraFertTask;
import tasks.procesar.UnirCosechasMapTask;
import tasks.procesar.UnirFertilizacionesMapTask;
import utils.DAH;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class JFXMain extends Application {


	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE = "gov.nasa.worldwind.avkey.InitialAltitude"; //$NON-NLS-1$
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE = "gov.nasa.worldwind.avkey.InitialLongitude"; //$NON-NLS-1$
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE = "gov.nasa.worldwind.avkey.InitialLatitude"; //$NON-NLS-1$
	//	private static final double MAX_VALUE = 1.0;
	//	private static final double MIN_VALUE = 0.2;
	public static final String VERSION = "0.2.24 dev"; //$NON-NLS-1$
	private static final String TITLE_VERSION = "Ursula GIS-v"+VERSION; //$NON-NLS-1$
	private static final String BUILD_INFO=Messages.getString("JFXMain.info1") //$NON-NLS-1$
			+Messages.getString("JFXMain.info2") //$NON-NLS-1$
			+Messages.getString("JFXMain.inf3") //$NON-NLS-1$
			+Messages.getString("JFXMain.info3") //$NON-NLS-1$
			+Messages.getString("JFXMain.info4"); //$NON-NLS-1$
	public static final String ICON ="gui/32x32-icon-earth.png";// "gui/1-512.png";//UrsulaGIS-Desktop/src/gui/32x32-icon-earth.png //$NON-NLS-1$
	private static final String SOUND_FILENAME = "gui/Alarm08.wav";//"Alarm08.wav" funciona desde eclipse pero no desde el jar  //$NON-NLS-1$

	private Stage stage=null;
	private Scene scene=null;
	
	private static Configuracion config = Configuracion.getInstance();

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

	public static ExecutorService executorPool = Executors.newCachedThreadPool();
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
				DAH.closeEm();
				System.out.println("em Closed"); //$NON-NLS-1$
				System.out.println("Application Closed by click to Close Button(X)"); //$NON-NLS-1$
				getWwd().shutdown();
				System.exit(0); 
			});
		});
		primaryStage.show();
		
		//start clearCache cronJob
		startClearCacheCronJob();
	}

	private void startClearCacheCronJob() {
		  ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		  Runnable clearCaches =() ->{
			 cosechas.forEach(l->{
				 LocalTime now = LocalTime.now().minusSeconds(60);
				 
				 synchronized(l){
					 if(l.cacheLastRead.isBefore(now)) {
						 l.clearCache();
					 }
				 }
			 });
			 fertilizaciones.forEach(l->l.clearCache());
			 siembras.forEach(l->l.clearCache());
			 pulverizaciones.forEach(l->l.clearCache());
			 suelos.forEach(l->l.clearCache());
			 margenes.forEach(l->l.clearCache());
			// System.out.println("termine de limpiar todas las caches");
		  };

		  ScheduledFuture<?> clearCachesHandle =  scheduler.scheduleAtFixedRate(clearCaches, 60, 30, TimeUnit.SECONDS);
		  //scheduler.schedule(()->  { clearCachesHandle.cancel(true);  }, 60 * 60, TimeUnit.SECONDS);//stop clearing caches after an hour
		
	}
		
	private static void setInitialPosition() {
		//Configuracion config =Configuracion.getInstance();
		double initLat = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, "-35")); //$NON-NLS-1$
		double initLong = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE, "-62")); //$NON-NLS-1$
		double initAltitude = Double.parseDouble(config.getPropertyOrDefault(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, "19.07e5")); //$NON-NLS-1$
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
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");//UIManager.getSystemLookAndFeelClassName()); //$NON-NLS-1$
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
		sp.setDividerPositions(0.15f);//15% de la pantalla
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
							String message = "Computer does not meet minimum graphics requirements.\n"; //$NON-NLS-1$
							message += "Please install up-to-date graphics driver and try again.\n"; //$NON-NLS-1$
							message += "Reason: " + t.getMessage() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
							message += "This program will end when you press OK."; //$NON-NLS-1$
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

		// descomentar esto para cargar los poligonos de la base de datos. bloquea la interface
		executorPool.execute(()->loadPoligonos());

		return sp;
	}

	public static void setDefaultSize(int size) {
		Set<Object> keySet = UIManager.getLookAndFeelDefaults().keySet();
		Object[] keys = keySet.toArray(new Object[keySet.size()]);

		for (Object key : keys) {
			if (key != null && key.toString().toLowerCase().contains("font")) { //$NON-NLS-1$
				//System.out.println(key);
				Font font = UIManager.getDefaults().getFont(key);
				if (font != null) {
					font = font.deriveFont((float)size);
					UIManager.put(key, font);
				}
			}
		}
	}

	private MenuBar constructMenuBar() {
		/*Menu Importar*/
		final Menu menuImportar = new Menu(Messages.getString("JFXMain.importar")); //$NON-NLS-1$
		//addMenuItem("Suelo",(a)->doOpenSoilMap(),menuImportar);		
		addMenuItem(Messages.getString("JFXMain.fertilizacion"),(a)->doOpenFertMap(null),menuImportar); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.siembra"),(a)->doOpenSiembraMap(null),menuImportar); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.pulverizacion"),(a)->doOpenPulvMap(null),menuImportar); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.cosecha"),(a)->doOpenCosecha(null),menuImportar);		 //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.NDVI"),(a)->doOpenNDVITiffFiles(),menuImportar); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.imagen"),(a)->importImagery(),menuImportar); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.suelo"),(a)->doOpenSoilMap(null),menuImportar); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.margen"),(a)->doOpenMarginlMap(),menuImportar); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.poligonos"),(a)->doImportarPoligonos(null),menuImportar); //$NON-NLS-1$

		final Menu menuHerramientas = new Menu(Messages.getString("JFXMain.herramientas")); //$NON-NLS-1$
		//addMenuItem("CosechaJD",(a)->doLeerCosechaJD(),menuHerramientas);
		//insertMenuItem(menuCalcular,"Retabilidades",a->doProcessMargin());
		addMenuItem(Messages.getString("JFXMain.distancia"),(a)->doMedirDistancia(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.superficie"),(a)->doMedirSuperficie(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.unirShapes"),(a)->JuntarShapefilesTask.process(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.rentabilidad"),(a)->doProcessMargin(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.unirCosechas"),(a)->doUnirCosechas(null),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.unirFertilizaciones"),(a)->doUnirFertilizaciones(null),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.balanceNutrientes"),(a)->doProcesarBalanceNutrientes(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.generarSiembraFert"),(a)->doGenerarSiembraFertilizada(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.generarOrdenCompra"),(a)->doGenerarOrdenDeCompra(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.goTo"),(a)->{ //$NON-NLS-1$
			TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.goToExample")); //$NON-NLS-1$
			anchoDialog.setTitle(Messages.getString("JFXMain.goToDialogTitle")); //$NON-NLS-1$
			anchoDialog.setHeaderText(Messages.getString("JFXMain.goToDialogHeader")); //$NON-NLS-1$
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


		},menuHerramientas);
		addMenuItem(Messages.getString("JFXMain.evoNDVI"),(a)->{ //$NON-NLS-1$
			doShowNDVIEvolution();
		},menuHerramientas);
		addMenuItem(Messages.getString("JFXMain.unirPoligonos"),(a)->doUnirPoligonos(),menuHerramientas); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.intersectarPoligonos"),(a)->doIntersectarPoligonos(),menuHerramientas); //$NON-NLS-1$

		/*Menu Exportar*/
		final Menu menuExportar = new Menu(Messages.getString("JFXMain.exportar"));		 //$NON-NLS-1$
		//	addMenuItem("Suelo",(a)->doExportSuelo(),menuExportar);
		addMenuItem(Messages.getString("JFXMain.exportarPantallaMenuItem"),(a)->doSnapshot(),menuExportar); //$NON-NLS-1$

		/*Menu Configuracion*/
		final Menu menuConfiguracion = new Menu(Messages.getString("JFXMain.configuracionMenu")); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.cultivosMenuItem"),(a)->doConfigCultivo(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.fertilizantesMenuItem"),(a)->doConfigFertilizantes(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.agroquimicosMenuItem"),(a)->doConfigAgroquimicos(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configSemillasMenuItem"),(a)->doConfigSemillas(),menuConfiguracion); //$NON-NLS-1$

		addMenuItem(Messages.getString("JFXMain.configEmpresaMI"),(a)->doConfigEmpresa(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configEstablecimientoMI"),(a)->doConfigEstablecimiento(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configLoteMi"),(a)->doConfigLote(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configCampaniaMI"),(a)->doConfigCampania(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configPoligonosMI"),(a)->doConfigPoligonos(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configNDVIMI"),(a)->doShowNdviTable(),menuConfiguracion); //$NON-NLS-1$
		//addMenuItem("Labores",(a)->doShowLaboresTable(),menuConfiguracion);

		addMenuItem(Messages.getString("JFXMain.configIdiomaMI"),(a)->doChangeLocale(),menuConfiguracion); //$NON-NLS-1$


		MenuItem actualizarMI=addMenuItem(Messages.getString("JFXMain.configUpdate"),null,menuConfiguracion); //$NON-NLS-1$
		actualizarMI.setOnAction((a)->doUpdate(actualizarMI));
		actualizarMI.setVisible(false);

		executorPool.submit(()->{
			if(UpdateTask.isUpdateAvailable()){
				actualizarMI.setVisible(true);
				//actualizarMI.getStyleClass().clear();

				actualizarMI.getStyleClass().add("menu-item:focused"); //$NON-NLS-1$
				actualizarMI.setStyle("-fx-background: -fx-accent;" //$NON-NLS-1$
						+"-fx-background-color: -fx-selection-bar;" //$NON-NLS-1$
						+ "-fx-text-fill: -fx-selection-bar-text;" //$NON-NLS-1$
						+ "fx-text-fill: white;"); //$NON-NLS-1$
				//menuConfiguracion.show();
				menuConfiguracion.setStyle("-fx-background: -fx-accent;" //$NON-NLS-1$
						+"-fx-background-color: -fx-selection-bar;" //$NON-NLS-1$
						+ "-fx-text-fill: -fx-selection-bar-text;" //$NON-NLS-1$
						+ "fx-text-fill: white;"); //$NON-NLS-1$

			}
		});

		addMenuItem(Messages.getString("JFXMain.configHelpMI"),(a)->doShowAcercaDe(),menuConfiguracion); //$NON-NLS-1$

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuImportar,menuHerramientas, menuExportar,menuConfiguracion);
		menuBar.setPrefWidth(scene.getWidth());
		return menuBar;
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

		List<Function<Layer, String>> suelosP = new ArrayList<Function<Layer,String>>();
		predicates.put(Suelo.class, suelosP);

		List<Function<Layer, String>> poligonosP = new ArrayList<Function<Layer,String>>();
		predicates.put(Poligono.class, poligonosP);

		List<Function<Layer, String>> laboresP = new ArrayList<Function<Layer,String>>();
		predicates.put(Labor.class, laboresP);
		List<Function<Layer, String>> todosP = new ArrayList<Function<Layer,String>>();
		predicates.put(Object.class, todosP);

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.editarLayer");  //$NON-NLS-1$
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					//mostrar un dialogo para editar el nombre del poligono
					Poligono p =(Poligono)layerObject;
					TextInputDialog nombreDialog = new TextInputDialog(p.getNombre());
					nombreDialog.setTitle(Messages.getString("JFXMain.editarLayerDialogTitle")); //$NON-NLS-1$
					nombreDialog.setContentText(Messages.getString("JFXMain.editarLayerPoligonName")); //$NON-NLS-1$

					Optional<String> nombreOptional = nombreDialog.showAndWait();
					if(nombreOptional.isPresent()){
						p.setNombre(nombreOptional.get());
						this.getLayerPanel().update(this.getWwd());
					}
				}
				return "converti a Siembra"; //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return "Convertir a Siembra";  //$NON-NLS-1$
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					//doConvertirASiembra((Polygon) layerObject);
					doCrearSiembra((Poligono) layerObject);
				}
				return "converti a Siembra"; //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.poligonToFertAction");  //$NON-NLS-1$
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){	
					doCrearFertilizacion((Poligono) layerObject);
				}
				return "converti a Fertilizacion"; //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.poligonToPulvAction");  //$NON-NLS-1$
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doCrearPulverizacion((Poligono) layerObject);
				}
				return "converti a Pulverizacion"; //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.poligonToHarvestAction");  //$NON-NLS-1$
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doCrearCosecha((Poligono) layerObject);
				}
				return "converti a Cosecha"; //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.poligonToSoilAction");  //$NON-NLS-1$
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doCrearSuelo((Poligono) layerObject);
					layer.setEnabled(false);
					//TODO deshabilitar layer para que el polygono no rompa la cosecha nueva
				}
				return "converti a Cosecha"; //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.saveAction");  //$NON-NLS-1$
			} else {
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
					doGuardarPoligono((Poligono) layerObject);
				}
				return "Guarde poligono"; //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.goToPoligonoAction");  //$NON-NLS-1$
			} else{
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (layerObject==null){
				}else if(Poligono.class.isAssignableFrom(layerObject.getClass())){
					Poligono poli = (Poligono)layerObject;
					Position pos =poli.getPositions().get(0);
					viewGoTo(pos);
				}
				return "went to " + layer.getName(); //$NON-NLS-1$
			}});

		poligonosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.downloadNDVIAction");  //$NON-NLS-1$
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
				if(o instanceof Poligono){
					doGetNdviTiffFile(o);
				}
				return "ndvi obtenido" + layer.getName();	 //$NON-NLS-1$
			}});

		laboresP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.goToLayerAction");  //$NON-NLS-1$
			} else{
				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (layerObject==null){
				}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
					viewGoTo((Labor<?>) layerObject);
				}
				return "went to " + layer.getName(); //$NON-NLS-1$
			}});
//		laboresP.add((layer)->{
//
//			if(layer==null){
//				return "Guardar"; 
//			} else {		
//				enDesarrollo();
//				Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//				if (layerObject==null){
//				}else if(Labor.class.isAssignableFrom(layerObject.getClass())){
//					doGuardarLabor((Labor<?>) layerObject);
//				}
//				return "guarde labor " + layer.getName();
//			}});

		/**
		 *Accion que permite editar un mapa de rentabilidad
		 */
		margenesP.add((layer)->{ 
			if(layer==null){
				return Messages.getString("JFXMain.editMargenAction");  //$NON-NLS-1$
			} else{
				doEditMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "margen editada" + layer.getName(); //$NON-NLS-1$

			}});


		/**
		 *Accion que permite editar una siembra
		 */
		pulverizacionesP.add((layer)-> {
			if(layer==null){
				return Messages.getString("JFXMain.editPulvAction");  //$NON-NLS-1$
			} else{
				doEditPulverizacion((PulverizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "pulverizacion editada" + layer.getName(); //$NON-NLS-1$
			}});


		/**
		 *Accion que permite editar una siembra
		 */
		siembrasP.add((layer)-> {
			if(layer==null){
				return Messages.getString("JFXMain.editSiembraAction");  //$NON-NLS-1$
			} else{
				doEditSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "siembra editada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 *Accion que permite editar una cosecha
		 */
		fertilizacionesP.add((layer)-> {
			if(layer==null){
				return Messages.getString("JFXMain.editFertAction");  //$NON-NLS-1$
			} else{
				doEditFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "fertilizacion editada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 *Accion que permite editar una cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.editCosechaAction");  //$NON-NLS-1$
			} else{
				doEditCosecha((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha editada" + layer.getName(); //$NON-NLS-1$

			}});

		suelosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.editSoilAction");  //$NON-NLS-1$
			} else{
				doEditSuelo((Suelo) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "suelo editado" + layer.getName(); //$NON-NLS-1$

			}});

		/**
		 * Accion que permite pasar una grilla sobre la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.grillarCosechaAction");  //$NON-NLS-1$
			} else{
				doGrillarCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha editada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion que permite pasar una grilla sobre la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.clonarCosechaAction");  //$NON-NLS-1$
			} else{
				doUnirCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha clonada" + layer.getName(); //$NON-NLS-1$
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
				return Messages.getString("JFXMain.showHeightVsAmountChart");  //$NON-NLS-1$
			} else{
				showAmountVsElevacionChart((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "grafico mostrado " + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion que permite generar un muestreo dirigido para los poligonos de la cosecha
		 */
		cosechasP.add((layer)-> {
			if(layer==null){
				return Messages.getString("JFXMain.generarMuestreoDirigido");  //$NON-NLS-1$
			} else{

				doGenerarMuestreoDirigido((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "muestreo dirigido " + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion que permite extraer los poligonos de una cosecha para guardar
		 */
		laboresP.add((layer)-> {
			if(layer==null){
				return Messages.getString("JFXMain.extraerPoligonoAction");  //$NON-NLS-1$
			} else{
				doExtraerPoligonos((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "poligonos Extraidos " + layer.getName(); //$NON-NLS-1$
			}});


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
				return Messages.getString("JFXMain.exportLaborAction");  //$NON-NLS-1$
			} else{
				doExportLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "labor Exportada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion permite exportar la labor como shp
		 */
		fertilizacionesP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.exportarFertPAction");  //$NON-NLS-1$
			} else{
				doExportPrescripcion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "labor Exportada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion permite exportar la labor como shp
		 */
		siembrasP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.exportarSiembraAction");  //$NON-NLS-1$
			} else{
				doExportPrescripcionSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "labor Exportada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion permite crear una fertilizacion P para reponer lo extraido por la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.recompendarFertP");  //$NON-NLS-1$
			} else{
				doRecomendFertPFromHarvest((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "Fertilizacion P Creada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion permite crear una fertilizacion P para reponer lo extraido por la cosecha
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.recomendarFertN");  //$NON-NLS-1$
			} else{
				doRecomendFertNFromHarvest((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "Fertilizacion N Creada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion muestra una tabla con los datos de la cosecha
		 */
		laboresP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.showTableLayerAction");  //$NON-NLS-1$
			} else{
				doShowDataTable((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "Tabla mostrada" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion permite exportar la cosecha como shp de puntos
		 */
		cosechasP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.exportarCosechaAPuntosAction");  //$NON-NLS-1$
			} else{
				doExportHarvestDePuntos((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
				return "cosecha exportada como puntos: " + layer.getName(); //$NON-NLS-1$
			}});

		ndviP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.convertirNDVIaCosechaAction");  //$NON-NLS-1$
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(o instanceof Ndvi){
					doConvertirNdviACosecha((Ndvi) o);
				}

				return "rinde estimado desde ndvi" + layer.getName(); //$NON-NLS-1$
			}});

		ndviP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.mostrarNDVIChartAction");  //$NON-NLS-1$
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(o instanceof Ndvi){
					showHistoNDVI((Ndvi)o);
				}

				return "histograma ndvi mostrado" + layer.getName(); //$NON-NLS-1$
			}});

		ndviP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.goToNDVIAction");  //$NON-NLS-1$
			} else{
				Object zoomPosition = layer.getValue(ProcessMapTask.ZOOM_TO_KEY);		

				//Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if (zoomPosition==null){
				}else if(zoomPosition instanceof Position){
					Position pos =(Position)zoomPosition;
					viewGoTo(pos);
				}
				return "went to " + layer.getName(); //$NON-NLS-1$
			}});

		ndviP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.guardarNDVIAction");  //$NON-NLS-1$
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(o instanceof Ndvi){
					Ndvi ndvi = (Ndvi)o;
					ndvi.updateContent();
					DAH.save(ndvi);
				}

				return "guarde" + layer.getName(); //$NON-NLS-1$
			}});

		/*
		 * funcionalidad que permite guardar el archivo tiff de este ndvi en una ubicacion definida por el usuario
		 */
		ndviP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.exportarNDVItoTIFFAction");  //$NON-NLS-1$
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(o instanceof Ndvi){
					Ndvi ndvi = (Ndvi)o;
					doExportarTiffFile(ndvi);
				}

				return "guarde" + layer.getName(); //$NON-NLS-1$
			}});

		/**
		 * Accion permite obtener ndvi
		 */
		laboresP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.downloadNDVI");  //$NON-NLS-1$
			} else{
				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
				if(o instanceof Labor){
					doGetNdviTiffFile(o);
				}
				return "ndvi obtenido" + layer.getName();	 //$NON-NLS-1$
			}});



		//		poligonosP.add((layer)->{
		//			if(layer==null){
		//				return "Obtener NDVI2"; 
		//			} else{
		//				Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
		//				if(o instanceof Poligono){
		//					doGetNdviTiffFile(o);
		//				}
		//				return "ndwi obtenido" + layer.getName();	
		//			}});


		/**
		 * Accion que permite eliminar una cosecha
		 */
		todosP.add((layer)->{
			if(layer==null){
				return Messages.getString("JFXMain.removeLayerAction");  //$NON-NLS-1$
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
				layer.dispose();
				getLayerPanel().update(getWwd());
				return "layer removido" + layer.getName(); //$NON-NLS-1$
			}});


		layerPanel.setMenuItems(predicates);
	}

	private void doExportarTiffFile(Ndvi ndvi) {
		ndvi.updateContent();

		File tiffFile = ndvi.getF();
		File dir =getNewTiffFile(tiffFile.getName());
		try{
			InputStream in = new FileInputStream(tiffFile);
			OutputStream out = new FileOutputStream(dir);

			// Copy the bits from instream to outstream
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}











	private void enDesarrollo() {
		Alert enDesarrollo = new Alert(AlertType.INFORMATION,Messages.getString("JFXMain.workInProgressAction")); //$NON-NLS-1$
		enDesarrollo.showAndWait();
	}


	private String applyHistogramaCosecha(Layer layer){
		if(layer==null){
			return Messages.getString("JFXMain.showHistogramaLaborAction");  //$NON-NLS-1$
		} else{
			showHistoLabor((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "histograma mostrado" + layer.getName(); //$NON-NLS-1$
		}
	}

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
		System.setProperty("java.net.useSystemProxies", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		if (Configuration.isMacOS()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name", //$NON-NLS-1$
					"World Wind Application"); //$NON-NLS-1$
			System.setProperty("com.apple.mrj.application.growbox.intrudes", //$NON-NLS-1$
					"false"); //$NON-NLS-1$
			System.setProperty("apple.awt.brushMetalLook", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (Configuration.isWindowsOS()) {
			System.setProperty("sun.awt.noerasebackground", "true");  //$NON-NLS-1$ //$NON-NLS-2$
			// prevents
			// flashing
			// during
			// window
			// resizing
		}
	}


	private void doLeerCosechaJD() {
		//TODO empezar por el fdl y leer el nombre del archivo y el formato del archivo fdd
		/**
		 <LogDataBlock logDataProcessor="EvenByteDelta">
			<EvenByteDelta filePathName="50b2706b-0000-1000-7fdb-e1e1e114c450.fdd"
				xmlns="urn:schemas-johndeere-com:LogDataBlock:EvenByteDelta"
				filePosition="0" />
		</LogDataBlock>
		 */
		List<File> files =chooseFiles("FDL", "*.fdl"); //$NON-NLS-1$ //$NON-NLS-2$
		ReadJDHarvestLog task = new ReadJDHarvestLog(files.get(0));
		task.installProgressBar(progressBox);

		task.setOnSucceeded(handler -> {
			//File ret = (File)handler.getSource().getValue();

			task.uninstallProgressBar();


			System.out.println("ReadJDHarvestLog succeded"); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();					
		//this.executorPool.execute(task);

		executorPool.submit(task);
	}

	/**
	 * metodo que toma las labores activas de siembra fertilizacion y pulverizacion y hace una lista con los insumos y cantidades para
	 * cotizar precios online. Permite exporta a excel y cotizar precios online y guardar
	 */
	private void doGenerarOrdenDeCompra() {
		enDesarrollo();
		Alert message = new Alert(Alert.AlertType.INFORMATION);
		message.setHeaderText(Messages.getString("JFXMain.generarOrdenCompraAction")); //$NON-NLS-1$
		message.setContentText(Messages.getString("JFXMain.gOC1") //$NON-NLS-1$
				+ Messages.getString("JFXMain.gOC2") //$NON-NLS-1$
				+ Messages.getString("JFXMain.gOC3")); //$NON-NLS-1$
		message.show();
		//TODO implementar este metodo


	}

	private void doGenerarSiembraFertilizada() {
		enDesarrollo();
		Alert message = new Alert(Alert.AlertType.INFORMATION);
		message.setHeaderText(Messages.getString("JFXMain.generarSiembraFertAction")); //$NON-NLS-1$
		message.setContentText(Messages.getString("JFXMain.generarSiembraFertText")  //$NON-NLS-1$
				+ Messages.getString("JFXMain.generarSiembraFertText2")); //$NON-NLS-1$
		message.show();

		//todo pasarlabor el filtrado por visibles aca y pasar nuevas listas solo con las visibles
		SiembraLabor siembraEnabled = siembras.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList()).get(0);
		FertilizacionLabor fertEnabled = fertilizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList()).get(0);
		//List<SiembraLabor> siemEnabled = siembras.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		//List<CosechaLabor> cosechasEnabled = cosechas.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());			


		SiembraFertTask siembraFertTask = new SiembraFertTask(siembraEnabled, fertEnabled);

		siembraFertTask.installProgressBar(progressBox);

		siembraFertTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			siembraFertTask.uninstallProgressBar();

			this.siembras.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println("SiembraFertTask succeded"); //$NON-NLS-1$
		});
		executorPool.execute(siembraFertTask);


	}



	private void doShowNDVIEvolution() {
		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
		//	executorPool.execute(()->{
		List<SurfaceImageLayer> ndviLayers = new ArrayList<SurfaceImageLayer>();
		LayerList layers = getWwd().getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Ndvi){
				//l.setEnabled(false);
				ndviLayers.add((SurfaceImageLayer) l);
			}
		}	

		DateFormat df =new  SimpleDateFormat("dd-MM-yyyy"); //$NON-NLS-1$
		ndviLayers.sort((c1,c2)->{
			String l1Name =c1.getName();
			String l2Name =c2.getName();
			Ndvi ndvi1 = (Ndvi)c1.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			Ndvi ndvi2 = (Ndvi)c2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			try{
				return ndvi1.getFecha().compareTo(ndvi2.getFecha());
			} catch(Exception e){
				System.err.println("no se pudo comparar las fechas de los ndvi. comparando nombres"); //$NON-NLS-1$
			}
			// comparar por el valor del layer en vez del nombre del layer
			try{
				Date d1 = df.parse(l1Name.substring(l1Name.length()-"dd-MM-yyyy".length())); //$NON-NLS-1$
				Date d2 = df.parse(l2Name.substring(l2Name.length()-"dd-MM-yyyy".length())); //$NON-NLS-1$
				return d1.compareTo(d2);
			} catch(Exception e){
				//no se pudo parsear como fecha entonces lo interpreto como string.
				//e.printStackTrace();
			}
			return l1Name.compareTo(l2Name);
		});


		//junto los ndvi segun fecha para hacer la evolucion correctamente.
		Map<Date, List<SurfaceImageLayer>>  fechaMap = ndviLayers.stream().collect(
				Collectors.groupingBy((l2)->{
					Ndvi lNdvi = (Ndvi)l2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					return lNdvi.getFecha().getTime();
				}));

		List<Date> dates= fechaMap.keySet().stream().distinct().sorted().collect(Collectors.toList());

		Timeline timeline = new Timeline();
		timeline.setCycleCount(1);
		timeline.setAutoReverse(false);	

		double s =0;

		KeyFrame startKF = new KeyFrame(Duration.seconds(s),(t)->{
			//System.out.println("cambiando opacity a "+n);
			for(SurfaceImageLayer l: ndviLayers){					
				//l.setOpacity(n.doubleValue());				
				l.setEnabled(false);	
				//l.setOpacity(0);
			}
			this.layerPanel.update(getWwd());
			getWwd().redraw();		
			//System.out.println("termine de apagar los layers");
		});
		timeline.getKeyFrames().add(startKF);
		//s+=0.1;//wait

		for(Date date:dates){

			ObjectProperty<Date> antesP = new SimpleObjectProperty<Date>(null);
			int dIndex = dates.indexOf(date);
			if(dIndex>0){
				antesP.set(dates.get(dIndex-1));
			}
			if(antesP.get()==null){		
				//s+=1;
				//System.out.println("agregando antesKeyFrame con s="+s);
				KeyFrame antesKeyFrame = new KeyFrame(Duration.seconds(s),(t)->{				
					List<SurfaceImageLayer> layerList = fechaMap.get(date);
					for(SurfaceImageLayer l: layerList){					
						//l.setOpacity(1);
						if(!l.isEnabled()){
							l.setEnabled(true);
						}
					}
					Platform.runLater(()->{
						this.layerPanel.update(getWwd());
						getWwd().redraw();
					});
				});
				timeline.getKeyFrames().add(antesKeyFrame);	
			}else{			
				List<SurfaceImageLayer> layerList = fechaMap.get(date);
				List<SurfaceImageLayer> antesLayerList = fechaMap.get(antesP.get());
				long days=(date.getTime()-antesP.get().getTime())/(1000*60*60*24);
				//System.out.println("days= "+days);
				double avanceDia =0.2;//0.2seg por dia ->2 seg por imagen
				double totalTime = days*avanceDia;			
				//System.out.println("agregando dateFrame con s="+s);

				s+=totalTime;		//1						
				timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(s),(t)->{				
					for(SurfaceImageLayer l: layerList){
						l.setEnabled(true);
						//l.setOpacity(0.2);					
					}
					for(Layer l: antesLayerList){	
						l.setEnabled(false);
						//l.setOpacity(0.8);
					}
					Platform.runLater(()->{
						this.layerPanel.update(getWwd());
						getWwd().redraw();
					});
				}));		

			}
		}
		s+=4;
		//System.out.println("agregando endKF con s="+s);
		KeyFrame endKF = new KeyFrame(Duration.seconds(s),(t)->{
			for(Layer l: ndviLayers){
				l.setEnabled(true);
			}
			this.layerPanel.update(getWwd());
			getWwd().redraw();
			//System.out.println("termine de habilitar todos los layers endKF "+System.currentTimeMillis());
		});
		timeline.getKeyFrames().add(endKF);
		timeline.play();
	}

	private void doUpdate(MenuItem actualizarMI) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setContentText(Messages.getString("JFXMain.doUpdateText")); //$NON-NLS-1$
		alert.initOwner(this.stage);
		alert.showAndWait();
		if(ButtonType.OK.equals(alert.getResult())){
			UpdateTask uTask = new UpdateTask();
			uTask.installProgressBar(progressBox);
			uTask.setOnSucceeded(handler -> {
				File newVersion = (File) handler.getSource().getValue();	
				if(newVersion==null){
					Alert error = new Alert(Alert.AlertType.ERROR);
					error.setContentText(Messages.getString("JFXMain.doUpdateErrorText")); //$NON-NLS-1$
					error.initOwner(this.stage);
					error.showAndWait();
				} else{
					Alert error = new Alert(Alert.AlertType.CONFIRMATION);
					error.setContentText(Messages.getString("JFXMain.doUpdateSuccessText")); //$NON-NLS-1$
					error.initOwner(this.stage);
					error.showAndWait();

					//					playSound();	
					//					actualizarMI.getParentMenu().setStyle("");
					//					actualizarMI.setVisible(false);
					//borrar estilos del menuConfiguracion

					// cerrar esta app y abrir la nueva?
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
		List<Poligono> poligonos = DAH.getPoligonosActivos();
		showPoligonos(poligonos);

		List<Ndvi> ndviActivos = DAH.getNdviActivos();
		for(Ndvi ndvi : ndviActivos){
			//System.out.println("mostrando ndvi activo");
			doShowNDVI(ndvi);
		}
	}

	private void showPoligonos(List<Poligono> poligonos) {
		for(Poligono poli : poligonos){
			MeasureTool measureTool = PoligonLayerFactory.createPoligonLayer(poli, this.getWwd(), this.getLayerPanel());
			Platform.runLater(()->{
				insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
				this.getLayerPanel().update(this.getWwd());
			});
		}
	}


	private void doMedirSuperficie(){
		Poligono poli = new Poligono();
		MeasureTool measureTool = PoligonLayerFactory.createPoligonLayer(poli, this.getWwd(), this.getLayerPanel());
		measureTool.setArmed(true);

		insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
		this.getLayerPanel().update(this.getWwd());


		Alert supDialog = new Alert(Alert.AlertType.INFORMATION);
		supDialog.initOwner(this.stage);
		supDialog.setTitle(Messages.getString("JFXMain.medirSuperficieDielogTitle")); //$NON-NLS-1$
		supDialog.setHeaderText(Messages.getString("JFXMain.medirSuperficieHeaderText")); //$NON-NLS-1$

		Text t = new Text();
		TextField nombreTF = new TextField();
		nombreTF.setPromptText(Messages.getString("JFXMain.medirSuperficieNombreLabel")); //$NON-NLS-1$
		VBox vb = new VBox();
		vb.getChildren().addAll(nombreTF,t);
		supDialog.setGraphic(vb);
		supDialog.initModality(Modality.NONE);
		nombreTF.textProperty().addListener((obj,old,n)->{
			poli.setNombre(n);
			// es importante para que se modifique el layerPanel con el nombre actualizado
			this.getLayerPanel().update(this.getWwd());
		});

		supDialog.show();
		supDialog.setOnHidden((event)->{			
			measureTool.setArmed(false);
		});
	}

	/**
	 * metodo que toma los poligonos seleccionados y los une si se intersectan
	 */
	private void doUnirPoligonos(){
		List<Geometry> geometriasActivas = new ArrayList<Geometry>();
		//1 obtener los poligonos activos
		LayerList layers = this.getWwd().getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Poligono){
				Poligono p = (Poligono)o;
				geometriasActivas.add(p.toGeometry());
			}
		}

		GeometryFactory fact = new GeometryFactory();
		Geometry[] geomArray = new Geometry[geometriasActivas.size()];
		for(int i=0;i<geometriasActivas.size();i++){
			geomArray[i]=geometriasActivas.get(i);
		}
		GeometryCollection collection = fact.createGeometryCollection(geomArray);
		Geometry union = collection.buffer(0);//ProyectionConstants.metersToLongLat(20));
		double has = ProyectionConstants.A_HAS(union.getArea());
		Poligono poli = ExtraerPoligonosDeLaborTask.geometryToPoligono(union);
		MeasureTool measureTool = PoligonLayerFactory.createPoligonLayer(poli, this.getWwd(), this.getLayerPanel());
		poli.setArea(has);
		poli.setNombre(Messages.getString("JFXMain.poligonUnionNamePrefixText")); //$NON-NLS-1$
		insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
		this.getLayerPanel().update(this.getWwd());		
	}

	/**
	 * metodo que toma los poligonos seleccionados calcula la inteseccion y agrega
	 * los poligonos intesectados
	 * a int b
	 * a - (a int b)
	 * b - (a int b)
	 * 
	 */
	private void doIntersectarPoligonos(){
		executorPool.submit(()->{
			try {
				List<Geometry> geometriasActivas = new ArrayList<Geometry>();
				//1 obtener los poligonos activos
				LayerList layers = this.getWwd().getModel().getLayers();
				for (Layer l : layers) {
					Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					if (l.isEnabled() && o instanceof Poligono){
						Poligono p = (Poligono)o;
						geometriasActivas.add(p.toGeometry());
					}
				}
				List<Geometry> geometriasOutput = new ArrayList<Geometry>();
				for(int i=0;i<geometriasActivas.size()-1;i++){
					Geometry a = geometriasActivas.get(i);
					for(int j = i+1;j<geometriasActivas.size();j++){
						Geometry b =geometriasActivas.get(j);
						try {geometriasOutput.add(a.difference(b));}catch(Exception e) {}//found non-noded intersection between LINESTRING (
						try {geometriasOutput.add(b.difference(a));}catch(Exception e) {}
						try {geometriasOutput.add(a.intersection(b));}catch(Exception e) {}						
					}
				}

				int num=0;
				for(Geometry g:geometriasOutput){
					for(int n=0;n<g.getNumGeometries();n++){
						Geometry gn=g.getGeometryN(n);
						if(g.getNumGeometries()==0)continue;
						Poligono poli = ExtraerPoligonosDeLaborTask.geometryToPoligono(gn);
						if(poli ==null)continue;
						MeasureTool measureTool = PoligonLayerFactory.createPoligonLayer(poli, this.getWwd(), this.getLayerPanel());
						double has = ProyectionConstants.A_HAS(gn.getArea());
						poli.setArea(has);
						poli.setNombre(Messages.getString("JFXMain.poligonIntersectionNamePrefix")+num+"["+n+"]");num++; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
					}
				}

				this.getLayerPanel().update(this.getWwd());
			}catch(Exception e) {
				System.err.println("Error al intesectar los poligonos"); //$NON-NLS-1$
				e.printStackTrace();
			}
		});
	}

	private void doMedirDistancia() {		 
		RenderableLayer layer = new RenderableLayer();
		layer.setName(Messages.getString("JFXMain.medirDistanciaLayerName")); //$NON-NLS-1$
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
		distanciaDialog.setTitle(Messages.getString("JFXMain.medirDistancia")); //$NON-NLS-1$
		distanciaDialog.setHeaderText(Messages.getString("JFXMain.medirDistanciaHeaderText")); //$NON-NLS-1$
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
			else if(event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REPLACE) ||
					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_ADD) ||
					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE)){
				double	value = measureTool.getLength();
				DecimalFormat dc = new DecimalFormat("0.00"); //$NON-NLS-1$
				dc.setGroupingSize(3);
				dc.setGroupingUsed(true);
				if(value != valueProperty.doubleValue() && value > 0){
					String formated = dc.format(value)+Messages.getString("JFXMain.metrosAbrevSufix"); //$NON-NLS-1$
					t.textProperty().set(formated);
					measureTool.getLayer().setName(formated);
					measureTool.getLayer().setValue(Labor.LABOR_LAYER_IDENTIFICATOR, Messages.getString("JFXMain.medirLayerTypeName")); //$NON-NLS-1$
					valueProperty.setValue(value);
					this.getLayerPanel().update(this.getWwd());
				}                	                  
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
			System.err.println("fallo hacer zoom a la cosecha nueva"); //$NON-NLS-1$
			e.printStackTrace();
		}
	}
	public void viewGoTo(Position position) {
		if(position==null) return;
		//Configuracion config =Configuracion.getInstance();
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE, String.valueOf(position.getLatitude().degrees));
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE,String.valueOf(position.getLongitude().degrees));
		config.setProperty(GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE, "64000"); //$NON-NLS-1$
		config.save();
		View view =getWwd().getView();
		view.goTo(position, 3000d);
	}

	private void showAmountVsElevacionChart(Labor<?> cosechaLabor) {
		TextInputDialog anchoDialog = new TextInputDialog("20"); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.heightVsAmountDialogTitle")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.heightVsAmountDialogMaxGroupsText")); //$NON-NLS-1$
		anchoDialog.initOwner(this.stage);
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
					System.out.println("no hay ninguna labor para mostrar"); //$NON-NLS-1$
					System.out.print(t.getMessage());
					return null;
				}
			}			
		};


		pfMapTask.setOnSucceeded(handler -> {
			AmountVsElevacionChart	histoChart = (AmountVsElevacionChart) handler.getSource().getValue();	
			if(histoChart!=null){
				Stage histoStage = new Stage();
				histoStage.setTitle(Messages.getString("JFXMain.heightVsAmountChartTitle")); //$NON-NLS-1$
				histoStage.getIcons().add(new Image(ICON));

				Scene scene = new Scene(histoChart, 800,450);
				histoStage.setScene(scene);
				System.out.println("termine de crear el grafico rinde vs altura"); //$NON-NLS-1$
				histoStage.initOwner(this.stage);
				histoStage.show();
				System.out.println("histoChart.show();"); //$NON-NLS-1$
			}else{
				Alert error = new Alert(AlertType.ERROR);
				error.setTitle(Messages.getString("JFXMain.heightVsAmountErrorTitle")); //$NON-NLS-1$
				error.setContentText(Messages.getString("JFXMain.heightVsAmountErrorText")); //$NON-NLS-1$
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
					System.out.println("no hay ninguna ndvi para mostrar"); //$NON-NLS-1$
					return new VBox(new Label(Messages.getString("JFXMain.207"))); //$NON-NLS-1$
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
		String configDate = config.getPropertyOrDefault(Messages.getString("JFXMain.208"), dc.toString(initialDate)); //$NON-NLS-1$
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
		dateDialog.setTitle(Messages.getString("JFXMain.209")); //$NON-NLS-1$
		dateDialog.setHeaderText(Messages.getString("JFXMain.210")); //$NON-NLS-1$
		dateDialog.initOwner(this.stage);
		Optional<ButtonType> res = dateDialog.showAndWait();
		if(res.get().equals(ButtonType.OK)){
			config.setProperty(Messages.getString("JFXMain.211"), dc.toString(ldp.get())); //$NON-NLS-1$
			config.save();
			return ldp.get();
		} else {
			return null;
		}
	}

	//	private void doGetNdviTiffFile(Object labor) {
	//		LocalDate ini =null;
	//		if(labor instanceof Labor){
	//			ini= (LocalDate)((Labor<?>)labor).fechaProperty.getValue();
	//		} 
	//		 ini = dateChooser(ini);
	//		
	//		if(ini!=null){
	//			File downloadLocation = directoryChooser();
	//			if(downloadLocation==null)return;
	//			ObservableList<File> observableList = FXCollections.observableArrayList(new ArrayList<File>());
	//			observableList.addListener((ListChangeListener<File>) c -> {
	//				System.out.println("mostrando los archivos agregados");
	//				if(c.next()){
	//					c.getAddedSubList().forEach((file)->{
	//						showNdviTiffFile(file);
	//					});//fin del foreach
	//				}			
	//			});
	//
	//			GetNDVIForLaborTask task = new GetNDVIForLaborTask(labor,downloadLocation,observableList);
	//			task.setDate(ini);
	//			task.installProgressBar(progressBox);
	//			task.setOnSucceeded(handler -> {
	//				if(labor instanceof Poligono){
	//					((Poligono)labor).getLayer().setEnabled(false);
	//				}
	//				task.uninstallProgressBar();
	//			});
	//			executorPool.submit(task);
	//		}
	//	}

	/**
	 * descargar los tiff correspondientes a un polygono y mostrarlos como ndvi
	 * @param placementObject
	 */
	private void doGetNdviTiffFile(Object placementObject) {//ndvi2
		LocalDate fin =null;
		if(placementObject !=null && Labor.class.isAssignableFrom(placementObject.getClass())){
			fin= DateConverter.asLocalDate((Date)((Labor<?>)placementObject).getFecha());
		} 
		//fin = dateChooser(fin);
		NdviDatePickerDialog ndviDpDLG =new  NdviDatePickerDialog(this.stage);
		LocalDate ret = ndviDpDLG.ndviDateChooser(fin);
		if(ret ==null)return;//seleccionar fecha termino en cancel.
		System.out.println(Messages.getString("JFXMain.212")+ndviDpDLG.initialDate+Messages.getString("JFXMain.213")+ndviDpDLG.finalDate); //$NON-NLS-1$ //$NON-NLS-2$
		//Begin: 2018-02-28 End: 2018-03-28
		fin = ndviDpDLG.finalDate;


		if(fin!=null){
			File downloadLocation=null;
			try {
				downloadLocation = File.createTempFile(Messages.getString("JFXMain.214"), Messages.getString("JFXMain.215")).getParentFile(); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {

				e.printStackTrace();
			}//directoryChooser();
			if(downloadLocation==null)return;
			ObservableList<File> observableList = FXCollections.observableArrayList(new ArrayList<File>());
			observableList.addListener((ListChangeListener<File>) c -> {
				System.out.println(Messages.getString("JFXMain.216")); //$NON-NLS-1$
				if(c.next()){
					c.getAddedSubList().forEach((file)->{
						showNdviTiffFile(file, placementObject,null);
					});//fin del foreach
				}			
			});

			GetNDVI2ForLaborTask task = new GetNDVI2ForLaborTask(placementObject,downloadLocation,observableList);
			task.setFinDate(fin);
			task.setBeginDate(ndviDpDLG.initialDate);
			task.installProgressBar(progressBox);
			task.setOnSucceeded(handler -> {
				if(placementObject instanceof Poligono){
					((Poligono)placementObject).getLayer().setEnabled(false);
				}
				task.uninstallProgressBar();
			});
			System.out.println(Messages.getString("JFXMain.217")); //$NON-NLS-1$
			executorPool.submit(task);
		}
	}

	/**
	 * tomar un Ndvi y mostrarlo como layer
	 * @param ndvi
	 */
	private void doShowNDVI(Ndvi ndvi) {
		executorPool.submit(()->{
			ndvi.loadFileFromContent();
			showNdviTiffFile(ndvi.getF(),null,ndvi);

		});

	}

	/**
	 * seleccionar archivos .tif y mostrarlos como Ndvi
	 */
	private void doOpenNDVITiffFiles() {
		List<File>	files =chooseFiles(Messages.getString("JFXMain.218"), Messages.getString("JFXMain.219")); //$NON-NLS-1$ //$NON-NLS-2$
		if(files!=null)	files.forEach((file)->{
			showNdviTiffFile(file,null,null);
		});//fin del foreach
	}

	private void showNdviTiffFile(File file, Object placementObject,Ndvi _ndvi) {
		ShowNDVITifFileTask task = new ShowNDVITifFileTask(file,_ndvi);
		if( placementObject!=null && Poligono.class.isAssignableFrom(placementObject.getClass())){
			task.setPoligono((Poligono) placementObject);
		}
		task.setOnSucceeded(handler -> {
			Layer ndviLayer = (Layer) handler.getSource().getValue();	
			insertBeforeCompass(getWwd(), ndviLayer);
			this.getLayerPanel().update(this.getWwd());
			viewGoTo(ndviLayer);
			playSound();	
		});
		executorPool.submit(task);

	}

	//	private boolean isLocalPath(String path){
	//		return new File(path).exists();
	//	}


	protected void importImagery()  {
		List<File>	files =chooseFiles(Messages.getString("JFXMain.220"), Messages.getString("JFXMain.221")); //$NON-NLS-1$ //$NON-NLS-2$
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
						throw new Exception(Messages.getString("JFXMain.222")); //$NON-NLS-1$

					// Read the file into the raster. read() returns potentially several rasters if there are multiple
					// files, but in this case there is only one so just use the first element of the returned array.
					DataRaster[] rasters = reader.read(sourceFile, null);
					if (rasters == null || rasters.length == 0)
						throw new Exception(Messages.getString("JFXMain.223")); //$NON-NLS-1$

					DataRaster raster = rasters[0];

					// Determine the sector covered by the image. This information is in the GeoTIFF file or auxiliary
					// files associated with the image file.
					final Sector sector = (Sector) raster.getValue(AVKey.SECTOR);
					if (sector == null)
						throw new Exception(Messages.getString("JFXMain.224")); //$NON-NLS-1$

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
						throw new Exception(Messages.getString("JFXMain.225")); //$NON-NLS-1$
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
						layer.setName(Messages.getString("JFXMain.226")); //$NON-NLS-1$
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

	//	private BufferWrapperRaster loadRasterFile(File file){
	//		if(!file.exists()){	
	//			// si el recurso es web podemos bajarlo a 
	//			// Download the data and save it in a temp file.
	//			String path = file.getAbsolutePath();
	//			file = ExampleUtil.saveResourceToTempFile(path, "." + WWIO.getSuffix(path));
	//		}
	//
	//
	//
	//		// Create a raster reader for the file type.
	//		DataRasterReaderFactory readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
	//				AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
	//		DataRasterReader reader = readerFactory.findReaderFor(file, null);
	//
	//		try{
	//			// Before reading the raster, verify that the file contains elevations.
	//			AVList metadata = reader.readMetadata(file, null);
	//			if (metadata == null || !AVKey.ELEVATION.equals(metadata.getStringValue(AVKey.PIXEL_FORMAT)))
	//			{
	//				Platform.runLater(()->{
	//					Alert imagenAlert = new Alert(Alert.AlertType.ERROR);
	//					imagenAlert.initOwner(stage);
	//					imagenAlert.initModality(Modality.NONE);
	//					imagenAlert.setTitle("Archivo no compatible");
	//					imagenAlert.setContentText("El archivo no continen informacion ndvi. Por favor seleccione un archivo con solo una capa");
	//					imagenAlert.show();
	//				});
	//				String msg = Logging.getMessage("ElevationModel.SourceNotElevations", file.getAbsolutePath());
	//				Logging.logger().severe(msg);
	//				throw new IllegalArgumentException(msg);
	//			}
	//
	//			// Read the file into the raster.
	//			DataRaster[] rasters = reader.read(file, null);
	//			if (rasters == null || rasters.length == 0)	{
	//				String msg = Logging.getMessage("ElevationModel.CannotReadElevations", file.getAbsolutePath());
	//				Logging.logger().severe(msg);
	//				throw new WWRuntimeException(msg);
	//			}
	//
	//			// Determine the sector covered by the elevations. This information is in the GeoTIFF file or auxiliary
	//			// files associated with the elevations file.
	//			Sector sector = (Sector) rasters[0].getValue(AVKey.SECTOR);
	//			if (sector == null)
	//			{
	//				String msg = Logging.getMessage("DataRaster.MissingMetadata", AVKey.SECTOR);
	//				Logging.logger().severe(msg);
	//				throw new IllegalArgumentException(msg);
	//			}
	//
	//			// Request a sub-raster that contains the whole file. This step is necessary because only sub-rasters
	//			// are reprojected (if necessary); primary rasters are not.
	//			int width = rasters[0].getWidth();
	//			int height = rasters[0].getHeight();
	//
	//			DataRaster subRaster = rasters[0].getSubRaster(width, height, sector, rasters[0]);
	//
	//			// Verify that the sub-raster can create a ByteBuffer, then create one.
	//			if (!(subRaster instanceof BufferWrapperRaster))
	//			{
	//				String msg = Logging.getMessage("ElevationModel.CannotCreateElevationBuffer", file.getName());
	//				Logging.logger().severe(msg);
	//				throw new WWRuntimeException(msg);
	//			}
	//
	//			return (BufferWrapperRaster) subRaster;
	//		}
	//		catch (Exception e)
	//		{
	//			e.printStackTrace();
	//			return null;
	//		}
	//	}

	private void doCrearSuelo(Poligono poli) {
		Suelo labor = new Suelo();
		labor.setNombre(Messages.getString("JFXMain.227")); //$NON-NLS-1$
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);

		//		Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(labor);
		//		if(!cosechaConfigured.isPresent()){//
		//			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
		//			labor.dispose();//libero los recursos reservados
		//			return;
		//		}							

		TextInputDialog ppmPDialog = new TextInputDialog(Messages.getString("JFXMain.228")); //$NON-NLS-1$
		ppmPDialog.setTitle(Messages.getString("JFXMain.229")); //$NON-NLS-1$
		ppmPDialog.setContentText(Messages.getString("JFXMain.230")); //$NON-NLS-1$
		Optional<String> ppmPOptional = ppmPDialog.showAndWait();
		Double ppmP = Double.valueOf(ppmPOptional.get());

		TextInputDialog ppmNDialog = new TextInputDialog(Messages.getString("JFXMain.231")); //$NON-NLS-1$
		ppmNDialog.setTitle(Messages.getString("JFXMain.232")); //$NON-NLS-1$
		ppmNDialog.setContentText(Messages.getString("JFXMain.233")); //$NON-NLS-1$
		Optional<String> ppmNOptional = ppmNDialog.showAndWait();
		Double ppmN = Double.valueOf(ppmNOptional.get());
		
		TextInputDialog pMODialog = new TextInputDialog(Messages.getString("JFXMain.234")); //$NON-NLS-1$
		pMODialog.setTitle(Messages.getString("JFXMain.235")); //$NON-NLS-1$
		pMODialog.setContentText(Messages.getString("JFXMain.236")); //$NON-NLS-1$
		Optional<String> pMOOptional = pMODialog.showAndWait();
		Double pMO = Double.valueOf(pMOOptional.get());


		CrearSueloMapTask umTask = new CrearSueloMapTask(labor,poli,ppmP,ppmN,pMO);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			suelos.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.237")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	/**
	 * metodo que toma los poligonos de la labor y genera un mapa de puntos con las densidades configuradas
	 * preguntar si desea generar el muestreo por cantidad de muestras por poligono o densidad de muestras por poligono
	 * permitir configurar cantidad max y min de muestras
	 * permitir configurar superficie minima relevante
	 * @param l una Labor
	 */
	private void doGenerarMuestreoDirigido(Labor<?> l) {		 

		double superficieMinimaAMuestrear=0;
		double densidadDeMuestrasDeseada=0;
		double cantidadMinimaDeMuestrasPoligonoAMuestrear=0;

		TextInputDialog supMinDialog = new TextInputDialog(Messages.getString("JFXMain.238")); //$NON-NLS-1$
		supMinDialog.setTitle(Messages.getString("JFXMain.239")); //$NON-NLS-1$
		supMinDialog.setContentText(Messages.getString("JFXMain.240")); //$NON-NLS-1$
		Optional<String> ppmPOptional = supMinDialog.showAndWait();
		superficieMinimaAMuestrear = Double.valueOf(ppmPOptional.get());

		TextInputDialog densidadDialog = new TextInputDialog(Messages.getString("JFXMain.241")); //$NON-NLS-1$
		densidadDialog.setTitle(Messages.getString("JFXMain.242")); //$NON-NLS-1$
		densidadDialog.setContentText(Messages.getString("JFXMain.243")); //$NON-NLS-1$
		Optional<String> densidadOptional = densidadDialog.showAndWait();
		densidadDeMuestrasDeseada = Double.valueOf(densidadOptional.get());

		TextInputDialog ppmNDialog = new TextInputDialog(Messages.getString("JFXMain.244")); //$NON-NLS-1$
		ppmNDialog.setTitle(Messages.getString("JFXMain.245")); //$NON-NLS-1$
		ppmNDialog.setContentText(Messages.getString("JFXMain.246")); //$NON-NLS-1$
		Optional<String> cantOptional = ppmNDialog.showAndWait();
		cantidadMinimaDeMuestrasPoligonoAMuestrear = Double.valueOf(cantOptional.get());

		GenerarMuestreoDirigidoTask umTask = new GenerarMuestreoDirigidoTask(Collections.singletonList(l),superficieMinimaAMuestrear,densidadDeMuestrasDeseada,cantidadMinimaDeMuestrasPoligonoAMuestrear);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			System.out.println(Messages.getString("JFXMain.247")); //$NON-NLS-1$
			suelos.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.248")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	private void doCrearPulverizacion(Poligono poli) {
		PulverizacionLabor labor = new PulverizacionLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<PulverizacionLabor> cosechaConfigured= PulverizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.249")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.250")); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.251")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.252")); //$NON-NLS-1$
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearPulverizacionMapTask umTask = new CrearPulverizacionMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
			pulverizaciones.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			poli.getLayer().setEnabled(false);
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.253")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}


	private void doCrearSiembra(Poligono poli) {
		SiembraLabor labor = new SiembraLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		labor.setNombre(poli.getNombre()+Messages.getString("JFXMain.254")+Messages.getString("JFXMain.255")); //$NON-NLS-1$ //$NON-NLS-2$
		Optional<SiembraLabor> siembraConfigured= SiembraConfigDialogController.config(labor);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							
//TODO modificar el dialogo para permitir ingresar la cantidad y la unidad incluyendo kg/ha plantas/m2 y miles de plantas/Ha
		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.257")); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.258")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.259")); //$NON-NLS-1$
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearSiembraMapTask umTask = new CrearSiembraMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			siembras.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			poli.getLayer().setEnabled(false);
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.260")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	private void doCrearFertilizacion(Poligono poli) {
		FertilizacionLabor labor = new FertilizacionLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.261")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.262")); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.263")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.264")); //$NON-NLS-1$
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearFertilizacionMapTask umTask = new CrearFertilizacionMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			fertilizaciones.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			poli.getLayer().setEnabled(false);
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.265")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	private void doCrearCosecha(Poligono poli) {
		CosechaLabor labor = new CosechaLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		labor.setNombre(poli.getNombre());
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.266")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.267")); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.268")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.269")); //$NON-NLS-1$
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = Double.valueOf(anchoOptional.get());
		CrearCosechaMapTask umTask = new CrearCosechaMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			cosechas.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			poli.getLayer().setEnabled(false);
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.270")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}



	private void doConvertirNdviACosecha(Ndvi ndvi) {
		CosechaLabor labor = new CosechaLabor();
		labor.setNombre(ndvi.getNombre());
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.271")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							

		Double rinde = null;
		try {
			TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.272")); //$NON-NLS-1$
			anchoDialog.setTitle(Messages.getString("JFXMain.273")); //$NON-NLS-1$
			anchoDialog.setContentText(Messages.getString("JFXMain.274")); //$NON-NLS-1$
			anchoDialog.initOwner(this.stage);
			Optional<String> anchoOptional = anchoDialog.showAndWait();
			rinde = Double.valueOf(anchoOptional.get());
		}catch(java.lang.NumberFormatException e) {

			DecimalFormat format=(DecimalFormat) DecimalFormat.getInstance();
			DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
			char sep=symbols.getDecimalSeparator();

			Alert a = new Alert(Alert.AlertType.ERROR);
			a.initOwner(this.stage);

			a.setTitle(Messages.getString("JFXMain.275")); //$NON-NLS-1$
			a.setHeaderText(Messages.getString("JFXMain.276")+sep+Messages.getString("JFXMain.277")); //$NON-NLS-1$ //$NON-NLS-2$
			a.setContentText(e.getMessage());
			a.show();
			return;
		}

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
			System.out.println(Messages.getString("JFXMain.278")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
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

		Optional<CosechaLabor> cosechaConfigured=HarvestConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(cConfigured);
			umTask.installProgressBar(progressBox);
			umTask.setOnSucceeded(handler -> {
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();

				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.279")); //$NON-NLS-1$
				playSound();
			});//fin del OnSucceeded						
			JFXMain.executorPool.execute(umTask);
		}
	}

	private void doExtraerPoligonos(Labor<?> labor ) {	
		ExtraerPoligonosDeLaborTask umTask = new ExtraerPoligonosDeLaborTask(labor);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {

			@SuppressWarnings("unchecked")
			List<Poligono> poligonos = (List<Poligono>)handler.getSource().getValue();
			this.showPoligonos(poligonos);
			umTask.uninstallProgressBar();

			this.wwjPanel.repaint();
			System.out.println(Messages.getString("JFXMain.280")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();
		JFXMain.executorPool.execute(umTask);

	}

	private void doEditSuelo(Suelo cConfigured) {			
		Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			OpenSoilMapTask umTask = new OpenSoilMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				//Suelo ret = (Suelo)handler.getSource().getValue();
				
			
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				this.wwjPanel.repaint();
				playSound();
			});//fin del OnSucceeded
			//umTask.start();
			JFXMain.executorPool.execute(umTask);
		}
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
				System.out.println(Messages.getString("JFXMain.281")); //$NON-NLS-1$
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			JFXMain.executorPool.execute(umTask);
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
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.282")); //$NON-NLS-1$
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			JFXMain.executorPool.execute(umTask);
		}
	}

	private void doEditFertilizacion(FertilizacionLabor cConfigured ) {
		Optional<FertilizacionLabor> cosechaConfigured=FertilizacionConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			ProcessFertMapTask umTask = new ProcessFertMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				//CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				//	viewGoTo(ret);
				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.283")); //$NON-NLS-1$
				playSound();
			});//fin del OnSucceeded						
			//umTask.start();
			JFXMain.executorPool.execute(umTask);
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
		boolean calibrar =false;

		if(cosechasAUnir.size()>1) {
			Alert calibrarAlert = new Alert(Alert.AlertType.CONFIRMATION);
			calibrarAlert.setTitle(Messages.getString("JFXMain.284")); //$NON-NLS-1$
			calibrarAlert.setContentText(Messages.getString("JFXMain.285")); //$NON-NLS-1$

			Optional<ButtonType> calibrarButton = calibrarAlert.showAndWait();
			if(calibrarButton.isPresent()){
				if(calibrarButton.get().equals(ButtonType.OK)){
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
				cosechas.add(ret);
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
			}
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			System.out.println(Messages.getString("JFXMain.286")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();					
		JFXMain.executorPool.execute(umTask);
		//			}
		//		}
	}

	private void doUnirFertilizaciones(FertilizacionLabor fertilizacionLabor) {
		List<FertilizacionLabor> fertilizacionesAUnir = new ArrayList<FertilizacionLabor>();
		if(fertilizacionLabor == null){
			List<FertilizacionLabor> fertilizacionesEnabled = fertilizaciones.stream().filter((l)->{
				Layer layer =l.getLayer();
				return layer!=null&&layer.isEnabled();}).collect(Collectors.toList());
			fertilizacionesAUnir.addAll( fertilizacionesEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {

			fertilizacionesAUnir.add(fertilizacionLabor);

		}
		UnirFertilizacionesMapTask umTask = new UnirFertilizacionesMapTask(fertilizacionesAUnir);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			if(ret.getLayer()!=null){
				fertilizaciones.add(ret);
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
			}
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			System.out.println(Messages.getString("JFXMain.287")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();					
		JFXMain.executorPool.execute(umTask);
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
		TextInputDialog anchoDialog = new TextInputDialog(config.getPropertyOrDefault(CosechaConfig.ANCHO_GRILLA_KEY,Messages.getString("JFXMain.288"))); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.289")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.290")); //$NON-NLS-1$
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

			System.out.println(Messages.getString("JFXMain.291")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();					
		JFXMain.executorPool.execute(umTask);
	}

	private void doGuardarPoligono(Poligono layerObject){
		DAH.save(layerObject);
	}


	private void doGuardarLabor(Labor<?> layerObject) {
		DAH.save(layerObject);		
	}

	// generar un layer de fertilizacion a partir de una cosecha
	//el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
	//que producto aplico y en que densidad por hectarea
	private void doRecomendFertPFromHarvest(CosechaLabor value) {
		FertilizacionLabor labor = new FertilizacionLabor();
		labor.setLayer(new LaborLayer());

		labor.setNombre(value.getNombre()+Messages.getString("JFXMain.292")); //$NON-NLS-1$
		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.293")); //$NON-NLS-1$
			return;
		}							

		//Dialogo preguntar min y max a aplicar
		Alert minMaxDialog = new Alert(AlertType.CONFIRMATION);
		DecimalFormat df = new DecimalFormat();
		TextField min = new TextField(df.format(0));
		TextField max = new TextField(df.format(0));

		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.294")),min)); //$NON-NLS-1$
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.295")),max)); //$NON-NLS-1$

		minMaxDialog.setGraphic(vb);
		minMaxDialog.setTitle(Messages.getString("JFXMain.296")); //$NON-NLS-1$
		minMaxDialog.setContentText(Messages.getString("JFXMain.297")); //$NON-NLS-1$
		//dateDialog.setHeaderText("Fecha Desde");
		minMaxDialog.initOwner(this.stage);
		Optional<ButtonType> res = minMaxDialog.showAndWait();
		Double minFert =null,maxFert=null; 
		if(res.get().equals(ButtonType.OK)){
			try {
				minFert=df.parse(min.getText()).doubleValue();
				if(minFert==0)minFert=null;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				maxFert=df.parse(max.getText()).doubleValue();
				if(maxFert==0)maxFert=null;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return;
		}

		RecomendFertPFromHarvestMapTask umTask = new RecomendFertPFromHarvestMapTask(labor,value);
		umTask.setMinFert(minFert);
		umTask.setMaxFert(maxFert);
		umTask.installProgressBar(progressBox);

		//	testLayer();
		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			fertilizaciones.add(ret);//TODO cambiar esto cuando cambie las acciones a un menu contextual en layerPanel
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();

			viewGoTo(ret);

			System.out.println(Messages.getString("JFXMain.298")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		//	umTask.start();
		JFXMain.executorPool.execute(umTask);
	}

	private void doRecomendFertNFromHarvest(CosechaLabor cosecha) {
		// TODO generar un layer de fertilizacion a partir de una cosecha
		//el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
		//que producto aplico y en que densidad por hectarea

		//TODO permitir generar una aplicacion partida en 2 o mas fechas.

		List<Suelo> suelosEnabled = suelos.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<FertilizacionLabor> fertEnabled = fertilizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());



		FertilizacionLabor fertN = new FertilizacionLabor();
		fertN.setLayer(new LaborLayer());

		fertN.setNombre(cosecha.getNombre()+Messages.getString("JFXMain.299")); //$NON-NLS-1$
		Optional<FertilizacionLabor> fertConfigured= FertilizacionConfigDialogController.config(fertN);
		if(!fertConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.300")); //$NON-NLS-1$
			return;
		}							

		//Dialogo preguntar min y max a aplicar
		Alert minMaxDialog = new Alert(AlertType.CONFIRMATION);
		DecimalFormat df = new DecimalFormat();
		TextField min = new TextField(df.format(0));
		TextField max = new TextField(df.format(0));

		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.301")),min)); //$NON-NLS-1$
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.302")),max)); //$NON-NLS-1$

		minMaxDialog.setGraphic(vb);
		minMaxDialog.setTitle(Messages.getString("JFXMain.303")); //$NON-NLS-1$
		minMaxDialog.setContentText(Messages.getString("JFXMain.304")); //$NON-NLS-1$
		//dateDialog.setHeaderText("Fecha Desde");
		minMaxDialog.initOwner(this.stage);
		Optional<ButtonType> res = minMaxDialog.showAndWait();
		Double minFert =null,maxFert=null; 
		if(res.get().equals(ButtonType.OK)){
			try {
				minFert=df.parse(min.getText()).doubleValue();
				if(minFert==0)minFert=null;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				maxFert=df.parse(max.getText()).doubleValue();
				if(maxFert==0)maxFert=null;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return;
		}

		RecomendFertNFromHarvestMapTask umTask = new RecomendFertNFromHarvestMapTask(fertN,cosecha, suelosEnabled,
				fertEnabled);
		umTask.setMinFert(minFert);
		umTask.setMaxFert(maxFert);
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

			System.out.println(Messages.getString("JFXMain.305")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		//	umTask.start();
		JFXMain.executorPool.execute(umTask);
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
					System.out.println(Messages.getString("JFXMain.306")); //$NON-NLS-1$
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
					System.out.println(Messages.getString("JFXMain.307")); //$NON-NLS-1$
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);

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
					System.out.println(Messages.getString("JFXMain.308")); //$NON-NLS-1$
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

					System.out.println(Messages.getString("JFXMain.309")); //$NON-NLS-1$
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores

		}//if stores != null

	}


	private void doOpenSiembraMap(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				SiembraLabor labor = new SiembraLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<SiembraLabor> cosechaConfigured= SiembraConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.310")); //$NON-NLS-1$
					continue;
				}							

				ProcessSiembraMapTask umTask = new ProcessSiembraMapTask(labor);
				umTask.installProgressBar(progressBox);

				umTask.setOnSucceeded(handler -> {
					SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
					siembras.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println(Messages.getString("JFXMain.311")); //$NON-NLS-1$
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}


	private void doImportarPoligonos(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		executorPool.submit(()->{
			if (stores != null) {for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				System.out.println(Messages.getString("JFXMain.312")); //$NON-NLS-1$
				try {
					SimpleFeatureSource	source = store.getFeatureSource();

					SimpleFeatureIterator iterator = source.getFeatures().features();

					while(iterator.hasNext()){
						SimpleFeature feature = iterator.next();			
						double has = ProyectionConstants.A_HAS(((Geometry)feature.getDefaultGeometry()).getArea());
						if(has<0.2)continue;//cada poli mayor a 10m2
						Poligono poli = ExtraerPoligonosDeLaborTask.featureToPoligono(feature);
						MeasureTool measureTool = PoligonLayerFactory.createPoligonLayer(poli, this.getWwd(), this.getLayerPanel());
						poli.setArea(has);
						insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
					}//fin del while sobre las features
					this.getLayerPanel().update(this.getWwd());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}//fin del for stores
			}//if stores != null
		});
	}




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
					System.out.println(Messages.getString("JFXMain.313")); //$NON-NLS-1$
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

					System.out.println(Messages.getString("JFXMain.314")); //$NON-NLS-1$
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				JFXMain.executorPool.execute(umTask);
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
					System.out.println(Messages.getString("JFXMain.315")); //$NON-NLS-1$
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

					System.out.println(Messages.getString("JFXMain.316")); //$NON-NLS-1$
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				JFXMain.executorPool.execute(umTask);
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
					System.out.println(Messages.getString("JFXMain.317")); //$NON-NLS-1$
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

					System.out.println(Messages.getString("JFXMain.318")); //$NON-NLS-1$
					playSound();
				});//fin del OnSucceeded
				//umTask.start();
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores

		}//if stores != null

	}

	private void doProcessMargin() {		
		System.out.println(Messages.getString("JFXMain.319")); //$NON-NLS-1$

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
		sb.append(Messages.getString("JFXMain.320")); //$NON-NLS-1$
		cosechasEnabled.forEach((c)->sb.append(c.getNombre()+Messages.getString("JFXMain.321"))); //$NON-NLS-1$
		margen.setNombre(sb.toString());

		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.322")); //$NON-NLS-1$
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

			System.out.println(Messages.getString("JFXMain.323")); //$NON-NLS-1$
		});
		executorPool.execute(uMmTask);
	}

	private void doEditMargin(Margen margen) {		
		System.out.println(Messages.getString("JFXMain.324")); //$NON-NLS-1$
		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.325")); //$NON-NLS-1$
			return;
		}							
		OpenMargenMapTask uMmTask = new OpenMargenMapTask(margen);
		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			this.getLayerPanel().update(this.getWwd());
			uMmTask.uninstallProgressBar();
			this.wwjPanel.repaint();
			System.out.println(Messages.getString("JFXMain.326")); //$NON-NLS-1$
			playSound();
		});
		executorPool.execute(uMmTask);
		//}
	}

	private void doProcesarBalanceNutrientes() {		
		System.out.println(Messages.getString("JFXMain.327")); //$NON-NLS-1$
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


		ProcessNewSoilMapTask balanceNutrientesTask = new ProcessNewSoilMapTask(suelosEnabled,
				cosechasEnabled, fertEnabled);

		balanceNutrientesTask.installProgressBar(progressBox);

		balanceNutrientesTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			balanceNutrientesTask.uninstallProgressBar();

			this.suelos.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.328")); //$NON-NLS-1$
		});
		executorPool.execute(balanceNutrientesTask);
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

		String nombre = laborToExport.getNombre();
		File shapeFile =  getNewShapeFile(nombre);

		ExportLaborMapTask ehTask = new ExportLaborMapTask(cosechaLabor,shapeFile);
		executorPool.execute(ehTask);
	}


	//TODO permitir al ususario definir el formato. para siembra fertilizada necesita 3 columnas
	//en la linea, al costado de la linea, siembra
	private void doExportPrescripcion(FertilizacionLabor laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile =  getNewShapeFile(nombre);
		executorPool.execute(()->{
			SimpleFeatureType type = null;
			String typeDescriptor = Messages.getString("JFXMain.329")+Polygon.class.getCanonicalName()+Messages.getString("JFXMain.330") //$NON-NLS-1$ //$NON-NLS-2$
					+ Messages.getString("JFXMain.331") + Messages.getString("JFXMain.332"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(Messages.getString("JFXMain.333")+typeDescriptor); //$NON-NLS-1$
			System.out.println(Messages.getString("JFXMain.334")+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
			try {
				type = DataUtilities.createType(Messages.getString("JFXMain.335"), typeDescriptor); //$NON-NLS-1$
			} catch (SchemaException e) {
				e.printStackTrace();
			}

			System.out.println(Messages.getString("JFXMain.336")+DataUtilities.spec(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$

			SimpleFeatureIterator it = laborToExport.outCollection.features();
			DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection(Messages.getString("JFXMain.337"),type); //$NON-NLS-1$
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok
			while(it.hasNext()){
				FertilizacionItem fi = laborToExport.constructFeatureContainerStandar(it.next(),false);
				Geometry itemGeometry=fi.getGeometry();
				List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
				for(Polygon p : flatPolygons){
					fb.add(p);
					Double dosisHa = fi.getDosistHa();

					System.out.println(Messages.getString("JFXMain.338")+dosisHa); //$NON-NLS-1$
					fb.add(dosisHa.longValue());

					SimpleFeature exportFeature = fb.buildFeature(fi.getId().toString());
					exportFeatureCollection.add(exportFeature);
				}
			}
			it.close();

			ShapefileDataStore newDataStore = createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1 
			SimpleFeatureSource featureSource = null;
			try {
				String typeName = newDataStore.getTypeNames()[0];
				featureSource = newDataStore.getFeatureSource(typeName);
			} catch (IOException e) {

				e.printStackTrace();
			}


			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
				Transaction transaction = new DefaultTransaction(Messages.getString("JFXMain.339")); //$NON-NLS-1$
				featureStore.setTransaction(transaction);

				/*
				 * SimpleFeatureStore has a method to add features from a
				 * SimpleFeatureCollection object, so we use the
				 * ListFeatureCollection class to wrap our list of features.
				 */

				try {
					featureStore.setFeatures(exportFeatureCollection.reader());
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

			System.out.println(Messages.getString("JFXMain.340")+ shapeFile); //$NON-NLS-1$
			Configuracion config = Configuracion.getInstance();
			config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
			config.save();
		});//fin del run later
	}

	//TODO permitir al ususario definir el formato. para siembra fertilizada necesita 3 columnas
	//en la linea, al costado de la linea, siembra
	private void doExportPrescripcionSiembra(SiembraLabor laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile =  getNewShapeFile(nombre);
		executorPool.execute(()->{
			SimpleFeatureType type = null;
			String typeDescriptor = Messages.getString("JFXMain.341")+Polygon.class.getCanonicalName()+Messages.getString("JFXMain.342") //$NON-NLS-1$ //$NON-NLS-2$

				+ SiembraLabor.COLUMNA_DOSIS_LINEA + Messages.getString("JFXMain.343") //$NON-NLS-1$
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO + Messages.getString("JFXMain.344") //$NON-NLS-1$
				+ Messages.getString("JFXMain.345") + Messages.getString("JFXMain.346"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(Messages.getString("JFXMain.347")+typeDescriptor); //$NON-NLS-1$
			System.out.println(Messages.getString("JFXMain.348")+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
			try {
				type = DataUtilities.createType(Messages.getString("JFXMain.349"), typeDescriptor); //$NON-NLS-1$
			} catch (SchemaException e) {
				e.printStackTrace();
			}

			System.out.println(Messages.getString("JFXMain.350")+DataUtilities.spec(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$

			List<LaborItem> items = new ArrayList<LaborItem>();
			int zonas = laborToExport.outCollection.size();
			if(zonas>=100) {
				//TODO reabsorver zonas mas chicas a las mas grandes vecinas
				System.out.println(Messages.getString("JFXMain.351")); //$NON-NLS-1$
				//TODO tomar las 100 zonas mas grandes y reabsorver las otras en estas
				
				SimpleFeatureIterator it = laborToExport.outCollection.features();
				while(it.hasNext()){
					SiembraItem fi = laborToExport.constructFeatureContainerStandar(it.next(),false);
					items.add(fi);
				}
				it.close();
				
				items.sort((i1,i2)->-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea()));					
				List<LaborItem> itemsAgrandar =items.subList(0,100-1);
				Quadtree tree=new Quadtree();
				for(LaborItem ar : itemsAgrandar) {
					Geometry gAr =ar.getGeometry();
					tree.insert(gAr.getEnvelopeInternal(), ar);
				}
				List<LaborItem> itemsAReducir =items.subList(100, items.size()-1);
				int n=0;
				while(itemsAReducir.size()>0 || n>10) {
					List<LaborItem> done = new ArrayList<LaborItem>();		
					for(LaborItem ar : itemsAReducir) {
						Geometry gAr =ar.getGeometry();
						List<LaborItem> vecinos =(List<LaborItem>) tree.query(gAr.getEnvelopeInternal());

						if(vecinos.size()>0) {
							Optional<LaborItem> opV = vecinos.stream().reduce((v1,v2)->{
								boolean v1i = gAr.intersects(v1.getGeometry());
								boolean v2i = gAr.intersects(v2.getGeometry());
								return v1i&&v2i?(v1.getGeometry().getArea()>v2.getGeometry().getArea()?v1:v2):(v1i?v1:v2);

							});
							if(opV.isPresent()) {
								LaborItem v = opV.get();
								Geometry g = v.getGeometry();
								tree.remove(g.getEnvelopeInternal(), v);
								Geometry union = g.union(gAr);
								v.setGeometry(union);
								tree.insert(union.getEnvelopeInternal(), v);
								done.add(ar);
							}
						}
					}
					n++;
					itemsAReducir.removeAll(done);
				}
				items.clear();
				items.addAll((List<LaborItem>)tree.queryAll());

			}

			DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection(Messages.getString("JFXMain.352"),type); //$NON-NLS-1$
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok
		
			for(LaborItem i:items) {//(it.hasNext()){
				SiembraItem fi=(SiembraItem) i;
				Geometry itemGeometry=fi.getGeometry();
				List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
				for(Polygon p : flatPolygons){
					fb.add(p);
					Double semilla = fi.getDosisML()*10;///XXX aca hago magia para convertir de plantas por metro a plantas cada 10 metros
					Double linea = fi.getDosisFertLinea();
					Double costado = fi.getDosisFertCostado();

					System.out.println(Messages.getString("JFXMain.353")+semilla); //$NON-NLS-1$
					fb.add(linea);
					fb.add(costado);
					fb.add(semilla.longValue());

					SimpleFeature exportFeature = fb.buildFeature(fi.getId().toString());
					exportFeatureCollection.add(exportFeature);
				}
			}
			//it.close();

			ShapefileDataStore newDataStore = createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1 
			SimpleFeatureSource featureSource = null;
			try {
				String typeName = newDataStore.getTypeNames()[0];
				featureSource = newDataStore.getFeatureSource(typeName);
			} catch (IOException e) {

				e.printStackTrace();
			}


			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
				Transaction transaction = new DefaultTransaction(Messages.getString("JFXMain.354")); //$NON-NLS-1$
				featureStore.setTransaction(transaction);

				/*
				 * SimpleFeatureStore has a method to add features from a
				 * SimpleFeatureCollection object, so we use the
				 * ListFeatureCollection class to wrap our list of features.
				 */

				try {
					featureStore.setFeatures(exportFeatureCollection.reader());
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

			System.out.println(Messages.getString("JFXMain.355")+ shapeFile); //$NON-NLS-1$
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

		String nombre = laborToExport.getNombre();
		File shapeFile =  getNewShapeFile(nombre);
		Platform.runLater(()->{//esto me introduce un error al grabar en el que se pierderon features

			SimpleFeatureType type = laborToExport.getPointType();

			ShapefileDataStore newDataStore = createShapefileDataStore(shapeFile,type);

			SimpleFeatureIterator it = laborToExport.outCollection.features();
			DefaultFeatureCollection pointFeatureCollection =  new DefaultFeatureCollection(Messages.getString("JFXMain.356"),type); //$NON-NLS-1$
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
				Transaction transaction = new DefaultTransaction(Messages.getString("JFXMain.357")); //$NON-NLS-1$
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


	private ShapefileDataStore createShapefileDataStore(File shapeFile,	SimpleFeatureType type) {
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put(Messages.getString("JFXMain.358"), shapeFile.toURI().toURL()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put(Messages.getString("JFXMain.359"), Boolean.TRUE); //$NON-NLS-1$


		ShapefileDataStore newDataStore=null;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(type);
			//newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
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
			System.out.println(Messages.getString("JFXMain.360")); //$NON-NLS-1$
			Iterator<?> it = labor.outCollection.iterator();
			while(it.hasNext()){
				LaborItem ci = labor.constructFeatureContainerStandar((SimpleFeature)it.next(), false);
				ciLista.add(ci);
			}

			final ObservableList<LaborItem> dataLotes =
					FXCollections.observableArrayList(
							ciLista
							);

			SmartTableView<LaborItem> table = new SmartTableView<LaborItem>(dataLotes);
			table.setEditable(false);

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(labor.getNombre());
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	private void doShowLaboresTable() {
		Platform.runLater(()->{
			final ObservableList<Labor<?>> data =
					FXCollections.observableArrayList(
							DAH.getAllLabores()
							);
			if(data.size()<1){
				System.out.println(Messages.getString("JFXMain.361")); //$NON-NLS-1$
				//data.add(new Lote());
			}
			SmartTableView<Labor<?>> table = new SmartTableView<Labor<?>>(data);//,data);
			table.setEditable(true);
			//table.setOnDoubleClick(()->new Lote("Nuevo Lote"));

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.362")); //$NON-NLS-1$
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}

	/**
	 * 
	 */
	private void doShowAcercaDe() {
		Alert acercaDe = new Alert(AlertType.INFORMATION);
		acercaDe.titleProperty().set(Messages.getString("JFXMain.363")+JFXMain.TITLE_VERSION); //$NON-NLS-1$
		acercaDe.initOwner(this.stage);
		//acercaDe.setHeaderText(this.TITLE_VERSION+"\n"+this.BUILD_INFO+"\nVisitar www.ursulagis.com");
		//acercaDe.contentTextProperty().set();
		String content =   Messages.getString("JFXMain.364")+JFXMain.TITLE_VERSION+Messages.getString("JFXMain.365") //$NON-NLS-1$ //$NON-NLS-2$
				+JFXMain.BUILD_INFO
				+ Messages.getString("JFXMain.366")+Messages.getString("JFXMain.367")+Messages.getString("JFXMain.368"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		WebView webView = new WebView();
		webView.getEngine().loadContent(Messages.getString("JFXMain.369")+content+Messages.getString("JFXMain.370")); //$NON-NLS-1$ //$NON-NLS-2$


		//   webView.setPrefSize(150, 60);
		acercaDe.setHeaderText(Messages.getString("JFXMain.371")); //$NON-NLS-1$
		acercaDe.setGraphic(null);

		acercaDe.getDialogPane().setContent(webView);;
		//  alert.showAndWait();
		acercaDe.setResizable(true);
		acercaDe.show();
	}

	private void doConfigCultivo() {
		Platform.runLater(()->{
			final ObservableList<Cultivo> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllCultivos()
							);

			SmartTableView<Cultivo> table = new SmartTableView<Cultivo>(dataLotes);//,dataLotes);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Cultivo(Messages.getString("JFXMain.372"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.373")); //$NON-NLS-1$
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

			SmartTableView<Fertilizante> table = new SmartTableView<Fertilizante>(dataLotes);//,dataLotes);
			table.setEditable(true);

			table.setOnDoubleClick(()->new Fertilizante(Messages.getString("JFXMain.374"))); //$NON-NLS-1$


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.375")); //$NON-NLS-1$
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	private void doConfigAgroquimicos() {
		Platform.runLater(()->{

			//			ArrayList<Fertilizante> ciLista = new ArrayList<Fertilizante>();
			//			System.out.println("Comenzando a cargar la los datos de la tabla");
			//
			//			ciLista.addAll(Fertilizante.fertilizantes.values());
			//			final ObservableList<Fertilizante> dataLotes =
			//					FXCollections.observableArrayList(
			//							ciLista
			//							);

			final ObservableList<Agroquimico> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllAgroquimicos()
							);

			SmartTableView<Agroquimico> table = new SmartTableView<Agroquimico>(dataLotes);//,dataLotes);
			table.setEditable(true);

			table.setOnDoubleClick(()->new Agroquimico(Messages.getString("JFXMain.376"))); //$NON-NLS-1$


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.377")); //$NON-NLS-1$
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
				data.add(new Campania(Messages.getString("JFXMain.378")));//TODO obtener el anio actual y armar 16/17 //$NON-NLS-1$
			}
			SmartTableView<Campania> table = new SmartTableView<Campania>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Campania(Messages.getString("JFXMain.379"))); //$NON-NLS-1$
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.380")); //$NON-NLS-1$
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

			SmartTableView<Poligono> table = new SmartTableView<Poligono>(data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((poli)->{
				poli.setActivo(true);
				showPoligonos(Collections.singletonList(poli));
				Position pos =poli.getPositions().get(0);
				viewGoTo(pos);
				Platform.runLater(()->{
					DAH.save(poli);
				});
			});


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.381")); //$NON-NLS-1$
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				this.getLayerPanel().update(this.getWwd());
				//getWwd().redraw();
			});
			try {
				tablaStage.show();	 
			}catch(Exception e){
				//aca lanza un error de que no encuentra el column header. probablemente porque en java 10 no tiene permiso para acceder al internal api
				//java.lang.NoSuchMethodException: javafx.scene.control.skin.TableViewSkin.getTableHeaderRow()

			}
		});	
	}

	private void doShowNdviTable() {
		Platform.runLater(()->{
			final ObservableList<Ndvi> data =
					FXCollections.observableArrayList(
							DAH.getAllNdvi()
							);

			SmartTableView<Ndvi> table = new SmartTableView<Ndvi>(data);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((ndvi)->{
				//poli.setActivo(true);
				doShowNDVI(ndvi);

			});


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.382")); //$NON-NLS-1$
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				this.getLayerPanel().update(this.getWwd());
				//getWwd().redraw();
			});

			tablaStage.show();	 
		});	
	}
	
	private void doChangeLocale() {
		List<Locale> locales = Messages.getLocales();
		Locale actual = Messages.getLocale();
		
		ChoiceDialog<Locale> dialog = new ChoiceDialog<>(actual, locales);
		dialog.setTitle(Messages.getString("JFXMain.383")); //$NON-NLS-1$
		dialog.setHeaderText(Messages.getString("JFXMain.384")); //$NON-NLS-1$
		dialog.setContentText(Messages.getString("JFXMain.385")); //$NON-NLS-1$
		dialog.initOwner(stage);
		Optional<Locale> result = dialog.showAndWait();
		// The Java 8 way to get the response value (with lambda expression).
		result.ifPresent(newLocale -> Messages.setLocale(newLocale));
		
		//TODO redibujar la ventana principal con el nuevo locale
	}

	private void doConfigEstablecimiento() {
		Platform.runLater(()->{

			final ObservableList<Establecimiento> data =
					FXCollections.observableArrayList(
							DAH.getAllEstablecimientos()
							);
			if(data.size()<1){
				data.add(new Establecimiento());
			}

			SmartTableView<Establecimiento> table = new SmartTableView<Establecimiento>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Establecimiento(Messages.getString("JFXMain.386"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.387")); //$NON-NLS-1$
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
			if(data.size()<1){
				data.add(new Lote());
			}
			SmartTableView<Lote> table = new SmartTableView<Lote>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Lote(Messages.getString("JFXMain.388"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.389")); //$NON-NLS-1$
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
			if(data.size()<1){
				data.add(new Empresa(Messages.getString("JFXMain.390"))); //$NON-NLS-1$
			}
			SmartTableView<Empresa> table = new SmartTableView<Empresa>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Empresa(Messages.getString("JFXMain.391"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.392")); //$NON-NLS-1$
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
			SmartTableView<Semilla> table = new SmartTableView<Semilla>(dataLotes);//,dataLotes);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Semilla(Messages.getString("JFXMain.393"),DAH.getAllCultivos().get(0))); //$NON-NLS-1$
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.394")); //$NON-NLS-1$
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}
	private void doSnapshot(){
		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);

		WritableImage image = wwNode.snapshot(params, null);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("JFXMain.395")); //$NON-NLS-1$


		File lastFile = null;
		String lastFileName =  config.getPropertyOrDefault(Configuracion.LAST_FILE,Messages.getString("JFXMain.396")); //$NON-NLS-1$
		if(lastFileName != Messages.getString("JFXMain.397")){ //$NON-NLS-1$
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 
		//if(lastFile != null && lastFile.exists()){
		fileChooser.setInitialDirectory(lastFile.getParentFile());
		fileChooser.setInitialFileName(lastFile.getName());


		//	if(file!=null)		fileChooser.setInitialDirectory(file.getParentFile());
		// Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				Messages.getString("JFXMain.398"), Messages.getString("JFXMain.399")); //$NON-NLS-1$ //$NON-NLS-2$
		fileChooser.getExtensionFilters().add(extFilter);
		// Show save file dialog
		File snapsthotFile = fileChooser.showSaveDialog(this.stage);

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), Messages.getString("JFXMain.400"), snapsthotFile); //$NON-NLS-1$
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	private List<String> getAvailableColumns(FileDataStore store) {
	//		List<String> availableColumns = new ArrayList<String>();
	//
	//		SimpleFeatureType sch;
	//		try {
	//			sch = store.getSchema();
	//			List<AttributeType> types = sch.getTypes();
	//			for (AttributeType at : types) {
	//				availableColumns.add(at.getName().toString());
	//			}
	//
	//		} catch (IOException e) {			
	//			e.printStackTrace();
	//		}
	//		return availableColumns;
	//	}

	//	private FileDataStore chooseShapeFileAndGetStore() {
	//		FileDataStore store = null;
	//		try{
	//			store = chooseShapeFileAndGetMultipleStores(null).get(0);
	//		}catch(Exception e ){
	//			e.printStackTrace();
	//		}
	//		return store;
	//	}

	private List<FileDataStore> chooseShapeFileAndGetMultipleStores(List<File> files) {
		if(files==null){
			//	List<File> 
			files =chooseFiles(Messages.getString("JFXMain.401"), Messages.getString("JFXMain.402")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		List<FileDataStore> stores = new ArrayList<FileDataStore>();
		if (files != null) {
			for(File f : files){
				try {
					stores.add(FileDataStoreFinder.getDataStore(f));//esto falla con java10 :(
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

	//	private File directoryChooser(){
	//		DirectoryChooser fileChooser = new DirectoryChooser();
	//		fileChooser.setTitle("Seleccione un directorio");
	//
	//		Configuracion config = Configuracion.getInstance();
	//		File lastFile = null;
	//		String lastFileName =config.getPropertyOrDefault(Configuracion.LAST_FILE,"");
	//
	//		if(lastFileName != null){
	//			//LAST_FILE=F\:\\AgGPS\\Data\\Cliente_Predet\\Establecimiento_Predet\\030817_0001_EZ64952\\Swaths.shp
	//			//lastfile es valido pero ya no existe
	//			lastFile = new File(lastFileName);
	//		}
	//
	//		if(lastFile != null && lastFile.exists()){
	//
	//			if(!lastFile.isDirectory()){
	//				lastFile= lastFile.getParentFile();
	//			}
	//			
	//			fileChooser.setInitialDirectory(lastFile);
	//		}
	//
	//		File selectedDirectory = fileChooser.showDialog(this.stage);
	//
	//		if(selectedDirectory!=null){
	//			File f = selectedDirectory;
	//			config.setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());	
	//			config.save();
	//		}
	//
	//		return selectedDirectory;
	//	}

	/**
	 * 
	 * @param f1 filter Title "JPG"
	 * @param f2 filter regex "*.jpg"
	 */
	private List<File> chooseFiles(String f1,String f2) {
		System.out.println(Messages.getString("JFXMain.403")); //$NON-NLS-1$
		List<File> files =null;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("JFXMain.404")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(f1, f2));

		//Configuracion config = Configuracion.getInstance();
		File lastFile = null;

		String lastFileName =config.getPropertyOrDefault(Configuracion.LAST_FILE,Messages.getString("JFXMain.405")); //$NON-NLS-1$
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 	
		try{
			System.out.println(Messages.getString("JFXMain.406")+lastFile); //$NON-NLS-1$
			//if(lastFile != null && lastFile.exists()){
			System.out.println(Messages.getString("JFXMain.407")+lastFile.getParent()); //$NON-NLS-1$
			System.out.println(Messages.getString("JFXMain.408")+lastFile.getName()); //$NON-NLS-1$
			fileChooser.setInitialDirectory(lastFile.getParentFile());
			fileChooser.setInitialFileName(lastFile.getName());
			System.out.println(Messages.getString("JFXMain.409")); //$NON-NLS-1$
			files = fileChooser.showOpenMultipleDialog(this.stage);
			System.out.println(Messages.getString("JFXMain.410")); //$NON-NLS-1$
			//		file = files.get(0);
		}catch(Exception e){
			e.printStackTrace();
			try{
			fileChooser.setInitialDirectory(null);
			files = fileChooser.showOpenMultipleDialog(this.stage);
			}catch(Exception e2){
				e2.printStackTrace();
				//give up
			}
			
		}
		System.out.println(Messages.getString("JFXMain.411")+files); //$NON-NLS-1$

		try {
		if(files!=null && files.size()>0){
			File f = files.get(0);
			config.setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());	
			config.save();
		}
		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println(Messages.getString("JFXMain.412")); //$NON-NLS-1$
		return files;
	}

	/**
	 * este metodo se usa para crear archivos shp al momento de exportar mapas
	 * @param nombre es el nombre del archivo que se desea crear
	 * @return el archivo creado en la carpeta seleccionada por el usuario
	 */
	private File getNewShapeFile(String nombre) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("JFXMain.413")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(Messages.getString("JFXMain.414"), Messages.getString("JFXMain.415"))); //$NON-NLS-1$ //$NON-NLS-2$

		File lastFile = null;
		//Configuracion config =Configuracion.getInstance();
		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 
		//if(lastFile != null && lastFile.exists()){
		fileChooser.setInitialDirectory(lastFile.getParentFile());

		if(nombre == null){
			nombre = lastFile.getName();
		}
		fileChooser.setInitialFileName(nombre);
		config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());


		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());

		File file = fileChooser.showSaveDialog(this.stage);

		System.out.println(Messages.getString("JFXMain.416")+file); //$NON-NLS-1$

		return file;
	}


	/**
	 * este metodo se usa para crear archivos shp al momento de exportar mapas
	 * @param nombre es el nombre del archivo que se desea crear
	 * @return el archivo creado en la carpeta seleccionada por el usuario
	 */
	private File getNewTiffFile(String nombre) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("JFXMain.417")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(Messages.getString("JFXMain.418"), Messages.getString("JFXMain.419"))); //$NON-NLS-1$ //$NON-NLS-2$

		File lastFile = null;
		//Configuracion config =Configuracion.getInstance();
		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 
		//if(lastFile != null && lastFile.exists()){
		fileChooser.setInitialDirectory(lastFile.getParentFile());

		if(nombre == null){
			nombre = lastFile.getName();
		}
		fileChooser.setInitialFileName(nombre);
		config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());


		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());

		File file = fileChooser.showSaveDialog(this.stage);

		System.out.println(Messages.getString("JFXMain.420")+file); //$NON-NLS-1$

		return file;
	}

	private void playSound() {
		executorPool.execute(()->{
			try	{
				URL url = JFXMain.class.getClassLoader().getResource(SOUND_FILENAME); //ok en el jar!!
				AudioInputStream ais =  AudioSystem.getAudioInputStream(url); 

				//				InputStream bufferedIn = new BufferedInputStream(getClass().getResourceAsStream(SOUND_FILENAME));
				//				AudioInputStream inputStream = AudioSystem
				//						.getAudioInputStream(bufferedIn);
				Clip clip = AudioSystem.getClip();
				clip.open(ais);
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
			//Configuracion config = Configuracion.getInstance();
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;

					//	String filePath = null;
					FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString("JFXMain.421"),Messages.getString("JFXMain.422")); //$NON-NLS-1$ //$NON-NLS-2$
					List<File> shpFiles = db.getFiles();
					shpFiles.removeIf(f->{
						return !filter.accept(f);
					});
					// update Configuracion.lasfFile
					
					if(shpFiles.size()>0){
						File lastFile = shpFiles.get(shpFiles.size()-1);
						
						config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
						config.save();
						doOpenCosecha(shpFiles);//ok!
					}



					FileNameExtensionFilter tifFilter = new FileNameExtensionFilter(Messages.getString("JFXMain.423"),Messages.getString("JFXMain.424")); //$NON-NLS-1$ //$NON-NLS-2$
					List<File> tifFiles = db.getFiles();
					tifFiles.removeIf(f->!tifFilter.accept(f));
					// update Configuracion.lasfFile
				//	Configuracion config = Configuracion.getInstance();
					if(tifFiles.size()>0){
						File lastFile = tifFiles.get(tifFiles.size()-1);
						
						config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
						config.save();
						tifFiles.stream().forEach((f)->showNdviTiffFile(f,null,null));
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
