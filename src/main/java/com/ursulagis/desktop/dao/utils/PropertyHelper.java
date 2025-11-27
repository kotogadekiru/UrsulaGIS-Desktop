package com.ursulagis.desktop.dao.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.utils.DateConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.StringConverter;

public class PropertyHelper {
	
	private static DecimalFormat converter=getDoubleConverter();
	
	public static Number parseDouble(String s) {
		s=fixDecimalSeparator(s);
		Number ret = 0.0;
		try {		ret = getDoubleConverter().parse(s);//Double.valueOf(ppmPOptional.get());
		}catch(Exception e){e.printStackTrace();}
		return ret;
	}
	
	public static String formatDouble(Number n) {
		String ret = new String("0");
		try {		ret = getDoubleConverter().format(n);//Double.valueOf(ppmPOptional.get());
		}catch(Exception e){e.printStackTrace();}
		return ret;
	}
	/**
	 * 
	 * @return StringConverter de Number que arregla los decimales si estan con . en vez de , y vice versa
	 */
	public static StringConverter<Number> buildStringConverter() {
		StringConverter<Number> converter = new StringConverter<Number>() {
		public String toString(Number n) {
			return PropertyHelper.formatDouble(n.doubleValue());				
		}
		
		public Number fromString(String s) {
			Number d = 0.0;
			try {
				d= PropertyHelper.parseDouble(s);
				return d.doubleValue();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;			
		}
	};
		return converter;
	}
	
	/**
	 * devuelve un converter que no arregla los decimales
	 * @return
	 */
	public static DecimalFormat getDoubleConverter() {
		if(converter == null) {
			
//			DecimalFormat df = new DecimalFormat() {
//				public String format(Number n) {
//					return PropertyHelper.formatDouble(n.doubleValue());				
//				}
//				
//				public Number parse(String s) {
//					Number d = 0.0;
//					try {
//						d= PropertyHelper.parseDouble(s);
//						return d.doubleValue();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					return 0;			
//				}
//			};
		
			
			NumberFormat nf = Messages.getNumberFormat();
			
			converter = (DecimalFormat)nf;
			converter.setMinimumFractionDigits(2);
			converter.setGroupingUsed(true);
			converter.setGroupingSize(3);
		}
		return converter;
	}

	public static SimpleDoubleProperty initDoubleProperty(String key,String def,Configuracion properties){
		SimpleDoubleProperty doubleProperty = new SimpleDoubleProperty(
				initDouble(key,def,properties)
				//Double.parseDouble(properties.getPropertyOrDefault(key, def))
				);
		
		doubleProperty.addListener((obs, bool1, bool2) -> {

			properties.setProperty(key,	getDoubleConverter().format(bool2));
		});
		return doubleProperty;
	}

	public static void bindDoubleToTextProperty(Supplier<Double> getDouble,Consumer<Double> setDouble, StringProperty textProperty, Configuracion configuracion, String key ) {
		Double d = getDouble.get();
		if(d!=null) {
			textProperty.set(formatDouble(d));
		}else {
			textProperty.set(configuracion.getPropertyOrDefault(key, "0"));
		}
		textProperty.addListener((obj,old,n)->{		
		//	System.out.println("actualizando el valor a "+n);
				setDouble.accept(parseDouble(n).doubleValue());
				configuracion.setProperty(key, n);		
		});
	}	
	
	public static Double initDouble(String key,String def,Configuracion properties){	
		Double ret = 0.0;
		try {
			ret = parseDouble(properties.getPropertyOrDefault(key, def)).doubleValue();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ret;// converter.parse(properties.getPropertyOrDefault(	key, def)).doubleValue();
		//return Double.parseDouble(properties.getPropertyOrDefault(	key, def));
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
			properties.setProperty(key,	bool2);
		});
		return sProperty;
	}

	public static void bindDateToObjectProperty(
			Supplier<Date> getDate,	Consumer<Date> setDate,
			ObjectProperty<LocalDate> valueProperty,
			Configuracion config, String fechaKey) {
				valueProperty.setValue(DateConverter.asLocalDate(getDate.get()));
				DateConverter dc = new DateConverter(); 
				valueProperty.addListener((obs, bool1, n) -> {
					setDate.accept(DateConverter.asDate(n));			
					config.setProperty(fechaKey,dc.toString(n));			
				});		
	}

	/**
	 * metodo que detecta si el usuario quiso agregar un decimal con el caracter equivocado y lo cambia
	 * 
	 * @param s un string que representa un numero
	 * @return un string que representa el mismo numero pero con el caracter de decimales acorde al locale seleccionado
	 */
	public static String fixDecimalSeparator(String s) {		
		DecimalFormat format=PropertyHelper.getDoubleConverter();
		DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
		char decimal=symbols.getDecimalSeparator();
		char grouping = symbols.getGroupingSeparator();
		String ret = s;
		int decIndex = s.lastIndexOf(decimal); //-1 si no existe
		int groupIndex = s.lastIndexOf(grouping);
		String [] groups =s.split(Character.toString(grouping)); 
		int groupCount = groups.length;
		//caso mas simple. si groupIndex es > dec Index estan invertidos
		if(groupIndex>decIndex || 
				(decIndex==-1 && groupIndex>-1 && groupCount==2 )) {//solo tiene grouping
			ret = replaceGroupingDecimal(decimal, grouping, ret);
		}
		
		return ret;
	}

	public static String replaceGroupingDecimal(char decimal, char grouping, String ret) {
		ret = ret.replace(decimal, 'd');
		ret = ret.replace(grouping,decimal);
		ret = ret.replace('d', grouping);
		return ret;
	}
	
//	public static void initDateProperty(String key,String def,Configuracion properties) {
//		DateConverter dc = new DateConverter(); 		
//		String defaultDate = properties.getPropertyOrDefault(key,	def);	
//		//LocalDate ld = dc.fromString(dc.toString(LocalDate.now()));		
//		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
//
//		ObjectProperty dateProperty = new SimpleObjectProperty();
//		try {
//			this.fecha = df.parse(defaultDate);// Unparseable date: "30/04/2018"
//		} catch (ParseException e) {
//			this.fecha=new Date();
//			System.out.println("fallo el parse de la fecha default");
//			e.printStackTrace();
//		}
//	}

}
