package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import javafx.geometry.Point2D;
import tasks.ProcessMapTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;

public class CrearCosechaMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	Double rinde = new Double(0);
	List<Poligono> polis=null;

	public CrearCosechaMapTask(CosechaLabor cosechaLabor,List<Poligono> _poli,Double _rinde){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		rinde=_rinde;
		polis=_poli;

	}

	public void doProcess() throws IOException {
		//labor.setContorno(GeometryHelper.unirPoligonos(polis));
		for(Poligono poli : polis) {
		CosechaItem ci = new CosechaItem();
		ci.setRindeTnHa(rinde);
//		ci.setPrecioTnGrano(labor.precioGranoProperty.get());
//		ci.setCostoLaborHa(labor.precioLaborProperty.get());
//		ci.setCostoLaborTn(labor.costoCosechaTnProperty.get());
		labor.setPropiedadesLabor(ci);

		ci.setGeometry(poli.toGeometry());
		ci.setId(labor.getNextID());
		ci.setElevacion(10.0);
		labor.insertFeature(ci);
		}
		labor.constructClasificador();

		
		runLater(this.getItemsList());
		updateProgress(0, featureCount);

	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//	}

	public static String buildTooltipText(CosechaItem cosechaItem, double area) {
		NumberFormat df = Messages.getNumberFormat();
		StringBuilder sb = new StringBuilder();
		//rinde
		sb.append(Messages.getString("ProcessHarvestMapTask.23")); 
		sb.append(df.format(cosechaItem.getAmount())); 
		sb.append(Messages.getString("ProcessHarvestMapTask.24"));
		//ancho
		sb.append(Messages.getString("ProcessHarvestMapTask.27")
				+df.format(cosechaItem.getAncho() ) 
				+ Messages.getString("ProcessHarvestMapTask.28")); 
		//rumbo
		sb.append(Messages.getString("ProcessHarvestMapTask.29")
				+df.format(cosechaItem.getRumbo() ) 
				+Messages.getString("ProcessHarvestMapTask.30")); 
		//id
		sb.append(Messages.getString("ProcessHarvestMapTask.31")
				+cosechaItem.getId() 
				+Messages.getString("ProcessHarvestMapTask.32")); 
		//elevacion
		sb.append(Messages.getString("ProcessHarvestMapTask.25")
				+df.format(cosechaItem.getElevacion() ) 
				+ Messages.getString("ProcessHarvestMapTask.26"));
		
		sb.append(Messages.getString("OpenMargenMapTask.15")
				+df.format(cosechaItem.getImporteHa() ) 
				+ Messages.getString("OpenMargenMapTask.16"));
		
//		sb.append("Observaciones: "
//				+cosechaItem.getObservaciones()
//				+"\n");
		
		//superficie
		if(area<1){
			sb.append( Messages.getString("ProcessHarvestMapTask.33")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessHarvestMapTask.34")); 
		} else {
			sb.append(Messages.getString("ProcessHarvestMapTask.35")+df.format(area ) + Messages.getString("ProcessHarvestMapTask.36")); 
		}
		return sb.toString();
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task