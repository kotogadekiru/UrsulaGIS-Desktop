package com.ursulagis.desktop.tasks.procesar;

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
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.geometry.Position;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class GrillarCosechasMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<CosechaLabor> cosechas;
	private boolean rellenarHuecos = false;
	private double ancho=10;

			
	public GrillarCosechasMapTask(List<CosechaLabor> cosechas){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.cosechas=cosechas;

		super.labor = new CosechaLabor();
		//TODO asignar las columnas a  los valores estanar
		labor.colAmount.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
		labor.colRendimiento.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
		labor.colAncho.set(CosechaLabor.COLUMNA_ANCHO);
		labor.colCurso.set(CosechaLabor.COLUMNA_CURSO);
		labor.colDistancia.set(CosechaLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(CosechaLabor.COLUMNA_ELEVACION);
		//labor.colVelocidad.set(CosechaLabor.COLUMNA_VELOCIDAD);
		//labor.colPasada.set(CosechaLabor.COLUMNA_ANCHO);
		
		CosechaConfig cConfig= labor.getConfiguracion();

		cConfig.valorMetrosPorUnidadDistanciaProperty().set(1.0);
		cConfig.correccionFlowToRindeProperty().setValue(false);
		
		cConfig.correccionDistanciaProperty().set(false);
		cConfig.correccionAnchoProperty().set(false);
		cConfig.correccionSuperposicionProperty().set(false);
		cConfig.correccionDemoraPesadaProperty().set(false);
		
		cConfig.calibrarRindeProperty().set(false);
	
		cConfig.resumirGeometriasProperty().setValue(false);
		
		cConfig.correccionOutlayersProperty().set(false);	
		cConfig.supMinimaProperty().set(0);
		
		labor.setNombre(Messages.getString("GrillarCosechasMapTask.0"));//este es el nombre que se muestra en el progressbar //$NON-NLS-1$
	}
	
	public void setAncho(double _ancho) {
		this.ancho=_ancho;
	}

	/**
	 * proceso que toma una lista de cosechas y las une 
	 * con una grilla promediando los valores de acuerdo a su promedio ponderado por la superficie
	 * superpuesta de cada item sobre la superficie superpuesta total de cada "pixel de la grilla"
	 */
	@Override
	protected void doProcess() throws IOException {
		long init = System.currentTimeMillis();
		// TODO 1 obtener el bounds general que cubre a todas las cosechas
		ReferencedEnvelope unionEnvelope = null;
		double ancho = this.ancho;//labor.getConfiguracion().getAnchoGrilla();
		String nombre =null;
		Cultivo cultivo =null;
		for(CosechaLabor c:cosechas){
			//labor.precioGrano=c.precioGrano;
			labor.costoCosechaTn=c.costoCosechaTn;
			labor.setFecha(c.getFecha());//fechaProperty.setValue(c.fechaProperty.getValue());
			labor.precioInsumo=c.precioInsumo;
			labor.precioLabor=c.precioLabor;
			
			labor.minRindeProperty.set(Math.min(labor.minRindeProperty.get(), c.minRindeProperty.get()));
			labor.maxRindeProperty.set(Math.max(labor.maxRindeProperty.get(), c.maxRindeProperty.get()));
			if(cultivo==null){
				cultivo=c.getCultivo();//.getValue();
			}
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
		labor.setCultivo(cultivo);
		labor.setNombre(nombre);
		labor.setLayer(new LaborLayer());
		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = construirGrilla(unionEnvelope, ancho);
		//List<Polygon>  grilla = construirGrillaTriangular(unionEnvelope, ancho);
		//double elementos = grilla.size();
		System.out.println(Messages.getString("GrillarCosechasMapTask.3")+grilla.size()+Messages.getString("GrillarCosechasMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
		// 3 recorrer cada pixel de la grilla promediando los valores y generando los nuevos items de la cosecha

		featureCount = grilla.size();

		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		//TODO ver de recorrer por cosecha en vez de por poligono
		//usar un map de poligonos, cosechaItem e ir actualizandola segun la cosecha
		// espero con eso reducir un poco el costo de abrir tantas querys como poligonos.
		//FIXME si uso parallelStream soy mucho mas rapido pero al grabar pierdo features

		
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
								SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
										labor.getType());
								SimpleFeature f = item.getFeature(fBuilder);
								if(f!=null){
								//	synchronized(this){
										boolean res = features.add(f);
										if(!res){
											System.out.println(Messages.getString("GrillarCosechasMapTask.5")+f); //$NON-NLS-1$
										}
								//	}

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
		for(CosechaLabor c:cosechas){
			c.clearCache();
		//	c.cachedEnvelopes.clear();
		}

		//		grilla.parallelStream().forEach(poly->{ //tarde 242762 milisegundos en unir las cosechas. es 2.6908974017912564 milisegundos por poligono (pierdo poligonos)
		//		//grilla.stream().forEach(poly->{ //tarde 1137859 milisegundos en unir las cosechas. es 12.612607519730425 milisegundos por poligono
		//			List<CosechaItem> cosechasPoly = Collections.synchronizedList(new ArrayList<CosechaItem>());
		//		
		//			cosechas.parallelStream().forEach(c->{			
		//					cosechasPoly.addAll(c.outStoreQuery(poly.getEnvelopeInternal()));	
		//			});
		//			CosechaItem item = construirFeature(cosechasPoly,poly);
		//			
		//		
		//			if(item!=null){
		//				items.add(item);
		//				SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
		//				labor.getType());
		//				SimpleFeature f = item.getFeature(fBuilder);
		//				if(f!=null){
		//					synchronized(this){
		//					boolean res = features.add(f);
		//					if(!res){
		//						System.out.println("no se pudo agregar la feature "+f);
		//					}
		//					}
		//					
		//				}
		//			}
		//			this.featureNumber++;
		//			updateProgress( this.featureNumber, featureCount);
		//		});

		System.out.println(Messages.getString("GrillarCosechasMapTask.7")+byPolygon.size()+Messages.getString("GrillarCosechasMapTask.8")); //$NON-NLS-1$ //$NON-NLS-2$

//FIXME esto hace que la grilla no tenga memoria
		if(labor.inCollection == null){
			labor.inCollection = new DefaultFeatureCollection(Messages.getString("GrillarCosechasMapTask.9"),labor.getType()); //$NON-NLS-1$
		}
		labor.inCollection.addAll(features);
		boolean ret= labor.outCollection.addAll(features);
		if(!ret){//XXX si esto falla es provablemente porque se estan creando mas de una feature con el mismo id
			System.out.println(Messages.getString("GrillarCosechasMapTask.10")); //$NON-NLS-1$
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
	private CosechaItem construirFeature(List<CosechaItem> cosechasPoly, Polygon poly) {
		if(cosechasPoly.size()<1){
			return null;
		}
		
		List<Geometry> intersections = new ArrayList<Geometry>();
		// sumar todas las supferficies,
		//y calcular el promedio ponderado de
		// cada una de las variables por la superficie superpuesta
		//Geometry union = null;
		double areaItersectadaTotal = 0;
		Map<CosechaItem,Double> areasIntersecciones = new HashMap<CosechaItem,Double>();
		for(CosechaItem cPoly : cosechasPoly){	//de cada cosecha obtengo que area se intersecta con la query		
			//XXX si es una cosecha de ambientes el area es importante
			Geometry g = cPoly.getGeometry();
			try{				
				Geometry interseccion= GeometryHelper.getIntersection(poly, g);//EnhancedPrecisionOp.intersection(poly,g);
				Double areaInterseccion = interseccion.getArea();
				areaItersectadaTotal+=areaInterseccion;
				areasIntersecciones.put(cPoly,areaInterseccion);
				intersections.add(interseccion);			
			}catch(Exception e){
				System.err.println("fallo la interseccion entre "+poly+" y "+g); //$NON-NLS-1$ error interno no traducir
			}		
		}

		CosechaItem c = null;

		if(areaItersectadaTotal>getAreaMinimaLongLat()){
			double rindeProm=0,desvioPromedio=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;
			ancho=labor.getConfiguracion().getAnchoGrilla();
			distancia=ancho;
//		if(this.rellenarHuecos) {//si se indico rellenar huecos hago la suma de kg en vez del promedio
//			areaPoly=poly.getArea();
//		}
	//		double pesoTotal=0;
		for(CosechaItem cPoly : areasIntersecciones.keySet()){
				Double gArea = areasIntersecciones.get(cPoly);//cPoly.getGeometry();
				if(gArea==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = gArea/areaItersectadaTotal;
		//		pesoTotal+=peso;
				rindeProm+=cPoly.getRindeTnHa()*peso;
				//ancho+=cPoly.getAncho()*peso;
				//distancia+=cPoly.getDistancia()*peso;
				elev+=cPoly.getElevacion()*peso;
				//rumbo+=cPoly.getRumbo()*peso;
			}
			
//		if(pesoTotal!=1){
//			System.err.println("la suma de los pesos en la ponderacion de la grilla no es 1, es "+pesoTotal);
		//la suma de los pesos en la ponderacion de la grilla no es 1, es 1.0000000000000002
//		}
//		cosechasPoly.parallelStream().collect(Collectors.summingDouble((cosecha)->{
//			return Math.abs(rindeProm- cosecha.getAmount());
//		}));
			double sumaDesvio2 = 0.0;
			for(CosechaItem cosecha : areasIntersecciones.keySet()){
				double cantidadCosecha = cosecha.getAmount();				
				sumaDesvio2+= Math.abs(rindeProm- cantidadCosecha);
			}
			desvioPromedio = Math.sqrt(sumaDesvio2)/areasIntersecciones.keySet().size();
			

			//	System.out.println("pesos = "+pesos);
			synchronized(labor){
				c = new CosechaItem();
				c.setId(labor.getNextID());
				labor.setPropiedadesLabor(c);
			}
//			for(Coordinate coord : poly.getCoordinates()){
//				coord.z=elev;
//			}

//			GeometryFactory fact = intersections.get(0).getFactory();
//			Geometry[] geomArray = new Geometry[intersections.size()];
//			GeometryCollection colectionCat = fact.createGeometryCollection(intersections.toArray(geomArray));

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
			
			c.setGeometry(union2);
			c.setRindeTnHa(rindeProm);
			c.setDesvioRinde(desvioPromedio);
			c.setAncho(ancho);
			c.setDistancia(distancia);
			c.setElevacion(elev);
			c.setRumbo(rumbo);
			//simpleFeature = c.getFeature(fBuilder);
		}
		return c;
	}

	private double getDesvio2(double promedio, double cantidad) {
		return Math.pow(cantidad-promedio,2);

	}
	private double getAreaMinimaLongLat() {
		return labor.getConfiguracion().supMinimaProperty().doubleValue()
				*ProyectionConstants.metersToLong()
				*ProyectionConstants.metersToLat();
	}

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	public static List<Polygon> construirGrilla(BoundingBox bounds,double ancho) {
		//System.out.println(Messages.getString("GrillarCosechasMapTask.16")); //$NON-NLS-1$
		List<Polygon> polygons = new ArrayList<Polygon>();
		Position esq = bounds.getUpperCorner();
		//System.out.println("esq.getOrdinate(1) "+esq.getOrdinate(1));//esq.getOrdinate(0) -61.9547387
		ProyectionConstants.setLatitudCalculo(esq.getOrdinate(1));
		
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
	
	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	private List<Polygon> construirGrillaTriangular(BoundingBox bounds,double ancho) {
		System.out.println(Messages.getString("GrillarCosechasMapTask.17")); //$NON-NLS-1$
		List<Polygon> polygons = new ArrayList<Polygon>();
		Double sen60 = Math.sin(Math.toRadians(60));
		Double h=ancho*sen60;
		
		System.out.println(Messages.getString("GrillarCosechasMapTask.18")+ancho+Messages.getString("GrillarCosechasMapTask.19")+h); //$NON-NLS-1$ //$NON-NLS-2$
		//convierte los bounds de longlat a metros
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong() - ancho/2;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat() - h/2;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong() + ancho/2;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat() + h/2;
		Double x0=minX;
		
		GeometryFactory fact = new GeometryFactory();
		for(int x=0;(x0)<maxX;x++){
			x0=minX+x*ancho;
			for(int y=0;(minY+y*h*2)<maxY;y++){
				Double y0=minY+y*2*h;

				Coordinate C1=new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
				Coordinate C2=new Coordinate((x0+ancho)*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
				Coordinate C3=new Coordinate((x0+3*ancho/2)*ProyectionConstants.metersToLong(), (y0+h)*ProyectionConstants.metersToLat());
				Coordinate C4=new Coordinate((x0+ancho)*ProyectionConstants.metersToLong(), (y0+2*h)*ProyectionConstants.metersToLat()); 
				Coordinate C5=new Coordinate(x0*ProyectionConstants.metersToLong(), (y0+2*h)*ProyectionConstants.metersToLat());
				Coordinate C6=new Coordinate((x0+ancho/2)*ProyectionConstants.metersToLong(), (y0+h)*ProyectionConstants.metersToLat()); 
		
				polygons.add(constructTriangle(C1,C2,C6,fact));//UL
				polygons.add(constructTriangle(C6,C2,C3,fact));//UR
				polygons.add(constructTriangle(C6,C3,C4,fact));//DL
				polygons.add(constructTriangle(C6,C4,C5,fact));//DR
			}
		}
		return polygons;
	}
	
	private Polygon constructTriangle(Coordinate A,Coordinate B, Coordinate C,GeometryFactory fact){
		Coordinate[] coordinates = { A, B, C, A };// Tiene que ser cerrado.
		LinearRing shell = fact.createLinearRing(coordinates);
		LinearRing[] holes = null;
		return new Polygon(shell, holes, fact);
	}
	
	
//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//	}
	
//	@Override
//	protected void getPathTooltip(Geometry poly,
//			CosechaItem cosechaFeature) {
//		//	System.out.println("getPathTooltip(); "+System.currentTimeMillis());
//	//	List<gov.nasa.worldwind.render.Polygon>  paths = super.getPathFromGeom2D(poly, cosechaFeature);
//		//ExtrudedPolygon  path = super.getPathFromGeom2D(poly, cosechaFeature);
//
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
//		DecimalFormat df = new DecimalFormat("0.00");
//
//		String tooltipText = new String("Rinde: "
//				+ df.format(cosechaFeature.getAmount()) + " Tn/Ha\n"
//				//	+ "Area: "+ df.format(area * ProyectionConstants.METROS2_POR_HA)+ " m2\n" + 
//
//				);
//
//		if(area<1){
//			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
//			tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
//		} else {
//			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
//		}
//
//		//tooltipText=tooltipText.concat("Pasada: "+df.format(cosechaFeature.getPasada() ) + "\n");
//		tooltipText=tooltipText.concat("feature: "+cosechaFeature.getId() + "\n");
//
//		super.getExrudedPolygonFromGeom(poly, cosechaFeature,tooltipText);
//	}
	
	public void setRellenarHuecos(boolean rellenar) {
		this.rellenarHuecos=rellenar;
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
