package com.ursulagis.desktop.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.ursulagis.desktop.utils.ExcelHelper;
//agrege que extende de vbox 
public class NDVIChart extends VBox {
	private WorldWindow wwd;
	private LineChart<Number,Number> lineChart =null;


	public NDVIChart(WorldWindow _wwd) {
		super ();//sueper
		this.wwd=_wwd;
		//	this.layerPanel=_lP;

	}

	public void doShowNDVIChart(boolean acumulado) {
		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
		List<SurfaceImageLayer> ndviLayers = extractLayers();
	
		//System.out.println("mostrar grafico");
		final NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel("Fecha");
		xAxis.setTickLabelFormatter(new StringConverter<Number>() {
			@Override
			public String toString(Number epochDay) {
				try {
					return LocalDate.ofEpochDay(epochDay.longValue()).toString();
				}catch(Exception e) {
					return "";
				}				
			}
			

			@Override
			public Number fromString(String string) {
				try {
				return LocalDate.parse(string).toEpochDay();
				}catch(Exception e) {
					return 0;
				}
			
			}			
		});
		
		xAxis.setLowerBound(Double.MAX_VALUE);	
		xAxis.setAutoRanging(false);
		
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("NDVI");
	
		ObservableList<Series<Number, Number>> data = FXCollections.observableArrayList();// new ArrayList<Series<Number, Number>>();

		Map<String, List<SurfaceImageLayer>>  contornoMap = ndviLayers.stream().collect(
				Collectors.groupingBy((l2)->{
					Ndvi lNdvi = (Ndvi)l2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					return lNdvi.getContorno().getNombre();// fecha me devuelve siempre hoy por eso no hace la animacion
				}));
		
		contornoMap.keySet().stream().forEach((c)->{
			XYChart.Series<Number,Number> sr = new XYChart.Series<Number,Number>(); 
			sr.setName(c );
			LocalDate[] lastFecha =new LocalDate[1];//.now();
			SimpleDoubleProperty ndviAcumProp = new SimpleDoubleProperty(0);
		
			contornoMap.get(c).stream()
			.map((layer)->(Ndvi)layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR))
			.sorted((n1,n2)->n1.compareTo(n2))
			.forEachOrdered(lNdvi->{
				try {
				LocalDate fecha = lNdvi.getFecha();
				long dias=5;//el minimo aporte que deberia tener una imagen es 5 dias
				if(lastFecha[0]==null) {
					lastFecha[0]=fecha;					
				} else {					
					dias = java.time.temporal.ChronoUnit.DAYS.between(lastFecha[0], fecha);
					lastFecha[0]=fecha;
				}
				//acumulo el ndvi		
				if (acumulado == true) {
					ndviAcumProp.set(ndviAcumProp.get()+lNdvi.getMeanNDVI().doubleValue()*dias);
				}else {
					ndviAcumProp.set( lNdvi.getMeanNDVI().doubleValue());
				}	
				xAxis.setLowerBound(Math.min(xAxis.getLowerBound(),lNdvi.getFecha().toEpochDay()-5));
				xAxis.setUpperBound(Math.max(xAxis.getUpperBound(),lNdvi.getFecha().toEpochDay()+5));
				BigDecimal bd = new BigDecimal(ndviAcumProp.get()).setScale(2, RoundingMode.HALF_EVEN);
				
				sr.getData().add(new XYChart.Data<Number, Number>(lNdvi.getFecha().toEpochDay(), bd.doubleValue()));
				}catch(Exception e) {
					System.err.println("Excepcion para "+lNdvi.getNombre());
					e.printStackTrace();
				}
			});
			data.add(sr);	
		});
		xAxis.setTickLabelRotation(90);
		xAxis.setTickUnit(5);
		
		lineChart = new LineChart<Number, Number>(xAxis, yAxis,data);				
		lineChart.setAxisSortingPolicy(LineChart.SortingPolicy.X_AXIS);
		
