package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Labor;
import dao.LaborItem;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import javafx.geometry.Point2D;
import utils.ProyectionConstants;

public class ProcessHarvestMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {

	private   int cantidadDistanciasEntradaRegimen =0;// new Integer(Configuracion.getInstance().getPropertyOrDefault(CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA2,"1"));//5
	private   int cantidadDistanciasTolerancia =0;//=new Integer(Configuracion.getInstance().getPropertyOrDefault(CANTIDAD_DISTANCIAS_TOLERANCIA2,"5"));//10

	private   int cantidadVarianzasTolera =0;//=new Integer(Configuracion.getInstance().getPropertyOrDefault(N_VARIANZAS_TOLERA2,"1")); //3;//9;
	private  Double toleranciaCoeficienteVariacion =new Double(0.0);//=new Double(Configuracion.getInstance().getPropertyOrDefault(TOLERANCIA_CV_0_1,"0.13")); //3;//9;
	private  Double supMinimaHas =  new Double(0.0);//Configuracion.getInstance().getPropertyOrDefault(SUP_MINIMA_M2, "10"))/ProyectionConstants.METROS2_POR_HA;//0.001

	Coordinate lastD = null, lastC = null;
	private Point lastX=null;
	//	double maxX = 0, maxY = 0, minX = 0, minY = 0;// variables para llevar la
	// cuenta de donde estan los
	// puntos y hubicarlos en el
	// centro del panel map
	double distanciaAvanceMax = 0;
	private int puntosEliminados=0;

	private List<Geometry> geomBuffer = new ArrayList<Geometry>();
	//	double anchoMax = 0;

	//Quadtree geometryTree = null;

	//private double precioGrano;
	//	private Double oldPasada=null;
	//	private MathTransform crsTransform = null;
	//	private MathTransform crsAntiTransform =null;

	public ProcessHarvestMapTask(CosechaLabor cosechaLabor){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);

		//labor.cachedEnvelopes.clear();//Makesure you start clean
		labor.clearCache();
		supMinimaHas = cosechaLabor.getConfiguracion().supMinimaProperty().get()/ProyectionConstants.METROS2_POR_HA;//5
		cantidadDistanciasEntradaRegimen = cosechaLabor.getConfiguracion().cantDistanciasEntradaRegimenProperty().get();//5
		cantidadDistanciasTolerancia =cosechaLabor.getConfiguracion().cantDistanciasToleraProperty().get();//10

		cantidadVarianzasTolera =cosechaLabor.getConfiguracion().nVarianzasToleraProperty().get();
		toleranciaCoeficienteVariacion =cosechaLabor.getConfiguracion().toleranciaCVProperty().get(); //3;//9;

