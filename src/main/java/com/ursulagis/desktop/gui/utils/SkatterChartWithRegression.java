package com.ursulagis.desktop.gui.utils;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;

import com.ursulagis.desktop.gui.CosechaHistoChart;
import com.ursulagis.desktop.gui.Messages;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import com.ursulagis.desktop.utils.LinearRegression;

public class SkatterChartWithRegression  extends XYChart<Number, Number> {

	public static SkatterChartWithRegression construct() {
		NumberAxis xAxis=new NumberAxis();
		NumberAxis yAxis=new NumberAxis();
		//		xAxis.setLabel(columnX);                
		//		yAxis.setLabel(columnY);	
		SkatterChartWithRegression chart = new SkatterChartWithRegression(xAxis,yAxis);
		ObservableList<XYChart.Series<Number, Number>> data = FXCollections.observableArrayList();
		chart.setData(data);
		return chart;
	}

	private HashMap<Series<Number, Number>, String> ecuationMap = new HashMap<Series<Number,Number>,String>();

	public SkatterChartWithRegression(NumberAxis xAxis,NumberAxis yAxis) {
		super(xAxis, yAxis);
		//super.getXAxis();


		//     yAxis.autoRangingProperty().set(true);
		yAxis.forceZeroInRangeProperty().setValue(Boolean.FALSE);
		yAxis.setForceZeroInRange(false);
		xAxis.setForceZeroInRange(false);

		//   xAxis.autoRangingProperty().set(true);
		xAxis.forceZeroInRangeProperty().setValue(Boolean.FALSE);
		//setTitle(title);
		setAnimated(true);
		//getStylesheets().add(getClass().getResource(CANDLE_STICK_CHART_STYLES_CSS).toExternalForm());
		xAxis.setAnimated(true);
		yAxis.setAnimated(true);
		verticalGridLinesVisibleProperty().set(false);
	}

	/**
	 * Create a new SkatterNode node to represent a single data item
	 *
	 * @param seriesIndex The index of the series the data item is in
	 * @param item The data item to create node for
	 * @param itemIndex The index of the data item in the series
	 * @return New candle node to represent the give data item
	 */
	private Node createSkatterNode(int seriesIndex, final Data<?,?> item, int itemIndex) {
		Node node = item.getNode();
		// check if candle has already been created
		if (node instanceof SkatterNode) {
			((SkatterNode) node).setSeriesAndDataStyleClasses("series" + seriesIndex, "data" + itemIndex);
		} else {
			SkatterNode skatterNode = new SkatterNode("series" + seriesIndex, "data" + itemIndex);
			skatterNode.updateTooltip((Number)item.getXValue(),(Number)item.getYValue());
			node = skatterNode;
			item.setNode(node);
			
		}
		
		return node;
	}

