package dao;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.config.Configuracion;
import dao.config.Cultivo;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.layers.RenderableLayer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import utils.ProyectionConstants;

/**
 * hace las veces de un featureStore con los metodos especificos para manejar el tipo de labor especifico
 * @author tomas
 *
 * @param <E>
 */
public abstract class Labor<E extends FeatureContainer>  {
	public static final String NONE_SELECTED = "Ninguna";
	public static final String LABOR_LAYER_IDENTIFICATOR = "LABOR";
	public FileDataStore inStore = null;
	//public ShapefileDataStore outStore = null;
	public DefaultFeatureCollection outCollection=null;
	public StringProperty nombreProperty = new SimpleStringProperty();
	public RenderableLayer layer;//realmente quiero guardar esto aca?? o me conviene ponerlo en un mapa en otro lado para evitar la vinculacion de objetos

	protected static final String COLUMNA_CATEGORIA = "Categoria";
	public static final String COLUMNA_DISTANCIA = "Distancia";
	public static final String COLUMNA_CURSO = "Curso(deg)";
	public static final String COLUMNA_ANCHO = "Ancho";
	public static final String COLUMNA_ELEVACION = "Elevacion";

	private static final String ANCHO_DEFAULT = "ANCHO_DEFAULT";

	public Clasificador clasificador=null;
	public SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(getType());
	public StringProperty colAmount=null; //usada por el clasificador para leer el outstore
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
	public DoubleProperty precioLaborProperty=null;
	public DoubleProperty precioInsumoProperty=null;
	public SimpleDoubleProperty anchoDefaultProperty= null;

	public Map<Envelope,List<E>> cachedEnvelopes=Collections.synchronizedMap(new HashMap<Envelope,List<E>>());
	private CoordinateReferenceSystem targetCRS;

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

		LaborConfig laborConfig = getConfigLabor();
		Configuracion properties = laborConfig.getConfigProperties();

		colElevacion = new SimpleStringProperty(
				properties.getPropertyOrDefault(CosechaLabor.COLUMNA_ELEVACION,
						CosechaLabor.COLUMNA_ELEVACION));
		if(!availableColums.contains(colElevacion.get()) && 
				availableColums.contains(CosechaLabor.COLUMNA_ELEVACION)){
			colElevacion.setValue(CosechaLabor.COLUMNA_ELEVACION);
		}
		colElevacion.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_ELEVACION,
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




		precioLaborProperty = initPrecioLaborHaProperty();// initDoubleProperty(CosechaLabor.COSTO_COSECHA_HA, properties);

		// anchoDefaultProperty
		anchoDefaultProperty = initDoubleProperty(ANCHO_DEFAULT, "8", properties);

