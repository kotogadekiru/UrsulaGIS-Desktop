package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.recorrida.Camino;
import com.ursulagis.desktop.dao.recorrida.Muestra;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.ShapeAttributes;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.utils.DateConverter;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class GenerarRecorridaDirigidaTask extends Task<RenderableLayer> {

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
	private List<Labor<? extends LaborItem>> aMuestrear;
	private double superficieMinimaAMuestrear=0;
	private double densidadDeMuestrasDeseada=0;
	//private double cantidadMaximaDeMuestrasTotal=Double.MAX_VALUE;
	

	private double cantidadMinimaDeMuestrasPoligonoAMuestrear=0;
	private Recorrida recorrida =null;

	/**
	 * 
	 * @param labores: labores a muestrear
	 * @param supMinima: sup minima del poligono a muestrar 
	 * @param hasPorMuestra: cantidad de has representadas por cada muestra
	 * @param cantMinPoly: cantidad minima de muestras a generar por poligono
	 */
	public GenerarRecorridaDirigidaTask(List<Labor<? extends LaborItem>> labores,double supMinima,double hasPorMuestra,double cantMinPoly){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.aMuestrear=labores;

		//	super.labor = new Suelo();
		//	super.labor.featureBuilder = new SimpleFeatureBuilder(super.labor.getPointType());
		this.superficieMinimaAMuestrear=supMinima;
		this.densidadDeMuestrasDeseada=1/hasPorMuestra;// 1/has por muestra  = 1/20
		this.cantidadMinimaDeMuestrasPoligonoAMuestrear=cantMinPoly;


		this.recorrida = new Recorrida();
		this.recorrida.setNombre(Messages.getString("GenerarMuestreoDirigidoTask.0"));//este es el nombre que se muestra en el progressbar //$NON-NLS-1$
		 // Seteando la fecha al dia que se crea
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		String fecha = dateFormat.format(new Date());
		this.recorrida.setFechaString(fecha);
	}

	/**
	 *Proceso que genera una lista de puntos al azar dentro de cada zona de acuerdo a la frecuencia minima especificada
	 */
	@Override
	public RenderableLayer call() throws IOException {
		String nombreRecorrida =null;
		//ancho me permite controlar la distancia minima entre los puntos y entre el punto y la frontera
		double ancho = 5+Math.sqrt(superficieMinimaAMuestrear*ProyectionConstants.METROS2_POR_HA)/10;
		System.out.println("ancho="+ancho); //ancho=86.60254037844386
		Map<String,Color> colorCat = new HashMap<String, Color>();
		Map<String,List<Geometry>> ambsRec = new HashMap<String, List<Geometry>>();

		for(Labor<? extends LaborItem> c:aMuestrear){			
			if(nombreRecorrida == null){				
				nombreRecorrida=this.recorrida.getNombre()+Messages.getString("GenerarMuestreoDirigidoTask.1")+c.getNombre();	 //$NON-NLS-1$
			}else {
				nombreRecorrida+=Messages.getString("GenerarMuestreoDirigidoTask.2")+c.getNombre(); //$NON-NLS-1$
			}

			int featureCount = c.outCollection.size();
			FeatureReader<SimpleFeatureType, SimpleFeature> reader =c.outCollection.reader();
		
			List<Muestra> muestrasGeneradas = new ArrayList<Muestra>();//agregadas+candidates
			int count=0;
			int countMuestraGenerada = 0;
			while (reader.hasNext()) {	//por cada poligono de las labores de entrada 
				SimpleFeature feature = reader.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				GeometryFactory fact = geometry.getFactory();
				LaborItem item=c.constructFeatureContainer(feature);
				LaborItem container =c.constructFeatureContainerStandar(feature, false);//(feature);

				Integer categoria =c.getClasificador().getCategoryFor(container.getAmount());
				Color color  = c.getClasificador().getColorForCategoria(categoria);
				
				String nombre =c.getClasificador().getLetraCat(categoria);//""+Clasificador.abc[size-categoria-1];
				colorCat.put(nombre,color);
				if(ambsRec.containsKey(nombre)) {
					List<Geometry> geomsAmb = ambsRec.get(nombre);
					geomsAmb.add(geometry);						
				} else {
					List<Geometry> geomsAmb = new ArrayList<Geometry>();
					geomsAmb.add(geometry);		
					ambsRec.put(nombre, geomsAmb);
				}
				

				boolean insertCentroid=true;
				Point centroid = geometry.getCentroid();
				ProyectionConstants.setLatitudCalculo(centroid.getY());
				double areaPoly = ProyectionConstants.A_HAS(geometry.getArea());
				if(areaPoly > superficieMinimaAMuestrear){//el poligono debe ser muestreado
					List<Muestra> puntosGeneradosGeom = new ArrayList<Muestra>();
					Random rand = new Random();
					double sigmaX = geometry.getEnvelopeInternal().getWidth()/2;
					double sigmaY = geometry.getEnvelopeInternal().getHeight()/2;
					double numeroMuestrasEstimadoFeature = densidadDeMuestrasDeseada*areaPoly;
					//mientas que la cantidad de puntos generados para el poligono sea menor que
					//la cantidadMinimaDeMuestrasPoligonoAMuestrear 
					//o la densidad de muestras sea menor que densidadDeMuestrasDeseada

					for(int i=0;(puntosGeneradosGeom.size() < cantidadMinimaDeMuestrasPoligonoAMuestrear 
							|| densidadDeMuestrasDeseada > (puntosGeneradosGeom.size()/areaPoly))
							&& i < 100000 * numeroMuestrasEstimadoFeature;  //limite de intentos si no se cumple la condicion
							i++){

						// generar puntos al azar que esten dentro del poligono y por cada punto crear agregar un sueloItem al suelo	
						/*
						 *if you want mean 1 hour and std-deviance 15 minutes you'll need to call it as nextGaussian()*15+60*/
						//TODO los puntos generados pueden tener una distribucion normal al rededor del centroide del poligono y desvio relacionado al area del poligono

						Point random =null;
						if(!insertCentroid) {
							double x = rand.nextGaussian()*sigmaX+centroid.getX();
							double y = rand.nextGaussian()*sigmaY+centroid.getY();
							random = fact.createPoint(new Coordinate(x,y));							
						} else {
							random = centroid;
							insertCentroid=false;
						}

						Coordinate l = new Coordinate(ProyectionConstants.metersToLongLat(ancho)/2  ,0);
						Coordinate d = new Coordinate(0, ProyectionConstants.metersToLongLat(ancho)/2);

						Polygon bufferMuestra = GeometryHelper.constructPolygon(l,d,random);

						//controlar que la distancia a una muestra anterior sea mayor a un minimo
						muestrasGeneradas.addAll(recorrida.muestras);
						muestrasGeneradas.addAll(puntosGeneradosGeom);//candidates

						double intersectionCount =
								muestrasGeneradas.stream().filter( m ->	
								bufferMuestra.intersects(fact.createPoint(new Coordinate(m.longitude,m.latitude)))
										).count();//verifico si random esta demasiado cerca de alguna de las muestras ya generadas						
						muestrasGeneradas.clear();
						//si el poligono esta dentro de la geometria a muestrear y la distancia mas chica a los puntos generados es mayor al minimo
						//lo agrego

						if(geometry.contains(bufferMuestra)//esto asegura distancia a la frontera 
								&& intersectionCount == 0) {//esto asegura distancia a los otros puntos
							Muestra muestra = new Muestra();
							muestra.setNombre(nombre);//una letra de A a I
							System.out.println("generando muestra con nombre "+nombre);
							muestra.setSubNombre(Integer.toString(countMuestraGenerada));
							countMuestraGenerada++;
							muestra.initObservacionSuelo();

							muestra.setLatitude(random.getY());
							muestra.setLongitude(random.getX());
							muestra.setRecorrida(recorrida);
							puntosGeneradosGeom.add(muestra);

							//labor.insertFeature(muestra);
						} 
					}//termine de crear los puntos para el poligono de tamanio suficiente
					recorrida.muestras.addAll(puntosGeneradosGeom);

				}//termino de evaluar el poligono con tamanio suficiente
				count++;//FIXME subnombre se repite
				updateProgress(count, featureCount);
				System.out.println("Termine de generar todos los puntos "+recorrida.muestras.size());
			}//termino de recorrer el while de una labor
			//c.getLayer().setEnabled(false);
			c.getLayer().setOpacity(0.18);
		}//termino de recorrer todas las labores
		recorrida.setAmbsGeoms(ambsRec);
		recorrida.setNombre(nombreRecorrida);

		//TODO crear un PathLayer con los puntos de itemsToShow
		ordenarMuestras(recorrida.muestras);
		System.out.println("Termine de ordenar las muestras");
		Muestra first = recorrida.muestras.get(0);
		
		recorrida.setLatitude(first.latitude);
		recorrida.setLongitude(first.longitude);		
		
		RenderableLayer layer = new RenderableLayer();
		renderRecorrida(layer,recorrida);
		return layer;
	}


	public static void renderRecorrida(RenderableLayer layer,Recorrida recorrida) {
		try {
			layer.setName(recorrida.getNombre());


			layer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, recorrida);//usar esto para no tener el layer dentro de la cosecha
			layer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, Recorrida.class);
			layer.setValue(ProcessMapTask.ZOOM_TO_KEY,recorrida.muestras.get(0).getPosition());
			
			List<String> categorias = recorrida.muestras.stream().map(Muestra::getNombre).distinct().collect(Collectors.toList());
			categorias.sort(Comparator.reverseOrder());
			String first = categorias.get(0);
			
			layer.removeAllRenderables();
			
			recorrida.muestras.stream().forEach(m->{
			
				Position pointPosition = m.getPosition();

				PointPlacemark pmStandard = new PointPlacemark(pointPosition);

				pmStandard.setLabelText(m.toString());//
				pmStandard.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
				
				int index = Clasificador.cba.indexOf(m.getNombre())-Clasificador.cba.indexOf(first);						
				Color color = Clasificador.getColorForCategoria(index , categorias.size());
				
				if(color==null)color = Color.WHITE;
				//int alfa = Double.parseDouble(color.getOpacity()*255).intValue();
				java.awt.Color awtColor = fxColorToAwtColor(color);
				
				PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
				//pointAttribute.setLabelMaterial(Material.LIGHT_GRAY);
				//0xAABBGGRR				
				//pointAttribute.setLabelColor(WWUtil.encodeColorABGR(awtColor));
				pointAttribute.setImageAddress("images/pushpins/castshadow-white.png");
				pointAttribute.setScale(0.6);
				pointAttribute.setImageColor(awtColor);
				pmStandard.setAttributes(pointAttribute);
				

				if(pmStandard!=null)layer.addRenderable(pmStandard);//extPoly.render(dc);
			});

			ShapeAttributes attrs = new BasicShapeAttributes();
			attrs.setOutlineMaterial(new Material(java.awt.Color.white));
			attrs.setOutlineWidth(5d);
			attrs.setOutlineOpacity(0.9);
			attrs.setEnableAntialiasing(true);
			attrs.setDrawOutline(true);
			

			Path path = new Path(recorrida.muestras.stream().map(m->m.getPosition()).collect(Collectors.toList()));
			path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
			path.setFollowTerrain(true);
			path.setNumSubsegments(1);
			path.setPathType(AVKey.LINEAR);
			path.setExtrude(true);
//			path.setPathType(AVKey.GREAT_CIRCLE);
//			path.setPathType(AVKey.LOXODROME);
//			path.setPathType(AVKey.RHUMB_LINE);
			path.setAttributes(attrs);
			//path.setShowPositionsScale(2);
			layer.addRenderable(path);


		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static java.awt.Color fxColorToAwtColor(Color color) {
		int red = Double.valueOf(color.getRed()*255).intValue();
		int green = Double.valueOf(color.getGreen()*255).intValue();
		int blue = Double.valueOf(color.getBlue()*255).intValue();
		java.awt.Color awtColor= new java.awt.Color (red,green,blue);
		return awtColor;
	}	


	/**
	 * metood que toma una lista
	 * @param muestras
	 */
	private void ordenarMuestras(List<Muestra> muestras) {
		//System.out.println("items "+muestras.size());
		List<Position> positions = muestras.stream().map(m->m.getPosition()).collect(Collectors.toList());

		Camino c = new Camino(positions);
		SimplificarCaminoTask t = new SimplificarCaminoTask(c);
		try {
			c = t.call();
		} catch (Exception e) {			
			e.printStackTrace();
		}

		//TODO poner items en el orden en el que aparecen en positions para que el recorrido sea el minimo
		List<Muestra> newItems = new ArrayList<Muestra>();
		for(Position np :c.getPositions()) {	//FIXME tengo dudas sobre la forma en que matchea los elementos		
			for(Muestra s : muestras) {

				Position pos =s.getPosition();
				if(pos.latitude.equals(np.latitude)&& pos.longitude.equals(np.longitude)) {
					newItems.add(s);
					//System.out.println("insertando "+pos+" suelo de "+muestras.indexOf(s)+" en "+newItems.indexOf(s));
					break;	
				}
			}
		}
		//System.out.println("newItems "+newItems.size());
		muestras.clear();
		muestras.addAll(newItems);	
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