		VBox vbox = new VBox(lineChart);
		VBox.setVgrow(lineChart, Priority.ALWAYS);
		VBox.setVgrow(vbox, Priority.ALWAYS);
		VBox right = new VBox();
		Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //$NON-NLS-1$
		exportButton.setOnAction(a->{doExportarExcel();});
		right.getChildren().add(exportButton);
		BorderPane bottom = new BorderPane();
		//bottom.setCenter(left);
		bottom.setRight(right);//getChildren().addAll(left,right);
		bottom.setPadding(new Insets(5,5,5,5));
		vbox.getChildren().add(bottom);
		//this.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());
		//String style= this.getStyle();
		
		// this.getStylesheets().add("""
		// 					.onHover{
		// 			-fx-background-color: BLACK;
		// 			-fx-font-size: 20;
		// 		}
		// 	""");
		this.getChildren().add(vbox);
		
		/**
         * Browsing through the Data and applying ToolTip
         * as well as the class on hover
         */
        for (XYChart.Series<Number, Number> s : lineChart.getData()) {
            for (XYChart.Data<Number, Number> d : s.getData()) {
                Tooltip.install(d.getNode(), 
                		new Tooltip(
                				s.getName()
                				+"\nNDVI: " + d.getYValue()
                				+ "\n" +
                				Messages.getString("JFXMain.show_ndvi_chart.Fecha")+": " + toString(d.getXValue())                               
                                )
                		);

                //Adding class on hover
                d.getNode().setOnMouseEntered(event -> d.getNode().getStyleClass().add("onHover"));

                //Removing class on exit
                d.getNode().setOnMouseExited(event -> d.getNode().getStyleClass().remove("onHover"));
            }
        }
		

	
		System.out.println("Mostre grafico");


	}
	
	
	private String toString(Number epochDay) {
		try {
			return LocalDate.ofEpochDay(epochDay.longValue()).toString();
		}catch(Exception e) {
			return "";
		}				
	}

	private void doExportarExcel() {
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportSeriesList(this.lineChart.getData());
	}
	
	public List<SurfaceImageLayer> extractLayers() {
		List<SurfaceImageLayer> ndviLayers = new ArrayList<SurfaceImageLayer>();
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Ndvi){
				//l.setEnabled(false);
				ndviLayers.add((SurfaceImageLayer) l);
			}
		}	

		//System.out.println("mostrando la evolucion de "+ndviLayers.size()+" layers");
		ndviLayers.sort(new NdviLayerComparator());
		return ndviLayers;
	}

	public class NdviLayerComparator implements Comparator<Layer>{
		DateTimeFormatter df =null;// DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		public NdviLayerComparator() {
			df = DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		}

		@Override
		public int compare(Layer c1, Layer c2) {			
			String l1Name =c1.getName();
			String l2Name =c2.getName();

			Object labor1 = c1.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			Object labor2 = c2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);


			if(labor1 != null && labor1 instanceof Ndvi && 
					labor2 != null && labor2 instanceof Ndvi ){
				Ndvi ndvi1 = (Ndvi)labor1;
				Ndvi ndvi2 = (Ndvi)labor2;

				try{
					return ndvi1.getFecha().compareTo(ndvi2.getFecha());
				} catch(Exception e){
					//System.err.println("no se pudo comparar las fechas de los ndvi. comparando nombres"); //$NON-NLS-1$
				}
				// comparar por el valor del layer en vez del nombre del layer
				try{
					LocalDate d1 = LocalDate.parse(l1Name.substring(l1Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					LocalDate d2 = LocalDate.parse(l2Name.substring(l2Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					return d1.compareTo(d2);
				} catch(Exception e){
					//no se pudo parsear como fecha entonces lo interpreto como string.
					e.printStackTrace();
				}
			}
			return l1Name.compareToIgnoreCase(l2Name);
		}
	}
}
