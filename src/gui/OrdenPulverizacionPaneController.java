package gui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.JOptionPane;

import api.OrdenPulverizacion;
import api.OrdenPulverizacionItem;
import dao.config.Configuracion;
import gui.utils.DateConverter;
import gui.utils.SmartTableView;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OrdenPulverizacionPaneController extends Dialog<OrdenPulverizacion>{
	private static final String ORDEN_PULV_DESCRIPCION = "OrdenPulv.DESCRIPCION";

	private static final String ORDEN_PULV_CULTIVO = "OrdenPulv.CULTIVO";

	private static final String ORDEN_PULV_CONTRATISTA = "OrdenPulv.CONTRATISTA";

	private static final String ORDEN_PULV_ESTABLECIMIENTO = "OrdenPulv.ESTABLECIMIENTO";

	private static final String ORDEN_PULV_PRODUCTOR = "OrdenPulv.PRODUCTOR";

	private static final String ORDEN_PULV_ING = "OrdenPulv.ING";

	private static final String ORDEN_PULV_NRO = "OrdenPulv.Nro";

	private static final String CONFIG_DIALOG_FXML = "OrdenPulverizacionPane.fxml"; //$NON-NLS-1$
	
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

	private OrdenPulverizacion ordenPulv;
	
	Locale loc = new Locale("en", "US");
	DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, loc);
    
	public OrdenPulverizacionPaneController() {
		super();
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("OrdenPulv.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		stage.getIcons().add(new Image(JFXMain.ICON));

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
				return updateOrdenPulverizacion();
			}else{
				return null;
			}
		});
	}
	
	private OrdenPulverizacion updateOrdenPulverizacion() {
		Configuracion config = Configuracion.getInstance();
		config.loadProperties();
		String fecha = dateFormat.format(DateConverter.asDate(this.dPfecha.getValue()));
		this.ordenPulv.setFecha(fecha);
		this.ordenPulv.setNumeroOrden(tfNroOrden.getText());
		config.setProperty(ORDEN_PULV_NRO, this.ordenPulv.getNumeroOrden());

		this.ordenPulv.setNombreIngeniero(tfIng.getText());
		config.setProperty(ORDEN_PULV_ING, this.ordenPulv.getNombreIngeniero());
		
		this.ordenPulv.setProductor(this.tfProductor.getText());
		config.setProperty(ORDEN_PULV_PRODUCTOR, this.ordenPulv.getProductor());
		
		this.ordenPulv.setEstablecimiento(this.tfEstablecimiento.getText());
		config.setProperty(ORDEN_PULV_ESTABLECIMIENTO, this.ordenPulv.getEstablecimiento());
		
		this.ordenPulv.setContratista(this.tfContratista.getText());
		config.setProperty(ORDEN_PULV_CONTRATISTA, this.ordenPulv.getContratista());

		this.ordenPulv.setCultivo(this.tfCultivo.getText());
		config.setProperty(ORDEN_PULV_CULTIVO, this.ordenPulv.getCultivo());
		this.ordenPulv.setDescription(this.taObservaciones.getText());
		config.setProperty(ORDEN_PULV_DESCRIPCION, this.ordenPulv.getDescription());
		config.save();
					
		return this.ordenPulv;//TODO read elements data from pane
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
	
	public void setLabor(OrdenPulverizacion l) {
		this.ordenPulv = l;		
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
			Number nroOrden = nf.parse(config.getPropertyOrDefault(ORDEN_PULV_NRO, "0"));
			this.tfNroOrden.setText(nf.format(nroOrden.doubleValue()+1));
		} catch (ParseException e) {			
			e.printStackTrace();
		}
		
		this.tfIng.setText(config.getPropertyOrDefault(ORDEN_PULV_ING, ""));
		this.tfProductor.setText(config.getPropertyOrDefault(ORDEN_PULV_PRODUCTOR, ""));
		this.tfEstablecimiento.setText(config.getPropertyOrDefault(ORDEN_PULV_ESTABLECIMIENTO, ""));
		this.tfContratista.setText(config.getPropertyOrDefault(ORDEN_PULV_CONTRATISTA, ""));		
		
		this.tfCultivo.setText(config.getPropertyOrDefault(ORDEN_PULV_CULTIVO, ""));	
		this.taObservaciones.setText(config.getPropertyOrDefault(ORDEN_PULV_DESCRIPCION, ""));		
		
		this.contructCaldoTable();

	}
	
	private void contructCaldoTable() {
		 List<OrdenPulverizacionItem> caldo = ordenPulv.getItems();
	
		final ObservableList<OrdenPulverizacionItem> data =
				FXCollections.observableArrayList(
						caldo
						);
		SmartTableView<OrdenPulverizacionItem> table = new SmartTableView<OrdenPulverizacionItem>(data,
				Arrays.asList("Id","OrdenPulverizacion"),//rejected
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
		System.out.println("iniciando OrdenPulverizacionController");
		VBox v= new VBox(new ImageView(new Image(JFXMain.ICON)));
		this.getDialogPane().setContent(content);

		this.getDialogPane().heightProperty().addListener((o,old,nu)->{
			content.setPrefSize(this.getWidth(), nu.doubleValue());	
		});
		this.getDialogPane().widthProperty().addListener((o,old,nu)->{			
			content.setPrefSize(nu.doubleValue(), this.getHeight());			
		});
		this.getDialogPane().setPrefSize(711,390);
	}
	
	public static Optional<OrdenPulverizacion> config(OrdenPulverizacion labor2) {
		Optional<OrdenPulverizacion> ret = Optional.empty();
		
		try{
			FXMLLoader myLoader = new FXMLLoader(OrdenPulverizacionPaneController.class.getResource(
					CONFIG_DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
			myLoader.load();//aca se crea el constructor
			
			OrdenPulverizacionPaneController controller = ((OrdenPulverizacionPaneController) myLoader.getController());
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
