package dao.siembra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.ConstantExpression;
import org.opengis.feature.simple.SimpleFeature;

import utils.ProyectionConstants;

import com.vividsolutions.jts.geom.Geometry;

import dao.LaborItem;

public class SiembraItem extends LaborItem {
	private Double elevacion;	
	private Double bolsasHa;
	private Double precioBolsa;
	private Double precioPasada;
	private Double importeHa;

	public SiembraItem(SimpleFeature harvestFeature) {
		super(harvestFeature);

	
//		Object cantObj = harvestFeature
//				.getAttribute(getColumn(COLUMNA_BOLSAS_POR_HA));
//		
//		bolsasHa = super.getDoubleFromObj(cantObj);
//		// BOLSAS/HA = SEM/MT x (MT2 HECTAREA / dist entre surcos) / semillas/bolsa
//		// SEM/MT x (10.000 / 0.525 ) / 80.0000
//		
//		bolsasHa = bolsasHa*(ProyectionConstants.METROS2_POR_HA/ENTRE_SURCO)/SEMILLAS_POR_BOLSA;
//		
//	//	this.bolsasHa = (Double) harvestFeature.getAttribute(getColumn(COLUMNA_BOLSAS_POR_HA));
//
//		this.precioBolsa = precioGrano;
//		this.precioPasada = precioPasada;
//		
//		this.importeHa = getImporteHa();// (bolsasHa * precioBolsa + precioPasada);
	}



	public Double getBolsasHa() {
		return bolsasHa;
	}

	public void setBolsasHa(Double rindeTnHa) {
		this.bolsasHa = rindeTnHa;
	}

	public Double getPrecioBolsa() {
		return precioBolsa;
	}

	public void setPrecioBolsa(Double precio) {
		this.precioBolsa = precio;
	}
	
	

	public Double getImporteHa() {
		this.importeHa =  (bolsasHa * precioBolsa + precioPasada);
		return importeHa;
	}

	@Override
	public Double getAmount() {
		return getBolsasHa();
	}


//	@Override
//	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {
////		String type = SiembraLabor.COLUMNA_BOLSAS_HA + ":Double,"
////				+ SiembraLabor.COLUMNA_PRECIO_BOLSA + ":Double,"
////				+ SiembraLabor.COLUMNA_PRECIO_PASADA + ":Double,"
////				+ SiembraLabor.COLUMNA_IMPORTE_HA + ":Double,"
////				+ SiembraLabor.COLUMNA_CATEGORIA + ":Integer";
//		
//		featureBuilder.addAll(new Object[]{super.getGeometry(),
//				getBolsasHa(),
//				getElevacion(),
//				getPrecioBolsa(),
//				getPrecioPasada(),
//				getImporteHa(),
//				getCategoria()});
//	
//SimpleFeature feature = featureBuilder.buildFeature("\\."+this.getId().intValue());
//	
//	return feature;
//	}

	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getBolsasHa(),
				getPrecioBolsa(),
				getPrecioPasada(),
				getImporteHa()
		};
		return elements;
	}
	

	/**
	 * @return the precioPasada
	 */
	public Double getPrecioPasada() {
		return precioPasada;
	}



	/**
	 * @param precioPasada the precioPasada to set
	 */
	public void setPrecioPasada(Double precioPasada) {
		this.precioPasada = precioPasada;
	}



	/**
	 * @return the elevacion
	 */
	public Double getElevacion() {
		return elevacion;
	}



	/**
	 * @param elevacion the elevacion to set
	 */
	public void setElevacion(Double elevacion) {
		this.elevacion = elevacion;
	}




}
