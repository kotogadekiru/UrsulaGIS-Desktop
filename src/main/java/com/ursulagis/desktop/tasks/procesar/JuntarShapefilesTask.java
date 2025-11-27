package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.Messages;
//import gov.nasa.worldwind.layers.RenderableLayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class JuntarShapefilesTask extends ProgresibleTask<File>{
	List<FileDataStore> stores =null;
	File shapeFile=null;
	public JuntarShapefilesTask(List<FileDataStore> _stores,File _shapeFile){
		super();
		stores = _stores;
		shapeFile =_shapeFile;
	}
	
	public void process(List<FileDataStore> stores,File shapeFile){

		// seleccionar un directorio
		// buscar todos los shapefiles dentro de el directorio
		//List<FileDataStore> stores = chooseShapeFileAndGetMultipleStores(null);
		// buscar todas los atributos de cada shape
		List<String> newDescriptors = new ArrayList<String>();
		//attributeMapping contiene la relacion entre el nombre de origen(key) y el nombre de destino(value) para cada store
		Map<FileDataStore,Map<String,String>> attributeMaping=Collections.synchronizedMap(new HashMap<FileDataStore,Map<String,String>>());

		ReferencedEnvelope unionEnvelope = null;
		updateProgress(0, 100);
		if (stores == null)return;
		for(int i=0; i < stores.size(); i++){//FileDataStore store : stores){
			FileDataStore store =stores.get(i);
			List<AttributeDescriptor> descriptors;
			try {
				descriptors = store.getSchema().getAttributeDescriptors();

				Map<String,String> storeAttributeMapping =Collections.synchronizedMap(new HashMap<String,String>());
				for(AttributeDescriptor att:descriptors){
					String attClassName = att.getType().getBinding().getName();
					if(attClassName.contains("org.locationtech.jts.geom")){
						//System.out.println("ignorando el atributo "+att);
						continue;}
					String nAttName = att.getName().toString();// i+att.getName().toString();
					String nAtt=new String(nAttName+":"+attClassName);
					storeAttributeMapping.put(att.getName().toString(), nAttName);
					System.out.println(nAtt+" en "+store.getNames().get(0));
					if(!newDescriptors.contains(nAtt)){
						newDescriptors.add(nAtt);
					}
				}
				System.out.println("newDescriptors al final:\n"+newDescriptors);
				
				ReferencedEnvelope b = store.getFeatureSource().getBounds();
				if(unionEnvelope==null){
					unionEnvelope=b;
					//CoordinateReferenceSystem ref = b.getCoordinateReferenceSystem();
				}else{
				//	unionEnvelope.include(b);
					
					ReferencedEnvelope e = new ReferencedEnvelope(							
							Math.min(b.getMinX(), unionEnvelope.getMinX()),
							Math.max(b.getMaxX(), unionEnvelope.getMaxX()),
							Math.min(b.getMinY(), unionEnvelope.getMinY()),
							Math.max(b.getMaxY(), unionEnvelope.getMaxY()),
							unionEnvelope.getCoordinateReferenceSystem()
							);
					
					unionEnvelope.setBounds(e);
					
					
//					if(b.getArea()<unionEnvelope.getArea()){
//						unionEnvelope=b;
//					}

				}

				attributeMaping.put(store,storeAttributeMapping);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}//fin del for stores
		//System.out.println("attibuteMappings : "+newDescriptors);
		


		// crear un nuevo shapeFile type que contenga todas las features de los shapes ingresados
		String typeDescriptor = "the_geom:MultiPolygon:4326";
		for(String descriptor : newDescriptors){
			typeDescriptor+=","+descriptor;
		}
		updateProgress(10, 100);
		try {
			SimpleFeatureType type = DataUtilities.createType("JuntarShapes", typeDescriptor);

			DefaultFeatureCollection outCollection=new DefaultFeatureCollection("internal",type);	
			
			Number ancho=10;
			try{
				Configuracion config = Configuracion.getInstance();
				ancho = PropertyHelper.parseDouble(config.getPropertyOrDefault(CosechaConfig.ANCHO_GRILLA_KEY, "10"));
				//ancho= Messages.getNumberFormat().parse(Configuracion.getInstance().getPropertyOrDefault(CosechaConfig.ANCHO_GRILLA_KEY, "10"));
			}catch(Exception e )
			{
				e.printStackTrace();
				ancho=10;
			}
			// constriur una grilla que cubra todos los shapes
			List<Polygon>  grilla = construirGrilla(unionEnvelope, ancho.doubleValue());
			updateProgress(11, 100);
			this.featureCount = grilla.size();
			
			// por cada poligono de la grilla crear un nuevo SimpleFeature del nuevo tipo que contenga los valores de todos los shapes de entrada
			ConcurrentMap<Polygon,SimpleFeature > byPolygon =
					grilla.parallelStream().collect(() -> new  ConcurrentHashMap< Polygon,SimpleFeature>(),
							(map, poly) -> {
								try {
									this.featureNumber++;
									updateProgress( this.featureNumber, featureCount);
								SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(type);
								//boolean polygonHasFeatures=false;
								Map<Boolean,List<FileDataStore>> storeHasFeaturesMap = new HashMap<Boolean,List<FileDataStore>>();
								storeHasFeaturesMap.put(true, new ArrayList<FileDataStore>());
								storeHasFeaturesMap.put(false, new ArrayList<FileDataStore>());
								
								Envelope envelope = poly.getEnvelopeInternal();

								for(FileDataStore store : stores){
									boolean polygonHasInformation=construirJoinedFeature(store, poly, attributeMaping.get(store), fBuilder);
									storeHasFeaturesMap.get(polygonHasInformation).add(store);				
								}

								boolean joinedFeatureHasInformation = (storeHasFeaturesMap.get(true).size() > 0);

								if(joinedFeatureHasInformation){
									for(FileDataStore storeToInterpolate:storeHasFeaturesMap.get(false)){
										//System.out.println("interpolando la informacion faltante para el store "+storeToInterpolate );
									
										double height = envelope.getHeight();
										double width = envelope.getWidth();
										for(int i=0;i<10;i++){							
											//System.out.println("aumentando el tamanio del envelope "+(i+1)*2+" veces");
											
											//System.out.println("width= "+width+" heigh="+height);
											envelope.expandBy(width/2, height/2);
											// mientras devuelva false seguir duplicando el tamanio del envelope
											if(construirJoinedFeature(storeToInterpolate, GeometryHelper.constructPolygon(envelope), attributeMaping.get(storeToInterpolate), fBuilder)){
												//System.out.println(i+" consegui informacion para interpolar el punto "+ fBuilder);
												continue;
											}											
										}
									}

									fBuilder.set("the_geom", poly);
									SimpleFeature joinedFeature = fBuilder.buildFeature(null);//el parametro que toma es el id. si es null lo crea?
									//System.out.println("agregando el feature: "+joinedFeature);
									map.put(poly,joinedFeature);
								}
								}catch(Exception e) {
									e.printStackTrace();
								}
							},
							(map1, map2) -> map1.putAll(map2));

			byPolygon.values().forEach(o->{
				boolean ret = outCollection.add(o);
				if(!ret) {
					System.err.println("no se pudo ingresar el feature "+o.getID());
				}
			});
			
			//todo grabar el nuevo shapefile del nuevo tipo en el directorio ingresado
			
			//File shapeFile =  getNewShapeFile(null);
			updateProgress( 90, 100);
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			try {
				params.put("url", shapeFile.toURI().toURL());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			params.put("create spatial index", Boolean.FALSE);

			ShapefileDataStore newDataStore=null;
			SimpleFeatureSource featureSource = null;
			try {
				ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
				newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
				newDataStore.createSchema(type);	
				newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

				String typeName = newDataStore.getTypeNames()[0];
				featureSource = newDataStore.getFeatureSource(typeName);
			} catch (IOException e) {

				e.printStackTrace();
			}

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

				Transaction transaction = new DefaultTransaction("create");
				featureStore.setTransaction(transaction);


				try {
					featureStore.setFeatures(outCollection.reader());
					try {
						transaction.commit();
					} catch (Exception e1) {
						System.err.println("fallo el comit de la transaccion");
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
			updateProgress( 100, 100);
		} catch (Exception e) {//si falla obtener el la creacion del type desde el schema
			e.printStackTrace();
		}
	}

	private static boolean construirJoinedFeature(FileDataStore store, Geometry poly,Map<String,String> storeMapping, SimpleFeatureBuilder fBuilder){
		boolean hasFeatures = false;
		//Envelope envelope = poly.getEnvelopeInternal();
		List<SimpleFeature> storeFeatures =new LinkedList<SimpleFeature>( fileDataStoreQuery(store, poly));
		if(storeFeatures.size()>0){
			hasFeatures=true;

			SimpleFeature item = construirFeaturePromedio(storeFeatures,poly.getEnvelopeInternal());  
			if(item!=null){
				//Map<String, String> storeMapping = attributeMaping.get(store);
				for(String storeAttName : storeMapping.keySet()){
					String nuStoreAttName = storeMapping.get(storeAttName);
					Object nuStoreItemValue = item.getAttribute(storeAttName);
					//	System.out.println("storeName="+storeName+" nuStoreName="+nuStoreName+" value="+nuStoreItemValue);
					fBuilder.set(nuStoreAttName, nuStoreItemValue);//por cada store se vuelve a setear cada attribute si se repiten
				}
			}
		}
		return hasFeatures;
	}

	private static Double getKrigingWeight(Object oGeom,Envelope envelope){
		double ancho = envelope.getWidth();
		//la distancia no deberia ser mayor que 2^1/2*ancho, me tomo un factor de 10 por seguridad e invierto la escala para tener mejor representatividad
		//en vez de tomar de 0 a inf, va de ancho*(10-2^1/2) a 0
		ancho = Math.sqrt(2)*ancho;
		
		Double retDist = Double.valueOf(0.0);
		if(oGeom instanceof Geometry){
			Geometry geo = (Geometry)oGeom;
			GeometryFactory fact = geo.getFactory(); 
			Geometry center = fact.createPoint(envelope.centre());
			//FIXME distancia es mayor al ancho si la geometria es muy grande y se encuentra en contacto con el envelope
			double distancia =geo.distance(center);// /ProyectionConstants.metersToLat();

			double distanciaInvert = (ancho-distancia)/ancho;
			//if(distanciaInvert<0)System.out.println("distancia-1 es menor a cero "+distanciaInvert);
			/*
			 * 
			 */
			//los pesos van de ~ancho^2 para los mas cercanos a 0 para los mas lejanos
			retDist =  Math.pow(distanciaInvert,2);
			//System.out.println("distancia "+distancia+" peso "+retDist);
		}
		return retDist;
	}


	/*
	 * metodo que toma todos los features de un store que coinciden con un sector de la grilla y los resume en un solo feature
	 */
	private static SimpleFeature construirFeaturePromedio(List<SimpleFeature> storeFeatures, Envelope envelope) {
		if(storeFeatures.size()==0)return null;
		SimpleFeature f0 = storeFeatures.get(0);
		SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(f0.getType());
	//	int size=storeFeatures.size();
		List<AttributeDescriptor> attributes = f0.getType().getAttributeDescriptors();
		attributes.forEach(att->{
			String attClassName = att.getType().getBinding().getName();
			if(!attClassName.contains("org.locationtech.jts.geom")){
				if(Number.class.isAssignableFrom(att.getType().getBinding())){
					Double value=Double.valueOf(0.0);
					Double pesosTotal=Double.valueOf(0.0);
					for(SimpleFeature f:storeFeatures){
						Object gObject = (Geometry) f.getDefaultGeometry();

						Double weight = getKrigingWeight(gObject,envelope);
						value+=weight*getDoubleFromObj(f.getAttribute(att.getName()));
						pesosTotal+=weight;
					}
					value=value/pesosTotal;
					fBuilder.set(att.getName(), value);

				} else {
					fBuilder.set(att.getName(),f0.getAttribute(att.getName()));
				}

			}
		});

		// TODO promediar los valores numericos y asignar el los valores de texto como unico 
		//devolviendo una sola feature que represente a todas
		return fBuilder.buildFeature(null);
	}

	private static Collection<? extends SimpleFeature> fileDataStoreQuery(FileDataStore store, Geometry poly) {
		List<SimpleFeature> objects = new ArrayList<SimpleFeature>();

		FilterFactory ff = CommonFactoryFinder.getFilterFactory( GeoTools.getDefaultHints() );
		FeatureType schema;
		try {
			schema = store.getSchema();
			
			//Geometry geoEnv = GeometryHelper.constructPolygon(poly);

			// usually "THE_GEOM" for shapefiles
			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
			CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

			ReferencedEnvelope bbox = new ReferencedEnvelope(poly.getEnvelopeInternal(),targetCRS);		    
			BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);

			SimpleFeatureCollection features = store.getFeatureSource().getFeatures(filter);//OK!! esto funciona

			SimpleFeatureIterator featuresIterator = features.features();
			while(featuresIterator.hasNext()){
				SimpleFeature next = featuresIterator.next();
				Geometry geom = (Geometry)next.getDefaultGeometry();
				if(poly.intersects(geom)){
					next.setDefaultGeometry(poly.intersection(geom));
					objects.add(next);
				}
			}
			featuresIterator.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return objects;
	}

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	private static List<Polygon> construirGrilla(BoundingBox bounds,double ancho) {
		System.out.println("construyendo grilla");
		List<Polygon> polygons = new ArrayList<Polygon>();
		//convierte los bounds de longlat a metros
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong() - ancho/2;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat() - ancho/2;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong() + ancho/2;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat() + ancho/2;
		Double x0=minX;
		
		for(int x=0;(x0)<maxX;x++){
			x0=minX+x*ancho;
			Double x1=minX+(x+1)*ancho;
			
			for(int y=0;(minY+y*ancho)<maxY;y++){
				Double y0=minY+y*ancho;
				Double y1=minY+(y+1)*ancho;


				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());

				/**
				 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
				 * carro--B
				 * 
				 */
				Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
				// Empezar y terminar en
				// el mismo punto.
				// sentido antihorario

				//			GeometryFactory fact = X.getFactory();
				GeometryFactory fact = new GeometryFactory();


				//				DirectPosition upper = positionFactory.createDirectPosition(new double[]{-180,-90});
				//				DirectPosition lower = positionFactory.createDirectPosition(new double[]{180,90});
				//	Envelope envelope = geometryFactory.createEnvelope( upper, lower );

				LinearRing shell = fact.createLinearRing(coordinates);
				LinearRing[] holes = null;
				Polygon poly = new Polygon(shell, holes, fact);			
				polygons.add(poly);
			}
		}
		return polygons;
	}

//	private static List<FileDataStore> chooseShapeFileAndGetMultipleStores(List<File> files) {
//		if(files==null){
//			files =chooseFiles("SHP", "*.shp");;
//		}
//		List<FileDataStore> stores = new ArrayList<FileDataStore>();
//		if (files != null) {
//			for(File f : files){
//				try {
//					stores.add(FileDataStoreFinder.getDataStore(f));
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return stores;
//	}

//	/**
//	 * 
//	 * @param f1 filter Title "JPG"
//	 * @param f2 filter regex "*.jpg"
//	 */
//	private static List<File> chooseFiles(String f1,String f2) {
//		List<File> files =null;
//		FileChooser fileChooser = new FileChooser();
//		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(f1, f2));
//
//		try{
//			files = fileChooser.showOpenMultipleDialog(new Stage());
//		}catch(IllegalArgumentException e){
//			fileChooser.setInitialDirectory(null);
//			File file = fileChooser.showOpenDialog(new Stage());
//			files = new LinkedList<File>();
//			files.add(file);
//		}
//
//		return files;
//	}
//
//	private static File getNewShapeFile(String nombre) {
//		FileChooser fileChooser = new FileChooser();
//		fileChooser.setTitle("Guardar ShapeFile");
//		fileChooser.getExtensionFilters().add(
//				new FileChooser.ExtensionFilter("SHP", "*.shp"));
//
//		File lastFile = null;
//		Configuracion config =Configuracion.getInstance();
//		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
//		if(lastFileName != null){
//			lastFile = new File(lastFileName);
//		}
//		if(lastFile != null ){
//			fileChooser.setInitialDirectory(lastFile.getParentFile());
//			if(nombre == null){
//				nombre = lastFile.getName();
//			}
//			fileChooser.setInitialFileName(nombre);
//			config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
//		} else {
//			fileChooser.setInitialDirectory(null);
//		}
//
//		//if(file!=null)	fileChooser.setInitialDirectory(file.getParentFile());
//
//		File file = fileChooser.showSaveDialog(new Stage());
//
//		//System.out.println("archivo seleccionado para guardar "+file);
//
//		return file;
//	}

	public static Double getDoubleFromObj(Object o){

		Double d = 0.0; 
		if(o instanceof Double){
			d = (Double) o;
		} else  if(o instanceof Integer){
			d = Double.valueOf((Integer) o);
		} else  if(o instanceof Long){
			d = Double.valueOf((Long) o);
		} else if(o instanceof String){			
			StringConverter<Number> converter = new NumberStringConverter(Messages.getLocale());

			try{
				d=converter.fromString((String) o).doubleValue();
				//	d = Double.parseDouble((String) o);
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			//System.err.println("no se pudo leer la cantidad de " +o);//no se pudo leer la cantidad de L3:CARG0003

		}
		return d;
	}

	@Override
	protected File call() throws Exception {
		process(stores,shapeFile);
		return shapeFile;
	}
}
