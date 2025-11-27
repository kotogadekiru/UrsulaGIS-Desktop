package com.ursulagis.desktop.gui.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Optional;
import java.util.function.Consumer;

import org.controlsfx.control.RangeSlider;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.StringConverter;

public class DateRangeSlider {
	private static final String DD_MM_YYYY = "dd/MM/yyyy";
	private static DateTimeFormatter formater = DateTimeFormatter.ofPattern(DD_MM_YYYY);	
	private LocalDate min,max,low,high;
	private RangeSlider innerSlider;
	//private Label datesLabel;
	private StringProperty datesProperty;
	private Consumer<Void> onUpdate;
	public DateRangeSlider(LocalDate _min,LocalDate _max,LocalDate _low,LocalDate _high) {
		min=_min;
		max=_max;
		low=_low;
		high=_high;
		innerSlider = new RangeSlider(
				localDateToMillis(min).longValue(),
				localDateToMillis(max).longValue(),
				localDateToMillis(low).longValue(),
				localDateToMillis(high).longValue()
				);
		innerSlider.setShowTickMarks(true);
		innerSlider.setShowTickLabels(true);
		double deltaTick = innerSlider.getMax()-innerSlider.getMin();
		System.out.println("delta ticks "+deltaTick);
		innerSlider.setMajorTickUnit(deltaTick/5);
		//innerSlider.setBlockIncrement(deltaTick/2);//1000*3600*24*5);//milis por 5 dias
		
		innerSlider.setLabelFormatter(createConverter());
		//TODO add listener to other components.	
		 
		innerSlider.highValueProperty().addListener((obs,n,o)->{
			updateLowHigh();
			if(onUpdate!=null)this.onUpdate.accept(null);
		});
		innerSlider.lowValueProperty().addListener((obs,n,o)->{
			updateLowHigh();
			if(onUpdate!=null) this.onUpdate.accept(null);
		});
	}

	private void updateLowHigh() {
		this.high = millisToLocalDate(innerSlider.getHighValue());
		this.low = millisToLocalDate(innerSlider.getLowValue());
		datesProperty.set(getDatesString());
	}
	private String getDatesString() {
		return formater.format(low)+" ~ "+formater.format(high);
	}
	public void showDateSlider(){	
		this.datesProperty = new SimpleStringProperty(getDatesString());
		
		innerSlider.setPrefWidth(500);
		HBox hb = new HBox(
				innerSlider);
		//hb.setPrefWidth(500);
		HBox.setHgrow(innerSlider, Priority.ALWAYS);
		VBox vb = new VBox();
		vb.getChildren().add(
				hb);
		VBox.setVgrow(hb, Priority.ALWAYS);
		vb.setPadding(new Insets(20));		
		
		Alert dateDialog = new Alert(AlertType.CONFIRMATION);
		dateDialog.getDialogPane().setContent(vb);
		dateDialog.setHeaderText(datesProperty.get());
		//dateDialog.setGraphic(vb);
		dateDialog.setResizable(true);
		dateDialog.setTitle(Messages.getString("NdviDatePickerDialog.ConfigureLasFechasDeseadas"));

		dateDialog.initOwner(JFXMain.stage);
		dateDialog.initModality(Modality.NONE);
		this.datesProperty.addListener((obs,old,n)->{
			dateDialog.setHeaderText(n);
		});
		
		updateLowHigh();
		Optional<ButtonType> res = dateDialog.showAndWait();
		if(res.get().equals(ButtonType.OK)){
			//TODO update min max range
			updateLowHigh();
			System.out.println("seleccione fecha low "+low+" high "+high);
						
		} else {
			//TODO fecha not selected
			System.out.println("ok button not selected");
			System.out.println(res.get().getText()+" pressed");
		}
	}
	
	private static LocalDate millisToLocalDate(Number number) {
		Instant instant = Instant.ofEpochMilli(number.longValue());
		LocalDate res = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
		return res;
	}
	
	private static Number localDateToMillis(LocalDate date) {		
		TemporalAccessor temp = date.atStartOfDay(ZoneId.systemDefault());
		return localDateToMillis(temp);
	}
	
	private static Number localDateToMillis(TemporalAccessor date) {		
		Instant inst=Instant.from(date);
		try {
			return inst.toEpochMilli();
		}catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	private static  StringConverter<Number> createConverter(){
		StringConverter<Number> converter=new StringConverter<Number>() {

			@Override
			public String toString(Number number) {

				//Instant instant = Instant.ofEpochMilli(number.longValue());
				LocalDate res = millisToLocalDate(number);// LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
				return formater.format(res);
			}

			@Override
			public Number fromString(String string) {
				TemporalAccessor temp = formater.parse(string);
				//LocalDate res = LocalDateTime.from(temp);
				localDateToMillis(temp);
				Instant inst=Instant.from(temp);
				return inst.toEpochMilli();
				
			}

		};
		return converter;
	}
	
	


	public LocalDate getLow() {
		return this.low;
	}


	public LocalDate getHigh() {	
		return this.high;
	}
	/**
	 * @param onDoubleClick the onDoubleClick to set
	 */
	public void setOnUpdate(Consumer<Void> _onUpdate) {
		this.onUpdate = _onUpdate;
	}
}
