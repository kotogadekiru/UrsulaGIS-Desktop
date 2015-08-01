package tasks;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Translate;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.index.quadtree.QuadTree;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.index.strtree.STRtree;

import dao.CosechaItem;
import dao.Pulverizacion;
import dao.Siembra;

public class ProcessSiembraMapTask extends ProcessMapTask {	
//	private int featureCount;
//	private int featureNumber;

//	private FileDataStore store = null;
//	Quadtree featureTree = null;

	private Double precioPasada;
	private Double precioBolsaSemilla;
	
	//ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();
	
	//public Group map = new Group();

	public ProcessSiembraMapTask(Group map1, Double precioPasada1,
			Double precioBolsaSemilla, FileDataStore store) {
		this.map=map1;
		this.store = store;
		
		precioPasada = precioPasada1;
		this.precioBolsaSemilla = precioBolsaSemilla;
		
	}

//	@Override
//	protected Quadtree call() throws Exception {
//		try {
//			openFile();
//		} catch (Exception e1) {
//			System.err.println("Failed to open shape file");
//			e1.printStackTrace();
//		}
//		return this.geometryTree;
//
//	}

	public void doProcess() throws IOException {
		SimpleFeatureSource featureSource = store.getFeatureSource();

		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featuresIterator = featureCollection.features();

		this.featureTree = new Quadtree();

		List<Siembra> itemsByIndex = new ArrayList<Siembra>();
		List<Siembra> itemsByAmount = new ArrayList<Siembra>();

		while (featuresIterator.hasNext()) {
			SimpleFeature simpleFeature = featuresIterator.next();
			itemsByIndex.add( new Siembra(simpleFeature, precioPasada,precioBolsaSemilla));
		}
		itemsByAmount.addAll(itemsByIndex);
		constructHistogram(itemsByAmount);

	
		featureCount = itemsByIndex.size();
		

		for (Siembra siembraFeature : itemsByIndex) {	
			featureNumber++;
			updateProgress(featureNumber, featureCount);
			System.out.println("Feature " + featureNumber + " of "
					+ featureCount);
			
			//Siembra siembraFeature = new Siembra(simpleFeature, precioBolsaSemilla,precioPasada);
			featureTree.insert(siembraFeature.getGeometry().getEnvelopeInternal(), siembraFeature);
			
			List<Polygon> mp =getPolygons(siembraFeature);
			for (int i = 0; i < mp.size(); i++) {
				
				Polygon p = mp.get(i);

				pathTooltips.add(0,	getPathTooltip(p,siembraFeature));
			}

		}// fin del while
			// saveFeaturesToNewShp(destinationFeatures);
		
		runLater();
		updateProgress(0, featureCount);

	}
	
	private  ArrayList<Object>  getPathTooltip( Polygon poly,Siembra siembraFeature) {
		Path path = getPathFromGeom(poly,siembraFeature);		
		
		double area = poly.getArea() *ProyectionConstants.A_HAS;// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00"); 
		String tooltipText = new String(
				"Densidad: "+ df.format(siembraFeature.getBolsaHa()) + " Bolsa/Ha\n\n"
				+"Costo: " + df.format(siembraFeature.getImporteHa()) + " U$S/Ha\n"				
			//	+"Sup: " +  df.format(area*ProyectionConstants.METROS2_POR_HA) + " m2\n"
		//		+"feature: " + featureNumber						
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
	
	protected  int getAmountMin(){return 0;} 
	protected  int gerAmountMax() {return 1;} 
	
}// fin del task

