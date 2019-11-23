package dao.config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import dao.Poligono;
import lombok.Data;


@Data
@Entity
@NamedQueries({
	@NamedQuery(name=Lote.FIND_ALL, query="SELECT o FROM Lote o ORDER BY lower(o.nombre)"),
	@NamedQuery(name=Lote.FIND_NAME, query="SELECT o FROM Lote o where o.nombre = :name") ,
}) 
public class Lote implements Comparable<Lote> {
	public static final String FIND_ALL = "Lote.findAll";
	public static final String FIND_NAME = "Lote.findNombre";

	@Id @GeneratedValue
	private Long id=null;

	public String nombre= new String();

	public Double superficie=new Double(0.0);
	
	@ManyToOne
	private Poligono contorno=null;
	
	@ManyToOne
	private Establecimiento establecimiento=null;

	public Lote() {
	}
	
	public Lote(Establecimiento e, String n){
		this.establecimiento=e;
		this.nombre=n;
	}

	public Lote(String n) {
	this.nombre=n;
	}

	@Override
	public int compareTo(Lote arg0) {
		return this.nombre.compareTo(arg0.nombre);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return nombre;
	}
}
