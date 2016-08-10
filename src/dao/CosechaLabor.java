package dao;

import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStore;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import utils.ProyectionConstants;

public class CosechaLabor extends Labor<CosechaItem> {
	private static final int KG_POR_TN = 1000;
	private static final double KG_POR_LIBRA = 0.453592;
	// private static final int KG_POR_TN = 1000;

	public static final String COLUMNA_ELEVACION = "Elevacion";
	public static final String COLUMNA_VELOCIDAD = "Velocidad";
	public static final String COLUMNA_RENDIMIENTO = "Rendimient";
	public static final String COLUMNA_ANCHO = "Ancho";
	public static final String COLUMNA_CURSO = "Curso(deg)";
	public static final String COLUMNA_DISTANCIA = "Distancia";
	//public static final String COLUMNA_PASADA = "Num__de_pa";

	public static final String COLUMNA_PRECIO = "precio_gra";
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";
	

	private static final String PRECIO_GRANO = "precioGrano";
	private static final String CORRECCION_COSECHA = "CORRECCION_COSECHA";
	private static final String COSTO_COSECHA_TN = "COSTO_COSECHA_TN";
	private static final String COSTO_COSECHA_HA = "COSTO_COSECHA_HA";
	private static final String ANCHO_DEFAULT = "ANCHO_DEFAULT";
	private static final String MAX_RINDE_KEY = "MAX_RINDE";
	private static final String MIN_RINDE_KEY = "MIN_RINDE";
	
	private static final String PRODUCTO_DEFAULT = "CultivoDefault";


	public CosechaConfig config = null;// HarvestFiltersConfig.getInstance();


	public StringProperty colVelocidad;// = new
										// SimpleStringProperty(CosechaLabor.COLUMNA_VELOCIDAD);
	// = new
										// SimpleStringProperty(CosechaLabor.COLUMNA_VELOCIDAD);
	public StringProperty colRendimiento;// = new
											// SimpleStringProperty(CosechaLabor.COLUMNA_RENDIMIENTO);
	

	//public StringProperty colPasada;// = new
									// SimpleStringProperty(CosechaLabor.COLUMNA_PASADA);

	public SimpleDoubleProperty precioGranoProperty;// = new
													// SimpleDoubleProperty(getPrecioGrano());
	public SimpleDoubleProperty correccionCosechaProperty;// = new
															// SimpleDoubleProperty(getCorreccionCosecha());
	public SimpleDoubleProperty maxRindeProperty;// = new
													// SimpleDoubleProperty(getCorreccionCosecha());
	public SimpleDoubleProperty minRindeProperty;// = new
													// SimpleDoubleProperty(getCorreccionCosecha());
	public SimpleDoubleProperty costoCosechaTnProperty;
	public SimpleDoubleProperty anchoDefaultProperty;
	
	public Property<Producto> producto;
	
	
	//Double nextCosechaID =new Double(0);//XXX este id no es global sino que depende de la labor
	/**
	 * constructor que sirve para crear una cosecha artificial cuando no tiene
	 * un datastore que la represente
	 */
	
	public CosechaLabor() {
		initConfig();
	}

	//XXX ver como los listeners de las propiedades me afectan el archivo de properties y 
	//el controller de la configuracion. creo que setea las variables pero nunca las graba a menos 
	//que las grabe el controller
	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		
		
		config = new CosechaConfig();
		Configuracion properties = config.config;

