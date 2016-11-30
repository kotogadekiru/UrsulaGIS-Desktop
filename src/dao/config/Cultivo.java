package dao.config;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Cultivo{
	static final String SOJA = "Soja";
	static final String TRIGO = "Trigo";
	static final String MAIZ = "Maiz";
	
	StringProperty nombre =new SimpleStringProperty();
	DoubleProperty absN03=new SimpleDoubleProperty();
	DoubleProperty extN03=new SimpleDoubleProperty();
	
	//es lo que pierde el lote despues de la cosecha
	DoubleProperty absP=new SimpleDoubleProperty();
	//es lo que se lleva el grano
	DoubleProperty extP=new SimpleDoubleProperty();
	
	DoubleProperty rindeEsperado=new SimpleDoubleProperty();
	
	public static Map<String,Cultivo> cultivos = new HashMap<String,Cultivo>();
	static{				//String _nombre, Double _absP, Double _extP,Double rinde
		cultivos.put(MAIZ,new Cultivo(MAIZ, 1.4, new Double(0.0),0.0,0.0,new Double(10.0)));
		cultivos.put(TRIGO,new Cultivo(TRIGO, new Double(1.76), 0.0,0.0,new Double(0.0),new Double(4.0)));
		cultivos.put(SOJA,new Cultivo(SOJA, new Double(1.76),0.0,0.0, new Double(0.0),new Double(4.0)));
	}
	
	public Cultivo(String nombre) {
		super();
		this.nombre.set(nombre);
	}
	
	public Cultivo(String _nombre, Double _absP, Double _extP, Double _absN, Double _extN,Double rinde) {
		this.nombre.set(_nombre);
		this.absP.set(_absP);
		this.extP.set(_extP);
		this.absN03.set(_absN);
		this.extN03.set(_extN);
		
		
		this.rindeEsperado.set(rinde);
	}

	public String getNombre() {
		return nombre.getValue();
	}


	public Double getRindeEsperado() {
		return rindeEsperado.getValue();
	}
	
	/**
	 * @return the absN03
	 */
	public Double getAbsN03() {
		return absN03.get();
	}

	/**
	 * @return the absP
	 */
	public Double getAbsP() {
		return absP.get();
	}
	
	/**
	 * @return the extN03
	 */
	public Double getExtN03() {
		return extN03.get();
	}
	
	/**
	 * @return the extP
	 */
	public Double getExtP() {
		return extP.get();
	}


	/**
	 * @param absP the absP to set
	 */
	public void setAbsP(Double absP) {
		this.absP.set(absP);
	}
	

	/**
	 * @param extP the extP to set
	 */
	public void setExtP(Double extP) {
		this.extP.set(extP);
	}
	
	/**
	 * @param absN03 the absN03 to set
	 */
	public void setAbsN03(Double absN03) {
		this.absN03.set(absN03);
	}
	/**
	 * @param extN03 the extN03 to set
	 */
	public void setExtN03(Double extN03) {
		this.extN03.set(extN03);
	}


	public void setRindeEsperado(Double reqP) {
		this.rindeEsperado.setValue(reqP);
	}

	public void setNombre(String nombre) {
		this.nombre.set(nombre);
	}

	@Override
	public String toString() {
		return nombre.getValue();
	}
	
	
}

