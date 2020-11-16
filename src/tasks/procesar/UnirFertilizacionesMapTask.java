package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
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
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.nww.LaborLayer;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

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
		//TODO asignar las columnas a  los valores estanar
		labor.colAmount.set(FertilizacionLabor.COLUMNA_KG_HA);
		labor.colKgHaProperty.set(FertilizacionLabor.COLUMNA_KG_HA);
		labor.colAncho.set(FertilizacionLabor.COLUMNA_ANCHO);
		labor.colCurso.set(FertilizacionLabor.COLUMNA_CURSO);
		labor.colDistancia.set(FertilizacionLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(FertilizacionLabor.COLUMNA_ELEVACION);
		//labor.colVelocidad.set(CosechaLabor.COLUMNA_VELOCIDAD);
		//labor.colPasada.set(CosechaLabor.COLUMNA_ANCHO);

	//	labor.getConfiguracion().valorMetrosPorUnidadDistanciaProperty().set(1.0);
	//	labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		String nombreProgressBar = "unir fertilizaciones";
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
		String prefijo = "clon";
		if(fertilizaciones.size()>1){
			prefijo = "union";
		}
		int featuresInsertadas=0;
		for(FertilizacionLabor fert:fertilizaciones){
			if(nombre == null){
				nombre=prefijo+" "+fert.getNombre();	
			}else {
				nombre+=" - "+fert.getNombre();
			}
			if(labor.fertilizanteProperty.getValue()==null){//inicializo las propiedades con los valores de la primera fert unida
				//esto no se corre porque en el constructor se inicializa con los valores default
				System.out.println("inicializando las variables de la nueva fertilizacion con los de la primera fert a unir");
				labor.fertilizanteProperty.setValue(fert.fertilizanteProperty.getValue());
				labor.setPrecioInsumo(fert.getPrecioInsumo());
				labor.fechaProperty.setValue(fert.fechaProperty.getValue());
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
							List<FertilizacionItem>  cosechasPoly = fertilizaciones.parallelStream().collect(
									()->new  ArrayList<FertilizacionItem>(),
									(list, cosecha) ->{			
										list.addAll(cosecha.cachedOutStoreQuery(poly.getEnvelopeInternal()));	
									},
									(list1, list2) -> list1.addAll(list2)
									);

							FertilizacionItem item = construirFeature(cosechasPoly,poly);                    			

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

		//TODO 4 mostrar la cosecha sintetica creada
		labor.constructClasificador();
		
		/*fin de grillar fertilizacion*/
		
	
	



		runLater(byPolygon.values());
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde "+time+" milisegundos en unir las fertilizaciones.");
	}

	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {

		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$

		String tooltipText = new String(// TODO ver si se puede instalar un
				// boton
				// que permita editar el dato
				"Densidad: " + df.format(fertFeature.getDosistHa())
				+ " Kg/Ha\n" + "Costo: "
				+ df.format(fertFeature.getImporteHa()) + " U$S/Ha\n"
				//+ "Sup: "
				//+ df.format(area * ProyectionConstants.METROS2_POR_HA)
				//+ " m2\n"
				// +"feature: " + featureNumber
				);
		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		//List  paths = 
		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);

		//return null;
	}

	/**
	 * 
	 * @param fertilizacionesPoly
	 * @param poly
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private FertilizacionItem construirFeature(List<FertilizacionItem> fertilizacionesPoly,
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
				g= EnhancedPrecisionOp.intersection(poly,g);
				Double areaInterseccion = g.getArea();
				sumAreaInterseccion+=areaInterseccion;
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
			double insumoProm=0,desvioPromedio=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;
			ancho=labor.getConfigLabor().getAnchoGrilla();
			distancia=ancho;
		

			double areaPoly = poly.getArea();
		for(FertilizacionItem fPoly : areasIntersecciones.keySet()){
				Double areaInterseccion = areasIntersecciones.get(fPoly);//cPoly.getGeometry();
				if(areaInterseccion==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = areaInterseccion/areaPoly;//al dividir por el area del poligono en vez del area de la interseccion saco la suma en vez del promedio
				insumoProm+=fPoly.getDosistHa()*peso;
				elev+=fPoly.getElevacion()*peso;
			}
			
			//	System.out.println("pesos = "+pesos);
			synchronized(labor){
				f = new FertilizacionItem();
				f.setId(labor.getNextID());
				labor.setPropiedadesLabor(f);
			}


			GeometryFactory fact = intersections.get(0).getFactory();
			Geometry[] geomArray = new Geometry[intersections.size()];
			GeometryCollection colectionCat = fact.createGeometryCollection(intersections.toArray(geomArray));
			
			try{
				union = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
				}catch(Exception e){			}
			
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
		return labor.config.supMinimaProperty().doubleValue()*ProyectionConstants.metersToLong()*ProyectionConstants.metersToLat();
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
