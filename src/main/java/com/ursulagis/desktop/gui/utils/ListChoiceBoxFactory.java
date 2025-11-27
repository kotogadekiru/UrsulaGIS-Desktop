package com.ursulagis.desktop.gui.utils;

import javafx.beans.property.ListProperty;
import javafx.util.Callback;

import com.dooapp.fxform.AbstractFXForm;
import com.dooapp.fxform.model.Element;
import com.dooapp.fxform.view.factory.impl.FXFormChoiceBoxNode;

public class ListChoiceBoxFactory<T> implements Callback<Void, FXFormChoiceBoxNode> {
    private final ListProperty<T> choices;

    public ListChoiceBoxFactory(ListProperty<T> choices) {
        this.choices = choices;
    }

    public FXFormChoiceBoxNode call(Void aVoid) {
        return new FXFormChoiceBoxNode() {            
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

            @Override
            public void init(Element arg0, AbstractFXForm arg1) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'init'");
            }
        };
    }
}