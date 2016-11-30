package mmg.gui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import mmg.gui.candlestickchart.BarData;
import mmg.gui.candlestickchart.CandleStickChart;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import utils.ProyectionConstants;

import com.vividsolutions.jts.geom.Geometry;

import dao.FeatureContainer;
import dao.Labor;
import dao.cosecha.CosechaLabor;

/**
 * grafico que muestra en un candle stick chart la relacion entre el rinde y la altura
 * @author tomas
 *
 */
public class AmountVsElevacionChart extends VBox {
	private static final String ICON = "gisUI/1-512.png";
	private String[] colors = {
			"rgb(158,1,66)",
			"rgb(213,62,79)",
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

	//Double[] histograma = null;//new Double[colors.length];
	//	Double[] promedios = new Double[colors.length];
	Double superficieTotal= new Double(0),produccionTotal= new Double(0);

	public AmountVsElevacionChart(Labor<?> labor) {
		super();
		final NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel("Rinde");
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("Altura");
		yAxis.autoRangingProperty().set(true);
		yAxis.forceZeroInRangeProperty().setValue(false);
		//final LineChart<Number, Number> chart = new LineChart<Number, Number>(xAxis, yAxis);
		CandleStickChart chart = new CandleStickChart("Correlacion Rinde Vs Altura", createSeries(labor));
		//chart.setAxisSortingPolicy(LineChart.SortingPolicy.X_AXIS);

		//		@SuppressWarnings("unchecked")
		//		XYChart.Series<Number, Number> series = createSeries(labor);
		chart.setTitle(labor.getNombreProperty().get());
		chart.legendVisibleProperty().setValue(false);
		//	chart.getData().add(series);
		VBox.getVgrow(chart);
		this.getChildren().add(chart);
		//	DecimalFormat df = new DecimalFormat("#.00");

	}


	private List<BarData> createSeries(Labor<?> labor) {			
	//	CosechaLabor cL = (CosechaLabor) labor;
		// List<BarData> bars = new ArrayList<>(); 
		 Map<Integer,BarData> bars=new HashMap<>(); 
//		for(int i=0;i<colors.length;i++){
//			bars.add(i, new BarData(0, 0, 0, 0, 0, 0));
//		}

		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			Double rinde =FeatureContainer.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));

			Double altura =FeatureContainer.getDoubleFromObj(f.getAttribute(CosechaLabor.COLUMNA_ELEVACION));

			int categoria = labor.getClasificador().getCategoryFor(rinde);
			BarData bar = bars.get(new Integer(categoria));
			if(bar ==null){
				bar = new BarData(0, 0, 0, 0, 0, 0);
				bar.setRinde(rinde);
				bar.setOpen(altura);
				bar.setClose(altura);
				bar.setHigh(altura);
				bar.setLow( altura);
				bar.setVolume(1);
			} else{
//			int N = bar.getVolume().intValue();
//			if(N==0){
//				bar.setRinde(rinde);
//				bar.setOpen(altura);
//				bar.setClose(altura);
//				bar.setHigh(altura);
//				bar.setLow( altura);
//
//				//		        double open = getNewValue(previousClose);
//				//	            double close = getNewValue(open);
//				//	            double high = Math.max(open + getRandom(),close);
//				//	            double low = Math.min(open - getRandom(),close);
//				//	            
//
//				bar.setVolume(1);
//
//
//
//
//			}else{
				//New average = old average * (n-1)/n + new value /n	
				int N = bar.getVolume().intValue();
				double rindeProm = bar.getRinde().doubleValue()*(N-1)/N+rinde/N;

				bar.setRinde(rindeProm);
				bar.setOpen(Math.min(bar.getOpen().doubleValue(), altura));
				bar.setClose(Math.max(bar.getClose().doubleValue(), altura));
				bar.setHigh(Math.max(bar.getHigh().doubleValue(), altura));
				bar.setLow(Math.min(bar.getLow().doubleValue(), altura));			
				bar.setVolume(N+1);
				//FIXME me muestra el minimo y el maximo pero no me muestra los promedios que son lo que importa. 
				//1 dato fuera de lugar me altera todo el grafico
			}
			
			bars.put(new Integer(categoria), bar);

		}
		it.close();

		ArrayList<BarData> ret = new ArrayList<BarData>();
		ret.addAll(bars.values());
		return ret;

	}

}
