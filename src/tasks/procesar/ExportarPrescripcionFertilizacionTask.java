package tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

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

		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":srid=4326,"
				+ FertilizacionLabor.COLUMNA_DOSIS + ":java.lang.Long";
		
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
				fb.add(p);
				Double dosisHa = fi.getDosistHa();

			
				fb.add(dosisHa.longValue());

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
		Configuracion config = Configuracion.getInstance();
		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
		config.save();
		updateProgress(100, 100);//all done;
	}

	
	private  List<LaborItem> resumirGeometrias(FertilizacionLabor labor) {
		//TODO antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		//XXX inicializo la lista de las features por categoria
		List<List<SimpleFeature>> colections = new ArrayList<List<SimpleFeature>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			colections.add(i, new ArrayList<SimpleFeature>());
		}
		//XXX recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			FertilizacionItem ci = labor.constructFeatureContainerStandar(f, false);
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			colections.get(cat).add(f);
		}
		it.close();
		updateProgress(1, 100);
		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<LaborItem> itemsCategoria = new ArrayList<LaborItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection(Messages.getString("ProcessHarvestMapTask.9"),labor.getType());		 //$NON-NLS-1$
		//TODO pasar esto a parallel streams
		//XXX por cada categoria 
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			List<Geometry> geometriesCat = new ArrayList<Geometry>();
			updateProgress(i+1, labor.clasificador.getNumClasses());
			//	Geometry slowUnion = null;
			Double sumRinde=new Double(0);
			Double sumatoriaAltura=new Double(0);
			int n=0;
			for(SimpleFeature f : colections.get(i)){//por cada item de la categoria i
				Object geomObj = f.getDefaultGeometry();
				geometriesCat.add((Geometry)geomObj);
				sumRinde+=LaborItem.getDoubleFromObj(f.getAttribute(FertilizacionLabor.COLUMNA_KG_HA));
				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(Labor.COLUMNA_ELEVACION));
				n++;
			} 
			double rindeProm =sumRinde/n;//si n ==o rindeProme es Nan
			double elevProm = sumatoriaAltura/n;
			
			double sumaDesvio2 = 0.0;
			for(SimpleFeature f:colections.get(i)){
				double cantidadCosecha = LaborItem.getDoubleFromObj(f.getAttribute(FertilizacionLabor.COLUMNA_KG_HA));	
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
		//labor.setOutCollection(newOutcollection);
		//FIXME esto las resume pero no garantiza que sean menos de 100
		return itemsCategoria;
	}

	
	public void reabsorverZonasChicas( List<LaborItem> items) {
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
		int n=0;
		int i=itemsAReducir.size();
		super.updateTitle("reabsorver zonas chicas");
		updateProgress(0, i);
		while(itemsAReducir.size() > 0 || n > 100) {//trato de reducirlos 10 veces
			List<LaborItem> done = new ArrayList<LaborItem>();		
			for(LaborItem ar : itemsAReducir) {
				Geometry gAr = ar.getGeometry();
				List<LaborItem> vecinos = (List<LaborItem>) tree.query(gAr.getEnvelopeInternal());

				if(vecinos.size()>0) {
					Optional<LaborItem> opV = vecinos.stream().reduce((v1,v2)->{
						boolean v1i = gAr.intersects(v1.getGeometry());
						boolean v2i = gAr.intersects(v2.getGeometry());
						return (v1i && v2i) 
								? (v1.getGeometry().getArea() > v2.getGeometry().getArea() ? v1 : v2) 
								: (v1i ? v1 : v2);
					});
					if(opV.isPresent()) {
						LaborItem v = opV.get();
						Geometry g = v.getGeometry();
						tree.remove(g.getEnvelopeInternal(), v);
						Geometry union = g.union(gAr);
						v.setGeometry(union);
						tree.insert(union.getEnvelopeInternal(), v);
						done.add(ar);
					}
				}
				updateProgress(done.size(),itemsAReducir.size());
			}
			updateProgress(i-itemsAReducir.size(),i);
			n++;
			itemsAReducir.removeAll(done);
		}
		
		items.clear();
		items.addAll((List<LaborItem>)tree.queryAll());
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
