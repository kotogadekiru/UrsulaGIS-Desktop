package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.LaborItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import gui.nww.LaborLayer;
import tasks.ProcessMapTask;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class SiembraFertTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	//private List<CosechaLabor> cosechas;
	private SiembraLabor siembra;
	private FertilizacionLabor fertilizacion;
	//	private SimpleFeatureType type = null;


	public SiembraFertTask(SiembraLabor _siembra, FertilizacionLabor _fertilizacion){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super( new SiembraLabor());
		this.siembra=_siembra;
		this.fertilizacion=_fertilizacion;

		labor.setSemilla(siembra.getSemilla());//Cultivo(cultivo);
		labor.setLayer(new LaborLayer());
		labor.setEntreSurco(siembra.getEntreSurco());
		labor.colAmount.set(SiembraLabor.COLUMNA_DOSIS_SEMILLA);

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
		// TODO 1 obtener el bounds general que cubre a todas las cosechas
		ReferencedEnvelope unionEnvelope = siembra.outCollection.getBounds();
		unionEnvelope.expandToInclude(fertilizacion.outCollection.getBounds());
		double anchoGrilla = labor.getConfiguracion().getAnchoGrilla();



		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = construirGrilla(unionEnvelope, anchoGrilla);
		//System.out.println("creando una grilla con "+grilla.size()+" elementos");
		// 3 recorrer cada pixel de la grilla promediando los valores y generando los nuevos items de la cosecha

		featureCount = grilla.size();

		int clasesA = siembra.clasificador.getNumClasses();
		//int clasesB = fertilizacion.clasificador.getNumClasses();
		//claseAB = clasesA*claseB(r)+claseA(r)
		ConcurrentHashMap< Integer,List<Object[]>> byBicluster =	grilla.parallelStream().collect(
				() -> new  ConcurrentHashMap< Integer,List<Object[]>>(),(map, poly) -> {
					try{
						List siembrasPoly=this.siembra.cachedOutStoreQuery(poly.getEnvelopeInternal()); 
						List fertilizacionesPoly=this.fertilizacion.cachedOutStoreQuery(poly.getEnvelopeInternal()); 
						if(siembrasPoly.size()<1 && fertilizacionesPoly.size()<1)return;
						Object[] siembraItem = construirFeatureGeometric(siembrasPoly,poly);     //geometry y amount
						Object[] fertilizacionItem = construirFeatureGeometric(fertilizacionesPoly,poly);
						
						//System.out.println("siembra grilla item "+siembraItem[1]);
						//System.out.println("fert grilla item "+fertilizacionItem[1]);
						List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(poly);//(Geometry) siembraItem[0]);
						Double dosisHa = new Double((double) siembraItem[1]);//.fi.getDosistHa();
						Double fertHa = new Double((double) fertilizacionItem[1]);//.fi.getDosistHa();
						int claseFert = fertilizacion.clasificador.getCategoryFor(fertHa) ;
						int claseSiembra = siembra.clasificador.getCategoryFor(dosisHa);
						Integer index = clasesA*claseFert + claseSiembra;
						//System.out.println("agregando un elemento de la clase conjunta "+clasesA+"*"+claseFert+"+"+claseSiembra+" = "+index);
						List<Object[]> catFeatures = map.get(index);
						if(catFeatures==null)catFeatures=new ArrayList<Object[]>();

						for(Polygon p : flatPolygons){
							Object[] values = {p,dosisHa,fertHa,new Double(0)}; 
							catFeatures.add(values);
						}
						map.put(index, catFeatures);
					}catch(Exception e){
						System.err.println("error al construir un elemento de la grilla");
						e.printStackTrace();
					}
				},
				(map1, map2) -> {
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
		List<Object[]> resumidas = resumirGeometrias(byBicluster);
		
		//double entresurco = siembra.getEntreSurco();
		double pesoSKg = labor.getSemilla().getPesoDeMil()/(1000*1000);//141
		//double metrosLHA = ProyectionConstants.METROS2_POR_HA/entresurco;//23,809.52
		

		List<SiembraItem> itemsToShow = new ArrayList<SiembraItem>();
		Double id = 0d;
		for(Object[] value :resumidas) {
			SiembraItem si = new SiembraItem();
			si.setId(id);
			id++;
		
			si.setDosisHa((Double)value[1]);
			
			Double seedsM2=si.getDosisHa()/(pesoSKg*ProyectionConstants.METROS2_POR_HA);
			
			double sML=seedsM2*labor.getEntreSurco();//esto es 0.42
			
			//double semillasMl =(si.getDosisHa()/pesoSKg)/metrosLHA; 
			System.out.println("dosisML "+sML);
			si.setDosisML(sML*10);//multipolicar por 10 porque al insertarla se divide por 10
			si.setDosisFertLinea((Double)value[2]);
			labor.setPropiedadesLabor(si);
			si.setGeometry((Geometry)value[0]);
			
			//System.out.println("termine de crear una siembra resumida : "+ci.toString());
			labor.insertFeature(si); //aca se divide por 10
			itemsToShow.add(si);		
		}

		labor.constructClasificador();
		runLater(itemsToShow);
	}

	/**
	 * toma un map con listas de features de una misma categoria y responde una lista de una feature resumida por cada categoria
	 * @param byBicluster
	 * @return
	 */
	private List<Object[]> resumirGeometrias(Map<Integer,List<Object[]>> byBicluster) {	
		System.out.println("resumiendoBicluster con "+byBicluster.size()+" clusters");
		List<Object[]> objects = new  ArrayList<Object[]>();
		for(Integer clase:  byBicluster.keySet()) {
			List<Object[]> cluster = byBicluster.get(clase);//.values()
			System.out.println("resumiendo el cluster "+clase+" con "+cluster.size()+" features");
			objects.addAll(construirFeatureResumido(cluster));
		}
		//System.out.println("biClustersCreados "+objects.size()+"");
		return objects;
//		return byBicluster.values().stream().collect(
//				() -> new  ArrayList<Object[]>(),
//				(list,features) ->list.addAll(construirFeatureResumido(features)),
//				(list1, list2) -> list1.addAll(list2)
//				);
	}
	
	private List<Object[]> construirFeatureResumidoGeometrico(List<Object[]> cluster){
		List<Object[]> ret = new ArrayList<Object[]>();
		
		if(cluster.size()<1){
			System.out.println("no hay features en el cluster");
			return ret;
		}

		//List<Geometry> aUnir = new ArrayList<Geometry>();
		Geometry[] geomArray = new Geometry[cluster.size()];

		double amountProm=0,fertProm=0;
		int n=0;
		for(Object[] li : cluster){
			geomArray[n]=(Geometry)li[0];
			//aUnir.add((Geometry)li[0]);
			amountProm+=(Double)li[1];
			fertProm+=(Double)li[2];
			n++;
		}
		amountProm=amountProm/n;
		fertProm=fertProm/n;
		
		Geometry union = geomArray[0];//.get(0);
		GeometryFactory fact = union.getFactory();
		
		GeometryCollection colectionCat = fact.createGeometryCollection(geomArray);

		try{
			union = colectionCat.buffer(0);//convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
		}catch(Exception e){
			System.out.println("fallo la union de las geometrias del Bicluster");
			e.printStackTrace();
		}

		//SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//local para no tener problemas de concurrencia
		List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(union);

		for(Polygon p:flatPolygons){
			//synchronized(fb) {
			//System.out.println("creando el bicluster con la geometria unida "+p);
			Object[] values = {p,amountProm,fertProm,new Double(0)}; 
			//fb.addAll(values);
			//SimpleFeature exportFeature = fb.buildFeature(null);//fb concurrentes ponen los mismos ids?

			ret.add(values);
			//		}
		}
		return ret;
	}

	/**
	 * 
	 * @param cluster lista de cosechasitems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private List<Object[]> construirFeatureResumido(List<Object[]> cluster) {
		if(cluster.size()<1){
			return null;
		}

		List<Geometry> aUnir = new ArrayList<Geometry>();
		// sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
		Geometry union = null;
		double areaPoly = 0;
		Map<Object[],Double> areasIntersecciones = new HashMap<Object[],Double>();
		for(Object[] cPoly : cluster){			
			//XXX si es una cosecha de ambientes el area es importante
			Geometry g = (Geometry) cPoly[0];//.getDefaultGeometry();
			try{				
				Double areaInterseccion = g.getArea();
				areaPoly+=areaInterseccion;
				areasIntersecciones.put(cPoly,areaInterseccion);
				if(union==null){
					union = g;		//union es la geometria que se va a devolver al final
				}
				aUnir.add(g);
			}catch(Exception e){
				System.err.println("no se pudo hacer la interseccion entre\n y\n"+g);
			}		
		}

		List<Object[]> catFeatures = new ArrayList<Object[]>();// new HashMap<Geometry,Double>();
		if(areaPoly>getAreaMinimaLongLat()){
			double siembraProm=0,fertProm = 0;

			for(Object[] f : cluster){			
				Double gArea = ((Geometry)f[0]).getArea();
				//			if(gArea==null)continue;
				double peso = gArea/areaPoly;
				siembraProm+=peso*(double)f[1];//LaborItem.getDoubleFromObj(f.getAttribute(SIEMBRA_COLUMN));
				fertProm+=peso*(double)f[2];//LaborItem.getDoubleFromObj(f.getAttribute(LINEA_COLUMN));
			}

			GeometryFactory fact = aUnir.get(0).getFactory();
			Geometry[] geomArray = new Geometry[aUnir.size()];
			GeometryCollection colectionCat = fact.createGeometryCollection(aUnir.toArray(geomArray));

			try{
				union = colectionCat.buffer(0);
				//union = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
			}catch(Exception e){
				e.printStackTrace();
			}

			//SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//local para no tener problemas de concurrencia
			List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(union);

			for(Polygon p:flatPolygons){
				//synchronized(fb) {
				Object[] values = {p,siembraProm,fertProm,new Double(0)}; 
				//fb.addAll(values);
				//SimpleFeature exportFeature = fb.buildFeature(null);//fb concurrentes ponen los mismos ids?

				catFeatures.add(values);
				//		}
			}
		}
		return catFeatures;
	}
	
	
	/**
	 * 
	 * @param siembrasPoly lista de cosechasitems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private Object[] construirFeatureGeometric(List<LaborItem> siembrasPoly, Polygon poly) {
		Object[] ret = new Object[2];
		ret[0]=poly;
		ret[1]=0d;
		if(siembrasPoly.size()<1){
			return ret;
		}



		//siembrasPoly.stream().mapToDouble(LaborItem::getAmount).average();
		double amountProm=0;
		OptionalDouble opt = siembrasPoly.stream().mapToDouble(LaborItem::getAmount).average();
		if(opt.isPresent()) {
			amountProm=opt.getAsDouble();
		}
		//		int n=0;
		//		for(LaborItem li : siembrasPoly){
		//			n++;
		//			amountProm+=li.getAmount();
		//		}
		//		amountProm=amountProm/n;

		ret[1]=amountProm;

		return ret;
	}
	

	/**
	 * 
	 * @param siembrasPoly lista de cosechasitems que se intersectan con el poligono de entrada
	 * @param poly ; el poligono a partir del cual se crea el cosecha Item promedio
	 * @return SimpleFeature de tipo CosechaItemStandar que represente a cosechasPoly 
	 */
	private Object[] construirFeature(List<LaborItem> siembrasPoly, Polygon poly) {
		Object[] ret = new Object[2];
		ret[0]=poly;
		ret[1]=0d;
		if(siembrasPoly.size()<1){
			return ret;
		}

		List<Geometry> intersections = new ArrayList<Geometry>();
		// sumar todas las supferficies, y calcular el promedio ponderado de cada una de las variables por la superficie superpuesta
		Geometry union = poly;
		double areaPoly = 0;
		Map<LaborItem,Double> areasIntersecciones = new HashMap<LaborItem,Double>();
		for(LaborItem cPoly : siembrasPoly){			
			//XXX si es una cosecha de ambientes el area es importante
			Geometry g = cPoly.getGeometry();
			List<Polygon> flatP = PolygonValidator.geometryToFlatPolygons(g);
			for(Polygon fp:flatP) {
				try{
					g= EnhancedPrecisionOp.intersection(poly,fp);
					Double areaInterseccion = g.getArea();
					areaPoly+=areaInterseccion;
					areasIntersecciones.put(cPoly,areaInterseccion);
					if(union==null){
						union = g;		//union es la geometria que se va a devolver al final
					}
					intersections.add(g);
				}catch(Exception e){
					System.err.println("no se pudo hacer la interseccion entre\n"+poly+"\n y\n"+fp);
				}		
			}
		}

		//	Object[] ret = new Object[2];// new HashMap<Geometry,Double>();

		double amountProm=0;
		for(LaborItem li : areasIntersecciones.keySet()){
			Double gArea = areasIntersecciones.get(li);
			if(gArea==null)continue;
			double peso = gArea/areaPoly;
			amountProm+=li.getAmount()*peso;
		}
		if(intersections.size()>0) {

			GeometryFactory fact = intersections.get(0).getFactory();
			Geometry[] geomArray = new Geometry[intersections.size()];
			GeometryCollection colectionCat = fact.createGeometryCollection(intersections.toArray(geomArray));

			try{
				union = colectionCat.convexHull();//esto hace que no se cubra el area entre polygonos a menos que la grilla sea mas grande que el area
			}catch(Exception e){

			}
			ret[0]=union;ret[1]=amountProm;
		}
		return ret;
	}

	private double getAreaMinimaLongLat() {
		return labor.config.supMinimaProperty().doubleValue()*ProyectionConstants.metersToLong()*ProyectionConstants.metersToLat();
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

	@Override
	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#,###.##");//$NON-NLS-2$
		df.setGroupingUsed(true);
		df.setGroupingSize(3);
		//densidad seeds/metro lineal
		String tooltipText = new String(Messages.getString("ProcessSiembraMapTask.1")+ df.format(siembraFeature.getDosisML()) + Messages.getString("ProcessSiembraMapTask.2")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Double seedsSup= siembraFeature.getDosisML()/labor.getEntreSurco();
		if(seedsSup<100) {//plantas por ha
			tooltipText=tooltipText.concat(df.format(seedsSup*ProyectionConstants.METROS2_POR_HA) + " s/"+ Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
		
		}else {
			tooltipText=tooltipText.concat(df.format(seedsSup) + " s/"+Messages.getString("ProcessSiembraMapTask.10")); //s/m2
		}
			//kg semillas por ha
		tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.3") + " "+df.format(siembraFeature.getDosisHa()) + Messages.getString("ProcessSiembraMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
		//fert l
		tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.5") +" "+ df.format(siembraFeature.getDosisFertLinea()) + Messages.getString("ProcessSiembraMapTask.6")		); //$NON-NLS-1$ //$NON-NLS-2$
		//fert costo
		tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.7") + " "+df.format(siembraFeature.getImporteHa()) + Messages.getString("ProcessSiembraMapTask.8")		); //$NON-NLS-1$ //$NON-NLS-2$

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.9")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessSiembraMapTask.10")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.11")+df.format(area ) + Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	
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
