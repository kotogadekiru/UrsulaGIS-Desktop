package dao;
import gov.nasa.worldwind.layers.CachedRenderableLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.JenksNaturalBreaksFunction;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

/**
 * hace las veces de un featureStore con los metodos especificos para manejar el tipo de labor especifico
 * @author tomas
 *
 * @param <E>
 */
public abstract class Labor<E extends FeatureContainer> {
	public FileDataStore inStore = null;
	public ShapefileDataStore outStore = null;
	public DefaultFeatureCollection outCollection=null;
	public StringProperty nombreProperty = new SimpleStringProperty();
	public RenderableLayer layer;//realmente quiero guardar esto aca?? o me conviene ponerlo en un mapa en otro lado para evitar la vinculacion de objetos
	
	protected static final String COLUMNA_CATEGORIA = "Categoria";
	
	public Clasificador clasificador;
	public SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
			getType());
	public StringProperty colAmount; //usada por el clasificador para leer el outstore
	//columnas configuradas para leer el instore
	public StringProperty colElevacion;
	public StringProperty colAncho;
	public StringProperty colCurso;
	public StringProperty colDistancia;// = new
	// SimpleStringProperty(CosechaLabor.COLUMNA_DISTANCIA);
	
	private Double nextID =new Double(0);//XXX este id no es global sino que depende de la labor
	
	/**
	 * precio es el costo por hectarea de la labor
	 */
	public DoubleProperty precioLaborProperty;
	public DoubleProperty precioInsumoProperty;
	
	public Labor(){
		clasificador=new Clasificador();
		outCollection = new DefaultFeatureCollection("internal",getType());
	}
	
	
	public abstract  SimpleFeatureType getType();

	public FileDataStore getInStore() {
		return inStore;
	}

	public void setInStore(FileDataStore inStore) {
		if(this.inStore!=null){
			this.inStore.dispose();
		}
		this.inStore = inStore;
		if(nombreProperty.getValue() == null && inStore!=null){
			nombreProperty.set(inStore.getInfo().getTitle().replaceAll("%20", " "));
			
		}
		if(outStore ==null){
			//createOutStore();
		}
	}

	public FileDataStore getOutStore() {
		return outStore;
	}

	public void setOutStore(ShapefileDataStore outStore) {
		this.outStore = outStore;
	}

	public Clasificador getClasificador() {
		return clasificador;
	}

	public RenderableLayer getLayer() {
		return layer;
	}
	
	public void setLayer(RenderableLayer renderableLayer) {		
		this.layer = renderableLayer;
		renderableLayer.setValue("LABOR", this);//TODO usar esto para no tener el layer dentro de la cosecha
		this.nombreProperty.addListener((o,old,nu)->{
			this.layer.setName(nu);});
		renderableLayer.setName(this.nombreProperty.get());
	}
	
	public StringProperty getNombreProperty(){
		return nombreProperty;
	}
	
	public void setOutCollection(DefaultFeatureCollection newOutcollection) {
		this.outCollection=newOutcollection;		
	}

	public Double getNextID() {
		Double nextID=this.nextID;
		this.nextID++;
		return nextID;
	}

	public List<E> outStoreQuery(Envelope envelope){
//		if(outStore ==null){
//			createOutStore();
//		}
		List<E> objects = new ArrayList<E>();
//		SimpleFeatureIterator it = outCollection.features();
//		outCollection.
//	while(it.hasNext()){
//		obj = it.next();
//		
//	}
//		try {  
		if(this.outCollection.getBounds().intersects(envelope)){//solo hago la query si el bounds esta dentro del mapa
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
		    FeatureType schema = this.outCollection.getSchema();
		    
		    // usually "THE_GEOM" for shapefiles
		    String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
		    CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor()
		            .getCoordinateReferenceSystem();
		
		    ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		    
		    BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);
		 
		 
			 SimpleFeatureCollection features = this.outCollection.subCollection(filter);//OK!! esto funciona
			// System.out.println("encontre "+features.size()+" que se intersectan con "+ bbox );
			
			 SimpleFeatureIterator featuresIterator = features.features();
			 while(featuresIterator.hasNext()){
				 objects.add(constructFeatureContainerStandar(featuresIterator.next(),false));
			 }
			 featuresIterator.close();
		}
//		  } catch (IOException e) {
//			  // TODO Auto-generated catch block
//			  e.printStackTrace();
//		  }
	//	System.out.println("devuelvo la query al outStore con "+objects.size()+" elementos");
		return objects;
	}
	
	public List<E> inStoreQuery(Envelope envelope) throws IOException{
//		if(outStore ==null){
//			createOutStore();
//		}
		List<E> objects = new ArrayList<E>();
//		SimpleFeatureIterator it = outCollection.features();
//		outCollection.
//	while(it.hasNext()){
//		obj = it.next();
//		
//	}
//		try {  
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
		    FeatureType schema = this.inStore.getSchema();
		    
		    // usually "THE_GEOM" for shapefiles
		    String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
		    CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor()
		            .getCoordinateReferenceSystem();
		
		    ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		    
		    BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);
		 
		 
			 SimpleFeatureCollection features = this.inStore.getFeatureSource().getFeatures(filter);//OK!! esto funciona
			// System.out.println("encontre "+features.size()+" que se intersectan con "+ bbox );
			
			 SimpleFeatureIterator featuresIterator = features.features();
			 while(featuresIterator.hasNext()){
				 objects.add(constructFeatureContainer(featuresIterator.next()));
			 }
			 featuresIterator.close();
