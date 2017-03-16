package dao.pulverizacion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.FileDataStore;
import org.opengis.feature.simple.SimpleFeature;

import dao.Clasificador;
import dao.Labor;
import dao.LaborConfig;
import dao.LaborItem;
import dao.config.Agroquimico;
import dao.config.Configuracion;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PulverizacionLabor extends Labor<PulverizacionItem> {
	private static final String COLUMNA_DOSIS = "Dosis";
	private static final String COLUMNA_PASADAS = "CantPasada";
	public static final String COLUMNA_PRECIO_PASADA = "CostoLab";	
	public static final String COLUMNA_IMPORTE_HA = "Importe_ha";

	private static final String COSTO_LABOR_PULVERIZACION = "costoLaborPulverizacion";
	private static final String AGROQUIMICO_DEFAULT = "AGROQUIMICO_DEFAULT_KEY";
	private static final String PRECIO_INSUMO_KEY = "PRECIO_INSUMO";
	
	public StringProperty colDosisProperty;
	public StringProperty colCantPasadasProperty;
	
	private static Map<String, String> columnsMap= new HashMap<String, String>();
	
	//public PulverizacionConfig config=null;
	public Property<Agroquimico> agroquimico=null;

	public PulverizacionLabor() {
		initConfig();
	}
	
	public PulverizacionLabor(FileDataStore store) {
		this.setInStore(store);// esto configura el nombre	
		initConfig();
	}

	//XXX ver como los listeners de las propiedades me afectan el archivo de properties y 
	//el controller de la configuracion. creo que setea las variables pero nunca las graba a menos 
	//que las grabe el controller
	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		
		
		//config = new PulverizacionConfig();
		Configuracion properties = getConfigLabor().getConfigProperties();
		
		colDosisProperty = new SimpleStringProperty(
				properties.getPropertyOrDefault(
						PulverizacionLabor.COLUMNA_DOSIS,
						PulverizacionLabor.COLUMNA_DOSIS));
		if(!availableColums.contains(colDosisProperty.get())&&availableColums.contains(PulverizacionLabor.COLUMNA_DOSIS)){
			colDosisProperty.setValue(PulverizacionLabor.COLUMNA_DOSIS);
		}
		colDosisProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.COLUMNA_DOSIS,
					bool2.toString());
		});
		
		colAmount= new SimpleStringProperty(PulverizacionLabor.COLUMNA_DOSIS);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		
		colCantPasadasProperty = new SimpleStringProperty(
				properties.getPropertyOrDefault(
						PulverizacionLabor.COLUMNA_PASADAS,
						PulverizacionLabor.COLUMNA_PASADAS));
		if(!availableColums.contains(colCantPasadasProperty.get())&&availableColums.contains(PulverizacionLabor.COLUMNA_PASADAS)){
			colCantPasadasProperty.setValue(PulverizacionLabor.COLUMNA_PASADAS);
		}
		colCantPasadasProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.COLUMNA_PASADAS,
					bool2.toString());
		});
		
		/*columnas nuevas*/
		colElevacion = new SimpleStringProperty(
				properties.getPropertyOrDefault(Labor.COLUMNA_ELEVACION,
						Labor.COLUMNA_ELEVACION));
		if(!availableColums.contains(colElevacion.get())&&availableColums.contains(Labor.COLUMNA_ELEVACION)){
			colElevacion.setValue(Labor.COLUMNA_ELEVACION);
		}
		colElevacion.addListener((obs, bool1, bool2) -> {
			properties.setProperty(Labor.COLUMNA_ELEVACION,
					bool2.toString());
		});
		
		colAncho = new SimpleStringProperty(properties.getPropertyOrDefault(
				Labor.COLUMNA_ANCHO, Labor.COLUMNA_ANCHO));
		if(!availableColums.contains(colAncho.get())&&availableColums.contains(Labor.COLUMNA_ANCHO)){
			colAncho.setValue(Labor.COLUMNA_ANCHO);
		} 
		colAncho.addListener((obs, bool1, bool2) -> {
			properties.setProperty(Labor.COLUMNA_ANCHO, bool2);
		});// bool2 es un string asi que no necesito convertirlo
		
		colDistancia = new SimpleStringProperty(properties.getPropertyOrDefault(
				Labor.COLUMNA_DISTANCIA, Labor.COLUMNA_DISTANCIA));
		if(!availableColums.contains(colAncho.get())&&availableColums.contains(Labor.COLUMNA_DISTANCIA)){
			colDistancia.setValue(Labor.COLUMNA_DISTANCIA);
		} 
		colAncho.addListener((obs, bool1, bool2) -> {
			properties.setProperty(Labor.COLUMNA_ANCHO, bool2);
		});// bool2 es un string asi que no necesito convertirlo
		
		colCurso = new SimpleStringProperty(properties.getPropertyOrDefault(
				Labor.COLUMNA_CURSO, Labor.COLUMNA_CURSO));
		if(!availableColums.contains(colCurso.get())&&availableColums.contains(Labor.COLUMNA_CURSO)){
			colCurso.setValue(Labor.COLUMNA_CURSO);
		}
		colCurso.addListener((obs, bool1, bool2) -> {
			properties.setProperty(Labor.COLUMNA_CURSO, bool2.toString());
		});

		colDistancia = new SimpleStringProperty(
				properties.getPropertyOrDefault(Labor.COLUMNA_DISTANCIA,
						Labor.COLUMNA_DISTANCIA));
		if(!availableColums.contains(colDistancia.get())&&availableColums.contains(Labor.COLUMNA_DISTANCIA)){
			colDistancia.setValue(Labor.COLUMNA_DISTANCIA);
		}
		colDistancia.addListener((obs, bool1, bool2) -> {
			properties.setProperty(Labor.COLUMNA_DISTANCIA,
					bool2.toString());
		});
		
		 /**/
		
		
		precioLaborProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						PulverizacionLabor.COLUMNA_PRECIO_PASADA, "0")));
		precioLaborProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.COLUMNA_PRECIO_PASADA,
					bool2.toString());
		});
		
		precioInsumoProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						PulverizacionLabor.PRECIO_INSUMO_KEY, "0.0")));
		precioInsumoProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.PRECIO_INSUMO_KEY,
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
				PulverizacionLabor.AGROQUIMICO_DEFAULT, Agroquimico.agroquimicos.values().iterator().next().getNombre());
		 agroquimico = new SimpleObjectProperty<Agroquimico>(Agroquimico.agroquimicos.get(fertKEY));//values().iterator().next());

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
////			getCostoPaquete(),
////			getCantPasadasHa(),
////			getCostoLaborHa(),
////			getImporteHa(),
////			getCategoria()});
//
//			type = DataUtilities.createType("Pulverizacion", "the_geom:MultiPolygon:srid=4326,"//"*geom:Polygon,"the_geom
//					+ PulverizacionLabor.COLUMNA_COSTO_PAQ + ":Double,"
//					+ PulverizacionLabor.COLUMNA_PASADAS + ":Double,"
//					+ PulverizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
//					+ PulverizacionLabor.COLUMNA_IMPORTE_HA + ":Double,"
//					+ PulverizacionLabor.COLUMNA_CATEGORIA + ":Integer");
//		} catch (SchemaException e) {
//
//			e.printStackTrace();
//		}
//		return type;
//	}
	
	@Override
	public String getTypeDescriptors() {
		/*
		 *getCostoPaquete(),
				getCantPasadasHa(),
				getCostoLaborHa(),
				getImporteHa()
		 */
		String type = PulverizacionLabor.COLUMNA_DOSIS + ":Double,"
				+ PulverizacionLabor.COLUMNA_PASADAS + ":Double,"
				+ PulverizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ PulverizacionLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public PulverizacionItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		PulverizacionItem pItem = new PulverizacionItem(next);
		super.constructFeatureContainerStandar(pItem,next,newIDS);
			
	pItem.setPrecioInsumo(this.precioInsumoProperty.get());
	pItem.setDosis( LaborItem.getDoubleFromObj(next
			.getAttribute(PulverizacionLabor.COLUMNA_DOSIS)));

	pItem.setCantPasadasHa(LaborItem.getDoubleFromObj(next
			.getAttribute(PulverizacionLabor.COLUMNA_PASADAS)));
	
	pItem.setCostoLaborHa(LaborItem.getDoubleFromObj(next
			.getAttribute(PulverizacionLabor.COLUMNA_PRECIO_PASADA)));	
	//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get
	
		return pItem;
	}


	@Override
	public PulverizacionItem constructFeatureContainer(SimpleFeature next) {
		
		PulverizacionItem fi = new PulverizacionItem(next);
		super.constructFeatureContainer(fi,next);

	fi.setDosis( LaborItem.getDoubleFromObj(next
			.getAttribute(colDosisProperty.get())));
	fi.setPrecioInsumo(this.precioInsumoProperty.get());
	
	fi.setCantPasadasHa(LaborItem.getDoubleFromObj(next
			.getAttribute(colCantPasadasProperty.get())));
	fi.setCostoLaborHa(this.precioLaborProperty.get());	
	//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get
	
		return fi;
	}
	
	public void constructClasificador() {
		super.constructClasificador(config.getConfigProperties()
		.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
				Clasificador.CLASIFICADOR_JENKINS));
	}

	public PulverizacionConfig getConfiguracion() {
		return (PulverizacionConfig) config;
	}

	@Override
	protected DoubleProperty initPrecioLaborHaProperty() {
		return initDoubleProperty(PulverizacionLabor.COSTO_LABOR_PULVERIZACION,"0",config.getConfigProperties());
	}

	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new PulverizacionConfig();
		}
		return config;
	}

		public void setColumnsMap(Map<String, String> columns) {
		columnsMap=columns;
		colDosisProperty.setValue(columnsMap.get(PulverizacionItem.COLUMNA_COSTO_PAQUETE));
		colCantPasadasProperty.setValue(columnsMap.get(PulverizacionItem.COLUMNA_CANT_PASADAS));

	}
}
