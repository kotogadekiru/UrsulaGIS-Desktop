package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ConvertirSueloACosechaTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	Double mmLluviaEstimados=0.0;
	Suelo suelo=null;

	public ConvertirSueloACosechaTask(CosechaLabor cosechaLabor,Suelo _suelo, Double _mm){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		mmLluviaEstimados = _mm;
		suelo=_suelo;
	}

	public void doProcess() throws IOException {		
		//labor.setContorno(suelo.getContorno());
		featureNumber = 0;
		featureCount = this.suelo.outCollection.getCount();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.suelo.outCollection.reader();
		while(reader.hasNext()){
			SimpleFeature f = reader.next();
			SueloItem si = suelo.constructFeatureContainerStandar(f,true);
			CosechaItem ci = estimarCosecha(si);
			ci.setId(si.getId());
			ci.setGeometry(si.getGeometry());
			ci.setElevacion(10.0);
			System.out.println("observaciones en doProcess "+ci.getObservaciones());
			labor.insertFeature(ci);
			//SimpleFeature nf=ci.getFeature(labor.featureBuilder);

			//System.out.println("insertando "+nf);
			//boolean ret = labor.outCollection.add(nf);
			featureNumber++;
			updateProgress(featureNumber, featureCount);
			//if(!ret){
			//	System.out.println("no se pudo agregar la feature "+f);
			//}
		}

		reader.close();
		labor.constructClasificador();
		System.out.println("antes de run later");
		runLater(this.getItemsList());
		System.out.println("despues de run later");
		updateProgress(0, featureCount);
	}
	
	private CosechaItem estimarCosecha(SueloItem si) {
		CosechaItem ci = new CosechaItem();
		Cultivo cultivo = labor.getCultivo();
		Double mmDispo = si.getAguaPerfil()+this.mmLluviaEstimados;
		Double mmTn = cultivo.getAbsAgua();
		Double kgAgua = mmTn!=0?mmDispo/mmTn:0;
		//TODO si hay demasiada agua bajar el rinde; mas de 1800mm/2000mm
		Double encharcamiento = Math.min(2000, mmDispo)/2000;
		
		Double rindeAgua=(encharcamiento>0.5?(1-encharcamiento):1)*kgAgua;
		//System.out.println("mmDispo="+mmDispo+" => rindeAgua= "+rindeAgua);
	
		String observaciones ="agua";
	
		Double kgP=Suelo.getKgPHa(si);
		Double rindeP = cultivo.getAbsP()!=0?kgP/cultivo.getAbsP():0;
		//System.out.println("kgP="+kgP+" => rindeP= "+rindeP);
		if(rindeAgua < rindeP) {//factor limitante es fosforo
			observaciones="fosforo";
		}
		
		Double kgN = Suelo.getKgNHa(si) 
				+(cultivo.isEstival()?2/3:1/3) * suelo.getKgNOrganicoHa(si);
		Double rindeN = cultivo.getAbsN()!=0?kgN/cultivo.getAbsN():0;
		//System.out.println("kgN="+kgN+" => rindeN: "+rindeN);
		
		
		List<Double> rindes = Arrays.asList(rindeAgua,rindeP,rindeN);
		Double rindeLimitante = rindes.stream().min((d1,d2)->Double.compare(d1, d2)).get();
//		Double rindeLimitante = Math.min(
//				Math.min(rindeAgua,rindeP),
//				rindeN);
		ci.setRindeTnHa(rindeLimitante);
		ci.setObservaciones("Rinde no limitado");
		if(rindeLimitante == rindeAgua) {
			if(rindeN<rindeP) {
				Double difRinde = rindeP-rindeAgua;
				Double mmFaltan =difRinde*cultivo.getAbsAgua();
				ci.setObservaciones("Rinde limitado por agua (faltan"
						+PropertyHelper.formatDouble(mmFaltan)
						+"mm), despues por N y finalmente por P");
			}else {
				Double difRinde = rindeN-rindeAgua;
				Double mmFaltan =difRinde*cultivo.getAbsAgua();
				ci.setObservaciones("Rinde limitado por agua (faltan"
						+PropertyHelper.formatDouble(mmFaltan)
						+"mm), despues por P y finalmente por N");
			}
			if(encharcamiento>0.5) {
				String obs = ci.getObservaciones()+" encharcamiento "+PropertyHelper.formatDouble(encharcamiento);
				ci.setObservaciones(obs);
			}
		} else if(rindeLimitante == rindeN) {
			if(rindeAgua<rindeP) {
				Double difRinde = rindeAgua-rindeN;
				Double kgNFaltan = difRinde*cultivo.getAbsN();
				ci.setObservaciones("Rinde limitado por N (faltan "
									+PropertyHelper.formatDouble(kgNFaltan)
									+" kgN), despues por Agua y finalmente por kgP");
			}else {
				Double difRinde = rindeP-rindeN;
				Double kgNFaltan = difRinde*cultivo.getAbsN();
				ci.setObservaciones("Rinde limitado por N (faltan "
				+PropertyHelper.formatDouble(kgNFaltan)
				+"kgN), despues por P y finalmente por Agua");
			}
		}else if(rindeLimitante == rindeP) {
			if(rindeAgua<rindeN) {
				Double difRinde = rindeAgua-rindeP;
				Double kgPFaltan = difRinde*cultivo.getAbsP();
				ci.setObservaciones("Rinde limitado por P faltan "+PropertyHelper.formatDouble(kgPFaltan)+" kgP despues por Agua y finalmente por N");
			}else {
				Double difRinde = rindeN-rindeP;
				Double kgPFaltan = difRinde*cultivo.getAbsP();
				ci.setObservaciones("Rinde limitado por P (faltan "+PropertyHelper.formatDouble(kgPFaltan)+" kgP) despues por N y finalmente por Agua");
			}
		}

		return ci;		
	}
		
//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,"tooltip de convertir suelo a cosecha",renderablePolygon);
//
//	}
	
//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		tooltipText+="\n Observaciones: "+cosechaItem.getObservaciones();
//		//System.out.println("observaciones en getPathTooltip de convertir A suelo es "+tooltipText);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task