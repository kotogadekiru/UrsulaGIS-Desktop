package com.ursulagis.desktop.utils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;


import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * web para probar poligonos
 * http://arthur-e.github.io/Wicket/sandbox-gmaps3.html
 * @author tomas
 *
 */
public class ProyectionConstants {
	//segun wgs84
	public static final double RADIO_TERRESTRE_ECUATORIAL = 6378137/1.021891112380502;//para que el area del pixel de landsat sea 100m2 pero me parece que es mas grande 2%
	public static final double RADIO_TERRESTRE_POLAR =  6356752.3;//6356752;

	private static final double LATITUD_ARGENTINA =-33.5;
	public static final double METROS2_POR_HA = 10000;

	private static double LATITUD_CALCULO=LATITUD_ARGENTINA;
	private static GeometryFactory factory=null;
	private static List<GeodeticCalculator> calculatorPool = new ArrayList<GeodeticCalculator>();
	//public static double metersToLong = 180 / ( Math.PI * RADIO_TERRESTRE_ECUATORIAL*Math.cos(Math.toRadians(LATITUD_ARGENTINA)));// para
	//public static double metersToLat = 180 / ( Math.PI * RADIO_TERRESTRE_POLAR);// para

	//getArea() - area returned in the same units as the coordinates (be careful of lat/lon data!)
	//public static final double A_HAS =1/(metersToLong()*metersToLat()*METROS2_POR_HA);//1/8.06e-11 * 10000

	private synchronized static GeodeticCalculator getCalculator() {
		GeodeticCalculator gc = null;
		
		if(calculatorPool.size()>0) {			
			gc= calculatorPool.get(0);
			calculatorPool.remove(0);
			if(calculatorPool.size()>100) {
				calculatorPool.clear();
			}
		} 
		if(gc==null){
			gc= new GeodeticCalculator(getCRS4326());
		}
		
		return gc;
	}
	public static void setLatitudCalculo(double lat){
		//	System.out.println("actualizando la latitud de trabajo a: "+lat);
		LATITUD_CALCULO=lat;
	}

	public static double A_HAS(){
		return 1/(metersToLong()*metersToLat()*METROS2_POR_HA);
	}

	/**
	 * 
	 * @return la constante por la que hay que multiplicar a una longitud para convertirla en metros
	 */
	public static double metersToLong(){
		//return 180 / ( Math.PI * RADIO_TERRESTRE_ECUATORIAL*Math.cos(Math.toRadians(LATITUD_CALCULO)));
		GeometryFactory fact = getGeometryFactory();
		Point start = fact.createPoint(new Coordinate(0,LATITUD_CALCULO));
		Point dest =getPoint(start,90,100);//dest esta a 100mts al este
		double deltaY=dest.getX()-start.getX();
		//double despues = deltaY/100;
		//antes=9.013372994427096E-6 despues=9.01515282770049E-6
		//System.out.println("antes="+antes+" despues="+despues);
		return deltaY/100;
	}

	/**
	 * 
	 * @return devuelve el valor por el que hay que multiplicar una longitud en metros para convertirla en un arco en grados latitud norte/sur
	 */
	public static double metersToLat(){
		//double antes=  180 / ( Math.PI * RADIO_TERRESTRE_POLAR);
		GeometryFactory fact = getGeometryFactory();
		Point start = fact.createPoint(new Coordinate(0,LATITUD_CALCULO));
		Point dest =getPoint(start,0,100);//dest esta a 100mts al norte
		double deltaY=dest.getY()-start.getY();
		//double despues = deltaY/100;
		//antes=9.013372994427096E-6 despues=9.01515282770049E-6
		//System.out.println("antes="+antes+" despues="+despues);
		return deltaY/100;



	}
	public static double metersToLongLat(double meters){
		//		GeometryFactory factory = new GeometryFactory(new PrecisionModel(
		//				PrecisionModel.FLOATING), SRID.WGS84_SRID.getSRID());

		//		CoordinateReferenceSystem utmArg = factory.createCoordinateReferenceSystem("EPSG:28880");
		//		CoordinateReferenceSystem defaultcrs = factory.createCoordinateReferenceSystem("EPSG:4326");
		//		   MathTransform transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);
		return meters*metersToLat();
	}

	public static GeometryFactory getGeometryFactory(){
		if(factory==null) {
			factory = new GeometryFactory(new PrecisionModel(
					PrecisionModel.FLOATING), SRID.WGS84_SRID.getSRID());
		}
		return factory;
	}

	public static CoordinateReferenceSystem getCRS4326() {	
		return getCRS("EPSG:4326");
	}

	public static CoordinateReferenceSystem getCRS(String crsID) {
		CoordinateReferenceSystem crs=null;
		try {
			crs = CRS.decode(crsID);
		} catch (FactoryException e) {			
			System.err.println("Failed to create CRS for: " + crsID);
			e.printStackTrace();
			// Try to create a default WGS84 CRS as fallback
			try {
				crs = CRS.decode("EPSG:4326", true); // lenient mode
			} catch (FactoryException e2) {
				System.err.println("Failed to create fallback CRS");
				e2.printStackTrace();
			}
		}
		return crs;
	}

