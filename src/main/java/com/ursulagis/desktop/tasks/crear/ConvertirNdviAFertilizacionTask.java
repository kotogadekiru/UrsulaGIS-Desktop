package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import com.ursulagis.desktop.gui.Messages;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.ShowNDVITifFileTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ConvertirNdviAFertilizacionTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	public double  ndviMin = ShowNDVITifFileTask.MIN_VALUE;//0.2;
	public double  ndviMax = 1;//ShowNDVITifFileTask.MIN_VALUE;//0.2;
	private boolean correguirOutlayer=false;
	Double dosisMax = 0.0;
	Double dosisMin = 0.0;
	Ndvi ndvi=null;

	public ConvertirNdviAFertilizacionTask(FertilizacionLabor cosechaLabor,Ndvi _ndvi,Double _dosisMax,Double _dosisMin){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		dosisMax=_dosisMax;
		dosisMin = _dosisMin;
		ndvi=_ndvi;
		correguirOutlayer=labor.getConfigLabor().correccionOutlayersEnabled();
		System.out.println("CooregirOut: " + correguirOutlayer);
	}
	
	public void doProcess() throws IOException {
		//labor.setContorno(ndvi.getContorno());
		Iterable<? extends GridPointAttributes> values = ndvi.getSurfaceLayer().getValues();
		Iterator<? extends GridPointAttributes> it = values.iterator();
		
		double max = 0;
		double min = Double.MAX_VALUE;
		double sum =0;
		int size = 0;
		
		while(it.hasNext()){
			GridPointAttributes gpa = it.next();
			Double value = Double.valueOf(gpa.getValue());
			//OJO cuando se trabaja con el jar la version de world wind que esta levantando no es la que usa eclipse.
			// es por eso que el jar permite valores Nan y en eclipse no.
			if(value < ShowNDVITifFileTask.MIN_VALUE 
					|| value > ShowNDVITifFileTask.MAX_VALUE 
					|| value == 0 
					|| value.isNaN() 
					|| value <= ndviMin
					|| value >= ndviMax){
					continue;
			} else{
			//System.out.println("calculando el promedio para el valor "+value+" suma parcial "+sum+" size "+size);
			
			max = Math.max(max,value);
			min = Math.min(min,value);
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
		System.out.println("maxNDVI "+max);
		System.out.println("minNDVI "+min);
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
		
		//List<FertilizacionItem> itemsToShow = new ArrayList<FertilizacionItem>();
		
		double elev = 10.0;
		double minLat = sector.getMaxLatitude().degrees;
		double minLon = sector.getMinLongitude().degrees;
		
		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(labor.getType());
		
		double id=0;
		//lineal r=a*ndvi+b
		//a=(r2-r1)/(ndvi2-ndvi1)=(rProm-0)/(ndviProm-0.1)
		//double pendienteNdviRinde=averageNdvi>NDVI_RINDE_CERO ? dosisMax/(averageNdvi-NDVI_RINDE_CERO):1;
		//b=0-a*0.1
		
		double pendiente = (dosisMax-dosisMin)/(max-min);
		double origen = dosisMin;//+pendiente*max;
		
		//max = 0.9 min=0.1 dosisMax = 100 dosisMin=0 => pendiente = (0-100)/(0.9-0.8)=-100/0.8=-125
		// dosis = -125*0.9+100 = 
		double offset = 0.8;
//		Function<Double,Double> calcDosis = interpolacionCuadratica(min, dosisMax ,
//																	averageNdvi, (dosisMax - dosisMin)*offset/2,
//																	max, dosisMin);
		
//		Function<Double,Double> calcDosis = interpolacionCuadratica(min, min,
//				averageNdvi, averageNdvi,
//				max, max);
		
		final double ndviMin = min;
		Function<Double,Double> calcDosis = (ndvi)->pendiente*(ndvi-ndviMin)+origen;
		
		System.out.println("min= "+min+" => "+calcDosis.apply(min));
		System.out.println("max= "+max+" => "+calcDosis.apply(max));
		
		//logaritmica
//		double pendienteNdviRinde=averageNdvi>NDVI_RINDE_CERO?Math.log(rindeProm)/(Math.log(averageNdvi)-Math.log(NDVI_RINDE_CERO)):1;
//		double origenNdviRinde = -pendienteNdviRinde*Math.log(NDVI_RINDE_CERO);
//		System.out.println("r="+pendienteNdviRinde+"* Math.log(_ndvi+0.8)"+origenNdviRinde);
//		Function<Double,Double> calcRinde = (_ndvi)->pendienteNdviRinde*Math.log(_ndvi+0.8)+origenNdviRinde;
		
		//cuadratica tiene muy buen ajuste
		
		Poligono contorno = this.ndvi.getContorno();
		Geometry contornoGeom =null;
		if(contorno !=null) {
			contornoGeom = contorno.toGeometry();
			System.out.println("hay controno");
		}
		
		for (int y = 0; y < height; y++){
			double lat = minLat + y * latStep;
			for (int x = 0; x < width; x++)	{
				double lon = minLon+x*lonStep;
				
				GridPointAttributes attr = it.hasNext() ? it.next() : null;
				double ndvi = attr.getValue();
				if(ndvi<=ndviMin||ndvi>=ndviMax){//&&(x<3 || y<3||x>width-3||y>height-3)){
					continue;//me salteo la primera fila de cada costado
				}
				
				
				if(ndvi <= ShowNDVITifFileTask.MAX_VALUE && ndvi >= ShowNDVITifFileTask.MIN_VALUE && ndvi !=0){
					FertilizacionItem ci = new FertilizacionItem();
					ci.setId(id);
					ci.setElevacion(elev);
					ProyectionConstants.setLatitudCalculo(lat);
					double ancho =lonStep * ProyectionConstants.metersToLong();
					ci.setAncho(ancho);
					ci.setDistancia(ancho);
					
					//Double rindeNDVI = Double.parseDouble(pendienteNdviRinde*ndvi+origenNdviRinde);//aproximacion lineal
					Double dosisNDVI = Double.valueOf(calcDosis.apply(ndvi));//aproximacion logaritmica
					//System.out.println("creado nueva cosecha con rinde "+rindeNDVI+" para el ndvi "+value+" rinde prom "+rinde+" ndvi promedio "+average);
					ci.setDosistHa(dosisNDVI);
					labor.setPropiedadesLabor(ci);
					
					Coordinate[] coordinates = new Coordinate[5];
					coordinates[0]= new Coordinate(lon,lat,elev);
					coordinates[1]= new Coordinate(lon+lonStep,lat,elev);
					coordinates[2]= new Coordinate(lon+lonStep,lat+latStep,elev);
					coordinates[3]= new Coordinate(lon,lat+latStep,elev);
					coordinates[4]= new Coordinate(lon,lat,elev);
					
					Geometry poly = fact.createPolygon(coordinates);	
				
					// recortar si esta afuera del contorno del ndvi
					if(contornoGeom!=null && !contornoGeom.covers(poly)) {//OK! funciona. no introducir poligonos empty!
						try {
						poly=GeometryHelper.getIntersection(poly,contornoGeom);
						if(poly == null || poly.isEmpty())continue;
						System.out.println("el contorno no cubre el polygono y la interseccion es: "+poly.toText());
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
				//	itemsToShow.add(ci);
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
	//	
		if (correguirOutlayer) {
				//System.out.println("corrijo outlayer" + labor.config.getAnchoFiltroOutlayers());
				corregirOutlayersParalell();
				System.out.println("corrio outlayer");
		}		
		labor.constructClasificador();	
		runLater(this.getItemsList());
		updateProgress(0, featureCount);

	}

	public static Function<Double,Double> interpolacionCuadratica(double x0,double y0, double x1, double y1, double x2,double y2) {
		//max = 0.9 min=0.1 dosisMax = 100 dosisMin=0 => pendiente = (0-100)/(0.9-0.8)=-100/0.8=-125
		// dosis = -125*0.9+100 = 
		//funcion cuadratica de lagrange segun 
		//https://sites.google.com/site/numerictron/unidad-4/4-1-interpolacion-lineal-y-cuadratica
		Function<Double,Double> func = (x)->{			
			double ret =  0;
			try{
				if(x==x2) {
					ret = 0+// si x ==x2 div by zero
						      (y1*(x-x0)*(x-x2)) / ((x1-x0)*(x1-x2))+
						      (y2*(x-x0)*(x-x1)) / ((x2-x0)*(x2-x1));
				} else {
				ret = (y0*(x-x1)*(x-x2)) / ((x0-x1)*(x-x2))+// si x ==x2 div by zero
				      (y1*(x-x0)*(x-x2)) / ((x1-x0)*(x1-x2))+
				      (y2*(x-x0)*(x-x1)) / ((x2-x0)*(x2-x1));
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				ret =y0;
			}
			if(!Double.isFinite(ret)) ret = y0;
			return ret;
		};
		return func;
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearFertilizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
	
	private void corregirOutlayersParalell() {			
		//GeodeticCalculator calc = new GeodeticCalculator(DefaultEllipsoid.WGS84); 
		System.out.println("se corrije Paralel outliers");
		//1) crear un circulo de radio a definir y centro en el centroide de la cosecha
		double ancho = 10;
		double alfa = 0;
		double distancia = ancho;
		Coordinate anchoLongLatCoord = constructCoordinate(alfa, ancho);
		Coordinate distanciaLongLat = constructCoordinate(alfa+ Math.PI / 2, distancia);
		
		int initOutCollectionSize = labor.outCollection.size();
		//System.out.println("Cantidad de colectios:" + initOutCollectionSize);
		SimpleFeature[] arrayF = new SimpleFeature[labor.outCollection.size()];
		labor.outCollection.toArray(arrayF);
		List<SimpleFeature> outFeatures = Arrays.asList(arrayF);
		List<SimpleFeature>  filteredFeatures = outFeatures.parallelStream().collect(
				()-> new  ArrayList<SimpleFeature>(),
				(list, pf) ->{		
					try{
						FertilizacionItem ci = labor.constructFeatureContainerStandar(pf,false);
						Point X = ci.getGeometry().getCentroid();
						Polygon poly = constructPolygon(anchoLongLatCoord, distanciaLongLat, X);
						List<FertilizacionItem> features = labor.cachedOutStoreQuery(poly.getEnvelopeInternal());
											
						if(features.size()>0){						
							outlayerCV(ci, poly,features);						
							SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(labor.getType());
							SimpleFeature f = ci.getFeature(fBuilder);
							list.add(f);	
							//This method is safe to be called from any thread.	
							//updateProgress((list.size()+featureCount)/2, featureCount);
						} else{
							System.out.println("la query devolvio cero elementos"); //$NON-NLS-1$
						}
					}catch(Exception e){
						System.err.println("error en corregirOutliersParalell"); //$NON-NLS-1$
						e.printStackTrace();
					}
				},	(list1, list2) -> list1.addAll(list2));
		//XXX esto termina bien. filteredFeatures tiene 114275 elementos como corresponde

		
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		 //$NON-NLS-1$
		boolean res = newOutcollection.addAll(filteredFeatures);
		if(!res){
			System.out.println("fallo el addAll(filteredFeatures)"); 
		}

		labor.clearCache();

		int endtOutCollectionSize = newOutcollection.size();
		if(initOutCollectionSize !=endtOutCollectionSize){
			System.err.println("se perdieron elementos al hacer el filtro de outlayers. init="
					+initOutCollectionSize
					+" end="+endtOutCollectionSize); 
		}
		labor.setOutCollection(newOutcollection);
		featureCount=labor.outCollection.size();
	}
	
	public Coordinate constructCoordinate(double alfa, double ancho) {
		return new Coordinate(
				ProyectionConstants.metersToLong() * ancho / 2
				* Math.sin(alfa),
				ProyectionConstants.metersToLat() * ancho / 2
				* Math.cos(alfa));
	}
	
	public Polygon constructPolygon(Coordinate l, Coordinate d, Point X) {
		double x = X.getX();
		double y = X.getY();

		Coordinate D = new Coordinate(x - l.x - d.x, y - l.y - d.y); // x-l-d
		Coordinate C = new Coordinate(x + l.x - d.x, y + l.y - d.y);// X+l-d
		Coordinate B = new Coordinate(x + l.x + d.x, y + l.y + d.y);// X+l+d
		Coordinate A = new Coordinate(x - l.x + d.x, y - l.y + d.y);// X-l+d

		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = X.getFactory();

		//		LinearRing shell = fact.createLinearRing(coordinates);
		//		LinearRing[] holes = null;
		//		Polygon poly = new Polygon(shell, holes, fact);
		Polygon poly = fact.createPolygon(coordinates);

		return poly;
	}
	
	/**
	 * 
	 * @param cosechaFeature
	 * @param poly es el area dentro de la que se calcula el outlayer
	 * @param features
	 * @return true si cosechaFeature fue modificada
	 */
	private boolean outlayerCV(FertilizacionItem cosechaFeature, Polygon poly,	List<FertilizacionItem> features) {
		boolean ret = false;
		Geometry geo = cosechaFeature.getGeometry().getCentroid();
		double rindeCosechaFeature = cosechaFeature.getDosistHa();
		double sumatoriaRinde = 0;			
		double sumatoriaAltura = 0;				
		double divisor = 0;
		// cambiar el promedio directo por el metodo de kriging de interpolacion. ponderando los rindes por su distancia al cuadrado de la muestra
		double ancho = 50;
		//la distancia no deberia ser mayor que 2^1/2*ancho, me tomo un factor de 10 por seguridad e invierto la escala para tener mejor representatividad
		//en vez de tomar de 0 a inf, va de ancho*(10-2^1/2) a 0
		ancho = Math.sqrt(2)*ancho;



		for(FertilizacionItem cosecha : features){
			double cantidadCosecha = cosecha.getDosistHa();	
		//	System.out.println("cantidad gertilizante  es:" + cantidadCosecha );
			Geometry geo2 = cosecha.getGeometry().getCentroid();
			double distancia =geo.distance(geo2)/ProyectionConstants.metersToLat();

			double distanciaInvert = (ancho-distancia);
			if(distanciaInvert<0)System.out.println(Messages.getString("ProcessHarvestMapTask.19")+distanciaInvert); //$NON-NLS-1$
			//los pesos van de ~ancho^2 para los mas cercanos a 0 para los mas lejanos
			double weight =  Math.pow(distanciaInvert,2);
			//System.out.println("distancia="+distancia+" distanciaInvert="+distanciaInvert+" weight="+weight);

			//			cantidadCosecha = Math.min(cantidadCosecha,labor.maxRindeProperty.doubleValue());
			//			cantidadCosecha = Math.max(cantidadCosecha,labor.minRindeProperty.doubleValue());
			if(isBetweenMaxMin(cantidadCosecha)){
				//TODO solo promediar la altura si esta dentro de los 2 quintiles centrales
				sumatoriaAltura+=cosecha.getElevacion()*weight;
				sumatoriaRinde+=cantidadCosecha*weight;
				divisor+=weight;		
			}			
		}
		boolean rindeEnRango = isBetweenMaxMin(rindeCosechaFeature);

		double promedioRinde = 0.0;
		double promedioAltura = 0.0;
		if(divisor>0){
			promedioRinde = sumatoriaRinde/divisor;
			//			promedioRinde = Math.min(promedioRinde,labor.maxRindeProperty.doubleValue());
			//			promedioRinde = Math.max(promedioRinde,labor.minRindeProperty.doubleValue());
			promedioAltura = sumatoriaAltura/divisor;
		}else{
			System.out.println(Messages.getString("ProcessHarvestMapTask.20")+ divisor); //$NON-NLS-1$
			System.out.println(Messages.getString("ProcessHarvestMapTask.21")+sumatoriaRinde); //$NON-NLS-1$
		}
		//4) obtener la varianza (LA DIF ABSOLUTA DEL DATO Y EL PROM DE LA MUESTRA) (EJ. ABS(10-9.3)/9.3 = 13%)
		//SI 13% ES MAYOR A TOLERANCIA CV% REEMPLAZAR POR PROMEDIO SINO NO

		if(!(promedioRinde==0)){
			double coefVariacionCosechaFeature = Math.abs(rindeCosechaFeature-promedioRinde)/promedioRinde;
		//	cosechaFeature.setDesvioRinde(coefVariacionCosechaFeature);
			System.out.println("EL COEFICIENTE ES: " + coefVariacionCosechaFeature);
			if(coefVariacionCosechaFeature > 0 ||!rindeEnRango){//si el coeficiente de variacion es mayor al 20% no es homogeneo
				//El valor esta fuera de los parametros y modifico el valor por el promedio
					System.out.println("reemplazo "+cosechaFeature.getDosistHa()+" por "+promedioRinde);
				cosechaFeature.setDosistHa(promedioRinde);

				cosechaFeature.setElevacion(promedioAltura);
				ret=true;
			}
		}
		return ret;
	}
	
	private boolean isBetweenMaxMin(double cantidadCosecha) {
		boolean ret = 0<=cantidadCosecha && cantidadCosecha<=1000;
		if(!ret){
			//	System.out.println(cantidadCosecha+">"+labor.maxRindeProperty.doubleValue()+" o <"+labor.minRindeProperty.doubleValue());
		}
		return ret;
	}

	
	public static void main(String[] args) {
		Function<Double,Double> calcDosis = interpolacionCuadratica(0, 0,
		5, 5*0.5,
		10, 10);
		for(double i=0;i<11;i++) {
			System.out.println("x= "+i+" y= "+calcDosis.apply(i));
		}

		
	}
}// fin del task