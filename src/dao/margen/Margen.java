package dao.margen;

import java.util.List;

import org.geotools.data.FileDataStore;
import org.opengis.feature.simple.SimpleFeature;

import dao.LaborItem;
import dao.Labor;
import dao.LaborConfig;
import dao.config.Configuracion;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;

public class Margen extends Labor<MargenItem> {
	private static final String COLUMNA_RENTABILIDAD = "RENTA";
	private static final String COLUMNA_MARGEN = "MARGEN";
	private static final String COLUMNA_COSTO_TOTAL = "COSTO_T";
	private static final String COLUMNA_IMPORTE_COSECHA = "IMP_COSECH";
	private static final String COLUMNA_IMPORTE_FERT = "IMP_FERT";
	private static final String COLUMNA_IMPORTE_PULV = "IMP_PULV";
	private static final String COLUMNA_IMPORTE_SIEMBR = "IMP_SIEM";
	private static final String COLUMNA_IMPORTE_FIJO = "IMP_FIJO";
	
	private static final String COSTO_FLETE_KEY = "COSTO_FLETE_KEY";
	private static final String COSTO_TN_KEY = "COSTO_TN_KEY";
	private static final String COSTO_FIJO_KEY = "COSTO_FIJO_KEY";
	private static final String AMOUNT_COLUMN_KEY = "AMOUNT_COLUMN_KEY";//define si se calcula la rentabilidad o el margen
	
	
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
	public Property<Number> costoTnProperty= null;
	public Property<Number> costoFleteProperty= null;
	public Property<String> amountProperty= null;//segun si es rentabilidad o margen se dibuja uno o el otro.
	
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
		this.colIngreso = initStringProperty(Margen.COLUMNA_IMPORTE_COSECHA, properties, availableColums);
		this.colCostoFertilizacion = initStringProperty(Margen.COLUMNA_IMPORTE_FERT, properties, availableColums);
		this.colCostoPulverizacion = initStringProperty(Margen.COLUMNA_IMPORTE_PULV, properties, availableColums);
		this.colCostoSiembra = initStringProperty(Margen.COLUMNA_IMPORTE_SIEMBR, properties, availableColums);
		this.colCostoTotal = initStringProperty(Margen.COLUMNA_COSTO_TOTAL, properties, availableColums);
		this.colCostoFijo = initStringProperty(Margen.COLUMNA_IMPORTE_FIJO, properties, availableColums);
		
		this.amountProperty=initStringProperty(Margen.AMOUNT_COLUMN_KEY, properties, availableColums);
		
		this.costoFleteProperty=initDoubleProperty(Margen.COSTO_FLETE_KEY, "0", properties);
		this.costoTnProperty=initDoubleProperty(Margen.COSTO_TN_KEY, "0", properties);
		this.costoFijoHaProperty = initDoubleProperty(Margen.COSTO_FIJO_KEY, "0", properties);
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
		ci.setMargenPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_MARGEN)));
		ci.setCostoFijoPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_FIJO)));
		ci.setImporteFertHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_FERT)));
		ci.setImporteCosechaHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_COSECHA)));
		ci.setImportePulvHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_PULV)));
		ci.setImporteSiembraHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_SIEMBR)));
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
		ci.setMargenPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colMargen.get())));
		ci.setCostoFijoPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoFijo.get())));
		ci.setImporteFertHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoFertilizacion.get())));
		ci.setImporteCosechaHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colIngreso.get())));
		ci.setImportePulvHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoPulverizacion.get())));
		ci.setImporteSiembraHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoSiembra.get())));
		return ci;
	}

	@Override
	protected DoubleProperty initPrecioLaborHaProperty() {
		return initDoubleProperty(Margen.COSTO_FIJO_KEY,"0",config.getConfigProperties());
	}

	
	/**
	 * 	
	 			getRentabilidadHa(),
				getMargenPorHa(),
				getCostoPorHa(),
				getIngresoHa(),
				getImporteFertHa(),		
				getImportePulvHa(),
				getImporteSiembraHa()		
	 */
	@Override
	public String getTypeDescriptors() {
		/*
		 //rentabilidad se calcula
		ci.setMargenPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colMargen.get())));
		//costo total se calcula
		ci.setCostoFijoPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoFijo.get())));
		ci.setImporteCosechaHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colIngreso.get())));
		ci.setImporteFertHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoFertilizacion.get())));		
		ci.setImportePulvHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoPulverizacion.get())));
		ci.setImporteSiembraHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoSiembra.get()))); 
		 */
		String type = Margen.COLUMNA_RENTABILIDAD + ":Double,"	
				+Margen.COLUMNA_MARGEN + ":Double,"	
				+Margen.COLUMNA_COSTO_TOTAL + ":Double,"	
				+Margen.COLUMNA_IMPORTE_FIJO + ":Double,"	
				+Margen.COLUMNA_IMPORTE_COSECHA + ":Double,"
				+Margen.COLUMNA_IMPORTE_FERT+":Double,"
				+Margen.COLUMNA_IMPORTE_PULV+":Double,"
				+Margen.COLUMNA_IMPORTE_SIEMBR+":Double";
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
