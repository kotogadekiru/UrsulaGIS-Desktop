package dao.margen;

import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.factory.GeoTools;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.FeatureContainer;


public class MargenItem extends FeatureContainer{
	//Polygon harvestPolygon = (Polygon) geometry;
	private Double importePulvHa ;//= getImportePulv(harvestPolygon);
	private Double importeFertHa ;//= getImporteFert(harvestPolygon);
	private Double importeSiembraHa ;//= getImporteSiembra(harvestPolygon);
	private Double area ;

	private Double margenPorHa ;//= (importeCosechaPorHa * areaCosecha  - importePulv - importeFert - importeSiembra) / areaCosecha;
	private Double costoFijoPorHa;
	private Double ingresoHa;
//	private Double rentabilidadHa;
	private Double importeCosechaHa;
	
	
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

	public Double getIngresoHa() {
		return ingresoHa;
	}

	public void setIngresoHa(Double ingresoHa) {
		this.ingresoHa = ingresoHa;
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

	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getRentabilidadHa(),
				getMargenPorHa(),
				getCostoPorHa(),
				getIngresoHa(),
				getImporteFertHa(),		
				getImportePulvHa(),
				getImporteSiembraHa()		
		};
		return elements;
	}
}
