
package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

public class Suelo extends Dao {
	public static final String KG_P_COLUMN = "Fosforo";
	public static final String KG_NO3_0_COLUMN = "Nitrogeno_0";
	public static final String KG_NO3_20_COLUMN = "Nitrogeno_20";
	private static Map<String, String> columnsMap= new HashMap<String, String>();
	
	private Double ppmNO3_0;	
	private Double ppmNO3_20;	
	private Double ppmP3_0;	
	
	
	
	
	public Suelo(SimpleFeature harvestFeature, Double precioFert,Double precioPasada) {
		super(harvestFeature);
		//System.out.println(harvestFeature);		
				 
		Object pObj = harvestFeature.getAttribute(getColumn(KG_P_COLUMN));
		
		this.ppmP3_0 = super.getDoubleFromObj(pObj);	
		Object n0Obj= harvestFeature.getAttribute(getColumn(KG_NO3_0_COLUMN));
		this.ppmNO3_0 = super.getDoubleFromObj(n0Obj);	
		Object n20Obj= harvestFeature.getAttribute(getColumn(KG_NO3_20_COLUMN));
		this.ppmNO3_20 = super.getDoubleFromObj(n20Obj);			
	}
	

	public Double getPpmNO3_0() {
		return ppmNO3_0;
	}

	public void setPpmNO3_0(Double ppmNO3_0) {
		this.ppmNO3_0 = ppmNO3_0;
	}

	public Double getPpmNO3_20() {
		return ppmNO3_20;
	}

	public void setPpmNO3_20(Double ppmNO3_20) {
		this.ppmNO3_20 = ppmNO3_20;
	}

	public Double getPpmP3_0() {
		return ppmP3_0;
	}

	public void setPpmP3_0(Double ppmP3_0) {
		this.ppmP3_0 = ppmP3_0;
	}

	@Override
	public Double getAmount() {		
		return getPpmNO3_0();
	}


	public static  List<String> getRequiredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(KG_P_COLUMN);
		requiredColumns.add(KG_NO3_0_COLUMN);
		requiredColumns.add(KG_NO3_20_COLUMN);
		return requiredColumns;		
	}


	@Override
	protected Map<String, String> getColumnsMap() {
		return Suelo.columnsMap;
	}
	

	public static void setColumnsMap(Map<String, String> columns) {
		Suelo.columnsMap.clear();
		Suelo.columnsMap.putAll(columns);
		
		columns.forEach(new BiConsumer<String, String>(){
			@Override
			public void accept(String key, String value) {
				Configuracion.getInstance().setProperty(key, value);				
			}
			
		});
	}

	
	
	public static SimpleFeatureType getType(){
		SimpleFeatureType type=null;
		try {
			
/*
			 geom tiene que ser Point, Line o Polygon. no puede ser Geometry porque podria ser cualquiera y solo permite un tipo por archivo
			 los nombre de las columnas no pueden ser de mas de 10 char
			  */

			
			type = DataUtilities.createType("Suelo",
					"*geom:Polygon,"
					+ Cosecha.COLUMNA_DISTANCIA+":Double,"
					+ Cosecha.COLUMNA_CURSO+":Double,"
					+ Cosecha.COLUMNA_ANCHO+":Double,"
					+ Cosecha.COLUMNA_RENDIMIENTO+":Double,"
					+ Cosecha.COLUMNA_VELOCIDAD+":Double,"
					+ Cosecha.COLUMNA_PRECIO+":Double,"
					+Cosecha.COLUMNA_IMPORTE_HA+":Double"
			);
		} catch (SchemaException e) {
			
			e.printStackTrace();
		}
		return type;
	}

@Override
	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {
	featureBuilder.add(super.getGeometry());
	featureBuilder.add(getDistancia());
	featureBuilder.add(getRumbo());
	featureBuilder.add(getAncho());
	featureBuilder.add(getRindeTnHa());
	featureBuilder.add(getVelocidad());		
	featureBuilder.add(getPrecioTnGrano());
	featureBuilder.add(getImporteHa());		
			
	SimpleFeature feature = featureBuilder.buildFeature(null);
	return feature;
	}
	
}

