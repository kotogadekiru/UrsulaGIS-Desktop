package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import com.google.gson.Gson;
import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.recorrida.Muestra;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * Task que toma una cosecha y una recorrida y crea un mapa de suelo
 * el resultado tiene los ambientes de la cosecha y los datos de la recorrida
 */
public class ConvertirASueloTask extends ProcessMapTask<SueloItem,Suelo > {
	CosechaLabor cosecha=null;
	Recorrida recorrida=null; 

	public ConvertirASueloTask(CosechaLabor cosecha, Recorrida recorrida) {
		this.cosecha=cosecha;
		this.recorrida=recorrida;
		this.labor=new Suelo();
		labor.setNombre(cosecha.getNombre());
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
	}

	@Override
	protected void doProcess() throws IOException {
	//	labor.setContorno(cosecha.getContorno());
		//crear un map de muestra segun su nombre
		Map<String, List<Muestra>> mMap = recorrida.getMuestras().stream().collect(Collectors.groupingBy(Muestra::getNombre));

		List<CosechaItem> cItems = new ArrayList<CosechaItem>();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
		featureCount=cosecha.outCollection.size();
		updateProgress(1, featureCount);
		this.featureNumber=0;
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);
			cItems.add(ci);
			updateProgress(featureNumber++, featureCount);
		}
		reader.close();
		this.featureNumber=0;
		cItems.parallelStream().forEach(ci->{			
			//XXX no tomo los puntos porque hay poligonos que no van a tener puntos
			Integer categoria = cosecha.getClasificador().getCategoryFor(ci.getAmount());			
			String catName = cosecha.getClasificador().getLetraCat(categoria);//""+Clasificador.abc[size-categoria-1];
			
			List<Muestra> muestrasK = mMap.get(catName); 
			if(muestrasK!=null && muestrasK.size()>0) {
				Muestra m = muestrasK.get(0);
				Map<String, Double> props = m.getProps();
//				String obs = m.getObservacion();

//				@SuppressWarnings("unchecked")
//				Map<String,String> map = new Gson().fromJson(obs, Map.class);	 

//				LinkedHashMap<String, Number> props = new LinkedHashMap<String,Number>();
//				for(String k : map.keySet()) {
//					Object value = map.get(k);
//					if(String.class.isAssignableFrom(value.getClass())) {				
//						Double dValue = 0.0;
//						try { dValue=Double.parseDouble((String)value);
//						}catch(Exception e) {
//							System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");}
//						props.put(k, dValue);//ojo number format exception
//					} else if(Number.class.isAssignableFrom(value.getClass())) {
//						props.put(k, (Number)value);
//					}			
//				}

				Number ppmP = props.get(SueloItem.PPM_FOSFORO);
				Number ppmN = props.get(SueloItem.PPM_N);
				Number ppmMO = props.get(SueloItem.PC_MO);
				Number ppmS = props.get(SueloItem.PPM_ASUFRE);
				Number ppmK = props.get(SueloItem.PPM_POTASIO);
				
				Number densidad = props.get(SueloItem.DENSIDAD);
				Number elevacion = props.get(SueloItem.ELEVACION);
				SueloItem si = new SueloItem();
				synchronized(labor){
					si.setId(labor.getNextID());
				}
				if(densidad!=null)si.setDensAp(densidad.doubleValue());
				if(ppmP!=null)si.setPpmP(ppmP.doubleValue());
				if(ppmN!=null)si.setPpmNO3(ppmN.doubleValue());
				if(ppmS!=null)si.setPpmS(ppmS.doubleValue());
				if(ppmK!=null)si.setPpmK(ppmK.doubleValue());
				
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
				
				if(ppmMO!=null)si.setPorcMO(ppmMO.doubleValue());
				if(elevacion!=null)si.setElevacion(elevacion.doubleValue());
				labor.setPropiedadesLabor(si);


				si.setGeometry(ci.getGeometry());

				labor.insertFeature(si);
			}
			updateProgress(featureNumber++, featureCount);
		});
		//TODO si el nombre de la categoria del cosechaitem coincide con el nombre de la muestra
		// crear un suelo item con la geometria de la cosecha y el dato de la muestra
		cosecha.getLayer().setEnabled(false);
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
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si, ExtrudedPolygon renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor, si, area);//buildTooltipText(this.labor,si,area);
//
//		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);		
//	}

	
}
