package com.ursulagis.desktop.gui;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Ndvi;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import com.ursulagis.desktop.gui.utils.TooltipUtil;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.ursulagis.desktop.tasks.ShowNDVITifFileTask;
import com.ursulagis.desktop.utils.ExcelHelper;
import lombok.extern.java.Log;
 @Log
public class NDVIHistoChart extends VBox {
//	private static final String ICON = "gisUI/1-512.png";
	public static final String[] colors = {
			//	"rgb(158,1,66)",
			//	"rgb(213,62,79)",
			" rgb(244,109,67)",  //$NON-NLS-1$
			" rgb(253,174,97)", //$NON-NLS-1$
			" rgb(254,224,139)", //$NON-NLS-1$
			" rgb(255,255,191)", //$NON-NLS-1$
			" rgb(230,245,152)", //$NON-NLS-1$
			" rgb(171,221,164)", //$NON-NLS-1$
			"rgb(102,194,165)", //$NON-NLS-1$
			"rgb(50,136,189)",// "BLUE"}; //$NON-NLS-1$
	"DARKBLUE" }; //$NON-NLS-1$

	// Color.rgb(94,79,162)};
	private	XYChart.Series<String, Number> series =null;
	private Double superficieTotal= 0.0;
	
	private Double superficieAgua= 0.0;
	private Double superficieNube= 0.0;
	private Double ndviPromedio= 0.0;
//	private Double produccionTotal= 0.0;
//	private Double entropia= 0.0;
	private int numClasses;
	private NumberFormat df=Messages.getNumberFormat();
	
