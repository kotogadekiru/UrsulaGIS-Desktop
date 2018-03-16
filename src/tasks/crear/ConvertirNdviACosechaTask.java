package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.Ndvi;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import tasks.ProcessMapTask;
import tasks.ShowNDVITifFileTask;
import utils.ProyectionConstants;

public class ConvertirNdviACosechaTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	Double rinde = new Double(0);
	Ndvi ndvi=null;

	public ConvertirNdviACosechaTask(CosechaLabor cosechaLabor,Ndvi _ndvi,Double _rinde){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		rinde=_rinde;
		ndvi=_ndvi;

	}

	public void doProcess() throws IOException {
		
		Iterable<? extends GridPointAttributes> values = ndvi.getSurfaceLayer().getValues();
		Iterator<? extends GridPointAttributes> it = values.iterator();
		
		double sum =0;
		int size =0;
		while(it.hasNext()){
			GridPointAttributes gpa = it.next();
			Double value = new Double(gpa.getValue());
			//OJO cuando se trabaja con el jar la version de world wind que esta levantando no es la que usa eclipse.
			// es por eso que el jar permite valores Nan y en eclipse no.
			if(value < ShowNDVITifFileTask.MIN_VALUE || value > ShowNDVITifFileTask.MAX_VALUE || value == 0 || value.isNaN()){
					continue;
			} else{
			//System.out.println("calculando el promedio para el valor "+value+" suma parcial "+sum+" size "+size);
			sum+=value;
			size++;
			}
		}
		if(size == 0){
			System.err.println("no hay puntos iterables para obtener el promedio");
			return;
		}
		this.featureCount=size;
		Double average = new Double(sum/size);
		System.out.println("el promedio de los ndvi es "+average);
		if(average.isNaN())average =1.0;
		 it = values.iterator();
		 
	 
		 
		ExportableAnalyticSurface exportableSurface = ndvi.getSurfaceLayer();//.getSurfaceAttributes();
		Sector sector = exportableSurface.getSector();
		int[] dimensions = exportableSurface.getDimensions();//raster.getWidth(), raster.getHeight()
		
		final int width = dimensions[0];
		final int height = dimensions[1];
		
		GeometryFactory fact = new GeometryFactory();
		
		double latStep = -sector.getDeltaLatDegrees() / (double) (height-1);//-1
		double lonStep = sector.getDeltaLonDegrees() / (double) (width-1);
		
		
		List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();
		
		double elev = 1;
		double minLat = sector.getMaxLatitude().degrees;
		double minLon = sector.getMinLongitude().degrees;
		
		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
				labor.getType());
		
		double id=0;
		for (int y = 0; y < height; y++){
			double lat = minLat + y * latStep;
			for (int x = 0; x < width; x++)	{
				double lon = minLon+x*lonStep;
				
				GridPointAttributes attr = it.hasNext() ? it.next() : null;
				if(x==0 || y==0||x==width-1||y==height-1)continue;//me salteo la primera fila de cada costado
				double value = attr.getValue();
				
				if(value <= ShowNDVITifFileTask.MAX_VALUE && value >= ShowNDVITifFileTask.MIN_VALUE && value !=0){
					CosechaItem ci = new CosechaItem();
					ci.setId(id);
					ci.setElevacion(elev);
					ProyectionConstants.setLatitudCalculo(lat);
					double ancho =lonStep * ProyectionConstants.metersToLong();
					ci.setAncho(ancho);
					ci.setDistancia(ancho);
					
					Double rindeNDVI = new Double(rinde*value/average);
					//System.out.println("creado nueva cosecha con rinde "+rindeNDVI+" para el ndvi "+value+" rinde prom "+rinde+" ndvi promedio "+average);
					ci.setRindeTnHa(rindeNDVI);
					labor.setPropiedadesLabor(ci);
					
					Coordinate[] coordinates = new Coordinate[5];
					coordinates[0]= new Coordinate(lon,lat,elev);
					coordinates[1]= new Coordinate(lon+lonStep,lat,elev);
					coordinates[2]= new Coordinate(lon+lonStep,lat+latStep,elev);
					coordinates[3]= new Coordinate(lon,lat+latStep,elev);
					coordinates[4]=new Coordinate(lon,lat,elev);
					
					Polygon poly = fact.createPolygon(coordinates);	
					ci.setGeometry(poly);
					
					SimpleFeature f = ci.getFeature(fBuilder);
					boolean res = features.add(f);
					if(!res){
						System.out.println("no se pudo agregar la feature "+ci);
					}
				//	labor.insertFeature(ci);
					itemsToShow.add(ci);
					id++;
					updateProgress(id, featureCount);

			}
				
				
			//	lon += lonStep;
			}
		//lat += latStep;
		}
		
	
		if(labor.inCollection == null){
			labor.inCollection = new DefaultFeatureCollection("internal",labor.getType());
		}
		labor.inCollection.addAll(features);
		boolean ret= labor.outCollection.addAll(features);
		if(!ret){//XXX si esto falla es provablemente porque se estan creando mas de una feature con el mismo id
			System.out.println("no se pudieron agregar las features al outCollection");
		}

				
		labor.constructClasificador();	
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}


	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("#.00");

		String tooltipText = new String("Rinde: "
				+ df.format(cosechaItem.getAmount()) + " Tn/Ha\n"
				//	+ "Area: "+ df.format(area * ProyectionConstants.METROS2_POR_HA)+ " m2\n" + 

				);

		tooltipText=tooltipText.concat("Elevacion: "+df.format(cosechaItem.getElevacion() ) + "\n");

		tooltipText=tooltipText.concat("Ancho: "+df.format(cosechaItem.getAncho() ) + "\n");
		tooltipText=tooltipText.concat("Rumbo: "+df.format(cosechaItem.getRumbo() ) + "\n");
		tooltipText=tooltipText.concat("feature: "+cosechaItem.getId() + "\n");
		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}
		//super.getRenderPolygonFromGeom(poly, cosechaItem,tooltipText);
	return 	super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText);

	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task