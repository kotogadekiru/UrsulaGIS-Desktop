package com.ursulagis.desktop.dao.ordenCompra;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
//@Inheritance(strategy=javax.persistence.InheritanceType.TABLE_PER_CLASS)
@Inheritance(strategy=javax.persistence.InheritanceType.JOINED)
@NamedQueries({
	@NamedQuery(name=Producto.FIND_ALL, query="SELECT o FROM Producto o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Producto.FIND_NAME, query="SELECT o FROM Producto o where o.nombre = :name") ,
}) 
public abstract class Producto {	
	public static final String FIND_ALL="Producto.findAll";
	public static final String FIND_NAME="Producto.findName";
	@Id @GeneratedValue
	private Long id=null;
	protected String nombre = new String();

	
	public Producto() {		
	}
	
	public Producto(String nom) {
		this.nombre=nom;
	}
	
	
	public int compareTo(Producto p) {
		System.out.println("comparando producto "+this+" con "+p);
		if(p==null)return -1;		
		return this.getNombre().compareTo(p.getNombre());	
	}

}
