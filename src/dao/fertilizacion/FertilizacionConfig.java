package dao.fertilizacion;

import dao.config.Configuracion;
import dao.cosecha.CosechaConfig;
import javafx.beans.property.Property;

public class FertilizacionConfig extends CosechaConfig {
//TODO agregar las keys a las propiedades especificas de la labor de fertilizacion
	//ej: costo pasada, precioFert
	/**
	 * hace referencia al archivo donde se guardan las configuraciones
	 */
	Configuracion config;
	public FertilizacionConfig(){
	super();
	config = Configuracion.getInstance();//levanto el archivo de propiedades default pero puedo guardarlo en otro archivo seteando el fileURL
	}
	
	
	public void save(){
		config.save();
	}


	
}
