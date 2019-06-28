package gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import dao.config.Configuracion;

public class Messages {
	private static final String LOCALE_NOT_SET = "LOCALE_NOT_SET";
	private static final String LOCALE_KEY = "LOCALE_KEY";
	private static final String BUNDLE_NAME ="/gui/messages";//"gui.MyResources";//"gui.messages";// "gui.messages"; //$NON-NLS-1$
	
	private static Configuracion conf = JFXMain.config;
	
	private static Locale locale = new Locale("ES");
	//private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME,new Locale("EN"));
	private static final ResourceBoundleContainer RESOURCE_BUNDLE_CONTAINER = new ResourceBoundleContainer();
	//	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	static {
//		try {
//			RESOURCE_BUNDLE_CONTAINER.set(BUNDLE_NAME,locale);
//		} catch(Exception e ) {
//			e.printStackTrace();
//		}
		String loc = conf.getPropertyOrDefault(LOCALE_KEY, LOCALE_NOT_SET);
		
		Locale defaultLoc = Locale.getDefault();
		System.out.println("default language es \""+defaultLoc.getLanguage()+ "\" supported? "+supports(defaultLoc.getLanguage()));
		if(loc.equals(LOCALE_NOT_SET) && Messages.supports(defaultLoc.getLanguage())) {
			locale=defaultLoc;
		} else if(!loc.equals(LOCALE_NOT_SET) ){
			locale =  new Locale(loc);
		} else {
			locale =  new Locale("ES");
		}
		setLocale(locale);
	}


	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE_CONTAINER.get().getString(key);
		} catch (Exception e) {
			return '!' + key + '!';
		}
	}
	
	public static void setLocale(Locale loc) {
		locale=loc;
		RESOURCE_BUNDLE_CONTAINER.set(BUNDLE_NAME, locale);
		
		conf.setProperty(LOCALE_KEY, locale.getLanguage());
		conf.save();
		
		System.out.println("guardando el nuevo locale "+locale.getLanguage());
	}
	
	public static Locale getLocale() {
		return locale;
	}
	
	public static boolean supports(String loc) {
		for(Locale supported : getLocales()) {
			if(supported.getLanguage().equalsIgnoreCase(loc)) {
				return true;
			}
		}
		return false;
	}
	
	public static ResourceBundle getBoundle() {
	
		
		return RESOURCE_BUNDLE_CONTAINER.get();
	}
	
	public static List<Locale> getLocales(){
		 List<Locale> locales = new ArrayList<Locale>();
		 locales.add(new Locale("ES"));
		 locales.add(new Locale("EN"));
		 locales.add(new Locale("PT"));
		return locales;
	}
}
