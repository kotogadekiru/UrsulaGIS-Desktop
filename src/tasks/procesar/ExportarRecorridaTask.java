package tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Labor;
import dao.LaborItem;
import dao.config.Configuracion;

import dao.pulverizacion.PulverizacionItem;
import dao.pulverizacion.PulverizacionLabor;
import dao.recorrida.Muestra;
import dao.recorrida.Recorrida;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gui.Messages;
import tasks.ProgresibleTask;
import utils.FileHelper;
import utils.GeometryHelper;
import utils.PolygonValidator;
import utils.ProyectionConstants;


/**
 * para el cufia la semilla es la 3ra culumna, los datos tienen que estar en enteros
 * diferencia entre AGFusion y 
 * FGS (mas nuevo: permite mas flexibilidad en cantidad de zonas y cantidad de columnas. 
 * permite cargar las prescripciones en diferentes mapas) 
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
		super.updateTitle(taskName);
		this.taskName= laborToExport.getNombre();
	}
	
	public void run(Recorrida recorrida,File shapeFile) {
		
		List<Muestra> items = recorrida.muestras;
		Muestra m0 = items.get(0);
		
		SimpleFeatureType type = null;	
		
//		String typeDescriptor = "the_geom:Point:srid=4326,"//"*geom:Polygon,"the_geom
//				+ COLUMNA_DISTANCIA + ":Double,"
//				+ COLUMNA_CURSO + ":Double,"
//				+ COLUMNA_ANCHO + ":Double,"
//				+ COLUMNA_ELEVACION + ":Double,"
//				+ COLUMNA_CATEGORIA + ":Integer,";

		StringBuilder sb =  new StringBuilder();
		sb.append("*the_geom:"+Point.class.getCanonicalName()+":srid=4326");
		//recorrida.nombre
		sb.append(",nombre" + ":"+String.class.getCanonicalName());
		sb.append(",obs" + ":"+String.class.getCanonicalName());
		
		 @SuppressWarnings("unchecked")
			Map<String,String> map = new Gson().fromJson(m0.observacion, Map.class);	 
			 
			// props = new LinkedHashMap<String,Number>();
			 for(String k : map.keySet()) {
				 Object value = map.get(k);
				 if(String.class.isAssignableFrom(value.getClass())) {				
					
//					 try {
//						new Double((String)value);						 
//					 }catch(Exception e) {
//						 System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");
//					}finally {
						 sb.append(","+k + ":"+Double.class.getCanonicalName());
//					}
					 //fb.add(dValue);
					 
				 } else if(Number.class.isAssignableFrom(value.getClass())) {
					 sb.append(","+k + ":"+Number.class.getCanonicalName());					 
				 }			
			 }
		
		
		
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
		
		for(Muestra m:items) {//(it.hasNext()){
			
			Geometry itemGeometry=	GeometryHelper.constructPoint(m.getPosition());
			fb.add(itemGeometry);
			
			fb.add(m.getNombre());
			fb.add(m.getObservacion());
			// String obs = m0.getObservacion();
			 
			 @SuppressWarnings("unchecked")
			Map<String,String> mMap = new Gson().fromJson(m.getObservacion(), Map.class);	 
			 
			// props = new LinkedHashMap<String,Number>();
			 for(String k : map.keySet()) {
				 Object value = mMap.get(k);
				 if(String.class.isAssignableFrom(value.getClass())) {				
					 Double dValue = new Double(0);
					 try {
						 dValue=new Double((String)value);
					 }catch(Exception e) {
						 System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");
					}
					 fb.add(dValue);
					 
				 } else if(Number.class.isAssignableFrom(value.getClass())) {
					 
					 fb.add((Number)value);
					 
				 }			
			 }

			SimpleFeature exportFeature = fb.buildFeature(null);//id generado automaticamente
			exportFeatureCollection.add(exportFeature);
			
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

	@Override
	protected File call() throws Exception {
		this.run(this.laborToExport,this.outFile);
		return outFile;
	}

//	public void exe(FertilizacionLabor laborToExport,File shapeFile)  {
//		SimpleFeatureType type = null;
//		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":srid=4326,"
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
//		System.out.println("PrescType:"+DataUtilities.spec(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$
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
