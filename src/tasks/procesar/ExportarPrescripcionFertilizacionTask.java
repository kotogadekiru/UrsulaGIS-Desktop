package tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Clasificador;
import dao.Labor;
import dao.LaborItem;
import dao.config.Configuracion;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gui.Messages;
import tasks.ProgresibleTask;
import utils.FileHelper;
import utils.GeometryHelper;
import utils.PolygonValidator;
import utils.ProyectionConstants;


/**
 * para el cufia la semilla es la 3ra culumna, los datos tienen que estar en enteros
 * diferencia entre AGFusion y 
 * FGS (mas nuevo: permite mas flexibilidad en cantidad de zonas y cantidad de columnas. 
 * permite cargar las prescripciones en diferentes mapas) 
 * @author quero
 *
 */

public class ExportarPrescripcionFertilizacionTask extends ProgresibleTask<File>{
	private FertilizacionLabor laborToExport=null;
	private File outFile=null;
	
	public ExportarPrescripcionFertilizacionTask(FertilizacionLabor laborToExport,File shapeFile) {
		super();
		this.laborToExport=laborToExport;
		this.outFile=shapeFile;
		super.updateTitle(taskName);
		this.taskName= laborToExport.getNombre();
	}
	
	public void run(FertilizacionLabor laborToExport,File shapeFile) {
		SimpleFeatureType type = null;

		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":srid=4326,"//$NON-NLS-1$
				+ SiembraLabor.COLUMNA_DOSIS_LINEA +":java.lang.Long,"//java.lang.Long,"//$NON-NLS-1$
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO +":java.lang.Long,"//$NON-NLS-1$
				+SiembraLabor.COLUMNA_SEM_10METROS+":"+"java.lang.Long";//$NON-NLS-1$ semilla siempre tiene que ser la 3ra columna
		
		/*String 
		typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":srid=4326,"
				+ FertilizacionLabor.COLUMNA_DOSIS + ":java.lang.Long";
		*/
		System.out.println("creando type con: "+typeDescriptor); //$NON-NLS-1$ the_geom:Polygon:srid=4326,Fert L:java.lang.Long,Fert C:java.lang.Long,seeding:java.lang.Long
		System.out.println("Long.SIZE="+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
		try {
			type = DataUtilities.createType("PrescType", typeDescriptor); //$NON-NLS-1$
		} catch (SchemaException e) {
			e.printStackTrace();
		}

		System.out.println("PrescType: "+DataUtilities.spec(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$

		List<LaborItem> items = new ArrayList<LaborItem>();
	
		SimpleFeatureIterator it = laborToExport.outCollection.features();
		while(it.hasNext()){
			FertilizacionItem fi = laborToExport.constructFeatureContainerStandar(it.next(),false);
			items.add(fi);
		}
		it.close();
		
		int zonas = items.size();
		
		
		updateProgress(0, zonas);
		
		if(zonas>=100) {
			//if(zonas>=1000) {//no esta ambientado
			items = resumirGeometrias(laborToExport);
			//}
			reabsorverZonasChicas(items);
		}

		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok
		super.updateTitle("exportando");
		updateProgress(0, items.size());
		for(LaborItem i:items) {//(it.hasNext()){
			FertilizacionItem fi=(FertilizacionItem) i;
			Geometry itemGeometry=fi.getGeometry();
			
			List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
			
			for(Polygon p : flatPolygons){
				if(p.getNumGeometries()>50) {
					//quedarse con las 50 mas grandes
				}
			
				//p=(Polygon)GeometryHelper.removeSmallTriangles(p, (0.00005)/ProyectionConstants.A_HAS());	
				p=(Polygon)GeometryHelper.douglassPeuckerSimplify(p);
				
				fb.add(p);
				Double dosisHa = fi.getDosistHa();

			
				fb.add(dosisHa.longValue());//fertL
				fb.add(0);//fertC
				fb.add(0);//semillas10m
				

				SimpleFeature exportFeature = fb.buildFeature(fi.getId().toString());
				exportFeatureCollection.add(exportFeature);
			}
			updateProgress(exportFeatureCollection.size(), items.size());
		}
		//it.close();

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
			Transaction transaction = new DefaultTransaction("create"); //$NON-NLS-1$
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */

			try {
				featureStore.setFeatures(exportFeatureCollection.reader());
				try {
					transaction.commit();
				} catch (Exception e1) {
					e1.printStackTrace();
				}finally {
					try {
						transaction.close();
						//TODO chequear que pese menos de 512KB
						//System.out.println("closing transaction");
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
        System.out.println(String.format("%,d kilobytes", bytes / 1024));
		}catch(Exception e) {e.printStackTrace();}
		Configuracion config = Configuracion.getInstance();
		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
		config.save();
		updateProgress(100, 100);//all done;
	}
	
	
	private List<CosechaItem> resumirGeometriasCosecha(CosechaLabor labor) {
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
			CosechaItem ci = labor.constructFeatureContainerStandar(f, false);
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			colections.get(cat).add(f);
		}
		it.close();

		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<CosechaItem> itemsCategoria = new ArrayList<CosechaItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection(Messages.getString("ProcessHarvestMapTask.9"),labor.getType());		 //$NON-NLS-1$
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
				sumRinde+=LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO));
				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(Labor.COLUMNA_ELEVACION));
				n++;
			} 
			double rindeProm =sumRinde/n;//si n ==o rindeProme es Nan
			double elevProm = sumatoriaAltura/n;

			double sumaDesvio2 = 0.0;
			for(SimpleFeature f:colections.get(i)){
				double cantidadCosecha = LaborItem.getDoubleFromObj(f.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO));	
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
					buffered = buffered.buffer(bufer);
				}catch(Exception e){
					System.out.println(Messages.getString("ProcessHarvestMapTask.10")); //$NON-NLS-1$
					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					try{
						buffered= EnhancedPrecisionOp.buffer(colectionCat, bufer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}

				SimpleFeature fIn = colections.get(i).get(0);
				//TODO recorrer buffered y crear una feature por cada geometria de la geometry collection
				for(int igeom=0;buffered!=null && igeom<buffered.getNumGeometries();igeom++){//null pointer exception at tasks.importar.ProcessHarvestMapTask.resumirGeometrias(ProcessHarvestMapTask.java:468)
					Geometry g = buffered.getGeometryN(igeom);

					CosechaItem ci=labor.constructFeatureContainerStandar(fIn,true);
					ci.setRindeTnHa(rindeProm);
					ci.setDesvioRinde(desvioPromedio);
					ci.setElevacion(elevProm);

					ci.setGeometry(g);

					itemsCategoria.add(ci);
					SimpleFeature f = ci.getFeature(labor.featureBuilder);
					boolean res = newOutcollection.add(f);
				}
			}	

		}//termino de recorrer las categorias
		labor.setOutCollection(newOutcollection);

