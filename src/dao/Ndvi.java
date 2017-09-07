package dao;

import java.io.File;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import dao.config.Agroquimico;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import lombok.Data;

@Data
@Entity
public class Ndvi {
	@javax.persistence.Id @GeneratedValue
	private long id;
 String nombre=null;
	@Temporal(TemporalType.DATE)
 Date fecha=null;
 @Transient
 File f=null;
 @Transient
 ExportableAnalyticSurface surfaceLayer=null;
 double pixelArea=0;
}
