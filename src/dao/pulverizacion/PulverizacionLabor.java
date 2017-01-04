package dao.pulverizacion;

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
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraLabor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PulverizacionLabor extends Labor<PulverizacionItem> {
	private static final String COLUMNA_COSTO_PAQ = "CostoPaq";
	private static final String COLUMNA_PASADAS = "CantPasada";
	public static final String COLUMNA_PRECIO_PASADA = "CostoLab";	
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";

	private static final String COSTO_LABOR_PULVERIZACION = "costoLaborPulverizacion";
	
	public StringProperty colCostoPaqProperty;
	public StringProperty colCantPasadasProperty;
	
	private static Map<String, String> columnsMap= new HashMap<String, String>();
	
	//public PulverizacionConfig config=null;

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
		
		colCostoPaqProperty = new SimpleStringProperty(
				properties.getPropertyOrDefault(
						PulverizacionLabor.COLUMNA_COSTO_PAQ,
						PulverizacionLabor.COLUMNA_COSTO_PAQ));
		if(!availableColums.contains(colCostoPaqProperty.get())&&availableColums.contains(PulverizacionLabor.COLUMNA_COSTO_PAQ)){
			colCostoPaqProperty.setValue(PulverizacionLabor.COLUMNA_COSTO_PAQ);
		}
		colCostoPaqProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.COLUMNA_COSTO_PAQ,
					bool2.toString());
		});
		
		colAmount= new SimpleStringProperty(PulverizacionLabor.COLUMNA_COSTO_PAQ);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		/*columnas nuevas*/
		colElevacion = new SimpleStringProperty(
				properties.getPropertyOrDefault(CosechaLabor.COLUMNA_ELEVACION,
						CosechaLabor.COLUMNA_ELEVACION));
		if(!availableColums.contains(colElevacion.get())&&availableColums.contains(CosechaLabor.COLUMNA_ELEVACION)){
			colElevacion.setValue(CosechaLabor.COLUMNA_ELEVACION);
		}
		colElevacion.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_ELEVACION,
					bool2.toString());
		});
		
		colAncho = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_ANCHO, CosechaLabor.COLUMNA_ANCHO));
		if(!availableColums.contains(colAncho.get())&&availableColums.contains(CosechaLabor.COLUMNA_ANCHO)){
			colAncho.setValue(CosechaLabor.COLUMNA_ANCHO);
		} 
		colAncho.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_ANCHO, bool2);
		});// bool2 es un string asi que no necesito convertirlo
		
		colDistancia = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_DISTANCIA, CosechaLabor.COLUMNA_DISTANCIA));
		if(!availableColums.contains(colAncho.get())&&availableColums.contains(CosechaLabor.COLUMNA_DISTANCIA)){
			colDistancia.setValue(CosechaLabor.COLUMNA_DISTANCIA);
		} 
		colAncho.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_ANCHO, bool2);
		});// bool2 es un string asi que no necesito convertirlo
		
		colCurso = new SimpleStringProperty(properties.getPropertyOrDefault(
				CosechaLabor.COLUMNA_CURSO, CosechaLabor.COLUMNA_CURSO));
		if(!availableColums.contains(colCurso.get())&&availableColums.contains(CosechaLabor.COLUMNA_CURSO)){
			colCurso.setValue(CosechaLabor.COLUMNA_CURSO);
		}
		colCurso.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_CURSO, bool2.toString());
		});

		colDistancia = new SimpleStringProperty(
				properties.getPropertyOrDefault(CosechaLabor.COLUMNA_DISTANCIA,
						CosechaLabor.COLUMNA_DISTANCIA));
		if(!availableColums.contains(colDistancia.get())&&availableColums.contains(CosechaLabor.COLUMNA_DISTANCIA)){
			colDistancia.setValue(CosechaLabor.COLUMNA_DISTANCIA);
		}
		colDistancia.addListener((obs, bool1, bool2) -> {
			properties.setProperty(CosechaLabor.COLUMNA_DISTANCIA,
					bool2.toString());
		});
		
		 /**/
		
		
		precioLaborProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						PulverizacionLabor.COLUMNA_PRECIO_PASADA, "0")));
		precioLaborProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.COLUMNA_PRECIO_PASADA,
					bool2.toString());
		});
		
		precioInsumoProperty = new SimpleDoubleProperty(
				Double.parseDouble(properties.getPropertyOrDefault(
						PulverizacionLabor.COLUMNA_COSTO_PAQ, "0")));
		precioInsumoProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(PulverizacionLabor.COLUMNA_COSTO_PAQ,
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
		
	}
		
