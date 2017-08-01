package dao;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.index.quadtree.QuadTree;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.api.client.util.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.config.Agroquimico;
import dao.config.Configuracion;
import dao.config.Cultivo;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
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
@Entity
public abstract class Labor<E extends LaborItem>  {
	public static final double FEET_TO_METERS = 0.3048;
	public static final String NONE_SELECTED = "Ninguna";
	public static final String LABOR_LAYER_IDENTIFICATOR = "LABOR";
	public FileDataStore inStore = null;
	//public ShapefileDataStore outStore = null;
	public DefaultFeatureCollection outCollection=null;
	public DefaultFeatureCollection inCollection=null;
	public StringProperty nombreProperty = new SimpleStringProperty();
	public LaborLayer layer;//realmente quiero guardar esto aca?? o me conviene ponerlo en un mapa en otro lado para evitar la vinculacion de objetos

	protected static final String COLUMNA_CATEGORIA = "Categoria";
	public static final String COLUMNA_DISTANCIA = "Distancia";
	public static final String COLUMNA_CURSO = "Curso(deg)";
	public static final String COLUMNA_ANCHO = "Ancho";
	public static final String COLUMNA_ELEVACION = "Elevacion";

	private static final String ANCHO_DEFAULT = "ANCHO_DEFAULT";
	private static final String FECHA_KEY = "FECHA_KEY";

	public Clasificador clasificador=null;
	public SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(getType());
	public StringProperty colAmount=null; //usada por el clasificador para leer el outstore tiene que ser parte de TYPE
	//columnas configuradas para leer el instore
	public StringProperty colElevacion=null;
	public StringProperty colAncho=null;
	public StringProperty colCurso=null;
	public StringProperty colDistancia=null;// = new
	// SimpleStringProperty(CosechaLabor.COLUMNA_DISTANCIA);

	private Double nextID =new Double(0);//XXX este id no es global sino que depende de la labor

	/**
	 * precio es el costo por hectarea de la labor
	 */
	public Property<LocalDate> fechaProperty=null;
	public DoubleProperty precioLaborProperty=null;
	public DoubleProperty precioInsumoProperty=null;
	public SimpleDoubleProperty anchoDefaultProperty= null;

	public Map<Envelope,List<E>> cachedEnvelopes=Collections.synchronizedMap(new HashMap<Envelope,List<E>>());
	
	public Quadtree treeCache = null;
	
//	private CoordinateReferenceSystem targetCRS;

	public LaborConfig config = null;

	public Double minElev=Double.MAX_VALUE;
	public Double maxElev=-Double.MAX_VALUE;
	//average 
	//desvio
	public Double minAmount=Double.MAX_VALUE;
	public Double maxAmount=-Double.MAX_VALUE;
	//average
	//desvio
	
	//TODO cambiar por un referencedEnvelope
	public Position minX =null; //Position.fromDegrees(latitude, longitude);
	public Position minY = null; //Double.MAX_VALUE;
	public Position maxX =null; //-Double.MAX_VALUE;
	public Position maxY = null;//-Double.MAX_VALUE;

	public Geometry envelope = null;// no puedo cachear la geometria total porque tarda mucho

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


	/**
	 * metodo que devuelve el costo de la labor por hectarea de acuerdo a la configuracion del tipo de labor que implemente
	 * @return DoubleProperty
	 */
	protected abstract DoubleProperty initPrecioLaborHaProperty() ;
	
	/**
	 * metodo que devuelve el costo de la labor por hectarea de acuerdo a la configuracion del tipo de labor que implemente
	 * @return DoubleProperty
	 */
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

	public abstract  String getTypeDescriptors();

	public FileDataStore getInStore() {
		return inStore;
	}

	public void setInStore(FileDataStore inStore) {
		if(this.inStore!=null){
			this.inStore.dispose();
		}
		if(inStore !=null){
			this.inStore = inStore;

			//	if(nombreProperty.getValue() == null){
			nombreProperty.set(inStore.getInfo().getTitle().replaceAll("%20", " "));

			//}
		}
	}

	public Clasificador getClasificador() {
		return clasificador;
	}

	public LaborLayer getLayer() {
		return layer;
	}

	public void setLayer(LaborLayer renderableLayer) {		
		this.layer = renderableLayer;
		renderableLayer.setValue(LABOR_LAYER_IDENTIFICATOR, this);//TODO usar esto para no tener el layer dentro de la cosecha
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
			if( treeCache==null){			
				updateAllCachedEnvelopes(envelope);			
			}
		}
		@SuppressWarnings("unchecked")
		List<SimpleFeature> cachedObjects = treeCache.query(envelope);

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

	public abstract E constructFeatureContainerStandar(SimpleFeature next,boolean newIDS) ;

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
			System.out.println("construyendo clasificador jenkins");
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
	public DefaultFeatureCollection getInCollection() {
		return inCollection;
	}


	/**
	 * @param inCollection the inCollection to set
	 */
	public void setInCollection(DefaultFeatureCollection inCollection) {
		this.inCollection = inCollection;
	}


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
