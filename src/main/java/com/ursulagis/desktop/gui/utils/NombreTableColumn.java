package com.ursulagis.desktop.gui.utils;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;

public class NombreTableColumn extends TableColumn<Map<String,Object>,String>{
	public NombreTableColumn(String nombreColumn) {
		super(nombreColumn);	
		this.setEditable(false);
		this.setCellFactory(TextFieldTableCell.forTableColumn());
		this.setCellValueFactory(
				cellData ->{
					String stringValue = null;
					try{
						stringValue =(String)  cellData.getValue().get(nombreColumn);						
						return new SimpleStringProperty(stringValue);	
					}catch(Exception e){						
						return new SimpleStringProperty("sin datos");
					}
				});
		this.setOnEditCommit(cellEditingEvent -> { 
			int row = cellEditingEvent.getTablePosition().getRow();
			Map<String,Object> p = cellEditingEvent.getTableView().getItems().get(row);
			try {
				p.put(nombreColumn, cellEditingEvent.getNewValue());
				cellEditingEvent.getTableView().refresh();
			} catch (Exception e) {	e.printStackTrace();}
		});		
	}

}
