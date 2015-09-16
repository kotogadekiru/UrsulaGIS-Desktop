package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Path;

import org.geotools.data.FileDataStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Configuracion;
import dao.CosechaItem;

public class ProcessHarvestMapTask extends ProcessMapTask {

	private static final String CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA2 = "CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA";
	private static final String CANTIDAD_DISTANCIAS_TOLERANCIA2 = "CANTIDAD_DISTANCIAS_TOLERANCIA";
	private static final String CLASIFICADOR_JENKINS = "JENKINS";
	private static final String TIPO_CLASIFICADOR = "CLASIFICADOR";
	private static final String CORRIMIENTO_PESADA = "CORRIMIENTO_PESADA";
	
	private static  int CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA = new Integer(Configuracion.getInstance().getPropertyOrDefault(CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA2,"1"));//5
	private static  int CANTIDAD_DISTANCIAS_TOLERANCIA =new Integer(Configuracion.getInstance().getPropertyOrDefault(CANTIDAD_DISTANCIAS_TOLERANCIA2,"5"));//10
	
	private static  int N_VARIANZAS_TOLERA =new Integer(Configuracion.getInstance().getPropertyOrDefault("N_VARIANZAS_TOLERA","1")); //3;//9;
	private static  Double TOLERANCIA_CV =new Double(Configuracion.getInstance().getPropertyOrDefault("TOLERANCIA_CV_0-1","0.13")); //3;//9;
	Coordinate lastD = null, lastC = null;
	double maxX = 0, maxY = 0, minX = 0, minY = 0;// variables para llevar la
	// cuenta de donde estan los
	// puntos y hubicarlos en el
	// centro del panel map
	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	//Quadtree geometryTree = null;

	private double precioGrano;
	private Double oldPasada=null;
	

	

	public ProcessHarvestMapTask(Group map, FileDataStore store, double d, Double correccionRinde) {
		this.precioGrano = d;
		CosechaItem.setCorreccionRinde(correccionRinde);
		super.map = map;
		this.store = store;
		
		CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA = new Integer(Configuracion.getInstance().getPropertyOrDefault(CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA2,"1"));//5
		CANTIDAD_DISTANCIAS_TOLERANCIA =new Integer(Configuracion.getInstance().getPropertyOrDefault(CANTIDAD_DISTANCIAS_TOLERANCIA2,"5"));//10
		TOLERANCIA_CV =new Double(Configuracion.getInstance().getPropertyOrDefault("TOLERANCIA_CV_0-1","0.13")); //3;//9;
	}

