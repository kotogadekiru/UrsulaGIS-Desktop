package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.shape.Path;

import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Fertilizacion;
import dao.Pulverizacion;

public class ProcessPulvMapTask extends ProcessMapTask {

	// private FileDataStore store = null;

	// double maxX = 0, maxY = 0, minX = 0, minY = 0;// variables para llevar la
	// // cuenta de donde estan los
	// // puntos y hubicarlos en el
	// // centro del panel map
	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	// Shape union = null;
	// Geometry geometryUnion = null;
//	Quadtree featureTree = null;
	// private int featureCount;
	// private int featureNumber;

	private Double precioPasada;

	// public Group map = new Group();

//	ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();

	public ProcessPulvMapTask(Group map1, Double precioPasada1,
			FileDataStore store) {
		this.map = map1;
		precioPasada = precioPasada1;
		this.store = store;
	}

	// @Override
	// protected Quadtree call() throws Exception {
	// try {
	// openFile();
	// } catch (Exception e1) {
	// System.err.println("Failed to open shape file");
	// e1.printStackTrace();
	// }
	// return this.geometryTree;
	//
	// }

	public void doProcess() throws IOException {
		SimpleFeatureSource featureSource = store.getFeatureSource();

		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featuresIterator = featureCollection.features();

		this.featureTree = new Quadtree();

		
		List<Pulverizacion> itemsByIndex = new ArrayList<Pulverizacion>();
		List<Pulverizacion> itemsByAmount = new ArrayList<Pulverizacion>();

		while (featuresIterator.hasNext()) {
			SimpleFeature simpleFeature = featuresIterator.next();
			itemsByIndex.add( new Pulverizacion(simpleFeature, precioPasada));
		}
		itemsByAmount.addAll(itemsByIndex);
		constructHistogram(itemsByAmount);

	
		featureCount = itemsByIndex.size();
		

		for (Pulverizacion pulv : itemsByIndex) {	
			
	
	//		SimpleFeature simpleFeature = featuresIterator.next();

			featureNumber++;
			updateProgress(featureNumber, featureCount);
			System.out.println("Feature " + featureNumber + " of "
					+ featureCount);

			//Pulverizacion pulv = new Pulverizacion(simpleFeature, precioPasada);

			featureTree.insert(pulv.getGeometry().getEnvelopeInternal(), pulv);

			List<Polygon> mp = getPolygons(pulv);
			for (int i = 0; i < mp.size(); i++) {

				Polygon p = mp.get(i);

				pathTooltips.add(0, getPathTooltip(p, pulv));
			}

		}// fin del while

		runLater();

		updateProgress(0, featureCount);

	}

	private ArrayList<Object> getPathTooltip(Polygon poly, Pulverizacion pulv) {
		Path path = getPathFromGeom(poly, pulv);

		double area = poly.getArea() * ProyectionConstants.A_HAS;

		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String("Costo Agroquimicos: "
				+ df.format(pulv.getCostoPaquete()) + " U$S/Ha\n"
				+ "Pulverizacion: " + df.format(pulv.getImporteHa())
				+ " U$S/Ha\n" 
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

	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 100;
	}

}// fin del task

