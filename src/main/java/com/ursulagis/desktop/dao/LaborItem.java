package com.ursulagis.desktop.dao;

import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ursulagis.desktop.utils.ProyectionConstants;

import java.util.Arrays;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Inheritance;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.gui.Messages;


@Data
//@EqualsAndHashCode(callSuper=true)//si no pones esto todos los hashmaps andan mal y grillar cosecha no anda
@Entity @Access(AccessType.FIELD)//variable (el default depende de donde pongas el @Id)
//@Entity @Access(AccessType.PROPERTY)//getter
@Inheritance(strategy=javax.persistence.InheritanceType.TABLE_PER_CLASS)
public abstract class LaborItem implements Comparable<Object>{
	@javax.persistence.Id @GeneratedValue
	protected Double id=Double.valueOf(-1);
	protected Geometry geometry=null;

	//solo es importante en las labores de puntos
	protected Double distancia =0.0;
	protected Double rumbo=0.0;
	protected Double ancho=0.0;

	protected Double elevacion=0.0;//ojo que a veces elev 0 jode sobre todo en AnalyticLayer
	protected Integer categoria=Integer.valueOf(0);

	protected Double areaSinSup= 0.0;

	protected String observaciones=new String("default obs");
	public Labor<? extends LaborItem> labor=null;
	public LaborItem() {
	}
	
	public LaborItem(SimpleFeature feature) {
		setGeometry( (Geometry) feature.getDefaultGeometry());
	}

	public LaborItem(LaborItem i) {
		setId(i.getId());
		setGeometry( i.getGeometry());
		setAncho(i.getAncho());
		setDistancia(i.getDistancia());
		setElevacion(i.getElevacion());
		setRumbo(i.getRumbo());
		setCategoria(i.getCategoria());
		setObservaciones(i.getObservaciones());
		setLabor(i.getLabor());
		setAreaSinSup(i.getAreaSinSup());
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
	public abstract void setAmount(Double amount);	

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
			String id = split[split.length - 1];
			if(id==null)id="0.0";
			return id;
		}

		return "0.0";
	}

	public static Double getDoubleFromObj(Object o){
		Double d = 0.0; 
		try {
		if(o instanceof Double){
			d = (Double) o;
		} else  if(o instanceof Integer){
			d = Double.valueOf((Integer) o);
		} else  if(o instanceof Long){
			
			d = Double.valueOf((Long) o);
		} else if(o instanceof String){			
//			try {
//				d = Messages.getNumberFormat().parse((String) o).doubleValue();
//			}catch(Exception e) {
//				e.printStackTrace();
//				System.out.println("returning 0 for "+o);
//			}
			StringConverter<Number> converter = new NumberStringConverter(Messages.getLocale());
			try{				
				d=converter.fromString((String) o).doubleValue();
				//	d = Double.parseDouble((String) o);
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("returning 0 for " + o);
			}
		}else{
			//es por que estoy leyendo una columna que no existe en ese feature. como ancho en una prescripcion.
		//	System.err.println("no se pudo leer la cantidad de " +o);//no se pudo leer la cantidad de L3:CARG0003

		}
		}catch(Exception e) {
			e.printStackTrace();
		}
//		if(0.0==d) {
//			System.out.println(o+" es cero");
//		}
		return d;
	}

//	@Override
//	public boolean equals(Object o){
//		if(o instanceof LaborItem){
//			return compareTo((LaborItem)o) == 0;
//		} else{
//			return false;
//		}
//	}
//
//	@Override public int hashCode() {
//		int result = this.getAmount().intValue();
//
//		return result;
//	}



	public Double getAreaSinSup() {
		if(areaSinSup.equals(0.0)) {
			areaSinSup = ProyectionConstants.A_HAS(this.geometry.getArea());
		}
		return areaSinSup;
	}

	public void setAreaSinSup(Double areaSinSup) {
		this.areaSinSup = areaSinSup;
	}



	//abstract public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder);
/**
 * metodo llamado para convertir un simple LaborItem a un SimpleFeature para ser insertado en el data store
 * @param featureBuilder
 * @return SimpleFeature representando este LaborItem
 */
	public  SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {

		Object[] basicElements = new Object[]{
				this.getGeometry(),//0
				distancia,//1
				rumbo,//2
				ancho,//3
				elevacion,//3
				getCategoria(),//5
				getObservaciones()//6
				};//7 elements

		Object[] specialElements= getSpecialElementsArray();
		//System.out.println("creando un array para "+basicElements.length+specialElements.length);//creando un array para 711
		Object[] completeElements = new Object[basicElements.length+specialElements.length];
		for(int i =0;i<basicElements.length;i++){// de 0 a 6 ; 7 elementos
			completeElements[i]=basicElements[i];
		}

		for(int i=0;i<specialElements.length;i++){
			completeElements[i+basicElements.length]=//7+i
					specialElements[i];
		}		
		
		SimpleFeature feature =null;
		synchronized(featureBuilder){
			try{
				//featureBuilder.addAll(completeElements);
				//feature = featureBuilder.buildFeature("\\."+this.getId().intValue());
				feature = featureBuilder.buildFeature("\\."+this.getId().intValue(),
						completeElements);
			}catch(Exception e){
				e.printStackTrace();
			}

			//System.out.println("construyendo el simplefeature para el id:"+this.getId());//construyendo el simplefeature para el id:0.0
			 
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
