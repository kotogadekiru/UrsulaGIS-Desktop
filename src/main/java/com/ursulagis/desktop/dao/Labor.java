package com.ursulagis.desktop.dao;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.geotools.data.DataUtilities;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.ServiceInfo;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.index.quadtree.Quadtree;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.ordenCompra.ProductoLabor;
import com.ursulagis.desktop.dao.utils.LaborDataStore;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.gui.utils.DateConverter;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.GeometryHelper;

/**
 * hace las veces de un featureStore con los metodos especificos para manejar el tipo de labor especifico
 * @author tomas
 *
 * @param <E>
 */

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)//variable (el default depende de donde pongas el @Id)
//@Entity @Access(AccessType.PROPERTY)//getter
@Inheritance(strategy=javax.persistence.InheritanceType.TABLE_PER_CLASS)
@NamedQueries({
	@NamedQuery(name=Labor.FIND_ALL, query="SELECT c FROM Labor c") ,
	@NamedQuery(name=Labor.FIND_NAME, query="SELECT o FROM Labor o where o.nombre = :name") ,
	@NamedQuery(name=Labor.FIND_ACTIVOS, query="SELECT o FROM Labor o where o.activo = true") ,
}) 
public abstract class Labor<E extends LaborItem>  {
	//private static final String THE_GEOM_OLUMN = "the_geom";
	@Transient public static final String FIND_ALL="Labor.findAll";
	@Transient public static final String FIND_NAME = "Labor.findName";
	@Transient public static final String FIND_ACTIVOS = "Labor.findActivos";

	@Transient public static final String NONE_SELECTED = "Ninguna";
	@Transient public static final String LABOR_LAYER_IDENTIFICATOR = "LABOR";
	@Transient public static final String LABOR_LAYER_CLASS_IDENTIFICATOR = "LABOR_LAYER_CLASS_IDENTIFICATOR";


	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	private Long id=null;
	private boolean activo=false;
	//public StringProperty nombreProperty = new SimpleStringProperty();
	public String nombre = new String();

	@Temporal(TemporalType.DATE)
	public Date fecha = new Date();

	public Double precioLabor=0.0;
	public Double precioInsumo=0.0;
	public Double cantidadInsumo=0.0;
	public Double cantidadLabor=0.0;
	
	//private Poligono contorno=null;
	
	@Lob
	private byte[] content=null;//el contenido zip shpfile
	
	/**
	 * ProductoLabor contiene el tipo de labor que es. ya sea labor de cosecha, siembra o fertilizacion
	 */
	@ManyToOne(cascade=CascadeType.PERSIST)
	public ProductoLabor productoLabor= null;

//	public Property<LocalDate> fechaProperty=new SimpleObjectProperty<LocalDate>();	

	public DoubleProperty anchoDefaultProperty= new SimpleDoubleProperty();

	@Transient public FileDataStore inStore = null;
	//public ShapefileDataStore outStore = null;	
	@Transient public DefaultFeatureCollection outCollection=null;	
	@Transient public DefaultFeatureCollection inCollection=null;	
	@Transient public LaborLayer layer=null;//realmente quiero guardar esto aca?? o me conviene ponerlo en un mapa en otro lado para evitar la vinculacion de objetos

	@Transient protected static final String COLUMNA_CATEGORIA = "Categoria";
	@Transient public static final String COLUMNA_DISTANCIA = "Distancia";
	@Transient public static final String COLUMNA_CURSO = "Curso(deg)";
	@Transient public static final String COLUMNA_ANCHO = "Ancho";
	@Transient public static final String COLUMNA_ELEVACION = "Elevacion";
	@Transient public static final String COLUMNA_OBSERVACIONES = "Observaciones";

	@Transient private static final String ANCHO_DEFAULT = "ANCHO_DEFAULT";
	@Transient
	public static final String FECHA_KEY = "FECHA_KEY";


	@Transient public Clasificador clasificador=null;	
	//@Transient public SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(getType());

	@Transient  public StringProperty colAmount=new SimpleStringProperty();	; //usada por el clasificador para leer el outstore tiene que ser parte de TYPE
	//columnas configuradas para leer el instore

