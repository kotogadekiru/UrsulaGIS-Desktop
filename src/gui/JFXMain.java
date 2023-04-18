package gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.ordenCompra.OrdenCompra;
import dao.pulverizacion.PulverizacionLabor;
import dao.recorrida.Muestra;
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
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwindx.examples.util.ExampleUtil;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import gui.nww.LayerPanel;
import gui.nww.WWPanel;
import gui.utils.DateConverter;
import gui.utils.SmartTableView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.Property;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tasks.CompartirRecorridaTask;
import tasks.ExportLaborMapTask;
import tasks.GetNdviForLaborTask4;
import tasks.GoogleGeocodingHelper;
import tasks.ProcessMapTask;
import tasks.ShowNDVITifFileTask;
import tasks.ShowRecorridaDirigidaTask;
import tasks.UpdateTask;
import tasks.crear.ConvertirAFertilizacionTask;
import tasks.crear.ConvertirAPulverizacionTask;
import tasks.crear.ConvertirASiembraTask;
import tasks.crear.ConvertirASueloTask;
import tasks.crear.ConvertirNdviACosechaTask;
import tasks.crear.ConvertirNdviAFertilizacionTask;
import tasks.crear.ConvertirSueloACosechaTask;
import tasks.crear.GenerarOrdenCompraTask;
import tasks.importar.OpenMargenMapTask;
import tasks.importar.OpenSoilMapTask;
import tasks.importar.ProcessFertMapTask;
import tasks.importar.ProcessHarvestMapTask;
import tasks.importar.ProcessSiembraMapTask;
import tasks.procesar.CortarCosechaMapTask;
import tasks.procesar.CrearSiembraDesdeFertilizacionTask;
import tasks.procesar.ExportarCosechaDePuntosTask;
import tasks.procesar.ExportarPrescripcionFertilizacionTask;
import tasks.procesar.ExportarPrescripcionSiembraTask;
import tasks.procesar.ExportarRecorridaTask;
import tasks.procesar.ExtraerPoligonosDeLaborTask;
import tasks.procesar.GenerarRecorridaDirigidaTask;
import tasks.procesar.GrillarCosechasMapTask;
import tasks.procesar.JuntarShapefilesTask;
import tasks.procesar.ProcessBalanceDeNutrientes;
import tasks.procesar.ProcessMarginMapTask;
import tasks.procesar.RecomendFertNFromHarvestMapTask;
import tasks.procesar.RecomendFertPFromHarvestMapTask;
import tasks.procesar.SiembraFertTask;
import tasks.procesar.UnirCosechasMapTask;
import tasks.procesar.UnirFertilizacionesMapTask;
import tasks.procesar.UnirSiembrasMapTask;
import utils.DAH;
import utils.FileHelper;
import utils.TarjetaHelper;

public class JFXMain extends Application {
	private static final String PREFERED_TREE_WIDTH_KEY = "PREFERED_TREE_WIDTH";
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_ALTITUDE = "gov.nasa.worldwind.avkey.InitialAltitude"; 
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LONGITUDE = "gov.nasa.worldwind.avkey.InitialLongitude"; 
	private static final String GOV_NASA_WORLDWIND_AVKEY_INITIAL_LATITUDE = "gov.nasa.worldwind.avkey.InitialLatitude"; 
	public static Configuracion config = Configuracion.getInstance();

	public static final String VERSION = "0.2.28"; 
	public static final String TITLE_VERSION = "Ursula GIS-"+VERSION; 
	public static final String buildDate = "13/04/2023";
	public static  final String ICON ="gui/ursula_logo_2020.png";//"gui/32x32-icon-earth.png";// "gui/1-512.png";//UrsulaGIS-Desktop/src/gui/32x32-icon-earth.png 
	private static final String SOUND_FILENAME = "gui/exito4.mp3";//"gui/Alarm08.wav";//"Alarm08.wav" funciona desde eclipse pero no desde el jar  

	public static Stage stage=null;
	private Scene scene=null;

	private Dimension canvasSize = new Dimension(1500, 800);

	protected WWPanel wwjPanel=null;
	protected LayerPanel layerPanel=null;
	VBox progressBox = new VBox();

