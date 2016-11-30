package dao.config;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Fertilizante{
	public static final String SUPERFOSFATO_TRIPLE_SPT = "Superfosfato triple (SPT)";
	public static final String SUPERFOSFATO_SIMPLE = "Superfosfato simple";
	public static final String FOSFATO_MONOAMONICO_MAP = "Fosfato Monoamonico (MAP)";
	public static final String FOSFATO_DIAMONICO_DAP = "Fosfato Diamonico (DAP)";
	
	public static Map<String,Fertilizante> fertilizantes = new HashMap<String,Fertilizante>();
	//http://semillastodoterreno.com/2013/03/concentracion-de-n-p-y-k-en-los-fertilizantes/
	static{																		//N-P-K-S kg cada 100kg de producto 
		fertilizantes.put(FOSFATO_DIAMONICO_DAP,new Fertilizante(FOSFATO_DIAMONICO_DAP,0.1472,0.0));	//18-46-0-2
		fertilizantes.put(FOSFATO_MONOAMONICO_MAP,new Fertilizante(FOSFATO_MONOAMONICO_MAP,0.1664,0.0));//11-52-0-2
		fertilizantes.put(SUPERFOSFATO_SIMPLE,new Fertilizante(SUPERFOSFATO_SIMPLE,0.576,0.0));		//0-(18~21)-0-0
		fertilizantes.put(SUPERFOSFATO_TRIPLE_SPT,new Fertilizante(SUPERFOSFATO_TRIPLE_SPT,0.1472,0.0));//0-(44~53)-0-0

	}
	
	StringProperty nombre = new SimpleStringProperty();
	DoubleProperty ppmP= new SimpleDoubleProperty();
	DoubleProperty ppmN= new SimpleDoubleProperty();
	
	public Fertilizante(String nombre) {
		super();
		this.nombre.set(nombre);
	}
	
	public Fertilizante(String string, Double _ppmP,Double _ppmN) {
		this.nombre.set(string);
		this.ppmP.set(_ppmP);
		this.ppmN.set(_ppmN);
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
	
	public Double getPpmN() {
		return ppmN.getValue();
	}
	public void setPpmN(Double ppmN) {
		this.ppmN.set(ppmN); 
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
	public DoubleProperty getPpmNProperty() {
		return ppmN;
	}

	/**
	 * @param ppmN the ppmN to set
	 */
	public void setPpmNProperty(DoubleProperty ppmN) {
		this.ppmN = ppmN;
	}
	
	@Override
	public String toString() {
		return nombre.getValue();
	}


	
	
}