	@Transient public StringProperty colElevacion=new SimpleStringProperty();	
	@Transient public StringProperty colAncho=new SimpleStringProperty();	
	@Transient public StringProperty colCurso=new SimpleStringProperty();	
	@Transient public StringProperty colDistancia=new SimpleStringProperty();	
	@Transient private Double nextID =0.0;//XXX este id no es global sino que depende de la labor

	//@Transient public Map<Envelope,List<E>> cachedEnvelopes=Collections.synchronizedMap(new HashMap<Envelope,List<E>>());
	@Transient public Quadtree treeCache = null;
	@Transient public Envelope treeCacheEnvelope = null;
	@Transient public LocalTime cacheLastRead = null;

	@Transient protected LaborConfig config = null;

	public Double minElev=Double.MAX_VALUE;
	public Double maxElev=-Double.MAX_VALUE;
	public Double minAmount=Double.MAX_VALUE;
	public Double maxAmount=-Double.MAX_VALUE;

	//no se puede cambiar por un referencedEnvelope porque son positions no cotas norte sur este oeste.
	@Transient	public Position minX = null;//Position.fromDegrees(Double.MAX_VALUE, Double.MAX_VALUE);
	@Transient public Position minY = null;//Position.fromDegrees(Double.MAX_VALUE, Double.MAX_VALUE);// null; //Double.MAX_VALUE;
	@Transient public Position maxX = null;//Position.fromDegrees(Double.MIN_VALUE, Double.MIN_VALUE);//null; //-Double.MAX_VALUE;
	@Transient public Position maxY = null;//Position.fromDegrees(-Double.MIN_VALUE, Double.MIN_VALUE);// null;//-Double.MAX_VALUE;
	/**
	 * usar getType()
	 */
	@Transient private SimpleFeatureType type = null;

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

	//constructor de copia superficial de la labor
	public Labor(Labor<?> l) {
		super();
		clasificador=new Clasificador();
		outCollection = new DefaultFeatureCollection("internal",getType());
		initConfigLabor();		
		
		this.nombre=l.nombre;
		this.fecha = l.fecha;
		this.precioLabor = l.precioLabor;//esto queda en 0
		this.precioInsumo = l.precioInsumo;//esto queda en 0 
		//this.cantidadInsumo = l.cantidadInsumo;//es total
		//this.cantidadLabor = l.cantidadLabor;//es total
		this.productoLabor=l.productoLabor;
		this.anchoDefaultProperty.set(l.getAnchoDefaultProperty().get());	
			
		//se inicia en la implementacion init 
		this.colAmount.set(l.colAmount.get());
		
		this.colElevacion.set(l.colElevacion.get());
		this.colAncho.set(l.colAncho.get());
		this.colCurso.set(l.colCurso.get());
		this.colDistancia.set(l.colDistancia.get());
	}

	private void initConfigLabor() {
		
		List<String> availableColums = this.getAvailableColumns();		
		LaborConfig laborConfig = getConfigLabor();
		Configuracion properties = laborConfig.getConfigProperties();

		colElevacion = PropertyHelper.initStringProperty(COLUMNA_ELEVACION, properties, availableColums);
		colAncho = PropertyHelper.initStringProperty(COLUMNA_ANCHO, properties, availableColums);
		colCurso = PropertyHelper.initStringProperty(COLUMNA_CURSO, properties, availableColums);
		colDistancia = PropertyHelper.initStringProperty(COLUMNA_DISTANCIA, properties, availableColums);
		

		/********************** inicializo las propiedades de la labor propiamente dichas********************************/
		//fechaProperty = new SimpleObjectProperty<LocalDate>();
		DateConverter dc = new DateConverter(); 		
		String defaultDate = properties.getPropertyOrDefault(Labor.FECHA_KEY,	dc.toString(LocalDate.now()));	
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");


		try {
			this.fecha = df.parse(defaultDate);// Unparseable date: "30/04/2018"
		} catch (ParseException e) {
			this.fecha=new Date();
			System.out.println("fallo el parse de la fecha default");
			e.printStackTrace();
		}

		precioLabor = initPrecioLaborHa();// initDoubleProperty(CosechaLabor.COSTO_COSECHA_HA, properties);
		precioInsumo = initPrecioInsumo(); //initDoubleProperty(FertilizacionLabor.COLUMNA_PRECIO_FERT,  "0", properties);	

		// anchoDefaultProperty
		anchoDefaultProperty = PropertyHelper.initDoubleProperty(ANCHO_DEFAULT, "8", properties);

		clasificador.tipoClasificadorProperty.set(
				properties.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,	Clasificador.clasficicadores[0]));
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

