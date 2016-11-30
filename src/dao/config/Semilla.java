package dao.config;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Semilla {
	StringProperty nombre = new SimpleStringProperty();
	Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Semilla> semillas = new HashMap<String,Semilla>();
	static{																		
		semillas.put("Semilla de Maiz",new Semilla("Semilla de Maiz",Cultivo.cultivos.get(Cultivo.MAIZ)));	

	}

	public Semilla(String _nombre, Cultivo producto) {
		nombre.set(_nombre);
		productoProperty.setValue(producto);
	}

	/**
	 * @return the nombre
	 */
	public StringProperty getNombre() {
		return nombre;
	}

	/**
	 * @param nombre the nombre to set
	 */
	public void setNombre(StringProperty nombre) {
		this.nombre = nombre;
	}

	
}
