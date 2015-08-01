package gisUI;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.Classifier;
import org.opengis.feature.simple.SimpleFeature;

import tasks.ProcessMapTask;
import tasks.ProyectionConstants;

import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.CosechaItem;
import dao.Dao;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

public class CosechaHistoChart extends Stage {
	 VBox root = new VBox();
	private static final String ICON = "gisUI/1-512.png";
	private String[] colors = {
	"rgb(158,1,66)", "rgb(213,62,79)", " rgb(244,109,67)", " rgb(253,174,97)",
			" rgb(254,224,139)", " rgb(255,255,191)", " rgb(230,245,152)",
			" rgb(171,221,164)", "rgb(102,194,165)", "rgb(50,136,189)",// "BLUE"};
			"DARKBLUE" };

	// Color.rgb(94,79,162)};

	//Double[] histograma = null;//new Double[colors.length];
//	Double[] promedios = new Double[colors.length];
	Double superficieTotal= new Double(0),produccionTotal= new Double(0);

	public CosechaHistoChart(Quadtree harvestTree) {
		super();
		this.setTitle("Histograma Cosecha");
		this.getIcons().add(new Image(ICON));

		final CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Rinde");
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Superficie");
		final BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);

		// chart.setTitle("Histograma cosecha");

//		@SuppressWarnings("unchecked")
//		XYChart.Series<String, Number> series = createSeries(harvestTree
//				.queryAll());
		
		
		@SuppressWarnings("unchecked")
		XYChart.Series<String, Number> series = createSeriesByJenkins(harvestTree
				.queryAll());
		

		chart.getData().add(series);
		VBox.getVgrow(chart);
		root.getChildren().add(chart);
		DecimalFormat df = new DecimalFormat("#.00");
		root.getChildren().addAll(new Label("Superficie Total: "+df.format(superficieTotal)),new Label("Produccion: "+df.format(produccionTotal)),new Label("Rinde Promedio: "+df.format(produccionTotal/superficieTotal)));
		//TODO agregar total cosechado y rinde promedio y superficie total
		Scene scene = new Scene(root, 800, 600);
		this.setScene(scene);

	}

	private XYChart.Series<String, Number> createSeries(List<? extends Dao> data) {		
		//creo el histograma
		Double [] histograma = ProcessMapTask.constructHistogram(data);
		
		


		// construir la data dividiendo rangos de rinde y sumando su superficie
		Double[] superficies = new Double[colors.length];
		data.forEach(d -> {
			Double rinde = d.getAmount();
			Double area = d.getGeometry().getArea() * ProyectionConstants.A_HAS;
			int categoria = ProcessMapTask.getColorByHistogram(rinde, histograma);
			
			Double sup = superficies[categoria];
			if (sup == null) sup = new Double(0);
			superficies[categoria] = sup + area;	
			
			this.produccionTotal+=rinde*area;
			this.superficieTotal+=area;
		});

		XYChart.Series<String, Number> series = new XYChart.Series<>();

		// Create a XYChart.Data object for each month. Add it to the series.
		DecimalFormat df = new DecimalFormat("#.00");
		for (int j = 0; j < colors.length-1; j++) {
			String label = df.format(histograma[j])+"~"+df.format(histograma[j+1]);// +"-"+histograma[j+1];
			Number number = superficies[j];
			if (number == null)
				number = new Double(0);
			Data<String, Number> cData = new XYChart.Data<>(label, number);
			String color = colors[j];
			cData.nodeProperty().addListener(new ChangeListener<Node>() {

				@Override
				public void changed(ObservableValue<? extends Node> ov,
						Node oldNode, Node newNode) {
					if (newNode != null) {
						newNode.setStyle("-fx-bar-fill: " + color + ";");
					}
				}
			});
			series.getData().add(cData);

			// series.getData().add(new XYChart.Data<>(label, 33));

		}

		return series;

	}
	
	private XYChart.Series<String, Number> createSeriesByJenkins(List<? extends Dao> data) {	
		
		SimpleFeatureCollection collection = new ListFeatureCollection(CosechaItem.getType());
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
				CosechaItem.getType());
		for (CosechaItem cosecha : (List<CosechaItem>)data) {
			SimpleFeature cosechaFeature = cosecha.getFeature(featureBuilder);
			collection.add(cosechaFeature);
		//	System.out.println("agregando a features "+rentaFeature);
		}
		//creo el histograma
		Classifier classifier = ProcessMapTask.constructJenksClasifier(collection,CosechaItem.COLUMNA_RENDIMIENTO);
		


		// construir la data dividiendo rangos de rinde y sumando su superficie
		Double[] superficies = new Double[colors.length];
		data.forEach(d -> {
			Double rinde = d.getAmount();
			Double area = d.getGeometry().getArea() * ProyectionConstants.A_HAS;
			int categoria = ProcessMapTask.getColorByJenks(rinde, classifier);
			
			Double sup = superficies[categoria];
			if (sup == null) sup = new Double(0);
			superficies[categoria] = sup + area;	
			
			this.produccionTotal+=rinde*area;
			this.superficieTotal+=area;
		});

		XYChart.Series<String, Number> series = new XYChart.Series<>();

		// Create a XYChart.Data object for each month. Add it to the series.
		DecimalFormat df = new DecimalFormat("#.00");
		for (int j = 0; j < colors.length; j++) {
			
			String rangoIni = classifier.getTitle(j);
			//String rangoFin = classifier.getTitle(j+1);
			System.out.println("rangoIni: "+rangoIni);//rangoIni: 1.0146282563539477..1.208709021558479		
			//System.out.println("rangoFin: "+rangoFin);//rangoFin: 1.208709021558479..1.2725564427424458
			
			String [] partesIni = rangoIni.split("\\.\\.");
		//	String [] partesFin = rangoFin.split("\\.");
			
		
			String label = df.format(new Double(partesIni[0]))+"~"+df.format(new Double(partesIni[1]));// +"-"+histograma[j+1];
			
			Number number = superficies[j];
			if (number == null)
				number = new Double(0);
			Data<String, Number> cData = new XYChart.Data<>(label, number);
			String color = colors[j];
			cData.nodeProperty().addListener(new ChangeListener<Node>() {

				@Override
				public void changed(ObservableValue<? extends Node> ov,
						Node oldNode, Node newNode) {
					if (newNode != null) {
						newNode.setStyle("-fx-bar-fill: " + color + ";");
					}
				}
			});
			series.getData().add(cData);

			// series.getData().add(new XYChart.Data<>(label, 33));

		}

		return series;

	}

}
