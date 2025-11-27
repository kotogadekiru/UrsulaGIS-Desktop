package com.ursulagis.desktop.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.utils.DateConverter;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
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
	private static final String CONFIG_DIALOG_FXML = "SueloConfigDialog.fxml"; //$NON-NLS-1$

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
	private ComboBox<String> comboDensidad;//ok
	
	@FXML
	private ChoiceBox<String> comboAmount;



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
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("SueloConfigDialogController.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		stage.getIcons().addAll(JFXMain.stage.getIcons());

		this.getDialogPane().getButtonTypes().add(ButtonType.OK);
		this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		this.setResizable(true);

		final Button btOk = (Button) this.getDialogPane().lookupButton(ButtonType.OK);
		btOk.addEventFilter(ActionEvent.ACTION, event -> {
			if (!validarDialog()) {
				System.out.println("la configuracion es incorrecta"); //$NON-NLS-1$
				event.consume();
			}
		});

		this.setResultConverter(e -> {		
			if(ButtonType.OK.equals(e)){					
				if(chkMakeDefault.selectedProperty().get()){
					labor.getConfigLabor().save();
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
			message.append(Messages.getString("SueloConfigDialogController.mensaje")); //$NON-NLS-1$
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
		
		this.comboDensidad.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDensidad.valueProperty().bindBidirectional(labor.colDensidadProperty);
		
		List<String> options = Arrays.asList(
				Suelo.COLUMNA_P,
				Suelo.COLUMNA_N,
				Suelo.COLUMNA_K,
				Suelo.COLUMNA_S,
				Suelo.COLUMNA_MO,
				Suelo.COLUMNA_DENSIDAD,
				Suelo.COLUMNA_ELEVACION,
				Suelo.COLUMNA_AGUA_PERFIL,
				Suelo.COLUMNA_CAPACIDAD_CAMPO,
				Suelo.COLUMNA_POROSIDAD,
				Suelo.COLUMNA_PROF_NAPA,
				Suelo.COLUMNA_TEXTURA				
				); 
		this.comboAmount.setItems(FXCollections.observableArrayList(options));
		this.comboAmount.getSelectionModel().select(options.indexOf(labor.colAmount.get()));
//		if(Suelo.COLUMNA_P.equalsIgnoreCase(labor.colAmount.get())){				
//			this.comboAmount.getSelectionModel().select(0);
//		} else{
//			this.comboAmount.getSelectionModel().select(1);
//		}
		
		this.comboAmount.valueProperty().addListener((o,s,s2)->{
			System.out.println("cambiando colAmount a "+s2); //$NON-NLS-1$
			
			labor.colAmount.set(s2);
//			if(options.get(0).equalsIgnoreCase(s2)){				
//				labor.colAmount.set(Suelo.COLUMNA_P);
//				//cambia el nombre de la labor para que empiece segun la columna que se esta mostrando
////				String n = textNombre.textProperty().get().replace(
////						Messages.getString("MargenConfigDialogController.texto"),//Margen
////						Messages.getString("MargenConfigDialogController.texto2")//Renta
////						);
////			
////				
////				System.out.println("nombre despues de reemplazar renta es "+n); //$NON-NLS-1$
////				textNombre.textProperty().set(n);
//			} else{
//				labor.colAmount.set(Suelo.COLUMNA_N);
////				String n = textNombre.textProperty().get().replace(
////						Messages.getString("MargenConfigDialogController.property"),
////						Messages.getString("MargenConfigDialogController.property2")
////						);
////				
////				textNombre.textProperty().set(n);
//			}
			
		});

		
		StringConverter<Number> converter = PropertyHelper.buildStringConverter();

		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);
		this.comboClasificador.setConverter(Clasificador.clasificadorStringConverter());
		//textNombre.textProperty().bindBidirectional(labor.nombreProperty);
		textNombre.textProperty().set(labor.getNombre());
		textNombre.textProperty().addListener((obj,old,nu)->labor.setNombre(nu));
		Configuracion config = l.getConfig().getConfigProperties();
		PropertyHelper.bindDateToObjectProperty(
				labor::getFecha,
				labor::setFecha,
				datePickerFecha.valueProperty(),
				config,
				Labor.FECHA_KEY);	
	}

	public void init() {
		this.getDialogPane().setContent(content);

	}

	public static Optional<Suelo> config(Suelo labor2) {
		Optional<Suelo> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(SueloConfigDialogController.class.getResource(
					CONFIG_DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
			myLoader.load();//aca se crea el constructor
			SueloConfigDialogController controller = ((SueloConfigDialogController) myLoader.getController());
			controller.setLabor(labor2);
			controller.init();
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+CONFIG_DIALOG_FXML); //$NON-NLS-1$
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}




}
