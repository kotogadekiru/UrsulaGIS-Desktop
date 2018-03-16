package dao;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Transient;

import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStore;
import org.geotools.data.ServiceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.api.client.util.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.config.Configuracion;
import dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.geom.Position;
import gui.nww.LaborLayer;
import gui.utils.DateConverter;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;
import utils.ProyectionConstants;

/**
 * hace las veces de un featureStore con los metodos especificos para manejar el tipo de labor especifico
 * @author tomas
 *
 * @param <E>
 */

@Data
@Entity @Access(AccessType.FIELD)//variable (el default depende de donde pongas el @Id)
//@Entity @Access(AccessType.PROPERTY)//getter
@Inheritance(strategy=javax.persistence.InheritanceType.TABLE_PER_CLASS)
public abstract class Labor<E extends LaborItem>  {
	
	public static final String NONE_SELECTED = "Ninguna";
	public static final String LABOR_LAYER_IDENTIFICATOR = "LABOR";
	
	@javax.persistence.Id @GeneratedValue
	private long id;
	
	@Transient
	public FileDataStore inStore = null;
	//public ShapefileDataStore outStore = null;
	@Transient
	public DefaultFeatureCollection outCollection=null;
	@Transient
	public DefaultFeatureCollection inCollection=null;
	@Transient
	public StringProperty nombreProperty = new SimpleStringProperty();
	@Transient
	public LaborLayer layer=null;//realmente quiero guardar esto aca?? o me conviene ponerlo en un mapa en otro lado para evitar la vinculacion de objetos

	protected static final String COLUMNA_CATEGORIA = "Categoria";
	public static final String COLUMNA_DISTANCIA = "Distancia";
	public static final String COLUMNA_CURSO = "Curso(deg)";
	public static final String COLUMNA_ANCHO = "Ancho";
	public static final String COLUMNA_ELEVACION = "Elevacion";

	private static final String ANCHO_DEFAULT = "ANCHO_DEFAULT";
	private static final String FECHA_KEY = "FECHA_KEY";

	@Transient
	public Clasificador clasificador=null;
	@Transient
	public SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(getType());
	@Transient
	public StringProperty colAmount=null; //usada por el clasificador para leer el outstore tiene que ser parte de TYPE
	//columnas configuradas para leer el instore
	@Transient
	public StringProperty colElevacion=null;
	@Transient
	public StringProperty colAncho=null;
	@Transient
	public StringProperty colCurso=null;
	@Transient
	public StringProperty colDistancia=null;// = new
	// SimpleStringProperty(CosechaLabor.COLUMNA_DISTANCIA);
	@Transient
	private Double nextID =new Double(0);//XXX este id no es global sino que depende de la labor

	/**
	 * precio es el costo por hectarea de la labor
	 */
	@Transient
	public Property<LocalDate> fechaProperty=null;
	@Transient
	public DoubleProperty precioLaborProperty=null;
	@Transient
	public DoubleProperty precioInsumoProperty=null;
	@Transient
	public SimpleDoubleProperty anchoDefaultProperty= null;
	@Transient
	public Map<Envelope,List<E>> cachedEnvelopes=Collections.synchronizedMap(new HashMap<Envelope,List<E>>());
	
	@Transient
	public Quadtree treeCache = null;
	
//	private CoordinateReferenceSystem targetCRS;
	@Transient
	public LaborConfig config = null;
	@Transient
	public Double minElev=Double.MAX_VALUE;
	@Transient
	public Double maxElev=-Double.MAX_VALUE;
	//average 
	//desvio
	@Transient
	public Double minAmount=Double.MAX_VALUE;
	@Transient
	public Double maxAmount=-Double.MAX_VALUE;
	//average
	//desvio
	
	//TODO cambiar por un referencedEnvelope
	@Transient
	public Position minX = null;//Position.fromDegrees(Double.MAX_VALUE, Double.MAX_VALUE);
	@Transient
	public Position minY = null;//Position.fromDegrees(Double.MAX_VALUE, Double.MAX_VALUE);// null; //Double.MAX_VALUE;
	@Transient
	public Position maxX = null;//Position.fromDegrees(Double.MIN_VALUE, Double.MIN_VALUE);//null; //-Double.MAX_VALUE;
	@Transient
	public Position maxY = null;//Position.fromDegrees(-Double.MIN_VALUE, Double.MIN_VALUE);// null;//-Double.MAX_VALUE;
	//CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

