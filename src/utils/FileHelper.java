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

import dao.config.Configuracion;
import gui.JFXMain;
import gui.Messages;
import javafx.stage.FileChooser;


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
			params.put("url", shapeFile.toURI().toURL()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		params.put("create spatial index", Boolean.TRUE); //$NON-NLS-1$


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
	
	/**
	 * 
	 * @param f1 filter Title "JPG"
	 * @param f2 filter regex "*.jpg"
	 */
	public static List<File> chooseFiles(String f1,String f2) {
		System.out.println(Messages.getString("JFXMain.403")); //$NON-NLS-1$
		List<File> files =null;
		FileChooser fileChooser = new FileChooser();
		
		fileChooser.setTitle(Messages.getString("JFXMain.404")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(f1, f2));

		//Configuracion config = Configuracion.getInstance();
		File lastFile = null;
		Configuracion config = JFXMain.config;
		String lastFileName =config.getPropertyOrDefault(Configuracion.LAST_FILE,Messages.getString("JFXMain.405")); //$NON-NLS-1$
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 	
		try{
			System.out.println(Messages.getString("JFXMain.406")+lastFile); //$NON-NLS-1$
			//if(lastFile != null && lastFile.exists()){
			System.out.println(Messages.getString("JFXMain.407")+lastFile.getParent()); //$NON-NLS-1$
			System.out.println(Messages.getString("JFXMain.408")+lastFile.getName()); //$NON-NLS-1$
			fileChooser.setInitialDirectory(lastFile.getParentFile());
			fileChooser.setInitialFileName(lastFile.getName());
			System.out.println(Messages.getString("JFXMain.409")); //$NON-NLS-1$
			files = fileChooser.showOpenMultipleDialog(JFXMain.stage);
			System.out.println(Messages.getString("JFXMain.410")); //$NON-NLS-1$
			//		file = files.get(0);
		}catch(Exception e){
			e.printStackTrace();
			try{
				fileChooser.setInitialDirectory(null);
				files = fileChooser.showOpenMultipleDialog(JFXMain.stage);
			}catch(Exception e2){
				e2.printStackTrace();
				//give up
			}

		}
		System.out.println(Messages.getString("JFXMain.411")+files); //$NON-NLS-1$

		try {
			if(files!=null && files.size()>0){
				File f = files.get(0);
				config.setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());	
				config.save();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println(Messages.getString("JFXMain.412")); //$NON-NLS-1$
		return files;
	}
}
