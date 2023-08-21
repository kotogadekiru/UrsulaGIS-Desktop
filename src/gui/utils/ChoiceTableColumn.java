package gui.utils;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

/**
 * 
 * @author tomas
 *
 * @param <T> el tipo de dato que tiene la tabla
 * @param <S> el tipo de dato que se permite seleccionar
 */
public class ChoiceTableColumn<T,S extends Comparable<S>> extends TableColumn<T,S>{
	private List<S> ops=null;	
	
	public ChoiceTableColumn(String title,List<S> choices,Function<T,S>  getMethod, BiConsumer<T,S> setMethod){
		super(title);		
		setEditable(true);
		//this.setComparator(comparator);

		setCellValueFactory(cellData ->{
			T value = cellData.getValue();
			S cellContent = getMethod.apply(value);
			return 	new SimpleObjectProperty<S>(cellContent);			
			});

		ops = choices;
		Callback<TableColumn<Object, S>, TableCell<Object, S>> tbC = ChoiceBoxTableCell.forTableColumn(FXCollections.observableArrayList(ops));
		setCellFactory((col)->{			
			 TableCell<Object, S> cell = tbC.call((TableColumn<Object, S>) col);
//		cell.contentDisplayProperty().bind(Bindings.when(cell.editingProperty())
//				.then(ContentDisplay.GRAPHIC_ONLY)
//				.otherwise(ContentDisplay.TEXT_ONLY));
			 return  (TableCell<T, S>)cell;
			}
		);
		
//		this.setOnEditStart((cellEditEvent)->{
//			S value = getMethod.apply(cellEditEvent.getRowValue());
//		//	((ComboBoxTableCell)cellEditEvent.getSource()).combo
//			
//		});
		
		this.setComparator(new Comparator<S>(){
			@Override
			public int compare(S arg0, S arg1) {
				try {
					System.out.println("comparando "+arg0+" con "+arg1);
					return arg0!=null?arg0.compareTo(arg1):arg1==null?0:-1;
				}catch(Exception e) {
					e.printStackTrace();
					return -1;
				}
			}
		});
		
		this.setOnEditCommit( cellEditingEvent -> {													
			T p = cellEditingEvent.getRowValue();
			setMethod.accept(p,cellEditingEvent.getNewValue());		
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
