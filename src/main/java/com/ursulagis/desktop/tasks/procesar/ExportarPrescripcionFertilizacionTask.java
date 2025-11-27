package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.api.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;


/**
 * para el cufia la semilla es la 3ra culumna, los datos tienen que estar en enteros
 * diferencia entre AGFusion y 
 * FGS (mas nuevo: permite mas flexibilidad en cantidad de zonas y cantidad de columnas. 
 * permite cargar las prescripciones en diferentes mapas) 
 * @author quero
 *
 */
public class ExportarPrescripcionFertilizacionTask extends ProgresibleTask<File>{
	private static final int MAX_ITEMS = 100;
	private FertilizacionLabor laborToExport=null;
	private File outFile=null;
	public boolean guardarConfig=true;
	private static Double COEF_EXPANCION = Math.sqrt(2);

	public ExportarPrescripcionFertilizacionTask(FertilizacionLabor laborToExport,File shapeFile) {
		super();
		this.laborToExport=laborToExport;
		this.outFile=shapeFile;
		super.updateTitle(taskName);
		this.taskName= laborToExport.getNombre();
	}

	public File call() {
		this.run(this.laborToExport,this.outFile);
		return outFile;
	}	

	public void run(FertilizacionLabor laborToExport,File shapeFile) {		
		List<LaborItem> items = getItems(laborToExport);
		int zonas = items.size();
		updateProgress(0, zonas);
		if(zonas >= MAX_ITEMS) {			
			//si no esta ambientado lo ambiento.
			items = resumirGeometrias(laborToExport);
			//si la ambientacion genera mas de 100 zonas trata de reabsorver zonas chicas
			reabsorverZonasChicas(items);
		}

		SimpleFeatureType type = constructType();		
		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok

		super.updateTitle("exportando");
		updateProgress(0, items.size());		
		int exportSize=0;
		for(LaborItem i:items) {
			FertilizacionItem fi=(FertilizacionItem) i;
			Geometry itemGeometry=fi.getGeometry();

			List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
			if(flatPolygons.size()>1) {
				System.out.println("flatPoligons es mas de 1 deberia ser 1 "+flatPolygons.size());
			}

			for(Polygon p : flatPolygons){
				//				if(p.getNumGeometries()>50) {
				//					//quedarse con las 50 mas grandes
				//				}

				p=(Polygon)GeometryHelper.douglassPeuckerSimplify(p);//esto hace que sea mas liviano
				Double dosisHa = fi.getDosistHa();
				SimpleFeature exportFeature = fb.buildFeature(null, new Object[]{p,dosisHa,0,0});
				if(exportSize<MAX_ITEMS) {
					boolean ret = exportFeatureCollection.add(exportFeature);
					if(!ret) {
						System.err.println("no se pudo ingresar la feature "+i.getId()+" ");
					}else {
						exportSize++;						
					}
				}else {
					System.err.println("tratando de agregar mas items de los maximos "+exportSize);
				}

			}
			updateProgress(exportFeatureCollection.size(), items.size());
		}

		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1 
		SimpleFeatureSource featureSource = null;
		try {
			String typeName = newDataStore.getTypeNames()[0];
			featureSource = newDataStore.getFeatureSource(typeName);
		} catch (IOException e) {

			e.printStackTrace();
		}

		super.updateTitle("escribiendo el archivo");
		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
			Transaction transaction = new DefaultTransaction("create");
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */

			try {
				featureStore.setFeatures(exportFeatureCollection.reader());
				try {
					transaction.commit();//Current fid index is null, next must be called before write()
				} catch (Exception e1) {
					e1.printStackTrace();
				}finally {
					try {
						transaction.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}		

		System.out.println("despues de guardar el shp el schema es: "+ shapeFile); //$NON-NLS-1$
		try {
			long bytes = Files.size(shapeFile.toPath());
			long kilobytes = bytes/1024;
			if(kilobytes > 500) {

				Platform.runLater(()->//esto hace que quede abajo del dialogo de importar				
				{
					Alert a = new Alert(Alert.AlertType.ERROR);
					a.setContentText(String.format("El archivo generado pesa %,dKB,  en algunos monitores 512KB es lo maximo", kilobytes));
					a.showAndWait();
				});

			}
			System.out.println(String.format("%,d kilobytes", bytes / 1024));
		}catch(Exception e) {e.printStackTrace();}
		if(guardarConfig) {
			//guardar un archivo txt con la configuracion de la labor para que quede como registro de las operaciones
			Configuracion config = Configuracion.getInstance();
			config.loadProperties();
			config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
			config.save();
		}

		updateProgress(100, 100);//all done;
	}

	public SimpleFeatureType constructType() {
		SimpleFeatureType type = null;

		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":4326,"
				+ SiembraLabor.COLUMNA_DOSIS_LINEA +":java.lang.Long,"
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO +":java.lang.Long,"
				+SiembraLabor.COLUMNA_SEM_10METROS+":"+"java.lang.Long";//semilla siempre tiene que ser la 3ra columna

		System.out.println("creando type con: "+typeDescriptor); 

		try {
			type = DataUtilities.createType("PrescType", typeDescriptor); 
		} catch (SchemaException e) {
			e.printStackTrace();
		}

		System.out.println("PrescType: "+DataUtilities.encodeType(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$
		return type;
	}

	public List<LaborItem> getItems(FertilizacionLabor laborToExport) {
		List<LaborItem> items = new ArrayList<LaborItem>();		

		SimpleFeatureIterator it = laborToExport.outCollection.features();
		while(it.hasNext()){
			FertilizacionItem fi = laborToExport.constructFeatureContainerStandar(it.next(),false);
			items.add(fi);
		}
		it.close();
		return items;
	}


	//	private List<CosechaItem> resumirGeometriasCosecha(CosechaLabor labor) {
	//		// antes de proceder a dibujar las features
	//		//agruparlas por clase y hacer un buffer cero
	//		//luego crear un feature promedio para cada poligono individual
	//
	//		// inicializo la lista de las features por categoria
	//		List<List<SimpleFeature>> colections = new ArrayList<List<SimpleFeature>>();
	//		for(int i=0;i<labor.clasificador.getNumClasses();i++){
	//			colections.add(i, new ArrayList<SimpleFeature>());
	//		}
	//		// recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
	//		SimpleFeatureIterator it = labor.outCollection.features();
	//		while(it.hasNext()){
	//			SimpleFeature f = it.next();
	//			CosechaItem ci = labor.constructFeatureContainerStandar(f, false);
	//			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
	//			colections.get(cat).add(f);
	//		}
	//		it.close();
	//
	//		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
	//		List<CosechaItem> itemsCategoria = new ArrayList<CosechaItem>();//es la lista de los items que representan a cada categoria y que devuelvo
	//		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection(Messages.getString("ProcessHarvestMapTask.9"),labor.getType());		 //$NON-NLS-1$
	//		// pasar esto a parallel streams
	//		// por cada categoria 
	//		for(int i=0;i<labor.clasificador.getNumClasses();i++){
	//			List<Geometry> geometriesCat = new ArrayList<Geometry>();
	//
	//			//	Geometry slowUnion = null;
	//			Double sumRinde=0.0;
	//			Double sumatoriaAltura=0.0;
	//			int n=0;
	//			for(SimpleFeature f : colections.get(i)){//por cada item de la categoria i
	//				Object geomObj = f.getDefaultGeometry();
	//				geometriesCat.add((Geometry)geomObj);
	//				sumRinde+=LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO));
	//				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(Labor.COLUMNA_ELEVACION));
	//				n++;
	//			} 
	//			double rindeProm =sumRinde/n;//si n ==o rindeProme es Nan
	//			double elevProm = sumatoriaAltura/n;
	//
	//			double sumaDesvio2 = 0.0;
	//			for(SimpleFeature f:colections.get(i)){
	//				double cantidadCosecha = LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO));	
	//				sumaDesvio2+= Math.abs(rindeProm- cantidadCosecha);
	//			}
	//
	//			double desvioPromedio = sumaDesvio2/n;
	//			if(n>0){//si no hay ningun feature en esa categoria esto da out of bounds
	//				GeometryFactory fact = geometriesCat.get(0).getFactory();
	//				Geometry[] geomArray = new Geometry[geometriesCat.size()];
	//				GeometryCollection colectionCat = fact.createGeometryCollection(geometriesCat.toArray(geomArray));
	//
	//				Geometry buffered = null;
	//				double bufer= ProyectionConstants.metersToLongLat(0.25);
	//				try{
	//					buffered = colectionCat.union();
	//					buffered = buffered.buffer(bufer);
	//				}catch(Exception e){
	//					System.out.println(Messages.getString("ProcessHarvestMapTask.10")); //$NON-NLS-1$
	//					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
	//					try{
	//						buffered= EnhancedPrecisionOp.buffer(colectionCat, bufer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
	//					}catch(Exception e2){
	//						e2.printStackTrace();
	//					}
	//				}
	//
	//				SimpleFeature fIn = colections.get(i).get(0);
	//				// recorrer buffered y crear una feature por cada geometria de la geometry collection
	//				for(int igeom=0;buffered!=null && igeom<buffered.getNumGeometries();igeom++){//null pointer exception at tasks.importar.ProcessHarvestMapTask.resumirGeometrias(ProcessHarvestMapTask.java:468)
	//					Geometry g = buffered.getGeometryN(igeom);
	//
	//					CosechaItem ci=labor.constructFeatureContainerStandar(fIn,true);
	//					ci.setRindeTnHa(rindeProm);
	//					ci.setDesvioRinde(desvioPromedio);
	//					ci.setElevacion(elevProm);
	//
	//					ci.setGeometry(g);
	//
	//					itemsCategoria.add(ci);
	//					SimpleFeature f = ci.getFeature(labor.getFeatureBuilder());
	//					boolean res = newOutcollection.add(f);
	//				}
	//			}	
	//
	//		}//termino de recorrer las categorias
	//		labor.setOutCollection(newOutcollection);
	//
	//		return itemsCategoria;
	//	}

	/**
	 * metodo que toma la labor y devuelve una lista de los items agrupados por categoria
	 * @param labor
	 * @return
	 */
	private List<LaborItem> resumirGeometrias(FertilizacionLabor labor) {	
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		//inicializo la lista de las features por categoria
		List<List<FertilizacionItem>> itemsByCat = new ArrayList<List<FertilizacionItem>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			itemsByCat.add(i, new ArrayList<FertilizacionItem>());
		}

		//recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = labor.outCollection.features();
		boolean esDePoligonos=false;
		while(it.hasNext()){
			SimpleFeature f = it.next();
			FertilizacionItem ci = labor.constructFeatureContainerStandar(f, false);

			if(ProyectionConstants.A_HAS(ci.getGeometry().getArea())>1) {//si hay una geometria de mas de 1ha considero que es de poligonos
				esDePoligonos=true;
			}
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			itemsByCat.get(cat).add(ci);
		}
		it.close();
		updateProgress(1, 100);
		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<LaborItem> itemsResumidos = new ArrayList<LaborItem>();//es la lista de los items que representan a cada categoria y que devuelvo

		//pasar esto a parallel streams
		// por cada categoria 
		if(!esDePoligonos) {
			for(int i=0;i<labor.clasificador.getNumClasses();i++){
				List<Geometry> geometriesCat = new ArrayList<Geometry>();
				updateProgress(i+1, labor.clasificador.getNumClasses());
				//	Geometry slowUnion = null;
				Double sumRinde = 0.0;
				Double sumatoriaAltura = 0.0;
				Double sumArea = 0.0;
				for(FertilizacionItem f : itemsByCat.get(i)){//por cada item de la categoria i				
					geometriesCat.add(f.getGeometry());
					Double geomArea = f.getGeometry().getArea();
					sumRinde += f.getDosistHa()*geomArea;
					sumatoriaAltura += f.getElevacion()*geomArea;
					sumArea+=geomArea;				
				} 			

				if(sumArea>0){//si no hay ningun feature en esa categoria esto da out of bounds
					double rindeProm =sumRinde/sumArea;//si n == 0 rindeProme es Nan
					double elevProm = sumatoriaAltura/sumArea;		

					Geometry buffered = GeometryHelper.unirGeometrias(geometriesCat);

					//				GeometryFactory fact = geometriesCat.get(0).getFactory();
					//				Geometry[] geomArray = new Geometry[geometriesCat.size()];
					//				GeometryCollection colectionCat = fact.createGeometryCollection(geometriesCat.toArray(geomArray));
					//
					//				Geometry buffered = null;
					//				double buffer = ProyectionConstants.metersToLongLat(0.25);//si pongo buffer cero pierdo geometrias. pero asi se me agranda la superficie a fertilizar
					//				try{
					//					buffered = colectionCat.union();
					//					buffered = buffered.buffer(buffer);
					//				}catch(Exception e){
					//					System.out.println("hubo una excepcion uniendo las geometrias. Procediendo con precision");
					//					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					//					try{
					//						buffered = EnhancedPrecisionOp.buffer(colectionCat, buffer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					//					}catch(Exception e2){
					//						e2.printStackTrace();
					//					}
					//				}

					List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(buffered);
					for(Polygon p:flatPolygons) {
						FertilizacionItem ci=new FertilizacionItem();					
						ci.setDosistHa(rindeProm);
						ci.setElevacion(elevProm);
						ci.setGeometry(p);
						itemsResumidos.add(ci);
					}

					//FertilizacionItem fIn = itemsByCat.get(i).get(0);
					//TODO recorrer buffered y crear una feature por cada geometria de la geometry collection
					//				System.out.println("la cantidad de geometrias generadas para la clase es: "+buffered.getNumGeometries() );
					//				for(int igeom=0; buffered!=null && igeom<buffered.getNumGeometries(); igeom++){//null pointer exception at tasks.importar.ProcessHarvestMapTask.resumirGeometrias(ProcessHarvestMapTask.java:468)
					//					Geometry g = buffered.getGeometryN(igeom);
					//				
					//					FertilizacionItem ci=new FertilizacionItem();
					//					
					//					ci.setDosistHa(rindeProm);
					//					ci.setElevacion(elevProm);
					//					ci.setGeometry(g);
					//
					//					itemsCategoria.add(ci);
					//				}
				}	

			}//termino de recorrer las categorias
		} else {//si no es de poligonos
			System.out.println("ya esta ambientado");
			//			itemsCategoria = itemsByCat.parallelStream().map(catItems -> 
			//						catItems.parallelStream().map( fIn->
			//							labor.constructFeatureContainerStandar(fIn,true)
			//							).collect(Collectors.toList())
			//						).collect(Collectors.toList());
			//			
			for(List<FertilizacionItem> catItems : itemsByCat) {
				itemsResumidos.addAll(catItems);					
			}
		}
//no descartar los poligonos extra aca. primero tratar de reabsorver zonas chicas
//		if(false && itemsResumidos.size()>100) {
//			itemsResumidos.sort((i1,i2)
//					->	(-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea())));			
//			itemsResumidos =itemsResumidos.subList(0,MAX_ITEMS-1);
//		}{
//			System.out.println("descartando "+(itemsResumidos.size()-100)+" poligonos que superan los 100");
//		}
		return itemsResumidos;
	}



	/**
	 * Metodo que transforma items reabsorviendo las zonas chicas en los vecinos mas grandes
	 * @param items con max size 99
	 */
	public void reabsorverZonasChicas( List<LaborItem> items) {
		try {
			if(items.size()< MAX_ITEMS){
				return;	
			}
			//reabsorver zonas mas chicas a las mas grandes vecinas
			System.out.println("tiene mas de 100 zonas, reabsorviendo..."); 
			//tomar las 100 zonas mas grandes y reabsorver las otras en estas

			items.sort((i1,i2)
					->	(-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea())));		
			//TODO antes de extraer la sublist convertir a flatPolygons
			List<LaborItem> itemsAgrandar =items.subList(0,100-1);

			Quadtree tree=new Quadtree();
			for(LaborItem ar : itemsAgrandar) {
				Geometry gAr =ar.getGeometry();
				tree.insert(gAr.getEnvelopeInternal(), ar);
			}

			List<LaborItem> itemsAReducir =items.subList(MAX_ITEMS, items.size()-1);//java.lang.IllegalArgumentException: fromIndex(100) > toIndex(98)
			int n = 0;
			int i = itemsAReducir.size();
			super.updateTitle("reabsorver zonas chicas");
			updateProgress(0, i);
			//double distanciaMax = ProyectionConstants.metersToLongLat(this.laborToExport.getConfigLabor().getAnchoFiltroOutlayers());
			System.out.println("tratando de reducir"+ itemsAReducir.size()+" items");//TODO adjuntar otros y probar mejor suerte

			Clasificador clasificador = laborToExport.getClasificador();
			List<LaborItem> done = new ArrayList<LaborItem>();		

			while(itemsAReducir.size() > 0 && n < 10) {//trato de reducirlos 10 veces

				for(LaborItem ar : itemsAReducir) {
					Geometry gAr = ar.getGeometry();
					Integer clase = laborToExport.getClasificador().getCategoryFor(ar.getAmount());
					Envelope envelope = gAr.getEnvelopeInternal();
					List<?> vecinos = tree.query(envelope);

					int vecinosCounter = 0;
					while(vecinos.size() == 0 && vecinosCounter < 100) {//solo duplico el envelope 100 veces
						System.out.println("no hay vecino para adjuntarlo"+ ar.getId());
						//buscar en un envelope mas grande					
						envelope.expandBy(envelope.getWidth()*COEF_EXPANCION);//raiz de 2 para duplicar el area de busqueda
						vecinos = tree.query(envelope);
						vecinosCounter++;
					}	

					LaborItem vecinoIdemCatIntersect = null;
					LaborItem vecinoIntersectMasGrande = null;
					LaborItem vecinoMasCercano = null;

					List<LaborItem> vecinosIntersects = ((List<LaborItem>)vecinos).stream()
							.filter(v->v.getGeometry().intersects(gAr))
							.collect(Collectors.toList());								

					List<LaborItem> idemClaseIntersects = vecinosIntersects.stream()
							.filter(v->clasificador.getCategoryFor(v.getAmount()).equals(clase))
							.collect(Collectors.toList());				

					if(idemClaseIntersects.size()>0) {
						//System.out.println("encontre el vecino de la misma clase mas grande que intersecta");
						List<LaborItem> masGrande = idemClaseIntersects.stream()
								.sorted((v1,v2)-> Double.compare(v1.getGeometry().getArea(), v2.getGeometry().getArea()))
								.collect(Collectors.toList());
						vecinoIdemCatIntersect = masGrande.get(masGrande.size()-1);// esto es lo que buscamos
					} else if(vecinosIntersects.size()>0) {
						//	System.out.println("encontre el vecino que intersecta mas grande");//el mas comun
						List<LaborItem> masGrande = vecinosIntersects.stream()
								.sorted((v1,v2)-> Double.compare(v1.getGeometry().getArea(), v2.getGeometry().getArea()))
								.collect(Collectors.toList());
						vecinoIntersectMasGrande = masGrande.get(masGrande.size()-1);// esto es lo que buscamos
					} else {					
						//System.out.println("encontre el vecino que no intersecta mas cercano");
						//					List<LaborItem> masCercano = ((List<LaborItem>)vecinos).stream()
						//							.sorted((v1,v2)-> Double.compare(v1.getGeometry().distance(gAr), v2.getGeometry().distance(gAr)))
						//							.collect(Collectors.toList());
						//					vecinoMasCercano = masCercano.get(0);// esto es lo que buscamos
						vecinoMasCercano = ((List<LaborItem>)vecinos).stream()
								.reduce(null, (v1,v2)->{
									if(v1==null) {return v2;}
									if(v2==null) {return v1;}
									if(Double.compare(
											v1.getGeometry().distance(gAr),
											v2.getGeometry().distance(gAr)
											)<0) {
										return v1;
									}else {
										return v2;
									}
								});
					}

					LaborItem v = vecinoIdemCatIntersect != null ? vecinoIdemCatIntersect:
						(vecinoIntersectMasGrande != null ? vecinoIntersectMasGrande:
							(vecinoMasCercano != null ? vecinoMasCercano : null));

					if(v != null) {
						Geometry g = v.getGeometry();
						try {					
							Geometry gArb=gAr;
							if(!g.intersects(gAr)) {
								gArb=gAr.buffer(g.distance(gAr));
							}
							GeometryCollection colectionCat = g.getFactory().createGeometryCollection(new Geometry[]{g,gArb});
							Geometry union = EnhancedPrecisionOp.buffer(colectionCat, 0);
							if(union != null) {						
								boolean removedOK = tree.remove(g.getEnvelopeInternal(), v);
								v.setGeometry(union);
								tree.insert(union.getEnvelopeInternal(), v);
								done.add(ar);
							}
						}catch(Exception e) {
							e.printStackTrace();
						}

					} else {
						System.out.println("no econtre en esta vuelta vecino para "+ar.getId());
					}				

					updateProgress(done.size(),itemsAReducir.size());
				}
				updateProgress(i-itemsAReducir.size(),i);

				itemsAReducir.removeAll(done);
			}
			System.out.println("reduci "+ done.size()+" items de "+i);//adjuntar otros y probar mejor suerte
			System.out.println("termine de reducir con n="+n+" poligonos no reducidos "+itemsAReducir.size());
			items.clear();
			boolean res = items.addAll((List<LaborItem>)tree.queryAll());
			System.out.println("items finales "+items.size()+" cambio? "+res);
		}catch(Exception e) {
			System.err.println("falle al reducir las geometrias chicas");
			e.printStackTrace();

		}
	}



}
