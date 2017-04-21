package dao.suelo;

import java.util.List;

import org.geotools.data.FileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import dao.Clasificador;
import dao.Labor;
import dao.LaborConfig;
import dao.LaborItem;
import dao.config.Configuracion;
import dao.config.Fertilizante;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionConfig;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;
@Data
public class Suelo extends Labor<SueloItem>{
	private static final double DENSIDAD_SUELO_20CM = 2.6;
	//los nombres de las columnas estandar
	public static final String COLUMNA_N = "PPM N";
	public static final String COLUMNA_P = "PPM P";
	public static final String COLUMNA_K = "PPM K";
	public static final String COLUMNA_S = "PPM S";
	public static final String COLUMNA_MO = "PPM MO";
	
	public static final String COLUMNA_PROF_NAPA= "Prof Napa";
	public static final String COLUMNA_AGUA_PERFIL= "Agua Perf";

	//las propiedades que le permiten al usuario definir el nombre de sus columnas
	public StringProperty colNProperty;
	public StringProperty colPProperty;
	public StringProperty colKProperty;
	public StringProperty colSProperty;
	
	public StringProperty colMOProperty;
	public StringProperty colProfNapaProperty;
	public StringProperty colAguaPerfProperty;
	
	public Suelo() {
		initConfig();
	}

	public Suelo(FileDataStore store) {
		super(store);
		initConfig();
	}


	@Override
	protected DoubleProperty initPrecioLaborHaProperty() {
		return new SimpleDoubleProperty();
	}

	@Override
	public String getTypeDescriptors() {
		String type = Suelo.COLUMNA_N + ":Double,"
				+ Suelo.COLUMNA_P + ":Double,"
				+ Suelo.COLUMNA_K + ":Double,"
				+ Suelo.COLUMNA_S + ":Double,"
				+ Suelo.COLUMNA_MO + ":Double,"
				+ Suelo.COLUMNA_PROF_NAPA + ":Double,"
				+ Suelo.COLUMNA_AGUA_PERFIL + ":Double";

		return type;
	}

	@Override
	public SueloItem constructFeatureContainerStandar(SimpleFeature next, boolean newIDS) {
		SueloItem si = new SueloItem(next);
		super.constructFeatureContainerStandar(si,next,newIDS);
		si.setPpmN(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_N)));
		si.setPpmP(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_P)));
		si.setPpmK(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_K)));
		si.setPpmS(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_S)));
		si.setPpmMO(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_MO)));
		si.setAguaPerfil(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_AGUA_PERFIL)));
		si.setProfNapa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PROF_NAPA)));
		return si;
	}

	@Override
	public SueloItem constructFeatureContainer(SimpleFeature next) {
		SueloItem si = new SueloItem(next);
		super.constructFeatureContainer(si,next);
		return si;
	}

	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new SueloConfig();
		}
		return config;
	}

	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		

		Configuracion properties = getConfigLabor().getConfigProperties();

		colNProperty = super.initStringProperty(COLUMNA_N, properties, availableColums);
		colPProperty = super.initStringProperty(COLUMNA_P, properties, availableColums);
		colKProperty = super.initStringProperty(COLUMNA_K, properties, availableColums);
		colSProperty = super.initStringProperty(COLUMNA_S, properties, availableColums);
		colMOProperty = super.initStringProperty(COLUMNA_MO, properties, availableColums);
		
		colAguaPerfProperty = super.initStringProperty(COLUMNA_AGUA_PERFIL, properties, availableColums);
		colProfNapaProperty = super.initStringProperty(COLUMNA_PROF_NAPA, properties, availableColums);

		//colAmount= colPProperty;
		colAmount= new SimpleStringProperty(COLUMNA_P);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		
	}

	public Double getDensidad() {
		return DENSIDAD_SUELO_20CM;
	}
}
