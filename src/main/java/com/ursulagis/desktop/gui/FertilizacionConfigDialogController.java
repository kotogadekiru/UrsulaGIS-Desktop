package com.ursulagis.desktop.gui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
import com.ursulagis.desktop.utils.DAH;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class FertilizacionConfigDialogController  extends Dialog<FertilizacionLabor>{
	private static final String FERT_CONFIG_DIALOG_FXML = "FertilizacionConfigDialog.fxml"; //$NON-NLS-1$

	@FXML
	private VBox content;


	@FXML
	private ComboBox<String> comboDosis;//ok

	@FXML
	private TextField textPrecioFert;//ok

	@FXML
	private ComboBox<String> comboElev;//ok

	@FXML
	private TextField textNombre;//ok

	@FXML
	private DatePicker datePickerFecha;//ok

	@FXML
	private TextField textCostoLaborHa;//ok

	@FXML
	private TextField textClasesClasificador;

	@FXML
	private CheckBox chkMakeDefault;//ok

	@FXML
	private ComboBox<String> comboClasificador;//ok


	@FXML
	private ComboBox<Fertilizante> comboFertilizante;


	private FertilizacionLabor labor;


	public FertilizacionConfigDialogController() {
		super();
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("FertilizacionConfigDialogController.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		//stage.getIcons().addAll(JFXMain.stage.getIcons());
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
					labor.getConfigLabor().save();//.getConfigProperties().save();
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
			message.append(Messages.getString("FertilizacionConfigDialogController.message")); //$NON-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboElev.getValue())==-1){
			//			message.append("Debe seleccionar la columna Elevacion\n");
			//			isValid=false;
			labor.colElevacion.set(Labor.NONE_SELECTED);
		}

		if(!isValid){
			Alert alert = new Alert(AlertType.ERROR, message.toString(), ButtonType.OK);
			alert.initOwner(this.getDialogPane().getScene().getWindow());
			alert.setTitle(Messages.getString("FertilizacionConfigDialogController.validacion")); //$NON-NLS-1$
			alert.showAndWait();
		}

		return isValid;
	}



	public void setLabor(FertilizacionLabor l) {
		this.labor = l;
		//LaborConfig config = labor.getConfigLabor();//labor.getConfigLabor().getConfigProperties();
		List<String> availableColums = labor.getAvailableColumns();
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});
		availableColums.add(Labor.NONE_SELECTED);


		//comboElev
		this.comboElev.setItems(FXCollections.observableArrayList(availableColums));
		this.comboElev.valueProperty().bindBidirectional(labor.colElevacion);



		// colRendimiento;
		this.comboDosis.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDosis.valueProperty().bindBidirectional(labor.colKgHaProperty);

		//col fertilizante
		this.comboFertilizante.setItems(FXCollections.observableArrayList(DAH.getAllFertilizantes()));
		this.comboFertilizante.getSelectionModel().select(labor.getFertilizante());//viene inicializada con el default desde init()
		this.comboFertilizante.valueProperty().addListener((obj,old,n)->{
			labor.fertilizante=n;
			//	if(n!=null)config.getConfigProperties().setProperty(FertilizacionLabor.f.CosechaLaborConstants.PRODUCTO_DEFAULT,n.getNombre());
		});		
		//this.comboFertilizante.setItems(FXCollections.observableArrayList(DAH.getAllFertilizantes()));
		//this.comboFertilizante.valueProperty().bindBidirectional(labor.fertilizanteProperty);

		//StringConverter<Number> converter = new NumberStringConverter(Messages.getLocale());
		

		Configuracion properties = labor.getConfigLabor().getConfigProperties();
		//textPrecioGrano
		//Bindings.bindBidirectional(this.textPrecioFert.textProperty(), labor.precioInsumoProperty, converter);
		this.textPrecioFert.textProperty().set(
				properties.getPropertyOrDefault(FertilizacionLabor.COLUMNA_PRECIO_FERT, 
						PropertyHelper.formatDouble(labor.getPrecioInsumo()))
				);
		this.textPrecioFert.textProperty().addListener((obj,old,n)->{
			labor.setPrecioInsumo(PropertyHelper.parseDouble(n).doubleValue());
			properties.setProperty(FertilizacionLabor.COLUMNA_PRECIO_FERT, n);
		});


		//textCostoCosechaHa
		//Bindings.bindBidirectional(this.textCostoLaborHa.textProperty(), labor.precioLaborProperty, converter);
		//		this.textCostoLaborHa.textProperty().set(labor.getConfigLabor().getConfigProperties().getPropertyOrDefault(FertilizacionLabor.COLUMNA_PRECIO_PASADA, labor.getPrecioLabor().toString()));
		//		this.textCostoLaborHa.textProperty().addListener((obj,old,n)->{			
		//			labor.setPrecioLabor(converter.fromString(n).doubleValue());
		//			//config.getConfigProperties().getPropertyOrDefault(CosechaLabor.PRECIO_GRANO,"0")
		//			labor.getConfigLabor().getConfigProperties().setProperty(FertilizacionLabor.COLUMNA_PRECIO_PASADA, n);
		//		});


		
		this.textCostoLaborHa.textProperty().set(
				properties.getPropertyOrDefault(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,
						PropertyHelper.formatDouble(labor.getPrecioLabor()))
				);
		labor.setPrecioLabor(PropertyHelper.parseDouble(this.textCostoLaborHa.textProperty().get()).doubleValue());
		this.textCostoLaborHa.textProperty().addListener((obj,old,n)->{	
			
			Number nuevoPrecio =PropertyHelper.parseDouble(n);// converter.fromString(n);
			labor.setPrecioLabor(nuevoPrecio.doubleValue());
			properties.setProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION, PropertyHelper.formatDouble(nuevoPrecio));
		});

		StringConverter<Number> converter = PropertyHelper.buildStringConverter();
		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);
		this.comboClasificador.setConverter(Clasificador.clasificadorStringConverter());
		textNombre.textProperty().set(labor.getNombre());
		textNombre.textProperty().addListener((obj,old,nu)->labor.setNombre(nu));

		PropertyHelper.bindDateToObjectProperty(
				labor::getFecha,
				labor::setFecha,
				datePickerFecha.valueProperty(),
				labor.getConfigLabor().getConfigProperties(),
				Labor.FECHA_KEY);	
	}





	public void init() {
		this.getDialogPane().setContent(content);

	}

	public static Optional<FertilizacionLabor> config(FertilizacionLabor labor2) {
		Optional<FertilizacionLabor> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(FertilizacionConfigDialogController.class.getResource(
					FERT_CONFIG_DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
			myLoader.load();//aca se crea el constructor
			FertilizacionConfigDialogController controller = ((FertilizacionConfigDialogController) myLoader.getController());
			controller.setLabor(labor2);
			controller.init();
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+FERT_CONFIG_DIALOG_FXML); //$NON-NLS-1$
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}




}
