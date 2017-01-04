package dao.pulverizacion;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import dao.FeatureContainer;

public class PulverizacionItem extends FeatureContainer{
public static final String COLUMNA_COSTO_PAQUETE = "COLUMNA_COSTO_PAQUETE_PULVERIZACION";
public static final Object COLUMNA_CANT_PASADAS = "CANT_PASADAS_PULVERIZACION";
	
	
	Double costoPaquete ;//= (Long) simpleFeature.getAttribute("Costo");
	Double cantPasadasHa ;//= (Integer) simpleFeature.getAttribute("Pasadas");	
	Double costoLaborHa;
	Double importeHa;//es el (costo de lo agroquimicos de una pasada + el costo de labor de una pasada) por la cantidad de pasadas 
	
	
	public PulverizacionItem(SimpleFeature pulvFeature) {
		super(pulvFeature);		
	//	this.geometry = (Geometry) pulvFeature.getDefaultGeometry();	
		
//		Object cantObj = pulvFeature
//				.getAttribute(getColumn(COLUMNA_COSTO));
//		
//		this.costoPaquete = super.getDoubleFromObj(cantObj);
//		
//		 cantObj = pulvFeature
//				.getAttribute(getColumn(COLUMNA_PASADAS));
//		
//		this.cantPasadasHa = super.getDoubleFromObj(cantObj);
//	
//		
////		this.costoPaquete = new Double((Long) pulvFeature.getAttribute(getColumn(COLUMNA_COSTO)));
////		this.cantPasadasHa = (Integer) pulvFeature.getAttribute(getColumn(COLUMNA_PASADAS));	
//		this.costoLaborHa=costoLaborHa;
//		
//		this.importeHa=(costoPaquete+costoLaborHa)*cantPasadasHa;		
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

//	public static List<String> getRequieredColumns() {
//		List<String> requiredColumns = new ArrayList<String>();
//		requiredColumns.add(COLUMNA_COSTO);
//		requiredColumns.add(COLUMNA_PASADAS);
//		return requiredColumns;
//	}


	@Override
	public Double getAmount() {
		return getCostoPaquete();
	}


	
//	protected Map<String, String> getColumnsMap() {
//		return PulverizacionItem.columnsMap;
//	}



//	public static void setColumnsMap(Map<String, String> columns) {
//		PulverizacionItem.columnsMap.clear();
//		PulverizacionItem.columnsMap.putAll(columns);	
//		
//		columns.forEach(new BiConsumer<String, String>(){
//			@Override
//			public void accept(String key, String value) {
//				Configuracion.getInstance().setProperty(key, value);				
//			}
//			
//		});
//		
//	}


//	@Override
//	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {
//		featureBuilder.addAll(new Object[]{super.getGeometry(),
//					getCostoPaquete(),
//					getCantPasadasHa(),
//					getCostoLaborHa(),
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
				getCostoPaquete(),
				getCantPasadasHa(),
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
