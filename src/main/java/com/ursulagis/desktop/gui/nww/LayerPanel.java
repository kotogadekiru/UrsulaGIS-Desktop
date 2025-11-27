package com.ursulagis.desktop.gui.nww;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.InputStream;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.utils.WeakAdapter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
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

	protected ScrollPane scrollPane=null;	 

	private TreeView<Layer> tree=null;
	private CheckBoxTreeItem<Layer> rootItem=null;
	private Map<Class<?>,  CheckBoxTreeItem<Layer>> rootItems= new HashMap<Class<?>,  CheckBoxTreeItem<Layer>>();

	private Map<Class<?>, List<LayerAction>> actions;
	private Map<Class<?>, List<LayerAction>> layerActions= new HashMap<Class<?>, List<LayerAction>>();
	private List<MenuItem> menuItemsPool = new ArrayList<MenuItem>();

	//private List<ChangeListener<? super Layer>> listeners=new ArrayList<ChangeListener<? super Layer>>();

	private WeakAdapter listenersAdapter = new WeakAdapter();

	/**
	 * Create a panel with a size.
	 *
	 * @param wwd
	 *            WorldWindow to supply the layer list.
	 * @param size
	 *            Size of the panel.
	 */
	public LayerPanel(WorldWindow wwd, ReadOnlyDoubleProperty width, ReadOnlyDoubleProperty height) {
		super();
		this.setPrefWidth(200.0);
		// Make a panel at a specified size.
		//super(new BorderPane());
		this.makePanel(wwd, width,height);
	}

	/**
	 * crea el un scrollPane con el treeView y lo agrega a este VBox
	 * @param wwd
	 * @param width
	 * @param height
	 */
	protected void makePanel(WorldWindow wwd, ReadOnlyDoubleProperty width,ReadOnlyDoubleProperty height){//, Dimension size)
		this.fill(wwd);//crea treeView

		// Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
		//		borderPane.setTop(this.layersPanel);
		//	borderPane.setMinHeight(height.doubleValue()*0.9);;
		//VBox layersPanel = new VBox();
		
		//layersPanel.prefHeightProperty().bind(height);
		//this.layersPanel.prefWidthProperty().bind(width);
		

		// Put the name panel in a scroll bar.
		this.scrollPane = new ScrollPane(tree);
		scrollPane.setFitToHeight(true);
		scrollPane.setFitToWidth(true);//sin esto no se ajusta al ancho del splitpane
		VBox.setVgrow(scrollPane, Priority.ALWAYS);
		this.getChildren().add(scrollPane);

		Messages.registerLocaleChangeListener(getLocaleChangeHandler(wwd));
	}

	private Consumer<Locale> getLocaleChangeHandler(WorldWindow wwd){
		return new Consumer<Locale>(){
			@Override
			public void accept(Locale t) {
				rootItem=null;
				constructRootItem();	
				update(wwd);
			}

		};
	}

	public void setMenuItems(Map<Class<?>,List<LayerAction>> actions){
		this.actions= actions;
	}

	protected void fill(WorldWindow wwd) {
		// Fill the layers panel with the titles of all layers in the world
		// window's current model.

		if(rootItem==null){//TODO si cambio el locale reconstriur el root item
			constructRootItem();  
		} else{//clear leaf nodes
			for(TreeItem<?> item : rootItem.getChildren()){
//				item.getChildren().stream().forEach(i->{
//					listeners.stream().forEach(l->{
//						i.valueProperty().removeListener(l);	
//					});
//					
//					});
				item.getChildren().clear();				
			}
			listenersAdapter.releaseCleared();
			//listeners.clear();//release all weak changelisteners
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
					"Compass".equalsIgnoreCase(nombre)||
					"Capas".equalsIgnoreCase(nombre))continue; 

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
			CheckBoxTreeItem<Layer> branchItem = rootItems.get(layerClass);
			if(branchItem!=null) {
				branchItem.getChildren().add(checkBoxTreeItem);
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

				CheckBoxTreeItem<Layer> newBranchItem = new CheckBoxTreeItem<Layer>(rootLayer);
				setGraphic(newBranchItem,"map.png");
				rootItems.put(layerClass,newBranchItem);
				rootItem.getChildren().add(newBranchItem);
				newBranchItem.getChildren().add(checkBoxTreeItem);
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
					//si empiezan con el mismo nombre los ordeno por fecha
					if(l2Name!=null && l2Name.length()>"02-01-2018".length()) {
						String nombreLote = l2Name.substring(0, l2Name.length()-"02-01-2018".length());
						if(	l1Name!=null && nombreLote!=null && l1Name.startsWith(nombreLote)){ 
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
				}
				if(l1Name==null)l1Name="";
				return l1Name.compareToIgnoreCase(l2Name);
				//}
			});
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("no se pudo ordenar"); 
			}
		}
		if(tree==null){
			tree = constructTreeView( rootItem);
			//this.layersPanel.getChildren().add(0,tree);
			VBox.setVgrow(tree, Priority.ALWAYS);
			//	tree.prefHeightProperty().bind(layersPanel.heightProperty());
		} else{
			tree.setRoot(rootItem);
		}

	}


	private void constructRootItem() {
		RenderableLayer rootLayer = new RenderableLayer(){
			@Override	
			public void dispose() {
				System.out.println("disposing of rootLayer");
				super.dispose();
			}
		};
		rootLayer.setName(Messages.getString("LayerPanel.layerRootLabel")); 
		rootItem = new CheckBoxTreeItem<Layer>(rootLayer);

		RenderableLayer poliLayer = new RenderableLayer();
		poliLayer.setName(Messages.getString("LayerPanel.rootItemNamePoligono")); 
		poliLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, Poligono.class);
		CheckBoxTreeItem<Layer>poliItem = new CheckBoxTreeItem<Layer>(poliLayer);
		setGraphic(poliItem,"map.png");
		rootItems.put(Poligono.class, poliItem);
		rootItem.getChildren().add(poliItem);


		RenderableLayer pulvLayer = new RenderableLayer();
		pulvLayer.setName(Messages.getString("LayerPanel.pulvLabel")); 
		pulvLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, PulverizacionLabor.class);
		CheckBoxTreeItem<Layer>pulverizacionesItem = new CheckBoxTreeItem<Layer>(pulvLayer);
		setGraphic(pulverizacionesItem,"pulv.png");
		rootItems.put(PulverizacionLabor.class, pulverizacionesItem);
		rootItem.getChildren().add(pulverizacionesItem);

		RenderableLayer fertLayer = new RenderableLayer();
		fertLayer.setName(Messages.getString("LayerPanel.fertLabel")); 
		fertLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, FertilizacionLabor.class);
		CheckBoxTreeItem<Layer>fertilizacionestItem = new CheckBoxTreeItem<Layer>(fertLayer);
		setGraphic(fertilizacionestItem,"ferti.png");
		rootItems.put(FertilizacionLabor.class, fertilizacionestItem);
		rootItem.getChildren().add(fertilizacionestItem);

		RenderableLayer siembrLayer = new RenderableLayer();
		siembrLayer.setName(Messages.getString("LayerPanel.SiembLabel")); 
		siembrLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, SiembraLabor.class);
		CheckBoxTreeItem<Layer>siembrasItem = new CheckBoxTreeItem<Layer>(siembrLayer);	
		setGraphic(siembrasItem,"siemb.png");
		rootItems.put(SiembraLabor.class, siembrasItem);
		rootItem.getChildren().add(siembrasItem);

		RenderableLayer cosechLayer = new RenderableLayer();
		cosechLayer.setName(Messages.getString("LayerPanel.cosechLabel")); 
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
		tree.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		tree.setStyle("-fx-background-color:transparent;");//-fx-focus-color: -fx-control-inner-background ; -fx-faint-focus-color: -fx-control-inner-background ; 
		//tree.setShowRoot(false);
		//tree.setCellFactory(CheckBoxTreeCell.<String>forTreeView());   
		
		tree.setCellFactory((treeView) ->{
			CheckBoxTreeCell<Layer> cell = (CheckBoxTreeCell<Layer>) CheckBoxTreeCell.<Layer>forTreeView().call(treeView);
			cell.setStyle("-fx-faint-focus-color: -fx-control-inner-background;");
			//-fx-focus-color: -fx-control-inner-background ; -fx-faint-focus-color: -fx-control-inner-background ;-fx-background-color:transparent;

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
		
			//cell.itemProperty().addListener(getItemPropertyListener(cell));
			
			listenersAdapter.addChangeListener(cell.itemProperty(),constructLayerObjectPropertyListener(cell));
			//cell.itemProperty().addListener(constructLayerObjectPropertyListener(cell));
			
			return cell;
		}
				);//fin del cell factory

		return tree;
	}
	private ChangeListener<Layer> constructLayerObjectPropertyListener( CheckBoxTreeCell<Layer> cell) {	 
		ChangeListener<Layer> listener = new ChangeListener<Layer>() {
			@Override
			public void changed(ObservableValue<? extends Layer> o, Layer old, Layer nuLayer) {
				//crea menu items para los layers de base
				if(nuLayer==null) {
//					if(old!=null) {//esto hace que no se me muestren las acciones correctamente
//						System.out.println("old no era null pero el nuevo si removiendo el listener");
//						o.removeListener(this);					
//					}
					return;//nuLayer no puede ser null			
				}

				Object layerObject = nuLayer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				Object layerObjectClass = nuLayer.getValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR);

				//Se Agrega un layer vacio para contener el root del arbol
				if(layerObject==null && layerObjectClass==null) {
					//System.out.println(nuLayer.getName()+" no es de ursula "+nuLayer.getClass().getName());
					return;//no es un layer de ursula. es de world wind
				}

				if(layerObject == null && layerObjectClass!=null){//es un root node
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

						constructMenuItem(nuLayer, cell, layersP);
					}
				} else if(layerObject != null) { //es un cell hoja
					Class<? extends Object> valueClass = layerObject.getClass();
					List<LayerAction> layersP = new ArrayList<LayerAction>();
					for(Class<?> key : actions.keySet()){
						if(key.isAssignableFrom(valueClass)
								|| (key==null && valueClass==null)){	
							layersP.addAll(actions.get(key));

						}					
					}
					//ContextMenu menu = getContextMenu(cell);
					constructMenuItem(nuLayer, cell, layersP);
				}				
			}			
		};
