package dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.Calendar;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;


import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import lombok.Data;

@Data
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Ndvi.FIND_ALL, query="SELECT c FROM Ndvi c") ,
	@NamedQuery(name=Ndvi.FIND_NAME, query="SELECT o FROM Ndvi o where o.nombre = :name") ,
	@NamedQuery(name=Ndvi.FIND_ACTIVOS, query="SELECT o FROM Ndvi o where o.activo = true") ,
}) 
public class Ndvi {

	public static final String FIND_ALL="Ndvi.findAll";
	public static final String FIND_NAME = "Ndvi.findName";
	public static final String FIND_ACTIVOS = "Ndvi.findActivos";
	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	String nombre=null;

	@Temporal(TemporalType.DATE)
	private Calendar fecha=Calendar.getInstance();
	
	@Transient
	File f=null;

	@Lob
	private byte[] content;//el contenido de la imagen ndvi
	
	private boolean activo =true;
	
 
	@Transient
	ExportableAnalyticSurface surfaceLayer=null;
	@Transient
	double pixelArea=0;
	
	public boolean getActivo(){
		return activo;
	}
	
	public boolean setActivo(boolean act){
		return this.activo=act;
	}
	
	public void updateContent(){
		//File file = new File("C:\\mavan-hibernate-image-mysql.gif");
        content = new byte[(int) f.length()];

        try {
	     FileInputStream fileInputStream = new FileInputStream(f);
	     //convert file into array of bytes
	     fileInputStream.read(content);
	     fileInputStream.close();
        } catch (Exception e) {
	     e.printStackTrace();
        }
	}
	
	public void loadFileFromContent(){
		try {
			
			String parent =nombre.replaceAll("[\\\\/:*?\"<>|]", "-");//URLEncoder.encode(nombre, "UTF-8");// nombre.replaceAll("\\", "-");
			System.out.println("parentFile "+parent);
			
			 f = File.createTempFile(parent, "");   //esto crea el archivo con un nombre random
			 f=new File(f.getParentFile(),parent);
			 FileOutputStream fos = new FileOutputStream(f.getPath());
			 fos.write(content);
			 fos.close();

        } catch (Exception e) {
	     e.printStackTrace();
        } 
	}
}
