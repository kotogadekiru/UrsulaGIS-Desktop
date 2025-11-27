package com.ursulagis.desktop.dao.cosecha;

import java.text.DecimalFormat;

import com.ursulagis.desktop.dao.LaborConfig;
import com.ursulagis.desktop.dao.config.Configuracion;
import javafx.beans.property.BooleanProperty;
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
public class CosechaConfig extends LaborConfig{
	/*constantes para el archivo de propiedasdes*/
//	public static final String ANCHO_GRILLA = "anchoGrilla";
//	private static final String CORRECCION_DEMORA_PESADA_KEY = "correccionDemoraPesada";
//	private static final String CORRIMIENTO_PESADA_KEY="corrimientoPesada";
//	private static final String CORRECCION_OUTLAYERS_KEY = "correccionOutlayers";
	private static final String CORRECCION_RINDE_AREA_KEY = "correccionRindeArea";
//	private static final String CORRECCION_SUPERPOSICION_KEY = "correccionSuperposicion";
//	private static final String CORRECCION_DISTANCIA_KEY = "correccionDistancia";
//	private static final String CORRECCION_ANCHO_KEY = "correccionAncho";
//	private static final String ANCHO_OUTLAYERS_KEY = "anchoFiltroOutlayers";
//	private static final String MAX_GEOMETRYES_KEY = "cantMaxGeometriasSuperpuestas";
//	
//	private static final String MAX_RINDE_KEY = "rindeMaximioCultivo";
	
//	private static final String RESYMIR_GEOMETRIAS_KEY = "RESUMIR_GEOMETRIAS";
//	//1 acre	43.560sq ft (survey) o 10 sq ch	4046,873m 2
//	private static final String METROS_POR_UNIDAD_DIST_KEY = "metrosPorUnidadDistancia";//1 pie =	0,3048 metros //1 pulgadas =0,0254 metros
	
	
	/*
	Avena:Estados Unidos: 32 lb = 14,51495584 kg
	Avena:Canad�: 34 lb = 15,42214058 kg
	Cebada: 48 lb = 21,77243376 kg
	Cebada Malteada: 34 lb = 15,42214058 kg
	Ma�z: 56 lb = 25,40117272 kg
	Trigo y porotos de soja: 60 lb = 27,2155422 kg 
	
	
	rinde = mas_flow/v*width
	 */
	private static final String CORRECCION_FLOW_TO_RINDE = "CORRECCION_FLOW_TO_RINDE";
	private static final String CALIBRAR_RINDE_KEY = "CALIBRAR_RINDE";
	
	/*las propiedades*/
	
	private BooleanProperty correccionRindeProperty;
	private BooleanProperty calibrarRindeProperty;

	//private Property<Double>  valorCorreccionAnchoProperty;

	private BooleanProperty correccionFlowToRindeProperty;
	//private IntegerProperty  cantDistanciasEntradaRegimenProperty;
	/**
	 * hace referencia al archivo donde se guardan las configuraciones
	 */
	//Configuracion config=null;
	
	public CosechaConfig(){
		super(Configuracion.getInstance());
		//Configuracion config =  super.getConfigProperties();//levanto el archivo de propiedades default pero puedo guardarlo en otro archivo seteando el fileURL
		//DecimalFormat df = new DecimalFormat("#.0000");
		try{

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
		 
		}catch(Exception e){
			e.printStackTrace();
		}
		 
	}
	
	/**
	 * devuelvo las propiedades para que puedan ser vinculadas con los componentes.
	 * @return
	 */

	public BooleanProperty correccionRindeProperty() { return correccionRindeProperty;}
	public BooleanProperty calibrarRindeProperty() { return calibrarRindeProperty;}

	

//	public IntegerProperty clasesClasificadorProperty() { return clasesClasificadorProperty;
//	}



	public boolean correccionRindeAreaEnabled() {
		return correccionRindeProperty().getValue();
		// return "true".equals(configProp.getProperty(CORRECCION_RINDE_AREA));
	}

	public Property<Boolean> correccionFlowToRindeProperty() {
		return correccionFlowToRindeProperty;
	}
}
