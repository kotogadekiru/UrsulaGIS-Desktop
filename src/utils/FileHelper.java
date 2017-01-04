package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.filechooser.FileNameExtensionFilter;

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
}
