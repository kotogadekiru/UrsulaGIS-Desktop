package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Labor;
import dao.LaborItem;
import dao.Ndvi;
import dao.Poligono;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import javafx.geometry.Point2D;
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
			double value = gpa.getValue();
			if(value <0.2||value>0.9 || value == 0){
					continue;
				}
			sum+=value;
			size++;
		}
		
		double average = sum/size;
		 it = values.iterator();
		 
//		 BufferWrapperRaster raster = ShowNDVITifFileTask.loadRasterFile(ndvi.getF());
//		 raster.getHeight()
//		 raster.getWidth()
//		 
		  ExportableAnalyticSurface exportableSurface = ndvi.getSurfaceLayer();//.getSurfaceAttributes();
		Sector sector = exportableSurface.getSector();
		int[] dimensions = exportableSurface.getDimensions();//raster.getWidth(), raster.getHeight()
		
		final int width = dimensions[0];
		final int height = dimensions[1];
		
		GeometryFactory fact = new GeometryFactory();
		
		double latStep = -sector.getDeltaLatDegrees() / (double) (height - 1);
		double lonStep = sector.getDeltaLonDegrees() / (double) (width - 1);
		
		
		List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();
		
		double elev =1;
		double minLat = sector.getMaxLatitude().degrees;
		double minLon = sector.getMinLongitude().degrees;
		
		double id=0;
		for (int y = 0; y < height; y++){
			double lat = minLat+y*latStep;
			for (int x = 0; x < width; x++)	{
				double lon = minLon+x*lonStep;
				
				GridPointAttributes attr = it.hasNext() ? it.next() : null;
				double value = attr.getValue();

				CosechaItem ci = new CosechaItem();
				ci.setId(id);
				ci.setElevacion(elev);
				ProyectionConstants.setLatitudCalculo(lat);
				double ancho =lonStep* ProyectionConstants.metersToLong();
				ci.setAncho(ancho);
				ci.setDistancia(ancho);
				ci.setRindeTnHa(rinde*value/average);
				labor.setPropiedadesLabor(ci);
				
				Coordinate[] coordinates = new Coordinate[5];
				coordinates[0]= new Coordinate(lon,lat,elev);
				coordinates[1]= new Coordinate(lon+lonStep,lat,elev);
				coordinates[2]= new Coordinate(lon+lonStep,lat+latStep,elev);
				coordinates[3]= new Coordinate(lon,lat+latStep,elev);
				coordinates[4]=new Coordinate(lon,lat,elev);
				
				Polygon poly = fact.createPolygon(coordinates);	
				
				ci.setGeometry(poly);
				
				if(value <0.9 && value > 0.2 && value !=0){
					labor.insertFeature(ci);
					itemsToShow.add(ci);
					id++;
			}
				
				
			//	lon += lonStep;
			}
		//lat += latStep;
		}
		
	

				
		labor.constructClasificador();	
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}


	@Override
	protected void getPathTooltip(Geometry poly,	CosechaItem cosechaItem) {
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
		super.getExrudedPolygonFromGeom(poly, cosechaItem,tooltipText);

	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task