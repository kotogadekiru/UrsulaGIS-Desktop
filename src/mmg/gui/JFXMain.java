package mmg.gui;



import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.data.BufferedImageRaster;
import gov.nasa.worldwind.data.ByteBufferRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.data.GDALDataRasterReader;
import gov.nasa.worldwind.data.GeotiffRasterReader;
import gov.nasa.worldwind.event.RenderingExceptionListener;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.StatisticsPanel;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.WWBufferUtil;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import gov.nasa.worldwindx.examples.dataimport.ImportImagery.AppFrame;
import gov.nasa.worldwindx.examples.util.ExampleUtil;
//import gov.nasa.worldwindx.examples.LayerPanel;
import gov.nasa.worldwindx.examples.util.HighlightController;
import gov.nasa.worldwindx.examples.util.ToolTipController;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import mmg.gui.nww.LayerPanel;
import mmg.gui.nww.WWPanel;
import mmg.gui.utils.SmartTableView;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureEvent;
import org.geotools.data.FeatureListener;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.python.google.common.util.concurrent.ExecutionError;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;
import tasks.ExportHarvestMapTask;
import tasks.GrillarCosechasMapTask;
import tasks.JuntarShapefilesTask;
import tasks.ProcessFertMapTask;
import tasks.ProcessHarvestMapTask;
import tasks.ProcessMapTask;
import tasks.ProcessMarginMapTask;
import tasks.ProcessNewSoilMapTask;
import tasks.ProcessPulvMapTask;
import tasks.ProcessSiembraMapTask;
import tasks.ProcessSoilMapTask;
import tasks.UnirCosechasMapTask;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.utils.MatrixOps;
import com.sun.javafx.runtime.async.BackgroundExecutor;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Clasificador;
import dao.FeatureContainer;
import dao.Labor;
import dao.SueloItem;
import dao.config.Configuracion;
//
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.config.Semilla;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.margen.MargenItem;
import dao.pulverizacion.PulverizacionItem;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;

public class JFXMain extends Application {
	private static final String TITLE_VERSION = "Agrotoolbox 0.2.15";
	static final String ICON = "mmg/gui/1-512.png";
	//private static final String SOUND_FILENAME = "D:/Users/workspaceHackaton2015/WorldWindMarginMap/src/mmg/gui/Alarm08.wav";
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

		VBox vBox1 = new VBox();
		vBox1.getChildren().add(menuBar);
		createSwingNode(vBox1);
		pane.getChildren().add(vBox1);

		primaryStage.setOnHiding((e)-> {
			Platform.runLater(()->{
				System.out.println("Application Closed by click to Close Button(X)");
				System.exit(0); 
			});
		});
		primaryStage.show();
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

		setAccionesCosechas();

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

