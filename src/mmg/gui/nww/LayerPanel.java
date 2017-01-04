package mmg.gui.nww;

import java.util.List;
import java.util.function.Function;

import dao.Labor;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class LayerPanel extends VBox {
	protected VBox layersPanel;

	protected ScrollPane scrollPane;
	private List<Function<Layer,String>> actions;

	private BorderPane borderPane = new BorderPane();

	private TreeView<Layer> tree=null;
	private CheckBoxTreeItem<Layer> rootItem=null;
	private CheckBoxTreeItem<Layer> pulverizacionesItem;
	private CheckBoxTreeItem<Layer> fertilizacionestItem;
	private CheckBoxTreeItem<Layer> siembrasItem;
	private CheckBoxTreeItem<Layer> cosechasItem;
	private CheckBoxTreeItem<Layer> ndviItem;
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
		this.layersPanel = new VBox();//(new GridLayout(0, 1, 0, 4));//rows,cols,hgap,vgap
		layersPanel.setPadding(new Insets(5));
		//this.layersPanel.getStyleClass().add("-fx-border-color: black;");

		this.fill(wwd);

		// Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
//		borderPane.setTop(this.layersPanel);
		//	borderPane.setMinHeight(height.doubleValue()*0.9);;

		this.borderPane.prefHeightProperty().bind(height);
		this.borderPane.prefWidthProperty().bind(width);
		this.borderPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		
//		   vbox.backgroundProperty().bind(Bindings.when(toggle.selectedProperty())
//	                .then(new Background(new BackgroundFill(Color.CORNFLOWERBLUE, CornerRadii.EMPTY, Insets.EMPTY)))
//	                .otherwise(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY))));


		// Put the name panel in a scroll bar.
		this.scrollPane = new ScrollPane(this.borderPane);
	//	this.scrollPane.getStyleClass().add("-fx-border-color: black;");


		this.scrollPane.prefHeightProperty().bind(height);
		this.scrollPane.prefWidthProperty().bind(width);

		this.getChildren().add(scrollPane);
	}

	public void setMenuItems(List<Function<Layer,String>> actions){
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

		for (Layer layer : wwd.getModel().getLayers()) {
			//LayerAction action = new LayerAction(layer, wwd, layer.isEnabled());
			String nombre = layer.getName();
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
			if(value != null && value instanceof CosechaLabor){				
				cosechasItem.getChildren().add(checkBoxTreeItem);
				cosechasItem.setExpanded(true);
			}else if(value != null && value instanceof SiembraLabor){				
				siembrasItem.getChildren().add(checkBoxTreeItem);
				cosechasItem.setExpanded(true);
			}else if(value != null && value instanceof PulverizacionLabor){				
				pulverizacionesItem.getChildren().add(checkBoxTreeItem);
				cosechasItem.setExpanded(true);
			}else if(value != null && value instanceof FertilizacionLabor){				
				fertilizacionestItem.getChildren().add(checkBoxTreeItem);
				cosechasItem.setExpanded(true);
			}else{
				//TODO agregar in identificador para los layers de ndvi
				ndviItem.getChildren().add(checkBoxTreeItem);
				cosechasItem.setExpanded(true);
			}

			//jcb.setText(layer.getName());


			checkBoxTreeItem.setSelected(layer.isEnabled());
			checkBoxTreeItem.selectedProperty().addListener((ob,old,nu)->{
				layer.setEnabled(nu);
			});


			//this.layersPanel.getChildren().add(jcb);
		}
		if(tree==null){
		tree = constructTreeView( rootItem);
		} else{
			tree.setRoot(rootItem);
		}
		this.borderPane.setTop(tree);
	}

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
		RenderableLayer ndviLayer = new RenderableLayer();
		ndviLayer.setName("Ndvi");
		ndviItem = new CheckBoxTreeItem<Layer>(ndviLayer);

		rootItem.getChildren().addAll(pulverizacionesItem,fertilizacionestItem,siembrasItem,cosechasItem,ndviItem);
		rootItem.setExpanded(true);
	}

	private TreeView<Layer> constructTreeView(CheckBoxTreeItem<Layer> rootItem) {
		final TreeView<Layer> tree = new TreeView<Layer>(rootItem);  
		
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
					return object.getValue().getName();
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
				if(nuLayer==null)return;
				Object value = nuLayer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				if(value != null && value instanceof CosechaLabor){				

					for(Function<Layer,String> p :actions){

						MenuItem cut = new MenuItem(p.apply(null));
						cut.setOnAction(e->{
							String res = p.apply(nuLayer);	
						//	this.update(wwd);//esto es para que se re dibuje el tree cuando se ejecuta una accion.
						});		
						menu.getItems().add(cut);
					}

					//checkBoxTreeItem.setContextMenu(contextMenu);
					//jcb.setUserData(layer);//userdatacontiene el layer
				}	
				cell.setContextMenu(menu);
			});


			return cell;
		}
				);//fin del cell factory
		
		return tree;
	}

	/**
	 * Update the panel to match the layer list active in a WorldWindow.
	 *
	 * @param wwd
	 *            WorldWindow that will supply the new layer list.
	 */
	public void update(WorldWindow wwd) {
		this.layersPanel.getChildren().clear();
		this.fill(wwd);
	}


	public void addToScrollPaneBottom(Node node){
		this.borderPane.setCenter(node);
	}


	//	protected static class LayerAction extends AbstractAction {
	//		protected WorldWindow wwd;
	//		protected Layer layer;
	//		protected boolean selected;
	//
	//		public LayerAction(Layer layer, WorldWindow wwd, boolean selected) {
	//			super(layer.getName());
	//			this.wwd = wwd;
	//			this.layer = layer;
	//			this.selected = selected;
	//			this.layer.setEnabled(this.selected);
	//		}
	//
	//		public void actionPerformed(ActionEvent actionEvent) {
	//			// Simply enable or disable the layer based on its toggle button.
	//			if (((JCheckBox) actionEvent.getSource()).isSelected())
	//				this.layer.setEnabled(true);
	//			else
	//				this.layer.setEnabled(false);
	//
	//			wwd.redraw();
	//		}
	//	}

}


