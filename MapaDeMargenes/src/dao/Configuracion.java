package dao;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

public class Configuracion {
	private static final String DEFAULT_CONFIG_PROPERTIES = "/dao/config.properties";
	private static final String FILE_CONFIG_PROPERTIES = "config.properties";
	private final Properties configProp = new Properties();
	public static SimpleBooleanProperty modified = new SimpleBooleanProperty();

	private static final String GENERAR_MAPA_RENTABILIDAD_FROM_SHP = "generarMapaRentabilidadFromShp";
	public static final String LAST_FILE = "LAST_FILE";
	
	private Property<Boolean> generarMapaRentabilidadFromShpProperty = new SimpleBooleanProperty();


	private Configuracion() {		
		loadProperties();
	
		generarMapaRentabilidadFromShpProperty.setValue(
				"true".equalsIgnoreCase(configProp
						.getProperty(GENERAR_MAPA_RENTABILIDAD_FROM_SHP)));
		
		generarMapaRentabilidadFromShpProperty
				.addListener(( ov, o,  n) ->{
					this.setProperty(GENERAR_MAPA_RENTABILIDAD_FROM_SHP, n.toString());					
				});		
		System.out.println("finished loading config");
		modified();
	}

	public void loadProperties() {
		System.out.println("loading properties");
		
		boolean success = false;
		try {
			FileReader reader = new FileReader(FILE_CONFIG_PROPERTIES);
			configProp.load(reader);
			success = true;
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		if (!success) {
			InputStream in = this.getClass().getResourceAsStream(
					DEFAULT_CONFIG_PROPERTIES);
			System.out.println("Read all properties from file");
			try {
				configProp.load(in);
				System.out.println("finished loading defaults");
			} catch (IOException e) {
				System.out.println("failed to load default configuration");
				e.printStackTrace();
			}
		}
	}
	
	public void modified() {
		modified.setValue(!modified.getValue());
	}

	// Bill Pugh Solution for singleton pattern
	private static class LazyHolder {
		public static Configuracion INSTANCE = new Configuracion();
	}

	public static Configuracion getInstance() {
		return LazyHolder.INSTANCE;
	}
	/**
	 * si la clave no existe devuelve null
	 * @param String el nombre de la propiedad a buscar 
	 * @return String el valor de la propiedad buscada 
	 */
    @Deprecated 
	public String getProperty(String key) {
    	return getPropertyOrDefault(key,"0");
	}
	
	public String getPropertyOrDefault(String key,String def) {
		String ret = configProp.getProperty(key);
		if(ret == null){
			setProperty(key,def);
			ret = def;
		}
		return ret;
	}

	public Set<String> getAllPropertyNames() {
		return configProp.stringPropertyNames();
	}

	public boolean containsKey(String key) {
		return configProp.containsKey(key);
	}

	public void setProperty(String key, String value) {
		loadProperties();
		configProp.setProperty(key, value);
		save();
	}

	public boolean getGenerarMapaRentabilidadFromShp() {
		return generarMapaRentabilidadFromShpProperty.getValue();
	}	

	public Property<Boolean> generarMapaRentabilidadFromShpProperty() {
		return generarMapaRentabilidadFromShpProperty;
	}

	public void save() {
		try {
			configProp.store(new FileWriter(FILE_CONFIG_PROPERTIES),"MarginMapGenerator");//Calendar.getInstance().getTime().toString());
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		modified();
	}
	

}
