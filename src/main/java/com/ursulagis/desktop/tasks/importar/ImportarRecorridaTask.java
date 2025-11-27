package com.ursulagis.desktop.tasks.importar;

import java.io.IOException;
import java.util.Map;

import org.geotools.api.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;

import com.google.gson.Gson;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.recorrida.Muestra;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.tasks.ProgresibleTask;

/**
 * el mapa de recorrida perfecto tiene los puntos de muestreo, el camiono para recorrer y los poligonos con los ambitentes. son 3 mapas en total.
 * Lineas, puntos y poligonos
 * @author quero
 *
 */
public class ImportarRecorridaTask extends ProgresibleTask<Recorrida>{
	private Recorrida recorrida=null;
	private FileDataStore store=null;

	public ImportarRecorridaTask(Recorrida laborToExport,FileDataStore shapeFile) {
		super();
		this.recorrida=laborToExport;
		this.store=shapeFile;
	
		this.taskName= laborToExport.getNombre();
		super.updateTitle(taskName);
	}

	public void run(Recorrida recorrida,FileDataStore store) {
//		sb.append("*the_geom:"+Point.class.getCanonicalName()+":4326");
//		sb.append(",name" + ":"+String.class.getCanonicalName());
//		sb.append(",obs" + ":"+String.class.getCanonicalName());	
	
		try {
			SimpleFeatureIterator features = store.getFeatureSource().getFeatures().features();
	
		
		int size = store.getFeatureSource().getFeatures().size();
		int progress=0;
		while(features !=null && features.hasNext()) {
			updateProgress(progress++/size,size );
			SimpleFeature next = features.next();
			Muestra m = new Muestra();
			Object geomObj = next.getDefaultGeometry();
			if(geomObj instanceof Point) {
				Point p = (Point)geomObj;
				m.setLatitude(p.getY());
				m.setLongitude(p.getX());
			}
			Object nameAtt = next.getAttribute("name");
			if(nameAtt instanceof String) {
				String nombre = (String)nameAtt;
				String[] parts = nombre.split(": ");
				m.setNombre(parts[1]);
				m.setSubNombre(parts[0]);
			}
			
			Object subNameAtt = next.getAttribute("subName");
			if(subNameAtt instanceof String) {
				String subNnombre = (String)subNameAtt;				
				m.setSubNombre(subNnombre);
			}
			
			Object obsAtt = next.getAttribute("obs");
			if(obsAtt instanceof String) {
				m.setObservacion((String)obsAtt);
			}
			recorrida.getMuestras().add(m);
		}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected Recorrida call() throws Exception {
		this.run(this.recorrida,this.store);
		return recorrida;
	}
}
