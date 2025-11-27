package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Poligono;

import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.pulverizacion.*;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;


/**
 * task que convierte una cosecha a una pulverizacion
 * @author quero
 *
 */
public class ConvertirAPulverizacionTask extends ProcessMapTask<PulverizacionItem,PulverizacionLabor> {
	Map<String,Double[]> dosisMap = null;//0.0;
	CosechaLabor cosecha=null;

	public ConvertirAPulverizacionTask(CosechaLabor _cosecha,PulverizacionLabor labor,Map<String,Double[]> valores){
		super(labor);
		dosisMap=valores;
		cosecha=_cosecha;

	}

	public void doProcess() throws IOException {
		//labor.setContorno(cosecha.getContorno());
		this.featureCount=cosecha.outCollection.size();
		this.featureNumber=0;
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
		Clasificador cl = cosecha.getClasificador();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);			
			String nombre = cl.getLetraCat(cl.getCategoryFor(ci.getRindeTnHa()));
			double dosis = dosisMap.get(nombre)[0];
		
			PulverizacionItem si = new PulverizacionItem();			
			si.setDosis(dosis);	
			labor.setPropiedadesLabor(si);
			si.setGeometry(ci.getGeometry());
			si.setId(labor.getNextID());
			si.setElevacion(10.0);
			labor.insertFeature(si);
			this.updateProgress(featureNumber++, featureCount);
		}
		reader.close();		

		labor.constructClasificador();
		cosecha.getLayer().setEnabled(false);
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, PulverizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearPulverizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task