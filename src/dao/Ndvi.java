package dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

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
@Entity
@NamedQueries({
	@NamedQuery(name=Ndvi.FIND_ALL, query="SELECT c FROM Ndvi c") ,
	@NamedQuery(name=Ndvi.FIND_NAME, query="SELECT o FROM Ndvi o where o.nombre = :name") ,
//	@NamedQuery(name=Ndvi.FIND_ACTIVOS, query="SELECT o FROM Ndvi o where o.activo = true") ,
}) 
public class Ndvi {

	public static final String FIND_ALL="Ndvi.findAll";
	public static final String FIND_NAME = "Ndvi.findName";
	public static final String FIND_ACTIVOS = "Ndvi.findActivos";
	@javax.persistence.Id @GeneratedValue
	private long id;
	String nombre=null;

	@Temporal(TemporalType.DATE)
	Date fecha=null;
	@Transient
	File f=null;

	@Lob
	private byte[] content;//el contenido de la imagen ndvi
	
	
 
	@Transient
	ExportableAnalyticSurface surfaceLayer=null;
	double pixelArea=0;
	
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
			 f = File.createTempFile(this.nombre, "");   
			 FileOutputStream fos = new FileOutputStream(f.getPath());
			 fos.write(content);
			 fos.close();

        } catch (Exception e) {
	     e.printStackTrace();
        } 
	}
}
