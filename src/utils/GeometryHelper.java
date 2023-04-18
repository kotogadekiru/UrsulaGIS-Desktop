package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
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
	public static Poligono unirPoligonos(List<Poligono> pActivos) {
		
		StringJoiner joiner = new StringJoiner("-");
		//joiner.add(Messages.getString("JFXMain.poligonUnionNamePrefixText"));

		List<Geometry> gActivas = pActivos.stream().map(p->{
			//p.getLayer().setEnabled(false);
			joiner.add(p.getNombre());
			return p.toGeometry();
		}).collect(Collectors.toList());


		Geometry union = GeometryHelper.unirGeometrias(gActivas);

		double has = ProyectionConstants.A_HAS(union.getArea());

		Poligono poli = ExtraerPoligonosDeLaborTask.geometryToPoligono(union);
		poli.setArea(has);
		poli.setNombre(joiner.toString()); //$NON-NLS-1$
		return poli;
	}
	
	public static Geometry splineInterpolation(Geometry g) {
		List<Float> X = new ArrayList<Float>();
		List<Float> Y = new ArrayList<Float>();
		for(Coordinate c: g.getCoordinates()) {
			X.add(new Float(c.x));
			Y.add(new Float(c.y));
		}
		SplineInterpolator spline = SplineInterpolator.createMonotoneCubicSpline(X, Y);
		List<Coordinate> lerps = new ArrayList<Coordinate>();
		for(Float x: X) {
			Float y = spline.interpolate(x);
			Coordinate c = new Coordinate(x,y);
			X.add(new Float(c.x));
			Y.add(new Float(c.y));
			lerps.add(c);
		}
		Geometry ret = g.getFactory().createPolygon(lerps.toArray(new Coordinate[lerps.size()]));

		return ret;
	}
	
	/**
	 * replace a set of coordinates with a line segment while 
	 * the error is less than error
	 * the error is defined as the distances to the line divided by the line length
	 * @param g geometria a simplificar
	 * @param maxError error is defined as the distances to the line divided by the line length
	 * @return la geometria simplificada
	 */
	public static Geometry removeClosePoints(Geometry g, Double minDistance) {
		Geometry ret=null;
		
		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);
		boolean changed = false;
	//	do {
			changed = false;
		for(int i=1;i<boundCoords.length;i++) {	
			Coordinate c0 = vertices.get(vertices.size()-1);//last
			Coordinate c1 = boundCoords[i];			
		
			if(c0.distance(c1)>minDistance) {
				vertices.add(c1);
			} else {
				
				changed=true;
			}
		}
	//	boundCoords=vertices.toArray(new Coordinate[vertices.size()]);
	//	}while(changed);
		vertices.add(vertices.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}

	/**
	 * replace a set of coordinates with a line segment while 
	 * the error is less than error
	 * the error is defined as the distances to the line divided by the line length
	 * @param g geometria a simplificar
	 * @param maxError error is defined as the distances to the line divided by the line length
	 * @return la geometria simplificada
	 */
	public static Geometry removeSinglePoints(Geometry g, Double minDistance) {
		Geometry ret=null;
		
		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);	
	System.out.println("bounds size ="+boundCoords.length);
		for(int i=1;i<boundCoords.length-1;i++) {	//busco i+1 asi que esta bien cortar en length-1
			Coordinate c0 = vertices.get(vertices.size()-1);//last
			
			Coordinate c1 = boundCoords[i];
			System.out.println("i: "+i+" "+c1);
			Coordinate c2 = boundCoords[i+1];		
			if(c0.distance(c2)>minDistance) {
				System.out.println(" agregando "+ c1);
				vertices.add(c1);
			}
		}
		//vertices.add(boundCoords[boundCoords.length-1]);//el ultimo vertice siempre va. 
		//System.out.println("n-1="+vertices.get(vertices.size()-1));
		//aunque deberia ser el mismo que el primero
		vertices.add(vertices.get(0));//Cerrar el ciclo
		System.out.println("n="+vertices.get(vertices.size()-1));
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}
	
	/**
	 * replace a set of coordinates with a line segment while 
	 * the error is less than error
	 * the error is defined as the distances to the line divided by the line length
	 * @param g geometria a simplificar
	 * @param maxError error is defined as the distances to the line divided by the line length
	 * @return la geometria simplificada
	 */
	public static Geometry reduceAlignedPoints(Geometry g, Double maxError) {
		Geometry ret=null;
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);

		List<Coordinate> segmentCandidates = new ArrayList<Coordinate>();
	
		for(int i=1;i<boundCoords.length;i++) {	
			Coordinate c0 = vertices.get(vertices.size()-1);//last
			Coordinate c1 = boundCoords[i];			

			//TODO add c1 to segmentCandidates and check the condition else pop c1			

			Coordinate[] candidatesArr =segmentCandidates.toArray(new Coordinate[segmentCandidates.size()]);
			
			Coordinate[] refCoords =new Coordinate[]{c0,c1}; //segmento de referencia
			
			LineString ls= fact.createLineString(refCoords);
			Double distances =0.0;
			
			for(Coordinate c:candidatesArr) {				
				distances+=	ls.distance(fact.createPoint(c));
			}
			if(maxError>(distances/ls.getLength())) {
				segmentCandidates.add(c1);				
			} else { 
				vertices.add(segmentCandidates.get(segmentCandidates.size()-1));
				segmentCandidates.clear();
				segmentCandidates.add(c1);			
			}
		}
		vertices.add(vertices.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}
	
	/**
	 * 
	 * @param g geometria a simplificar
	 * @param ratio el error tiene que ser menor a r
	 * @return la geometria simplificada
	 */
	public static Geometry lerpIf(Geometry g, Double ratio) {
		Geometry ret=null;

		List<Coordinate> lerps = new ArrayList<Coordinate>();
		//TODO smooth edges in geometry
		Coordinate[] coords = g.getCoordinates();

		for(int i=0;i<coords.length;i++) {		//FIXME for mal formado	
			int c0Index =i-1>=0?i-1:coords.length-1; 
			Coordinate c0p = coords[c0Index];//i-1/3
			if(lerps.size()>0) {
				c0p=lerps.get(lerps.size()-1);
			}

			Coordinate c1 = coords[i];

			//c2 coordenada intermedia creada para que la derivada sea continua
			Coordinate c2 = new Coordinate();
			c2.x=c1.x+(c1.x-c0p.x)/3;
			c2.y=c1.y+(c1.y-c0p.y)/3;

			int c4Index =i+1<coords.length?i+1:0; 
			Coordinate c4 = coords[c4Index];

			int c5Index =i+2<coords.length?i+2:i+2-(coords.length); 
			Coordinate c5p = coords[c5Index];

			System.out.println("i:"+i+" ["+c0Index+","+i+","+c4Index+","+c5Index+"]");
			//c3 coordenada intemedia para que la derivada sea continua
			Coordinate c3 = new Coordinate();
			c3.x=c4.x-(c5p.x-c4.x)/3;
			c3.y=c4.y-(c5p.y-c4.y)/3;
			//TODO si los puntos ya estan alineados no interpolar
			Coordinate[] candidatesArr =new Coordinate[]{c0p,c1,c2,c3,c4,c5p};
			Coordinate[] refCoords =new Coordinate[]{c1,c4}; //segmento de referencia
			GeometryFactory fact = ProyectionConstants.getGeometryFactory();
			LineString ls= fact.createLineString(refCoords);
			Double distances =0.0;
			
			for(Coordinate c:candidatesArr) {				
				distances+=	ls.distance(fact.createPoint(c));
			}
			if(ratio>(distances/ls.getLength())) {
				lerps.add(c1);
			} else { 
				List<Coordinate> candidates = new ArrayList<Coordinate>();
				//t es la coordenada del punto intermedio
				for(double t=0 ;t<=5;t++) {                      
					candidates.add(cubicLerp(c1, c2, c3, c4, t/5));
				}
				//TODO check candidates for ratio of error or input the original values

				distances =0.0;
				for(Coordinate c:candidates) {
					distances+=	ls.distance(fact.createPoint(c));
				}
				if(ratio>(distances/ls.getLength())) {
					System.out.println("agregando lerps");
					lerps.addAll(candidates);
				} else { 
					System.out.println("el error es muy grande no interpolando. r = "+distances/ls.getLength());
					lerps.add(c1);
				}
			}
			//c1=lerps.get(lerps.size()-1);
			//lerps.add(c3);

		}
		lerps.add(lerps.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(lerps.toArray(new Coordinate[lerps.size()]));

		return ret;
	}

	public static Geometry simplificarContorno(Geometry g) {
		g=g.buffer(ProyectionConstants.metersToLongLat(10));
//		g=GeometryHelper.removeClosePoints(g, ProyectionConstants.metersToLongLat(2));
//		g=GeometryHelper.removeSinglePoints(g, ProyectionConstants.metersToLongLat(2));
//		g=GeometryHelper.reduceAlignedPoints(g, 0.2);
		return g;
	}
	public static Geometry smooth(Geometry g) {
		Geometry ret=null;
		//TODO remove duplicate vertices
		//TODO usar las coordenadas de la geometria como puntos de control para popular los puntos intermedios
		//de una spline 
		//TODO populate with vertices every X distance


		List<Coordinate> lerps = new ArrayList<Coordinate>();
		//TODO smooth edges in geometry
		Coordinate[] coords = g.getCoordinates();

		for(int i=0;i<coords.length;i++) {//FIXME for mal formado
			Coordinate c1 = coords[i];
			int c0Index =i-1>=0?i-1:coords.length-1; 
			Coordinate c0p = coords[c0Index];//i-1/3
			if(lerps.size()>0) {
				c0p=lerps.get(lerps.size()-1);
			}
			Coordinate c2 = new Coordinate();
			c2.x=c1.x+(c1.x-c0p.x)/3;
			c2.y=c1.y+(c1.y-c0p.y)/3;

			int c4Index =i+1<coords.length?i+1:0; 
			Coordinate c4 = coords[c4Index];

			int c5Index =i+2<coords.length?i+2:i+2-(coords.length); 
			Coordinate c5p = coords[c5Index];

			System.out.println("i:"+i+" ["+c0Index+","+i+","+c4Index+","+c5Index+"]");
			Coordinate c3 = new Coordinate();
			c3.x=c4.x-(c5p.x-c4.x)/3;
			c3.y=c4.y-(c5p.y-c4.y)/3;

			//ProyectionConstants.setLatitudCalculo(c1.y);
			//double d10 = ProyectionConstants.metersToLongLat(10);
			//double dist = c1.distance(c2)+c2.distance(c3);
			//lerps.add(c1);
			for(double t=0 ;t<=5;t++) {
				//t=Math.min(t, 1);
				//lerps.add(lerp(c1,c2,c3,t/5));
				lerps.add(cubicLerp(c1, c2, c3, c4, t/5));
			}
			c1=lerps.get(lerps.size()-1);
			//lerps.add(c3);

		}
		lerps.add(lerps.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(lerps.toArray(new Coordinate[lerps.size()]));

		return ret;
	}


	public static Coordinate cubicLerp(Coordinate c1,Coordinate c2,Coordinate c3,Coordinate c4,double t) {
		double P0x=c1.x,P1x=c2.x,P2x=c3.x,P3x=c4.x;
		double P0y=c1.y,P1y=c2.y,P2y=c3.y,P3y=c4.y;
		Coordinate ret = new Coordinate();
		//		ret.x=P0x
		//				+t*(-3*P0x+3*P1x)
		//				+t*t*(3*P0x-6*P1x+3*P2x)
		//				+t*t*t*(-P0x+3*P1x-3*P2x+P3x);

		ret.x=P0x
				+t*3*(-P0x+P1x)
				+t*t*(3*P0x-6*P1x+3*P2x)
				+t*t*t*(-P0x+3*P1x-3*P2x+P3x);
		ret.y=P0y
				+t*(-3*P0y+3*P1y)
				+t*t*(3*P0y-6*P1y+3*P2y)
				+t*t*t*(-P0y+3*P1y-3*P2y+P3y);
		return ret;
	}

	public static Coordinate lerp(Coordinate c1,Coordinate c2,Coordinate c3,double t) {
		Coordinate c12 =lerp(c1,c2,t);
		Coordinate c23 =lerp(c2,c3,t);
		return lerp(c12,c23,t);
	}
	public static Coordinate lerp(Coordinate c1,Coordinate c2,double t) {
		//TODO sumarle a c1 un porcentaje t de su diferencia con c2
		double deltaX = c2.x-c1.x;
		double deltaY = c2.y-c1.y;
		return new Coordinate(c1.x+deltaX*t,c1.y+deltaY*t);
	}

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