	/**
	 * @return the precioLaborProperty
	 */

	//	public Double getPrecioLabor() {
	//		return precioLaborProperty.get();
	//	}
	//
	//
	//	/**
	//	 * @param precioLaborProperty the precioLaborProperty to set
	//	 */
	//	public void setPrecioLabor(Double precioLabor) {
	//		if(precioLabor==null)precioLabor=0.0;
	//		this.precioLaborProperty.set(precioLabor); 
	//	}
	//
	//
	//	
	//	public Double getPrecioInsumo() {
	//		if(precioInsumoProperty==null) {
	//			precioInsumoProperty=	new SimpleDoubleProperty();
	//		}
	//		return precioInsumoProperty.get();
	//	}
	//	public void setPrecioInsumo(Double precioInsumo) {
	//		if(precioInsumoProperty==null) {
	//			precioInsumoProperty=	new SimpleDoubleProperty();
	//		}
	//		if(precioInsumo==null)precioInsumo=0.0;
	//		this.precioInsumoProperty.set(precioInsumo); 
	//	}

	//	//@Temporal(TemporalType.DATE)
	//	public Date getFecha() {
	//		LocalDate lDate = this.fechaProperty.getValue();
	//		Date date = null;
	//		if(lDate !=null) {
	//			date = Date.from(lDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	//		}
	//		return date ;
	//	}
	//	
	//	public void setFecha(Date date) {	
	////		Calendar cal = Calendar.getInstance();
	////		if(date !=null)		cal.setTime(date);	
	//		LocalDate lDate = null;
	//		if(date!=null) {
	//			DateConverter dc = new DateConverter(); 		
	//			String defaultDate = date.toString();//config.getConfigProperties().getPropertyOrDefault(Labor.FECHA_KEY,	dc.toString(LocalDate.now()));	
	//			//LocalDate ld = dc.fromString(dc.toString(LocalDate.now()));		
	//			lDate = dc.fromString(defaultDate);		
	//		}
	//		this.fechaProperty = new SimpleObjectProperty<LocalDate>(lDate);
	//	}


	/**
	 * metodo que devuelve el costo de la labor por hectarea de acuerdo a la configuracion del tipo de labor que implemente
	 * @return DoubleProperty
	 */
	@Transient
	protected abstract Double initPrecioLaborHa() ;

	/**
	 * metodo que devuelve el costo de la labor por hectarea de acuerdo a la configuracion del tipo de labor que implemente
	 * @return DoubleProperty
	 */
	@Transient
	protected abstract Double initPrecioInsumo() ;


	/**
	 * 
	 * @param key el valor por defecto para la propiedad
	 * @param properties un objeto Configuracion a ser modificado
	 * @param availableColums la lista de opsiones para configurar las propiedades
	 * @return devuelve una nueva StringProperty inicializada con el valor correspondiente de las availableColums o la key proporcionada
	 */
	//	public static SimpleStringProperty initStringProperty(String key,Configuracion properties,List<String> availableColums){
	//		SimpleStringProperty sProperty = new SimpleStringProperty(
	//				properties.getPropertyOrDefault(key, key));
	//
	//		if(availableColums!=null && !availableColums.contains(sProperty.get()) && availableColums.contains(key)){
	//			sProperty.setValue(key);
	//		}
	//
	//		sProperty.addListener((obs, bool1, bool2) -> {
	//			properties.setProperty(key,	bool2.toString());
	//		});
	//		return sProperty;
	//	}

	@Transient
	public abstract  String getTypeDescriptors();

	@Transient
	public FileDataStore getInStore() {
		return inStore;
	}

