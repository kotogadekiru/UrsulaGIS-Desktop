package com.ursulagis.desktop.gui.candlestickchart;
/*
 Copyright 2014 Zoi Capital, LLC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * A candlestick chart is a style of bar-chart used primarily to describe price
 * movements of a security, derivative, or currency over time.
 *
 * The Data Y value is used for the opening price and then the close, high and
 * low values are stored in the Data's extra value property using a
 * CandleStickExtraValues object.
 * 
 * 
 */
public class CandleStickChart extends XYChart<Number, Number> {

  //  private static final double AVE_HEIGHT = 0.05;
	private static final String CANDLE_STICK_CHART_STYLES_CSS = "/gui/candlestickchart/CandleStickChartStyles.css";
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    protected static final Logger logger = Logger.getLogger(CandleStickChart.class.getName());
    protected int maxBarsToDisplay;
    protected ObservableList<XYChart.Series<Number, Number>> dataSeries;
    protected BarData lastBar;
    protected NumberAxis yAxis;
    protected NumberAxis xAxis;

    
    
    /**
     * 
     * @param title The chart title
     * @param bars  The bars data to display in the chart.
     */
    public CandleStickChart(String title, List<BarData> bars) {
        this(title, bars, Integer.MAX_VALUE);
     
    }

    
    /**
     * 
     * @param title The chart title
     * @param bars The bars to display in the chart
     * @param maxBarsToDisplay The maximum number of bars to display in the chart.
     */
    public CandleStickChart(String title, List<BarData> bars, int maxBarsToDisplay) {
        this(title, new NumberAxis(), new NumberAxis(), bars, maxBarsToDisplay);
    }

    /**
     * Construct a new CandleStickChart with the given axis.
     *
     * @param title The chart title
     * @param xAxis The x axis to use
     * @param yAxis The y axis to use
     * @param bars The bars to display on the chart
     * @param maxBarsToDisplay The maximum number of bars to display on the chart.
     */
    @SuppressWarnings("unchecked")
	public CandleStickChart(String title, NumberAxis xAxis, NumberAxis yAxis, List<BarData> bars, int maxBarsToDisplay) {
        super(xAxis, yAxis);
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.maxBarsToDisplay = maxBarsToDisplay;

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
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        List<BarData> sublist = getSubList(bars, maxBarsToDisplay);
        for (BarData bar : sublist) {
            series.getData().add(new XYChart.Data<>(bar.getElevacion(), bar.getAverage(), bar));
            logger.log(Level.INFO, "Adding bar with elevacion: {0}", bar.getElevacion());
            logger.log(Level.INFO, "Adding bar with rinde: {0}", bar.getAverage());
        }

        dataSeries = FXCollections.observableArrayList(series);

        setData(dataSeries);
        lastBar = sublist.get(sublist.size() - 1);
    }

    
    /**
     * Defines a formatter to use when formatting the y-axis values.
     * @param formatter The formatter to use when formatting the y-axis values.
     */
    public void setYAxisFormatter(DecimalAxisFormatter formatter) {
        yAxis.setTickLabelFormatter(formatter);
    }

    
//    /**
//     * Appends a new bar on to the end of the chart.
//     * @param bar The bar to append to the chart
//     */
//    public void addBar(BarData bar) {
//
//        if (dataSeries.get(0).getData().size() >= maxBarsToDisplay) {
//            dataSeries.get(0).getData().remove(0);
//        }
//
//        int datalength = dataSeries.get(0).getData().size();
//        dataSeries.get(0).getData().get(datalength - 1).setYValue(bar.getAverage());
//        dataSeries.get(0).getData().get(datalength - 1).setExtraValue(bar);
//      
//        logger.log(Level.INFO, "Adding bar with rinde:  {0}", bar.getElevacion());
//      //  logger.log(Level.INFO, "Adding bar with formated time: {0}", label);
//
//        lastBar = bar;//new BarData();
//        		//bar.getElevacion(), bar.getAverage(), bar.getAverage(), bar.getAverage(), bar.getAverage(), 0);
//        
//        Data<Number, Number> data = new XYChart.Data<>(lastBar.getElevacion(), lastBar.getAverage(), lastBar);
//        dataSeries.get(0).getData().add(data);  
//    }

    
    /**
     * Update the "Last" price of the most recent bar
     * @param price The Last price of the most recent bar.
     */
    public void updateLast(double price) {
        if (lastBar != null) {
            lastBar.update(price);
            logger.log(Level.INFO, "Updating last bar with rinde: {0}", lastBar.getElevacion());

            int datalength = dataSeries.get(0).getData().size();
            dataSeries.get(0).getData().get(datalength - 1).setYValue(lastBar.getAverage());

            dataSeries.get(0).getData().get(datalength - 1).setExtraValue(lastBar);
            logger.log(Level.INFO, "Updating last bar with formatteddate/time: {0}", dataSeries.get(0).getData().get(datalength - 1).getXValue());
        }
    }

    
    
