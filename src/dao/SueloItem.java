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

public class SueloItem extends FeatureContainer {
	public static final String AGUA_PERFIL= "Agua Perfil";
	public static final String PROF_NAPA= "Napa";
	public static final String PPM_FOSFORO = "Fosforo";
	public static final String PPM_NITROGENO = "Nitrogeno";
	public static final String PPM_ASUFRE = "Asufre";
	public static final String PPM_MO = "Materia Organica";
	
	//private static Map<String, String> columnsMap = new HashMap<String, String>();
	
	//los ingenieros usan 2.6 para pasar de ppm a kg/ha. deben tomar la densidad en 1.3 en vez de 2
	//para pasar de Ppm a kg/ha hay que multiplicar por 2.6. 
	//es por que hay 2600tns en cada ha de 20cm de suelo.
	//ppm=x/1.000.000 => ppm/ha=X(kg/ha)/2.600.000(kg/ha)=(1/2.6)
	private Double ppmNO3_0;
	private Double ppmNO3_20;//mg P / kg de suelo
	
	private Double ppmP;
	/*La profundidad en cm hasta la napa*/
	private Double napa;
	
	private Double aguaPerfil;

	public SueloItem(SimpleFeature harvestFeature) {
		super(harvestFeature);
		System.out.println(harvestFeature);

		Object pObj = harvestFeature.getAttribute(getColumn(PPM_FOSFORO));
		this.ppmP = super.getDoubleFromObj(pObj);
		

		Object napaObj = harvestFeature.getAttribute(getColumn(PROF_NAPA));
		this.napa = super.getDoubleFromObj(napaObj);
		
		
		Object aguaOBJ = harvestFeature.getAttribute(getColumn(AGUA_PERFIL));
		this.aguaPerfil = super.getDoubleFromObj(aguaOBJ);
		
		
//		Object n0Obj = harvestFeature.getAttribute(getColumn(KG_NO3_0_COLUMN));
//		this.ppmNO3_0 = super.getDoubleFromObj(n0Obj);
//		Object n20Obj = harvestFeature
//				.getAttribute(getColumn(KG_NO3_20_COLUMN));
//		this.ppmNO3_20 = super.getDoubleFromObj(n20Obj);
	}

	public SueloItem() {
	}

//	public Double getPpmNO3_0() {
//		return ppmNO3_0;
//	}
//
//	public void setPpmNO3_0(Double ppmNO3_0) {
//		this.ppmNO3_0 = ppmNO3_0;
//	}
//
//	public Double getPpmNO3_20() {
//		return ppmNO3_20;
//	}
//
//	public void setPpmNO3_20(Double ppmNO3_20) {
//		this.ppmNO3_20 = ppmNO3_20;
//	}

	public Double getPpmP() {
		return ppmP;
	}

	public void setPpmP(Double ppmP3_0) {
		this.ppmP = ppmP3_0;
	}

	@Override
	public Double getAmount() {
		return getPpmP();
	}

	public static List<String> getRequiredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(PPM_FOSFORO);
//		requiredColumns.add(KG_NO3_0_COLUMN);
//		requiredColumns.add(KG_NO3_20_COLUMN);
		return requiredColumns;
	}

//	@Override
//	protected Map<String, String> getColumnsMap() {
//		return Suelo.columnsMap;
//	}

//	public static void setColumnsMap(Map<String, String> columns) {
//		Suelo.columnsMap.clear();
//		Suelo.columnsMap.putAll(columns);
//
//		columns.forEach(new BiConsumer<String, String>() {
//			@Override
//			public void accept(String key, String value) {
//				Configuracion.getInstance().setProperty(key, value);
//			}
//
//		});
//	}

	public static SimpleFeatureType getType() {
		SimpleFeatureType type = null;
		try {

			/*
			 * geom tiene que ser Point, Line o Polygon. no puede ser Geometry
			 * porque podria ser cualquiera y solo permite un tipo por archivo
			 * los nombre de las columnas no pueden ser de mas de 10 char
			 */

			type = DataUtilities.createType("Suelo", "*geom:Polygon,"
					+ SueloItem.PPM_FOSFORO + ":Double,");
		} catch (SchemaException e) {

			e.printStackTrace();
		}
		return type;
	}

	@Override
	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {
		featureBuilder.add(super.getGeometry());
		featureBuilder.add(getPpmP());

		SimpleFeature feature = featureBuilder.buildFeature(null);
		return feature;
	}

}
