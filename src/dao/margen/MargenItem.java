package dao.margen;

import org.opengis.feature.simple.SimpleFeature;


import dao.LaborItem;
import lombok.Data;

@Data
public class MargenItem extends LaborItem{
	//Polygon harvestPolygon = (Polygon) geometry;
	private Double importePulvHa =new Double(0);//= getImportePulv(harvestPolygon);
	private Double importeFertHa =new Double(0);//= getImporteFert(harvestPolygon);
	private Double importeSiembraHa =new Double(0);//= getImporteSiembra(harvestPolygon);
	private Double importeCosechaHa=new Double(0);
	
	private Double area =new Double(0);

	private Double margenPorHa =new Double(0);//= (importeCosechaPorHa * areaCosecha  - importePulv - importeFert - importeSiembra) / areaCosecha;
	private Double costoFijoPorHa=new Double(0);
	
	private boolean showMargen = true;
//	private Double ingresoHa=new Double(0);
//	private Double rentabilidadHa;

	
	
	public MargenItem(SimpleFeature feature) {
		super(feature);
	}	
	
	public MargenItem() {
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

	public Double getMargenPorHa() {
		return margenPorHa;
	}

	public void setMargenPorHa(Double margenPorHa) {
		this.margenPorHa = margenPorHa;
	}

	public Double getCostoPorHa() {
		return getImporteFertHa()+getImporteSiembraHa()+getImportePulvHa()+getCostoFijoPorHa();		
	}

	public Double getRentabilidadHa() {
		if(getCostoPorHa()>0){
			return new Double(getMargenPorHa()/getCostoPorHa()*100);
		} else{
			return new Double(0);
		}		
	}

	@Override
	public Double getAmount() {
		if(showMargen){
			return getMargenPorHa();
		}
		//TODO cambiar esto segun lo que se haya seleccionado
		return getRentabilidadHa();
	}

	public void setImporteCosechaHa(Double importeCosechaPorHa) {
		this.importeCosechaHa =importeCosechaPorHa; 
	}
	
	public Double getImporteCosechaHa() {
		return this.importeCosechaHa; 
	}
	
	public Double getCostoFijoPorHa() {
		return costoFijoPorHa;
	}

	public void setCostoFijoPorHa(Double costoFijoPorHa) {
		this.costoFijoPorHa = costoFijoPorHa;
	}
	
//	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder){		
//		featureBuilder.add(super.getGeometry());
//		featureBuilder.add(getRentabilidadHa());
//		featureBuilder.add(getMargenPorHa());
//		featureBuilder.add(getCostoPorHa());
//		featureBuilder.add(getIngresoHa());
//		featureBuilder.add(getImporteFertHa());		
//		featureBuilder.add(getImportePulvHa());
//		featureBuilder.add(getImporteSiembraHa());		
//				
//		SimpleFeature feature = featureBuilder.buildFeature(null);
//		return feature;
//		
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