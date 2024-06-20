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
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import dao.Labor;
import dao.Poligono;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.measure.MeasureTool;
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

		Poligono poli = constructPoligono(union);//ExtraerPoligonosDeLaborTask.geometryToPoligono(union);
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

	public static Geometry createCircle(Point c,Point c2) {	
		Double radius = ProyectionConstants.getDistancia(c, c2);
		return createCircle (c,radius);
	}

	public static Geometry createCircle(Point c,double radius) {
		System.out.println("creando un circulo con radio "+radius);
		double latRadius = ProyectionConstants.metersToLat()*radius;

		double fact = ProyectionConstants.metersToLat()/ProyectionConstants.metersToLong();

		GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
		shapeFactory.setNumPoints(64); // adjustable
		shapeFactory.setCentre(c.getCoordinate());
		// Length in meters of 1° of latitude = always 111.32 km
		shapeFactory.setHeight(2*latRadius);//diameterInMeters/111320d);

		double longRadius = latRadius/fact;
		// Length in meters of 1° of longitude = 40075 km * cos( latitude ) / 360
		shapeFactory.setWidth(2*longRadius);//diameterInMeters / (40075000 * Math.cos(Math.toRadians(latitude)) / 360));

		Polygon circle = shapeFactory.createEllipse();

		return circle;//c.buffer(radius);//esto me genera una elipse
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

	/**
	 * 
	 * @param deltaX cateto X
	 * @param deltaY cateto Y
	 * @return devuelve el angulo en grados; null si no se puede calcular
	 */
	public static Double getAzimuth(double deltaX,double deltaY) {
		Double rumbo = null;
		if(deltaX==0) {
			if(deltaY>0) {
				return 0.0;
			} else if (deltaY==0) {
				return null;//no hay angulo para 0,0
			}else {return 180.0;}
		}
		double tan = deltaY/deltaX;//+Math.PI/2;
		rumbo = Math.atan(tan);
		rumbo = Math.toDegrees(rumbo);//como esto me da entre -90 y 90 le sumo 90 para que me de entre 0 180

		rumbo=90-rumbo;
		return rumbo<0?rumbo+360:rumbo;
		//return rumbo;
	}


	public static Poligono constructPoligono(Geometry g) {
		//ExtraerPoligonosDeLaborTask.geometryToPoligono((Geometry)g);
		System.out.println("convirtiendo geometria a poligono "+g);		
		List<Position> positions = new ArrayList<Position>();		
			
		if(g instanceof Polygon) {
			Polygon pol =(Polygon)g;
			System.out.println("es polygon");
			
			Coordinate[] coords = pol.getExteriorRing().getCoordinates();
			for(int i=0;i<coords.length;i++) {
				Coordinate c = coords[i];
				positions.add(Position.fromDegrees(c.y, c.x));
			}
			positions.add(positions.get(0));
			
			
			for(int r=0;r<pol.getNumInteriorRing();r++) {
				List<Position> hole =new ArrayList<Position>();	
				LineString ring = pol.getInteriorRingN(r);
				Coordinate[] ringCoords = ring.reverse().getCoordinates();
				for(int i=0;i<ringCoords.length;i++) {
					Coordinate c = ringCoords[i];
					hole.add(Position.fromDegrees(c.y, c.x));
				}
				//hole.add(hole.get(0));
				insertHole(positions,hole);
			}			
		}

		
		Poligono p = new Poligono();
		p.setPositions(positions);		
		p.setArea(GeometryHelper.getHas(g));
		return p;
	}
	
	public static Double distance(Position p1,Position p2) {		
		return Position.linearDistance(p1, p2).degrees;
	}
	public static void insertHole(List<Position> ring,List<Position> hole) {
		//TODO encontrar los puntos pas cercanos e insertar hole en outerRing
		Position minR=null,minH=null;
		int minI=-1,minJ=-1;
		Double minDist =null; 
		for(int i=0;i<ring.size();i++) {
			Position r=ring.get(i);
			for(int j=0;j<hole.size();j++) {
				Position h =hole.get(j);			
				Double dist = distance(r,h);
				if(minDist==null || minDist>dist) {
					minDist = dist;
					minI=i;
					minJ=j;
					minR=r;
					minH=h;
				}
			}
		}		
		//TODO insertar en minI hole empezando por minJ
		List<Position> sortedHole = new ArrayList<Position>();
		for(int h = 0; h<hole.size();h++) {
			Position hPos = hole.get((h+minJ)%(hole.size()));
			sortedHole.add(h,hPos);
			//ring.add(minI+h, hPos);
		}
		sortedHole.add(sortedHole.get(0));
		sortedHole.add(0,minR);
		ring.addAll(minI,sortedHole);
		ring.add(minI+sortedHole.size(),minR);
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
	//FIXME check thread safety
	public static Geometry getIntersection(Geometry g1, Geometry g2){
		if(g1==null || g2 ==null) {
			System.err.println("antes de validar geometrias devolviendo null porque una de las geometrias a intersectar es null. g1= "+g1+",g2= "+g2);
			return null;
		}
		g1 = PolygonValidator.validate(g1);
		g2 = PolygonValidator.validate(g2);
		Geometry intersection = null;
		if(g1==null || g2 ==null) {
			System.err.println("devolviendo null porque una de las geometrias a intersectar es null. g1= "+g1+",g2= "+g2);
			return null;
		}
		if (g1 != null && g2!=null && g1.intersects(g2)){
			try {			
				//intersection= EnhancedPrecisionOp.intersection(g1,g2);
				intersection = g1.intersection(g2);// Computes a Geometry//found non-noded intersection between LINESTRING ( -61.9893807883
				intersection = PolygonValidator.validate(intersection);

			} catch (Exception te) {
				System.err.println("error al hacer la interseccion de las geometrias "+g1+", "+g2);
				System.err.println("EnhancedPrecisionOp.intersection");
				te.printStackTrace();
				try{
					intersection = EnhancedPrecisionOp.intersection(g1, g2);
				}catch(Exception e){
					e.printStackTrace();
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
	 * @param aIntersectar: Lista de geometrias a intersectar 
	 * @return el Set de las partes de las geometrias intersectadas
	 */
	public static Set<Geometry> obtenerIntersecciones(List<Geometry> aIntersectar){
		//	import org.locationtech.jts.densify.Densifier;

		// unir todas las geometrias.
		//crear un poligono con los exterior rings de todas las geometrias
		//obtener la diferencia entre la union y los vertices

		Geometry[] boundaryArr =  aIntersectar.stream().map(g->g.getBoundary()).toArray(s->new Geometry[s]);

		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		GeometryCollection boundarysCol = fact.createGeometryCollection(boundaryArr);
		Double buffer25=ProyectionConstants.metersToLongLat(0.25);
		Geometry boundary_buffer = boundarysCol.buffer(buffer25,1,BufferParameters.CAP_FLAT);

		GeometryCollection colectionCat = fact.createGeometryCollection(
				aIntersectar.toArray(new Geometry[aIntersectar.size()]));
		//(buffer0,1,BufferParameters.CAP_SQUARE);
		Double buffer0=ProyectionConstants.metersToLongLat(0);
		Geometry convexHull = colectionCat.buffer(buffer0,1,BufferParameters.CAP_FLAT);

		Geometry diff = convexHull.difference(boundary_buffer);
		Set<Geometry> geometriasOutput = new HashSet<Geometry>();
		//double tolerance = ProyectionConstants.metersToLongLat(1);
		double bufferWidth = 2*buffer25;
		Double buffer30=ProyectionConstants.metersToLongLat(0.28);
		for(int n = 0; n < diff.getNumGeometries(); n++){
			Geometry g = diff.getGeometryN(n);
			//XXX al hacer el buffer se crean puntitos en las esquinas. las descarto
			if(g.getArea()<bufferWidth*bufferWidth) {
				continue;
			}

			g=g.buffer(buffer30,1,BufferParameters.CAP_FLAT);//mitad del buffer esta en esta y mitad en la otra
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
	public static GeometryCollection toGeometryCollection(List<Geometry> list) {
		list = list.stream().filter(g->g!=null).collect(Collectors.toList());
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Geometry[] array =list.toArray(new Geometry[list.size()]);
		GeometryCollection collection = fact.createGeometryCollection(array );
		return collection;
	}

	public static Geometry unirCascading(Labor<?> aUnir,Envelope bounds) {
		try {
		List<Geometry> boundsGeoms = new ArrayList<Geometry>();
		//System.out.println("bounds area = "+ProyectionConstants.A_HAS(bounds.getArea()));
		if(ProyectionConstants.A_HAS(bounds.getArea())>1) {
			//si es mayor a 100m2 divido en 4
			List<Envelope> envelopes = splitEnvelope(bounds);
			//System.out.println("split "+bounds);
		//	int i=0;
			for(Envelope e:envelopes) {
			//	System.out.println("envelope "+i+": "+e);
			//	i++;
				Geometry eGeom = unirCascading(aUnir,e);
				if(eGeom !=null) {
					boundsGeoms.add(eGeom);
				} 
//				else {
//					System.out.println("eGeom es null");
//				}
			}
			//System.out.println("fin split "+bounds);
			//System.out.println("geoms "+boundsGeoms.size());
		} else {
			boundsGeoms.addAll( aUnir.cachedOutStoreQuery(bounds).stream()
					.map(i->i.getGeometry())					
					.collect(Collectors.toList()));			 
		}

		
		//System.out.println("juntando "+boundsGeoms.size()+" geoms");
		Geometry union = null; 
				//toGeometryCollection(boundsGeoms).buffer(buffer,1,BufferParameters.CAP_FLAT);//buffer the collection
		try {
			union = unirGeometrias(boundsGeoms);
//		for(Geometry g : boundsGeoms) {
//			if(union==null) {
//				union=g;
//			}else {
//				
//				union = union.union(g);// This method does not support GeometryCollection arguments
//			
//			}
//		}
		}catch(Exception e ) {
			Double buffer = ProyectionConstants.metersToLongLat(0.25);
			union = toGeometryCollection(boundsGeoms).buffer(buffer,1,BufferParameters.CAP_FLAT);//buffer the collection
			e.printStackTrace();
		}
		return union;
		}catch(Exception e ) {
			e.printStackTrace();
			return null;
			}	
		}



	public static List<Envelope> splitEnvelope(Envelope e){
		List<Envelope> result = new ArrayList<Envelope>();
		double ancho = e.getWidth()/2;
		double alto = e.getHeight()/2;
		for(double x = e.getMinX(); x <= e.getMaxX()-ancho; x+=ancho) {
			for(double y = e.getMinY(); y <= e.getMaxY()-alto; y+=alto) {			
				result.add(new Envelope(x,x+ancho,y,y+ancho));
			}
		}
		return result;
	}

	public static void main(String [] args) {
		Envelope e = new Envelope(0,10,0,10);
		List<Envelope> envelopes = splitEnvelope(e);
		//Arrays.toString
		//	String s = Arrays.asList(j).stream().collect(Collectors.joining("</td><td>","<td>","</td>"));
		System.out.println("envelopes created = "+envelopes);
	}

	public static Geometry unirGeometrias(List<Geometry> aUnir) {
		try {
			List<Geometry> aUnird = aUnir.parallelStream().filter(g->g!=null&&!g.isEmpty()).map(g->{
				try {
					if(!g.isEmpty()) {
				Densifier densifier = new Densifier(g);
				densifier.setDistanceTolerance(ProyectionConstants.metersToLongLat(10));
				g=densifier.getResultGeometry();//java.lang.ArrayIndexOutOfBoundsException: -1
					}
				}catch(Exception e) {
					System.err.println("fallo densifier con "+g);
					//e.printStackTrace();
				}
				return  g;			
			}).collect(Collectors.toList());

			//		GeometryFactory fact = ProyectionConstants.getGeometryFactory();		
			//		Geometry[] geomArray = aUnir.toArray(new Geometry[aUnir.size()]);//put into an array
			//		GeometryCollection collection = fact.createGeometryCollection(geomArray);//create a collection
			GeometryCollection collection = toGeometryCollection(aUnird);
			Double buffer = ProyectionConstants.metersToLongLat(0.25);

			Geometry union =collection.buffer(buffer,1,BufferParameters.CAP_FLAT);//buffer the collection
			Geometry boundary = union.getBoundary().buffer(buffer,1,BufferParameters.CAP_FLAT);
			Geometry dif=union.difference(boundary);
			dif = PolygonValidator.validate(dif);
			if(dif.isValid()) {
				//System.out.println("dif es valid");
				return dif;
			}else {
				//System.out.println("dif no es valid");
				return union;
			}
			//buffered = CascadedPolygonUnion.union(geometriesCat);
			//Geometry union = collection.buffer(0);//ProyectionConstants.metersToLongLat(20));
			//TODO hacer un buffer 0.25
			//y despues hacer un dif contra el boundary buffer 0.25 para evitar que crezcan los items
			//System.out.println("geometria densa unida "+union);
			//return dif;
		}catch(Exception e) {
			System.err.println("fallo collection buffer uniendo de a una "+aUnir);
			//e.printStackTrace();
			Geometry union=null;
			for(Geometry g:aUnir) {
				if(union==null) {
					union=g;
				}else {
					try {
						union=union.union(g);
					}catch(Exception e2) {
						e2.printStackTrace();
					}
				}
			}
			return union;
		}
	}

	/**
	 * metodo que se llama al construir la grilla en correlacionarLayers
	 * @param labor
	 * @return
	 */
	public static synchronized Geometry extractContornoGeometry(Labor<?> labor) {
		try{					
			ReferencedEnvelope bounds = labor.outCollection.getBounds();
			Geometry cascadedUnion = unirCascading(labor,bounds);
			return cascadedUnion;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * metodo llamado en labor.getContorno
	 * @param labor
	 */
//	public static void extractContorno(Labor<?> labor) {
//		//TODO compute contorno
//		//		List<Geometry> geometriesCat = new ArrayList<Geometry>();
//		//		SimpleFeatureIterator it = labor.outCollection.features();
//
//
//
//		//		while(it.hasNext()){
//		//			SimpleFeature f=it.next();
//		//			geometriesCat.add((Geometry)f.getDefaultGeometry());
//		//		}
//		//		it.close();		
//
//		try{					
//			ReferencedEnvelope bounds = labor.outCollection.getBounds();
//			//System.out.println("outCollectionBounds "+bounds);
//			Geometry cascadedUnion = unirCascading(labor,bounds);
//			//Geometry buffered = GeometryHelper.unirGeometrias(geometriesCat);
//			//CascadedPolygonUnion.union(geometriesCat);
//			//sino le pongo buffer al resumir geometrias me quedan rectangulos medianos
//			//				buffered = buffered.buffer(
//			//						ProyectionConstants.metersToLongLat(0.25),
//			//						1,BufferParameters.CAP_SQUARE);
//			//buffered =GeometryHelper.simplificarContorno(buffered);
//			Poligono contorno =GeometryHelper.constructPoligono(cascadedUnion);
//			//	simplificarPoligono(contorno);
//			contorno.setNombre(labor.getNombre());
//			//labor.setContorno(contorno);
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//	}

	public static void simplificarPoligono(Poligono p) {
		Geometry g = p.toGeometry();
		//g=g.buffer(ProyectionConstants.metersToLongLat(10));

		//TODO remover un punto si el area que forma el triangulo con sus vecinos es suficientemente pequenia
		g=GeometryHelper.removeSmallTriangles(g, (0.005)/ProyectionConstants.A_HAS());

		g=GeometryHelper.douglassPeuckerSimplify(g,ProyectionConstants.metersToLongLat(5));
		//g=GeometryHelper.removeClosePoints(g, ProyectionConstants.metersToLongLat(2));
		//g=GeometryHelper.removeSinglePoints(g, ProyectionConstants.metersToLongLat(2));
		//g=GeometryHelper.reduceAlignedPoints(g, 0.2);
		Poligono pol =GeometryHelper.constructPoligono(g);
		if(p.getLayer()!=null) {
			MeasureTool measureTool = (MeasureTool) p.getLayer().getValue(PoligonLayerFactory.MEASURE_TOOL);
			measureTool.setPositions((ArrayList<? extends Position>) pol.getPositions());
		}
	}

	public static Geometry removeSmallTriangles(Geometry g, double minLongLatArea) {
		Geometry ret=null;
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Coordinate[] boundCoords = g.getCoordinates();
		List<Coordinate> vertices = new ArrayList<Coordinate>();
		vertices.add(boundCoords[0]);
		boolean changed = false;

		changed = false;
		//double minTriangleHas = ProyectionConstants.A_HAS(minLongLatArea);
		//System.out.println("minTriangleHas ="+minTriangleHas);
		//System.out.println("bounds length "+boundCoords.length);
		for(int i=1;i<boundCoords.length-1;i++) {	
			Coordinate c0 = vertices.get(vertices.size()-1);//last
			ProyectionConstants.setLatitudCalculo(c0.y);
			Coordinate c1 = boundCoords[i];
			Coordinate c2 = boundCoords[i+1];
			Coordinate[] tCoords = {c0,c1,c2,c0};
			try {
				Geometry triangle = fact.createPolygon(tCoords);//.createPolygon({c0,c1,c2});
				//double triangleArea = triangle.getArea();
				//double triangleHas = ProyectionConstants.A_HAS(triangleArea);
				//System.out.println("triangleHas ="+triangleHas);
				if(triangle.getArea()>minLongLatArea) {
					vertices.add(c1);
				} else {

					changed=true;
				}
			}catch(Exception e) {
				vertices.add(c1);
				e.printStackTrace();
			}

		}

		vertices.add(vertices.get(0));//Cerrar el ciclo
		ret = g.getFactory().createPolygon(vertices.toArray(new Coordinate[vertices.size()]));

		return ret;
	}

	/**
	 * The Douglas-Peucker algorithm uses a point-to-edge distance tolerance. 
	 * The algorithm starts with a crude simplification that is the single edge joining the 
	 * first and last vertices of the original polyline. 
	 * It then computes the distance of all intermediate vertices to that edge.
	 * The vertex that is furthest away from that edge,
	 * and that has a computed distance that is larger than a specified tolerance,
	 * will be marked as a key and added to the simplification.
	 * This process will recurse for each edge in the current simplification, 
	 * until all vertices of the original polyline are within tolerance of the
	 * simplification results.
	 * @param g
	 * @return simplified geometry
	 */
	public static Geometry douglassPeuckerSimplify(Geometry g) {
		//org.locationtech.jts.simplify.
		//DouglasPeuckerSimplifier simp;
		return DouglasPeuckerSimplifier.simplify(g, 0.000001);

	}

	/**
	 * 
	 * @param g geometria a simplificar
	 * @param tolerance distancia en grados
	 * @return geometria simplificada
	 */
	public static Geometry douglassPeuckerSimplify(Geometry g,double tolerance) {
		//org.locationtech.jts.simplify.
		//DouglasPeuckerSimplifier simp;
		return DouglasPeuckerSimplifier.simplify(g, tolerance);

	}


}
