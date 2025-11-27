package com.ursulagis.desktop.dao.fertilizacion;

import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.LaborItem;
/**
 *   Cuando el ingreso marginal es igual al costo unitario el beneficio ($/Ha) de agregar N es m�ximo
 * @author tomas
 *
 */
public class FertilizacionItem extends LaborItem {	
	private Double dosisHa=0d;	
	private Double importeHa=0d;
	private Double precioInsumo=0d;
	private Double costoLaborHa=0d;	
	
	public FertilizacionItem(SimpleFeature harvestFeature) {
		super(harvestFeature);
	}
	

	public FertilizacionItem() {
		super();
	}


	public void setDosistHa(Double cantFertHa) {
		this.dosisHa = cantFertHa;
	}

	public Double getDosistHa() {
		return 	this.dosisHa;
	}
	
	public Double getPrecioInsumo() {
		return precioInsumo;
	}

	public void setPrecioInsumo(Double precioFert) {
		this.precioInsumo = precioFert;
	}

	public Double getCostoLaborHa() {
		return costoLaborHa;
	}

	public void setCostoLaborHa(Double precioPasada) {
		this.costoLaborHa = precioPasada;
	}

	public void setImporteHa(Double doubleFromObj) {
		this.importeHa = doubleFromObj;
		
	}
	
	public Double getImporteHa() {
		this.importeHa = (dosisHa * precioInsumo + costoLaborHa);
		return importeHa;
	}

	@Override
	public Double getAmount() {		
		return getDosistHa();
	}
	public void setAmount(Double amount) {
		this.dosisHa = amount;
	}
	
	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getDosistHa(),
				getPrecioInsumo(),
				getCostoLaborHa(),
				getImporteHa()
		};
		return elements;
	}



	
	
}
