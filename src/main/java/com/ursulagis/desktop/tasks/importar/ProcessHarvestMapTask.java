package com.ursulagis.desktop.tasks.importar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.api.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ProcessHarvestMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	private int cantidadDistanciasEntradaRegimen =0;
	private int cantidadDistanciasTolerancia =0;

	private int cantidadVarianzasTolera =0;
	private Double toleranciaCoeficienteVariacion =Double.valueOf(0.0);
	private Double supMinimaHas =  Double.valueOf(0.0);

	double maxRinde = Double.MAX_VALUE;
	double minRinde = 0;//no tiene sentido rindes negativos

	Coordinate lastA = null, lastB = null;
	private Point lastX=null;
	private double lastRumbo;

	double distanciaAvanceProm = 0;
	private int puntosEliminados=0;

	private List<Geometry> geomBuffer = new ArrayList<Geometry>();
	private List<Double> heightBuffer = new ArrayList<Double>();
	private List<CosechaItem> itemBuffer = new ArrayList<CosechaItem>();
	public ProcessHarvestMapTask(CosechaLabor cosechaLabor){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		labor.clearCache();
		supMinimaHas = cosechaLabor.getConfiguracion().supMinimaProperty().get()/ProyectionConstants.METROS2_POR_HA;//5
		cantidadDistanciasEntradaRegimen = cosechaLabor.getConfiguracion().cantDistanciasEntradaRegimenProperty().get();//5
		cantidadDistanciasTolerancia =cosechaLabor.getConfiguracion().cantDistanciasToleraProperty().get();//10

		cantidadVarianzasTolera =cosechaLabor.getConfiguracion().nVarianzasToleraProperty().get();
		toleranciaCoeficienteVariacion =cosechaLabor.getConfiguracion().toleranciaCVProperty().get(); //3;//9;

		maxRinde = labor.maxRindeProperty.doubleValue()!=0?labor.maxRindeProperty.doubleValue():maxRinde;
		minRinde = labor.minRindeProperty.doubleValue();
	}

	public void doProcess() throws IOException {
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =null;
		String mappableColumn = null;

		if(labor.getInStore()!=null){
			if(labor.outCollection!=null)labor.outCollection.clear();
			reader = labor.getInStore().getFeatureReader();
			featureCount=labor.getInStore().getFeatureSource().getFeatures().size();

			List<AttributeDescriptor> descriptors = labor.getInStore().getSchema().getAttributeDescriptors();
			for(AttributeDescriptor att:descriptors){
				String colName = att.getName().toString();

				System.out.println(att.getType().getBinding().getName()+": "+colName);
				if("Mappable".equalsIgnoreCase(colName)){ 
					mappableColumn=colName;	
				}
			}

		} else {//editando
			if(labor.getInCollection() == null){//solo cambio la inCollection por la outCollection la primera vez
				labor.setInCollection(labor.outCollection);
				labor.outCollection=  new DefaultFeatureCollection("internal",labor.getType()); //$NON-NLS-1$
			}
			// cuando es una grilla los datos estan en outstore y instore es null
			// si leo del outCollection y luego escribo en outCollection me quedo sin memoria
			reader = labor.getInCollection().reader();
			labor.outCollection.clear();
			featureCount=labor.getInCollection().size();
		}

		System.out.println("procesando una cosecha con "+featureCount+" elementos");
		int divisor = 1;
		if(labor.getConfiguracion().correccionOutlayersEnabled()){
			divisor =2;
		}

		while (readerHasNext(reader)) {//reader.hasNext() tira un NegativeArrayException
			featureNumber++;
			updateProgress(featureNumber/divisor, featureCount);

			SimpleFeature simpleFeature = reader.next();
			// si simpleFeature contiene la columna Mappable solo quedarme con los registros donde Mappable==1
			//al convertir de flow a rinde tomar en cuenta la columna Moisture_s 0~20
			if(mappableColumn != null ){
				Object mappable = simpleFeature.getAttribute(mappableColumn);
				Double mappableValue = Double.valueOf(1);
				if(!mappableValue.equals(mappable)){//OK! Funciona
					System.out.println("descartando el registro por no ser mapeable mappable="+mappable);
					continue;
				}
			}
			CosechaItem ci = labor.constructFeatureContainer(simpleFeature);
			distanciaAvanceProm = (ci.getDistancia()+ distanciaAvanceProm*featureNumber)/(featureNumber+1); 
			Double rumbo = ci.getRumbo();
			Double ancho = ci.getAncho();		
			Double distancia = ci.getDistancia();
			Double elevacion = ci.getElevacion();
			Object geometry = ci.getGeometry();

			/**
			 * si la geometria es un point procedo a poligonizarla
			 */
			if (geometry instanceof Point) {
				Point longLatPoint = (Point) geometry;
				ProyectionConstants.setLatitudCalculo(longLatPoint.getY());
				rumbo = corregirRumbo(longLatPoint,rumbo);
				ci.setRumbo(rumbo);			
				Geometry longLatGeom = createGeomPoint(longLatPoint, ancho, distancia, rumbo, elevacion);
				if(longLatGeom!=null)longLatGeom = removerSuperposiciones(longLatGeom);

				//la geometria puede ser null si en ancho el 0.0 (la maquina reporta ancho cero cuando esta con el cabezal en alto)
				if(longLatGeom == null 
						//			|| geom.getArea()*ProyectionConstants.A_HAS()*10000<labor.config.supMinimaProperty().doubleValue()
						){//con esto descarto las geometrias muy chicas
					System.out.println("geom es null, ignorando...");
					continue;
				}				

				/**
				 * solo ingreso la cosecha al arbol si la geometria es valida
				 */
				boolean empty = longLatGeom.isEmpty();
				boolean valid = longLatGeom.isValid();
				boolean big = (ProyectionConstants.A_HAS(longLatGeom.getArea())>supMinimaHas);
				if(!empty
						&&valid
						&&big//esta fallando por aca
						){				

					if(labor.getConfiguracion().correccionDemoraPesadaEnabled()){
						int n=labor.getConfiguracion().getCorrimientoPesada().intValue();
						double elev = ci.getElevacion();
						if(n>0){
							//tomar los primeros n rindes y ponerlos en el rindesBuffer							
							geomBuffer.add(longLatGeom);
							heightBuffer.add(elev);
							//a partir del punto n cambiar el rinde de cosechaFeature por el rinde(0) de rindesBuffer
							longLatGeom =geomBuffer.get(0);
							Double heightGeom= heightBuffer.get(0);
							ci.setElevacion(heightGeom);
							if(geomBuffer.size()>n){
								geomBuffer.remove(0);
								heightBuffer.remove(0);
							}
						} else if(n<0){//demora inversa
							itemBuffer.add(ci);
							ci=itemBuffer.get(0);
							ci.setElevacion(elev);
							if(itemBuffer.size()>Math.abs(n)){
								itemBuffer.remove(0);
							}
						}
					}					
					ci.setGeometry(longLatGeom);
					corregirRinde(ci);
					labor.insertFeature(ci);
				} else{
					//System.out.println("no inserto la feature "+ci+" "+empty+" "+valid+" "+big );


// ProcessHarvestMapTask.2=no inserto el feature 
// ProcessHarvestMapTask.3=\ porque tiene una geometria empty=
// ProcessHarvestMapTask.4=\ valid =
// ProcessHarvestMapTask.5=\ big=
// ProcessHarvestMapTask.6=\ puntos eliminados por punto duplicado

					System.out.println(
						"no inserto el feature "
						+featureNumber+" porque tiene una geometria empty="
						+empty
						+" valid ="
						+valid
						+" big="
						+big); 
				}

			} else { // no es point. Estoy abriendo una cosecha de poligonos.
				
				double area = ci.getGeometry().getArea();
				double has = ProyectionConstants.A_HAS(area);

				if(has>supMinimaHas){
					labor.insertFeature(ci);//es posible que no se inserte si ya existe el id
				}else{
					System.out.println("descarto el punto por area menor al minimo. "+ci);
				}
				/*
				List<Polygon> mp = getPolygons(ci);	
				if(mp.size()>0) {
					for(Polygon p:mp) {
					//Polygon p = mp.get(0);//porque solo el cero?

					for(Coordinate c :p.getCoordinates()){
						c.z=ci.getElevacion();
					}
					ci.setGeometry(p);	
					// si el filtro de superposiciones esta activado tambien sirve para los poligonos
					double area = ci.getGeometry().getArea();
					double has = ProyectionConstants.A_HAS(area);

					if(has>supMinimaHas){
						labor.insertFeature(ci);//es posible que no se inserte si ya existe el id
					}else{
						System.out.println("descarto el punto por area menor al minimo. "+ci);
					}
					}
				}*/
			}

		}// fin del while que recorre las features	
		labor.clearCache();
		reader.close();

		System.out.println(+puntosEliminados+Messages.getString("ProcessHarvestMapTask.6"));	
		if(labor.getConfiguracion().correccionOutlayersEnabled()){
			System.out.println(Messages.getString("ProcessHarvestMapTask.7")+toleranciaCoeficienteVariacion); //$NON-NLS-1$
			corregirOutlayersParalell();		
		} else { 
			System.out.println(Messages.getString("ProcessHarvestMapTask.8")); //$NON-NLS-1$
		}
		CosechaConfig config = (CosechaConfig)labor.getConfiguracion();

		labor.constructClasificador();
		if(config.resumirGeometriasProperty().getValue()){
			resumirGeometrias();
		}

		//resumir geometrias pero en base a la altimetria y dibujar los contornos en otra capa		
		runLater(this.getItemsList());		

		updateProgress(0, featureCount);
		//		System.out.println("min: (" + minX + "," + minY + ") max: (" + maxX
		//				+ "," + maxY + ")");
	}



	private Double corregirRumbo(Point longLatPoint, Double rumbo) {
		rumbo=rumbo>0?rumbo:(rumbo+360);
		if(lastX!=null && labor.getConfiguracion().correccionDistanciaEnabled() ) {
			double dist = ProyectionConstants.getDistancia(lastX, longLatPoint);
			if(dist<distanciaAvanceProm*cantidadDistanciasTolerancia) {
				rumbo=ProyectionConstants.getRumbo(lastX, longLatPoint);
			}
		}
		lastX=longLatPoint;
		return rumbo;
	}

	private List<CosechaItem> resumirGeometrias() {
		//antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual

		// inicializo la lista de las features por categoria
		List<List<SimpleFeature>> itemsByCat = new ArrayList<List<SimpleFeature>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			itemsByCat.add(i, new ArrayList<SimpleFeature>());
		}
		// recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			CosechaItem ci = this.labor.constructFeatureContainerStandar(f, false);
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			itemsByCat.get(cat).add(f);
		}
		it.close();

		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<CosechaItem> itemsCategoria = new ArrayList<CosechaItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection(Messages.getString("ProcessHarvestMapTask.9"),labor.getType());		 //$NON-NLS-1$
		// pasar esto a parallel streams
		// por cada categoria 
		for(int catIndex=0; catIndex < itemsByCat.size(); catIndex++){
			List<Geometry> geometriesCat = new ArrayList<Geometry>();

			//	Geometry slowUnion = null;
			Double sumRinde=0.0;
			Double sumatoriaAltura=0.0;
			int n=0;
			for(SimpleFeature f : itemsByCat.get(catIndex)){//por cada item de la categoria i
				Object geomObj = f.getDefaultGeometry();
				geometriesCat.add((Geometry)geomObj);
				sumRinde+=LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO));
				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(Labor.COLUMNA_ELEVACION));
				n++;
			} 
			double rindeProm =sumRinde/n;//si n ==o rindeProme es Nan
			double elevProm = sumatoriaAltura/n;

			double sumaDesvio2 = 0.0;
			for(SimpleFeature f:itemsByCat.get(catIndex)){
				double cantidadCosecha = LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO));	
				sumaDesvio2+= Math.abs(rindeProm- cantidadCosecha);
			}

			double desvioPromedio = sumaDesvio2/n;
			if(n > 0){//si no hay ningun feature en esa categoria esto da out of bounds
				double bufer= ProyectionConstants.metersToLongLat(0.25);


				//				GeometryFactory fact = geometriesCat.get(0).getFactory();
				//				GeometryCollection colectionCat = fact.createGeometryCollection(
				//						GeometryFactory.toGeometryArray(geometriesCat));
				//	Geometry[] geomArray = new Geometry[geometriesCat.size()];
				//	GeometryCollection colectionCat = fact.createGeometryCollection(geometriesCat.toArray(geomArray));

				Geometry buffered = null;

				try{
					System.out.println("haciendo cascade union");
					buffered = CascadedPolygonUnion.union(geometriesCat);
					//buffered = at.transform(colectionCat);
					buffered = buffered.buffer(bufer,1,BufferParameters.CAP_SQUARE);//sino le pongo buffer al resumir geometrias me quedan rectangulos medianos
					Geometry boundary = buffered.getBoundary();
					boundary=boundary.buffer(bufer,1,BufferParameters.CAP_SQUARE);
					buffered = buffered.difference(boundary);
					//buffered = colectionCat.buffer(0,1,BufferParameters.CAP_SQUARE);
					//	buffered = inverse.transform(buffered);
					//buffered = buffered.buffer(-bufer,1,BufferParameters.CAP_ROUND);
					//	buffered =buffered.buffer(bufer);
				}catch(Exception e){
					System.out.println("probando union con parallelStream"); 	
					try {
						Geometry[] array =	geometriesCat.parallelStream().collect(() -> new Geometry[1],
								(unionArray, geom) -> {
									try {
										Geometry union1 = unionArray[0];
										if(union1==null) {
											union1=geom;
										}else {
											union1 = union1.union(geom);	
										}
										unionArray[0]=union1;
									}catch(Exception error_uniendo) {
										error_uniendo.printStackTrace();
										System.out.println("skipping union of "+geom);
										if(unionArray[0]==null) {
											unionArray[0]=geom;
										}else {
											//nada
										}
									}
								},
								(unionArray1, unionArray2) ->{
									Geometry union1 = unionArray1[0];
									Geometry union2 = unionArray2[0];
									if(union1==null) {
										union1=union2;
									}else {
										union1 = union1.union(union2);	
									}
									unionArray1[0]=union1;//.union(bufer,1,BufferParameters.CAP_SQUARE);//no junta los polis
								}	
								);
						buffered=array[0].buffer(bufer,1,BufferParameters.CAP_SQUARE);
						//	buffered=buffered.buffer(-bufer,1,BufferParameters.CAP_ROUND);
					}catch(Exception e3) {
						System.out.println("fallo unir poligonos en parallell stream");
						e3.printStackTrace();
						for(Geometry gu: geometriesCat) {
							try {
								if(buffered==null) {
									buffered=gu;
								}else {
									buffered=buffered.union(gu);
								}
							}catch(Exception e4) {
								e4.printStackTrace();
							}
						}

					}
					//}
				}
				//buffered=GeometryHelper.simplify(buffered);
				//				Densifier densifier = new Densifier(buffered);
				//				densifier.setDistanceTolerance(ProyectionConstants.metersToLongLat(2));
				//				buffered=densifier.getResultGeometry();
				//				//buffered = TopologyPreservingSimplifier.simplify(buffered, ProyectionConstants.metersToLongLat(2));
				//				buffered = DouglasPeuckerSimplifier.simplify(buffered, ProyectionConstants.metersToLongLat(5));


				SimpleFeature fIn = itemsByCat.get(catIndex).get(0);
				// recorrer buffered y crear una feature por cada geometria de la geometry collection
				for(int igeom=0;buffered != null && 
						igeom < buffered.getNumGeometries(); igeom++){//null pointer exception at tasks.importar.ProcessHarvestMapTask.resumirGeometrias(ProcessHarvestMapTask.java:468)
					Geometry g = buffered.getGeometryN(igeom);

					CosechaItem ci=labor.constructFeatureContainerStandar(fIn,true);
					ci.setRindeTnHa(rindeProm);
					ci.setDesvioRinde(desvioPromedio);
					ci.setElevacion(elevProm);
					ci.setGeometry(g);

					itemsCategoria.add(ci);
					SimpleFeature f = ci.getFeature(labor.getFeatureBuilder());
					boolean ret = newOutcollection.add(f);
					if(!ret) {
						System.err.println("no se pudo agregar la feature id "+ci.getId()+" en ProcessHarvestMapTask.resumirGeometrias" );
					}
				}
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
	//	private CosechaItem sintentizarCosechasIdemCatEnContacto(List<CosechaItem> cosechasItemAUnir){		
	//		GeometryFactory fact = new GeometryFactory();
	//		int n = cosechasItemAUnir.size();
	//		Geometry[] geomArray = new Geometry[n];
	//		GeometryCollection colectionCat = fact.createGeometryCollection(geomArray);//error de casteo
	//		Geometry buffered = colectionCat.union();		
	//
	//		Double sumRinde=0.0;
	//		Double sumatoriaElevacion=0.0;
	//
	//		for(CosechaItem cosechaAUnir:cosechasItemAUnir){
	//			sumRinde+=cosechaAUnir.getRindeTnHa();
	//			sumatoriaElevacion += cosechaAUnir.getElevacion();
	//		}
	//
	//		CosechaItem cosechaSintetica = new CosechaItem();
	//		cosechaSintetica.setRindeTnHa(sumRinde/n);
	//		cosechaSintetica.setElevacion(sumatoriaElevacion/n);	
	//		cosechaSintetica.setGeometry(buffered);
	//		return cosechaSintetica;
	//
	//	}

	private void corregirRinde(CosechaItem cosechaFeature) {
		if(labor.getConfiguracion().correccionRindeAreaEnabled()){
			//corregir el rinde de a cuerdo a la diferencia de superficie
			Geometry p = cosechaFeature.getGeometry();

			//todo usar para calcular la produccion total antes de la superposicion sin la correccion de ancho

			//si el ancho de la cosecha era dinamico no hay mucha correccion. depende de la cosechadora
			Double anchoOrig =cosechaFeature.getAncho();
			//al usar el ancho orignal en vez del ancho corregido para la
			//correccion descarto la posibilidad de corregir el error de imputacion de ancho. aunque si corrijo la geometria

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
		//1) crear un circulo de radio a definir y centro en el centroide de la cosecha
		double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();

		int initOutCollectionSize = labor.outCollection.size();
		SimpleFeature[] arrayF = new SimpleFeature[labor.outCollection.size()];
		labor.outCollection.toArray(arrayF);
		List<SimpleFeature> outFeatures = Arrays.asList(arrayF);
		List<SimpleFeature>  filteredFeatures = outFeatures.parallelStream().collect(
				()-> new  ArrayList<SimpleFeature>(),
				(list, pf) ->{		
					try{
						CosechaItem cosechaFeature = labor.constructFeatureContainerStandar(pf,false);
						Point X = cosechaFeature.getGeometry().getCentroid();	
						Polygon poly = createGeomPoint(X,ancho,ancho,0,0);
						List<CosechaItem> features = labor.cachedOutStoreQuery(poly.getEnvelopeInternal());
						if(features.size()>0){						
							outlayerCV(cosechaFeature, poly,features);		
							//XXX porque creo un featureBuilder por cada feature? sera para que pueda hacerlo en paralelo?
							SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(labor.getType());
							SimpleFeature f = cosechaFeature.getFeature(fBuilder);
							list.add(f);	
							//This method is safe to be called from any thread.	
							//updateProgress((list.size()+featureCount)/2, featureCount);
						} else{
							System.out.println("la query devolvio cero elementos"); //$NON-NLS-1$
						}
					}catch(Exception e){
						System.err.println("error en corregirOutliersParalell"); //$NON-NLS-1$
						e.printStackTrace();
					}
				},	(list1, list2) -> list1.addAll(list2));
		// esto termina bien. filteredFeatures tiene 114275 elementos como corresponde

		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		 //$NON-NLS-1$
		boolean res = newOutcollection.addAll(filteredFeatures);
		if(!res){
			System.out.println("fallo el addAll(filteredFeatures)"); 
		}

		labor.clearCache();

		int endtOutCollectionSize = newOutcollection.size();
		if(initOutCollectionSize !=endtOutCollectionSize){
			System.err.println("se perdieron elementos al hacer el filtro de outlayers. init="
					+initOutCollectionSize
					+" end="+endtOutCollectionSize); 
		}
		labor.setOutCollection(newOutcollection);
		featureCount=labor.outCollection.size();
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
		double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();
		//la distancia no deberia ser mayor que 2^1/2*ancho, me tomo un factor de 10 por seguridad e invierto la escala para tener mejor representatividad
		//en vez de tomar de 0 a inf, va de ancho*(10-2^1/2) a 0
		ancho = Math.sqrt(2)*ancho;



		for(CosechaItem cosecha : features){
			double cantidadCosecha = cosecha.getAmount();	
			Geometry geo2 = cosecha.getGeometry().getCentroid();
			double distancia =geo.distance(geo2)/ProyectionConstants.metersToLat();

			double distanciaInvert = (ancho-distancia);
			if(distanciaInvert<0)System.out.println(Messages.getString("ProcessHarvestMapTask.19")+distanciaInvert); //$NON-NLS-1$
			//los pesos van de ~ancho^2 para los mas cercanos a 0 para los mas lejanos
			double weight =  Math.pow(distanciaInvert,2);	
			if(isBetweenMaxMin(cantidadCosecha)){
				sumatoriaAltura+=cosecha.getElevacion()*weight;
				sumatoriaRinde+=cantidadCosecha*weight;
				divisor+=weight;		
			}			
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
			System.out.println(Messages.getString("ProcessHarvestMapTask.20")+ divisor); //$NON-NLS-1$
			System.out.println(Messages.getString("ProcessHarvestMapTask.21")+sumatoriaRinde); //$NON-NLS-1$
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
		boolean ret = minRinde<=cantidadCosecha && cantidadCosecha<=maxRinde;
		if(!ret){
			//	System.out.println(cantidadCosecha+">"+labor.maxRindeProperty.doubleValue()+" o <"+labor.minRindeProperty.doubleValue());
		}
		return ret;
	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}



	/**
	 * 
	 * @param center centro del poligono a dibujar
	 * @param ancho
	 * @param distancia
	 * @param rumbo
	 * @param elevacion
	 * @return
	 */
	private Polygon createGeomPoint(Point center,double ancho, double distancia,double rumbo,double elevacion) {
		try {
			GeometryFactory fact = ProyectionConstants.getGeometryFactory();
			Polygon ret=null;
			//partir de center y calcular distancia/2 
			Point d = ProyectionConstants.getPoint(center,  rumbo, distancia/2);
			Point a = ProyectionConstants.getPoint(center,  (rumbo+90)%360, ancho/2);

			//       adelante
			// A ^^^^^^^^^^^^^^^ B
			//          |
			// D ^^^^^^^^^^^^^^^ C
			//        atras

			//center+(distancia-center)-(ancho-center)=center-distancia+ancho
			Coordinate A = new Coordinate(center.getX()+d.getX()-a.getX(),
					center.getY()+d.getY()-a.getY(),elevacion);
			//center+(distancia-center)+(ancho-center)=-center+distancia+ancho
			Coordinate B =  new Coordinate(-center.getX()+d.getX()+a.getX(),
					-center.getY()+d.getY()+a.getY(),elevacion);
			//center-(distancia-center)+(ancho-center)=center-distancia+ancho
			Coordinate C = new Coordinate(center.getX()-d.getX()+a.getX(),
					center.getY()-d.getY()+a.getY(),elevacion);
			//center-(distancia-center)-(ancho-center)=3*center-distancia+ancho
			Coordinate D = new Coordinate(3*center.getX()-d.getX()-a.getX(),
					3*center.getY()-d.getY()-a.getY(),elevacion);


			if (labor.getConfiguracion().correccionDistanciaEnabled() 
					&& cantidadDistanciasTolerancia > 0) {
				if (lastA != null ) {
					//verificar que el delta de rumbo sea menor a 90
					double ang =Math.abs(lastRumbo-rumbo);
					boolean check = Math.tan(Math.toRadians(ang))<distancia/(ancho/2);
					if(ang<45 && check) {
						//System.out.println("rumbo="+rumbo+" lasRumbo="+lastRumbo+" dif="+Math.abs(lastRumbo-rumbo));

						double distD = ProyectionConstants.getDistancia(
								fact.createPoint(D)
								, fact.createPoint(lastA));
						if(distD < cantidadDistanciasTolerancia*distanciaAvanceProm) {						
							//D=lastA;// = A;//A se convierte en D del siguiente			
							if(!verificarCruce(A,B,C,lastA)) {
								D=lastA;
								//A=lastA;
							}
							//C=lastB;// = B;//B se convierte en C del siguiente
						}
						double distC = ProyectionConstants.getDistancia(
								fact.createPoint(C)
								, fact.createPoint(lastB));
						if(distC < cantidadDistanciasTolerancia*distanciaAvanceProm) {
							//D=lastA;// = A;//A se convierte en D del siguiente

							if(!verificarCruce(A,B,lastB,D)) {
								C=lastB;// = B;//B se convierte en C del siguiente
							}
						}
					}
				} 			
			}//fin corregir distancia
			// corregir distancia y entrada en regimen
			boolean esNuevaPasada=Math.abs(lastRumbo-rumbo)>90;
			esNuevaPasada=esNuevaPasada||(lastA==null);
			lastA = A;//A se convierte en D del siguiente
			lastB = B;//B se convierte en C del siguiente
			lastRumbo =rumbo;

			if(labor.getConfiguracion().correccionDemoraPesadaEnabled() && esNuevaPasada){
				System.out.println("corrigiendo entrada en regimen");
				double distEntradaRegimen = distanciaAvanceProm*cantidadDistanciasEntradaRegimen;
				C = ProyectionConstants.getPoint(fact.createPoint(C), rumbo+180, distEntradaRegimen).getCoordinate();
				D = ProyectionConstants.getPoint(fact.createPoint(D), rumbo+180, distEntradaRegimen).getCoordinate();
			}

			Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
			ret = fact.createPolygon(coordinates);
			return ret;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param A
	 * @param B
	 * @param C
	 * @param D
	 * @return devuelve true si AB se cruza con CD
	 */
	private boolean verificarCruce(Coordinate A,Coordinate B,Coordinate C,Coordinate D) {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Coordinate[] coordinatesAB = { A, B };
		LineString AB = fact.createLineString(coordinatesAB);
		Coordinate[] coordinatesCD = { C, D };
		LineString CD = fact.createLineString(coordinatesCD);
		boolean intersects = AB.intersects(CD);
		return intersects;
	}

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	//	private List<Polygon> construirGrilla(BoundingBox bounds,double ancho) {
	//		System.out.println(Messages.getString("ProcessHarvestMapTask.39")); //$NON-NLS-1$
	//		List<Polygon> polygons = new ArrayList<Polygon>();
	//		//convierte los bounds de longlat a metros
	//
	//		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong() - ancho/2;
	//		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat() - ancho/2;
	//		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong()+ ancho/2;
	//		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat()+ ancho/2;
	//		Double x0=minX;
	//		for(int x=0;(x0)<maxX;x++){
	//			x0=minX+x*ancho;
	//			Double x1=minX+(x+1)*ancho;
	//			for(int y=0;(minY+y*ancho)<maxY;y++){
	//				Double y0=minY+y*ancho;
	//				Double y1=minY+(y+1)*ancho;
	//
	//
	//				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
	//				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
	//				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
	//				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
	//
	//				/**
	//				 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
	//				 * carro--B
	//				 * 
	//				 */
	//				Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
	//				// Empezar y terminar en
	//				// el mismo punto.
	//				// sentido antihorario
	//
	//
	//				GeometryFactory fact =ProyectionConstants.getGeometryFactory();
	//
	//				LinearRing shell = fact.createLinearRing(coordinates);
	//				LinearRing[] holes = null;
	//				Polygon poly = new Polygon(shell, holes, fact);			
	//				polygons.add(poly);
	//			}
	//		}
	//		return polygons;
	//	}

	/**
	 * 
	 * @param poly geometria a agregar quitando superposiciones
	 * @return devuelve la parte de poly que no se superpone con las geometrias procesadas anteriormente
	 */
	private Geometry removerSuperposiciones(Geometry poly) {
		/*
		 * ahora que tengo el poligono lo filtro con los anteriores para
		 * corregir
		 */
		Geometry difGeom = poly;
		Geometry geometryUnion = null;
		if(poly!=null && labor.getConfiguracion().correccionSuperposicionEnabled()){
			Geometry longlatPoly = poly;// crsAntiTransform(poly);
			Envelope query = longlatPoly.getEnvelopeInternal();		//hago la query en coordenadas long/lat
			List<CosechaItem> objects = labor.cachedOutStoreQuery(query);
			//si uso cached tengo que actualizarlo al hacer insert o no anda
			//System.out.println("la cantidad de objects para construir la geometria es "+objects.size());
			//si el rinde es menor asumo que fue cosechado despues y por lo tanto no corresponde quitarselo a la geometria
			//			objects = objects.stream()
			//					.filter(c ->  c.getAmount()> rindeCosecha).collect(Collectors.toList());
			//esto funciona bien pero cuando los rindes son similares evita que corte poligonos que corresponde cortar
			// marcar los objects que no entraron en la lista final como elementos a volver a repasar al final

			// si la geometria tiene elevacion esto genera errores. hacer la superposicion sin elevacion y luego volver a aplicar la elevacion?
			geometryUnion = getUnion(ProyectionConstants.getGeometryFactory(), objects, poly);
			try {			
				if (geometryUnion != null 
						&& geometryUnion.isValid() ){
					Geometry polyG =poly;
					//creo que no puedo hacer la superposicion de 2 poligonos que no son coplanares
					difGeom = polyG.difference(geometryUnion);// Computes a Geometry//found non-noded intersection between LINESTRING ( -61.9893807883
				}
				difGeom = makeGood(difGeom);
			} catch (Exception te) {
				try{
					difGeom = EnhancedPrecisionOp.difference(poly, geometryUnion);
				}catch(Exception e){
					difGeom=poly;
				}
			}
		}//fin de corregir superposiciones
		return difGeom;
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