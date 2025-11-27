package com.ursulagis.desktop.dao.ordenCompra;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.JsonUtil.Exclude;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
//@NamedQueries({
//	@NamedQuery(name=Ndvi.FIND_ALL, query="SELECT c FROM Ndvi c") ,
//	@NamedQuery(name=Ndvi.FIND_NAME, query="SELECT o FROM Ndvi o where o.nombre = :name") ,
//	@NamedQuery(name=Ndvi.FIND_ACTIVOS, query="SELECT o FROM Ndvi o where o.activo = true") ,
//}) 
public class OrdenCompraItem {

	public static final String FIND_ALL="OrdenCompraItem.findAll";
	public static final String FIND_NAME = "OrdenCompraItem.findName";

	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	
	@ManyToOne
	@Exclude
	private OrdenCompra ordenCompra =null;
	
	@ManyToOne//(cascade= {CascadeType.DETACH})
	private Producto producto =null;
	
	private Double cantidad = 0.0;
	@Column(name = "oc_item_precio20", nullable = true, precision=20, scale = 2)
	private Double precio = 0.0;

	//@Column(precision=30, scale=2)
	//private BigDecimal importe = BigDecimal.valueOf(0.0);
	//@Column(precision=20, scale=2)
	@Column(name = "oc_item_importe20", nullable = true, precision=20, scale = 2)
	private Double importe =0.0;
	public OrdenCompraItem() {
		
	}
	
	public OrdenCompraItem(Producto producto2, Double cantidad2) {
		this.producto=producto2;
		this.cantidad=cantidad2;
	}
	
	public void setCantidad(Double newCant) {
		this.cantidad=newCant;
		calcImporte();
	}
	
	public void setPrecio(Double newPrecio) {
		this.precio=newPrecio;
		calcImporte();
	}
	
	public void calcImporte() {
		
		this.importe =cantidad*precio; //BigDecimal.valueOf(cantidad*precio).round(mc);
		//return this.importe;
		if(ordenCompra!=null) {
			this.ordenCompra.calcImporteTotal();
		}
	}
}
