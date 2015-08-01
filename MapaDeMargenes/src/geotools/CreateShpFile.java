package geotools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.GeoTools;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * This example reads data for point locations and associated attributes from a
 * comma separated text (CSV) file and exports them as a new shapefile. It
 * illustrates how to build a feature type.
 * <p>
 * Note: to keep things simple in the code below the input file should not have
 * additional spaces or tabs between fields.
 */
public class CreateShpFile extends Application {

	public void csv2Shp(File file) throws Exception {

		// File file = JFileDataStoreChooser.showOpenFile("csv", null);
		// if (file == null) {
		// return;
		// }
		//
		/*
		 * We use the DataUtilities class to create a FeatureType that will
		 * describe the data in our shapefile.
		 * 
		 * See also the createFeatureType method below for another, more
		 * flexible approach.
		 */
		final SimpleFeatureType TYPE = DataUtilities.createType("Location",
				"location:Point:srid=4326," + // <- the geometry attribute:
												// Point type
						"name:String," + // <- a String attribute
						"number:Integer" // a number attribute
		);

		

		/*
		 * GeometryFactory will be used to create the geometry attribute of each
		 * feature (a Point object for the location)
		 */
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		/*
		 * A list to collect features as we create them.
		 */
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		try {
			/* First line of the data file is the header */
			String line = reader.readLine();
			System.out.println("Header: " + line);

			for (line = reader.readLine(); line != null; line = reader
					.readLine()) {
				if (line.trim().length() > 0) { // skip blank lines
					String tokens[] = line.split("\\,");

					double latitude = Double.parseDouble(tokens[0]);
					double longitude = Double.parseDouble(tokens[1]);
					String name = tokens[2].trim();// trim saca los espacios
					int number = Integer.parseInt(tokens[3].trim());

					/* Longitude (= x coord) first ! */
					Point point = geometryFactory.createPoint(new Coordinate(
							longitude, latitude));

					featureBuilder.add(point);
					featureBuilder.add(name);
					featureBuilder.add(number);
					SimpleFeature feature = featureBuilder.buildFeature(null);
					features.add(feature);
				}
			}
		} finally {
			reader.close();
		}
		/*
		 * Get an output file name and create the new shapefile
		 */
		File newFile = getNewShapeFile(file);		

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(TYPE);

		/*
		 * You can comment out this line if you are using the createFeatureType
		 * method (at end of class file) rather than DataUtilities.createType
		 */
		newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

		/*
		 * Write the features to the shapefile
		 */
	

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		if (featureSource instanceof SimpleFeatureStore) {			
			
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			Transaction transaction = new DefaultTransaction("create");
			featureStore.setTransaction(transaction);
			
			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */
			SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
			try {
				featureStore.addFeatures(collection);
				transaction.commit();

			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();

			} finally {
				transaction.close();
			}
			System.exit(0); // success!
		} else {
			System.out
					.println(typeName + " does not support read/write access");
			System.exit(1);
		}
	}

	/**
	 * Prompt the user for the name and path to use for the output shapefile
	 * 
	 * @param csvFile
	 *            the input csv file used to create a default shapefile name
	 * 
	 * @return name and path for the shapefile as a new File object
	 */
	private static File getNewShapeFile(File csvFile) {
		String path = csvFile.getAbsolutePath();
		String newPath = path.substring(0, path.length() - 4) + ".shp";

		JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
		chooser.setDialogTitle("Save shapefile");
		chooser.setSelectedFile(new File(newPath));

		int returnVal = chooser.showSaveDialog(null);

		if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
			// the user cancelled the dialog
			System.exit(0);
		}

		File newFile = chooser.getSelectedFile();
		if (newFile.equals(csvFile)) {
			System.out.println("Error: cannot replace " + csvFile);
			System.exit(0);
		}

		return newFile;
	}

	@Override
	public void start(Stage stage) throws Exception {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");

		fileChooser.getExtensionFilters().addAll(
		// new FileChooser.ExtensionFilter("All Images", "*.*"),
		// new FileChooser.ExtensionFilter("JPG", "*.jpg"),
				new FileChooser.ExtensionFilter("CSV", "*.csv"));

		File file = fileChooser.showOpenDialog(stage);
		this.csv2Shp(file);

		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
		Application.launch(args);
	}
}
