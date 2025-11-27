package com.ursulagis.desktop.dao.pulverizacion;

import java.util.ArrayList;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.LaborItem;

public class PulverizacionItem extends LaborItem{
	public static final String COLUMNA_COSTO_PAQUETE = "COLUMNA_COSTO_PAQUETE_PULVERIZACION";
	public static final Object COLUMNA_CANT_PASADAS = "CANT_PASADAS_PULVERIZACION";

	private Double dosisHa =0.0 ;//= (Long) simpleFeature.getAttribute("Costo");
	private Double precioInsumo=0.0;
	//private Double cantPasadasHa=Double.parseDouble(1) ;//= (Integer) simpleFeature.getAttribute("Pasadas");	
	private Double costoLaborHa=0.0;
	private Double importeHa=0.0;//es el (costo de lo agroquimicos de una pasada + el costo de labor de una pasada) por la cantidad de pasadas 



	public PulverizacionItem(SimpleFeature pulvFeature) {
		super(pulvFeature);				
	}

	public PulverizacionItem() {
		super();
	}

	public Double getDosis() {
		return dosisHa;
	}

	public void setDosis(Double costoPaquete) {
		this.dosisHa = costoPaquete;
	}
	

	/**
	 * @return the precioInsumo
	 */
	public Double getPrecioInsumo() {
		return precioInsumo;
	}



	/**
	 * @param precioInsumo the precioInsumo to set
	 */
	public void setPrecioInsumo(Double precioInsumo) {
		this.precioInsumo = precioInsumo;
	}



//	public Double getCantPasadasHa() {
//		return cantPasadasHa;
//	}
//
//	public void setCantPasadasHa(Double cantPasadasHa) {
//
//		this.cantPasadasHa = cantPasadasHa;
//	}


	public Double getCostoLaborHa() {
		return costoLaborHa;
	}

	public void setCostoLaborHa(Double costoLaborHa) {
		this.costoLaborHa = costoLaborHa;
	}

	public Double getImporteHa() {
		this.importeHa = (dosisHa * precioInsumo + costoLaborHa);
	//	this.importeHa=(dosisHa*precioInsumo+costoLaborHa);//*cantPasadasHa;		
		return importeHa;
	}

	public void setImporteHa(Double importeHa) {
		this.importeHa=importeHa;
	}

	@Override
	public Double getAmount() {
		return getDosis();
	}
	public void setAmount(Double amount) {		
		setDosis(amount);		
}

	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getDosis(),
				getPrecioInsumo(),
				//getCantPasadasHa(),
				getCostoLaborHa(),
				getImporteHa()
		};
		return elements;
	}

	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_COSTO_PAQUETE);		
		return requiredColumns;
	}

}
