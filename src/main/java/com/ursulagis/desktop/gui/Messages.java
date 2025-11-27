package com.ursulagis.desktop.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ursulagis.desktop.dao.config.Configuracion;

public class Messages {
	private static final String LOCALE_NOT_SET = "LOCALE_NOT_SET";
	private static final String LOCALE_KEY = "LOCALE_KEY";
	private static final String BUNDLE_NAME ="messages";//
	//src/main/java/com/ursulagis/desktop/gui/messages_es.properties
	private static Configuracion conf = JFXMain.config;
	
	private static List<Consumer<Locale>> localeChangeListeners=new ArrayList<>();
	
	private static NumberFormat nf =null;
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
		nf=NumberFormat.getInstance(locale);
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(2);
		
		RESOURCE_BUNDLE_CONTAINER.set(BUNDLE_NAME, locale);
		conf.loadProperties();
		conf.setProperty(LOCALE_KEY, locale.getLanguage());
		conf.save();
		
		localeChangeListeners.stream().forEach(f->f.accept(locale));
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
	
	public static void registerLocaleChangeListener(Consumer<Locale> f) {
		localeChangeListeners.add(f);
	}
	
	public static ResourceBundle getBoundle() {
	
		
		return RESOURCE_BUNDLE_CONTAINER.get();
	}
	
	public static List<Locale> getLocales(){
		 List<Locale> locales = new ArrayList<Locale>();
		 locales.add(new Locale("ES"));
		 locales.add(new Locale("EN"));
		 locales.add(new Locale("PT"));
		 locales.add(new Locale("FR"));
		return locales;
	}
	
	public static NumberFormat getNumberFormat() {
		return nf;
	}
	
	public static char getDecimalSeparator() {
		DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Messages.getLocale());
		DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
		char sep=symbols.getDecimalSeparator();
		return sep;
	}
	
	public static void main(String[] args) {
		//TEST NumberFormater
		try {
			Double d = Messages.getNumberFormat().parse("1085").doubleValue();
			System.out.println("1085 es "+d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	

}

 


/* ejemplo de como cargar un bundle en menoria
import java.util.ListResourceBundle;

public class MyResources extends ListResourceBundle {

	@Override
	protected Object[][] getContents() {
		
		return new Object[][] {
            // LOCALIZE THE SECOND STRING OF EACH ARRAY (e.g., "OK")
            {"OkKey", "OK"},
            {"CancelKey", "Cancel"},
            {"JFXMain.importar","Importar"},
            // END OF MATERIAL TO LOCALIZE
       };
	}

}
*/