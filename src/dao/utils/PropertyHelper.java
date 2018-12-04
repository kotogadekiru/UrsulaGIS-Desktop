package dao.utils;

import java.util.List;

import dao.config.Configuracion;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class PropertyHelper {
	public static SimpleDoubleProperty initDoubleProperty(String key,String def,Configuracion properties){
		SimpleDoubleProperty doubleProperty = new SimpleDoubleProperty(
				initDouble(key,def,properties)
				//Double.parseDouble(properties.getPropertyOrDefault(key, def))
				);
		doubleProperty.addListener((obs, bool1, bool2) -> {

			properties.setProperty(key,	bool2.toString());
		});
		return doubleProperty;
	}
	
	public static Double initDouble(String key,String def,Configuracion properties){	
				 return Double.parseDouble(properties.getPropertyOrDefault(	key, def));
	}
	
	/**
	 * 
	 * @param key el valor por defecto para la propiedad
	 * @param properties un objeto Configuracion a ser modificado
	 * @param availableColums la lista de opsiones para configurar las propiedades
	 * @return devuelve una nueva StringProperty inicializada con el valor correspondiente de las availableColums o la key proporcionada
	 */
	public static SimpleStringProperty initStringProperty(String key,Configuracion properties,List<String> availableColums){
		SimpleStringProperty sProperty = new SimpleStringProperty(properties.getPropertyOrDefault(key, key));

		if(availableColums!=null && !availableColums.contains(sProperty.get()) && availableColums.contains(key)){
			sProperty.setValue(key);
		}

		sProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(key,	bool2.toString());
		});
		return sProperty;
	}

}