		return itemsCategoria;
	}

	/**
	 * metodo que toma la labor y devuelve una lista de los items agrupados por categoria
	 * @param labor
	 * @return
	 */
	private List<LaborItem> resumirGeometrias(FertilizacionLabor labor) {
		//TODO antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		//XXX inicializo la lista de las features por categoria
		List<List<SimpleFeature>> itemsByCat = new ArrayList<List<SimpleFeature>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			itemsByCat.add(i, new ArrayList<SimpleFeature>());
		}
		
		//XXX recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = labor.outCollection.features();
		boolean esDePoligonos=false;
		while(it.hasNext()){
			SimpleFeature f = it.next();
			FertilizacionItem ci = labor.constructFeatureContainerStandar(f, false);
		
			if(ProyectionConstants.A_HAS(ci.getGeometry().getArea())>1) {//si hay una geometria de mas de 1ha considero que es de poligonos
				esDePoligonos=true;
			}
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			itemsByCat.get(cat).add(f);
		}
		it.close();
		updateProgress(1, 100);
		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<LaborItem> itemsCategoria = new ArrayList<LaborItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		//DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection(Messages.getString("ProcessHarvestMapTask.9"),labor.getType());		 //$NON-NLS-1$
		
		//TODO pasar esto a parallel streams
		//XXX por cada categoria 
		if(!esDePoligonos) {
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			List<Geometry> geometriesCat = new ArrayList<Geometry>();
			updateProgress(i+1, labor.clasificador.getNumClasses());
			//	Geometry slowUnion = null;
			Double sumRinde=new Double(0);
			Double sumatoriaAltura=new Double(0);
			int n=0;
			for(SimpleFeature f : itemsByCat.get(i)){//por cada item de la categoria i
				Object geomObj = f.getDefaultGeometry();
				geometriesCat.add((Geometry)geomObj);
				
				sumRinde += LaborItem.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));
				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(labor.colElevacion.get()));
				n++;
			} 
			double rindeProm =sumRinde/n;//si n ==o rindeProme es Nan
			double elevProm = sumatoriaAltura/n;
			
