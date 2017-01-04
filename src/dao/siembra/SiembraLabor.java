package dao.siembra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import dao.config.Semilla;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import utils.ProyectionConstants;

public class SiembraLabor extends Labor<SiembraItem> {
	private static final String SEMILLAS_POR_BOLSA_DEFAULT = "80000";
	private static final String ENTRE_SURCO_DEFAULT = "0.525";
	private static final String SEMILLAS_POR_BOLSA_KEY = "SEMILLAS_POR_BOLSA";
	private static final String ENTRE_SURCO_KEY = "ENTRE_SURCO";
	private static Map<String, String> columnsMap= new HashMap<String, String>();
	//esta columna es la que viene con los mapas de siembra
	private static final String COLUMNA_SEMILLAS_METRO = "Semillas/m";
	private static final String COLUMNA_ELEVACION = "Elevacion";
	
	//esta columna es la que voy a exportar
	public static final String COLUMNA_BOLSAS_HA = "BolsasHa";//=semillasMetro*(ProyectionConstants.METROS2_POR_HA/entreSurco)/semillasPorBolsa;
	public static final String COLUMNA_PRECIO_BOLSA = "PrecioBolsa";
	public static final String COLUMNA_PRECIO_PASADA = "CostoLabor";	
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";
	
	private static final String PRECIO_SEMILLA = "precioSemilla";
	private static final String COSTO_LABOR_SIEMBRA = "costoLaborSiembra";
	private static final String SEMILLA_DEFAULT = "SEMILLA_DEFAULT";

	private static SimpleDoubleProperty entreSurco =null; //new Double(Configuracion.getInstance().getPropertyOrDefault(ENTRE_SURCO_KEY, ENTRE_SURCO_DEFAULT));
	private static SimpleDoubleProperty semillasPorBolsa=null;// new Double(Configuracion.getInstance().getPropertyOrDefault(SEMILLAS_POR_BOLSA_KEY, SEMILLAS_POR_BOLSA_DEFAULT));


	public StringProperty colSemillasMetroProperty;

	//public SiembraConfig config=null;
	public Property<Semilla> producto=null;


	public SiembraLabor() {
		initConfig();
	}

	public SiembraLabor(FileDataStore store) {
		this.setInStore(store);// esto configura el nombre	
		initConfig();
	}


	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		

		//config = new SiembraConfig();
		Configuracion properties = getConfigLabor().getConfigProperties();

		colSemillasMetroProperty = new SimpleStringProperty(
				properties.getPropertyOrDefault(
						SiembraLabor.COLUMNA_SEMILLAS_METRO,
						SiembraLabor.COLUMNA_SEMILLAS_METRO));
		if(!availableColums.contains(colSemillasMetroProperty.get())&&availableColums.contains(SiembraLabor.COLUMNA_SEMILLAS_METRO)){
			colSemillasMetroProperty.setValue(SiembraLabor.COLUMNA_SEMILLAS_METRO);
		}
		colSemillasMetroProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.COLUMNA_SEMILLAS_METRO,
					bool2.toString());
		});

		colAmount= new SimpleStringProperty(SiembraLabor.COLUMNA_SEMILLAS_METRO);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		/*columnas nuevas*/
		colElevacion = new SimpleStringProperty(
				properties.getPropertyOrDefault(SiembraLabor.COLUMNA_ELEVACION,
						SiembraLabor.COLUMNA_ELEVACION));
		if(!availableColums.contains(colElevacion.get())&&availableColums.contains(SiembraLabor.COLUMNA_ELEVACION)){
			colElevacion.setValue(SiembraLabor.COLUMNA_ELEVACION);
		}
		colElevacion.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.COLUMNA_ELEVACION,
					bool2.toString());
		});

