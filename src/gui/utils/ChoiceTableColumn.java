package gui.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

import com.sun.javafx.collections.ObservableListWrapper;

/**
 * 
 * @author tomas
 *
 * @param <T> el tipo de dato que tiene la tabla
 * @param <S> el tipo de dato que se permite seleccionar
 */
public class ChoiceTableColumn<T,S extends Comparable<S>> extends TableColumn<T,String>{
	private Map<String,S> ops=null; 
	
	public ChoiceTableColumn(String title,List<S> choices,Function<T,S>  getMethod, BiConsumer<T,S> setMethod){
		super(title);		
		setEditable(true);

		setCellValueFactory(cellData ->{
			T value = cellData.getValue();
			S cellContent = getMethod.apply(value);
			return 	new SimpleStringProperty(String.valueOf(cellContent));			
			});

		ops = new HashMap<String,S>();
		if(choices != null){
			for(S choice: choices){
				ops.put(choice.toString(),choice);		
			}
		}
		
//		setCellFactory((param)->{
//			List<String> stringChoices = new LinkedList<String>(ops.keySet());
//			ChoiceTableCell<T> cell = new ChoiceTableCell<T>(stringChoices);
//			return cell;
//		});
//		
		setCellFactory((col)->{
			 TableCell<Object, String> cell = ChoiceBoxTableCell.forTableColumn(FXCollections.observableArrayList(ops.keySet())).call((TableColumn<Object, String>) col);
//		cell.contentDisplayProperty().bind(Bindings.when(cell.editingProperty())
//				.then(ContentDisplay.GRAPHIC_ONLY)
//				.otherwise(ContentDisplay.TEXT_ONLY));
			 return  (TableCell<T, String>)cell;
			}
		);
//	

		
//		this.setOnEditStart((cellEditEvent)->{
//			S value = getMethod.apply(cellEditEvent.getRowValue());
//		//	((ComboBoxTableCell)cellEditEvent.getSource()).combo
//			
//		});
		
		this.setOnEditCommit( cellEditingEvent -> {													
			T p = cellEditingEvent.getRowValue();
			setMethod.accept(p,ops.get(cellEditingEvent.getNewValue()));		
		//	System.out.println("comiteando el valor "+cellEditingEvent.getNewValue());
			//DAH.save(p);
		});

		this.setComparator(new Comparator<String>(){

			@Override
			public int compare(String arg0, String arg1) {
				S s0 = ops.get(arg0);
				S s1 = ops.get(arg1);
				if(s0==null)return -1;
				return s0.compareTo(s1);

			}

		});
		this.setPrefWidth(70);
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
	}

}