	//public ReferencedEnvelope envelope = null;//new ReferencedEnvelope(envelope,targetCRS);
	
	//@Transient
	//public Geometry envelope = null;// no puedo cachear la geometria total porque tarda mucho

	public Labor(){
		clasificador=new Clasificador();
		outCollection = new DefaultFeatureCollection("internal",getType());
		initConfigLabor();
	}


	public Labor(FileDataStore store) {
		clasificador=new Clasificador();
		outCollection = new DefaultFeatureCollection("internal",getType());
		this.setInStore(store);// esto configura el nombre
		initConfigLabor();
	}

	private void initConfigLabor() {
		//	initConfig();//inicio el config de la sub labor 
		List<String> availableColums = this.getAvailableColumns();		
		//	Configuracion properties = getConfigLabor().getConfigProperties();
		LaborConfig laborConfig = getConfigLabor();
		Configuracion properties = laborConfig.getConfigProperties();

		colElevacion = new SimpleStringProperty(
				properties.getPropertyOrDefault(COLUMNA_ELEVACION,
						COLUMNA_ELEVACION));
		if(!availableColums.contains(colElevacion.get()) && 
				availableColums.contains(COLUMNA_ELEVACION)){
			colElevacion.setValue(COLUMNA_ELEVACION);
		}
		colElevacion.addListener((obs, bool1, bool2) -> {
			properties.setProperty(COLUMNA_ELEVACION,
					bool2.toString());
		});

		colAncho = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_ANCHO, CosechaLabor.COLUMNA_ANCHO));
		if(!availableColums.contains(colAncho.get()) 
				&& availableColums.contains(CosechaLabor.COLUMNA_ANCHO)){
			colAncho.setValue(CosechaLabor.COLUMNA_ANCHO);
		} 
		colAncho.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_ANCHO, bool2);
		});// bool2 es un string asi que no necesito convertirlo

		colCurso = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_CURSO, CosechaLabor.COLUMNA_CURSO));
		if(!availableColums.contains(colCurso.get())&&availableColums.contains(CosechaLabor.COLUMNA_CURSO)){
			colCurso.setValue(CosechaLabor.COLUMNA_CURSO);
		}
		colCurso.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_CURSO, bool2.toString());
		});

		colDistancia = new SimpleStringProperty(
				properties.getPropertyOrDefault(CosechaLabor.COLUMNA_DISTANCIA,
						CosechaLabor.COLUMNA_DISTANCIA));
		if(!availableColums.contains(colDistancia.get())&&availableColums.contains(CosechaLabor.COLUMNA_DISTANCIA)){
			colDistancia.setValue(CosechaLabor.COLUMNA_DISTANCIA);
		}
		colDistancia.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_DISTANCIA,
					bool2.toString());
		});
		
		fechaProperty = new SimpleObjectProperty<LocalDate>();
		DateConverter dc = new DateConverter(); 		
		String defaultDate = properties.getPropertyOrDefault(Labor.FECHA_KEY,	dc.toString(LocalDate.now()));	
		//LocalDate ld = dc.fromString(dc.toString(LocalDate.now()));		
		LocalDate ld = dc.fromString(defaultDate);		
		fechaProperty.setValue(ld);
		fechaProperty.addListener((obs, bool1, bool2) -> {
			System.out.println("cambiando la fecha a "+bool2);
			properties.setProperty(Labor.FECHA_KEY,dc.toString(bool2));
				//	bool2.toString());
		});

		precioLaborProperty = initPrecioLaborHaProperty();// initDoubleProperty(CosechaLabor.COSTO_COSECHA_HA, properties);
		precioInsumoProperty = initPrecioInsumoProperty(); //initDoubleProperty(FertilizacionLabor.COLUMNA_PRECIO_FERT,  "0", properties);	

		// anchoDefaultProperty
		anchoDefaultProperty = initDoubleProperty(ANCHO_DEFAULT, "8", properties);

		clasificador.tipoClasificadorProperty.set(
				properties.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
						Clasificador.CLASIFICADOR_JENKINS));
		clasificador.tipoClasificadorProperty
		.addListener((obs, bool1, bool2) -> {
			properties.setProperty(Clasificador.TIPO_CLASIFICADOR,
					bool2.toString());
		});

		clasificador.clasesClasificadorProperty = new SimpleIntegerProperty(Integer.parseInt(properties.getPropertyOrDefault(Clasificador.NUMERO_CLASES_CLASIFICACION,String.valueOf(Clasificador.colors.length))));
		clasificador.clasesClasificadorProperty.addListener((obs,bool1,bool2)->{
			properties.setProperty(Clasificador.NUMERO_CLASES_CLASIFICACION, bool2.toString());
		});


	}
	

	public Long getId(){
		return this.id;
	}
	
	public void setId(Long id){
		this.id=id;
	}


	/**
	 * metodo que devuelve el costo de la labor por hectarea de acuerdo a la configuracion del tipo de labor que implemente
	 * @return DoubleProperty
	 */
	@Transient
	protected abstract DoubleProperty initPrecioLaborHaProperty() ;
	
	/**
	 * metodo que devuelve el costo de la labor por hectarea de acuerdo a la configuracion del tipo de labor que implemente
	 * @return DoubleProperty
	 */
	@Transient
	protected abstract DoubleProperty initPrecioInsumoProperty() ;

	public static SimpleDoubleProperty initDoubleProperty(String key,String def,Configuracion properties){
		SimpleDoubleProperty doubleProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						key, def)));
		doubleProperty.addListener((obs, bool1, bool2) -> {

			properties.setProperty(key,	bool2.toString());
		});
		return doubleProperty;
	}

	/**
	 * 
	 * @param key el valor por defecto para la propiedad
	 * @param properties un objeto Configuracion a ser modificado
	 * @param availableColums la lista de opsiones para configurar las propiedades
	 * @return devuelve una nueva StringProperty inicializada con el valor correspondiente de las availableColums o la key proporcionada
	 */
	public static SimpleStringProperty initStringProperty(String key,Configuracion properties,List<String> availableColums){
		SimpleStringProperty sProperty = new SimpleStringProperty(
				properties.getPropertyOrDefault(key, key));

		if(availableColums!=null && !availableColums.contains(sProperty.get()) && availableColums.contains(key)){
			sProperty.setValue(key);
		}

		sProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(key,	bool2.toString());
		});
		return sProperty;
	}

	@Transient
	public abstract  String getTypeDescriptors();

	@Transient
	public FileDataStore getInStore() {
		return inStore;
	}

	public void setInStore(FileDataStore inStore) {
		if(this.inStore!=null){
			this.inStore.dispose();
		}
		if(inStore !=null){
			this.inStore = inStore;
			ServiceInfo info = inStore.getInfo();
			System.out.println("labor inStore.info = "+info );
			try {
				SimpleFeatureType schema = inStore.getSchema();
				System.out.println("Prescription Type: "+DataUtilities.spec(schema));
				System.out.println(schema);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//	if(nombreProperty.getValue() == null){
			nombreProperty.set(inStore.getInfo().getTitle().replaceAll("%20", " "));

			//}
		}
	}

	
	public Clasificador getClasificador() {
		return clasificador;
	}

	@Transient
	public LaborLayer getLayer() {
		return layer;
	}

	public void setLayer(LaborLayer renderableLayer) {		
		this.layer = renderableLayer;
		renderableLayer.setValue(LABOR_LAYER_IDENTIFICATOR, this);//usar esto para no tener el layer dentro de la cosecha
		this.nombreProperty.addListener((o,old,nu)->{
			this.layer.setName(nu);});
		renderableLayer.setName(this.nombreProperty.get());
	}

	@Transient
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

//	public List<E> cachedOutStoreQueryOLD(Envelope envelope){
//		List<E> objects = new ArrayList<E>();
//		Envelope cachedEnvelope=null;
//
//		CoordinateReferenceSystem targetCRS=null;
//		synchronized(cachedEnvelopes){//tengo que poner todo el metodo en synchro sino pierdo valores al hacer el filtro outlayers
//			if(cachedEnvelopes.size()>0){
//
//				for(Envelope ce : cachedEnvelopes.keySet()){
//					if(ce.contains(envelope))cachedEnvelope=ce;
//				}
//			}
//
//			if( cachedEnvelope==null ){
//				cachedEnvelope = updateCachedEnvelope(envelope);
//				FeatureType schema = this.outCollection.getSchema();			    
//				targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();		
//			}
//		}
//		ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		
//		Polygon boundsPolygon = constructPolygon(bbox);
//		List<E> cachedObjects = cachedEnvelopes.get(cachedEnvelope);
//
//		for(E cachedObject : cachedObjects){
//			Geometry geomEnvelope = cachedObject.getGeometry();
//			boolean intersects = false;
//			if(geomEnvelope!=null){
//				intersects = geomEnvelope.intersects(boundsPolygon);
//			}
//			if(intersects){
//				objects.add(cachedObject);
//			}
//		}
//
//		return objects;
//	}
	
	public List<E> cachedOutStoreQuery(Envelope envelope){
		List<E> objects = new ArrayList<E>();
		synchronized(this){
			//si la cache crecio mucho la limito a un tamanio
//			if(treeCache!=null && treeCache.size()>50*1000){//71053 se limpia todo el timepo
//				System.out.println("limpiando cache con size = "+treeCache.size()+" envelope = "+envelope.toString());
//				treeCache=null;
//			}//esto no sirve porque updateAllCachedEnvelopes carga todas las features no solo las del envelope
			//TODO poner un timer si no se uso el treeCache en x segundos limpiarlo.
			if( treeCache==null){			
				updateAllCachedEnvelopes(envelope);			
			} 
		}
		@SuppressWarnings("unchecked")
		List<SimpleFeature> cachedObjects = treeCache.query(envelope);//FIXME Exception in thread "pool-2-thread-5" java.util.ConcurrentModificationException
		//el error se produjo al convertir un ndvi a cosecha

		FeatureType schema = this.outCollection.getSchema();			    
		CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();		
		ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		
		Geometry geoEnv = constructPolygon(bbox);
		for(SimpleFeature sf : cachedObjects){
			Geometry sfGeom = (Geometry) sf.getDefaultGeometry();
			boolean intersects = false;
			if(sfGeom!=null){
				intersects = geoEnv.intersects(sfGeom);
			}
			if(intersects){
				objects.add(constructFeatureContainerStandar(sf,false));
			}
		}

		return objects;
	}

	public void clearCache(){
		treeCache = null;
	}
	private Envelope updateCachedEnvelope(Envelope envelope){
		Envelope cachedEnvelope = new Envelope(envelope);
		double height = cachedEnvelope.getHeight();
		double width = cachedEnvelope.getHeight();
		cachedEnvelope.expandBy(width*4, height*4);
		cachedEnvelopes.put(cachedEnvelope, outStoreQuery(cachedEnvelope));
		return cachedEnvelope;
	}
	
	private void updateAllCachedEnvelopes(Envelope envelope){
		treeCache=new Quadtree();
//TODO cargar todas las features en memoria pero en guardarlas indexadas en cachedEnvelopes
		@SuppressWarnings("unchecked")
		Collection<SimpleFeature> items= Lists.newArrayList(outCollection.iterator());
		items.forEach((it)->{
			Geometry g =(Geometry) it.getDefaultGeometry();
			treeCache.insert(g.getEnvelopeInternal(), it);
		});
	}

	public List<E> outStoreQuery(Envelope envelope){
		List<E> objects = new ArrayList<E>();
		//TODO tratar de cachear todo lo posible para evitar repetir trabajo en querys consecutivas.
		//una udea es cachear un sector del out collection y solo hacer la query si el envelope esta fuera de lo cacheado
		if(this.outCollection.getBounds().intersects(envelope)){//solo hago la query si el bounds esta dentro del mapa
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
			FeatureType schema = this.outCollection.getSchema();

			// usually "THE_GEOM" for shapefiles
			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
			CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

			ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		

			BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);

			SimpleFeatureCollection features = this.outCollection.subCollection(filter);//OK!! esto funciona
			// System.out.println("encontre "+features.size()+" que se intersectan con "+ bbox );

			Polygon boundsPolygon = constructPolygon(bbox);

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
					objects.add(constructFeatureContainerStandar(next,false));
				}
			}
			featuresIterator.close();
		}

		return objects;
	}
	public Polygon constructPolygon(ReferencedEnvelope e) {
		Coordinate D = new Coordinate(e.getMaxX(), e.getMaxY()); // x-l-d
		Coordinate C = new Coordinate(e.getMinX(), e.getMaxY());// X+l-d
		Coordinate B = new Coordinate(e.getMaxX(), e.getMinY());// X+l+d
		Coordinate A = new Coordinate(e.getMinX(), e.getMinY());// X-l+d

		/**
		 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
		 * carro--B
		 * 
		 */
		Coordinate[] coordinates = { A, C, D, B, A };// Tiene que ser cerrado.
		// Empezar y terminar en
		// el mismo punto.
		// sentido antihorario

		GeometryFactory fact = ProyectionConstants.getGeometryFactory();

		//		LinearRing shell = fact.createLinearRing(coordinates);
		//		LinearRing[] holes = null;
		//		Polygon poly = new Polygon(shell, holes, fact);
		Polygon poly = fact.createPolygon(coordinates);
		return poly;
	}

	public List<E> inStoreQuery(Envelope envelope) throws IOException{
		List<E> objects = new ArrayList<E>();
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
		return objects;
	}

	/**
	 * metodo que construye una feature leyendo las columnas estandar definidas para el tipo de labor
	 * @param next
	 * @param newIDS
	 * @return
	 */
	public abstract E constructFeatureContainerStandar(SimpleFeature next,boolean newIDS) ;

	/**
	 * metodo que construye una feature leyendo las columnas seleccionadas por el usuario de las disponibles en el shp
	 * @param next
	 * @return
	 */
	public abstract E constructFeatureContainer(SimpleFeature next) ;


	public void insertFeature(E cosechaFeature) {
		Geometry cosechaGeom = cosechaFeature.getGeometry();
		Envelope geomEnvelope=cosechaGeom.getEnvelopeInternal();
//		if(cachedEnvelopes.size()>0){
//			synchronized(cachedEnvelopes){
//				for(Envelope ce : cachedEnvelopes.keySet()){
//					if(ce.contains(geomEnvelope)){
//						List<E> objects = cachedEnvelopes.get(ce);
//						objects.add(cosechaFeature);
//						cachedEnvelopes.replace(ce,  objects);
//					}						
//				}					
//			}
//		}
		synchronized(featureBuilder){
			SimpleFeature fe = cosechaFeature.getFeature(featureBuilder);
			if(treeCache!=null){
				treeCache.insert(geomEnvelope, fe);
			}
			this.insertFeature(fe);
		}
	}

	public void insertFeature(SimpleFeature f){
		outCollection.add(f);
	}

	public void constructClasificador() {
		constructClasificador(getClasificador().tipoClasificadorProperty.get());
//				getConfigLabor().getConfigProperties().getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
//				Clasificador.CLASIFICADOR_JENKINS));
	}

	public void constructClasificador(String nombreClasif) {
		System.out.println("constructClasificador "+nombreClasif);
		if (Clasificador.CLASIFICADOR_JENKINS.equalsIgnoreCase(nombreClasif)) {
			System.out.println("construyendo clasificador jenkins "+this.colAmount.get());
			this.clasificador.constructJenksClasifier(this.outCollection,this.colAmount.get());
		} else {
			System.out
			.println("no hay jenks Classifier falling back to histograma");
			List<E> items = new ArrayList<E>();

			SimpleFeatureIterator ocReader = this.outCollection.features();
			while (ocReader.hasNext()) {
				items.add(constructFeatureContainerStandar(ocReader.next(),false));
			}
			ocReader.close();
			this.clasificador.constructHistogram(items);
		}
	}

	@Transient
	public List<String> getAvailableColumns() {
		List<String> availableColumns = new ArrayList<String>();
		SimpleFeatureType sch=null;
		try {
			if(inStore==null){
				//XXX quizas haya que tener en cuenta inCollection tambien
				sch =this.outCollection.getSchema();
			} else {
				sch = inStore.getSchema();	
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

	/**
	 * metodo que se ocupa de hacer la limpieza al momento de quitar la labor
	 */
	public void dispose() {
		if(inStore!=null){

			inStore.dispose();
			inStore = null;
		}

//		if(cachedEnvelopes!=null){
//			cachedEnvelopes.clear();
//			cachedEnvelopes = null;
//		}
		clearCache();
		
		if(outCollection!=null){
			outCollection.clear();
			outCollection=null;
		}

		if(inCollection!=null){
			inCollection.clear();
			inCollection=null;
		}

		if(layer!=null){
			layer.setValue(LABOR_LAYER_IDENTIFICATOR, null);
			layer.removeAllRenderables();
			layer.dispose();
			layer.getValues().clear();
			layer=null;
		}

	}


	/**
	 * @return the inCollection
	 */
	@Transient
	public DefaultFeatureCollection getInCollection() {
		return inCollection;
	}


	/**
	 * @param inCollection the inCollection to set
	 */
	public void setInCollection(DefaultFeatureCollection inCollection) {
		this.inCollection = inCollection;
	}


	@Transient
	public SimpleFeatureType getType() {
		SimpleFeatureType type = null;
		String typeDescriptor = "the_geom:MultiPolygon:srid=4326,"//"*geom:Polygon,"the_geom
				+ COLUMNA_DISTANCIA + ":Double,"
				+ COLUMNA_CURSO + ":Double,"
				+ COLUMNA_ANCHO + ":Double,"
				+ COLUMNA_ELEVACION + ":Double,"
				+ COLUMNA_CATEGORIA + ":Integer,";
		typeDescriptor+= getTypeDescriptors();

		try {
			type = DataUtilities.createType("LABOR", typeDescriptor);
		} catch (SchemaException e) {

			e.printStackTrace();
		}
		return type;
	}

	@Transient
	public SimpleFeatureType getPointType() {
		SimpleFeatureType type = null;
		String typeDescriptor = "the_geom:Point:srid=4326,"//"*geom:Polygon,"the_geom
				+ COLUMNA_DISTANCIA + ":Double,"
				+ COLUMNA_CURSO + ":Double,"
				+ COLUMNA_ANCHO + ":Double,"
				+ COLUMNA_ELEVACION + ":Double,"
				+ COLUMNA_CATEGORIA + ":Integer,";
		typeDescriptor+= getTypeDescriptors();

		try {
			type = DataUtilities.createType("LABOR", typeDescriptor);
		} catch (SchemaException e) {

			e.printStackTrace();
		}
		return type;
	}

	
	public void constructFeatureContainerStandar(LaborItem ci, SimpleFeature harvestFeature, Boolean newIDS) {
		ci.id = LaborItem.getDoubleFromObj(LaborItem.getID(harvestFeature));
		if(ci.id ==null || newIDS){// flag que me permita ignorar el id del feature y asignar uno nuevo
			ci.id= this.getNextID();
		}

		ci.distancia = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_DISTANCIA));
		ci.rumbo = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_CURSO));
		ci.ancho = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_ANCHO));
		ci.elevacion = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_ELEVACION));
