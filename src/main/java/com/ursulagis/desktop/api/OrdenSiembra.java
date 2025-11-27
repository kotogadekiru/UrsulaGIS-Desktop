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
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=OrdenSiembra.FIND_ALL, query="SELECT c FROM OrdenSiembra c ") ,
//	@NamedQuery(name=OrdenSiembra.FIND_NAME, query="SELECT o FROM OrdenSiembra o where o.nombre = :name") ,
//	@NamedQuery(name=OrdenSiembra.FIND_ACTIVOS, query="SELECT o FROM OrdenSiembra o where o.activo = true") ,
}) 
public class OrdenSiembra extends AbstractBaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String FIND_ALL="OrdenSiembra.findAll";	
	
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
	private String unidad = SiembraLabor.COLUMNA_SEM_10METROS;

	private String productor="default";
	private String establecimiento="default";
	private String contratista="default";	
	
	@OneToMany(cascade=CascadeType.ALL, mappedBy="ordenSiembra")
	private List<OrdenSiembraItem> items=new ArrayList<OrdenSiembraItem>();
	
	public OrdenSiembra() {
	//	super();
	}
	
	public void setItems(List<OrdenSiembraItem> _items) {
		this.items=_items;
	}
}