	@Transient
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
				System.out.println("Prescription Type: "+DataUtilities.encodeType(schema));
				System.out.println(schema);
				String title = schema.getName().toString();
				//String title = inStore.getInfo().getTitle();
				if (title != null && !title.trim().isEmpty()) {
					setNombre(title.replaceAll("%20", " "));
				} else {
					// Fallback to a default name if title is null or empty
					setNombre("Labor_" + System.currentTimeMillis());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			//	if(nombreProperty.getValue() == null){
			//nombreProperty.set(inStore.getInfo().getTitle().replaceAll("%20", " "));
			

			// Get the name of the feature type
		

			//}
		}
	}

	@Transient
	public Clasificador getClasificador() {
		return clasificador;
	}

	@Transient
	public LaborLayer getLayer() {
		return layer;
	}

	@Transient
	public void setLayer(LaborLayer renderableLayer) {		
		this.layer = renderableLayer;
		renderableLayer.setValue(LABOR_LAYER_IDENTIFICATOR, this);//usar esto para no tener el layer dentro de la cosecha
		renderableLayer.setValue(LABOR_LAYER_CLASS_IDENTIFICATOR, this.getClass());
		//this.nombreProperty.addListener((o,old,nu)->{this.layer.setName(nu);});
		renderableLayer.setName(getNombre());//this.nombreProperty.get());
	}


	//	public StringProperty getNombreProperty(){
	//		return nombreProperty;
	//	}

	public void setNombre(String n) {
		if(layer!=null)this.layer.setName(n);
		this.nombre = n;
	}

	public void setOutCollection(DefaultFeatureCollection newOutcollection) {
		this.outCollection=newOutcollection;		
		this.clearCache();
	}


	@Transient
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

	@SuppressWarnings("unchecked")
	public List<E> cachedOutStoreQuery(Envelope envelope){
		return (List<E>) LaborDataStore.cachedOutStoreQuery(envelope, this);
		//		List<E> objects = new ArrayList<E>();
		//		synchronized(this){
		//			//si la cache crecio mucho la limito a un tamanio
		////			if(treeCache!=null && treeCache.size()>50*1000){//71053 se limpia todo el timepo
		////				System.out.println("limpiando cache con size = "+treeCache.size()+" envelope = "+envelope.toString());
		////				treeCache=null;
		////			}//esto no sirve porque updateAllCachedEnvelopes carga todas las features no solo las del envelope
		//			//TODO poner un timer si no se uso el treeCache en x segundos limpiarlo.
		//			if( treeCache==null){			
		//				updateAllCachedEnvelopes(envelope);			
		//			} 
		//		}
		//		@SuppressWarnings("unchecked")
		//		List<SimpleFeature> cachedObjects = treeCache.query(envelope);//FIXME Exception in thread "pool-2-thread-5" java.util.ConcurrentModificationException
		//		//el error se produjo al convertir un ndvi a cosecha
		//
		//		FeatureType schema = this.outCollection.getSchema();			    
		//		CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();		
		//		ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		
		//		Geometry geoEnv = GeometryHelper.constructPolygon(bbox);
		//		for(SimpleFeature sf : cachedObjects){
		//			Geometry sfGeom = (Geometry) sf.getDefaultGeometry();
		//			boolean intersects = false;
		//			if(sfGeom!=null){
		//				intersects = geoEnv.intersects(sfGeom);
		//			}
		//			if(intersects){
		//				objects.add(constructFeatureContainerStandar(sf,false));
		//			}
		//		}
		//
		//		return objects;
	}
	
	public int compareTo(Object dao){
		if(dao !=null && Labor.class.isAssignableFrom(dao.getClass())){
			
			return id.compareTo(((Labor<?>)dao).id);
		} else {
			return 0;
		}
	}
	
	public synchronized void clearCache(){//do not clear cache while people are working 
		synchronized(this) {
		if(treeCache!=null) {
			System.out.println("clearing Cache de "+this.nombre+" con size "+treeCache.size());
		}
		treeCache = null;
		treeCacheEnvelope=null;
		//long init = System.currentTimeMillis();
		//System.gc();
		//long end = System.currentTimeMillis();
		//System.out.println("tarde "+(end-init)+"ms en hacer gc()");
		}
		//tarde 3852ms en hacer gc()
	}
	//	private Envelope updateCachedEnvelope(Envelope envelope){
	//		Envelope cachedEnvelope = new Envelope(envelope);
	//		double height = cachedEnvelope.getHeight();
	//		double width = cachedEnvelope.getHeight();
	//		cachedEnvelope.expandBy(width*4, height*4);
	//		cachedEnvelopes.put(cachedEnvelope, outStoreQuery(cachedEnvelope));
	//		return cachedEnvelope;
	//	}

	//	private void updateAllCachedEnvelopes(Envelope envelope){
	//		treeCache=new Quadtree();
	////TODO cargar todas las features en memoria pero en guardarlas indexadas en cachedEnvelopes
	//		@SuppressWarnings("unchecked")
	//		Collection<SimpleFeature> items= Lists.newArrayList(outCollection.iterator());
	//		items.forEach((it)->{
	//			Geometry g =(Geometry) it.getDefaultGeometry();
	//			treeCache.insert(g.getEnvelopeInternal(), it);
	//		});
	//	}

	@SuppressWarnings("unchecked")
	public List<E> outStoreQuery(Envelope envelope){
		return (List<E>) LaborDataStore.outStoreQuery(envelope,this);
		//		List<E> objects = new ArrayList<E>();
		//		//TODO tratar de cachear todo lo posible para evitar repetir trabajo en querys consecutivas.
		//		//una udea es cachear un sector del out collection y solo hacer la query si el envelope esta fuera de lo cacheado
		//		if(this.outCollection.getBounds().intersects(envelope)){//solo hago la query si el bounds esta dentro del mapa
		//			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
		//			FeatureType schema = this.outCollection.getSchema();
		//
		//			// usually "THE_GEOM" for shapefiles
		//			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
		//			CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
		//
		//			ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		
		//
		//			BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);
		//
		//			SimpleFeatureCollection features = this.outCollection.subCollection(filter);//OK!! esto funciona
		//			// System.out.println("encontre "+features.size()+" que se intersectan con "+ bbox );
		//
		//			Polygon boundsPolygon = GeometryHelper.constructPolygon(bbox);
		//
		//			SimpleFeatureIterator featuresIterator = features.features();
		//			while(featuresIterator.hasNext()){
		//				SimpleFeature next = featuresIterator.next();
		//				Object obj = next.getDefaultGeometry();
		//
		//				Geometry geomEnvelope = null;
		//				if(obj instanceof Geometry){					
		//					geomEnvelope =(Geometry)obj;					 
		//				} 
		//
		//				boolean intersects = false;
		//				if(geomEnvelope!=null){
		//					intersects = geomEnvelope.intersects(boundsPolygon );
		//				}
		//				if(intersects){
		//					objects.add(constructFeatureContainerStandar(next,false));
		//				}
		//			}
		//			featuresIterator.close();
		//		}
		//
		//		return objects;
	}


	@SuppressWarnings("unchecked")
	public List<E> inStoreQuery(Envelope envelope) throws IOException{
		return (List<E>) LaborDataStore.inStoreQuery(envelope,this);
		//		List<E> objects = new ArrayList<E>();
		//		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
		//		FeatureType schema = this.inStore.getSchema();
		//
		//		// usually "THE_GEOM" for shapefiles
		//		String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
		//		CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor()
		//				.getCoordinateReferenceSystem();
		//
		//		ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		    
		//		BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);
		//
		//
		//		SimpleFeatureCollection features = this.inStore.getFeatureSource().getFeatures(filter);//OK!! esto funciona
		//
		//
		//		SimpleFeatureIterator featuresIterator = features.features();
		//		while(featuresIterator.hasNext()){
		//			objects.add(constructFeatureContainer(featuresIterator.next()));
		//		}
		//		featuresIterator.close();
		//		return objects;
	}




	public void insertFeature(E laborItem) {
		if(-1.0 == laborItem.getId()) {
			laborItem.setId(this.getNextID());
			System.out.println("actualizando el item con id "+laborItem.getId());			
		}
		LaborDataStore.insertFeature(laborItem,this);
		//		Geometry cosechaGeom = laborItem.getGeometry();
		//		Envelope geomEnvelope=cosechaGeom.getEnvelopeInternal();
		//
		//		synchronized(featureBuilder){
		//			SimpleFeature fe = laborItem.getFeature(featureBuilder);
		//			if(treeCache!=null){
		//				treeCache.insert(geomEnvelope, fe);
		//			}
		//			this.insertFeature(fe);
		//		}
	}
	/**
	 * 
	 * @param f feature para ser insertado
	 * Metodo que inserta el feature en outCollection.
	 * usar labor.add(feature) que lo agrega antes al cache
	 */
	public void insertFeature(SimpleFeature f){
		if(!outCollection.add(f)) {
			System.err.println("No se pudo insertar la feature "+f);
		}
	}

	public void constructClasificador() {
		constructClasificador(getClasificador().tipoClasificadorProperty.get());
	}

	public void constructClasificador(String nombreClasif) {
		this.clasificador.constructClasificador(nombreClasif,this);
	}

	@Transient
	public List<String> getAvailableColumns() {
		return LaborDataStore.getAvailableColumns(this);
	}

	@Override
	public String toString() {
		return getNombre();//nombreProperty.get();
	}

	/**
	 * metodo que se ocupa de hacer la limpieza al momento de quitar la labor
	 */
	public void dispose() {
		
		LaborDataStore.dispose(this);
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


	public static void main(String[] args) {
		System.out.println("testing build type");
		CosechaLabor l = new CosechaLabor();
		CosechaItem item = new CosechaItem();
		item.setObservaciones("observo una observacion");
		l.insertFeature(item);
		SimpleFeature sf = item.getFeature(l.getFeatureBuilder());
		if(!l.outCollection.add(sf)) {
			System.err.println("No se pudo insertar la feature "+sf);
		}
		
		SimpleFeatureIterator features = l.outCollection.features();
		
		
		for(SimpleFeature f=null ; features.hasNext();f=features.next()) {
			if(f!=null)System.out.println(f.toString());
		}
		/*
		 * "building descriptor 
		 * the_geom:MultiPolygon:4326,
		 * Distancia:Double,
		 * Curso(deg):Double,
		 * Ancho:Double,
		 * Elevacion:Double,
		 * Categoria:Integer,
		 * Observaciones:String,
		 * Rendimient:Double,
		 * DesvRendim:Double,
		 * CostoLbTn:Double,
		 * CostoLbHa:Double,
		 * precio_gra:Double,
		 * importe_ha:Double"
		 */

		
	}
	
	@Transient 
	public SimpleFeatureBuilder getFeatureBuilder() {
		return new SimpleFeatureBuilder(getType());
	}
	
	@Transient
	public SimpleFeatureType getType() {
		if(type==null) {
		String typeDescriptor = "the_geom:MultiPolygon:4326,"//"*geom:Polygon,"the_geom
				+ COLUMNA_DISTANCIA + ":Double,"
				+ COLUMNA_CURSO + ":Double,"
				+ COLUMNA_ANCHO + ":Double,"
				+ COLUMNA_ELEVACION + ":Double,"
				+ COLUMNA_CATEGORIA + ":Integer,"
				+ COLUMNA_OBSERVACIONES + ":String,";
		typeDescriptor+= getTypeDescriptors();
		System.out.println("building descriptor "+typeDescriptor);
		try {
			type = DataUtilities.createType("LABOR", typeDescriptor);
		} catch (SchemaException e) {

			e.printStackTrace();
		}
		}
		return type;
	}

	@Transient
	public SimpleFeatureType getPointType() {
		SimpleFeatureType type = null;
		String typeDescriptor = "the_geom:Point:4326,"//"*geom:Polygon,"the_geom
				+ COLUMNA_DISTANCIA + ":Double,"
				+ COLUMNA_CURSO + ":Double,"
				+ COLUMNA_ANCHO + ":Double,"
				+ COLUMNA_ELEVACION + ":Double,"
				+ COLUMNA_CATEGORIA + ":Integer,";
		typeDescriptor+= getTypeDescriptors();
		//typeDescriptor.split(",");
		try {
			type = DataUtilities.createType("LABOR", typeDescriptor);
		} catch (SchemaException e) {

			e.printStackTrace();
		}
		return type;
	}
	//este metodo estaba en cosechaLabor y lo traje a Labor para generalizarlo.
	public void changeFeature(SimpleFeature old, LaborItem ci) {
		LaborDataStore.changeFeature(old,ci,this);
	}

	/**
	 * read from simple feature standar to LaborItem
	 * @param ci
	 * @param harvestFeature
	 * @param newIDS
	 */
	public void constructFeatureContainerStandar(LaborItem ci, SimpleFeature harvestFeature, Boolean newIDS) {
		ci.labor=this;
		ci.id = LaborItem.getDoubleFromObj(LaborItem.getID(harvestFeature));
		if(ci.id ==null || newIDS){// flag que me permita ignorar el id del feature y asignar uno nuevo
			ci.id= this.getNextID();
		}
		ci.categoria = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_CATEGORIA)).intValue();

		ci.distancia = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_DISTANCIA));
		ci.rumbo = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_CURSO));
		ci.ancho = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_ANCHO));
		ci.elevacion = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_ELEVACION));
		String obs = (String) harvestFeature.getAttribute(COLUMNA_OBSERVACIONES);
		//System.out.println("obs es "+obs);//obs es 
		ci.observaciones =obs!=null?obs:""; 
	}

	/**
	 * 
	 * @param feature
	 * @param attribute
	 * @return devuelve true si el feature tiene el attribute
	 */
	public boolean contieneAtributte(SimpleFeature feature,String attribute) {
		if( attribute==null)return false;
		for(AttributeDescriptor att : feature.getType().getAttributeDescriptors()) {
			if(att != null && attribute.equals(att.getLocalName())) {
				return true;
			}
		}
		return false;	
	}

	/**
	 * este metodo es llamado desde la labor para inicializar LaborItem con los datos del SimpleFeature
	 * read from simple feature with defined columns to LaborItem
	 * @param ci
	 * @param harvestFeature
	 * @param newIDS
	 */
	public void constructFeatureContainer(LaborItem ci, SimpleFeature harvestFeature) {
		ci.setId(getNextID());

		String idString = LaborItem.getID(harvestFeature);
		ci.setId(LaborItem.getDoubleFromObj(idString));

		double toMetros = getConfigLabor().valorMetrosPorUnidadDistanciaProperty().doubleValue();

		if(contieneAtributte(harvestFeature,colAncho.get())) {
			ci.setAncho(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colAncho.get())));
			ci.setAncho(ci.getAncho() * toMetros);
		}
		String distColumn = colDistancia.get();// getColumn(COLUMNA_DISTANCIA);//TODO
		if(contieneAtributte(harvestFeature,distColumn)) {
			try {//para permitir editar la distancia
				ci.setDistancia(Double.parseDouble(distColumn));
			} catch (Exception e) {//si distColumn no es un numero procedo a parsear el contenido
				Object distAttribute = harvestFeature.getAttribute(distColumn);

				ci.setDistancia(LaborItem.getDoubleFromObj(distAttribute)
						* toMetros);
			}
		}
		if(contieneAtributte(harvestFeature,colCurso.get())) {
			ci.setRumbo(LaborItem.getDoubleFromObj(harvestFeature
					.getAttribute(colCurso.get())));
		}


		if(!colElevacion.get().equals(Labor.NONE_SELECTED)){

			if(contieneAtributte(harvestFeature,colElevacion.get())) {
				Double elevacion =  LaborItem.getDoubleFromObj(harvestFeature
						.getAttribute(colElevacion.get()));

				ci.setElevacion(elevacion);
			}
		} else{
			ci.setElevacion(1.0);
		}
		if(contieneAtributte(harvestFeature,COLUMNA_OBSERVACIONES)) {
			String obs = (String) harvestFeature.getAttribute(COLUMNA_OBSERVACIONES);
			ci.setObservaciones(obs);
		}		
	}
	
	/**
	 * 
	 * @param item
	 * @return devuelve true si el item esta en outCollection
	 */
	public boolean owns(LaborItem item) {
		//FIXME si tengo un clon y una original se confunde
		Boolean ret =false;
		Class<? extends LaborItem> itemClass = item.getClass();
		List<E> queryRes = this.cachedOutStoreQuery(item.geometry.getEnvelopeInternal());
		if(queryRes.size()>0) {
			for(E i:queryRes) {
				if(i.getClass().isAssignableFrom(itemClass)) {
					if(i.equals(item)) {
						return true;
					}
				}else {
					return false;//no es de la misma clase
				}
			}
		}
		return ret;
	}

//	public Poligono getContorno() {
//		if(contorno==null) {
//			GeometryHelper.extractContorno(this);
//		}
//		return contorno;
//	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if(o==null)return false;
		if (!(Labor.class.isAssignableFrom(o.getClass()))) return false;
		Labor<E> lab = (Labor<E>) o;
		return id != null && id.equals((lab).id);
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
	 * solo usar al leer un shp por primera vez
	 * @param next
	 * @return
	 */
	public abstract E constructFeatureContainer(SimpleFeature next) ;

	public abstract LaborConfig getConfigLabor();
}
