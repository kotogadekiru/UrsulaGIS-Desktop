package dao;


import javafx.beans.property.SimpleDoubleProperty;

public class Costos {
	private static final String COSTO_FIJO_POR_HA = "COSTO_FIJO_POR_HA";
	private static final String COSTO_LABOR_PULVERIZACION = "costoLaborPulverizacion";
	private static final String COSTO_LABOR_SIEMBRA = "costoLaborSiembra";
	private static final String PRECIO_SEMILLA = "precioSemilla";
	private static final String COSTO_LABOR_FERTILIZACION = "costoLaborFertilizacion";
	private static final String PRECIO_FERTILIZANTE = "precioFertilizante";
	private static final String PRECIO_GRANO = "precioGrano";
	private static final String CORRECCION_COSECHA = "CORRECCION_COSECHA";

	public  final SimpleDoubleProperty precioGranoProperty = new SimpleDoubleProperty(getPrecioGrano());
	public  final SimpleDoubleProperty precioFertProperty= new SimpleDoubleProperty(getPrecioFertilizante());
	public  final SimpleDoubleProperty precioLabFertProperty= new SimpleDoubleProperty(getCostoLaborFert());
	public  final SimpleDoubleProperty precioPulvProperty= new SimpleDoubleProperty(getCostoLaborPulverizacion());
	public  final SimpleDoubleProperty precioSiembraProperty= new SimpleDoubleProperty(getCostoLaborSiembra());
	public  final SimpleDoubleProperty precioSemillaProperty= new SimpleDoubleProperty(getPrecioSemilla());
	public  final SimpleDoubleProperty costoFijoHaProperty= new SimpleDoubleProperty(getCostoFijoHa());
	public  final SimpleDoubleProperty correccionCosechaProperty= new SimpleDoubleProperty(getCorreccionCosecha());


	private static class LazyHolder {
		public static Costos INSTANCE = new Costos();
	}
	public static Costos getInstance() {
		return LazyHolder.INSTANCE;
	}

	private Costos(){
		System.out.println("inicializando Costos");
		Configuracion conf = Configuracion.getInstance();
		
		Configuracion.modified.addListener((ov,o,n)->{initProperties();});
		//XXX ojo puede haber una situacion de carrera circular
		precioFertProperty.addListener((ov,o,n)->{conf.setProperty(PRECIO_FERTILIZANTE, n.toString());});		
		precioGranoProperty.addListener((ov,o,n)->{conf.setProperty(PRECIO_GRANO, n.toString());});
		precioLabFertProperty.addListener((ov,o,n)->{conf.setProperty(COSTO_LABOR_FERTILIZACION, n.toString());});
		precioPulvProperty.addListener((ov,o,n)->{conf.setProperty(COSTO_LABOR_PULVERIZACION, n.toString());});
		precioSiembraProperty.addListener((ov,o,n)->{conf.setProperty(COSTO_LABOR_SIEMBRA, n.toString());});		
		precioSemillaProperty.addListener((ov,o,n)->{conf.setProperty(PRECIO_SEMILLA, n.toString());});
		costoFijoHaProperty.addListener((ov,o,n)->{conf.setProperty(COSTO_FIJO_POR_HA, n.toString());});
		correccionCosechaProperty.addListener((ov,o,n)->{conf.setProperty(CORRECCION_COSECHA, n.toString());});
	}

	private void initProperties() {
		precioGranoProperty.setValue(getPrecioGrano());
		precioFertProperty.setValue(getPrecioFertilizante());
		precioLabFertProperty.setValue(getCostoLaborFert());
		precioPulvProperty.setValue(getCostoLaborPulverizacion());
		precioSiembraProperty.setValue(getCostoLaborSiembra());
		precioSemillaProperty.setValue(getPrecioSemilla());
		costoFijoHaProperty.setValue(getCostoFijoHa());
		correccionCosechaProperty.setValue(getCorreccionCosecha());
	}

	private static Double getPrecioFertilizante(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(PRECIO_FERTILIZANTE,"0"));
	}

	//	public static void setPrecioFertilizante(String newVal) {
	//		Configuracion conf = Configuracion.getInstance();
	//		conf.setProperty(PRECIO_FERTILIZANTE, newVal);
	//		conf.save();		
	//	}

	private static Double getCostoLaborFert(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(COSTO_LABOR_FERTILIZACION,"0"));
	}

	//	public static void setCostoLaborFert(String newVal) {
	//		Configuracion conf = Configuracion.getInstance();
	//		conf.setProperty(COSTO_LABOR_FERTILIZACION, newVal);
	//	}

	private static Double getPrecioSemilla(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(PRECIO_SEMILLA,"0"));
	}

	//	public static void setPrecioSemilla(String newVal) {
	//		Configuracion conf = Configuracion.getInstance();
	//		conf.setProperty(PRECIO_SEMILLA, newVal);	
	//	}

	private static Double getCostoLaborSiembra(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(COSTO_LABOR_SIEMBRA,"0"));
	}

	//	public static void setCostoLaborSiembra(String newVal) {
	//		Configuracion conf = Configuracion.getInstance();
	//		conf.setProperty(COSTO_LABOR_SIEMBRA, newVal);
	//	}

	private static Double getCostoLaborPulverizacion(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(COSTO_LABOR_PULVERIZACION,"0"));
	}

	//	public static void setCostoLaborPulverizacion(String newVal) {	
	//		Configuracion conf = Configuracion.getInstance();
	//		conf.setProperty(COSTO_LABOR_PULVERIZACION, newVal);
	//	}

	private static Double getPrecioGrano(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(PRECIO_GRANO,"0"));
	}

	//	public static void setPrecioGrano(String newVal) {
	//		Configuracion conf = Configuracion.getInstance();
	//		conf.setProperty(PRECIO_GRANO, newVal);
	//	}
	//
	//	public static void setCostoFijoHa(String newVal) {
	//		Configuracion conf = Configuracion.getInstance();
	//		conf.setProperty(COSTO_FIJO_POR_HA, newVal);		
	//	}
	private static Double getCostoFijoHa(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(COSTO_FIJO_POR_HA,"0"));
	}

	private static Double getCorreccionCosecha(){
		return new Double(Configuracion.getInstance().getPropertyOrDefault(CORRECCION_COSECHA,"100"));
	}

}
