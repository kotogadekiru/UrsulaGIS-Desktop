package utils;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

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

}
