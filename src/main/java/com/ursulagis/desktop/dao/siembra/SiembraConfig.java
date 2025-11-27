package com.ursulagis.desktop.dao.siembra;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.cosecha.CosechaConfig;
import javafx.beans.property.SimpleObjectProperty;


public class SiembraConfig extends CosechaConfig {
	//TODO agregar las keys a las propiedades especificas de la labor de fertilizacion
	//ej: costo pasada, precioFert
	public static enum Unidad {kgHa,milPlaHa,pla10MtLineal,pla1MtLineal,plaMetroCuadrado, Kg, Bolsa}
	private static final String DOSIS_UNIDAD_KEY = "DOSIS_UNIDAD_KEY";
	private static final String INSUMO_UNIDAD_KEY = "INSUMO_UNIDAD_KEY";
	private SimpleObjectProperty<Unidad>  dosisUnitProperty;//property que contiene el factor de conversion
	private SimpleObjectProperty<Unidad>  precioInsumoUnitProperty;//property que contiene el factor de conversion
	/**
	 * hace referencia al archivo donde se guardan las configuraciones
	 */
	//	Configuracion config;
	public SiembraConfig(){
		super();
		if(config == null) {
			System.out.println("iniciando config con la instancia en SiembraConfig porque era null");
			config = Configuracion.getInstance();
		}

		Unidad configuredDosis = Unidad.kgHa;
		String defaultDosisUnit =config.getPropertyOrDefault(DOSIS_UNIDAD_KEY,Unidad.kgHa.name()); 
		if(defaultDosisUnit!=null) {
			System.out.println("default unit Dosis es "+ defaultDosisUnit);
			configuredDosis= Unidad.valueOf(defaultDosisUnit);
		}
		dosisUnitProperty = new SimpleObjectProperty<Unidad>(configuredDosis);
		dosisUnitProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(DOSIS_UNIDAD_KEY, bool2.toString());
		});
		
		Unidad insumoUnidad = Unidad.Kg;
		String defaultinsumoUnidad =config.getPropertyOrDefault(INSUMO_UNIDAD_KEY,Unidad.Kg.name()); 
		if(defaultinsumoUnidad!=null) {
			System.out.println("default unit Dosis es "+ defaultinsumoUnidad);
			insumoUnidad= Unidad.valueOf(defaultinsumoUnidad);
		}
		precioInsumoUnitProperty = new SimpleObjectProperty<Unidad>(insumoUnidad);
		precioInsumoUnitProperty.addListener((obs,bool1,bool2)->{
			config.setProperty(INSUMO_UNIDAD_KEY, bool2.toString());
		});

		//config = Configuracion.getInstance();//levanto el archivo de propiedades default pero puedo guardarlo en otro archivo seteando el fileURL
	}

	public SimpleObjectProperty<SiembraConfig.Unidad> dosisUnitProperty() { return dosisUnitProperty;}

	public SimpleObjectProperty<SiembraConfig.Unidad> precioInsumoUnitProperty() {
		
		return precioInsumoUnitProperty;
	}
}
