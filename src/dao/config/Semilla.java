package dao.config;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Semilla {
	private StringProperty nombre = new SimpleStringProperty();
	private Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Semilla> semillas = new HashMap<String,Semilla>();
	static{																		
		semillas.put("Semilla de Maiz",new Semilla("Semilla de Maiz",Cultivo.cultivos.get(Cultivo.MAIZ)));	
		semillas.put("Semilla de Soja",new Semilla("Semilla de Soja",Cultivo.cultivos.get(Cultivo.SOJA)));
		semillas.put("Semilla de Trigo",new Semilla("Semilla de Trigo",Cultivo.cultivos.get(Cultivo.TRIGO)));

	}

	public Semilla(String _nombre, Cultivo producto) {
		nombre.set(_nombre);
		productoProperty.setValue(producto);
	}

	public String getNombre(){
		return this.nombre.get();
	}

	public void setNombre(String n){
		this.nombre.set(n);
	}

	public String getCultivo(){
		return this.productoProperty.getValue().getNombre();
	}

	public void setCultivo(String nombreC){
		Cultivo c=	Cultivo.cultivos.get(nombreC);	
		if(c!=null){
			this.productoProperty.setValue(c);
		}

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
