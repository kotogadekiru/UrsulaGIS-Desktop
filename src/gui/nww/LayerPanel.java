package gui.nww;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dao.Labor;
import dao.Ndvi;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class LayerPanel extends VBox {
	//public static final String TODOS_ACTION = "todos";
	//public static final String LABOR_ACTION = "labor";
	//public static final String COSECHA_ACTION = "cosecha";
	//public static final String FERTILIZACION_ACTION = "fertilizacion";
	//public static final String SIEMBRA_ACTION = "siembra";
	//public static final String PULVERIZACION_ACTION = "pulverizacion";

	protected ScrollPane scrollPane;
	private Map<Class<?>, List<Function<Layer, String>>> actions;

	private VBox layersPanel = new VBox();

	private TreeView<Layer> tree=null;
	private CheckBoxTreeItem<Layer> rootItem=null;
	private CheckBoxTreeItem<Layer> pulverizacionesItem;
	private CheckBoxTreeItem<Layer> fertilizacionestItem;
	private CheckBoxTreeItem<Layer> siembrasItem;
	private CheckBoxTreeItem<Layer> cosechasItem;
	//private CheckBoxTreeItem<Layer> ndviItem;
	/**
	 * Create a panel with the default size.
	 *
	 * @param wwd
	 *            WorldWindow to supply the layer list.
	 */
	//	public LayerPanel(WorldWindow wwd) {
	//		// Make a panel at a default size.
	//		//	super(new BorderPane());
	//
	//		this.makePanel(wwd, new SimpleDoubleProperty(210), new SimpleDoubleProperty(500));
	//	}

	/**
	 * Create a panel with a size.
	 *
	 * @param wwd
	 *            WorldWindow to supply the layer list.
	 * @param size
	 *            Size of the panel.
	 */
	public LayerPanel(WorldWindow wwd, ReadOnlyDoubleProperty width, ReadOnlyDoubleProperty height) {
		// Make a panel at a specified size.
		//super(new BorderPane());
		this.makePanel(wwd, width,height);
	}

	protected void makePanel(WorldWindow wwd, ReadOnlyDoubleProperty width,ReadOnlyDoubleProperty height){//, Dimension size)
		// Make and fill the panel holding the layer titles.
		//		this.layersPanel = new VBox();//(new GridLayout(0, 1, 0, 4));//rows,cols,hgap,vgap
		//		layersPanel.setPadding(new Insets(5));
		//		this.layersPanel.prefHeightProperty().bind(height);
		//this.layersPanel.getStyleClass().add("-fx-border-color: black;");

		this.fill(wwd);

		// Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
		//		borderPane.setTop(this.layersPanel);
		//	borderPane.setMinHeight(height.doubleValue()*0.9);;

		this.layersPanel.prefHeightProperty().bind(height);
		this.layersPanel.prefWidthProperty().bind(width);
		this.layersPanel.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		//		   vbox.backgroundProperty().bind(Bindings.when(toggle.selectedProperty())
		//	                .then(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, CornerRadii.EMPTY, Insets.EMPTY)))
		//	                .otherwise(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY))));


		// Put the name panel in a scroll bar.
		this.scrollPane = new ScrollPane(this.layersPanel);
		scrollPane.setFitToHeight(true);
		scrollPane.setFitToWidth(true);
		//	this.scrollPane.getStyleClass().add("-fx-border-color: black;");


		//		this.scrollPane.prefHeightProperty().bind(height);
		//		this.scrollPane.prefWidthProperty().bind(width);

		this.getChildren().add(scrollPane);
	}

	public void setMenuItems(Map<Class<?>,List<Function<Layer,String>>> actions){
		this.actions= actions;
		//contextMenu.getItems().setAll(items);
	}

	protected void fill(WorldWindow wwd) {
		// Fill the layers panel with the titles of all layers in the world
		// window's current model.

		if(rootItem==null){
			constructRootItem();  
		} else{
			for(TreeItem<?> item : rootItem.getChildren()){
				item.getChildren().clear();
			}
		}
		String nombre ="";
		for (Layer layer : wwd.getModel().getLayers()) {
			//LayerAction action = new LayerAction(layer, wwd, layer.isEnabled());
			nombre = layer.getName();
			if("Stars".equalsIgnoreCase(nombre)||
					"Atmosphere".equalsIgnoreCase(nombre)||	
					"Blue Marble May 2004".equalsIgnoreCase(nombre)||
					"i-cubed landsat".equalsIgnoreCase(nombre)||
					"bing imagery".equalsIgnoreCase(nombre)||
					"Place Names".equalsIgnoreCase(nombre)||
					"World Map".equalsIgnoreCase(nombre)||
					"Scale Bar".equalsIgnoreCase(nombre)||
					"View Controls".equalsIgnoreCase(nombre)||
					"Annotations".equalsIgnoreCase(nombre)||
					"Compass".equalsIgnoreCase(nombre))continue;
			//CheckBox jcb = new CheckBox();
			final CheckBoxTreeItem<Layer> checkBoxTreeItem = new CheckBoxTreeItem<Layer>(layer);

			Object value = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			//TODO crear un map que mantenga la configuracion del usuario respecto de que ramas estan expandidas
			if(value != null && value instanceof CosechaLabor){				
				cosechasItem.getChildren().add(checkBoxTreeItem);
				//	cosechasItem.setExpanded(true);
			}else if(value != null && value instanceof SiembraLabor){				
				siembrasItem.getChildren().add(checkBoxTreeItem);
				//	siembrasItem.setExpanded(true);
			}else if(value != null && value instanceof PulverizacionLabor){				
				pulverizacionesItem.getChildren().add(checkBoxTreeItem);
				//	pulverizacionesItem.setExpanded(true);
			}else if(value != null && value instanceof FertilizacionLabor){				
				fertilizacionestItem.getChildren().add(checkBoxTreeItem);
				//	fertilizacionestItem.setExpanded(true);
				//			}else if(value != null && value instanceof Margen){				
				//					margenesItem.getChildren().add(checkBoxTreeItem);
				//					margenesItem.setExpanded(true);

			}else if(value != null){
				TreeItem<Layer> knownItem=null;

				String rootItemName ="unknown";

				if(value instanceof String){
					rootItemName=(String) value;

				} else if(value instanceof Object){//margen y lo que pueda suceder mas adelante
					rootItemName=  value.getClass().getSimpleName();
				} else {
					continue;
				}

				for(TreeItem<Layer> item :rootItem.getChildren()){
					if(item.getValue().getName().equals(rootItemName)){//antes comparaba con value en vez de rootItem
						knownItem=item;		
					}
				}
				if(knownItem==null){//si esto tira un falso positivo se me generan los layers duplicados
					RenderableLayer rootLayer = new RenderableLayer();
					rootLayer.setName(rootItemName);
					knownItem = new CheckBoxTreeItem<Layer>(rootLayer);
					//	knownItem.setExpanded(true);
					rootItem.getChildren().add(knownItem);
				}

				knownItem.getChildren().add(checkBoxTreeItem);



			}
			checkBoxTreeItem.setSelected(layer.isEnabled());
			checkBoxTreeItem.selectedProperty().addListener((ob,old,nu)->{
				layer.setEnabled(nu);//aca se repinta el layer?
			});


			//this.layersPanel.getChildren().add(jcb);
		}

		//		List<TreeItem<Layer>> aQuitar = new ArrayList<>();
		//		for(TreeItem<Layer> item :rootItem.getChildren()){
		//			if(item.getChildren().size()==0){
		//				aQuitar.add(item);
		//			}
		//		}
		//		rootItem.getChildren().removeAll(aQuitar);

		for(TreeItem<Layer> item : rootItem.getChildren()){
			try{ item.getChildren().sort((c1,c2)->{
				//fijarse si es de tipo ndvi
				Object labor1 = c1.getValue().getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				Object labor2 = c2.getValue().getValue(Labor.LABOR_LAYER_IDENTIFICATOR);

				String l1Name =c1.getValue().getName();
				String l2Name =c2.getValue().getName();

				if(labor1 != null && labor1 instanceof Ndvi && 
						labor2 != null && labor2 instanceof Ndvi &&
						l1Name.startsWith(l2Name.substring(0, l2Name.length()-"02-01-2018".length()))){
					try {
					Date fecha1 = ((Ndvi)labor1).getFecha().getTime();
					Date fecha2 = ((Ndvi)labor2).getFecha().getTime();
					return fecha1.compareTo(fecha2);
					}catch(Exception e) {
						return l1Name.compareToIgnoreCase(l2Name);
					}
				} else {



					//			//TODO comparar por el valor del layer en vez del nombre del layer
					//			DateFormat df =new  SimpleDateFormat("dd-MM-yyyy");
					//			
					//			try{
					//				Date d1 = df.parse(l1Name);
					//				Date d2 = df.parse(l2Name);
					//				return d1.compareTo(d2);
					//			} catch(Exception e){
					//				//no se pudo parsear como fecha entonces lo interpreto como string.
					//				//e.printStackTrace();
					//			}
					return l1Name.compareToIgnoreCase(l2Name);
				}
			});
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("no se pudo ordenar");
			}
		}
		if(tree==null){
			tree = constructTreeView( rootItem);
			this.layersPanel.getChildren().add(0,tree);
			VBox.setVgrow(tree, Priority.ALWAYS);
			//	tree.prefHeightProperty().bind(layersPanel.heightProperty());
		} else{
			tree.setRoot(rootItem);
		}

	}

	@SuppressWarnings("unchecked")
	private void constructRootItem() {
		RenderableLayer rootLayer = new RenderableLayer();
		rootLayer.setName("Capas");
		rootItem = new CheckBoxTreeItem<Layer>(rootLayer);
		RenderableLayer pulvLayer = new RenderableLayer();
		pulvLayer.setName("Pulverizaciones");
		pulverizacionesItem = new CheckBoxTreeItem<Layer>(pulvLayer);
		RenderableLayer fertLayer = new RenderableLayer();
		fertLayer.setName("Fertilizaciones");
		fertilizacionestItem = new CheckBoxTreeItem<Layer>(fertLayer);
		RenderableLayer siembrLayer = new RenderableLayer();
		siembrLayer.setName("Siembras");
		siembrasItem = new CheckBoxTreeItem<Layer>(siembrLayer);		
		RenderableLayer cosechLayer = new RenderableLayer();
		cosechLayer.setName("Cosechas");
		cosechasItem = new CheckBoxTreeItem<Layer>(cosechLayer);
		//		RenderableLayer ndviLayer = new RenderableLayer();
		//		ndviLayer.setName("Ndvi");
		//		ndviItem = new CheckBoxTreeItem<Layer>(ndviLayer);



		//TODO agregar un menu contextual con acciones para los root Items como quitar todas

		rootItem.getChildren().addAll(pulverizacionesItem,fertilizacionestItem,siembrasItem,cosechasItem);
		rootItem.setExpanded(true);
	}

	private TreeView<Layer> constructTreeView(CheckBoxTreeItem<Layer> rootItem) {
		final TreeView<Layer> tree = new TreeView<Layer>(rootItem);  
		//tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		//los puedo seleccionar pero no puedo actuar sobre mas de uno  a la vez por que set on action es del item en foco.
		//tendria que poner acciones en los nodos y buscar los subitems seleccionados y ahi aplicar

		tree.setEditable(false);
		tree.setStyle("-fx-background-color:transparent;");//-fx-focus-color: -fx-control-inner-background ; -fx-faint-focus-color: -fx-control-inner-background ;
		//tree.setShowRoot(false);
		// tree.setCellFactory(CheckBoxTreeCell.<String>forTreeView());   

		tree.setCellFactory((treeView) ->{
			CheckBoxTreeCell<Layer> cell = (CheckBoxTreeCell<Layer>) CheckBoxTreeCell.<Layer>forTreeView().call(treeView);
			cell.setStyle("-fx-faint-focus-color: -fx-control-inner-background;");//-fx-focus-color: -fx-control-inner-background ; -fx-faint-focus-color: -fx-control-inner-background ;-fx-background-color:transparent;


			cell.setConverter(new StringConverter<TreeItem<Layer>>(){
				@Override
				public String toString(TreeItem<Layer> object) {			
					if(object.getValue()!=null){
						return object.getValue().getName();
					}
					return "item sin layer";
				}

				@Override
				public TreeItem<Layer> fromString(String string) {
					//buscar de una tabla de treeItems?
					return null;
				}        	    	
			} );

			cell.itemProperty().addListener((o,old,nuLayer)->{
				ContextMenu menu = cell.getContextMenu();
				if(menu == null){
					menu = new ContextMenu();
				}else{
					menu.getItems().clear();
				}
				if(nuLayer==null)return;//nuLayer no puede ser null
				Object value = nuLayer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(value == null){
					String rootLayerName = nuLayer.getName();
					List<Function<Layer, String>> cosechasP = new ArrayList<Function<Layer,String>>();
					for(Function<Layer, String> act : actions.get(Object.class)){

						CheckBoxTreeItem<Layer> actTreeItem = null;
						for(TreeItem<Layer> item :rootItem.getChildren()){
							if(item.getValue().getName().equals(rootLayerName)){//antes comparaba con value en vez de rootItem
								actTreeItem=(CheckBoxTreeItem<Layer>) item;		
							}
						}
						if(actTreeItem==null)continue;
						ObservableList<TreeItem<Layer>> children = actTreeItem.getChildren();
						if(children.size()>0 
						//		&& (
						//		actTreeItem.isSelected() //selected no parece andar
						//	actTreeItem.isIndeterminate() //tampoco anda :(
						//		)
								){
							Function<Layer, String> multiAct = (layer)->{
								if(layer==null){
									return act.apply(null) + " Seleccionados";
								} else{
									for(TreeItem<Layer> item: children){
										Layer itemLayer = item.getValue();
										if(itemLayer.isEnabled()){
											act.apply(itemLayer);
										}
									}
									return act.apply(null);	
								}};
								cosechasP.add(multiAct);
								constructMenuItem(nuLayer, menu, cosechasP );
						}
					}						


				} else {

					Class<? extends Object> valueClass = value.getClass();

					for(Class<?> key : actions.keySet()){
						if(key.isAssignableFrom(valueClass)
								|| (key==null && valueClass==null)){						
							constructMenuItem(nuLayer, menu, actions.get(key));
						}					
					}
				}
				cell.setContextMenu(menu);
			});


			return cell;
		}
				);//fin del cell factory

		return tree;
	}

	private void constructMenuItem(Layer nuLayer, ContextMenu menu, List<Function<Layer, String>> actions) {
		for(Function<Layer,String> p :actions){
			MenuItem cut = new MenuItem(p.apply(null));
			cut.setOnAction(e->{Platform.runLater(()->p.apply(nuLayer));});		
			menu.getItems().add(cut);
		}
	}

	/**
	 * Update the panel to match the layer list active in a WorldWindow.
	 *
	 * @param wwd
	 *            WorldWindow that will supply the new layer list.
	 */
	public void update(WorldWindow wwd) {
		//	this.layersPanel.getChildren().clear();
		Platform.runLater(()->		this.fill(wwd));


	}


	public void addToScrollPaneBottom(Node node){
		this.layersPanel.getChildren().add(node);
	}

}


