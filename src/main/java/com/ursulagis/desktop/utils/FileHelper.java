package com.ursulagis.desktop.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.api.feature.simple.SimpleFeatureType;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import javafx.stage.FileChooser;
import com.ursulagis.desktop.tasks.ExportLaborMapTask;


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
	public static Path createTempDir(String prefix) {
		Path tempDirWithPrefix=null;
		try {
			tempDirWithPrefix = Files.createTempDirectory(prefix);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tempDirWithPrefix;
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
		config.loadProperties();
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

	public static List<FileDataStore> chooseShapeFileAndGetMultipleStores(List<File> files) {
		if(files==null){
			//	List<File> 
			files =chooseFiles(Messages.getString("JFXMain.401"), Messages.getString("JFXMain.402")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		List<FileDataStore> stores = new ArrayList<FileDataStore>();
		if (files != null) {
			
			List<File> shapeFiles = files.stream().filter(f->f.getName().endsWith("shp")).collect(Collectors.toList());
			for(File f : shapeFiles){
				try {
					stores.add(FileDataStoreFinder.getDataStore(f));//esto falla con java10 :(
				} catch (IOException e) {
					e.printStackTrace();
				}
				//stage.setTitle(TITLE_VERSION+" "+f.getName());
				//Configuracion.getInstance().setProperty(Configuracion.LAST_FILE,f.getAbsolutePath());

			}


			/*
			 * miro el archivo y pregunto cuales son las columnas
			 * correspondientes
			 */

		}
		return stores;
	}


	//	/**
	//	 * 
	//	 * @param f1 filter Title "JPG"
	//	 * @param f2 filter regex "*.jpg"
	//	 */
	//	private List<File> chooseFiles(String f1,String f2) {
	//		return FileHelper.chooseFiles(f1, f2);
	//	}

	/**
	 * este metodo se usa para crear archivos shp al momento de exportar mapas
	 * @param nombre es el nombre del archivo que se desea crear
	 * @return el archivo creado en la carpeta seleccionada por el usuario
	 */
	public static File getNewShapeFile(String nombre) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("JFXMain.413")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(Messages.getString("JFXMain.414"), Messages.getString("JFXMain.415"))); //$NON-NLS-1$ //$NON-NLS-2$


		File lastFile = null;
		//Configuracion config =Configuracion.getInstance();
		Configuracion config = JFXMain.config;
		config.loadProperties();
		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 
		//if(lastFile != null && lastFile.exists()){
		fileChooser.setInitialDirectory(lastFile.getParentFile());

		if(nombre == null){
			nombre = lastFile.getName();
		}
		fileChooser.setInitialFileName(nombre);

		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());

		File file = fileChooser.showSaveDialog(JFXMain.stage);
		if(file!=null) {
			config.setProperty(Configuracion.LAST_FILE, file.getAbsolutePath());
			config.save();
		}
		System.out.println(Messages.getString("JFXMain.416")+file); //$NON-NLS-1$

		return file;
	}


	/**
	 * este metodo se usa para crear archivos tiff al momento de exportar mapas
	 * @param nombre es el nombre del archivo que se desea crear
	 * @return el archivo creado en la carpeta seleccionada por el usuario
	 */
	public static File getNewTiffFile(String nombre) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("JFXMain.417")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(Messages.getString("JFXMain.418"), Messages.getString("JFXMain.419"))); //$NON-NLS-1$ //$NON-NLS-2$

		File lastFile = null;
		//Configuracion config =Configuracion.getInstance();
		Configuracion config = JFXMain.config;
		config.loadProperties();
		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 
		//if(lastFile != null && lastFile.exists()){
		fileChooser.setInitialDirectory(lastFile.getParentFile());

		if(nombre == null){
			nombre = lastFile.getName();
		}
		fileChooser.setInitialFileName(nombre);


		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());

		File file = fileChooser.showSaveDialog(JFXMain.stage);
		if(file!=null) {
			config.setProperty(Configuracion.LAST_FILE, file.getAbsolutePath());
			config.save();
		}
		System.out.println(Messages.getString("JFXMain.420")+file); //$NON-NLS-1$

		return file;
	}

	/**
	 * este metodo se usa para crear archivos tiff al momento de exportar mapas
	 * @param nombre es el nombre del archivo que se desea crear
	 * @return el archivo creado en la carpeta seleccionada por el usuario
	 */
	public static File getNewFile(String nombre,String ext) {
		FileChooser fileChooser = new FileChooser();		
		fileChooser.setTitle(Messages.getString("JFXMain.417")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(ext.toUpperCase(), ext)); //$NON-NLS-1$ //$NON-NLS-2$
		File lastFile = null;
		//Configuracion config =Configuracion.getInstance();
		Configuracion config = JFXMain.config;
		config.loadProperties();
		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 
		//if(lastFile != null && lastFile.exists()){
		fileChooser.setInitialDirectory(lastFile.getParentFile());

		if(nombre == null){
			nombre = lastFile.getName();
		}
		fileChooser.setInitialFileName(nombre);


		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());

		File file = fileChooser.showSaveDialog(JFXMain.stage);
		//File file = fileChooser.showOpenDialog(JFXMain.stage);
		if(file!=null) {
			config.setProperty(Configuracion.LAST_FILE, file.getAbsolutePath());
			config.save();
		}
		System.out.println(Messages.getString("JFXMain.420")+file); //$NON-NLS-1$

		return file;
	}

	/**
	 * este metodo se usa para crear archivos tiff al momento de exportar mapas
	 * @param nombre es el nombre del archivo que se desea crear
	 * @return el archivo creado en la carpeta seleccionada por el usuario
	 */
	public static File chooseFile(String path,String ext) {
		FileChooser fileChooser = new FileChooser();		
		fileChooser.setTitle(Messages.getString("JFXMain.417")); //$NON-NLS-1$
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter(ext.toUpperCase(), ext)); //$NON-NLS-1$ //$NON-NLS-2$
		File lastFile = new File(path);
		System.out.println("proyecto actual "+lastFile);
		Configuracion config = JFXMain.config;
		if(lastFile==null||!lastFile.exists()) {			
			String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
			System.out.println("lasfFile era "+lastFileName);
			if(lastFileName != null){
				lastFile = new File(lastFileName);
			}
			if(lastFile ==null || ! lastFile.exists()) {
				lastFile=File.listRoots()[0];
			} 
		}
		fileChooser.setInitialDirectory(lastFile.getParentFile());	

		File file = fileChooser.showOpenDialog(JFXMain.stage);
		if(file!=null) {
			config.setProperty(Configuracion.LAST_FILE, file.getAbsolutePath());
			config.save();
		}
		System.out.println(Messages.getString("archivo seleccionado para guardar")+file); 

		return file;
	}

	public static File getNewShapeFileAt(Path dir,String fileName) {
		String filePath = dir.toString() + File.separator + fileName;
		return new File(filePath);
	}

	public static File zipLaborToTmpDir(Labor<?> labor) {
		//1 crear un directorio temporal
		Path dir = FileHelper.createTempDir("toUpload");
		//2 crear un archivo shape dentro del directorio para subir
		File shpFile = FileHelper.getNewShapeFileAt(dir,"labor.shp");
		//2 exportar la labor al directorio
		ExportLaborMapTask export = new ExportLaborMapTask(labor,shpFile);
		export.guardarConfig=false;//como es un temp dir no quiero guardar LAST_FILE
		export.call();
		File zipFile = UnzipUtility.zipFiles(FileHelper.selectAllFiles(dir),dir.toFile());
		return zipFile;
	}

	public static byte[]  fileToByteArray(File f) {		
		byte[] byteArray = new byte[(int) f.length()];

		try {
			FileInputStream fileInputStream = new FileInputStream(f);
			//convert file into array of bytes
			fileInputStream.read(byteArray);
			fileInputStream.close();
			//f.delete();//clean up temp file dont delete other peoples files >S
		} catch (Exception e) {
			e.printStackTrace();
		}
		return byteArray;

	}
}
