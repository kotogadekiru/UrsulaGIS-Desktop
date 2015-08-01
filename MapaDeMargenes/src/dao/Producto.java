package dao;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;



public class Producto{
	StringProperty nombre =new SimpleStringProperty();
	DoubleProperty reqN03=new SimpleDoubleProperty();
	DoubleProperty reqP=new SimpleDoubleProperty();
	
	public static Map<String,Producto> productos = new HashMap<String,Producto>();
	public Producto(String nombre, Double reqN03, Double reqP) {
		super();
		this.nombre.set(nombre);
		this.reqN03.set(reqN03);
		this.reqP.set(reqP);
	}
	
	public String getNombre() {
		return nombre.getValue();
	}
	public void setNombre(String nombre) {
		this.nombre.set(nombre);
	}
	public Double getReqN03() {
		return reqN03.getValue();
	}
	public void setReqN03(Double reqN03) {
		this.reqN03.set(reqN03); 
	}
	public Double getReqP() {
		return reqP.getValue();
	}
	public void setReqP(Double reqP) {
		this.reqP.setValue(reqP);
	}	
	
	public DoubleProperty getReqN03Property(){
		return this.reqN03;
	}
	
	public DoubleProperty getReqPProperty(){
		return this.reqP;
	}
	
	public StringProperty getNombreProperty(){
		return this.nombre;
	}
}

