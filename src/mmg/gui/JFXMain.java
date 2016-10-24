package mmg.gui;



import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.RenderingExceptionListener;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polygon;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.StatisticsPanel;
import gov.nasa.worldwind.util.StatusBar;
//import gov.nasa.worldwindx.examples.LayerPanel;
import gov.nasa.worldwindx.examples.util.HighlightController;
import gov.nasa.worldwindx.examples.util.ToolTipController;





import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;





import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.TextField;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;





import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
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





import sun.audio.AudioPlayer;
import sun.audio.AudioStream;
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
import tasks.old.ProcessGroupsMapTask;
import tasks.old.ProcessHarvest3DMapTask;





import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;





import dao.Configuracion;
import dao.CosechaItem;
import dao.CosechaLabor;
import dao.Costos;
import dao.FeatureContainer;
import dao.FertilizacionItem;
import dao.FertilizacionLabor;
import dao.Fertilizante;
import dao.Labor;
import dao.Margen;
import dao.Producto;
import dao.PulverizacionItem;
import dao.PulverizacionLabor;
import dao.RentabilidadItem;
import dao.SiembraItem;
import dao.SiembraLabor;
import dao.SueloItem;
//

public class JFXMain extends Application {
	private static final String TITLE_VERSION = "WorldWind MarginMapViewer 0.2.14";
	static final String ICON = "mmg/gui/1-512.png";
	//private static final String SOUND_FILENAME = "D:/Users/workspaceHackaton2015/WorldWindMarginMap/src/mmg/gui/Alarm08.wav";
	private static final String SOUND_FILENAME = "Alarm08.wav";//TODO cortar este wav porque suena 2 veces
	private Stage stage;
	private Scene scene;



	StackPane pane = new StackPane();
	
	private Dimension canvasSize = new Dimension(1500, 800);

	protected WWPanel wwjPanel;
	protected LayerPanel layerPanel;


	private VBox progressBox = new VBox();


	private List<CosechaLabor> cosechas = new ArrayList<CosechaLabor>();
	private List<FertilizacionLabor> fertilizaciones = new ArrayList<FertilizacionLabor>();
	private List<SiembraLabor> siembras = new ArrayList<SiembraLabor>();
	private List<PulverizacionLabor> pulverizaciones = new ArrayList<PulverizacionLabor>();
	//	private Producto producto;
	//	private Fertilizante fertilizante;




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
		

		scene = new Scene(pane,canvasSize.getWidth()*1,canvasSize.getHeight()*0.3);//, Color.White);
		primaryStage.setScene(scene);


		addDragAndDropSupport();
		// scene.getStylesheets().add("gisUI/style.css");//esto funciona

