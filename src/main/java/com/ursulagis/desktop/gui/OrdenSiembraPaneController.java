package com.ursulagis.desktop.gui;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.ursulagis.desktop.api.OrdenSiembra;
import com.ursulagis.desktop.api.OrdenSiembraItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.gui.controller.SiembraGUIController;
import com.ursulagis.desktop.gui.utils.DateConverter;
import com.ursulagis.desktop.gui.utils.SmartTableView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OrdenSiembraPaneController extends Dialog<OrdenSiembra>{
	private static final String ORDEN_SIEMBRA_DESCRIPCION = "OrdenSiembra.DESCRIPCION";

	private static final String ORDEN_SIEMBRA_CULTIVO = "OrdenSiembra.CULTIVO";

	private static final String ORDEN_SIEMBRA_CONTRATISTA = "OrdenSiembra.CONTRATISTA";

	private static final String ORDEN_SIEMBRA_ESTABLECIMIENTO = "OrdenSiembra.ESTABLECIMIENTO";

	private static final String ORDEN_SIEMBRA_PRODUCTOR = "OrdenSiembra.PRODUCTOR";

	private static final String ORDEN_SIEMBRA_ING = "OrdenSiembra.ING";

	private static final String ORDEN_SIEMBRA_NRO = "OrdenSiembra.Nro";

	private static final String CONFIG_DIALOG_FXML = "OrdenSiembraPane.fxml"; //$NON-NLS-1$

	private static final String ORDEN_SIEMBRA_UNIDAD = "OrdenSiembra.Unidad";

	@FXML
	private VBox content;

	@FXML
	private BorderPane bpItems;

	@FXML
	private DatePicker dPfecha;

	@FXML
	private Label lblNombre;    

	@FXML
	private TextArea taObservaciones;

	@FXML
	private TextField tfContratista;

	@FXML
	private TextField tfCultivo;

	@FXML
	private TextField tfEstablecimiento;

	@FXML
	private TextField tfNroOrden;

	@FXML
	private TextField tfProductor;

	@FXML
	private TextField tfIng;

	@FXML
	private ComboBox<String> cb;// = new ComboBox<String>();

	private OrdenSiembra ordenSiembra;

	Locale loc = Locale.forLanguageTag("en");
	DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, loc);

	public OrdenSiembraPaneController() {
		super();
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("OrdenSiem.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		stage.getIcons().addAll(JFXMain.stage.getIcons());

		this.getDialogPane().getButtonTypes().add(ButtonType.OK);
		this.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		this.setResizable(true);

		final Button btOk = (Button) this.getDialogPane().lookupButton(ButtonType.OK);
		btOk.addEventFilter(ActionEvent.ACTION, event -> {
			System.out.println("acion en okBtn");
			if (!validarDialog()) {
				System.out.println("la configuracion es incorrecta"); //$NON-NLS-1$
				event.consume();
			}
		});

		this.setResultConverter(e -> {		
			if(ButtonType.OK.equals(e)){			
				return updateOrdenSiembra();
			}else{
				return null;
			}
		});
	}

	private OrdenSiembra updateOrdenSiembra() {
		Configuracion config = Configuracion.getInstance();
		config.loadProperties();
		String fecha = dateFormat.format(DateConverter.asDate(this.dPfecha.getValue()));
		this.ordenSiembra.setFecha(fecha);
		this.ordenSiembra.setNumeroOrden(tfNroOrden.getText());
		config.setProperty(ORDEN_SIEMBRA_NRO, this.ordenSiembra.getNumeroOrden());

		this.ordenSiembra.setNombreIngeniero(tfIng.getText());
		//XXX el ingeniero deberia ser el mismo en todas las ordenes?
		config.setProperty(ORDEN_SIEMBRA_ING, this.ordenSiembra.getNombreIngeniero());

		this.ordenSiembra.setProductor(this.tfProductor.getText());
		config.setProperty(ORDEN_SIEMBRA_PRODUCTOR, this.ordenSiembra.getProductor());

		this.ordenSiembra.setEstablecimiento(this.tfEstablecimiento.getText());
		config.setProperty(ORDEN_SIEMBRA_ESTABLECIMIENTO, this.ordenSiembra.getEstablecimiento());

		this.ordenSiembra.setContratista(this.tfContratista.getText());
		config.setProperty(ORDEN_SIEMBRA_CONTRATISTA, this.ordenSiembra.getContratista());

		this.ordenSiembra.setCultivo(this.tfCultivo.getText());
		config.setProperty(ORDEN_SIEMBRA_CULTIVO, this.ordenSiembra.getCultivo());
		this.ordenSiembra.setDescription(this.taObservaciones.getText());
		config.setProperty(ORDEN_SIEMBRA_DESCRIPCION, this.ordenSiembra.getDescription());

		String unidadKey = cb.getSelectionModel().getSelectedItem();
		String unidadValue =SiembraGUIController.getUnidadesPrescripcionSiembra().get(unidadKey);
		this.ordenSiembra.setUnidad(unidadValue);
		config.setProperty(ORDEN_SIEMBRA_UNIDAD, this.ordenSiembra.getUnidad());
		config.save();

		return this.ordenSiembra;//TODO read elements data from pane
	}
	private boolean validarDialog() {
		//		List<String> cols = labor.getAvailableColumns();
		StringBuilder message = new StringBuilder();
		boolean isValid =true;

		//		if(cols.indexOf(comboDosis.getValue())==-1){
		//			message.append(Messages.getString("PulverizacionConfigDialogController.select")); //$NON-NLS-1$
		//			isValid=false;
		//		}


		if(!isValid){
			Alert alert = new Alert(AlertType.ERROR, message.toString(), ButtonType.OK);
			alert.initOwner(this.getDialogPane().getScene().getWindow());
			alert.setTitle(Messages.getString("OrdenPulv.title2")); //$NON-NLS-1$
			alert.showAndWait();

		}

		return isValid;
	}

	public void setLabor(OrdenSiembra l) {
		this.ordenSiembra = l;		
		try {
			if(dateFormat != null && l.getFecha()!=null) {
				Date d = dateFormat.parse(l.getFecha());
				LocalDate ld =DateConverter.asLocalDate(d);
				this.dPfecha.setValue(ld);
			}
		} catch (ParseException e) {			
			e.printStackTrace();
		}
		this.lblNombre.setText(l.getNombre());
		Configuracion config = Configuracion.getInstance();
		NumberFormat nf = Messages.getNumberFormat();
		nf.setMaximumFractionDigits(0);
		try {
			Number nroOrden = nf.parse(config.getPropertyOrDefault(ORDEN_SIEMBRA_NRO, "0"));
			this.tfNroOrden.setText(nf.format(nroOrden.doubleValue()+1));
		} catch (ParseException e) {			
			e.printStackTrace();
		}

		this.tfIng.setText(config.getPropertyOrDefault(ORDEN_SIEMBRA_ING, ""));
		this.tfProductor.setText(config.getPropertyOrDefault(ORDEN_SIEMBRA_PRODUCTOR, ""));
		this.tfEstablecimiento.setText(config.getPropertyOrDefault(ORDEN_SIEMBRA_ESTABLECIMIENTO, ""));
		this.tfContratista.setText(config.getPropertyOrDefault(ORDEN_SIEMBRA_CONTRATISTA, ""));		

		this.tfCultivo.setText(config.getPropertyOrDefault(ORDEN_SIEMBRA_CULTIVO, ""));	
		this.taObservaciones.setText(config.getPropertyOrDefault(ORDEN_SIEMBRA_DESCRIPCION, ""));		

		Map<String,String> availableColums = SiembraGUIController.getUnidadesPrescripcionSiembra();
		this.cb.setItems(FXCollections.observableArrayList(availableColums.keySet()));
		this.cb.getSelectionModel().select(0);	

		this.contructCaldoTable();
	}

	private void contructCaldoTable() {
		List<OrdenSiembraItem> caldo = ordenSiembra.getItems();

		final ObservableList<OrdenSiembraItem> data =
				FXCollections.observableArrayList(
						caldo
						);
		SmartTableView<OrdenSiembraItem> table = new SmartTableView<OrdenSiembraItem>(data,
				Arrays.asList("Id","OrdenSiembra"),//rejected
				Arrays.asList("Producto","DosisHa","Cantidad","Observaciones")// no lo toma
				);
		table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);

		table.setEditable(false);

		if(bpItems ==null) {
			System.out.println("no puedo cargar la tabla porque caldoPane es null");
		} else {
			bpItems.setCenter(table);
		}
	}

	public void init() {
		System.out.println("iniciando OrdenSiembraController");
		//VBox v= new VBox(new ImageView(new Image(JFXMain.ICON)));
		this.getDialogPane().setContent(content);

		this.getDialogPane().heightProperty().addListener((o,old,nu)->{
			content.setPrefSize(this.getWidth(), nu.doubleValue());	
		});
		this.getDialogPane().widthProperty().addListener((o,old,nu)->{			
			content.setPrefSize(nu.doubleValue(), this.getHeight());			
		});
		this.getDialogPane().setPrefSize(711,390);
	}

	public static Optional<OrdenSiembra> config(OrdenSiembra labor2) {
		Optional<OrdenSiembra> ret = Optional.empty();

		try{
			FXMLLoader myLoader = new FXMLLoader(OrdenSiembraPaneController.class.getResource(
					CONFIG_DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
			myLoader.load();//aca se crea el constructor

			OrdenSiembraPaneController controller = ((OrdenSiembraPaneController) myLoader.getController());
			if(controller==null) {
				System.out.println("controller es null "+CONFIG_DIALOG_FXML);
			} else {
				controller.setLabor(labor2);
				controller.init();//null pointer


				ret = controller.showAndWait();
			}
		} catch (Exception e1) {
			System.err.println("no se pudo levantar el fxml "+CONFIG_DIALOG_FXML); //$NON-NLS-1$
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}
}
