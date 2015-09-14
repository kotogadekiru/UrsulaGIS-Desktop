package dao;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Date;
import java.util.Calendar;
import java.util.Properties;
import java.util.Set;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class Configuracion {


	private static final String DEFAULT_CONFIG_PROPERTIES = "/dao/config.properties";
	private static final String FILE_CONFIG_PROPERTIES = "config.properties";
	private final Properties configProp = new Properties();

	private static final String GENERAR_MAPA_RENTABILIDAD_FROM_SHP = "generarMapaRentabilidadFromShp";
	public static final String LAST_FILE = "LAST_FILE";
	
	private Property<Boolean> generarMapaRentabilidadFromShpProperty;


	private Configuracion() {
		boolean success = false;

		try {
			FileReader reader = new FileReader(FILE_CONFIG_PROPERTIES);
			configProp.load(reader);
			success = true;
		} catch (Exception e1) {
			// TODO Auto-generated catch block
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
		
		System.out.println("loading properties");

	
		

		generarMapaRentabilidadFromShpProperty = new SimpleBooleanProperty(
				"true".equals(configProp
						.getProperty(GENERAR_MAPA_RENTABILIDAD_FROM_SHP)));
		generarMapaRentabilidadFromShpProperty
				.addListener(new ChangeListener<Boolean>() {
					@Override
					public void changed(
							ObservableValue<? extends Boolean> arg0,
							Boolean arg1, Boolean arg2) {
						configProp.put(GENERAR_MAPA_RENTABILIDAD_FROM_SHP,
								arg2.toString());
						save();
					}
				});
		
		System.out.println("finished loading config");

	}

	// Bill Pugh Solution for singleton pattern
	private static class LazyHolder {
		private static final Configuracion INSTANCE = new Configuracion();
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
		configProp.setProperty(key, value);
		save();
	}

	

	public boolean generarMapaRentabilidadFromShp() {
		return generarMapaRentabilidadFromShpProperty.getValue();
		// return
		// "true".equals(configProp.getProperty(GENERAR_MAPA_RENTABILIDAD_FROM_SHP));
	}

	public void save() {
		try {
			// URL url = this.getClass().getResource(DAO_CONFIG_PROPERTIES);
			// OutputStream os = new FileOutputStream(url.toExternalForm());
			configProp.store(new FileWriter(FILE_CONFIG_PROPERTIES), Calendar
					.getInstance().getTime().toString());
			// os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Property<Boolean> generarMapaRentabilidadFromShpProperty() {
		return generarMapaRentabilidadFromShpProperty;
	}
	
	public static void main(String[] args) {
		// Get individual properties
		System.out
				.println(Configuracion.getInstance().getProperty("firstName"));
		System.out.println(Configuracion.getInstance().getProperty("lastName"));

		// All property names
		System.out.println(Configuracion.getInstance().getAllPropertyNames());

		Configuracion cache = Configuracion.getInstance();
		if (cache.containsKey("country") == false) {
			cache.setProperty("country", "INDIA");
		}
		// Verify property
		System.out.println(cache.getProperty("country"));
	}



}

/**
 * firstName=Lokesh lastName=Gupta blog=howtodoinjava
 * technology=javafirstName=Lokesh lastName=Gupta blog=howtodoinjava
 * technology=java
 */
