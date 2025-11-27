package com.ursulagis.desktop.dao.pulverizacion;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborConfig;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.ordenCompra.ProductoLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.DAH;

@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity
public class PulverizacionLabor extends Labor<PulverizacionItem> {
	public static final String COLUMNA_DOSIS = "Dosis";

	public static final String COLUMNA_PRECIO_PASADA = "CostoLab";	
	public static final String COLUMNA_IMPORTE_HA = "Importe_ha";
	public static final String COLUMNA_PRECIO_INSUMO = "CostoIns";

	public static final String COSTO_LABOR_PULVERIZACION = "costoLaborPulverizacion";
	public static final String AGROQUIMICO_DEFAULT = "AGROQUIMICO_DEFAULT_KEY";
	public static final String PRECIO_INSUMO_KEY = "PRECIO_INSUMO";

	@Transient
	public StringProperty colDosisProperty;
	//public StringProperty colCantPasadasProperty;
	//	@Transient
	//	public Property<Agroquimico> agroquimico=null;

	public List<CaldoItem> items= new ArrayList<CaldoItem>();;

	public PulverizacionLabor() {
		super();
		initConfig();
	}

	public PulverizacionLabor(FileDataStore store) {
		super();
		this.setInStore(store);// esto configura el nombre	
		initConfig();
	}

	public PulverizacionLabor(PulverizacionLabor l) {
		super(l);
		initConfig();
		for(CaldoItem i :l.getItems()) {
			CaldoItem ci = new CaldoItem(i);				

			ci.setLabor(this);
			getItems().add(ci);
		}
		setPrecioInsumo(l.getPrecioInsumo());
		setPrecioLabor(l.getPrecioLabor());
		setFecha(l.getFecha());
		setClasificador(l.getClasificador().clone());
	}

	//XXX ver como los listeners de las propiedades me afectan el archivo de properties y 
	//el controller de la configuracion. creo que setea las variables pero nunca las graba a menos 
	//que las grabe el controller
	private void initConfig() {

		this.productoLabor=DAH.getProductoLabor(ProductoLabor.LABOR_DE_PULVERIZACION);
		List<String> availableColums = this.getAvailableColumns();		

		Configuracion properties = getConfigLabor().getConfigProperties();

		colDosisProperty = PropertyHelper.initStringProperty(PulverizacionLabor.COLUMNA_DOSIS, properties, availableColums);
		colAmount= new SimpleStringProperty(PulverizacionLabor.COLUMNA_DOSIS);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection
	}



	@Override
	public String getTypeDescriptors() {
		/*
		getCostoPaquete(),
		getCantPasadasHa(),
		getCostoLaborHa(),
		getImporteHa()
		 */
		String type = PulverizacionLabor.COLUMNA_DOSIS + ":Double,"
				+ PulverizacionLabor.COLUMNA_PRECIO_INSUMO + ":Double,"
				+ PulverizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ PulverizacionLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public PulverizacionItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		PulverizacionItem pItem = new PulverizacionItem(next);
		super.constructFeatureContainerStandar(pItem,next,newIDS);

		pItem.setDosis( LaborItem.getDoubleFromObj(next
				.getAttribute(PulverizacionLabor.COLUMNA_DOSIS)));		
		setPropiedadesLabor(pItem);
		return pItem;
	}


	public void setPropiedadesLabor(PulverizacionItem pi){
		pi.setPrecioInsumo(this.getPrecioInsumo());
		pi.setCostoLaborHa(this.getPrecioLabor());	
	}

	@Override
	public PulverizacionItem constructFeatureContainer(SimpleFeature next) {		
		PulverizacionItem fi = new PulverizacionItem(next);
		super.constructFeatureContainer(fi,next);

		fi.setDosis( LaborItem.getDoubleFromObj(next.getAttribute(colDosisProperty.get())));
		setPropiedadesLabor(fi);

		return fi;
	}

	public void constructClasificador() {
		super.constructClasificador(config.getConfigProperties()
				.getPropertyOrDefault(Clasificador.TIPO_CLASIFICADOR,
						Clasificador.clasficicadores[0]));
	}

	public PulverizacionConfig getConfiguracion() {
		return (PulverizacionConfig) config;
	}

	@Override
	protected Double initPrecioLaborHa() {
		return PropertyHelper.initDouble(PulverizacionLabor.COSTO_LABOR_PULVERIZACION,"0",config.getConfigProperties());
	}

	@Override
	protected Double initPrecioInsumo() {
		return PropertyHelper.initDouble(PulverizacionLabor.PRECIO_INSUMO_KEY, "0", config.getConfigProperties()); 
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
