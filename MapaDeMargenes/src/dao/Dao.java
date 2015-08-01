package dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public abstract class Dao implements Comparable<Dao>{
	
	private Geometry geometry;
	private Double areaSinSup= new Double(0);
	
	
	public Dao(SimpleFeature feature) {
		this.geometry = (Geometry) feature.getDefaultGeometry();
		
		//todo esto para evitar que sea multipoligons esta de mas. tiene que haber una mejor solucion.
//		List<Polygon> polygons = this.getPolygons(this.geometry);
//		if(polygons.size()>0){
//			this.geometry = polygons.get(0);
//		} else { 
//			System.out.println("no pude poligonizar la geometria "+ geometry);
//		}
	}
	
	public Dao() {
		
	}
	
	public void setGeometry(Geometry geom) {
		if(geom instanceof Point){
			Point p = (Point) geom;
			Coordinate c=p.getCoordinate();
			if(c.x==0&&c.y==0){
				System.out.println("seteando una geometria POINT 0,0 "+geom);
				return;
			}
		}
		this.geometry = geom;
		
	}
	public  Geometry getGeometry(){
		return geometry;
	}
	public String getColumn(String key){
		String column = getColumnsMap().getOrDefault(key, key);
	//	System.out.println("returning column "+column+ " for key "+key);
		return column;
	}
	
	public int compareTo(Dao dao){
		return getAmount().compareTo(dao.getAmount());
		
	}
			
	public abstract Double getAmount();		
	protected abstract Map<String, String> getColumnsMap();
	
	
	protected Double getDoubleFromObj(Object o){
		Double d = new Double(0); 
		if(o == null){
			// Do nothing.
			System.out.println("devolviendo 0");
		} else if(o instanceof Double){
			 d = (Double) o;
		 } else  if(o instanceof Integer){
			 d = new Double((Integer) o);
		 } else  if(o instanceof Long){
			 d = new Double((Long) o);
		 } else if(o instanceof String){
			d = new Double((String) o);
		 }else{
			 System.err.println("no se pudo leer la cantidad de " +o);//no se pudo leer la cantidad de L3:CARG0003
//			 try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		 }
		 return d;
	}
	
	
	
	public Double getAreaSinSup() {
		return areaSinSup;
	}

	public void setAreaSinSup(Double areaSinSup) {
		this.areaSinSup = areaSinSup;
	}

	/**
	 * 
	 * @param geometry
	 * @return lista de poligonos contenida en la geometria
	 */
	protected List<Polygon> getPolygons(Geometry geometry){
		List<Polygon> polygons = new ArrayList<Polygon>();
//		Object geometry = dao.getGeometry();

		if (geometry instanceof MultiPolygon) {		
			MultiPolygon mp = (MultiPolygon) geometry;
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry g = mp.getGeometryN(i);
				if(g instanceof Polygon){
					polygons.add((Polygon) g);
				}				
			}

		} else if (geometry instanceof Polygon) {
			polygons.add((Polygon) geometry);
		} else  if (geometry instanceof Point) {
			System.out.println("tratando de obtener los poligonos contenidos en un punto. no se puede y devuelvo null "+geometry);
			polygons = null;
		}
//System.out.println("devolviendo los polygons "+polygons);
		return polygons;
	}

	abstract public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder);
	
}
