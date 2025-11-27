package com.ursulagis.desktop.gui.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.Messages;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

public class DoubleTableColumn<T> extends TableColumn<T,String> {
	public DoubleTableColumn(String title,Function<T,Double>  getMethod, BiConsumer<T,Double> setMethod){
		super(title);	
		setEditable(setMethod != null);
//		NumberFormat nf = Messages.getNumberFormat();
//		nf.setGroupingUsed(true);
//		nf.setMaximumFractionDigits(3);//DosisHa necesita 3 decimales
		//DecimalFormat df = new DecimalFormat("###,###.##");

		//	 this.setCellValueFactory(new PropertyValueFactory<T, Date>("date"));
		setCellValueFactory(cellData ->{
			Double doubleValue = getMethod.apply(cellData.getValue());			
			try{
				String stringValue = "0.0";
				if(doubleValue!=null) {
					
					stringValue=PropertyHelper.formatDouble(doubleValue);
				}
				//cellData.getTableView().refresh();
				return new SimpleStringProperty(stringValue);	
			}catch(Exception e){
				System.out.println("Fall� el Decimal Format en DoubleTableColumn "+title +" para "+doubleValue);
				return new SimpleStringProperty(String.valueOf(doubleValue));
			}
		});

		this.setPrefWidth(70);
		//hago que la cabecera se ajuste en tama�o
		Label label = new Label(this.getText());
		label.setStyle("-fx-padding: 8px;");
		label.setWrapText(true);
		label.setAlignment(Pos.CENTER);
		label.setTextAlignment(TextAlignment.CENTER);

		StackPane stack = new StackPane();
		stack.getChildren().add(label);
		stack.prefWidthProperty().bind(this.widthProperty().subtract(5));
		label.prefWidthProperty().bind(stack.prefWidthProperty());
		this.setGraphic(stack);

		setCellFactory(TextFieldTableCell.<T>forTableColumn());
		this.setStyle("-fx-alignment: CENTER-RIGHT;");// alinear a la derecha OK!!

		this.setOnEditCommit( cellEditingEvent -> {													
			T p = cellEditingEvent.getRowValue();
			try {
				Double newVal;
				//TODO fix si tiene coma pero el separador de decimales es .
				String newStringVal = cellEditingEvent.getNewValue();
				
				//newVal = nf.parse(newStringVal).doubleValue();
				newVal = PropertyHelper.parseDouble(newStringVal).doubleValue();
				setMethod.accept(p,newVal);//Double.valueOf( cellEditingEvent.getNewValue()));		
				//DAH.save(p);
			} catch (Exception e) {				
				e.printStackTrace();
			}
		});

		this.setComparator(new Comparator<String>(){
			@Override
			public int compare(String arg0, String arg1) {

//				try {					
					Double d0 = PropertyHelper.parseDouble(arg0).doubleValue();
					Double d1 =PropertyHelper.parseDouble(arg1).doubleValue();

					return d0.compareTo(d1);
//				} catch (ParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				return 0;
			}
		});
	}
	
	public static DoubleTableColumn<Map<String,Object>> createMapTableColumn(String k) {
		DoubleTableColumn<Map<String,Object>> dColumn = new DoubleTableColumn<Map<String,Object>>(k,
				(p)->{	try {//getMethod
					System.out.println("obteniendo "+k);
					Number n =(Number) p.get(k);
					if(n!=null) {
						return n.doubleValue();
					} else {
						return 0.0;
					}
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {//setMethod
					System.out.println("tratando de editar "+k);
					p.put(k,d);
					//tabla.refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});
		dColumn.setEditable(true);
		return dColumn;
	}
}