		colVelocidad = new SimpleStringProperty(
				properties.getPropertyOrDefault(CosechaLabor.COLUMNA_VELOCIDAD,
						CosechaLabor.COLUMNA_VELOCIDAD));
		if(!availableColums.contains(colVelocidad.get())&&availableColums.contains(CosechaLabor.COLUMNA_VELOCIDAD)){
			colVelocidad.setValue(CosechaLabor.COLUMNA_VELOCIDAD);
		}
		
		
		//TODO si colVelocidad no esta en el store y si esta CosechaLabor.COLUMNA_VELOCIDAD seleccionar CosechaLabor.COLUMNA_VELOCIDAD
		colVelocidad.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_VELOCIDAD,
					bool2.toString());
		});

		colElevacion = new SimpleStringProperty(
				properties.getPropertyOrDefault(CosechaLabor.COLUMNA_ELEVACION,
						CosechaLabor.COLUMNA_ELEVACION));
		if(!availableColums.contains(colElevacion.get())&&availableColums.contains(CosechaLabor.COLUMNA_ELEVACION)){
			colElevacion.setValue(CosechaLabor.COLUMNA_ELEVACION);
		}
		colElevacion.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_ELEVACION,
					bool2.toString());
		});

		colRendimiento = new SimpleStringProperty(
				properties.getPropertyOrDefault(
						CosechaLabor.COLUMNA_RENDIMIENTO,
						CosechaLabor.COLUMNA_RENDIMIENTO));
		if(!availableColums.contains(colRendimiento.get())&&availableColums.contains(CosechaLabor.COLUMNA_RENDIMIENTO)){
			colRendimiento.setValue(CosechaLabor.COLUMNA_RENDIMIENTO);
		}
		colRendimiento.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_RENDIMIENTO,
					bool2.toString());
		});
		colAmount= new SimpleStringProperty(CosechaLabor.COLUMNA_RENDIMIENTO);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		colAncho = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_ANCHO, CosechaLabor.COLUMNA_ANCHO));
		if(!availableColums.contains(colAncho.get())&&availableColums.contains(CosechaLabor.COLUMNA_ANCHO)){
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

//		colPasada = new SimpleStringProperty(properties.getPropertyOrDefault(
//				CosechaLabor.COLUMNA_PASADA, CosechaLabor.COLUMNA_PASADA));
//		if(!availableColums.contains(colPasada.get())&&availableColums.contains(CosechaLabor.COLUMNA_PASADA)){
//			colPasada.setValue(CosechaLabor.COLUMNA_PASADA);
//		}
//		colPasada.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(CosechaLabor.COLUMNA_PASADA,
//					bool2.toString());
//		});

		correccionCosechaProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						CosechaLabor.CORRECCION_COSECHA, "100")));
		correccionCosechaProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.CORRECCION_COSECHA,
					bool2.toString());
		});

		maxRindeProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						CosechaLabor.MAX_RINDE_KEY, "0")));
		maxRindeProperty
				.addListener((obs, bool1, bool2) -> {
					properties.setProperty(CosechaLabor.MAX_RINDE_KEY,
							bool2.toString());
				});

		minRindeProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						CosechaLabor.MIN_RINDE_KEY, "0")));
		minRindeProperty
				.addListener((obs, bool1, bool2) -> {
					properties.setProperty(CosechaLabor.MIN_RINDE_KEY,
							bool2.toString());
				});

		precioGranoProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						CosechaLabor.PRECIO_GRANO, "0")));
		precioGranoProperty
				.addListener((obs, bool1, bool2) -> {
					properties.setProperty(CosechaLabor.PRECIO_GRANO,
							bool2.toString());
				});

		costoCosechaTnProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						CosechaLabor.COSTO_COSECHA_TN, "0")));
		costoCosechaTnProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COSTO_COSECHA_TN,
					bool2.toString());
		});

		precioLaborProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						CosechaLabor.COSTO_COSECHA_HA, "0")));
		precioLaborProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COSTO_COSECHA_HA,
					bool2.toString());
		});

		// anchoDefaultProperty
		anchoDefaultProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						CosechaLabor.ANCHO_DEFAULT, "8")));
		anchoDefaultProperty
				.addListener((obs, bool1, bool2) -> {
					properties.setProperty(CosechaLabor.ANCHO_DEFAULT,
							bool2.toString());
				});

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
				}
			);
		

		
		String productoKEY = properties.getPropertyOrDefault(
				CosechaLabor.PRODUCTO_DEFAULT, "Maiz");
		 producto = new SimpleObjectProperty<Producto>(Producto.productos.get(productoKEY));//values().iterator().next());
	}

	public CosechaLabor(FileDataStore store) {
		this.setInStore(store);// esto configura el nombre
		
		initConfig();

	}

	// public void setPrecioGrano(Double precioGrano) {
	// this.precioLabor=precioGrano;//precio es el costo de la labor por ha
	// }
	// public void setCorreccionRinde(Double correccionRinde1) {
	// //System.out.println("nuevo correccion rinde es "+correccionRinde1);
	// correccionRinde = correccionRinde1;
	// }

