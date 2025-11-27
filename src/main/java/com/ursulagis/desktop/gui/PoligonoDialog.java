package com.ursulagis.desktop.gui;

import java.util.ArrayList;
import java.util.List;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Lote;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Window;
import com.ursulagis.desktop.utils.DAH;

public class PoligonoDialog extends Dialog<Poligono>{
	private Poligono poligono = null;
	
	Text nombreLabel = null;
	Text loteLabel=null;
	TextField nombreTF=null;
	ChoiceBox<Lote> cb =null;
	public PoligonoDialog(Poligono _pol, boolean editar) {
		super();
		
		this.poligono=_pol;
		this.initOwner(JFXMain.stage);
		this.getDialogPane().setMinSize(400, 200);

		this.getDialogPane().getButtonTypes().add(ButtonType.OK);
		this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		// La posicion del Dialog la pongo relativa a la ventana y lo corro del medio para que se vea mejor el lote
		this.setOnShowing(event -> {
	        Window owner = this.getOwner();
	        if (owner != null) {
	            double ownerX = owner.getX();
	            double ownerY = owner.getY();
	            double ownerHeight = owner.getHeight();

	            double dialogX = ownerX + 270.0; // Offset en X
	            double dialogY = ownerY + ownerHeight - 200.0; // Offset en Y
	            this.setX(dialogX);
	            this.setY(dialogY);
	        }
	    });
		this.setResizable(true);
		
		 nombreTF = new TextField(this.poligono.getNombre());
		if(editar) {
			this.setTitle(Messages.getString("JFXMain.editarLayerDialogTitle")); //$NON-NLS-1$	
		 nombreLabel = new Text(Messages.getString("JFXMain.editarLayerPoligonName"));//superficie		
		 loteLabel = new Text(Messages.getString("JFXMain.configLoteMi"));//"Lote"
		
		nombreTF.setPromptText(Messages.getString("JFXMain.medirSuperficieNombreLabel")); //$NON-NLS-1$
		}else {
			this.setTitle(Messages.getString("JFXMain.medirSuperficieDielogTitle")); //$NON-NLS-1$
			this.setHeaderText(null);
			nombreLabel = new Text(Messages.getString("JFXMain.editarLayerPoligonName"));//superficie			
			loteLabel = new Text(Messages.getString("JFXMain.configLoteMi"));//"Lote"		
			nombreTF.setPromptText(Messages.getString("JFXMain.medirSuperficieNombreLabel")); //$NON-NLS-1$
		}
		List<Lote> lotes = new ArrayList<Lote>();
		try {
			lotes = DAH.getAllLotes();
		} catch (Exception e) {
			e.printStackTrace();
		}

		cb = new ChoiceBox<Lote>(FXCollections.observableArrayList(lotes));
		cb.getSelectionModel().select(this.poligono.getLote());
		cb.setPrefWidth(250);
		
		
		GridPane vb = new GridPane();
		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(20);
		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(80);
		vb.getColumnConstraints().addAll(column1, column2); 
		
		vb.add(nombreLabel,0,0); vb.add(nombreTF,1,0);
		vb.add(loteLabel,0,1);   vb.add(cb,1,1);
		
		GridPane.setMargin(nombreLabel, new Insets(5,10, 5, 5));
		GridPane.setMargin(nombreTF,new Insets(5,10, 5, 5));
		GridPane.setMargin(loteLabel, new Insets(5,10, 5, 5));
		GridPane.setMargin(cb, new Insets(5,10, 5, 5));
		vb.setPadding(new Insets(10, 10, 10, 10));
		GridPane.setHgrow(cb, Priority.ALWAYS);


		this.getDialogPane().setContent(vb);
		this.initModality(Modality.NONE);		


		final Button btOk = (Button) this.getDialogPane().lookupButton(ButtonType.OK);
		btOk.addEventFilter(ActionEvent.ACTION, event -> {
			if (!validarDialog()) {
				System.out.println("la configuracion es incorrecta");
				event.consume();
			}
		});

		this.setResultConverter(e -> {		
			if(ButtonType.OK.equals(e)){				
				poligono.setLote(cb.getSelectionModel().selectedItemProperty().get());
				poligono.setNombre(nombreTF.textProperty().get());
					//asignar variables a poligono
				return poligono;

			}else{
				return null;
			}
		});
	}

	private boolean validarDialog() {
		boolean isValid = true;
/* 		if(nombreTF.textProperty().get()==null) {
			isValid = false;
		}
		if(cb.getSelectionModel().selectedItemProperty().get()==null) {
			isValid = false;
		} */
		return isValid;
	}
}
