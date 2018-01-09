package dao;

import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.Data;

import java.util.Arrays;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Inheritance;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

@Data
@Entity @Access(AccessType.FIELD)//variable (el default depende de donde pongas el @Id)
//@Entity @Access(AccessType.PROPERTY)//getter
@Inheritance(strategy=javax.persistence.InheritanceType.TABLE_PER_CLASS)
public abstract class LaborItem implements Comparable<Object>{
	@javax.persistence.Id @GeneratedValue
	protected Double id=new Double(0);
	protected Geometry geometry=null;

	//solo es importante en las labores de puntos
	protected Double distancia =new Double(0);
	protected Double rumbo=new Double(0);
	protected Double ancho=new Double(0);

	protected Double elevacion=new Double(0);
	protected Integer categoria=new Integer(0);

	protected Double areaSinSup= new Double(0);

	public LaborItem() {
	}
	
	public LaborItem(SimpleFeature feature) {
		this.geometry = (Geometry) feature.getDefaultGeometry();
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
		//System.out.println("seteando una geometria en LaborItem "+geom);
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

	/**
	 * @return the distancia
	 */
	public Double getDistancia() {
		return distancia;
	}

	/**
	 * @param distancia the distancia to set
	 */
	public void setDistancia(Double distancia) {
		this.distancia = distancia;
	}

	/**
	 * @return the rumbo
	 */
	public Double getRumbo() {
		return rumbo;
	}

	/**
	 * @param rumbo the rumbo to set
	 */
	public void setRumbo(Double rumbo) {
		this.rumbo = rumbo;
	}

	/**
	 * @return the ancho
	 */
	public Double getAncho() {
		return ancho;
	}

	/**
	 * @param ancho the ancho to set
	 */
	public void setAncho(Double ancho) {
		this.ancho = ancho;
	}

	/**
	 * @return the elevacion
	 */
	public Double getElevacion() {
		return elevacion;
	}

	/**
	 * @param elevacion the elevacion to set
	 */
	public void setElevacion(Double elevacion) {
		if(elevacion !=null){
			this.elevacion = elevacion>1?elevacion:1;//esto es un hack para que no se rompa si la elevacion es ninguna. no puede ser cero
		}
	}

	public int compareTo(Object dao){
		if(dao !=null && LaborItem.class.isAssignableFrom(dao.getClass())){
			return id.compareTo(((LaborItem)dao).id);
		} else {
			return 0;
		}
		//return getAmount().compareTo(dao.getAmount());

	}

	public abstract Double getAmount();		

	public abstract Double getImporteHa();		

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
		if(o instanceof LaborItem){
			return compareTo((LaborItem)o) == 0;
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



	//abstract public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder);

	public  SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {

		Object[] basicElements = new Object[]{
				this.getGeometry(),
				distancia,
				rumbo,
				ancho,
				elevacion,
				getCategoria()};

		Object[] specialElements= getSpecialElementsArray();
		Object[] completeElements = new Object[basicElements.length+specialElements.length];
		for(int i =0;i<basicElements.length;i++){
			completeElements[i]=basicElements[i];
		}

		for(int i =0;i<specialElements.length;i++){
			completeElements[i+basicElements.length]=
					specialElements[i];
		}
		
		
		synchronized(featureBuilder){
			try{
				featureBuilder.addAll(completeElements);
			}catch(Exception e){
				e.printStackTrace();
			}

			//System.out.println("construyendo el simplefeature para el id:"+this.getId());//construuendo el simplefeature para el id:0.0
			SimpleFeature feature = featureBuilder.buildFeature("\\."+this.getId().intValue());
			return feature;
		}

	}

	//	public SimpleFeature getFeatureAsPoint(SimpleFeatureBuilder featureBuilder) { 
	//		this.geometry=this.geometry.getCentroid();
	//		return getFeature(featureBuilder);
	//	}

	/**
	 * devuelve un array con los elementos particulares de la subclase
	 * @return
	 */
	public abstract Object[] getSpecialElementsArray();

}
