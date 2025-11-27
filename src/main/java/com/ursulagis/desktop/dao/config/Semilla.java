package com.ursulagis.desktop.dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.ursulagis.desktop.dao.ordenCompra.Producto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.CultivoHelper;

@Data
@EqualsAndHashCode(callSuper=true)
@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity //@Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Semilla.FIND_ALL, query="SELECT o FROM Semilla o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Semilla.FIND_NAME, query="SELECT o FROM Semilla o where o.nombre = :name") ,
}) 
public class Semilla  extends Producto implements Comparable<Semilla>{
	public static final String FIND_ALL="Semilla.findAll";
	public static final String FIND_NAME="Semilla.findName";

	public static final String SEMILLA_DE_TRIGO = "Semilla de Trigo";
	public static final String SEMILLA_DE_SOJA = "Semilla de Soja";
	public static final String SEMILLA_DE_MAIZ = "Semilla de Maiz";

	/**
	 * poder germinativo
	 */
	private Double PG = Double.valueOf(1);
	/**
	 * peso de mil granos en gramos
	 */
	private Double pesoDeMil = Double.valueOf(150);

	@ManyToOne(cascade=CascadeType.PERSIST)
	private Cultivo cultivo = null;
	
	public static Map<String,Semilla> getSemillasDefault(){
		 Map<String,Semilla> semillas = new HashMap<String,Semilla>();
		 Map<String, Cultivo> cultivos = CultivoHelper.getCultivosDefault();
			semillas.put(SEMILLA_DE_MAIZ,new Semilla(SEMILLA_DE_MAIZ,cultivos.get(CultivoHelper.MAIZ)));	
			semillas.put(SEMILLA_DE_SOJA,new Semilla(SEMILLA_DE_SOJA,cultivos.get(CultivoHelper.SOJA)));
			semillas.put(SEMILLA_DE_TRIGO,new Semilla(SEMILLA_DE_TRIGO,cultivos.get(CultivoHelper.TRIGO)));
		return semillas;
	}
	public Semilla(){
		super();
	}

	public Semilla(String _nombre, Cultivo producto) {
		super();
		nombre=_nombre;
		cultivo=producto;
		//productoProperty.setValue(producto);
	}	
	
	public void setPG(Double pg) {
		if(pg!=null && pg>0 && pg<=1) {
			this.PG=pg;
		}else {
			this.PG=1.0;
		}
	}

	@Override
	public String toString() {
		return nombre;
	}
	
	@Override
	public int compareTo(Semilla p) {
		return super.compareTo(p);	
	}

}
