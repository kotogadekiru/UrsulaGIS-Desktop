package mmg.gui.utils;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

public class DateColumn<T> extends TableColumn<T, Date> {



	public DateColumn(String title,Function<T,Calendar>  getMethod, BiConsumer<T,Calendar> setMethod){
		super(title);	
		//TableColumn<Monitor,String> vacasOrdenie = new TableColumn<Monitor,String>(title);		

		setEditable(setMethod != null);
		//DecimalFormat df = new DecimalFormat("###,###.###");
		//SimpleDateFormat df = new SimpleDateFormat("dd-mm-yy");

		//	 this.setCellValueFactory(new PropertyValueFactory<T, Date>("date"));
		setCellValueFactory(cellData ->{
			Calendar calendarValue = getMethod.apply(cellData.getValue());
			if(calendarValue==null)return null;
			try{
				
				return new SimpleObjectProperty<Date>(calendarValue.getTime());	
			}catch(Exception e){
				System.out.println("Falló ella conversion de Calendar a Date "+title +" para \""+calendarValue+"\"");

				return new SimpleObjectProperty(String.valueOf(calendarValue));
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



		//setCellFactory(TextFieldTableCell.<T>forTableColumn());//TODO cambiar esto por un dateCell
		setCellFactory(col -> new DateTableCell<T>());
		this.setStyle("-fx-alignment: CENTER-RIGHT;");// alinear a la derecha OK!!

		this.setOnEditCommit( cellEditingEvent -> {													
			T p = cellEditingEvent.getRowValue();

			try {
				Calendar newVal=Calendar.getInstance();
				newVal.setTime(cellEditingEvent.getNewValue());
				setMethod.accept(p,newVal);//Double.valueOf( cellEditingEvent.getNewValue()));		
			//	DAH.save(p);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});
		this.setComparator(new Comparator<Date>(){

			@Override
			public int compare(Date arg0, Date arg1) {
				return arg0.compareTo(arg1);


			}

		});
	}

}