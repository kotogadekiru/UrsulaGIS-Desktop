package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;


/**
 * task que convierte una fertilizacion en 2
 * @author quero
 *
 */
public class ProcessSplitFertMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	Double firtPartPC=0.5;
	Double minFert=0.0;
	Double maxFert=Double.MAX_VALUE;
	FertilizacionLabor laborAPartir=null;

	public ProcessSplitFertMapTask(
			FertilizacionLabor laborConfig,
			FertilizacionLabor _laborAPartir,
			Double _firtPartPC,
			Double min,
			Double max){
		super(laborConfig);
		firtPartPC=_firtPartPC;
		laborAPartir=_laborAPartir;
		minFert=min;
		maxFert=max;

	}

	public void doProcess() throws IOException {

		FeatureReader<SimpleFeatureType, SimpleFeature> reader =laborAPartir.outCollection.reader();
		
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			FertilizacionItem aPartirItem = laborAPartir.constructFeatureContainerStandar(simpleFeature,false);			
							
			double dosis = aPartirItem.getDosistHa()*firtPartPC;
			if(minFert!=null)dosis = Math.max(minFert, dosis);
			if(maxFert!=null)dosis = Math.min(maxFert, dosis);
	
			
			FertilizacionItem si = new FertilizacionItem();			
			si.setDosistHa(dosis);
			labor.setPropiedadesLabor(si);

			si.setGeometry(aPartirItem.getGeometry());
			si.setId(labor.getNextID());
			si.setElevacion(aPartirItem.getElevacion());
			labor.insertFeature(si);
		}
		reader.close();
		
//		for(Poligono pol : this.polis) {
//			SiembraItem si = new SiembraItem();
//					
//			si.setDosisHa(semillasHa*pmil/(1000*1000));//1000semillas*1000gramos para pasar a kg/ha
//
//			si.setDosisML(semillasMetroLineal);
//			//dosis sembradora va en semillas cada 10mts
//			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg
//			
//			labor.setPropiedadesLabor(si);
//
//			si.setGeometry(pol.toGeometry());
//			si.setId(labor.getNextID());
//
//			labor.insertFeature(si);
//		}
		labor.constructClasificador();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearFertilizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task