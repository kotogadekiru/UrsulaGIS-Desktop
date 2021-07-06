package tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import dao.Clasificador;
import dao.Labor;
import dao.LaborItem;
import dao.recorrida.Camino;
import dao.recorrida.Muestra;
import dao.recorrida.Recorrida;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.ShapeAttributes;
import gui.Messages;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import tasks.procesar.GenerarRecorridaDirigidaTask;
import tasks.procesar.SimplificarCaminoTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;

public class ShowRecorridaDirigidaTask extends Task<RenderableLayer> {


	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";
//	int MAX_URL_LENGHT = 4443;//2048 segun un stackoverflow //4443 segun pruevas con chrome// corresponde a 129 puntos
//	protected int featureCount=0;
//	protected int featureNumber=0;
//	protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();

	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;
	
	/**
	 * la lista de las cosechas a unir
	 */
	
	
	
	//private double cantidadMaximaDeMuestrasTotal=Double.MAX_VALUE;
	
	
	
	private Recorrida recorrida =null;

	public ShowRecorridaDirigidaTask(Recorrida recorrida) {
		this.recorrida=recorrida;
		}

	/**
	 *Proceso que genera una lista de puntos al azar dentro de cada zona de acuerdo a la frecuencia minima especificada
	 */
	@Override
	public RenderableLayer call()  {
		
		
		RenderableLayer layer = new RenderableLayer();
		
		GenerarRecorridaDirigidaTask.renderRecorrida(layer,recorrida);
	
		
		return layer;
	}



	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Recorrida "+this.recorrida.getNombre());
		progressBarLabel.setTextFill(Color.BLACK);

		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("Cancelando GenerarRecorridaDirigidaTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(MMG_GUI_EVENT_CLOSE_PNG));
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

