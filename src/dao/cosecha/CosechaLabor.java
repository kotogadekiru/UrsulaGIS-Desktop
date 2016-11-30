package dao.cosecha;

import java.util.List;

import javafx.beans.property.DoubleProperty;
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

import dao.Clasificador;
import dao.FeatureContainer;
import dao.Labor;
import dao.LaborConfig;
import dao.config.Configuracion;
import dao.config.Cultivo;
import utils.ProyectionConstants;

public class CosechaLabor extends Labor<CosechaItem> {
	private static final int KG_POR_TN = 1000;
	private static final double KG_POR_LIBRA = 0.453592;
	// private static final int KG_POR_TN = 1000;
	
	public static final String COLUMNA_VELOCIDAD = "Velocidad";
	public static final String COLUMNA_RENDIMIENTO = "Rendimient";
	public static final String COLUMNA_DESVIO_REND = "DesvRendim";
	
	//public static final String COLUMNA_PASADA = "Num__de_pa";

	public static final String COLUMNA_PRECIO = "precio_gra";
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";


	private static final String PRECIO_GRANO = "precioGrano";
	private static final String CORRECCION_COSECHA = "CORRECCION_COSECHA";
	private static final String COSTO_COSECHA_TN = "COSTO_COSECHA_TN";
	private static final String COSTO_COSECHA_HA = "COSTO_COSECHA_HA";

	private static final String MAX_RINDE_KEY = "MAX_RINDE";
	private static final String MIN_RINDE_KEY = "MIN_RINDE";

	private static final String PRODUCTO_DEFAULT = "CultivoDefault";

	public CosechaConfig config = null;

	public StringProperty colVelocidad= null;
	public StringProperty colRendimiento= null;
	
	public SimpleDoubleProperty precioGranoProperty= null;
	public SimpleDoubleProperty correccionCosechaProperty= null;
	public SimpleDoubleProperty maxRindeProperty= null;
	public SimpleDoubleProperty minRindeProperty= null;
	public SimpleDoubleProperty costoCosechaTnProperty= null;


	public Property<Cultivo> producto=null;


	//Double nextCosechaID =new Double(0);//XXX este id no es global sino que depende de la labor
	/**
	 * constructor que sirve para crear una cosecha artificial cuando no tiene
	 * un datastore que la represente
	 */

	public CosechaLabor() {
		super();
		initConfig();
	}
	
	public CosechaLabor(FileDataStore store) {
		super(store);
		initConfig();
	}
	
	//XXX ver como los listeners de las propiedades me afectan el archivo de properties y 
	//el controller de la configuracion. creo que setea las variables pero nunca las graba a menos 
	//que las grabe el controller
	protected void initConfig() {
		System.out.println("inicioando la configuracion de CosechLabor");
		List<String> availableColums = this.getAvailableColumns();		

		Configuracion properties = getConfigLabor().getConfigProperties();

		colVelocidad = new SimpleStringProperty(
				properties.getPropertyOrDefault(CosechaLabor.COLUMNA_VELOCIDAD,
						CosechaLabor.COLUMNA_VELOCIDAD));
		if(!availableColums.contains(colVelocidad.get())&&availableColums.contains(CosechaLabor.COLUMNA_VELOCIDAD)){
			colVelocidad.setValue(CosechaLabor.COLUMNA_VELOCIDAD);
		}
		colVelocidad.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_VELOCIDAD,
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

		correccionCosechaProperty = initDoubleProperty(CosechaLabor.CORRECCION_COSECHA, "100", properties);
		minRindeProperty = initDoubleProperty(CosechaLabor.MIN_RINDE_KEY, "0", properties);
		maxRindeProperty = initDoubleProperty(CosechaLabor.MAX_RINDE_KEY, "0", properties);
		precioGranoProperty = initDoubleProperty(CosechaLabor.PRECIO_GRANO, "0", properties);
		costoCosechaTnProperty = initDoubleProperty(CosechaLabor.COSTO_COSECHA_TN, "0", properties);
		
		String productoKEY = properties.getPropertyOrDefault(
				CosechaLabor.PRODUCTO_DEFAULT, "Maiz");
		producto = new SimpleObjectProperty<Cultivo>(Cultivo.cultivos.get(productoKEY));//values().iterator().next());
	}
	
	@Override
	protected DoubleProperty initPrecioLaborHaProperty(){
		return initDoubleProperty(CosechaLabor.COSTO_COSECHA_HA,"0",config.config);
	} 



