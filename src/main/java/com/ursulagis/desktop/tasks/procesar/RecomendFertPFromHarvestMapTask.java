package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.geotools.api.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearFertilizacionMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class RecomendFertPFromHarvestMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	private CosechaLabor cosecha;
	private Double minFert=null;
	private Double maxFert=null;

	public RecomendFertPFromHarvestMapTask(FertilizacionLabor labor,CosechaLabor c) {
		super(labor);
		this.cosecha =c;
	}
	public void doProcess() throws IOException {

		featureCount=cosecha.outCollection.size();
	//	List<FertilizacionItem> itemsToShow = new ArrayList<FertilizacionItem>();
		Cultivo cultivo = cosecha.getCultivo();
		Fertilizante fert = this.labor.fertilizante;


		List<CosechaItem> cItems = new ArrayList<CosechaItem>();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);
			cItems.add(ci);
		}
		reader.close();

		cItems.parallelStream().forEach(
				cItem->{

					FertilizacionItem fi =null;
					synchronized(labor){
						fi= new FertilizacionItem();					
						fi.setId(labor.getNextID());
						labor.setPropiedadesLabor(fi);
					}

					fi.setGeometry(cItem.getGeometry());
					double extraccionP = cItem.getRindeTnHa()*cultivo.getExtP();
					double reposicionP = extraccionP/(fert.getPorcP()/100);
					if(this.minFert!=null&&this.minFert>reposicionP) {
						reposicionP=minFert;
					}
					if(this.maxFert!=null&&this.maxFert<reposicionP) {
						reposicionP=maxFert;
					}
					fi.setDosistHa(reposicionP);
					fi.setElevacion(10d);
					labor.setPropiedadesLabor(fi);
					//segun el cultivo de la cosecha


					labor.insertFeature(fi);
			//		itemsToShow.add(fi);
					featureNumber++;
					updateProgress(featureNumber, featureCount);
				});

		labor.constructClasificador();
		runLater(getItemsList());
		updateProgress(0, featureCount);	
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearFertilizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 1000;
	}
	public void setMinFert(Double _minFert) {
		this.minFert=_minFert;

	}
	public void setMaxFert(Double _maxFert) {
		this.maxFert=_maxFert;

	}
}