	public static ExecutorService executorPool = Executors.newCachedThreadPool();
	private Node wwNode=null;//contiene el arbol con los layers y el swingnode con el world wind
	private boolean isPlayingSound=false;
	public PoligonoGUIController poligonoGUIController= new PoligonoGUIController(this);;
	public PulverizacionGUIController pulverizacionGUIController = new PulverizacionGUIController(this);
	public ConfigGUI configGUIController = new ConfigGUI(this);
	
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
					System.out.println("em Closed"); 
					System.out.println("Application Closed by click to Close Button(X)"); 
					getWwd().shutdown();
					System.exit(0); 
				});
			});
			primaryStage.show();

			//start clearCache cronJob
			startClearCacheCronJob();
		}catch(Exception e) {
			System.out.println("no se pudo hascer start de JFXMain.start(stage)");
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
		try {//com.sun.java.swing.plaf.windows.WindowsLookAndFeel
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");//UIManager.getSystemLookAndFeelClassName()); 
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		}

		//setDefaultSize(50);//esto funciona para la barra de abajo pero no para los placemarks
		try {
			// Create the WorldWindow.
			this.wwjPanel =	new WWPanel(canvasSize, true);
		}catch(Exception e) {
			Platform.runLater(()->{
				Alert a = new Alert(Alert.AlertType.ERROR);
				a.setHeaderText("No se pudo crear WorldWindow");
				String stackTrace = Arrays.toString(e.getStackTrace());
				a.setContentText(stackTrace);
				a.show();
			});
		}
		//una vez que se establecio el tamaño inicial ese es el tamaño maximo
		//this.wwjPanel.setPreferredSize(canvasSize);
		final SwingNode wwSwingNode = new SwingNode();
		wwSwingNode.setContent(wwjPanel);
		
		// Put the pieces together.
		//wwSwingNode.autosize();
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

	public static void setDefaultSize(int size) {
		Set<Object> keySet = UIManager.getLookAndFeelDefaults().keySet();
		//Object[] keys = keySet.toArray(new Object[keySet.size()]);
		keySet.forEach(key->{
			if (key != null && key.toString().toLowerCase().contains("font")) {
				Font font = UIManager.getDefaults().getFont(key);
				if (font != null) {
					font = font.deriveFont((float)size);
					UIManager.put(key, font);
				}
			}
		});
	}

	private MenuBar constructMenuBar() {		
		/*Menu Importar*/
		final Menu menuImportar = new Menu(Messages.getString("JFXMain.importar")); 
		addMenuItem(Messages.getString("JFXMain.NDVI"),(a)->doOpenNDVITiffFiles(),menuImportar); 
		addMenuItem(Messages.getString("JFXMain.imagen"),(a)->importImagery(),menuImportar); 
		addMenuItem(Messages.getString("JFXMain.suelo"),(a)->doOpenSoilMap(null),menuImportar); 
		addMenuItem(Messages.getString("JFXMain.margen"),(a)->doOpenMarginMap(),menuImportar); 
		addMenuItem(Messages.getString("JFXMain.poligonos"),(a)->poligonoGUIController.doImportarPoligonos(null),menuImportar); 
		/*Menu herramientas*/
		final Menu menuHerramientas = new Menu(Messages.getString("JFXMain.herramientas")); 		
		addMenuItem(Messages.getString("JFXMain.distancia"),(a)->poligonoGUIController.doMedirDistancia(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.superficie"),(a)->poligonoGUIController.doCrearPoligono(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.unirShapes"),(a)->doJuntarShapefiles(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.rentabilidad"),(a)->doProcessMargin(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.balanceNutrientes"),(a)->doProcesarBalanceNutrientes(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.generarOrdenCompra"),(a)->doGenerarOrdenDeCompra(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.goTo"),(a)->showGoToDialog(),menuHerramientas);
		addMenuItem(Messages.getString("JFXMain.bulk_ndvi_download"),(a)->{	doBulkNDVIDownload();},menuHerramientas);
		/*Menu Exportar*/
		final Menu menuExportar = new Menu(Messages.getString("JFXMain.exportar"));		 
		addMenuItem(Messages.getString("JFXMain.exportarPantallaMenuItem"),(a)->doSnapshot(),menuExportar);
		/*Menu Configuracion*/
		final Menu menuConfiguracion = configGUIController.contructConfigMenu();

		MenuItem actualizarMI=addMenuItem(Messages.getString("JFXMain.configUpdate"),null,menuConfiguracion); 
		actualizarMI.setOnAction((a)->doUpdate(actualizarMI));
		actualizarMI.setVisible(false);

		executorPool.submit(()->{
			if(UpdateTask.isUpdateAvailable()){
				actualizarMI.setVisible(true);
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

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuImportar,menuHerramientas, menuExportar,menuConfiguracion);
		menuBar.setPrefWidth(scene.getWidth());
		return menuBar;
	}
	
	private void addFertilizacionesRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();
		rootNodeP.add(new LayerAction((layer)->{
			doOpenFertMap(null);
			return "opened";	
		},Messages.getString("JFXMain.importar")));
 
		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.unirFertilizaciones"),(layer)->{
			doUnirFertilizaciones();
			return "unidas";	
		},2));
		layerPanel.addAccionesClase(rootNodeP,FertilizacionLabor.class);
	}

	private void addSiembrasRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();
		rootNodeP.add(new LayerAction((layer)->{
			this.doOpenSiembraMap(null);
			return "opened";	
		},Messages.getString("JFXMain.importar")));

		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.unir"),(layer)->{
			this.doUnirSiembras(null);
			return "joined";	
		},2));

		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.generarSiembraFert"),(layer)->{
			this.doGenerarSiembraFertilizada();
			return "generated";	
		},1));

		layerPanel.addAccionesClase(rootNodeP,SiembraLabor.class);
	}

	private void addCosechasRootNodeActions() {
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


		layerPanel.addAccionesClase(rootNodeP,CosechaLabor.class);
	}
	private void addNdviRootNodeActions() {
		List<LayerAction> rootNodeNDVI = new ArrayList<LayerAction>();

		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.evoNDVI"),(layer)->{ 
			ShowNDVIEvolution sEvo= new ShowNDVIEvolution(this.getWwd(),this.layerPanel);
			sEvo.doShowNDVIEvolution();
			return "mostre la evolucion del ndvi";
		}));

		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.show_ndvi_chart"),
				(layer)->{ 
			Platform.runLater(()->{
				NDVIChart sChart= new NDVIChart(this.getWwd());
				sChart.doShowNDVIChart(false);
				Stage histoStage = new Stage();
				histoStage.setTitle(Messages.getString("JFXMain.show_ndvi_chart"));
				histoStage.getIcons().add(new Image(ICON));
				VBox.setVgrow(sChart, Priority.ALWAYS);
				Scene scene = new Scene(sChart, 800,450);
				histoStage.setScene(scene);
				histoStage.initOwner(JFXMain.stage);
				histoStage.show();
			});
			return "mostre el grafico del ndvi";
		}));

		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.show_ndvi_acum_chart"),
				(layer)->{
			Platform.runLater(()->{
				NDVIChart sChart= new NDVIChart(this.getWwd());
				sChart.doShowNDVIChart(true);
				Stage histoStage = new Stage();
				histoStage.setTitle(Messages.getString("JFXMain.show_ndvi_acum_chart"));
				histoStage.getIcons().add(new Image(ICON));
				VBox.setVgrow(sChart, Priority.ALWAYS);
				Scene scene = new Scene(sChart, 800,450);
				histoStage.setScene(scene);
				histoStage.initOwner(JFXMain.stage);
				histoStage.show();
			});
			return "mostre el grafico del ndvi";
		}));


		//Exporta todos los ndvi cargados a un archivo excel donde las filas son las coordenadas y las columnas son los valores en esa fecha
		rootNodeNDVI.add(LayerAction.constructPredicate(Messages.getString("JFXMain.expoNDVI"),(layer)->{ 
			ExportNDVIToExcel sEvo= new ExportNDVIToExcel(this.getWwd(),this.layerPanel);
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
							LayerList layers = this.getWwd().getModel().getLayers();
							for (Layer l : layers) {
								Object o =  l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
								if(o instanceof Ndvi){
									Ndvi ndvi = (Ndvi)o;							
									DAH.save(ndvi);
								}
							}
						}catch(Exception e) {
							System.err.println("Error al guardar los poligonos"); 
							e.printStackTrace();
						}
					});
					return "Guarde los poligonos"; 
				}));
		layerPanel.addAccionesClase(rootNodeNDVI,Ndvi.class);
	}

	/**
	 * aca se configuran los menues contextuales del arbol de capas
	 //XXX agregar nuevas funcionalidades aca!!! 
	 */
	private void setAccionesTreePanel() {		
		pulverizacionGUIController.addPulverizacionesRootNodeActions();
		addFertilizacionesRootNodeActions();
		addSiembrasRootNodeActions();
		addCosechasRootNodeActions();
		poligonoGUIController.addPoligonosRootNodeActions();
		addNdviRootNodeActions();

		Map<Class<?>,List<LayerAction>> predicates = new HashMap<Class<?>,List<LayerAction>>();

		addAccionesCosecha(predicates);
		addAccionesFertilizacion(predicates);
		addAccionesSiembras(predicates);	
		pulverizacionGUIController.addAccionesPulverizaciones(predicates);

		poligonoGUIController.addAccionesPoligonos(predicates);
		poligonoGUIController.addAccionesCaminos(predicates);
		
		addAccionesRecorridas(predicates);
		addAccionesNdvi(predicates);

		addAccionesMargen(predicates);
		addAccionesSuelos(predicates);		

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
	}

	private void addAccionesSiembras(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> siembrasP = new ArrayList<LayerAction>();
		predicates.put(SiembraLabor.class, siembrasP);
		/**
		 *Accion que permite editar una siembra
		 */
		siembrasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editSiembraAction"),(layer)->{
			doEditSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "siembra editada" + layer.getName(); 
		}));

		/**
		 * Accion permite exportar la prescripcion de siembra
		 */
		siembrasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportarSiembraAction"),(layer)->{
			doExportPrescripcionSiembra((SiembraLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "prescripcion Exportada" + layer.getName(); 
		}));
	}

	private void addAccionesFertilizacion(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> fertilizacionesP = new ArrayList<LayerAction>();
		predicates.put(FertilizacionLabor.class, fertilizacionesP);
		/**
		 *Accion que permite ediytar una fertilizacion
		 */
		fertilizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editFertAction"),(layer)->{			
			doEditFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "fertilizacion editada" + layer.getName(); 
		}));

		/**
		 * Accion permite exportar la labor como shp
		 */
		fertilizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportarFertPAction"),(layer)->{			
			doExportPrescripcionFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "labor Exportada" + layer.getName(); 
		}));

		fertilizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.generarSiembraDeFertAction"),(layer)->{			
			doGenerarSiembraDesdeFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "labor siembraFertilziada Exportada" + layer.getName(); 
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
	}

	private void addAccionesSuelos(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> suelosP = new ArrayList<LayerAction>();
		predicates.put(Suelo.class, suelosP);
		suelosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editSoilAction"),(layer)->{	
			doEditSuelo((Suelo) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "suelo editado" + layer.getName(); 
		}));
		
		//TODO implementar estimar potencial de rendimiento desde suelo
		suelosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.estimarPotencialRendimiento"),(layer)->{	
			doEstimarPotencialRendimiento((Suelo) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "potencial de rendimiento suelo estimado" + layer.getName(); 
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
			doExtraerPoligonos((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "poligonos Extraidos " + layer.getName(); 
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
				doGetNdviTiffFile(o);
			}
			return "ndvi obtenido" + layer.getName();	 
		}));
	}

	private void addAccionesRecorridas(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> recorridasP = new ArrayList<LayerAction>();
		predicates.put(Recorrida.class, recorridasP);

		//editar Recorrida
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editarLayer"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;				
				configGUIController.doShowRecorridaTable(Collections.singletonList(recorrida));
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

	private List<LayerAction> addAccionesCosecha(
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
			showAmountVsElevacionChart((Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
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
			doCrearSiembra((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
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

	private void addAccionesNdvi(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> ndviP = new ArrayList<LayerAction>();
		predicates.put(Ndvi.class, ndviP);

		ndviP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editarLayer"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Ndvi.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Ndvi ndvi =(Ndvi)layerObject;
				TextInputDialog nombreDialog = new TextInputDialog(ndvi.getNombre());
				nombreDialog.initOwner(stage);
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
				ExportNDVIToKMZ toKMZ= new ExportNDVIToKMZ(this.getWwd(),this.layerPanel);				
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

	private void enDesarrollo() {
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

	/**
	 * metodo que toma las labores activas de siembra fertilizacion y pulverizacion y hace una lista con los insumos y cantidades para
	 * cotizar precios online. Permite exporta a excel y cotizar precios online y guardar
	 */
	private void doGenerarOrdenDeCompra() {
		GenerarOrdenCompraTask gOCTask = new GenerarOrdenCompraTask(
														getSiembrasSeleccionadas(),
														getFertilizacionesSeleccionadas(),
														getPulverizacionesSeleccionadas());
		gOCTask.installProgressBar(progressBox);
		gOCTask.setOnSucceeded(handler -> {
			OrdenCompra ret = (OrdenCompra)handler.getSource().getValue();
			gOCTask.uninstallProgressBar();
			playSound();
			configGUIController.doShowOrdenCompraItems(ret);
			System.out.println("SiembraFertTask succeded"); 
		});
		executorPool.execute(gOCTask);
	}

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

	private void doGenerarSiembraFertilizada() {
		SiembraLabor siembraEnabled = getSiembrasSeleccionadas().get(0);
		FertilizacionLabor fertEnabled = getFertilizacionesSeleccionadas().get(0);

		boolean esFertLinea=true;
		Alert selectTipoFert = new Alert(Alert.AlertType.CONFIRMATION);
		selectTipoFert.initOwner(JFXMain.stage);
		selectTipoFert.setTitle("Seleccione tipo fertilizacion");
		selectTipoFert.setContentText("Seleccione OK si es fertilizacion en la linea");
		Optional<ButtonType> esFertLineaOP = selectTipoFert.showAndWait();
		if(!esFertLineaOP.isPresent()) {
			return;
		} else if(!esFertLineaOP.get().equals(ButtonType.OK)){
			esFertLinea=false;
		}		
		SiembraFertTask siembraFertTask = new SiembraFertTask(siembraEnabled, fertEnabled,esFertLinea);
		siembraFertTask.installProgressBar(progressBox);
		siembraFertTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			siembraFertTask.uninstallProgressBar();
			siembraEnabled.getLayer().setEnabled(false);
			fertEnabled.getLayer().setEnabled(false);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println("SiembraFertTask succeded"); 
		});
		executorPool.execute(siembraFertTask);
	}

	private void doUpdate(MenuItem actualizarMI) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setContentText(Messages.getString("JFXMain.doUpdateText")); 
		alert.initOwner(JFXMain.stage);
		alert.showAndWait();
		if(ButtonType.OK.equals(alert.getResult())){
			UpdateTask uTask = new UpdateTask();
			uTask.installProgressBar(progressBox);
			uTask.setOnSucceeded(handler -> {
				File newVersion = (File) handler.getSource().getValue();	
				if(newVersion==null){
					Alert error = new Alert(Alert.AlertType.ERROR);
					error.setContentText(Messages.getString("JFXMain.doUpdateErrorText")); 
					error.initOwner(JFXMain.stage);
					error.showAndWait();
				} else{
					Alert error = new Alert(Alert.AlertType.CONFIRMATION);
					error.setContentText(Messages.getString("JFXMain.doUpdateSuccessText")); 
					error.initOwner(JFXMain.stage);
					error.showAndWait();
				}			
				uTask.uninstallProgressBar();
			});
			executorPool.submit(uTask);
		}//fin del if OK	
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
		List<Poligono> poligonos = DAH.getPoligonosActivos();
		this.poligonoGUIController.showPoligonos(poligonos);

		List<Ndvi> ndviActivos = DAH.getNdviActivos();
		for(int i=0;i<ndviActivos.size();i++) {
			Ndvi ndvi = ndviActivos.get(i);
			boolean isLast = i==(ndviActivos.size()-1);

			if(ndvi!=null)System.out.println("showing ndvi "+ndvi.getNombre());
			ShowNDVITifFileTask task = new ShowNDVITifFileTask(ndvi);

			task.setOnSucceeded(handler -> {
				Layer ndviLayer = (Layer) handler.getSource().getValue();	
				if(ndviLayer != null) {
					insertBeforeCompass(getWwd(), ndviLayer);
					this.getLayerPanel().update(this.getWwd());
					if(isLast) {//solo hago centro y sonido en el ultimo
						viewGoTo(ndviLayer);
						playSound();
					}
				}
			});
			executorPool.execute(task);
		}
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
	
	private void showGoToDialog() {
		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.goToExample")); 
		anchoDialog.setTitle(Messages.getString("JFXMain.goToDialogTitle")); 
		anchoDialog.setHeaderText(Messages.getString("JFXMain.goToDialogHeader")); 
		anchoDialog.initOwner(JFXMain.stage);
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		if(anchoOptional.isPresent()){
			Position pos = GoogleGeocodingHelper.obtenerPositionDirect(anchoOptional.get());
			if(pos!=null){
				viewGoTo(pos);
			}				
		} else{
			return;
		}
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

	private void showAmountVsElevacionChart(Labor<?> cosechaLabor) {
		TextInputDialog anchoDialog = new TextInputDialog("20"); 
		anchoDialog.setTitle(Messages.getString("JFXMain.heightVsAmountDialogTitle")); 
		anchoDialog.setContentText(Messages.getString("JFXMain.heightVsAmountDialogMaxGroupsText")); 
		anchoDialog.initOwner(JFXMain.stage);
		Optional<String> oGrupos = anchoDialog.showAndWait();
		int grupos=Integer.parseInt(oGrupos.get());
		Labor<?>[] cosechasAux = new Labor[]{cosechaLabor};
		if(cosechaLabor==null){
			Optional<CosechaLabor> optional = HarvestSelectDialogController.select(getCosechasSeleccionadas());
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
				histoStage.setTitle(Messages.getString("JFXMain.heightVsAmountChartTitle")); 
				histoStage.getIcons().add(new Image(ICON));

				Scene scene = new Scene(histoChart, 800,450);
				histoStage.setScene(scene);
				System.out.println("termine de crear el grafico rinde vs altura"); 
				histoStage.initOwner(JFXMain.stage);
				histoStage.show();
				System.out.println("histoChart.show();"); 
			}else{
				Alert error = new Alert(AlertType.ERROR);
				error.setTitle(Messages.getString("JFXMain.heightVsAmountErrorTitle")); 
				error.setContentText(Messages.getString("JFXMain.heightVsAmountErrorText")); 
				error.show();
			}
		});
		executorPool.execute(pfMapTask);
	}

	/**
	 * metodo que muestra una tabla con poligonos que se pueden seleccionar para descargar el valor de los ndvi
	 * de cada uno dentro de un periodo determinado
	 */
	private void doBulkNDVIDownload() {
		BulkNdviDownloadGUI gui = new BulkNdviDownloadGUI();
		gui.show();
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
			histoStage.getIcons().add(new Image(ICON));
			histoStage.setTitle(Messages.getString("NDVIHistoChart.Title"));
			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			histoStage.initOwner(JFXMain.stage);
			histoStage.show();
		});
		executorPool.submit(pfMapTask);
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

	/**
	 *  updload recorrida to server and show url to access
	 * @param recorrida
	 */
	private void doCompartirRecorrida(Recorrida recorrida) {		
		if(recorrida.getUrl()!=null && recorrida.getUrl().length()>0) {			
			configGUIController.showQR(recorrida.getUrl());
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
				configGUIController.showQR(ret);
			}
			//XXX agregar boton de actualizar desde la nube?
			task.uninstallProgressBar();			
		});
		System.out.println("ejecutando Compartir Recorrida"); 
		executorPool.submit(task);
	}

	// junta las muestras con mismo nombre y permite completar los datos de las objervaciones
	private void doAsignarValoresRecorrida(Recorrida recorrida) {
		configGUIController.doAsignarValoresRecorrida(recorrida);
	}

	public void doShowRecorrida(Recorrida recorrida) {
		ShowRecorridaDirigidaTask umTask = new ShowRecorridaDirigidaTask(recorrida);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			RenderableLayer ret = (RenderableLayer)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret);
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);	}

	/**
	 * descargar los tiff correspondientes a un polygono y mostrarlos como ndvi
	 * @param placementObject
	 */
	@SuppressWarnings("unchecked")
	private void doGetNdviTiffFile(Object placementObject) {//ndvi2
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
				System.out.println(Messages.getString("JFXMain.216")); 
				if(c.next()){
					c.getAddedSubList().forEach((ndvi)->{
						doShowNDVI(ndvi);
					});//fin del foreach
				}			
			});
			if(placementObject !=null && Labor.class.isAssignableFrom(placementObject.getClass())){
				Labor<?> l =(Labor<?>)placementObject;
				
				placementObject =  l.getContorno();
//				ReferencedEnvelope bounds =
//				Polygon pol = GeometryHelper.constructPolygon(bounds);
//				placementObject =GeometryHelper.constructPoligono(pol);
				
			} 
			GetNdviForLaborTask4 task = new GetNdviForLaborTask4((Poligono)placementObject, observableList);
			task.setBeginDate(ndviDpDLG.initialDate);
			task.setFinDate(ndviDpDLG.finalDate);
			task.setIgnoreNDVI((List<Ndvi>) getObjectFromLayersOfClass(Ndvi.class));

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
	 * tomar un Ndvi y mostrarlo como layer
	 * @param ndvi
	 */
	public void doShowNDVI(Ndvi ndvi) {
		executorPool.submit(()->{			
			showNdvi(null,ndvi);
		});
	}

	/**
	 * seleccionar archivos .tif y mostrarlos como Ndvi
	 */
	private void doOpenNDVITiffFiles() {
		List<File>	files =FileHelper.chooseFiles("TIF","*.tif");  
		if(files!=null)	files.forEach((file)->{
			showNdviTiffFile(file,null);
		});//fin del foreach
	}
	
	private void showNdvi( Object placementObject,Ndvi _ndvi) {
		if(_ndvi!=null)System.out.println("showing ndvi "+_ndvi.getNombre());
		ShowNDVITifFileTask task = new ShowNDVITifFileTask(_ndvi);
		if( placementObject!=null && Poligono.class.isAssignableFrom(placementObject.getClass())){
			task.setPoligono((Poligono) placementObject);
		} else 	if( placementObject!=null && Labor.class.isAssignableFrom(placementObject.getClass())){
			task.setPoligono(((Labor<?>) placementObject).getContorno());
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

	private void showNdviTiffFile(File file, Object placementObject) {
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

	protected void importImagery()  {
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

			insertBeforeCompass(getWwd(), ret);
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}
	
	private void doConvertirNdviACosecha(Ndvi ndvi) {
		CosechaLabor labor = new CosechaLabor();
		labor.setNombre(ndvi.getNombre());

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

		ConvertirNdviACosechaTask umTask = new ConvertirNdviACosechaTask(labor,ndvi,rinde);
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
				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.279")); 
				playSound();
				viewGoTo(ret);
			});
			pmtask.run();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
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
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println("convertir a fertiliacion tuvo exito"); 
			playSound();
			ndvi.getLayer().setEnabled(false);
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}


	private void doGenerarSiembraDesdeFertilizacion(FertilizacionLabor fertilizacionLabor) {
		SiembraLabor labor = new SiembraLabor();
		LaborLayer layer = new LaborLayer();
		boolean directa = true;

		labor.setLayer(layer);
		labor.setNombre(fertilizacionLabor.getNombre()+" "+Messages.getString("JFXMain.255"));  
		Optional<SiembraLabor> siembraConfigured= SiembraConfigDialogController.config(labor);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); 
			labor.dispose();//libero los recursos reservados
			return;
		}		
		
		//Dialogo preguntar min y max a aplicar y dosis
		Alert minMaxDialog = new Alert(AlertType.CONFIRMATION);	
		
		NumberFormat df=Messages.getNumberFormat();
		TextField dc = new TextField(df.format(0));
		TextField min = new TextField(df.format(0));
		TextField max = new TextField(df.format(0));

		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.425")),dc)); 
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.426")),min)); 
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.427")),max)); 

		minMaxDialog.setGraphic(vb);
		minMaxDialog.setTitle(Messages.getString("JFXMain.303")); 
		minMaxDialog.setContentText(Messages.getString("JFXMain.304")); 
		//dateDialog.setHeaderText("Fecha Desde");
		minMaxDialog.initOwner(JFXMain.stage);
		Optional<ButtonType> res = minMaxDialog.showAndWait();
		Double minSem =null,maxSem=null, dosisC=null; 
		if(res.get().equals(ButtonType.OK)){
			try {
				dosisC=df.parse(dc.getText()).doubleValue();
				if(dosisC==0)dosisC=(double) 0;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			try {
				minSem=df.parse(min.getText()).doubleValue();
				if(minSem==0)minSem=(double) 0;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			try {
				maxSem=df.parse(max.getText()).doubleValue();
				if(maxSem<=0)maxSem=(double) 0;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

		double dosisXha = dosisC;
		double dosisMin = minSem;
		double dosisMax = maxSem;

		//con esto se recibe la relacion si es directa o indirecta
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Tipo de Relacion");
		alert.setHeaderText("Confirmar Relacion Directa");
		alert.setContentText("Seleccione OK para una relacion directa");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK){
			directa = true;
		} else {
			directa = false;		
		}

		CrearSiembraDesdeFertilizacionTask siembraFert = new CrearSiembraDesdeFertilizacionTask(labor, fertilizacionLabor, dosisXha,dosisMin,dosisMax,directa);
		siembraFert.installProgressBar(progressBox);

		siembraFert.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			siembraFert.uninstallProgressBar();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println("SiembraFertTask succeded"); 
		});
		executorPool.execute(siembraFert);
	}


	private void doEditCosecha(CosechaLabor cConfigured ) {
		if(cConfigured==null){
			Optional<CosechaLabor> cosechaSelected = HarvestSelectDialogController.select(getCosechasSeleccionadas());
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
				System.out.println(Messages.getString("JFXMain.279")); 
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
			this.poligonoGUIController.showPoligonos(poligonos);			
			umTask.uninstallProgressBar();
			this.wwjPanel.repaint();
			System.out.println(Messages.getString("JFXMain.280")); 
			playSound();
		});//fin del OnSucceeded						
		JFXMain.executorPool.execute(umTask);
	}

	//TODO tomar un suelo y una configuracion de cosecha 
	//y crear un mapa de potencial de rendimiento segun el agua en el perfil
	private void doEstimarPotencialRendimiento(Suelo suelo) {
		this.enDesarrollo();//XXX remover enDesarrollo cuando este terminado

		CosechaLabor labor = new CosechaLabor();
		labor.setNombre(suelo.getNombre());

		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			//System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha"); 
			labor.dispose();//libero los recursos reservados
			return;
		}					
		Double mmLluvia = null;
		try {
			Double rindeEsperado = cosechaConfigured.get().getCultivo().getAbsAgua();
			TextInputDialog rindePromDialog = new TextInputDialog(Messages.getNumberFormat().format(rindeEsperado));//Messages.getString("JFXMain.272")); 
			rindePromDialog.setTitle(Messages.getString("LluviaCampania")); 
			rindePromDialog.setContentText(Messages.getString("LluviaCampania")); 
			rindePromDialog.initOwner(JFXMain.stage);
			Optional<String> rPromOptional = rindePromDialog.showAndWait();
			mmLluvia = PropertyHelper.parseDouble(rPromOptional.get()).doubleValue();//Double.valueOf(anchoOptional.get());
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

		ConvertirSueloACosechaTask umTask = new ConvertirSueloACosechaTask(labor,suelo,mmLluvia);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			insertBeforeCompass(getWwd(), ret.getLayer());
			umTask.uninstallProgressBar();
			suelo.getLayer().setEnabled(false);

			ProcessHarvestMapTask pmtask = new ProcessHarvestMapTask(ret);
			pmtask.installProgressBar(progressBox);
			pmtask.setOnSucceeded(handler2 -> {
				this.getLayerPanel().update(this.getWwd());
				pmtask.uninstallProgressBar();

				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.279")); 
				playSound();
				viewGoTo(ret);
			});
			pmtask.run();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}
	
	private void doEditSuelo(Suelo cConfigured) {			
		Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			OpenSoilMapTask umTask = new OpenSoilMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				this.wwjPanel.repaint();
				playSound();
			});//fin del OnSucceeded
			JFXMain.executorPool.execute(umTask);
		}
	}
	
	private void doEditSiembra(SiembraLabor cConfigured ) {
		Optional<SiembraLabor> cosechaConfigured=SiembraConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			ProcessSiembraMapTask umTask = new ProcessSiembraMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.282")); 
				playSound();
			});//fin del OnSucceeded						
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
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.283")); 
				playSound();
			});//fin del OnSucceeded						
			JFXMain.executorPool.execute(umTask);
		}
	}

	// junta 2 o mas cosechas en una 
	private void doUnirSiembras(SiembraLabor siembraLabor) {
		List<SiembraLabor> siemrbasAUnir = new ArrayList<SiembraLabor>();
		if(siembraLabor == null){
			List<SiembraLabor> cosechasEnabled = getSiembrasSeleccionadas();
			siemrbasAUnir.addAll( cosechasEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {
			siemrbasAUnir.add(siembraLabor);
		}

		UnirSiembrasMapTask umTask = new UnirSiembrasMapTask(siemrbasAUnir);
		umTask.installProgressBar(progressBox);
		umTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
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

	private void doCortarCosecha(CosechaLabor cosechaAcortar) {
		List<Poligono> geometriasActivas = this.poligonoGUIController.getEnabledPoligonos();
		
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



	// junta 2 o mas cosechas en una 
	private void doUnirCosechas(CosechaLabor cosechaLabor) {
		List<CosechaLabor> cosechasAUnir = new ArrayList<CosechaLabor>();
		if(cosechaLabor == null){
			List<CosechaLabor> cosechasEnabled = getCosechasSeleccionadas();
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

	private void doUnirFertilizaciones() {
		List<FertilizacionLabor> fertilizacionesAUnir = getFertilizacionesSeleccionadas();//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		UnirFertilizacionesMapTask umTask = new UnirFertilizacionesMapTask(fertilizacionesAUnir);
		umTask.installProgressBar(progressBox);
		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
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

	private void doGrillarCosechas(CosechaLabor cosechaAGrillar) {
		List<CosechaLabor> cosechasAUnir = new ArrayList<CosechaLabor>();
		if(cosechaAGrillar == null){
			List<CosechaLabor> cosechasEnabled = getCosechasSeleccionadas();
			cosechasAUnir.addAll( cosechasEnabled);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {
			cosechasAUnir.add(cosechaAGrillar);
		}
		TextInputDialog anchoDialog = new TextInputDialog(config.getPropertyOrDefault(CosechaConfig.ANCHO_GRILLA_KEY,Messages.getString("JFXMain.288"))); 
		anchoDialog.initOwner(JFXMain.stage);
		anchoDialog.setTitle(Messages.getString("JFXMain.289")); 
		anchoDialog.setContentText(Messages.getString("JFXMain.290")); 
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		if(anchoOptional.isPresent()){
			config.loadProperties();
			config.setProperty(CosechaConfig.ANCHO_GRILLA_KEY,anchoOptional.get());
			config.save();
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

	private void doGuardarLabor(Labor<?> labor) {
		File zipFile = FileHelper.zipLaborToTmpDir(labor);//ok funciona
		byte[] byteArray = FileHelper.fileToByteArray(zipFile);		
		labor.setContent(byteArray);
		DAH.save(labor);

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
		List<Recorrida> recorridasActivas = getRecorridasActivas();
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
			configGUIController.doAsignarValoresRecorrida(recorrida);//esto guarda una recorrida nueva
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
	private void doCrearSiembra(CosechaLabor cosecha) {
		SiembraLabor siembra = new SiembraLabor();
		LaborLayer layer = new LaborLayer();
		siembra.setLayer(layer);
		siembra.setNombre(cosecha.getNombre()+" "+Messages.getString("JFXMain.255"));  
		Optional<SiembraLabor> siembraConfigured= SiembraConfigDialogController.config(siembra);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); 
			siembra.dispose();//libero los recursos reservados
			return;
		}		
		String[] columnas = new String[]{
				Messages.getString("JFXMain.Densidad"),
				Messages.getString("JFXMain.FertL"),
				Messages.getString("JFXMain.FertC")
				};
		Map<String,Double[]> mapClaseValor = configGUIController.doAsignarValoresCosecha(cosecha,columnas);//"Densidad pl/m2"
		ConvertirASiembraTask csTask = new ConvertirASiembraTask(cosecha,siembra,mapClaseValor);
		csTask.installProgressBar(progressBox);
		csTask.setOnSucceeded(handler -> {
			cosecha.getLayer().setEnabled(false);
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
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
		Map<String,Double[]> mapClaseValor = configGUIController.doAsignarValoresCosecha(cosecha,new String[] {Messages.getString("JFXMain.Dosis")});//"Densidad pl/m2"
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
		Map<String,Double[]> mapClaseValor = configGUIController.doAsignarValoresCosecha(cosecha,new String[] {Messages.getString("JFXMain.Dosis")});//"Densidad pl/m2"
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

	/**
	 * genera un layer de fertilizacion a partir de una cosecha
	 * el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
	 * que producto aplico y en que densidad por hectarea
	 * @param cosecha
	 */
	private void doRecomendFertNFromHarvest(CosechaLabor cosecha) {
		List<Suelo> suelosEnabled = getSuelosSeleccionados();
		List<FertilizacionLabor> fertEnabled = getFertilizacionesSeleccionadas();

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

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	private void doOpenCosecha(List<File> files) {
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

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	private void doOpenFertMap(List<File> files) {
		List<FileDataStore> stores =FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				FertilizacionLabor labor = new FertilizacionLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.308")); 
					continue;
				}							

				ProcessFertMapTask umTask = new ProcessFertMapTask(labor);
				umTask.installProgressBar(progressBox);

				umTask.setOnSucceeded(handler -> {
					FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println(Messages.getString("JFXMain.309")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}

	private void doOpenSiembraMap(List<File> files) {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				SiembraLabor labor = new SiembraLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<SiembraLabor> cosechaConfigured= SiembraConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.310")); 
					continue;
				}							

				ProcessSiembraMapTask umTask = new ProcessSiembraMapTask(labor);
				umTask.installProgressBar(progressBox);
				umTask.setOnSucceeded(handler -> {
					SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println(Messages.getString("JFXMain.311")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}

	private void doOpenSoilMap(List<File> files) {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Suelo labor = new Suelo(store);
				labor.setLayer(new LaborLayer());
				Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.315")); 
					continue;
				}							

				OpenSoilMapTask umTask = new OpenSoilMapTask(labor);
				umTask.installProgressBar(progressBox);

				umTask.setOnSucceeded(handler -> {
					Suelo ret = (Suelo)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println(Messages.getString("JFXMain.316")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}

	private void doOpenMarginMap() {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(null);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Margen labor = new Margen(store);
				labor.setLayer(new LaborLayer());
				Optional<Margen> cosechaConfigured= MargenConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.317")); 
					continue;
				}							

				OpenMargenMapTask umTask = new OpenMargenMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					Margen ret = (Margen)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);
					System.out.println(Messages.getString("JFXMain.318")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}


	private void doJuntarShapefiles() {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(null);
		File shapeFile = FileHelper.getNewShapeFile("union");
		executorPool.execute(()->JuntarShapefilesTask.process(stores,shapeFile));
	}

	private void doProcessMargin() {		
		System.out.println(Messages.getString("JFXMain.319")); 

		Margen margen = new Margen();
		margen.setLayer(new LaborLayer());

		//todo pasar el filtrado por visibles aca y pasar nuevas listas solo con las visibles
		List<PulverizacionLabor> pulvEnabled = getPulverizacionesSeleccionadas();//pulverizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<FertilizacionLabor> fertEnabled = getFertilizacionesSeleccionadas();//fertilizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<SiembraLabor> siemEnabled = getSiembrasSeleccionadas();//t());
		List<CosechaLabor> cosechasEnabled = getCosechasSeleccionadas();//cosechas.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());

		margen.setFertilizaciones(fertEnabled);
		margen.setPulverizaciones(pulvEnabled);
		margen.setSiembras(siemEnabled);
		margen.setCosechas(cosechasEnabled);

		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("JFXMain.320")); 
		cosechasEnabled.forEach((c)->sb.append(c.getNombre()+Messages.getString("JFXMain.321"))); 
		margen.setNombre(sb.toString());

		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.322")); 
			return;
		}							

		ProcessMarginMapTask uMmTask = new ProcessMarginMapTask(margen);

		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			Margen ret = (Margen)handler.getSource().getValue();
			uMmTask.uninstallProgressBar();			
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			playSound();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.323")); 
		});
		executorPool.execute(uMmTask);
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

	private void doProcesarBalanceNutrientes() {		
		System.out.println(Messages.getString("JFXMain.327")); 
		//todo pasar el filtrado por visibles aca y pasar nuevas listas solo con las visibles
		List<Suelo> suelosEnabled = getSuelosSeleccionados();
		List<FertilizacionLabor> fertEnabled = getFertilizacionesSeleccionadas();		
		List<CosechaLabor> cosechasEnabled = getCosechasSeleccionadas();//cosechas.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());

		ProcessBalanceDeNutrientes balanceNutrientesTask = 
				new ProcessBalanceDeNutrientes(suelosEnabled,
											   cosechasEnabled,
											   fertEnabled);

		balanceNutrientesTask.installProgressBar(progressBox);

		balanceNutrientesTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			balanceNutrientesTask.uninstallProgressBar();
			suelosEnabled.stream().forEach(l->l.getLayer().setEnabled(false));
			cosechasEnabled.stream().forEach(l->l.getLayer().setEnabled(false));
			fertEnabled.stream().forEach(l->l.getLayer().setEnabled(false));

			//this.suelos.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.328")); 
		});
		executorPool.execute(balanceNutrientesTask);
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


	//permitir al ususario definir el formato. para siembra fertilizada necesita 3 columnas
	//en la linea, al costado de la linea, siembra
	private void doExportPrescripcionFertilizacion(FertilizacionLabor laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile =  FileHelper.getNewShapeFile(nombre);

		ExportarPrescripcionFertilizacionTask ept = new ExportarPrescripcionFertilizacionTask(laborToExport, shapeFile); 
		ept.installProgressBar(progressBox);

		ept.setOnSucceeded(handler -> {
			laborToExport.getLayer().setEnabled(false);
			File ret = (File)handler.getSource().getValue();
			playSound();
			ept.uninstallProgressBar();
			this.doOpenFertMap(Collections.singletonList(ret));
		});
		executorPool.execute(ept);		
	}
	
	private void doExportRecorrida(Recorrida recorrida) {
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

	private void doExportPrescripcionSiembra(SiembraLabor laborToExport) {
		//preguntar en que unidad exportar la dosis de semilla
		Dialog<String> d= new Dialog<String>();
		d.initOwner(JFXMain.stage);//exportarSiembraAction
		d.setTitle(Messages.getString("JFXMain.doExportPrescripcionSiembraTitle"));//"Seleccione unidad de dosis semilla");
		d.getDialogPane().getButtonTypes().add(ButtonType.OK);
		d.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		d.setResizable(true);
		ComboBox<String> cb = new ComboBox<String>();
		Map<String,String> availableColums = new LinkedHashMap<String,String>();
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_10METROS"),SiembraLabor.COLUMNA_SEM_10METROS);//("Sem10ml");
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_DOSIS_SEMILLA"),SiembraLabor.COLUMNA_KG_SEMILLA);//("kgSemHa");
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_MILES_SEM_HA"),SiembraLabor.COLUMNA_MILES_SEM_HA);//("MilSemHa");
		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_ML"),SiembraLabor.COLUMNA_SEM_ML);//("semML");

		cb.setItems(FXCollections.observableArrayList(availableColums.keySet()));
		cb.getSelectionModel().select(0);
		d.getDialogPane().setContent(cb);
		//d.getDialogPane().getChildren().add(cb);

		d.setResultConverter((bt)->{
			if(ButtonType.OK.equals(bt)) {
				String unidad = cb.getSelectionModel().getSelectedItem();
				return availableColums.get(unidad);
			}else {
				return null;
			}
			//d.setResult(unidad);
		});
		d.showAndWait();
		String unidad = d.getResult();
		System.out.println("unidad seleccionada " + d.getResult());
		if(unidad!=null) {
			String nombre = laborToExport.getNombre();
			File shapeFile = FileHelper.getNewShapeFile(nombre);
			//executorPool.execute(()->ExportarPrescripcionSiembraTask.run(laborToExport, shapeFile,unidad));

			ExportarPrescripcionSiembraTask ept = new ExportarPrescripcionSiembraTask(laborToExport, shapeFile,unidad); 
			ept.installProgressBar(progressBox);

			ept.setOnSucceeded(handler -> {				
				File ret = (File)handler.getSource().getValue();
				playSound();
				ept.uninstallProgressBar();
				this.doOpenSiembraMap(Collections.singletonList(ret));
			});
			executorPool.execute(ept);	
		}
	}


	private void doExportHarvestDePuntos(CosechaLabor laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile = FileHelper.getNewShapeFile(nombre);
		executorPool.execute(()->ExportarCosechaDePuntosTask.run(laborToExport, shapeFile));
	}


	/**
	 * metodo que toma una labor y muestra una tabla con los campos de la labor
	 * @param labor
	 */
	private void doShowDataTable(Labor<?> labor) {		   
		SmartTableView.showLaborTable(labor);
	}


	private void doSnapshot(){
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
						doOpenCosecha(shpFiles);//ok!
					}



					FileNameExtensionFilter tifFilter = new FileNameExtensionFilter(Messages.getString("JFXMain.423"),Messages.getString("JFXMain.424"));  
					List<File> tifFiles = db.getFiles();
					tifFiles.removeIf(f->!tifFilter.accept(f));
					if(tifFiles.size()>0){
						File lastFile = tifFiles.get(tifFiles.size()-1);

						config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
						config.save();
						tifFiles.stream().forEach((f)->showNdviTiffFile(f,null));
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
