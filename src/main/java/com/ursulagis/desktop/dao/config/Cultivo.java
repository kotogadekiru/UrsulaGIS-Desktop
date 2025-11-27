package com.ursulagis.desktop.dao.config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;


//La informaci�n acerca del cultivo modelado debe contener:
//o Fecha de siembra
//o Fecha estimada de cosecha
//o Duraci�n aproximada de cada etapa fenol�gica
//o Profundidad radicular en cada etapa fenol�gica
//o Consumo h�drico en cada etapa fenol�gica
@Data
@EqualsAndHashCode(callSuper=false)
@Entity //@Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Cultivo.FIND_ALL, query="SELECT c FROM Cultivo c ORDER BY lower(c.nombre)") ,
	@NamedQuery(name=Cultivo.FIND_NAME, query="SELECT o FROM Cultivo o where o.nombre = :name") ,
	@NamedQuery(name=Cultivo.COUNT_ALL, query="SELECT COUNT(o) FROM Cultivo o") ,
	
}) 
public class Cultivo implements Comparable<Cultivo>{
	public static final String COUNT_ALL="Cultivo.countAll";
	public static final String FIND_ALL="Cultivo.findAll";
	public static final String FIND_NAME = "Cultivo.findName";
	
//	public static final String GIRASOL = "Girasol";
//	public static final String SOJA = "Soja";
//	public static final String TRIGO = "Trigo";
//	public static final String MAIZ = "Maiz";
//	public static final String SORGO = "Sorgo";
//	public static final String CEBADA = "Cebada";
	
	@Id @GeneratedValue
	private Long id=null;
	
	private String nombre =new String();
	
	//es lo que absorve (kg) la planta para producir una tonelada de grano seco
	private Double absN=0.0;
	private Double absP=0.0;
	private Double absK=0.0;
	private Double absS=0.0;
	private Double absCa=0.0, 
			absMg=0.0, 
			absB=0.0, 
			absCl=0.0,
			absCo=0.0,
			absCu=0.0,
			absFe=0.0,
			absMn=0.0,
			absMo=0.0,
			absZn=0.0;
	
	
	//mm absorvidos de agua por tn de grano producido
	private Double absAgua=0.0;
	private Double aporteMO=0.0;
	
	//es lo que se lleva el grano por cada TN 
	private Double extN=0.0;
	private Double extP=0.0;
	private Double extK=0.0;
	private Double extS=0.0;
	private Double extCa=0.0,
			extMg=0.0, 
			extB=0.0,
			extCl=0.0, 
			extCo=0.0, 
			extCu=0.0, 
			extFe=0.0, 
			extMn=0.0,
			extMo=0.0,
			extZn=0.0;
	
	private Double rindeEsperado=0.0;
	private Double ndviRindeCero=0.0;
	
	private Boolean estival = true;
	private Double semPorBolsa = 1.0;
	
//	private Double tasaCrecimientoPendiente=0.0;
//	private Double tasaCrecimientoOrigen=0.0;
	

	public Cultivo() {
		aporteMO=0.0;
		estival = true;
	}
	
	public Cultivo(String _nombre) {
		super();
		this.nombre=_nombre;
	}

	@Override
	public int compareTo(Cultivo arg0) {
		return this.nombre.compareTo(arg0.nombre);
	}
	
	@Override
	public String toString() {
		return nombre;
	}


	public boolean isEstival() {
		return this.estival;
	}
	
//	public Double getSemPorBolsa() {
//		if(this.semPorBolsa==null|| this.semPorBolsa==0.0) {
//			//supongo que es una bolsa de 40kg
//			double gramosBolsa = 40000;
//			double PMS = 40;
//			double milesSemBolsa = gramosBolsa/PMS;
//			this.semPorBolsa=milesSemBolsa*1000;
//		}
//		return this.semPorBolsa;
//	}


}

