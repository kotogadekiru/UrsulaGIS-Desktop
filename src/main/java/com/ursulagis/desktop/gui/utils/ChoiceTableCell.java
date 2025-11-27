package com.ursulagis.desktop.gui.utils;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class ChoiceTableCell<T> extends TableCell<T, String> {
	//private final SimpleDateFormat formatter ;
	private final ChoiceBox<String> combo ;//choice box muestra el valor seleccionado ComboBox No

	public ChoiceTableCell(List<String> ops) {
		combo = new ChoiceBox<String>() ;
		combo.setItems(FXCollections.observableArrayList(ops));
		
//		combo.getSelectionModel().selectedItemProperty()
//		.addListener(new ChangeListener<String>() {
//			@Override
//			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
//				if (isEditing()) {
//					commitEdit(newValue);
//				}}});
		this.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
		//	this.getTableRow().startEdit();
			if(event.getClickCount()>1){
			System.out.println("cell clicked ");
			startEdit();
			}
			event.consume();
			});
//		combo.valueProperty().addListener((on,o,n)->{
//			System.out.println("cambiando el valor de "+o+" a "+n);
//		//	startEdit();
//			commitEdit(n);
//		});
		
//		combo.setOnMousePressed((me)->startEdit());
		//startEdit();
		
		// Commit edit on Enter and cancel on Escape.
		// Note that the default behavior consumes key events, so we must 
		// register this as an event filter to capture it.
		// Consequently, with Enter, the datePicker's value won't yet have been updated, 
		// so commit will sent the wrong value. So we must update it ourselves from the
		// editor's text value.

//		combo.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
//			if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
//
//				commitEdit(combo.getValue());
//			}
//			if (event.getCode() == KeyCode.ESCAPE) {
//				cancelEdit();
//			}
//		});
		
//		combo.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
//		//	this.getTableRow().startEdit();
//			if(event.getClickCount()>1){
//			System.out.println("combo clicked");
//			startEdit();
//			event.consume();
//			} 
//		});
		
//		Callback<ListView<String>, ListCell<String>> cellFactory;
//		combo.setCellFactory(cellFactory);
		
//		contentDisplayProperty().bind(Bindings.when(editingProperty())
//				.then(ContentDisplay.GRAPHIC_ONLY)
//				.otherwise(ContentDisplay.TEXT_ONLY));


	//	setGraphic(combo);
		contentDisplayProperty().set(ContentDisplay.GRAPHIC_ONLY);
	}


	@Override
	public void updateItem(String birthday, boolean empty) {	
		try{
		if (empty || birthday==null) {
			setText(null);
			setGraphic(null);
		} else {
			String stringDate =birthday ;//formatter.format(birthday);
	//		setText(stringDate);
	//		combo.setValue("feliz Navidad");
			//combo.valueProperty().set(stringDate);
			if(!isEditing()){
				combo.getSelectionModel().select(stringDate);
			}
			System.out.println("updateItem "+stringDate);
			setGraphic(combo);
		}
		}catch(Exception e){
			e.printStackTrace();
		}
		//super.updateItem(birthday, empty);
	}

	@Override
	public void startEdit() {
		
		System.out.println("empezando a editar");
		if (!isEmpty()) {
			//combo.setValue("valor inicial al hacer start edit");//getItem().atYear(LocalDate.now().getYear()
		}
		super.startEdit();
	}
}