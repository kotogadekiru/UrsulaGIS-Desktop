package gui;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Iterator;

import dao.Clasificador;
import dao.Ndvi;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gui.utils.TooltipUtil;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import tasks.ShowNDVITifFileTask;
import utils.ExcelHelper;

public class NDVIHistoChart extends VBox {
//	private static final String ICON = "gisUI/1-512.png";
	public static final String[] colors = {
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
	
	private Double superficieAgua= new Double(0);
	private Double superficieNube= new Double(0);
	private Double ndviPromedio= new Double(0);
//	private Double produccionTotal= new Double(0);
//	private Double entropia= new Double(0);
	private int numClasses;

	public NDVIHistoChart(Ndvi ndvi) {
		super();

		final CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("NDVI");
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Superficie");
		final BarChart<String, Number> chart = new BarChart<String, Number>(xAxis, yAxis);
		chart.setTitle(ndvi.getNombre()+" NDVI");

		series = createSeries(ndvi);

		chart.legendVisibleProperty().setValue(false);
		chart.getData().add(series);
		VBox.getVgrow(chart);
		this.getChildren().add(chart);
		DecimalFormat df = new DecimalFormat("0.00");
		BorderPane bottom = new BorderPane();
		
		double superficieCultivo = superficieTotal-superficieAgua-superficieNube;
		
		VBox left = new VBox();
		left.getChildren().addAll(
				new Label("NDVI Promedio: "+df.format(ndviPromedio)),
				new Label("Superficie Cultivo: "+df.format(superficieCultivo)+"Has "+df.format(superficieCultivo/superficieTotal*100)+"%"),
				new Label("Superficie Nubes: "+df.format(superficieNube)+"Has "+df.format(superficieNube/superficieTotal*100)+"%"),
				new Label("Superficie Agua: "+df.format(superficieAgua)+"Has "+df.format(superficieAgua/superficieTotal*100)+"%"),
				new Label("Superficie Total: "+df.format(superficieTotal)+"Has")
				);
		
	
		
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
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportSeries(series);
	}


	private XYChart.Series<String, Number> createSeries(Ndvi ndvi) {	
		Double[] histograma = new Double[]{0.2,0.29, 0.38, 0.46, 0.55, 0.64, 0.73, 0.81, 0.90};
		numClasses=histograma.length+1;//histograma tiene un elemento menos que clases porque se extiende a inf
		
		//numClasses = 9;//ndvi.clasificador.getNumClasses();
		Double[] superficies = null;
		String[] colorStrings = null;
		
	//	if(numClasses<colors.length){
			superficies = new Double[numClasses];
			colorStrings = new String[numClasses];
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

		int size = 0;
		while(it.hasNext()){
			
			GridPointAttributes f = it.next();
//			Color color = f.getColor();
			double value = f.getValue();
			if(value < ShowNDVITifFileTask.MIN_VALUE || value > ShowNDVITifFileTask.MAX_VALUE || value == 0 ){
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
			} else if(value >= ShowNDVITifFileTask.MIN_VALUE && value <= ShowNDVITifFileTask.MAX_VALUE) {
				size++;
				int categoria = clasi.getCategoryFor(value);
				String colorString = colorStrings[categoria];
			//	if(colorString == null){
					Color color = f.getColor();
					colorString = " rgb("+color.getRed()
					+","+color.getGreen()
					+","+color.getBlue()+")";
					colorStrings[categoria]=colorString;
			//	}
				Double sup = superficies[categoria];		
				
				//System.out.println("value: "+value+" categoria: "+categoria+" sup:"+sup);

				if (sup == null) sup = new Double(0);		
				superficies[categoria] = sup + ndvi.getPixelArea();	

				this.superficieTotal+=ndvi.getPixelArea();
				this.ndviPromedio+=value;
			}
			
			
		}
		ndviPromedio = ndviPromedio/size;
		//System.out.println("la cantidad de valores del ndvi es "+size);

		XYChart.Series<String, Number> series = new XYChart.Series<>();


		for (int iCat = 0; iCat <numClasses; iCat++) {
			String label =clasi.getCategoryNameFor(iCat);//classifier.getTitle(j);
			Number superficie = superficies[iCat];
			if (superficie == null)		superficie = new Double(0);
			//System.out.println("value: "+label+" categoria: "+iCat+" sup:"+superficie);
			Data<String, Number> cData = new XYChart.Data<>(label, superficie);
		
			String color = getColorString(iCat,colorStrings);
			TooltipUtil.setupCustomTooltipBehavior(50, 60000, 50);
			cData.nodeProperty().addListener(( ov, oldNode, newNode)-> {
					if (newNode != null) {
						if(color!=null){
							newNode.setStyle("-fx-bar-fill: " + color + ";");
						}
						DecimalFormat df = new DecimalFormat("0.00");
							df.setGroupingSize(3);
							df.setGroupingUsed(true);
							double val = cData.getYValue().doubleValue();
							Tooltip tooltip = new Tooltip(df.format(val)+" Has\n"+df.format(val/superficieTotal*100)+"%");
							tooltip.autoHideProperty().set(false);
							Tooltip.install(newNode,tooltip );					
					}				
			});
			series.getData().add(cData);
		}
		return series;
	}


	private String getColorString(int absCat) {
		int length =colors.length;
		int clases = numClasses;//las clases van de cero a numclases -1 para un total de numclases
		int colorIndex = absCat*(length/clases);
		return colors[colorIndex];
	}
	
	private String getColorString(int absCat,String[] colors) {
		int length =colors.length;
		int clases = numClasses;//las clases van de cero a numclases -1 para un total de numclases
		int colorIndex = absCat*(length/clases);
		return colors[colorIndex];
	}

}