		//
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
		if (Configuration.isMacOS()) {
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name",
					JFXMain.TITLE_VERSION);
		}
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
			Node wwNode = (Node) handler.getSource().getValue();
			vBox1.getChildren().add( wwNode);
			this.wwjPanel.repaint();	
		});

		Thread currentTaskThread = new Thread(pfMapTask);
		currentTaskThread.setDaemon(false);
		currentTaskThread.start();
	}

	protected BorderPane initializeWorldWind() {
		//canvasSize=new Dimension(1920,1080);
		// Create the WorldWindow.
		this.wwjPanel =	new WWPanel(canvasSize, true);
		//una vez que se establecio el tamaño inicial ese es el tamaño maximo
		//this.wwjPanel.setPreferredSize(canvasSize);
		final SwingNode wwSwingNode = new SwingNode();
		wwSwingNode.setContent(wwjPanel);
		// Put the pieces together.

		this.layerPanel = new LayerPanel(this.wwjPanel.getWwd());
		this.layerPanel.getChildren().add(progressBox);

		setAccionesCosechas();

		this.stage.widthProperty().addListener((o,old,nu)->{
			this.wwjPanel.setPreferredSize(new Dimension(nu.intValue(),(int)stage.getHeight()));
			this.wwjPanel.repaint();
			//	this.layerPanel.repaint();		
		});

		this.stage.heightProperty().addListener((o,old,nu)->{
			this.wwjPanel.setPreferredSize(new Dimension((int)stage.getHeight(),nu.intValue()));
			this.wwjPanel.repaint();
		});

		//ok
		this.stage.maximizedProperty().addListener((o,ov,nu)->{
			this.wwjPanel.repaint();	
		});

		BorderPane hbox = new BorderPane();
		hbox.setLeft(layerPanel);
		hbox.setCenter(wwSwingNode);
		//layerPanel.repaint();


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

		return hbox;

	}

	private void setAccionesCosechas() {
		List<Function<Layer, String>> predicates = new ArrayList<Function<Layer,String>>();
		predicates.add(new Function<Layer,String>(){			
			@Override
			public String apply(Layer layer) {
				if(layer==null){
					return "Ir a"; 
				} else{
					viewGoTo((CosechaLabor) layer.getValue("LABOR"));
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
					doEditCosecha((CosechaLabor) layer.getValue("LABOR"));
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
					doGrillarCosechas((CosechaLabor) layer.getValue("LABOR"));
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
					doUnirCosechas((CosechaLabor) layer.getValue("LABOR"));
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
					showHistoCosecha((CosechaLabor) layer.getValue("LABOR"));
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
					showChartRindeAltura((CosechaLabor) layer.getValue("LABOR"));
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
//					doRecomendFertFromHarvestPotential((CosechaLabor) layer.getValue("LABOR"));
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
					doExportHarvest((CosechaLabor) layer.getValue("LABOR"));
					return "histograma mostrado" + layer.getName();
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
					doExportHarvestDePuntos((CosechaLabor) layer.getValue("LABOR"));
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
					Labor<?> labor = (Labor<?>) layer.getValue("LABOR");
					if(labor.inStore!=null){
						labor.inStore.dispose();
					}
					cosechas.remove(layer.getValue("LABOR"));
					return "layer removido" + layer.getName();
				}
			}});




		layerPanel.setMenuItems(predicates);
	}

	protected void importElevations()
	{
		try
		{
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

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
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
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}





	//	public Dimension getCanvasSize() {
	//		return canvasSize;
	//	}

	//	public AppPanel getWwjPanel() {
	//		return wwjPanel;
	//	}

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

	public void setToolTipController(ToolTipController controller) {
		if (this.wwjPanel.toolTipController != null)
			this.wwjPanel.toolTipController.dispose();

		this.wwjPanel.toolTipController = controller;
	}

	public void setHighlightController(HighlightController controller) {
		if (this.wwjPanel.highlightController != null)
			this.wwjPanel.highlightController.dispose();

		this.wwjPanel.highlightController = controller;
	}

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

	public static void insertBeforePlacenames(WorldWindow wwd, Layer layer) {
		// Insert the layer into the layer list just before the placenames.
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l instanceof PlaceNameLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition, layer);
	}

	public static void insertAfterPlacenames(WorldWindow wwd, Layer layer) {
		// Insert the layer into the layer list just after the placenames.
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l instanceof PlaceNameLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition + 1, layer);
	}

	public static void insertBeforeLayerName(WorldWindow wwd, Layer layer,
			String targetName) {
		// Insert the layer into the layer list just before the target layer.
		int targetPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l.getName().indexOf(targetName) != -1) {
				targetPosition = layers.indexOf(l);
				break;
			}
		}
		layers.add(targetPosition, layer);
	}

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

		MenuItem menuItemSuelo = new MenuItem("Suelo");
		menuItemSuelo.setOnAction(a->doOpenSoilMap());
