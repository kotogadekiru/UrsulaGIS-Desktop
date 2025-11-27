package com.ursulagis.desktop.dao.siembra;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;

import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.LaborItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.GeometryHelper;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
public class SiembraItem extends LaborItem {	
	private Double dosisHa =0.0;	
	private Double dosisML =0.0;	//dosis metro lineal
	private Double precioInsumo= 0.0;	
	private Double costoLaborHa=0.0;	
	private Double importeHa=0.0;	
	private Double dosisFertLinea=0.0;
	private Double dosisFertCostado=0.0;

	public SiembraItem(SimpleFeature harvestFeature) {
		super(harvestFeature);
	}

	public SiembraItem() {
		super();
	}

	public SiembraItem(SiembraItem s) {
		super(s);	
		setCostoLaborHa(s.getCostoLaborHa());
		setDosisFertCostado(s.getDosisFertCostado());	
		setDosisFertLinea(s.getDosisFertLinea());
		setDosisHa(s.getDosisHa());
		setDosisML(s.getDosisML());
		setImporteHa(s.getImporteHa());
		setPrecioInsumo(s.getPrecioInsumo());		
	}
	
	public Double getDosisHa() {
		return dosisHa;
	}

	public void setDosisHa(Double kgHa) {
		this.dosisHa = kgHa;
	}

	public Double getPrecioInsumo() {
		return precioInsumo;
	}

	public void setPrecioInsumo(Double precio) {
		this.precioInsumo = precio;
	}

	public Double getImporteHa() {
		this.importeHa =  (dosisHa * precioInsumo + costoLaborHa);
		return importeHa;
	}

	public void setImporteHa(Double doubleFromObj) {
		this.importeHa = doubleFromObj;	
	}
	
	
	
	/**
	 * @return the dosisFertLinea
	 */
	public Double getDosisFertLinea() {
		return dosisFertLinea;
	}

	/**
	 * @param dosisFertLinea the dosisFertLinea to set
	 */
	public void setDosisFertLinea(Double dosisFertLinea) {
		this.dosisFertLinea = dosisFertLinea;
	}

	/**
	 * @return the dosisFertCostado
	 */
	public Double getDosisFertCostado() {
		return dosisFertCostado;
	}

	/**
	 * @param dosisFertCostado the dosisFertCostado to set
	 */
	public void setDosisFertCostado(Double dosisFertCostado) {
		this.dosisFertCostado = dosisFertCostado;
	}

	@Override
	public Double getAmount() {
		return getDosisHa();
	}

	public void setAmount(Double amount) {		
		setDosisHa(amount);		
}
	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getDosisML(),
				getDosisHa(),
				getDosisFertLinea(),
				getDosisFertCostado(),
				getPrecioInsumo(),
				getCostoLaborHa(),
				getImporteHa()
		};
		return elements;
	}
	

	/**
	 * @return the precioPasada
	 */
	public Double getCostoLaborHa() {
		return costoLaborHa;
	}



	/**
	 * @param precioPasada the precioPasada to set
	 */
	public void setCostoLaborHa(Double precioPasada) {
		this.costoLaborHa = precioPasada;
	}
}
