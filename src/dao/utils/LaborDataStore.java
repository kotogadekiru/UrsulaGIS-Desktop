package dao.utils;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Labor;
import dao.LaborItem;
import utils.GeometryHelper;

public class LaborDataStore<E> {
	private static List<Labor<?>> locked = Collections.synchronizedList(new ArrayList<Labor<?>>());
	
	public static List<String> getAvailableColumns(Labor<? extends LaborItem> labor) {
		List<String> availableColumns = new ArrayList<String>();
		SimpleFeatureType sch=null;
		try {
			if(labor.inStore==null){
				//XXX quizas haya que tener en cuenta inCollection tambien
				sch =labor.outCollection.getSchema();
			} else {
				sch = labor.inStore.getSchema();	
			}

			List<AttributeType> types = sch.getTypes();
			for (AttributeType at : types) {
				//at binding para Importe_ha es class java.lang.Double
				//System.out.println("at binding para "+at.getName() +" es "+at.getBinding());
				if(Number.class.isAssignableFrom(at.getBinding() )) {
					availableColumns.add(at.getName().toString());
				}
			}

		} catch (IOException e) {			
			e.printStackTrace();
		}
		return availableColumns;
	}
	
	public static  List<LaborItem> inStoreQuery(Envelope envelope,Labor<? extends LaborItem> labor) throws IOException{
		List<LaborItem> objects = new ArrayList<>();
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
		FeatureType schema = labor.inStore.getSchema();

		// usually "THE_GEOM" for shapefiles
		String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
		CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor()
				.getCoordinateReferenceSystem();

		ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		    
		BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);


		SimpleFeatureCollection features = labor.inStore.getFeatureSource().getFeatures(filter);//OK!! esto funciona


