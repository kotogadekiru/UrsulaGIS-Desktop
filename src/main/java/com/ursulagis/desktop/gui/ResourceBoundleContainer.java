package com.ursulagis.desktop.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.tasks.GoogleTranslatorHelper;

//Clase que permite tener el mismo resourceBoundle en toda la aplicacion y cambiar el lenguaje en tiempo de ejecucion
public class ResourceBoundleContainer {

	private	ResourceBundle RESOURCE_BUNDLE =null;
	public ResourceBoundleContainer() {super();}

	public ResourceBundle get() {
		return RESOURCE_BUNDLE;
	}

	public void set(String bUNDLE_NAME,Locale locale) {
		// /gui/messages_en.properties
		String resourceName = bUNDLE_NAME+"_"+locale.getLanguage().toLowerCase()+".properties";

		System.out.println("searching for resourceBoundle in  "+resourceName);		

		//URL path = this.getClass().getResource(resourceName);
		//System.out.println("boundle file found at "+path);
		InputStream is = this.getClass().getResourceAsStream(resourceName);
		if(is!=null) {
			URL path = this.getClass().getResource(resourceName);
			System.out.println("boundle file found at "+path);
			try {
				RESOURCE_BUNDLE=new TxtResourceBundle(is);
				//RESOURCE_BUNDLE = ResourceBundle.getBundle(bUNDLE_NAME,locale, Messages.class.getClassLoader());
			} catch (Exception e) {
				//TODO si el recurso no existe traducir el espaniol al idioma deseado,
				//guardarlo en el directorio local y devolver el boundle apuntando al nuevo recurso
				//XXX podemos crear una api en ursula? de esa manera cacheamos los ya generados
				e.printStackTrace();
			}
		}else {
			System.out.println("boundle file not found at "+resourceName);
		}
		//  else {//locale is not suported by default
		// 	//TODO try to load a local boundle or translate a base bundle
		// 	//TODO first look for messages in C:\Users\<user>\AppData\Roaming\UrsulaGIS\messages_fr.properties
		// 	String fileName = Configuracion.ursulaGISFolder+"\\messages_"+locale.getLanguage()+".properties";
		// 	File localFile = new File(fileName);
		// 	if(!localFile.exists()) {
		// 		ResourceBundle baseBoundle1 = Messages.getBoundle();//verificar que esto no explote
		// 		GoogleTranslatorHelper t = new GoogleTranslatorHelper(baseBoundle1,locale);
		// 		t.run();
		// 		//JFXMain.executorPool.submit(t);
		// 	}
		// 	TxtResourceBundle localBoundle =getLocalResourceBundle(fileName);
		// 	if(localBoundle != null) {
		// 		RESOURCE_BUNDLE = localBoundle;				
		// 	} 
			
		// }
	}

	private static TxtResourceBundle getLocalResourceBundle(String fileName) {
		try {
			FileInputStream  fr = new FileInputStream (fileName);			
			return new TxtResourceBundle(fr);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}		
	}
	private static class TxtResourceBundle extends ResourceBundle {
		private Properties props;

		public TxtResourceBundle(InputStream stream) throws IOException {
			props = new Properties();
			try {
				// Use UTF-8 encoding to properly handle accented characters
				props.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
				System.out.println("finished loading language bundle");
				//props.values().stream().forEach((v)->System.out.println(v));
				
			} catch (Exception e) {
				System.out.println("failed to load language bundle");
				e.printStackTrace();
			}
		}
		protected Object handleGetObject(String key) {
			return props.getProperty(key);
		}
		@SuppressWarnings("unchecked")
		@Override
		public Enumeration<String> getKeys() {
			return (Enumeration<String>) props.propertyNames();
		}

	}


}