//		  } catch (IOException e) {
//			  // TODO Auto-generated catch block
//			  e.printStackTrace();
//		  }
	//	System.out.println("devuelvo la query al outStore con "+objects.size()+" elementos");
		return objects;
	}

	public abstract E constructFeatureContainerStandar(SimpleFeature next,boolean newIDS) ;

	public abstract E constructFeatureContainer(SimpleFeature next) ;


	public void insertFeature(E cosechaFeature) {
		// SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
		// getType());
		SimpleFeature fe = cosechaFeature.getFeature(featureBuilder);
		this.insertFeature(fe);

	}
	
	public void insertFeature(SimpleFeature f){
		outCollection.add(f);
		//FIXME el acceso a disco es demasiado lento para crear un fileOutstore
//		try {
//			String typeName = outStore.getTypeNames()[0];
//			SimpleFeatureStore	store2 = (SimpleFeatureStore) outStore.getFeatureSource( typeName );//esto falla porque no encuentra el tipo cosecha en outStore
//
//			List<SimpleFeature> list = new ArrayList<SimpleFeature>();
//			list.add(f);
//			//  list.add( build.buildFeature("fid2", new Object[]{ geom.point(2,3), "martin" } ) );
//			SimpleFeatureCollection collection = new ListFeatureCollection( f.getType(), list);
//
//			Transaction transaction = new DefaultTransaction("insertFeatureTransaction");
//			store2.setTransaction( transaction );
//			try {
//				store2.addFeatures( collection );
//				transaction.commit(); // actually writes out the features in one go
//			}
//			catch( Exception problem){
//				problem.printStackTrace();
//				try {
//					transaction.rollback();
//					//	System.out.println("transaction rolledback");
//				} catch (IOException e) {
//
//					e.printStackTrace();
//				}
//
//			} finally {
//				try {
//					transaction.close();
//					//System.out.println("closing transaction");
//				} catch (IOException e) {
//
//					e.printStackTrace();
//				}
//			}
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
	}
	

	
	public void modifyFeature(String columna, Object newValue, String fid ){
		
		 try {
	//	String fid = f.getId().toString();

		//f.getType().getName()
			 
		SimpleFeatureStore	featureSource = (SimpleFeatureStore) outStore.getFeatureSource("*geom:Polygon" );
		Transaction transaction = new DefaultTransaction("edit");
		
		featureSource.setTransaction(transaction);

		// reference the feature to modify by FID
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints());
		Set<FeatureIdImpl> ids = new HashSet<FeatureIdImpl>();
		ids.add(new FeatureIdImpl(fid));
		 Id filter = ff.id(ids);
	
		
			featureSource.modifyFeatures(columna,newValue, filter);
			transaction.commit();
		} catch (IOException e) {
		System.err.println("error al modificar un feature en outStore");
			e.printStackTrace();
		}
		
	}
	
	private void createOutStore(){
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		Configuracion conf =  Configuracion.getInstance();
		String lastfile =conf.getPropertyOrDefault(Configuracion.LAST_FILE, "newStore.shp");
		lastfile=	lastfile.substring(0,lastfile.length()-4)+"_"+System.currentTimeMillis()+"_Out.shp";//XXX tiene que terminar en .shp
		//lastfile = lastfile.split(".")[0];
		File outFile = new File(lastfile);
	
			try {
				params.put("url",outFile.toURI().toURL());
				params.put(ShapefileDataStoreFactory.CACHE_MEMORY_MAPS.key,"true");
				params.put( ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, new Boolean(false)); 
		  //      params.put( "fstype","shape-ng"); 
			//	params.put("enable spatial index" ,"false");
				
				
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		
		try {
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			
			outStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			outStore.createSchema(getType());

			//		System.out.println("antes de forzar wgs 84");

			/*
			 * You can comment out this line if you are using the createFeatureType
			 * method (at end of class file) rather than DataUtilities.createType
			 */
			outStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
			//		System.out.println("forzando dataStore WGS84");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void constructClasificador(String nombreClasif) {
		if (Clasificador.CLASIFICADOR_JENKINS.equalsIgnoreCase(nombreClasif)) {

			this.clasificador.constructJenksClasifier(this.outCollection,
					this.colAmount.get());
		} else {
			System.out
					.println("no hay jenks Classifier falling back to histograma");
			List<E> cosechas = new ArrayList<E>();
			
			SimpleFeatureIterator ocReader = this.outCollection.features();
			while (ocReader.hasNext()) {
				cosechas.add(constructFeatureContainerStandar(ocReader.next(),false));
			}
			ocReader.close();
			this.clasificador.constructHistogram(cosechas);
			
		}
	}
	
	public List<String> getAvailableColumns() {
		List<String> availableColumns = new ArrayList<String>();
		
	

		SimpleFeatureType sch;
		try {
			if(inStore==null){
				sch =this.outCollection.getSchema();
			} else {
				sch = inStore.getSchema();//FIXME esto es null si la cosecha es generada por una union		
			}
		
			List<AttributeType> types = sch.getTypes();
			for (AttributeType at : types) {
				availableColumns.add(at.getName().toString());
			}

		} catch (IOException e) {			
			e.printStackTrace();
		}
		return availableColumns;
	}



	@Override
	public String toString() {
		return nombreProperty.get();
	}
	
	
}
