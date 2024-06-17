package gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

import dao.Labor;
import dao.LaborItem;
import gui.utils.SkatterChartWithRegression;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tasks.procesar.CorrelacionarLayersTask;
import utils.ExcelHelper;

public class CorrelacionarCapas {
	JFXMain main;
	//protected ObservableList<XYChart.Series<Number, Number>> dataSeries;
	
	public CorrelacionarCapas(JFXMain _main) {
		this.main=_main;
	
	}
	
	public List<String> getOutColumns(Labor<? extends LaborItem> labor) {
		List<String> availableColumns = new ArrayList<String>();
		SimpleFeatureType sch=null;
	//	try {
		//	if(labor.inStore==null){
				//XXX quizas haya que tener en cuenta inCollection tambien
				sch =labor.outCollection.getSchema();
//			} else {
//				sch = labor.inStore.getSchema();	
//			}

			List<AttributeType> types = sch.getTypes();
			for (AttributeType at : types) {
				//at binding para Importe_ha es class java.lang.Double
				//System.out.println("at binding para "+at.getName() +" es "+at.getBinding());
				if(Number.class.isAssignableFrom(at.getBinding() )) {
					availableColumns.add(at.getName().toString());
				}
			}
//
//		} catch (IOException e) {			
//			e.printStackTrace();
//		}
		return availableColumns;
	}
	
	
	public void show() {
		List<Labor<?>> labores = main.getLaboresCargadas();
		ObservableList<Labor<?>> observableLabores = FXCollections.observableArrayList(labores);
		
		ChoiceBox<Labor<?>> choiceX = new ChoiceBox<Labor<?>>();
		choiceX.setItems(observableLabores);
		ObservableList<String> observableColumnsX = FXCollections.observableArrayList(new ArrayList<String>());
		choiceX.setOnAction(a->{
			Labor<?> selectedL = choiceX.getValue();
			List<String> columns = getOutColumns(selectedL);
			observableColumnsX.clear();
			observableColumnsX.addAll(columns);
		});
		ChoiceBox<String> choiceXColumn = new ChoiceBox<String>();
		choiceXColumn.setItems(observableColumnsX);
		
		ChoiceBox<Labor<?>> choiceY = new ChoiceBox<Labor<?>>();
		choiceY.setItems(observableLabores);
		ObservableList<String> observableColumnsY = FXCollections.observableArrayList(new ArrayList<String>());
		choiceY.setOnAction(a->{
			Labor<?> selectedL = choiceY.getValue();
			List<String> columns = getOutColumns(selectedL);
			observableColumnsY.clear();
			observableColumnsY.addAll(columns);
		});
		ChoiceBox<String> choiceYColumn = new ChoiceBox<String>();
		choiceYColumn.setItems(observableColumnsY);
		
		Button accept = new Button(Messages.getString("Recorrida.Aceptar"));//"Aceptar");
	
		//TODO arreglar layout
		VBox vb = new VBox();
		Insets outerMargin = new Insets(10);
		Insets innerMargin = new Insets(5);
		
		Label xAxisLabel = new Label("X: ");
		HBox.setMargin(xAxisLabel, innerMargin);
		HBox.setMargin(choiceX, innerMargin);
		HBox.setMargin(choiceXColumn, innerMargin);
		HBox.setHgrow(choiceXColumn, Priority.ALWAYS);
		HBox hb1 = new HBox(xAxisLabel,choiceX,choiceXColumn);
		VBox.setMargin(hb1, outerMargin);
		Label yAxisLabel = new Label("Y: ");
		HBox.setMargin(yAxisLabel, innerMargin);
		HBox.setMargin(choiceY, innerMargin);
		HBox.setMargin(choiceYColumn, innerMargin);
		HBox.setHgrow(choiceYColumn, Priority.ALWAYS);
		HBox hb2 = new HBox(yAxisLabel,choiceY,choiceYColumn);
		VBox.setMargin(hb2, outerMargin);
		BorderPane hb3 = new BorderPane();
		VBox.setMargin(hb3, outerMargin);
		hb3.setRight(accept);
		vb.getChildren().addAll(hb1,hb2,hb3);
		Scene sc = new Scene(vb, 500, 400);
		
		Stage s= new Stage();
		accept.setOnAction((ae)->{
			s.close();
		});
		s.setTitle("Seleccione labores a correlacionar y sus columnas");
		s.initOwner(JFXMain.stage);
		s.getIcons().addAll(JFXMain.stage.getIcons());
		s.setScene(sc);
		s.sizeToScene();
		s.showAndWait();
		
		
		Labor<?> laborX = choiceX.getValue();
		String columnX = choiceXColumn.getValue();
		
		Labor<?> laborY = choiceY.getValue();
		String columnY = choiceYColumn.getValue();
		
		//TODO construir grilla y para cada punto agregar un item a la serie y mostrar un grafico de la serie
		 //XYChart.Series<Number, Number> series = 
				 constructAndShowSeries(laborX, columnX, laborY, columnY);
		// dataSeries.add(series);
		
		
		
	}
	
	public void showSkatterSeries( String xAxisName, String yAxisName, XYChart.Series<Number, Number> series) {
		Stage s= new Stage();	
		s.setTitle("Grafico de correlación");
		s.initOwner(JFXMain.stage);
		s.getIcons().addAll(JFXMain.stage.getIcons());
	    
		SkatterChartWithRegression chart = SkatterChartWithRegression.construct();
		chart.getXAxis().setLabel(xAxisName);                
		chart.getYAxis().setLabel(yAxisName);

		chart.setTitle("Correlación");
		
		chart.getData().add(series);
		String ecuation = chart.getEcuation(series);
		Button exportButton = new Button(Messages.getString("NDVIHistoChart.exportar")); //$NON-NLS-1$
		exportButton.setOnAction(a->{doExportarExcell(series);});
		VBox v = new VBox();
		VBox.setVgrow(chart, Priority.ALWAYS);
		BorderPane bottom = new BorderPane();
		bottom.setLeft(new Label(ecuation));
		bottom.setRight(exportButton);
		VBox.setMargin(bottom, new Insets(10));
		v.getChildren().addAll(chart,bottom);
		Scene scene  = new Scene(v, 500, 400);
		s.setScene(scene);
		
		s.showAndWait();
	}


	private void doExportarExcell( XYChart.Series<Number, Number> series) {
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportNumberSeries(series);
	}

	
	public  void constructAndShowSeries(Labor<?> laborX, String columnX, Labor<?> laborY, String columnY) {
		CorrelacionarLayersTask task = new CorrelacionarLayersTask(laborX,  columnX,  laborY,  columnY);
		task.installProgressBar(main.getProgressBox());
		task.setOnSucceeded(handler -> {
			task.uninstallProgressBar();
			
			try {
				XYChart.Series<Number, Number> series = task.get();
				showSkatterSeries(laborX.getNombre()+"-"+columnX, laborY.getNombre()+"-"+columnY, series);
			} catch (InterruptedException | ExecutionException e) {				
				e.printStackTrace();
			}
			
			
		});
		JFXMain.executorPool.execute(task);
	}
}
