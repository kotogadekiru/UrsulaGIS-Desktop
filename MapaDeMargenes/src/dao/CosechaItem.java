package dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.identity.FeatureId;

import tasks.HarvestFiltersConfig;

import com.vividsolutions.jts.geom.Geometry;

public class CosechaItem extends FeatureContainer{	
	private static final int KG_POR_TN = 1000;
	
	private static final String COLUMNA_VELOCIDAD = "Elevacion";
	public static final String COLUMNA_RENDIMIENTO = "Rendimiento";
	private static final String COLUMNA_ANCHO = "Ancho";
	private static final String COLUMNA_CURSO = "Curso(deg)";
	private static final String COLUMNA_DISTANCIA = "Distancia";
	private static final String COLUMNA_PASADA = "Num__de_pa";
	//private static final String COLUMNA_ID = "Id_obj_";

	private static final String COLUMNA_PRECIO = "precio_grano";

	private static final String COLUMNA_IMPORTE_HA = "importe_ha";
	
	@Deprecated//(value ="usar CosechaLabor.columnsMap");
	private static Map<String, String> columnsMap= new HashMap<String, String>();

	
	private static Double correccionRinde = new Double(100);

	Double distancia;
	Double rumbo;
	Double ancho;
	Double rindeTnHa;
	Double elevacion;
	Double precioTnGrano;
	Double importeHa;
	Double pasada;
	Double id;

	public CosechaItem(SimpleFeature harvestFeature, Double precioGrano) {
		super(harvestFeature);
		
		double toMetros=HarvestFiltersConfig.getInstance().getMetrosPorUnidadDistancia();
		
		String distColumn = getColumn(COLUMNA_DISTANCIA);
		try{
			distancia = new Double(distColumn);
		}catch(Exception e){
			Object distAttribute = harvestFeature.getAttribute(distColumn);
		
		distancia =
				super.getDoubleFromObj(distAttribute)
				*toMetros;		
		}
		
		rumbo =super.getDoubleFromObj(harvestFeature.getAttribute(getColumn(COLUMNA_CURSO)));
		
		pasada =super.getDoubleFromObj(harvestFeature.getAttribute(getColumn(COLUMNA_PASADA)));
		
		String identifier = harvestFeature.getIdentifier().getID();
		String[] split = identifier.split("\\.");
	
		id =super.getDoubleFromObj(split[split.length-1]);
		

		String anchoColumn = getColumn(COLUMNA_ANCHO);
//		try{
//			ancho = new Double(anchoColumn);
//		}catch(Exception e){
//			Object distAttribute = harvestFeature.getAttribute(distColumn);
//		
//			ancho =
//				super.getDoubleFromObj(distAttribute)
//				*toMetros;		
//		}
		
		ancho = super.getDoubleFromObj(harvestFeature.getAttribute(anchoColumn));
		ancho=ancho*toMetros;
			
		Double rindeDouble =  super.getDoubleFromObj(harvestFeature.getAttribute(getColumn( COLUMNA_RENDIMIENTO)));
		
		if(rindeDouble>100){//esta en kilogramos
			rindeTnHa = rindeDouble *(correccionRinde/100);//KG_POR_TN ;
		} else if(rindeDouble <1){ //esta en decenas de toneladas?
		
				rindeTnHa = rindeDouble *(correccionRinde/100);// *4.28;
		}else{
			rindeTnHa = rindeDouble *(correccionRinde/100) ;
		}
	

		elevacion = super.getDoubleFromObj(harvestFeature.getAttribute(getColumn(COLUMNA_VELOCIDAD)))*toMetros;
		this.precioTnGrano = precioGrano;
		this.importeHa = rindeTnHa * this.precioTnGrano;
	}



	public Double getDistancia() {
		return distancia;
	}

	public void setDistancia(Double distancia) {
		this.distancia = distancia;
	}

	public Double getRumbo() {
		return rumbo;
	}

