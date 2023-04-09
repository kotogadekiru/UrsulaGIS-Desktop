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

import dao.ordenCompra.Producto;
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
	
	//private String nombre = new String();
	//private Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Agroquimico> agroquimicos = new HashMap<String,Agroquimico>();
	static{																		
		agroquimicos.put("RoundUp(lts)",new Agroquimico("RoundUp(lts)"));	
		agroquimicos.put("Superwet(lts)",new Agroquimico("Superwet(lts)"));
		agroquimicos.put("Atrazina(lts)",new Agroquimico("Atrazina(lts)"));
		agroquimicos.put("Cletodim(lts)",new Agroquimico("Cletodim(lts)"));
		agroquimicos.put("Rizospray extremo(lts)",new Agroquimico("Rizospray extremo(lts)"));
	}
	
	public Agroquimico() {
	}

	
	public Agroquimico(String _nombre) {
		nombre=_nombre;
	}

	//@Id @GeneratedValue
//	public Long getId(){
//		return this.id;
//	}
//	
//	public void setId(Long id){
//		this.id=id;
//	}


	@Override
	public int compareTo(Agroquimico o) {
		return (int) (this.getId().compareTo(o.getId()));
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return nombre;
	}
	
	
}
