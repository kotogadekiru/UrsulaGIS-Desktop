package com.ursulagis.desktop.gui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.gui.candlestickchart.BarData;
import com.ursulagis.desktop.gui.candlestickchart.CandleStickChart;
import com.ursulagis.desktop.gui.utils.TooltipUtil;
import com.ursulagis.desktop.dao.Labor;

import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * grafico que muestra en un candle stick chart la relacion entre el rinde y la altura
 * @author tomas
 *
 */
public class AmountVsElevacionChart extends VBox {
	private static final double TOLERANCIA = 0.01;
	//	private static final double OPEN = 0.9;
	//	private static final double CLOSE = 1.1;
	private static final String ICON = "gisUI/1-512.png"; //$NON-NLS-1$
	//	private String[] colors = {
	//			"rgb(158,1,66)",
	//			"rgb(213,62,79)",
	//			" rgb(244,109,67)", 
	//			" rgb(253,174,97)",
	//			" rgb(254,224,139)",
	//			" rgb(255,255,191)",
	//			" rgb(230,245,152)",
	//			" rgb(171,221,164)",
	//			"rgb(102,194,165)",
	//			"rgb(50,136,189)",// "BLUE"};
	//	"DARKBLUE" };

	// Color.rgb(94,79,162)};

	//Double[] histograma = null;//Double.parseDouble[colors.length];
	//	Double[] promedios = Double.parseDouble[colors.length];
	Double superficieTotal= 0.0;
	Double produccionTotal= 0.0;
	private int grupos = 20;

	public AmountVsElevacionChart(Labor<?> labor,int grupos) {
		super();
		TooltipUtil.setupCustomTooltipBehavior(50,100000,50);
		this.grupos=grupos;
		CandleStickChart chart = new CandleStickChart(Messages.getString("AmountVsElevacionChart.titulo"), createSeries(labor)); //$NON-NLS-1$
		//chart.setAxisSortingPolicy(LineChart.SortingPolicy.X_AXIS);

		//		@SuppressWarnings("unchecked")
		//		XYChart.Series<Number, Number> series = createSeries(labor);
		chart.setTitle(labor.getNombre());
		chart.legendVisibleProperty().setValue(false);
		VBox.getVgrow(chart);
		this.getChildren().add(chart);
		chart.prefHeightProperty().bind(this.heightProperty());

	}