		SimpleFeatureIterator featuresIterator = features.features();
		while(featuresIterator.hasNext()){
			objects.add(labor.constructFeatureContainer(featuresIterator.next()));
		}
		featuresIterator.close();
		return objects;
	}

	public static List<LaborItem> outStoreQuery(Envelope envelope,Labor<? extends LaborItem> labor){
		List<LaborItem> objects = new ArrayList<>();
		//TODO tratar de cachear todo lo posible para evitar repetir trabajo en querys consecutivas.
		//una udea es cachear un sector del out collection y solo hacer la query si el envelope esta fuera de lo cacheado
		if(labor.outCollection.getBounds().intersects(envelope)){//solo hago la query si el bounds esta dentro del mapa
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
			FeatureType schema = labor.outCollection.getSchema();

			// usually "THE_GEOM" for shapefiles
			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
			CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

			ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		

			BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);

			SimpleFeatureCollection features = labor.outCollection.subCollection(filter);//OK!! esto funciona
			// System.out.println("encontre "+features.size()+" que se intersectan con "+ bbox );

			Polygon boundsPolygon = GeometryHelper.constructPolygon(bbox);

			SimpleFeatureIterator featuresIterator = features.features();
			while(featuresIterator.hasNext()){
				SimpleFeature next = featuresIterator.next();
				Object obj = next.getDefaultGeometry();

				Geometry geomEnvelope = null;
				if(obj instanceof Geometry){					
					geomEnvelope =(Geometry)obj;					 
				} 

				boolean intersects = false;
				if(geomEnvelope!=null){
					intersects = geomEnvelope.intersects(boundsPolygon );
				}
				if(intersects){
					objects.add(labor.constructFeatureContainerStandar(next,false));
				}
			}
			featuresIterator.close();
		}

		return objects;
	}

	@SuppressWarnings("unchecked")
	public static List<? extends LaborItem> cachedOutStoreQuery(Envelope envelope, Labor<? extends LaborItem> labor){
		checkLock(labor);
		List<LaborItem> objects = new ArrayList<>();
		List<SimpleFeature> cachedObjects = null;
	//	synchronized(labor){
			if( labor.treeCache == null ) {
				LaborDataStore.updateAllCachedEnvelopes(envelope,labor);			
			} 
//			else {
//				System.out.println("labor.treeCache != null "+labor.nombre);
//			}
			labor.cacheLastRead=LocalTime.now();
			cachedObjects = labor.treeCache.query(envelope);// Exception in thread "pool-2-thread-5" java.util.ConcurrentModificationException

			//			if(labor.treeCache.size()>CACHE_MAX_SIZE) {
			//				labor.clearCache();
			//			}


			//el error se produjo al convertir un ndvi a cosecha

			FeatureType schema = labor.outCollection.getSchema();			    
			CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();		
			ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		
			Geometry geoEnv = GeometryHelper.constructPolygon(bbox);
			for(SimpleFeature sf : cachedObjects){
				Geometry sfGeom = (Geometry) sf.getDefaultGeometry();
				boolean intersects = false;
				if(sfGeom!=null){
					intersects = geoEnv.intersects(sfGeom);
				}
				if(intersects){
					objects.add(labor.constructFeatureContainerStandar(sf,false));
				}
			}
	//	}
		locked.remove(labor);//release labor lock
		return objects;
	}
	private static void checkLock(Labor<? extends LaborItem> labor) {
		while(locked.contains(labor)) {
			//System.out.println("locked contains "+labor.nombre);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		locked.add(labor);
	}

	/**
	 * metodo que se ejecuta la primera vez que se hace un cachedOutStoreQuery y tree
	 * @param envelope
	 * @param labor
	 */
	private static void updateAllCachedEnvelopes(Envelope envelope,Labor<? extends LaborItem> labor){		
		//System.out.println(labor.nombre+" empezando updateAllCachedEnvelopes en "+Thread.currentThread().getId());
		if(labor.outCollection == null) {
			System.err.println("No se puede iterar sobre outCollection porque es null en "+labor.getNombre());
			return;
		}

		//List<SimpleFeature> sFeaturesToAdd = new ArrayList<SimpleFeature>();
		// cargar todas las features en memoria pero guardarlas indexadas en cachedEnvelopes
		
		Quadtree auxTreeCache = new Quadtree();
		final Envelope auxTreeCacheEnvelope = new Envelope();
		try {
			SimpleFeatureIterator features = labor.outCollection.features();
			while(features.hasNext()) {
				//sFeaturesToAdd.add(features.next());	
				SimpleFeature sf = features.next();
				Geometry g =(Geometry) sf.getDefaultGeometry();
				Envelope ge = g.getEnvelopeInternal();
				auxTreeCache.insert(ge, sf);
				auxTreeCacheEnvelope.expandToInclude(ge);
			}
		}catch(Exception e) {
			//e.printStackTrace();
		}
	
//		sFeaturesToAdd.stream().forEach(sf->{
//			Geometry g =(Geometry) sf.getDefaultGeometry();
//			Envelope ge = g.getEnvelopeInternal();
//			auxTreeCache.insert(ge, sf);
//			auxTreeCacheEnvelope.expandToInclude(ge);
//		});
		labor.treeCache = auxTreeCache;
		labor.treeCacheEnvelope = auxTreeCacheEnvelope;
		//System.out.println(labor.nombre+" termine updateAllCachedEnvelopes en "+Thread.currentThread().getId());
	}

	public static void dispose(Labor<? extends LaborItem> labor) {
		System.out.println("antes de dispose locked contiene "+locked.size()+" elementos");
		if(locked.contains(labor)) {
			System.out.println("no puedo hacer dispose porque esta lockeada");
		}
		checkLock(labor);
		System.out.println("haciendo dispose de "+labor.getNombre());
		
		if(labor.inStore!=null){
			labor.inStore.dispose();
			labor.inStore = null;
		}

		labor.clearCache();//aca se borra quadtree

		if(labor.outCollection!=null){
			labor.outCollection.clear();
			labor.outCollection=null;
		}

		if(labor.inCollection!=null){
			labor.inCollection.clear();
			labor.inCollection=null;
		}

		if(labor.layer!=null){		
			labor.layer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, null);
			labor.layer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, null);
			labor.layer.removeAllRenderables();
			labor.layer.dispose();
			labor.layer.getValues().clear();
			labor.layer=null;
		}
		locked.remove(labor);
		System.out.println("despues de dispose locked contiene "+locked.size()+" elementos");
	}
	
	public static void insertFeature(LaborItem laborItem, Labor<? extends LaborItem> labor) {
		if(laborItem == null) {
			return;
		}	
		checkLock(labor);//wait until lock released
		Geometry cosechaGeom = laborItem.getGeometry();
		Envelope geomEnvelope=cosechaGeom.getEnvelopeInternal();

		//synchronized(labor.featureBuilder){
			SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(labor.getType());
			SimpleFeature fe = laborItem.getFeature(fBuilder);
			//SimpleFeature fe = laborItem.getFeature(labor.featureBuilder);

			if(labor.treeCache!=null){
				//if treeCache is too big clearCache. it only clears on insert.
				//				if(labor.treeCache.size()+1>LaborDataStore.CACHE_MAX_SIZE) {
				//					System.out.println("clearing cache on size "+labor.treeCache.size());
				//					labor.clearCache();
				//					labor.treeCache=new Quadtree();
				//					labor.treeCacheEnvelope=new Envelope();
				//				}

				labor.treeCacheEnvelope.expandToInclude(geomEnvelope);
				labor.treeCache.insert(geomEnvelope, fe);

			}
			labor.insertFeature(fe);
		//}//fin del synchronized
		locked.remove(labor);
	}
	
	public static void changeFeature(SimpleFeature old, LaborItem ci, Labor<? extends LaborItem> labor) {
		checkLock(labor);//wait until feature released
		try {
		if(old!=null) {
			boolean removed = labor.outCollection.remove(old);
			Geometry g= (Geometry) old.getDefaultGeometry();
			if(labor.treeCache!=null) {
				labor.treeCache.remove(g.getEnvelopeInternal(), labor);
			}
			if(removed) {
				System.out.println("removi el feature "+old);
			}
		}
		}finally {
			locked.remove(labor);
		}
		
		insertFeature(ci, labor);//insertFeature tiene su propio lock

		
	}
}
