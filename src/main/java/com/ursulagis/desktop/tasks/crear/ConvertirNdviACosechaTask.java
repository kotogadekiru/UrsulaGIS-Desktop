package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.ShowNDVITifFileTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.LinearRegression;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ConvertirNdviACosechaTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	private static  double NDVI_RINDE_CERO = ShowNDVITifFileTask.MIN_VALUE;//0.2;
	Double rindeProm = 0.0;
	Ndvi ndvi=null;

	public ConvertirNdviACosechaTask(CosechaLabor cosechaLabor,Ndvi _ndvi,Double _rinde){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		rindeProm=_rinde;
		ndvi=_ndvi;
		try {
			NDVI_RINDE_CERO=
					cosechaLabor
					.getCultivo()
					.getNdviRindeCero();
		}catch(Exception e) {
			e.printStackTrace();
			NDVI_RINDE_CERO=ShowNDVITifFileTask.MIN_VALUE;
		}
		
	}

	public void doProcess() throws IOException {
	//	labor.setContorno(ndvi.getContorno());
		Iterable<? extends GridPointAttributes> values = ndvi.getSurfaceLayer().getValues();
		Iterator<? extends GridPointAttributes> it = values.iterator();
		
		double sum =0;
		int size =0;
		while(it.hasNext()){
			GridPointAttributes gpa = it.next();
			Double value = Double.valueOf(gpa.getValue());
			//OJO cuando se trabaja con el jar la version de world wind que esta levantando no es la que usa eclipse.
			// es por eso que el jar permite valores Nan y en eclipse no.
			if(value < NDVI_RINDE_CERO || value > ShowNDVITifFileTask.MAX_VALUE || value == 0 || value.isNaN()){
					continue;
			} else{
			//System.out.println("calculando el promedio para el valor "+value+" suma parcial "+sum+" size "+size);
			sum+=value;
			size++;
			}
		}
		if(size == 0){
			System.err.println("no hay puntos iterables para obtener el promedio");
			return;
		}
		this.featureCount=size;
		Double averageNdvi = Double.valueOf(sum/size);
		System.out.println("el promedio de los ndvi es "+averageNdvi);
		// si el rinde promedio es >0.5 => NDVI_RINDE_CERO es 0.3
		//si el rinde promedio es <0.5 => NDVI_RINDE_CERO es 0.1
		//chequear el minimo ndvi segun el cultivo? para el trigo inicial en macollo el minimo de 0.5 es malo
//		Cultivo cult =super.labor.getCultivo();
//		if(cult.isEstival()) {
//			//ver si la imagen es del final del ciclo o del principio?
//			NDVI_RINDE_CERO= averageNdvi>0.5?0.2:0.1;//depende del cultivo?
//		}
		if(averageNdvi.isNaN()) averageNdvi = 1.0;
		 it = values.iterator();
		 
	 
		 
		ExportableAnalyticSurface exportableSurface = ndvi.getSurfaceLayer();//.getSurfaceAttributes();
		Sector sector = exportableSurface.getSector();
		int[] dimensions = exportableSurface.getDimensions();//raster.getWidth(), raster.getHeight()
		
		final int width = dimensions[0];
		final int height = dimensions[1];
		
		GeometryFactory fact = new GeometryFactory();
		
		double latStep = -sector.getDeltaLatDegrees() / (double) (height);//-1
		double lonStep = sector.getDeltaLonDegrees() / (double) (width);
		
		
		List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();
		
		double elev = 10.0;
		double minLat = sector.getMaxLatitude().degrees;
		double minLon = sector.getMinLongitude().degrees;
		
		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
				labor.getType());
		
		double id=0;
		
		Configuracion config = Configuracion.getInstance();
		String useSigmoidString = config.getPropertyOrDefault(this.getClass().getName()+".USE_SIGMOID", "false");
		System.out.println(this.getClass().getName()+".USE_SIGMOID="+useSigmoidString);
		Function<Double, Double> calcRinde = getRineForNDVILinealFunction(averageNdvi);
		
		if("true".equals(useSigmoidString)) {
			System.out.println("usando interpolacion sigmoidea");
			calcRinde = getRindeForNDVISigmoidFunction(averageNdvi);
		}
		//Function<Double, Double> calcRinde = getRineForNDVILinealFunction(averageNdvi);
		
		//Function<Double, Double> calcRinde = getRindeForNDVISmoothstepFunction(averageNdvi);
		
		
		//logaritmica
