package gui;

import java.util.Locale;
import java.util.ResourceBundle;

//Clase que permite tener el mismo resourceBounde en toda la aplicacion y cambiar el lenguaje en tiempo de ejecucion
public class ResourceBoundleContainer {
	
	private	ResourceBundle RESOURCE_BUNDLE =null;
	
	public ResourceBoundleContainer(String bUNDLE_NAME,Locale locale) {
		super();
		RESOURCE_BUNDLE = ResourceBundle.getBundle(bUNDLE_NAME,locale);
	}
		
	public ResourceBundle get() {
		return RESOURCE_BUNDLE;
	}
	
	public void set(String bUNDLE_NAME,Locale locale) {
		RESOURCE_BUNDLE = ResourceBundle.getBundle(bUNDLE_NAME,locale);
	}
}
