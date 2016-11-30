package mmg.gui.utils;

import javafx.beans.property.ListProperty;
import javafx.util.Callback;

import com.dooapp.fxform.model.Element;
import com.dooapp.fxform.view.FXFormNode;
import com.dooapp.fxform.view.factory.impl.FXFormChoiceBoxNode;

public class ListChoiceBoxFactory<T> implements Callback<Void, FXFormNode> {

    private final ListProperty<T> choices;

    public ListChoiceBoxFactory(ListProperty<T> choices) {
        this.choices = choices;
    }

    public FXFormNode call(Void aVoid) {

        return new FXFormChoiceBoxNode() {
            @Override
            public void init(Element element) {
                choiceBox.itemsProperty().bind(choices);
                choiceBox.getSelectionModel().select(element.getValue());
            }

            @Override
            public void dispose() {
                choiceBox.itemsProperty().unbind();
                super.dispose();
            }

            @Override
            public boolean isEditable() {
                return true;
            }

        };

    }


}