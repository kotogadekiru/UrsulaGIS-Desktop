package tasks.importar;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.fertilizacion.FertilizacionItem;
import dao.margen.Margen;
import dao.margen.MargenItem;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import javafx.scene.Group;
import javafx.scene.shape.Path;
import tasks.ProcessMapTask;

import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import utils.ProyectionConstants;

import java.io.IOException;
import java.text.DecimalFormat;
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
		} else{//XXX cuando es una grilla los datos estan en outstore y instore es null
			reader = labor.outCollection.reader();
			//	 storeCRS = labor.outCollection.getSchema().getCoordinateReferenceSystem();
			//convierto los features en cosechas
			featureCount=labor.outCollection.size();
		}
		
		int divisor = 1;
		List<MargenItem> itemsToShow = new ArrayList<MargenItem>();
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
				itemsToShow.add(si);
			}
			
		}// fin del for que recorre las cosechas por indice
		reader.close();
		labor.constructClasificador();
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}

	
	protected ExtrudedPolygon getPathTooltip( Geometry poly,MargenItem renta) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();


		DecimalFormat df = new DecimalFormat("0.00");
		df.setGroupingSize(3);
		
		df.setGroupingUsed(true);

		String tooltipText = new String(
				"Rentabilidad: "+ df.format(renta.getRentabilidadHa())+ "%\n\n" 
						+"Margen: "+ df.format(renta.getMargenPorHa())	+ "U$S/Ha\n" 
						+ "Costo: "	+ df.format(renta.getCostoPorHa())		+ "U$S/Ha\n\n"
						+ "Fertilizacion: "	+ df.format(renta.getImporteFertHa())+ "U$S/Ha\n" 
						+ "Pulverizacion: "	+ df.format(renta.getImportePulvHa())	+ "U$S/Ha\n"
						+ "Siembra: "	+ df.format(renta.getImporteSiembraHa())+ "U$S/Ha\n"
						+ "Fijo: "	+ df.format(renta.getCostoFijoPorHa())+ "U$S/Ha\n"
						+ "Cosecha: "	+ df.format(renta.getImporteCosechaHa()) + "U$S/Ha\n" 
						//		+ df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n"
						// +"feature: " + featureNumber
				);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}
		return super.getExtrudedPolygonFromGeom(poly, renta,tooltipText);
	//	super.getRenderPolygonFromGeom(poly, renta,tooltipText);
	}

	
	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}

}// fin del task

