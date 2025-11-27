package com.ursulagis.desktop.tasks.procesar;

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
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.geometry.BoundingBox;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.siembra.SiembraConfig.Unidad;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.ConvertirASiembraTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * clase que toma una siembra y una fertilizacion y genera una nueva siembra que combian las dos.
 * @author quero
 *
 */

public class SiembraFertTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	private SiembraLabor siembra;
	private FertilizacionLabor fertilizacion;
	private boolean esFertLinea=true;

	public SiembraFertTask(SiembraLabor _siembra, FertilizacionLabor _fertilizacion,boolean _esFertLinea){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super( new SiembraLabor());
		this.siembra=_siembra;
		this.fertilizacion=_fertilizacion;
		this.esFertLinea=_esFertLinea;		
		labor.getConfiguracion().dosisUnitProperty().set(Unidad.kgHa);
		labor.setSemilla(siembra.getSemilla());//Cultivo(cultivo);
		if(_esFertLinea) {
			labor.setFertLinea(_fertilizacion.getFertilizante());
			//labor.setCantidadFertilizanteLinea(_fertilizacion.getCantidadInsumo());
			
			labor.setFertCostado(_siembra.getFertCostado());
		//	labor.setCantidadFertilizanteCostado(_siembra.getCantidadFertilizanteCostado());
		} else {
			labor.setFertCostado(_fertilizacion.getFertilizante());
			///labor.setCantidadFertilizanteCostado(_fertilizacion.getCantidadInsumo());
			
			labor.setFertLinea(_siembra.getFertCostado());
		//	labor.setCantidadFertilizanteLinea(_siembra.getCantidadFertilizanteCostado());
		}
		labor.setLayer(new LaborLayer());
		labor.setEntreSurco(siembra.getEntreSurco());
		System.out.println("asignando entresurco a siembra fert con valor "+siembra.getEntreSurco()+" queda en "+labor.getEntreSurco());
		labor.colAmount.set(SiembraLabor.COLUMNA_KG_SEMILLA);
		labor.setClasificador(siembra.getClasificador().clone());

		labor.setNombre("SiembraFertilizada "+siembra.getNombre()+"-"+fertilizacion.getNombre());//este es el nombre que se muestra en el progressbar
	}

	/**
	 * proceso que toma una lista de cosechas y las une 
	 * con una grilla promediando los valores de acuerdo a su promedio ponderado por la superficie
	 * superpuesta de cada item sobre la superficie superpuesta total de cada "pixel de la grilla"
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void doProcess() throws IOException {
		//	long init = System.currentTimeMillis();		

//		List<Labor<?>> labores = new LinkedList<Labor<?>>();
//		labores.add(siembra);
//		labores.add(fertilizacion);

		//obtener una lista con todas las geometrias de las labores
//		List<Geometry> geometriasActivas = labores.parallelStream().collect(
//				()->new ArrayList<Geometry>(),
//				(activas, labor) ->{		
//					//List<LaborItem> features = (List<LaborItem>) labor.outStoreQuery(unionEnvelope);					 
//					SimpleFeatureIterator featureIterator = labor.getOutCollection().features();
//					while(featureIterator.hasNext()) {
//						SimpleFeature next = featureIterator.next();
//						activas.add((Geometry)next.getDefaultGeometry());
//					}
//					featureIterator.close();
//				},	(env1, env2) -> env1.addAll(env2));

		//FIXME partes pierde los bordes
//		Set<Geometry> partes = GeometryHelper.obtenerIntersecciones(geometriasActivas);//quito los duplicados
		
		//quito los nulls y las multypoligons
//		List<Polygon> grillaCover1 =  partes.parallelStream().collect(
//				()->new ArrayList<Polygon>(),
//				(activas, poly) ->{					
//					if(poly!=null) {
//						activas.addAll(PolygonValidator.geometryToFlatPolygons(poly));
//					}
//				},	(env1, env2) -> env1.addAll(env2));
		
		// TODO 1 obtener el bounds general que cubre a todas las siembras y fertilizaciones
		ReferencedEnvelope unionEnvelope = siembra.outCollection.getBounds();
		unionEnvelope.expandToInclude(fertilizacion.outCollection.getBounds());
		double anchoGrilla = labor.getConfiguracion().getAnchoGrilla();
				// 2 generar una grilla de ancho ="ancho" que cubra bounds
				//TODO no construir una grilla, explotar todos los poligonos de las siembras y cosechas como en balance de nutrientes
		List<Polygon>  grillaCover = construirGrilla(unionEnvelope, anchoGrilla);
				//System.out.println("creando una grilla con "+grilla.size()+" elementos");
				// 3 recorrer cada pixel de la grilla promediando los valores y generando los nuevos items de la cosecha
//		Geometry contornoSiembra = GeometryHelper.extractContornoGeometry(siembra);//contorno siembra devuelve null
//		Geometry contornoFert = GeometryHelper.extractContornoGeometry(fertilizacion);
//		Geometry contornosUnion = contornoSiembra.union(contornoFert);
		
//		grillaCover =  grillaCover.parallelStream().collect(
//		()->new ArrayList<Polygon>(),
//		(activas, poly) ->{					
//			if(poly!=null) {
//				try {
//				//Geometry intSiembra = contornoSiembra.intersection(poly);
//				Geometry intFert = contornosUnion.intersection(poly);
//				activas.addAll(PolygonValidator.geometryToFlatPolygons(intFert));
//				}catch(Exception e) {
//					e.printStackTrace();
//				}
//			}
//		},	(env1, env2) -> env1.addAll(env2));
		
		featureCount = grillaCover.size();
		System.out.println("termine de crear la grilla con "+grillaCover.size()+" elementos");
		int clasesSiembra = siembra.clasificador.getNumClasses();
		//int clasesB = fertilizacion.clasificador.getNumClasses();
		//claseAB = clasesA*claseB(r)+claseA(r)
		ConcurrentHashMap< Integer,List<SiembraItem>> byBicluster = grillaCover.parallelStream().collect(
				() -> new  ConcurrentHashMap< Integer,List<SiembraItem>>(),
				(map, poly) -> {
					try{
						//XXX podria ahorrarme estas querys si primero filtro los poligonos que no entran en el contorno
						List siembrasPoly=this.siembra.cachedOutStoreQuery(poly.getEnvelopeInternal()); 						 

						if(siembrasPoly.size()==0 ) {
							return;//salteo los poligonos que no tienen siembra
						} else {
							//System.out.println("tengo siembras para poly");
						}
						SiembraItem siembraItem = construirFeature(siembrasPoly,poly);     //geometry y amount
						if(siembraItem == null )return;
						List fertilizacionesPoly=this.fertilizacion.cachedOutStoreQuery(siembraItem.getGeometry().getEnvelopeInternal());
						LaborItem fertilizacionItem = construirFertilizacionItem(fertilizacionesPoly,siembraItem.getGeometry());						

						Double fertHa = fertilizacionItem.getAmount();
					
						if(esFertLinea) {
							siembraItem.setDosisFertLinea(fertHa);
						}else {
							siembraItem.setDosisFertCostado(fertHa);
						}
						
						int claseFert = fertilizacion.clasificador.getCategoryFor(fertHa) ;
						int claseSiembra = siembra.clasificador.getCategoryFor(siembraItem.getDosisHa());
					
						Integer index = clasesSiembra*claseFert + claseSiembra;
						//System.out.println("clase "+claseSiembra+" valor"+siembraItem.getDosisHa()+" index "+index);//TODO remove syso
					
						List<SiembraItem> catFeatures = map.get(index);
						if(catFeatures==null)catFeatures=new ArrayList<SiembraItem>();
						catFeatures.add(siembraItem);

						map.put(index, catFeatures);
						featureNumber++;
						updateProgress(featureNumber, featureCount);
					}catch(Exception e){
						System.err.println("error al construir un elemento de la grilla");
						e.printStackTrace();
					}
				},
				(map1, map2) -> {//recorro map 2 y agrego donde corresponden los items en map 1
					map2.keySet().forEach((k)->{
						if(map1.keySet().contains(k)) {
							map1.get(k).addAll(map2.get(k));
						}else {
							map1.put(k, map2.get(k));
						}
					});
				}
				);





		//XXX byPolygon contiene un map con la categoria de destino y todos los simplefeatures
		//		Optional<List<SiembraItem>> noRes = byBicluster.values().stream().reduce((l1,l2)->{
		//			l1.addAll(l2);
		//			return l1;
		//		});
		//List<SiembraItem> resumidas = noRes.get();
		List<SiembraItem> resumidas = resumirClases(byBicluster);

		double pesoSKg = labor.getSemilla().getPesoDeMil()/(1000*1000);



		List<SiembraItem> itemsToShow = new ArrayList<SiembraItem>();
		//Double id = 0d;
		for(SiembraItem value :resumidas) {
			SiembraItem si = new SiembraItem();
			si.setGeometry(value.getGeometry());
			si.setId(this.labor.getNextID());
			si.setDosisHa(value.getDosisHa());

			Double seedsM2=si.getDosisHa()/(pesoSKg*ProyectionConstants.METROS2_POR_HA);
			double sML=seedsM2*labor.getEntreSurco();//esto es 0.42
			si.setDosisML(sML*10);//multipolicar por 10 porque al insertarla se divide por 10

			si.setDosisFertLinea(value.getDosisFertLinea());
			si.setDosisFertCostado(value.getDosisFertCostado());
			labor.setPropiedadesLabor(si);			

			//System.out.println("termine de crear una siembra resumida : "+ci.toString());
			labor.insertFeature(si); //aca se divide por 10
			itemsToShow.add(si);		
		}

		labor.constructClasificador();
		runLater(itemsToShow);
	}

	
	/**
	 * 
	 * @param siembrasPoly lista de cosechasItems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private SiembraItem construirFeature(List<SiembraItem> siembrasPoly, Polygon poly) {
		SiembraItem si = null;
		if(siembrasPoly.size()<1){
			return null;
		}
		
		List<Geometry> intersections = new ArrayList<Geometry>();

		double areaInterseccionesTotal = 0;
		Map<SiembraItem,Double> areasIntersecciones = new HashMap<SiembraItem,Double>();
		//calculo el area intersectada total y cacheo las intersecciones y sus areas
		for(SiembraItem sPoly : siembrasPoly){	//de cada cosecha obtengo que area se intersecta con la query		
			Geometry g = sPoly.getGeometry();
			try{		
				g= GeometryHelper.getIntersection(poly, g);//EnhancedPrecisionOp.intersection(poly,g);
				intersections.add(g);		
				Double areaInterseccion = g.getArea();
				areaInterseccionesTotal+=areaInterseccion;
				areasIntersecciones.put(sPoly,areaInterseccion);				
					
			}catch(Exception e){
				System.err.println(Messages.getString("GrillarCosechasMapTask.14")+poly+Messages.getString("GrillarCosechasMapTask.15")+g); //$NON-NLS-1$ //$NON-NLS-2$
			}		
		}		
		//si el area intersectada es mayor a un minimo procedo a crear la siembra promedio
		if(areaInterseccionesTotal>getAreaMinimaLongLat()){
			double dosisHaProm=0,fertL=0,fertC=0,elev=0;
		for(SiembraItem aPoly : siembrasPoly){
				Double gArea = areasIntersecciones.get(aPoly);//cPoly.getGeometry();
				if(gArea==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = gArea/areaInterseccionesTotal;
				dosisHaProm+=aPoly.getDosisHa()*peso;
				fertL+=aPoly.getDosisFertLinea()*peso;
				if(Double.isNaN(fertL)) {
					System.out.println("item fertL es NaN");
					fertL=0;
				}
				if(Double.isNaN(fertC)) {
					System.out.println("item fertC es NaN");
					fertC=0;
				}
				fertC+=aPoly.getDosisFertCostado()*peso;
				elev+=aPoly.getElevacion()*peso;
			}
			
			synchronized(labor){
				si = new SiembraItem();
				si.setId(labor.getNextID());
				labor.setPropiedadesLabor(si);
			}

			
			Geometry union2 = null;
//			if(!rellenarHuecos) {				
			try{
			GeometryCollection colectionCat = GeometryHelper.toGeometryCollection(intersections);
				union2 = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
				}catch(Exception e){

				}
//			} else { 
//				union2 = poly;
//			}
			
			si.setGeometry(union2);
			si.setDosisHa(dosisHaProm);
			si.setDosisFertCostado(fertC);
			si.setDosisFertLinea(fertL);
		
			si.setElevacion(elev);
		}
		return si;
	}
	
	@Deprecated
	/**
	 * cambie este metodo por una copia del que se usa en grillarCosechas porque es mas rapido y anda mejor
	 * @param siembrasPoly
	 * @param polyGrilla
	 * @return
	 */
	private SiembraItem construirSiembraItem(List<SiembraItem> siembrasPoly, Polygon polyGrilla) {
		if(siembrasPoly.size()<1){
			return null;
		}
		SiembraItem ret = new SiembraItem();//[4];
		
		//			ret[FEAURE_POLYGON_INDEX]=poly;
		//			ret[SIEMBRA_SEMILLAS_INDEX]=-1d;
		//			ret[SIEMBRA_FERTL_INDEX]=-1d;
		//			ret[SIEMBRA_FERTC_INDEX]=-1d;
		if(polyGrilla==null || siembrasPoly==null || siembrasPoly.size()<1){
			return null;
		}

		double semillasPoly=0;
		double fertLPoly=0;
		double fertCpoly=0;

		double areaTotal=polyGrilla.getArea();
		double areaIntersection=0.0;
		List<Geometry> intersecciones = new ArrayList<Geometry>();
		for(SiembraItem li : siembrasPoly){
			Geometry inter = GeometryHelper.getIntersection(li.getGeometry(), polyGrilla);				
			if(inter != null) {
				intersecciones.add(inter);
				double intersection = inter.getArea();					
				areaIntersection+=intersection;
				SiembraItem si = (SiembraItem)li;
				//					System.out.println("agregando al promedio s="+si.getDosisHa()
				//					+" costado="+si.getDosisFertCostado()
				//					+" linea="+si.getDosisFertLinea());
				semillasPoly+=si.getDosisHa()*intersection;
				fertLPoly+=si.getDosisFertLinea()*intersection;
				fertCpoly+=si.getDosisFertCostado()*intersection;

			} else {
				//System.err.println("la interseccion devuelve null");
			}
		}
		Geometry union =GeometryHelper.unirGeometrias(intersecciones);

		ret.setGeometry(union);
		if(areaIntersection>0 || semillasPoly<0) {
			semillasPoly=semillasPoly/areaIntersection;
			fertLPoly=fertLPoly/areaIntersection;
			fertCpoly=fertCpoly/areaIntersection;
		} 
		//			else {
		//				semillasPoly=-1;
		//				fertLPoly=-1;
		//				fertCpoly=-1;
		//				System.err.println("error al calcular las superposiciones de la siembra fertilizada; la suma de las intersecciones da cero");
		//			}

		//			System.out.println("promedio final s="+semillasPoly
		//			+" costado="+fertCpoly
		//			+" linea="+fertLPoly);
		//ret[SIEMBRA_SEMILLAS_INDEX]=semillasPoly;
		ret.setDosisHa(semillasPoly);
		//ret[SIEMBRA_FERTL_INDEX]=fertLPoly;
		ret.setDosisFertLinea(fertLPoly);
		//ret[SIEMBRA_FERTC_INDEX]=fertCpoly;
		ret.setDosisFertCostado(fertCpoly);
		return ret;		
	}

	private FertilizacionItem construirFertilizacionItem(List<FertilizacionItem> fertilizacionesPoly, Geometry poly) {
		FertilizacionItem ret = new FertilizacionItem();
		ret.setGeometry(poly);
		if(poly==null || fertilizacionesPoly==null || fertilizacionesPoly.size()<1){
			return ret;
		}
		double fertLPoly=0;	

		double areaTotal=0;
		for(FertilizacionItem fi : fertilizacionesPoly){
			Geometry inter = GeometryHelper.getIntersection(fi.getGeometry(), poly);				
			if(inter != null) {
				double intersection = inter.getArea();	
				areaTotal+=intersection;			
				fertLPoly+=fi.getDosistHa()*intersection;
			} else {
				//System.err.println("la interseccion devuelve null");
			}
		}	
		if(areaTotal>0) {
			fertLPoly=fertLPoly/areaTotal;
		}
		ret.setDosistHa(fertLPoly);
		return ret;		
	}

	/**
	 * toma un map con listas de features de una misma categoria y responde una lista de una feature resumida por cada categoria
	 * @param byBicluster
	 * @return
	 */
	private List<SiembraItem> resumirClases(Map<Integer,List<SiembraItem>> byBicluster) {	
		System.out.println("resumiendoBicluster con "+byBicluster.size()+" clusters");
		List<SiembraItem> objects = new  ArrayList<SiembraItem>();
		for(Integer clase:  byBicluster.keySet()) {
			List<SiembraItem> cluster = byBicluster.get(clase);//.values()
			//objects.addAll(cluster);
			System.out.println("resumiendo el cluster "+clase+" con "+cluster.size()+" features");
			objects.addAll(construirSiembraItemResumido(cluster));
		}	
		return objects;
	}

	//	// metodo que resume todos los elementos de una misma categoria
	//	private List<Object[]> construirFeatureResumidoGeometrico(List<Object[]> cluster){
	//		List<Object[]> ret = new ArrayList<Object[]>();
	//
	//		if(cluster.size()<1){
	//			System.out.println("no hay features en el cluster");
	//			return ret;
	//		}
	//
	//		Geometry[] geomArray = new Geometry[cluster.size()];
	//
	//		double amountProm=0,fertProm=0;
	//		int n=0;
	//		for(Object[] li : cluster){
	//			geomArray[n]=(Geometry)li[0];
	//			//aUnir.add((Geometry)li[0]);
	//			amountProm+=(Double)li[1];
	//			fertProm+=(Double)li[2];
	//			n++;
	//		}
	//		amountProm=amountProm/n;
	//		fertProm=fertProm/n;
	//
	//		Geometry union = geomArray[0];//.get(0);
	//		GeometryFactory fact = union.getFactory();
	//
	//		GeometryCollection colectionCat = fact.createGeometryCollection(geomArray);
	//
	//		try{
	//			union = colectionCat.buffer(0);//convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
	//		}catch(Exception e){
	//			System.out.println("fallo la union de las geometrias del Bicluster");
	//			e.printStackTrace();
	//		}
	//
	//		//SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//local para no tener problemas de concurrencia
	//		List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(union);
	//
	//		for(Polygon p:flatPolygons){
	//			//synchronized(fb) {
	//			//System.out.println("creando el bicluster con la geometria unida "+p);
	//			Object[] values = {p,amountProm,fertProm,0.0}; 
	//			//fb.addAll(values);
	//			//SimpleFeature exportFeature = fb.buildFeature(null);//fb concurrentes ponen los mismos ids?
	//
	//			ret.add(values);
	//			//		}
	//		}
	//		return ret;
	//	}

	/**
	 * 
	 * @param cluster lista de SiembraItems de una misma clase
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private List<SiembraItem> construirSiembraItemResumido(List<SiembraItem> cluster) {
		if(cluster.size()<1){
			return null;
		}


		// sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
		List<SiembraItem> catFeatures = new ArrayList<SiembraItem>();// new HashMap<Geometry,Double>();


		double siembraProm=0,fertPromL = 0,fertPromC = 0,areaInterseccion = 0,elevProm= 0;
		List<Geometry> aUnir = new ArrayList<Geometry>();
		
		for(SiembraItem f : cluster){			
			if(f.getGeometry()==null)continue;
			Double gArea = f.getGeometry().getArea();
			areaInterseccion+=gArea;
			aUnir.add(f.getGeometry());

			siembraProm+=gArea*f.getDosisHa();//LaborItem.getDoubleFromObj(f.getAttribute(SIEMBRA_COLUMN));
			if(!Double.isNaN(f.getDosisFertLinea())) {			
				fertPromL+=gArea*f.getDosisFertLinea();//LaborItem.getDoubleFromObj(f.getAttribute(LINEA_COLUMN));
			}
			
			fertPromC+=gArea*f.getDosisFertCostado();//LaborItem.getDoubleFromObj(f.getAttribute(LINEA_COLUMN));
			elevProm+=gArea*f.getElevacion();
		}

		Geometry union = GeometryHelper.unirGeometrias(aUnir);
		Double unionArea = union.getArea();//no da igual el area de la interseccion que el area de la union
		if(unionArea==0) {
			System.err.println("area union es null");
			return catFeatures;}
		siembraProm=siembraProm/areaInterseccion;
		fertPromL=fertPromL/areaInterseccion;
		if(Double.isNaN(fertPromL)) {
			System.out.println("item fertPromL es NaN");
			fertPromL=0;
		}
		fertPromC=fertPromC/areaInterseccion;
		elevProm=elevProm/areaInterseccion;
		List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(union);

		for(Polygon p:flatPolygons){
			SiembraItem siRes = new SiembraItem();
			siRes.setGeometry(p);
			siRes.setDosisHa(siembraProm);
			siRes.setDosisFertLinea(fertPromL);
			siRes.setDosisFertCostado(fertPromC);		 
			siRes.setElevacion(elevProm);
			
			catFeatures.add(siRes);
		}

		return catFeatures;
	}


	/**
	 * 
	 * @param siembrasPoly lista de laborItems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return new Object[2]{geom,dosisSiembra o dosisFert segun lista de items de entrada}
	 */
	//	private Object[] construirFeatureGeometric(List<LaborItem> siembrasPoly, Polygon poly) {
	//		Object[] ret = new Object[4];
	//		ret[FEAURE_POLYGON_INDEX]=poly;
	//		ret[1]=0d;
	//		ret[2]=0d;
	//		ret[3]=0d;
	//		if(siembrasPoly.size()<1){
	//			return ret;
	//		}
	//
	//
	//
	//		//siembrasPoly.stream().mapToDouble(LaborItem::getAmount).average();
	//		double amountProm=0;
	//		double fertLProm=0;
	//		double fertCProm=0;
	////		OptionalDouble opt = siembrasPoly.stream().mapToDouble(LaborItem::getAmount).average();
	////		if(opt.isPresent()) {
	////			amountProm=opt.getAsDouble();
	////		}
	////		int n=0;
	//		double areaTotal=0;
	//		for(LaborItem li : siembrasPoly){
	//			Geometry inter = GeometryHelper.getIntersection(li.getGeometry(), poly);
	//			
	//			if(inter != null) {
	//				double intersection = inter.getArea();
	//				areaTotal+=intersection;
	//				
	//				if(li instanceof SiembraItem) {
	//					SiembraItem si = (SiembraItem)li;
	//					System.out.println("agregando al promedio s="+si.getDosisHa()
	//					+" costado="+si.getDosisFertCostado()
	//					+" linea="+si.getDosisFertLinea());
	//					amountProm+=si.getDosisHa()*intersection;
	//					fertLProm+=si.getDosisFertLinea()*intersection;
	//					fertCProm+=si.getDosisFertCostado()*intersection;
	//				}
	//			} else {
	//				System.err.println("la interseccion devuelve null");
	//			}
	//		}
	//		if(areaTotal>0) {
	//			amountProm=amountProm/areaTotal;
	//			fertLProm=fertLProm/areaTotal;
	//			fertCProm=fertCProm/areaTotal;
	//		} else {
	//			amountProm=-1;
	//			fertLProm=-1;
	//			fertCProm=-1;
	//			System.err.println("error al calcular las superposiciones de la siembra fertilizada; la suma de las intersecciones da cero");
	//		}
	//
	//		System.out.println("promedio final s="+amountProm
	//		+" costado="+fertCProm
	//		+" linea="+fertLProm);
	//		ret[1]=amountProm;
	//		ret[2]=fertLProm;
	//		ret[3]=fertCProm;
	//
	//		return ret;
	//	}


	/**
	 * 
	 * @param siembrasPoly lista de cosechasitems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	//	private Object[] construirFeature(List<LaborItem> siembrasPoly, Polygon poly) {
	//		Object[] ret = new Object[2];
	//		ret[0]=poly;
	//		ret[1]=0d;
	//		if(siembrasPoly.size()<1){
	//			return ret;
	//		}
	//
	//		List<Geometry> intersections = new ArrayList<Geometry>();
	//		// sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
	//		Geometry union = poly;
	//		double areaPoly = 0;
	//		Map<LaborItem,Double> areasIntersecciones = new HashMap<LaborItem,Double>();
	//		for(LaborItem cPoly : siembrasPoly){			
	//			//XXX si es una cosecha de ambientes el area es importante
	//			Geometry g = cPoly.getGeometry();
	//			List<Polygon> flatP = PolygonValidator.geometryToFlatPolygons(g);
	//			for(Polygon fp:flatP) {
	//				try{
	//					g= EnhancedPrecisionOp.intersection(poly,fp);
	//					Double areaInterseccion = g.getArea();
	//					areaPoly+=areaInterseccion;
	//					areasIntersecciones.put(cPoly,areaInterseccion);
	//					if(union==null){
	//						union = g;		//union es la geometria que se va a devolver al final
	//					}
	//					intersections.add(g);
	//				}catch(Exception e){
	//					System.err.println("no se pudo hacer la interseccion entre\n"+poly+"\n y\n"+fp);
	//				}		
	//			}
	//		}
	//
	//		//	Object[] ret = new Object[2];// new HashMap<Geometry,Double>();
	//
	//		double amountProm=0;
	//		for(LaborItem li : areasIntersecciones.keySet()){
	//			Double gArea = areasIntersecciones.get(li);
	//			if(gArea==null)continue;
	//			double peso = gArea/areaPoly;
	//			amountProm+=li.getAmount()*peso;
	//		}
	//		if(intersections.size()>0) {
	//
	//			GeometryFactory fact = intersections.get(0).getFactory();
	//			Geometry[] geomArray = new Geometry[intersections.size()];
	//			GeometryCollection colectionCat = fact.createGeometryCollection(intersections.toArray(geomArray));
	//
	//			try{
	//				union = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
	//			}catch(Exception e){
	//
	//			}
	//			ret[0]=union;
	//			ret[1]=amountProm;
	//		}
	//		return ret;
	//	}

	private double getAreaMinimaLongLat() {
		return labor.getConfiguracion().supMinimaProperty().doubleValue()*ProyectionConstants.metersToLong()*ProyectionConstants.metersToLat();
	}

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	public static List<Polygon> construirGrilla(BoundingBox bounds,double ancho) {
		System.out.println("construyendo grilla");
		List<Polygon> polygons = new ArrayList<Polygon>();
		//convierte los bounds de longlat a metros
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong() - ancho/2;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat() - ancho/2;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong() + ancho/2;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat() + ancho/2;
		Double x0=minX;
		for(int x=0;(x0)<maxX;x++){
			x0=minX+x*ancho;
			Double x1=minX+(x+1)*ancho;
			for(int y=0;(minY+y*ancho)<maxY;y++){
				Double y0=minY+y*ancho;
				Double y1=minY+(y+1)*ancho;


				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());

				/**
				 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
				 * carro--B
				 * 
				 */
				Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
				// Empezar y terminar en
				// el mismo punto.
				// sentido antihorario

				//			GeometryFactory fact = X.getFactory();
				GeometryFactory fact = new GeometryFactory();


				//				DirectPosition upper = positionFactory.createDirectPosition(new double[]{-180,-90});
				//				DirectPosition lower = positionFactory.createDirectPosition(new double[]{180,90});
				//	Envelope envelope = geometryFactory.createEnvelope( upper, lower );

				LinearRing shell = fact.createLinearRing(coordinates);
				LinearRing[] holes = null;
				Polygon poly = new Polygon(shell, holes, fact);			
				polygons.add(poly);
			}
		}
		return polygons;
	}

	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = ConvertirASiembraTask.buildTooltipText(siembraFeature, area,labor);
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	
	}

	@Override
	protected int getAmountMin() {		
		return 0;
	}

	@Override
	protected int gerAmountMax() {		
		return 0;
	}

}
