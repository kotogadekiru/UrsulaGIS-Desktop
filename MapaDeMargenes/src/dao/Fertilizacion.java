package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class Fertilizacion extends Dao {
	public static final String KG_HA_COLUMN = "DOSIS_T";
	private static Map<String, String> columnsMap= new HashMap<String, String>();
	
	private Double cantFertHa;	
	private Double importeHa;
	private Double precioFert;
	private Double precioPasada;	
	
	public Fertilizacion(SimpleFeature harvestFeature, Double precioFert,Double precioPasada) {
		super(harvestFeature);
		//System.out.println(harvestFeature);
		
		
				 
		Object cantObj = harvestFeature
				.getAttribute(getColumn(KG_HA_COLUMN));
		
		cantFertHa = super.getDoubleFromObj(cantObj);
	

		this.precioFert = precioFert;
		this.precioPasada = precioPasada;		
		this.importeHa = (cantFertHa * precioFert + precioPasada);
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


	public static  List<String> getRequiredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(KG_HA_COLUMN);
		return requiredColumns;		
	}


	@Override
	protected Map<String, String> getColumnsMap() {
		return Fertilizacion.columnsMap;
	}
	

	public static void setColumnsMap(Map<String, String> columns) {
		Fertilizacion.columnsMap.clear();
		Fertilizacion.columnsMap.putAll(columns);
		
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
