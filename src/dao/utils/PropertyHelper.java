package dao.utils;

import java.util.ArrayList;
import java.util.Comparator;
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
	 * @param availableColums la lista de opciones para configurar las propiedades
	 * @return devuelve una nueva StringProperty inicializada con el valor correspondiente de las availableColums o la key proporcionada
	 */
	public static SimpleStringProperty initStringProperty(String key,Configuracion properties,List<String> availableColums){
		SimpleStringProperty sProperty = new SimpleStringProperty(properties.getPropertyOrDefault(key, key));

		//		availableColums.sort(new Comparator<String>() {
		//			@Override
		//			public int compare(String o1, String o2) {
		//				int c1 = configuredValue.compareTo(o1);
		//				int c2 = configuredValue.compareTo(o2);
		//				return Integer.compare(c1, c2);
		//			}
		//		});

		String configuredValue=sProperty.get();
		if(availableColums!=null && !availableColums.contains(configuredValue) && availableColums.contains(key)){
			sProperty.setValue(key);
		} else {
			List<String> l=new ArrayList<String>(availableColums);
			l.add(configuredValue);
			l.sort((a,b)->{
				return a.compareTo(b);
			});
			availableColums.sort((a,b)->{
				return a.compareTo(b);
			});
			int index = l.indexOf(configuredValue);
			//System.out.println("buscando lo mas parecido a "+configuredValue);
			//System.out.println("en "+String.join(", ", l));
			//System.out.println("en "+String.join(", ", availableColums));
			//System.out.println("en el indice "+index);
			String closest = configuredValue;
			if(index>=0 && index<availableColums.size()) {
				closest = availableColums.get(index);//si es el ultimo da error
			} else if(index >= availableColums.size()){
				closest = availableColums.get(availableColums.size()-1);//el ultimo
						
			}
//			for(String column : availableColums) {//columnas esta ordenado en orden alfabetico
//				
//				if(closest==null) {
//					closest=column;
//				} else {
//					int c1 =configuredValue.compareToIgnoreCase(closest);
//					int c2 =configuredValue.compareToIgnoreCase(column);//tengo que elegir el mas parecido a cero
//					System.out.println("comparando "+closest+": "+c1+" "+column+": "+c2);
//					if(Math.abs(c1)>Math.abs(c2)) {
//						closest=column;
//					}
//				}	
//			}
			sProperty.setValue(closest);
		}

		sProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(key,	bool2.toString());
		});
		return sProperty;
	}

}
