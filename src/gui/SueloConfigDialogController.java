package gui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import dao.Clasificador;
import dao.Labor;
import dao.suelo.Suelo;
import gui.utils.DateConverter;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
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
public class SueloConfigDialogController  extends Dialog<Suelo>{
	private static final String CONFIG_DIALOG_FXML = "SueloConfigDialog.fxml";

	@FXML
	private VBox content;

	@FXML
	private TextField textNombre;//ok

	@FXML
	private ComboBox<String> comboPpmN;//ok
	
	@FXML
	private ComboBox<String> comboPpmP;//ok
	
	@FXML
	private ComboBox<String> comboPpmK;//ok
	
	@FXML
	private ComboBox<String> comboPpmS;//ok
	
	@FXML
	private ComboBox<String> comboPpmMO;//ok
	
	@FXML
	private ComboBox<String> comboProfNapa;//ok
	
	@FXML
	private ComboBox<String> comboAguaUtil;//ok

	@FXML
	private ComboBox<String> comboElev;//ok


	
	@FXML
	private DatePicker datePickerFecha;//ok


	@FXML
	private TextField textClasesClasificador;

	@FXML
	private CheckBox chkMakeDefault;//ok

	@FXML
	private ComboBox<String> comboClasificador;//ok



	private Suelo labor;


	public SueloConfigDialogController() {
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
		
		if(cols.indexOf(comboPpmP.getValue())==-1){
			message.append("Debe seleccionar la columna Dosis\n");
			isValid=false;
		}
		if(cols.indexOf(comboPpmN.getValue())==-1){
//			message.append("Debe seleccionar la columna Elevacion\n");
//			isValid=false;
			labor.colElevacion.set(Labor.NONE_SELECTED);
		}
		
//		if(!isValid){
//			Alert alert = new Alert(AlertType.ERROR, message.toString(), ButtonType.OK);
//			alert.initOwner(this.getDialogPane().getScene().getWindow());
//			alert.setTitle("Validar configuracion");
//			alert.showAndWait();
//
//		}
		
		return isValid;

	}


	public void setLabor(Suelo l) {
		this.labor = l;

		List<String> availableColums = labor.getAvailableColumns();
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});

		availableColums.add(Labor.NONE_SELECTED);

		// colRendimiento;
		this.comboPpmN.setItems(FXCollections.observableArrayList(availableColums));
		this.comboPpmN.valueProperty().bindBidirectional(labor.colNProperty);
		
		//comboElev
		this.comboPpmP.setItems(FXCollections.observableArrayList(availableColums));
		this.comboPpmP.valueProperty().bindBidirectional(labor.colPProperty);
		
		// colRendimiento;
		this.comboPpmK.setItems(FXCollections.observableArrayList(availableColums));
		this.comboPpmK.valueProperty().bindBidirectional(labor.colKProperty);

		this.comboPpmS.setItems(FXCollections.observableArrayList(availableColums));
		this.comboPpmS.valueProperty().bindBidirectional(labor.colSProperty);
		
		this.comboPpmMO.setItems(FXCollections.observableArrayList(availableColums));
		this.comboPpmMO.valueProperty().bindBidirectional(labor.colMOProperty);

		this.comboProfNapa.setItems(FXCollections.observableArrayList(availableColums));
		this.comboProfNapa.valueProperty().bindBidirectional(labor.colProfNapaProperty);
		
		this.comboAguaUtil.setItems(FXCollections.observableArrayList(availableColums));
		this.comboAguaUtil.valueProperty().bindBidirectional(labor.colAguaPerfProperty);
		
		
		this.comboElev.setItems(FXCollections.observableArrayList(availableColums));
		this.comboElev.valueProperty().bindBidirectional(labor.colElevacion);
		
		StringConverter<Number> converter = new NumberStringConverter();

		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);

		//textNombre.textProperty().bindBidirectional(labor.nombreProperty);
		textNombre.textProperty().set(labor.getNombre());
		textNombre.textProperty().addListener((obj,old,nu)->labor.setNombre(nu));

		datePickerFecha.setValue(DateConverter.asLocalDate(l.fecha));
		datePickerFecha.setConverter(new DateConverter());
		datePickerFecha.valueProperty().addListener((obs, bool1, n) -> {
			l.setFecha(DateConverter.asDate(n));
			//l.fechaProperty.setValue(bool2);
		});
	}


	public void init() {
		this.getDialogPane().setContent(content);

	}



	public static Optional<Suelo> config(Suelo labor2) {
		Optional<Suelo> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(SueloConfigDialogController.class.getResource(
					CONFIG_DIALOG_FXML));
			myLoader.load();//aca se crea el constructor
			SueloConfigDialogController controller = ((SueloConfigDialogController) myLoader.getController());
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
