package com.ursulagis.desktop.gui.utils;

import com.dooapp.fxform.FXForm;
import com.dooapp.fxform.filter.ExcludeFilter;
import com.dooapp.fxform.handler.ElementHandler;
import com.dooapp.fxform.model.Element;
import com.dooapp.fxform.view.FXFormNode;
import com.dooapp.fxform.view.factory.DefaultFactoryProvider;

import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.*;
import com.ursulagis.desktop.gui.JFXMain;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.ursulagis.desktop.utils.DAH;

public class SimpleForm<T> {
	static final String ICON = "com/ursulagis/desktop/gui/1-512.png";
	public SimpleForm(T myBean){
		Stage stage = new Stage();
		
		FXForm<T> fxForm = new FXForm<T>(myBean); 

		
		DefaultFactoryProvider factoryProvider = new DefaultFactoryProvider();

		factoryProvider.addFactory(
				e -> e.getClass().equals(Establecimiento.class)
				,(Void v)->
				new ListChoiceBoxFactory<Establecimiento>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllEstablecimientos()))).call(null));
		
		factoryProvider.addFactory(
				e ->  e.getClass().equals(Lote.class)
				,(Void v)->	new ListChoiceBoxFactory<Lote>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllLotes()))).call(null));
		factoryProvider.addFactory(
				e -> e.getClass().equals(Cultivo.class)
				,(Void v)->	new ListChoiceBoxFactory<Cultivo>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllCultivos()))).call(null));
		factoryProvider.addFactory(
				e -> e.getClass().equals(Empresa.class)
				,(Void v)->
				new ListChoiceBoxFactory<Empresa>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllEmpresas()))).call(null));
		
		factoryProvider.addFactory(
				e ->  e.getClass().equals(Campania.class)
				,(Void v)->
				new ListChoiceBoxFactory<Campania>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllCampanias()))).call(null));
		
		factoryProvider.addFactory(
				e ->  e.getClass().equals(Poligono.class)
				,(Void v)->
				new ListChoiceBoxFactory<Poligono>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllPoligonos()))).call(null));
		
		factoryProvider.addFactory(
				e ->  e.getClass().equals(Ndvi.class)
				,(Void v)->
				new ListChoiceBoxFactory<Ndvi>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllNdvi()))).call(null));

		fxForm.setEditorFactoryProvider(factoryProvider);
		fxForm.addFilters(new ExcludeFilter(new String[]{"id"}));

		VBox vbox = new VBox();

		Button commitBtn = new Button("Guardar");
		commitBtn.setOnAction(a->{
			T lote = fxForm.getSource();
			System.out.println("guartdando el lote "+lote);
			DAH.save(lote);
			stage.close();
		});
		vbox.getChildren().addAll(fxForm,commitBtn);

		Scene scene = new Scene(vbox, 300, 250);
		VBox.setMargin(commitBtn, new Insets(0,0,0,5));
		stage.setTitle("Editar "+myBean.getClass().getSimpleName());
		stage.setScene(scene);
		stage.initOwner(JFXMain.stage);
		stage.show();	
	}
}
