package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
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
	
	public static Poligono constructPoligono(Geometry g) {
		Poligono p = new Poligono();
		List<Position> positions = new ArrayList<Position>();
		Coordinate[] coords = g.getBoundary().getCoordinates();
		for(int i=0;i<coords.length;i++) {
			Coordinate c = coords[i];
			positions.add(Position.fromDegrees(c.y, c.x));
			
		}
		p.setPositions(positions);
		
		return p;
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
	
	public static LineString constructLineString(Position p1, Position p2) {
		Coordinate[] coords ={constructPoint(p1).getCoordinate(),constructPoint(p2).getCoordinate()};
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		return fact.createLineString(coords);
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
				//intersection= EnhancedPrecisionOp.intersection(g1,g2);
				intersection = g1.intersection(g2);// Computes a Geometry//found non-noded intersection between LINESTRING ( -61.9893807883
				intersection = PolygonValidator.validate(intersection);
		
		} catch (Exception te) {
			try{
				intersection = EnhancedPrecisionOp.intersection(g1, g2);
			}catch(Exception e){
				te.printStackTrace();
			}
		}
		}
		return intersection;
	}
	
	public static Geometry getIntersectionSlow(Geometry g1, Geometry g2){
		List<Geometry> toIntersect = Arrays.asList(g1,g2);
		
		Set<Geometry> parts = obtenerIntersecciones(toIntersect);
		List<Geometry> intersections = new ArrayList<Geometry>();
		Geometry intersection =null;
		for(Geometry candidate:parts) {
			boolean isContained = true;
			for(Geometry check:toIntersect) {
				isContained = isContained && check.contains(candidate);
			}
			if(isContained) {
				intersections.add(candidate);
				//intersection = intersection==null?candidate:intersection.union(candidate);
			}
			
		}
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		
		intersection= fact.createGeometryCollection(intersections.toArray(new Geometry[intersections.size()])).buffer(0);
		if(intersection==null) {
			//System.out.println("no se pudo calcular la interseccion entre "+g1+" y "+g2);
		}
		return intersection;
	}
	/**
	 * metodo que recorre todas las geometrias haciendo las intersecciones de todos con todos.
	 * @param geometriasActivas
	 * @return
	 */
	public static Set<Geometry> obtenerIntersecciones(List<Geometry> geometriasActivas){
	//	import org.locationtech.jts.densify.Densifier;
	
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
		//double tolerance = ProyectionConstants.metersToLongLat(1);
		double bufferWidth = ProyectionConstants.metersToLongLat(0.25);
		for(int n = 0; n < diff.getNumGeometries(); n++){
			Geometry g = diff.getGeometryN(n);
			g=g.buffer(bufferWidth);//mitad del buffer esta en esta y mitad en la otra
			//g=	Densifier.densify(g,tolerance );
			g = PolygonValidator.validate(g);
			geometriasOutput.add(g);
		}
		
		
		return geometriasOutput;
	}

	/**
	 * 
	 * @param complex
	 * @return la geometria reemplazando 2 vertices por su promedio 
	 */
	public static Geometry simplify(Geometry complex) {
		complex = ExtraerPoligonosDeLaborTask.geometryToPoligono(complex).toGeometry();
		if(complex instanceof Polygon) {
		Densifier densifier = new Densifier(complex);
		densifier.setDistanceTolerance(ProyectionConstants.metersToLongLat(10));
		complex=densifier.getResultGeometry();
		//buffered = TopologyPreservingSimplifier.simplify(buffered, ProyectionConstants.metersToLongLat(2));
	//	buffered = DouglasPeuckerSimplifier.simplify(buffered, ProyectionConstants.metersToLongLat(5));
		
		Geometry simple = null;
		Coordinate[] complexCoordinates = complex.getCoordinates();
		
		List<Coordinate> middleCoords = new ArrayList<Coordinate>();
		middleCoords.add(complexCoordinates[0]);
		for(int i=1;i<complexCoordinates.length;i++) {
			Coordinate last = complexCoordinates[i-1];
			Coordinate next = complexCoordinates[i];
			Point p1 = ProyectionConstants.getGeometryFactory().createPoint(last);
			Point p2 = ProyectionConstants.getGeometryFactory().createPoint(next);
			Double dist = p1.distance(p2);
			if(dist>ProyectionConstants.metersToLongLat(0.25)) {
				middleCoords.add(next);
			}
		}
		
		List<Coordinate> simpleCoords = new ArrayList<Coordinate>();
		//simpleCoords.add(middleCoords.get(0));
		for(int i=1;i<middleCoords.size();i++) {
			Coordinate last = middleCoords.get(i-1);
			Coordinate next = middleCoords.get(i);
			Coordinate newCoord = new Coordinate();
			newCoord.x=(last.x+next.x)/2;
			newCoord.y=(last.y+next.y)/2;
			simpleCoords.add(newCoord);
		}
//		Coordinate last = middleCoords.get(middleCoords.size()-1);
//		Coordinate next = middleCoords.get(0);
//		Coordinate newCoord = new Coordinate();
//		newCoord.x=(last.x+next.x)/2;
//		newCoord.y=(last.y+next.y)/2;
//		simpleCoords.add(0,newCoord);
//		simpleCoords.add(newCoord);
		simpleCoords.add(simpleCoords.get(0));
		simple =complex.getFactory().createPolygon(simpleCoords.toArray(new Coordinate[simpleCoords.size()]));
		return simple;
		} else if(complex instanceof MultiPolygon) {
			
			List<Geometry> simples = new ArrayList<Geometry>();
			for(int i=0;i<complex.getNumGeometries();i++) {
				Geometry g = complex.getGeometryN(i);
				simples.add(simplify(g));
			}
			GeometryCollection col = new GeometryCollection(simples.toArray(new Geometry[simples.size()]),complex.getFactory());
			return col.buffer(ProyectionConstants.metersToLongLat(0.25));
		}
		return complex;
	}
	
	public static Double getHas(Geometry g) {
		Double area =0.0;
		try {
			if(g!=null) {
				area = ProyectionConstants.A_HAS(g.getArea());
			}else {
				//System.out.println("No se pudo calcular el area de la geometria "+g);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return area;
	}

	public static Geometry unirGeometrias(List<Geometry> geometriasActivas) {
		
		geometriasActivas = geometriasActivas.stream().map(g->{
			  Densifier densifier = new Densifier(g);
			  densifier.setDistanceTolerance(ProyectionConstants.metersToLongLat(10));
			  g=densifier.getResultGeometry();
			  return  g;			
			}).collect(Collectors.toList());
		
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();		
		Geometry[] geomArray = geometriasActivas.toArray(new Geometry[geometriasActivas.size()]);
		GeometryCollection collection = fact.createGeometryCollection(geomArray);
		
		Geometry union = collection.buffer(0);//ProyectionConstants.metersToLongLat(20));
		//System.out.println("geometria densa unida "+union);
		return union;		
	}
	

}
