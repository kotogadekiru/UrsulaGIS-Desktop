package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.DoubleStream;

import org.geotools.data.FeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.Labor;
import dao.LaborItem;
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.config.Nutriente;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.suelo.Suelo;
import dao.suelo.Suelo.SueloParametro;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import gui.nww.LaborLayer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import tasks.ProcessMapTask;
import tasks.crear.CrearSueloMapTask;
import utils.GeometryHelper;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class ProcessBalanceDeNutrientes2 extends ProcessMapTask<SueloItem,Suelo> {
	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	private int featureCount=0;
	private int featureNumber=0;

	private List<Suelo> suelos;
	private List<CosechaLabor> cosechas;
	private List<FertilizacionLabor> fertilizaciones;

	public ProcessBalanceDeNutrientes2(List<Suelo> suelos,List<CosechaLabor> cosechas,List<FertilizacionLabor> fertilizaciones) {
		this.suelos=suelos;
		this.fertilizaciones=fertilizaciones;
		this.cosechas =cosechas;

		Suelo suelo = new Suelo();
		suelo.colDensidadProperty=new SimpleStringProperty("Densidad");
		suelo.setLayer(new LaborLayer());

		StringBuilder sb = new StringBuilder();
		sb.append("Balance de Nutrientes ");
		cosechas.forEach((c)->sb.append(c.getNombre()+" "));
		suelo.setNombre(sb.toString());
		suelo.setLayer(new LaborLayer());
		super.labor=suelo;

	}

	public void doProcess() throws IOException {
		// establecer los bounds de los inputs
		// crear una grilla cubriendo los bounds
		// para cada item de la grilla calcular el balance de nutrientes y producir el nuevo suelo
		// crear el clasificador
		// crear los paths
		// devolver el nuevo suelo
		featureNumber = 0;
		updateProgress(featureNumber, featureCount);
		double ancho = labor.getConfigLabor().getAnchoGrilla();

		List<Labor<?>> labores = new LinkedList<Labor<?>>();
		labores.addAll(cosechas);
		labores.addAll(fertilizaciones);
		labores.addAll(suelos);
		ReferencedEnvelope unionEnvelope = labores.parallelStream().collect(
				()->new ReferencedEnvelope(),
				(env, labor) ->{		
					ReferencedEnvelope b = labor.outCollection.getBounds();
					//System.out.println("agregando al envelope "+b );
					env.expandToInclude(b);
				},	(env1, env2) -> env1.expandToInclude(env2));

		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);
		//System.out.println("construi una grilla con "+grilla.size()+" elementos");//construi una grilla con 5016 elementos
		//obtener una lista con todas las geometrias de las labores
		List<Geometry> geometriasActivas = labores.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(activas, labor) ->{		
					activas.add(labor.getContorno().toGeometry());
				},	(env1, env2) -> env1.addAll(env2));		

		//unir las geometrias de todas las labores para obtener un poligono de contorno
		Geometry cover = GeometryHelper.unirGeometrias(geometriasActivas);
		//System.out.println("el area del cover es: "+GeometryHelper.getHas(cover));//el area del cover es: 3.114509320893096E-12
		//intersectar la grilla con el contorno
		List<Geometry> grillaCover = grilla.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(intersecciones, poly) ->{					
					Geometry intersection = GeometryHelper.getIntersection(poly, cover); 
					if(intersection!=null) {
						intersecciones.add(intersection);
					}
				},	(env1, env2) -> env1.addAll(env2));


		featureCount = grillaCover.size();
		//System.out.println("grilla cover size "+featureCount);

		grillaCover.parallelStream().forEach(g->{		
			SueloItem sueloItem = createSueloForPoly(g);		
			if(sueloItem!=null){
				labor.insertFeature(sueloItem);
			}
			featureNumber++;
			updateProgress(featureNumber, featureCount);			
		});

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	private SueloItem createSueloForPoly(Geometry geomQuery) {	

		Double areaQuery =GeometryHelper.getHas(geomQuery);//ProyectionConstants.A_HAS( geomQuery.getArea());
		if(!(areaQuery>0)) {
			//System.out.println("salteando la geometria "+geomQuery+" porque el area no es >0");
			return null;//si el area no es mayor a cero paso a la siguiente
		}

		//System.out.println("obteniendo los nutrientes de los suelos");
		Map<SueloParametro,Double> kgNutrienteSuelos = getKgNutrientesSuelos(geomQuery);
		Double sumaAreasSuelos = kgNutrienteSuelos.get(SueloParametro.Area);
		
		Double elev =  10.0;
		Double newDensSuelo =SueloItem.DENSIDAD_SUELO_KG;
		if(sumaAreasSuelos!=null && sumaAreasSuelos>0) {
			if(kgNutrienteSuelos.containsKey(SueloParametro.Elevacion)) {
				elev= kgNutrienteSuelos.get(SueloParametro.Elevacion)/sumaAreasSuelos;
			}
			
		
			if(kgNutrienteSuelos.containsKey(SueloParametro.Densidad)) {
				//FIXME solo deberia dividir por las areas de los suelos; en el caso normal solo habria un suelo
				newDensSuelo = kgNutrienteSuelos.get(SueloParametro.Densidad)/sumaAreasSuelos;//aca tiro una excepcion
			}
		}

		//System.out.println("sumaAreas "+sumaAreas);
	

		
		//System.out.println("obteniendo los nutrientes de las fertilizaciones");
		Map<SueloParametro,Double> kgNutrienteFerts = getKgNutrientesFertilizaciones(geomQuery);
		//System.out.println("obteniendo los nutrientes de las cosechas");
		Map<SueloParametro,Double> kgNutrientesCosechas = getKgNutrientesCosechas(geomQuery);


		//System.out.println("uniendo los parametros de los suelos y fertilizaciones");
		joinParametrosSuelosMaps(kgNutrienteSuelos,kgNutrienteFerts);
		//System.out.println("uniendo los parametros de los suelos y cosechas");
		joinParametrosSuelosMaps(kgNutrienteSuelos,kgNutrientesCosechas);

	

		// y si hay superposicion entre los suelo item? no deberia
		//N
		//no tomo el n organico porque lo calculo al momento de hacer la recomendacion y no quiero duplicaciones
		Double newKgHaNSuelo = 	0.0;
		if(kgNutrienteSuelos.containsKey(SueloParametro.Nitrogeno)) {
			newKgHaNSuelo=kgNutrienteSuelos.get(SueloParametro.Nitrogeno)/ areaQuery;
		}
		//P
		Double newKgHaPSuelo = 0.0;
		if(kgNutrienteSuelos.containsKey(SueloParametro.Fosforo)) {
			newKgHaPSuelo = kgNutrienteSuelos.get(SueloParametro.Fosforo)/ areaQuery;
		}
		
		Double newKgHaKsuelo = 0.0;
		if(kgNutrienteSuelos.containsKey(SueloParametro.Potasio)) {
			newKgHaKsuelo = kgNutrienteSuelos.get(SueloParametro.Potasio)/ areaQuery;
		}

		Double newKgHaSsuelo = 0.0;
		if(kgNutrienteSuelos.containsKey(SueloParametro.Azufre)) {
			newKgHaSsuelo = kgNutrienteSuelos.get(SueloParametro.Azufre)/ areaQuery;
		}
		
		Double newKgHaMOSuelo =0.0;
		if(kgNutrienteSuelos.containsKey(SueloParametro.MateriaOrganica)) {
			newKgHaMOSuelo= kgNutrienteSuelos.get(SueloParametro.MateriaOrganica)/ (areaQuery);
		}
		Double newPpmNsuelo=labor.calcPpmNHaKg(newDensSuelo,newKgHaNSuelo);
		Double newPpmPsuelo=labor.calcPpm_0_20(newDensSuelo,newKgHaPSuelo);
		Double newPpmKsuelo=labor.calcPpm_0_20(newDensSuelo,newKgHaKsuelo);
		Double newPpmSsuelo=labor.calcPpm_0_20(newDensSuelo,newKgHaSsuelo);
		Double newPMoSuelo = labor.calcPorcMoHaKg(newDensSuelo,newKgHaMOSuelo);
		
		SueloItem sueloItem = new SueloItem();
		synchronized(labor){
			//fi= new FertilizacionItem();					
			sueloItem.setId(labor.getNextID());
		}
		sueloItem.setGeometry(geomQuery);

		sueloItem.setDensAp(newDensSuelo);	
		sueloItem.setPpmNO3(newPpmNsuelo);
		sueloItem.setPpmP(newPpmPsuelo);
		sueloItem.setPpmK(newPpmKsuelo);
		sueloItem.setPpmS(newPpmSsuelo);
		
		sueloItem.setPorcMO(newPMoSuelo);
		sueloItem.setElevacion(elev>10?elev:10.0);//10.0);//para que aparezca en el mapa
		return sueloItem;
	}	

	private Map<SueloParametro,Double> getKgNutrientesSuelos(Geometry geometry) {
		Map<SueloParametro,Double> nutrientesGeom = suelos.parallelStream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(mapG, suelo)->getKgNutrientesSuelo(geometry, mapG, suelo)			 
				,
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
				);
		return nutrientesGeom;
	}
	private void getKgNutrientesSuelo(Geometry geometry, Map<SueloParametro, Double> mapG, Suelo suelo) {		
		List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		Map<SueloParametro,Double> nutrientesFertilizacion = items.stream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(map, item)->{
					Double area = 0.0;
					try {
						Geometry itemG = item.getGeometry();		
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, itemG);//geometry.intersection(fertGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);
					} catch (Exception e) {
						e.printStackTrace();
					}

					Map<SueloParametro,Double> nutrientesHaItem= Suelo.getKgNutrientes(item);// (Double) item.getPpmP()*suelo.getDensidad()/2;//TODO multiplicar por la densidad del suelo

					for(SueloParametro k :nutrientesHaItem.keySet()) {					
						addValueToMap(map,k,nutrientesHaItem.get(k)*area);
					}		
					//if(item.getElevacion()>1) {
					//calcular la mineralizacion N de la materia organica. no eso va en recomendacion
					addValueToMap(map,SueloParametro.Densidad,item.getDensAp()*area);			
					addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);	
					
					Double pesoMOHa =item.getPorcMO()*item.getDensAp()*0.2*ProyectionConstants.METROS2_POR_HA/100;
					addValueToMap(map,SueloParametro.MateriaOrganica,pesoMOHa*area);		
					addValueToMap(map,SueloParametro.Area,area);
					//						}else {
					//							System.out.println("la elevacion no es > 1");
					//						}

				}, 
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
				);
		joinParametrosSuelosMaps(mapG, nutrientesFertilizacion);
	}

	private Map<SueloParametro,Double> getKgNutrientesCosechas(Geometry geometry) {
		Map<SueloParametro,Double> nutrientesGeom = cosechas.parallelStream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(mapG, cosecha)->getKgNutrientesCosecha(geometry, mapG, cosecha),
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
				);
		return nutrientesGeom;
	}

	private void getKgNutrientesCosecha(Geometry geometry, Map<SueloParametro, Double> mapG, CosechaLabor cosecha) {		
		Cultivo cultivo = cosecha.getCultivo();		

		List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		Map<SueloParametro,Double> nutrientesFertilizacion = items.parallelStream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(map, item)->{
					Geometry itemG = item.getGeometry();				
					Double area = 0.0;
					try {						
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, itemG);//geometry.intersection(pulvGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);					
					} catch (Exception e) {
						e.printStackTrace();
					}					
					Double rinde = item.getRindeTnHa();
					addValueToMap(map,SueloParametro.Nitrogeno,-1*rinde * cultivo.getExtN() * area);
					addValueToMap(map,SueloParametro.Fosforo,-1*rinde * cultivo.getExtP() * area);
					addValueToMap(map,SueloParametro.Potasio,-1*rinde * cultivo.getExtK() * area);
					addValueToMap(map,SueloParametro.Azufre,-1*rinde * cultivo.getExtS() * area);

					addValueToMap(map,SueloParametro.MateriaOrganica,rinde * cultivo.getAporteMO() * area);	
					//if(item.getElevacion()>1) {
					addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);				
					addValueToMap(map,SueloParametro.Area,area);
					//	}


				}, 
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
				);
		joinParametrosSuelosMaps(mapG, nutrientesFertilizacion);
	}

	private void addValueToMap(Map<SueloParametro,Double> map,SueloParametro p,Double value) {		
		Double kgMap = map.get(p);
		//System.out.println("agregando "+value+" al parametro "+p+" del map. era "+kgMap);
		if(kgMap!=null)value+=kgMap;
		//System.out.println("queda "+value);
		map.put(p, value);	
	}




	private Map<SueloParametro,Double> getKgNutrientesFertilizaciones(Geometry geometry) {
		Map<SueloParametro,Double> nutrientesGeom = fertilizaciones.parallelStream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(mapG, fert)->getKgNutrientesFertilizacion(geometry, mapG, fert),
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
				);
		return nutrientesGeom;
	}

	public void getKgNutrientesFertilizacion(Geometry geometry, Map<SueloParametro, Double> mapG, FertilizacionLabor fert) {
		Fertilizante fertilizante = fert.fertilizanteProperty.getValue();
		List<FertilizacionItem> items = fert.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		Map<SueloParametro,Double> nutrientesFertilizacion = items.stream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(map, item)->{	
					Double area = 0.0;
					try {
						Geometry fertGeom = item.getGeometry();		
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);						
					} catch (Exception e) {
						e.printStackTrace();
					}

					Map<SueloParametro,Double> cFert = fertilizante.getCNutrientes();//%				
					Double dosis = item.getDosistHa();//kg/ha
					if(cFert!=null) {
					for(SueloParametro k :cFert.keySet()) {					
						addValueToMap(map,k,dosis*cFert.get(k)*area/100);						
					}				
					}else {
						System.out.println("cFert es null para "+fertilizante.getNombre());
					}
				//	if(item.getElevacion()>10) {
						addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);				
						addValueToMap(map,SueloParametro.Area,area);
				//	}

				}, 
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
				);
		joinParametrosSuelosMaps(mapG, nutrientesFertilizacion);
	}	

	private void joinParametrosSuelosMaps(Map<SueloParametro, Double> map1, Map<SueloParametro, Double> map2) {			
		map2.keySet().forEach(k->{
			Double kg=0.0;
			Double kg2 = map2.get(k);
			if(kg2!=null)kg+=kg2;
			Double kg1 = map1.get(k);
			if(kg1!=null)kg+=kg1;
			map1.put(k,kg);
		});		
	}


	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();

		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor,si,area);

		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);
	}

	protected int getAmountMin() {
		return 100;
	}

	protected int gerAmountMax() {
		return 500;
	}
}
