package tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.MultiPolygon;

import dao.Labor;
import dao.LaborItem;
import dao.config.Configuracion;
import dao.cosecha.CosechaLabor;
import gui.Messages;
import tasks.ProgresibleTask;
import utils.FileHelper;


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
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(laborToExport.getPointType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
			//FIXME a veces me da access us denied
			//java.io.FileNotFoundException: D:\Dropbox\hackatonAgro\EmengareGis\MapasCrudos\shp\sup\out\grid\amb\Girszol_lote_19_s0limano_-_Harvesting.shp (Access is denied)
		}

		String typeName = newDataStore.getTypeNames()[0];
		//	System.out.println("typeName 0 del newDataStore es "+typeName);
		SimpleFeatureSource featureSource = null;
		try {
			featureSource = newDataStore.getFeatureSource(typeName);
			//	System.out.println("cree new featureSource "+featureSource.getInfo());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			
			Transaction transaction = new DefaultTransaction("create");
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */
			//	SimpleFeatureCollection collection = new ListFeatureCollection(CosechaItem.getType(), features);
			//	System.out.println("agregando features al store " +collection.size());
			//	DefaultFeatureCollection colectionToSave = ;
	
			try {
				
				featureStore.setFeatures(laborToExport.outCollection.reader());
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
		if(guardarConfig) {
		//TODO guardar un archivo txt con la configuracion de la labor para que quede como registro de las operaciones
		 Configuracion config = Configuracion.getInstance();
		 	config.loadProperties();
			config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
			config.save();
		}
		return shapeFile;
	}

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


			SimpleFeature pointFeature = fb.buildFeature(LaborItem.getID(sf));
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
