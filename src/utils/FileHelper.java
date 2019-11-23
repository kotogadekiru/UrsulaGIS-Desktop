package utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.opengis.feature.simple.SimpleFeatureType;

import gui.Messages;

public class FileHelper {
	public static  List<File> selectShpFiles(Path uploadedShpFilePath) {
		List<File> shpFiles =selectAllFiles(uploadedShpFilePath);

		FileNameExtensionFilter filter = new FileNameExtensionFilter("shp only","shp");

		// List<File> shpFiles = db.getFiles();
		shpFiles.removeIf(f->{
			return !filter.accept(f) || f.isDirectory();
		});
		return shpFiles;
	}
	
	public static File selectPropertiesFile(Path path){
		List<File> shpFiles =selectAllFiles(path);

		FileNameExtensionFilter filter = new FileNameExtensionFilter("properties","properties");

		// List<File> shpFiles = db.getFiles();
		shpFiles.removeIf(f->{
			return !filter.accept(f) || f.isDirectory();
		});
		if(shpFiles.isEmpty())return null;
		return shpFiles.get(0);
	}
	public static  List<File> selectAllFiles(Path uploadedShpFilePath) {
		List<File> shpFiles = new LinkedList<File>();
		try(Stream<Path> paths = Files.walk(uploadedShpFilePath)) {
			paths.forEach(filePath -> {
				if(!filePath.toFile().isDirectory()){
					System.out.println("agregando "+filePath+" a la respuesta");
					shpFiles.add(filePath.toFile());
				}
			});
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
		return shpFiles;
	}
	
	public static ShapefileDataStore createShapefileDataStore(File shapeFile,	SimpleFeatureType type) {
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		try {
			params.put(Messages.getString("JFXMain.358"), shapeFile.toURI().toURL()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put(Messages.getString("JFXMain.359"), Boolean.TRUE); //$NON-NLS-1$


		ShapefileDataStore newDataStore=null;
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			newDataStore.createSchema(type);
			//newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
			//java.io.FileNotFoundException: D:\Dropbox\hackatonAgro\EmengareGis\MapasCrudos\shp\sup\out\grid\amb\Girszol_lote_19_s0limano_-_Harvesting.shp (Access is denied)
		}
		return newDataStore;
	}
}