	@Override
	protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
		Node node = createSkatterNode(getData().indexOf(series), item, itemIndex);
		if (shouldAnimate()) {
			node.setOpacity(0);
			getPlotChildren().add(node);
			// fade in new candle
			FadeTransition ft = new FadeTransition(Duration.millis(500), node);
			ft.setToValue(1);
			ft.play();
		} else {
			getPlotChildren().add(node);
		}
		// always draw average line on top
		if (series.getNode() != null) {
			series.getNode().toFront();
		}
	}

	@Override
	protected void dataItemRemoved(Data<Number, Number> item, Series<Number, Number> series) {
		final Node itemNode = item.getNode();
		if (shouldAnimate()) {
			// fade out old candle
			FadeTransition ft = new FadeTransition(Duration.millis(500), itemNode);
			ft.setToValue(0);
			ft.setOnFinished((ActionEvent actionEvent) -> {
				getPlotChildren().remove(itemNode);
			});
			ft.play();
		} else {
			getPlotChildren().remove(itemNode);
		}

	}

	@Override
	protected void dataItemChanged(Data<Number, Number> item) {		
	}

	/**
	 * @seriesIndex The index of the series the data item is in
	 * @item The data item to create node for
	 * @itemIndex The index of the data item in the series
	 */
	@Override
	protected void seriesAdded(Series<Number, Number> series, int seriesIndex) {
		double[] xs = new double[series.getData().size()];
		double[] ys = new double[series.getData().size()];
		double minX=Double.MIN_VALUE;
		double maxX=Double.MAX_VALUE;

		for (int j = 0; j < series.getData().size(); j++) {
			Data<Number, Number> item = series.getData().get(j);
			double xValue =item.getXValue().doubleValue();
			minX=Math.max(minX, xValue);
			maxX=Math.min(maxX, xValue);
			xs[j]=xValue;
			ys[j]=item.getYValue().doubleValue();

			Node node = createSkatterNode(seriesIndex, item, j);
			if (shouldAnimate()) {
				node.setOpacity(0);
				getPlotChildren().add(node);
				// fade in new node
				FadeTransition ft = new FadeTransition(Duration.millis(500), node);
				ft.setToValue(1);
				ft.play();
			} else {
				getPlotChildren().add(node);
			}
		} 
		TrendLine tl = new TrendLine(xs, ys);
	    ecuationMap.put(series,tl.tooltip.getText());
		series.setNode(tl);
		getPlotChildren().add(tl);

	}

	@Override
	protected void seriesRemoved(Series<Number, Number> series) {	}

	@Override
	protected void layoutPlotChildren() {
		// we have nothing to layout if no data is present
		if (getData() == null) {
			return;
		}
		// update mark positions
		for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++) {
			Series<Number, Number> series = getData().get(seriesIndex);			
			
			if (series.getNode() instanceof TrendLine) {
				TrendLine trendline = (TrendLine) series.getNode();
				trendline.update(getXAxis(),getYAxis());
			}
			
			Iterator<Data<Number, Number>> iter = getDisplayedDataIterator(series);
			while (iter.hasNext()) {
				Data<Number, Number> item = iter.next();
				double x = getXAxis().getDisplayPosition(getCurrentDisplayedXValue(item));
				double y = getYAxis().getDisplayPosition(getCurrentDisplayedYValue(item));
				Node itemNode = item.getNode();

				if (itemNode instanceof SkatterNode && item.getYValue() != null) {
					SkatterNode node = (SkatterNode) itemNode;                

					node.color = CosechaHistoChart.colors[Math.min(seriesIndex, CosechaHistoChart.colors.length-1)];
					node.update(x,y);

					// update tooltip content
					//node.updateTooltip(item.getXValue(),item.getYValue());

					// position the node
//					node.setLayoutX(x);
//					node.setLayoutY(y);
				}
			}	
		}		
	}

	private class TrendLine extends Group{
		private final Tooltip tooltip = new Tooltip();
		private LinearRegression lr = null;
		Double minX=Double.MIN_VALUE;
		Double maxX=Double.MAX_VALUE;
		private Line line=new Line(0,0,0,0);

		public TrendLine(double[] xs,double[] ys) {
			lr = new LinearRegression(xs, ys);
			for(double xValue:xs) {//find min max
				minX=Math.max(minX, xValue);
				maxX=Math.min(maxX, xValue);
			}
			//line.setFill(Color.RED);
			line.setStroke(Color.BLACK);
			line.setStrokeWidth(3);  
			line.setOpacity(0.9);		
			String a = Messages.getNumberFormat().format(lr.slope());
			String b = Messages.getNumberFormat().format(lr.predict(0));
			String r2 =  Messages.getNumberFormat().format(lr.R2());
			if(!b.startsWith("-")) {
				b=" + "+b;
			}else {
				b=" - "+b.substring(1);
			}
			
			tooltip.setText("f(x) = "+a+" * x "+b+"  R2 = "+r2);

			Tooltip.install(line, tooltip);
			this.getChildren().add(line);
		}

		public void update(Axis<Number> xa,Axis<Number> ya) {		
			line.setStartX(xa.getDisplayPosition(minX));
			line.setStartY(ya.getDisplayPosition(lr.predict(minX)));
			line.setEndX(xa.getDisplayPosition(maxX));
			line.setEndY(ya.getDisplayPosition(lr.predict(maxX)));		
		}
	}
	
	/**
	 * Candle node used for drawing a candle
	 */
	private class SkatterNode extends Group {
		private final Region mark = new Region();
		private final Tooltip tooltip = new Tooltip();
		String color = "blue";

		private SkatterNode(String seriesStyleClass, String dataStyleClass) {
			setAutoSizeChildren(false);
			getChildren().addAll(mark);
			//            this.seriesStyleClass = seriesStyleClass;
			//            this.dataStyleClass = dataStyleClass;
			updateStyleClasses();
			//  tooltip.setGraphic(new XYTooltipContent());


			Tooltip.install(mark, tooltip);
		}

		public void setSeriesAndDataStyleClasses(String seriesStyleClass, String dataStyleClass) {
			//            this.seriesStyleClass = seriesStyleClass;
			//            this.dataStyleClass = dataStyleClass;
			updateStyleClasses();
		}

		public void update(double x, double y) {
			//            openAboveClose = closeOffset > 0;
			updateStyleClasses();

			int width = 7;
			/*
			 * x the target x coordinate location
			 * y the target y coordinate location
			 * width the target layout bounds width
			 * height the target layout bounds height
			 */
			mark.resizeRelocate(x-width / 2, y-width / 2, width, width);

		}

		public void updateTooltip( Number x, Number y) {
			NumberFormat df=Messages.getNumberFormat();    

			tooltip.setText("("+df.format(x)+"; "+df.format(y)+")");

			//            XYTooltipContent tooltipContent = (XYTooltipContent) tooltip.getGraphic();
			//            tooltipContent.update( x, y);
		}

		private void updateStyleClasses() {
			// getStyleClass().setAll("candlestick-candle", seriesStyleClass, dataStyleClass);

			mark.setStyle("  -fx-background-color:  " + color + ";");
			//		    +"-fx-border-width: 1, 1,1,1;"
			//		    +"-fx-border-color: #e2e2e2;");
		}
	}

	public String getEcuation(Series<Number, Number> series) {
		
		return ecuationMap.get(series);
		
	}


}
