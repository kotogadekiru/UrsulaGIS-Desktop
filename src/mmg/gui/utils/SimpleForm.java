package mmg.gui.utils;

import java.util.List;



import com.dooapp.fxform.FXForm;
import com.dooapp.fxform.filter.ExcludeFilter;
import com.dooapp.fxform.view.factory.DefaultFactoryProvider;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SimpleForm<T> {
	static final String ICON = "gui/1-512.png";
	public SimpleForm(T myBean){
		Stage stage = new Stage();
		
		FXForm<T> fxForm = new FXForm<T>(myBean); 

		DefaultFactoryProvider factoryProvider = new DefaultFactoryProvider();
		//TODO agregar factorys para los otros objetos de configuracion
		factoryProvider.addFactory(
				e -> {
					return e.getName().equals("establecimiento");
				},
				new ListChoiceBoxFactory<Establecimiento>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllEstablecimientos()))));
		
		factoryProvider.addFactory(
				e -> {
					return e.getName().equals("lote");
				},
				new ListChoiceBoxFactory<Lote>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllLotes()))));
		factoryProvider.addFactory(
				e -> {
					return e.getName().equals("cultivo");
				},
				new ListChoiceBoxFactory<Cultivo>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllCultivos()))));
		factoryProvider.addFactory(
				e -> {
					return e.getName().equals("empresa");
				},
				new ListChoiceBoxFactory<Empresa>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllEmpresas()))));
		
		factoryProvider.addFactory(
				e -> {
					return e.getName().equals("campania");
				},
				new ListChoiceBoxFactory<Campania>(
						new SimpleListProperty<>(FXCollections
								.observableArrayList(DAH
										.getAllCampanias()))));

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
		stage.getIcons().add(new Image(ICON));
		stage.setScene(scene);
		stage.show();	
	}
}
