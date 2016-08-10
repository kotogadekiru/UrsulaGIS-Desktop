package dao;

import java.util.Map;

import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public abstract class FeatureContainer implements Comparable<FeatureContainer>{
	Double id=new Double(0);
	Integer categoria=new Integer(0);
	
	private Geometry geometry;
	private Double areaSinSup= new Double(0);


	public FeatureContainer(SimpleFeature feature) {
		this.geometry = (Geometry) feature.getDefaultGeometry();
	}

	public FeatureContainer() {

	}

	public void setGeometry(Geometry geom) {
		if(geom instanceof Point){
			Point p = (Point) geom;
			Coordinate c=p.getCoordinate();
			if(c.x==0 &&c.y==0){
				System.out.println("seteando una geometria POINT 0,0 "+geom);
				return;
			}
		}
		this.geometry = geom;

	}

	public  Geometry getGeometry(){
		return geometry;
	}


	public Integer getCategoria() {
		return categoria;
	}



	public void setCategoria(Integer categoria) {
		this.categoria = categoria;
	}
	
	public int compareTo(FeatureContainer dao){
		return getAmount().compareTo(dao.getAmount());

	}

	public abstract Double getAmount();		

	public Double getId() {
		return id;
	}
	
	public void setId(Double _id) {
		 id=_id;
	}
	//protected abstract Map<String, String> getColumnsMap();


	public static String getID(SimpleFeature harvestFeature) {
		String identifier = harvestFeature.getIdentifier().getID();
		String[] split = identifier.split("\\.");
		if (split.length > 1) {
			return split[split.length - 1];
		}
		
		return "0.0";
	}
	
	public static Double getDoubleFromObj(Object o){
		
		Double d = new Double(0); 
		if(o instanceof Double){
			d = (Double) o;
		} else  if(o instanceof Integer){
			d = new Double((Integer) o);
		} else  if(o instanceof Long){
			d = new Double((Long) o);
		} else if(o instanceof String){
			StringConverter<Number> converter = new NumberStringConverter();
			
			try{
				d=converter.fromString((String) o).doubleValue();
			//	d = new Double((String) o);
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			 System.err.println("no se pudo leer la cantidad de " +o);//no se pudo leer la cantidad de L3:CARG0003

		}
		return d;
	}
	@Override
	public boolean equals(Object o){
		if(o instanceof FeatureContainer){
			return compareTo((FeatureContainer)o) == 0;
		} else{
			return false;
		}
	}

	@Override public int hashCode() {
		int result = this.getAmount().intValue();

		return result;
	}



	public Double getAreaSinSup() {
		return areaSinSup;
	}

	public void setAreaSinSup(Double areaSinSup) {
		this.areaSinSup = areaSinSup;
	}



	abstract public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder);

}
