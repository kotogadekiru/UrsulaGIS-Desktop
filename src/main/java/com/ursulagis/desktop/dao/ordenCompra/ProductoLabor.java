package com.ursulagis.desktop.dao.ordenCompra;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@NamedQueries({
	@NamedQuery(name=ProductoLabor.FIND_ALL, query="SELECT o FROM ProductoLabor o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=ProductoLabor.FIND_NAME, query="SELECT o FROM ProductoLabor o where o.nombre = :name") ,
	@NamedQuery(name=ProductoLabor.COUNT_ALL, query="SELECT COUNT(o) FROM ProductoLabor o") ,
}) 
public class ProductoLabor extends Producto {
	public static final String LABOR_DE_COSECHA = "Labor de Cosecha";
	public static final String LABOR_DE_SIEMBRA = "Labor de Siembra";
	public static final String LABOR_DE_FERTILIZACION = "Labor de Fertilizacion";
	public static final String LABOR_DE_PULVERIZACION = "Labor de Pulverizacion";
	
	public static final String FIND_ALL="ProductoLabor.findAll";
	public static final String FIND_NAME="ProductoLabor.findName";
	public static final String COUNT_ALL="ProductoLabor.countAll";
	
	public static Map<String,ProductoLabor> laboresDefault = new HashMap<String,ProductoLabor>();

	static{
		laboresDefault.put(LABOR_DE_PULVERIZACION,new ProductoLabor(LABOR_DE_PULVERIZACION));//ok
		laboresDefault.put(LABOR_DE_FERTILIZACION,new ProductoLabor(LABOR_DE_FERTILIZACION));//ok
		laboresDefault.put(LABOR_DE_SIEMBRA,new ProductoLabor(LABOR_DE_SIEMBRA));//ok
		laboresDefault.put(LABOR_DE_COSECHA,new ProductoLabor(LABOR_DE_COSECHA));//ok
	}
		
	//String nombre = new String();
	
	public ProductoLabor() {
		
	}
	
	public ProductoLabor(String _nombre) {
		this.nombre=_nombre;
	}

}
