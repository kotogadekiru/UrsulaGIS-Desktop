package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.api.data.FeatureReader;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.geometry.BoundingBox;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import javafx.geometry.Point2D;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class CrearCosechaMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	Double rinde = 0.0;
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
				+"\n"); 
		//rumbo
		sb.append(Messages.getString("ProcessHarvestMapTask.29")
				+df.format(cosechaItem.getRumbo() ) 
				+"\n"); 
		//id
		sb.append(Messages.getString("ProcessHarvestMapTask.31")
				+cosechaItem.getId() 
				+"\n"); 
		//elevacion
		sb.append(Messages.getString("ProcessHarvestMapTask.25")
				+df.format(cosechaItem.getElevacion() ) 
				+"\n"
				);
		
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