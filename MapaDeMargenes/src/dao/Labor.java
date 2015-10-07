package dao;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.geotools.data.FileDataStore;

/**
 * hace las veces de un featureStore con los metodos especificos para manejar el tipo de labor especifico
 * @author tomas
 *
 * @param <E>
 */
public class Labor<E extends FeatureContainer> {
	protected Configuracion config;
	protected Map<String, String> columnsMap= new HashMap<String, String>();
	protected FileDataStore store = null;
	protected String nombre = "Nombre";
	
	public Labor(){
		
	}

	protected  Map<String, String> getColumnsMap(){
		return columnsMap;
	}
	
	public String getColumnName(String internalName){
		return columnsMap.get(internalName);
	}
	
	public  void setColumnsMap(Map<String, String> columns) {
		columnsMap.clear();
		columnsMap.putAll(columns);		
		
		columns.forEach(new BiConsumer<String, String>(){
			@Override
			public void accept(String key, String value) {
				config.setProperty(key, value);				
			}
			
		});
		
	}

}
