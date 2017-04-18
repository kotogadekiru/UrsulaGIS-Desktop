package mmg.gui.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

public class IntegerTableColumn<T> extends TableColumn<T,String> {
	public IntegerTableColumn(String title,Function<T,Integer>  getMethod, BiConsumer<T,Integer> setMethod){
		super(title);	
		//TableColumn<Monitor,String> vacasOrdenie = new TableColumn<Monitor,String>(title);		
	
			setEditable(setMethod != null);
			//DecimalFormat df = new DecimalFormat("###,###.###");
			NumberFormat df = NumberFormat.getIntegerInstance();
		
		//	 this.setCellValueFactory(new PropertyValueFactory<T, Date>("date"));
		setCellValueFactory(cellData ->{
			Integer IntegerValue = getMethod.apply(cellData.getValue());
			try{
			return new SimpleStringProperty(df.format(IntegerValue));	
			}catch(Exception e){
				System.out.println("Falló el Decimal Format en String Table Column "+title +" para "+IntegerValue);
				
				return new SimpleStringProperty(String.valueOf(IntegerValue));
			}
		}
		
		);
		
		this.setPrefWidth(70);
		//hago que la cabecera se ajuste en tamaño
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
														Integer newVal;
														newVal = df.parse(cellEditingEvent.getNewValue()).intValue();
														setMethod.accept(p,newVal);//Integer.valueOf( cellEditingEvent.getNewValue()));		
														//DAH.save(p);
													} catch (Exception e) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}
												
													});
		this.setComparator(new Comparator<String>(){

			@Override
			public int compare(String arg0, String arg1) {
				
				try {
					Integer d0 = df.parse(arg0).intValue();
					Integer d1 = df.parse(arg1).intValue();
					
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
