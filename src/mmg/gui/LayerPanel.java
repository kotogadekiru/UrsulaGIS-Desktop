package mmg.gui;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import dao.CosechaLabor;

//import javax.swing.*;
//import javax.swing.border.*;
//import java.awt.*;
//import java.awt.event.*;
public class LayerPanel extends VBox {
	protected VBox layersPanel;
	protected VBox westPanel;
	protected ScrollPane scrollPane;
	//private ContextMenu contextMenu;
	private List<Function<Layer,String>> actions;
//	protected Font defaultFont;

	/**
	 * Create a panel with the default size.
	 *
	 * @param wwd
	 *            WorldWindow to supply the layer list.
	 */
	public LayerPanel(WorldWindow wwd) {
		// Make a panel at a default size.
		super(new BorderPane());

//	List<Function<Layer, String>> predicates = new ArrayList<Function<Layer,String>>();
//	predicates.add(new Function<Layer,String>(){
//
//		@Override
//		public String apply(Layer layer) {
//		if(layer==null){
//			return "Quitar"; 
//		} else{
//			//source es MenuItem no el CheckBox entonces userData es null
////			Layer layer = (Layer) ((MenuItem)e.getSource()).getUserData();//userdatacontiene el layer
////			Object value = layer.getValue("LABOR");
////			if(value != null && value instanceof CosechaLabor){
////				System.out.println("cut action on labor "+((CosechaLabor)value).nombreProperty.get());
////			}		
//			wwd.getModel().getLayers().remove(layer);
//			return "layer removido" + layer.getName();
//		}
//		}});
//	setMenuItems(predicates);
		
		this.makePanel(wwd, new SimpleDoubleProperty(210), new SimpleDoubleProperty(1000));
	}

	/**
	 * Create a panel with a size.
	 *
	 * @param wwd
	 *            WorldWindow to supply the layer list.
	 * @param size
	 *            Size of the panel.
	 */
	public LayerPanel(WorldWindow wwd, DoubleProperty witdh, DoubleProperty height) {
		// Make a panel at a specified size.
		super(new BorderPane());
		this.makePanel(wwd, witdh, height);
	}

	protected void makePanel(WorldWindow wwd, DoubleProperty width,DoubleProperty height){//, Dimension size)
		// Make and fill the panel holding the layer titles.
		this.layersPanel = new VBox();//(new GridLayout(0, 1, 0, 4));//rows,cols,hgap,vgap
		
		layersPanel.setPadding(new Insets(5));
		
//		layersPanel.setOnMousePressed((me)->{ 
//			if (me.isSecondaryButtonDown()) {
//            contextMenu.show(layersPanel, me.getScreenX(), me.getScreenY());
//        }});
		
	
		//  layersPanel.setPadding(new Insets(5));
//		layersPanel.setHgap(0);
//		layersPanel.setVgap(4);
		//	        ColumnConstraints column1 = new ColumnConstraints(100);
		//	        ColumnConstraints column2 = new ColumnConstraints(50, 150, 300);
		//	        column2.setHgrow(Priority.ALWAYS);
		//	        layersPanel.getColumnConstraints().addAll(column1, column2);


		this.layersPanel.getStyleClass().add("-fx-border-color: black;");
		//  this.layersPanel.setBorder(new Border());
		// this.layersPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.fill(wwd);

		// Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
		BorderPane dummyPanel = new BorderPane();

		dummyPanel.setTop(this.layersPanel);

		// Put the name panel in a scroll bar.
		this.scrollPane = new ScrollPane(dummyPanel);
		this.scrollPane.getStyleClass().add("-fx-border-color: black;");
		//   this.scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	
		//if (size != null){//setPreferredSize(size);
			this.scrollPane.prefHeightProperty().bind(height);
			this.scrollPane.prefWidthProperty().bind(width);
			
	//	}


		// Add the scroll bar and name panel to a titled panel that will resize with the main window.
		westPanel = new VBox();//0, 1, 0, 10);	   
//		westPanel.setHgap(0);
//		westPanel.setVgap(10);
		westPanel.getStyleClass().add("bordered-titled-title");
		// westPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Layers")));

		//Tooltip.install(westPanel, new Tooltip("Layers to Show"));//not on fxAplication thread
		//  westPanel.setToolTipText("Layers to Show");
		westPanel.getChildren().add(scrollPane);
		this.getChildren().add(westPanel);
		//   this.add(westPanel, BorderLayout.CENTER);
	}

	public void setMenuItems(List<Function<Layer,String>> actions){
		this.actions= actions;
		//contextMenu.getItems().setAll(items);
	}
	
	protected void fill(WorldWindow wwd) {
		// Fill the layers panel with the titles of all layers in the world
		// window's current model.
		for (Layer layer : wwd.getModel().getLayers()) {
			//LayerAction action = new LayerAction(layer, wwd, layer.isEnabled());
			CheckBox jcb = new CheckBox();
			Object value = layer.getValue("LABOR");
			if(value != null && value instanceof CosechaLabor){
				
				ContextMenu contextMenu = new ContextMenu();
				for(Function<Layer,String> p :actions){
					
					MenuItem cut = new MenuItem(p.apply(null));
					cut.setOnAction(e->{
						String res = p.apply(layer);	
						this.update(wwd);
					});		
					contextMenu.getItems().add(cut);
				}
				
				jcb.setContextMenu(contextMenu);
				jcb.setUserData(layer);//userdatacontiene el layer
			}		
			
			jcb.setText(layer.getName());
		

			jcb.setSelected(layer.isEnabled());
			jcb.selectedProperty().addListener((ob,old,nu)->{
				layer.setEnabled(nu);
			});
			
			this.layersPanel.getChildren().add(jcb);
		}
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