public static SimpleFeatureType getFeatureType() {
	SimpleFeatureType type = null;
	try {
		/*
		 * geom tiene que ser Point, Line o Polygon. no puede ser Geometry
		 * porque podria ser cualquiera y solo permite un tipo por archivo
		 * los nombre de las columnas no pueden ser de mas de 10 char
		 */
		
		

		type = DataUtilities.createType("Cosecha", "the_geom:MultiPolygon:srid=4326,"//"*geom:Polygon,"the_geom
				+ CosechaLabor.COLUMNA_DISTANCIA + ":Double,"
				+ CosechaLabor.COLUMNA_CURSO + ":Double,"
				+ CosechaLabor.COLUMNA_ANCHO + ":Double,"
				+ CosechaLabor.COLUMNA_RENDIMIENTO + ":Double,"
				+ CosechaLabor.COLUMNA_VELOCIDAD + ":Double,"
				+ CosechaLabor.COLUMNA_ELEVACION + ":Double,"
				+ CosechaLabor.COLUMNA_PRECIO + ":Double,"
				+ CosechaLabor.COLUMNA_IMPORTE_HA + ":Double,"
				+ CosechaLabor.COLUMNA_CATEGORIA + ":Integer");
	} catch (SchemaException e) {

		e.printStackTrace();
	}
	return type;
}

public static SimpleFeatureType getPointsFeatureType() {
	SimpleFeatureType type = null;
	try {
		/*
		 * geom tiene que ser Point, Line o Polygon. no puede ser Geometry
		 * porque podria ser cualquiera y solo permite un tipo por archivo
		 * los nombre de las columnas no pueden ser de mas de 10 char
		 */

		type = DataUtilities.createType("Cosecha", "the_geom:Point:srid=4326,"//"*geom:Polygon,"the_geom
				+ CosechaLabor.COLUMNA_DISTANCIA + ":Double,"
				+ CosechaLabor.COLUMNA_CURSO + ":Double,"
				+ CosechaLabor.COLUMNA_ANCHO + ":Double,"
				+ CosechaLabor.COLUMNA_RENDIMIENTO + ":Double,"
				+ CosechaLabor.COLUMNA_VELOCIDAD + ":Double,"
				+ CosechaLabor.COLUMNA_ELEVACION + ":Double,"
				+ CosechaLabor.COLUMNA_PRECIO + ":Double,"
				+ CosechaLabor.COLUMNA_IMPORTE_HA + ":Double,"
				+ CosechaLabor.COLUMNA_CATEGORIA + ":Integer");
	} catch (SchemaException e) {

		e.printStackTrace();
	}
	return type;
}

	@Override
	public SimpleFeatureType getType() {
		return CosechaLabor.getFeatureType();
	}

//	public void insertFeature(CosechaItem cosechaFeature) {
//		// SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
//		// getType());
//		SimpleFeature fe = cosechaFeature.getFeature(featureBuilder);
//		this.insertFeature(fe);
//
//	}

	public void changeFeature(SimpleFeature old, CosechaItem ci) {
		outCollection.remove(old);
		outCollection.add(ci.getFeature(featureBuilder));
	}

	@Override
	public CosechaItem constructFeatureContainer(SimpleFeature harvestFeature) {
		CosechaItem ci = new CosechaItem(harvestFeature);
		ci.id=getNextID();
		
		double toMetros = config.valorMetrosPorUnidadDistanciaProperty()
				.doubleValue();

		String anchoColumn = colAncho.get();// getColumn(COLUMNA_ANCHO);
		ci.ancho = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(anchoColumn));
		ci.ancho = ci.ancho * toMetros;

		String distColumn = colDistancia.get();// getColumn(COLUMNA_DISTANCIA);//TODO
												// pasar el constructor de
												// cosechaItem a la labor
		try {
			ci.distancia = new Double(distColumn);
		} catch (Exception e) {
			Object distAttribute = harvestFeature.getAttribute(distColumn);

			ci.distancia = FeatureContainer.getDoubleFromObj(distAttribute)
					* toMetros;
		}

		ci.rumbo = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(colCurso.get()));//hay valores que tienen rumbo 0 o 270 que parecen ser errores por no tener continuidad

