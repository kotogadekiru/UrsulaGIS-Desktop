package tasks.procesar;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

import dao.Labor;
import dao.Poligono;
import gov.nasa.worldwind.geom.Position;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import utils.ProyectionConstants;

public class ExtraerPoligonosDeLaborTask extends Task<List<Poligono>> {
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";

	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	private Labor<?> labor;
	public ExtraerPoligonosDeLaborTask(Labor<?> l) {
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
				double has = ProyectionConstants.A_HAS(((Geometry)next.getDefaultGeometry()).getArea());
				if(has>0.2){//cada poli mayor a 10m2
				Poligono poli = featureToPoligono(next);

				poli.setNombre(labor.getNombreProperty().get()+" "+index);
				GeometryFactory fact = ((Geometry)next.getDefaultGeometry()).getFactory();
				List<Position> positions = poli.getPositions();

				Coordinate[] coordinates = new Coordinate[positions.size()];
				for(int i =0;i<positions.size();i++){
					Position pos = positions.get(i);
					coordinates[i]=new Coordinate(pos.getLongitude().degrees,pos.getLatitude().degrees);
				}
				Polygon p =fact.createPolygon(coordinates);
				//		p = (Polygon) JTS.smooth( p,1 );

			

				Geometry bp = p.getBoundary();
			
				poli.setArea(has);

				
					Coordinate[] finalCoords = bp.getCoordinates();
					poli.getPositions().clear();
					for(Coordinate c :finalCoords){//las coordenadas no estan ordenadas o tienen huecos
						poli.getPositions().add(Position.fromDegrees(c.y,c.x));
					}
					poligonos.add(poli);
				} else{
					System.out.println("el poligono es chico "+has);
				}
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

	public static Poligono featureToPoligono(SimpleFeature feature){			
		Object g=feature.getDefaultGeometry();
		if(g instanceof Geometry){						
			ArrayList<Position> iterable = new ArrayList<Position>();
			
			try{	
				TopologyPreservingSimplifier ts =new TopologyPreservingSimplifier((Geometry) g);
				ts.setDistanceTolerance(ProyectionConstants.metersToLongLat(1));
				g=	ts.getResultGeometry();
			}catch(Exception e){
				e.printStackTrace();
			}
			
			Geometry mainBoundary = ((Geometry) g).getBoundary();
			Geometry seed =mainBoundary.getGeometryN(0);// mp.getGeometryN(0);
			Coordinate[] coordinates = seed.getCoordinates();
			for(Coordinate c : coordinates){
				iterable.add(Position.fromDegrees(c.y, c.x));							
			}

			for(int n =1;n<mainBoundary.getNumGeometries();n++){
				Geometry toAdd =mainBoundary.getGeometryN(n);// mp.getGeometryN(0);
				Coordinate[] cToAdd= toAdd.getCoordinates();
				//1 buscar los puntos de cada una de las geometrias que esten mas cerca
				int minIt=0;
				int minCoord=0;
				double minDistance=-1;
				for(int i=0;i<iterable.size();i++){
					Position itPos = iterable.get(i);
					for(int j=0;j<cToAdd.length;j++){
						Coordinate c=cToAdd[j];
						Position pToAdd = Position.fromDegrees(c.y, c.x);
						double dist = Position.linearDistance(pToAdd,itPos ).degrees;
						if(minDistance<0 || dist<minDistance){
							minDistance=dist;
							minIt=i;
							minCoord=j;												
						}
					}
				}			
				//insertar en iterable position minIt las coordenadas de ToAdd empezando por minCoord
				for(int j=0;j<cToAdd.length;j++){//empiezo en j=0
					int index = (j+minCoord)%cToAdd.length;

					Coordinate c = cToAdd[index];//para recorrer hasta cero al final
					Position pToAdd = Position.fromDegrees(c.y, c.x);
					int finalIndex = minIt+1+j;
					iterable.add(finalIndex, pToAdd);			
				}

				Coordinate c = cToAdd[minCoord];//para recorrer hasta cero al final
				Position pToAdd = Position.fromDegrees(c.y, c.x);
				iterable.add(minIt+cToAdd.length+1,pToAdd);
				iterable.add(minIt+cToAdd.length+2, iterable.get(minIt));
			}


			if(!iterable.get(0).equals(iterable.get(iterable.size()-1))){
				iterable.add(iterable.get(0));
			}
			Poligono poli = new Poligono();
			poli.setPositions(iterable);
			return poli;
		} else {return null;}
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
