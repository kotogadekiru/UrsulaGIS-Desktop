package dao.fertilizacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import dao.LaborItem;
/**
 *   Cuando el ingreso marginal es igual al costo unitario el beneficio ($/Ha) de agregar N es máximo
 * @author tomas
 *
 */
public class FertilizacionItem extends LaborItem {	
	private Double cantFertHa=0d;	
	private Double importeHa=0d;
	private Double precioFert=0d;
	private Double precioPasada=0d;	
	
	public FertilizacionItem(SimpleFeature harvestFeature) {
		super(harvestFeature);
	}
	

	public FertilizacionItem() {
		super();
	}


	public void setCantFertHa(Double cantFertHa) {
		this.cantFertHa = cantFertHa;
	}

	public Double getCantFertHa() {
		return 	this.cantFertHa;
	}
	
	public Double getPrecioFert() {
		return precioFert;
	}

	public void setPrecioFert(Double precioFert) {
		this.precioFert = precioFert;
	}

	public Double getPrecioPasada() {
		return precioPasada;
	}

	public void setPrecioPasada(Double precioPasada) {
		this.precioPasada = precioPasada;
	}


	public Double getImporteHa() {
		this.importeHa = (cantFertHa * precioFert + precioPasada);
		return importeHa;
	}

	@Override
	public Double getAmount() {		
		return getCantFertHa();
	}





//	@Override
//	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {
//		featureBuilder.addAll(new Object[]{super.getGeometry(),
//				getCantFertHa(),
//					getPrecioFert(),
//					getPrecioPasada(),
//					getImporteHa(),
//					getCategoria()});
//		
//	SimpleFeature feature = featureBuilder.buildFeature("\\."+this.getId().intValue());
//		
//		return feature;
//	}
	
	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getCantFertHa(),
				getPrecioFert(),
				getPrecioPasada(),
				getImporteHa()
		};
		return elements;
	}
	
	
}
