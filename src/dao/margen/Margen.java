package dao.margen;

import java.util.List;

import org.geotools.data.FileDataStore;
import org.opengis.feature.simple.SimpleFeature;

import dao.FeatureContainer;
import dao.Labor;
import dao.LaborConfig;
import dao.config.Configuracion;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;

public class Margen extends Labor<MargenItem> {
	private static final String COLUMNA_RENTABILIDAD = "RENTA";
	private static final String COLUMNA_MARGEN = "MARGEN";
	private static final String COLUMNA_COSTO = "COSTO_T";
	private static final String COLUMNA_INGRESO = "INGRESO";
	private static final String COLUMNA_COSTO_FERT = "COSTO_FERT";
	private static final String COLUMNA_COSTO_PULV = "COSTO_PULV";
	private static final String COLUMNA_COSTO_SIEMBR = "COSTO_SIEM";
	private static final String COLUMNA_COSTO_FIJO = "COSTO_FIJO";
	
	public StringProperty colRentabilidad= null;
	public StringProperty colMargen= null;
	public StringProperty colCostoTotal= null;
	public StringProperty colIngreso= null;
	public StringProperty colCostoFertilizacion= null;
	public StringProperty colCostoPulverizacion= null;
	public StringProperty colCostoSiembra= null;
	public StringProperty colCostoFijo= null;

	public DoubleProperty costoFijoHaProperty = null;
	
	private LaborConfig config=null;
	
	public Margen() {
		super();
		initConfig();
	}

	public Margen(FileDataStore store) {
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

		this.colRentabilidad = initStringProperty(Margen.COLUMNA_RENTABILIDAD, properties, availableColums);
		this.colMargen = initStringProperty(Margen.COLUMNA_MARGEN, properties, availableColums);
		this.colIngreso = initStringProperty(Margen.COLUMNA_INGRESO, properties, availableColums);
		this.colCostoFertilizacion = initStringProperty(Margen.COLUMNA_COSTO_FERT, properties, availableColums);
		this.colCostoPulverizacion = initStringProperty(Margen.COLUMNA_COSTO_PULV, properties, availableColums);
		this.colCostoSiembra = initStringProperty(Margen.COLUMNA_COSTO_SIEMBR, properties, availableColums);
		this.colCostoTotal = initStringProperty(Margen.COLUMNA_COSTO, properties, availableColums);
		this.colCostoFijo = initStringProperty(Margen.COLUMNA_COSTO_FIJO, properties, availableColums);
		
		this.costoFijoHaProperty = initDoubleProperty(Margen.COLUMNA_COSTO_FIJO, "0", properties);
	}

	@Override
	public MargenItem constructFeatureContainerStandar(SimpleFeature harvestFeature, boolean newIDS) {
		MargenItem ci = new MargenItem(harvestFeature);
		super.constructFeatureContainerStandar(ci,harvestFeature,newIDS);
/*
		getRentabilidadHa(),
		getMargenPorHa(),
		getCostoPorHa(),
		getIngresoHa(),
		getImporteFertHa(),		
		getImportePulvHa(),
		getImporteSiembraHa()		 
*/
		//rentabilidad
		ci.setMargenPorHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colMargen.get())));
		ci.setCostoFijoPorHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoFijo.get())));
		ci.setImporteFertHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoFertilizacion.get())));
		ci.setImporteCosechaHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colIngreso.get())));
		ci.setImportePulvHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoPulverizacion.get())));
		ci.setImporteSiembraHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoSiembra.get())));
		return ci;
	}

	@Override
	public MargenItem constructFeatureContainer(SimpleFeature harvestFeature) {
		MargenItem ci = new MargenItem(harvestFeature);
		super.constructFeatureContainer(ci,harvestFeature);
/*
		getRentabilidadHa(),
		getMargenPorHa(),
		getCostoPorHa(),
		getIngresoHa(),
		getImporteFertHa(),		
		getImportePulvHa(),
		getImporteSiembraHa()		 
*/
		//rentabilidad
		ci.setMargenPorHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colMargen.get())));
		ci.setCostoFijoPorHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoFijo.get())));
		ci.setImporteFertHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoFertilizacion.get())));
		ci.setImporteCosechaHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colIngreso.get())));
		ci.setImportePulvHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoPulverizacion.get())));
		ci.setImporteSiembraHa(FeatureContainer.getDoubleFromObj(harvestFeature.getAttribute(colCostoSiembra.get())));
		return ci;
	}

	@Override
	protected DoubleProperty initPrecioLaborHaProperty() {
		return initDoubleProperty(Margen.COLUMNA_COSTO,"0",config.getConfigProperties());
	}

	@Override
	public String getTypeDescriptors() {
		String type = Margen.COLUMNA_RENTABILIDAD + ":Double,"	
				+Margen.COLUMNA_MARGEN + ":Double,"	
				+Margen.COLUMNA_COSTO + ":Double,"	
				+ Margen.COLUMNA_INGRESO + ":Double,"
				+Margen.COLUMNA_COSTO_FERT+":Double,"
				+Margen.COLUMNA_COSTO_PULV+":Double,"
				+Margen.COLUMNA_COSTO_SIEMBR+":Double";
		return type;
	}

	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new LaborConfig(Configuracion.getInstance());
		}
		return config;
	}
}
