package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.Group;
import javafx.scene.shape.Path;

import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.operation.buffer.BufferOp;

/**
 * esta clase tiene el objetivo de buscar los grupos por amount y por cercania
 * es decir: dado un conjunto de elementos, los agrupa de acuerdo al amount de los elementos y luego de acuerdo a su cercania
 * @author tomas
 *
 */
public class ProcessGroupsMapTask extends ProcessMapTask {
	String amountColumn;

	public ProcessGroupsMapTask(FileDataStore store, Group map,String amountColumn2) {
		this.store = store;
		super.map = map;
		amountColumn=amountColumn2;
	}

	public void doProcess() throws IOException {
		this.featureTree = new Quadtree();

		SimpleFeatureSource featureSource = store.getFeatureSource();

		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featuresIterator = featureCollection.features();

		//1 recorrer todos los elementos y obtener media y desvio estandar para separarlo en grupos

		constructJenksClasifier(featureCollection, amountColumn);
		
		Map<Integer,List<Geometry>> clases = 	new HashMap<Integer,List<Geometry>>();
		while (featuresIterator.hasNext()) {
			try{
				SimpleFeature simpleFeature = featuresIterator.next();
				Object gObj = (Geometry) simpleFeature.getDefaultGeometry();// cosehaItem.getGeometry();
				Geometry geometry = null;
				if(gObj instanceof Geometry){
					geometry= (Geometry)gObj;
					//System.out.println("agregando una geometria a las clases. es valida? "+geometry.isValid());//si
				}
				
				Double ammount =	(Double)simpleFeature.getAttribute(amountColumn);
				Integer clase = clasifier.classify(ammount);			
		
				List<Geometry> classGeom = clases.get(clase);
				
				if(classGeom == null){
					classGeom =	new ArrayList<Geometry>();		
				}
				classGeom.add(geometry);
				clases.put(clase,classGeom);
			}catch(Exception e){
				e.printStackTrace();

			}

		}// fin del while
		
		//XXX memoria o velocidad. ver si no es mejor primero clasificar todos los elementos en un array y despues hacer una union por cada grupo
//		Map<Integer,Geometry> clases = 	new HashMap<Integer,Geometry>();
//		while (featuresIterator.hasNext()) {
//			try{
//				SimpleFeature simpleFeature = featuresIterator.next();
//				Geometry geometry = (Geometry) simpleFeature.getDefaultGeometry();
//
//				Double ammount =	(Double)simpleFeature.getAttribute(amountColumn);
//				Integer clase = clasifier.classify(ammount);	
//				Geometry classGeom = clases.get(clase);
//				Geometry union=null;
//				//XXX memoria o velocidad. ver si no es mejor primero clasificar todos los elementos en un array y despues hacer una union por cada grupo
//				if(classGeom == null){				
//					union = geometry;
//
//				} else {
//					GeometryFactory fact=geometry.getFactory();
//					Geometry[] geomArray = {geometry,classGeom};
//					GeometryCollection polygonCollection = fact.createGeometryCollection(geomArray);				
//					union = polygonCollection.buffer(0); 
//				}
//				clases.put(clase, union);
//			}catch(Exception e){
//				e.printStackTrace();
//
//			}
//		}// fin del while

		this.pathTooltips.clear();
		clases.forEach((integer,geometries)->{
			Geometry g =geometries.get(0);//todas las geometrias que pertenecen a una clase
			GeometryFactory fact=g.getFactory();
		//	Geometry[] geomArray = (Geometry[]) geometries.toArray();
			Geometry[] geomArray = new Geometry[geometries.size()];
			geomArray = geometries.toArray(geomArray);
			GeometryCollection polygonCollection = fact.createGeometryCollection(geomArray);			
		//	Geometry union=	polygonCollection.isValid();
			//Geometry union = polygonCollection.buffer(1*ProyectionConstants.metersToLongLat); 
			Geometry union =null;
			
			//FIXME manejar todos los posibles tipos de geometrias y corregir los invalidos
			//Linestring / union /LineMerger/Poligonizer.getPolygons();
			if(polygonCollection.isValid()){
				System.out.println("valid "+polygonCollection);
				// union =polygonCollection.union(); 
				BufferOp bufOp = new BufferOp(polygonCollection);
				bufOp.setEndCapStyle(BufferOp.CAP_BUTT);
				union = bufOp.getResultGeometry(0);
			}else{
			//	union = polygonCollection.convexHull();//.getBoundary();
				System.out.println("invalid "+polygonCollection);
			//	union = polygonCollection.buffer(1*ProyectionConstants.metersToLongLat); 
				
//				BufferOp bufOp = new BufferOp(polygonCollection);
//				bufOp.setEndCapStyle(BufferOp.CAP_BUTT);
//				union = bufOp.getResultGeometry(1*ProyectionConstants.metersToLongLat);
				for(Geometry geom : geometries){
					if(union ==null){
						union =  geom;
					}else{
						if(geom instanceof Polygon){
							System.out.println("es una Polygon "+geom);
							union = union.union(geom);
						} else if(geom instanceof MultiPolygon){
							
							MultiPolygon mp = (MultiPolygon)geom;		 
						//	System.out.println("es una MultiPolygon "+mp);
							for(int i=0;i<mp.getNumGeometries();i++){
								Polygon p = (Polygon) (mp).getGeometryN(i);
								try{
									union = union.union(p);
								}catch(Exception e ){
									e.printStackTrace();
								}
							}
						}else if(geom instanceof GeometryCollection){
							GeometryCollection gc = (GeometryCollection)geom;		 
							System.out.println("es una geometryCollection "+gc);
							for(int i=0;i<gc.getNumGeometries();i++){
								Geometry p = (Geometry) (gc).getGeometryN(i);
								union = union.union(p);
							}
						}
					}
					
				}
			}
		
			
			if(union instanceof Polygon){
				pathTooltips.add(getPathTooltip((Polygon)union,integer));	
			} else if(union instanceof MultiPolygon){
				MultiPolygon mp = (MultiPolygon)union;		
				
//				pathTooltips.add(getPathTooltip((Polygon) (mp).getGeometryN(2),integer));	 
				
				for(int i=0;i<mp.getNumGeometries();i++){
					Polygon p = (Polygon) (mp).getGeometryN(i);
					pathTooltips.add(getPathTooltip(p,integer));	
				}
			}else if(union instanceof GeometryCollection){	
				System.out.println("hay que crear un pathTooltipo para el geometryCollection "+union);
				
			}
		});
		
		
//		clases.forEach((integer,geometry)->{
//			Geometry g =geometry;
//			if(g instanceof Polygon){
//				pathTooltips.add(getPathTooltip((Polygon)g,integer));	
//
//			} else if(g instanceof MultiPolygon){
//				MultiPolygon mp = (MultiPolygon)g;			
//				for(int i=0;i<mp.getNumGeometries();i++){
//					Polygon p = (Polygon) (mp).getGeometryN(i);
//					pathTooltips.add(getPathTooltip(p,integer));	
//				}
//
//			}	
//		});

		runLater();
		updateProgress(0, featureCount);
	}

	private ArrayList<Object> getPathTooltip(Polygon poly, Integer label) {
		Path path = getPathFromGeom(poly, label);
		path.setStrokeWidth(1);
		double area = poly.getArea() * ProyectionConstants.A_HAS;// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String(
				" Label: " + df.format(label) + "\n"
				);

		if (area < 1) {
			tooltipText = tooltipText.concat("Sup: "
					+ df.format(area * ProyectionConstants.METROS2_POR_HA)
					+ "m2\n");
		} else {
			tooltipText = tooltipText.concat("Sup: " + df.format(area)
					+ "Has\n");
		}

		ArrayList<Object> ret = new ArrayList<Object>();
		ret.add(path);
		ret.add(tooltipText);
		return ret;
	}

	protected int getAmountMin() {
		return 100;
	}

	protected int gerAmountMax() {
		return 500;
	}

}