	private void setAccionesCosechas() {
		List<Function<Layer, String>> predicates = new ArrayList<Function<Layer,String>>();
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Ir a"; 
				} else{
					viewGoTo((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "went to " + layer.getName();
				}
			}});

		/**
		 *Accion que permite editar una cosecha
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Editar"; 
				} else{
					doEditCosecha((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "cosecha editada" + layer.getName();
				}
			}});

		/**
		 * Accion que permite pasar una grilla sobre la cosecha
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Grillar cosecha"; 
				} else{
					doGrillarCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "cosecha editada" + layer.getName();
				}
			}

		});

		/**
		 * Accion que permite pasar una grilla sobre la cosecha
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Clonar cosecha"; 
				} else{
					doUnirCosechas((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "cosecha clonada" + layer.getName();
				}
			}

		});

		/**
		 * Accion que muesta el histograma
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Histograma"; 
				} else{
					showHistoCosecha((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "histograma mostrado" + layer.getName();
				}
			}});

		/**
		 * Accion que muesta el la relacion entre el rinde y la elevacion
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Elevacion vs Rinde"; 
				} else{
					showAmountVsElevacionChart((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "histograma mostrado" + layer.getName();
				}
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
		 * Accion permite exportar la cosecha como shp
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Exportar"; 
				} else{
					doExportHarvest((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "histograma mostrado" + layer.getName();
				}
			}});

		/**
		 * Accion permite obtener ndvi
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Obtener NDVI"; 
				} else{
					getNdviTiffFile((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "ndvi obtenido" + layer.getName();
				}
			}});

		/**
		 * Accion muestra una tabla con los datos de la cosecha
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Ver Tabla"; 
				} else{
					doShowDataTable((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "Tabla mostrada" + layer.getName();
				}
			}});

		/**
		 * Accion permite exportar la cosecha como shp de puntos
		 */
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Exportar a puntos"; 
				} else{
					doExportHarvestDePuntos((CosechaLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
					return "cosecha exportada como puntos: " + layer.getName();
				}
			}});


		/**
		 * Accion que permite eliminar una cosecha
		 */
		predicates.add(new Function<Layer,String>(){
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Quitar"; 
				} else{
					getWwd().getModel().getLayers().remove(layer);
					Labor<?> labor = (Labor<?>) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					if(labor!=null){
						labor.dispose();
						System.out.println("cosechas antes de remover: "+cosechas.size());
						cosechas.remove(labor);
						System.out.println("cosechas despues de remover: "+cosechas.size());
						layer.dispose();
						getLayerPanel().update(getWwd());
					}
					return "layer removido" + layer.getName();
				}
			}});

		layerPanel.setMenuItems(predicates);
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
				if (currentElevationModel instanceof CompoundElevationModel)
					((CompoundElevationModel) currentElevationModel).addElevationModel(elevationModel);
				else
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
			System.setProperty("sun.awt.noerasebackground", "true"); // prevents
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
		addMenuItem("Siembra",(a)->doOpenSiembraMap(),menuImportar);
		addMenuItem("Pulverizacion",(a)->doOpenPulvMap(),menuImportar);
		addMenuItem("Cosecha",(a)->doOpenHarvestMap(null),menuImportar);		
		addMenuItem("NDVI",(a)->doOpenTiffFile(),menuImportar);
		addMenuItem("Imagen",(a)->importImagery(),menuImportar);

		final Menu menuCalcular = new Menu("Calcular");
		//insertMenuItem(menuCalcular,"Retabilidades",a->doProcessMargin());
		addMenuItem("Unir Shapefiles",(a)->JuntarShapefilesTask.process(),menuCalcular);
		//	addMenuItem("Rentabilidades",(a)->doExportMargins(),menuCalcular);
		addMenuItem("Unir Cosechas",(a)->doUnirCosechas(null),menuCalcular);

		/*Menu Exportar*/
		final Menu menuExportar = new Menu("Exportar");		
		//	addMenuItem("Suelo",(a)->doExportSuelo(),menuExportar);
		addMenuItem("Pantalla",(a)->doSnapshot(),menuExportar);

		/*Menu Configuracion*/
		final Menu menuConfiguracion = new Menu("Configuracion");
		addMenuItem("Cultivos",(a)->doShowABMProductos(),menuConfiguracion);
		addMenuItem("Fertilizantes",(a)->doShowABMFertilizantes(),menuConfiguracion);
		addMenuItem("Semillas",(a)->doShowABMSemillas(),menuConfiguracion);
		addMenuItem("Acerca De",(a)->doShowAcercaDe(),menuConfiguracion);

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuImportar,menuCalcular, menuExportar,menuConfiguracion);
		menuBar.setPrefWidth(scene.getWidth());
		return menuBar;
	}




	private void addMenuItem(String name, EventHandler<ActionEvent> action, Menu parent){
		MenuItem menuItemProductos = new MenuItem(name);
		menuItemProductos.setOnAction(action);
		parent.getItems().addAll(menuItemProductos);
	}



	public void viewGoTo(Labor<?> labor) {
		try {
			Position position=(Position) labor.getLayer().getValue(ProcessMapTask.ZOOM_TO_KEY);
			viewGoTo(position);
		} catch (Exception e) {
			System.err.println("fallo hacer zoom a la cosecha nueva");
			e.printStackTrace();
		}
	}

	public void viewGoTo(Position harvestPosition) {
		View view =getWwd().getView();
		view.goTo(harvestPosition, 3000d);
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
					return null;
				}

			}			
		};


		pfMapTask.setOnSucceeded(handler -> {
			AmountVsElevacionChart	histoChart = (AmountVsElevacionChart) handler.getSource().getValue();	
			Stage histoStage = new Stage();
			histoStage.setTitle("Correlacion Rinde Vs Altura");
			histoStage.getIcons().add(new Image(ICON));

			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			System.out.println("termine de crear el grafico rinde vs altura");
			histoStage.initOwner(this.stage);
			histoStage.show();
			System.out.println("histoChart.show();");
		});
		executorPool.execute(pfMapTask);

		//		Thread currentTaskThread = new Thread(pfMapTask);
		//		currentTaskThread.setDaemon(true);
		//		currentTaskThread.start();
	}



	private void showHistoCosecha(CosechaLabor cosechaLabor) {	
		CosechaLabor[] cosechasAux = new CosechaLabor[]{cosechaLabor};
		if(cosechaLabor==null){
			Optional<CosechaLabor> optional = HarvestSelectDialogController.select(this.cosechas);
			if(!optional.isPresent()){
				return;
			}else{
				cosechasAux[0] =optional.get();
			}
		}
		Task<Parent> pfMapTask = new Task<Parent>(){
			@Override
			protected Parent call() throws Exception {
				try{	
					CosechaHistoChart histoChart = new CosechaHistoChart(cosechasAux[0]);
					return histoChart;
				}catch(Throwable t){
					t.printStackTrace();
					System.out.println("no hay ninguna labor para mostrar");
					return new VBox(new Label("Upps!!"));
				}
			}			
		};


		pfMapTask.setOnSucceeded(handler -> {
			Parent	histoChart = (Parent) handler.getSource().getValue();	
			Stage histoStage = new Stage();
			histoStage.setTitle("Histograma Cosecha");
			histoStage.getIcons().add(new Image(ICON));

			Scene scene = new Scene(histoChart, 800,450);
			histoStage.setScene(scene);
			System.out.println("termine de crear el histo chart");
			histoStage.initOwner(this.stage);
			histoStage.show();
			System.out.println("histoChart.show();");
		});
		executorPool.submit(pfMapTask);
		//		Thread currentTaskThread = new Thread(pfMapTask);
		//		currentTaskThread.setDaemon(true);
		//		currentTaskThread.start();
	}

	private void getNdviTiffFile(Labor labor){
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
					}
				});
		//PlusUrl url = PlusUrl.listPublicActivities(USER_ID).setMaxResults(MAX_RESULTS);
		String geeURL ="http://gee-api-helper.herokuapp.com";
		GenericUrl url = new GenericUrl( "http://www.lanacion.com.ar");
		url.put("fields", "items(id,url,object(content,plusoners/totalItems))");

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			parseResponse(request.execute());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void parseResponse(HttpResponse response) throws IOException {
		String content = response.parseAsString();
		System.out.println("la nacion content :\n"+content);
		//	    ActivityFeed feed = response.parseAs(ActivityFeed.class);
		//	    if (feed.getActivities().isEmpty()) {
		//	      System.out.println("No activities found.");
		//	    } else {
		//	      if (feed.getActivities().size() == MAX_RESULTS) {
		//	        System.out.print("First ");
		//	      }
		//	      System.out.println(feed.getActivities().size() + " activities found:");
		//	      for (Activity activity : feed.getActivities()) {
		//	        System.out.println();
		//	        System.out.println("-----------------------------------------------");
		//	        System.out.println("HTML Content: " + activity.getActivityObject().getContent());
		//	        System.out.println("+1's: " + activity.getActivityObject().getPlusOners().getTotalItems());
		//	        System.out.println("URL: " + activity.getUrl());
		//	        System.out.println("ID: " + activity.get("id"));
		//	      }
		//	    }
	}



	private void doOpenTiffFile() {
		List<File>	files =chooseFiles("TIF", "*.tif");
		files.forEach((file)->{
			executorPool.execute(()->{

				try{
					BufferWrapperRaster raster = this.loadRasterFile(file);

					if (raster == null){

						return;
					}

					double[] extremes = WWBufferUtil.computeExtremeValues(raster.getBuffer(), raster.getTransparentValue());

					Collection<Object> values = raster.getValues();

					if (extremes == null)
						return;

					final AnalyticSurface surface = new ExportableAnalyticSurface();
					surface.setSector(raster.getSector());
					surface.setDimensions(raster.getWidth(), raster.getHeight());


					double HUE_MIN = Clasificador.colors[0].getHue()/360d;//0d / 360d;
					double HUE_MAX = Clasificador.colors[Clasificador.colors.length-1].getHue()/360d;//240d / 360d;
					double transparentValue =raster.getTransparentValue();
					//double transparentValue =extremes[0];
					surface.setValues(AnalyticSurface.createColorGradientValues(
							raster.getBuffer(), transparentValue, extremes[0], extremes[1], HUE_MIN, HUE_MAX));
					// surface.setVerticalScale(5e3);
					surface.setVerticalScale(10);

					AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
					attr.setDrawOutline(false);
					attr.setDrawShadow(false);
					attr.setInteriorOpacity(1);
					surface.setSurfaceAttributes(attr);

					Format legendLabelFormat = new DecimalFormat() ;
					//					{
					//						public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)  {
					//						//	double valueInFeet = number * WWMath.METERS_TO_FEET;
					//							return super.format(number, result, fieldPosition);
					//						}
					//					};

					final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(extremes[0], extremes[1],
							HUE_MIN, HUE_MAX,
							AnalyticSurfaceLegend.createDefaultColorGradientLabels(extremes[0], extremes[1], legendLabelFormat),
							AnalyticSurfaceLegend.createDefaultTitle(file.getName() + " NDVI Values"));
					legend.setOpacity(1);
					legend.setScreenLocation(new Point(100, 400));

					Renderable renderable =  new Renderable()
					{
						public void render(DrawContext dc)
						{
							Extent extent = surface.getExtent(dc);
							if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
								return;

							if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300)
								return;

							legend.render(dc);
						}
					};
					SurfaceImageLayer layer = new SurfaceImageLayer();
					layer.setName(file.getName());
					layer.setPickEnabled(false);
					layer.addRenderable(surface);
					layer.addRenderable(renderable);
					Platform.runLater(()-> {
						// Add the layer to the model and update the application's layer panel.
						insertBeforeCompass(getWwd(), layer);
						this.getLayerPanel().update(this.getWwd());

						// Set the view to look at the imported image.
						ExampleUtil.goTo(getWwd(), raster.getSector());
						playSound();


					});//fin del run later
				} catch (Exception e)     {
					e.printStackTrace();
				}
			});//fin del executor pool

		});//fin del foreach

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
					 BarnesHutTSne  tsne = new ParallelBHTsne();
					  double [][] Y = tsne.tsne(doubleArray, 1, 3, 20);   
					  System.out.println(MatrixOps.doubleArrayToPrintString(Y, ", ", 50,10));
					
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

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	private void doOpenHarvestMap(List<File> files) {
		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				CosechaLabor labor = new CosechaLabor(store);
				
				//TODO sobreescribir el metodo render del layer para que solo renderee una vez y despues solo lo haga en una imagen de backup
				RenderableLayer layer = new RenderableLayer();
//				{
//					Image backing =null;
//					@Override
//					public void render(DrawContext dc){
//						if(backing ==null){
//						for(Renderable r:this.renderables){
//						
//							super.render(dc);
//							if(r instanceof gov.nasa.worldwind.render.Polygon){
//								gov.nasa.worldwind.render.Polygon p = (gov.nasa.worldwind.render.Polygon) r;
////								Object is = p.getTexture();
//							}
//							
//						}}
//					
//					}
//				};
				labor.setLayer(layer);

				Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
					labor.dispose();//libero los recursos reservados
					continue;
				}							

				ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
					cosechas.add(ret);
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("OpenHarvestMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				this.executorPool.execute(umTask);

				//	umTask.start();//crea un nuevo thread y ejecuta el task (ProcessMapTask)
				//Platform.runLater(umTask);
			}//fin del for stores

		}//if stores != null

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

	// junta 2 o mas cosechas en una 
	private void doUnirCosechas(CosechaLabor cosechaLabor) {

		List<CosechaLabor> cosechasAUnir = new ArrayList<CosechaLabor>();
		if(cosechaLabor == null){
			cosechasAUnir.addAll( this.cosechas);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		} else {

			cosechasAUnir.add(cosechaLabor);

		}
		UnirCosechasMapTask umTask = new UnirCosechasMapTask(cosechasAUnir);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			cosechas.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
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
			cosechasAUnir.addAll( this.cosechas);//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
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
			cosechas.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);

			System.out.println("GrillarCosechasMapTask succeded");
			playSound();
		});//fin del OnSucceeded						
		//umTask.start();					
		this.executorPool.execute(umTask);
	}


	private void doRecomendFertFromHarvestPotential(CosechaLabor value) {
		// TODO generar un layer de fertilizacion a partir de una cosecha
		//el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
		//que producto aplico y en que densidad por hectarea


		FertilizacionLabor labor = new FertilizacionLabor();
		labor.setLayer(new RenderableLayer());


		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			return;
		}							

