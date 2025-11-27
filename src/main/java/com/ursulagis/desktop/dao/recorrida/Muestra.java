package com.ursulagis.desktop.dao.recorrida;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import com.google.gson.Gson;

import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.Messages;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * clase que representa una observacion
 * @author quero
 *
 */

@Getter
@Setter(value = AccessLevel.PUBLIC)

@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Muestra.FIND_ALL, query="SELECT o FROM Muestra o"),
	@NamedQuery(name=Muestra.FIND_NAME, query="SELECT o FROM Muestra o where o.nombre = :name") ,
}) 

public class Muestra {
	public static final String FIND_ALL = "Muestra.findAll";
	public static final String FIND_NAME = "Muestra.findName";

	@Id @GeneratedValue//(strategy = GenerationType.IDENTITY)
	private Long id;
	/**
	 * @name categoria
	 */
	public String nombre=new String();
	public String subNombre=new String();
	/**
	 * @description jsonString with values
	 */
	public String observacion=new String();//{nombre:valor,nombre2:valor2...}
	//public String posicion=new String();//json {long,lat}
	public Double latitude= Double.valueOf(0.0);
	public Double longitude=Double.valueOf(0.0);


	@ManyToOne
	private Recorrida recorrida=null;

	public Muestra() {

	}

	public Position getPosition() {
		Double elevacion = this.getProps().getOrDefault("Elevacion", 10.0);	
		return Position.fromDegrees(this.latitude,this.longitude,elevacion);
	}

	@Override
	public String toString() {
		return subNombre==null?nombre:nombre+" ("+subNombre+")";
	}

	@Transient
	public void initObservacionSuelo() {		
		Map<String,String> map = new HashMap<String,String>();
		map.put(SueloItem.PPM_FOSFORO, "");
		map.put(SueloItem.PPM_N, "");
		map.put(SueloItem.PPM_ASUFRE, "");
		map.put(SueloItem.PPM_POTASIO, "");
		
		map.put(SueloItem.Calcio, "");
		map.put(SueloItem.Magnecio, "");
		map.put(SueloItem.Boro, "");
		map.put(SueloItem.Cloro, "");
		map.put(SueloItem.Cobalto, "");
		map.put(SueloItem.Cobre, "");
		map.put(SueloItem.Hierro, "");
		map.put(SueloItem.Manganeso, "");
		map.put(SueloItem.Molibdeno, "");
		map.put(SueloItem.Zinc, "");
		
		map.put(SueloItem.PC_MO, "");
		map.put(SueloItem.PROF_NAPA, "");
		map.put(SueloItem.AGUA_PERFIL, "");

		String densidadDefault =  Messages.getNumberFormat().format(SueloItem.DENSIDAD_SUELO_KG);
		map.put(SueloItem.DENSIDAD,densidadDefault);

		map.put(SueloItem.ELEVACION,"10");

		String observacion = new Gson().toJson(map);
		this.setObservacion(observacion);		
	}

	/**
	 * metodo practico que convierte de obs a un map de numeros
	 * @return
	 */
	@Transient
	public Map<String,Double> getProps(){
		String obs = this.getObservacion();
		NumberFormat nf = Messages.getNumberFormat();
		@SuppressWarnings("unchecked")
		Map<String,String> map = new Gson().fromJson(obs, Map.class);	 

		LinkedHashMap<String, Double> props = new LinkedHashMap<String,Double>();
		for(String k : map.keySet()) {
			Object value = map.get(k);
			if(String.class.isAssignableFrom(value.getClass())) {				
				Double dValue = 0.0;
				try {					
					if(value != null 
							&& !"".equals(value)) {
						dValue = nf.parse((String)value).doubleValue(); 
					}
					//dValue=Double.parseDouble((String)value);
				}catch(Exception e) {
					System.err.println("error en k: "+k+" tratando de parsear \""+value+"\" reemplazo por 0");
					e.printStackTrace();
				}
				props.put(k, dValue);//ojo number format exception
			} else if(Number.class.isAssignableFrom(value.getClass())) {
				props.put(k, ((Number)value).doubleValue());
			}			
		}
		return props;
	}
}
