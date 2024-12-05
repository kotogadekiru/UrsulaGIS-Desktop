package gui;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import dao.Labor;
import dao.LaborItem;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Clase que maneja los eventos directos sobre los items
 */
public class LaborItemGUIController {

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

	private static void doDeleteAction(LaborItem item, WorldWindow wwd ) {
		try {
			List<Labor<?>> labores = getLaboresCargadas(wwd);
			for(Labor<?> l:labores) {
				if(l.owns(item)) {
					System.out.println("el item pertenece a la labor "+l.getNombre());
					//el item pertenece a la labor Margarita Tr 2425  Cosecha
					SimpleFeature f = item.getFeature(l.getFeatureBuilder());
					l.changeFeature(f, null);
					wwd.redraw();
					//FIXME Funciona para la primera vez pero despues se cuelga
					//levanta bien cuando haces clone
				}
			}	
		}catch(Exception e ) {
			e.printStackTrace();
		}
	}

	//Llamado desde tootlipController al hacer right click sobre el item
	public static void showDialog( LaborItem item, WorldWindow wwd) {
		Platform.runLater(()->{
			try {
				Dialog<Boolean> d= new Dialog<Boolean>();
				Window window = d.getDialogPane().getScene().getWindow();
				window.setOnCloseRequest((e) -> d.hide());
				d.initOwner(JFXMain.stage);
				d.setTitle("Item "+item.getId()+" Actions");

				//		d.getDialogPane().getButtonTypes().add(ButtonType.OK);
				//		d.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
				Button delete = new Button("Delete");
				delete.setOnAction(a->{
					System.out.println("borrar item "+item.getId());
					doDeleteAction(item, wwd);
					window.hide();				
				});

				VBox.setMargin(delete, new Insets(5,10, 5, 5));
				Button edit =new Button("Edit");
				edit.setOnAction(a->{
					System.out.println("editar item "+item.getId());	

					window.hide();				
				});

				VBox.setMargin(edit, new Insets(5,10, 5, 5));
				VBox v= new VBox();
				v.getChildren().addAll(delete,edit);
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

}
