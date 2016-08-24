package tasks;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

import utils.ProyectionConstants;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import dao.CosechaItem;
import dao.CosechaLabor;

public class GrillarCosechasMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<CosechaLabor> cosechas;

			
	public GrillarCosechasMapTask(List<CosechaLabor> cosechas){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.cosechas=new ArrayList<CosechaLabor>();
		for(CosechaLabor l:cosechas){
			if(l.getLayer().isEnabled()){
				this.cosechas.add(l);
			}
		};

		super.labor = new CosechaLabor();
		//TODO asignar las columnas a  los valores estanar
		labor.colAmount.set(CosechaLabor.COLUMNA_RENDIMIENTO);
		labor.colRendimiento.set(CosechaLabor.COLUMNA_RENDIMIENTO);
		labor.colAncho.set(CosechaLabor.COLUMNA_ANCHO);
		labor.colCurso.set(CosechaLabor.COLUMNA_CURSO);
		labor.colDistancia.set(CosechaLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(CosechaLabor.COLUMNA_ELEVACION);
		labor.colVelocidad.set(CosechaLabor.COLUMNA_VELOCIDAD);
		//labor.colPasada.set(CosechaLabor.COLUMNA_ANCHO);

		labor.getConfiguracion().valorMetrosPorUnidadDistanciaProperty().set(1.0);
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);

		labor.getNombreProperty().setValue("grilla cosechas");//este es el nombre que se muestra en el progressbar
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
		double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();
		String nombre =null;
		for(CosechaLabor c:cosechas){
			if(nombre == null){
				nombre="grilla "+c.getNombreProperty().get();	
			}else {
				nombre+=" - "+c.getNombreProperty().get();
			}

			ReferencedEnvelope b = c.outCollection.getBounds();
			if(unionEnvelope==null){
				unionEnvelope=b;
			}else{
				unionEnvelope.expandToInclude(b);
			}
		}

		labor.nombreProperty.set(nombre);
		labor.setLayer(new RenderableLayer());
		//TODO 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = construirGrilla(unionEnvelope, ancho);
		double elementos = grilla.size();
		System.out.println("creando una grilla con "+grilla.size()+" elementos");
		//TODO 3 recorrer cada pixel de la grilla promediando los valores y generando los nuevos items de la cosecha

		featureCount = grilla.size();

		//	CopyOnWriteArrayList<SimpleFeature> features = new CopyOnWriteArrayList<SimpleFeature>();
		//CopyOnWriteArrayList<CosechaItem> items = new CopyOnWriteArrayList<CosechaItem>();
		//	 List<CosechaItem> items = Collections.synchronizedList(new ArrayList<CosechaItem>());
		List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
		//TODO ver de recorrer por cosecha en vez de por poligono
		//usar un map de poligonos, cosechaItem e ir actualizandola segun la cosecha
		// espero con eso reducir un poco el costo de abrir tantas querys como poligonos.
		//FIXME si uso parallelStream soy mucho mas rapido pero al grabar pierdo features

		ConcurrentMap<Polygon,CosechaItem > byPolygon =
				grilla.parallelStream().collect(() -> new  ConcurrentHashMap< Polygon,CosechaItem>(),
						(map, poly) -> {
							List<CosechaItem>  cosechasPoly = cosechas.parallelStream().collect(
									()->new  ArrayList<CosechaItem>(),
									(list, cosecha) ->{			
										list.addAll(cosecha.outStoreQuery(poly.getEnvelopeInternal()));	
									},
									(list1, list2) -> list1.addAll(list2));

							CosechaItem item = construirFeature(cosechasPoly,poly);                    			

							if(item!=null){
								map.put(poly,item);
								//	items.add(item);
								SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
										labor.getType());
								SimpleFeature f = item.getFeature(fBuilder);
								if(f!=null){
									synchronized(this){
										boolean res = features.add(f);
										if(!res){
											System.out.println("no se pudo agregar la feature "+f);
										}
									}

								}
							}
							this.featureNumber++;
							updateProgress( this.featureNumber, featureCount);


						},
						(map1, map2) -> map1.putAll(map2));

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

		System.out.println("cree una union de "+byPolygon.size()+" elementos");


		boolean ret = labor.outCollection.addAll(features);
		if(!ret){//XXX si esto falla es provablemente porque se estan creando mas de una feature con el mismo id
			System.out.println("no se pudieron agregar las features al outCollection");
		}

		//TODO 4 mostrar la cosecha sintetica creada
		labor.constructClasificador();

	//	this.pathTooltips.clear();

