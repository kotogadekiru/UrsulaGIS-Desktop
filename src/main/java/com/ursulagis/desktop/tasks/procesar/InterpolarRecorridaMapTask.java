package com.ursulagis.desktop.tasks.procesar;


import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.geotools.geometry.jts.ReferencedEnvelope;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;
import com.ursulagis.desktop.dao.recorrida.Muestra;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import javafx.beans.property.SimpleStringProperty;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearSueloMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class InterpolarRecorridaMapTask extends ProcessMapTask<SueloItem,Suelo> {

	private List<Poligono> contornos=null;
	private Double anchoGrilla;
	private Recorrida recorrida;
	private Double minDist=null;


	public InterpolarRecorridaMapTask(Recorrida r,List<Poligono> contornos) {
		this.contornos = contornos;
		this.recorrida =r;
		Suelo suelo = new Suelo();
		suelo.colDensidadProperty=new SimpleStringProperty("Densidad");
		suelo.setLayer(new LaborLayer());
		
		StringBuilder sb = new StringBuilder();
		sb.append("Interpolado de "+r.getNombre()+" ");
		contornos.forEach((c)->sb.append(c.getNombre()+" "));
		suelo.setNombre(sb.toString());
		suelo.setLayer(new LaborLayer());
		super.labor=suelo;
		
		String anchoGrillaString = JFXMain.config.getPropertyOrDefault(CosechaConfig.ANCHO_GRILLA_KEY,Messages.getString("JFXMain.288"));
		try {
			this.anchoGrilla = Messages.getNumberFormat().parse(anchoGrillaString).doubleValue();
			minDist= ProyectionConstants.metersToLongLat(anchoGrilla);
		} catch (ParseException e) {
			this.anchoGrilla =10.0;
		}
		
	}
	@Override
	protected void doProcess() throws IOException {
		//unir poligonos
		Poligono contorno = GeometryHelper.unirPoligonos(contornos);
		Geometry contornoG = contorno.toGeometry();
		Envelope env= contornoG.getEnvelopeInternal();
		ReferencedEnvelope contornoEnvelope =new ReferencedEnvelope() ;
		contornoEnvelope.expandToInclude(env);
		double anchoMaxLongLat = Math.hypot(env.getHeight(), env.getWidth());
		//System.out.println("ancho max = "+Messages.getNumberFormat().format(anchoMaxLongLat));
		//crear una grilla
		List<Polygon> grilla = GrillarCosechasMapTask.construirGrilla(contornoEnvelope, this.anchoGrilla);
		this.featureCount=grilla.size();
		//filtrar los elemetos de la grilla que no esten en los contornos iniciales
		Stream<Polygon> grillaFiltradaStream = grilla.parallelStream().filter(p->contornoG.intersects(p));
		//por cada elemento de la grilla crear un nuevo sueloItem
		grillaFiltradaStream.forEach(p->{
			
			Map<SueloParametro,Double> sueloProps =new HashMap<SueloParametro,Double>();
			Double sumaPesos = 0.0;
			for(Muestra m :this.recorrida.getMuestras()) {
				org.locationtech.jts.geom.Point pm =GeometryHelper.constructPoint(m.getPosition());
				Double dist = p.distance(pm);//si la muestra esta dentro del poligono? dist==0		
				
				if(dist<this.minDist) {
					dist=this.minDist;					 
				}				
				Double peso = Math.pow(anchoMaxLongLat/dist,2);
				//System.out.println("peso = "+Messages.getNumberFormat().format(peso));
				sumaPesos+=peso;
				Map<String,Double> props = m.getProps();
				for(String k:props.keySet()) {
					SueloParametro spk = Suelo.getSueloParametro(k);//convierto del muestreo al parametro de suelo
					if(spk!=null && sueloProps.containsKey(spk)) {
						sueloProps.put(spk, sueloProps.get(spk)+props.get(k) * peso);
					}else {
						sueloProps.put(spk, props.get(k) * peso);
					}					
				}				
			}//termine de sumar todas las muestras
			
			for(SueloParametro k:sueloProps.keySet()) {		
					sueloProps.put(k, sueloProps.get(k) / sumaPesos);
									
			}	//sueloProps contiene los promedios ponderados por la distancia a la muestra			

			SueloItem sueloItem = new SueloItem();
			synchronized(labor){					
				sueloItem.setId(labor.getNextID());
			}
			sueloItem.setElevacion(sueloProps.get(SueloParametro.Elevacion));//10.0);//para que aparezca en el mapa
			sueloItem.setDensAp(sueloProps.get(SueloParametro.Densidad));
			
			sueloItem.setPpmNO3(sueloProps.get(SueloParametro.Nitrogeno));
			sueloItem.setPpmP(sueloProps.get(SueloParametro.Fosforo));
			sueloItem.setPpmK(sueloProps.get(SueloParametro.Potasio));
			sueloItem.setPpmS(sueloProps.get(SueloParametro.Azufre));
			//TODO agregar micronutrientes
			sueloItem.setPorcMO(sueloProps.get(SueloParametro.MateriaOrganica));	
			Geometry g=p;
			synchronized(contornoG) {
			if(!contornoG.covers(g)) {
				g=GeometryHelper.getIntersection(p, contornoG);
			}
			}
			if(g==null)return;//continuar con el siguiente poligono este no tiene geometria de interseccion
			for(Coordinate coord:g.getCoordinates()) {
				coord.z=sueloItem.getElevacion();
			}
			sueloItem.setGeometry(g);			
			labor.insertFeature(sueloItem);
			updateProgress(featureNumber++, featureCount);
		});
		
		
		
		
//		if(labor.outCollection!=null)labor.outCollection.clear();
//		labor.treeCache=null;
//		labor.treeCacheEnvelope=null;
//		resumidas.stream().forEach(renta->
//			labor.insertFeature(renta)
//		);

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);		
	}

	@Override
	protected int getAmountMin() {	
		return 0;
	}

	@Override
	protected int gerAmountMax() {	
		return 0;
	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		
//		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor,si,area);
//	
//		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);
//	}

}
