package com.ursulagis.desktop.dao.margen;

import javax.persistence.Entity;

import org.geotools.api.feature.simple.SimpleFeature;


import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)//si no pones esto todos los hashmaps andan mal y grillar cosecha no anda
//@Entity
public class MargenItem extends LaborItem{
	private Double importePulvHa =0.0;//= getImportePulv(harvestPolygon);
	private Double importeFertHa =0.0;//= getImporteFert(harvestPolygon);
	private Double importeSiembraHa =0.0;//= getImporteSiembra(harvestPolygon);
	private Double importeCosechaHa=0.0;
	
	private Double area =0.0;

	private Double margenPorHa =0.0;//= (importeCosechaPorHa * areaCosecha  - importePulv - importeFert - importeSiembra) / areaCosecha;
	private Double costoFijoPorHa=0.0;
	
	private boolean showMargen = true;
	
	public MargenItem(SimpleFeature feature) {
		super(feature);
	}	
	
	public MargenItem() {
		super();
	}

	public Double getImportePulvHa() {
		return importePulvHa;
	}

	public void setImportePulvHa(Double importePulvHa) {
		this.importePulvHa = importePulvHa;
	}

	public Double getImporteFertHa() {
		return importeFertHa;
	}


	public void setImporteFertHa(Double importeFertHa) {
		this.importeFertHa = importeFertHa;
	}


	public Double getImporteSiembraHa() {
		return importeSiembraHa;
	}

	public void setImporteSiembraHa(Double importeSiembraHa) {
		this.importeSiembraHa = importeSiembraHa;
	}

	public Double getArea() {
		return area;
	}

	public void setArea(Double area) {
		this.area = area;
	}

	//FIXME no toma en cuenta el costo variable por tonelada (Flete+Gastos de comercializacion)
	//Lo paso a la cosecha?
	//lo pongo en variables de la labor? el problema es que no tengo el dato del rinde.
	public Double getMargenPorHa() {
		return  (getImporteCosechaHa()
					- getImportePulvHa() - getImporteFertHa() - getImporteSiembraHa()-getCostoFijoPorHa());
		//return margenPorHa;
	}

	public void setMargenPorHa(Double margenPorHa) {
		this.margenPorHa = margenPorHa;
	}

	public Double getCostoPorHa() {
		return getImporteFertHa()+getImporteSiembraHa()+getImportePulvHa()+getCostoFijoPorHa();		
	}

	public Double getRentabilidadHa() {
		if(getCostoPorHa()>0){
			return Double.valueOf(getMargenPorHa()/getCostoPorHa()*100);
		} else{
			return 0.0;
		}		
	}

	@Override
	public Double getAmount() {
		if(showMargen){
			return getMargenPorHa();
		}
		return getRentabilidadHa();
	}
	
	public void setAmount(Double amount) {		
			setMargenPorHa(amount);		
	}

//	public void setImporteCosechaHa(Double importeCosechaPorHa) {
//		this.importeCosechaHa =importeCosechaPorHa; 
//	}
//	
//	public Double getImporteCosechaHa() {
//		return this.importeCosechaHa; 
//		
//	}
	
//	public Double getCostoFijoPorHa() {
//		return costoFijoPorHa;
//	}
//
//	public void setCostoFijoPorHa(Double costoFijoPorHa) {
//		this.costoFijoPorHa = costoFijoPorHa;
//	}


	@Override
	public Double getImporteHa() {
		return this.margenPorHa;
	}
	
/*
 * String type = Margen.COLUMNA_RENTABILIDAD + ":Double,"	
				+Margen.COLUMNA_MARGEN + ":Double,"	
				+Margen.COLUMNA_COSTO_TOTAL + ":Double,"	
				+Margen.COLUMNA_IMPORTE_FIJO + ":Double,"	
				+Margen.COLUMNA_IMPORTE_COSECHA + ":Double,"
				+Margen.COLUMNA_IMPORTE_FERT+":Double,"
				+Margen.COLUMNA_IMPORTE_PULV+":Double,"
				+Margen.COLUMNA_IMPORTE_SIEMBR+":Double";

 */
	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getRentabilidadHa(),
				getMargenPorHa(),
				getCostoPorHa(),
				getCostoFijoPorHa(),
				getImporteCosechaHa(),
				getImporteFertHa(),		
				getImportePulvHa(),
				getImporteSiembraHa()		
		};
		return elements;
	}
}
