package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.pulverizacion.PulverizacionItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearPulverizacionMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class UnirPulverizacionesMapTask extends ProcessMapTask<PulverizacionItem,PulverizacionLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<PulverizacionLabor> pulverizaciones;

	public UnirPulverizacionesMapTask(List<PulverizacionLabor> pulverizaciones){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.pulverizaciones=new ArrayList<PulverizacionLabor>();
		for(PulverizacionLabor l:pulverizaciones){
			if(l.getLayer().isEnabled()){
				this.pulverizaciones.add(l);
				
			}
		};

		super.labor = new PulverizacionLabor();
		//asignar las columnas a  los valores estandar
		labor.colAncho.set(PulverizacionLabor.COLUMNA_ANCHO);
		labor.colCurso.set(PulverizacionLabor.COLUMNA_CURSO);
		labor.colDistancia.set(PulverizacionLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(PulverizacionLabor.COLUMNA_ELEVACION);

		String nombreProgressBar = "grillar pulverizacion";
		if(pulverizaciones.size()>1){
			nombreProgressBar = "unir pulverizaciones";
		}
		labor.setLayer(new LaborLayer());
		labor.setNombre(nombreProgressBar);//este es el nombre que se muestra en el progressbar
	}

	/**
	 * proceso que toma una lista de pulverizaciones y las une sin tener en cuenta superposiciones ni nada
	 */
	@Override
	protected void doProcess() throws IOException {
		long init = System.currentTimeMillis();
		// TODO 1 obtener el bounds general que cubre a todas las pulverizaciones
		ReferencedEnvelope unionEnvelope = null;
		double ancho = labor.getConfigLabor().getAnchoGrilla();
		String nombre =null;
		String prefijo = "Grilla";
		if(pulverizaciones.size()>1){
			prefijo = "Union";
		}
//		int featuresInsertadas=0;
		for(PulverizacionLabor pulv:pulverizaciones){
			if(nombre == null){
				nombre=prefijo+" "+pulv.getNombre();	
			}else {
				nombre+=" - "+pulv.getNombre();
			}
//			FeatureReader<SimpleFeatureType, SimpleFeature> reader = pulv.outCollection.reader();
//			while(reader.hasNext()){
//				SimpleFeature f = reader.next();
//				PulverizacionItem ci = labor.constructFeatureContainerStandar(f,true);
//				SimpleFeature nf=ci.getFeature(labor.featureBuilder);
//				boolean ret = labor.outCollection.add(nf);
//				featuresInsertadas++;
//				if(!ret){
//					System.out.println("no se pudo agregar la feature "+f);
//				}
//			}
//			reader.close();
			
			ReferencedEnvelope b = pulv.outCollection.getBounds();
			if(unionEnvelope==null){
				unionEnvelope=b;
			}else{
				unionEnvelope.expandToInclude(b);
			}
		}
		labor.setNombre(nombre);
		labor.setLayer(new LaborLayer());
		
		/*grillar la pulverizacion resultante sumando las dosis*/
		
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);
		//List<Polygon>  grilla = construirGrillaTriangular(unionEnvelope, ancho);
		//double elementos = grilla.size();
		System.out.println("creando una grilla con "+grilla.size()+" elementos");
		// 3 recorrer cada pixel de la grilla promediando los valores y generando los nuevos items de la cosecha
		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		
		ConcurrentMap<Polygon,PulverizacionItem > byPolygon =
				grilla.parallelStream().collect(
						() -> new  ConcurrentHashMap< Polygon,PulverizacionItem>(),
						(map, poly) -> {
							
						try{
							List<PulverizacionItem>  fertsPoly = pulverizaciones.parallelStream().collect(
									()->new  ArrayList<PulverizacionItem>(),
									(list, ferts) ->{			
										list.addAll(ferts.cachedOutStoreQuery(poly.getEnvelopeInternal()));	
									},
									(list1, list2) -> list1.addAll(list2)
									);

							PulverizacionItem item = construirFeature(fertsPoly,poly);                    			

							if(item!=null){
								map.put(poly,item);
								//	items.add(item);
								SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
										labor.getType());
								SimpleFeature f = item.getFeature(fBuilder);
								if(f!=null){
									boolean res = features.add(f);
									if(!res){
										System.out.println("no se pudo agregar la feature "+f);
									}
								}
							}
							this.featureNumber++;
							updateProgress( this.featureNumber, featureCount);

						}catch(Exception e){
							System.err.println("error al construir un elemento de la grilla");
							e.printStackTrace();
						}
						},
						(map1, map2) -> map1.putAll(map2)
						);
		
		for(PulverizacionLabor l:pulverizaciones){
			l.clearCache();
		}
		
		System.out.println("cree una union de "+byPolygon.size()+" elementos");