//		double pendienteNdviRinde=averageNdvi>NDVI_RINDE_CERO?Math.log(rindeProm)/(Math.log(averageNdvi)-Math.log(NDVI_RINDE_CERO)):1;
//		double origenNdviRinde = -pendienteNdviRinde*Math.log(NDVI_RINDE_CERO);
//		System.out.println("r="+pendienteNdviRinde+"* Math.log(_ndvi+0.8)"+origenNdviRinde);
//		Function<Double,Double> calcRinde = (_ndvi)->pendienteNdviRinde*Math.log(_ndvi+0.8)+origenNdviRinde;
		
		
		Poligono contorno = this.ndvi.getContorno();
		Geometry contornoGeom =null;
		if(contorno !=null) {
			contornoGeom = contorno.toGeometry();
		}
		
		for (int y = 0; y < height; y++){
			double lat = minLat + y * latStep;
			for (int x = 0; x < width; x++)	{
				double lon = minLon+x*lonStep;
				
				GridPointAttributes attr = it.hasNext() ? it.next() : null;
				double ndvi = attr.getValue();
				if((ndvi<=NDVI_RINDE_CERO)){//&&(x<3 || y<3||x>width-3||y>height-3)){
					continue;//me salteo la primera fila de cada costado
				}
				
				
				if( ndvi <= ShowNDVITifFileTask.MAX_VALUE && ndvi >= NDVI_RINDE_CERO && ndvi !=0){
					CosechaItem ci = new CosechaItem();
					ci.setId(id);
					ci.setElevacion(elev);
					ProyectionConstants.setLatitudCalculo(lat);
					double ancho =lonStep / ProyectionConstants.metersToLong();
					double distancia  = latStep /ProyectionConstants.metersToLat();
					ci.setAncho(ancho);
					ci.setDistancia(distancia);
					
					//Double rindeNDVI = Double.parseDouble(pendienteNdviRinde*ndvi+origenNdviRinde);//aproximacion lineal
					Double rindeNDVI = Double.valueOf(calcRinde.apply(ndvi));//aproximacion logaritmica
					//System.out.println("creado nueva cosecha con rinde "+rindeNDVI+" para el ndvi "+value+" rinde prom "+rinde+" ndvi promedio "+average);
					ci.setRindeTnHa(rindeNDVI);
					labor.setPropiedadesLabor(ci);
					
					Coordinate[] coordinates = new Coordinate[5];
					coordinates[0]= new Coordinate(lon,lat,elev);
					coordinates[1]= new Coordinate(lon+lonStep,lat,elev);
					coordinates[2]= new Coordinate(lon+lonStep,lat+latStep,elev);
					coordinates[3]= new Coordinate(lon,lat+latStep,elev);
					coordinates[4]= new Coordinate(lon,lat,elev);//coordinates[0];
					
					Geometry poly = fact.createPolygon(coordinates);	
				
					// recortar si esta afuera del contorno del ndvi
					if(contornoGeom!=null && !contornoGeom.covers(poly)) {//OK! funciona. no introducir poligonos empty!
						try {
						poly= GeometryHelper.getIntersection(poly, contornoGeom);//poly.intersection(contornoGeom);
						if(poly==null || poly.isEmpty())continue;
						//System.out.println("el contorno no cubre el polygono y la interseccion es: "+poly.toText());
						}catch(Exception e) {//org.locationtech.jts.geom.TopologyException: Found null DirectedEdge
							e.printStackTrace();//org.locationtech.jts.geom.TopologyException: side location conflict [ (-61.920393510481325, -33.66456750394795, NaN) ]
						}
					}
					ci.setGeometry(poly);
					
					SimpleFeature f = ci.getFeature(fBuilder);
					boolean res = features.add(f);
					if(!res){
						System.out.println("no se pudo agregar la feature "+ci);
					}
				//	labor.insertFeature(ci);
					itemsToShow.add(ci);
					id++;
					updateProgress(id, featureCount);

			}
				
				
			//	lon += lonStep;
			}
		//lat += latStep;
		}
		
	
		if(labor.inCollection == null){
			labor.inCollection = new DefaultFeatureCollection("internal",labor.getType());
		}
		labor.inCollection.addAll(features);
		boolean ret= labor.outCollection.addAll(features);
		if(!ret){//XXX si esto falla es provablemente porque se estan creando mas de una feature con el mismo id
			System.out.println("no se pudieron agregar las features al outCollection");
		}

				
		labor.constructClasificador();	
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}

	
	private Function<Double, Double> getRineForNDVILinealFunction(Double averageNdvi) {
		double[] xs= {0,averageNdvi};
		double[] ys= {0,rindeProm};
		LinearRegression lr= new LinearRegression(xs,ys);
		
		Function<Double,Double> calcRinde = (ndvi)->lr.predict(ndvi);
		return calcRinde;
	}
	
	private Function<Double, Double> getRineForNDVILinealFunctionOLD(Double averageNdvi) {
		//lineal r=a*ndvi+b
		//a=(r2-r1)/(ndvi2-ndvi1)=(rProm-0)/(ndviProm-0.1)
		double pendienteNdviRinde = averageNdvi>NDVI_RINDE_CERO?rindeProm/(averageNdvi-NDVI_RINDE_CERO):1;
		//b=0-a*0.1
		double origenNdviRinde = 0-pendienteNdviRinde*NDVI_RINDE_CERO;
		
		Function<Double,Double> calcRinde = (ndvi)->pendienteNdviRinde*ndvi+origenNdviRinde;
		return calcRinde;
	}

	
	//rinde=rindeMax/(1+EXP(-(ndvi-ndviProm)*alfa^beta))
	//ndvi: valor de entrada de la funcion corresponde al valor del ndvi para ese lugar
	//rindeMax: valor asintotico maximo no alcanzado
	//ndviPromedio: valor de ndvi al que se alcalza el 50% del rinde max
	//alfa : pendiente de la curva
	//beta : potencia la pendiente de la curva
	private Function<Double, Double> getRindeForNDVISigmoidFunction(Double ndviProm) {
		//lineal r=a*ndvi+b
		//a=(r2-r1)/(ndvi2-ndvi1)=(rProm-0)/(ndviProm-0.1)
		//double pendienteNdviRinde = ndviProm>NDVI_RINDE_CERO?rindeProm/(ndviProm-NDVI_RINDE_CERO):1;
		//b=0-a*0.1
		//double origenNdviRinde = 0-pendienteNdviRinde*NDVI_RINDE_CERO;
		Configuracion config = Configuracion.getInstance();
		 DecimalFormat dc = PropertyHelper.getDoubleConverter();	
		 Number A =2;
		 Number alfa=2;
		 Number beta=2;
		try {
			 A = dc.parse(config.getPropertyOrDefault("ConvertirNdviACosechaTask.SigmoidA","2"));
			 alfa = dc.parse(config.getPropertyOrDefault("ConvertirNdviACosechaTask.SigmoidAlfa","2"));
			 beta = dc.parse(config.getPropertyOrDefault("ConvertirNdviACosechaTask.SigmoidBeta","2"));
		
			 //XXX aplastar sigmoidea entre rinde min y rinde max?
		} catch (ParseException e) {		
			e.printStackTrace();
		}
		
		double max = this.labor.getMaxRindeProperty().get();
		double min = this.labor.getMinRindeProperty().get();
		double maxTimesRinde =2*(rindeProm-min);//A.doubleValue();
		alfa=2;
		beta=-4.4*ndviProm+5.6;//numeros magicos para que la sigmoidea sea solo un poco mas empinada que la lineal

		double pot = Math.pow(alfa.doubleValue(), beta.doubleValue());//alfa^beta
		Function<Double,Double> calcRinde = (ndvi)->min+maxTimesRinde/(1+Math.exp(-(ndvi-ndviProm)*pot));
		return calcRinde;
	}
	
	private Function<Double, Double> getRindeForNDVISmoothstepFunction(Double ndviProm) {
		  // Scale, bias and saturate x to 0..1 range
		double edge0=NDVI_RINDE_CERO; double edge1=0.9; 
		double lowerlimit= 0.0;double upperlimit=ndviProm*2;
		
		Function<Double,Double> calcRinde = (ndvi)->{
			ndvi = (ndvi - edge0) / (edge1 - edge0);
			if (ndvi < lowerlimit)    ndvi = lowerlimit;
			if (ndvi > upperlimit)	    ndvi = upperlimit;
			 return  ndvi * ndvi * (3 - 2 * ndvi);
		};
		return calcRinde;
	}

		
//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task