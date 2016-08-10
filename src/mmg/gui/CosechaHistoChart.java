package mmg.gui;

import java.text.DecimalFormat;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import utils.ProyectionConstants;

import com.vividsolutions.jts.geom.Geometry;

import dao.CosechaLabor;
import dao.ExcelHelper;
import dao.FeatureContainer;
import dao.Labor;

public class CosechaHistoChart extends VBox {
	// VBox root = new VBox();
	private static final String ICON = "gisUI/1-512.png";
	private String[] colors = {
		//	"rgb(158,1,66)",
		//	"rgb(213,62,79)",
			" rgb(244,109,67)", 
			" rgb(253,174,97)",
			" rgb(254,224,139)",
			" rgb(255,255,191)",
			" rgb(230,245,152)",
			" rgb(171,221,164)",
			"rgb(102,194,165)",
			"rgb(50,136,189)",// "BLUE"};
	"DARKBLUE" };

	// Color.rgb(94,79,162)};
 	@SuppressWarnings("unchecked")
 	private	XYChart.Series<String, Number> series =null;
 	private Double superficieTotal= new Double(0);
 	private Double produccionTotal= new Double(0);
	private Double entropia= new Double(0);
	private int numClasses;

	public CosechaHistoChart(Labor<?> labor) {
		super();
		

		final CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Rinde");
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Superficie");
		final BarChart<String, Number> chart = new BarChart<String, Number>(xAxis, yAxis);
		chart.setTitle(labor.getNombreProperty().get());
//				{
//			 /** @inheritDoc */
//		    @Override 
//		    protected void layoutPlotChildren() {
//		        double catSpace = xAxis.getCategorySpacing();
//		        // calculate bar spacing
//		        final double avilableBarSpace = catSpace - (getCategoryGap() + getBarGap());
//		        double barWidth = (avilableBarSpace / getData().size()) - getBarGap();
//		        final double barOffset = -((catSpace - getCategoryGap()) / 2);
//		        final double zeroPos = (yAxis.getLowerBound() > 0) ? 
//		        		yAxis.getDisplayPosition(yAxis.getLowerBound()) : yAxis.getZeroPosition();
//		        // RT-24813 : if the data in a series gets too large, barWidth can get negative.
//		        if (barWidth <= 0) barWidth = 1;
//		        // update bar positions and sizes
//		        int catIndex = 0;
//		        for (String category : xAxis.getCategories()) {
//		            int index = 0;
//		            for (Iterator<Series<String, Number>> sit = getDisplayedSeriesIterator(); sit.hasNext(); ) {
//		                Series<String, Number> series = sit.next();
//		                final Data<String, Number> item = getDataItem(series, index, catIndex, category);
//		                if (item != null) {
//		                    final Node bar = item.getNode();
//		                    final double categoryPos;
//		                    final double valPos;
//		                    if (orientation == Orientation.VERTICAL) {
//		                        categoryPos = getXAxis().getDisplayPosition(item.getCurrentX());
//		                        valPos = getYAxis().getDisplayPosition(item.getCurrentY());
//		                    } else {
//		                        categoryPos = getYAxis().getDisplayPosition(item.getCurrentY());
//		                        valPos = getXAxis().getDisplayPosition(item.getCurrentX());
//		                    }
//		                    if (Double.isNaN(categoryPos) || Double.isNaN(valPos)) {
//		                        continue;
//		                    }
//		                    final double bottom = Math.min(valPos,zeroPos);
//		                    final double top = Math.max(valPos,zeroPos);
//		                    bottomPos = bottom;
//		                    if (orientation == Orientation.VERTICAL) {
//		                        bar.resizeRelocate( categoryPos + barOffset + (barWidth + getBarGap()) * index,
//		                                            bottom, barWidth, top-bottom);
//		                    } else {
//		                        //noinspection SuspiciousNameCombination
//		                        bar.resizeRelocate( bottom, categoryPos + barOffset + (barWidth + getBarGap()) * index,
//		                                            top-bottom, barWidth);
//		                    }
//
//		                    index++;
//		                }
//		            }
//		            catIndex++;
//		        }
//		    }
//		};


		
		
	series = createSeries(labor);
		
		chart.legendVisibleProperty().setValue(false);
		chart.getData().add(series);
		VBox.getVgrow(chart);
		this.getChildren().add(chart);
		DecimalFormat df = new DecimalFormat("#.00");
		BorderPane bottom = new BorderPane();
		VBox left = new VBox();
		left.getChildren().addAll(
				new Label("Superficie Total: "+df.format(superficieTotal)),
				new Label("Produccion: "+df.format(produccionTotal)),
				new Label("Rinde Promedio: "+df.format(produccionTotal/superficieTotal)),
				new Label("Entropia: "+df.format(entropia)));
		VBox right = new VBox();
		Button exportButton = new Button("Exportar");
		exportButton.setOnAction(a->{doExportarExcell();});
		right.getChildren().add(exportButton);
		bottom.setCenter(left);
		bottom.setRight(right);//getChildren().addAll(left,right);
		bottom.setPadding(new Insets(5,5,5,5));
		this.getChildren().add(bottom);
		


	}
	
		
	private void doExportarExcell() {
		System.out.println("TODO implementar doExportarExcell");
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportSeries(series);
		//TODO seleccionar archivo de destino
		//TODO crear una columna de strings y una columna de nummeros
		for(Data<String, Number> data:series.getData()){
			//todo agregar datos a las columnas
			
		}
		//TODO guardar el archivo
		
	}