		//this.precioGrano = cosechaLabor.precioGranoProperty.doubleValue();

	}

	public void doProcess() throws IOException {
		//	System.out.println("doProcess(); "+System.currentTimeMillis());

		FeatureReader<SimpleFeatureType, SimpleFeature> reader =null;
		//	CoordinateReferenceSystem storeCRS =null;
		if(labor.getInStore()!=null){
			if(labor.outCollection!=null)labor.outCollection.clear();
			reader = labor.getInStore().getFeatureReader();
			featureCount=labor.getInStore().getFeatureSource().getFeatures().size();
		} else{
			if(labor.getInCollection() == null){//solo cambio la inCollection por la outCollection la primera vez
				labor.setInCollection(labor.outCollection);
				labor.outCollection=  new DefaultFeatureCollection("internal",labor.getType());
			}
			//XXX cuando es una grilla los datos estan en outstore y instore es null
			//FIXME si leo del outCollection y luego escribo en outCollection me quedo sin memoria
			reader = labor.getInCollection().reader();
			featureCount=labor.getInCollection().size();
		}

		int divisor = 1;
		if(labor.getConfiguracion().correccionOutlayersEnabled()){
			divisor =2;
		}
		
		double tolerancia = distanciaAvanceMax
				* cantidadDistanciasTolerancia// 10
				* ProyectionConstants.metersToLat();

		
		while (reader.hasNext()) {

			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = labor.constructFeatureContainer(simpleFeature);
			//ci.getRumbo()==90 ||ci.getRumbo()==270 ||			
			//if(ci.getAncho()==0||ci.getDistancia()==0||					ci.getAmount()==0)continue;//XXX evito ingresar puntos invalidos
			distanciaAvanceMax = Math.max(ci.getDistancia(), distanciaAvanceMax);//TODO pasar al while
			//		anchoMax = Math.max(ci.getAncho(), anchoMax);//TODO pasar al while	

			featureNumber++;

			updateProgress(featureNumber/divisor, featureCount);

			Double rumbo = ci.getRumbo();
			Double ancho = ci.getAncho();
			Double anchoOrig =ancho;
			Double distancia = ci.getDistancia();
		//	Double pasada = ci.getPasada();
			Double altura = ci.getElevacion();


			//			System.out.println("rumbo ="+rumbo);
			//			System.out.println("alfa ="+alfa);
			//			rumbo =238.0
			//					alfa =5.724679946541401

//			if(labor.getConfiguracion().correccionAnchoEnabled()){			
//				//				if (ancho < anchoMax) {			
//				//					ancho = anchoMax;
//				//				}
//
//				//	if (ancho < labor.anchoDefaultProperty.doubleValue()) {	
//			
//				ancho= labor.anchoDefaultProperty.doubleValue();
//				//	}
//				ci.setAncho(ancho);
//			} 

			//distanciaAvanceMax = (distancia+ distanciaAvanceMax)/2;

			Object geometry = ci.getGeometry();

			/**
			 * si la geometria es un point procedo a poligonizarla
			 */
			if (geometry instanceof Point) {
				Point longLatPoint = (Point) geometry;

				//double distanciaXLast = lastX.distance(longLatPoint)*ProyectionConstants.metersToLat();
				ProyectionConstants.setLatitudCalculo(longLatPoint.getY());
				if(	lastX!=null && labor.getConfiguracion().correccionDistanciaEnabled() && 
						(lastX.distance(longLatPoint)*ProyectionConstants.metersToLat() <tolerancia)){

					double aMetros=1;// 1/ProyectionConstants.metersToLong()Lat;
					//	BigDecimal x = new BigDecimal();
					double deltaY = longLatPoint.getY()*aMetros-lastX.getY()*aMetros;
					double deltaX = longLatPoint.getX()*aMetros-lastX.getX()*aMetros;
					if((deltaY==0.0 && deltaX ==0.0)|| lastX.equals(longLatPoint)){
						puntosEliminados++;
						//	System.out.println("salteando el punto "+longLatPoint+" porque tiene la misma posicion que el punto anterior "+lastX);
						continue;//ignorar este punto
					}

					distancia = Math.hypot(deltaX, deltaY);
					ci.setDistancia(distancia);
					double tan = deltaY/deltaX;//+Math.PI/2;
					rumbo = Math.atan(tan);
					rumbo = Math.toDegrees(rumbo);//como esto me da entre -90 y 90 le sumo 90 para que me de entre 0 180
					rumbo = 90-rumbo;

					/**
					 * 
					 * deltaX=0.0 ;deltaY=0.0
					 *	rumbo1=NaN
					 *	rumbo0=310.0
					 */

					if(rumbo.isNaN()){//como el avance en x es cero quiere decir que esta yerndo al sur o al norte
						if(deltaY>0){
							rumbo = 0.0;
						}else{
							rumbo=180.0;
						}
					}

					if(deltaX<0){//si el rumbo estaba en el cuadrante 3 o 4 sumo 180 para volverlo a ese cuadrante
						rumbo = rumbo+180;
					}
					ci.setRumbo(rumbo);

				}

				lastX=longLatPoint;
				Double alfa  = Math.toRadians(rumbo) + Math.PI / 2;

				// convierto el ancho y la distancia a verctores longLat poder
				// operar con la posicion del dato
				Coordinate anchoLongLat = constructCoordinate(alfa,ancho);
				Coordinate distanciaLongLat = constructCoordinate(alfa+ Math.PI / 2,distancia);


//				if(labor.getConfiguracion().correccionDemoraPesadaEnabled()){
//					//TODO no mover la geometria, mover los datos que contiene
//					Double corrimientoPesada =	labor.getConfiguracion().getCorrimientoPesada();
//					//Coordinate corrimientoLongLat =constructCoordinate(alfa + Math.PI / 2,corrimientoPesada);
//					// mover el punto 3.5 distancias hacia atras para compenzar el retraso de la pesada
//
//					longLatPoint = longLatPoint.getFactory().createPoint(new Coordinate(longLatPoint.getX()+corrimientoPesada*distanciaLongLat.x,longLatPoint.getY()+corrimientoPesada*distanciaLongLat.y));
//					//utmPoint = utmPoint.getFactory().createPoint(new Coordinate(utmPoint.getX()-corrimientoLongLat.x,utmPoint.getY()-corrimientoLongLat.y));
//				}

				/**
				 * creo la geometria que corresponde a la feature tomando en cuenta si esta activado el filtro de distancia y el de superposiciones
				 */				
				//				Geometry utmGeom = createGeometryForHarvest(anchoLongLat,
				//						distanciaLongLat, utmPoint,pasada,altura,ci.getRindeTnHa());		
				Geometry longLatGeom = createGeometryForHarvest(anchoLongLat,
						distanciaLongLat, longLatPoint,altura,ci.getRindeTnHa());
				if(longLatGeom == null 
						//			|| geom.getArea()*ProyectionConstants.A_HAS()*10000<labor.config.supMinimaProperty().doubleValue()
						){//con esto descarto las geometrias muy chicas
					//System.out.println("geom es null, ignorando...");
					continue;
				}

				/**
				 * solo ingreso la cosecha al arbol si la geometria es valida
				 */
				boolean empty = longLatGeom.isEmpty();
				boolean valid = longLatGeom.isValid();
				boolean big = (longLatGeom.getArea()*ProyectionConstants.A_HAS()>supMinimaHas);
				if(!empty
						&&valid
						&&big//esta fallando por aca
						){

					//Geometry longLatGeom =	crsAntiTransform(utmGeom);//hasta aca se entrega la geometria correctamente

					if(labor.getConfiguracion().correccionDemoraPesadaEnabled()){
						int n=labor.getConfiguracion().getCorrimientoPesada().intValue();
						//TODO tomar los primeros n rindes y ponerlos en el rindesBuffer
						geomBuffer.add(longLatGeom);
						
						
						//TODO a partir del punto n cambiar el rinde de cosechaFeature por el rinde(0) de rindesBuffer
						longLatGeom =geomBuffer.get(0);
						if(geomBuffer.size()>n){
							geomBuffer.remove(0);
						}
					}
					
					ci.setGeometry(longLatGeom);
					corregirRinde(ci,anchoOrig);

					labor.insertFeature(ci);//featureTree.insert(geom.getEnvelopeInternal(), cosechaFeature);
				} else{
					//	System.out.println("no inserto el feature "+featureNumber+" porque tiene una geometria invalida empty="+empty+" valid ="+valid+" area="+big+" "+geom);
				}

			} else { // no es point. Estoy abriendo una cosecha de poligonos.
						List<Polygon> mp = getPolygons(ci);
								Polygon p = mp.get(0);
								
								for(Coordinate c :p.getCoordinates()){
									c.z=ci.getElevacion();
								}
								ci.setGeometry(p);
				//	featureTree.insert(p.getEnvelopeInternal(), cosechaFeature);
				//TODO si el filtro de superposiciones esta activado tambien sirve para los poligonos
				
				labor.insertFeature(ci);
			}
			
		}// fin del while que recorre las features
		//labor.cachedEnvelopes.clear();//limpio la cache despues de hacer la limpieza principal
		labor.clearCache();
		reader.close();

		System.out.println(+puntosEliminados+" puntos eliminados por punto duplicado");
		//TODO antes de corregir outliers usar el criterio de los cuartiles para determinar el minimo y maximo de los outliers
		//
		/**
		 * Q1=X(N/4)
		 * Q3=X(3N/3)
		 * LímInf = Q1- 1.5(Q3-Q1)
		 * LímSup = Q3 + 1.5(Q3-Q1)
		 */
		if(labor.getConfiguracion().correccionOutlayersEnabled()){
			System.out.println("corriegiendo outlayers con CV Max "+toleranciaCoeficienteVariacion);
			corregirOutlayersParalell();		
		} else { 
			System.out.println("no corrijo outlayers");
		}
		if(((CosechaConfig)labor.config).calibrarRindeProperty().get()){
			//TODO obtener el promedio ponderado por la superficie y calcular el 
			//indice de correccion necesario para llegar al rinde objetivo
			//		reader = labor.getOutStore().getFeatureReader();
			//		while(reader.hasNext()){
			//			f=reader.next();
			//		double correccionRinde = labor.correccionCosechaProperty.doubleValue();		
			//		ci.rindeTnHa = rindeDouble * (correccionRinde / 100);
			//		}
		}//fin de calibrar rinde
		
	
				
		labor.constructClasificador();

		List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();
		if(labor.config.resumirGeometriasProperty().getValue()){
			itemsToShow = resumirGeometrias();
			//labor.constructClasificador();//los items cambiaron
		} else{
			SimpleFeatureIterator it = labor.outCollection.features();
			while(it.hasNext()){
				SimpleFeature f=it.next();
				itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
			}
			it.close();
		}

		//TODO resumir geometrias pero en base a la altimetria y dibujar los contornos en otra capa

		//XXX ojo! si son muchos esto me puede tomar toda la memoria.
			runLater(itemsToShow);
	//canvasRunLater();

		updateProgress(0, featureCount);
		//		System.out.println("min: (" + minX + "," + minY + ") max: (" + maxX
		//				+ "," + maxY + ")");
	}

	//	private Geometry crsTransform(Geometry point)  {
	//		try {
	//			return JTS.transform(point, crsTransform);
	//		} catch (MismatchedDimensionException | TransformException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//			return null;//XXX deberia devolver point?
	//		}
	//	}
	//	
	//	private Geometry crsAntiTransform(Geometry point)  {
	//		try {
	//			return JTS.transform(point, crsAntiTransform);
	//		} catch (MismatchedDimensionException | TransformException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//			return null;//XXX deberia devolver point?
	//		}
	//	}

	//	private void initCrsTransform(CoordinateReferenceSystem storeCRS) {
	//		try {
	//			CoordinateReferenceSystem crs=CRS.decode("EPSG:3005");//en metros +
	//			
	//			  boolean lenient=true;
	//				 crsTransform = CRS.findMathTransform(storeCRS, crs, lenient);
	//				  crsAntiTransform = CRS.findMathTransform( crs, storeCRS,lenient);
	//		} catch (FactoryException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//	}

	private List<CosechaItem> resumirGeometrias() {
		//TODO antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual

		//XXX inicializo la lista de las features por categoria
		List<List<SimpleFeature>> colections = new ArrayList<List<SimpleFeature>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			colections.add(i, new ArrayList<SimpleFeature>());
		}
		//XXX recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			CosechaItem ci = this.labor.constructFeatureContainerStandar(f, false);
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			colections.get(cat).add(f);
		}
		it.close();

		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<CosechaItem> itemsCategoria = new ArrayList<CosechaItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		
		//TODO pasar esto a parallel streams
		//XXX por cada categoria 
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			List<Geometry> geometriesCat = new ArrayList<Geometry>();

			//	Geometry slowUnion = null;
			Double sumRinde=new Double(0);
			Double sumatoriaAltura=new Double(0);
			int n=0;
			for(SimpleFeature f : colections.get(i)){//por cada item de la categoria i
				Object geomObj = f.getDefaultGeometry();
				geometriesCat.add((Geometry)geomObj);
				sumRinde+=LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.COLUMNA_RENDIMIENTO));
				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(Labor.COLUMNA_ELEVACION));
				n++;
			} 
			double rindeProm =sumRinde/n;//si n ==o rindeProme es Nan
			double elevProm = sumatoriaAltura/n;
			
			double sumaDesvio2 = 0.0;
			for(SimpleFeature f:colections.get(i)){
				double cantidadCosecha = LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.COLUMNA_RENDIMIENTO));	
				sumaDesvio2+= Math.abs(rindeProm- cantidadCosecha);
		
			}
			double desvioPromedio = sumaDesvio2/n;


			if(n>0){//si no hay ningun feature en esa categoria esto da out of bounds

				GeometryFactory fact = geometriesCat.get(0).getFactory();
				Geometry[] geomArray = new Geometry[geometriesCat.size()];
				GeometryCollection colectionCat = fact.createGeometryCollection(geometriesCat.toArray(geomArray));

				Geometry buffered = null;
				double bufer= ProyectionConstants.metersToLongLat(0.25);
				try{
					buffered = colectionCat.union();
					buffered =buffered.buffer(bufer);
				}catch(Exception e){
					System.out.println("hubo una excepcion uniendo las geometrias. Procediendo con presicion");
					
					buffered= EnhancedPrecisionOp.buffer(colectionCat, bufer);
				}

				
				SimpleFeature fIn = colections.get(i).get(0);
				//FIXME si la cosecha no fue clonada o guardada este metodo falla; rinde es 0 porque no se puede leer la cantidad de null
				CosechaItem ci=labor.constructFeatureContainerStandar(fIn,true);
				//	ci.setId(new Double(i));//creo solo un feature por categoria

				//if(rindeProm == 0.0)continue;
				ci.setRindeTnHa(rindeProm);
				ci.setDesvioRinde(desvioPromedio);
				ci.setElevacion(elevProm);

				ci.setGeometry(buffered);

				itemsCategoria.add(ci);
			//	itemsCategoria.set(i, ci);
				SimpleFeature f = ci.getFeature(labor.featureBuilder);
				boolean res = newOutcollection.add(f);


			}	

		}//termino de recorrer las categorias
		labor.setOutCollection(newOutcollection);
		return itemsCategoria;
	}

	/**
	 * 
	 * @param cosechasItemaUnir lista de cosechasItem que pertenecen todas a la misma categoria y cuyas geometrias se tocan
	 * @return la cosecha que sintetiza a todas las cosechas de esa categoria y la union de sus geometrias
	 */
	private CosechaItem sintentizarCosechasIdemCatEnContacto(List<CosechaItem> cosechasItemAUnir){		
		GeometryFactory fact = new GeometryFactory();
		int n = cosechasItemAUnir.size();
		Geometry[] geomArray = new Geometry[n];
		GeometryCollection colectionCat = fact.createGeometryCollection(geomArray);//error de casteo
		Geometry buffered = colectionCat.union();		

		Double sumRinde=new Double(0);
		Double sumatoriaElevacion=new Double(0);

		for(CosechaItem cosechaAUnir:cosechasItemAUnir){
			sumRinde+=cosechaAUnir.getRindeTnHa();
			sumatoriaElevacion += cosechaAUnir.getElevacion();
		}

		CosechaItem cosechaSintetica = new CosechaItem();
		cosechaSintetica.setRindeTnHa(sumRinde/n);
		cosechaSintetica.setElevacion(sumatoriaElevacion/n);	
		cosechaSintetica.setGeometry(buffered);
		return cosechaSintetica;

	}

	private void corregirRinde(CosechaItem cosechaFeature, Double anchoOrig) {
		
		
		if(labor.getConfiguracion().correccionRindeAreaEnabled()){
			//corregir el rinde de a cuerdo a la diferencia de superficie
			Geometry p = cosechaFeature.getGeometry();

			//todo usar para calcular la produccion total antes de la superposicion sin la correccion de ancho
			
			//si el ancho de la cosecha era dinamico no hay mucha correccion. depende de la cosechadora
			anchoOrig =cosechaFeature.getAncho();
			//XXX al usar el ancho orignal en vez del ancho corregido para la
			//XXX correccion descarto la posibilidad de corregir el error de imputacion de ancho. aunque si corrijo la geometria
			
			double supOriginal = anchoOrig*cosechaFeature.getDistancia()/(ProyectionConstants.METROS2_POR_HA);

			double supNueva = p.getArea()*ProyectionConstants.A_HAS();
			double rindeOriginal = cosechaFeature.getRindeTnHa();

			double correccionRinde = supOriginal/supNueva;

		//	if(correccionRinde>1){//solo corrijo las cosechas que se achicaron. no las que alargue para completar huecos
				double rindeNuevo=rindeOriginal*correccionRinde;//supNueva/supOriginal; 
				//			if(rindeNuevo > 20){
				//				System.out.println(cosechaFeature.getId()+" rindeNuevo >20");
				//			}
				if(isBetweenMaxMin(rindeNuevo)){
					cosechaFeature.setRindeTnHa(rindeNuevo);
				}
		//	}
		}
	}

	/**
	 * metodo que toma los elementos en outCollection y los cambia por el promedio
	 * de su entorno si es un outlayer.
	 * el entorno esta difinido por un circulo de radio igual al ancho outlayers configurado y centrado en el elemento
	 * se define como outlayer si el desvio entre el valor del elemento y el promedio de su entorno es mayor a la tolerancia configurada
	 * el metodo realiza la tarea en forma paralelizada
	 */
	private void corregirOutlayersParalell() {			

		//GeodeticCalculator calc = new GeodeticCalculator(DefaultEllipsoid.WGS84); 


		//1) crear un circulo de radio a definir y centro en el centroide de la cosecha
		double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();
		double alfa =0;
		double distancia = ancho;
		Coordinate anchoLongLatCoord = constructCoordinate(alfa, ancho);
		Coordinate distanciaLongLat =constructCoordinate(alfa+ Math.PI / 2, distancia);

		//		SimpleFeature[] arrayF = new SimpleFeature[labor.outCollection.size()];
		//			labor.outCollection.toArray(arrayF);
		//		List<SimpleFeature> outFeatures = Arrays.asList(arrayF);//  new CopyOnWriteArrayList<SimpleFeature>(arrayF);
		//	List<SimpleFeature> filteredFeatures = new ArrayList<SimpleFeature>();
		//DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		

		//		int i = 0;

		//		GeometricShapeBuilder gb = new GeometricShapeBuilder();//DefaultEllipsoid.WGS84
		//		   gb.circle(0,0, 1,1, 6);
		//			Circle.

		//XXX este metodo es eficiente en el uso de memoria pero demasiado lento al no paralelizar
		//		labor.outCollection.accepts(new FeatureVisitor(){
		//			@Override
		//			public void visit(Feature pf) {
		//				CosechaItem cosechaFeature = labor.constructFeatureContainerStandar((SimpleFeature) pf);
		//				Point X = cosechaFeature.getGeometry().getCentroid();
		//
		//				Polygon poly = constructPolygon(anchoLongLatCoord, distanciaLongLat, X);
		//
		//				//circle = new Circle(X, ancho*ProyectionConstants.metersToLong()Lat);
		//				//2) obtener todas las cosechas dentro det circulo
		//				List<CosechaItem> features = labor.outStoreQuery(poly.getEnvelopeInternal());
		//				if(features.size()>0){
		//					//outlayerVarianza(cosechaFeature, poly,features);
		//					if(outlayerCV(cosechaFeature, poly,features)){
		//					}
		//				} else {
		//					System.out.println("zero features");
		//				}
		//				SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
		//						labor.getType());
		//				SimpleFeature f = cosechaFeature.getFeature(fBuilder);
		//				boolean res = newOutcollection.add(f);				
		//			}
		//
		//		}, new DefaultProgressListener(){
		//			@Override
		//			public void progress(float percent){
		//				super.progress(percent);
		//				System.out.println("percent "+percent);
		//				updateProgress(0.5+percent/2,1);
		//			}
		//		});

		int initOutCollectionSize = labor.outCollection.size();
		SimpleFeature[] arrayF = new SimpleFeature[labor.outCollection.size()];
		labor.outCollection.toArray(arrayF);
		List<SimpleFeature> outFeatures = Arrays.asList(arrayF);
		List<SimpleFeature>  filteredFeatures = outFeatures.parallelStream().collect(
				()->new  ArrayList<SimpleFeature>(),
				(list, pf) ->{		
					try{
					CosechaItem cosechaFeature = labor.constructFeatureContainerStandar(pf,false);
					Point X = cosechaFeature.getGeometry().getCentroid();
					Polygon poly = constructPolygon(anchoLongLatCoord, distanciaLongLat, X);
					//List<CosechaItem> features = labor.cachedOutStoreQuery(poly.getEnvelopeInternal());
					List<CosechaItem> features = labor.cachedOutStoreQuery(poly.getEnvelopeInternal());
					if(features.size()>0){						
						outlayerCV(cosechaFeature, poly,features);						
						SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(labor.getType());
						SimpleFeature f = cosechaFeature.getFeature(fBuilder);
						list.add(f);	
						//This method is safe to be called from any thread.	
						//updateProgress((list.size()+featureCount)/2, featureCount);
					} else{
						System.out.println("la query devolvio cero elementos");
					}
					}catch(Exception e){
						System.err.println("error en corregirOutliersParalell");
						e.printStackTrace();
					}
				},	(list1, list2) -> list1.addAll(list2));
		//XXX esto termina bien. filteredFeatures tiene 114275 elementos como corresponde
		
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		
		boolean res =	newOutcollection.addAll(filteredFeatures);
		if(!res){
			System.out.println("fallo el addAll(filteredFeatures)");
		}
		//labor.cachedEnvelopes.clear();
		labor.clearCache();

		//	List<SimpleFeature> filteredFeatures = new ArrayList<SimpleFeature>();
		//		outFeatures.parallelStream().forEach(pf->{
		//
		//			CosechaItem cosechaFeature = labor.constructFeatureContainerStandar(pf);
		//			Point X = cosechaFeature.getGeometry().getCentroid();
		//
		//			Polygon poly = constructPolygon(anchoLongLatCoord, distanciaLongLat, X);
		//
		//			//circle = new Circle(X, ancho*ProyectionConstants.metersToLong()Lat);
		//			//2) obtener todas las cosechas dentro det circulo
		//			List<CosechaItem> features = labor.outStoreQuery(poly.getEnvelopeInternal());
		//			if(features.size()>0){
		//				//outlayerVarianza(cosechaFeature, poly,features);
		//				if(outlayerCV(cosechaFeature, poly,features)){
		//				}
		//			}
		//			SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
		//					labor.getType());
		//			SimpleFeature f = cosechaFeature.getFeature(fBuilder);
		//			boolean res = newOutcollection.add(f);
		//			//			i++;
		//
		//			//	updateProgress((i+featureCount)/2, featureCount);
		//
		//		});

		
		int endtOutCollectionSize = newOutcollection.size();
		if(initOutCollectionSize !=endtOutCollectionSize){
			System.err.println("se perdieron elementos al hacer el filtro de outlayers. init="+initOutCollectionSize+" end="+endtOutCollectionSize);
		}
		labor.setOutCollection(newOutcollection);
		featureCount=labor.outCollection.size();
		//TODO tratar de paralelizar este proceso que acelera mucho
		//		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		
		//		newOutcollection.addAll(filteredFeatures);

		//labor.setOutCollection(newOutcollection);

	}


	/**
	 * recorrer featureTree buscando outlayers y reemplazandolos con el promedio		
	 * @param cosechaItemIndex
	 * @deprecated
	 */
