package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import dao.Labor;
import dao.LaborItem;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.nww.LaborLayer;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

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
	super.labor.featureBuilder = new SimpleFeatureBuilder(super.labor.getPointType());
		this.superficieMinimaAMuestrear=supMinima;
		this.densidadDeMuestrasDeseada=1/densidad;
		this.cantidadMinimaDeMuestrasPoligonoAMuestrear=cantMaxPoly;


		labor.getNombreProperty().setValue("Muestreo Dirigido");//este es el nombre que se muestra en el progressbar
	}

	/**
	 * proceso que toma una lista de cosechas y las une 
	 * con una grilla promediando los valores de acuerdo a su promedio ponderado por la superficie
	 * superpuesta de cada item sobre la superficie superpuesta total de cada "pixel de la grilla"
	 */
	@Override
	protected void doProcess() throws IOException {
	
		Random rand=new Random();

		String nombre =null;

		//List<SueloItem> features = Collections.synchronizedList(new ArrayList<SueloItem>());
		for(Labor<? extends LaborItem> c:aMuestrear){			
			if(nombre == null){
				nombre=labor.getNombreProperty().get()+" "+c.getNombreProperty().get();	
			}else {
				nombre+=" - "+c.getNombreProperty().get();
			}

			FeatureReader<SimpleFeatureType, SimpleFeature> reader =c.outCollection.reader();
			//por cada poligono de las labores de entrada 
			while (reader.hasNext()) {
				SimpleFeature feature = reader.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();

				//TODO si el area del poligono es mayor que la superficieMinimaAMuestrear

				Point centroid = geometry.getCentroid();
				ProyectionConstants.setLatitudCalculo(centroid.getY());
				double areaPoly = ProyectionConstants.A_HAS(geometry.getArea());
				if(areaPoly>superficieMinimaAMuestrear){
					List<SueloItem> puntosGenerados = new ArrayList<SueloItem>();
					//TODO mientas que la cantidad de puntos generados para el poligono sea menor que la cantidadMinimaDeMuestrasPoligonoAMuestrear o la densidad de muestras sea menor que densidadDeMuestrasDeseada
					while(puntosGenerados.size()<cantidadMinimaDeMuestrasPoligonoAMuestrear || densidadDeMuestrasDeseada > (puntosGenerados.size()/areaPoly)  ){

						//TODO generar puntos al azar que esten dentro del poligono y por cada punto crear agregar un sueloItem al suelo	
						/*
						 *if you want mean 1 hour and std-deviance 15 minutes you'll need to call it as nextGaussian()*15+60*/
						//TODO los puntos generados pueden tener una distribucion normal al rededor del centroide del poligono y desvio relacionado al area del poligono

						double sigma = Math.sqrt(geometry.getArea());
						double x =rand.nextGaussian()*sigma+centroid.getX();
						double y =rand.nextGaussian()*sigma+centroid.getY();

						Point random =centroid.getFactory().createPoint(new Coordinate(x,y));
						
						if(geometry.contains(random)){
							System.out.println("generando un punto random "+random);
							SueloItem muestra = new SueloItem();
							muestra.setId(labor.getNextID());
							muestra.setGeometry(random);
							puntosGenerados.add(muestra);
							labor.insertFeature(muestra);
							
						}				
					}//termine de crear los puntos para el poligono de tamanio suficiente
					//features.addAll(puntosGenerados);

				}//termino de evaluar el poligono con tamanio suficiente
			}//termino de recorrer el while de una labor
		}//termino de recorrer todas las labores

		
		List<SueloItem> itemsToShow = new ArrayList<SueloItem>();

		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f=it.next();
			
			itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
		}
		it.close();


		labor.nombreProperty.set(nombre);
		labor.setLayer(new LaborLayer());
		//List<?> featureList = features.stream().map(f ->{
		//	System.out.println("recorriendo features "+f);
		//	return f.getFeature( labor.getFeatureBuilder());	
		//}).
        //collect(Collectors.toList());
		//labor.outCollection.addAll(featureList);

		//TODO 4 mostrar la cosecha sintetica creada
		labor.constructClasificador();


		
		System.out.println("items to show antes del runlater "+itemsToShow.size());
		runLater(itemsToShow);
		updateProgress(0, featureCount);


	}


	private double getAreaMinimaLongLat() {	
		return this.superficieMinimaAMuestrear*(ProyectionConstants.metersToLong()*ProyectionConstants.metersToLat());
	}




	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly,	SueloItem sueloItem) {

		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("0.00");

		String tooltipText = new String(
				" Fosforo: "+ df.format(sueloItem.getPpmP()) +"Ppm\n"
				);
		tooltipText=tooltipText.concat("Nitrogeno: "+ df.format(sueloItem.getPpmN()) + "Ppm\n");
		tooltipText=tooltipText.concat("Azufre: "+ df.format(sueloItem.getPpmS()) + "Ppm\n");
		tooltipText=tooltipText.concat("Azufre: "+ df.format(sueloItem.getPpmS()) + "Ppm\n");
		tooltipText=tooltipText.concat("Potasio: "+ df.format(sueloItem.getPpmS()) + "Ppm\n");

		tooltipText=tooltipText.concat("Elevacion: "+df.format(sueloItem.getElevacion() ) + "\n");


		tooltipText=tooltipText.concat("Id: "+sueloItem.getId() + "\n");
		tooltipText=tooltipText.concat("Coordenadas: "+sueloItem.getGeometry().getCoordinate() + "\n");
		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}
		//super.getRenderPolygonFromGeom(poly, cosechaItem,tooltipText);
		return super.getExtrudedPolygonFromGeom(poly, sueloItem,tooltipText);

	}

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