//			double sumaDesvio2 = 0.0;
//			for(SimpleFeature f:colections.get(i)){
//				double cantidadCosecha = LaborItem.getDoubleFromObj(f.getAttribute(FertilizacionLabor.COLUMNA_KG_HA));	
//				sumaDesvio2+= Math.abs(rindeProm- cantidadCosecha);
//			}
			
	//		double desvioPromedio = sumaDesvio2/n;
			if(n>0){//si no hay ningun feature en esa categoria esto da out of bounds
				GeometryFactory fact = geometriesCat.get(0).getFactory();
				Geometry[] geomArray = new Geometry[geometriesCat.size()];
				GeometryCollection colectionCat = fact.createGeometryCollection(geometriesCat.toArray(geomArray));

				Geometry buffered = null;
				double buffer = ProyectionConstants.metersToLongLat(0.25);//si pongo buffer cero pierdo geometrias. pero asi se me agranda la superficie a fertilizar
				try{
					buffered = colectionCat.union();
					buffered = buffered.buffer(buffer);
				}catch(Exception e){
					System.out.println("hubo una excepcion uniendo las geometrias. Procediendo con precision");
					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					try{
						buffered = EnhancedPrecisionOp.buffer(colectionCat, buffer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}

				SimpleFeature fIn = itemsByCat.get(i).get(0);
				//TODO recorrer buffered y crear una feature por cada geometria de la geometry collection
				System.out.println("la cantidad de geometrias generadas para la clase es: "+buffered.getNumGeometries() );
				for(int igeom=0; buffered!=null && igeom<buffered.getNumGeometries(); igeom++){//null pointer exception at tasks.importar.ProcessHarvestMapTask.resumirGeometrias(ProcessHarvestMapTask.java:468)
					Geometry g = buffered.getGeometryN(igeom);
				
					FertilizacionItem ci=labor.constructFeatureContainerStandar(fIn,true);
					ci.setDosistHa(rindeProm);
					//ci.setDesvioRinde(desvioPromedio);
					ci.setElevacion(elevProm);

					ci.setGeometry(g);

					itemsCategoria.add(ci);
					//SimpleFeature f = ci.getFeature(labor.featureBuilder);
					//boolean res = newOutcollection.add(f);
				}
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
			for(List<SimpleFeature> catItems : itemsByCat) {
				itemsCategoria.addAll(
						catItems.parallelStream().map( fIn->
						labor.constructFeatureContainerStandar(fIn,true)
								).collect(Collectors.toList())
						);
			}
			
		}
		//labor.setOutCollection(newOutcollection);
		//FIXME esto las resume pero no garantiza que sean menos de 100
		return itemsCategoria;
	}

	
	
	//FIXME se pierden geometrias
	public void reabsorverZonasChicas( List<LaborItem> items) {
		try {
		//TODO reabsorver zonas mas chicas a las mas grandes vecinas
		System.out.println("tiene mas de 100 zonas, reabsorviendo..."); //$NON-NLS-1$
		//TODO tomar las 100 zonas mas grandes y reabsorver las otras en estas

		items.sort((i1,i2)
				->	(-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea())));		
		
		List<LaborItem> itemsAgrandar =items.subList(0,100-1);
		
		Quadtree tree=new Quadtree();
		for(LaborItem ar : itemsAgrandar) {
			Geometry gAr =ar.getGeometry();
			tree.insert(gAr.getEnvelopeInternal(), ar);
		}
		
		List<LaborItem> itemsAReducir =items.subList(100, items.size()-1);
		int n = 0;
		int i = itemsAReducir.size();
		super.updateTitle("reabsorver zonas chicas");
		updateProgress(0, i);
		//double distanciaMax = ProyectionConstants.metersToLongLat(this.laborToExport.getConfigLabor().getAnchoFiltroOutlayers());
		System.out.println("tratando de reducir"+ itemsAReducir.size()+" items");//TODO adjuntar otros y probar mejor suerte
		
		Clasificador clasificador = laborToExport.getClasificador();
		List<LaborItem> done = new ArrayList<LaborItem>();		
		while(itemsAReducir.size() > 0 && n < 10000) {//trato de reducirlos 10000 veces
		
			for(LaborItem ar : itemsAReducir) {
				Geometry gAr = ar.getGeometry();
				//Geometry buffered = gAr.buffer(ProyectionConstants.metersToLongLat)).convexHull();
				Integer clase = laborToExport.getClasificador().getCategoryFor(ar.getAmount());
				Envelope envelope = gAr.getEnvelopeInternal();
				List<LaborItem> vecinos = (List<LaborItem>) tree.query(envelope);
				int vecinosCounter = 0;
				while(vecinos.size() == 0 && vecinosCounter < 100) {//solo duplico el envelope 100 veces
					System.out.println("no hay vecino para adjuntarlo"+ ar.getId());//TODO adjuntar otros y probar mejor suerte
					//TODO buscar en un envelope mas grande
					 envelope.expandBy(envelope.getWidth()*2);
					 vecinos = (List<LaborItem>) tree.query(envelope);
					 vecinosCounter++;
				}
				
				
				LaborItem vecinoIdemCatIntersect = null;
				//LaborItem vecinoIdemCat = null;
				LaborItem vecinoIntersectMasGrande = null;
				LaborItem vecinoMasCercano = null;
				
				List<LaborItem> vecinosIntersects = vecinos.stream().filter(v->v.getGeometry().intersects(gAr)).collect(Collectors.toList());								
				
				List<LaborItem> idemClaseIntersects = vecinosIntersects.stream().filter(v->clasificador.getCategoryFor(v.getAmount()).equals(clase)).collect(Collectors.toList());				
				
				//List<LaborItem> idemClaseIntersects = vecinosIntersects.stream().filter(v->v.getGeometry().intersects(gAr)).collect(Collectors.toList());
				
				if(idemClaseIntersects.size()>0) {
					System.out.println("encontre el vecino de la misma clase mas grande que intersecta");
					List<LaborItem> masGrande = idemClaseIntersects.stream().sorted((v1,v2)-> Double.compare(v1.getGeometry().getArea(), v2.getGeometry().getArea())).collect(Collectors.toList());
					vecinoIdemCatIntersect = masGrande.get(masGrande.size()-1);// esto es lo que buscamos
				} else if(vecinosIntersects.size()>0) {
				//	System.out.println("encontre el vecino que intersecta mas grande");//el mas comun
					List<LaborItem> masGrande = vecinosIntersects.stream().sorted((v1,v2)-> Double.compare(v1.getGeometry().getArea(), v2.getGeometry().getArea())).collect(Collectors.toList());
					vecinoIntersectMasGrande = masGrande.get(masGrande.size()-1);// esto es lo que buscamos
				} else {
					System.out.println("encontre el vecino que no intersecta mas cercano");
					List<LaborItem> masCercano = vecinos.stream().sorted((v1,v2)-> Double.compare(v1.getGeometry().distance(gAr), v2.getGeometry().distance(gAr))).collect(Collectors.toList());
					vecinoMasCercano = masCercano.get(0);// esto es lo que buscamos
				}
				
				LaborItem v = vecinoIdemCatIntersect!=null?vecinoIdemCatIntersect:
					(vecinoIntersectMasGrande!=null?vecinoIntersectMasGrande:
						(vecinoMasCercano!=null?vecinoMasCercano:null));
				
				if(v!=null) {
					Geometry g = v.getGeometry();
					try {
						
						GeometryCollection colectionCat = g.getFactory().createGeometryCollection(new Geometry[]{g,gAr});
						Geometry union = EnhancedPrecisionOp.buffer(colectionCat, 0);
						//Geometry union = g.union(gAr);
						if(union!=null) {
						
						tree.remove(g.getEnvelopeInternal(), v);
						v.setGeometry(union);
						tree.insert(union.getEnvelopeInternal(), v);
						done.add(ar);
						}
					//	System.out.println("econtre en esta vuelta vecino para "+ar.getId());
					}catch(Exception e) {
						e.printStackTrace();
					}
				
				} else {
					System.out.println("no econtre en esta vuelta vecino para "+ar.getId());
				}
				
//				else if(){//no hay un vecino de igual categoria
//					
//					 List<LaborItem> masCercano = vecinos.stream().sorted((v1,v2)-> Double.compare(v1.getGeometry().distance(gAr), v2.getGeometry().distance(gAr))).collect(Collectors.toList());
//					
//				}
//				
//				for(LaborItem candidato : vecinos) {
//					Integer claseCandidato = laborToExport.getClasificador().getCategoryFor(candidato.getAmount());
//					if(clase.equals(claseCandidato)) {
//						if(candidato.getGeometry().isWithinDistance(gAr, distanciaMax)) {
//							
//						}
//					}
//					//si el candidato es de la misma clase agregarl
//					//seleccionar el vecino mas grande si no es de la misma clase
//				}
			
				
//				if(vecinos.size()>0) {
//					Stream<LaborItem> bufferStream = vecinos.stream().filter(li->gAr.distance(li.getGeometry())<100);
//					// long intersects = bufferStream.count();
//					 //System.out.println("encontrre "+intersects+" poligonos que intersectan con el buffer de la geometria");
//			
//					
//					Optional<LaborItem> opV = bufferStream.
//							reduce((v1,v2)->{//seleccionar el poligono mas grande que se intersecte o toque
////						if(v1==null || v2==null) {
////							return v1==null?v2:v1;
////						}
////						boolean v1i = gAr.intersects(v1.getGeometry());
////						boolean v2i = gAr.intersects(v2.getGeometry());
//						return  (v1.getGeometry().getArea() > v2.getGeometry().getArea() ? v1 : v2);
//					});
//					if(opV.isPresent() &&  opV.get()!=null) {
//					//	System.out.println("encontre un match");
//						LaborItem v = opV.get();
//						Geometry g = v.getGeometry();
//						tree.remove(g.getEnvelopeInternal(), v);
//						GeometryCollection colectionCat = g.getFactory().createGeometryCollection(new Geometry[]{g,gAr});
//						Geometry union = EnhancedPrecisionOp.buffer(colectionCat, 0);
//						//Geometry union = g.union(gAr);
//						v.setGeometry(union);
//						tree.insert(union.getEnvelopeInternal(), v);
//						done.add(ar);
//					} else {
//						System.out.println("no hay vecinos para absorver poligono "+n);
//						n++;//
//					}
//				}else {
//					System.out.println("no hay vecinos");
//				}
				updateProgress(done.size(),itemsAReducir.size());
			}
			updateProgress(i-itemsAReducir.size(),i);
			
			itemsAReducir.removeAll(done);
		}
		System.out.println("reduci "+ done.size()+" items de "+i);//TODO adjuntar otros y probar mejor suerte
		System.out.println("termine de reducir con n="+n+" poligonos no reducidos "+itemsAReducir.size());
		items.clear();
		boolean res = items.addAll((List<LaborItem>)tree.queryAll());
		System.out.println("items finales "+items.size()+" cambio? "+res);
		}catch(Exception e) {
			e.printStackTrace();
			System.err.println("falle al reducir las geometrias chicas");
		}
	}


	@Override
	protected File call() throws Exception {
		this.run(this.laborToExport,this.outFile);
		return outFile;
	}

