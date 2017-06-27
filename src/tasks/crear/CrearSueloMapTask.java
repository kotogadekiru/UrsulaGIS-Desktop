package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import dao.Poligono;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.geom.Position;
import javafx.geometry.Point2D;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class CrearSueloMapTask extends ProcessMapTask<SueloItem,Suelo> {
	Double amount = new Double(0);
	Double ppmN = new Double(0);
	Poligono poli=null;

	public CrearSueloMapTask(Suelo labor,Poligono _poli,Double _amount, Double _ppmN){
		super(labor);
		amount=_amount;
		ppmN=_ppmN;
		poli=_poli;
		labor.getNombreProperty().setValue(poli.getNombre());

	}

	public void doProcess() throws IOException {
		SueloItem si = new SueloItem();
		si.setPpmP(amount);
		si.setPpmN(ppmN);
		labor.setPropiedadesLabor(si);
		GeometryFactory fact = new GeometryFactory();
		List<? extends Position> positions = poli.getPositions();
		Coordinate[] coordinates = new Coordinate[positions.size()];
		for(int i=0;i<positions.size();i++){
			Position p = positions.get(i);	
			Coordinate c = new Coordinate(p.getLongitude().getDegrees(),p.getLatitude().getDegrees(),p.getElevation());
			
			coordinates[i]=c;
		}
		coordinates[coordinates.length-1]=coordinates[0];//en caso de que la geometria no este cerrada
		Polygon poly = fact.createPolygon(coordinates);	

		si.setGeometry(poly);
		
		labor.insertFeature(si);
				
		labor.constructClasificador();

		List<SueloItem> itemsToShow = new ArrayList<SueloItem>();
		itemsToShow.add(si);
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}


	@Override
	public  void  getPathTooltip( Geometry poly,SueloItem si) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String(
				" PpmFosforo/Ha: "+ df.format(si.getPpmP()) +"\n"
				+"PpmNitrogeno/Ha: "+ df.format(si.getPpmN()) +"\n"
				);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		super.getRenderPolygonFromGeom(poly, si,tooltipText);
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task