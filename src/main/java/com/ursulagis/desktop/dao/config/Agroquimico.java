package com.ursulagis.desktop.dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.ursulagis.desktop.dao.ordenCompra.Producto;
import com.ursulagis.desktop.dao.ordenCompra.ProductoLabor;
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
	@NamedQuery(name=Agroquimico.FIND_ALL, query="SELECT o FROM Agroquimico o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Agroquimico.FIND_NAME, query="SELECT o FROM Agroquimico o where o.nombre = :name") ,
	@NamedQuery(name=Agroquimico.FIND_ACTIVOS, query="SELECT o FROM Agroquimico o where o.activo = true ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Agroquimico.FIND_NUM_REG, query="SELECT o FROM Agroquimico o where o.numRegistro = :numReg") ,

}) 
public class Agroquimico extends Producto implements Comparable<Agroquimico>{
	public static final String FIND_ALL="Agroquimico.findAll";
	public static final String FIND_NAME="Agroquimico.findName";

	public static final String FIND_ACTIVOS = "Agroquimico.findActivos";	
	
	public static final String FIND_NUM_REG="Agroquimico.findNumReg";

	
//	public static Map<String,Agroquimico> getAgroquimicosDefault(){
//		HashMap<String,Agroquimico> agroquimicos = new HashMap<String,Agroquimico>();
//		agroquimicos.put("RoundUp(lts)",new Agroquimico("RoundUp(lts)"));	
//		agroquimicos.put("Superwet(lts)",new Agroquimico("Superwet(lts)"));
//		agroquimicos.put("Atrazina(lts)",new Agroquimico("Atrazina(lts)"));
//		agroquimicos.put("Cletodim(lts)",new Agroquimico("Cletodim(lts)"));
//		
//		agroquimicos.put("Rizospray extremo(lts)",new Agroquimico("Rizospray extremo(lts)"));
//		agroquimicos.put("Benazolin(lts)",new Agroquimico("Benazolin(lts)"));
//		agroquimicos.put("Fomesafen(lts)",new Agroquimico("Fomesafen(lts)"));
//		agroquimicos.put("Glifosato 66%(lts)",new Agroquimico("Glifosato 66%(lts)"));
//		agroquimicos.put("Dinotefuran(lts)",new Agroquimico("Dinotefuran(lts)"));
//		agroquimicos.put("Abamectina 1,8(lts)",new Agroquimico("Abamectina 1,8(lts)"));
//		agroquimicos.put("Coragen(lts)",new Agroquimico("Coragen(lts)"));
//		agroquimicos.put("Opera(lts)",new Agroquimico("Opera(lts)"));		
//		agroquimicos.put("Haloxifop 90% (Galant max)(lts)",new Agroquimico("Haloxifop 90% (Galant max)(lts)"));	
//		return agroquimicos;
//	}	
//	
	private String unidadDosis = new String();
	private String unidadStock = new String(); 
	
	private boolean activo = false;
	private String numRegistro=null;
	private String empresa=null;
	private String activos=null;

	private String bandaToxicologica=null;
	
	public Agroquimico() {
	}

	
	public Agroquimico(String _nombre) {
		nombre=_nombre;
		activo=true;
	}

	@Override
	public String toString() {
		return nombre;
	}
	
	@Override
	public int compareTo(Agroquimico p) {
		return super.compareTo(p);	
	}
	
	public void toggleActivo() {
		this.activo = !this.activo;
	}
}
