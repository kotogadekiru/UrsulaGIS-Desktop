package com.ursulagis.desktop.dao.margen;

import java.util.List;

import javax.persistence.Transient;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborConfig;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter(value = AccessLevel.PUBLIC)
public class Margen extends Labor<MargenItem> {
	public static final String COLUMNA_RENTABILIDAD = "RENTA";
	public static final String COLUMNA_MARGEN = "MARGEN";
	private static final String COLUMNA_COSTO_TOTAL = "COSTO_T";
	private static final String COLUMNA_IMPORTE_COSECHA = "IMP_COSECH";
	private static final String COLUMNA_IMPORTE_FERT = "IMP_FERT";
	private static final String COLUMNA_IMPORTE_PULV = "IMP_PULV";
	private static final String COLUMNA_IMPORTE_SIEMBR = "IMP_SIEM";
	private static final String COLUMNA_IMPORTE_FIJO = "IMP_FIJO";
	
	private static final String COSTO_FLETE_KEY = "COSTO_FLETE_KEY";
	private static final String COSTO_TN_KEY = "COSTO_TN_KEY";
	private static final String COSTO_FIJO_KEY = "COSTO_FIJO_KEY";
	public static final String AMOUNT_COLUMN_KEY = COLUMNA_RENTABILIDAD;//"AMOUNT_COLUMN_KEY";//define si se calcula la rentabilidad o el margen
	
	
	@Transient public StringProperty colRentabilidad= null;
	@Transient public StringProperty colMargen= null;
	@Transient public StringProperty colCostoTotal= null;
	@Transient public StringProperty colIngreso= null;
	@Transient public StringProperty colCostoFertilizacion= null;
	@Transient 
	public StringProperty colCostoPulverizacion= null;
	@Transient 
	public StringProperty colCostoSiembra= null;
	@Transient 
	public StringProperty colCostoFijo= null;

	@Transient 
	public DoubleProperty costoFijoHaProperty = null;
	@Transient 
	private LaborConfig config=null;
	@Transient 
	public Property<Number> costoTnProperty= null;
	@Transient 
	public Property<Number> costoFleteProperty= null;
//	public Property<String> amountProperty= null;//segun si es rentabilidad o margen se dibuja uno o el otro.
	
	@Transient 
	private List<FertilizacionLabor> fertilizaciones;
	@Transient 
	private List<SiembraLabor> siembras;
	@Transient 
	private List<CosechaLabor> cosechas;
	@Transient 
	private List<PulverizacionLabor> pulverizaciones;
	
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

		this.colRentabilidad = PropertyHelper.initStringProperty(Margen.COLUMNA_RENTABILIDAD, properties, availableColums);
		this.colMargen = PropertyHelper.initStringProperty(Margen.COLUMNA_MARGEN, properties, availableColums);
		this.colIngreso = PropertyHelper.initStringProperty(Margen.COLUMNA_IMPORTE_COSECHA, properties, availableColums);
		this.colCostoFertilizacion = PropertyHelper.initStringProperty(Margen.COLUMNA_IMPORTE_FERT, properties, availableColums);
		this.colCostoPulverizacion = PropertyHelper.initStringProperty(Margen.COLUMNA_IMPORTE_PULV, properties, availableColums);
		this.colCostoSiembra = PropertyHelper.initStringProperty(Margen.COLUMNA_IMPORTE_SIEMBR, properties, availableColums);
		this.colCostoTotal = PropertyHelper.initStringProperty(Margen.COLUMNA_COSTO_TOTAL, properties, availableColums);
		this.colCostoFijo = PropertyHelper.initStringProperty(Margen.COLUMNA_IMPORTE_FIJO, properties, availableColums);
		
		this.colAmount=PropertyHelper.initStringProperty(Margen.AMOUNT_COLUMN_KEY, properties, availableColums);
		
		System.out.println("colAmount de Margen en initConfig es "+colAmount.get());
		this.costoFleteProperty=PropertyHelper.initDoubleProperty(Margen.COSTO_FLETE_KEY, "0", properties);
		this.costoTnProperty=PropertyHelper.initDoubleProperty(Margen.COSTO_TN_KEY, "0", properties);
		this.costoFijoHaProperty = PropertyHelper.initDoubleProperty(Margen.COSTO_FIJO_KEY, "0", properties);
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
		ci.setMargenPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_MARGEN)));//XXX este dato no importa porque se recalcula
		ci.setCostoFijoPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_FIJO)));
		ci.setImporteFertHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_FERT)));
		ci.setImporteCosechaHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_COSECHA)));
		ci.setImportePulvHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_PULV)));
		ci.setImporteSiembraHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(Margen.COLUMNA_IMPORTE_SIEMBR)));
		ci.setShowMargen(Margen.COLUMNA_MARGEN.equals(this.colAmount.get()));
		setPropiedadesLabor(ci);
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
		ci.setMargenPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colMargen.get())));//no importa porque se recalcula
		//setear el costoFijo por HA de la labor de acuerdo a lo que dice el feature??
		ci.setCostoFijoPorHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoFijo.get())));
		//this.getCostoFijoHaProperty().set(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoFijo.get())));
		ci.setImporteFertHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoFertilizacion.get())));
		ci.setImporteCosechaHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colIngreso.get())));
		ci.setImportePulvHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoPulverizacion.get())));
		ci.setImporteSiembraHa(LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colCostoSiembra.get())));
		ci.setShowMargen(Margen.COLUMNA_MARGEN.equals(this.colAmount.get()));
		setPropiedadesLabor(ci);
		return ci;
	}

	
	public void setPropiedadesLabor(MargenItem mi){
		if(mi.getCostoFijoPorHa()==0.0) {
			mi.setCostoFijoPorHa(this.getCostoFijoHaProperty().get());
		}
	
	}
	
	@Override
	protected Double initPrecioLaborHa() {
		return PropertyHelper.initDouble(Margen.COSTO_FIJO_KEY,"0",config.getConfigProperties());
	}

	@Override
	protected Double initPrecioInsumo() {
		return PropertyHelper.initDouble(Margen.COSTO_TN_KEY,  "0", config.getConfigProperties());
	//	return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
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
