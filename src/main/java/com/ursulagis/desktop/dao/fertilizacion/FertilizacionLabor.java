package com.ursulagis.desktop.dao.fertilizacion;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborConfig;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.ordenCompra.ProductoLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.DAH;

@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity @Access(AccessType.FIELD)//variable (el default depende de donde pongas el @Id)
public class FertilizacionLabor extends Labor<FertilizacionItem> {
	public static final String COLUMNA_KG_HA = "Kg_FertHa";
	public static final String COLUMNA_PRECIO_FERT = "Precio Kg Fert";
	public static final String COLUMNA_PRECIO_PASADA = "Precio labor/Ha";	
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";
	
	public static final String COLUMNA_DOSIS="Rate";
	
	private static final String FERTILIZANTE_DEFAULT = "FERTILIZANTE_DEFAULT";

	public static final String COSTO_LABOR_FERTILIZACION = "costoLaborFertilizacion";

	public StringProperty colKgHaProperty;

	public Fertilizante fertilizante=null;

	public FertilizacionLabor() {
		initConfig();
	}

	public FertilizacionLabor(FileDataStore store) {
		super(store);
		initConfig();
	}

	//crea una nueva fertilizacion pero con los datos de la fertilizacion de ingreso
	//fecha, fertilizante, titulo, etc
	public FertilizacionLabor(FertilizacionLabor fAPartir) {
		super(fAPartir);
		initConfig();
		//colKgHaProperty.set(fAPartir.colKgHaProperty.get());
		
		setFertilizante(fAPartir.getFertilizante());
		setPrecioInsumo(fAPartir.getPrecioInsumo());
		setPrecioLabor(fAPartir.getPrecioLabor());
		setFecha(fAPartir.getFecha());
		setClasificador(fAPartir.getClasificador().clone());
		
	}

	private void initConfig() {
		this.productoLabor=DAH.getProductoLabor(ProductoLabor.LABOR_DE_FERTILIZACION);
		
		List<String> availableColums = this.getAvailableColumns();		
		Configuracion properties = getConfigLabor().getConfigProperties();

		colKgHaProperty = PropertyHelper.initStringProperty(FertilizacionLabor.COLUMNA_KG_HA,properties,availableColums);
		colAmount= new SimpleStringProperty(FertilizacionLabor.COLUMNA_KG_HA);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		String fertKEY = properties.getPropertyOrDefault(FertilizacionLabor.FERTILIZANTE_DEFAULT,
				Fertilizante.FOSFATO_DIAMONICO_DAP);
		fertilizante = DAH.getFertilizante(fertKEY);//values().iterator().next());
		
//		fertilizante.addListener((obs, bool1, bool2) -> {
//			properties.setProperty(FertilizacionLabor.FERTILIZANTE_DEFAULT,
//					bool2.getNombre());
//		});
	}


	@Override
	public String getTypeDescriptors() {
		/*
		 * 	getCantFertHa(),
				getPrecioFert(),
				getPrecioPasada(),
				getImporteHa()
		 */
		String type = FertilizacionLabor.COLUMNA_KG_HA + ":Double,"
				+ FertilizacionLabor.COLUMNA_PRECIO_FERT + ":Double,"
				+ FertilizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ FertilizacionLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public FertilizacionItem constructFeatureContainer(SimpleFeature next) {
		FertilizacionItem fi = new FertilizacionItem(next);
		super.constructFeatureContainer(fi,next);
		String kgHaCol = colKgHaProperty.get();
		Object o = next.getAttribute(kgHaCol);
		if(o!=null) {
			fi.setDosistHa( LaborItem.getDoubleFromObj(o));
		} else {
			fi.setDosistHa( 0.0);
			System.err.print("leyendo la columna "+kgHaCol+" devolvio null?=> "+o+"  "+next);
		}
		setPropiedadesLabor(fi);
		return fi;
	}

	public void setPropiedadesLabor(FertilizacionItem fi){
		fi.setPrecioInsumo(this.getPrecioInsumo());
		fi.setCostoLaborHa(this.getPrecioLabor());	
	}

	@Override
	public FertilizacionItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		FertilizacionItem fi = new FertilizacionItem(next);
		super.constructFeatureContainerStandar(fi,next,newIDS);
		String kgHaCol = COLUMNA_KG_HA;
		Object o = next.getAttribute(kgHaCol);
		if(o!=null) {
			fi.setDosistHa( LaborItem.getDoubleFromObj(o));
		} else {
			fi.setDosistHa( 0.0);
			System.err.print("leyendo la columna "+kgHaCol+" devolvio null?=> "+o+"  "+next);
		}
		

	//	fi.setDosistHa( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_KG_HA)));		
//		fi.setPrecioInsumo( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PRECIO_FERT)));
//		fi.setCostoLaborHa(LaborItem.getDoubleFromObj(next.getAttribute(COSTO_LABOR_FERTILIZACION)));
//		fi.setImporteHa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_IMPORTE_HA)));
		setPropiedadesLabor(fi);
		return fi;
	}

	@Override
	protected Double initPrecioLaborHa() {
		return PropertyHelper.initDouble(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}

	
	@Override
	protected Double initPrecioInsumo() {
		return PropertyHelper.initDouble(FertilizacionLabor.COLUMNA_PRECIO_FERT,  "0", config.getConfigProperties());
	//	return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}
	
	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new FertilizacionConfig();
		}
		return config;
	}
}
