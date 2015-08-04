package dao;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Producto{
	StringProperty nombre =new SimpleStringProperty();
	DoubleProperty absN03=new SimpleDoubleProperty();
	DoubleProperty extN03=new SimpleDoubleProperty();
	
	//es lo que pierde el lote despues de la cosecha
	DoubleProperty absP=new SimpleDoubleProperty();
	//es lo que se lleva el grano
	DoubleProperty extP=new SimpleDoubleProperty();
	
	DoubleProperty rindeEsperado=new SimpleDoubleProperty();
	
	public static Map<String,Producto> productos = new HashMap<String,Producto>();
	static{
		productos.put("Maiz",new Producto("Maiz", new Double(1.4), new Double(0.0),new Double(10.0)));
		productos.put("Trigo",new Producto("Trigo", new Double(1.76), new Double(0.0),new Double(4.0)));
		productos.put("Soja",new Producto("Soja", new Double(1.76), new Double(0.0),new Double(4.0)));
	}
	
	public Producto(String nombre) {
		super();
		this.nombre.set(nombre);
	}
	
	public Producto(String string, Double _absP, Double _extP,Double rinde) {
		this.nombre.set(string);
		this.absP.set(_absP);
		this.extP.set(_extP);
		this.rindeEsperado.set(rinde);
	}

	public String getNombre() {
		return nombre.getValue();
	}
	public void setNombre(String nombre) {
		this.nombre.set(nombre);
	}
	public Double getReqN03() {
		return extN03.getValue();
	}
	public void setReqN03(Double reqN03) {
		this.extN03.set(reqN03); 
	}
	public Double getReqP() {
		return absP.getValue();
	}
	public void setReqP(Double reqP) {
		this.absP.setValue(reqP);
	}	
	
	public DoubleProperty getReqN03Property(){
		return this.extN03;
	}
	
	public DoubleProperty getReqPProperty(){
		return this.absP;
	}
	
	public StringProperty getNombreProperty(){
		return this.nombre;
	}

	public DoubleProperty getAbsN03() {
		return absN03;
	}

	public void setAbsN03(DoubleProperty absN03) {
		this.absN03 = absN03;
	}

	public DoubleProperty getExtN03() {
		return extN03;
	}

	public void setExtN03(DoubleProperty extN03) {
		this.extN03 = extN03;
	}

	public DoubleProperty getAbsP() {
		return absP;
	}

	public void setAbsP(DoubleProperty absP) {
		this.absP = absP;
	}

	public DoubleProperty getExtP() {
		return extP;
	}

	public void setExtP(DoubleProperty extP) {
		this.extP = extP;
	}

	public void setNombre(StringProperty nombre) {
		this.nombre = nombre;
	}

	public DoubleProperty getRindeEsperado() {
		return rindeEsperado;
	}

	public void setRindeEsperado(DoubleProperty rindeEsperado) {
		this.rindeEsperado = rindeEsperado;
	}

	@Override
	public String toString() {
		return nombre.getValue();
	}
	
	
}