	public void doProcess() throws IOException {
	//	System.out.println("doProcess(); "+System.currentTimeMillis());
		SimpleFeatureSource featureSource = store.getFeatureSource();

		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featuresIterator = featureCollection.features();

//		SimpleFeatureCollection destinationFeatures = new ListFeatureCollection(
//				featureCollection.getSchema());

		//this.geometryTree = new Quadtree();
		this.featureTree = new Quadtree();
	
	//lista de cosechas ordenadas por indice	
		List<CosechaItem> cosechaItemIndex = new ArrayList<CosechaItem>();
		//lista de cosechas ordenadas por rendimiento
	//	List<Cosecha> cosechaItemAmount = new ArrayList<Cosecha>();
		
		
		//convierto los features en cosechas
		while (featuresIterator.hasNext()) {
			SimpleFeature simpleFeature = featuresIterator.next();
			CosechaItem ci =  new CosechaItem(simpleFeature, precioGrano);
			cosechaItemIndex.add(ci);
			distanciaAvanceMax = Math.max(ci.getDistancia(), distanciaAvanceMax);//TODO pasar al while
			anchoMax = Math.max(ci.getAncho(), anchoMax);//TODO pasar al while
		}	

		featureCount = cosechaItemIndex.size();
		for(CosechaItem cosechaFeature : cosechaItemIndex){			

			featureNumber++;
			updateProgress(featureNumber, featureCount);
//			System.out.println("Feature " + featureNumber + " of "
//					+ featureCount);

			Double distancia = cosechaFeature.getDistancia();
			Double rumbo = cosechaFeature.getRumbo();
			Double ancho = cosechaFeature.getAncho();
			Double pasada = cosechaFeature.getPasada();
	
			Double alfa = rumbo * Math.PI / 180 + Math.PI / 2;

			if(HarvestFiltersConfig.getInstance().correccionAnchoEnabled()){			
			if (ancho < anchoMax) {			
				ancho = anchoMax;
			}
		}
			
			//distanciaAvanceMax = (distancia+ distanciaAvanceMax)/2;
			
			Object geometry = cosechaFeature.getGeometry();

			/**
			 * si la geometria es un point procedo a poligonizarla
			 */
			if (geometry instanceof Point) {
				Point point = (Point) geometry;

				// convierto el ancho y la distancia a verctores longLat poder
				// operar con la posicion del dato
				Coordinate anchoLongLat = new Coordinate(
						ProyectionConstants.metersToLongLat * ancho / 2
								* Math.sin(alfa),
						ProyectionConstants.metersToLongLat * ancho / 2
								* Math.cos(alfa));
				Coordinate distanciaLongLat = new Coordinate(
						ProyectionConstants.metersToLongLat * distancia / 2
								* Math.sin(alfa + Math.PI / 2),
						ProyectionConstants.metersToLongLat * distancia / 2
								* Math.cos(alfa + Math.PI / 2));
				
				String corrimiento= 	Configuracion.getInstance().getPropertyOrDefault(CORRIMIENTO_PESADA, "4");
				double corrimientoPesada = new Integer(corrimiento);//Funciona bien en el maiz 1011 de ep7B			
				
				Coordinate corrimientoLongLat = new Coordinate(
						ProyectionConstants.metersToLongLat * corrimientoPesada / 2
								* Math.sin(alfa + Math.PI / 2),
						ProyectionConstants.metersToLongLat * corrimientoPesada / 2
								* Math.cos(alfa + Math.PI / 2));
				
				if(HarvestFiltersConfig.getInstance().correccionDemoraPesadaEnabled()){
				// mover el punto 3.5 distancias hacia atras para compenzar el retraso de la pesada
					
			//	point = point.getFactory().createPoint(new Coordinate(point.getX()+corrimientoPesada*distanciaLongLat.x,point.getY()+corrimientoPesada*distanciaLongLat.y));
					point = point.getFactory().createPoint(new Coordinate(point.getX()-corrimientoLongLat.x,point.getY()-corrimientoLongLat.y));
				}
				
				/**
				 * creo la geometria que corresponde a la feature tomando en cuenta si esta activado el filtro de distancia y el de superposiciones
				 */				
				Geometry geom = createGeometryForHarvest(anchoLongLat,
						distanciaLongLat, point,pasada);				
				
				/**
				 * solo ingreso la cosecha al arbol si la geometria es valida
				 */
				if(!geom.isEmpty()
						&&geom.isValid()
						&&(geom.getArea()*ProyectionConstants.A_HAS>MINIMA_SUP_HAS)
						){
					cosechaFeature.setGeometry(geom);
					corregirRinde(cosechaFeature);
					featureTree.insert(geom.getEnvelopeInternal(), cosechaFeature);
				} else{
					System.out.println("no inserto el feature "+featureNumber+" porque tiene una geometria invalida "+cosechaFeature);
				}

			} else { // no es point. Estoy abriendo una cosecha de poligonos.
				List<Polygon> mp = getPolygons(cosechaFeature);
				Polygon p = mp.get(0);
				featureTree.insert(p.getEnvelopeInternal(), cosechaFeature);
			}

		}// fin del for que recorre las cosechas por indice
		
	//vuelvo a crearCosechaItemIndex respetando el indice
		cosechaItemIndex= featureTree.queryAll();
		//XXX esto lo puedo hacer despues de corregir outlayers solo una vex
		cosechaItemIndex.sort((CosechaItem a, CosechaItem b) -> (int) (a.getId() - b.getId()));
		
		
	if(HarvestFiltersConfig.getInstance().correccionOutlayersEnabled()){
		System.out.println("corriegiendo outlayers con CV Max "+TOLERANCIA_CV);
		corregirOutlayers(	cosechaItemIndex);
		
		cosechaItemIndex= featureTree.queryAll();
		cosechaItemIndex.sort((CosechaItem a, CosechaItem b) -> (int) (a.getId() - b.getId()));
	} else { 
		System.out.println("no corrijo outlayers");
	}
		
	
	//Vuelvo a regenerar las features para usar el clasificador Jenkins
	SimpleFeatureCollection collection = new ListFeatureCollection(CosechaItem.getType());
	SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
			CosechaItem.getType());
	for (CosechaItem cosecha : cosechaItemIndex) {
		SimpleFeature cosechaFeature = cosecha.getFeature(featureBuilder);
		collection.add(cosechaFeature);
	//	System.out.println("agregando a features "+rentaFeature);
	}
	clasifier = null;