//		RecomendFertFromHarvestPotentialMapTask umTask = new RecomendFertFromHarvestPotentialMapTask(labor);
//		umTask.installProgressBar(progressBox);
//
//		//	testLayer();
//		umTask.setOnSucceeded(handler -> {
//			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
//			fertilizaciones.add(ret);//TODO cambiar esto cuando cambie las acciones a un menu contextual en layerPanel
//			insertBeforeCompass(getWwd(), ret.getLayer());
//			this.getLayerPanel().update(this.getWwd());
//			umTask.uninstallProgressBar();
//			viewGoTo(ret);
//
//			System.out.println("RecomendFertFromHarvestPotentialMapTask succeded");
//		});//fin del OnSucceeded
//		umTask.start();
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
				labor.setLayer(new RenderableLayer());
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

	private void doOpenSiembraMap() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {
			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */
			List<String> availableColumns = getAvailableColumns(store);

			ColumnSelectDialog csd = new ColumnSelectDialog(
					SiembraLabor.getRequieredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			SiembraLabor siembra = new SiembraLabor(store);

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				siembra.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}

			//			Double precioLabor = Costos.getInstance().precioSiembraProperty.getValue(); 
			//			Double precioInsumo = Costos.getInstance().precioSemillaProperty.getValue();



			siembra.setLayer(new RenderableLayer());
			//			siembra.precioInsumoProperty.set(precioInsumo);
			//			siembra.precioLaborProperty.set(precioLabor);

			//			Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
			//			if(!cosechaConfigured.isPresent()){//
			//				System.out.println("el dialogo termino con cancel asi que no continuo con la fertilizacion");
			//				continue;
			//			}							


			ProcessSiembraMapTask psMapTask = 
					new ProcessSiembraMapTask(siembra);
			//		sgroup, precioLabor,  precioInsumo, store);
			psMapTask.installProgressBar(progressBox);
			//			ProgressBar progressBarTask = new ProgressBar();
			//			progressBox.getChildren().add(progressBarTask);
			//			progressBarTask.setProgress(0);
			//			progressBarTask.progressProperty().bind(
			//					psMapTask.progressProperty());
			//			Thread currentTaskThread = new Thread(psMapTask);
			//			currentTaskThread.setDaemon(true);
			//			currentTaskThread.start();

			psMapTask.setOnSucceeded(handler -> {
				SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
				siembras.add(ret);
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
				psMapTask.uninstallProgressBar();
				viewGoTo(ret);

				System.out.println("OpenSiembraMap succeded");
				playSound();
			});
			this.executorPool.execute(psMapTask);
		}
	}

	// leer mapa de pulverizaciones y calcular costos
	private void doOpenPulvMap() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {
			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */
			List<String> availableColumns = getAvailableColumns(store);

			ColumnSelectDialog csd = new ColumnSelectDialog(
					PulverizacionItem.getRequieredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			PulverizacionLabor labor = new PulverizacionLabor(store);
			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				labor.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}

			//Double precioLabor = Costos.getInstance().precioPulvProperty.getValue(); 

			//Group pGroup = new Group();
			//PulverizacionLabor pl = new PulverizacionLabor(store);
			ProcessPulvMapTask pulvmTask = new ProcessPulvMapTask(labor);
			pulvmTask.installProgressBar(progressBox);
			//			Thread currentTaskThread = new Thread(pulvmTask);
			//			currentTaskThread.setDaemon(true);
			//			currentTaskThread.start();

			pulvmTask.setOnSucceeded(handler -> {
				PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
				pulverizaciones.add(ret);
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
				pulvmTask.uninstallProgressBar();
				viewGoTo(ret);

				System.out.println("OpenPulvMap succeded");
				playSound();
			});
			executorPool.execute(pulvmTask);
		}
	}

	private void doProcessMargin() {		
		//FileDataStore store = chooseShapeFileAndGetStore();

		//if (store != null) {

			System.out.println("processingMargins");


			Margen margen = new Margen();


			margen.setLayer(new RenderableLayer());

			ProcessMarginMapTask uMmTask = new ProcessMarginMapTask(margen,
					pulverizaciones, fertilizaciones, siembras, cosechas);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(uMmTask.progressProperty());
			//			Thread currentTaskThread = new Thread(uMmTask);
			//			currentTaskThread.setDaemon(true);
			//			currentTaskThread.start();

			uMmTask.setOnSucceeded(handler -> {
				//	rentaTree = (Margen) handler.getSource().getValue();
				this.margenes.add(margen);
				System.out.println("ProcessMarginTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
			executorPool.execute(uMmTask);
		//}
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

	private void doExportHarvest(CosechaLabor cosechaLabor) {

		if(cosechaLabor==null){
			Optional<CosechaLabor> cosechaSelected = HarvestSelectDialogController.select(this.cosechas);
			if(cosechaSelected.isPresent()){
				cosechaLabor= cosechaSelected.get();
			} else {
				return;
			}
		}

		final CosechaLabor laborToExport = cosechaLabor;

		String nombre = laborToExport.getNombreProperty().get();
		File shapeFile =  getNewShapeFile(nombre);

		ExportHarvestMapTask ehTask = new ExportHarvestMapTask(cosechaLabor,shapeFile);
		executorPool.execute(ehTask);
		//		Future<File> future = BackgroundExecutor.getExecutor().submit(()->ehTask.call());
		//		try {
		//			future.get();
		//		} catch (InterruptedException | ExecutionException e) {
		//			e.printStackTrace();
		//		}
		//		//		Platform.runLater(()->{
		//		//		});//fin del run later
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


				SimpleFeature pointFeature = fb.buildFeature(FeatureContainer.getID(sf));
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

	private void doShowDataTable(CosechaLabor labor) {		   
		Platform.runLater(()->{

			ArrayList<CosechaItem> ciLista = new ArrayList<CosechaItem>();
			System.out.println("Comenzando a cargar la los datos de la tabla");
			Iterator<?> it = labor.outCollection.iterator();
			while(it.hasNext()){
				CosechaItem ci = labor.constructFeatureContainerStandar((SimpleFeature)it.next(), false);
				ciLista.add(ci);
			}

			final ObservableList<CosechaItem> dataLotes =
					FXCollections.observableArrayList(
							ciLista
							);

			SmartTableView<CosechaItem> table = new SmartTableView<CosechaItem>(dataLotes,dataLotes);
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
		//TODO mostrar dialogo con informacion de la version, link a www.agrotoolbox.com y los creadores
	}

	private void doShowABMProductos() {
		Platform.runLater(()->{

			ArrayList<Cultivo> ciLista = new ArrayList<Cultivo>();
			System.out.println("Comenzando a cargar la los datos de la tabla");

			ciLista.addAll(Cultivo.cultivos.values());
			final ObservableList<Cultivo> dataLotes =
					FXCollections.observableArrayList(
							ciLista
							);

			SmartTableView<Cultivo> table = new SmartTableView<Cultivo>(dataLotes,dataLotes);
			table.setEditable(true);

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Cultivos");
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	private void doShowABMFertilizantes() {
		Platform.runLater(()->{

			ArrayList<Fertilizante> ciLista = new ArrayList<Fertilizante>();
			System.out.println("Comenzando a cargar la los datos de la tabla");

			ciLista.addAll(Fertilizante.fertilizantes.values());
			final ObservableList<Fertilizante> dataLotes =
					FXCollections.observableArrayList(
							ciLista
							);

			SmartTableView<Fertilizante> table = new SmartTableView<Fertilizante>(dataLotes,dataLotes);
			table.setEditable(true);

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle("Fertilizantes");
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}


	private void doShowABMSemillas() {
		Platform.runLater(()->{

			ArrayList<Semilla> ciLista = new ArrayList<Semilla>();
			System.out.println("Comenzando a cargar la los datos de la tabla");

			ciLista.addAll(Semilla.semillas.values());
			final ObservableList<Semilla> dataLotes =
					FXCollections.observableArrayList(
							ciLista
							);

			SmartTableView<Semilla> table = new SmartTableView<Semilla>(dataLotes,dataLotes);
			table.setEditable(true);

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

	/**
	 * 
	 * @param f1 filter Title "JPG"
	 * @param f2 filter regex "*.jpg"
	 */
	private List<File> chooseFiles(String f1,String f2) {
		List<File> files =null;
		FileChooser fileChooser = new FileChooser();
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
			files = fileChooser.showOpenMultipleDialog(new Stage());
			//		file = files.get(0);
		}catch(IllegalArgumentException e){
			fileChooser.setInitialDirectory(null);
			File file = fileChooser.showOpenDialog(new Stage());
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

		File file = fileChooser.showSaveDialog(new Stage());

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
					}
					doOpenHarvestMap(shpFiles);//ok!

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