//	private void corregirOutlayers() {			
//
//		//1) crear un circulo de radio a definir y centro en el centroide de la cosecha
//		double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();
//		double alfa =0;
//		double distancia = ancho;
//		Coordinate anchoLongLatCoord = constructCoordinate(alfa, ancho);
//		Coordinate distanciaLongLat =constructCoordinate(alfa+ Math.PI / 2, distancia);
//
//
//		int i = 0;
//		SimpleFeatureIterator reader = labor.outCollection.features();
//
//		featureCount=labor.outCollection.size();
//		//TODO tratar de paralelizar este proceso que acelera mucho
//		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		
//
//		while( reader.hasNext()){
//			SimpleFeature sf = reader.next();
//			CosechaItem cosechaFeature = labor.constructFeatureContainerStandar(sf,false);
//			Point X = cosechaFeature.getGeometry().getCentroid();
//
//			Polygon poly = constructPolygon(anchoLongLatCoord, distanciaLongLat, X);
//
//			//2) obtener todas las cosechas dentro det circulo
//			List<CosechaItem> features = labor.cachedOutStoreQuery(poly.getEnvelopeInternal());
//			if(features.size()>0){
//				//outlayerVarianza(cosechaFeature, poly,features);
//				if(outlayerCV(cosechaFeature, poly,features)){
//				}
//			} else {
//				System.out.println("zero features");
//			}
//			SimpleFeature f = cosechaFeature.getFeature(labor.featureBuilder);
//			boolean res = newOutcollection.add(f);
//			i++;
//
//			//updateProgress((i+featureCount)/2, featureCount);
//		}//fin del while
//		reader.close();
//		labor.cachedEnvelopes.clear();
//		labor.setOutCollection(newOutcollection);
//
//	}

	/**
	 * metodo que busca mejorar la eficiencia de corregirOutlayers 
	 * pero que genera un resultado de cuadricula en vez de ondas suaves
	 */
	private void corregirOutlayersSquare(){
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());
		double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();		
		double anchoLongLat = ancho*ProyectionConstants.metersToLat();
		ReferencedEnvelope bounds = labor.outCollection.getBounds();
		List<Polygon> poligonosGrilla = construirGrilla(bounds,ancho);
		for(Polygon poligono :poligonosGrilla){
			List<CosechaItem> features = labor.outStoreQuery(poligono.getEnvelopeInternal());
			if(features.size()>0){

				double sumatoriaRinde = 0;			
				double sumatoriaAltura = 0;				
				int divisor = 0;
				for(CosechaItem cosecha : features){							
					double cantidadCosecha = cosecha.getAmount();	
					if(isBetweenMaxMin(cantidadCosecha)){
						sumatoriaAltura+=cosecha.getElevacion();
						sumatoriaRinde+=cantidadCosecha;
						divisor++;		
					}					
				}

				double promedioRinde = sumatoriaRinde/divisor;
				double promedioAltura = sumatoriaAltura/divisor;

				Point centro = poligono.getCentroid();
				Point cosechaPoint =null;
				for(CosechaItem cosecha : features){
					cosechaPoint = cosecha.getGeometry().getCentroid();
					double dX=Math.abs(cosechaPoint.getX()-centro.getX());
					double dY=Math.abs(cosechaPoint.getY()-centro.getY());
					if(dX<anchoLongLat/2 && dY<anchoLongLat/2){
						double cantidadCosecha = cosecha.getAmount();	
						double coefVariacionCosechaFeature = Math.abs(cantidadCosecha-promedioRinde)/promedioRinde;
						if(coefVariacionCosechaFeature > toleranciaCoeficienteVariacion ){//3 desvios(9 varianzas) 95% probabilidad de no cortar como error un dato verdadero.
							//El valor esta fuera de los parametros y modifico el valor por el promedio
							//	System.out.println("reemplazo "+cosechaFeature.getRindeTnHa()+" por "+promedio);
							cosecha.setRindeTnHa(promedioRinde);
							cosecha.setElevacion(promedioAltura);
						}
						SimpleFeature f = cosecha.getFeature(labor.featureBuilder);
						boolean res = newOutcollection.add(f);
						if(!res){
							System.out.println("no se pudo reinsertar la cosecha en corregirOutlayers");
						}
					}
				}
			}
		}
		labor.setOutCollection(newOutcollection);

	}
	public Coordinate constructCoordinate(double alfa, double ancho) {
		return new Coordinate(
				ProyectionConstants.metersToLong() * ancho / 2
				* Math.sin(alfa),
				ProyectionConstants.metersToLat() * ancho / 2
				* Math.cos(alfa));
	}

	/**
	 * Metodo que construye un poligono rectangular centrado en X de ancho l y alto d
	 * @param l ancho que tiene que tener la caja en unidades long/lat
	 * @param d alto que tiene que tener la caja en unidades long/lat
	 * @param X punto al rededor del que se va a construir la caja de ancho l y alto d
	 * @return Polygon Rectangulo centrado en X de ancho l y alto d
	 */
	public Polygon constructPolygon(Coordinate l, Coordinate d, Point X) {
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

		//		LinearRing shell = fact.createLinearRing(coordinates);
		//		LinearRing[] holes = null;
		//		Polygon poly = new Polygon(shell, holes, fact);
		Polygon poly = fact.createPolygon(coordinates);
	
		return poly;
	}

	/**
	 * 
	 * LímInf = Q1- 1.5(Q3-Q1)
	 * LímSup = Q3 + 1.5(Q3-Q1)
	 * @param cosechaFeature
	 * @param poly
	 * @param features
	 */

	private void outlayerVarianza(CosechaItem cosechaFeature, Polygon poly,	List<CosechaItem> features) {
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
		if(desvioCosechaFeature > cantidadVarianzasTolera*varianza ){//3 desvios(9 varianzas) 95% probabilidad de no cortar como error un dato verdadero.
			//El valor esta fuera de los parametros y modifico el valor por el promedio
			cosechaFeature.setRindeTnHa(promedio);
		}
	}
	/**
	 * 
	 * @param cosechaFeature
	 * @param poly es el area dentro de la que se calcula el outlayer
	 * @param features
	 * @return true si cosechaFeature fue modificada
	 */
	private boolean outlayerCV(CosechaItem cosechaFeature, Polygon poly,	List<CosechaItem> features) {
		boolean ret = false;
		Geometry geo = cosechaFeature.getGeometry().getCentroid();
		double rindeCosechaFeature = cosechaFeature.getAmount();
		double sumatoriaRinde = 0;			
		double sumatoriaAltura = 0;				
		double divisor = 0;
		// cambiar el promedio directo por el metodo de kriging de interpolacion. ponderando los rindes por su distancia al cuadrado de la muestra
		double ancho = labor.config.getAnchoFiltroOutlayers();
		//la distancia no deberia ser mayor que 2^1/2*ancho, me tomo un factor de 10 por seguridad e invierto la escala para tener mejor representatividad
		//en vez de tomar de 0 a inf, va de ancho*(10-2^1/2) a 0
		ancho = Math.sqrt(2)*ancho;
		
		for(CosechaItem cosecha : features){
			double cantidadCosecha = cosecha.getAmount();	
			Geometry geo2 = cosecha.getGeometry().getCentroid();
			double distancia =geo.distance(geo2)/ProyectionConstants.metersToLat();
			
			double distanciaInvert = (ancho-distancia);
			if(distanciaInvert<0)System.out.println("distancia-1 es menor a cero "+distanciaInvert);
			//los pesos van de ~ancho^2 para los mas cercanos a 0 para los mas lejanos
			double weight =  Math.pow(distanciaInvert,2);
			//System.out.println("distancia="+distancia+" distanciaInvert="+distanciaInvert+" weight="+weight);
			
			cantidadCosecha = Math.min(cantidadCosecha,labor.maxRindeProperty.doubleValue());
			cantidadCosecha = Math.max(cantidadCosecha,labor.minRindeProperty.doubleValue());
			//if(isBetweenMaxMin(cantidadCosecha)){
				
				sumatoriaAltura+=cosecha.getElevacion()*weight;
				sumatoriaRinde+=cantidadCosecha*weight;
				divisor+=weight;		
			//}			
		}
		boolean rindeEnRango = isBetweenMaxMin(rindeCosechaFeature);
	
		double promedioRinde = 0.0;
		double promedioAltura = 0.0;
		if(divisor>0){
			promedioRinde = sumatoriaRinde/divisor;
//			promedioRinde = Math.min(promedioRinde,labor.maxRindeProperty.doubleValue());
//			promedioRinde = Math.max(promedioRinde,labor.minRindeProperty.doubleValue());
			promedioAltura = sumatoriaAltura/divisor;
		}else{
			System.out.println("divisor es <0 "+ divisor);
			System.out.println("sumatoria de rindes = "+sumatoriaRinde);
		}
		//4) obtener la varianza (LA DIF ABSOLUTA DEL DATO Y EL PROM DE LA MUESTRA) (EJ. ABS(10-9.3)/9.3 = 13%)
		//SI 13% ES MAYOR A TOLERANCIA CV% REEMPLAZAR POR PROMEDIO SINO NO

		if(!(promedioRinde==0)){
		double coefVariacionCosechaFeature = Math.abs(rindeCosechaFeature-promedioRinde)/promedioRinde;
		cosechaFeature.setDesvioRinde(coefVariacionCosechaFeature);
		
		if(coefVariacionCosechaFeature > toleranciaCoeficienteVariacion ||!rindeEnRango){//si el coeficiente de variacion es mayor al 20% no es homogeneo
			//El valor esta fuera de los parametros y modifico el valor por el promedio
			//	System.out.println("reemplazo "+cosechaFeature.getRindeTnHa()+" por "+promedio);
			cosechaFeature.setRindeTnHa(promedioRinde);
			
			cosechaFeature.setElevacion(promedioAltura);
			ret=true;
		}
		}
		return ret;
	}

	private boolean isBetweenMaxMin(double cantidadCosecha) {
		boolean ret = cantidadCosecha<=labor.maxRindeProperty.doubleValue()&& cantidadCosecha>=labor.minRindeProperty.doubleValue();
		if(!ret){
			//	System.out.println(cantidadCosecha+">"+labor.maxRindeProperty.doubleValue()+" o <"+labor.minRindeProperty.doubleValue());
		}
		return ret;
	}

	private double getDesvio2(double promedio, double cantidad) {
		return Math.pow(cantidad-promedio,2);

	}

	@Override
	protected void getPathTooltip(Geometry poly,	CosechaItem cosechaItem) {
		//	System.out.println("getPathTooltip(); "+System.currentTimeMillis());
		//List<SurfacePolygon>  paths = getSurfacePolygons(poly, cosechaFeature);//
		//	List<gov.nasa.worldwind.render.Polygon>  paths = super.getPathFromGeom2D(poly, cosechaFeature);
		//ExtrudedPolygon  path = super.getPathFromGeom2D(poly, cosechaFeature);

		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("#.00");

		String tooltipText = new String("Rinde: "
				+ df.format(cosechaItem.getAmount()) + " Tn/Ha\n"
				//	+ "Area: "+ df.format(area * ProyectionConstants.METROS2_POR_HA)+ " m2\n" + 

				);

		tooltipText=tooltipText.concat("Elevacion: "+df.format(cosechaItem.getElevacion() ) + "\n");

		tooltipText=tooltipText.concat("Ancho: "+df.format(cosechaItem.getAncho() ) + "\n");
		tooltipText=tooltipText.concat("Rumbo: "+df.format(cosechaItem.getRumbo() ) + "\n");
		tooltipText=tooltipText.concat("feature: "+cosechaItem.getId() + "\n");
		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}
		//super.getRenderPolygonFromGeom(poly, cosechaItem,tooltipText);
		super.getExrudedPolygonFromGeom(poly, cosechaItem,tooltipText);

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
	 * @param rindeCosecha servia para asegurar que no se corte esta geometria por cosechas con rinde menor. deprecated
	 * @return la geometria cosechada en coordenadas longLat
	 */
	private Geometry createGeometryForHarvest( Coordinate l, Coordinate d, Point X,Double elevacion, Double rindeCosecha) {
		//System.out.println("createGeometryForPoint(); "+System.currentTimeMillis());
		double x = X.getX();
		double y = X.getY();
		double z = elevacion;
		
	//	System.out.println("creando geometria con elevacion "+z);
		//z=20;
		boolean mismaPasada = true;//pasada.equals(oldPasada);
		//	oldPasada =pasada;
		Coordinate D = new Coordinate(x - l.x - d.x, y - l.y - d.y,z); // x-l-d
		Coordinate C = new Coordinate(x + l.x - d.x, y + l.y - d.y,z);// X+l-d
		Coordinate B = new Coordinate(x + l.x + d.x, y + l.y + d.y,z);// X+l+d
		Coordinate A = new Coordinate(x - l.x + d.x, y - l.y + d.y,z);// X-l+d



		Coordinate Afinal =A;
		Coordinate Bfinal =B;

		if (labor.getConfiguracion().correccionDistanciaEnabled() && cantidadDistanciasTolerancia>0) {
			if (lastD == null) {
				lastD = A;
				lastC = B;
			}else{
				lastC.z=z;
				lastD.z=z;
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
					* cantidadDistanciasTolerancia// 10
					* ProyectionConstants.metersToLat();
			// .out.println("distAlD: "+distAlD+" < tolerancia: "+tolerancia+" : "+(distAlD<tolerancia
			// && distBlC < distAlC && distAlD < distBlD));

			// evito juntar las puntas cuando las lineas se cruzan o quedan
			// demaciado lejos
			//FIXME corregir esto para que tenga en cuenta que es un cuadrilatero y no un rectangulo; hay otras opciones de cuadrilateros validos que no estoy tomando
			if (distAlD < tolerancia 
					&& distBlC < distAlC*1.01 
					&& distAlD < distBlD*1.01
					&& mismaPasada) {
				Afinal = lastD;
				Bfinal = lastC;				
				Afinal.z=z;
				Bfinal.z=z;
			} else {
				// Cuando la cosechadora termina de dar una vuelta y empieza a
				// cosechar un nuevo tramo tarda hasta entrar en regimen
				// por lo tanto se puede corregir la primera medida
				// extendiendola
				// hacia atras 1 o 2 distancias

				int escAvIni = 0;
				if(labor.getConfiguracion().correccionDemoraPesadaEnabled()){
					escAvIni = cantidadDistanciasEntradaRegimen;// 5
				}				
				B = new Coordinate(x + l.x + escAvIni * d.x, y + l.y + escAvIni 
						* d.y);// X+l+d  //B.x=B.x+escAvIni * d.x;
				A = new Coordinate(x - l.x + escAvIni * d.x, y - l.y + escAvIni
						* d.y,10);// X-l+d
			}
			lastD = D;
			lastC = C;
		}
		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates1 = { Afinal, Bfinal, C, D, Afinal };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		Coordinate[] coordinates2 = { Afinal, Bfinal, D, C, Afinal };// Tiene que ser cerrado.
		Coordinate[] coordinates3 = { Afinal,C, Bfinal, D,  Afinal };// Tiene que ser cerrado.
		Coordinate[] coordinates4 = { Afinal,C, D,  Bfinal, Afinal };// Tiene que ser cerrado.
		Coordinate[] coordinates5 = { Afinal,D,Bfinal, C,  Afinal };// Tiene que ser cerrado.
		Coordinate[] coordinates6 = { Afinal,D,C, Bfinal, Afinal };// Tiene que ser cerrado.

		List<Coordinate[]>coordinateCollection=  new ArrayList<Coordinate[]>();
		coordinateCollection.add(coordinates2);
		coordinateCollection.add(coordinates3);
		coordinateCollection.add(coordinates4);
		coordinateCollection.add(coordinates5);
		coordinateCollection.add(coordinates6);

		GeometryFactory fact = X.getFactory();

		//		 PrecisionModel pm = new PrecisionModel(10000);
		//		
		//		 GeometryFactory fact= new GeometryFactory(pm);

		Polygon poly = fact.createPolygon(coordinates1);
		double area = poly.getArea();
		for(Coordinate[] coordinates: coordinateCollection){
			Polygon aux = fact.createPolygon(coordinates);
			double auxArea = aux.getArea();
			if(!poly.isValid() || area<auxArea){//como tengo 2 posibilidades de hacer un poligono valido me quedo con la mas grande
				poly = aux;			
			}
		}

		lastC = poly.getCoordinates()[2];
		lastD = poly.getCoordinates()[3];
		if(poly.getNumPoints()<3){
			System.out.println("numpoints menor a 3");//esto es porque A y B son iguales y C y D son iguales porque l es (0,0) porque ancho es 0
			//	return null;
		}

		poly = (Polygon) makeGood(poly);


		/*
		 * ahora que tengo el poligono lo filtro con los anteriores para
		 * corregir
		 */
		Geometry difGeom = poly;
		Geometry geometryUnion = null;
		if(labor.getConfiguracion().correccionSuperposicionEnabled()){
			Geometry longlatPoly = poly;// crsAntiTransform(poly);
			Envelope query = longlatPoly.getEnvelopeInternal();		//hago la query en coordenadas long/lat

			//List<CosechaItem> objects = labor.outStoreQuery(query);// new ArrayList<CosechaItem>();
			List<CosechaItem> objects = labor.cachedOutStoreQuery(query);
			//si uso cached tengo que actualizarlo al hacer insert o no anda
			//System.out.println("la cantidad de objects para construir la geometria es "+objects.size());
			//si el rinde es menor asumo que fue cosechado despues y por lo tanto no corresponde quitarselo a la geometria
			//			objects = objects.stream()
			//					.filter(c ->  c.getAmount()> rindeCosecha).collect(Collectors.toList());
			//esto funciona bien pero cuando los rindes son similares evita que corte poligonos que corresponde cortar
			//TODO marcar los objects que no entraron en la lista final como elementos a volver a repasar al final


			geometryUnion = getUnion(fact, objects, poly);
			try {			
				if (geometryUnion != null 
						&& geometryUnion.isValid() ){
					Geometry polyG =poly;
					//si bajo el precisionModel a 1000 no tira errores pero las geometrias no tienen la forma correcta
					//	GeometryPrecisionReducer pr = new GeometryPrecisionReducer(pm);
					//					geometryUnion= pr.reduce(geometryUnion);
					//					 polyG = pr.reduce(poly);

					difGeom = polyG.difference(geometryUnion);// Computes a Geometry//found non-noded intersection between LINESTRING ( -61.9893807883
				}
				difGeom = makeGood(difGeom);
				// .out.println("tarde "+(fin-init)+" milisegundos en insertar");
			} catch (Exception te) {
				try{
					difGeom = EnhancedPrecisionOp.difference(poly, geometryUnion);
				}catch(Exception e){
					difGeom=poly;
				}
				//te.printStackTrace();//esto demora mucho la ejecucion

				// cuando la topology exception es side location conflict, el
				// poligono no se intersecta y se imprime completo pisando al
				// poligono anterior

				// .err.println("Error al calcular la diferencia entre el poligono y la union de poligonos hasta el momento");
				// side location conflict [ (-1.3430180316476026E-4,
				// -9.411259613045786E-5, NaN) ]
				//	te.printStackTrace();
				//System.err.println("TopologyException en ProcessHarvestMapTask createGeometryForPoint(). insertando el poligono entero "+poly);


			}
		}//fin de corregir superposiciones


		//		if(difGeom.isEmpty()){
		//			System.out.println("difGeom es empty " );
		//		}

		if(difGeom instanceof Point){
			System.out.println("difGeom es POINT "+difGeom );
		}

		//	cosechaFeature.setGeometry(difGeom);
		//difGeom a veces devuelve un POINT y eso es una geometria invalida
		return difGeom;//XXX aca esta ok, se ve el poligono inclinado correctamente
	}





	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	public List<Polygon> construirGrilla(BoundingBox bounds,double ancho) {
		System.out.println("construyendo grilla");
		List<Polygon> polygons = new ArrayList<Polygon>();
		//convierte los bounds de longlat a metros
		
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong() - ancho/2;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat() - ancho/2;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong()+ ancho/2;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat()+ ancho/2;
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
	//	public Geometry computeUnion (Geometry geom) 
	//	{
	//		if (geom instanceof GeometryCollection) 
	//		{
	//			GeometryCollection collection = (GeometryCollection)geom;
	//			LinkedList glist = new LinkedList();
	//			for (int i = 0; i < collection.getNumGeometries(); i += 1) 
	//			{
	//				glist.add(computeUnion(collection.getGeometryN(i)));
	//			}
	//			while (glist.size() > 1) 
	//			{
	//				Geometry geom1 = (Geometry)glist.removeFirst();
	//				Geometry geom2 = (Geometry)glist.removeFirst();
	//				Geometry result = geom1.union(geom2);
	//				if (result.getClass() == GeometryCollection.class) 
	//				{
	//					glist.addLast(collapse((GeometryCollection)result));
	//				} 
	//				else 
	//				{
	//					glist.addLast(result);
	//				}
	//			}
	//			return (Geometry)glist.getFirst();
	//		} 
	//		else 
	//		{
	//			return geom;
	//		}
	//	}





}// fin del task