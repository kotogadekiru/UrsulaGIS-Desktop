package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.geotools.api.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.recorrida.Camino;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

@Deprecated
public class GenerarMuestreoDirigidoTask extends ProcessMapTask<SueloItem,Suelo> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<Labor<? extends LaborItem>> aMuestrear;
	private double superficieMinimaAMuestrear=0;
	private double densidadDeMuestrasDeseada=0;
	//private double cantidadMaximaDeMuestrasTotal=Double.MAX_VALUE;
	
	private double cantidadMinimaDeMuestrasPoligonoAMuestrear=0;


	public GenerarMuestreoDirigidoTask(List<Labor<? extends LaborItem>> cosechas,double supMinima,double densidad,double cantMaxPoly){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.aMuestrear=cosechas;

		super.labor = new Suelo();
		//super.labor.featureBuilder = new SimpleFeatureBuilder(super.labor.getPointType());
		this.superficieMinimaAMuestrear=supMinima;
		this.densidadDeMuestrasDeseada=1/densidad;
		this.cantidadMinimaDeMuestrasPoligonoAMuestrear=cantMaxPoly;


		labor.setNombre(Messages.getString("GenerarMuestreoDirigidoTask.0"));//este es el nombre que se muestra en el progressbar //$NON-NLS-1$
	}

	/**
	 *Proceso que genera una lista de puntos al azar dentro de cada zona de acuerdo a la frecuencia minima especificada
	 */
	@Override
	protected void doProcess() throws IOException {
		String nombre =null;
		//ancho me permite controlar la distancia minima entre los puntos y entre el punto y la frontera
		double ancho = 1 + Math.sqrt(superficieMinimaAMuestrear*ProyectionConstants.METROS2_POR_HA)/10;
		System.out.println("ancho="+ancho); //ancho=86.60254037844386
		featureCount = 100;//TODO estimar cantidad a procesar
		//List<SueloItem> features = Collections.synchronizedList(new ArrayList<SueloItem>());
		for(Labor<? extends LaborItem> c:aMuestrear){			
			if(nombre == null){
				nombre=labor.getNombre()+Messages.getString("GenerarMuestreoDirigidoTask.1")+c.getNombre();	 //$NON-NLS-1$
			}else {
				nombre+=Messages.getString("GenerarMuestreoDirigidoTask.2")+c.getNombre(); //$NON-NLS-1$
			}
			labor.setClasificador(c.getClasificador().clone());
			FeatureReader<SimpleFeatureType, SimpleFeature> reader =c.outCollection.reader();
			//por cada poligono de las labores de entrada 
			int count=0;
			while (reader.hasNext()) {
				SimpleFeature feature = reader.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				LaborItem container =c.constructFeatureContainer(feature);
				Integer categoria =c.getClasificador().getCategoryFor(container.getAmount());
				//System.out.println("categoria para Amount "+container.getAmount()+" es: "+categoria);//OK! categoria para Amount 12.28167988386877 es: 1
				
				//TODO si el area del poligono es mayor que la superficieMinimaAMuestrear
				boolean insertCentroid=true;
				Point centroid = geometry.getCentroid();
				ProyectionConstants.setLatitudCalculo(centroid.getY());
				double areaPoly = ProyectionConstants.A_HAS(geometry.getArea());
				if(areaPoly>superficieMinimaAMuestrear){
					List<SueloItem> puntosGenerados = new ArrayList<SueloItem>();
					Random rand = new Random();
					double sigmaX = geometry.getEnvelopeInternal().getWidth()/2;
					double sigmaY = geometry.getEnvelopeInternal().getHeight()/2;
					//double sigma = Math.sqrt(geometry.getArea())/3;//area en longLat
					//System.out.println("creando un muestreo con sigma = "+sigmaX+" , "+sigmaY);
					double posiciones =densidadDeMuestrasDeseada*areaPoly+1000;
					//TODO mientas que la cantidad de puntos generados para el poligono sea menor que la cantidadMinimaDeMuestrasPoligonoAMuestrear o la densidad de muestras sea menor que densidadDeMuestrasDeseada
					
					for(int i=0;(puntosGenerados.size()<cantidadMinimaDeMuestrasPoligonoAMuestrear 
							|| densidadDeMuestrasDeseada > (puntosGenerados.size()/areaPoly))
							&&i<100*posiciones  ;i++){

						//TODO generar puntos al azar que esten dentro del poligono y por cada punto crear agregar un sueloItem al suelo	
						/*
						 *if you want mean 1 hour and std-deviance 15 minutes you'll need to call it as nextGaussian()*15+60*/
						//TODO los puntos generados pueden tener una distribucion normal al rededor del centroide del poligono y desvio relacionado al area del poligono
						
						Point random =null;
						
						if(!insertCentroid) {
							double x =rand.nextGaussian()*sigmaX+centroid.getX();
							double y =rand.nextGaussian()*sigmaY+centroid.getY();
							random =centroid.getFactory().createPoint(new Coordinate(x,y));							
							
						} else {
							random =centroid;
							insertCentroid=false;
						}
						
						Coordinate l = new Coordinate(ProyectionConstants.metersToLongLat(ancho)/2  ,0);
						Coordinate d = new Coordinate(0,ProyectionConstants.metersToLongLat(ancho)/2 );
						
						Polygon poly =constructPolygon(l,d,random);
						//System.out.println(poly.toText());
					//	System.out.println("has "+ProyectionConstants.A_HAS(poly.getArea()));
						//TODO controlar que la distancia a una muestra anterior sea mayor a un minimo
						double minDist = puntosGenerados.parallelStream().flatMapToDouble(p->
						DoubleStream.of(p.getGeometry().distance(poly))
								).min().orElse(Double.MAX_VALUE)/ProyectionConstants.metersToLat();
						//System.out.println("minDist="+minDist);
						
						if(geometry.contains(poly) && minDist>ancho) {
						//	System.out.println(Messages.getString("GenerarMuestreoDirigidoTask.3")+random); //$NON-NLS-1$
							SueloItem muestra = new SueloItem();
							muestra.setCategoria(categoria);
							muestra.setPpmP(categoria.doubleValue());
							muestra.setId(labor.getNextID());
							muestra.setGeometry(poly);
							puntosGenerados.add(muestra);
							labor.insertFeature(muestra);
						} else if(geometry.contains(poly)) {
							//double distancia = geometry.distance(poly)/ProyectionConstants.metersToLat();							
							//System.out.println("rechazando punto por estar cerca de la frontera o de otro punto "+"minDist="+minDist);
						}
					}//termine de crear los puntos para el poligono de tamanio suficiente
					//features.addAll(puntosGenerados);

				}//termino de evaluar el poligono con tamanio suficiente
				featureNumber++;
				updateProgress( featureNumber,featureCount);
			}//termino de recorrer el while de una labor
		}//termino de recorrer todas las labores

		

		//TODO crear un PathLayer con los puntos de itemsToShow
		createMuestreoPathLayer(this.getItemsList());
		labor.setNombre(nombre);
		labor.setLayer(new LaborLayer());
		//List<?> featureList = features.stream().map(f ->{
		//	System.out.println("recorriendo features "+f);
		//	return f.getFeature( labor.getFeatureBuilder());	
		//}).
        //collect(Collectors.toList());
		//labor.outCollection.addAll(featureList);

		//TODO 4 mostrar la cosecha sintetica creada
		labor.constructClasificador(); //no construir clasificador. usar el existente.
		
		
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	
	public Polygon constructPolygon(Coordinate l, Coordinate d, Point X) {
		double x = X.getX();
		double y = X.getY();
	
		Coordinate D = new Coordinate(x - l.x - d.x, y - l.y - d.y); // x-l-d
		Coordinate C = new Coordinate(x + l.x - d.x, y + l.y - d.y);// X+l-d
		Coordinate B = new Coordinate(x + l.x + d.x, y + l.y + d.y);// X+l+d
		Coordinate A = new Coordinate(x - l.x + d.x, y - l.y + d.y);// X-l+d

		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = X.getFactory();

		//		LinearRing shell = fact.createLinearRing(coordinates);
		//		LinearRing[] holes = null;
		//		Polygon poly = new Polygon(shell, holes, fact);
		Polygon poly = fact.createPolygon(coordinates);
	
		return poly;
	}
	
	/**
	 * metood que toma una lista
	 * @param itemsToShow
	 */
	private void createMuestreoPathLayer(List<SueloItem> itemsToShow) {
		System.out.println("items "+itemsToShow.size());
		List<Position> positions = itemsToShow.stream().map(sueloItem->{
			Point p = sueloItem.getGeometry().getCentroid();
			Position pos = Position.fromDegrees(p.getY(),p.getX());
			return pos;
			}
		).collect(Collectors.toList());
		
		Camino c = new Camino(positions);
		SimplificarCaminoTask t = new SimplificarCaminoTask(c);
		t.run();
		
		//TODO poner items en el orden en el que aparecen en positions para que el recorrido sea el minimo
		List<SueloItem> newItems = new ArrayList<SueloItem>();
		for(Position np :positions) {	//FIXME tengo dudas sobre la forma en que matchea los elementos		
			for(SueloItem s : itemsToShow) {
				Point p = s.getGeometry().getCentroid();
				Position pos = Position.fromDegrees(p.getY(),p.getX());
				if(pos.latitude.equals(np.latitude)&& pos.longitude.equals(np.longitude)) {
					newItems.add(s);
					System.out.println("insertando "+pos+" suelo de "+itemsToShow.indexOf(s)+" en "+newItems.indexOf(s));
					break;	
				}
			}
		}
		System.out.println("newItems "+newItems.size());
		itemsToShow.clear();
		itemsToShow.addAll(newItems);	
	}

	private double getAreaMinimaLongLat() {	
		return this.superficieMinimaAMuestrear*(ProyectionConstants.metersToLong()*ProyectionConstants.metersToLat());
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	SueloItem sueloItem,ExtrudedPolygon  renderablePolygon) {
//
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
//		NumberFormat df = Messages.getNumberFormat();//new DecimalFormat("0.00");//$NON-NLS-2$
//
//		String tooltipText = new String(Messages.getString("GenerarMuestreoDirigidoTask.6")+ df.format(sueloItem.getPpmP()) +Messages.getString("GenerarMuestreoDirigidoTask.7")); //$NON-NLS-1$ //$NON-NLS-2$
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.8")+ df.format(sueloItem.getPpmNO3()) + Messages.getString("GenerarMuestreoDirigidoTask.9")); //$NON-NLS-1$ //$NON-NLS-2$
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.10")+ df.format(sueloItem.getPpmS()) + Messages.getString("GenerarMuestreoDirigidoTask.11")); //$NON-NLS-1$ //$NON-NLS-2$
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.12")+ df.format(sueloItem.getPpmK()) + Messages.getString("GenerarMuestreoDirigidoTask.13")); //$NON-NLS-1$ //$NON-NLS-2$
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.14")+ df.format(sueloItem.getPorcMO()) + Messages.getString("GenerarMuestreoDirigidoTask.15")); //$NON-NLS-1$ //$NON-NLS-2$
//
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.16")+df.format(sueloItem.getElevacion() ) + Messages.getString("GenerarMuestreoDirigidoTask.17")); //$NON-NLS-1$ //$NON-NLS-2$
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.18")+df.format(sueloItem.getCategoria() ) + Messages.getString("GenerarMuestreoDirigidoTask.19")); //$NON-NLS-1$ //$NON-NLS-2$
//
//
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.20")+sueloItem.getId() + Messages.getString("GenerarMuestreoDirigidoTask.21")); //$NON-NLS-1$ //$NON-NLS-2$
//		tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.22")+sueloItem.getGeometry().getCoordinate() + Messages.getString("GenerarMuestreoDirigidoTask.23")); //$NON-NLS-1$ //$NON-NLS-2$
//		if(area<1){
//			tooltipText=tooltipText.concat( Messages.getString("GenerarMuestreoDirigidoTask.24")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("GenerarMuestreoDirigidoTask.25")); //$NON-NLS-1$ //$NON-NLS-2$
//			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
//		} else {
//			tooltipText=tooltipText.concat(Messages.getString("GenerarMuestreoDirigidoTask.26")+df.format(area ) + Messages.getString("GenerarMuestreoDirigidoTask.27")); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		//super.getRenderPolygonFromGeom(poly, cosechaItem,tooltipText);
//		return super.getExtrudedPolygonFromGeom(poly, sueloItem,tooltipText,renderablePolygon);
//
//	}

	@Override
	protected int getAmountMin() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		// TODO Auto-generated method stub
		return 0;
	}

}
