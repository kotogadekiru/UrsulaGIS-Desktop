package com.ursulagis.desktop.utils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This utility extracts files and directories of a standard zip file to
 * a destination directory.
 * @author www.codejava.net
 *
 */
public class UnzipUtility {
	/**
	 * Size of the buffer to read/write data
	 */
	private static final int BUFFER_SIZE = 4096;
	
	/**
	 * 
	 * @param is ZipInputStrean
	 * @return Map de ZipEntry con su Byte[]
	 */
	public static Map<ZipEntry,byte[]> readFrom(InputStream is){
		Map<ZipEntry,byte[]> entrys = new HashMap<ZipEntry,byte[]>();
	
		ZipInputStream zipIn = new ZipInputStream(is);//new FileInputStream(zipFilePath));
		
		try {
			ZipEntry entry = zipIn.getNextEntry();
			System.out.println("reading entry "+entry.getName());
			while (entry != null) {
				if (!entry.isDirectory()) {					
					byte[] bytes = extractBytes(zipIn);
					entrys.put(entry,bytes);
				}				
				System.out.println("\nfinished reading bytes");
								
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();
		} catch (Exception e) {			
			e.printStackTrace();
		}		
		System.out.println("returning entrys "+entrys.size());
		return entrys;
	}
	
	/**
	 * Extracts a zip file specified by the zipFilePath to a directory specified by
	 * destDirectory (will be created if does not exists)
	 * @param zipFilePath
	 * @param destDirectory
	 * @throws IOException
	 */
	public static List<String> unzip(InputStream is, Path destDirectoryPath) throws IOException {
		List<String> files = new ArrayList<String>();
		File destDir = destDirectoryPath.toFile();//new File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		ZipInputStream zipIn = new ZipInputStream(is);//new FileInputStream(zipFilePath));
		ZipEntry entry = zipIn.getNextEntry();
		// iterates over entries in the zip file
		while (entry != null) {
			String filePath = destDirectoryPath.toString() + File.separator + entry.getName();
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				File fout = new File(filePath); 
				int i =1;
				while(fout.isFile()){//si fout is file ya existe entonces creo uno distinto
			//		System.out.println(filePath + " ya existe!");
					int dotIndex = filePath.lastIndexOf('.');
					//modifico el nombre del archivo para que no pise al anterior.
					String filePath2 = filePath.substring(0,dotIndex)+"("+i+")"+filePath.substring(dotIndex);
					i++;
			//		System.out.println("creando el archivo "+filePath2);
					fout = new File(filePath2);
				}
				filePath = fout.getAbsolutePath();
				extractFile(zipIn, filePath);
				files.add(filePath);
			} else {
				// if the entry is a directory, make the directory
				File dir = new File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
		return files;
	}
	/**
	 * Extracts a zip entry (file entry)
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		File fout = new File(filePath);
	//	System.out.println("creando el archivo "+filePath);


		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fout));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
	
	/**
	 * Extracts a zip entry (file entry)
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private static byte[] extractBytes(ZipInputStream zipIn) throws IOException {
		ByteArrayOutputStream bar = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(bar);
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
		return bar.toByteArray();
	}

	public static File zipFiles(List<File> files,File outDirectoryFile){
		byte[] buffer = new byte[1024];
		//long time = System.currentTimeMillis();
		//File zipedFile = new File("temp"+time+".zip");//C:\MyFile.zip (Access is denied)
		File zipedFile= null;
		try{
			zipedFile = File.createTempFile("out_", ".zip",outDirectoryFile);
			//FileOutputStream fos = ;
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipedFile));

			for(File f:files){
				ZipEntry ze= new ZipEntry(f.getName());
				zos.putNextEntry(ze);
				FileInputStream in = new FileInputStream(f);
				int len;
				while ((len = in.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				in.close();
			}

			zos.closeEntry();

			//remember close it
			zos.close();

			System.out.println("Done");

		}catch(IOException ex){
			ex.printStackTrace();
		}
		return zipedFile;
	}
}