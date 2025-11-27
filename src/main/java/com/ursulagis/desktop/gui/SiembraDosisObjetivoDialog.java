package com.ursulagis.desktop.gui;

import com.ursulagis.desktop.dao.siembra.SiembraConfig.Unidad;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.siembra.SiembraConfig;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * clase que permite ingresar la dosis objetivo de la siembra a crear
 * mostrando adecuadamente todos los parametros involucrados
 * @author quero
 *
 */

public class SiembraDosisObjetivoDialog extends Dialog<Double>{
	private static final String DIALOG_FXML = "SiembraDosisObjetivoDialog.fxml"; //$NON-NLS-1$

	private SiembraLabor labor;
	private Unidad unidadSelected=null;
	private Double plM2Obj=null;

    @FXML
    private Pane content;    

	@FXML
	private Label lblSemilla;
	@FXML
	private Label lblCultivo;
	@FXML
	private Label lblPG;
	@FXML
	private Label lblPMS;
	@FXML
	private Label lblEntresurco;
	@FXML
	private Label lblSemBolsa;

	@FXML
	private TextField tfDosis;
	@FXML
	private ChoiceBox<String> cbUnidad;

	@FXML
	private Label lblSm2;
    @FXML
    private Label lblKgHa;
    @FXML
    private Label lblBolsasHa;
	@FXML
	private Label lblsHa;
	@FXML
	private Label lblsML;


	public SiembraDosisObjetivoDialog() {
		super();
		System.out.println("construyendo el controller"); //$NON-NLS-1$

		this.setTitle(Messages.getString("SiembraConfigDialogController.title")); //$NON-NLS-1$
		Stage stage = ((Stage)this.getDialogPane().getScene().getWindow());
		stage.setMinWidth(800);
		stage.setMinHeight(400);
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

		this.setResultConverter(bt->convert(bt));
	}

	private Double convert(ButtonType bt) {
		if(ButtonType.OK.equals(bt)) {
			//Double kgHa=PropertyHelper.parseDouble(lblKgHa.getText()).doubleValue();
			return this.plM2Obj;
		}
		return null;
	}

	/***
	 * metodo que devuelve un callback que toma
	 * @return
	 */


	/***
	 * metodo que valida que los campos sean validos
	 * @return
	 */
	private boolean validarDialog() {
		String dosisString = tfDosis.getText();
		Number dosis = PropertyHelper.parseDouble(dosisString);
		cbUnidad.getSelectionModel().getSelectedItem();

		return dosis.doubleValue()>0;
	}
	
	void updatePlM2Obj() {
		
		Unidad unidad = unidadSelected;
    	String value = tfDosis.getText();
    	if("".equals(value))return;
    	Semilla s = labor.getSemilla();
    	System.out.println("updating preview value = "+value+" unidad = "+unidad);
    	Double dosis = PropertyHelper.parseDouble(value).doubleValue();
    	switch(unidad) {
    	case plaMetroCuadrado:{
    		this.plM2Obj=dosis;
    		break;
    	}
    	case kgHa:{
    		//Convertir de kg/Ha a sem/m2
    		double kgHa=dosis;
    		double kgM2=dosis/ProyectionConstants.METROS2_POR_HA;
    		double grM2=kgM2*1000;
    		double ksemM2=grM2/s.getPesoDeMil();
    		this.plM2Obj=ksemM2*1000;
    		System.out.println(kgHa+"kgHa es igual a "+PropertyHelper.formatDouble(plM2Obj)+" sem/m2");
    		break;
    	}
    	case Bolsa:{
    		double bolsaHa=dosis;
    		double semHa=bolsaHa*s.getCultivo().getSemPorBolsa();    		
    		this.plM2Obj=semHa/ProyectionConstants.METROS2_POR_HA;
    		break;
    	}
    	case milPlaHa:{
    		double kplHa=dosis;
    		double plHa=kplHa*1000;
    		
    		this.plM2Obj=plHa/ProyectionConstants.METROS2_POR_HA;
    		break;
    	}
    	case pla10MtLineal:{
    		double pla10ML=dosis;
    		double plaML=pla10ML/10;    		
    		this.plM2Obj=plaML/labor.getEntreSurco();
    		break;
    	}
    	case pla1MtLineal:{
    		double plaML=dosis;
    		this.plM2Obj=plaML/labor.getEntreSurco();
    		break;
    	}
		default:
			break;

    	}    	
	}
    
    void updatePreview() {    	    	
    	updatePlM2Obj();     	
    	Semilla s = this.labor.getSemilla();
    	double sM2=this.plM2Obj/s.getPG();
    	lblSm2.setText(PropertyHelper.formatDouble(sM2));
    	updateLblKgHa();
    	updateLblBolsaHa();
    	updateLblSHa();
    	updateLblsML();    
		
		//TODO actualizar los datos provisorios
		//calcular semillas por metro
		//calcular kg/ha
		//calcular bolsas/ha
		//calcular plantas por m2
		//calcular plantas por ha
    }
    
