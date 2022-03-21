package dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import dao.OrdenDeCompra.Producto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

//@Data
@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity //@Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Semilla.FIND_ALL, query="SELECT o FROM Semilla o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Semilla.FIND_NAME, query="SELECT o FROM Semilla o where o.nombre = :name") ,
}) 
public class Semilla extends Producto{
	public static final String FIND_ALL="Semilla.findAll";
	public static final String FIND_NAME="Semilla.findName";
	
	public static final String SEMILLA_DE_TRIGO = "Semilla de Trigo";
	public static final String SEMILLA_DE_SOJA = "Semilla de Soja";
	public static final String SEMILLA_DE_MAIZ = "Semilla de Maiz";
	
//	@Id @GeneratedValue
//	private Long id=null;
	private String nombre = new String();
	/**
	 * poder germinativo
	 */
	private Double PG = new Double(1);
	/**
	 * peso de mil granos en gramos
	 */
	private Double pesoDeMil = new Double(150);
	
	@ManyToOne(cascade=CascadeType.PERSIST)
	private Cultivo cultivo = null;
	
	//@Transient
	//private Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Semilla> semillas = new HashMap<String,Semilla>();
	static{																		
		semillas.put(SEMILLA_DE_MAIZ,new Semilla(SEMILLA_DE_MAIZ,Cultivo.cultivos.get(Cultivo.MAIZ)));	
		semillas.put(SEMILLA_DE_SOJA,new Semilla(SEMILLA_DE_SOJA,Cultivo.cultivos.get(Cultivo.SOJA)));
		semillas.put(SEMILLA_DE_TRIGO,new Semilla(SEMILLA_DE_TRIGO,Cultivo.cultivos.get(Cultivo.TRIGO)));

	}

	public Semilla(){
	}
	
	public Semilla(String _nombre, Cultivo producto) {
		nombre=_nombre;
		cultivo=producto;
		//productoProperty.setValue(producto);
	}

//	/**
//	 * @return the id
//	 */
//	
//	public long getId() {
//		return id;
//	}
//
//	/**
//	 * @param id the id to set
//	 */
//	public void setId(long id) {
//		this.id = id;
//	}
	
//	public String getNombre(){
//		return this.nombre.get();
//	}
//
//	public void setNombre(String n){
//		this.nombre.set(n);
//	}

	
/*	public Cultivo getCultivo(){
		return this.productoProperty.getValue();
	}

	public void setCultivo(Cultivo cultivo){
		if(cultivo != null)
			this.productoProperty.setValue(cultivo);
	}*/

	/**
	 * @return the nombre
	 */
//	@Transient
//	public StringProperty getNombreProperty() {
//		return nombre;
//	}
//
//	/**
//	 * @param nombre the nombre to set
//	 */
//
//	public void setNombreProperty(StringProperty nombre) {
//		this.nombre = nombre;
//	}
	
//	@Transient
//	public Property<Cultivo> getProductoPorperty(){
//		return this.productoProperty;
//	}

	@Override
	public String toString() {
		return nombre;
	}
}
