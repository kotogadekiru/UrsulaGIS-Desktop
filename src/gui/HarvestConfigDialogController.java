package gui;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import utils.DAH;
import dao.Clasificador;
import dao.Labor;
import dao.config.Cultivo;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import gui.utils.DateConverter;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class HarvestConfigDialogController  extends Dialog<CosechaLabor>{
	

	private static final String HARVEST_CONFIG_DIALOG_FXML = "HarvestConfigDialog.fxml"; //$NON-NLS-1$

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
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("HarvestConfigDialogController.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		stage.getIcons().add(new Image(JFXMain.ICON));

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

		if(cols.indexOf(comboRend.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje")); //$NON-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboAnch.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje2")); //$NON-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboDist.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje3")); //$NON-NLS-1$
			isValid=false;
		}
		if(cols.indexOf(comboCurs.getValue())==-1){
			message.append(Messages.getString("HarvestConfigDialogController.mensaje4")); //$NON-NLS-1$
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
		if(!isValid){
			Alert alert = new Alert(AlertType.ERROR, message.toString(), ButtonType.OK);
			alert.initOwner(this.getDialogPane().getScene().getWindow());
			alert.setTitle(Messages.getString("HarvestConfigDialogController.title2")); //$NON-NLS-1$
			alert.showAndWait();

		}

		return isValid;
	}



	public void setLabor(CosechaLabor l) {
		this.labor = l;

		List<String> availableColums = labor.getAvailableColumns();
		availableColums.removeIf(s->s.length()>10);
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});
		availableColums.add(Labor.NONE_SELECTED);

		//TODO si avalilableColumns contiene las columnas estandar seleccionarlas


