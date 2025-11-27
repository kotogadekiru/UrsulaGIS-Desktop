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

import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearFertilizacionMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class UnirFertilizacionesMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<FertilizacionLabor> fertilizaciones;

	public UnirFertilizacionesMapTask(List<FertilizacionLabor> fertilizaciones){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.fertilizaciones=new ArrayList<FertilizacionLabor>();
		for(FertilizacionLabor l:fertilizaciones){
			if(l.getLayer().isEnabled()){
				this.fertilizaciones.add(l);

			}
		};

		super.labor = new FertilizacionLabor();
		//asignar las columnas a  los valores estandar
		labor.colAmount.set(FertilizacionLabor.COLUMNA_KG_HA);
		labor.colKgHaProperty.set(FertilizacionLabor.COLUMNA_KG_HA);
		labor.colAncho.set(FertilizacionLabor.COLUMNA_ANCHO);
		labor.colCurso.set(FertilizacionLabor.COLUMNA_CURSO);
		labor.colDistancia.set(FertilizacionLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(FertilizacionLabor.COLUMNA_ELEVACION);

		String nombreProgressBar = "grillar fertilizacion";
		if(fertilizaciones.size()>1){
			nombreProgressBar = "unir fertilizaciones";
		}
		labor.setLayer(new LaborLayer());
		labor.setNombre(nombreProgressBar);//este es el nombre que se muestra en el progressbar
	}

	/**
	 * proceso que toma una lista de fertilizaciones y las une sin tener en cuenta superposiciones ni nada
	 */
	@Override
	protected void doProcess() throws IOException {
		long init = System.currentTimeMillis();
		// TODO 1 obtener el bounds general que cubre a todas las fertilizaciones
		ReferencedEnvelope unionEnvelope = null;
		double ancho = labor.getConfigLabor().getAnchoGrilla();
		String nombre =null;
		String prefijo = "grilla";
		if(fertilizaciones.size()>1){
			prefijo = "union";
		}
		//		int featuresInsertadas=0;
		for(FertilizacionLabor fert:fertilizaciones){
			if(nombre == null){
				nombre=prefijo+" "+fert.getNombre();	
			}else {
				nombre+=" - "+fert.getNombre();
			}
			if(labor.fertilizante==null){//inicializo las propiedades con los valores de la primera fert unida
				//esto no se corre porque en el constructor se inicializa con los valores default
				System.out.println("inicializando las variables de la nueva fertilizacion con los de la primera fert a unir");
				labor.fertilizante=fert.fertilizante;
				labor.setPrecioInsumo(fert.getPrecioInsumo());
				labor.setFecha(fert.getFecha());
				labor.setPrecioLabor(fert.getPrecioLabor());
			}
			//			FeatureReader<SimpleFeatureType, SimpleFeature> reader = fert.outCollection.reader();
			//			while(reader.hasNext()){
			//				SimpleFeature f = reader.next();
			//				FertilizacionItem ci = labor.constructFeatureContainerStandar(f,true);
			//				SimpleFeature nf=ci.getFeature(labor.featureBuilder);
			//				boolean ret = labor.outCollection.add(nf);
			//				featuresInsertadas++;
			//				if(!ret){
			//					System.out.println("no se pudo agregar la feature "+f);
			//				}
			//			}
			//			reader.close();

			ReferencedEnvelope b = fert.outCollection.getBounds();
			if(unionEnvelope==null){
				unionEnvelope=b;
			}else{
				unionEnvelope.expandToInclude(b);
			}
		}
		labor.setNombre(nombre);
		labor.setLayer(new LaborLayer());

		/*grillar la fertilizacion resultante sumando las dosis*/

		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);
		//List<Polygon>  grilla = construirGrillaTriangular(unionEnvelope, ancho);
		//double elementos = grilla.size();
		System.out.println("creando una grilla con "+grilla.size()+" elementos");
		// 3 recorrer cada pixel de la grilla promediando los valores y generando los nuevos items de la cosecha
		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());

		ConcurrentMap<Polygon,FertilizacionItem > byPolygon =
				grilla.parallelStream().collect(
						() -> new  ConcurrentHashMap< Polygon,FertilizacionItem>(),
						(map, poly) -> {

							try{
								List<FertilizacionItem>  fertsPoly = fertilizaciones.parallelStream().collect(
										()->new  ArrayList<FertilizacionItem>(),
										(list, ferts) ->{			
											list.addAll(ferts.cachedOutStoreQuery(poly.getEnvelopeInternal()));	
										},
										(list1, list2) -> list1.addAll(list2)
										);

								FertilizacionItem item = construirFeature(fertsPoly,poly);                    			

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

		for(FertilizacionLabor l:fertilizaciones){
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

		// 4 mostrar la fertilizacion sintetica creada
		labor.constructClasificador();

		/*fin de grillar fertilizacion*/

		runLater(byPolygon.values());
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde "+time+" milisegundos en unir las fertilizaciones.");
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearFertilizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	/**
	 * 
	 * @param fertilizacionesPoly: la lista de FertilizacionItems a unir
	 * @param poly
	 * @return la union de las fertilizacion items promediada por superficie intersectada con poly 
	 */
	private FertilizacionItem construirFeature(
			List<FertilizacionItem> fertilizacionesPoly,
			Polygon poly) {
		if(fertilizacionesPoly.size()<1){
			return null;
		}

		List<Geometry> intersections = new ArrayList<Geometry>();
		// sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
		Geometry union = null;
		double sumAreaInterseccion = 0;
		Map<FertilizacionItem,Double> areasIntersecciones = new HashMap<FertilizacionItem,Double>();
		for(FertilizacionItem fPoly : fertilizacionesPoly){			
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

		FertilizacionItem f = null;

		if(sumAreaInterseccion>getAreaMinimaLongLat()){
			double insumoProm=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;
			ancho=labor.getConfigLabor().getAnchoGrilla();
			distancia=ancho;

			GeometryFactory fact = intersections.get(0).getFactory();
			Geometry[] geomArray = new Geometry[intersections.size()];
			GeometryCollection colectionCat = fact.createGeometryCollection(intersections.toArray(geomArray));

			try{
				union = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
			}catch(Exception e){	
				System.err.println("fallo convexHull de collectionCat");
				e.printStackTrace();
				return null;
			}


			double areaPoly = union.getArea();//areaPoly es el area de la union de las intersecciones
			double sumAreaInteseccion =0.0;
			for(FertilizacionItem fPoly : areasIntersecciones.keySet()){
				Double areaInterseccion = areasIntersecciones.get(fPoly);//cPoly.getGeometry();
				sumAreaInteseccion+=areaInterseccion;
				if(areaInterseccion==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = areaInterseccion/areaPoly;//al dividir por el area del poligono en vez del area de la interseccion saco la suma en vez del promedio
				insumoProm+=fPoly.getDosistHa()*peso;
				elev+=fPoly.getElevacion()*areaInterseccion;//la elevacion debe ser el promedio no la suma
			}
			elev=elev/sumAreaInteseccion;

			//	System.out.println("pesos = "+pesos);
			synchronized(labor){
				f = new FertilizacionItem();
				f.setId(labor.getNextID());
				labor.setPropiedadesLabor(f);
			}



			f.setGeometry(union);
			f.setDosistHa(insumoProm);
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
