package tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.MultiPolygon;

import dao.LaborItem;
import dao.config.Configuracion;
import dao.cosecha.CosechaLabor;
import gui.Messages;
import utils.FileHelper;


/**
 * para el cufia la semilla es la 3ra culumna, los datos tienen que estar en enteros
 * diferencia entre AGFusion y 
 * FGS (mas nuevo: permite mas flexibilidad en cantidad de zonas y cantidad de columnas. 
 * permite cargar las prescripciones en diferentes mapas) 
 * @author quero
 *
 */

public class ExportarCosechaDePuntosTask {

	public static void run(CosechaLabor laborToExport,File shapeFile) {

		SimpleFeatureType type = laborToExport.getPointType();

		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);

		SimpleFeatureIterator it = laborToExport.outCollection.features();
		DefaultFeatureCollection pointFeatureCollection =  new DefaultFeatureCollection(Messages.getString("JFXMain.356"),type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
		
		while(it.hasNext()){
			SimpleFeature sf = it.next();		
			List<Object> attributes = sf.getAttributes();
			for(Object o : attributes){
				if(o instanceof MultiPolygon){
					int index =attributes.indexOf(o);
					attributes.set(index, ((MultiPolygon)sf.getDefaultGeometry()).getCentroid());

				}
			}
			fb.addAll(sf.getAttributes());


			SimpleFeature pointFeature = fb.buildFeature(LaborItem.getID(sf));
			pointFeatureCollection.add(pointFeature);

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