//	@Override
//	public SimpleFeatureType getType() {
//		SimpleFeatureType type = null;
//		try {
//			/*
//			 * geom tiene que ser Point, Line o Polygon. no puede ser Geometry
//			 * porque podria ser cualquiera y solo permite un tipo por archivo
//			 * los nombre de las columnas no pueden ser de mas de 10 char
//			 */
//			
////			featureBuilder.addAll(new Object[]{super.getGeometry(),
////			getCostoPaquete(),
////			getCantPasadasHa(),
////			getCostoLaborHa(),
////			getImporteHa(),
////			getCategoria()});
//
//			type = DataUtilities.createType("Pulverizacion", "the_geom:MultiPolygon:srid=4326,"//"*geom:Polygon,"the_geom
//					+ PulverizacionLabor.COLUMNA_COSTO_PAQ + ":Double,"
//					+ PulverizacionLabor.COLUMNA_PASADAS + ":Double,"
//					+ PulverizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
//					+ PulverizacionLabor.COLUMNA_IMPORTE_HA + ":Double,"
//					+ PulverizacionLabor.COLUMNA_CATEGORIA + ":Integer");
//		} catch (SchemaException e) {
//
//			e.printStackTrace();
//		}
//		return type;
//	}
	
	@Override
	public String getTypeDescriptors() {
		/*
		 *getCostoPaquete(),
				getCantPasadasHa(),
				getCostoLaborHa(),
				getImporteHa()
		 */
		String type = PulverizacionLabor.COLUMNA_COSTO_PAQ + ":Double,"
				+ PulverizacionLabor.COLUMNA_PASADAS + ":Double,"
				+ PulverizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ PulverizacionLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public PulverizacionItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		PulverizacionItem pItem = new PulverizacionItem(next);
		super.constructFeatureContainerStandar(pItem,next,newIDS);
			
	pItem.setCostoPaquete( FeatureContainer.getDoubleFromObj(next
			.getAttribute(PulverizacionLabor.COLUMNA_COSTO_PAQ)));

	pItem.setCantPasadasHa(FeatureContainer.getDoubleFromObj(next
			.getAttribute(PulverizacionLabor.COLUMNA_PASADAS)));
	
	pItem.setCostoLaborHa(FeatureContainer.getDoubleFromObj(next
			.getAttribute(PulverizacionLabor.COLUMNA_PRECIO_PASADA)));	
	//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get
	
		return pItem;
	}


	@Override
	public PulverizacionItem constructFeatureContainer(SimpleFeature next) {
		
		PulverizacionItem fi = new PulverizacionItem(next);
		super.constructFeatureContainer(fi,next);
		
	

	fi.setCostoPaquete( FeatureContainer.getDoubleFromObj(next
			.getAttribute(colCostoPaqProperty.get())));
	fi.setCantPasadasHa(FeatureContainer.getDoubleFromObj(next
			.getAttribute(colCantPasadasProperty.get())));
	fi.setCostoLaborHa(this.precioLaborProperty.get());	
	//ci.setImporteHa(cantFertHa * precioFert + precioPasada);//no hace falta setearlo porque se actualiza en el get
	
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
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new PulverizacionConfig();
		}
		return config;
	}

		public void setColumnsMap(Map<String, String> columns) {
		columnsMap=columns;
		colCostoPaqProperty.setValue(columnsMap.get(PulverizacionItem.COLUMNA_COSTO_PAQUETE));
		colCantPasadasProperty.setValue(columnsMap.get(PulverizacionItem.COLUMNA_CANT_PASADAS));

	}
}
