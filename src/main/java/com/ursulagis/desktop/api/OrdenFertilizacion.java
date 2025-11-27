package com.ursulagis.desktop.api;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import com.ursulagis.desktop.dao.AbstractBaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=OrdenFertilizacion.FIND_ALL, query="SELECT c FROM OrdenFertilizacion c ") ,
//	@NamedQuery(name=OrdenFertilizacion.FIND_NAME, query="SELECT o FROM OrdenFertilizacion o where o.nombre = :name") ,
//	@NamedQuery(name=OrdenFertilizacion.FIND_ACTIVOS, query="SELECT o FROM OrdenFertilizacion o where o.activo = true") ,
}) 
public class OrdenFertilizacion extends AbstractBaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String FIND_ALL="OrdenFertilizacion.findAll";	
	
	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	private String owner=new String();
	private String poligonoString=null;
	
	public String url=new String();
	private String ordenShpZipUrl =null;
	
	private String nombreIngeniero="default";
	private String numeroOrden="default";
	private String nombre="nombre default";
	private String fecha="default";
	private String description="default";
	private String superficie="default";	
	
	private String estado=new String("Pendiente");	
	
	private String cultivo = "default";

	private String productor="default";
	private String establecimiento="default";
	private String contratista="default";	
	
	@OneToMany(cascade=CascadeType.ALL, mappedBy="ordenFertilizacion")
	private List<OrdenFertilizacionItem> items=new ArrayList<OrdenFertilizacionItem>();
	
	public OrdenFertilizacion() {
	//	super();
	}
	
	public void setItems(List<OrdenFertilizacionItem> _items) {
		this.items=_items;
	}
}
