package com.ursulagis.desktop.dao;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

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
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity //@Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=LaborShpFile.FIND_ALL, query="SELECT c FROM " +LaborShpFile.CLASS_NAME+" c ") ,
}) 

/**
 * para leer el datastore necesito un File. asi que tengo que escribir content en una carpeta temp
 * @author quero
 *
 */
public class LaborShpFile {//implements Comparable<Object>{//extends AbstractBaseEntity {
	public static final String CLASS_NAME = "LaborShpFile";//LaborShpFile.class.getName(); 
	public static final String FIND_ALL=CLASS_NAME+".findAll";


	@javax.persistence.Id @GeneratedValue
	private Long id=null;


	@Transient
	private File f=null;

	@ManyToOne 
	private List<Labor<?>> labores = null;

	@Lob
	private byte[] content;// shp; dbf; shx;
	@OneToMany(cascade=CascadeType.ALL, mappedBy="laborFile",orphanRemoval=true)
	private List<Labor<?>> getImagenesPoligono(){
		return this.labores;
	}


	/**
	 * lee del archivo f y escribe a content
	 */
	public void updateContent(File f){//zipfile conteniendo shp, dbf y shp files

		if(content == null) {

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



	@Override
	public String toString() {
		return Long.valueOf(this.id).toString();
	}

}