//		menuImportar.getItems().add(menuItemSuelo);

		MenuItem menuItemFertilizacion = new MenuItem("Fertilizacion");
		menuItemFertilizacion.setOnAction(a->doOpenFertMap(null));
		menuImportar.getItems().add(menuItemFertilizacion);


		MenuItem menuItemSiembra = new MenuItem("Siembra");
		menuItemSiembra.setOnAction(a->doOpenSiembraMap());
		//menuImportar.getItems().add(menuItemSiembra);

		MenuItem menuItemPulverizacion = new MenuItem("Pulverizacion");
		menuItemPulverizacion.setOnAction(a->doOpenPulvMap());
	//	menuImportar.getItems().add(menuItemPulverizacion);

		MenuItem menuItemCosecha = new MenuItem("Cosecha");
		menuItemCosecha.setOnAction(a->doOpenHarvestMap(null));
		menuImportar.getItems().add(menuItemCosecha);

		MenuItem menuItemUnirCosechas = new MenuItem("Unir Cosechas");
		menuItemUnirCosechas.setOnAction(a->doUnirCosechas(null));
		menuImportar.getItems().add(menuItemUnirCosechas);

		final Menu menuCalcular = new Menu("Calcular");
		//insertMenuItem(menuCalcular,"Retabilidades",a->doProcessMargin());


		/*Menu Exportar*/
		final Menu menuExportar = new Menu("Exportar");
		MenuItem menuItemExportarRentabilidades = new MenuItem("Rentabilidades");
		menuItemExportarRentabilidades.setOnAction(a->doExportMargins());
	//	menuExportar.getItems().add(menuItemExportarRentabilidades);
		
		MenuItem menuItemUnirShapefiles = new MenuItem("Unir Shapefiles");
		menuItemUnirShapefiles.setOnAction(a->{
			JuntarShapefilesTask.process();			
			});
		menuExportar.getItems().add(menuItemUnirShapefiles);

		MenuItem menuItemExportarSuelo = new MenuItem("Suelo");
		menuItemExportarSuelo.setOnAction(a->doExportSuelo());
	//	menuExportar.getItems().add(menuItemExportarSuelo);

		MenuItem menuItemExportarCaptura= new MenuItem("Pantalla");
		menuItemExportarCaptura.setOnAction(a->doSnapshot());
		menuExportar.getItems().add(menuItemExportarCaptura);


		/*Menu Configuracion*/
		final Menu menuConfiguracion = new Menu("Configuracion");
		MenuItem menuItemProductos = new MenuItem("Productos");
		menuItemProductos.setOnAction(a->doShowABMProductos());
		menuConfiguracion.getItems().addAll(menuItemProductos);
		//menuConfiguracion.getItems().add(new CustomMenuItem(constructDatosPane()));

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menuImportar, menuExportar,menuConfiguracion);
		menuBar.setPrefWidth(scene.getWidth());
		return menuBar;
	}



	private void insertMenuItem(final Menu menu,String title,EventHandler<ActionEvent> handler) {
		MenuItem menuItem = new MenuItem(title);//"Retabilidades");
		menuItem.setOnAction(handler);//a->doProcessMargin());
		menu.getItems().add(menuItem);
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




	private void showChartRindeAltura(Labor cosechaLabor) {
		Labor[] cosechasAux = new Labor[]{cosechaLabor};
		if(cosechaLabor==null){
			Optional<CosechaLabor> optional = HarvestSelectDialogController.select(this.cosechas);
			if(!optional.isPresent()){
				return;
			}else{
				cosechasAux[0] =optional.get();
			}
		}

		Task<RindeAlturaChart> pfMapTask = new Task<RindeAlturaChart>(){
			@Override
			protected RindeAlturaChart call() throws Exception {
				try{
					//	Labor labor = optional.get();		
					RindeAlturaChart histoChart = new RindeAlturaChart(cosechasAux[0]);
					return histoChart;
				}catch(Throwable t){
					t.printStackTrace();
					System.out.println("no hay ninguna labor para mostrar");
					return null;
				}

			}			
		};


		pfMapTask.setOnSucceeded(handler -> {
			RindeAlturaChart	histoChart = (RindeAlturaChart) handler.getSource().getValue();	
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

		Thread currentTaskThread = new Thread(pfMapTask);
		currentTaskThread.setDaemon(true);
		currentTaskThread.start();
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

		Thread currentTaskThread = new Thread(pfMapTask);
		currentTaskThread.setDaemon(true);
		currentTaskThread.start();
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

				//	try {
				//	ReferencedEnvelope bounds = labor.getInStore().getFeatureSource().getBounds();
				//	Sector sector = Sector.fromDegrees(bounds.getMinY(), bounds.getMaxY(), bounds.getMinX(), bounds.getMinY());
				//					Sector sector = Sector.fromDegrees(45,50, 45, 50);
				//					CachedRenderableLayer layer = new CachedRenderableLayer(sector);				
				labor.setLayer(new RenderableLayer());
				//				} catch (IOException e) {
				//					// TODO Auto-generated catch block
				//					e.printStackTrace();
				//				}

				//layer.pick(dc, point);

				Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
					continue;
				}							

				ProcessHarvestMapTask umTask = new ProcessHarvestMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
					cosechas.add(ret);//TODO cambiar esto cuando cambie las acciones a un menu contextual en layerPanel
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("OpenHarvestMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				umTask.start();
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
				CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
			//	viewGoTo(ret);

				System.out.println("EditHarvestMapTask succeded");
				playSound();
			});//fin del OnSucceeded						
			umTask.start();					
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
		umTask.start();					
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
		umTask.start();					
		//			}
		//		}
		
	}

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
//	private void doOpenGroupsMap() {
//		List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(null);
//		if (stores != null) {
//
//			List<String> availableColumns = getAvailableColumns(stores.get(0));
//
//			List<String> requieredColumns = new ArrayList<String>();
//			requieredColumns.add("GroupBy");
//			ColumnSelectDialog csd = new ColumnSelectDialog(
//					requieredColumns, availableColumns);
//
//			Optional<Map<String, String>> result = csd.showAndWait();
//
//			Map<String, String> columns = null;
//			if (result.isPresent()) {
//				columns = result.get();
//				CosechaItem.setColumnsMap(columns);
//
//			} else {
//
//				return;
//			}
//			harvestMap.getChildren().clear();
//
//
//
//			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
//				Group group = new Group();//harvestMap
//				ProcessGroupsMapTask umTask = new ProcessGroupsMapTask(store,
//						group, columns.getOrDefault("GroupBy", "elevacion"));
//				ProgressBar progressBarTask = new ProgressBar();			
//				progressBarTask.setProgress(0);
//				progressBarTask.progressProperty().bind(umTask.progressProperty());
//				progressBox.getChildren().add(progressBarTask);
//				Thread currentTaskThread = new Thread(umTask);
//				currentTaskThread.setDaemon(true);
//				currentTaskThread.start();
//
//				umTask.setOnSucceeded(handler -> {
//					harvestTree = (Quadtree) handler.getSource().getValue();//TODO en vez de pizarlo agregar las nuevas features
//					harvestMap.getChildren().add(group);
//					Bounds bl = harvestMap.getBoundsInLocal();
//					System.out.println("bounds de harvestMap es: " + bl);
//
//					System.out.println("OpenHarvestMapTask succeded");
//
//					progressBox.getChildren().remove(progressBarTask);
//
//					int size = this.harvestMap.getChildren().size();
//
//					harvestMap.visibleProperty().set(true);
//
//				});//fin del OnSucceeded
//			}//fin del for stores
//
//		}//if stores != null
//
//	}


	private void doRecomendFertFromHarvestPotential(CosechaLabor value) {
		// TODO generar un layer de fertilizacion a partir de una cosecha
		//el proceso consiste el levantar las geometrias de la cosecha y preguntarle la usuario
		//que producto aplico y en que densidad por hectarea


		FertilizacionLabor labor = new FertilizacionLabor();
		labor.setLayer(new RenderableLayer());


		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
			continue;
		}							

		RecomendFertFromHarvestPotentialMapTask umTask = new RecomendFertFromHarvestPotentialMapTask(labor);
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
		});//fin del OnSucceeded
		umTask.start();




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
					fertilizaciones.add(ret);//TODO cambiar esto cuando cambie las acciones a un menu contextual en layerPanel
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println("OpenFertMapTask succeded");
					playSound();
				});//fin del OnSucceeded
				umTask.start();
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
					SiembraItem.getRequieredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				SiembraItem.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}

			//map.getChildren().remove(siembraMap);
			siembraMap.getChildren().clear();

			Double precioLabor = Costos.getInstance().precioSiembraProperty.getValue(); 
			Double precioInsumo = Costos.getInstance().precioSemillaProperty.getValue();

			Group sgroup = new Group();
			ProcessSiembraMapTask psMapTask = new ProcessSiembraMapTask(
					sgroup, precioLabor,  precioInsumo, store);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(
					psMapTask.progressProperty());
			Thread currentTaskThread = new Thread(psMapTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			psMapTask.setOnSucceeded(handler -> {
				siembraTree = (Quadtree) handler.getSource().getValue();
				// set Layout x, y para que coincida con el origen de map
				siembraMap.getChildren().add(sgroup);

				Bounds bl = siembraMap.getBoundsInLocal();
				System.out.println("bounds de siembraMap es: " + bl);
				//					siembraMap.setLayoutX(-bl.getMinX());
				//					siembraMap.setLayoutY(-bl.getMinY());
				//	map.getChildren().add( siembraMap);
				siembraMap.visibleProperty().set(true);
				// Group taskMap = (Group) handler.getSource().getValue();
				System.out.println("OpenSiembraMapTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
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

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				PulverizacionItem.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}


			//	String precioPulv = precioPulvProperty.getValue();
			//System.out.println("precioPulv=" + precioPulv);

			//map.getChildren().remove(pulvMap);

			pulvMap.getChildren().clear();

			Double precioLabor = Costos.getInstance().precioPulvProperty.getValue(); 

			Group pGroup = new Group();
			ProcessPulvMapTask pulvmTask = new ProcessPulvMapTask(pGroup,
					precioLabor, store);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(
					pulvmTask.progressProperty());
			Thread currentTaskThread = new Thread(pulvmTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			pulvmTask.setOnSucceeded(handler -> {
				pulvTree = (Quadtree) handler.getSource().getValue();
				pulvMap.getChildren().add(pGroup);
				Bounds bl = pulvMap.getBoundsInLocal();
				System.out.println("bounds de pulvMap es: " + bl);
				//				pulvMap.setLayoutX(-bl.getMinX());
				//				pulvMap.setLayoutY(-bl.getMinY());

				//	map.getChildren().add( pulvMap);
				pulvMap.visibleProperty().set(true);
				// Group taskMap = (Group) handler.getSource().getValue();
				System.out.println("OpenPulvMapTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}

	private void doProcessMargin() {		
		FileDataStore store = chooseShapeFileAndGetStore();

		if (store != null) {

			System.out.println("processingMargins");
			
			
			Margen margen = new Margen();

							
			margen.setLayer(new RenderableLayer());
			
			ProcessMarginMapTask uMmTask = new ProcessMarginMapTask(margen,
					pulverizaciones, fertilizaciones, siembras, cosechas);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(uMmTask.progressProperty());
			Thread currentTaskThread = new Thread(uMmTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			uMmTask.setOnSucceeded(handler -> {
				rentaTree = (Margen) handler.getSource().getValue();
				
				System.out.println("ProcessMarginTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}

	private void doExportMargins() {
		@SuppressWarnings("unchecked")
		List<RentabilidadItem> rentas = this.rentaTree.queryAll();
		//	System.out.println("construyendo el shp para las rentas "+rentas.size());
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
				RentabilidadItem.getType());
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		for (RentabilidadItem renta : rentas) {
			SimpleFeature rentaFeature = renta.getFeature(featureBuilder);
			features.add(rentaFeature);
			//	System.out.println("agregando a features "+rentaFeature);

		}

		File shapeFile =  getNewShapeFile();

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
			newDataStore.createSchema(RentabilidadItem.getType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String typeName = newDataStore.getTypeNames()[0];
		//	System.out.println("typeName 0 del newDataStore es "+typeName);
		SimpleFeatureSource featureSource = null;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
			//	System.out.println("cree new featureSource "+featureSource.getInfo());
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
			SimpleFeatureCollection collection = new ListFeatureCollection(RentabilidadItem.getType(), features);
			//	System.out.println("agregando features al store " +collection.size());
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
				//	System.out.println("commiting transaction "+ featureStore.getCount(new Query()));
			} catch (Exception problem) {
				problem.printStackTrace();
				try {
					transaction.rollback();
					//	System.out.println("transaction rolledback");
				} catch (IOException e) {

					e.printStackTrace();
				}

			} finally {
				try {
					transaction.close();
					//System.out.println("closing transaction");
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

		}		
	}

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
		Platform.runLater(()->{//esto me introduce un error al grabar en el que se pierderon features
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
			newDataStore.createSchema(laborToExport.getType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
			//FIXME a veces me da access us denied
			//java.io.FileNotFoundException: D:\Dropbox\hackatonAgro\EmengareGis\MapasCrudos\shp\sup\out\grid\amb\Girszol_lote_19_s0limano_-_Harvesting.shp (Access is denied)
		}

		String typeName = newDataStore.getTypeNames()[0];
		//	System.out.println("typeName 0 del newDataStore es "+typeName);
		SimpleFeatureSource featureSource = null;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
			//	System.out.println("cree new featureSource "+featureSource.getInfo());
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
			//	SimpleFeatureCollection collection = new ListFeatureCollection(CosechaItem.getType(), features);
			//	System.out.println("agregando features al store " +collection.size());
			//	DefaultFeatureCollection colectionToSave = ;
	
			try {
				
				featureStore.setFeatures(laborToExport.outCollection.reader());
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
		//TODO guardar un archivo txt con la configuracion de la labor para que quede como registro de las operaciones
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
	
		SimpleFeatureType type = CosechaLabor.getPointsFeatureType();
			
			
		ShapefileDataStore newDataStore = createShapefileDataStore(shapeFile,type);

		
		SimpleFeatureIterator it = laborToExport.outCollection.features();
		DefaultFeatureCollection pointFeatureCollection =  new DefaultFeatureCollection("internal",CosechaLabor.getPointsFeatureType());
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
		//TODO guardar un archivo txt con la configuracion de la labor para que quede como registro de las operaciones
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
			//FIXME a veces me da access us denied
			//java.io.FileNotFoundException: D:\Dropbox\hackatonAgro\EmengareGis\MapasCrudos\shp\sup\out\grid\amb\Girszol_lote_19_s0limano_-_Harvesting.shp (Access is denied)
		}
		return newDataStore;
	}
	
	

	private void doOpenSoilMap() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {
			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */
			List<String> availableColumns = getAvailableColumns(store);

			ColumnSelectDialog csd = new ColumnSelectDialog(
					SueloItem.getRequiredColumns(), availableColumns);

			Optional<Map<String, String>> result = csd.showAndWait();

			Map<String, String> columns = null;
			if (result.isPresent()) {
				columns = result.get();

				SueloItem.setColumnsMap(columns);
				System.out.println("columns map: " + columns);
			} else {
				System.out.println("columns names not set");
			}

			// // The Java 8 way to get the response value (with lambda
			// expression).
			// result.ifPresent(letter -> System.out.println("Your choice: " +
			// letter));

			/**/
			//map.getChildren().remove(fertMap);
			this.suelosMap.getChildren().clear();
			//	resetMapScale();

			Group fgroup = new Group();
			ProcessSoilMapTask pfMapTask = new ProcessSoilMapTask(
					fgroup,store);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(
					pfMapTask.progressProperty());
			Thread currentTaskThread = new Thread(pfMapTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();

			pfMapTask.setOnSucceeded(handler -> {
				this.sueloTree = (Quadtree) handler.getSource().getValue();
				suelosMap.getChildren().add(fgroup);
				Bounds bl = suelosMap.getBoundsInLocal();
				System.out.println("bounds de siembraMap es: " + bl);
				//				fertMap.setLayoutX(-bl.getMinX());
				//				fertMap.setLayoutY(-bl.getMinY());

				//		map.getChildren().add(fertMap);
				suelosMap.visibleProperty().set(true);
				// Group taskMap = (Group) handler.getSource().getValue();
				System.out.println("OpenSoilMapTask succeded");
				progressBox.getChildren().remove(progressBarTask);
			});
		}
	}

	/**
	 * generar un shp a partir del mapa de suelos anterior, la fertilizacion y la extraccion de nutrientes estimada por el cultivo
	 */
	private void doExportSuelo() {
		FileDataStore store = chooseShapeFileAndGetStore();
		if (store != null) {

			System.out.println("exportNewSoilMap");

			//map.getChildren().remove(marginMap);
			newSoilMap.getChildren().clear();
			//		resetMapScale();
			Group mGroup = new Group();
			ProcessNewSoilMapTask uMmTask = new ProcessNewSoilMapTask(store, mGroup,sueloTree
					, fertTree,harvestTree , producto, fertilizante);
			ProgressBar progressBarTask = new ProgressBar();
			progressBox.getChildren().add(progressBarTask);
			progressBarTask.setProgress(0);
			progressBarTask.progressProperty().bind(uMmTask.progressProperty());
			Thread currentTaskThread = new Thread(uMmTask);
			currentTaskThread.setDaemon(true);
			currentTaskThread.start();


			uMmTask.setOnSucceeded(handler -> {
				Quadtree newSoilTree = (Quadtree)	handler.getSource().getValue();
				newSoilMap.getChildren().add(mGroup);

				Bounds bl = marginMap.getBoundsInLocal();
				System.out.println("bounds de marginMap es: " + bl);
				//			marginMap.setLayoutX(-bl.getMinX());
				//			marginMap.setLayoutY(-bl.getMinY());
				//	map.getChildren().add( marginMap);
				newSoilMap.visibleProperty().set(true);

				System.out.println("ProcessMarginTask succeded");
				progressBox.getChildren().remove(progressBarTask);
				exportarSueloAShp(newSoilTree);		
			});
		}




	}

	public void exportarSueloAShp(Quadtree newSoilTree) {
		@SuppressWarnings("unchecked")
		List<SueloItem> cosechas = newSoilTree.queryAll();
		//	System.out.println("construyendo el shp para las rentas "+rentas.size());
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
				SueloItem.getType());
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		for (SueloItem cosecha : cosechas) {
			SimpleFeature cosechaFeature = cosecha.getFeature(featureBuilder);
			features.add(cosechaFeature);
			//	System.out.println("agregando a features "+rentaFeature);

		}

		File shapeFile =  getNewShapeFile();

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
			newDataStore.createSchema(SueloItem.getType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String typeName = newDataStore.getTypeNames()[0];
		//	System.out.println("typeName 0 del newDataStore es "+typeName);
		SimpleFeatureSource featureSource = null;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
			//	System.out.println("cree new featureSource "+featureSource.getInfo());
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
			SimpleFeatureCollection collection = new ListFeatureCollection(CosechaLabor.getFeatureType(), features);
			//	System.out.println("agregando features al store " +collection.size());
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
				//	System.out.println("commiting transaction "+ featureStore.getCount(new Query()));
			} catch (Exception problem) {
				problem.printStackTrace();
				try {
					transaction.rollback();
					//	System.out.println("transaction rolledback");
				} catch (IOException e) {

					e.printStackTrace();
				}

			} finally {
				try {
					transaction.close();
					//System.out.println("closing transaction");
				} catch (IOException e) {

					e.printStackTrace();
				}
			}

		}
	}
	
	private void doShowABMProductos() {
		// TODO Auto-generated method stub
		
	}

	private void doSnapshot(){

		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);

		WritableImage image = pane.snapshot(params, null);



		// WritableImage image = barChart.snapshot(new SnapshotParameters(), null);
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
	
	private  File getNewShapeFile() {
		return getNewShapeFile(null);
//		FileChooser fileChooser = new FileChooser();
//		fileChooser.setTitle("Guardar ShapeFile");
//		fileChooser.getExtensionFilters().add(
//				new FileChooser.ExtensionFilter("SHP", "*.shp"));
//
//		File lastFile = null;
//		Configuracion config = new  Configuracion();
//		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
//		if(lastFileName != null){
//			lastFile = new File(lastFileName);
//		}
//		if(lastFile != null ){
//			fileChooser.setInitialDirectory(lastFile.getParentFile());
//			fileChooser.setInitialFileName(lastFile.getName());
//			config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
//		}
//
//		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());
//
//		File file = fileChooser.showSaveDialog(new Stage());
//
//		System.out.println("archivo seleccionado para guardar "+file);
//
//		return file;
	}

	private void playSound() 
	{
		Platform.runLater(()->{
		try
		{
			// get the sound file as a resource out of my jar file;
			// the sound file must be in the same directory as this class file.
			// the input stream portion of this recipe comes from a javaworld.com article.
			InputStream in = getClass().getResourceAsStream(SOUND_FILENAME);// es null
			//  String gongFile = SOUND_FILENAME;
			//  InputStream in = new FileInputStream(gongFile);
			AudioStream audioStream = new AudioStream(in);
			AudioPlayer.player.start(audioStream);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		});
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
