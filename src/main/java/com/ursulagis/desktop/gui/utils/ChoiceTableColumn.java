package com.ursulagis.desktop.gui.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.tableview2.cell.ComboBox2TableCell;

import com.ursulagis.desktop.dao.config.Asignacion;
import com.ursulagis.desktop.utils.DAH;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

/**
 * 
 * @author tomas
 *
 * @param <T> el tipo de dato que tiene la tabla
 * @param <C> el tipo de dato que se permite seleccionar
 */
public class ChoiceTableColumn<T,C extends Comparable<C>> extends TableColumn<T,C>{
	private List<C> ops=null;	
	
	public ChoiceTableColumn(String title,List<C> choices,Function<T,C>  getMethod, BiConsumer<T,C> setMethod){
		super(title);		
		setEditable(true);
		//this.setComparator(comparator);

		setCellValueFactory(cellData ->{
			T value = cellData.getValue();
			C cellContent = getMethod.apply(value);
			return 	new SimpleObjectProperty<C>(cellContent);			
			});

		ops = choices;
		Callback<TableColumn<Object, C>, TableCell<Object, C>> tbCFactory = 
		(TableColumn<Object, C> param) ->{

			SearcheableTableCell<Object, C> cell =new	SearcheableTableCell<Object, C>(FXCollections.observableArrayList(ops));

			//cell.setGraphic(new SearchableComboBox<C>());
			//coiceBox es null
			//  ComboBox<C> choiceBox = (ComboBox<C>) cell.getGraphic();
			//  choiceBox.valueProperty().addListener((observable, oldValue, newValue)->{
			// 	ops.stream().filter(o->o.toString().startsWith(newValue.toString()));
			// 	System.out.println("cambiando el valor de "+newValue+" filtered "+Arrays.toString(ops.toArray()));
			//  });
			// if(choiceBox!=null){
			// 	choiceBox.setVisibleRowCount(10);
			// 	choiceBox.setEditable(true);
			// }
			
			return cell;
			
			 //return ChoiceBoxTableCell.forTableColumn(FXCollections.observableArrayList(ops)).call(param);
			};

		Function<T,List<C>> choiceProvider = (T item)->{
			// if(item!=null && item instanceof Asignacion){
			// 	Asignacion asignacion = (Asignacion) item;
			// 	if(asignacion.getLote()!=null){
			// 		return (List<C>) DAH.getPoligonos(asignacion.getLote());
			// 	}
			// }
			return ops;
		};
		//Callback<TableColumn<T, C>, TableCell<T, C>> tbCFactory = FilterableChoiceBoxTableCell.forTableColumn(choiceProvider);

		setCellFactory((col)->{			
			 TableCell<Object, C> cell = tbCFactory.call((TableColumn<Object, C>) col);
//		cell.contentDisplayProperty().bind(Bindings.when(cell.editingProperty())
//				.then(ContentDisplay.GRAPHIC_ONLY)
//				.otherwise(ContentDisplay.TEXT_ONLY));
			 return  (TableCell<T, C>)cell;
			}
		);
		
//		this.setOnEditStart((cellEditEvent)->{
//			S value = getMethod.apply(cellEditEvent.getRowValue());
//		//	((ComboBoxTableCell)cellEditEvent.getSource()).combo
//			
//		});
		
		this.setComparator(new Comparator<C>(){
			@Override
			public int compare(C arg0, C arg1) {
				try {
					//System.out.println("comparando "+arg0+" con "+arg1);
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

		this.setPrefWidth(200);
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
