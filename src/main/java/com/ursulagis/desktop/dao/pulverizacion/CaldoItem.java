package com.ursulagis.desktop.dao.pulverizacion;

import java.math.BigDecimal;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;

import com.ursulagis.desktop.dao.config.Agroquimico;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity @Access(AccessType.FIELD)
public class CaldoItem {
	public static final String FIND_ALL="CaldoItem.findAll";
	public static final String FIND_NAME = "CaldoItem.findName";

	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	
	//@ManyToOne
	private PulverizacionLabor labor =null;
	
	//@ManyToOne//(cascade= {CascadeType.DETACH})
	private Agroquimico producto =null;
	
	private String unidadDosis= new String();
	private String unidadStock = new String();
	
	private Double dosisHa = 0.0;
	private String observaciones =  null;
	
	public CaldoItem() {
		super();
	}
	
	public CaldoItem(CaldoItem i) {
		super();
		setDosisHa(i.getDosisHa());				
		setProducto(i.getProducto());

		setObservaciones(i.getObservaciones());
	}

	public void setProducto(Agroquimico producto) {
		this.producto=producto;
		this.unidadDosis=producto.getUnidadDosis();
		this.unidadStock=producto.getUnidadStock();
	}
	
	@Override
	public String toString() {
		return "CaldoItem:{"+producto+", "+dosisHa+", "+observaciones+"}";
	}
}
