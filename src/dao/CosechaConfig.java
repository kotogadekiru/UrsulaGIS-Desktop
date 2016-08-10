package dao;

import java.text.DecimalFormat;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * clase que engloba los parametros necesarios para procesar una cosecha.
 * tengo que poder hacer una configuracion individual por cada mapa
 * @author tomas
 *
 */
public class CosechaConfig {
	/*constantes para el archivo de propiedasdes*/
	private static final String CORRECCION_DEMORA_PESADA_KEY = "correccionDemoraPesada";
	private static final String CORRIMIENTO_PESADA_KEY="corrimientoPesada";
	private static final String CORRECCION_OUTLAYERS_KEY = "correccionOutlayers";
	private static final String CORRECCION_RINDE_AREA_KEY = "correccionRindeArea";
	private static final String CORRECCION_SUPERPOSICION_KEY = "correccionSuperposicion";
	private static final String CORRECCION_DISTANCIA_KEY = "correccionDistancia";
	private static final String CORRECCION_ANCHO_KEY = "correccionAncho";
	private static final String ANCHO_OUTLAYERS_KEY = "anchoFiltroOutlayers";
	private static final String MAX_GEOMETRYES_KEY = "cantMaxGeometriasSuperpuestas";
	
	private static final String MAX_RINDE_KEY = "rindeMaximioCultivo";
	
	private static final String RESYMIR_GEOMETRIAS_KEY = "RESUMIR_GEOMETRIAS";
	//1 acre	43.560sq ft (survey) o 10 sq ch	4046,873m 2
	private static final String METROS_POR_UNIDAD_DIST_KEY = "metrosPorUnidadDistancia";//1 pie =	0,3048 metros //1 pulgadas =0,0254 metros
	
	
	/*
	Avena:Estados Unidos: 32 lb = 14,51495584 kg
	Avena:Canadá: 34 lb = 15,42214058 kg
	Cebada: 48 lb = 21,77243376 kg
	Cebada Malteada: 34 lb = 15,42214058 kg
	Maíz: 56 lb = 25,40117272 kg
	Trigo y porotos de soja: 60 lb = 27,2155422 kg 
	
	
	rinde = mas_flow/v*width
	 */
	private static final String SUP_MINIMA_M2_KEY = "SUP_MINIMA_M2";
	private static final String CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA_KEY = "CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA";
	private static final String CANTIDAD_DISTANCIAS_TOLERANCIA_KEY = "CANTIDAD_DISTANCIAS_TOLERANCIA";
	private static final String N_VARIANZAS_TOLERA_KEY = "N_VARIANZAS_TOLERA2";
	private static final String TOLERANCIA_CV_0_1_KEY = "TOLERANCIA_CV_0_1";
	private static final String CORRECCION_FLOW_TO_RINDE = "CORRECCION_FLOW_TO_RINDE";
	private static final String CALIBRAR_RINDE_KEY = "CALIBRAR_RINDE";
	
	/*las propiedades*/
	private BooleanProperty correccionDemoraPesadaProperty;
	private SimpleDoubleProperty  valorCorreccionPesadaProperty;
	private BooleanProperty correccionOutlayersProperty;
	private BooleanProperty correccionRindeProperty;
	private BooleanProperty calibrarRindeProperty;
	private BooleanProperty correccionSuperposicionProperty;
	private BooleanProperty correccionDistanciaProperty;
	private BooleanProperty correccionAnchoProperty;
	//private Property<Double>  valorCorreccionAnchoProperty;
	private SimpleDoubleProperty  anchoFiltroOutlayersProperty;//coorresponde al tamaÃ±o del area considerada para tomar el promedio
	
	private SimpleIntegerProperty  cantMaxGeometriasSuperpuestasProperty;
	
