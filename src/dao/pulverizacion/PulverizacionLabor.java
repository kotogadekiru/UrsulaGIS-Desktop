package dao.pulverizacion;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Transient;

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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;

@Data
@Entity
public class PulverizacionLabor extends Labor<PulverizacionItem> {
	private static final String COLUMNA_DOSIS = "Dosis";
	private static final String COLUMNA_PASADAS = "CantPasada";
	public static final String COLUMNA_PRECIO_PASADA = "CostoLab";	
	public static final String COLUMNA_IMPORTE_HA = "Importe_ha";
	private static final String COLUMNA_PRECIO_INSUMO = "CostoIns";

	private static final String COSTO_LABOR_PULVERIZACION = "costoLaborPulverizacion";
	private static final String AGROQUIMICO_DEFAULT = "AGROQUIMICO_DEFAULT_KEY";
	private static final String PRECIO_INSUMO_KEY = "PRECIO_INSUMO";

@Transient
	public StringProperty colDosisProperty;
	//public StringProperty colCantPasadasProperty;
@Transient
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

		colDosisProperty = initStringProperty(PulverizacionLabor.COLUMNA_DOSIS, properties, availableColums);
		colAmount= new SimpleStringProperty(PulverizacionLabor.COLUMNA_DOSIS);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection
		
//		colCantPasadasProperty =initStringProperty(PulverizacionLabor.COLUMNA_PASADAS, properties, availableColums);

		
	

		String fertKEY = properties.getPropertyOrDefault(
				PulverizacionLabor.AGROQUIMICO_DEFAULT, Agroquimico.agroquimicos.values().iterator().next().getNombre());
		agroquimico = new SimpleObjectProperty<Agroquimico>(Agroquimico.agroquimicos.get(fertKEY));//values().iterator().next());
		agroquimico.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.AGROQUIMICO_DEFAULT,
					bool2.getNombre());
		});
	}



	@Override
	public String getTypeDescriptors() {
		/*
		 *getCostoPaquete(),
				getCantPasadasHa(),
				getCostoLaborHa(),
				getImporteHa()
		 */
		String type = PulverizacionLabor.COLUMNA_DOSIS + ":Double,"
				+ PulverizacionLabor.COLUMNA_PRECIO_INSUMO + ":Double,"
		//		+ PulverizacionLabor.COLUMNA_PASADAS + ":Double,"
				+ PulverizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ PulverizacionLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public PulverizacionItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		PulverizacionItem pItem = new PulverizacionItem(next);
		super.constructFeatureContainerStandar(pItem,next,newIDS);

//		pItem.setPrecioInsumo(LaborItem.getDoubleFromObj(next
//				.getAttribute(PulverizacionLabor.COLUMNA_PRECIO_INSUMO)));
		
		pItem.setDosis( LaborItem.getDoubleFromObj(next
				.getAttribute(PulverizacionLabor.COLUMNA_DOSIS)));

//		pItem.setCantPasadasHa(LaborItem.getDoubleFromObj(next
//				.getAttribute(PulverizacionLabor.COLUMNA_PASADAS)));

//		pItem.setCostoLaborHa(LaborItem.getDoubleFromObj(next
//				.getAttribute(PulverizacionLabor.COLUMNA_PRECIO_PASADA)));	
//		
//		pItem.setImporteHa(LaborItem.getDoubleFromObj(next
//				.getAttribute(PulverizacionLabor.COLUMNA_IMPORTE_HA)));	
		
		setPropiedadesLabor(pItem);

		return pItem;
	}


	public void setPropiedadesLabor(PulverizacionItem pi){
		pi.setPrecioInsumo(this.precioInsumoProperty.get());
		pi.setCostoLaborHa(this.precioLaborProperty.get());	
	}

	@Override
	public PulverizacionItem constructFeatureContainer(SimpleFeature next) {		
		PulverizacionItem fi = new PulverizacionItem(next);
		super.constructFeatureContainer(fi,next);

		fi.setDosis( LaborItem.getDoubleFromObj(next.getAttribute(colDosisProperty.get())));
	//	fi.setCantPasadasHa(LaborItem.getDoubleFromObj(next.getAttribute(colCantPasadasProperty.get())));

		setPropiedadesLabor(fi);

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
	protected DoubleProperty initPrecioInsumoProperty() {
		return initDoubleProperty(PulverizacionLabor.PRECIO_INSUMO_KEY, "0", config.getConfigProperties()); 
		//return initDoubleProperty(Margen.COSTO_TN_KEY,  "0", );
	//	return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}
	
	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new PulverizacionConfig();
		}
		return config;
	}
}
