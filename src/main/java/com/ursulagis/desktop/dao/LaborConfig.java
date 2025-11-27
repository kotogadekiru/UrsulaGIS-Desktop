package com.ursulagis.desktop.dao;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.siembra.SiembraConfig;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.dao.LaborConfig;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * clase que agrupa las configuraciones comunes a todas las labores
 * @author quero
 *
 */

public class LaborConfig {
	/*constantes para el archivo de propiedasdes*/
	public static final String ANCHO_GRILLA_KEY = "anchoGrilla";
	private static final String CORRECCION_DEMORA_PESADA_KEY = "correccionDemoraPesada";
	private static final String CORRIMIENTO_PESADA_KEY="corrimientoPesada";
	private static final String CORRECCION_OUTLAYERS_KEY = "correccionOutlayers";
	private static final String CORRECCION_SUPERPOSICION_KEY = "correccionSuperposicion";
	private static final String CORRECCION_DISTANCIA_KEY = "correccionDistancia";
	private static final String CORRECCION_ANCHO_KEY = "correccionAncho";
	private static final String ANCHO_OUTLAYERS_KEY = "anchoFiltroOutlayers";
	private static final String MAX_GEOMETRYES_KEY = "cantMaxGeometriasSuperpuestas";
	
	private static final String RESUMIR_GEOMETRIAS_KEY = "RESUMIR_GEOMETRIAS";
	//1 acre	43.560sq ft (survey) o 10 sq ch	4046,873m 2
	private static final String METROS_POR_UNIDAD_DIST_KEY = "metrosPorUnidadDistancia";//1 pie =	0,3048 metros //1 pulgadas =0,0254 metros

	public static final String SUP_MINIMA_M2_KEY = "SUP_MINIMA_M2";
	private static final String CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA_KEY = "CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA";
	private static final String CANTIDAD_DISTANCIAS_TOLERANCIA_KEY = "CANTIDAD_DISTANCIAS_TOLERANCIA";
	private static final String N_VARIANZAS_TOLERA_KEY = "N_VARIANZAS_TOLERA2";
	private static final String TOLERANCIA_CV_0_1_KEY = "TOLERANCIA_CV_0_1";
	
	/*las propiedades*/
	private BooleanProperty correccionDemoraPesadaProperty;
	private SimpleDoubleProperty  valorCorrimientoPesadaProperty;
	private BooleanProperty correccionOutlayersProperty;

	private BooleanProperty correccionSuperposicionProperty;
	private BooleanProperty correccionDistanciaProperty;
	private BooleanProperty correccionAnchoProperty;

	private SimpleDoubleProperty  anchoFiltroOutlayersProperty;//coorresponde al tamaño del area considerada para tomar el promedio
	private SimpleDoubleProperty  anchoGrillaProperty;
	
	private SimpleIntegerProperty  cantMaxGeometriasSuperpuestasProperty;
	
	private BooleanProperty resumirGeometriasProperty;
	
	private SimpleDoubleProperty  valorMetrosPorUnidadDistanciaProperty;
	
	private SimpleDoubleProperty  supMinimaProperty;
	private IntegerProperty  cantDistanciasEntradaRegimenProperty;
	private IntegerProperty  cantDistanciasToleraProperty;
	//private IntegerProperty clasesClasificadorProperty;
	private IntegerProperty  nVarianzasToleraProperty;
	private SimpleDoubleProperty  toleranciaCVProperty;
	
	

	/**
	 * hace referencia al archivo donde se guardan las configuraciones
	 */
	protected Configuracion config=null;

