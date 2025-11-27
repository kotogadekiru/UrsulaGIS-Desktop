package com.ursulagis.desktop.dao.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Nutriente.FIND_ALL, query="SELECT o FROM Nutriente o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Nutriente.FIND_NAME, query="SELECT o FROM Nutriente o where o.nombre = :name") ,
}) 

/**
 * Clase que modela los diferentes tipos de nutrientes que puede tener un fertilizante o necesitar un cultivo
 */

/**
 * Macronutrientes primarios - nitr�geno (N), f�sforo (P), potasio (K),
Macronutrientes secundarios � azufre (S), calcio (Ca), magnesio (Mg)
Micronutrientes - boro (B), cloro (Cl), cobalto (Co), cobre (Cu), hierro (Fe), manganeso (Mn), molibdeno (Mo) y zinc (Zn)
 */
public class Nutriente {	
	public static final String ZINC = "Zinc";
	public static final String MOLIBDENO = "Molibdeno";
	public static final String MANGANESO = "Manganeso";
	public static final String HIERRO = "Hierro";
	public static final String COBRE = "Cobre";
	public static final String COBALTO = "Cobalto";
	public static final String CLORO = "Cloro";
	public static final String BORO = "Boro";
	public static final String MAGNECIO = "Magnecio";
	public static final String CALCIO = "Calcio";
	public static final String AZUFRE = "Azufre";//Azufre de sulfato?
	public static final String POTASIO = "Potasio";
	public static final String FOSFORO = "Fosforo";
	public static final String NITROGENO = "Nitrogeno";
	
	public static final String FIND_ALL="Nutriente.findAll";
	public static final String FIND_NAME="Nutriente.findName";
	//peso N=14.0067 peso O=15.999 N/NO3=14.0067/(14.1167+15.999*3)=0.2259
	public static final Double porcN_NO3=0.2259;
	//pongo 1 porque quiero trabajar con S como elemento no como ion Sulfato
	public static final Double porcS_SO4=1.0;// 32.06/(32.06+4*15.99);//=0.3338;
	
	private static Map<SueloParametro,Nutriente> nutrientesDefault=null;
	private static List<SueloParametro> microNutrientesDefault=null;
	static{
		nutrientesDefault = new ConcurrentHashMap<SueloParametro,Nutriente>();
		
		nutrientesDefault.put(SueloParametro.Nitrogeno,new Nutriente("Nitrogeno de Nitratos","N-NO3",porcN_NO3,0.6));//ok
		nutrientesDefault.put(SueloParametro.Fosforo,new Nutriente("Fosforo de Fosfato","P",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Potasio,new Nutriente(POTASIO,"K",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Azufre,new Nutriente(AZUFRE,"S-SO4",porcS_SO4,0.2));//S04
		
		nutrientesDefault.put(SueloParametro.Calcio,new Nutriente(CALCIO,"Ca",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Magnecio,new Nutriente(MAGNECIO,"Mg",1.0,0.2));		
		nutrientesDefault.put(SueloParametro.Boro,new Nutriente(BORO,"B",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Cloro,new Nutriente(CLORO,"Cl",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Cobalto,new Nutriente(COBALTO,"Co",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Cobre,new Nutriente(COBRE,"Cu",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Hierro,new Nutriente(HIERRO,"Fe",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Manganeso,new Nutriente(MANGANESO,"Mn",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Molibdeno,new Nutriente(MOLIBDENO,"Mo",1.0,0.2));
		nutrientesDefault.put(SueloParametro.Zinc,new Nutriente(ZINC,"Zn",1.0,0.2));
		
		microNutrientesDefault = new ArrayList<SueloParametro>();
		microNutrientesDefault.add(SueloParametro.Calcio);
		microNutrientesDefault.add(SueloParametro.Magnecio);	
		microNutrientesDefault.add(SueloParametro.Boro);
		microNutrientesDefault.add(SueloParametro.Cloro);
		microNutrientesDefault.add(SueloParametro.Cobalto);
		microNutrientesDefault.add(SueloParametro.Cobre);
		microNutrientesDefault.add(SueloParametro.Hierro);
		microNutrientesDefault.add(SueloParametro.Manganeso);
		microNutrientesDefault.add(SueloParametro.Molibdeno);
		microNutrientesDefault.add(SueloParametro.Zinc);
	}
	
	@Id @GeneratedValue
	private Long id=null;
	
	private String nombre;
	private String simbolo;
	private Double porcNutrienteEnMolecula=1.0;//sirve para convertir de ppm molecular a kg de nutriente
	private Double profundidad=0.2;
	
	Nutriente(){
		
	}
			
	Nutriente(String nombre, String simbolo, Double porcNutriente,Double profundidad){
		this.nombre=nombre;
		this.simbolo=simbolo;
		this.porcNutrienteEnMolecula=porcNutriente;
		this.profundidad=profundidad;
	}
	public static List<SueloParametro> getMicroNutrientes(){
		return microNutrientesDefault;
	}
	
	public static Map<SueloParametro,Nutriente> getNutrientesDefault(){
		//System.out.println("devolviendo nutrientes default con "+nutrientesDefault.size()+" elementos");
//		if(nutrientesDefault !=null)return nutrientesDefault;
//		nutrientesDefault = new ConcurrentHashMap<SueloParametro,Nutriente>();
//		
//		nutrientesDefault.put(SueloParametro.Nitrogeno,new Nutriente("Nitrogeno de Nitratos","N-NO3",porcN_NO3));//ok
//		nutrientesDefault.put(SueloParametro.Fosforo,new Nutriente("Fosforo de Fosfato","P",1.0));
//		nutrientesDefault.put(SueloParametro.Potasio,new Nutriente(POTASIO,"K",1.0));
//		nutrientesDefault.put(SueloParametro.Azufre,new Nutriente(AZUFRE,"S",1.0));
//		nutrientesDefault.put(SueloParametro.Calcio,new Nutriente(CALCIO,"Ca",1.0));
//		nutrientesDefault.put(SueloParametro.Magnecio,new Nutriente(MAGNECIO,"Mg",1.0));
//		
//		nutrientesDefault.put(SueloParametro.Boro,new Nutriente(BORO,"B",1.0));
//		nutrientesDefault.put(SueloParametro.Cloro,new Nutriente(CLORO,"Cl",1.0));
//		nutrientesDefault.put(SueloParametro.Cobalto,new Nutriente(COBALTO,"Co",1.0));
//		nutrientesDefault.put(SueloParametro.Cobre,new Nutriente(COBRE,"Cu",1.0));
//		nutrientesDefault.put(SueloParametro.Hierro,new Nutriente(HIERRO,"Fe",1.0));
//		nutrientesDefault.put(SueloParametro.Manganeso,new Nutriente(MANGANESO,"Mn",1.0));
//		nutrientesDefault.put(SueloParametro.Molibdeno,new Nutriente(MOLIBDENO,"Mo",1.0));
//		nutrientesDefault.put(SueloParametro.Zinc,new Nutriente(ZINC,"Zn",1.0));
		return nutrientesDefault;
	}
	

}
