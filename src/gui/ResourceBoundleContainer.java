package gui;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

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

		URL path = this.getClass().getResource(resourceName);
		System.out.println("boundle file found at "+path);
		InputStream is = this.getClass().getResourceAsStream(resourceName);
		if(is!=null) {
			try {
				RESOURCE_BUNDLE=new TxtResourceBundle(is);
				//RESOURCE_BUNDLE = ResourceBundle.getBundle(bUNDLE_NAME,locale, Messages.class.getClassLoader());
			} catch (Exception e) {
				//TODO si el recurso no existe traducir el espaniol al idioma deseado,
				//guardarlo en el directorio local y devolver el boundle apuntando al nuevo recurso
				//XXX podemos crear una api en ursula? de esa manera cacheamos los ya generados
				e.printStackTrace();
			}
		} 

			//RESOURCE_BUNDLE = ResourceBundle.getBundle(bUNDLE_NAME,locale,Messages.class.getClassLoader());
	}

	private static class TxtResourceBundle extends ResourceBundle {
		private Properties props;

		public TxtResourceBundle(InputStream stream) throws IOException {
			props = new Properties();


			try {
				props.load(stream);
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
		@Override
		public Enumeration<String> getKeys() {
			return (Enumeration<String>) props.propertyNames();
		}

	}


}
