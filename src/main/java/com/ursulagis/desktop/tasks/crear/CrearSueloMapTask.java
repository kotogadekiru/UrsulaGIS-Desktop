package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Nutriente;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * Task que toma un poligono y crea un mapa de suelo con los datos pasados
 */
public class CrearSueloMapTask extends ProcessMapTask<SueloItem,Suelo> {
	//	Double ppmP = 0.0;
	//	Double ppmN = 0.0;
	//	Double pMO = 0.0;
	//	Double densidad = 0.0;
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
		//	labor.setContorno(poli);
		rec.getMuestras().stream().forEach(m->{
			Map<String, Double> props = m.getProps();
			SueloItem si = new SueloItem();
			Double ppmP = props.get(SueloItem.PPM_FOSFORO);
			Double ppmN = props.get(SueloItem.PPM_N);
			Double pcMO = props.get(SueloItem.PC_MO);
			Double ppmS = props.get(SueloItem.PPM_ASUFRE);
			Double ppmK = props.get(SueloItem.PPM_POTASIO);			
			

			Double aguaPerf = props.get(SueloItem.AGUA_PERFIL);
			Double profNapa = props.get(SueloItem.PROF_NAPA);

			Double densidad = props.get(SueloItem.DENSIDAD);
			Double elevacion = props.get(SueloItem.ELEVACION);

			//agua perfil
			//prof napa

			si.setDensAp(densidad);
			si.setPpmP(ppmP);
			si.setPpmNO3(ppmN);
			si.setPorcMO(pcMO);
			si.setPpmS(ppmS);
			si.setPpmK(ppmK);
			
			si.setPpmCa(props.get(SueloItem.Calcio));
			si.setPpmMg(props.get(SueloItem.Magnecio));
			si.setPpmB(props.get(SueloItem.Boro));
			si.setPpmCl(props.get(SueloItem.Cloro));
			si.setPpmCo(props.get(SueloItem.Cobalto));
			si.setPpmCu(props.get(SueloItem.Cobre));
			si.setPpmFe(props.get(SueloItem.Hierro));
			si.setPpmMn(props.get(SueloItem.Manganeso));
			si.setPpmMo(props.get(SueloItem.Molibdeno));
			si.setPpmZn(props.get(SueloItem.Zinc));	
			

			if(elevacion.doubleValue()>10) {
				si.setElevacion(elevacion.doubleValue());		
			}else {
				si.setElevacion(10.0);
			}

			si.setAguaPerfil(aguaPerf.doubleValue());
			si.setProfNapa(profNapa.doubleValue());

			labor.setPropiedadesLabor(si);

			si.setGeometry(poli.toGeometry());

			labor.insertFeature(si);
		});

