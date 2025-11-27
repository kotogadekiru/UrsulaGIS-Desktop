package com.ursulagis.desktop.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import com.ursulagis.desktop.gui.controller.AbstractGUIController;
import com.ursulagis.desktop.gui.utils.SmartTableView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.ursulagis.desktop.tasks.procesar.RedrawMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;

/**
 * Clase que maneja los eventos directos sobre los items
 */
public class LaborItemGUIController extends AbstractGUIController{

	public LaborItemGUIController(JFXMain _main) {
		super(_main);
		// TODO Auto-generated constructor stub
	}

	public static List<Labor<?>> getLaboresCargadas(WorldWindow wwd) {
		List<Labor<?>> recorridasActivas =new ArrayList<Labor<?>>();

		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if ( o instanceof Labor<?>){
				Labor<?> r = (Labor<?>)o;
				recorridasActivas.add(r);
			}
		}
		return recorridasActivas;
	}

	@SuppressWarnings("unchecked")
	private static void doDeleteAction(LaborItem item, WorldWindow wwd ) {
		try {
			Labor<?> l = findOwner(item,wwd);
			if(l==null)return;
					System.out.println("el item pertenece a la labor "+l.getNombre());
					//el item pertenece a la labor Margarita Tr 2425  Cosecha
					SimpleFeature f = item.getFeature(l.getFeatureBuilder());
					l.changeFeature(f, null);
					RedrawMapTask.redraw((Labor<LaborItem>) l);
				//	l.inCollection=null;//para que no se vuelva a procesar desde cero
					wwd.redraw();
					//FIXME Funciona para la primera vez pero despues se cuelga
					//levanta bien cuando haces clone
	
		}catch(Exception e ) {
			e.printStackTrace();
		}
	}
	
	private static Labor<?> findOwner(LaborItem item, WorldWindow wwd ) {
		if(item.labor!=null)return item.labor;
//		try {
//			List<Labor<?>> labores = getLaboresCargadas(wwd);
//			for(Labor<?> l:labores) {
//				if(l.owns(item)) {
//					return l;
//				}
//			}
//		}catch(Exception e) {
//			e.printStackTrace();
//		}
		return null;
	}

	//Llamado desde tootlipController al hacer right click sobre el item
	public void showDialog( LaborItem item) {
		Platform.runLater(()->{
			try {
				Dialog<Boolean> d= new Dialog<Boolean>();
				Window window = d.getDialogPane().getScene().getWindow();
				window.setOnCloseRequest((e) -> d.hide());
				d.initOwner(JFXMain.stage);
				
				

				
				d.setTitle(Messages.getString("LaborItemGUIController.Item")+item.getId()
				+" "+Messages.getString("LaborItemGUIController.Acciones"));

				//		d.getDialogPane().getButtonTypes().add(ButtonType.OK);
				//		d.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
				Button delete = new Button(Messages.getString("LaborItemGUIController.Borrar"));
				delete.setOnAction(a->{
					System.out.println("borrar item "+item.getId());
					doDeleteAction(item, this.getWwd());
					window.hide();				
				});
				VBox.setMargin(delete, new Insets(5,10, 5, 5));
				
				Button edit =new Button(Messages.getString("LaborItemGUIController.Editar"));
				edit.setOnAction(a->{
					System.out.println("editar item "+item.getId());	
					doEditAction(item,this.getWwd());
					window.hide();				
				});
				VBox.setMargin(edit, new Insets(5,10, 5, 5));
				
				Button extractPoligono =new Button(Messages.getString("LaborItemGUIController.ExtraerPoligono"));
				extractPoligono.setOnAction(a->{
					System.out.println("editar item "+item.getId());	
					doExtraerPoligonoAction(item,this.getWwd());
					window.hide();				
				});
				VBox.setMargin(extractPoligono, new Insets(5,10, 5, 5));
				
				VBox v= new VBox();
				v.getChildren().addAll(delete,edit,extractPoligono);
				v.setPadding(new Insets(10, 10, 10, 10));

				d.getDialogPane().setContent(v);
				d.initModality(Modality.NONE);		
				d.getDialogPane().setPrefWidth(250); // <-- solution
				d.setWidth(100);	
				d.setResizable(true);

				d.setResultConverter((buttonType)->true);
				d.showAndWait();
			}catch(Exception e ) {
				e.printStackTrace();
			}
		});
	}

	private void doExtraerPoligonoAction(LaborItem item, WorldWindow wwd) {
		Geometry g = item.getGeometry();
		Poligono p = GeometryHelper.constructPoligono(g);
		this.main.poligonoGUIController.showPoligonos(Arrays.asList(p));
		
	}

	@SuppressWarnings("unchecked")
	private static void doEditAction(LaborItem item, WorldWindow wwd) {
		// TODO Usar Mostrar tabla con un solo item con editar activo
		Platform.runLater(()->{
			Labor<?> l = findOwner(item,wwd);
			if(l==null)return;		
			SimpleFeature old = item.getFeature(l.getFeatureBuilder());

			ArrayList<LaborItem> liLista = new ArrayList<LaborItem>();
			System.out.println("Comenzando a cargar la los datos de la tabla"); //$NON-NLS-1$
			liLista.add(item);

			final ObservableList<LaborItem> dataLotes =	FXCollections.observableArrayList(liLista);

			SmartTableView<LaborItem> table = new SmartTableView<LaborItem>(dataLotes);
			table.setEditable(true);
			//Button toExcel = new Button("To Excel");
//			Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //"Exportar"
//			exportButton.setOnAction(a->{
//				table.toExcel();
//			});
			
			
			
//			BorderPane bottom = new BorderPane();
//			bottom.setRight(exportButton);
			VBox.setVgrow(table, Priority.ALWAYS);
			VBox vBox = new VBox(table);
			Scene scene = new Scene(vBox, 800, 600);
			Stage tablaStage = new Stage();
			
			//tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle("editar item");
			tablaStage.setScene(scene);
			tablaStage.showAndWait();
			System.out.println("el item editado quedo en "+item);	
				
			
			l.changeFeature(old, item);
			//l.inCollection=null;//para que no se vuelva a procesar desde cero
			RedrawMapTask.redraw((Labor<LaborItem>) l);
		});
		
	}

}