//	public void exe(FertilizacionLabor laborToExport,File shapeFile)  {
//		SimpleFeatureType type = null;
//		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":srid=4326,"
//				+ FertilizacionLabor.COLUMNA_DOSIS + ":java.lang.Long";
//		
//		System.out.println("creando type con: "+typeDescriptor); //$NON-NLS-1$ 
//		System.out.println("Long.SIZE="+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
//		try {
//			type = DataUtilities.createType("PrescType", typeDescriptor); //$NON-NLS-1$
//		} catch (SchemaException e) {
//			e.printStackTrace();
//		}
//
//		System.out.println("PrescType:"+DataUtilities.spec(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$
//
//		SimpleFeatureIterator it = laborToExport.outCollection.features();
//		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
//		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok
//		while(it.hasNext()){
//			FertilizacionItem fi = laborToExport.constructFeatureContainerStandar(it.next(),false);
//			Geometry itemGeometry=fi.getGeometry();
//			List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
//			for(Polygon p : flatPolygons){
//				fb.add(p);
//				Double dosisHa = fi.getDosistHa();
//
//				System.out.println("presc Dosis = "+dosisHa); //$NON-NLS-1$
//				fb.add(dosisHa.longValue());
//
//				SimpleFeature exportFeature = fb.buildFeature(fi.getId().toString());
//				exportFeatureCollection.add(exportFeature);
//			}
//		}
//		it.close();
//
//		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1 
//		SimpleFeatureSource featureSource = null;
//		try {
//			String typeName = newDataStore.getTypeNames()[0];
//			featureSource = newDataStore.getFeatureSource(typeName);
//		} catch (IOException e) {
//
//			e.printStackTrace();
//		}
//
//
//		if (featureSource instanceof SimpleFeatureStore) {
//			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
//			Transaction transaction = new DefaultTransaction("create"); //$NON-NLS-1$
//			featureStore.setTransaction(transaction);
//
//			/*
//			 * SimpleFeatureStore has a method to add features from a
//			 * SimpleFeatureCollection object, so we use the
//			 * ListFeatureCollection class to wrap our list of features.
//			 */
//
//			try {
//				featureStore.setFeatures(exportFeatureCollection.reader());
//				try {
//					transaction.commit();
//				} catch (Exception e1) {
//					e1.printStackTrace();
//				}finally {
//					try {
//						transaction.close();
//						//System.out.println("closing transaction");
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			} catch (Exception e1) {
//				e1.printStackTrace();
//			}
//		}		
//
//		System.out.println("despues de guardar el shp el schema es: "+ shapeFile); //$NON-NLS-1$
//		Configuracion config = Configuracion.getInstance();
//		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
//		config.save();
//	}
}