//		colAncho = new SimpleStringProperty(properties.getPropertyOrDefault(
//				SiembraLabor.COLUMNA_ANCHO, SiembraLabor.COLUMNA_ANCHO));
//		if(!availableColums.contains(colAncho.get())&&availableColums.contains(SiembraLabor.COLUMNA_ANCHO)){
//			colAncho.setValue(SiembraLabor.COLUMNA_ANCHO);
//		} 
//		colAncho.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(SiembraLabor.COLUMNA_ANCHO, bool2);
//		});// bool2 es un string asi que no necesito convertirlo
//
//		colDistancia = new SimpleStringProperty(properties.getPropertyOrDefault(
//				SiembraLabor.COLUMNA_DISTANCIA, SiembraLabor.COLUMNA_DISTANCIA));
//		if(!availableColums.contains(colAncho.get())&&availableColums.contains(SiembraLabor.COLUMNA_DISTANCIA)){
//			colDistancia.setValue(SiembraLabor.COLUMNA_DISTANCIA);
//		} 
//		colAncho.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(SiembraLabor.COLUMNA_ANCHO, bool2);
//		});// bool2 es un string asi que no necesito convertirlo
//
//		colCurso = new SimpleStringProperty(properties.getPropertyOrDefault(
//				SiembraLabor.COLUMNA_CURSO, SiembraLabor.COLUMNA_CURSO));
//		if(!availableColums.contains(colCurso.get())&&availableColums.contains(SiembraLabor.COLUMNA_CURSO)){
//			colCurso.setValue(SiembraLabor.COLUMNA_CURSO);
//		}
//		colCurso.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(SiembraLabor.COLUMNA_CURSO, bool2.toString());
//		});
//
//		colDistancia = new SimpleStringProperty(
//				properties.getPropertyOrDefault(SiembraLabor.COLUMNA_DISTANCIA,
//						SiembraLabor.COLUMNA_DISTANCIA));
//		if(!availableColums.contains(colDistancia.get())&&availableColums.contains(SiembraLabor.COLUMNA_DISTANCIA)){
//			colDistancia.setValue(SiembraLabor.COLUMNA_DISTANCIA);
//		}
//		colDistancia.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(SiembraLabor.COLUMNA_DISTANCIA,
//					bool2.toString());
//		});

		/**/
		
		entreSurco = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						SiembraLabor.ENTRE_SURCO_KEY, ENTRE_SURCO_DEFAULT))
				);		
		entreSurco.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.ENTRE_SURCO_KEY,
					bool2.toString());
		});
		
		semillasPorBolsa = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						SiembraLabor.SEMILLAS_POR_BOLSA_KEY, SEMILLAS_POR_BOLSA_DEFAULT))
				);		
		semillasPorBolsa.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.SEMILLAS_POR_BOLSA_KEY,
					bool2.toString());
		});

		precioLaborProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						SiembraLabor.COLUMNA_PRECIO_PASADA, "0")));
		precioLaborProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.COLUMNA_PRECIO_PASADA,
					bool2.toString());
		});

		precioInsumoProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						SiembraLabor.COLUMNA_PRECIO_BOLSA, "0")));
		precioInsumoProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.COLUMNA_PRECIO_BOLSA,
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
				SiembraLabor.SEMILLA_DEFAULT, "Semilla de Soja");
		producto = new SimpleObjectProperty<Semilla>(Semilla.semillas.get(fertKEY));//values().iterator().next());
	}

	@Override
	public String getTypeDescriptors() {
		/*
		 * getBolsasHa(),
				getPrecioBolsa(),
				getPrecioPasada(),
				getImporteHa()
		 */
		String type = SiembraLabor.COLUMNA_BOLSAS_HA + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_BOLSA + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ SiembraLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public SiembraItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		
		SiembraItem siembraItem = new SiembraItem(next);
		super.constructFeatureContainerStandar(siembraItem,next,newIDS);


		Double bolsasHa = FeatureContainer.getDoubleFromObj(next
				.getAttribute(COLUMNA_BOLSAS_HA));
	//	bolsasHa = bolsasHa*(ProyectionConstants.METROS2_POR_HA/ENTRE_SURCO)/SEMILLAS_POR_BOLSA;
		siembraItem.setBolsasHa(bolsasHa);

		siembraItem.setPrecioBolsa(this.precioInsumoProperty.get());
		siembraItem.setPrecioPasada(this.precioLaborProperty.get());	
		//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get

		return siembraItem;
	}


	@Override
	public SiembraItem constructFeatureContainer(SimpleFeature next) {
//		SiembraItem si = new SiembraItem(next);
//		si.id=getNextID();
		SiembraItem si = new SiembraItem(next);
		super.constructFeatureContainer(si,next);
		

		Double bolsasHa = FeatureContainer.getDoubleFromObj(next
				.getAttribute(colSemillasMetroProperty.get()));
		Double semillasMetro = bolsasHa*(ProyectionConstants.METROS2_POR_HA/entreSurco.get())/semillasPorBolsa.get();

		si.setBolsasHa(semillasMetro);
		//	Object cantObj = harvestFeature.getAttribute(getColumn(KG_HA_COLUMN));
		//	ci.cantFertHa = super.getDoubleFromObj(cantObj);


		si.setPrecioBolsa(this.precioInsumoProperty.get());
		si.setPrecioPasada(this.precioLaborProperty.get());	
		//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get

		return si;
	}

	public void constructClasificador() {
		super.constructClasificador(config.getConfigProperties()
				.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
						Clasificador.CLASIFICADOR_JENKINS));
	}

	public SiembraConfig getConfiguracion() {
		return (SiembraConfig) config;
	}

	public void setColumnsMap(Map<String, String> columns) {
		columnsMap=columns;
		colSemillasMetroProperty.setValue(columnsMap.get(SiembraLabor.COLUMNA_SEMILLAS_METRO));

	}
	
	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_SEMILLAS_METRO);		
		return requiredColumns;
	}

	@Override
	protected DoubleProperty initPrecioLaborHaProperty() {
		return initDoubleProperty(SiembraLabor.COSTO_LABOR_SIEMBRA,"0",config.getConfigProperties());
	}

	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new SiembraConfig();
		}
		return config;
	}

	//	public static void setColumnsMap(Map<String, String> columns) {
	//	columnsMap.clear();
	//	columnsMap.putAll(columns);
	//	
	//	columns.forEach(new BiConsumer<String, String>(){
	//		@Override
	//		public void accept(String key, String value) {
	//			Configuracion.getInstance().setProperty(key, value);				
	//		}
	//		
	//	});
	//}
}
