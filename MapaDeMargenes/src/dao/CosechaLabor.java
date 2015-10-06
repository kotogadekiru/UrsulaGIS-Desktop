
package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.identity.FeatureId;

import tasks.HarvestFiltersConfig;

import com.vividsolutions.jts.geom.Geometry;

public class CosechaLabor extends Labor<CosechaItem>{	
	private static final int KG_POR_TN = 1000;
	
	private static final String COLUMNA_VELOCIDAD = "Velocidad";
	public static final String COLUMNA_RENDIMIENTO = "Rendimiento";
	private static final String COLUMNA_ANCHO = "Ancho";
	private static final String COLUMNA_CURSO = "Curso(deg)";
	private static final String COLUMNA_DISTANCIA = "Distancia";
	private static final String COLUMNA_PASADA = "Num__de_pa";
	
	private static final String COLUMNA_PRECIO = "precio_grano";
	private static final String COLUMNA_IMPORTE_HA = "importe_ha";
	
	private static Double correccionRinde = new Double(100);
	
	private Double precioTnGrano;
	
	public CosechaLabor(FileDataStore store) {

		double toMetros=HarvestFiltersConfig.getInstance().getMetrosPorUnidadDistancia();
	
			

	}

	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_VELOCIDAD);		
		requiredColumns.add(COLUMNA_RENDIMIENTO);	
		requiredColumns.add(COLUMNA_ANCHO);	
		requiredColumns.add(COLUMNA_CURSO);	
		requiredColumns.add(COLUMNA_DISTANCIA);	
		requiredColumns.add(COLUMNA_PASADA);	
	//	requiredColumns.add(COLUMNA_ID);	
		return requiredColumns;
	}


	public static void setCorreccionRinde(Double correccionRinde1) {
		//System.out.println("nuevo correccion rinde es "+correccionRinde1);
		correccionRinde  = correccionRinde1;
		
	}
	public static SimpleFeatureType getType(){
		SimpleFeatureType type=null;
		try {
		
			/*
			 geom tiene que ser Point, Line o Polygon. no puede ser Geometry porque podria ser cualquiera y solo permite un tipo por archivo
			 los nombre de las columnas no pueden ser de mas de 10 char
			  */

			
			type = DataUtilities.createType("Cosecha",
					"*geom:Polygon,"
					+ CosechaLabor.COLUMNA_DISTANCIA+":Double,"
					+ CosechaLabor.COLUMNA_CURSO+":Double,"
					+ CosechaLabor.COLUMNA_ANCHO+":Double,"
					+ CosechaLabor.COLUMNA_RENDIMIENTO+":Double,"
					+ CosechaLabor.COLUMNA_VELOCIDAD+":Double,"
					+ CosechaLabor.COLUMNA_PRECIO+":Double,"
					+CosechaLabor.COLUMNA_IMPORTE_HA+":Double"
			);
		} catch (SchemaException e) {
			
			e.printStackTrace();
		}
		return type;
	}


}

