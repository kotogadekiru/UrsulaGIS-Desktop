package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

public class PulverizacionItem extends FeatureContainer{
	private static final String COLUMNA_PASADAS = "Pasadas";
	private static final String COLUMNA_COSTO = "Costo";
	private static Map<String, String> columnsMap= new HashMap<String, String>();
	
	
	Double costoPaquete ;//= (Long) simpleFeature.getAttribute("Costo");
	Double cantPasadasHa ;//= (Integer) simpleFeature.getAttribute("Pasadas");	
	Double costoLaborHa;
	
	Double importeHa;//es el (costo de lo agroquimicos de una pasada + el costo de labor de una pasada) por la cantidad de pasadas 
	
	
	public PulverizacionItem(SimpleFeature pulvFeature,Double costoLaborHa) {
		super(pulvFeature);		
	//	this.geometry = (Geometry) pulvFeature.getDefaultGeometry();	
		
		Object cantObj = pulvFeature
				.getAttribute(getColumn(COLUMNA_COSTO));
		
		this.costoPaquete = super.getDoubleFromObj(cantObj);
		
		 cantObj = pulvFeature
				.getAttribute(getColumn(COLUMNA_PASADAS));
		
		this.cantPasadasHa = super.getDoubleFromObj(cantObj);
	
		
//		this.costoPaquete = new Double((Long) pulvFeature.getAttribute(getColumn(COLUMNA_COSTO)));
//		this.cantPasadasHa = (Integer) pulvFeature.getAttribute(getColumn(COLUMNA_PASADAS));	
		this.costoLaborHa=costoLaborHa;
		
		this.importeHa=(costoPaquete+costoLaborHa)*cantPasadasHa;		
	}


	public Double getCostoPaquete() {
		return costoPaquete;
	}

	public void setCostoPaquete(Double costoPaquete) {
		this.costoPaquete = costoPaquete;
	}

	public Double getCantPasadasHa() {
		return cantPasadasHa;
	}

	public void setCantPasadasHa(Double cantPasadasHa) {
		
		this.cantPasadasHa = cantPasadasHa;
	}
	

	public Double getCostoLaborHa() {
		return costoLaborHa;
	}

	public void setCostoLaborHa(Double costoLaborHa) {
		this.costoLaborHa = costoLaborHa;
	}

	public Double getImporteHa() {
		this.importeHa=(costoPaquete+costoLaborHa)*cantPasadasHa;		
		return importeHa;
	}

	public void setImporteHa(Double importeHa) {
		this.importeHa=importeHa;
	}

	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_COSTO);
		requiredColumns.add(COLUMNA_PASADAS);
		return requiredColumns;
	}


	@Override
	public Double getAmount() {
		return getCostoPaquete();
	}


	@Override
	protected Map<String, String> getColumnsMap() {
		return PulverizacionItem.columnsMap;
	}



	public static void setColumnsMap(Map<String, String> columns) {
		PulverizacionItem.columnsMap.clear();
		PulverizacionItem.columnsMap.putAll(columns);	
		
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