//FIXME el clasificador se esta llamando sin haber inicializado el histograma y entra por jenkins. corregir antes de descomentar
//		if(this.clasificador!=null && clasificador.isInitialized()){
//			Integer categoria = this.clasificador.getCategoryFor(ci.getAmount());
//			if(categoria !=null)		ci.setCategoria(categoria);
//		}	

	}

	public void constructFeatureContainer(LaborItem ci, SimpleFeature harvestFeature) {
		ci.setId(getNextID());

		String idString = LaborItem.getID(harvestFeature);
		ci.setId(LaborItem.getDoubleFromObj(idString));

		double toMetros = getConfigLabor().valorMetrosPorUnidadDistanciaProperty()
				.doubleValue();

		String anchoColumn = colAncho.get();
		ci.setAncho(LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(anchoColumn)));
		ci.setAncho(ci.getAncho() * toMetros);

		String distColumn = colDistancia.get();// getColumn(COLUMNA_DISTANCIA);//TODO
		// pasar el constructor de
		// cosechaItem a la labor
		try {//para permitir editar la distancia
			ci.setDistancia(new Double(distColumn));
		} catch (Exception e) {//si distColumn no es un numero procedo a parsear el contenido
			Object distAttribute = harvestFeature.getAttribute(distColumn);

			ci.setDistancia(LaborItem.getDoubleFromObj(distAttribute)
					* toMetros);
		}

		ci.setRumbo(LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(colCurso.get())));

		if(!colElevacion.get().equals(Labor.NONE_SELECTED)){
			Double elevacion =  LaborItem.getDoubleFromObj(harvestFeature
					.getAttribute(colElevacion.get()));

			ci.setElevacion(elevacion);
		} else{
			ci.setElevacion(1.0);
		}

	}

	public abstract LaborConfig getConfigLabor();
}
