package dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;
@Data
@Entity @Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Semilla.FIND_ALL, query="SELECT o FROM Semilla o") ,
	@NamedQuery(name=Semilla.FIND_NAME, query="SELECT o FROM Semilla o where o.nombre = :name") ,
}) 
public class Semilla {
	public static final String FIND_ALL="Semilla.findAll";
	public static final String FIND_NAME="Semilla.findName";
	
	public static final String SEMILLA_DE_TRIGO = "Semilla de Trigo";
	public static final String SEMILLA_DE_SOJA = "Semilla de Soja";
	public static final String SEMILLA_DE_MAIZ = "Semilla de Maiz";
	
	
	private long id;
	@Transient
	private StringProperty nombre = new SimpleStringProperty();
	@Transient
	private Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Semilla> semillas = new HashMap<String,Semilla>();
	static{																		
		semillas.put(SEMILLA_DE_MAIZ,new Semilla(SEMILLA_DE_MAIZ,Cultivo.cultivos.get(Cultivo.MAIZ)));	
		semillas.put(SEMILLA_DE_SOJA,new Semilla(SEMILLA_DE_SOJA,Cultivo.cultivos.get(Cultivo.SOJA)));
		semillas.put(SEMILLA_DE_TRIGO,new Semilla(SEMILLA_DE_TRIGO,Cultivo.cultivos.get(Cultivo.TRIGO)));

	}

	public Semilla(){
	}
	
	public Semilla(String _nombre, Cultivo producto) {
		nombre.set(_nombre);
		productoProperty.setValue(producto);
	}

	/**
	 * @return the id
	 */
	@Id @GeneratedValue
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	public String getNombre(){
		return this.nombre.get();
	}

	public void setNombre(String n){
		this.nombre.set(n);
	}

	@ManyToOne(cascade=CascadeType.PERSIST)
	public Cultivo getCultivo(){
		return this.productoProperty.getValue();
	}

	public void setCultivo(Cultivo cultivo){
		if(cultivo != null)
			this.productoProperty.setValue(cultivo);
	}

	/**
	 * @return the nombre
	 */
	@Transient
	public StringProperty getNombreProperty() {
		return nombre;
	}

	/**
	 * @param nombre the nombre to set
	 */

	public void setNombreProperty(StringProperty nombre) {
		this.nombre = nombre;
	}
	
	@Transient
	public Property<Cultivo> getProductoPorperty(){
		return this.productoProperty;
	}

	@Override
	public String toString() {
		return nombre.getValue();
	}
}
