package com.ursulagis.desktop.gui;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import javafx.util.converter.PercentageStringConverter;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.utils.DateConverter;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class HarvestConfigDialogController  extends Dialog<CosechaLabor>{


	private static final String HARVEST_CONFIG_DIALOG_FXML = "HarvestConfigDialog.fxml"; //-NLS-1$

	@FXML
	private VBox content;

	//	@FXML
	//	private ComboBox<String> comboVelo;//ok

	@FXML
	private ComboBox<String> comboRend;//ok

	@FXML
	private TextField textPrecioGrano;//ok

	@FXML
	private DatePicker datePickerFecha;//ok

	@FXML
	private CheckBox chkOutlayers;//ok

	@FXML
	private ComboBox<String> comboDist;//ok

	@FXML
	private ComboBox<String> comboAnch;//ok

	@FXML
	private ComboBox<String> comboElev;//ok

	@FXML
	private TextField textNombre;//ok

	@FXML
	private TextField textSupMin;//ok

	@FXML
	private TextField textCostoCosechaHa;//ok

	@FXML
	private TextField textCostoCosechaTn;//

	//@FXML
	//private TextField textMaxSuper;//ok

	@FXML
	private TextField textAnchoDef;//ok

	@FXML
	private CheckBox chkAncho;//ok

	@FXML
	private TextField textPorcCorreccion;//ok

	@FXML
	private TextField textMaxRinde;

	@FXML
	private TextField textMinRinde;

	@FXML
	private TextField textClasesClasificador;

	@FXML
	private CheckBox chkDemora;//ok

	//	@FXML
	//	private ComboBox<String> comboPasa;//ok

	@FXML
	private TextField textDistanciasRegimen;//ok

	@FXML
	private ChoiceBox<String> cbMetrosPorUnidad;//ok

	@FXML
	private TextField textAnchoFiltro;//ok

	@FXML
	private TextField textCorrimientoPesada;//ok

	@FXML
	private CheckBox chkRinde;//ok

	@FXML
	private ComboBox<String> comboCurs;//ok

	@FXML
	private CheckBox chkMakeDefault;//ok

	@FXML
	private CheckBox chkSuperposicion;//ok

	@FXML
	private TextField textToleranciaCV;//ok

	@FXML
	private TextField textDistTolera;//ok

	@FXML
	private ComboBox<String> comboClasificador;//ok


	@FXML
	private ComboBox<Cultivo> comboCultivo;

	@FXML
	private CheckBox chkDistancia;

	@FXML
	private CheckBox chkFlow;

	@FXML
	private CheckBox chkResumirGeometrias;

	private CosechaLabor labor;


	public HarvestConfigDialogController() {
		super();
		//System.out.println("construyendo el controller"); //-NLS-1$

		this.setTitle(Messages.getString("HarvestConfigDialogController.title")); //-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		//stage.getIcons().addAll(JFXMain.stage.getIcons());
		stage.getIcons().addAll(JFXMain.stage.getIcons());
		this.getDialogPane().getButtonTypes().add(ButtonType.OK);
		this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		this.setResizable(true);

		final Button btOk = (Button) this.getDialogPane().lookupButton(ButtonType.OK);
		btOk.addEventFilter(ActionEvent.ACTION, event -> {
			if (!validarDialog()) {
				System.out.println("la configuracion es incorrecta"); //-NLS-1$
				event.consume();
			}
		});

		this.setResultConverter(e -> {		
			if(ButtonType.OK.equals(e)){					
				if(chkMakeDefault.selectedProperty().get()){
					labor.getConfiguracion().save();
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
		if(labor.cultivo== null) {
			message.append(Messages.getString("HarvestConfigDialogController.faltaCultivo")); //-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboRend.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje")); //-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboAnch.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje2")); //-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboDist.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje3")); //-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboCurs.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje4")); //-NLS-1$
			isValid=false;
		}
		
		

		if(cols.indexOf(comboElev.getValue())==-1){
			//			message.append("Debe seleccionar la columna Elevacion\n");
			//			isValid=false;
			labor.colElevacion.set(Labor.NONE_SELECTED);
		}
		//		if(cols.indexOf(comboVelo.getValue())==-1){
		////			message.append("Debe seleccionar la columna velocidad\n");
		////			isValid=false;
		//			labor.colVelocidad.set(Labor.NONE_SELECTED);
		//		}
		
		if(labor.minRindeProperty.get()>labor.maxRindeProperty.get()){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje5")); //-NLS-1$
			isValid=false;
		}
		
		if(!isValid){
			Alert alert = new Alert(AlertType.ERROR, message.toString(), ButtonType.OK);
			alert.initOwner(this.getDialogPane().getScene().getWindow());
			alert.setTitle(Messages.getString("HarvestConfigDialogController.title2")); //-NLS-1$
			alert.showAndWait();

		}

		return isValid;
	}



	public void setLabor(CosechaLabor l) {
		this.labor = l;
		Configuracion config = labor.getConfiguracion().getConfigProperties();
		List<String> availableColums = labor.getAvailableColumns();
		//availableColums.removeIf(s->s.length()>10);//si la columna tiene mas de 10 caracteres no la puedo leer
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});
		availableColums.add(Labor.NONE_SELECTED);

		//si avalilableColumns contiene las columnas estandar seleccionarlas

		//comboElev
		this.comboElev.setItems(FXCollections.observableArrayList(availableColums));
		this.comboElev.valueProperty().bindBidirectional(labor.colElevacion);

		// colRendimiento;
		this.comboRend.setItems(FXCollections.observableArrayList(availableColums));

		this.comboRend.valueProperty().bindBidirectional(labor.colRendimiento);

		//colAncho;
		this.comboAnch.getItems().addAll(availableColums);
		this.comboAnch.valueProperty().bindBidirectional(labor.colAncho);

		//colCurso;
		this.comboCurs.setItems(FXCollections.observableArrayList(availableColums));
		this.comboCurs.valueProperty().bindBidirectional(labor.colCurso);

		//colDistancia;
		this.comboDist.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDist.valueProperty().bindBidirectional(labor.colDistancia);


		//colCultivo ;
		this.comboCultivo.setItems(FXCollections.observableArrayList(DAH.getAllCultivos()));
		
		this.comboCultivo.valueProperty().addListener((obj,old,n)->{
			labor.cultivo=n;
			if(n!=null)config.setProperty(CosechaLabor.CosechaLaborConstants.PRODUCTO_DEFAULT,n.getNombre());
		});
		
		String sDefautlName = config.getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.PRODUCTO_DEFAULT, "");
		 
		if(labor.getCultivo()!=null) {
			this.comboCultivo.getSelectionModel().select(labor.getCultivo());
		} else {
			Optional<Cultivo> sDefault = this.comboCultivo.getItems().stream().filter((s)->s.getNombre().equals(sDefautlName)).findFirst();
			if(sDefault.isPresent()) {
				this.comboCultivo.getSelectionModel().select(sDefault.get());
			}
		}
		
		//this.comboCultivo.getSelectionModel().select(labor.getCultivo());
		
		//DecimalFormat converter = PropertyHelper.getDoubleConverter();
		StringConverter<Number> converter = PropertyHelper.buildStringConverter();

		//textPrecioGrano
		PropertyHelper.bindDoubleToTextProperty(labor::getPrecioInsumo,
				labor::setPrecioInsumo,
				this.textPrecioGrano.textProperty(),
				config,
				CosechaLabor.CosechaLaborConstants.PRECIO_GRANO);
		
		//textCostoCosechaHa		
		PropertyHelper.bindDoubleToTextProperty(labor::getPrecioLabor,
				labor::setPrecioLabor,
				this.textCostoCosechaHa.textProperty(),
				config,
				CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_HA);

		//textCostoCosechaTn
		PropertyHelper.bindDoubleToTextProperty(labor::getCostoCosechaTn,
												labor::setCostoCosechaTn,
												this.textCostoCosechaTn.textProperty(),
												config,
												CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_TN);
		
		//textAnchoDef
		Bindings.bindBidirectional(this.textAnchoDef.textProperty(), labor.anchoDefaultProperty, converter);

		//textPorcCorreccion
		Bindings.bindBidirectional(this.textPorcCorreccion.textProperty(), labor.correccionCosechaProperty, converter);



	//	DecimalFormat converterMax = getMaxDecimalConverter();
		
		//textMaxRinde
//		this.textMaxRinde.textProperty().set(
//				config.getPropertyOrDefault(
//						CosechaLabor.CosechaLaborConstants.MAX_RINDE_KEY, 
//						converterMax.format(labor.maxRindeProperty.doubleValue())
//								 ));
		Bindings.bindBidirectional(this.textMaxRinde.textProperty(), labor.maxRindeProperty,converter);

//		this.textMinRinde.textProperty().set(
//				config.getPropertyOrDefault(
//						CosechaLabor.CosechaLaborConstants.MIN_RINDE_KEY, 
//						converter.format(labor.minRindeProperty.doubleValue())
//								 ));
		Bindings.bindBidirectional(this.textMinRinde.textProperty(), labor.minRindeProperty, converter);

		

		//textDistanciasRegimen
		Bindings.bindBidirectional(this.textDistanciasRegimen.textProperty(), labor.getConfigLabor().cantDistanciasEntradaRegimenProperty(), converter);

		//textSupMin
		Bindings.bindBidirectional(this.textSupMin.textProperty(), labor.getConfigLabor().supMinimaProperty(), converter);

		//TODO cambiar cbMetrosPorUnidad a ComboBox para que pueda ser editable
		Map<String,Double> unidades = new HashMap<String,Double>();
		unidades.put(Messages.getString("HarvestConfigDialogController.12"),1d); //-NLS-1$
		unidades.put(Messages.getString("HarvestConfigDialogController.13"),0.0254); //-NLS-1$
		unidades.put(Messages.getString("HarvestConfigDialogController.14"),0.01d); //-NLS-1$
		unidades.put(Messages.getString("HarvestConfigDialogController.15"),0.001d); //-NLS-1$


		this.cbMetrosPorUnidad.setItems(FXCollections.observableArrayList(unidades.keySet()));
		this.cbMetrosPorUnidad.valueProperty().addListener((ov,old,nv)->{
			labor.getConfigLabor().valorMetrosPorUnidadDistanciaProperty().set(unidades.get(nv));
		});

		double configured=labor.getConfigLabor().valorMetrosPorUnidadDistanciaProperty().get();
		unidades.forEach((key,value)->{
			if(value.equals(configured)){
				cbMetrosPorUnidad.getSelectionModel().select(key);//
			}
		});


		//textAnchoFiltro
		Bindings.bindBidirectional(this.textAnchoFiltro.textProperty(), labor.getConfigLabor().anchoFiltroOutlayersProperty(), converter);

		//textCorrimientoPesada
		Bindings.bindBidirectional(this.textCorrimientoPesada.textProperty(), labor.getConfigLabor().valorCorreccionPesadaProperty(), converter);

		
		StringConverter<Number> nsConverter = new NumberStringConverter(Messages.getLocale()){
			@Override
			public Number fromString(String s){
				Number d=0.0;
				try {
					//s.substring(0, s.length()-1);
					d= PropertyHelper.parseDouble(s);
					//d = converter.parse(s);
					return d.doubleValue()/100;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return 0;
			}

			@Override 
			public String toString(Number n){
				return PropertyHelper.formatDouble(n.doubleValue()*100);
				//return converter.format(n.doubleValue()*100);//+"%";
			}
		};
		//textToleranciaCV
		Bindings.bindBidirectional(this.textToleranciaCV.textProperty(), labor.getConfigLabor().toleranciaCVProperty(), nsConverter);

		Bindings.bindBidirectional(this.textDistTolera.textProperty(), labor.getConfigLabor().cantDistanciasToleraProperty(), converter);

		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);
		this.comboClasificador.setConverter(Clasificador.clasificadorStringConverter());
		//choiceClasificador.textProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);

		//textNombre.textProperty().bindBidirectional(labor.nombreProperty);
		textNombre.textProperty().set(labor.getNombre());
		textNombre.textProperty().addListener((obj,old,nu)->labor.setNombre(nu));

		CosechaConfig cosechaConfig = labor.getConfiguracion();
		chkOutlayers.selectedProperty().bindBidirectional(cosechaConfig.correccionOutlayersProperty());
		chkAncho.selectedProperty().bindBidirectional(cosechaConfig.correccionAnchoProperty());
		chkDemora.selectedProperty().bindBidirectional(cosechaConfig.correccionDemoraPesadaProperty());
		chkDemora.setTooltip(new Tooltip(Messages.getString("HarvestConfigDialogController.16"))); //-NLS-1$
		chkRinde.selectedProperty().bindBidirectional(cosechaConfig.correccionRindeProperty());
		chkSuperposicion.selectedProperty().bindBidirectional(cosechaConfig.correccionSuperposicionProperty());
		chkDistancia.selectedProperty().bindBidirectional(cosechaConfig.correccionDistanciaProperty());

		chkFlow.selectedProperty().bindBidirectional(cosechaConfig.correccionFlowToRindeProperty());

		chkResumirGeometrias.selectedProperty().bindBidirectional(cosechaConfig.resumirGeometriasProperty());

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

	public static Optional<CosechaLabor> config(CosechaLabor labor) {
		Optional<CosechaLabor> ret = Optional.empty();
		try{
			ResourceBundle boundle = Messages.getBoundle();
			if(boundle==null){
				Messages.setLocale(Messages.getLocales().get(0));
				boundle=Messages.getBoundle();
			}
			System.out.println("boundle "+boundle);
			FXMLLoader myLoader = new FXMLLoader(HarvestConfigDialogController.class.getResource(
					HARVEST_CONFIG_DIALOG_FXML));
			myLoader.setResources(boundle);
			myLoader.load();//aca se crea el constructor
			HarvestConfigDialogController controller = ((HarvestConfigDialogController) myLoader.getController());
			controller.setLabor(labor);
			controller.init();
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+HARVEST_CONFIG_DIALOG_FXML); //$NON-NLS-1$
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}
}
