package tasks.importar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;


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
		} else{//XXX cuando es una grilla los datos estan en outstore y instore es null
			reader = labor.outCollection.reader();
			//	 storeCRS = labor.outCollection.getSchema().getCoordinateReferenceSystem();
			//convierto los features en cosechas
			featureCount=labor.outCollection.size();
		}
		
		int divisor = 1;
		List<SueloItem> itemsToShow = new ArrayList<SueloItem>();
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
				itemsToShow.add(si);
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
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}

	
	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String(
				" Fosforo: "+ df.format(si.getPpmP()) +"Ppm\n"
				+"Nitrogeno: "+ df.format(si.getPpmN()) +"Ppm\n"
		);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText);

		
	}
	
	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}

}// fin del task

