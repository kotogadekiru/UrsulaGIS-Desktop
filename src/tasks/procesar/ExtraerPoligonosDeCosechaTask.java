package tasks.procesar;

import java.awt.Component;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import dao.Poligono;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.UnitsFormat;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwind.util.measure.MeasureToolController;
import dao.Labor;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class ExtraerPoligonosDeCosechaTask extends Task<List<Poligono>> {
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";

	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;
	
	private Labor<?> labor;
	public ExtraerPoligonosDeCosechaTask(Labor<?> l) {
		this.labor = l;
	}

	@Override
	protected List<Poligono> call() {
		try{
		List<Poligono> poligonos = new ArrayList<Poligono>();
		SimpleFeatureIterator it = labor.getOutCollection().features();
		int featureCount=labor.getOutCollection().size();
		int index =0;
		while(it.hasNext()){
			updateProgress(index, featureCount);
			SimpleFeature next = it.next();

			List<Polygon> mp = getPolygons(next.getDefaultGeometry());

		//	for(Polygon p : mp){
			Polygon p =null;
			Geometry boundary =  mp.get(0);//.getBoundary();// com.vividsolutions.jts.geom.MultiLineString
			if(boundary.getNumGeometries()>1){
				System.out.println("boundary tiene dim >1");
			
				continue;
			} else{
				p=(Polygon) boundary;
			}
			
				p = (Polygon) PolygonValidator.validate((Geometry)p);
				Poligono poli =new Poligono();
				poli.setNombre(labor.getNombreProperty().get()+" "+index);

			//	p =(Polygon) p.buffer(ProyectionConstants.metersToLongLat(10));
				p=(Polygon) p.convexHull();
				for(Coordinate c :p.getBoundary().getCoordinates()){//las coordenadas no estan ordenadas o tienen huecos
					poli.getPositions().add(Position.fromDegrees(c.y,c.x));
				}
				poligonos.add(poli);
			//}
			index++;
		}
		return poligonos;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	
	/**
	 * metodo usado por las capas de siembra fertilizacion, pulverizacion y suelo para obtener los poligonos
	 * @param dao
	 * @return
	 */
	protected List<Polygon> getPolygons(Object geometry){
		List<Polygon> polygons = new ArrayList<Polygon>();

		if (geometry instanceof Geometry) {		

			Geometry mp = (Geometry) geometry;
			if( mp.getNumGeometries()>1 || geometry instanceof MultiPolygon){
				for (int i = 0; i < mp.getNumGeometries() ; i++) {
					Geometry g = mp.getGeometryN(i);
					polygons.addAll(getPolygons(g));//recursion			
				}

			} else {
				polygons.add((Polygon) mp);//com.vividsolutions.jts.geom.MultiPolygon cannot be cast to com.vividsolutions.jts.geom.Polygon
			}
		}
		return polygons;
	}
	
	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label(labor.nombreProperty.get());
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(TASK_CLOSE_ICON));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}

	public void uninstallProgressBar() {
		progressPane.getChildren().remove(progressContainer);
	}

}