		labor.constructClasificador();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);

	}
	
	public static String getLabelForNutriente(Nutriente n) {
		NumberFormat df = Messages.getNumberFormat();
		df.setMaximumFractionDigits(0);
		Double profundidad = n.getProfundidad()*100;				
		String prof = df.format(profundidad);
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
		
		return n.getSimbolo()+"(0-"+prof+"cm): ";
		
	}

	public static String buildTooltipText(SueloItem si,double area) {
		NumberFormat df = Messages.getNumberFormat();//new DecimalFormat("#,###.##");//$NON-NLS-2$

		StringBuilder sb = new StringBuilder();
		sb.append("______________________\n");
		//Fosforo		
		Map<SueloParametro, Nutriente> spd = Nutriente.getNutrientesDefault();
		Nutriente fosforo =spd.get(SueloParametro.Fosforo);		
		sb.append(getLabelForNutriente(fosforo)+df.format(si.getPpmP())+"ppm\n");
		//sb.append(" " + Messages.getString("OpenSoilMapTask.2") + " 0-20cm \n "); //ppm

		//Nitrogeno	//FIXME verificar si es N-NO3 o NO3			
		sb.append(getLabelForNutriente(spd.get(SueloParametro.Nitrogeno))+df.format(si.getPpmNO3())+"ppm\n");
		//sb.append("NO3(0-60): "+df.format(si.getPpmNO3())+"ppm\n");

		sb.append("           "+df.format(Suelo.getKgNHa(si))+"kgN/ha\n");// por kg/ha
		
		//POTASIO
		Double ppmK = si.getPpmK();
		if(ppmK != null && Math.abs(ppmK)>0)
		sb.append(getLabelForNutriente(spd.get(SueloParametro.Potasio))+df.format(ppmK)+"ppm\n");
		//sb.append("K(0-20): "+df.format(si.getPpmK())+"ppm\n");

		//AZUFRE
		Double ppmS = si.getPpmS();
		if(ppmS != null && Math.abs(ppmS)>0)
		sb.append(getLabelForNutriente(spd.get(SueloParametro.Azufre))+df.format(ppmS)+"ppm\n");
		//sb.append("S(0-20): "+df.format(si.getPpmS())+"ppm\n");
		//sb.append(df.format(si.getPpmS()));
		//sb.append("Ppm-S(0-20cm)\n");

		
		List<SueloParametro> microNutrientes = Nutriente.getMicroNutrientes();
		microNutrientes.forEach((sp)->{
			Nutriente n = Nutriente.getNutrientesDefault().get(sp);
			Double ppm = Suelo.getPpm(sp, si);
			if(ppm != null && Math.abs(ppm)>0) {			
				sb.append(getLabelForNutriente(n));
				sb.append(df.format(ppm)+"ppm\n");				
			}
		});
		
		//MATERIA ORGANICA
		sb.append("MO(0-20): "+df.format(si.getPorcMO())+"%\n");
	//	sb.append(df.format(si.getPorcMO()));
	//	sb.append("% " + Messages.getString("JFXMain.236") + " 0-20cm \n ");//JFXMain.236=%M0

		//DENSIDAD APARENTE
		df.setMaximumFractionDigits(0);
		sb.append("DA(0-60): " + df.format(si.getDensAp()) + "kg/m�\n");//�=alt+0179
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
		//sb.append(df.format(si.getDensAp()));
		//sb.append(" " + "dens" + " kg/m3 \n "); //kg/m3
		
		//OTROS
		//getProfNapa(),
		//getAguaPerfil(),		
		//getTextura(),
		//getPorosidad(),
		//getPorcCC()
		
		//ELEVACION
		sb.append(Messages.getString("ProcessHarvestMapTask.25")
				+df.format(si.getElevacion() ) 
				+ "m\n"); 
		
		//SUPERFICIE
		if(area<1){
			sb.append( Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m�\n");
		} else {
			sb.append( Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area ) + "Has\n");
		}

		String tooltipText = sb.toString();
		//System.out.println(tooltipText);
		return tooltipText;
	}



	public static String buildTooltipText(Suelo s, SueloItem si,double area) {		
		return buildTooltipText(si,area);
	}

//	@Override
//	public  ExtrudedPolygon  getPathTooltip( Geometry poly,SueloItem si,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		//	DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$
//
//		//		CrearSueloMapTask.fosforo=Fosforo
//		//		CrearSueloMapTask.nitrogeno=Nitrogeno
//		//		CrearSueloMapTask.sup=Sup
//
//		String tooltipText = buildTooltipText(si,area);
//
//		//		String tooltipText = new String(
//		//				Messages.getString("CrearSueloMapTask.fosforo")+": " +df.format(si.getPpmP()) +Messages.getString("OpenSoilMapTask.2")
//		//				+Messages.getString("CrearSueloMapTask.nitrogeno")+": "+ df.format(si.getPpmN()) +Messages.getString("OpenSoilMapTask.2")
//		//				);
//
//		//		if(area<1){
//		//			tooltipText=tooltipText.concat( Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
//		//		} else {
//		//			tooltipText=tooltipText.concat(Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area ) + "Has\n");
//		//		}
//
//		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task