    protected List<BarData> getSubList(List<BarData> bars, int maxBars) {
      //  List<BarData> sublist;
        if (bars.size() > maxBars) {
            return bars.subList(bars.size() - 1 - maxBars, bars.size() - 1);
        } else {
            return bars;
        }
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------
    /**
     * Called to update and layout the content for the plot
     */
    @Override
    protected void layoutPlotChildren() {
        // we have nothing to layout if no data is present
        if (getData() == null) {
            return;
        }
        // update candle positions
        for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++) {
            Series<Number, Number> series = getData().get(seriesIndex);
            Iterator<Data<Number, Number>> iter = getDisplayedDataIterator(series);
            Path seriesPath = null;
            if (series.getNode() instanceof Path) {
                seriesPath = (Path) series.getNode();
                seriesPath.getElements().clear();
            }
            while (iter.hasNext()) {
                Data<Number, Number> item = iter.next();
                double x = getXAxis().getDisplayPosition(getCurrentDisplayedXValue(item));
                double y = getYAxis().getDisplayPosition(getCurrentDisplayedYValue(item));
                Node itemNode = item.getNode();
                BarData bar = (BarData) item.getExtraValue();
                if (itemNode instanceof Candle && item.getYValue() != null) {
                    Candle candle = (Candle) itemNode;

                    //double close = getYAxis().getDisplayPosition(bar.getAverage())*(1-AVE_HEIGHT);
                    double high = getYAxis().getDisplayPosition(bar.getMax());
                    double low = getYAxis().getDisplayPosition(bar.getMin());
                    double candleWidth = Math.min(100, bar.getVolume().intValue()/ProyectionConstants.METROS2_POR_HA);
                   
                    candleWidth=Math.max(2, candleWidth);//aseguro que por lo menos tiene 5 de ancho
                    // update candle
                    double closeOfset = -10;//close - y;// es el alto de la barra que muestra el promedio
                    candle.color = CosechaHistoChart.colors[bar.getClase()];
                    candle.update(closeOfset, high-y, low - y, candleWidth);//esto da cero porque y y close, etc son iguales

                    // update tooltip content
                    candle.updateTooltip(bar.getElevacion(),
                    		bar.getVolume(),              
                    		bar.getAverage(),
                    		bar.getMax(),
                    		bar.getMin());

                    // position the candle
                    candle.setLayoutX(x);
                    candle.setLayoutY(y);
                }

            }
        }
    }

    @Override
    protected void dataItemChanged(Data<Number, Number> item) {
    }

