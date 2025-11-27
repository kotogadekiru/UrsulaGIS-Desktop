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
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.margen.MargenItem;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;

public class SumarMargenesMapTask extends ProcessMapTask<MargenItem,Margen> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<Margen> margenes;
	private boolean rellenarHuecos = false;
			
	public SumarMargenesMapTask(List<Margen> _margenes){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.margenes=_margenes;
		for(Margen l:margenes){
			l.getLayer().setEnabled(false);
		};
		super.labor = new Margen();
		//TODO asignar las columnas a  los valores estandar
//		labor.colAmount.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
//		labor.colRendimiento.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
//		labor.colAncho.set(CosechaLabor.COLUMNA_ANCHO);
//		labor.colCurso.set(CosechaLabor.COLUMNA_CURSO);
//		labor.colDistancia.set(CosechaLabor.COLUMNA_DISTANCIA);
//		labor.colElevacion.set(CosechaLabor.COLUMNA_ELEVACION);

		
//		CosechaConfig cConfig= labor.getConfiguracion();
//
//		cConfig.valorMetrosPorUnidadDistanciaProperty().set(1.0);
//		cConfig.correccionFlowToRindeProperty().setValue(false);
//		
//		cConfig.correccionDistanciaProperty().set(false);
//		cConfig.correccionAnchoProperty().set(false);
//		cConfig.correccionSuperposicionProperty().set(false);
//		cConfig.correccionDemoraPesadaProperty().set(false);
//		
//		cConfig.calibrarRindeProperty().set(false);
//	
//		cConfig.resumirGeometriasProperty().setValue(false);
//		
//		cConfig.correccionOutlayersProperty().set(false);	
//		cConfig.supMinimaProperty().set(0);
		
		labor.setNombre(Messages.getString("SumarMargenesMapTask.suma"));//este es el nombre que se muestra en el progressbar //$NON-NLS-1$
	}
	


	/**
	 * proceso que toma una lista de cosechas y las une 
	 * con una grilla promediando los valores de acuerdo a su promedio ponderado por la superficie
	 * superpuesta de cada item sobre la superficie superpuesta total de cada "pixel de la grilla"
	 */
	@Override
	protected void doProcess() throws IOException {
		long init = System.currentTimeMillis();
		// 1 obtener el bounds general que cubre a todas las cosechas
		ReferencedEnvelope unionEnvelope = null;
	
		String nombre =null;
		Cultivo cultivo =null;
		for(Margen c:margenes){
			//labor.precioGrano=c.precioGrano;
			//labor.costoCosechaTn=c.costoCosechaTn;
			labor.setFecha(c.getFecha());
			labor.precioInsumo=c.precioInsumo;
			labor.precioLabor=c.precioLabor;
			
//			labor.minRindeProperty.set(Math.min(labor.minRindeProperty.get(), c.minRindeProperty.get()));
//			labor.maxRindeProperty.set(Math.max(labor.maxRindeProperty.get(), c.maxRindeProperty.get()));
//			if(cultivo==null){
//				cultivo=c.getCultivo();//.getValue();
//			}
			if(nombre == null){
				nombre=labor.getNombre()+Messages.getString("GrillarCosechasMapTask.1")+c.getNombre();	 //$NON-NLS-1$
			}else {
				nombre+=Messages.getString("GrillarCosechasMapTask.2")+c.getNombre(); //$NON-NLS-1$
			}

			ReferencedEnvelope b = c.outCollection.getBounds();
			if(unionEnvelope==null){
				unionEnvelope=b;
			}else{
				unionEnvelope.expandToInclude(b);
			}
		}
	//	labor.setCultivo(cultivo);
		labor.setNombre(nombre);
		labor.setLayer(new LaborLayer());
		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, labor.getConfig().getAnchoGrilla());

		System.out.println(Messages.getString("GrillarCosechasMapTask.3")+grilla.size()+Messages.getString("GrillarCosechasMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
		// 3 recorrer cada pixel de la grilla sumando los valores y generando los nuevos items de la cosecha

		featureCount = grilla.size();

		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		
		ConcurrentMap<Polygon,MargenItem > byPolygon =
				grilla.parallelStream().collect(
						() -> new  ConcurrentHashMap< Polygon,MargenItem>(),
						(map, poly) -> {
							
						try{
							List<MargenItem>  cosechasPoly = margenes.parallelStream().collect(
									()->new  ArrayList<MargenItem>(),
									(list, cosecha) ->{			
										list.addAll(cosecha.cachedOutStoreQuery(poly.getEnvelopeInternal()));	
									},
									(list1, list2) -> list1.addAll(list2)
									);

							MargenItem item = construirFeature(cosechasPoly,poly);                    			

							if(item!=null){
								map.put(poly,item);
								
								SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
										labor.getType());
								SimpleFeature f = item.getFeature(fBuilder);
								if(f!=null){		
										boolean res = features.add(f);
										if(!res){
											System.out.println(Messages.getString("GrillarCosechasMapTask.5")+f); //$NON-NLS-1$
										}		
								}
							}
							this.featureNumber++;
							updateProgress( this.featureNumber, featureCount);

						}catch(Exception e){
							System.err.println(Messages.getString("GrillarCosechasMapTask.6")); //$NON-NLS-1$
							e.printStackTrace();
						}
						},
						(map1, map2) -> map1.putAll(map2)
						// putAll reemplaza los valores de map1 con los de map 2 si los poligonos coinciden
						// pero no deberia haber poligonos que coincidan.
						);
		//Limpio la cache de las labores despues de hacer las querys
		for(Margen c:margenes){
			c.clearCache();
		}
		System.out.println(Messages.getString("GrillarCosechasMapTask.7")+byPolygon.size()+Messages.getString("GrillarCosechasMapTask.8")); //$NON-NLS-1$ //$NON-NLS-2$

//FIXME esto hace que la grilla no tenga memoria
		if(labor.inCollection == null){
			labor.inCollection = new DefaultFeatureCollection(Messages.getString("GrillarCosechasMapTask.9"),labor.getType()); //$NON-NLS-1$
		}
		labor.inCollection.addAll(features);
		boolean ret= labor.outCollection.addAll(features);
		if(!ret){//XXX si esto falla es probablemente porque se estan creando mas de una feature con el mismo id
			System.out.println(Messages.getString("GrillarCosechasMapTask.10"));
		}

		//TODO 4 mostrar la cosecha sintetica creada
		labor.constructClasificador();

		runLater(byPolygon.values());
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println(Messages.getString("GrillarCosechasMapTask.11")+time+Messages.getString("GrillarCosechasMapTask.12")+time/featureCount+Messages.getString("GrillarCosechasMapTask.13")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * 
	 * @param cosechasPoly lista de cosechasItems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private MargenItem construirFeature(List<MargenItem> cosechasPoly, Polygon poly) {
		if(cosechasPoly.size()<1){
			return null;
		}
		
		List<Geometry> intersections = new ArrayList<Geometry>();
		// sumar todas las supferficies,
		//y calcular el promedio ponderado de
		// cada una de las variables por la superficie superpuesta
		//Geometry union = null;
		//double areaItersectadaTotal = 0;
		Map<MargenItem,Double> areasIntersecciones = new HashMap<MargenItem,Double>();
		for(MargenItem cPoly : cosechasPoly){	//de cada cosecha obtengo que area se intersecta con la query		
			//XXX si es una cosecha de ambientes el area es importante
			Geometry g = cPoly.getGeometry();
			try{				
				g= GeometryHelper.getIntersection(poly, g);//EnhancedPrecisionOp.intersection(poly,g);
				Double areaInterseccion = g.getArea();
			//	areaItersectadaTotal+=areaInterseccion;
				areasIntersecciones.put(cPoly,areaInterseccion);
				intersections.add(g);			
			}catch(Exception e){
				System.err.println(Messages.getString("GrillarCosechasMapTask.14")+poly+Messages.getString("GrillarCosechasMapTask.15")+g); //$NON-NLS-1$ //$NON-NLS-2$
			}		
		}
		Geometry union2 = null;
		if(!rellenarHuecos) {				
		try{
			GeometryCollection colectionCat = GeometryHelper.toGeometryCollection(intersections);
			union2 = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
			}catch(Exception e){

			}
		} else { 
			union2 = poly;
		}

		

	//	if(true) {//areaItersectadaTotal>getAreaMinimaLongLat()){
			 Double importePulvHa =0.0;
			 Double importeFertHa =0.0;
			 Double importeSiembraHa =0.0;
			 Double importeCosechaHa=0.0;
			
			
			 Double elev =0.0;

			 Double margenPorHa =0.0;
			 Double costoFijoPorHa=0.0;
			//double rindeProm=0,desvioPromedio=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;
			//TODO usar las propiedades de MargenItem
//			ancho=labor.getConfig().getAnchoGrilla();
//			distancia=ancho;

		for(MargenItem cPoly : areasIntersecciones.keySet()){
				Double gArea = areasIntersecciones.get(cPoly);//cPoly.getGeometry();
				if(gArea==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = gArea;

				importePulvHa+=cPoly.getImportePulvHa()*peso;
				importeFertHa+=cPoly.getImporteFertHa()*peso;
				importeSiembraHa+=cPoly.getImporteSiembraHa()*peso;
				importeCosechaHa+=cPoly.getImporteCosechaHa()*peso;
				margenPorHa+=cPoly.getMargenPorHa()*peso;
				costoFijoPorHa+=cPoly.getCostoFijoPorHa()*peso;
				

				elev+=cPoly.getElevacion()*peso;
			}
			

			MargenItem c = null;
			synchronized(labor){
				c = new MargenItem();
				c.setId(labor.getNextID());
				labor.setPropiedadesLabor(c);
			}
			double area = union2.getArea();
			
			c.setImportePulvHa(importePulvHa/area);
			c.setImporteFertHa(importeFertHa/area);
			c.setImporteSiembraHa(importeSiembraHa/area);
			c.setImporteCosechaHa(importeCosechaHa/area);
			c.setMargenPorHa(margenPorHa/area);
			c.setCostoFijoPorHa(costoFijoPorHa/area);
			
			c.setGeometry(union2);
			c.setElevacion(elev/area);
		//}
		return c;
	}
	

	public void setRellenarHuecos(boolean rellenar) {
		this.rellenarHuecos=rellenar;
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
