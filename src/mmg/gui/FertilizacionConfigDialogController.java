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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import dao.Clasificador;
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class FertilizacionConfigDialogController  extends Dialog<FertilizacionLabor>{
	private static final String FERT_CONFIG_DIALOG_FXML = "FertilizacionConfigDialog.fxml";

	@FXML
	private VBox content;


	@FXML
	private ComboBox<String> comboDosis;//ok

	@FXML
	private TextField textPrecioFert;//ok

	@FXML
	private CheckBox chkOutlayers;//ok

	@FXML
	private ComboBox<String> comboDistancia;//ok

	@FXML
	private ComboBox<String> comboAnch;//ok

	@FXML
	private ComboBox<String> comboElev;//ok

	@FXML
	private TextField textNombre;//ok
	
	@FXML
	private DatePicker datePickerFecha;//ok

	@FXML
	private TextField textSupMin;//ok

	@FXML
	private TextField textCostoLaborHa;//ok

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
	private ComboBox<String> comboCurso;//ok

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
	private ComboBox<Fertilizante> comboFertilizante;

	@FXML
	private CheckBox chkDistancia;

	@FXML
	private CheckBox chkFlow;

	@FXML
	private CheckBox chkResumirGeometrias;

	private FertilizacionLabor labor;


	public FertilizacionConfigDialogController() {
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
				(cols.indexOf(comboCurso.getValue())>-1)&&
				(cols.indexOf(comboDistancia.getValue())>-1)&&
				(cols.indexOf(comboElev.getValue())>-1)&&
				//	(cols.indexOf(comboPasa.getValue())>-1)&&
				(cols.indexOf(comboDosis.getValue())>-1);
	}



	public void setLabor(FertilizacionLabor l) {
		this.labor = l;

		List<String> availableColums = labor.getAvailableColumns();
		availableColums.sort((a,b)->{
			return a.compareTo(b);
		});

		//TODO si avalilableColumns contiene las columnas estanddar seleccionarlas


//		this.comboVelo.setItems(FXCollections.observableArrayList(availableColums));
//		this.comboVelo.valueProperty().bindBidirectional(labor.colVelocidad);


		//	this.comboVelo.getSelectionModel().select(-1);


		//comboElev
		this.comboElev.setItems(FXCollections.observableArrayList(availableColums));
		this.comboElev.valueProperty().bindBidirectional(labor.colElevacion);



		// colRendimiento;
		this.comboDosis.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDosis.valueProperty().bindBidirectional(labor.colKgHaProperty);

		//colAncho;
		//this.comboAnch.setItems(FXCollections.observableArrayList(availableColums));
		this.comboAnch.getItems().addAll(availableColums);
		this.comboAnch.valueProperty().bindBidirectional(labor.colAncho);
		
		this.comboDistancia.getItems().addAll(availableColums);
		this.comboDistancia.valueProperty().bindBidirectional(labor.colDistancia);


		//colCurso;
		this.comboCurso.setItems(FXCollections.observableArrayList(availableColums));
		this.comboCurso.valueProperty().bindBidirectional(labor.colCurso);
	
		this.comboFertilizante.setItems(FXCollections.observableArrayList(Fertilizante.fertilizantes.values()));
		this.comboFertilizante.valueProperty().bindBidirectional(labor.fertilizante);


		StringConverter<Number> converter = new NumberStringConverter();

		//textPrecioGrano
		Bindings.bindBidirectional(this.textPrecioFert.textProperty(), labor.precioInsumoProperty, converter);

		//textCostoCosechaHa
		Bindings.bindBidirectional(this.textCostoLaborHa.textProperty(), labor.precioLaborProperty, converter);

		//textAnchoDef
		//Bindings.bindBidirectional(this.textAnchoDef.textProperty(), labor.anchoDefaultProperty, converter);

		//textPorcCorreccion
		//Bindings.bindBidirectional(this.textPorcCorreccion.textProperty(), labor.correccionCosechaProperty, converter);

		//textMaxRinde
		//Bindings.bindBidirectional(this.textMaxRinde.textProperty(), labor.maxRindeProperty, converter);

		//textMinRinde
		//Bindings.bindBidirectional(this.textMinRinde.textProperty(), labor.minRindeProperty, converter);

		//textDistanciasRegimen
		Bindings.bindBidirectional(this.textDistanciasRegimen.textProperty(), labor.config.cantDistanciasEntradaRegimenProperty(), converter);

		//textMaxSuper
	//	Bindings.bindBidirectional(this.textMaxSuper.textProperty(), labor.config.cantMaxGeometriasSuperpuestasProperty(), converter);

		//textSupMin
	//	Bindings.bindBidirectional(this.textSupMin.textProperty(), labor.config.supMinimaProperty(), converter);

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

		textNombre.textProperty().bindBidirectional(labor.nombreProperty);

		chkOutlayers.selectedProperty().bindBidirectional(labor.config.correccionOutlayersProperty());
		chkAncho.selectedProperty().bindBidirectional(labor.config.correccionAnchoProperty());
		chkDemora.selectedProperty().bindBidirectional(labor.config.correccionDemoraPesadaProperty());
		chkDemora.setTooltip(new Tooltip("Permite adelantar o atrazar cada pesada y estirar la pesada de entrada en regimen"));
	//	chkRinde.selectedProperty().bindBidirectional(labor.config.correccionRindeProperty());
	//	chkSuperposicion.selectedProperty().bindBidirectional(labor.config.correccionSuperposicionProperty());
		chkDistancia.selectedProperty().bindBidirectional(labor.config.correccionDistanciaProperty());

	//	chkFlow.selectedProperty().bindBidirectional(labor.config.correccionFlowToRindeProperty());

		chkResumirGeometrias.selectedProperty().bindBidirectional(labor.config.resumirGeometriasProperty());
	}




	public void init() {
		this.getDialogPane().setContent(content);

	}



	public static Optional<FertilizacionLabor> config(FertilizacionLabor labor2) {
		Optional<FertilizacionLabor> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(FertilizacionConfigDialogController.class.getResource(
					FERT_CONFIG_DIALOG_FXML));
			myLoader.load();//aca se crea el constructor
			FertilizacionConfigDialogController controller = ((FertilizacionConfigDialogController) myLoader.getController());
			controller.setLabor(labor2);
			controller.init();
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+FERT_CONFIG_DIALOG_FXML);
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}




}
