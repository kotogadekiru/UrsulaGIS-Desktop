package mmg.gui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import dao.Clasificador;
import dao.CosechaLabor;
import dao.Producto;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class HarvestConfigDialogController  extends Dialog<CosechaLabor>{
	private static final String HARVEST_CONFIG_DIALOG_FXML = "HarvestConfigDialog.fxml";
	
	@FXML
	private VBox content;

	@FXML
	private ComboBox<String> comboVelo;//ok

	@FXML
	private ComboBox<String> comboRend;//ok

	@FXML
	private TextField textPrecioGrano;//ok

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

	@FXML
	private TextField textMaxSuper;//ok

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
	private TextField textMetrosPorUnidad;//ok

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
	private ComboBox<Producto> comboCultivo;

	@FXML
	private CheckBox chkDistancia;
	
	@FXML
	private CheckBox chkFlow;

	@FXML
	private CheckBox chkResumirGeometrias;
	
	private CosechaLabor labor;
	
	
	public HarvestConfigDialogController() {
		super();
		System.out.println("construyendo el controller");

		this.setTitle("Configure las opciones para su cosecha");
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
		return (cols.indexOf(comboAnch.getValue())>-1)&&
				(cols.indexOf(comboCurs.getValue())>-1)&&
				(cols.indexOf(comboDist.getValue())>-1)&&
				(cols.indexOf(comboElev.getValue())>-1)&&
			//	(cols.indexOf(comboPasa.getValue())>-1)&&
				(cols.indexOf(comboRend.getValue())>-1)&&
				(cols.indexOf(comboVelo.getValue())>-1);
	}



	public void setLabor(CosechaLabor l) {
		this.labor = l;

		List<String> availableColums = labor.getAvailableColumns();
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});

		//TODO si avalilableColumns contiene las columnas estanddar seleccionarlas
		
		
		this.comboVelo.setItems(FXCollections.observableArrayList(availableColums));
	
			this.comboVelo.valueProperty().bindBidirectional(labor.colVelocidad);
		
		
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
		this.comboCultivo.setItems(FXCollections.observableArrayList(Producto.productos.values()));
		this.comboCultivo.valueProperty().bindBidirectional(labor.producto);
		

		StringConverter<Number> converter = new NumberStringConverter();

		//textPrecioGrano
		Bindings.bindBidirectional(this.textPrecioGrano.textProperty(), labor.precioGranoProperty, converter);

		//textCostoCosechaHa
		Bindings.bindBidirectional(this.textCostoCosechaHa.textProperty(), labor.precioLaborProperty, converter);

		//textCostoCosechaTn
		Bindings.bindBidirectional(this.textCostoCosechaTn.textProperty(), labor.costoCosechaTnProperty, converter);

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
		Bindings.bindBidirectional(this.textMaxSuper.textProperty(), labor.config.cantMaxGeometriasSuperpuestasProperty(), converter);

		//textSupMin
		Bindings.bindBidirectional(this.textSupMin.textProperty(), labor.config.supMinimaProperty(), converter);

		//textMetrosPorUnidad
		Bindings.bindBidirectional(this.textMetrosPorUnidad.textProperty(), labor.config.valorMetrosPorUnidadDistanciaProperty(), converter);

		//textAnchoFiltro
		Bindings.bindBidirectional(this.textAnchoFiltro.textProperty(), labor.config.anchoFiltroOutlayersProperty(), converter);

		//textCorrimientoPesada
		Bindings.bindBidirectional(this.textCorrimientoPesada.textProperty(), labor.config.valorCorreccionPesadaProperty(), converter);

		//textToleranciaCV
		Bindings.bindBidirectional(this.textToleranciaCV.textProperty(), labor.config.toleranciaCVProperty(), converter);

		//textDistTolera
		//		this.textDistTolera.textProperty().set(labor.config.cantDistanciasToleraProperty().getValue().toString());
		//		this.textDistTolera.textProperty().addListener((ov,old,nv)->{labor.config.cantDistanciasToleraProperty().set(Integer.parseInt(nv));});

		Bindings.bindBidirectional(this.textDistTolera.textProperty(), labor.config.cantDistanciasToleraProperty(), converter);
		
		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);
		
		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);
		//choiceClasificador.textProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);

		textNombre.textProperty().bindBidirectional(labor.nombreProperty);

		chkOutlayers.selectedProperty().bindBidirectional(labor.config.correccionOutlayersProperty());
		chkAncho.selectedProperty().bindBidirectional(labor.config.correccionAnchoProperty());
		chkDemora.selectedProperty().bindBidirectional(labor.config.correccionDemoraPesadaProperty());
		chkDemora.setTooltip(new Tooltip("Permite adelantar o atrazar cada pesada y estirar la pesada de entrada en regimen"));
		chkRinde.selectedProperty().bindBidirectional(labor.config.correccionRindeProperty());
		chkSuperposicion.selectedProperty().bindBidirectional(labor.config.correccionSuperposicionProperty());
		chkDistancia.selectedProperty().bindBidirectional(labor.config.correccionDistanciaProperty());
		
		chkFlow.selectedProperty().bindBidirectional(labor.config.correccionFlowToRindeProperty());
		
		chkResumirGeometrias.selectedProperty().bindBidirectional(labor.config.resumirGeometriasProperty());
	}




	public void init() {
		this.getDialogPane().setContent(content);

	}



	public static Optional<CosechaLabor> config(CosechaLabor labor) {
		Optional<CosechaLabor> ret = Optional.empty();
	try{
		FXMLLoader myLoader = new FXMLLoader(HarvestConfigDialogController.class.getResource(
				HARVEST_CONFIG_DIALOG_FXML));
		myLoader.load();//aca se crea el constructor
		HarvestConfigDialogController controller = ((HarvestConfigDialogController) myLoader.getController());
		controller.setLabor(labor);
		controller.init();
		ret = controller.showAndWait();
	} catch (IOException e1) {
		System.err.println("no se pudo levantar el fxml "+HARVEST_CONFIG_DIALOG_FXML);
		e1.printStackTrace();
		System.exit(0);
	}
		return ret;
	}




}
