package com.ursulagis.desktop.tasks.importar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.geotools.api.data.FeatureReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearSueloMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;


/**
 * 
 * @author quero
 * task que toma un store y devuelve una capa de suelo a partir del store
 */

public class OpenSoilMapTask extends ProcessMapTask<SueloItem,Suelo> {
	public OpenSoilMapTask(Suelo sueloMap) {
		super(sueloMap);
		//this.labor=sueloMap;
	}
	
	public void doProcess() throws IOException {
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =null;
		//	CoordinateReferenceSystem storeCRS =null;
		if(labor.getInStore()!=null){
			if(labor.outCollection!=null)labor.outCollection.clear();
			reader = labor.getInStore().getFeatureReader();
			//		 storeCRS = labor.getInStore().getSchema().getCoordinateReferenceSystem();
			//convierto los features en cosechas
			featureCount=labor.getInStore().getFeatureSource().getFeatures().size();
		}
//		else{//XXX cuando es una grilla los datos estan en outstore y instore es null
//			reader = labor.outCollection.reader();
//			//	 storeCRS = labor.outCollection.getSchema().getCoordinateReferenceSystem();
//			//convierto los features en cosechas
//			featureCount=labor.outCollection.size();
//		}
		else{
			if(labor.getInCollection() == null){//solo cambio la inCollection por la outCollection la primera vez
				labor.setInCollection(labor.outCollection);
				labor.outCollection=  new DefaultFeatureCollection("internal",labor.getType()); //$NON-NLS-1$
			}
			// cuando es una grilla los datos estan en outstore y instore es null
			// si leo del outCollection y luego escribo en outCollection me quedo sin memoria
			reader = labor.getInCollection().reader();
			labor.outCollection.clear();
			featureCount=labor.getInCollection().size();
		}
		
		
		int divisor = 1;
		//List<SueloItem> itemsToShow = new ArrayList<SueloItem>();
		while (reader.hasNext()) {

			SimpleFeature simpleFeature = reader.next();
			SueloItem si = labor.constructFeatureContainer(simpleFeature);


			featureNumber++;

			updateProgress(featureNumber/divisor, featureCount);
			Object geometry = si.getGeometry();

			/**
			 * si la geometria es un point procedo a poligonizarla
			 *
			 */
			if (geometry instanceof Point) {
				//TODO crear una grilla e interpolar los valores con el promedio ponderado po las distancias (como se llamaba? <=kriging) resumir geometrias?
			
			} else { // no es point. Estoy abriendo una cosecha de poligonos.
				labor.insertFeature(si);
		//		itemsToShow.add(si);
			}
			
		}// fin del for que recorre las cosechas por indice
		reader.close();
		
	
//		SimpleFeatureIterator it = labor.outCollection.features();
//		while(it.hasNext()){
//			SimpleFeature f=it.next();
//			itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
//		}
//		it.close();
		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);

	}

	
//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor,si,area);
//
//		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);
//	}
	
	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}

}// fin del task

