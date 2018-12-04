package dao.siembra;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.geotools.data.FileDataStore;
import org.opengis.feature.simple.SimpleFeature;

import dao.Clasificador;
import dao.Labor;
import dao.LaborConfig;
import dao.LaborItem;
import dao.config.Configuracion;
import dao.config.Cultivo;
import dao.config.Establecimiento;
import dao.config.Semilla;
import dao.utils.PropertyHelper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
public class SiembraLabor extends Labor<SiembraItem> {
//	private static final String SEMILLAS_POR_BOLSA_KEY = "SEMILLAS_POR_BOLSA";
//	private static final String ENTRE_SURCO_KEY = "ENTRE_SURCO";

	//esta columna es la que viene con los mapas de siembra //mentira las siembras vienen con Rate o AppIdRate o AppRate
	public static final String COLUMNA_DOSIS_SEMILLA = "DosisSemilla";
	
	//esta columna es la que voy a exportar
	//public static final String COLUMNA_BOLSAS_HA = "BolsasHa";//=semillasMetro*(ProyectionConstants.METROS2_POR_HA/entreSurco)/semillasPorBolsa;
	public static final String COLUMNA_PRECIO_SEMILLA = "PrecioSemilla";
	public static final String COLUMNA_PRECIO_PASADA = "CostoLabor";	
	public static final String COLUMNA_IMPORTE_HA = "Importe_ha";
	
//keys configuracion
	public static final String COSTO_LABOR_SIEMBRA = "costoLaborSiembra";
	public static final String SEMILLA_DEFAULT = "SEMILLA_DEFAULT";
//	private static final String SEMILLAS_POR_BOLSA_DEFAULT = "80000";
//	private static final String ENTRE_SURCO_DEFAULT = "0.525";

	public static final String COLUMNA_DOSIS_LINEA = "Fert L";
	public static final String COLUMNA_DOSIS_COSTADO= "Fert C";

//	public  SimpleDoubleProperty entreSurco =null; 
//	public  SimpleDoubleProperty semillasPorBolsa=null;
	@Transient
	public StringProperty colDosisSemilla;
	@Transient
	public Property<Semilla> semillaProperty=null;
	
	@ManyToOne
	private Semilla semilla=null;

	public SiembraLabor() {
		super();
		initConfig();
	}

	public SiembraLabor(FileDataStore store) {
		super(store);
		//this.setInStore(store);// esto configura el nombre	
		initConfig();
	}


	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		

		Configuracion properties = getConfigLabor().getConfigProperties();

		colDosisSemilla = PropertyHelper.initStringProperty(SiembraLabor.COLUMNA_DOSIS_SEMILLA, properties, availableColums);
		colAmount= new SimpleStringProperty(SiembraLabor.COLUMNA_DOSIS_SEMILLA);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection
		
//		entreSurco = new SimpleDoubleProperty(
//				Double.parseDouble(properties.getPropertyOrDefault(
//						SiembraLabor.ENTRE_SURCO_KEY, ENTRE_SURCO_DEFAULT))
//				);		
//		entreSurco.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(SiembraLabor.ENTRE_SURCO_KEY,
//					bool2.toString());
//		});
//		
//		semillasPorBolsa = new SimpleDoubleProperty(
//				Double.parseDouble(properties.getPropertyOrDefault(
//						SiembraLabor.SEMILLAS_POR_BOLSA_KEY, SEMILLAS_POR_BOLSA_DEFAULT))
//				);		
//		semillasPorBolsa.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(SiembraLabor.SEMILLAS_POR_BOLSA_KEY,
//					bool2.toString());
//		});


	//	precioInsumoProperty = initDoubleProperty(SiembraLabor.COLUMNA_PRECIO_SEMILLA, "0", properties);

		Semilla sDefault = Semilla.semillas.get(Semilla.SEMILLA_DE_MAIZ);
		String semillaKEY = properties.getPropertyOrDefault(SiembraLabor.SEMILLA_DEFAULT, sDefault.getNombre());
		semillaProperty = new SimpleObjectProperty<Semilla>(Semilla.semillas.get(semillaKEY));//values().iterator().next());
		semillaProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.SEMILLA_DEFAULT,
					bool2.getNombre());
		});
	}

	@Override
	@Transient
	public String getTypeDescriptors() {
		String type = SiembraLabor.COLUMNA_DOSIS_SEMILLA + ":Double,"
				+ SiembraLabor.COLUMNA_DOSIS_LINEA + ":Double,"
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_SEMILLA + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ SiembraLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	@Transient
	public SiembraItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		
		SiembraItem siembraItem = new SiembraItem(next);
		super.constructFeatureContainerStandar(siembraItem,next,newIDS);

		siembraItem.setDosisHa( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_SEMILLA)));
		siembraItem.setDosisFertLinea( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_LINEA)));
		siembraItem.setDosisFertCostado( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_COSTADO)));
//		siembraItem.setPrecioInsumo(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PRECIO_SEMILLA)));
//		siembraItem.setCostoLaborHa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PRECIO_PASADA)));	
//		siembraItem.setImporteHa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_IMPORTE_HA)));	
		setPropiedadesLabor(siembraItem);
		return siembraItem;
	}
	
	public void setPropiedadesLabor(SiembraItem si){
		si.setPrecioInsumo(this.getPrecioInsumo());
		si.setCostoLaborHa(this.getPrecioLabor());	
	}

	@Override
	@Transient
	public SiembraItem constructFeatureContainer(SimpleFeature next) {
		SiembraItem si = new SiembraItem(next);
		super.constructFeatureContainer(si,next);
		
		//Double bolsasHa = LaborItem.getDoubleFromObj(next.getAttribute(colSemillasMetroProperty.get()));
		//Double semillasMetro = bolsasHa*(ProyectionConstants.METROS2_POR_HA/entreSurco.get())/semillasPorBolsa.get();

		si.setDosisHa(LaborItem.getDoubleFromObj(next.getAttribute(colDosisSemilla.get())));
		si.setDosisFertLinea( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_LINEA)));
		si.setDosisFertCostado( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_COSTADO)));
		setPropiedadesLabor(si);
		return si;
	}

	public void constructClasificador() {
		super.constructClasificador(config.getConfigProperties()
				.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
						Clasificador.CLASIFICADOR_JENKINS));
	}

	@Transient
	public SiembraConfig getConfiguracion() {
		return (SiembraConfig) config;
	}
	
	@Transient
	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_DOSIS_SEMILLA);		
		return requiredColumns;
	}

	@Override
	protected Double initPrecioLaborHa() {
		return PropertyHelper.initDouble(SiembraLabor.COSTO_LABOR_SIEMBRA,"0",config.getConfigProperties());
	}


	@Override
	protected Double initPrecioInsumo() {
		return PropertyHelper.initDouble(SiembraLabor.COLUMNA_PRECIO_SEMILLA, "0", config.getConfigProperties()); 
		//return initDoubleProperty(Margen.COSTO_TN_KEY,  "0", );
	//	return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}
	
	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new SiembraConfig();
		}
		return config;
	}
}