	public NDVIHistoChart(Ndvi ndvi) {
		super();
		
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		log.addHandler(consoleHandler);
		
		final CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("NDVI"); //$NON-NLS-1$
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel(Messages.getString("NDVIHistoChart.Superficie")); //$NON-NLS-1$
		final BarChart<String, Number> chart = new BarChart<String, Number>(xAxis, yAxis);
		chart.setTitle(ndvi.getNombre()+" NDVI"); //$NON-NLS-1$

		series = createSeries(ndvi);

		chart.legendVisibleProperty().setValue(false);
		chart.getData().add(series);
		VBox.setVgrow(chart,Priority.ALWAYS);
		this.getChildren().add(chart);
		BorderPane bottom = new BorderPane();
		
		double superficieCultivo = superficieTotal-superficieAgua-superficieNube;
		
		VBox left = new VBox();
		left.getChildren().addAll(
				new Label(Messages.getString("NDVIHistoChart.ndvi_promedio")+df.format(ndviPromedio)), //$NON-NLS-1$
				new Label(Messages.getString("NDVIHistoChart.sup_cultivo")+df.format(superficieCultivo)+Messages.getString("NDVIHistoChart.has5")+df.format(superficieCultivo/superficieTotal*100)+"%"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new Label(Messages.getString("NDVIHistoChart.sup_nubes")+df.format(superficieNube)+Messages.getString("NDVIHistoChart.has4")+df.format(superficieNube/superficieTotal*100)+"%"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new Label(Messages.getString("NDVIHistoChart.sup_agua")+df.format(superficieAgua)+Messages.getString("NDVIHistoChart.has3")+df.format(superficieAgua/superficieTotal*100)+"%"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new Label(Messages.getString("NDVIHistoChart.sup_total")+df.format(superficieTotal)+Messages.getString("NDVIHistoChart.has2")) //$NON-NLS-1$ //$NON-NLS-2$
				);
		
	
		
		VBox right = new VBox();
		Button exportButton = new Button(Messages.getString("NDVIHistoChart.exportar")); //$NON-NLS-1$
		exportButton.setOnAction(a->{doExportarExcell();});
		right.getChildren().add(exportButton);
		bottom.setCenter(left);
		bottom.setRight(right);//getChildren().addAll(left,right);
		bottom.setPadding(new Insets(2,30,30,30));
		this.getChildren().add(bottom);
		
		

	}


	private void doExportarExcell() {
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportSeriesNdvi(series);
	}


	private XYChart.Series<String, Number> createSeries(Ndvi ndvi) {	
	   //Double[] histograma = new Double[]{ShowNDVITifFileTask.MIN_VALUE,0.29, 0.38, 0.46, 0.55, 0.64, 0.73, 0.81, ShowNDVITifFileTask.MAX_VALUE};
		Double[] histograma = new Double[]{0.20,0.30,0.40,0.50,0.60,0.70,0.80,0.90,ShowNDVITifFileTask.MAX_VALUE};



		double delta = (ShowNDVITifFileTask.MAX_VALUE-ShowNDVITifFileTask.MIN_VALUE)/(histograma.length);
		for(int i =0;i<histograma.length;i++) {
			histograma[i]=ShowNDVITifFileTask.MIN_VALUE+delta*(i+1);			
		}
		//FIXME este histograma va de -infinito a +infinito. deberia ir de ShowNDVITifFileTask.MIN_VALUE a ShowNDVITifFileTask.MAX_VALUE
		numClasses=histograma.length;//histograma tiene un elemento menos que clases porque se extiende a inf
		
		//numClasses = 9;//ndvi.clasificador.getNumClasses();
		Double[] superficies = null;
		String[] colorStrings = null;
		Double[] ndvisCat = null;
		
	//	if(numClasses<colors.length){
			superficies = new Double[numClasses];
			colorStrings = new String[numClasses];
			ndvisCat = new Double[numClasses];
//		}else{
//			superficies = new Double[colors.length];
//			colorStrings = new String[colors.length];
//		}

//		double width = 2;
//		double desvioEstandar =(0.2);// width/numClasses;

	
//		int nDesvios = 5;//XXX que es este 8? es la cantidad de desvios que voya a dibujar
//		Double deltaForColour =(nDesvios*desvioEstandar)/numClasses;
//
//		for(int i = 0;i<numClasses;i++){	
//			histograma[i]=(0.25-(nDesvios/2)*desvioEstandar)+deltaForColour*(i+1);
//		}

		//System.out.println("pixel area = "+ndvi.getPixelArea());
		Clasificador clasi = new Clasificador();
		clasi.setHistograma(histograma);
		Iterable<? extends GridPointAttributes> values = ndvi.getSurfaceLayer().getValues();
		Iterator<? extends GridPointAttributes> it = values.iterator();
		
		if(ndvi.getPixelArea()==0.0) {
			//log.fine("seteandoPixelArea porque esta en cero");
			ndvi.setPixelArea(0.008084403745300213);
		}
		//log.fine("ndvi pixel area "+ndvi.getPixelArea());
		

		int size = 0;
		while(it.hasNext()){
			
			GridPointAttributes f = it.next();
//			Color color = f.getColor();
			double value = f.getValue();
			if(value < ShowNDVITifFileTask.MIN_VALUE || value > ShowNDVITifFileTask.MAX_VALUE || value == ShowNDVITifFileTask.TRANSPARENT_VALUE ){
				if(value==ShowNDVITifFileTask.CLOUD_RENDER_VALUE){
					this.superficieNube+=ndvi.getPixelArea();
					this.superficieTotal+=ndvi.getPixelArea();
				} else if(value == ShowNDVITifFileTask.WATER_RENDER_VALUE){//me esta sumando todos los valores nulos
					//System.out.println("sumando un valor de agua "+value);
					this.superficieAgua+=ndvi.getPixelArea();
					this.superficieTotal+=ndvi.getPixelArea();
				} 
			//	System.out.println("continuando por ndvi fuera de rango "+value);
				continue;
			} else if(value >= ShowNDVITifFileTask.MIN_VALUE && value <= ShowNDVITifFileTask.MAX_VALUE ) {
				size++;
				//log.fine("agregando a cultivo el value "+value);
				int categoria = clasi.getCategoryFor(value);
				String colorString = colorStrings[categoria];
			//	if(colorString == null){
					Color color = f.getColor();
					colorString = " rgb("+color.getRed() //$NON-NLS-1$
					+","+color.getGreen() //$NON-NLS-1$
					+","+color.getBlue()+")"; //$NON-NLS-1$ //$NON-NLS-2$
					colorStrings[categoria]=colorString;
			//	}
					
					
				Double sup = superficies[categoria];		
				
				//System.out.println("value: "+value+" categoria: "+categoria+" sup:"+sup);

				if (sup == null) sup = 0.0;		
				superficies[categoria] = sup + ndvi.getPixelArea();	
				Double ndviCat = ndvisCat[categoria];
				if(ndviCat==null)ndviCat = 0.0;
				ndvisCat[categoria] = (ndviCat*sup + value*ndvi.getPixelArea())/superficies[categoria];	

				this.superficieTotal+=ndvi.getPixelArea();
				this.ndviPromedio+=value;
			}
			
			
		}
		if(size>0) {
			ndviPromedio = ndviPromedio/size;	
		} else {
			ndviPromedio = 0.0;
		}
		
		//System.out.println("la cantidad de valores del ndvi es "+size);

		XYChart.Series<String, Number> series = new XYChart.Series<>();


		for (int iCat = 0; iCat <histograma.length; iCat++) {
			String label =clasi.getCategoryNameFor(iCat);//classifier.getTitle(j);
			if(iCat==0)label= df.format(ShowNDVITifFileTask.MIN_VALUE) +" ~ "+ df.format(histograma[0]);
			//if(iCat==histograma.length)label=  df.format(histograma[iCat-1])+" ~ "+ ShowNDVITifFileTask.MAX_VALUE;
			Number superficie = superficies[iCat];
			if (superficie == null)		superficie = 0.0;
			//System.out.println("value: "+label+" categoria: "+iCat+" sup:"+superficie);
			Data<String, Number> cData = new XYChart.Data<>(label, superficie);
			if(superficie.doubleValue()>0) {
				cData.setExtraValue(ndvisCat[iCat]);
			} else { 
				cData.setExtraValue(0.0);
			}
			String color = getColorString(iCat,colorStrings);
			TooltipUtil.setupCustomTooltipBehavior(50, 60000, 50);
			cData.nodeProperty().addListener(( ov, oldNode, newNode)-> {
					if (newNode != null) {
						if(color!=null){
							newNode.setStyle("-fx-bar-fill: " + color + ";"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						
						double has = cData.getYValue().doubleValue();
						double ndviProm = (double)cData.getExtraValue();
						Tooltip tooltip = new Tooltip(
								Messages.getString("NDVIHistoChart.ndvi_promedio")+df.format(ndviProm)+"\n"
								+df.format(has)+Messages.getString("NDVIHistoChart.has1")
								+df.format(has/superficieTotal*100)+"%"); //$NON-NLS-1$ //$NON-NLS-2$
						tooltip.autoHideProperty().set(false);
						Tooltip.install(newNode,tooltip );					
					}				
			});
			series.getData().add(cData);
		}
		return series;
	}


//	private String getColorString(int absCat) {
//		int length =colors.length;
//		int clases = numClasses;//las clases van de cero a numclases -1 para un total de numclases
//		int colorIndex = absCat*(length/clases);
//		return colors[colorIndex];
//	}
	
	private String getColorString(int absCat,String[] colors) {
		int length =colors.length;
		int clases = numClasses;//las clases van de cero a numclases -1 para un total de numclases
		int colorIndex = absCat*(length/clases);
		return colors[colorIndex];
	}

}
