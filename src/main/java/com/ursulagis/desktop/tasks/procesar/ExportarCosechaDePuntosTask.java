package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.api.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.FileHelper;


/**
 * para el cufia la semilla es la 3ra culumna, los datos tienen que estar en enteros
 * diferencia entre AGFusion y 
 * FGS (mas nuevo: permite mas flexibilidad en cantidad de zonas y cantidad de columnas. 
 * permite cargar las prescripciones en diferentes mapas) 
 * @author quero
 *
 */

public class ExportarCosechaDePuntosTask  extends ProgresibleTask<File>{
	Labor<?> laborToExport=null;
	File shapeFile=null;
	public boolean guardarConfig=true;

	public ExportarCosechaDePuntosTask(Labor<?> _laborToExport,File _shapeFile){		
		laborToExport=_laborToExport;
		shapeFile=_shapeFile;
		super.updateTitle(taskName);
		this.taskName= laborToExport.getNombre();
	}

	public File call()  {//copiado de exportar labor
		System.out.println("llamando a call en ExportHarvestMap");
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put("url", shapeFile.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore=null;
		DefaultFeatureCollection pointFeatureCollection =null;
		try {
			String typeDescriptor = "*the_geom:"+Point.class.getCanonicalName()+":4326,"
					+ CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO +":java.lang.Double,"
					+ CosechaLabor.COLUMNA_ANCHO+":java.lang.Double,"
					+ CosechaLabor.COLUMNA_DISTANCIA+":java.lang.Double,"
					+ CosechaLabor.COLUMNA_CURSO+":java.lang.Double,"
					+ CosechaLabor.COLUMNA_ELEVACION+":java.lang.Double,";

			System.out.println("creando type con: "+typeDescriptor); 


			SimpleFeatureType pointType = DataUtilities.createType("PrescType", typeDescriptor); //$NON-NLS-1$

			//SimpleFeatureType pointType =laborToExport.getPointType();
			System.out.println("cosecha point type es "+pointType);

			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(pointType);
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

			pointFeatureCollection =  new DefaultFeatureCollection("internal",pointType); //$NON-NLS-1$
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(pointType);
			//ComplexFeatureBuilder pfb = new ComplexFeatureBuilder(pointType);
			ReferencedEnvelope bounds = laborToExport.outCollection.getBounds();
			List<CosechaItem> items = 
					(List<CosechaItem>) laborToExport.cachedOutStoreQuery(bounds);
			for(CosechaItem i: items){
				Object[] attributes = new Object[] {
						i.getGeometry().getCentroid(),
						i.getRindeTnHa(),
						i.getAncho(),
						i.getDistancia(),
						i.getRumbo(),
						i.getElevacion()
				};
				boolean res = pointFeatureCollection.add(fb.buildFeature(null,attributes));
				if(!res) {
					System.out.println("no se pudo insertar point para "+i);
				}
			}			

			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

				Transaction transaction = new DefaultTransaction("create");
				featureStore.setTransaction(transaction);

				try {
					//FeatureReader<SimpleFeatureType, SimpleFeature> reader = pointFeatureCollection.reader();			
					featureStore.addFeatures(pointFeatureCollection);			

					transaction.commit();					
					transaction.close();

				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(guardarConfig) {
			//TODO guardar un archivo txt con la configuracion de la labor para que quede como registro de las operaciones
			Configuracion config = Configuracion.getInstance();
			config.loadProperties();
			config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
			config.save();
		}
		return shapeFile;
	}

	@Deprecated
	//se llama el call del Task
	public static void run(CosechaLabor laborToExport,File shapeFile) {
		SimpleFeatureType type = laborToExport.getPointType();

		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);

		SimpleFeatureIterator it = laborToExport.outCollection.features();
		DefaultFeatureCollection pointFeatureCollection =  new DefaultFeatureCollection("internal",type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
		System.out.println("exportando type: "+type);
		while(it.hasNext()){
			SimpleFeature sf = it.next();		
			List<Object> attributes = sf.getAttributes();
			for(Object o : attributes){
				if(o instanceof MultiPolygon){
					int index =attributes.indexOf(o);
					attributes.set(index, ((MultiPolygon)sf.getDefaultGeometry()).getCentroid());

				}
			}
			System.out.println(attributes);
			fb.addAll(attributes);//Can handle 12 attributes only, index is 12

			//FIXME no se esta exportando bien la cosecha a puntos
			SimpleFeature pointFeature = fb.buildFeature(null);
			boolean ret = pointFeatureCollection.add(pointFeature);
			if(!ret) {
				System.err.println("no se pudo agregar la feature id "+LaborItem.getID(sf)+" en ExportarCosechaDePuntos" );
			}

		}
		it.close();

		SimpleFeatureSource featureSource = null;
		try {
			String typeName = newDataStore.getTypeNames()[0];
			featureSource = newDataStore.getFeatureSource(typeName);
		} catch (IOException e) {

			e.printStackTrace();
		}


		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			Transaction transaction = new DefaultTransaction("create"); //$NON-NLS-1$
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */

			try {
				featureStore.setFeatures(pointFeatureCollection.reader());
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

		Configuracion config = Configuracion.getInstance();
		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
		config.save();
	}

}
