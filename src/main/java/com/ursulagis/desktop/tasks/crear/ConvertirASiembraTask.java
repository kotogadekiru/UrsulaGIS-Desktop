package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;


/**
 * task que genera una siembra con dosis fija a partir de un poligono
 * @author quero
 *
 */
public class ConvertirASiembraTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	Map<String,Double[]> plantasM2ObjetivoMap = null;//0.0;
	CosechaLabor cosecha=null;

	public ConvertirASiembraTask(CosechaLabor _cosecha,SiembraLabor labor,Map<String,Double[]> valores){
		super(labor);
		plantasM2ObjetivoMap=valores;
		cosecha=_cosecha;

	}

	public void doProcess() throws IOException {
		//labor.setContorno(cosecha.getContorno());
		Semilla semilla = labor.getSemilla();
		System.out.println("semilla es "+semilla);
		double entresurco = labor.getEntreSurco();
		double pmil = semilla.getPesoDeMil();
		double pg = semilla.getPG();
		double metrosLinealesHa = ProyectionConstants.METROS2_POR_HA/entresurco;//23809 a 0.42
		//System.out.println("metrosLinealesHa "+metrosLinealesHa);//metrosLinealesHa 52631.57894736842 ok!
		//double semillasHa = ProyectionConstants.METROS2_POR_HA*plantasM2Objetivo/pg;// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas


		//System.out.println("semillasMetroLineal "+semillasMetroLineal);//semillasMetroLineal 38.0 ok!
		//List<CosechaItem> cItems = new ArrayList<CosechaItem>();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
		Clasificador cl = cosecha.getClasificador();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);			
			String nombre = cl.getLetraCat(cl.getCategoryFor(ci.getRindeTnHa()));
			double plantasM2Objetivo = plantasM2ObjetivoMap.get(nombre)[0];
			double fertL = plantasM2ObjetivoMap.get(nombre)[1];
			double fertC = plantasM2ObjetivoMap.get(nombre)[2];
			double semillasHa = ProyectionConstants.METROS2_POR_HA*plantasM2Objetivo/pg;// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas
			double semillasMetroLineal = semillasHa/metrosLinealesHa;//si es trigo va en plantas /m2 si es maiz o soja va en miles de plantas por ha

			SiembraItem si = new SiembraItem();

			si.setDosisHa(semillasHa*pmil/(1000*1000));//1000semillas*1000gramos para pasar a kg/ha
			si.setDosisFertLinea(fertL);
			si.setDosisFertCostado(fertC);
			si.setDosisML(semillasMetroLineal);
			//dosis sembradora va en semillas cada 10mts
			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg

			labor.setPropiedadesLabor(si);

			si.setGeometry(ci.getGeometry());			
			si.setId(labor.getNextID());
			si.setElevacion(10.0);
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


	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = ConvertirASiembraTask.buildTooltipText(siembraFeature, area,labor);
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	
	}
	
	public static String buildTooltipText(SiembraItem siembraFeature, double area) {
		NumberFormat df = Messages.getNumberFormat();

		//densidad seeds/metro lineal
		String tooltipText = new String(Messages.getString("ProcessSiembraMapTask.1")+ df.format(siembraFeature.getDosisML()) + Messages.getString("ProcessSiembraMapTask.2")); //$NON-NLS-1$ //$NON-NLS-2$

		//kg semillas por ha
		tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.3") + df.format(siembraFeature.getDosisHa()) + Messages.getString("ProcessSiembraMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
		//fert l y c		
		tooltipText=tooltipText.concat( Messages.getString("JFXMain.FertL") +": "+ df.format(siembraFeature.getDosisFertLinea()) + Messages.getString("ProcessSiembraMapTask.6")		); //$NON-NLS-1$ //$NON-NLS-2$
		tooltipText=tooltipText.concat( Messages.getString("JFXMain.FertC") +": "+ df.format(siembraFeature.getDosisFertCostado()) + Messages.getString("ProcessSiembraMapTask.6")		); //$NON-NLS-1$ //$NON-NLS-2$
		//fert costo
		tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.7") + df.format(siembraFeature.getImporteHa()) + Messages.getString("ProcessSiembraMapTask.8")		); //$NON-NLS-1$ //$NON-NLS-2$

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.9")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessSiembraMapTask.10")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.11")+df.format(area ) + Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return tooltipText;
	}

	public static String buildTooltipText(SiembraItem siembraFeature, double area, SiembraLabor labor) {
		NumberFormat df = Messages.getNumberFormat();

		//densidad seeds/metro lineal
		String tooltipText = new String(Messages.getString("ProcessSiembraMapTask.1")+ df.format(siembraFeature.getDosisML()) + Messages.getString("ProcessSiembraMapTask.2")); //$NON-NLS-1$ //$NON-NLS-2$

		if(labor.getEntreSurco()>0) {
			Double seedsSup= siembraFeature.getDosisML()/labor.getEntreSurco();
			if(seedsSup<100) {//plantas por ha
				int digits =df.getMaximumFractionDigits();
				df.setMaximumFractionDigits(2);
				tooltipText=tooltipText.concat(df.format(seedsSup*ProyectionConstants.METROS2_POR_HA) 
						+ " s/"+ Messages.getString("ProcessSiembraMapTask.12")); // "Has\n"
				df.setMaximumFractionDigits(digits);

			}else {
				tooltipText=tooltipText.concat(df.format(seedsSup) 
						+ " s/"+Messages.getString("ProcessSiembraMapTask.10")); // "s/m2"
			}
		}else {
			System.out.println("Enrtesurco es cero o menos "+labor.getEntreSurco());
		}
		//kg semillas por ha
		tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.3") + df.format(siembraFeature.getDosisHa()) + Messages.getString("ProcessSiembraMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
		//fert l y c		
		tooltipText=tooltipText.concat( Messages.getString("JFXMain.FertL") +": "+ df.format(siembraFeature.getDosisFertLinea()) + Messages.getString("ProcessSiembraMapTask.6")		); //$NON-NLS-1$ //$NON-NLS-2$
		tooltipText=tooltipText.concat( Messages.getString("JFXMain.FertC") +": "+ df.format(siembraFeature.getDosisFertCostado()) + Messages.getString("ProcessSiembraMapTask.6")		); //$NON-NLS-1$ //$NON-NLS-2$
		//fert costo
		tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.7") + df.format(siembraFeature.getImporteHa()) + Messages.getString("ProcessSiembraMapTask.8")		); //$NON-NLS-1$ //$NON-NLS-2$

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.9")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessSiembraMapTask.10")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.11")+df.format(area ) + Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return tooltipText;
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task