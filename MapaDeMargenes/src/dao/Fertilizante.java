package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Fertilizante{
	StringProperty nombre = new SimpleStringProperty();
	DoubleProperty ppmP= new SimpleDoubleProperty();
	
	public static List<Fertilizante> fertilizantes = new ArrayList<Fertilizante>();
	static{
		fertilizantes.add(new Fertilizante("Fosfato Diamonico (DAP)",14.72));
		fertilizantes.add(new Fertilizante("Fosfato Monoamonico (MAP)",16.64));
		fertilizantes.add(new Fertilizante("Superfosfato simple",5.76));
		fertilizantes.add(new Fertilizante("Superfosfato triple (SPT)",14.72));

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

	@Override
	public String toString() {
		return nombre.getValue();
	}
	
	
}

