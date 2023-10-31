package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.Poligono;
import dao.config.Fertilizante;
import dao.recorrida.Recorrida;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

/**
 * Task que toma un poligono y crea un mapa de suelo con los datos pasados
 */
public class CrearSueloMapTask extends ProcessMapTask<SueloItem,Suelo> {
//	Double ppmP = new Double(0);
//	Double ppmN = new Double(0);
//	Double pMO = new Double(0);
//	Double densidad = new Double(0);
	Poligono poli=null;
	Recorrida rec=null;

	public CrearSueloMapTask(Suelo labor,Poligono _poli,Double _amount, Double _ppmN, Double _pMO,Double _densidad){
		super(labor);
		
//		ppmP=_amount;
//		ppmN=_ppmN;
//		pMO=_pMO;
//		densidad=_densidad;
		poli=_poli;

		labor.setNombre(poli.getNombre());

	}

	public CrearSueloMapTask(Suelo labor,Poligono _poli,Recorrida recorrida){
		super(labor);
	
		poli=_poli;
		rec=recorrida;
		
		labor.setNombre(poli.getNombre());

	}
	
	public void doProcess() throws IOException {
		labor.setContorno(poli);
		rec.getMuestras().stream().forEach(m->{
			Map<String, Number> props = m.getProps();
			SueloItem si = new SueloItem();
			Number ppmP = props.get(SueloItem.PPM_FOSFORO);
			Number ppmN = props.get(SueloItem.PPM_N);
			Number pcMO = props.get(SueloItem.PC_MO);
			
			Number densidad = props.get(SueloItem.DENSIDAD);
			Number elevacion = props.get(SueloItem.ELEVACION);
			
			si.setDensAp(densidad.doubleValue());
			si.setPpmP(ppmP.doubleValue());
			si.setPpmNO3(ppmN.doubleValue());
			si.setPorcMO(pcMO.doubleValue());
			si.setElevacion(elevacion.doubleValue());		

			labor.setPropiedadesLabor(si);

			si.setGeometry(poli.toGeometry());
			
			labor.insertFeature(si);
		});

				
		labor.constructClasificador();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);

	}

	
	public static String buildTooltipText(Suelo s, SueloItem si,double area) {
		//DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$
		NumberFormat df = Messages.getNumberFormat();//new DecimalFormat("#,###.##");//$NON-NLS-2$

		StringBuilder sb = new StringBuilder();
		//Fosforo
		//sb.append(Messages.getString("OpenSoilMapTask.1")+"\n ");//"Fosforo: "
		sb.append(df.format(si.getPpmP()));
		sb.append(" " + Messages.getString("OpenSoilMapTask.2") + " 0-20cm \n "); //ppm
		//Nitrogeno
		//sb.append(Messages.getString("OpenSoilMapTask.3")+"\n ");//"Nitrogeno: "
		sb.append(df.format(si.getPpmNO3()));
		sb.append(" " + Messages.getString("OpenSoilMapTask.4") + " 0-60cm \n");//Ppm NO3
		
		sb.append(df.format(Suelo.getKgNHa(si)));
		//sb.append(df.format(Suelo.ppmToKg(si.getDensAp(),si.getPpmNO3(),0.6)*Fertilizante.porcN_NO3));
		sb.append(" kgN/Ha 0-60cm \n");
		
		sb.append(df.format(si.getPorcMO()));
		sb.append("% " + Messages.getString("JFXMain.236") + " 0-20cm \n ");//JFXMain.236=%M0
		
		sb.append(df.format(si.getDensAp()));
		sb.append(" " + "dens" + " kg/m3 \n "); //kg/m3
		sb.append(Messages.getString("ProcessHarvestMapTask.25")
				+df.format(si.getElevacion() ) 
				+ Messages.getString("ProcessHarvestMapTask.26")); 
		if(area<1){
			sb.append( Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			sb.append( Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area ) + "Has\n");
		}
		
		String tooltipText = sb.toString();
		return tooltipText;
	}

	@Override
	public  ExtrudedPolygon  getPathTooltip( Geometry poly,SueloItem si,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
	//	DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$
		
//		CrearSueloMapTask.fosforo=Fosforo
//		CrearSueloMapTask.nitrogeno=Nitrogeno
//		CrearSueloMapTask.sup=Sup
		
		String tooltipText = buildTooltipText(this.labor,si,area);
		
//		String tooltipText = new String(
//				Messages.getString("CrearSueloMapTask.fosforo")+": " +df.format(si.getPpmP()) +Messages.getString("OpenSoilMapTask.2")
//				+Messages.getString("CrearSueloMapTask.nitrogeno")+": "+ df.format(si.getPpmN()) +Messages.getString("OpenSoilMapTask.2")
//				);

//		if(area<1){
//			tooltipText=tooltipText.concat( Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
//		} else {
//			tooltipText=tooltipText.concat(Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area ) + "Has\n");
//		}

		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task