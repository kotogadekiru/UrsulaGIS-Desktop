package dao;

import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.factory.GeoTools;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;


public class Rentabilidad extends Dao{
	//Polygon harvestPolygon = (Polygon) geometry;
	private Double importePulvHa ;//= getImportePulv(harvestPolygon);
	private Double importeFertHa ;//= getImporteFert(harvestPolygon);
	private Double importeSiembraHa ;//= getImporteSiembra(harvestPolygon);
	private Double area ;

	private Double margenPorHa ;//= (importeCosechaPorHa * areaCosecha  - importePulv - importeFert - importeSiembra) / areaCosecha;
	//private Double costoPorHa;
	private Double ingresoHa;
//	private Double rentabilidadHa;
	private Double importeCosechaHa;
	
	
	public Rentabilidad(SimpleFeature feature) {
		super(feature);
	}	
	
	public Rentabilidad() {
	}

	public Double getImportePulvHa() {
		return importePulvHa;
	}

	public void setImportePulvHa(Double importePulvHa) {
		this.importePulvHa = importePulvHa;
	}

	public Double getImporteFertHa() {
		return importeFertHa;
	}


	public void setImporteFertHa(Double importeFertHa) {
		this.importeFertHa = importeFertHa;
	}


	public Double getImporteSiembraHa() {
		return importeSiembraHa;
	}

	public void setImporteSiembraHa(Double importeSiembraHa) {
		this.importeSiembraHa = importeSiembraHa;
	}

	public Double getArea() {
		return area;
	}

	public void setArea(Double area) {
		this.area = area;
	}

	public Double getMargenPorHa() {
		return margenPorHa;
	}

	public void setMargenPorHa(Double margenPorHa) {
		this.margenPorHa = margenPorHa;
	}

	public Double getCostoPorHa() {
		return getImporteFertHa()+getImporteSiembraHa()+getImportePulvHa();
		
	}

//	public void setCostoPorHa(Double costoPorHa) {
//		this.costoPorHa = costoPorHa;
//	}

	public Double getIngresoHa() {
		return ingresoHa;
	}

	public void setIngresoHa(Double ingresoHa) {
		this.ingresoHa = ingresoHa;
	}

	public Double getRentabilidadHa() {
		if(getCostoPorHa()>0){
			return new Double(getMargenPorHa()/getCostoPorHa()*100);
		} else{
			return new Double(0);
		}
		
	}



	@Override
	public Double getAmount() {
	
		return getRentabilidadHa();
	}
	@Override
	protected Map<String, String> getColumnsMap() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setImporteCosechaHa(Double importeCosechaPorHa) {
		this.importeCosechaHa =importeCosechaPorHa; 
		
	}
	
	public Double getImporteCosechaHa() {
		return this.importeCosechaHa; 
		
	}
	
	
	public static SimpleFeatureType getType(){
		SimpleFeatureType type=null;
		try {
			/*
			 geom tiene que ser Point, Line o Polygon. no puede ser Geometry porque podria ser cualquiera y solo permite un tipo por archivo
			 los nombre de las columnas no pueden ser de mas de 10 char
			  */
			
			type = DataUtilities.createType("Rentabilidad",
					"*geom:Polygon,"
					+ "renta:Double,"
					+ "margen:Double,"
					+ "costoTo:Double,"
					+ "ingreso:Double,"
					+ "fertili:Double,"
					+ "siembra:Double,"
					+ "pulveri:Double"
			);
		} catch (SchemaException e) {
			
			e.printStackTrace();
		}
		return type;
	}
	
	public SimpleFeature getFeature(SimpleFeatureBuilder featureBuilder){
		
		//SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(getType());
		
		featureBuilder.add(super.getGeometry());
		featureBuilder.add(getRentabilidadHa());
		featureBuilder.add(getMargenPorHa());
		featureBuilder.add(getCostoPorHa());
		featureBuilder.add(getIngresoHa());
		featureBuilder.add(getImporteFertHa());		
		featureBuilder.add(getImportePulvHa());
		featureBuilder.add(getImporteSiembraHa());		
				
		SimpleFeature feature = featureBuilder.buildFeature(null);
		return feature;
		
	}

}
