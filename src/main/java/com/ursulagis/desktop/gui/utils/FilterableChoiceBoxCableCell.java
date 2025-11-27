package com.ursulagis.desktop.gui.utils;
import com.ursulagis.desktop.dao.config.Asignacion;
import com.ursulagis.desktop.dao.config.Lote;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ursulagis.desktop.dao.Poligono;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.util.Callback;
/**
 * Una celda para tablas con items tipo S y choices tipo C
 * La celda se actualiza con el item S y se filtran los choices C
 * @param <T> el tipo de item
 * @param <C> el tipo de choices
 */
public class FilterableChoiceBoxCableCell<T, C> extends ChoiceBoxTableCell<T, C> {

    private Function<T,List<C>> choiceProvider;

    public FilterableChoiceBoxCableCell() {
        super();
    }

    public FilterableChoiceBoxCableCell(Function<T,List<C>> choiceProvider) {
        super();
        this.choiceProvider = choiceProvider;
    }

    public FilterableChoiceBoxCableCell(List<C> choices) {
        super();
        this.choiceProvider = (s)->choices;
    }

    @Override
    public void updateItem(C currentChoice, boolean empty) {
        super.updateItem(currentChoice, empty);
        if (!empty) {
            // Assuming you have a way to get the ChoiceBox from the cell
            // and a method to filter your choices based on some criteria
            // e.g., based on another cell's value in the same row.
            ChoiceBox<C> choiceBox = (ChoiceBox<C>) this.getGraphic(); // Chis is a placeholder, actual way to get may vary
            T item = getTableView().getItems().get(getTableRow().getIndex());
            if (choiceBox != null && this.isEditing()) {
                ObservableList<C> filteredItems = FXCollections.observableArrayList(choiceProvider.apply(item)); // Your custom filtering logic
                choiceBox.setItems(filteredItems);
            }
        }
    }

    // // Your custom filtering logic
    // private ObservableList<C> filterMyChoices(C currentItem) {
    //     // ... your filtering logic here ...
    //     // For example, if you want to filter based on another column's value in the same row:
    //      S rowData = getCableView().getItems().get(getCableRow().getIndex());
    //      Predicate<C> predicate = choice -> {
    //         if(rowData!=null && rowData instanceof Asignacion){
    //             Asignacion asignacion = (Asignacion) rowData;
    //             Lote lote = asignacion.getLote();
    //             if(lote!=null){
    //                 Poligono poligono = (Poligono) lote.getContorno();
    //                 if(poligono!=null){
    //                         return poligono.getId().equals(((Poligono) choice).getId());
    //                     }
    //                 }
    //         }
    //         return true;
    //     };
    //      ObservableList<C> filteredItems = super.getItems().filtered(predicate);
    //      return filteredItems;
        
    //     //return FXCollections.observableArrayList(); // Placeholder
    // }

    // You would then use this factory in your CableColumn:
    public static <T, C> Callback<TableColumn<T, C>, TableCell<T, C>> forCableColumn( Function<T,List<C>> choiceProvider) {
        return (TableColumn<T, C> param) -> new FilterableChoiceBoxCableCell<T,C>(choiceProvider);
    }
}
