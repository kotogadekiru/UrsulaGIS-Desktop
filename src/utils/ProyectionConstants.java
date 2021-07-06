package utils;

import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.geotools.referencing.GeodeticCalculator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.primitive.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
	//public static double metersToLong = 180 / ( Math.PI * RADIO_TERRESTRE_ECUATORIAL*Math.cos(Math.toRadians(LATITUD_ARGENTINA)));// para
	//public static double metersToLat = 180 / ( Math.PI * RADIO_TERRESTRE_POLAR);// para
	
	//getArea() - area returned in the same units as the coordinates (be careful of lat/lon data!)
	//public static final double A_HAS =1/(metersToLong()*metersToLat()*METROS2_POR_HA);//1/8.06e-11 * 10000
	
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
		return 180 / ( Math.PI * RADIO_TERRESTRE_ECUATORIAL*Math.cos(Math.toRadians(LATITUD_CALCULO)));
	}
	
	public static double metersToLat(){
		return 180 / ( Math.PI * RADIO_TERRESTRE_POLAR);
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
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(
				PrecisionModel.FLOATING), SRID.WGS84_SRID.getSRID());
		return factory;
	}
	
	public static double getDistancia(Point2D origen, Point2D destino){
		GeodeticCalculator gc = new GeodeticCalculator();//Constructs a new geodetic calculator associated with the WGS84 ellipsoid.
		gc.setStartingGeographicPoint(origen);
		gc.setDestinationGeographicPoint(destino);
		return gc.getOrthodromicDistance();
	}
	
	public static Point2D getPoint(Point2D origen, double azimut,double metros){
		GeodeticCalculator gc = new GeodeticCalculator();
		gc.setStartingGeographicPoint(origen);
		gc.setDirection(azimut, metros);
		return gc.getDestinationGeographicPoint();
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
