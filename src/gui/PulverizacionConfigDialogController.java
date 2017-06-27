package gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import dao.Clasificador;
import dao.Labor;
import dao.config.Agroquimico;
import dao.pulverizacion.PulverizacionLabor;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class PulverizacionConfigDialogController  extends Dialog<PulverizacionLabor>{
	private static final String CONFIG_DIALOG_FXML = "PulvConfigDialog.fxml";

	@FXML
	private VBox content;

//variables
	@FXML
	private ComboBox<String> comboDosis;//ok


//fijos

	@FXML
	private TextField textNombre;//ok
	
	@FXML
	private DatePicker datePickerFecha;//ok
	
	@FXML
	private TextField textPrecioInsumo;//ok

	@FXML
	private ComboBox<Agroquimico> comboInsumo;
	
	@FXML
	private TextField textCostoLaborHa;//ok

	//opciones
	@FXML
	private TextField textClasesClasificador;

	@FXML
	private CheckBox chkMakeDefault;//ok

	@FXML
	private ComboBox<String> comboClasificador;//ok

	private PulverizacionLabor labor;


	public PulverizacionConfigDialogController() {
		super();
		System.out.println("construyendo el controller");

		this.setTitle("Configure las opciones para su fertilización");
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		stage.getIcons().add(new Image(JFXMain.ICON));

		this.getDialogPane().getButtonTypes().add(ButtonType.OK);
		this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		this.setResizable(true);

		final Button btOk = (Button) this.getDialogPane().lookupButton(ButtonType.OK);
		btOk.addEventFilter(ActionEvent.ACTION, event -> {
			if (!validarDialog()) {
				System.out.println("la configuracion es incorrecta");
				event.consume();
			}
		});

		this.setResultConverter(e -> {		
			if(ButtonType.OK.equals(e)){					
				if(chkMakeDefault.selectedProperty().get()){
					labor.getConfigLabor().getConfigProperties().save();
				}				
				return labor;

			}else{
				return null;
			}
		});
	}



	private boolean validarDialog() {
		List<String> cols = labor.getAvailableColumns();
		StringBuilder message = new StringBuilder();
		boolean isValid =true;
		
//		return 	(cols.indexOf(comboElev.getValue())>-1)&&
//				//	(cols.indexOf(comboPasa.getValue())>-1)&&
//				(cols.indexOf(comboDosis.getValue())>-1);
		
		if(cols.indexOf(comboDosis.getValue())==-1){
			message.append("Debe seleccionar la columna Dosis\n");
			isValid=false;
		}

		
		if(!isValid){
			Alert alert = new Alert(AlertType.ERROR, message.toString(), ButtonType.OK);
			alert.initOwner(this.getDialogPane().getScene().getWindow());
			alert.setTitle("Validar configuracion");
			alert.showAndWait();

		}
		
		return isValid;

	}



	public void setLabor(PulverizacionLabor l) {
		this.labor = l;

		List<String> availableColums = labor.getAvailableColumns();
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});
		availableColums.add(Labor.NONE_SELECTED);
		textNombre.textProperty().bindBidirectional(labor.nombreProperty);
		
		//datePickerFecha.valueProperty().bindBidirectional(l.fechaProperty,);
		datePickerFecha.setValue(l.fechaProperty.getValue());
		datePickerFecha.valueProperty().addListener((obs, bool1, bool2) -> {
			
			l.fechaProperty.setValue(bool2);
		});

		// colDosis
		this.comboDosis.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDosis.valueProperty().bindBidirectional(labor.colDosisProperty);
		//pasadas
//		this.comboPasadas.setItems(FXCollections.observableArrayList(availableColums));
//		this.comboPasadas.valueProperty().bindBidirectional(labor.colCantPasadasProperty);

		//insumo
		this.comboInsumo.setItems(FXCollections.observableArrayList(Agroquimico.agroquimicos.values()));
		this.comboInsumo.valueProperty().bindBidirectional(labor.agroquimico);


		StringConverter<Number> converter = new NumberStringConverter();

		//textPrecioGrano
		Bindings.bindBidirectional(this.textPrecioInsumo.textProperty(), labor.precioInsumoProperty, converter);

		//textCostoLaborHa
		Bindings.bindBidirectional(this.textCostoLaborHa.textProperty(), labor.precioLaborProperty, converter);


		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);


	}

	public void init() {
		this.getDialogPane().setContent(content);

	}



	public static Optional<PulverizacionLabor> config(PulverizacionLabor labor2) {
		Optional<PulverizacionLabor> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(PulverizacionConfigDialogController.class.getResource(
					CONFIG_DIALOG_FXML));
			myLoader.load();//aca se crea el constructor
			PulverizacionConfigDialogController controller = ((PulverizacionConfigDialogController) myLoader.getController());
			controller.setLabor(labor2);
			controller.init();
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+CONFIG_DIALOG_FXML);
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}




}