	/**
	 * 
	 * @param startPoint
	 * @param destPoint
	 * @return Returns the orthodromic distance (expressed in meters)
	 */
	public static double getDistancia(Point startPoint, Point destPoint){
		Point2D start =  new Point2D.Double(startPoint.getX(),startPoint.getY());
		Point2D dest =  new Point2D.Double(destPoint.getX(),destPoint.getY());
		//startPoint.getFactory().getSRID()

		//Constructs a new geodetic calculator associated with the WGS84 ellipsoid.
		GeodeticCalculator gc = getCalculator();	
		double dist=-1;
	//	synchronized(gc) {
		gc.setStartingGeographicPoint(start);
		gc.setDestinationGeographicPoint(dest);
		dist =  gc.getOrthodromicDistance();//Returns the orthodromic distance (expressed in meters)
	//	}
		calculatorPool.add(gc);
		return dist;		
	}

	public static double getRumbo(Point startPoint,Point destPoint){
		Point2D start =  new Point2D.Double(startPoint.getX(),startPoint.getY());
		Point2D dest =  new Point2D.Double(destPoint.getX(),destPoint.getY());
		//GeodeticCalculator gc = new GeodeticCalculator(getCRS4326());//Constructs a new geodetic calculator associated with the WGS84 ellipsoid.
		GeodeticCalculator gc = getCalculator();	
		gc.setStartingGeographicPoint(start);
		gc.setDestinationGeographicPoint(dest);
		double azimuth =gc.getAzimuth(); 
		calculatorPool.add(gc);
		return azimuth>0?azimuth:(azimuth+360);//devuelve el rumbo que hay que tomar para llegar de start a dest
		
	}

	public static synchronized Point getPoint(Point origen, double azimut,double metros){
		azimut=azimut>180?azimut-360:azimut;
		//Azimuth 231�24.0'E is out of range (�180�).
		GeometryFactory fact =origen.getFactory();
		Point2D start =  new Point2D.Double(origen.getX(),origen.getY());
		//GeodeticCalculator gc = new GeodeticCalculator(getCRS4326());
		GeodeticCalculator gc = getCalculator();	
		gc.setStartingGeographicPoint(start);
		gc.setDirection(azimut, metros);
		Point2D dest2D=  gc.getDestinationGeographicPoint();//java.lang.IllegalStateException: The direction has not been set.
		calculatorPool.add(gc);
		Point dest = fact.createPoint(new Coordinate(dest2D.getX(),dest2D.getY()));
		return dest;
		
	}

	public static void main(String[] args) {
		GeometryFactory fact = getGeometryFactory();
		double lon=0;
		double lat=0;
		double delta=-1;//1grado en el ecuador dist = 111319.4907932264 azimut=90.0
		Point start = fact.createPoint(new Coordinate(lon,lat));
		Point dest =fact.createPoint(new Coordinate(lon+delta,lat));
		System.out.println("start="+start+" dest="+dest);//dist 57.8mts vs  57.87768844431185
		double dist = getDistancia(start,dest);

		double azimut=getRumbo(start, dest);
		System.out.println("dist = "+dist+" azimut="+azimut);
		Point point = getPoint(start,45,10);
		System.out.println("start="+start+" +10a45="+point);//dist 57.8mts vs  57.87768844431185

	}

	public enum SRID { 

		/**
		 * 4326;"EPSG";4326;"GEOGCS["WGS 84"... 
		 */ 

		WGS84_SRID { 
			@Override 
			public int getSRID() { 
				return 4326; 
			} 
		}, 
		EPSG3005_SRID { //3005 es el sistema que permite hacer conversion a metros x metros
			@Override 
			public int getSRID() { 
				return 3005; 
			} 
		}, 
		/**
		 * 3395;"EPSG";3395;"PROJCS["WGS 84 / World Mercator"... 
		 */ 
		WGS84_SRID_PROJCS { 
			@Override 
			public int getSRID() { 
				return 4326; 
			} 
		}; 

		/**
		 * @return an SRID (Spatial Reference IDentifier) * 
		 * @see <a href="http://en.wikipedia.org/wiki/SRID" >SRID</a> 
		 */ 
		public abstract int getSRID(); 
	}

	/**
	 * 
	 * @param feature
	 * @return la superficie en has de la geometria default tomando en cuenta la latitudo de la primera coordenada
	 */
	public static double getHasFeature(SimpleFeature feature) {
		Geometry geom =((Geometry)feature.getDefaultGeometry());
		Coordinate[] c = geom.getCoordinates();
		setLatitudCalculo(c[0].y);
		return A_HAS(geom.getArea());

	}
	/**
	 * 
	 * @param area area en grados al cuadrado como viene de Geometry.getArea();
	 * @return area en Hectareas
	 */
	public static Double A_HAS(double area) {
		return area*A_HAS();
	}
}
