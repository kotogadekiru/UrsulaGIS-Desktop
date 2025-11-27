package com.ursulagis.desktop.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.ursulagis.desktop.dao.config.Configuracion;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ColumnSelectDialog extends Dialog<Map<String, String>> {

	private List<String> requiredColumns;
	private List<String> availableColums;
	//private List<ChoiceBox<String>> choices = new ArrayList<ChoiceBox<String>>();

	private Map<String, String> result = new HashMap<String, String>();


	public ColumnSelectDialog(List<String> requiredColumns1,
			List<String> availableColums1) {
		super();
		this.requiredColumns = requiredColumns1;
		this.availableColums = availableColums1;

		Collections.sort(availableColums);
		

	//	VBox content = new VBox();
		GridPane gp = new GridPane();
		// creo un choiceBox para cada una de las columnas y lo coloco en la
		// lista de choices para buscar la seleccion
		int i =0;
		for (String req : requiredColumns) {
		//	HBox hb = new HBox();
			Label lbl = new Label(req);
			ComboBox<String> cb = new ComboBox<String>();
			cb.setItems(FXCollections.observableArrayList(availableColums));
			
			  cb.setPromptText(Messages.getString("ColumnSelectDialog.seleccionar")); //$NON-NLS-1$
		        cb.setEditable(true);        
//		        cb.valueProperty().addListener(new ChangeListener<String>() {
//		            @Override 
//		            public void changed(ObservableValue ov, String t, String t1) {                
//		                address = t1;                
//		            }    
//		        });
			
			cb.getSelectionModel().selectedItemProperty()
			.addListener(new ChangeListener<String>() {

				@Override
				public void changed(
						ObservableValue<? extends String> cb,
						String oldValue, String newValue) {
					result.replace(req, newValue);
					result.putIfAbsent(req, newValue);
				}
			});
			
			/*busco de las columnas disponibles la que mas se parece a la requerida para preseleccionarla*/
			/*esto tiene que estar despues de agregar el ChangeListener al ChoiceBox*/
			String defaultSelected = req;
		//	String query = req.substring(0,Math.min(4, req.length()));
			String query =  req;//Configuracion.getInstance().getPropertyOrDefault(req,req);	
			
			Optional<String> found = null;
					
			if(query != null){
				found= availableColums.stream().filter(s -> s.contains(query)).findFirst();
				 
				
				} else { 
					String query2 =  req.substring(0,Math.min(4, req.length()));
					found= availableColums.stream().filter(s -> s.contains(query2)).findFirst();
				}
			
			// availableColums.stream().filter(s -> s.contains(query)).findFirst();
			
			if(found.isPresent()){
				defaultSelected=found.get(); 
		//			System.out.println("default selected is "+defaultSelected+" from query "+query);					
			cb.getSelectionModel().select(defaultSelected);
			//XXX confirmar que se dispare el evento de seleccion o agregar a result a mano
			result.replace(req, defaultSelected);
			result.putIfAbsent(req, defaultSelected);
			}
			
		

			//hb.getChildren().addAll(lbl, cb);
			gp.add(lbl, 0,i);
			gp.add(cb, 1, i);
			i++;
		//	HBox.setHgrow(hb, Priority.ALWAYS);
		//	content.getChildren().add(hb);

		}
		this.setTitle(Messages.getString("ColumnSelectDialog.title")); //$NON-NLS-1$
		this.getDialogPane().setContent(gp);
		this.getDialogPane().getButtonTypes().add(ButtonType.OK);
		this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		this.setResizable(true);
		
		this.setResultConverter(e -> {
			return result;
		});

	}
}
