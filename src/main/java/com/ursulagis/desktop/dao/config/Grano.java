package com.ursulagis.desktop.dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.ursulagis.desktop.dao.ordenCompra.Producto;
import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
@Data
@EqualsAndHashCode(callSuper=true)
@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Grano.FIND_ALL, query="SELECT o FROM Grano o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Grano.FIND_NAME, query="SELECT o FROM Grano o where o.nombre = :name") ,
	@NamedQuery(name=Grano.FIND_ACTIVOS, query="SELECT o FROM Grano o where o.activo = true ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Grano.FIND_BY_CULTIVO, query="SELECT o FROM Grano o where o.cultivo = :cultivo") ,

}) 
public class Grano extends Producto implements Comparable<Grano>{
	public static final String FIND_ALL="Grano.findAll";
	public static final String FIND_NAME="Grano.findName";

	public static final String FIND_ACTIVOS = "Grano.findActivos";	
	
	public static final String FIND_BY_CULTIVO="Grano.findCultivo";	

	private String unidadDosis = new String();
	private String unidadStock = new String(); 
	
	private boolean activo = false;

	private Cultivo cultivo = null;
	
	public Grano() {
	}

	
	public Grano(String _nombre, Cultivo c) {
		nombre = _nombre;
		activo = true;
		cultivo = c;
	}

	@Override
	public String toString() {
		return nombre;
	}
	
	@Override
	public int compareTo(Grano p) {
		return super.compareTo(p);	
	}
	
	public void toggleActivo() {
		this.activo = !this.activo;
	}
}
