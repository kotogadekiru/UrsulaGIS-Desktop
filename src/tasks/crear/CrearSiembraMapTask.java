package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.Poligono;
import dao.config.Semilla;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;


/**
 * task que genera una siembra con dosis fija a partir de un poligono
 * @author quero
 *
 */
public class CrearSiembraMapTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	Double plantasM2Objetivo = new Double(0);
	Poligono poli=null;

	public CrearSiembraMapTask(SiembraLabor labor,Poligono _poli,Double _amount){
		super(labor);
		plantasM2Objetivo=_amount;
		poli=_poli;

	}

	public void doProcess() throws IOException {
		SiembraItem ci = new SiembraItem();
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
	
		ci.setDosisHa(semillasHa*pmil/(1000*1000));//1000semillas*1000gramos para pasar a kg/ha
		
		ci.setDosisML(semillasMetroLineal);
		//dosis sembradora va en semillas cada 10mts
		//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg

		labor.setPropiedadesLabor(ci);
		GeometryFactory fact = new GeometryFactory();
		List<? extends Position> positions = poli.getPositions();
		Coordinate[] coordinates = new Coordinate[positions.size()];
		for(int i=0;i<positions.size();i++){
			Position p = positions.get(i);	
			Coordinate c = new Coordinate(p.getLongitude().getDegrees(),p.getLatitude().getDegrees(),p.getElevation());
			
			coordinates[i]=c;
		}
	//	if(coordinates[0]!=coordinates[coordinates.length-1]){
			coordinates[coordinates.length-1]=coordinates[0];//en caso de que la geometria no este cerrada
	//	}
		Polygon poly = fact.createPolygon(coordinates);	

		ci.setGeometry(poly);
		
		labor.insertFeature(ci);
				
		labor.constructClasificador();

		List<SiembraItem> itemsToShow = new ArrayList<SiembraItem>();
		itemsToShow.add(ci);
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}


	@Override
	public  ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature) {
	//	Path path = getPathFromGeom(poly,siembraFeature);		
		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//	DecimalFormat df = new DecimalFormat("0.00"); 
//		
//		String tooltipText = new String("Densidad: "+ df.format(siembraFeature.getDosisML()) + " Sem/m\n");
//		tooltipText=tooltipText.concat("Kg: " + df.format(siembraFeature.getDosisHa()) + " kg/Ha\n");
//		
//		tooltipText=tooltipText.concat( "Fert: " + df.format(siembraFeature.getDosisFertLinea()) + " Kg/Ha\n"		);
//		tooltipText=tooltipText.concat( "Costo: " + df.format(siembraFeature.getImporteHa()) + " U$S/Ha\n"		);
//		
//		
////		String tooltipText = new String(
////				"Densidad: "+ df.format(siembraFeature.getDosisML()) + " Sem/m\n"
////				+"Kg: " + df.format(siembraFeature.getDosisHa()) + " kg/Ha\n"				
////				+"Costo: " + df.format(siembraFeature.getImporteHa()) + " U$S/Ha\n"				
////			//	+"Sup: " +  df.format(area*ProyectionConstants.METROS2_POR_HA) + " m2\n"
////		//		+"feature: " + featureNumber						
////		);
////		
//		if(area<1){
//			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
//		} else {
//			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
//		}
		DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$
		String tooltipText = new String(Messages.getString("ProcessSiembraMapTask.1")+ df.format(siembraFeature.getDosisML()) + Messages.getString("ProcessSiembraMapTask.2")); //$NON-NLS-1$ //$NON-NLS-2$
		tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.3") + df.format(siembraFeature.getDosisHa()) + Messages.getString("ProcessSiembraMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
		
		tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.5") + df.format(siembraFeature.getDosisFertLinea()) + Messages.getString("ProcessSiembraMapTask.6")		); //$NON-NLS-1$ //$NON-NLS-2$
		tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.7") + df.format(siembraFeature.getImporteHa()) + Messages.getString("ProcessSiembraMapTask.8")		); //$NON-NLS-1$ //$NON-NLS-2$
		
//		String tooltipText = new String(
//				"Densidad: "+ df.format(siembraFeature.getDosisHa()) + " Kg/Ha\n"								
//				);
//		tooltipText=tooltipText.concat( "Fert: " + df.format(siembraFeature.getDosisFertLinea()) + " Kg/Ha\n"		);
//		tooltipText=tooltipText.concat( "Costo: " + df.format(siembraFeature.getImporteHa()) + " U$S/Ha\n"		);

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.9")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessSiembraMapTask.10")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.11")+df.format(area ) + Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText);
	//	return ret;		
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task