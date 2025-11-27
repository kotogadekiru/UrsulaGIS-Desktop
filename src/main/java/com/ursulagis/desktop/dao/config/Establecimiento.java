package com.ursulagis.desktop.dao.config;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import com.ursulagis.desktop.dao.Poligono;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@NamedQueries({
	@NamedQuery(name=Establecimiento.FIND_ALL, query="SELECT o FROM Establecimiento o ORDER BY lower(o.nombre)"),
	@NamedQuery(name=Establecimiento.FIND_NAME, query="SELECT o FROM Establecimiento o where o.nombre = :name") ,
}) 
public class Establecimiento implements Comparable<Establecimiento> {
	public static final String FIND_ALL = "Establecimiento.findAll";
	public static final String FIND_NAME = "Establecimiento.findName";
	
	@Id @GeneratedValue
	private Long id=null;
	
	public String nombre=new String();
	
	public Double superficieTotal= Double.valueOf(0.0);
	public Double superficieAgricola=Double.valueOf(0.0);
	public Double superficieGanadera=Double.valueOf(0.0);
	public Double superficieDesperdicio=Double.valueOf(0.0);
	
	@ManyToOne
	private Empresa empresa;
	
	@ManyToOne
	private Poligono contorno;
	
	@OneToMany(cascade=CascadeType.ALL, mappedBy="establecimiento")
	private List<Lote> lotes;
	
	public Establecimiento() {
	}
	
	 public Establecimiento(String establecimientoName) {
			nombre= establecimientoName;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the empresa
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * @param empresa the empresa to set
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	/**
	 * @return the lotes
	 */
	public List<Lote> getLotes() {
		return lotes;
	}

	/**
	 * @param lotes the lotes to set
	 */
	public void setLotes(List<Lote> lotes) {
		this.lotes = lotes;
	}
	
	/**
	 * @return the nombre
	 */
	@Column
	public String getNombre() {
		return nombre;
	}

	/**
	 * @param nombre the nombre to set
	 */
	public void setNombre(String nombre) {
		this.nombre=nombre;// = nombre;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return nombre;
	}

	@Override
	public int compareTo(Establecimiento arg0) {
		if (this.nombre==null){
			return -1;
		}
		if (arg0==null){
			return 1;
		}
	return this.nombre.compareTo(arg0.nombre);
	}
	
	
	 
}
