package com.ursulagis.desktop.gui;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.siembra.SiembraConfig;
import com.ursulagis.desktop.dao.siembra.SiembraConfig.Unidad;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.utils.DateConverter;
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
import com.ursulagis.desktop.utils.DAH;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class SiembraConfigDialogController  extends Dialog<SiembraLabor>{
	private static final String SEED_CONFIG_DIALOG_FXML = "SiembraConfigDialog.fxml"; //$NON-NLS-1$

	@FXML
	private VBox content;


	@FXML
	private ComboBox<String> comboDosis;//ok

	@FXML
	private ComboBox<String> comboDosisUnit;//ok

	@FXML
	private ComboBox<String> precioInsumoUnit;//ok

	//	@FXML
	//	private ComboBox<String> comboUnidadInsumo;

	@FXML
	private TextField textPrecioInsumo;//ok


	@FXML
	private TextField textEntresurco;//ok

	//	@FXML
	//	private TextField textSemillasBolsa;//ok


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
	private ComboBox<Semilla> comboInsumo;


	private SiembraLabor labor;

	//StringConverter<Number> converter = new NumberStringConverter(Messages.getLocale());
	//	DecimalFormat converter = PropertyHelper.getDoubleConverter();

	public SiembraConfigDialogController() {
		super();
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("SiembraConfigDialogController.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		stage.setMinWidth(1000);
		stage.setMinHeight(600);
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

		//XXX aca se setea Result converter
		this.setResultConverter(e -> {		
			if(ButtonType.OK.equals(e)){		
				return convert();

			}else{
				return null;
			}
		});
	}



	private SiembraLabor convert() {		
		applyNuevoPrecioInsumo(this.textPrecioInsumo.getText());			

		//En verdad deberia editar la labor aca. no antes de que se confirmen los cambios
		if(chkMakeDefault.selectedProperty().get()){
			labor.getConfiguracion().save();
		}				
		return labor;
	}



	private boolean validarDialog() {
		List<String> cols = labor.getAvailableColumns();
		StringJoiner message = new StringJoiner("\n");
		boolean isValid =true;

		//		return 	(cols.indexOf(comboElev.getValue())>-1)&&
		//				//	(cols.indexOf(comboPasa.getValue())>-1)&&
		//				(cols.indexOf(comboDosis.getValue())>-1);
		try{
			PropertyHelper.parseDouble(textPrecioInsumo.getText());
			//converter.fromString(textPrecioInsumo.getText());
		}catch(Exception e) {
			isValid=false;
			message.add("precio is invalid");
		}

		try{
			PropertyHelper.parseDouble(textCostoLaborHa.getText());
		}catch(Exception e) {
			isValid=false;
			message.add("costo labor is invalid");
		}

		try{
			Number entresurco = PropertyHelper.parseDouble(textEntresurco.getText());
			if(entresurco.doubleValue()<= 0)isValid=false;
		}catch(Exception e) {
			isValid=false;
			message.add("entresurco is invalid");
		}


		if(cols.indexOf(comboDosis.getValue())==-1){
			message.add(Messages.getString("SiembraConfigDialogController.mensaje")); //$NON-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboElev.getValue())==-1){
			//			message.append("Debe seleccionar la columna Elevacion\n");
			//			isValid=false;
			labor.colElevacion.set(Labor.NONE_SELECTED);
		}

		if(!isValid){
			Alert alert = new Alert(AlertType.ERROR, 
					message.toString()
					, ButtonType.OK);
			alert.initOwner(this.getDialogPane().getScene().getWindow());
			alert.setTitle(Messages.getString("SiembraConfigDialogController.title2")); //$NON-NLS-1$
			alert.showAndWait();

		}

		return isValid;

	}


	public void setLabor(SiembraLabor l) {
		this.labor = l;

		List<String> availableColums = labor.getAvailableColumns();
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});

		availableColums.add(Labor.NONE_SELECTED);

		SiembraConfig config = labor.getConfiguracion();
		Configuracion props = config.getConfigProperties();
		PropertyHelper.bindDateToObjectProperty(
				labor::getFecha,
				labor::setFecha,
				datePickerFecha.valueProperty(),
				props,
				Labor.FECHA_KEY);	
		//comboElev
		this.comboElev.setItems(FXCollections.observableArrayList(availableColums));
		this.comboElev.valueProperty().bindBidirectional(labor.colElevacion);



		// colRendimiento;
		this.comboDosis.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDosis.valueProperty().bindBidirectional(labor.colDosisSemilla);


		this.comboInsumo.setItems(FXCollections.observableArrayList(DAH.getAllSemillas()));//Semilla.semillas.values()));

		this.comboInsumo.valueProperty().addListener((obj,old,n)->{		
			labor.setSemilla(n);
			props.setProperty(SiembraLabor.SEMILLA_DEFAULT, n.getNombre());
		});

		String sDefautlName = props.getPropertyOrDefault(SiembraLabor.SEMILLA_DEFAULT, "");

		if(labor.getSemilla()!=null) {
			this.comboInsumo.getSelectionModel().select(labor.getSemilla());
		} else {
			Optional<Semilla> sDefault = this.comboInsumo.getItems().stream().filter((s)->s.getNombre().equals(sDefautlName)).findFirst();
			if(sDefault.isPresent()) {
				this.comboInsumo.getSelectionModel().select(sDefault.get());
			}
		}
		
		//textPrecioGrano
		configPrecioInsumoUnit();
		String precioInsumoLaborUsdKg = props.getPropertyOrDefault(SiembraLabor.COLUMNA_PRECIO_SEMILLA, PropertyHelper.formatDouble(labor.getPrecioInsumo()));
		SiembraConfig.Unidad unidadPrecioInsumo = labor.getConfiguracion().precioInsumoUnitProperty().get();
		String precioDialog =precioInsumoLaborUsdKg;
		if(unidadPrecioInsumo == SiembraConfig.Unidad.Bolsa) {
			System.out.println("cargando una labor con precio en bolsa");
			double usdKg =PropertyHelper.parseDouble(precioInsumoLaborUsdKg).doubleValue();
			double semBolsa = labor.getSemilla().getCultivo().getSemPorBolsa();
			double kSemBolsa=semBolsa/1000;
			double grBolsa = kSemBolsa*labor.getSemilla().getPesoDeMil();
			double kgBolsa = grBolsa/1000;
			
			precioDialog=PropertyHelper.formatDouble(usdKg*kgBolsa);
		}
		textPrecioInsumo.textProperty().set(
				precioDialog
				);

		//TODO si la unidad del precioinsumo es bolsa convertir del precio por kg a precio por bolsa
		
		labor.setPrecioInsumo(
				PropertyHelper.parseDouble(textPrecioInsumo.textProperty().get())
				.doubleValue());

		//		this.textPrecioInsumo.textProperty().addListener((obj,old,n)->{	
		//			applyNuevoPrecioInsumo(n);			
		//		});

		//textCostoCosechaHa
		//Bindings.bindBidirectional(this.textCostoLaborHa.textProperty(), labor.precioLaborProperty, converter);
		//TODO tomar el valor de la labor y si es null levantar la configuracion. sino tomar el valor de la labor.
		this.textCostoLaborHa.textProperty().set(props.getPropertyOrDefault(SiembraLabor.COSTO_LABOR_SIEMBRA, labor.getPrecioLabor().toString()));


		labor.setPrecioLabor(
				PropertyHelper.parseDouble(this.textCostoLaborHa.textProperty().get()).doubleValue()
				);


		
		this.textCostoLaborHa.textProperty().addListener((obj,old,n)->{	
			Number nuevoPrecio;

			nuevoPrecio = PropertyHelper.parseDouble(n);
			labor.setPrecioLabor(nuevoPrecio.doubleValue());
			props.setProperty(SiembraLabor.COSTO_LABOR_SIEMBRA, PropertyHelper.formatDouble(nuevoPrecio));


		});

		//System.out.println("valor entresurco Labor = "+converter.toString(labor.getEntreSurco()));
		//System.out.println("valor entresurco default = "+labor.config.getConfigProperties().getPropertyOrDefault(SiembraLabor.ENTRE_SURCO_DEFAULT_KEY,null));
		this.textEntresurco.textProperty().set(props.getPropertyOrDefault(SiembraLabor.ENTRE_SURCO_DEFAULT_KEY, PropertyHelper.formatDouble(labor.getEntreSurco())));

		labor.setEntreSurco(PropertyHelper.parseDouble(this.textEntresurco.textProperty().get()).doubleValue());
		//inicializo el entresurco de la labor con lo del texProperty
		//this.textEntresurco.textProperty().set(converter.toString(labor.getEntreSurco()));
		this.textEntresurco.textProperty().addListener((obj,old,n)->{
			Number nuevoEntresurco;

			nuevoEntresurco = PropertyHelper.parseDouble(n);
			labor.setEntreSurco(nuevoEntresurco.doubleValue());
			props.setProperty(SiembraLabor.ENTRE_SURCO_DEFAULT_KEY,PropertyHelper.formatDouble(nuevoEntresurco));

		});

		//Bindings.bindBidirectional(this.textEntresurco.textProperty(), labor.entreSurco, converter);
		//		
		//		Bindings.bindBidirectional(this.textSemillasBolsa.textProperty(), labor.semillasPorBolsa, converter);


		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, PropertyHelper.getDoubleConverter());

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);
		this.comboClasificador.setConverter(Clasificador.clasificadorStringConverter());

		//cbMetrosPorUnidad a ComboBox para que pueda ser editable
		Map<String,SiembraConfig.Unidad> unidades = new HashMap<String,SiembraConfig.Unidad>();
		unidades.put(Messages.getString("SiembraConfigDialogController.kgHa"),SiembraConfig.Unidad.kgHa); //$NON-NLS-1$
		unidades.put(Messages.getString("SiembraConfigDialogController.milPlaHa"),SiembraConfig.Unidad.milPlaHa); //$NON-NLS-1$
		unidades.put(Messages.getString("SiembraConfigDialogController.pla10MtLineal"),SiembraConfig.Unidad.pla10MtLineal); //$NON-NLS-1$
		unidades.put(Messages.getString("SiembraConfigDialogController.pla1MtLineal"),SiembraConfig.Unidad.pla1MtLineal); //$NON-NLS-1$
		unidades.put(Messages.getString("SiembraConfigDialogController.plaMetroCuadrado"),SiembraConfig.Unidad.plaMetroCuadrado); //$NON-NLS-1$


		this.comboDosisUnit.setItems(FXCollections.observableArrayList(unidades.keySet()));
		this.comboDosisUnit.valueProperty().addListener((ov,old,nv)->{
			labor.getConfiguracion().dosisUnitProperty().set(unidades.get(nv));
		});

		SiembraConfig.Unidad configured = labor.getConfiguracion().dosisUnitProperty().get();
		unidades.forEach((key,value)->{
			if(value.equals(configured)){
				comboDosisUnit.getSelectionModel().select(key);
			}
		});

		//	this.comboDosisUnit.setItems(FXCollections.observableArrayList(unidades.keySet()));
		//	this.comboDosisUnit.valueProperty().bindBidirectional(labor.config.dosisUnitProperty());

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

	private void applyNuevoPrecioInsumo(String n) {
		Number nuevoPrecio;		
		nuevoPrecio = PropertyHelper.parseDouble(n);

		Unidad unidadInsumo = labor.getConfiguracion().precioInsumoUnitProperty().getValue();
		System.out.println("editando precio con unidad "+unidadInsumo);
		if(unidadInsumo==Unidad.Bolsa) {
			System.out.println("ingresando un precio por bolsa "+nuevoPrecio.doubleValue());
			// convertir de precio por bolsa a precio por kg
			Semilla s = labor.getSemilla();
			Double semBolsa = s.getCultivo().getSemPorBolsa();
			Double pMil = s.getPesoDeMil();
			Double kgBolsa = semBolsa*pMil;//semBolsa*pMil/1000000;
			System.out.println("peso bolsa kg="+kgBolsa);
			Double precioKg = nuevoPrecio.doubleValue()*1000000/kgBolsa;
			nuevoPrecio=precioKg;
			//TODO fix error de redondeo convierte 180 en 179,994 y 200 en 199,987 (depende del peso de mil)
			System.out.println("precio por kg queda en "+nuevoPrecio.doubleValue());
		}
		labor.setPrecioInsumo(nuevoPrecio.doubleValue());
		labor.getConfiguracion().getConfigProperties().setProperty(SiembraLabor.COLUMNA_PRECIO_SEMILLA, nuevoPrecio.toString());


	}
	public void configPrecioInsumoUnit() {
		//cbMetrosPorUnidad a ComboBox para que pueda ser editable
		Map<String,SiembraConfig.Unidad> unidades = new HashMap<String,SiembraConfig.Unidad>();
		unidades.put(Messages.getString("SiembraConfigDialogController.kg"),SiembraConfig.Unidad.Kg); //$NON-NLS-1$
		unidades.put(Messages.getString("SiembraConfigDialogController.bolsa"),SiembraConfig.Unidad.Bolsa); //$NON-NLS-1$


		this.precioInsumoUnit.setItems(FXCollections.observableArrayList(unidades.keySet()));
		this.precioInsumoUnit.valueProperty().addListener((ob,old,nv)->{
			System.out.println("cambiando unidad insumo de "+old+" a "+nv);
			labor.getConfiguracion().precioInsumoUnitProperty().set(unidades.get(nv));

		});

		SiembraConfig.Unidad configured = labor.getConfiguracion().precioInsumoUnitProperty().get();
		unidades.forEach((key,value)->{
			if(value.equals(configured)){
				precioInsumoUnit.getSelectionModel().select(key);
			}
		});
	}

	public void init() {
		this.getDialogPane().setContent(content);

	}



	public static Optional<SiembraLabor> config(SiembraLabor labor2) {
		Optional<SiembraLabor> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(SiembraConfigDialogController.class.getResource(
					SEED_CONFIG_DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
			myLoader.load();//aca se crea el constructor
			SiembraConfigDialogController controller = ((SiembraConfigDialogController) myLoader.getController());
			controller.setLabor(labor2);
			controller.init();
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+SEED_CONFIG_DIALOG_FXML); //$NON-NLS-1$
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}




}
