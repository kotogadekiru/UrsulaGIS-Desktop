package gui;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dao.Clasificador;
import dao.Labor;
import dao.config.Agroquimico;
import dao.pulverizacion.Caldo;
import dao.pulverizacion.CaldoItem;
import dao.pulverizacion.PulverizacionLabor;
import gui.utils.DateConverter;
import gui.utils.SmartTableView;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import utils.DAH;


/**
 * clase que toma una objeto de configuracion lo muestra y permite editarlo y lo devuelve
 * @author tomas
 *
 */
public class PulverizacionConfigDialogController  extends Dialog<PulverizacionLabor>{
	private static final String CONFIG_DIALOG_FXML = "PulvConfigDialog.fxml"; //$NON-NLS-1$

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

//	@FXML
//	private ComboBox<Caldo> comboInsumo;
	
    @FXML
    private GridPane gridPane;

    @FXML
    private BorderPane bpCaldo;//ok
	
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
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("PulverizacionConfigDialogController.title")); //$NON-NLS-1$
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
		
		if(cols.indexOf(comboDosis.getValue())==-1){
			message.append(Messages.getString("PulverizacionConfigDialogController.select")); //$NON-NLS-1$
			isValid=false;
		}

		
		if(!isValid){
			Alert alert = new Alert(AlertType.ERROR, message.toString(), ButtonType.OK);
			alert.initOwner(this.getDialogPane().getScene().getWindow());
			alert.setTitle(Messages.getString("PulverizacionConfigDialogController.title2")); //$NON-NLS-1$
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
		//textNombre.textProperty().bindBidirectional(labor.nombreProperty);
		textNombre.textProperty().set(labor.getNombre());
		textNombre.textProperty().addListener((obj,old,nu)->labor.setNombre(nu));
		
		//datePickerFecha.valueProperty().bindBidirectional(l.fechaProperty,);
		datePickerFecha.setValue(DateConverter.asLocalDate(l.fecha));
		datePickerFecha.setConverter(new DateConverter());
		datePickerFecha.valueProperty().addListener((obs, bool1, n) -> {
			l.setFecha(DateConverter.asDate(n));
			//l.fechaProperty.setValue(bool2);
		});

		// colDosis
		this.comboDosis.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDosis.valueProperty().bindBidirectional(labor.colDosisProperty);
		//pasadas
//		this.comboPasadas.setItems(FXCollections.observableArrayList(availableColums));
//		this.comboPasadas.valueProperty().bindBidirectional(labor.colCantPasadasProperty);

		//insumo
		/*
		List<Caldo> caldos = DAH.getAllCaldos();
		caldos.add(null);
		Caldo nCaldo = new Caldo();
		List<CaldoItem> items = new ArrayList<CaldoItem>();
		CaldoItem item = new CaldoItem();
		item.setCaldo(nCaldo);
		items.add(item);
		nCaldo.setItems(items);
		nCaldo.setNombre(labor.getNombre());	
		if(labor.getCaldo()== null) {
					
			caldos.add(nCaldo);
			labor.setCaldo(nCaldo);
		}
		
		this.comboInsumo.setItems(FXCollections.observableArrayList(caldos));//Agroquimico.agroquimicos.values()));
		this.comboInsumo.getSelectionModel().select(labor.getCaldo());
		this.comboInsumo.getSelectionModel().selectedItemProperty().addListener((obj,old,n)->{
			System.out.println("nuevo caldo selected "+n);
			if(nCaldo.equals(n)) {
				n = ConfigGUI.doConfigCaldo(nCaldo);
				//comboInsumo.getItems().add(n);
				comboInsumo.getSelectionModel().select(n);
			} 
			labor.setCaldo(n);
			
		});
		*/
		this.contructCaldoTable();
		//select(labor.getCaldo());
		//this.comboInsumo.valueProperty().bindBidirectional(labor.agroquimico);


		StringConverter<Number> converter = new NumberStringConverter(Messages.getLocale());
		this.textPrecioInsumo.textProperty().addListener((obj,old,n)->{
			labor.setPrecioInsumo(converter.fromString(n).doubleValue());
			labor.getConfigLabor().getConfigProperties().setProperty(PulverizacionLabor.COLUMNA_PRECIO_PASADA, n);
		});
		//textPrecioGrano
		//Bindings.bindBidirectional(this.textPrecioInsumo.textProperty(), labor.precioInsumoProperty, converter);

		//textCostoLaborHa
		//Bindings.bindBidirectional(this.textCostoLaborHa.textProperty(), labor.precioLaborProperty, converter);
		this.textCostoLaborHa.textProperty().addListener((obj,old,n)->{
			labor.setPrecioLabor(converter.fromString(n).doubleValue());
			labor.getConfigLabor().getConfigProperties().setProperty(PulverizacionLabor.COLUMNA_IMPORTE_HA, n);
		});


		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);


	}

	public void init() {
		this.getDialogPane().setContent(content);
		
//		this.getDialogPane().heightProperty().addListener((o,old,nu)->{
//			System.out.println("cambiando el alto del dialog");//si se ejecuta pero no se modifica el tamanio
//			//content.setPrefSize(this.getWidth(), nu.doubleValue());	
//		});
//		this.getDialogPane().widthProperty().addListener((o,old,nu)->{			
//			//content.setPrefSize(nu.doubleValue(), this.getHeight());			
//		});

	}

	private void contructCaldoTable() {
		Caldo caldo = labor.getCaldo();
		if(caldo ==null) {
			caldo = new Caldo();
			CaldoItem item = new CaldoItem();
			item.setCaldo(caldo);
			caldo.getItems().add(item);
			labor.setCaldo(caldo);
		} else if(caldo.getItems().size()<1) {
			System.out.println("existe caldo pero sin items");
			CaldoItem item = new CaldoItem();
			item.setCaldo(caldo);
			caldo.getItems().add(item);
		}
		final ObservableList<CaldoItem> data =
				FXCollections.observableArrayList(
						labor.getCaldo().getItems()
						);
		SmartTableView<CaldoItem> table = new SmartTableView<CaldoItem>(data,
				Arrays.asList("Id"),//rejected
				Arrays.asList("Producto","DosisHa")//order
				);
		table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
		table.setEliminarAction(
				list->{											
					list.stream().forEach(i->{
						i.getCaldo().getItems().remove(i);	
					});
				}
				);
		table.setEditable(true);
		table.setOnDoubleClick(()->{
			System.out.println("haciendo dobleClick");
			CaldoItem i = new CaldoItem();
			labor.getCaldo().getItems().add(i);
			i.setCaldo(labor.getCaldo());
			return i;
		}); //$NON-NLS-1$
		if(bpCaldo ==null) {
			System.out.println("no puedo cargar la tabla porque caldoPane es null");
		} else {
			bpCaldo.setCenter(table);
		}
	}

	public static Optional<PulverizacionLabor> config(PulverizacionLabor labor2) {
		Optional<PulverizacionLabor> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(PulverizacionConfigDialogController.class.getResource(
					CONFIG_DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
			myLoader.load();//aca se crea el constructor
			
			PulverizacionConfigDialogController controller = ((PulverizacionConfigDialogController) myLoader.getController());
			controller.init();
			controller.setLabor(labor2);
		
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+CONFIG_DIALOG_FXML); //$NON-NLS-1$
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}




}
