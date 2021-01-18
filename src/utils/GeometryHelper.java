package utils;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

public class GeometryHelper {
	public static Polygon constructPolygon(ReferencedEnvelope e) {
		Coordinate D = new Coordinate(e.getMaxX(), e.getMaxY()); // x-l-d
		Coordinate C = new Coordinate(e.getMinX(), e.getMaxY());// X+l-d
		Coordinate B = new Coordinate(e.getMaxX(), e.getMinY());// X+l+d
		Coordinate A = new Coordinate(e.getMinX(), e.getMinY());// X-l+d

		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, C, D, B, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Polygon poly = fact.createPolygon(coordinates);
		return poly;
	}
	
	/**
	 * 
	 * @param g1
	 * @param g2
	 * @return computes validated intersection. returns null if geometrys dont intersect
	 */
	
	public static Geometry getIntersection(Geometry g1, Geometry g2){
		g1 = PolygonValidator.validate(g1);
		g2 = PolygonValidator.validate(g2);
		Geometry intersection =null;
		if (g1 != null && g2!=null && g1.intersects(g2)){
		try {			
				intersection = g1.intersection(g2);// Computes a Geometry//found non-noded intersection between LINESTRING ( -61.9893807883
				intersection = PolygonValidator.validate(intersection);
		
			
		} catch (Exception te) {
			try{
				intersection = EnhancedPrecisionOp.difference(g1, g2);
			}catch(Exception e){
				te.printStackTrace();
			}
		}
		}
		return intersection;
	}
}