	private XYChart.Series<String, Number> createSeries(Labor labor) {	
		 numClasses = labor.clasificador.getNumClasses();
		Double[] superficies = null;
		Double[] producciones = null;
		if(numClasses<colors.length){
			superficies = new Double[numClasses];
			producciones = new Double[numClasses];
		}else{
			superficies = new Double[colors.length];
			producciones = new Double[colors.length];
		}

		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			Double rinde =FeatureContainer.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));//labor.colAmount.get()
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Double area = geometry.getArea() * ProyectionConstants.A_HAS;
			int categoria = labor.getClasificador().getCategoryFor(rinde);

			Double sup = superficies[categoria];
			Double prod = producciones[categoria];
			if (sup == null) sup = new Double(0);
			if (prod == null) prod = new Double(0);
			superficies[categoria] = sup + area;	
			producciones[categoria] = prod + rinde*area;	
			
			this.produccionTotal+=rinde*area;
			this.superficieTotal+=area;
		}
		it.close();
		
		this.entropia=new Double(0);
		for(int i=0;i<superficies.length;i++){
			Double s = superficies[i];
			if(superficieTotal>0&&s!=null&&s>0){
			double p = superficies[i]/this.superficieTotal;
			entropia-=p*Math.log(p)/Math.log(2);
			}
		}


		XYChart.Series<String, Number> series = new XYChart.Series<>();

		// Create a XYChart.Data object for each month. Add it to the series.
	
		for (int j = 0; j <numClasses; j++) {
			//FIXME esto solo funciona despues de pedir una cosecha. en otro caso el clasifier se cambia y muestra datos equivocados
			
			String label =labor.getClasificador().getCategoryNameFor(j);//classifier.getTitle(j);
			//String rangoFin = classifier.getTitle(j+1);
			//System.out.println("rangoIni: "+rangoIni);//rangoIni: 1.0146282563539477..1.208709021558479		
			//System.out.println("rangoFin: "+rangoFin);//rangoFin: 1.208709021558479..1.2725564427424458
			
		
		//	String [] partesFin = rangoFin.split("\\.");
			
		
//			String label = df.format(new Double(partesIni[0]))+"~"+df.format(new Double(partesIni[1]));// +"-"+histograma[j+1];
			
			
			Number superficie = superficies[j];
			if (superficie == null)		superficie = new Double(0);
			Number produccion = producciones[j];
			if (produccion == null)		superficie = new Double(0);
			Data<String, Number> cData = new XYChart.Data<>(label, superficie);
			cData.setExtraValue(produccion);
			String color = getColorString(j);
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


	private String getColorString(int absCat) {
		int length =colors.length-1;
		int clases = numClasses-1;//las clases van de cero a numclases -1 para un total de numclases
		int colorIndex = absCat*(length/clases);
		//System.out.println(absCat+"*"+length+"/"+clases+" = "+colorIndex+" colorIndex");
		return colors[colorIndex];
	}

}
