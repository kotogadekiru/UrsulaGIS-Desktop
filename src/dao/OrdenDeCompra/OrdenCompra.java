package dao.OrdenDeCompra;
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
public class OrdenCompra {
	
	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	
	@OneToMany(cascade=CascadeType.ALL, mappedBy="ordenCompra")
	private List<OrdenCompraItem> items=new ArrayList<OrdenCompraItem>();

}
