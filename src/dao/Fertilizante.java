package dao;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Fertilizante{
	StringProperty nombre = new SimpleStringProperty();
	DoubleProperty ppmP= new SimpleDoubleProperty();
	DoubleProperty ppmN= new SimpleDoubleProperty();
	
	//public static List<Fertilizante> fertilizantes = new ArrayList<Fertilizante>();
	public static Map<String,Fertilizante> fertilizantes = new HashMap<String,Fertilizante>();
	//http://semillastodoterreno.com/2013/03/concentracion-de-n-p-y-k-en-los-fertilizantes/
	static{																		//N-P-K-S kg cada 100kg de producto 
		fertilizantes.put("Fosfato Diamonico (DAP)",new Fertilizante("Fosfato Diamonico (DAP)",0.1472));	//18-46-0-2
		fertilizantes.put("Fosfato Monoamonico (MAP)",new Fertilizante("Fosfato Monoamonico (MAP)",0.1664));//11-52-0-2
		fertilizantes.put("Superfosfato simple",new Fertilizante("Superfosfato simple",0.576));		//0-(18~21)-0-0
		fertilizantes.put("Superfosfato triple (SPT)",new Fertilizante("Superfosfato triple (SPT)",0.1472));//0-(44~53)-0-0

	}
	
	public Fertilizante(String nombre) {
		super();
		this.nombre.set(nombre);
	}
	
	public Fertilizante(String string, Double _ppmP) {
		this.nombre.set(string);
		this.ppmP.set(_ppmP);
	}

	public String getNombre() {
		return nombre.getValue();
	}
	public void setNombre(String nombre) {
		this.nombre.set(nombre);
	} 
	public Double getPpmP() {
		return ppmP.getValue();
	}
	public void setPpmP(Double ppmP) {
		this.ppmP.set(ppmP); 
	}
	
	public StringProperty getNombreProperty(){
		return this.nombre;
	}

	public void setNombre(StringProperty nombre) {
		this.nombre = nombre;
	}

	/**
	 * @return the ppmN
	 */
	public DoubleProperty getPpmN() {
		return ppmN;
	}

	/**
	 * @param ppmN the ppmN to set
	 */
	public void setPpmN(DoubleProperty ppmN) {
		this.ppmN = ppmN;
	}
	
	@Override
	public String toString() {
		return nombre.getValue();
	}


	
	
}

