package utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Poligono;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gui.Messages;
import gui.PoligonLayerFactory;
import tasks.procesar.ExtraerPoligonosDeLaborTask;

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
	 * @param l
	 * @param d
	 * @param X
	 * @return devuelve un poligono con centro en X y expandido en l y d
	 */
	public static Polygon constructPolygon(Coordinate l, Coordinate d, Point X) {
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
	
	public static Polygon constructPolygon(Envelope e) {
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
	
	public static Point constructPoint(Position e) {
		Coordinate A = new Coordinate(e.getLongitude().getDegrees(), e.getLatitude().getDegrees());// X-l+d
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Point point = fact.createPoint(A);
		return point;
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
	
	
	/**
	 * metodo que recorre todas las geometrias haciendo las intersecciones de todos con todos.
	 * @param geometriasActivas
	 * @return
	 */
	public static Set<Geometry> obtenerIntersecciones(List<Geometry> geometriasActivas){
	// unir todas las geometrias.
		//crear un poligono con los exterior rings de todas las geometrias
		//obtener la diferencia entre la union y los vertices
		GeometryFactory fact = geometriasActivas.get(0).getFactory();
		GeometryCollection colectionCat = fact.createGeometryCollection(geometriasActivas.toArray(new Geometry[geometriasActivas.size()]));
		Geometry convexHull = colectionCat.buffer(ProyectionConstants.metersToLongLat(0));
		
		List<Geometry> boundarys = new ArrayList<Geometry>();
		for(Geometry g : geometriasActivas) {
			boundarys.add(g.getBoundary());
		}
		GeometryCollection boundarysCol = fact.createGeometryCollection(boundarys.toArray(new Geometry[boundarys.size()]));
		Geometry boundary_buffer = boundarysCol.buffer(ProyectionConstants.metersToLongLat(0.25));
		
		Geometry diff = convexHull.difference(boundary_buffer);
		Set<Geometry> geometriasOutput = new HashSet<Geometry>();
		
		for(int n = 0; n < diff.getNumGeometries(); n++){
			Geometry g = diff.getGeometryN(n);
			g = PolygonValidator.validate(g);
			geometriasOutput.add(g);
		}
		
		
		return geometriasOutput;
	}
	

}