//		WeakChangeListener<Layer> weakListener = new WeakChangeListener<Layer>(listener);
//		listeners.add(listener);
	 return listener;
	}
	
//	private ChangeListener<Layer> getItemPropertyListener(CheckBoxTreeCell<Layer> cell) {
//		WeakChangeListener<Layer> weakListener = new WeakChangeListener<Layer>(
//		(layerObjectProperty,old,nuLayer)->{
//			
//			//crea menu items para los layers de base
//			if(nuLayer==null) {
//				//listeners.stream().forEach(l->layerObjectProperty.removeListener(l));				
//				//nuLayer puede ser null cuando quiero eliminar el listener
//				return;//nuLayer no puede ser null			
//			}
//
//			Object layerObject = nuLayer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//			Object layerObjectClass = nuLayer.getValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR);
//
//			if(layerObject==null && layerObjectClass==null) {
//				//Capas no es de ursula
//				//System.out.println(nuLayer.getName()+" no es de ursula");
//				return;//no es un layer de ursula. es de world wind
//			}
//			
//			if(layerObject == null ){//es un root node
//				if(layerObjectClass instanceof Class) {//estoy cargando las acciones genericas
//					Class<? extends Object> valueClass = (Class<?>) layerObjectClass;
//					List<LayerAction> layersP = new ArrayList<LayerAction>();
//
//					System.out.println("creando el menu para la clase "+valueClass);//falla con dao.Poligono
//					int size = rootItems.get(valueClass).getChildren().size();
//					//System.out.println("size "+valueClass+" es "+size);//falla con dao.Poligono
//					List<LayerAction> layerActionsForClass = layerActions.get(valueClass);
//					if(layerActionsForClass!=null) {
//						List<LayerAction> filtered =  layerActionsForClass.stream().filter(p->
//						p.minElementsRequired<=size).collect(Collectors.toList());
//
//						layersP.addAll(filtered);
//					}
//					if(size > 0) {
//						List<LayerAction> accionesGenericas = actions.get(Object.class);
//						accionesGenericas.forEach(a->
//						layersP.add(
//								constructAllSelectedPredicate(a, rootItems.get(valueClass).getChildren())
//								));	
//					}
//
//					constructMenuItem(nuLayer, cell, layersP);
//				}
//			} else { //es un cell hoja
//				System.out.println("creando el menu para el layer "+nuLayer.getName());//falla con dao.Poligono
//				Class<? extends Object> valueClass = layerObject.getClass();
//				List<LayerAction> layersP = new ArrayList<LayerAction>();
//				for(Class<?> key : actions.keySet()){
//					if(key.isAssignableFrom(valueClass)
//							|| (key==null && valueClass==null)){	
//						layersP.addAll(actions.get(key));
//
//					}					
//				}
//				//ContextMenu menu = getContextMenu(cell);
//				constructMenuItem(nuLayer, cell, layersP);
//			}
//
//
//		});
//		//listeners.add(weakListener);
//		return weakListener;
//	}

	//	public ContextMenu getContextMenu(CheckBoxTreeCell<Layer> cell) {
	//		ContextMenu menu = cell.getContextMenu();
	//		if( menu == null){
	//			menu = new ContextMenu();
	//		}
	//		System.out.println("nuevo layer");
	//		//menu.getItems().clear();
	//		
	//		return menu;
	//	}

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

				return act.apply(null) + Messages.getString("LayerPanel.23"); 
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
	private void constructMenuItem(Layer nuLayer, CheckBoxTreeCell<Layer> cell, List<LayerAction> actions) {		
		ContextMenu menu = cell.getContextMenu();		
		if( menu == null){		
			menu = new ContextMenu();
			Collections.sort(actions);
			for(LayerAction p :actions){
				MenuItem cut = new MenuItem(p.apply(null));
				cut.setOnAction(e->p.apply(nuLayer)
						//{Platform.runLater(()->p.apply(nuLayer));}
						);
				menu.getItems().add(cut);
			}
			cell.setContextMenu(menu);
		}	else {
			
		//	Layer cellLayer = cell.getItem();
		//	if(cellLayer == null || !nuLayer.equals(cellLayer)) {
				menu.getItems().forEach(mi->{
					mi.setOnAction(null);
					mi.setText("cleared MI");					
					if(menuItemsPool.size()<50) {
						menuItemsPool.add(mi);
					}
				});
				menu.getItems().clear();
			
				Collections.sort(actions);
				for(LayerAction p :actions){
					MenuItem cut =null;
					if(menuItemsPool.size()>0) {
						//System.out.println("menuItemsPool.size() "+menuItemsPool.size());
						cut = menuItemsPool.get(0);
						menuItemsPool.remove(0);
					}else {
						cut = new MenuItem("recent MI");	
					}
					cut.setText(p.apply(null));
					cut.setOnAction(e->p.apply(nuLayer)
							//{Platform.runLater(()->p.apply(nuLayer));}
							);
					menu.getItems().add(cut);
				}				

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
		this.getChildren().add(node);
	}


	public void addAccionesClase(List<LayerAction> cosechasP,Class<?> clazz) {
		layerActions.put(clazz, cosechasP);
	}


}


