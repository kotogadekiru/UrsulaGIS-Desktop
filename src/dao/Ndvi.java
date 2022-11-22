package dao;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Calendar;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import dao.config.Configuracion;
import dao.utils.LocalDateAttributeConverter;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity //@Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Ndvi.FIND_ALL, query="SELECT c FROM Ndvi c ORDER BY lower(c.nombre)") ,
	@NamedQuery(name=Ndvi.FIND_NAME, query="SELECT o FROM Ndvi o where o.nombre = :name") ,
	@NamedQuery(name=Ndvi.FIND_ACTIVOS, query="SELECT o FROM Ndvi o where o.activo = true") ,
	@NamedQuery(name=Ndvi.FIND_BY_CONTORNO_DATE, query="SELECT o FROM Ndvi o where o.contorno = :contorno and o.fecha = :date") ,
	@NamedQuery(name=Ndvi.FIND_BY_CONTORNO, query="SELECT o FROM Ndvi o where o.contorno = :contorno") ,
}) 
public class Ndvi implements Comparable<Object>{//extends AbstractBaseEntity {
	public static final String FIND_ALL="Ndvi.findAll";
	public static final String FIND_NAME = "Ndvi.findName";
	public static final String FIND_ACTIVOS = "Ndvi.findActivos";
	public static final String FIND_BY_CONTORNO_DATE = "Ndvi.findByContornoDate";
	public static final String FIND_BY_CONTORNO = "Ndvi.findByContorno";

	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	private String nombre=null;

	//	@Temporal(TemporalType.DATE)
	//	@Convert(converter = LocalDateAttributeConverter.class)
	private LocalDate fecha=LocalDate.now();

	private Double meanNDVI=null;
	private Double porcNubes=null;
	private Double rFAA=null;

	@Transient
	private File f=null;

	@ManyToOne 
	//@ManyToOne(cascade= {CascadeType.DETACH})
	//FIXME no puedo borrar poligonos que apunten a un ndvi
	private Poligono contorno = null;

	@Lob
	private byte[] content;//el contenido de la imagen ndvi

	private boolean activo =true;


	@Transient
	ExportableAnalyticSurface surfaceLayer=null;
	@Transient
	Layer layer=null;

	double pixelArea=100/10000;//100m2

	public boolean getActivo(){
		return activo;
	}

	public boolean setActivo(boolean act){
		return this.activo=act;
	}

	/**
	 * lee del archivo f y escribe a content
	 */
	public void updateContent(File f){
		//File file = new File("C:\\mavan-hibernate-image-mysql.gif");
		if(content == null) {
//			if(f==null) {
//				loadFileFromContent();
//			}
			content = new byte[(int) f.length()];

			try {
				FileInputStream fileInputStream = new FileInputStream(f);
				//convert file into array of bytes
				fileInputStream.read(content);
				fileInputStream.close();
				//f.delete();//clean up temp file dont delete other peoples files >S
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getFileName() {
		String parent =nombre.replaceAll("[\\\\/:*?\"<>|]", "-");//URLEncoder.encode(nombre, "UTF-8");// nombre.replaceAll("\\", "-");
		return parent;
	}
//	public File getF(){
//		try {
//
//			//String parent =nombre.replaceAll("[\\\\/:*?\"<>|]", "-");//URLEncoder.encode(nombre, "UTF-8");// nombre.replaceAll("\\", "-");
//			//System.out.println("parentFile "+parent);
//
//			//f = File.createTempFile(parent, "");   //esto crea el archivo con un nombre random
//
//			File ursulaGISFolder = new File(Configuracion.ursulaGISFolder);
//			File f = File.createTempFile(getFileName(), ".tif", ursulaGISFolder);
//			//f=new File(f.getParentFile(),parent);
//			f.deleteOnExit();
//
//			FileOutputStream fos = new FileOutputStream(f.getPath());
//			fos.write(content);
//			fos.close();
//			//FIXME por alguna razon esto no funciona en el ejecutable parece que devuelve null.
//			return f;
//		} catch (Exception e) {
//			e.printStackTrace();
//		} 
//		if(this.pixelArea==0) {
//			this.pixelArea=0.001;//0.008084403745300213;
//		}
//		return null;
//	}

	@Override
	public int compareTo(Object o) {
		if(o instanceof Ndvi) {
			return this.fecha.compareTo(((Ndvi) o).fecha);
		}
		return 0;
	}

	@Override
	public String toString() {
		return this.nombre;
	}

}
