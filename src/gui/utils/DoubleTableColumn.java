package gui.utils;

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

public class DoubleTableColumn<T> extends TableColumn<T,String> {
	public DoubleTableColumn(String title,Function<T,Double>  getMethod, BiConsumer<T,Double> setMethod){
		super(title);	
		setEditable(setMethod != null);
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(2);
		//DecimalFormat df = new DecimalFormat("###,###.##");

		//	 this.setCellValueFactory(new PropertyValueFactory<T, Date>("date"));
		setCellValueFactory(cellData ->{
			Double doubleValue = getMethod.apply(cellData.getValue());			
			try{
				String stringValue = "0.0";
				if(doubleValue!=null) {
					stringValue=nf.format(doubleValue);
				}
				return new SimpleStringProperty(stringValue);	
			}catch(Exception e){
				System.out.println("Falló el Decimal Format en DoubleTableColumn "+title +" para "+doubleValue);
				return new SimpleStringProperty(String.valueOf(doubleValue));
			}
		});

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
				Double newVal;
				newVal = nf.parse(cellEditingEvent.getNewValue()).doubleValue();
				setMethod.accept(p,newVal);//Double.valueOf( cellEditingEvent.getNewValue()));		
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
					Double d0 = nf.parse(arg0).doubleValue();
					Double d1 = nf.parse(arg1).doubleValue();

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