		clasificador.tipoClasificadorProperty = new SimpleStringProperty(
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


	public static SimpleDoubleProperty initDoubleProperty(String key,String def,Configuracion properties){
		SimpleDoubleProperty doubleProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						key, def)));
		doubleProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(key,
					bool2.toString());
		});
		return doubleProperty;
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

	public RenderableLayer getLayer() {
		return layer;
	}

	public void setLayer(RenderableLayer renderableLayer) {		
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

	public List<E> cachedOutStoreQuery(Envelope envelope){
		List<E> objects = new ArrayList<E>();
		Envelope cachedEnvelope=null;
		if(cachedEnvelopes.size()>0){
			synchronized(cachedEnvelopes){
				for(Envelope ce : cachedEnvelopes.keySet()){
					if(ce.contains(envelope))cachedEnvelope=ce;
				}
			}
		}
		if( cachedEnvelope==null ){
			cachedEnvelope = updateCachedEnvelope(envelope);
			FeatureType schema = this.outCollection.getSchema();			    
			targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();		
		}

		ReferencedEnvelope bbox = new ReferencedEnvelope(envelope,targetCRS);		
		Polygon boundsPolygon = constructPolygon(bbox);
		List<E> cachedObjects = cachedEnvelopes.get(cachedEnvelope);

		for(E cachedObject : cachedObjects){
			Geometry geomEnvelope = cachedObject.getGeometry();
			boolean intersects = false;
			if(geomEnvelope!=null){
				intersects = geomEnvelope.intersects(boundsPolygon);
			}
			if(intersects){
				objects.add(cachedObject);
			}
		}

		return objects;
	}

	private Envelope updateCachedEnvelope(Envelope envelope){
		Envelope cachedEnvelope = new Envelope(envelope);
		double height = cachedEnvelope.getHeight();
		double width = cachedEnvelope.getHeight();
		cachedEnvelope.expandBy(width*4, height*4);

		cachedEnvelopes.put(cachedEnvelope, outStoreQuery(cachedEnvelope));
		return cachedEnvelope;
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
		if(cachedEnvelopes.size()>0){
			synchronized(cachedEnvelopes){
				for(Envelope ce : cachedEnvelopes.keySet()){
					if(ce.contains(geomEnvelope)){
						List<E> objects = cachedEnvelopes.get(ce);
						objects.add(cosechaFeature);
						cachedEnvelopes.replace(ce,  objects);
					}						
				}					
			}
		}

		SimpleFeature fe = cosechaFeature.getFeature(featureBuilder);
		this.insertFeature(fe);
	}

	public void insertFeature(SimpleFeature f){
		outCollection.add(f);
	}


	public void constructClasificador(String nombreClasif) {
		if (Clasificador.CLASIFICADOR_JENKINS.equalsIgnoreCase(nombreClasif)) {

			this.clasificador.constructJenksClasifier(this.outCollection,
					this.colAmount.get());
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

	/**
	 * metodo que se ocupa de hacer la limpieza al momento de quitar la labor
	 */
	public void dispose() {
		if(inStore!=null){

			inStore.dispose();
			inStore = null;
		}

		if(cachedEnvelopes!=null){
			cachedEnvelopes.clear();
			cachedEnvelopes = null;
		}

		if(outCollection!=null){
			outCollection.clear();
			outCollection=null;
		}

		if(layer!=null){
			layer.setValue(LABOR_LAYER_IDENTIFICATOR, null);
			layer.removeAllRenderables();
			layer.dispose();
			layer.getValues().clear();
			layer=null;
		}

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

	public void constructFeatureContainerStandar(FeatureContainer ci, SimpleFeature harvestFeature, Boolean newIDS) {
		ci.id = FeatureContainer.getDoubleFromObj(FeatureContainer.getID(harvestFeature));
		if(ci.id ==null || newIDS){// flag que me permita ignorar el id del feature y asignar uno nuevo
			ci.id= this.getNextID();
		}

		ci.distancia = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_DISTANCIA));
		ci.rumbo = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_CURSO));
		ci.ancho = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_ANCHO));
		ci.elevacion = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(COLUMNA_ELEVACION));

		if(this.clasificador!=null && clasificador.isInitialized()){
			Integer categoria = this.clasificador.getCategoryFor(ci.getAmount());
			if(categoria !=null)		ci.setCategoria(categoria);
		}	

		//		if(this.clasificador!=null){
		//			ci.categoria = clasificador.getCategoryFor(ci.getAmount());
		//		}

	}

	public void constructFeatureContainer(FeatureContainer ci, SimpleFeature harvestFeature) {
		ci.setId(getNextID());

		String idString = FeatureContainer.getID(harvestFeature);
		ci.setId(FeatureContainer.getDoubleFromObj(idString));

		double toMetros = getConfigLabor().valorMetrosPorUnidadDistanciaProperty()
				.doubleValue();

		String anchoColumn = colAncho.get();
		ci.setAncho(FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(anchoColumn)));
		ci.setAncho(ci.getAncho() * toMetros);

		String distColumn = colDistancia.get();// getColumn(COLUMNA_DISTANCIA);//TODO
		// pasar el constructor de
		// cosechaItem a la labor
		try {//para permitir editar la distancia
			ci.setDistancia(new Double(distColumn));
		} catch (Exception e) {
			Object distAttribute = harvestFeature.getAttribute(distColumn);

			ci.setDistancia(FeatureContainer.getDoubleFromObj(distAttribute)
					* toMetros);
		}

		ci.setRumbo(FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(colCurso.get())));//hay valores que tienen rumbo 0 o 270 que parecen ser errores por no tener continuidad

		//		ci.pasada = FeatureContainer.getDoubleFromObj(harvestFeature
		//				.getAttribute(colPasada.get()));

		if(!colElevacion.get().equals(Labor.NONE_SELECTED)){
			ci.setElevacion( FeatureContainer.getDoubleFromObj(harvestFeature
					.getAttribute(colElevacion.get())));
		}

	}

	public abstract LaborConfig getConfigLabor();



}
