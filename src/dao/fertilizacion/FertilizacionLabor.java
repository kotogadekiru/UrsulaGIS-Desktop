package dao.fertilizacion;

import java.util.List;

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
import dao.config.Fertilizante;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import dao.siembra.SiembraItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FertilizacionLabor extends Labor<FertilizacionItem> {
	public static final String COLUMNA_KG_HA = "Kg Fert/Ha";
	
	public static final String COLUMNA_PRECIO_FERT = "Precio Kg Fert";
	public static final String COLUMNA_PRECIO_PASADA = "Precio labor/Ha";	
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";

	private static final String FERTILIZANTE_DEFAULT = "FERTILIZANTE_DEFAULT";
	
	private static final String COSTO_LABOR_FERTILIZACION = "costoLaborFertilizacion";
	
	public StringProperty colKgHaProperty;
	
	public FertilizacionConfig config=null;

	public Property<Fertilizante> fertilizante=null;


	public FertilizacionLabor() {
		initConfig();
	}
	
	public FertilizacionLabor(FileDataStore store) {
		super(store);
		//this.setInStore(store);// esto configura el nombre	
		initConfig();
	}

	//XXX ver como los listeners de las propiedades me afectan el archivo de properties y 
	//el controller de la configuracion. creo que setea las variables pero nunca las graba a menos 
	//que las grabe el controller
	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		
		
		//config = new FertilizacionConfig();
		Configuracion properties = getConfigLabor().getConfigProperties();
		
		colKgHaProperty = new SimpleStringProperty(
				properties.getPropertyOrDefault(
						FertilizacionLabor.COLUMNA_KG_HA,
						FertilizacionLabor.COLUMNA_KG_HA));
		if(!availableColums.contains(colKgHaProperty.get())&&availableColums.contains(FertilizacionLabor.COLUMNA_KG_HA)){
			colKgHaProperty.setValue(FertilizacionLabor.COLUMNA_KG_HA);
		}
		colKgHaProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(FertilizacionLabor.COLUMNA_KG_HA,
					bool2.toString());
		});
		
		colAmount= new SimpleStringProperty(FertilizacionLabor.COLUMNA_KG_HA);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		/*columnas nuevas*/
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
		
		colAncho = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_ANCHO, CosechaLabor.COLUMNA_ANCHO));
		if(!availableColums.contains(colAncho.get())&&availableColums.contains(CosechaLabor.COLUMNA_ANCHO)){
			colAncho.setValue(CosechaLabor.COLUMNA_ANCHO);
		} 
		colAncho.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_ANCHO, bool2);
		});// bool2 es un string asi que no necesito convertirlo
		
		colDistancia = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_DISTANCIA, CosechaLabor.COLUMNA_DISTANCIA));
		if(!availableColums.contains(colAncho.get())&&availableColums.contains(CosechaLabor.COLUMNA_DISTANCIA)){
			colDistancia.setValue(CosechaLabor.COLUMNA_DISTANCIA);
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
		
		 /**/
		
		
		precioLaborProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						FertilizacionLabor.COLUMNA_PRECIO_PASADA, "0")));
		precioLaborProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(FertilizacionLabor.COLUMNA_PRECIO_PASADA,
					bool2.toString());
		});
		
		precioInsumoProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						FertilizacionLabor.COLUMNA_PRECIO_FERT, "0")));
		precioInsumoProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(FertilizacionLabor.COLUMNA_PRECIO_FERT,
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
		
		String fertKEY = properties.getPropertyOrDefault(
				FertilizacionLabor.FERTILIZANTE_DEFAULT, Fertilizante.FOSFATO_DIAMONICO_DAP);
		 fertilizante = new SimpleObjectProperty<Fertilizante>(Fertilizante.fertilizantes.get(fertKEY));//values().iterator().next());
	}
		
