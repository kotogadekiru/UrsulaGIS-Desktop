package com.ursulagis.desktop.dao.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.ursulagis.desktop.dao.Poligono;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Asignacion.FIND_ALL, query="SELECT o FROM Asignacion o ORDER BY lower(o.lote.nombre)") ,

}) 
public class Asignacion {
	public static final String FIND_ALL="Asignacion.findAll";
	//public static final String FIND_NAME="Asignacion.findName";
	
	@Id @GeneratedValue
	private Long id=null;
	@ManyToOne(fetch = FetchType.LAZY)
	private Lote lote;
	@ManyToOne(fetch = FetchType.LAZY)
	private Cultivo cultivo;
	@ManyToOne(fetch = FetchType.LAZY)
	private Campania campania;
	@ManyToOne(fetch = FetchType.LAZY)
	private Poligono contorno;
	private Double superficie;
	
	public Asignacion() {
		
	}
	public void setLote(Lote lote) {
		this.lote = lote;
		if(lote!=null) {
			this.setPoligono( lote.getContorno());
		}
	}
	public void setPoligono(Poligono poligono) {
		if(poligono ==null)return;
		this.contorno = poligono;
		if(poligono!=null) {
			this.superficie = poligono.getArea();
		}
	}

	public Double getSuperficie() {
		if(superficie==null && contorno!=null) {
			superficie = contorno.getArea();
		}
		return superficie;
	}
}