	public LaborConfig(Configuracion _config){
		super();
		 config =  _config;//levanto el archivo de propiedades default pero puedo guardarlo en otro archivo seteando el fileURL
		
		
//		DecimalFormat df = new DecimalFormat("#.0000");
//		df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("EN")));
		NumberFormat df = Messages.getNumberFormat();
		try{
		
		//XXX ver si me conviene actualizar el archivo de propiedades on the fly o on demand
		correccionDemoraPesadaProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_DEMORA_PESADA_KEY,"false")));		
		correccionDemoraPesadaProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRECCION_DEMORA_PESADA_KEY, bool2.toString());
		});	
		
		valorCorrimientoPesadaProperty = PropertyHelper.initDoubleProperty(CORRIMIENTO_PESADA_KEY, "0", config);
		
		correccionOutlayersProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_OUTLAYERS_KEY,"false")));
		correccionOutlayersProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRECCION_OUTLAYERS_KEY, bool2.toString());
		});	
				
	
		correccionSuperposicionProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_SUPERPOSICION_KEY,"false")));
		correccionSuperposicionProperty
				.addListener((obs,bool1,bool2)->{
					config.setProperty(CORRECCION_SUPERPOSICION_KEY, bool2.toString());
				});	
		
		correccionDistanciaProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_DISTANCIA_KEY,"false")));
		correccionDistanciaProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRECCION_DISTANCIA_KEY, bool2.toString());
		});
		
		correccionAnchoProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_ANCHO_KEY,"false")));		
		correccionAnchoProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRECCION_ANCHO_KEY, bool2.toString());
			}
		);
		
		anchoFiltroOutlayersProperty = PropertyHelper.initDoubleProperty(ANCHO_OUTLAYERS_KEY, "50", config);
	
		
		anchoGrillaProperty = PropertyHelper.initDoubleProperty(ANCHO_GRILLA_KEY, "10", config);

		
		 cantMaxGeometriasSuperpuestasProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(MAX_GEOMETRYES_KEY,"50")).intValue());
		 cantMaxGeometriasSuperpuestasProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(MAX_GEOMETRYES_KEY, bool2.toString());
				}
			);
		 
			resumirGeometriasProperty = new SimpleBooleanProperty(
					"true".equals(config.getPropertyOrDefault(RESUMIR_GEOMETRIAS_KEY,"false")));		
			resumirGeometriasProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(RESUMIR_GEOMETRIAS_KEY, bool2.toString());
				}
			);
			
		 valorMetrosPorUnidadDistanciaProperty =  PropertyHelper.initDoubleProperty(METROS_POR_UNIDAD_DIST_KEY, "1", config);
		 supMinimaProperty =  PropertyHelper.initDoubleProperty(SUP_MINIMA_M2_KEY, "10", config);
	
		 
		
		 cantDistanciasEntradaRegimenProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(LaborConfig.CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA_KEY,"0")).intValue());
		 cantDistanciasEntradaRegimenProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA_KEY, bool2.toString());
				}
			);
		 
		 
		 cantDistanciasToleraProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(LaborConfig.CANTIDAD_DISTANCIAS_TOLERANCIA_KEY,"5")).intValue());
		 cantDistanciasToleraProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(CANTIDAD_DISTANCIAS_TOLERANCIA_KEY, bool2.toString());
				}
			);
		 
	
		 
			
		 
		 nVarianzasToleraProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(LaborConfig.N_VARIANZAS_TOLERA_KEY,"1")).intValue());
		 nVarianzasToleraProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(N_VARIANZAS_TOLERA_KEY, bool2.toString());
				}
			);
		 
		 
		 toleranciaCVProperty = new SimpleDoubleProperty(df.parse(config.getPropertyOrDefault(LaborConfig.TOLERANCIA_CV_0_1_KEY,"0.13")).doubleValue());
		 toleranciaCVProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(TOLERANCIA_CV_0_1_KEY, bool2.toString());
				}
			);
		}catch(Exception e){
			e.printStackTrace();
		}
		 
	}
	
	/**
	 * devuelvo las propiedades para que puedan ser vinculadas con los componentes.
	 * @return
	 */
	public BooleanProperty correccionDemoraPesadaProperty() {return correccionDemoraPesadaProperty;}
	public DoubleProperty  valorCorreccionPesadaProperty(){return valorCorrimientoPesadaProperty;}
	public BooleanProperty correccionOutlayersProperty() {return correccionOutlayersProperty;}

	public BooleanProperty correccionSuperposicionProperty() { return correccionSuperposicionProperty;}
	public BooleanProperty correccionDistanciaProperty() { return correccionDistanciaProperty;}
	public BooleanProperty correccionAnchoProperty() { return correccionAnchoProperty;}
	
	public DoubleProperty anchoFiltroOutlayersProperty() { return anchoFiltroOutlayersProperty;}//coorresponde al tamaño del area considerada para tomar el promedio
	public DoubleProperty anchoGrillaProperty() { return anchoGrillaProperty;}
	public IntegerProperty cantMaxGeometriasSuperpuestasProperty() { return cantMaxGeometriasSuperpuestasProperty;}
	public DoubleProperty valorMetrosPorUnidadDistanciaProperty() { return valorMetrosPorUnidadDistanciaProperty;}

	public DoubleProperty supMinimaProperty() { return supMinimaProperty;}
	public IntegerProperty cantDistanciasEntradaRegimenProperty() { return cantDistanciasEntradaRegimenProperty;}
	public IntegerProperty cantDistanciasToleraProperty() { return cantDistanciasToleraProperty;}
	public IntegerProperty nVarianzasToleraProperty() { return nVarianzasToleraProperty;}
	public DoubleProperty toleranciaCVProperty() { return toleranciaCVProperty;}
	
//	public IntegerProperty clasesClasificadorProperty() { return clasesClasificadorProperty;
//	}
/**
 * devuelvo los valores de las propiedades para ser utilizadas en los filtros
 * @return
 */
	public boolean correccionAnchoEnabled() {
		return correccionAnchoProperty.getValue();
		// return "true".equals(configProp.getProperty(CORRECCION_ANCHO));
	}

	public boolean correccionDistanciaEnabled() {
		return correccionDistanciaProperty.getValue();
		// return "true".equals(configProp.getProperty(CORRECCION_DISTANCIA));
	}

	public boolean correccionSuperposicionEnabled() {
		return correccionSuperposicionProperty.getValue();
		// return
		// "true".equals(configProp.getProperty(CORRECCION_SUPERPOSICION));
	}



	public boolean correccionOutlayersEnabled() {
		return correccionOutlayersProperty.getValue();
		// return "true".equals(configProp.getProperty(CORRECCION_OUTLAYERS));
	}

	public boolean correccionDemoraPesadaEnabled() {
		return correccionDemoraPesadaProperty.getValue();
		// return
		// "true".equals(configProp.getProperty(CORRECCION_DEMORA_PESADA));
	}

	public double getAnchoFiltroOutlayers() {
		return (double) anchoFiltroOutlayersProperty.getValue();
	}
	
	public double getAnchoGrilla() {
		return (double) anchoGrillaProperty.getValue();
	}
	
	public double getMetrosPorUnidadDistancia(){
		return (double) valorMetrosPorUnidadDistanciaProperty.getValue(); 
	}

	public int getMAXGeometries() {
		return (int) cantMaxGeometriasSuperpuestasProperty.getValue();
	}

	public Double getCorrimientoPesada() {
		return  valorCorrimientoPesadaProperty.getValue(); 		
	}
	
	public void save(){
		getConfigProperties().save();
	}

	public Property<Boolean> resumirGeometriasProperty() {
		return resumirGeometriasProperty;
	}

	public  Configuracion getConfigProperties(){
		return config;
	}


}
