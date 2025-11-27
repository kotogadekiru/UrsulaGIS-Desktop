package com.ursulagis.desktop.gui.utils;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import com.ursulagis.desktop.utils.DAH;
import javafx.beans.property.SimpleBooleanProperty;

public class BooleanTableColumn<T> extends TableColumn<T,Boolean> {

	public BooleanTableColumn(String title,Function<T,Boolean>  getMethod, BiConsumer<T,Boolean> setMethod){
		super(title);	
		setEditable(setMethod != null);

		setCellValueFactory(cellData ->{
			Boolean doubleValue = getMethod.apply(cellData.getValue());
			if(doubleValue == null)doubleValue=true;
			try{
				SimpleBooleanProperty sbp = new SimpleBooleanProperty(doubleValue);
				sbp.addListener((o,old, n)->{
					T entity = cellData.getValue();
					if(setMethod!=null)setMethod.accept(entity,n);
				});
				return sbp;	
			}catch(Exception e){
				return new SimpleBooleanProperty(doubleValue);
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

		setCellFactory(CheckBoxTableCell.<T>forTableColumn(this));
		this.setStyle("-fx-alignment: CENTER-RIGHT;");// alinear a la derecha OK!!

		this.setOnEditCommit( cellEditingEvent -> {													
			T p = cellEditingEvent.getRowValue();

			try {
				Boolean newVal;
				newVal = cellEditingEvent.getNewValue();
				if(setMethod!=null)setMethod.accept(p,newVal);//Double.valueOf( cellEditingEvent.getNewValue()));		
			
				System.out.println("modificando el objeto boolean");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		this.setComparator(new Comparator<Boolean>(){

			@Override
			public int compare(Boolean arg0, Boolean arg1) {	
				return arg0.compareTo(arg1);	
			}

		});
	}
}
