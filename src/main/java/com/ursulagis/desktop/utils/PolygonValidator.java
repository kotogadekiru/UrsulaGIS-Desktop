package com.ursulagis.desktop.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.locationtech.jts.geom.*;

import org.locationtech.jts.operation.polygonize.Polygonizer;

public class PolygonValidator {

	public static List<Polygon> geometryToFlatPolygons(Geometry itemGeometry){
		List<Polygon> ret=new ArrayList<Polygon>();

		if(itemGeometry instanceof MultiPolygon){
			//System.out.println("convirtiendo un multipolygon en polygon " +itemGeometry);
			MultiPolygon mp = (MultiPolygon)itemGeometry;
			for(int i=0;i<mp.getNumGeometries();i++){
				Geometry gi=mp.getGeometryN(i);
				if(gi instanceof Polygon){
					Polygon pi =(Polygon)gi;
					ret.add(polygonToFlatPolygon(pi));
				}else if(gi instanceof MultiPolygon){
					System.err.println("gi es MultyPolygon en geometryToFlatPolygons");
					ret.addAll(geometryToFlatPolygons(gi));
				}else {
					System.err.println("estoy perdiendo la geometria "+gi.toText());
				}
				//si no es polygono ignorarla?
			}

		} else if(itemGeometry instanceof Polygon) {
			Polygon pi =(Polygon)itemGeometry;
			ret.add(polygonToFlatPolygon(pi));
		} else {
			System.out.println("geometry no es multiPolygon ni poligon "+ itemGeometry);
		}
		return ret;
	}

	/**
	 * metodo que toma un poligono y delvuelve un poligono igual nuevo pero sin la coordenada z.
	 * @param pi
	 * @return
	 */
	public static Polygon polygonToFlatPolygon(Polygon pi){
		GeometryFactory fact = pi.getFactory();		
		LinearRing shell = fact.createLinearRing(coordsToFlat( pi.getExteriorRing().getCoordinates()));
		LinearRing[] holes = new LinearRing[pi.getNumInteriorRing()];
		for(int i=0;i<pi.getNumInteriorRing();i++){
			holes[i]=fact.createLinearRing(coordsToFlat( pi.getInteriorRingN(i).getCoordinates()));
		}
		Polygon p = pi.getFactory().createPolygon(shell,holes);
		return p;
	}

	/**
	 * metodo que elimina la coordenada z de las coordenadas
	 * @param boundaryCoords
	 * @return
	 */
	public static Coordinate[] coordsToFlat(Coordinate[] boundaryCoords) {		
		Coordinate[] coordinates = new Coordinate[boundaryCoords.length];
		for(int i =0;i<boundaryCoords.length;i++){						
			Coordinate c = boundaryCoords[i];
			coordinates[i]=new Coordinate(c.x,c.y);
		}
		return coordinates;
	}



	/**
	 * Get / create a valid version of the geometry given. If the geometry is a polygon or multi polygon, self intersections /
	 * inconsistencies are fixed. Otherwise the geometry is returned.
	 * 
	 * @param geom
	 * @return a valid geometry or null if not posible
	 */
	@SuppressWarnings("unchecked")
	public static Geometry validate(Geometry geom){
		try {
			if(geom instanceof Polygon){
				try {
				if(geom.isValid()){//exception por cannot compute quadrant for point (0.0, 0.0)
					geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
					return geom; // If the polygon is valid just return it
				}
				}catch(Exception e) {
					System.out.println("no puedo ver si geom.isValid "+geom);
				//	e.printStackTrace();
				}
				Polygonizer polygonizer = new Polygonizer();
			
				addPolygon((Polygon)geom, polygonizer);
				return toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
			}else if(geom instanceof MultiPolygon){
				try {
					if(geom!=null && geom.isValid()){
					geom.normalize(); // validate does not pick up rings in the wrong order - this will fix that
					return geom; // If the multipolygon is valid just return it
				}}catch(Exception ex){
					System.out.println("no puedo ver si geom.isValid "+geom);
					//ex.printStackTrace();
				}
				Polygonizer polygonizer = new Polygonizer();
				for(int n = geom.getNumGeometries(); n-- > 0;){
					addPolygon((Polygon)geom.getGeometryN(n), polygonizer);
				}
				return toPolygonGeometry(polygonizer.getPolygons(), geom.getFactory());
			}else{
				return geom; // In my case, I only care about polygon / multipolygon geometries
			}
		}catch(Exception e) {//java.lang.IllegalArgumentException: Cannot compute the quadrant for point ( 0.0, 0.0 )
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Add all line strings from the polygon given to the polygonizer given
	 * 
	 * @param polygon polygon from which to extract line strings
	 * @param polygonizer polygonizer
	 */
	static void addPolygon(Polygon polygon, Polygonizer polygonizer){
		addLineString(polygon.getExteriorRing(), polygonizer);
		for(int n = polygon.getNumInteriorRing(); n-- > 0;){
			addLineString(polygon.getInteriorRingN(n), polygonizer);
		}
	}

	/**
	 * Add the linestring given to the polygonizer
	 * 
	 * @param linestring line string
	 * @param polygonizer polygonizer
	 */
	static void addLineString(LineString lineString, Polygonizer polygonizer){

		if(lineString instanceof LinearRing){ // LinearRings are treated differently to line strings : we need a LineString NOT a LinearRing
			lineString = lineString.getFactory().createLineString(lineString.getCoordinateSequence());
		}

		// unioning the linestring with the point makes any self intersections explicit.
		Point point = lineString.getFactory().createPoint(lineString.getCoordinateN(0));
		Geometry toAdd = lineString.union(point); 

		//Add result to polygonizer
		polygonizer.add(toAdd);
	}

	/**
	 * Get a geometry from a collection of polygons.
	 * 
	 * @param polygons collection
	 * @param factory factory to generate MultiPolygon if required
	 * @return null if there were no polygons, the polygon if there was only one, or a MultiPolygon containing all polygons otherwise
	 */
	static Geometry toPolygonGeometry(Collection<Polygon> polygons, GeometryFactory factory){
		switch(polygons.size()){
		case 0:
			return null; // No valid polygons!
		case 1:
			return polygons.iterator().next(); // single polygon - no need to wrap
		default:
			//polygons may still overlap! Need to sym difference them
			Iterator<Polygon> iter = polygons.iterator();
			Geometry ret = iter.next();
			while(iter.hasNext()){
				ret = ret.symDifference(iter.next());
			}
			return ret;
		}
	}
}