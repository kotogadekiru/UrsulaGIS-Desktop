package com.ursulagis.desktop.tasks;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.api.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Configuracion;


public class ExportLaborMapTask extends ProgresibleTask<File>{
	Labor<?> laborToExport=null;
	File shapeFile=null;
	public boolean guardarConfig=true;
	
	public ExportLaborMapTask(Labor<?> _laborToExport,File _shapeFile){		
		 laborToExport=_laborToExport;
		 shapeFile=_shapeFile;
		 super.updateTitle(taskName);
		 this.taskName= laborToExport.getNombre();
	}
	
	
	public File call()  {
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
			if(newDataStore==null)return null;
			newDataStore.createSchema(laborToExport.getType());
			

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

		String typeName = null;
		try {
			if(newDataStore==null)return null;
			typeName = newDataStore.getTypeNames()[0];
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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


}
