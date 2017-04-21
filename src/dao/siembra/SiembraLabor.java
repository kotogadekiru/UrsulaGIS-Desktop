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
import dao.LaborItem;
import dao.Labor;
import dao.LaborConfig;
import dao.config.Configuracion;
import dao.config.Semilla;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import utils.ProyectionConstants;

public class SiembraLabor extends Labor<SiembraItem> {
//	private static final String SEMILLAS_POR_BOLSA_KEY = "SEMILLAS_POR_BOLSA";
//	private static final String ENTRE_SURCO_KEY = "ENTRE_SURCO";

	//esta columna es la que viene con los mapas de siembra //mentira las siembras vienen con Rate o AppIdRate o AppRate
	private static final String COLUMNA_DOSIS_SEMILLA = "DosisSemilla";
	
	//esta columna es la que voy a exportar
	//public static final String COLUMNA_BOLSAS_HA = "BolsasHa";//=semillasMetro*(ProyectionConstants.METROS2_POR_HA/entreSurco)/semillasPorBolsa;
	public static final String COLUMNA_PRECIO_SEMILLA = "PrecioSemilla";
	public static final String COLUMNA_PRECIO_PASADA = "CostoLabor";	
	public static final String COLUMNA_IMPORTE_HA = "Importe_ha";
	
//keys configuracion
	private static final String COSTO_LABOR_SIEMBRA = "costoLaborSiembra";
	private static final String SEMILLA_DEFAULT = "SEMILLA_DEFAULT";
//	private static final String SEMILLAS_POR_BOLSA_DEFAULT = "80000";
//	private static final String ENTRE_SURCO_DEFAULT = "0.525";

//	public  SimpleDoubleProperty entreSurco =null; 
//	public  SimpleDoubleProperty semillasPorBolsa=null;

	public StringProperty colDosisSemilla;

	public Property<Semilla> semillaProperty=null;

	public SiembraLabor() {
		initConfig();
	}

	public SiembraLabor(FileDataStore store) {
		this.setInStore(store);// esto configura el nombre	
		initConfig();
	}


	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		

		Configuracion properties = getConfigLabor().getConfigProperties();

		colDosisSemilla = initStringProperty(SiembraLabor.COLUMNA_DOSIS_SEMILLA, properties, availableColums);
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


		precioInsumoProperty = initDoubleProperty(SiembraLabor.COLUMNA_PRECIO_SEMILLA, "0", properties);

		Semilla sDefault = Semilla.semillas.get(Semilla.SEMILLA_DE_MAIZ);
		String semillaKEY = properties.getPropertyOrDefault(SiembraLabor.SEMILLA_DEFAULT, sDefault.getNombre());
		semillaProperty = new SimpleObjectProperty<Semilla>(Semilla.semillas.get(semillaKEY));//values().iterator().next());
		semillaProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(SiembraLabor.SEMILLA_DEFAULT,
					bool2.getNombre());
		});
	}

	@Override
	public String getTypeDescriptors() {
		String type = SiembraLabor.COLUMNA_DOSIS_SEMILLA + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_SEMILLA + ":Double,"
				+ SiembraLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ SiembraLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public SiembraItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		
		SiembraItem siembraItem = new SiembraItem(next);
		super.constructFeatureContainerStandar(siembraItem,next,newIDS);

		siembraItem.setDosisHa( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DOSIS_SEMILLA)));
//		siembraItem.setPrecioInsumo(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PRECIO_SEMILLA)));
//		siembraItem.setCostoLaborHa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PRECIO_PASADA)));	
//		siembraItem.setImporteHa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_IMPORTE_HA)));	
		setPropiedadesLabor(siembraItem);
		return siembraItem;
	}
	
	public void setPropiedadesLabor(SiembraItem si){
		si.setPrecioInsumo(this.precioInsumoProperty.get());
		si.setCostoLaborHa(this.precioLaborProperty.get());	
	}

	@Override
	public SiembraItem constructFeatureContainer(SimpleFeature next) {
		SiembraItem si = new SiembraItem(next);
		super.constructFeatureContainer(si,next);
		
		//Double bolsasHa = LaborItem.getDoubleFromObj(next.getAttribute(colSemillasMetroProperty.get()));
		//Double semillasMetro = bolsasHa*(ProyectionConstants.METROS2_POR_HA/entreSurco.get())/semillasPorBolsa.get();

		si.setDosisHa(LaborItem.getDoubleFromObj(next.getAttribute(colDosisSemilla.get())));
		setPropiedadesLabor(si);
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
	
	public static List<String> getRequieredColumns() {
		List<String> requiredColumns = new ArrayList<String>();
		requiredColumns.add(COLUMNA_DOSIS_SEMILLA);		
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
}