//		for(CosechaItem c:byPolygon.values()){
//			Geometry g = c.getGeometry();
//			if(g instanceof Polygon){
//
//				//pathTooltips.add(
//						getPathTooltip((Polygon)g,c);
//						//);	
//
//
//			} else if(g instanceof MultiPolygon){
//				MultiPolygon mp = (MultiPolygon)g;			
//				for(int i=0;i<mp.getNumGeometries();i++){
//					Polygon p = (Polygon) (mp).getGeometryN(i);
//					//pathTooltips.add(
//							getPathTooltip(p,c);
//							//);	
//				}
//
//			}
//		}
		runLater(byPolygon.values());
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde "+time+" milisegundos en unir las cosechas. es "+time/elementos+" milisegundos por poligono");
	}

	/**
	 * 
	 * @param cosechasPoly
	 * @param poly
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private CosechaItem construirFeature(List<CosechaItem> cosechasPoly,
			Polygon poly) {
		if(cosechasPoly.size()<1){
			return null;
		}

		//TODO sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
		double areaPoly = 0;
		Map<CosechaItem,Double> intersecciones = new HashMap<CosechaItem,Double>();
		for(CosechaItem cPoly : cosechasPoly){
			Geometry g = cPoly.getGeometry();
			//XXX si es una cosecha de ambientes el area es importante
			//			try{
			//g= poly.intersection(g);
			Double areaPoly2 = g.getArea();
			areaPoly+=areaPoly2;
			intersecciones.put(cPoly,areaPoly2);
			//			}catch(Exception e){
			//				System.err.println("no se pudo hacer la interseccion entre\n"+poly+"\n y\n"+g);
			//			}
		}
		//	System.out.println("area superpuesta = "+areaPoly);
		//SimpleFeature simpleFeature=null;
		CosechaItem c = null;
		//		SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
		//					labor.getType());
		if(areaPoly>labor.config.supMinimaProperty().doubleValue()*(ProyectionConstants.metersToLong*ProyectionConstants.metersToLat)){
			double rinde=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;

			for(CosechaItem cPoly : cosechasPoly){
				//				Geometry g = cPoly.getGeometry();				
				//				g= poly.intersection(g);

				Double gArea = intersecciones.get(cPoly);//cPoly.getGeometry();
				if(gArea==null){
					//System.out.println("g es null asi que no lo incluyo en la suma "+cPoly);
					continue;}
				double peso = gArea/areaPoly;

				//	System.out.println("peso = "+peso);
				//	pesos+=peso;
				rinde+=cPoly.getRindeTnHa()*peso;
				ancho+=cPoly.getAncho()*peso;
				distancia+=cPoly.getDistancia()*peso;
				elev+=cPoly.getElevacion()*peso;
				rumbo+=cPoly.getRumbo()*peso;
			}

			//	System.out.println("pesos = "+pesos);
			synchronized(this){
				c = new CosechaItem();
				c.setId(labor.getNextID());
			}
			c.setGeometry(poly);
			c.setRindeTnHa(rinde);
			c.setAncho(ancho);
			c.setDistancia(distancia);
			c.setElevacion(elev);
			c.setRumbo(rumbo);
			//simpleFeature = c.getFeature(fBuilder);
		}
		return c;
	}

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	private List<Polygon> construirGrilla(BoundingBox bounds,double ancho) {
		System.out.println("construyendo grilla");
		List<Polygon> polygons = new ArrayList<Polygon>();
		//convierte los bounds de longlat a metros
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong - ancho/2;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat - ancho/2;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong + ancho/2;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat + ancho/2;
		Double x0=minX;
		for(int x=0;(x0)<maxX;x++){
			x0=minX+x*ancho;
			Double x1=minX+(x+1)*ancho;
			for(int y=0;(minY+y*ancho)<maxY;y++){
				Double y0=minY+y*ancho;
				Double y1=minY+(y+1)*ancho;


				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong, y0*ProyectionConstants.metersToLat); 
				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong, y0*ProyectionConstants.metersToLat);
				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong, y1*ProyectionConstants.metersToLat);
				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong, y1*ProyectionConstants.metersToLat);

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
	
	@Override
	protected void getPathTooltip(Geometry poly,
			CosechaItem cosechaFeature) {
		//	System.out.println("getPathTooltip(); "+System.currentTimeMillis());
	//	List<gov.nasa.worldwind.render.Polygon>  paths = super.getPathFromGeom2D(poly, cosechaFeature);
		//ExtrudedPolygon  path = super.getPathFromGeom2D(poly, cosechaFeature);

		double area = poly.getArea() * ProyectionConstants.A_HAS;// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("#.00");

		String tooltipText = new String("Rinde: "
				+ df.format(cosechaFeature.getAmount()) + " Tn/Ha\n"
				//	+ "Area: "+ df.format(area * ProyectionConstants.METROS2_POR_HA)+ " m2\n" + 

				);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
			tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		tooltipText=tooltipText.concat("Pasada: "+df.format(cosechaFeature.getPasada() ) + "\n");
		tooltipText=tooltipText.concat("feature: "+cosechaFeature.getId() + "\n");

		super.getPathFromGeom2D(poly, cosechaFeature,tooltipText);
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
