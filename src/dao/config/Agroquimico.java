package dao.config;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Agroquimico {
	private StringProperty nombre = new SimpleStringProperty();
	//private Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Agroquimico> agroquimicos = new HashMap<String,Agroquimico>();
	static{																		
		agroquimicos.put("RoundUp",new Agroquimico("RoundUp"));	
		agroquimicos.put("Superwet",new Agroquimico("Superwet"));
		agroquimicos.put("Atrazina",new Agroquimico("Atrazina"));

	}

	public Agroquimico(String _nombre) {
		nombre.set(_nombre);
	
	}

	public String getNombre(){
		return this.nombre.get();
	}

	public void setNombre(String n){
		this.nombre.set(n);
	}






	/**
	 * @return the nombre
	 */
	public StringProperty getNombreProperty() {
		return nombre;
	}

	/**
	 * @param nombre the nombre to set
	 */
	public void setNombreProperty(StringProperty nombre) {
		this.nombre = nombre;
	}

	@Override
	public String toString() {
		return nombre.getValue();
	}
}
