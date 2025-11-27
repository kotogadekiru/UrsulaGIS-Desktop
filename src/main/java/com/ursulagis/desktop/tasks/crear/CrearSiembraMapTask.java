package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;


/**
 * task que genera una siembra con dosis fija a partir de un poligono
 * @author quero
 *
 */
public class CrearSiembraMapTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	Double plantasM2Objetivo = 0.0;
	List<Poligono> polis=null;

	public CrearSiembraMapTask(SiembraLabor labor,List<Poligono> _poli,Double _amount){
		super(labor);		
		plantasM2Objetivo=_amount;
		polis=_poli;

	}

	public void doProcess() throws IOException {
		//labor.setContorno(GeometryHelper.unirPoligonos(polis));
		Semilla semilla = labor.getSemilla();
		System.out.println("semilla es "+semilla);
		double entresurco = labor.getEntreSurco();
		double pmil = semilla.getPesoDeMil();
		double pg = semilla.getPG();
		double metrosLinealesHa = ProyectionConstants.METROS2_POR_HA/entresurco;//23809 a 0.42
		//System.out.println("metrosLinealesHa "+metrosLinealesHa);//metrosLinealesHa 52631.57894736842 ok!
		double semillasHa = ProyectionConstants.METROS2_POR_HA*plantasM2Objetivo/pg;// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas

		double semillasMetroLineal = semillasHa/metrosLinealesHa;//si es trigo va en plantas /m2 si es maiz o soja va en miles de plantas por ha
		//System.out.println("semillasMetroLineal "+semillasMetroLineal);//semillasMetroLineal 38.0 ok!

		for(Poligono pol : this.polis) {
			SiembraItem si = new SiembraItem();
					
			si.setDosisHa(semillasHa*pmil/(1000*1000));//1000semillas*1000gramos para pasar a kg/ha

			si.setDosisML(semillasMetroLineal);
			//dosis sembradora va en semillas cada 10mts
			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg
			
			labor.setPropiedadesLabor(si);
			Geometry g = GeometryHelper.simplificarContorno(pol.toGeometry());
			si.setGeometry(g);
			si.setId(labor.getNextID());
			si.setElevacion(10.0);
			labor.insertFeature(si);
		}
		labor.constructClasificador();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}


	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = ConvertirASiembraTask.buildTooltipText(siembraFeature, area,labor);
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	
	}
	
	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task