	private BooleanProperty resumirGeometriasProperty;
	private BooleanProperty correccionFlowToRindeProperty;
	
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
	Configuracion config;

	
	
	
	public CosechaConfig(){
		super();
		config =  Configuracion.getInstance();//levanto el archivo de propiedades default pero puedo guardarlo en otro archivo seteando el fileURL
		
		
		DecimalFormat df = new DecimalFormat("#.0000");
		try{
		
		//XXX ver si me conviene actualizar el archivo de propiedades on the fly o on demand
		correccionDemoraPesadaProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_DEMORA_PESADA_KEY,"false")));		
		correccionDemoraPesadaProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRECCION_DEMORA_PESADA_KEY, bool2.toString());
		});	
		
		valorCorreccionPesadaProperty = new SimpleDoubleProperty(df.parse(config.getPropertyOrDefault(CORRIMIENTO_PESADA_KEY,"0")).doubleValue());		
		valorCorreccionPesadaProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRIMIENTO_PESADA_KEY, bool2.toString());
		});	
		
		correccionOutlayersProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_OUTLAYERS_KEY,"false")));
		correccionOutlayersProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRECCION_OUTLAYERS_KEY, bool2.toString());
		});	
		
		correccionRindeProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_RINDE_AREA_KEY,"false")));
		correccionRindeProperty
				.addListener((obs,bool1,bool2)->{
					config.setProperty(CORRECCION_RINDE_AREA_KEY, bool2.toString());
				});	
		
		calibrarRindeProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CALIBRAR_RINDE_KEY,"true")));
		calibrarRindeProperty
				.addListener((obs,bool1,bool2)->{
					config.setProperty(CALIBRAR_RINDE_KEY, bool2.toString());
				});	
		
		correccionFlowToRindeProperty = new SimpleBooleanProperty(
				"true".equals(config.getPropertyOrDefault(CORRECCION_FLOW_TO_RINDE,"false")));
		correccionFlowToRindeProperty
		.addListener((obs,bool1,bool2)->{
			config.setProperty(CORRECCION_FLOW_TO_RINDE, bool2.toString());
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
		
		anchoFiltroOutlayersProperty = new SimpleDoubleProperty(df.parse(config.getPropertyOrDefault(ANCHO_OUTLAYERS_KEY,"50")).doubleValue());
		anchoFiltroOutlayersProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(ANCHO_OUTLAYERS_KEY, bool2.toString());
			}
		);
		
		 cantMaxGeometriasSuperpuestasProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(MAX_GEOMETRYES_KEY,"50")).intValue());
		 cantMaxGeometriasSuperpuestasProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(MAX_GEOMETRYES_KEY, bool2.toString());
				}
			);
		 
			resumirGeometriasProperty = new SimpleBooleanProperty(
					"true".equals(config.getPropertyOrDefault(RESYMIR_GEOMETRIAS_KEY,"false")));		
			resumirGeometriasProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(RESYMIR_GEOMETRIAS_KEY, bool2.toString());
				}
			);
			
		 valorMetrosPorUnidadDistanciaProperty = new SimpleDoubleProperty(df.parse(config.getPropertyOrDefault(METROS_POR_UNIDAD_DIST_KEY,"1")).doubleValue());
		 valorMetrosPorUnidadDistanciaProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(METROS_POR_UNIDAD_DIST_KEY, bool2.toString());
				}
			);		
		 
		 
		 supMinimaProperty = new SimpleDoubleProperty(df.parse(config.getPropertyOrDefault(CosechaConfig.SUP_MINIMA_M2_KEY,"10")).doubleValue());
		 supMinimaProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(SUP_MINIMA_M2_KEY, bool2.toString());
				}
			);		
		 
		
		 cantDistanciasEntradaRegimenProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(CosechaConfig.CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA_KEY,"0")).intValue());
		 cantDistanciasEntradaRegimenProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(CANTIDAD_DISTANCIAS_ENTRADA_REGIMEN_PASADA_KEY, bool2.toString());
				}
			);
		 
		 
		 cantDistanciasToleraProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(CosechaConfig.CANTIDAD_DISTANCIAS_TOLERANCIA_KEY,"5")).intValue());
		 cantDistanciasToleraProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(CANTIDAD_DISTANCIAS_TOLERANCIA_KEY, bool2.toString());
				}
			);
		 
	
		 
			
		 
		 nVarianzasToleraProperty = new SimpleIntegerProperty(df.parse(config.getPropertyOrDefault(CosechaConfig.N_VARIANZAS_TOLERA_KEY,"1")).intValue());
		 nVarianzasToleraProperty.addListener((obs,bool1,bool2)->{
				config.setProperty(N_VARIANZAS_TOLERA_KEY, bool2.toString());
				}
			);
		 
		 
		 toleranciaCVProperty = new SimpleDoubleProperty(df.parse(config.getPropertyOrDefault(CosechaConfig.TOLERANCIA_CV_0_1_KEY,"0.13")).doubleValue());
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
	public DoubleProperty  valorCorreccionPesadaProperty(){return valorCorreccionPesadaProperty;}
	public BooleanProperty correccionOutlayersProperty() {return correccionOutlayersProperty;}
	public BooleanProperty correccionRindeProperty() { return correccionRindeProperty;}
	public BooleanProperty calibrarRindeProperty() { return calibrarRindeProperty;}
	public BooleanProperty correccionSuperposicionProperty() { return correccionSuperposicionProperty;}
	public BooleanProperty correccionDistanciaProperty() { return correccionDistanciaProperty;}
	public BooleanProperty correccionAnchoProperty() { return correccionAnchoProperty;}
	
	public DoubleProperty anchoFiltroOutlayersProperty() { return anchoFiltroOutlayersProperty;}//coorresponde al tamaÃ±o del area considerada para tomar el promedio
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

	public boolean correccionRindeAreaEnabled() {
		return correccionRindeProperty().getValue();
		// return "true".equals(configProp.getProperty(CORRECCION_RINDE_AREA));
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
	
	public double getMetrosPorUnidadDistancia(){
		return (double) valorMetrosPorUnidadDistanciaProperty.getValue(); 
	}

	public int getMAXGeometries() {
		return (int) cantMaxGeometriasSuperpuestasProperty.getValue();
	}

	public Double getCorrimientoPesada() {
		return (double) valorCorreccionPesadaProperty.getValue(); 		
	}
	
	public void save(){
		config.save();
	}

	public Property<Boolean> resumirGeometriasProperty() {
		return resumirGeometriasProperty;
	}

	public Property<Boolean> correccionFlowToRindeProperty() {
		return correccionFlowToRindeProperty;
	}
}
