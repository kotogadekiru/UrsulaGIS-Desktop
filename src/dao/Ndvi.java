package dao;

import java.io.File;
import java.util.Date;

import javax.persistence.Entity;

import dao.config.Agroquimico;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import lombok.Data;

@Data
@Entity
public class Ndvi {
 String nombre=null;
 Date fecha=null;
 File f=null;
 ExportableAnalyticSurface surfaceLayer=null;
 double pixelArea=0;



}
