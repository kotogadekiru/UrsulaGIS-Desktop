package com.ursulagis.desktop.gui;

import java.text.NumberFormat;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.gui.utils.TooltipUtil;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.ursulagis.desktop.utils.ExcelHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class CosechaHistoChart extends VBox {
	// VBox root = new VBox();
	//private static final String ICON = "gisUI/1-512.png"; //$NON-NLS-1$
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
	@SuppressWarnings("unchecked")
	private	XYChart.Series<String, Number> series =null;
	private Double superficieTotal= 0.0;
	private Double produccionTotal= 0.0;
	//private Double entropia= 0.0;
	private int numClasses;

	public CosechaHistoChart(Labor<?> labor) {
		super();
		final CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel(Messages.getString("CosechaHistoChart.10")); //$NON-NLS-1$
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel(Messages.getString("CosechaHistoChart.11")); //$NON-NLS-1$
		final BarChart<String, Number> chart = new BarChart<String, Number>(xAxis, yAxis);
		chart.setTitle(labor.getNombre());
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
		VBox.setVgrow(chart,Priority.ALWAYS);
		this.getChildren().add(chart);
		NumberFormat df = Messages.getNumberFormat();
		BorderPane bottom = new BorderPane();
		VBox left = new VBox();
		left.getChildren().addAll(
				new Label(Messages.getString("CosechaHistoChart.13")+df.format(superficieTotal)), //$NON-NLS-1$
				new Label(Messages.getString("CosechaHistoChart.14")+df.format(produccionTotal)), //$NON-NLS-1$
				new Label(Messages.getString("CosechaHistoChart.15")+df.format(produccionTotal/superficieTotal)) //$NON-NLS-1$
				//,new Label("Entropia: "+df.format(entropia))
				);
		VBox right = new VBox();
		Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //$NON-NLS-1$
		exportButton.setOnAction(a->{doExportarExcell();});
		right.getChildren().add(exportButton);
		bottom.setCenter(left);
		bottom.setRight(right);//getChildren().addAll(left,right);
		bottom.setPadding(new Insets(2,30,30,30));
		this.getChildren().add(bottom);
	}

	private void doExportarExcell() {
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportSeries(series);
	}

	private XYChart.Series<String, Number> createSeries(Labor<?> labor) {	
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
			Double rinde = LaborItem.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));//labor.colAmount.get()
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Double area = geometry.getArea() * ProyectionConstants.A_HAS();
			int categoria = labor.getClasificador().getCategoryFor(rinde);

			Double sup = categoria<superficies.length? superficies[categoria]:superficies[superficies.length-1];
			Double prod =  categoria<producciones.length? producciones[categoria]:producciones[producciones.length-1]; //producciones[categoria];
			if (sup == null) sup = 0.0;
			if (prod == null) prod = 0.0;
			superficies[categoria] = sup + area;	
			producciones[categoria] = prod + rinde*area;	

			this.produccionTotal+=rinde*area;
			this.superficieTotal+=area;
		}
		it.close();

		//		this.entropia=0.0;
		//		for(int i=0;i<superficies.length;i++){
		//			Double s = superficies[i];
		//			if(superficieTotal>0&&s!=null&&s>0){
		//			double p = superficies[i]/this.superficieTotal;
		//			entropia-=p*Math.log(p)/Math.log(2);
		//			}
		//		}


		XYChart.Series<String, Number> series = new XYChart.Series<>();
		series.setName("Histograma");

		// Create a XYChart.Data object for each month. Add it to the series.
		TooltipUtil.setupCustomTooltipBehavior(50, 60000, 50);
		for (int j = 0; j <numClasses; j++) {
			//FIXME esto solo funciona despues de pedir una cosecha. en otro caso el clasifier se cambia y muestra datos equivocados

			String label =labor.getClasificador().getCategoryNameFor(j);//classifier.getTitle(j);
			//String rangoFin = classifier.getTitle(j+1);
			//System.out.println("rangoIni: "+rangoIni);//rangoIni: 1.0146282563539477..1.208709021558479		
			//System.out.println("rangoFin: "+rangoFin);//rangoFin: 1.208709021558479..1.2725564427424458


			//	String [] partesFin = rangoFin.split("\\.");


			//			String label = df.format(Double.parseDouble(partesIni[0]))+"~"+df.format(Double.parseDouble(partesIni[1]));// +"-"+histograma[j+1];


			Number superficie = superficies[j];
			if (superficie == null)		superficie =0.0;
			Number produccion = producciones[j];
			if (produccion == null)		produccion = 0.0;
			Data<String, Number> cData = new XYChart.Data<>(label, superficie);
			cData.setExtraValue(produccion);
			String color = getColorString(j);
		
			
			cData.nodeProperty().addListener(new ChangeListener<Node>() {
				@Override
				public void changed(ObservableValue<? extends Node> ov,	Node oldNode, Node newNode) {
					if (newNode != null) {
						newNode.setStyle("-fx-bar-fill: " + color + ";"); //$NON-NLS-1$ //$NON-NLS-2$
						NumberFormat df = Messages.getNumberFormat();
						
						double sup = cData.getYValue().doubleValue();//superficie
						double prod = (Double)cData.getExtraValue();
						Tooltip tooltip = new Tooltip(
								Messages.getString("CosechaHistoChart.10") +" "+df.format(prod/sup)
								//25has
								+"\n"+Messages.getString("CosechaHistoChart.21")+" "+df.format(sup)
								//66% lote
								+" "+df.format(sup/superficieTotal*100)+"%"); //$NON-NLS-1$ //$NON-NLS-2$
						tooltip.autoHideProperty().set(false);
						Tooltip.install(newNode,tooltip );		
						
						//Tooltip.install(newNode, new Tooltip(df.format(cData.getYValue())+Messages.getString("CosechaHistoChart.21"))); //$NON-NLS-1$
					}
				}
			});
			series.getData().add(cData);
		}

		return series;
	}


	private String getColorString(int absCat) {
		int length =colors.length-1;
		int clases = numClasses-1;//las clases van de cero a numclases -1 para un total de numclases
		int colorIndex = clases ==0?length:absCat*(length/clases);
		//System.out.println(absCat+"*"+length+"/"+clases+" = "+colorIndex+" colorIndex");
		return colors[colorIndex];
	}

}