    private void updateLblKgHa() {
    	Semilla s = this.labor.getSemilla();
    	double sM2=this.plM2Obj/s.getPG();
    	double sHa = sM2*ProyectionConstants.METROS2_POR_HA;
    	double ksHA=sHa/1000;    	
    	double grHA =ksHA*s.getPesoDeMil();
    	double kgHa =grHA/1000;
    	lblKgHa.setText(PropertyHelper.formatDouble(kgHa));
    }
    
    private void updateLblBolsaHa() {
    	try {
    	Semilla s = this.labor.getSemilla();
    	double sM2=this.plM2Obj/s.getPG();
    	double sHa = sM2*ProyectionConstants.METROS2_POR_HA;
    	double bolsaHa=sHa/s.getCultivo().getSemPorBolsa(); 
    	lblBolsasHa.setText(PropertyHelper.formatDouble(bolsaHa));
    	}catch(Exception e) {
    		e.printStackTrace();
    		lblBolsasHa.setText("-");
    	}
    }
    
    private void updateLblSHa() {
    	Semilla s = this.labor.getSemilla();
    	double sM2=this.plM2Obj/s.getPG();
    	double sHa = sM2*ProyectionConstants.METROS2_POR_HA;
    	lblsHa.setText(PropertyHelper.formatDouble(sHa));
    }

    private void updateLblsML() {
    	Semilla s = this.labor.getSemilla();
    	double sM2=this.plM2Obj/s.getPG();
    	double sML = sM2*labor.getEntreSurco();
    	lblsML.setText(PropertyHelper.formatDouble(sML));    	
    }
	/***
	 * metodo que toma una labor y actualiza los campos visibles
	 * @param l
	 */
	public void setLabor(SiembraLabor l) {
		this.labor = l;
		Semilla s = l.getSemilla();
		this.lblSemilla.setText(s.getNombre());
		this.lblCultivo.setText(s.getCultivo().getNombre());
		this.lblPG.setText(PropertyHelper.formatDouble(s.getPG()));
		this.lblPMS.setText(PropertyHelper.formatDouble(s.getPesoDeMil()));
		this.lblEntresurco.setText(PropertyHelper.formatDouble(l.getEntreSurco()));
		try {
		this.lblSemBolsa.setText(PropertyHelper.formatDouble(s.getCultivo().getSemPorBolsa()));
		}catch(Exception e) {
			e.printStackTrace();
			this.lblSemBolsa.setText("-");
		}
		configDosisInsumoUnit();
	}
	
	private void configDosisInsumoUnit() {
		//cbMetrosPorUnidad a ComboBox para que pueda ser editable
				Map<String,SiembraConfig.Unidad> unidades = new HashMap<String,SiembraConfig.Unidad>();
				unidades.put(Messages.getString("SiembraConfigDialogController.kgHa"),SiembraConfig.Unidad.kgHa); //$NON-NLS-1$
				unidades.put(Messages.getString("SiembraConfigDialogController.bolsa"),SiembraConfig.Unidad.Bolsa); //$NON-NLS-1$
				
				unidades.put(Messages.getString("SiembraConfigDialogController.milPlaHa"),SiembraConfig.Unidad.milPlaHa); //$NON-NLS-1$
				unidades.put(Messages.getString("SiembraConfigDialogController.pla10MtLineal"),SiembraConfig.Unidad.pla10MtLineal); //$NON-NLS-1$
				unidades.put(Messages.getString("SiembraConfigDialogController.pla1MtLineal"),SiembraConfig.Unidad.pla1MtLineal); //$NON-NLS-1$
				unidades.put(Messages.getString("SiembraConfigDialogController.plaMetroCuadrado"),SiembraConfig.Unidad.plaMetroCuadrado); //$NON-NLS-1$
				

				this.cbUnidad.setItems(FXCollections.observableArrayList(unidades.keySet()));
				this.cbUnidad.valueProperty().addListener((ov,old,nv)->{
					System.out.println("cambiando unidad insumo de "+old+" a "+nv);
							unidadSelected = unidades.get(nv);		
							updatePreview();
				});

				SiembraConfig.Unidad configured = labor.getConfiguracion().precioInsumoUnitProperty().get();
				unidades.forEach((key,value)->{
					if(value.equals(configured)){
						cbUnidad.getSelectionModel().select(key);
					}
				});
			
	}
	
	public void init() {
		this.getDialogPane().setContent(content);
		tfDosis.textProperty().addListener((observable, oldValue, newValue) -> {
		   updatePreview();
		});

	}
	
	public static Optional<Double> config(SiembraLabor labor2) {
		Optional<Double> ret = Optional.empty();
		try{
			FXMLLoader myLoader = new FXMLLoader(SiembraConfigDialogController.class.getResource(
					DIALOG_FXML));
			myLoader.setResources(Messages.getBoundle());
			myLoader.load();//aca se crea el constructor
			SiembraDosisObjetivoDialog controller = ((SiembraDosisObjetivoDialog) myLoader.getController());
			controller.setLabor(labor2);
			controller.init();
			ret = controller.showAndWait();
		} catch (IOException e1) {
			System.err.println("no se pudo levantar el fxml "+DIALOG_FXML); //$NON-NLS-1$
			e1.printStackTrace();
			System.exit(0);
		}
		return ret;
	}

}
