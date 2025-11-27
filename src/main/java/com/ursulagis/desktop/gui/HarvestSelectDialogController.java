package com.ursulagis.desktop.gui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class HarvestSelectDialogController extends Dialog<CosechaLabor> {
	private static final String HARVEST_SELECT_DIALOG_FXML = "HarvestSelectDialog.fxml"; //$NON-NLS-1$
	
	@FXML
	private GridPane content;
	
	@FXML
	private ChoiceBox<CosechaLabor> cosechaChoiceBox;
	
	public HarvestSelectDialogController() {
		super();
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("HarvestSelectDialogController.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		//stage.getIcons().addAll(JFXMain.stage.getIcons());
		stage.getIcons().addAll(JFXMain.stage.getIcons());
		this.getDialogPane().getButtonTypes().add(ButtonType.OK);
		this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		this.setResizable(true);

	//	this.getDialogPane().setContent(content);
		
		this.setResultConverter(e -> {		
			if(ButtonType.OK.equals(e)){
				return cosechaChoiceBox.getValue();
			}else{
				return null;
			}
			
			
		});
	}
	public void init() {
		this.getDialogPane().setContent(content);

	}
	
	public void setLabores(List<CosechaLabor> cosechas) {
		this.cosechaChoiceBox.getItems().setAll(cosechas);
		this.cosechaChoiceBox.getSelectionModel().select(0);
	}
	
	public static Optional<CosechaLabor> select(List<CosechaLabor> cosechas) {
		Optional<CosechaLabor> ret = Optional.empty();
		
	try{
		FXMLLoader myLoader = new FXMLLoader(HarvestSelectDialogController.class.getResource(
				HARVEST_SELECT_DIALOG_FXML));
		myLoader.load();//aca se crea el constructor
		HarvestSelectDialogController controller = ((HarvestSelectDialogController) myLoader
				.getController());
		
		controller.setLabores(cosechas);		
		controller.init();
		ret = controller.showAndWait();
	} catch (IOException e1) {
		System.err.println("no se pudo levantar el fxml "+HARVEST_SELECT_DIALOG_FXML); //$NON-NLS-1$
		e1.printStackTrace();
		System.exit(0);
	}
		return ret;
	}
	

}
