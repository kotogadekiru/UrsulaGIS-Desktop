package com.ursulagis.desktop.gui;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.pulverizacion.CaldoItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.utils.SmartTableView;
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
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;


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
    private TabPane tabPane;
	
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
					System.out.println("guardando configuracion de pulverizacion");
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
		
		Configuracion config = labor.getConfig().getConfigProperties();
		
		PropertyHelper.bindDateToObjectProperty(
				labor::getFecha,
				labor::setFecha,
				datePickerFecha.valueProperty(),
				config,
				Labor.FECHA_KEY);	
		
		// colDosis
		this.comboDosis.setItems(FXCollections.observableArrayList(availableColums));
		this.comboDosis.valueProperty().bindBidirectional(labor.colDosisProperty);

		this.contructCaldoTable();
		
		PropertyHelper.bindDoubleToTextProperty(
				labor::getPrecioInsumo,
				labor::setPrecioInsumo,
				this.textPrecioInsumo.textProperty(),
				config,
				PulverizacionLabor.COSTO_LABOR_PULVERIZACION);
		
		PropertyHelper.bindDoubleToTextProperty(
				labor::getPrecioLabor,
				labor::setPrecioLabor,
				this.textCostoLaborHa.textProperty(),
				config,
				PulverizacionLabor.PRECIO_INSUMO_KEY);
		
		//DecimalFormat converter = PropertyHelper.getDoubleConverter();
		StringConverter<Number> converter = PropertyHelper.buildStringConverter();//
		Bindings.bindBidirectional(this.textClasesClasificador.textProperty(), labor.clasificador.clasesClasificadorProperty, converter);

		this.comboClasificador.setItems(FXCollections.observableArrayList(Clasificador.clasficicadores));
		this.comboClasificador.valueProperty().bindBidirectional(labor.clasificador.tipoClasificadorProperty);
		this.comboClasificador.setConverter(Clasificador.clasificadorStringConverter());
	}

	public void init() {
		this.getDialogPane().setContent(content);
//		content.setBorder(new Border(new BorderStroke(
//				Paint.valueOf("black"),
//				BorderStrokeStyle.SOLID,		
//				CornerRadii.EMPTY,
//				BorderStroke.THIN)));
//		tabPane.setBorder(new Border(new BorderStroke(
//				Paint.valueOf("black"),
//				BorderStrokeStyle.SOLID,		
//				CornerRadii.EMPTY,
//				BorderStroke.THIN)));
		this.getDialogPane().heightProperty().addListener((o,old,nu)->{
		//	System.out.println("cambiando el alto del dialog a "+nu);//si se ejecuta pero no se modifica el tamanio
		//	System.out.println("content height "+content.getHeight());//si se ejecuta pero no se modifica el tamanio
			content.setPrefSize(this.getWidth(), nu.doubleValue());	
		});
		this.getDialogPane().widthProperty().addListener((o,old,nu)->{			
			content.setPrefSize(nu.doubleValue(), this.getHeight());			
		});
		this.getDialogPane().setPrefSize(711,390);
	}

	private void contructCaldoTable() {
		List<CaldoItem> items = labor.getItems();
		if(items ==null) {
			items = new ArrayList<CaldoItem>();
			CaldoItem item = new CaldoItem();
			item.setLabor(labor);
			items.add(item);
			labor.setItems(items);
		} else if(items.size()<1) {
			System.out.println("existe caldo pero sin items");
			CaldoItem item = new CaldoItem();
			item.setLabor(labor);
			labor.getItems().add(item);
		}
		final ObservableList<CaldoItem> data =
				FXCollections.observableArrayList(
						labor.getItems()
						);
		SmartTableView<CaldoItem> table = new SmartTableView<CaldoItem>(data,
				Arrays.asList("Id","UnidadStock"),//rejected
				Arrays.asList("Producto","DosisHa",
						"UnidadDosis","Observaciones"),//order
				Arrays.asList("Producto","Dosis",
						"Unidad","Observaciones")//Names
				);
		table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
		table.setEliminarAction(
				list->{											
					list.stream().forEach(i->{
						i.getLabor().getItems().remove(i);	
					});
				}
				);
		table.setEditable(true);
		table.setOnDoubleClick(()->{
			System.out.println("haciendo dobleClick");
			CaldoItem i = new CaldoItem();
			labor.getItems().add(i);
			i.setLabor(labor);
			return i;
		});
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
