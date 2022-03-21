package dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import dao.OrdenDeCompra.Producto;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Agroquimico.FIND_ALL, query="SELECT o FROM Agroquimico o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Agroquimico.FIND_NAME, query="SELECT o FROM Agroquimico o where o.nombre = :name") ,
}) 
public class Agroquimico extends Producto implements Comparable<Agroquimico>{
	public static final String FIND_ALL="Agroquimico.findAll";
	public static final String FIND_NAME="Agroquimico.findName";
	
	
//	@Id @GeneratedValue
//	private Long id=null;
	
	private StringProperty nombre = new SimpleStringProperty();
	//private Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Agroquimico> agroquimicos = new HashMap<String,Agroquimico>();
	static{																		
		agroquimicos.put("RoundUp",new Agroquimico("RoundUp"));	
		agroquimicos.put("Superwet",new Agroquimico("Superwet"));
		agroquimicos.put("Atrazina",new Agroquimico("Atrazina"));
	}


	
	public Agroquimico() {
	}

	
	public Agroquimico(String _nombre) {
		nombre.set(_nombre);
	}

	//@Id @GeneratedValue
//	public Long getId(){
//		return this.id;
//	}
//	
//	public void setId(Long id){
//		this.id=id;
//	}
	
	public String getNombre(){
		return this.nombre.get();
	}

	public void setNombre(String n){
		if(this.nombre==null){
			 nombre = new SimpleStringProperty();
		}
		this.nombre.set(n);
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

	@Override
	public String toString() {
		return nombre.getValue();
	}

	@Override
	public int compareTo(Agroquimico o) {
		return (int) (this.getId()-o.getId());
	}
}
