package com.ursulagis.desktop.dao.config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@NamedQueries({
	@NamedQuery(name=Empresa.FIND_ALL, query="SELECT o FROM Empresa o ORDER BY lower(o.nombre)"),
	@NamedQuery(name=Empresa.FIND_NAME, query="SELECT o FROM Empresa o where o.nombre = :name") ,
}) 
public class Empresa implements Comparable<Empresa>{
	 public static final String FIND_ALL = "Empresa.findAll";
	 public static final String FIND_NAME = "Empresa.findName";

	@Id @GeneratedValue
	private Long id=null;
	
	
	private String nombre= new String();
	
//	@OneToMany(cascade=CascadeType.ALL, mappedBy="empresa")
//	private List<Establecimiento> establecimientos=new ArrayList<Establecimiento>();
	
	
	public Empresa() {
	}
	
	public Empresa(String nombre){
		this.nombre=nombre;
	}

	/**
	 * @return the nombre
	 */
	
	public String getNombre() {
		return nombre;
	}

	/**
	 * @param nombre the nombre to set
	 */
	public void setNombre(String nombre) {
		this.nombre= nombre;// = nombre;
	}

//	/**
//	 * @return the establecimientos
//	 */
//	public List<Establecimiento> getEstablecimientos() {
//		return establecimientos;
//	}
//
//	/**
//	 * @param establecimientos the establecimientos to set
//	 */
//	public void setEstablecimientos(List<Establecimiento> establecimientos) {
//		this.establecimientos = establecimientos;
//	}

	
	
	@Override
	public int compareTo(Empresa arg0) {
		return this.nombre.compareTo(arg0.nombre);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return nombre;
	}
	
	
	
}
