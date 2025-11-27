package com.ursulagis.desktop.gui;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.gui.utils.DateConverter;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;

public class NDVIDatePickerDialog {
	private Stage owner=null;
	
	public LocalDate initialDate = LocalDate.now();
	public LocalDate finalDate = LocalDate.now();
	
	public NDVIDatePickerDialog(Stage _owner){
		owner=_owner;
	}
	
	
	public LocalDate ndviDateChooser(LocalDate fin) {
	    return ndviDateChooser(null, fin);
	}

	public LocalDate ndviDateChooser(LocalDate ini, LocalDate fin){
		SimpleObjectProperty<LocalDate> iniLdp = new SimpleObjectProperty<LocalDate>();
		SimpleObjectProperty<LocalDate> finLdp = new SimpleObjectProperty<LocalDate>();
		 initialDate = LocalDate.now();
		 finalDate = LocalDate.now();
		 
		DateConverter dc = new DateConverter();		
		Configuracion config = JFXMain.config;
		String configDate = config.getPropertyOrDefault("LAST_DATE", dc.toString(finalDate)); //$NON-NLS-1$
		finalDate = dc.fromString(configDate);
		
		if(fin!=null){
			finalDate= fin;
		}
		finLdp.set(finalDate);
		
//		GregorianCalendar cal = new GregorianCalendar();
//		cal.setTime(java.sql.Date.valueOf(finalDate));
//		cal.add(Calendar.MONTH, -1);
		if (ini != null) {
	        initialDate = ini;
	    } else {
	        initialDate = finalDate.minusMonths(1);
	    }
		
		iniLdp.set(initialDate);
		

		Alert dateDialog = new Alert(AlertType.CONFIRMATION);//dc.toString(initialDate));
		DatePicker datePickerFechaDesde=new DatePicker();
		datePickerFechaDesde.setConverter(dc);
		datePickerFechaDesde.valueProperty().bindBidirectional(iniLdp);
		datePickerFechaDesde.valueProperty().addListener((ob,old,n)->{
			System.out.println("date picker fecha desde cambio a "+datePickerFechaDesde.valueProperty().get()+" initial quedo en "+iniLdp.get()); //$NON-NLS-1$ //$NON-NLS-2$
		});
		
		DatePicker datePickerFechaHasta=new DatePicker();
		datePickerFechaHasta.setConverter(dc);
		datePickerFechaHasta.valueProperty().bindBidirectional(finLdp);
		datePickerFechaHasta.valueProperty().addListener((ob,old,n)->{
			iniLdp.set(n.minusMonths(1));
			System.out.println("date picker fecha hasta cambio a "+datePickerFechaHasta.valueProperty().get()+" initial quedo en "+iniLdp.get()); //$NON-NLS-1$ //$NON-NLS-2$
		});
		
		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label(Messages.getString("NdviDatePickerDialog.fechaDesde")),datePickerFechaDesde)); //$NON-NLS-1$
		vb.getChildren().add(new HBox(new Label(Messages.getString("NdviDatePickerDialog.fechaHasta")),datePickerFechaHasta)); //$NON-NLS-1$
		
		

		dateDialog.setGraphic(vb);
		dateDialog.setTitle(Messages.getString("NdviDatePickerDialog.ConfigureLasFechasDeseadas")); //$NON-NLS-1$
		//dateDialog.setHeaderText("Fecha Desde");
		dateDialog.initOwner(owner);
		Optional<ButtonType> res = dateDialog.showAndWait();
		if(res.get().equals(ButtonType.OK)){
			config.loadProperties();
			config.setProperty("LAST_DATE", dc.toString(finLdp.get())); //$NON-NLS-1$
			config.save();
			
			this.initialDate=iniLdp.get();
			this.finalDate=finLdp.get();
			//this.finalDate=this.finalDate.plusDays(1);//le agrego un dia para que sea inclusive
					
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
