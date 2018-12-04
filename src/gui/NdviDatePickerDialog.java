package gui;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;

import dao.config.Configuracion;
import gui.utils.DateConverter;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;

public class NdviDatePickerDialog {
	private Stage owner=null;
	
	public LocalDate initialDate = LocalDate.now();
	public LocalDate finalDate = LocalDate.now();
	
	public NdviDatePickerDialog(Stage _owner){
		owner=_owner;
	}

	public LocalDate ndviDateChooser(LocalDate fin){
		SimpleObjectProperty<LocalDate> iniLdp = new SimpleObjectProperty<LocalDate>();
		SimpleObjectProperty<LocalDate> finLdp = new SimpleObjectProperty<LocalDate>();
		 initialDate = LocalDate.now();
		 finalDate = LocalDate.now();
		 
		 
		DateConverter dc = new DateConverter();		
		Configuracion config = Configuracion.getInstance();
		String configDate = config.getPropertyOrDefault("LAST_DATE", dc.toString(finalDate));
		finalDate = dc.fromString(configDate);
		if(fin!=null){
			finalDate= fin;
		}
		finLdp.set(finalDate);
		
//		GregorianCalendar cal = new GregorianCalendar();
//		cal.setTime(java.sql.Date.valueOf(finalDate));
//		cal.add(Calendar.MONTH, -1);
		initialDate=finalDate.minusMonths(1);
		
		iniLdp.set(initialDate);
		

		Alert dateDialog = new Alert(AlertType.CONFIRMATION);//dc.toString(initialDate));
		DatePicker datePickerFechaDesde=new DatePicker();
		datePickerFechaDesde.setConverter(dc);
		datePickerFechaDesde.valueProperty().bindBidirectional(iniLdp);
		datePickerFechaDesde.valueProperty().addListener((n,old,ob)->{
			System.out.println("date piker fecha desde cambio a "+datePickerFechaDesde.valueProperty().get()+" initial quedo en "+initialDate);
		});
		
		DatePicker datePickerFechaHasta=new DatePicker();
		datePickerFechaHasta.setConverter(dc);
		datePickerFechaHasta.valueProperty().bindBidirectional(finLdp);
		datePickerFechaHasta.valueProperty().addListener((ob,old,n)->{
			iniLdp.set(n.minusMonths(1));
		});
		
		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label("Fecha Hasta"),datePickerFechaHasta));
		vb.getChildren().add(new HBox(new Label("Fecha Desde"),datePickerFechaDesde));
		
		

		dateDialog.setGraphic(vb);
		dateDialog.setTitle("Configure la fecha requerida");
		//dateDialog.setHeaderText("Fecha Desde");
		dateDialog.initOwner(owner);
		Optional<ButtonType> res = dateDialog.showAndWait();
		if(res.get().equals(ButtonType.OK)){
			config.setProperty("LAST_DATE", dc.toString(finLdp.get()));
			config.save();
			
			this.initialDate=iniLdp.get();
			this.finalDate=finLdp.get();
					
			return finLdp.get();
		} else {
			return null;
		}
	}
	
//	public LocalDate dateChooser(LocalDate ini){
//		SimpleObjectProperty<LocalDate> ldp = new SimpleObjectProperty<LocalDate>();
//		LocalDate initialDate = LocalDate.now();
//		DateConverter dc = new DateConverter();
//		Configuracion config = Configuracion.getInstance();
//		String configDate = config.getPropertyOrDefault("LAST_DATE", dc.toString(initialDate));
//		initialDate = dc.fromString(configDate);
//		if(ini!=null){
//			initialDate= ini;
//		}
//		ldp.set(initialDate);
//
//		Alert dateDialog = new Alert(AlertType.CONFIRMATION);//dc.toString(initialDate));
//		DatePicker datePickerFecha=new DatePicker();
//		datePickerFecha.setConverter(new DateConverter());
//		datePickerFecha.valueProperty().bindBidirectional(ldp);
//
//		dateDialog.setGraphic(datePickerFecha);
//		dateDialog.setTitle("Configure la fecha requerida");
//		dateDialog.setHeaderText("Fecha");
//		dateDialog.initOwner(owner);
//		Optional<ButtonType> res = dateDialog.showAndWait();
//		if(res.get().equals(ButtonType.OK)){
//			config.setProperty("LAST_DATE", dc.toString(ldp.get()));
//			config.save();
//			finalDate = ldp.get();
//			initialDate=idp.get();
//			
//			return ldp.get();
//		} else {
//			return null;
//		}
//	}
}