//		this.comboVelo.setItems(FXCollections.observableArrayList(availableColums));
//
//		this.comboVelo.valueProperty().bindBidirectional(labor.colVelocidad);


		//	this.comboVelo.getSelectionModel().select(-1);


		//comboElev
		this.comboElev.setItems(FXCollections.observableArrayList(availableColums));

		this.comboElev.valueProperty().bindBidirectional(labor.colElevacion);



		// colRendimiento;
		this.comboRend.setItems(FXCollections.observableArrayList(availableColums));

		this.comboRend.valueProperty().bindBidirectional(labor.colRendimiento);
		

		//		int index = availableColums.indexOf(labor.colRendimiento);
		//		comboRend.getSelectionModel().select(index);


		//colAncho;
		//this.comboAnch.setItems(FXCollections.observableArrayList(availableColums));
		this.comboAnch.getItems().addAll(availableColums);

		this.comboAnch.valueProperty().bindBidirectional(labor.colAncho);


		//		this.comboAnch.valueProperty().addListener((ov,old,nv)->{
		//			System.out.println("comboAncho new value "+nv);
		//			labor.colAncho.set(nv);
		//		});
		//
		//		this.comboAnch.setConverter(new StringConverter<String>() {
		//			@Override
		//			public String toString(String s) {
		//				System.out.println("convirtiendo "+s+" a string");
		//				if(!comboAnch.getItems().contains(s)){
		//					comboAnch.getItems().add(s);
		//				}
		//				return s;
		//			}
		//
		//			@Override
		//			public String fromString(String s) {
		//				return s;
		//			}
		//		});
		//this.comboAnch.setEditable(true);

		//colCurso;
		this.comboCurs.setItems(FXCollections.observableArrayList(availableColums));
		this.comboCurs.valueProperty().bindBidirectional(labor.colCurso);

		//colDistancia;
		this.comboDist.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDist.valueProperty().bindBidirectional(labor.colDistancia);

		//colPasada ;


		//		this.comboPasa.setItems(FXCollections.observableArrayList(availableColums));
		//		this.comboPasa.valueProperty().bindBidirectional(labor.colPasada);

		//colPasada ;
		this.comboCultivo.setItems(FXCollections.observableArrayList(DAH.getAllCultivos()));
		this.comboCultivo.getSelectionModel().select(labor.getCultivo());
		this.comboCultivo.valueProperty().addListener((obj,old,n)->{
			labor.cultivo=n;
			if(n!=null)labor.config.getConfigProperties().setProperty(CosechaLabor.CosechaLaborConstants.PRODUCTO_DEFAULT,n.getNombre());
		});
		


		//StringConverter<Number> converter = new NumberStringConverter();//FIXME corregir que el separador de miles
		DecimalFormat converter = new DecimalFormat("0.00"){ //$NON-NLS-1$
	            @Override
	            public Object parseObject(String source)  {
	            	if("".equals(source)||source==null){ //$NON-NLS-1$
	            		source="0.00"; //$NON-NLS-1$
	            	}
	            	try {
						return super.parseObject(source);
					} catch (ParseException e) {
						e.printStackTrace();
						return new Double(0.0);
					
					}
	            }
		};
		
		converter.setGroupingUsed(true);
		converter.setGroupingSize(3);

		//textPrecioGrano
		//Bindings.bindBidirectional(this.textPrecioGrano.textProperty(), labor.precioGranoProperty, converter);
		//TODO init textPrecioGrano con el valor de properties o el valor de la labor
		if(labor.getPrecioGrano()!=null) {
			this.textPrecioGrano.textProperty().set(labor.getPrecioGrano().toString());
		}else {
			this.textPrecioGrano.textProperty().set(labor.config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.PRECIO_GRANO, labor.getPrecioGrano().toString()));
		}
		this.textPrecioGrano.textProperty().addListener((obj,old,n)->{
			try {
				labor.setPrecioInsumo(converter.parse(n).doubleValue());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			//config.getConfigProperties().getPropertyOrDefault(CosechaLabor.PRECIO_GRANO,"0")
			labor.config.getConfigProperties().setProperty(CosechaLabor.CosechaLaborConstants.PRECIO_GRANO, n);
		});

		//textCostoCosechaHa
		//Bindings.bindBidirectional(this.textCostoCosechaHa.textProperty(), labor.precioLaborProperty, converter);
		if(labor.getPrecioLabor()!=null) {
			this.textCostoCosechaHa.textProperty().set(labor.getPrecioLabor().toString());
		}else {
			this.textCostoCosechaHa.textProperty().set(labor.config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_HA, labor.getPrecioLabor().toString()));
		}
		//this.textCostoCosechaHa.textProperty().set(labor.config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_HA, labor.getPrecioLabor().toString()));
		this.textCostoCosechaHa.textProperty().addListener((obj,old,n)->{
			try {
				labor.setPrecioLabor(converter.parse(n).doubleValue());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			labor.config.getConfigProperties().setProperty(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_HA, n);
		});
		

		//textCostoCosechaTn
		//Bindings.bindBidirectional(this.textCostoCosechaTn.textProperty(), labor.costoCosechaTnProperty, converter);
		if(labor.getCostoCosechaTn()!=null) {
			this.textCostoCosechaTn.textProperty().set(labor.getCostoCosechaTn().toString());
		}else {
			this.textCostoCosechaTn.textProperty().set(labor.config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_TN, labor.getCostoCosechaTn().toString()));
		}
	//	this.textCostoCosechaTn.textProperty().set(labor.config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_TN, labor.getCostoCosechaTn().toString()));
		this.textCostoCosechaTn.textProperty().addListener((obj,old,n)->{
			try {
				labor.setCostoCosechaTn(converter.parse(n).doubleValue());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			labor.config.getConfigProperties().setProperty(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_TN, n);
		});

		//textAnchoDef
		Bindings.bindBidirectional(this.textAnchoDef.textProperty(), labor.anchoDefaultProperty, converter);

		//textPorcCorreccion
		Bindings.bindBidirectional(this.textPorcCorreccion.textProperty(), labor.correccionCosechaProperty, converter);

		//textMaxRinde
		Bindings.bindBidirectional(this.textMaxRinde.textProperty(), labor.maxRindeProperty, converter);

		//textMinRinde
		Bindings.bindBidirectional(this.textMinRinde.textProperty(), labor.minRindeProperty, converter);

		//textDistanciasRegimen
		Bindings.bindBidirectional(this.textDistanciasRegimen.textProperty(), labor.config.cantDistanciasEntradaRegimenProperty(), converter);

		//textMaxSuper
	//	Bindings.bindBidirectional(this.textMaxSuper.textProperty(), labor.config.cantMaxGeometriasSuperpuestasProperty(), converter);

		//textSupMin
		Bindings.bindBidirectional(this.textSupMin.textProperty(), labor.config.supMinimaProperty(), converter);

		//TODO cambiar cbMetrosPorUnidad a ComboBox para que pueda ser editable
		Map<String,Double> unidades = new HashMap<String,Double>();
		unidades.put(Messages.getString("HarvestConfigDialogController.12"),1d); //$NON-NLS-1$
		unidades.put(Messages.getString("HarvestConfigDialogController.13"),0.0254); //$NON-NLS-1$
		unidades.put(Messages.getString("HarvestConfigDialogController.14"),0.01d); //$NON-NLS-1$
		unidades.put(Messages.getString("HarvestConfigDialogController.15"),0.001d); //$NON-NLS-1$
		
		
		this.cbMetrosPorUnidad.setItems(FXCollections.observableArrayList(unidades.keySet()));
		this.cbMetrosPorUnidad.valueProperty().addListener((ov,old,nv)->{
			labor.config.valorMetrosPorUnidadDistanciaProperty().set(unidades.get(nv));
		});

	double configured=labor.config.valorMetrosPorUnidadDistanciaProperty().get();
	unidades.forEach((key,value)->{
		if(value.equals(configured)){
			cbMetrosPorUnidad.getSelectionModel().select(key);//
		}
	});
	
		//textMetrosPorUnidad
	//	Bindings.bindBidirectional(property1, property2);bindBidirectional(, labor.config.valorMetrosPorUnidadDistanciaProperty(), converter);
		
		//labor.config.valorMetrosPorUnidadDistanciaProperty().get());	

		//textAnchoFiltro
		Bindings.bindBidirectional(this.textAnchoFiltro.textProperty(), labor.config.anchoFiltroOutlayersProperty(), converter);

		//textCorrimientoPesada
		Bindings.bindBidirectional(this.textCorrimientoPesada.textProperty(), labor.config.valorCorreccionPesadaProperty(), converter);

		
		StringConverter<Number> nsConverter = new NumberStringConverter(){
			@Override
			public Number fromString(String s){
				Number d;
				try {
					d = converter.parse(s);
					return d.doubleValue()/100;
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0;
				
			}
			
			@Override 
			public String toString(Number n){
				return converter.format(n.doubleValue()*100);
			}
		};
		//textToleranciaCV
		Bindings.bindBidirectional(this.textToleranciaCV.textProperty(), labor.config.toleranciaCVProperty(), nsConverter);

		//textDistTolera
		//		this.textDistTolera.textProperty().set(labor.config.cantDistanciasToleraProperty().getValue().toString());
		//		this.textDistTolera.textProperty().addListener((ov,old,nv)->{labor.config.cantDistanciasToleraProperty().set(Integer.parseInt(nv));});

		Bindings.bindBidirectional(this.textDistTolera.textProperty(), labor.config.cantDistanciasToleraProperty(), converter);

		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);
		//choiceClasificador.textProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);

		//textNombre.textProperty().bindBidirectional(labor.nombreProperty);
		textNombre.textProperty().set(labor.getNombre());
		textNombre.textProperty().addListener((obj,old,nu)->labor.setNombre(nu));

		CosechaConfig cosechaConfig = (CosechaConfig) labor.config;
		chkOutlayers.selectedProperty().bindBidirectional(labor.config.correccionOutlayersProperty());
		chkAncho.selectedProperty().bindBidirectional(labor.config.correccionAnchoProperty());
		chkDemora.selectedProperty().bindBidirectional(labor.config.correccionDemoraPesadaProperty());
		chkDemora.setTooltip(new Tooltip(Messages.getString("HarvestConfigDialogController.16"))); //$NON-NLS-1$
		chkRinde.selectedProperty().bindBidirectional(cosechaConfig.correccionRindeProperty());
		chkSuperposicion.selectedProperty().bindBidirectional(labor.config.correccionSuperposicionProperty());
		chkDistancia.selectedProperty().bindBidirectional(cosechaConfig.correccionDistanciaProperty());

		chkFlow.selectedProperty().bindBidirectional(cosechaConfig.correccionFlowToRindeProperty());

		chkResumirGeometrias.selectedProperty().bindBidirectional(labor.config.resumirGeometriasProperty());
		
		//StringConverter<LocalDate> dateConverter = this.datePickerFecha.getConverter();

		datePickerFecha.setValue(DateConverter.asLocalDate(l.fecha));
		datePickerFecha.setConverter(new DateConverter());
		datePickerFecha.valueProperty().addListener((obs, bool1, n) -> {
			l.setFecha(DateConverter.asDate(n));
			//l.fechaProperty.setValue(bool2);
		});
	//	Bindings.bindBidirectional(this.datePickerFecha.valueProperty()., l.fechaProperty, );
		
	}




	public void init() {
		this.getDialogPane().setContent(content);

	}



	public static Optional<CosechaLabor> config(CosechaLabor labor) {
		Optional<CosechaLabor> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(HarvestConfigDialogController.class.getResource(
					HARVEST_CONFIG_DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
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