//		ci.pasada = FeatureContainer.getDoubleFromObj(harvestFeature
//				.getAttribute(colPasada.get()));

		String idString = FeatureContainer.getID(harvestFeature);
		ci.id =FeatureContainer.getDoubleFromObj(idString);

		double correccionRinde = correccionCosechaProperty.doubleValue();// me
																			// devuelve
																			// cero
		Double rindeDouble = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(colRendimiento.get()));

		if (config.correccionFlowToRindeProperty().getValue()) {
			// ("Mass_Flow_" *0.453592) /((("Width" *2.54/100)*
			// ("Distance"*2.54/100))/10000)
			/*
			 * convertir el rinde que es un flow en libras por segundo a kg por
			 * ha. para eso hay que usar la formula rinde = flow*[kg por
			 * libra]*[1 segundo]*[m2 por Ha]/(width*distance)
			 */
			double constantes = ProyectionConstants.METROS2_POR_HA
					* KG_POR_LIBRA/KG_POR_TN;// XXX asume un dato por segundo
			rindeDouble = rindeDouble * constantes / (ci.distancia * ci.ancho);
		}

		/*
		 * mass_flow[kg/s]=0.4535*lb/s ancho[m] velocidad[m/s] rinde[kg/ha] =
		 * 10000*mass_flow/(ancho*velocidad)
		 */
		ci.rindeTnHa = rindeDouble * (correccionRinde / 100);

		ci.velocidad = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(colVelocidad.get()));
		ci.elevacion = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(colElevacion.get()));
		ci.precioTnGrano = precioGranoProperty.doubleValue();
		ci.importeHa = ci.rindeTnHa * ci.precioTnGrano;

		return ci;
	}



	@Override
	public  CosechaItem constructFeatureContainerStandar(
			SimpleFeature harvestFeature,boolean newIDS) {
		CosechaItem ci = new CosechaItem(harvestFeature);
	
		ci.id = FeatureContainer.getDoubleFromObj(FeatureContainer.getID(harvestFeature));
		if(ci.id ==null || newIDS){// flag que me permita ignorar el id del feature y asignar uno nuevo
			ci.id= this.getNextID();
		}

		ci.distancia = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_DISTANCIA));
		ci.rumbo = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_CURSO));
		// ci.pasada
		// =FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(CosechaLabor.COLUMNA_PASADA));//no
		// se pudo leer

		ci.velocidad = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_VELOCIDAD));
		ci.ancho = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_ANCHO));
		ci.rindeTnHa = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_RENDIMIENTO));
		ci.elevacion = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_ELEVACION));
		ci.precioTnGrano = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_PRECIO));
		ci.importeHa = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_IMPORTE_HA));
		
//		ci.categoria = FeatureContainer.getDoubleFromObj(harvestFeature
//				.getAttribute(CosechaLabor.COLUMNA_CATEGORIA));

//		if(this.clasificador!=null){
//			ci.categoria = clasificador.getCategoryFor(ci.getRindeTnHa());
//		}
		return ci;
	}



	public CosechaConfig getConfiguracion() {
		return config;
	}



	public void modifyFeature(CosechaItem f) {
		modifyFeature(this.colRendimiento.get(), f.getAmount(), f.getId()
				.toString());
	}

	public void constructClasificador() {
		this.constructClasificador(config.config
		.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
				Clasificador.CLASIFICADOR_JENKINS));
		// TODO Auto-generated method stub
		
	}

	// public String getColumn(String key){
	// String column = getColumnsMap().getOrDefault(key, key);
	// // System.out.println("returning column "+column+ " for key "+key);
	// return column;
	// }

}
