package com.ursulagis.desktop.dao.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.ursulagis.desktop.utils.CustomProperties;

/**
 * clase util para acceder al archivo de configuracion persistente en el sistema.
 * el usuario debe construir la instancia leer, modificar y guardar los cambios realizados. 
 * si quere volver a leer puede actualizar la instancia llamando a loadProperties()
 * no asegura que este acutalizado contra el archivo config.properties
 * @author tomas
 *
 */


public class Configuracion{
	public static final String URSULA_GIS_APPDATA_FOLDER = "UrsulaGIS";
	public static final String APPDATA = "APPDATA";
	private static final String DEFAULT_CONFIG_PROPERTIES = "com/ursulagis/desktop/dao/config/config.properties";
	private static final String FILE_CONFIG_PROPERTIES = "config.properties";
	private final CustomProperties configProp = new CustomProperties();
	
	private String propertiesFileUrl = FILE_CONFIG_PROPERTIES;
	public static String ursulaGISFolder = getUrsulaGISFolder();
	//public static SimpleBooleanProperty modified = new SimpleBooleanProperty();

	//private static final String GENERAR_MAPA_RENTABILIDAD_FROM_SHP = "generarMapaRentabilidadFromShp";
	public static final String LAST_FILE = "LAST_FILE";
	
	//private Property<Boolean> generarMapaRentabilidadFromShpProperty = new SimpleBooleanProperty();

	private Configuracion(String propertiesUrl) {		
		this.propertiesFileUrl=propertiesUrl;
		loadProperties();
	}

	private static String getUrsulaGISFolder() {
		String currentUsersHomeDir =System.getenv(APPDATA);
		//System.out.println("obtuve la direccion de appData : "+currentUsersHomeDir);
		//obtuve la direccion de appData : C:\Users\quero\AppData\Roaming
		String ursulaGISFolder = currentUsersHomeDir + File.separator + URSULA_GIS_APPDATA_FOLDER;
		 return ursulaGISFolder;
	}
	private Configuracion() {		
		String currentUsersHomeDir =System.getenv(APPDATA);
		//System.out.println("obtuve la direccion de appData : "+currentUsersHomeDir);
		//obtuve la direccion de appData : C:\Users\quero\AppData\Roaming
		 ursulaGISFolder = currentUsersHomeDir + File.separator + URSULA_GIS_APPDATA_FOLDER;
		 //seteo el path para el log de objectdb
		  System.setProperty("objectdb.home", ursulaGISFolder); //TODO remove. no se usa mas
		this.propertiesFileUrl=ursulaGISFolder+ File.separator +FILE_CONFIG_PROPERTIES;
		File propF =new File(propertiesFileUrl);
		try {
			propF.getParentFile().mkdirs();
			propF.createNewFile();
		} catch (IOException e) {
			System.err.println("no pude crear el config.properties");
			e.printStackTrace();
		}
		loadProperties();
	
//		generarMapaRentabilidadFromShpProperty.setValue(
//				"true".equalsIgnoreCase(configProp
//						.getProperty(GENERAR_MAPA_RENTABILIDAD_FROM_SHP)));
//		
//		generarMapaRentabilidadFromShpProperty
//				.addListener(( ov, o,  n) ->{
//					this.setProperty(GENERAR_MAPA_RENTABILIDAD_FROM_SHP, n.toString());					
//				});		
		
//		System.out.println("finished loading config");
	//	modified();
	}

	public void loadProperties() {
		//System.out.println("loading properties");
		
		boolean success = false;
		try {
			// Use UTF-8 encoding for reading properties files
			FileInputStream fis = new FileInputStream(propertiesFileUrl);
			InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
			configProp.load(reader);
			success = true;
			reader.close();
			fis.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		if (!success) {
			InputStream in = this.getClass().getResourceAsStream(
					DEFAULT_CONFIG_PROPERTIES);
		//	propertiesFileUrl=DEFAULT_CONFIG_PROPERTIES;
			System.out.println("Read all properties from file");
			try {
				// Use UTF-8 encoding for reading default properties
				configProp.load(new InputStreamReader(in, StandardCharsets.UTF_8));
				System.out.println("finished loading defaults");
			} catch (IOException e) {
				System.out.println("failed to load default configuration");
				e.printStackTrace();
			}
		}
	}

	// Bill Pugh Solution for singleton pattern
//	private static class LazyHolder {
//		//FIXME al hacer esto y bindear todas las propiedades con la configuracion hice que efectivamente todas las labores tengan la misma configuracion
//		//FIXME cambiar los bindings por sets al momento de hacer el save?
//		public static Configuracion INSTANCE = new Configuracion();
//	}

	public static Configuracion getInstance() {
		try {
			return  new Configuracion();
		}catch(Exception e) {
			System.out.println("error al conseguir la instancia de configuracion");
			return null;
		}
		//return LazyHolder.INSTANCE;
	}

	
	public String getPropertyOrDefault(String key,String def) {
		String ret = configProp.getProperty(key);
		if(ret == null){
			setProperty(key,def);//save?
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
		//loadProperties();//load before set.
		configProp.setProperty(key, value);//FIXME null pointer exception si key no existe? lastFile en exportar chart

	}

	public void save() {
		try {
			// Use UTF-8 encoding for writing properties files
			FileOutputStream fos = new FileOutputStream(propertiesFileUrl);
			OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			configProp.store(writer,URSULA_GIS_APPDATA_FOLDER);//Calendar.getInstance().getTime().toString());
			writer.close();
			fos.close();
		} catch (IOException e) {
			System.err.println("fallo el guardar config.properties en "+propertiesFileUrl);
			e.printStackTrace();
		}
	}
}