//	@Override
//	public SimpleFeatureType getType() {
//		SimpleFeatureType type = null;
//		try {
//			/*
//			 * geom tiene que ser Point, Line o Polygon. no puede ser Geometry
//			 * porque podria ser cualquiera y solo permite un tipo por archivo
//			 * los nombre de las columnas no pueden ser de mas de 10 char
//			 */
//			
////			featureBuilder.addAll(new Object[]{super.getGeometry(),
////					getCantFertHa(),
////						getPrecioFert(),
////						getPrecioPasada(),
////						getImporteHa(),
////						getCategoria()});
//
//			type = DataUtilities.createType("Fertilizacion", "the_geom:MultiPolygon:srid=4326,"//"*geom:Polygon,"the_geom
//					+ FertilizacionLabor.COLUMNA_KG_HA + ":Double,"
//					+ FertilizacionLabor.COLUMNA_PRECIO_FERT + ":Double,"
//					+ FertilizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
//					+ FertilizacionLabor.COLUMNA_IMPORTE_HA + ":Double,"
//					+ FertilizacionLabor.COLUMNA_CATEGORIA + ":Integer");
//		} catch (SchemaException e) {
//
//			e.printStackTrace();
//		}
//		return type;
//	}
	
	@Override
	public String getTypeDescriptors() {
		/*
		 * 	getCantFertHa(),
				getPrecioFert(),
				getPrecioPasada(),
				getImporteHa()
		 */
		String type = FertilizacionLabor.COLUMNA_KG_HA + ":Double,"
				+ FertilizacionLabor.COLUMNA_PRECIO_FERT + ":Double,"
				+ FertilizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ FertilizacionLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public FertilizacionItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
//		FertilizacionItem fi = new FertilizacionItem(next);
//		fi.id=getNextID();
		FertilizacionItem fi = new FertilizacionItem(next);
		super.constructFeatureContainerStandar(fi,next,newIDS);

		 
	
	
	fi.setCantFertHa( FeatureContainer.getDoubleFromObj(next
			.getAttribute(COLUMNA_KG_HA)));
//	Object cantObj = harvestFeature.getAttribute(getColumn(KG_HA_COLUMN));
//	ci.cantFertHa = super.getDoubleFromObj(cantObj);


	fi.setPrecioFert(this.precioInsumoProperty.get());
	fi.setPrecioPasada(this.precioLaborProperty.get());	
	//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get
	
		return fi;
	}


	@Override
	public FertilizacionItem constructFeatureContainer(SimpleFeature next) {
//		FertilizacionItem fi = new FertilizacionItem(next);
//		fi.id=getNextID();

		FertilizacionItem fi = new FertilizacionItem(next);
		super.constructFeatureContainer(fi,next);

		
	fi.setCantFertHa( FeatureContainer.getDoubleFromObj(next
			.getAttribute(colKgHaProperty.get())));
//	Object cantObj = harvestFeature.getAttribute(getColumn(KG_HA_COLUMN));
//	ci.cantFertHa = super.getDoubleFromObj(cantObj);


	fi.setPrecioFert(this.precioInsumoProperty.get());
	fi.setPrecioPasada(this.precioLaborProperty.get());	
	//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get
	
		return fi;
	}
	
//	public void constructClasificador() {
//		super.constructClasificador(config.config
//		.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
//				Clasificador.CLASIFICADOR_JENKINS));
//	}

//	@Override
//	public void constructClasificador() {
//		if (Clasificador.CLASIFICADOR_JENKINS.equalsIgnoreCase(config.config
//				.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
//						Clasificador.CLASIFICADOR_JENKINS))) {
//			// try {
//			// this.clasificador.constructJenksClasifier(this.outStore
//			// .getFeatureSource().getFeatures(),
//			// CosechaLabor.COLUMNA_RENDIMIENTO);
//
//			// } catch (IOException e) {
//			// // TODO Auto-generated catch block
//			// e.printStackTrace();
//			// }
//
//			this.clasificador.constructJenksClasifier(this.outCollection,
//					this.colAmount.get());
//		} else {
//			// if(clasifier == null ){
//			System.out
//					.println("no hay jenks Classifier falling back to histograma");
//			List<FertilizacionItem> items = new ArrayList<FertilizacionItem>();
//			
//			SimpleFeatureIterator ocReader = this.outCollection.features();
//			while (ocReader.hasNext()) {
//				items.add(constructFeatureContainerStandar(ocReader.next(),false));
//			}
//			ocReader.close();
//			this.clasificador.constructHistogram(items);
//			
//		}
//	}

//	public FertilizacionConfig getConfiguracion() {
//		return config;
//	}

	@Override
	protected DoubleProperty initPrecioLaborHaProperty() {
		return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.config);
	}

	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new FertilizacionConfig();
		}
		return config;
	}



}
