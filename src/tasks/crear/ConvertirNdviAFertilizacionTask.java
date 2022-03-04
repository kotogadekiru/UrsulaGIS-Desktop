package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.Ndvi;
import dao.Poligono;
import dao.config.Cultivo;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gui.Messages;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import tasks.ProcessMapTask;
import tasks.ShowNDVITifFileTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;

public class ConvertirNdviAFertilizacionTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	public double  ndviMin = ShowNDVITifFileTask.MIN_VALUE;//0.2;
	public double  ndviMax = 1;//ShowNDVITifFileTask.MIN_VALUE;//0.2;
	Double dosisMax = new Double(0);
	Double dosisMin = new Double(0);
	Ndvi ndvi=null;

	public ConvertirNdviAFertilizacionTask(FertilizacionLabor cosechaLabor,Ndvi _ndvi,Double _dosisMax,Double _dosisMin){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		dosisMax=_dosisMax;
		dosisMin = _dosisMin;
		ndvi=_ndvi;
	}

	public void doProcess() throws IOException {
		Iterable<? extends GridPointAttributes> values = ndvi.getSurfaceLayer().getValues();
		Iterator<? extends GridPointAttributes> it = values.iterator();
		
		double max = 0;
		double min = Double.MAX_VALUE;
		double sum =0;
		int size = 0;
		
		while(it.hasNext()){
			GridPointAttributes gpa = it.next();
			Double value = new Double(gpa.getValue());
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
			System.out.println("calculando el promedio para el valor "+value+" suma parcial "+sum+" size "+size);
			
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
		Double averageNdvi = new Double(sum/size);
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
		
		double latStep = -sector.getDeltaLatDegrees() / (double) (height-1);//-1
		double lonStep = sector.getDeltaLonDegrees() / (double) (width-1);
		
		//List<FertilizacionItem> itemsToShow = new ArrayList<FertilizacionItem>();
		
		double elev = 1;
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
					
					//Double rindeNDVI = new Double(pendienteNdviRinde*ndvi+origenNdviRinde);//aproximacion lineal
					Double dosisNDVI = new Double(calcDosis.apply(ndvi));//aproximacion logaritmica
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
						if(poly.isEmpty())continue;
						}catch(Exception e) {//com.vividsolutions.jts.geom.TopologyException: Found null DirectedEdge
							e.printStackTrace();//com.vividsolutions.jts.geom.TopologyException: side location conflict [ (-61.920393510481325, -33.66456750394795, NaN) ]
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

	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {

		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$

		String tooltipText = new String(// TODO ver si se puede instalar un
				// boton
				// que permita editar el dato
				Messages.getString("ProcessFertMapTask.2") + df.format(fertFeature.getDosistHa()) //$NON-NLS-1$
				+ Messages.getString("ProcessFertMapTask.3") + Messages.getString("ProcessFertMapTask.4") //$NON-NLS-1$ //$NON-NLS-2$
				+ df.format(fertFeature.getImporteHa()) + Messages.getString("ProcessFertMapTask.5") //$NON-NLS-1$
				//+ "Sup: "
				//+ df.format(area * ProyectionConstants.METROS2_POR_HA)
				//+ " m2\n"
				// +"feature: " + featureNumber
				);
		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessFertMapTask.6")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessFertMapTask.7")); //$NON-NLS-1$ //$NON-NLS-2$
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessFertMapTask.8")+df.format(area ) + Messages.getString("ProcessFertMapTask.9")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		//List  paths = 
		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);

		//return null;
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
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