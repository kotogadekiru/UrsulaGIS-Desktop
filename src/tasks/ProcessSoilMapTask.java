package tasks;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.FertilizacionItem;
import dao.SueloItem;
import javafx.scene.Group;
import javafx.scene.shape.Path;

import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;

import utils.ProyectionConstants;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProcessSoilMapTask extends ProcessMapTask {


	public ProcessSoilMapTask(Group map1,  FileDataStore store) {
		super.layer = map1;
		super.store = store;
	
	}

	public void doProcess() throws IOException {
		SimpleFeatureSource featureSource = store.getFeatureSource();
		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featuresIterator = featureCollection.features();

		this.featureTree = new Quadtree();

		featureCount = featureCollection.size();
	//	pathTooltips = new ArrayList<ArrayList<Object>>();
		//
		List<SueloItem> itemsByIndex = new ArrayList<SueloItem>();
		List<SueloItem> itemsByAmount = new ArrayList<SueloItem>();

		while (featuresIterator.hasNext()) {
			SimpleFeature simpleFeature = featuresIterator.next();
			itemsByIndex.add(new SueloItem(simpleFeature));
		}
		itemsByAmount.addAll(itemsByIndex);
		constructHistogram(itemsByAmount);

	
		featureCount = itemsByIndex.size();
		

		for (SueloItem soilFeature : itemsByIndex) {		

			featureNumber++;
			updateProgress(featureNumber, featureCount);

//			System.out.println("Feature " + featureNumber + " of "
//					+ featureCount);

			featureTree.insert(soilFeature.getGeometry().getEnvelopeInternal(),
					soilFeature);

			List<Polygon> mp = getPolygons(soilFeature);
			for (int i = 0; i < mp.size(); i++) {
				Polygon p = mp.get(i);

				pathTooltips.add(0, getPathTooltip(p, soilFeature));
			}
		}// fin del while

		runLater();

		// saveFeaturesToNewShp(destinationFeatures);
		updateProgress(0, featureCount);

	}

	private ArrayList<Object> getPathTooltip(Polygon poly,
			SueloItem fertFeature) {

		Path path = getPathFromGeom(poly, fertFeature);

		double area = poly.getArea() * ProyectionConstants.A_HAS;// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String(
				
				 " PpmFosforo/Ha: "+ df.format(fertFeature.getPpmP()) +"\n"
//						+ "Sup: "
//						+ df.format(area * ProyectionConstants.METROS2_POR_HA)
//						+ " m2\n"
		// +"feature: " + featureNumber
		);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}
		
		ArrayList<Object> ret = new ArrayList<Object>();
		ret.add(path);
		ret.add(tooltipText);
		return ret;
	}

	// TODO devolver un color entre el minimo y el maximo variando el hue entre
	// el hue del rojo hasta el hue del verde

	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}

}// fin del task

