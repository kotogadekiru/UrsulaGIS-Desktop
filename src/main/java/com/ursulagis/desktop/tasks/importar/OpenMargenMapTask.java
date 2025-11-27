package com.ursulagis.desktop.tasks.importar;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;

import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.margen.MargenItem;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import javafx.scene.Group;
import javafx.scene.shape.Path;
import com.ursulagis.desktop.tasks.ProcessMapTask;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import com.ursulagis.desktop.utils.ProyectionConstants;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author quero
 * task que toma un store y devuelve una capa de suelo a partir del store
 */

public class OpenMargenMapTask extends ProcessMapTask<MargenItem,Margen> {
	public OpenMargenMapTask(Margen sueloMap) {
		this.labor=sueloMap;
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
		//List<MargenItem> itemsToShow = new ArrayList<MargenItem>();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			MargenItem si = labor.constructFeatureContainer(simpleFeature);
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
			//	itemsToShow.add(si);
			}
			
		}// fin del for que recorre las cosechas por indice
		reader.close();
		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);

	}

	public static String buildTooltipText(MargenItem renta,double area) {
		NumberFormat df = Messages.getNumberFormat();

		String tooltipText = new String(
						  Messages.getString("OpenMargenMapTask.1") + df.format(renta.getRentabilidadHa()) + Messages.getString("OpenMargenMapTask.2")  //$NON-NLS-1$ //$NON-NLS-2$
						+ Messages.getString("OpenMargenMapTask.3") + df.format(renta.getMargenPorHa()) + Messages.getString("OpenMargenMapTask.4")  //$NON-NLS-1$ //$NON-NLS-2$
						+ Messages.getString("OpenMargenMapTask.5")	+ df.format(renta.getCostoPorHa()) + Messages.getString("OpenMargenMapTask.6") //$NON-NLS-1$ //$NON-NLS-2$
						+ Messages.getString("OpenMargenMapTask.7")	+ df.format(renta.getImporteFertHa()) + Messages.getString("OpenMargenMapTask.8")  //$NON-NLS-1$ //$NON-NLS-2$
						+ Messages.getString("OpenMargenMapTask.9")	+ df.format(renta.getImportePulvHa()) + Messages.getString("OpenMargenMapTask.10") //$NON-NLS-1$ //$NON-NLS-2$
						+ Messages.getString("OpenMargenMapTask.11")+ df.format(renta.getImporteSiembraHa()) + Messages.getString("OpenMargenMapTask.12") //$NON-NLS-1$ //$NON-NLS-2$
						+ Messages.getString("OpenMargenMapTask.13")+ df.format(renta.getCostoFijoPorHa()) + Messages.getString("OpenMargenMapTask.14") //$NON-NLS-1$ //$NON-NLS-2$
						+ Messages.getString("OpenMargenMapTask.15")+ df.format(renta.getImporteCosechaHa()) + Messages.getString("OpenMargenMapTask.16")  //$NON-NLS-1$ //$NON-NLS-2$
				);

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("OpenMargenMapTask.17")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("OpenMargenMapTask.18")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			tooltipText=tooltipText.concat(Messages.getString("OpenMargenMapTask.19")+df.format(area ) + Messages.getString("OpenMargenMapTask.20")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return tooltipText;
	}
	protected ExtrudedPolygon getPathTooltip( Geometry poly,MargenItem renta,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();

		String tooltipText= buildTooltipText(renta,area);
		return super.getExtrudedPolygonFromGeom(poly, renta,tooltipText,renderablePolygon);
	//	super.getRenderPolygonFromGeom(poly, renta,tooltipText);
	}

	
	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}

}// fin del task