	//XXX aca construyo el clasificador
	if(CLASIFICADOR_JENKINS.equalsIgnoreCase(Configuracion.getInstance().getPropertyOrDefault(TIPO_CLASIFICADOR, CLASIFICADOR_JENKINS))){
		constructJenksClasifier(collection,CosechaItem.COLUMNA_RENDIMIENTO);
	}
	if(clasifier == null ){
		System.out.println("no hay jenks Classifier falling back to histograma");
		constructHistogram(cosechaItemIndex);
	}
	
	this.pathTooltips.clear();
	cosechaItemIndex.forEach(c->{
		Geometry g = c.getGeometry();
		if(g instanceof Polygon){
			
			pathTooltips.add(getPathTooltip((Polygon)g,c));	
			
			
		} else if(g instanceof MultiPolygon){
			MultiPolygon mp = (MultiPolygon)g;			
			for(int i=0;i<mp.getNumGeometries();i++){
				Polygon p = (Polygon) (mp).getGeometryN(i);
				pathTooltips.add(getPathTooltip(p,c));	
			}
			
		}
	});

		
		runLater();
	//	canvasRunLater();
		
		updateProgress(0, featureCount);
		System.out.println("min: (" + minX + "," + minY + ") max: (" + maxX
				+ "," + maxY + ")");
	}

	private void corregirRinde(CosechaItem cosechaFeature) {
		if(HarvestFiltersConfig.getInstance().correccionRindeAreaEnabled()){
		//corregir el rinde de a cuerdo a la diferencia de superficie
			 Geometry p = cosechaFeature.getGeometry();
		double supOriginal = cosechaFeature.getAncho()*cosechaFeature.getDistancia()/(ProyectionConstants.METROS2_POR_HA);//si el ancho de la cosecha era dinamico no hay mucha correccion. depende de la cosechadora
				
		double supNueva = p.getArea()*ProyectionConstants.A_HAS;
		double rindeOriginal = cosechaFeature.getRindeTnHa();

		double correccionRinde = supOriginal/supNueva;
	
		
		double rindeNuevo=rindeOriginal*correccionRinde;//supNueva/supOriginal; 
		if(rindeNuevo > 20){
			System.out.println(cosechaFeature.getId()+" rindeNuevo >20");
		}
		cosechaFeature.setRindeTnHa(rindeNuevo);
		}
	}

	/**
	 * recorrer featureTree buscando outlayers y reemplazandolos con el promedio		
	 * @param cosechaItemIndex
	 */
	private void corregirOutlayers(List<CosechaItem> cosechaItemIndex) {			
		Quadtree featureTree2 = new Quadtree();
		cosechaItemIndex.parallelStream().forEach(new Consumer<CosechaItem>(){
			@SuppressWarnings("unchecked")
			@Override
			public void accept(CosechaItem cosechaFeature) {
				//1) crear un circulo de radio a definir y centro en el centroide de la cosecha
				double alfa =0;
				//double ancho =500;//Configuracion.getInstance().getProperty("AnchoFiltroOutlayers"));//500;//50
				double ancho = HarvestFiltersConfig.getInstance().getAnchoFiltroOutlayers();
				double distancia = ancho;
				Coordinate anchoLongLat = new Coordinate(
						ProyectionConstants.metersToLongLat * ancho / 2
								* Math.sin(alfa),
						ProyectionConstants.metersToLongLat * ancho / 2
								* Math.cos(alfa));
				Coordinate distanciaLongLat = new Coordinate(
						ProyectionConstants.metersToLongLat * distancia / 2
								* Math.sin(alfa + Math.PI / 2),
						ProyectionConstants.metersToLongLat * distancia / 2
								* Math.cos(alfa + Math.PI / 2));


				Coordinate l=anchoLongLat;
				Coordinate d =distanciaLongLat;
				Point X = cosechaFeature.getGeometry().getCentroid();
				
				double x = X.getX();
				double y = X.getY();
				Coordinate D = new Coordinate(x - l.x - d.x, y - l.y - d.y); // x-l-d
				Coordinate C = new Coordinate(x + l.x - d.x, y + l.y - d.y);// X+l-d
				Coordinate B = new Coordinate(x + l.x + d.x, y + l.y + d.y);// X+l+d
				Coordinate A = new Coordinate(x - l.x + d.x, y - l.y + d.y);// X-l+d

				/**
				 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
				 * carro--B
				 * 
				 */
				Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
				// Empezar y terminar en
				// el mismo punto.
				// sentido antihorario

				GeometryFactory fact = X.getFactory();

				LinearRing shell = fact.createLinearRing(coordinates);
				LinearRing[] holes = null;
				Polygon poly = new Polygon(shell, holes, fact);
			  		  
				//2) obtener todas las cosechas dentro det circulo
				List<CosechaItem> features = featureTree.query(poly.getEnvelopeInternal());
				if(features.size()>0){
					//outlayerVarianza(cosechaFeature, poly,features);
					outlayerCV(cosechaFeature, poly,features);
					featureTree2.insert(cosechaFeature.getGeometry().getEnvelopeInternal(), cosechaFeature);
				} else {
					System.out.println("zero features");
				}
			}

			public void outlayerVarianza(CosechaItem cosechaFeature, Polygon poly,	List<CosechaItem> features) {
				//3) obtener el promedio 
				double cantidadCosechaFeature = cosechaFeature.getAmount();
				double sumatoria = 0;				
				int divisor = 0;
				for(CosechaItem cosecha : features){
					if(poly.contains(cosecha.getGeometry())){						
						double cantidadCosecha = cosecha.getAmount();					
						sumatoria+=cantidadCosecha;
						divisor++;		
					}
				}
				sumatoria -=cantidadCosechaFeature;
				//divisor--;//n-1 para que sea insesgado.
				divisor--;
				double promedio = sumatoria/divisor;				
				//4) obtener la varianza

				double sumatoriaDesvios2 = 0;				
				for(CosechaItem cosecha : features){			
					if(poly.contains(cosecha.getGeometry())){
						double cantidadCosecha = cosecha.getAmount();				
						sumatoriaDesvios2+= getDesvio2(promedio, cantidadCosecha);
					}
				}
				sumatoriaDesvios2-=getDesvio2(promedio, cantidadCosechaFeature);	
				
				double varianza=sumatoriaDesvios2/divisor;		

				//5) si el rinde de la cosecha menos el promedio es mayor a la varianza reemplazar el rinde por el promedio
				double desvioCosechaFeature = getDesvio2(cantidadCosechaFeature,promedio);
				if(desvioCosechaFeature > N_VARIANZAS_TOLERA*varianza ){//3 desvios(9 varianzas) 95% probabilidad de no cortar como error un dato verdadero.
					//El valor esta fuera de los parametros y modifico el valor por el promedio
					cosechaFeature.setRindeTnHa(promedio);
				}
			}
			
			public void outlayerCV(CosechaItem cosechaFeature, Polygon poly,	List<CosechaItem> features) {
				//3) obtener el promedio 
				double cantidadCosechaFeature = cosechaFeature.getAmount();
				double sumatoria = 0;				
				int divisor = 0;
				for(CosechaItem cosecha : features){
					if(poly.contains(cosecha.getGeometry())){						
						double cantidadCosecha = cosecha.getAmount();					
						sumatoria+=cantidadCosecha;
						divisor++;		
					}
				}
				sumatoria -=cantidadCosechaFeature;
				//divisor--;//n-1 para que sea insesgado.
				divisor--;
				double promedio = sumatoria/divisor;

				//4) obtener la varianza (LA DIF ABSOLUTA DEL DATO Y EL PROM DE LA MUESTRA) (EJ. ABS(10-9.3)/9.3 = 13%)
				//SI 13% ES MAYOR A TOLERANCIA CV% REEMPLAZAR POR PROMEDIO SINO NO
				
				double coefVariacionCosechaFeature = Math.abs(cantidadCosechaFeature-promedio)/promedio;
				
				if(coefVariacionCosechaFeature > TOLERANCIA_CV ){//3 desvios(9 varianzas) 95% probabilidad de no cortar como error un dato verdadero.
					//El valor esta fuera de los parametros y modifico el valor por el promedio
					cosechaFeature.setRindeTnHa(promedio);
				}
			}

			private double getDesvio2(double promedio, double cantidad) {
				return Math.pow(cantidad-promedio,2);

			}

		});//fin del Consumer
		
		this.featureTree = featureTree2;
	}


	private ArrayList<Object> getPathTooltip(Geometry poly,
			CosechaItem cosechaFeature) {
	//	System.out.println("getPathTooltip(); "+System.currentTimeMillis());
		Node path = super.getPathFromGeom(poly, cosechaFeature);

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
		
		
		ArrayList<Object> ret = new ArrayList<Object>();
		ret.add(path);
		ret.add(tooltipText);
		return ret;
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}

	/**
	 * 
	 * 
	 * creo la geometria que corresponde a la feature tomando en cuenta si esta activado el filtro de distancia y el de superposiciones			
	 * 
	 * @param l
	 *            vector ancho de cosecha en coordenadas longLat
	 * @param d
	 *            vector avance de cosecha en coordenadas longLat
	 * @param X
	 *            el punto en coordenadas long/lat
	 *            
	 * @param pasada el numero de pasada al que corresponde el feature           
	 * @return la geometria cosechada en coordenadas longLat
	 */
	private Geometry createGeometryForHarvest( Coordinate l, Coordinate d, Point X, Double pasada) {
		//System.out.println("createGeometryForPoint(); "+System.currentTimeMillis());
		double x = X.getX();
		double y = X.getY();
		
		boolean mismaPasada = pasada.equals(oldPasada);
		 oldPasada =pasada;
		Coordinate D = new Coordinate(x - l.x - d.x, y - l.y - d.y); // x-l-d
		Coordinate C = new Coordinate(x + l.x - d.x, y + l.y - d.y);// X+l-d
		Coordinate B = new Coordinate(x + l.x + d.x, y + l.y + d.y);// X+l+d
		Coordinate A = new Coordinate(x - l.x + d.x, y - l.y + d.y);// X-l+d

		if (HarvestFiltersConfig.getInstance().correccionDistanciaEnabled()) {
			if (lastD == null) {
				lastD = A;
				lastC = B;
			}
			Point2D a2d = new Point2D(A.x, A.y);
			Point2D b2d = new Point2D(B.x, B.y);
			Point2D lc2d = new Point2D(lastC.x, lastC.y);
			Point2D ld2d = new Point2D(lastD.x, lastD.y);

			double distAlC = a2d.distance(lc2d);
			double distBlC = b2d.distance(lc2d);

			double distAlD = a2d.distance(ld2d);
			double distBlD = b2d.distance(ld2d);

			double tolerancia = distanciaAvanceMax
					* CANTIDAD_DISTANCIAS_TOLERANCIA// 10
					* ProyectionConstants.metersToLongLat;
			// .out.println("distAlD: "+distAlD+" < tolerancia: "+tolerancia+" : "+(distAlD<tolerancia
			// && distBlC < distAlC && distAlD < distBlD));

			// evito juntar las puntas cuando las lineas se cruzan o quedan
			// demaciado lejos
			if (distAlD < tolerancia && distBlC < distAlC && distAlD < distBlD
					&& mismaPasada) {
				// .out.println("encolando con el poligono anterior");
				A = lastD;
				B = lastC;
				// .out.println("me pego a los puntos anteriores");
			} else {
				// Cuando la cosechadora termina de dar una vuelta y empieza a
				// cosechar un nuevo tramo tarda hasta entrar en regimen
				// por lo tanto se puede corregir la primera medida
				// extendiendola
				// hacia atras 1 o 2 distancias

				int escAvIni = 0;
				if(HarvestFiltersConfig.getInstance().correccionDemoraPesadaEnabled()){
					 escAvIni = CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA;// 5
				}
				B = new Coordinate(x + l.x + escAvIni * d.x, y + l.y + escAvIni
						* d.y);// X+l+d
				A = new Coordinate(x - l.x + escAvIni * d.x, y - l.y + escAvIni
						* d.y);// X-l+d

				// .out
				// .println("el punto anterior esta muy lejos como para pegarlo a este o hay un cruce de lineas");
				// .out.println("avance es: " + d);
			}
			lastD = D;
			lastC = C;
		}
		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = X.getFactory();
		// PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
		// fact= new GeometryFactory(pm);

		LinearRing shell = fact.createLinearRing(coordinates);
		LinearRing[] holes = null;
		Polygon poly = new Polygon(shell, holes, fact);

		
		//cosechaFeature.setAreaSinSup(poly.getArea()*ProyectionConstants.A_HAS*ProyectionConstants.METROS2_POR_HA);
		
		/*
		 * ahora que tengo el poligono lo filtro con los anteriores para
		 * corregir
		 */
		Geometry difGeom = poly;
		Geometry geometryUnion = null;

		Envelope query = poly.getEnvelopeInternal();		
	
		@SuppressWarnings("unchecked")
		List<CosechaItem> objects = featureTree.query(query);		
	
		if(HarvestFiltersConfig.getInstance().correccionSuperposicionEnabled()){
			geometryUnion = getUnion(fact, objects, poly);
			try {			
				if (geometryUnion != null)
					difGeom = poly.difference(geometryUnion);// Computes a Geometry
				// .out.println("tarde "+(fin-init)+" milisegundos en insertar");
			} catch (Exception te) {
				// cuando la topology exception es side location conflict, el
				// poligono no se intersecta y se imprime completo pisando al
				// poligono anterior

				// .err.println("Error al calcular la diferencia entre el poligono y la union de poligonos hasta el momento");
				// side location conflict [ (-1.3430180316476026E-4,
				// -9.411259613045786E-5, NaN) ]
			//	te.printStackTrace();
				System.err.println("TopologyException en ProcessHarvestMapTask createGeometryForPoint()");
				difGeom = poly;

			}
		}
	
	
		if(difGeom.isEmpty()){
			System.out.println("difGeom es empty " );
		}
		
	//	cosechaFeature.setGeometry(difGeom);
		return difGeom;
	}

	/**
	 * @Description Metodo recomendado para unir varios poligonos rapido
	 */
	public Geometry getUnion(GeometryFactory fact, List<CosechaItem> objects, Geometry query) {
//		System.out.println("getUnion(); "+System.currentTimeMillis());
		if (objects == null || objects.size() < 1) {
			return null;
		} else if (objects.size() == 1) {
			return (Geometry) objects.get(0).getGeometry();
		} else {// hay mas de un objeto para unir
			//System.out.println( "tratando de unir"+ objects.size());
			ArrayList<Geometry> geomList = new ArrayList<Geometry>();
			Point zero = fact.createPoint(new Coordinate (0,0));
			/*
			 *recorro todas las cosechas y si su geometria interna se cruza con la query la agrego a la lista de geometrias 
			 */
			int maxGeometries = HarvestFiltersConfig.getInstance().getMAXGeometries();
			Envelope queryEnvelope = query.getEnvelopeInternal();		
			for (CosechaItem o : objects) {				
					Geometry g = o.getGeometry();
					try{
					if (g.intersects(query)) {//acelera mucho el proceso //g.getEnvelopeInternal().intersects(query) 
					
						boolean contains = g.touches(zero);
						if(!contains
								&&geomList.size()<maxGeometries
								){//limito la cantidad de geometrias a unir arbitrariamente por una cuestion de performance 100 parece ser un techo 
							geomList.add(g);
						} 
//						else{
//							System.out.println("contains zero o hay mas de 100 geometrias en superposicion");
//						}
					}
					}catch(java.lang.IllegalArgumentException e){
						e.printStackTrace();
					}
			
			}
			Geometry union = null;
			Geometry[] geomArray = geomList.toArray(new Geometry[geomList
					.size()]);
	
		
			
			try {
				GeometryCollection polygonCollection = fact
						.createGeometryCollection(geomArray);
				
				Long antes = System.currentTimeMillis();
				//System.out.println("antes de hacer buffer(0) "+antes);
				/*uno las geometrias para sacar la interseccion. deberia funcionar bien con pocas geometrias*/
				 union = polygonCollection.buffer(0); 
				// Devuelve la
				// union de
				// todos los
				// poligonos
				// en la
				// coleccion
				// mucho mas
				// rapido que
				// haciendo la
				// union de
				// a uno por vez
				// .out.println("devolviendo union");
				 Long despues = System.currentTimeMillis();
				 Long demora = despues - antes;
				 if(demora > 1000){
					 System.out.println("tardo mas de 1 segundos en unir "+ polygonCollection.getNumGeometries());
					 System.out.println("tarde "+demora/1000+"s en hacer buffer(0)");
//					 tardo mas de 10 segundos en unir 1390
//					 despues de hacer buffer(0) 110850
				 }
				// System.out.println("tarde "+demora+" en hacer buffer(0)");
				
				
			} catch (Exception e) {
				e.printStackTrace();
				/*java.lang.IllegalArgumentException: Ring has fewer than 3 points, so orientation cannot be determined*/
			}
			
			return union;
		}
		

	}
}// fin del task