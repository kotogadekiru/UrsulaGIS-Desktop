package com.ursulagis.desktop.dao.recorrida;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import org.geotools.data.DataUtilities;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.feature.simple.SimpleFeatureType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.ursulagis.desktop.dao.AbstractBaseEntity;
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
	@NamedQuery(name=Recorrida.FIND_ALL, query="SELECT o FROM Recorrida o"),
	@NamedQuery(name=Recorrida.FIND_NAME, query="SELECT o FROM Recorrida o where o.nombre = :name") ,
	@NamedQuery(name=Recorrida.FIND_ID, query="SELECT o FROM Recorrida o where o.id = :id") ,
}) 

public class Recorrida extends AbstractBaseEntity{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//public static final String FIND_ALL = "Recorrida.findAll";
	public static final String CLASS_NAME= "Recorrida";
	public static final String FIND_ALL = CLASS_NAME+".findAll";
	public static final String FIND_NAME = CLASS_NAME+".findName";
	public static final String FIND_ID = CLASS_NAME+".findID";

	@Id @GeneratedValue//(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long numero = null;
	public String nombre=new String();
	public String observacion=new String();
	public String url=new String();

	//public String posicion=new String();//json {long,lat}
	public Double latitude= Double.valueOf(0.0);
	public Double longitude=Double.valueOf(0.0);
	private String fechaString="default";
	
	@OneToMany(cascade=CascadeType.ALL, mappedBy="recorrida",orphanRemoval=true)
	@OrderBy("id ASC")
	public List<Muestra> muestras =new ArrayList<Muestra>();

	/**
	 * representacion json de la ambientacion para la que esta hecha la recorrida
	 */
	public String jsonAmb="";

	public Recorrida()  {

		//		Recorrida r=this;
		//		muestras.addListener(new ListChangeListener<Muestra>() {
		//			List<Muestra> added = new ArrayList<Muestra>();
		//			@Override
		//			public void onChanged(Change<? extends Muestra> c) {
		//				if(!c.next()) {//me aseguro de estar en el ultimo cambio
		//				System.out.println("muestras changed "+c);
		//				
		//				added.addAll(c.getAddedSubList());
		//				added.stream().forEach(m->m.setRecorrida(r));
		//				added.clear();
		//				
		//				c.getRemoved().stream().forEach(m->m.setRecorrida(null));
		//				}
		//			}
		//			
		//		});
	}

	public Recorrida(FileDataStore store) {
		if(store !=null){
			ServiceInfo info = store.getInfo();
			System.out.println("labor inStore.info = "+info );
			try {
				SimpleFeatureType schema = store.getSchema();
				System.out.println("Prescription Type: "+DataUtilities.encodeType(schema));
				System.out.println(schema);
			} catch (IOException e) {
				e.printStackTrace();
			}

			//	if(nombreProperty.getValue() == null){
			//nombreProperty.set(inStore.getInfo().getTitle().replaceAll("%20", " "));
			String title = store.getInfo().getTitle();
			if (title != null && !title.trim().isEmpty()) {
				setNombre(title.replaceAll("%20", " "));
			} else {
				// Fallback to a default name if title is null or empty
				setNombre("Recorrida_" + System.currentTimeMillis());
			}

			//}
		}
	}

	public void setAmbs(Map<String,String> ambs) {
		Gson gson = new Gson();
		Type typeObject = new TypeToken<Map<String,String>>() {}.getType();
		String gsonData = gson.toJson(ambs, typeObject);
		//System.out.println("setting ambs "+gsonData);
		this.jsonAmb=gsonData;
	}

	public Map<String,String> getAmbs(){
		Gson gson = new Gson();
		Type typeObject = new TypeToken<Map<String,String>>() {}.getType();
		return gson.fromJson(jsonAmb, typeObject);
	}

	@Override
	public String toString() {
		return nombre;
	}

	/**
	 * toma una map de nombres de ambientes y sus geometrias y los asigna a jsonAmb en formato json
	 * @param ambsRec
	 */
	public void setAmbsGeoms(Map<String, List<Geometry>> ambsRec) {
		try {			
			Type typeObject = new TypeToken<String[]>() {}.getType();
			Gson gson = new Gson();

			Map<String,String> ambs = new HashMap<>();
			ambsRec.forEach((cat,geoms)->{
				List<String> geomsCat = geoms.stream().map(g->g.toText()).collect(Collectors.toList());
				String jsonGeoms = gson.toJson(geomsCat.toArray(), typeObject);
				ambs.put(cat, jsonGeoms);
			});
			this.setAmbs(ambs);
			//getAmbsGeoms();//verificando la inversa
		}catch(Exception e) {e.printStackTrace();}
	}

	public Map<String, List<Geometry>> getAmbsGeoms() {
		Map<String, List<Geometry>> ambsRec = new HashMap<String, List<Geometry>>();
		Map<String,String> ambString = getAmbs();
		Type typeObject = new TypeToken<String[]>() {}.getType();
		Gson gson = new Gson();
		for(String amb: ambString.keySet()) {
			String geomsString = ambString.get(amb);
			//	System.out.println("reading geomsString "+geomsString);			
			String[] geoms= gson.fromJson(geomsString, typeObject);

			for(String wktString :geoms) {				
				//	System.out.println("reading geom "+wktString);
				WKTReader reader = new WKTReader();
				try {
					Geometry geometry = reader.read(wktString);
					//System.out.println("area: "+geometry.getArea());
					if(ambsRec.containsKey(amb)) {
						List<Geometry> geomsAmb = ambsRec.get(amb);
						geomsAmb.add(geometry);						
					} else {
						List<Geometry> geomsAmb = new ArrayList<Geometry>();
						geomsAmb.add(geometry);		
						ambsRec.put(amb, geomsAmb);
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return ambsRec;
	}
}
