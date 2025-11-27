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

import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class SumarCosechasMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<CosechaLabor> cosechas;

	public SumarCosechasMapTask(List<CosechaLabor> cosechas){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.cosechas=cosechas;
		for(CosechaLabor l:cosechas){
			l.getLayer().setEnabled(false);
//			if(l.getLayer().isEnabled()){
//				this.cosechas.add(l);
//				
//			}
		};

		super.labor = new CosechaLabor();
		//asignar las columnas a  los valores estandar
		//labor.colAmount.set(CosechaLabor.);

		labor.colAncho.set(CosechaLabor.COLUMNA_ANCHO);
		labor.colCurso.set(CosechaLabor.COLUMNA_CURSO);
		labor.colDistancia.set(CosechaLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(CosechaLabor.COLUMNA_ELEVACION);

		String nombreProgressBar = "sumar cosechas";
		if(cosechas.size()>1){
			nombreProgressBar = "sumar cosechas";
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
		if(cosechas.size()>1){
			prefijo = "suma";
		}
//		int featuresInsertadas=0;
		for(CosechaLabor fert:cosechas){
			if(nombre == null){
				nombre=prefijo+" "+fert.getNombre();	
			}else {
				nombre+=" - "+fert.getNombre();
			}
			if(labor.getCultivo()==null){//inicializo las propiedades con los valores de la primera fert unida
				//esto no se corre porque en el constructor se inicializa con los valores default
				System.out.println("inicializando las variables de la nueva fertilizacion con los de la primera fert a unir");
				labor.setCultivo(fert.getCultivo());
				labor.setPrecioInsumo(fert.getPrecioInsumo());
				labor.setFecha(fert.getFecha());
				labor.setPrecioLabor(fert.getPrecioLabor());
			}
//			FeatureReader<SimpleFeatureType, SimpleFeature> reader = fert.outCollection.reader();
//			while(reader.hasNext()){
//				SimpleFeature f = reader.next();
//				CosechaItem ci = labor.constructFeatureContainerStandar(f,true);
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
		
		ConcurrentMap<Polygon,CosechaItem > byPolygon =
				grilla.parallelStream().collect(
						() -> new  ConcurrentHashMap< Polygon,CosechaItem>(),
						(map, poly) -> {
							
						try{
							List<CosechaItem>  cosechasPoly = cosechas.parallelStream().collect(
									()->new  ArrayList<CosechaItem>(),
									(list, cosecha) ->{			
										list.addAll(cosecha.cachedOutStoreQuery(poly.getEnvelopeInternal()));	
									},
									(list1, list2) -> list1.addAll(list2)
									);

							CosechaItem item = construirFeature(cosechasPoly,poly);                    			

							if(item!=null){
								map.put(poly,item);
								//	items.add(item);
								SimpleFeatureBuilder fBuilder = labor.getFeatureBuilder();								
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
		
		for(CosechaLabor l:cosechas){
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
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, CosechaItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	/**
	 * 
	 * @param fertilizacionesPoly
	 * @param poly
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private CosechaItem construirFeature(List<CosechaItem> fertilizacionesPoly,
			Polygon poly) {
		if(fertilizacionesPoly.size()<1){
			return null;
		}
		
		List<Geometry> intersections = new ArrayList<Geometry>();
		// sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
		Geometry union = null;
		double sumAreaInterseccion = 0;
		Map<CosechaItem,Double> areasIntersecciones = new HashMap<CosechaItem,Double>();
		for(CosechaItem fPoly : fertilizacionesPoly){			
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

		CosechaItem c = null;

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
		for(CosechaItem fPoly : areasIntersecciones.keySet()){
				Double areaInterseccion = areasIntersecciones.get(fPoly);//cPoly.getGeometry();
				if(areaInterseccion==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = areaInterseccion/areaPoly;//al dividir por el area del poligono en vez del area de la interseccion saco la suma en vez del promedio
				insumoProm+=fPoly.getAmount()*peso;
				elev+=fPoly.getElevacion()*peso;
			}
			
			synchronized(labor){
				c = new CosechaItem();
				c.setId(labor.getNextID());
				labor.setPropiedadesLabor(c);
			}			
			c.setGeometry(union);
			c.setRindeTnHa(insumoProm);
			c.setAncho(ancho);
			c.setDistancia(distancia);
			c.setElevacion(elev);
			c.setRumbo(rumbo);
		}
		return c;
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