	private List<BarData> createSeries(Labor<?> labor) {			
		//	CosechaLabor cL = (CosechaLabor) labor;
		// List<BarData> bars = new ArrayList<>(); 
		Map<Double,BarData> bars=new HashMap<>(); 
		//		for(int i=0;i<colors.length;i++){
		//			bars.add(i, new BarData(0, 0, 0, 0, 0, 0));
		//		}

		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			Double rinde =LaborItem.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));//cuando leo de outCollection amount siempre es Rinde
			//CosechaLabor.COLUMNA_ELEVACION
			Double elevacion=LaborItem.getDoubleFromObj(f.getAttribute(Labor.COLUMNA_ELEVACION));

			Geometry g = ((Geometry)f.getDefaultGeometry());
			double dSup = g.getArea();
			long sup =(long)(dSup*ProyectionConstants.A_HAS()*ProyectionConstants.METROS2_POR_HA); 

			//int categoria = labor.getClasificador().getCategoryFor(x);
			//buscar una barra para la que x este dentro del 10%
			BarData bar = null;

			//averageMin<x<averageMax
			for(Double elevKey : bars.keySet()){
				Double cvElev = Math.abs((elevacion-elevKey)/elevKey);
				if(bar!=null)break;
				if(cvElev<TOLERANCIA ){
					//System.out.println("rank es "+rank);
					bar = bars.get(elevKey);
					double rinde2 = bar.getAverage().doubleValue();
					Double cvRinde = Math.abs((rinde2-rinde)/rinde);
					if(cvRinde>TOLERANCIA)bar = null;
				}
			}

			//	BarData bar = bars.get(new Integer(categoria));
			if(bar == null){
				bar = new BarData(0, 0, 0, 0, 0, 0);
				bar.setElevacion(elevacion);
				bar.setAverage(rinde);
				//bar.setClose(rinde*CLOSE);
				bar.setMax(rinde);
				bar.setMin( rinde);
				bar.setVolume(sup);
			} else{
				bars.remove(bar.getElevacion());//elevKey

				//New average = old average * (n-1)/n + new value /n	
				double supAnt = bar.getVolume().doubleValue();
				double elevAnterior = bar.getElevacion().doubleValue();
				double rindeAnt = bar.getAverage().doubleValue();
				double supDesp = supAnt+sup;
				//double rindeProm = bar.getRinde().doubleValue()*(N-1)/N+x/N;
				double elevProm = (elevAnterior*supAnt+elevacion*sup)/supDesp;

				double rindeProm = (rindeAnt*supAnt+rinde*sup)/supDesp;

				bar.setElevacion(elevProm);
				bar.setAverage(rindeProm);//Math.min(bar.getOpen().doubleValue(), y));
				//bar.setClose(rinde*CLOSE);//Math.max(bar.getClose().doubleValue(), y));
				bar.setMax(Math.max(bar.getMax().doubleValue(), rinde));
				bar.setMin(Math.min(bar.getMin().doubleValue(), rinde));			
				bar.setVolume(supDesp);

				//1 dato fuera de lugar me altera todo el grafico
			}
			//new Integer(categoria)
			bars.put(bar.getElevacion().doubleValue(), bar);

		}
		it.close();

		// recombinar los bars que quedaron mas cerca que la tolerancia despues de todo
		List<BarData> resumidas = recombinar(bars);
		for(BarData resumida :resumidas){
			int index = labor.getClasificador().getCategoryFor(resumida.getAverage().doubleValue());
			
//				int length =CosechaHistoChart.colors.length-1;
//				int clases = resumidas.size()-1;//las clases van de cero a numclases -1 para un total de numclases
				int colorIndex = index;//*(length/clases);
				//System.out.println(absCat+"*"+length+"/"+clases+" = "+colorIndex+" colorIndex");
		
			resumida.setClase(colorIndex);
		}
		return resumidas;

	}


	private List<BarData> recombinar(Map<Double, BarData> bars) {
		List<BarData> ret = new ArrayList<BarData>(bars.values());
		
		// usar stream y flatmap o algo para recorrer la lista buscando los duplicados
		//XXX no estoy seguro porque pero si no filtro los datos no los puedo ordenar
		ret =ret.stream().filter((b)->{
			try{new BigDecimal(b.getElevacion().toString());
			}catch(Exception e){return false;}					
			return true;
		}).collect(Collectors.toList());
		
		ret.sort((b1,b2)->b1.compareTo(b2));

//		ret.forEach((b)->System.out.println(b));
		

		boolean huboMatch=false;
		int iTol=1;

		do{

			huboMatch=false;

			for(int index=0;(index<ret.size()-1)&&(ret.size()>2);index++){	
				BarData test = ret.get(index);
				BarData test2 = ret.get(index+1);


				double elev1 = test.getElevacion().doubleValue();
				double elev2 = test2.getElevacion().doubleValue();

				double rinde1 = test.getAverage().doubleValue();
				double rinde2 = test2.getAverage().doubleValue();

				double vol1 = test.getVolume().doubleValue();
				double vol2 = test2.getVolume().doubleValue();

				double supDesp = vol1+vol2;

				Double cvElev = Math.abs((elev2-elev1)/elev1);
				Double cvRinde = Math.abs((rinde2-rinde1)/rinde1);
				double cvTol = TOLERANCIA*iTol;
				huboMatch = (cvElev<cvTol);//&&(cvRinde<cvTol);
				if(huboMatch){
					//	System.out.println(index +" y "+(index+1)+ " hubo match r1="+elev1+" r2="+elev2);

					double elevProm = (elev1*vol1+elev2*vol2)/supDesp;
					double rindeProm = (rinde1*vol1+rinde2*vol2)/supDesp;
					BarData sintesis = new BarData(elevProm,
							Math.max(test.getMax().doubleValue(), test2.getMax().doubleValue()),
							Math.min(test.getMin().doubleValue(), test2.getMin().doubleValue()),
							rindeProm, 
							supDesp);

					ret.remove(test);
					ret.remove(test2);
					ret.add(index, sintesis);
					//System.out.println("sintesis = "+sintesis);
				}
			}
			iTol++;
		}while(//huboMatch&&
				ret.size()>=grupos);//huboMatch);
		return ret;
	}

}
