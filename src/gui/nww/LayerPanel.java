package gui.nww;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gui.Messages;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class LayerPanel extends VBox {

	private static final int TREE_ITEM_ICON_WIDTH = 50;

	protected ScrollPane scrollPane;

	private VBox layersPanel = new VBox();

	private TreeView<Layer> tree=null;

	private CheckBoxTreeItem<Layer> rootItem=null;

	private Map<Class<?>,  CheckBoxTreeItem<Layer>> rootItems= new HashMap<Class<?>,  CheckBoxTreeItem<Layer>>();
	//	private CheckBoxTreeItem<Layer> pulverizacionesItem;
	//	private CheckBoxTreeItem<Layer> fertilizacionestItem;
	//	private CheckBoxTreeItem<Layer> siembrasItem;
	//	private CheckBoxTreeItem<Layer> cosechasItem;

	private Map<Class<?>, List<LayerAction>> actions;
	private Map<Class<?>, List<LayerAction>> layerActions= new HashMap<Class<?>, List<LayerAction>>();

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
		this.fill(wwd);

		// Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
		//		borderPane.setTop(this.layersPanel);
		//	borderPane.setMinHeight(height.doubleValue()*0.9);;

		this.layersPanel.prefHeightProperty().bind(height);
		this.layersPanel.prefWidthProperty().bind(width);
		this.layersPanel.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		// Put the name panel in a scroll bar.
		this.scrollPane = new ScrollPane(this.layersPanel);
		scrollPane.setFitToHeight(true);
		scrollPane.setFitToWidth(true);

		this.getChildren().add(scrollPane);
	}

	public void setMenuItems(Map<Class<?>,List<LayerAction>> actions){
		this.actions= actions;
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
		String nombre =""; //$NON-NLS-1$
		for (Layer layer : wwd.getModel().getLayers()) {
			//LayerAction action = new LayerAction(layer, wwd, layer.isEnabled());
			nombre = layer.getName();
			if("Stars".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"Atmosphere".equalsIgnoreCase(nombre)||	 //$NON-NLS-1$
					"Blue Marble May 2004".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"i-cubed landsat".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"bing imagery".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"Place Names".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"World Map".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"Scale Bar".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"View Controls".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"Annotations".equalsIgnoreCase(nombre)|| //$NON-NLS-1$
					"Compass".equalsIgnoreCase(nombre))continue; //$NON-NLS-1$
			//CheckBox jcb = new CheckBox();
			final CheckBoxTreeItem<Layer> checkBoxTreeItem = new CheckBoxTreeItem<Layer>(layer);

			Object value = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			Object clazz = layer.getValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR);
			Class<?> layerClass =null;
			if(value!=null) {
				layerClass = value.getClass();
			}else if(clazz!=null) {
				layerClass = (Class<?>) clazz;
			}

			//Obtengo los items para esa clase
			CheckBoxTreeItem<Layer> item = rootItems.get(layerClass);
			if(item!=null) {
				item.getChildren().add(checkBoxTreeItem);
			}else if(layerClass != null){

				//crear el nuevo rootItem y agregarlo a la lista de rootItems
				String rootItemName = "unknown";
				if(value instanceof String) {
					rootItemName = (String)value;//caso especial para las capas de distancia
				} else {
					rootItemName = Messages.getString("LayerPanel.rootItemName"+layerClass.getSimpleName());
				}

				RenderableLayer rootLayer = new RenderableLayer();
				rootLayer.setName(rootItemName);
				rootLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR,layerClass);

				CheckBoxTreeItem<Layer> newRootItem = new CheckBoxTreeItem<Layer>(rootLayer);
				setGraphic(newRootItem,"map.png");
				rootItems.put(layerClass,newRootItem);
				rootItem.getChildren().add(newRootItem);
				newRootItem.getChildren().add(checkBoxTreeItem);
			}

			checkBoxTreeItem.setSelected(layer.isEnabled());
			checkBoxTreeItem.selectedProperty().addListener((ob,old,nu)->{
				layer.setEnabled(nu);//aca se repinta el layer?
			});
		}

