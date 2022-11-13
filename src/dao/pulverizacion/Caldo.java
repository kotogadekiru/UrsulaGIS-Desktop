package dao.pulverizacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Caldo.FIND_ALL, query="SELECT c FROM Caldo c ") ,
//	@NamedQuery(name=OrdenCompra.FIND_NAME, query="SELECT o FROM OrdenCompra o where o.nombre = :name") ,
//	@NamedQuery(name=OrdenCompra.FIND_ACTIVOS, query="SELECT o FROM OrdenCompra o where o.activo = true") ,
}) 
public class Caldo {
	private static final long serialVersionUID = 1L;
	public static final String FIND_ALL="Caldo.findAll";	
//    @Id
//    @GeneratedValue(generator="EMP_ID_GEN")
//    private String uuid_id;
	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	private String nombre=null;
	private String descripcion=null;

	

	@OneToMany(cascade=CascadeType.ALL, mappedBy="caldo",orphanRemoval=true)
	private List<CaldoItem> items=new ArrayList<CaldoItem>();
	
	public String toString() {
		return this.getNombre();
	}
}
