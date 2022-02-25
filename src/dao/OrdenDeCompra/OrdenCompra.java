package dao.OrdenDeCompra;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import org.eclipse.persistence.annotations.UuidGenerator;

import dao.AbstractBaseEntity;
import dao.Ndvi;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=OrdenCompra.FIND_ALL, query="SELECT c FROM OrdenCompra c ") ,
//	@NamedQuery(name=OrdenCompra.FIND_NAME, query="SELECT o FROM OrdenCompra o where o.nombre = :name") ,
//	@NamedQuery(name=OrdenCompra.FIND_ACTIVOS, query="SELECT o FROM OrdenCompra o where o.activo = true") ,
}) 
//@UuidGenerator(name="EMP_ID_GEN")
public class OrdenCompra extends AbstractBaseEntity {
	public static final String FIND_ALL="OrdenCompra.findAll";	
//    @Id
//    @GeneratedValue(generator="EMP_ID_GEN")
//    private String uuid_id;
	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	private String url=null;
	private String description=null;
	private String mail=null;
	

	@OneToMany(cascade=CascadeType.ALL, mappedBy="ordenCompra",orphanRemoval=true)
	private List<OrdenCompraItem> items=new ArrayList<OrdenCompraItem>();
	
	private Double importeTotal=null;
	
//	public void setItems(List<OrdenCompraItem> _items) {
//		this.items=_items;
//	}
//	
	public Double getImporteTotal() {
		this.importeTotal=items.stream().mapToDouble(i->i.getImporte()).sum(); 
		return importeTotal;
	}
}