    @Override
    protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
        Node candle = createCandle(getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            candle.setOpacity(0);
            getPlotChildren().add(candle);
            // fade in new candle
            FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
            ft.setToValue(1);
            ft.play();
        } else {
            getPlotChildren().add(candle);
        }
        // always draw average line on top
        if (series.getNode() != null) {
            series.getNode().toFront();
        }
    }

    @Override
    protected void dataItemRemoved(Data<Number, Number> item, Series<Number, Number> series) {
        final Node candle = item.getNode();
        if (shouldAnimate()) {
            // fade out old candle
            FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
            ft.setToValue(0);
            ft.setOnFinished((ActionEvent actionEvent) -> {
                getPlotChildren().remove(candle);
            });
            ft.play();
        } else {
            getPlotChildren().remove(candle);
        }
    }

    @Override
    protected void seriesAdded(Series<Number, Number> series, int seriesIndex) {
        // handle any data already in series
        for (int j = 0; j < series.getData().size(); j++) {
            Data<?,?> item = series.getData().get(j);
            Node candle = createCandle(seriesIndex, item, j);
            if (shouldAnimate()) {
                candle.setOpacity(0);
                getPlotChildren().add(candle);
                // fade in new candle
                FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(1);
                ft.play();
            } else {
                getPlotChildren().add(candle);
            }
        }
        // create series path
        Path seriesPath = new Path();
        seriesPath.getStyleClass().setAll("candlestick-average-line", "series" + seriesIndex);
        series.setNode(seriesPath);
        getPlotChildren().add(seriesPath);
    }

    @Override
    protected void seriesRemoved(Series<Number, Number> series) {
        // remove all candle nodes
        for (XYChart.Data<Number, Number> d : series.getData()) {
            final Node candle = d.getNode();
            if (shouldAnimate()) {
                // fade out old candle
                FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(0);
                ft.setOnFinished((ActionEvent actionEvent) -> {
                    getPlotChildren().remove(candle);
                });
                ft.play();
            } else {
                getPlotChildren().remove(candle);
            }
        }
    }

    /**
     * Create a new Candle node to represent a single data item
     *
     * @param seriesIndex The index of the series the data item is in
     * @param item The data item to create node for
     * @param itemIndex The index of the data item in the series
     * @return New candle node to represent the give data item
     */
    private Node createCandle(int seriesIndex, final Data<?,?> item, int itemIndex) {
        Node candle = item.getNode();
        // check if candle has already been created
        if (candle instanceof Candle) {
            ((Candle) candle).setSeriesAndDataStyleClasses("series" + seriesIndex, "data" + itemIndex);
        } else {
            candle = new Candle("series" + seriesIndex, "data" + itemIndex);
            item.setNode(candle);
        }
        return candle;
    }

    /**
     * This is called when the range has been invalidated and we need to update
     * it. If the axis are auto ranging then we compile a list of all data that
     * the given axis has to plot and call invalidateRange() on the axis passing
     * it that data.
     */
    @Override
    protected void updateAxisRange() {
        // For candle stick chart we need to override this method as we need to let the axis know that they need to be able
        // to cover the whole area occupied by the high to low range not just its center data value
        final Axis<Number> xa = getXAxis();
        final Axis<Number> ya = getYAxis();
        List<Number> xData = null;
        List<Number> yData = null;
        if (xa.isAutoRanging()) {
            xData = new ArrayList<>();
        }
        if (ya.isAutoRanging()) {
            yData = new ArrayList<>();
        }
        if (xData != null || yData != null) {
            for (Series<Number, Number> series : getData()) {
                for (Data<Number, Number> data : series.getData()) {
                    if (xData != null) {
                        xData.add(data.getXValue());
                    }
                    if (yData != null) {
                        BarData extras = (BarData) data.getExtraValue();
                        if (extras != null) {
                            yData.add(extras.getMax());
                            Number low = extras.getMin();
                            if(low.doubleValue()!=0){
                            	yData.add(low);
                            }
                        } else {
                            yData.add(data.getYValue());
                        }
                    }
                }
            }
            if (xData != null) {
                xa.invalidateRange(xData);
            }
            if (yData != null) {
                ya.invalidateRange(yData);
            }
        }
    }

    /**
     * Candle node used for drawing a candle
     */
    private class Candle extends Group {

        private final Line highLowLine = new Line();
        private final Region bar = new Region();
        private String seriesStyleClass;
        private String dataStyleClass;
        private boolean openAboveClose = true;
        private final Tooltip tooltip = new Tooltip();
        String color = "blue";

        private Candle(String seriesStyleClass, String dataStyleClass) {
            setAutoSizeChildren(false);
            getChildren().addAll(highLowLine, bar);
            this.seriesStyleClass = seriesStyleClass;
            this.dataStyleClass = dataStyleClass;
            updateStyleClasses();
            tooltip.setGraphic(new TooltipContent());
         
            Tooltip.install(bar, tooltip);
          //  Tooltip.install(highLowLine, tooltip);
        }

        public void setSeriesAndDataStyleClasses(String seriesStyleClass, String dataStyleClass) {
            this.seriesStyleClass = seriesStyleClass;
            this.dataStyleClass = dataStyleClass;
            updateStyleClasses();
        }
/**
 * 
 * @param closeOffset altura de la vela
 * @param highOffset valor maximo del bigote
 * @param lowOffset valor minimo del bigote
 * @param candleWidth ancho de la vela
 */
        public void update(double closeOffset, double highOffset, double lowOffset, double candleWidth) {
            openAboveClose = closeOffset > 0;
            updateStyleClasses();
            highLowLine.setStartY(highOffset);
            highLowLine.setEndY(lowOffset);
            if (candleWidth == -1) {
                candleWidth = bar.prefWidth(-1);
            }
            if (openAboveClose) {
                bar.resizeRelocate(-candleWidth / 2, 0, candleWidth, closeOffset);
            } else {//close above open, es positivo
                bar.resizeRelocate(-candleWidth / 2, closeOffset, candleWidth, closeOffset * -1);
            }
        }

        public void updateTooltip(Number open,Number sup, Number average, Number max, Number min) {
            TooltipContent tooltipContent = (TooltipContent) tooltip.getGraphic();
            tooltipContent.update(open.doubleValue(),sup.doubleValue(), average.doubleValue(), max.doubleValue(), min.doubleValue());
        }

        private void updateStyleClasses() {
            getStyleClass().setAll("candlestick-candle", seriesStyleClass, dataStyleClass);
            
            highLowLine.getStyleClass().setAll("candlestick-line", seriesStyleClass, dataStyleClass,
                    openAboveClose ? "open-above-close" : "close-above-open");
            
//            bar.getStyleClass().setAll("candlestick-bar", seriesStyleClass, dataStyleClass,
//                    openAboveClose ? "open-above-close" : "close-above-open");
          
			
			bar.setStyle("  -fx-background-color:  " + color + ";"
		    +"-fx-border-width: 1, 1,1,1;"
		    +"-fx-border-color: #e2e2e2;");
        }
    }

    private class TooltipContent extends GridPane {

        private final Label elevValue = new Label();
        private final Label supValue = new Label();
        private final Label maxValue = new Label();
        private final Label minValue = new Label();
        private final Label aveValue = new Label();

        private TooltipContent() {
        	//TODO traducir mensajes en CandleStickChart
            Label elev = new Label("Elevacion:");
            Label sup = new Label("Sup (Ha):");
            Label max = new Label("Max Rinde:");
            Label min = new Label("Min Rinde:");
            Label ave = new Label("Rinde:");
            elev.getStyleClass().add("candlestick-tooltip-label");
            elevValue.getStyleClass().add("candlestick-tooltip-label");
            sup.getStyleClass().add("candlestick-tooltip-label");
            supValue.getStyleClass().add("candlestick-tooltip-label");
            max.getStyleClass().add("candlestick-tooltip-label");
            maxValue.getStyleClass().add("candlestick-tooltip-label");
            min.getStyleClass().add("candlestick-tooltip-label");
            minValue.getStyleClass().add("candlestick-tooltip-label");
            ave.getStyleClass().add("candlestick-tooltip-label");
            aveValue.getStyleClass().add("candlestick-tooltip-label");
            
            int row =0;
            setConstraints(ave, 0, row);
            setConstraints(aveValue, 1,row);
            row++;
            setConstraints(elev, 0, row);
            setConstraints(elevValue, 1, row);
            row++;
            setConstraints(max, 0, row);
            setConstraints(maxValue, 1, row);
            row++;
            setConstraints(min, 0, row);
            setConstraints(minValue, 1, row);
            row++;
            setConstraints(sup, 0, row);
            setConstraints(supValue, 1, row);
        
           
            
           
            getChildren().addAll(elev, elevValue, 
            		sup, supValue,
            		ave, aveValue,
            		max, maxValue, min, minValue);
        }

        public void update(Number elev, Number sup,Number average, Number max, Number min) {
    		NumberFormat df=Messages.getNumberFormat();    
            elevValue.setText(df.format(elev));//.toString());
            supValue.setText(df.format(sup.doubleValue()/ProyectionConstants.METROS2_POR_HA));//close.toString());
            maxValue.setText(df.format(max));//high.toString());
            aveValue.setText(df.format(average));//low.toString());
            minValue.setText(df.format(min));//low.toString());
        }
    }

    protected static CandleStickChart chart;

}
