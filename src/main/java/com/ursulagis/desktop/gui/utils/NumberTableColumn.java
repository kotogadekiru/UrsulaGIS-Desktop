package com.ursulagis.desktop.gui.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.ursulagis.desktop.gui.Messages;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
@Deprecated
public class NumberTableColumn<T> extends TableColumn<T,String> {
	public NumberTableColumn(String title,Function<T,Number>  getMethod, BiConsumer<T,Number> setMethod){
		super(title);	

		setEditable(setMethod != null);
		NumberFormat df=Messages.getNumberFormat();

		setCellValueFactory(cellData ->{
			Number numberValue = getMethod.apply(cellData.getValue());
			if(numberValue ==null)return  new SimpleStringProperty();
			try{
				return new SimpleStringProperty(df.format(numberValue));	
			}catch(Exception e){
				System.out.println("Fall� el Decimal Format en String Table Column "+title +" para "+numberValue);

				return new SimpleStringProperty(String.valueOf(numberValue));
			}
		}

				);

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
				Number newVal = df.parse(cellEditingEvent.getNewValue());//Long?
				setMethod.accept(p,newVal);//Integer.valueOf( cellEditingEvent.getNewValue()));		
				//DAH.save(p);
			} catch (Exception e) {	e.printStackTrace();}

		});
		this.setComparator(new Comparator<String>(){

			@Override
			public int compare(String arg0, String arg1) {

				try {
					Double d0 = df.parse(arg0).doubleValue();
					Double d1 = df.parse(arg1).doubleValue();

					return d0.compareTo(d1);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0;

			}

		});
	}
}
