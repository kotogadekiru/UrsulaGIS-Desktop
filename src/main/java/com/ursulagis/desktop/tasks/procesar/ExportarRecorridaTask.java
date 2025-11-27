package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.api.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import com.google.gson.Gson;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.recorrida.Muestra;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.GeometryHelper;

/**
 * el mapa de recorrida perfecto tiene los puntos de muestreo, el camiono para recorrer y los poligonos con los ambitentes. son 3 mapas en total.
 * Lineas, puntos y poligonos
 * @author quero
 *
 */
public class ExportarRecorridaTask extends ProgresibleTask<File>{
	private Recorrida laborToExport=null;
	private File outFile=null;

	public ExportarRecorridaTask(Recorrida laborToExport,File shapeFile) {
		super();
		this.laborToExport=laborToExport;
		this.outFile=shapeFile;
	
		this.taskName= "Exportando "+laborToExport.getNombre();
		super.updateTitle(taskName);
	}

	public void run(Recorrida recorrida,File shapeFile) {

		List<Muestra> items = recorrida.muestras;
		Muestra m0 = items.get(0);

		SimpleFeatureType type = null;	

		//		String typeDescriptor = "the_geom:Point:4326,"//"*geom:Polygon,"the_geom
		//				+ COLUMNA_DISTANCIA + ":Double,"
		//				+ COLUMNA_CURSO + ":Double,"
		//				+ COLUMNA_ANCHO + ":Double,"
		//				+ COLUMNA_ELEVACION + ":Double,"
		//				+ COLUMNA_CATEGORIA + ":Integer,";

		StringBuilder sb =  new StringBuilder();
		sb.append("*the_geom:"+Point.class.getCanonicalName()+":4326");
		//sb.append("*the_geom:"+LineString.class.getCanonicalName()+":4326");
		//recorrida.nombre
		sb.append(",name" + ":"+String.class.getCanonicalName());
		sb.append(",subName" + ":"+String.class.getCanonicalName());		
		sb.append(",obs" + ":"+String.class.getCanonicalName());

		


		Map<String, String> map = constructObservacionTypesMap(m0, sb);
		String typeDescriptor =sb.toString();

		try {
			type = DataUtilities.createType("PrescType", typeDescriptor); //$NON-NLS-1$
		} catch (SchemaException e) {
			e.printStackTrace();
		}


		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok

		super.updateTitle("exportando");
		updateProgress(0, items.size());

		
		for(int i=0;i<items.size();i++) {//Muestra m:items) {//(it.hasNext()){
			
			SimpleFeature pointFeature = constructPointFeature(items, map, fb, i);
			boolean ret = exportFeatureCollection.add(pointFeature);
			if(!ret) {
				System.err.println("no se pudo agregar feature "+i+" en ExportarRecorridaTask");
			}
			
			//SimpleFeature lineFeature = constructLineFeature(items, map, fb, i);
			//exportFeatureCollection.add(lineFeature);

			updateProgress(exportFeatureCollection.size(), items.size());

		}
		//it.close();

		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1 
		SimpleFeatureSource featureSource = null;
		try {
			String typeName = newDataStore.getTypeNames()[0];
			featureSource = newDataStore.getFeatureSource(typeName);
		} catch (IOException e) {

			e.printStackTrace();
		}

		super.updateTitle("escribiendo el archivo");
		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
			Transaction transaction = new DefaultTransaction("create"); //$NON-NLS-1$
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */

			try {
				featureStore.setFeatures(exportFeatureCollection.reader());
				try {
					transaction.commit();
				} catch (Exception e1) {
					e1.printStackTrace();
				}finally {
					try {
						transaction.close();
						//System.out.println("closing transaction");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}		

		System.out.println("despues de guardar el shp el schema es: "+ shapeFile); //$NON-NLS-1$
		Configuracion config = Configuracion.getInstance();
		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
		config.save();
		updateProgress(100, 100);//all done;

	}

	public Map<String, String> constructObservacionTypesMap(Muestra m0, StringBuilder sb) {
		@SuppressWarnings("unchecked")
		Map<String,String> map = new Gson().fromJson(m0.observacion, Map.class);	 

		// props = new LinkedHashMap<String,Number>();
		for(String k : map.keySet()) {
			Object value = map.get(k);
			if(String.class.isAssignableFrom(value.getClass())) {				

				//					 try {
				//						Double.parseDouble((String)value);						 
				//					 }catch(Exception e) {
				//						 System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");
				//					}finally {
				sb.append(","+k + ":"+Double.class.getCanonicalName());//convierto los datos de string a double 
				//					}
				//fb.add(dValue);

			} else if(Number.class.isAssignableFrom(value.getClass())) {
				sb.append(","+k + ":"+Number.class.getCanonicalName());					 
			}			
		}
		return map;
	}
	
	public SimpleFeature constructPointFeature(List<Muestra> items, Map<String, String> map, SimpleFeatureBuilder fb, int i) {
		//Muestra anterior = (i==0)?items.get(0):items.get(i-1);//repito el primer item para no perder datos
		Muestra m = items.get(i);

		Geometry itemGeometry=	GeometryHelper.constructPoint(m.getPosition());
		//Geometry itemGeometry=	GeometryHelper.constructLineString(anterior.getPosition(), m.getPosition());//(m.getPosition());
		fb.add(itemGeometry);

		fb.add(i+": "+m.getNombre());
		fb.add(m.getSubNombre());
		fb.add(m.getObservacion());
		// String obs = m0.getObservacion();

		@SuppressWarnings("unchecked")
		Map<String,String> mMap = new Gson().fromJson(m.getObservacion(), Map.class);	 

		// props = new LinkedHashMap<String,Number>();
		for(String k : map.keySet()) {
			Object value = mMap.get(k);
			if(String.class.isAssignableFrom(value.getClass())) {				
				Double dValue = 0.0;
				try {
					if(value!=null && value!="" ) {
						dValue=Double.parseDouble((String)value);
					}
				}catch(Exception e) {
					e.printStackTrace();
					System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");
				}
				fb.add(dValue);

			} else if(Number.class.isAssignableFrom(value.getClass())) {

				fb.add((Number)value);

			}			 
		}

		SimpleFeature exportFeature = fb.buildFeature(null);//id generado automaticamente
		return exportFeature;
	}

	public SimpleFeature constructLineFeature(List<Muestra> items, Map<String, String> map, SimpleFeatureBuilder fb, int i) {
		Muestra anterior = (i==0)?items.get(0):items.get(i-1);//repito el primer item para no perder datos
		Muestra m = items.get(i);

		//Geometry itemGeometry=	GeometryHelper.constructPoint(m.getPosition());
		Geometry itemGeometry=	GeometryHelper.constructLineString(anterior.getPosition(), m.getPosition());//(m.getPosition());
		fb.add(itemGeometry);

		fb.add(i+":"+anterior.getNombre()+"->"+m.getNombre());
		fb.add(m.getObservacion());
		// String obs = m0.getObservacion();

		@SuppressWarnings("unchecked")
		Map<String,String> mMap = new Gson().fromJson(m.getObservacion(), Map.class);	 

		// props = new LinkedHashMap<String,Number>();
		for(String k : map.keySet()) {
			Object value = mMap.get(k);
			if(String.class.isAssignableFrom(value.getClass())) {				
				Double dValue = 0.0;
				try {
					if(!"".equals(value) ) {
						dValue=Double.parseDouble((String)value);
					}
				}catch(Exception e) {
					e.printStackTrace();
					System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");
				}
				fb.add(dValue);

			} else if(Number.class.isAssignableFrom(value.getClass())) {

				fb.add((Number)value);

			}			 
		}

		SimpleFeature exportFeature = fb.buildFeature(null);//id generado automaticamente
		return exportFeature;
	}

	
	
	@Override
	protected File call() throws Exception {
		this.run(this.laborToExport,this.outFile);
		return outFile;
	}

	//	public void exe(FertilizacionLabor laborToExport,File shapeFile)  {
	//		SimpleFeatureType type = null;
	//		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":4326,"
	//				+ FertilizacionLabor.COLUMNA_DOSIS + ":java.lang.Long";
	//		
	//		System.out.println("creando type con: "+typeDescriptor); //$NON-NLS-1$ 
	//		System.out.println("Long.SIZE="+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
	//		try {
	//			type = DataUtilities.createType("PrescType", typeDescriptor); //$NON-NLS-1$
	//		} catch (SchemaException e) {
	//			e.printStackTrace();
	//		}
	//
	//		System.out.println("PrescType:"+DataUtilities.encodeType(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$
	//
	//		SimpleFeatureIterator it = laborToExport.outCollection.features();
	//		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
	//		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok
	//		while(it.hasNext()){
	//			FertilizacionItem fi = laborToExport.constructFeatureContainerStandar(it.next(),false);
	//			Geometry itemGeometry=fi.getGeometry();
	//			List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
	//			for(Polygon p : flatPolygons){
	//				fb.add(p);
	//				Double dosisHa = fi.getDosistHa();
	//
	//				System.out.println("presc Dosis = "+dosisHa); //$NON-NLS-1$
	//				fb.add(dosisHa.longValue());
	//
	//				SimpleFeature exportFeature = fb.buildFeature(fi.getId().toString());
	//				exportFeatureCollection.add(exportFeature);
	//			}
	//		}
	//		it.close();
	//
	//		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1 
	//		SimpleFeatureSource featureSource = null;
	//		try {
	//			String typeName = newDataStore.getTypeNames()[0];
	//			featureSource = newDataStore.getFeatureSource(typeName);
	//		} catch (IOException e) {
	//
	//			e.printStackTrace();
	//		}
	//
	//
	//		if (featureSource instanceof SimpleFeatureStore) {
	//			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
	//			Transaction transaction = new DefaultTransaction("create"); //$NON-NLS-1$
	//			featureStore.setTransaction(transaction);
	//
	//			/*
	//			 * SimpleFeatureStore has a method to add features from a
	//			 * SimpleFeatureCollection object, so we use the
	//			 * ListFeatureCollection class to wrap our list of features.
	//			 */
	//
	//			try {
	//				featureStore.setFeatures(exportFeatureCollection.reader());
	//				try {
	//					transaction.commit();
	//				} catch (Exception e1) {
	//					e1.printStackTrace();
	//				}finally {
	//					try {
	//						transaction.close();
	//						//System.out.println("closing transaction");
	//					} catch (IOException e) {
	//						e.printStackTrace();
	//					}
	//				}
	//			} catch (Exception e1) {
	//				e1.printStackTrace();
	//			}
	//		}		
	//
	//		System.out.println("despues de guardar el shp el schema es: "+ shapeFile); //$NON-NLS-1$
	//		Configuracion config = Configuracion.getInstance();
	//		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
	//		config.save();
	//	}
}
