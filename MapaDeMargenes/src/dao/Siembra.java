package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class Siembra extends FeatureContainer {
	private static final String COLUMNA_BOLSAS_POR_HA = "BolsasHa";
	private static Map<String, String> columnsMap= new HashMap<String, String>();
	//Geometry geometry;
	Double bolsasHa;
	Double precioBolsa;
	Double precioPasada;
	Double importeHa;

	public Siembra(SimpleFeature harvestFeature, Double precioPasada ,Double precioGrano) {
		super(harvestFeature);
	//	System.out.println(harvestFeature);
		
		Object cantObj = harvestFeature
				.getAttribute(getColumn(COLUMNA_BOLSAS_POR_HA));
		
		bolsasHa = super.getDoubleFromObj(cantObj);
		
		
	//	this.bolsasHa = (Double) harvestFeature.getAttribute(getColumn(COLUMNA_BOLSAS_POR_HA));

		this.precioBolsa = precioGrano;
		this.precioPasada = precioPasada;
		
		this.importeHa = getImporteHa();// (bolsasHa * precioBolsa + precioPasada);
	}



	public Double getBolsaHa() {
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
	
	//Dao Methods
	@Override
	protected Map<String, String> getColumnsMap() {
		return Siembra.columnsMap;
	}
	
	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_BOLSAS_POR_HA);		
		return requiredColumns;
	}

	@Override
	public Double getAmount() {
		return getBolsaHa();
	}


	public static void setColumnsMap(Map<String, String> columns) {
		Siembra.columnsMap.clear();
		Siembra.columnsMap.putAll(columns);
		
		columns.forEach(new BiConsumer<String, String>(){
			@Override
			public void accept(String key, String value) {
				Configuracion.getInstance().setProperty(key, value);				
			}
			
		});
		
	}



	@Override
	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {
		// TODO Auto-generated method stub
		return null;
	}
}
