package tasks;

import dao.Configuracion;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;



public class HarvestFiltersConfig {
	
	private static final String CORRECCION_DEMORA_PESADA = "correccionDemoraPesada";
	private static final String CORRECCION_OUTLAYERS = "correccionOutlayers";
	private static final String CORRECCION_RINDE_AREA = "correccionRindeArea";
	private static final String CORRECCION_SUPERPOSICION = "correccionSuperposicion";
	private static final String CORRECCION_DISTANCIA = "correccionDistancia";
	private static final String CORRECCION_ANCHO = "correccionAncho";
	private static final String ANCHO_OUTLAYERS = "anchoFiltroOutlayers";
	private static final String MAX_GEOMETRYES = "cantMaxGeometriasSuperpuestas";
	//1 acre	43.560sq ft (survey) o 10 sq ch	4046,873m 2
	private static final String METROS_POR_UNIDAD_DIST = "metrosPorUnidadDistancia";//1 pie =	0,3048 metros //1 pulgadas =0,0254 metros
	private Property<Boolean> correccionAnchoProperty;
	private Property<Boolean> correccionDistanciaProperty;
	private Property<Boolean> correccionSuperposicionProperty;
	private Property<Boolean> correccionRindeProperty;
	private Property<Boolean> correccionOutlayersProperty;
	private Property<Boolean> correccionDemoraProperty;
	private Property<Number> anchoFiltroOutlayersProperty;
	
	// Bill Pugh Solution for singleton pattern
	private static class LazyHolder {
		private static final HarvestFiltersConfig INSTANCE = new HarvestFiltersConfig();
		
		
	}
	
	public static HarvestFiltersConfig getInstance() {
		return LazyHolder.INSTANCE;
	}
	
	public HarvestFiltersConfig(){
		Configuracion config = Configuracion.getInstance();
		
		correccionAnchoProperty = new SimpleBooleanProperty(
				"true".equals(config.getProperty(CORRECCION_ANCHO)));
		correccionAnchoProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0,
					Boolean arg1, Boolean arg2) {
				config.setProperty(CORRECCION_ANCHO, arg2.toString());
				
			}
		});

		correccionDistanciaProperty = new SimpleBooleanProperty(
				"true".equals(config.getProperty(CORRECCION_DISTANCIA)));
		correccionDistanciaProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0,
					Boolean arg1, Boolean arg2) {
				config.setProperty(CORRECCION_DISTANCIA, arg2.toString());
				
			}
		});
		correccionSuperposicionProperty = new SimpleBooleanProperty(
				"true".equals(config.getProperty(CORRECCION_SUPERPOSICION)));
		correccionSuperposicionProperty
				.addListener(new ChangeListener<Boolean>() {
					@Override
					public void changed(
							ObservableValue<? extends Boolean> arg0,
							Boolean arg1, Boolean arg2) {
						config.setProperty(CORRECCION_SUPERPOSICION,
								arg2.toString());
						
					}
				});

		correccionRindeProperty = new SimpleBooleanProperty(
				"true".equals(config.getProperty(CORRECCION_RINDE_AREA)));
		correccionRindeProperty
				.addListener(new ChangeListener<Boolean>() {
					@Override
					public void changed(
							ObservableValue<? extends Boolean> arg0,
							Boolean arg1, Boolean arg2) {
						config.setProperty(CORRECCION_RINDE_AREA,
								arg2.toString());
						
					}
				});

		correccionOutlayersProperty = new SimpleBooleanProperty(
				"true".equals(config.getProperty(CORRECCION_OUTLAYERS)));
		correccionOutlayersProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0,
					Boolean arg1, Boolean arg2) {
				config.setProperty(CORRECCION_OUTLAYERS, arg2.toString());
				
			}
		});
		
		anchoFiltroOutlayersProperty = new SimpleDoubleProperty(Double.valueOf(config.getProperty(ANCHO_OUTLAYERS)));
		

		correccionDemoraProperty = new SimpleBooleanProperty(
				"true".equals(config.getProperty(CORRECCION_DEMORA_PESADA)));
		
		
		
		
		correccionDemoraProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0,
					Boolean arg1, Boolean arg2) {
				config.setProperty(CORRECCION_DEMORA_PESADA, arg2.toString());
				
			}
		});
		
		
	}
	/**
	 * correccionAncho=true correccionDistancia=true
	 * correccionSuperposicion=true correccionRindeArea=true
	 * correccionOutlayers=true correccionDemoraPesada=true
	 * 
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
		return correccionDemoraProperty.getValue();
		// return
		// "true".equals(configProp.getProperty(CORRECCION_DEMORA_PESADA));
	}
	
	public Property<Boolean> correccionAnchoProperty() {
		return correccionAnchoProperty;
	}

	public Property<Boolean> correccionDistanciaProperty() {
		return correccionDistanciaProperty;
	}

	public Property<Boolean> correccionSuperposicionProperty() {
		return correccionSuperposicionProperty;
	}

	public Property<Boolean> correccionRindeProperty() {
		return correccionRindeProperty;
	}

	public Property<Boolean> correccionOutlayersProperty() {
		return correccionOutlayersProperty;
	}

	public Property<Boolean> correccionDemoraProperty() {
		return correccionDemoraProperty;
	}

	public double getAnchoFiltroOutlayers() {
		return (double) anchoFiltroOutlayersProperty.getValue();
	}
	
	public double getMetrosPorUnidadDistancia(){
		Configuracion config = Configuracion.getInstance();
		String max = config.getPropertyOrDefault(METROS_POR_UNIDAD_DIST, "1");
		double iMax= Double.parseDouble(max);
		return iMax; 
	}

	public int getMAXGeometries() {
		Configuracion config = Configuracion.getInstance();
		String max = config.getPropertyOrDefault(MAX_GEOMETRYES, "20");
		int iMax= Integer.parseInt(max);
		return iMax;
	}
}