//FIXME esto hace que la grilla no tenga memoria
		if(labor.inCollection == null){
			labor.inCollection = new DefaultFeatureCollection("internal",labor.getType());
		}
		labor.inCollection.addAll(features);
		boolean ret= labor.outCollection.addAll(features);
		if(!ret){//XXX si esto falla es provablemente porque se estan creando mas de una feature con el mismo id
			System.out.println("no se pudieron agregar las features al outCollection");
		}

		// 4 mostrar la pulverizacion sintetica creada
		labor.constructClasificador();
		
		/*fin de grillar pulverizacion*/

		runLater(byPolygon.values());
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde "+time+" milisegundos en unir las pulverizaciones.");
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, PulverizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearPulverizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	/**
	 * 
	 * @param pulverizacionesPoly
	 * @param poly
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private PulverizacionItem construirFeature(List<PulverizacionItem> pulverizacionesPoly,
			Polygon poly) {
		if(pulverizacionesPoly.size()<1){
			return null;
		}
		
		List<Geometry> intersections = new ArrayList<Geometry>();
		// sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
		Geometry union = null;
		double sumAreaInterseccion = 0;
		Map<PulverizacionItem,Double> areasIntersecciones = new HashMap<PulverizacionItem,Double>();
		for(PulverizacionItem fPoly : pulverizacionesPoly){			
			//XXX si es una cosecha de ambientes el area es importante
			Geometry g = fPoly.getGeometry();
			try{
				g = EnhancedPrecisionOp.intersection(poly,g);
				Double areaInterseccion = g.getArea();
				sumAreaInterseccion += areaInterseccion;
				areasIntersecciones.put(fPoly,areaInterseccion);
				if(union==null){
					union = g;
				}
				intersections.add(g);
			
			}catch(Exception e){
				System.err.println("no se pudo hacer la interseccion entre\n"+poly+"\n y\n"+g);
			}		
		}

		PulverizacionItem f = null;

		if(sumAreaInterseccion>getAreaMinimaLongLat()){
			double insumoProm=0,desvioPromedio=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;
			ancho=labor.getConfigLabor().getAnchoGrilla();
			distancia=ancho;
		
			GeometryFactory fact = intersections.get(0).getFactory();
			Geometry[] geomArray = new Geometry[intersections.size()];
			GeometryCollection colectionCat = fact.createGeometryCollection(intersections.toArray(geomArray));
			
			try{
				union = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
				}catch(Exception e){			}
			
			
			double areaPoly = union.getArea();
		for(PulverizacionItem fPoly : areasIntersecciones.keySet()){
				Double areaInterseccion = areasIntersecciones.get(fPoly);//cPoly.getGeometry();
				if(areaInterseccion==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = areaInterseccion/areaPoly;//al dividir por el area del poligono en vez del area de la interseccion saco la suma en vez del promedio
				insumoProm+=fPoly.getDosis()*peso;
				elev+=fPoly.getElevacion()*peso;
			}
			
			//	System.out.println("pesos = "+pesos);
			synchronized(labor){
				f = new PulverizacionItem();
				f.setId(labor.getNextID());
				labor.setPropiedadesLabor(f);
			}


			
			f.setGeometry(union);
			f.setDosis(insumoProm);
			f.setAncho(ancho);
			f.setDistancia(distancia);
			f.setElevacion(elev);
			f.setRumbo(rumbo);
			//simpleFeature = c.getFeature(fBuilder);
		}
		return f;
	}
	
	private double getAreaMinimaLongLat() {
		return labor.getConfigLabor().supMinimaProperty().doubleValue()*ProyectionConstants.metersToLong()*ProyectionConstants.metersToLat();
	}
	@Override
	protected int getAmountMin() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		// TODO Auto-generated method stub
		return 0;
	}

}