//ordenar los ndvi por fecha
		for(TreeItem<Layer> item : rootItem.getChildren()){
			try{ item.getChildren().sort((c1,c2)->{
				//fijarse si es de tipo ndvi
				Object labor1 = c1.getValue().getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				Object labor2 = c2.getValue().getValue(Labor.LABOR_LAYER_IDENTIFICATOR);

				String l1Name =c1.getValue().getName();//((Ndvi)labor1).get
				String l2Name =c2.getValue().getName();

				if(labor1 != null && labor1 instanceof Ndvi && 
					labor2 != null && labor2 instanceof Ndvi) {
					 l1Name =((Ndvi)labor1).getNombre();
					 l2Name =((Ndvi)labor2).getNombre();
					 //si empi1=null ezan con el mismo nombre los ordeno por fecha
					if(	l1Name!=null && l1Name.startsWith(l2Name.substring(0, l2Name.length()-"02-01-2018".length()))){ //$NON-NLS-1$
						try {
							LocalDate fecha1 = ((Ndvi)labor1).getFecha();
							LocalDate fecha2 = ((Ndvi)labor2).getFecha();
						//	System.out.println("comprarando fecha1= "+fecha1+" con fecha2= "+fecha2);
							return fecha1.compareTo(fecha2);
						}catch(Exception e) {
							return l1Name.compareToIgnoreCase(l2Name);
						}
					} 
				} //else {//ordenar el resto por nombre

					return l1Name.compareToIgnoreCase(l2Name);
				//}
			});
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("no se pudo ordenar"); //$NON-NLS-1$
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


	private void constructRootItem() {
		RenderableLayer rootLayer = new RenderableLayer();
		rootLayer.setName(Messages.getString("LayerPanel.layerRootLabel")); //$NON-NLS-1$
		rootItem = new CheckBoxTreeItem<Layer>(rootLayer);
		
		RenderableLayer poliLayer = new RenderableLayer();
		poliLayer.setName(Messages.getString("LayerPanel.rootItemNamePoligono")); //$NON-NLS-1$
		poliLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, Poligono.class);
		CheckBoxTreeItem<Layer>poliItem = new CheckBoxTreeItem<Layer>(poliLayer);
		setGraphic(poliItem,"map.png");
		rootItems.put(Poligono.class, poliItem);
		rootItem.getChildren().add(poliItem);

		
		RenderableLayer pulvLayer = new RenderableLayer();
		pulvLayer.setName(Messages.getString("LayerPanel.pulvLabel")); //$NON-NLS-1$
		pulvLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, PulverizacionLabor.class);
		CheckBoxTreeItem<Layer>pulverizacionesItem = new CheckBoxTreeItem<Layer>(pulvLayer);
		setGraphic(pulverizacionesItem,"pulv.png");
		rootItems.put(PulverizacionLabor.class, pulverizacionesItem);
		rootItem.getChildren().add(pulverizacionesItem);

		RenderableLayer fertLayer = new RenderableLayer();
		fertLayer.setName(Messages.getString("LayerPanel.fertLabel")); //$NON-NLS-1$
		fertLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, FertilizacionLabor.class);
		CheckBoxTreeItem<Layer>fertilizacionestItem = new CheckBoxTreeItem<Layer>(fertLayer);
		setGraphic(fertilizacionestItem,"ferti.png");
		rootItems.put(FertilizacionLabor.class, fertilizacionestItem);
		rootItem.getChildren().add(fertilizacionestItem);

		RenderableLayer siembrLayer = new RenderableLayer();
		siembrLayer.setName(Messages.getString("LayerPanel.SiembLabel")); //$NON-NLS-1$
		siembrLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, SiembraLabor.class);
		CheckBoxTreeItem<Layer>siembrasItem = new CheckBoxTreeItem<Layer>(siembrLayer);	
		setGraphic(siembrasItem,"siemb.png");
		rootItems.put(SiembraLabor.class, siembrasItem);
		rootItem.getChildren().add(siembrasItem);

		RenderableLayer cosechLayer = new RenderableLayer();
		cosechLayer.setName(Messages.getString("LayerPanel.cosechLabel")); //$NON-NLS-1$
		cosechLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, CosechaLabor.class);
		CheckBoxTreeItem<Layer>cosechasItem = new CheckBoxTreeItem<Layer>(cosechLayer);
		setGraphic(cosechasItem,"cose.png");
		rootItems.put(CosechaLabor.class, cosechasItem);
		rootItem.getChildren().add(cosechasItem);

		//rootItem.getChildren().addAll(rootItems.values());//pulverizacionesItem,fertilizacionestItem,siembrasItem,cosechasItem);
		rootItem.setExpanded(true);
	}

	private void setGraphic(CheckBoxTreeItem<Layer> item,String iconUrl) {
		ImageView mv = new ImageView();		
		mv.setImage(new Image(this.getClass().getResourceAsStream(iconUrl)));		
		mv.setFitWidth(TREE_ITEM_ICON_WIDTH);
		mv.setPreserveRatio(true);
		mv.setSmooth(true);
		mv.setCache(true);
		item.setGraphic(mv);

	}

	//TODO permitir agrupar por establecimiento campania y lote
	private TreeView<Layer> constructTreeView(CheckBoxTreeItem<Layer> rootItem) {
		final TreeView<Layer> tree = new TreeView<Layer>(rootItem);  
		//tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		//los puedo seleccionar pero no puedo actuar sobre mas de uno  a la vez por que set on action es del item en foco.
		//tendria que poner acciones en los nodos y buscar los subitems seleccionados y ahi aplicar

		tree.setEditable(false);
		tree.setStyle("-fx-background-color:transparent;");//-fx-focus-color: -fx-control-inner-background ; -fx-faint-focus-color: -fx-control-inner-background ; //$NON-NLS-1$
		//tree.setShowRoot(false);
		// tree.setCellFactory(CheckBoxTreeCell.<String>forTreeView());   

		tree.setCellFactory((treeView) ->{
			CheckBoxTreeCell<Layer> cell = (CheckBoxTreeCell<Layer>) CheckBoxTreeCell.<Layer>forTreeView().call(treeView);
			cell.setStyle("-fx-faint-focus-color: -fx-control-inner-background;");//-fx-focus-color: -fx-control-inner-background ; -fx-faint-focus-color: -fx-control-inner-background ;-fx-background-color:transparent; //$NON-NLS-1$


			cell.setConverter(new StringConverter<TreeItem<Layer>>(){
				@Override
				public String toString(TreeItem<Layer> object) {			
					if(object.getValue()!=null){
						return object.getValue().getName();
					}
					return "item sin layer"; //$NON-NLS-1$
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
				Object layerObject = nuLayer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				Object layerObjectClass = nuLayer.getValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR);

				if(layerObject == null ){//es un root node

					if(layerObjectClass instanceof Class) {//estoy cargando las acciones genericas
						Class<? extends Object> valueClass = (Class<?>) layerObjectClass;
						List<LayerAction> layersP = new ArrayList<LayerAction>();

						//System.out.println("creando el menu para la clase "+valueClass);//falla con dao.Poligono
						int size = rootItems.get(valueClass).getChildren().size();
						//System.out.println("size "+valueClass+" es "+size);//falla con dao.Poligono
						List<LayerAction> layerActionsForClass = layerActions.get(valueClass);
						if(layerActionsForClass!=null) {
							List<LayerAction> filtered =  layerActionsForClass.stream().filter(p->
							p.minElementsRequired<=size).collect(Collectors.toList());

							layersP.addAll(filtered);
						}
						if(size > 0) {
							List<LayerAction> accionesGenericas = actions.get(Object.class);
							accionesGenericas.forEach(a->
							layersP.add(
									constructAllSelectedPredicate(a, rootItems.get(valueClass).getChildren())
									));	
						}
						constructMenuItem(nuLayer, menu, layersP);
					}
				} else { //es un cell hoja
					Class<? extends Object> valueClass = layerObject.getClass();
					List<LayerAction> layersP = new ArrayList<LayerAction>();
					for(Class<?> key : actions.keySet()){
						if(key.isAssignableFrom(valueClass)
								|| (key==null && valueClass==null)){	
							layersP.addAll(actions.get(key));
							
						}					
					}
					constructMenuItem(nuLayer, menu, layersP);
				}
				
				cell.setContextMenu(menu);
			});
			return cell;
		}
				);//fin del cell factory

		return tree;
	}

	//	private CheckBoxTreeItem<Layer> buscarTreeItemConNombre(String rootLayerName) {
	//		CheckBoxTreeItem<Layer> actTreeItem = null;
	//		for(TreeItem<Layer> item :rootItem.getChildren()){
	//			if(item.getValue().getName().equals(rootLayerName)){//antes comparaba con value en vez de rootItem
	//				actTreeItem=(CheckBoxTreeItem<Layer>) item;		
	//			}
	//		}
	//		return actTreeItem;
	//	}
	private LayerAction constructAllSelectedPredicate( Function<Layer, String> act, List<TreeItem<Layer>> children){
		Function<Layer, String> removeSelected = (layer)->{//creo un predicado que devuelve "Remove Selected" como nombre y al ser ejecutado corre la accion de remover en todos los hijos de este nodo
			if(layer==null){
				//System.out.println("ejetutando una accion de un treeItem sin Layer viendo si tengo que aplicar "+act.apply(null)+ " en "+rootLayerName);

				return act.apply(null) + Messages.getString("LayerPanel.23"); //$NON-NLS-1$
			} else{
				for(TreeItem<Layer> item: children){
					Layer itemLayer = item.getValue();
					if(itemLayer.isEnabled()){
						act.apply(itemLayer);
					}
				}
				return act.apply(null);	
			}};
			
			LayerAction lAction = new LayerAction(removeSelected);
			lAction.name=removeSelected.apply(null);
			return lAction;
			//return removeSelected;
	}

	/**
	 * metodo que crea los menu items para las acciones layer y menu indicados
	 * @param nuLayer layer para el cual se va a generar el menu
	 * @param menu menu al cual agregar las acciones
	 * @param actions acciones a agregar
	 */
	private void constructMenuItem(Layer nuLayer, ContextMenu menu, List<LayerAction> actions) {
		//actions = actions.stream().sorted().collect(Collectors.toList());//intento para hacer que mantenga el orden
		Collections.sort(actions);
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
		Platform.runLater(()->	this.fill(wwd));
	}


	public void addToScrollPaneBottom(Node node){
		this.layersPanel.getChildren().add(node);
	}


	public void addAccionesClase(List<LayerAction> cosechasP,Class<?> clazz) {
		layerActions.put(clazz, cosechasP);
	}


}