	// public void setPrecioGrano(Double precioGrano) {
	// this.precioLabor=precioGrano;//precio es el costo de la labor por ha
	// }
	// public void setCorreccionRinde(Double correccionRinde1) {
	// //System.out.println("nuevo correccion rinde es "+correccionRinde1);
	// correccionRinde = correccionRinde1;
	// }

//	public static SimpleFeatureType getFeatureType() {
//		SimpleFeatureType type = null;
//		try {
//			/*
//			 * geom tiene que ser Point, Line o Polygon. no puede ser Geometry
//			 * porque podria ser cualquiera y solo permite un tipo por archivo
//			 * los nombre de las columnas no pueden ser de mas de 10 char
//			 */
//
//
//
//			type = DataUtilities.createType("Cosecha", "the_geom:MultiPolygon:srid=4326,"//"*geom:Polygon,"the_geom
//					+ CosechaLabor.COLUMNA_DISTANCIA + ":Double,"
//					+ CosechaLabor.COLUMNA_CURSO + ":Double,"
//					+ CosechaLabor.COLUMNA_ANCHO + ":Double,"
//					+ CosechaLabor.COLUMNA_RENDIMIENTO + ":Double,"
//					+ CosechaLabor.COLUMNA_VELOCIDAD + ":Double,"
//					+ CosechaLabor.COLUMNA_ELEVACION + ":Double,"
//					+ CosechaLabor.COLUMNA_PRECIO + ":Double,"
//					+ CosechaLabor.COLUMNA_IMPORTE_HA + ":Double,"
//					+ CosechaLabor.COLUMNA_CATEGORIA + ":Integer");
//		} catch (SchemaException e) {
//
//			e.printStackTrace();
//		}
//		return type;
//	}
	@Override
	public String getTypeDescriptors() {
		String type = CosechaLabor.COLUMNA_RENDIMIENTO + ":Double,"	
				+CosechaLabor.COLUMNA_DESVIO_REND + ":Double,"	
				+CosechaLabor.COLUMNA_VELOCIDAD + ":Double,"	
					+ CosechaLabor.COLUMNA_PRECIO + ":Double,"
					+CosechaLabor.COLUMNA_IMPORTE_HA+":Double";
		return type;
	}
	

//	public static SimpleFeatureType getPointsFeatureType() {
//		SimpleFeatureType type = null;
//		try {
//			/*
//			 * geom tiene que ser Point, Line o Polygon. no puede ser Geometry
//			 * porque podria ser cualquiera y solo permite un tipo por archivo
//			 * los nombre de las columnas no pueden ser de mas de 10 char
//			 */
//
//			type = DataUtilities.createType("Cosecha", "the_geom:Point:srid=4326,"//"*geom:Polygon,"the_geom
//					+ CosechaLabor.COLUMNA_DISTANCIA + ":Double,"
//					+ CosechaLabor.COLUMNA_CURSO + ":Double,"
//					+ CosechaLabor.COLUMNA_ANCHO + ":Double,"
//					+ CosechaLabor.COLUMNA_RENDIMIENTO + ":Double,"
//					+ CosechaLabor.COLUMNA_VELOCIDAD + ":Double,"
//					+ CosechaLabor.COLUMNA_ELEVACION + ":Double,"
//					+ CosechaLabor.COLUMNA_PRECIO + ":Double,"
//					+ CosechaLabor.COLUMNA_IMPORTE_HA + ":Double,"
//					+ CosechaLabor.COLUMNA_CATEGORIA + ":Integer");
//		} catch (SchemaException e) {
//
//			e.printStackTrace();
//		}
//		return type;
//	}

//	@Override
//	public SimpleFeatureType getType() {
//		return CosechaLabor.getFeatureType();
//	}

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
		super.constructFeatureContainer(ci,harvestFeature);
		double correccionRinde = correccionCosechaProperty.doubleValue();// me
		Double rindeDouble = FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colRendimiento.get()));

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
			rindeDouble = rindeDouble * constantes / (ci.getDistancia() * ci.getAncho());
		}

		/*
		 * mass_flow[kg/s]=0.4535*lb/s ancho[m] velocidad[m/s] rinde[kg/ha] =
		 * 10000*mass_flow/(ancho*velocidad)
		 */
		ci.rindeTnHa = rindeDouble * (correccionRinde / 100);

		if(!colVelocidad.get().equals(Labor.NONE_SELECTED)){
		ci.velocidad = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(colVelocidad.get()));
		}
//		ci.elevacion = FeatureContainer.getDoubleFromObj(harvestFeature
//				.getAttribute(colElevacion.get()));
		ci.precioTnGrano = precioGranoProperty.doubleValue();
		ci.importeHa = ci.rindeTnHa * ci.precioTnGrano;

		return ci;
	}

	@Override
	public  CosechaItem constructFeatureContainerStandar(
			SimpleFeature harvestFeature,boolean newIDS) {
		CosechaItem ci = new CosechaItem(harvestFeature);
		super.constructFeatureContainerStandar(ci,harvestFeature,newIDS);

//		ci.id = FeatureContainer.getDoubleFromObj(FeatureContainer.getID(harvestFeature));
//		if(ci.id ==null || newIDS){// flag que me permita ignorar el id del feature y asignar uno nuevo
//			ci.id= this.getNextID();
//		}
//
//		ci.distancia = FeatureContainer.getDoubleFromObj(harvestFeature
//				.getAttribute(CosechaLabor.COLUMNA_DISTANCIA));
//		ci.rumbo = FeatureContainer.getDoubleFromObj(harvestFeature
//				.getAttribute(CosechaLabor.COLUMNA_CURSO));
		// ci.pasada
		// =FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(CosechaLabor.COLUMNA_PASADA));//no
		// se pudo leer

		ci.velocidad = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_VELOCIDAD));
//		ci.ancho = FeatureContainer.getDoubleFromObj(harvestFeature
//				.getAttribute(CosechaLabor.COLUMNA_ANCHO));
		ci.rindeTnHa = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_RENDIMIENTO));
		ci.desvioRinde = FeatureContainer.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.COLUMNA_DESVIO_REND));
	
//		if(this.clasificador!=null && clasificador.isInitialized()){
//		Integer categoria = this.clasificador.getCategoryFor(ci.rindeTnHa);
//		if(categoria !=null)		ci.setCategoria(categoria);
//		}	


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

	public void constructClasificador() {
		this.constructClasificador(config.config
				.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
						Clasificador.CLASIFICADOR_JENKINS));

	}

	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new CosechaConfig();
		}
		return config;
	}


}