	public void setRumbo(Double rumbo) {
		this.rumbo = rumbo;
	}

	public Double getAncho() {
		return ancho;
	}
	
	public Double getPasada() {
		return pasada;
	}

	public Double getId() {
		return id;
	}

	public void setAncho(Double ancho) {
		this.ancho = ancho;
	}

	public Double getRindeTnHa() {
		return rindeTnHa;
	}

	public void setRindeTnHa(Double rindeTnHa) {
		this.rindeTnHa = rindeTnHa;
	}

	public Double getElevacion() {
		return elevacion;
	}

	public void setElevacion(Double elev) {
		this.elevacion = elev;
	}

	public Double getPrecioTnGrano() {
		return precioTnGrano;
	}

	public void setPrecioTnGrano(Double precioTnGrano) {
		this.precioTnGrano = precioTnGrano;
	}

	public Double getImporteHa() {
		this.importeHa = rindeTnHa * this.precioTnGrano;
		return importeHa;
	}

	public void setImporteHa(Double importeHa) {
		this.importeHa = importeHa;
	}
	
	@Deprecated //use CosechaLabor.getRequieredcolumns
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

	@Override
	public Double getAmount() {
		return getRindeTnHa();
	}



	@Override
	protected Map<String, String> getColumnsMap() {
		return CosechaItem.columnsMap;
	}

	public static String getColumnName(String internalName){
		return CosechaItem.columnsMap.get(internalName);
	}


	public static void setColumnsMap(Map<String, String> columns) {
		CosechaItem.columnsMap.clear();
		CosechaItem.columnsMap.putAll(columns);		
		
		columns.forEach(new BiConsumer<String, String>(){
			@Override
			public void accept(String key, String value) {
				Configuracion.getInstance().setProperty(key, value);				
			}
			
		});
		
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
					+ CosechaItem.COLUMNA_DISTANCIA+":Double,"
					+ CosechaItem.COLUMNA_CURSO+":Double,"
					+ CosechaItem.COLUMNA_ANCHO+":Double,"
					+ CosechaItem.COLUMNA_RENDIMIENTO+":Integer,"
					+ CosechaItem.COLUMNA_VELOCIDAD+":Double,"
					+ CosechaItem.COLUMNA_PRECIO+":Double,"
					+CosechaItem.COLUMNA_IMPORTE_HA+":Double"
			);
		} catch (SchemaException e) {
			
			e.printStackTrace();
		}
		return type;
	}

@Override
	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder) {
	featureBuilder.add(super.getGeometry());
	featureBuilder.add(round(getDistancia()));
	featureBuilder.add(getRumbo());
	featureBuilder.add(round(getAncho()));
	featureBuilder.add(getRindeTnHa().intValue());
	featureBuilder.add(round(getElevacion()));	
	featureBuilder.add(round(getPrecioTnGrano()));
	featureBuilder.add(round(getImporteHa()));		
			
	SimpleFeature feature = featureBuilder.buildFeature(null);
	return feature;
	}

private double round(double d){
	//return d;
	//return Math.round(d*100)/100;
//	
	try {
		BigDecimal bd = new BigDecimal(d);//java.lang.NumberFormatException: Infinite or NaN
		bd = bd.setScale(3, RoundingMode.HALF_UP);
		return bd.doubleValue();
	} catch (Exception e) {
		System.err.println("CosechaItem::round");
		e.printStackTrace();
		return 0;
	}

}

@Override
public String toString(){
	String ret =new String("id"+id+"\n");
	ret += "ndistancia="+ distancia+"\n";
	ret += "rumbo="+ rumbo+"\n";
	ret += "ancho="+ ancho+"\n";
	ret += "rindeTnHa="+ rindeTnHa+"\n";
	ret += "velocidad="+ elevacion+"\n";	
	ret += "pasada="+ pasada+"\n";
	return ret;
}
}
