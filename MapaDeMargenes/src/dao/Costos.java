package dao;

public class Costos {
	private static final String COSTO_LABOR_PULVERIZACION = "costoLaborPulverizacion";
	private static final String COSTO_LABOR_SIEMBRA = "costoLaborSiembra";
	private static final String PRECIO_SEMILLA = "precioSemilla";
	private static final String COSTO_LABOR_FERTILIZACION = "costoLaborFertilizacion";
	private static final String PRECIO_FERTILIZANTE = "precioFertilizante";
	private static final String PRECIO_GRANO = "precioGrano";

	public static String getPrecioFertilizante(){
		return Configuracion.getInstance().getProperty(PRECIO_FERTILIZANTE);
	}
	
	public static void setPrecioFertilizante(String newVal) {
		Configuracion conf = Configuracion.getInstance();
		conf.setProperty(PRECIO_FERTILIZANTE, newVal);
		conf.save();		
	}
	
	public static String getCostoLaborFert(){
		return Configuracion.getInstance().getProperty(COSTO_LABOR_FERTILIZACION);
	}
	
	public static void setCostoLaborFert(String newVal) {
		Configuracion conf = Configuracion.getInstance();
		conf.setProperty(COSTO_LABOR_FERTILIZACION, newVal);
		conf.save();
		
	}
	
	public static String getPrecioSemilla(){
		return Configuracion.getInstance().getProperty(PRECIO_SEMILLA);
	}
	
	public static void setPrecioSemilla(String newVal) {
		Configuracion conf = Configuracion.getInstance();
		conf.setProperty(PRECIO_SEMILLA, newVal);
		conf.save();		
	}
	
	public static String getCostoLaborSiembra(){
		return Configuracion.getInstance().getProperty(COSTO_LABOR_SIEMBRA);
	}
	
	public static void setCostoLaborSiembra(String newVal) {
		Configuracion conf = Configuracion.getInstance();
		conf.setProperty(COSTO_LABOR_SIEMBRA, newVal);
		conf.save();	
		
	}
	
	public static String getCostoLaborPulverizacion(){
		return Configuracion.getInstance().getProperty(COSTO_LABOR_PULVERIZACION);
	}
	
	public static void setCostoLaborPulverizacion(String newVal) {	
		Configuracion conf = Configuracion.getInstance();
		conf.setProperty(COSTO_LABOR_PULVERIZACION, newVal);
		conf.save();
	}
	
	public static String getPrecioGrano(){
		return Configuracion.getInstance().getProperty(PRECIO_GRANO);
	}

	public static void setPrecioGrano(String newVal) {
		Configuracion conf = Configuracion.getInstance();
		conf.setProperty(PRECIO_GRANO, newVal);
		conf.save();
	}
}
