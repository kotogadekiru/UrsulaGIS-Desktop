package com.ursulagis.desktop.dao.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import com.ursulagis.desktop.dao.ordenCompra.Producto;
import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@NamedQueries({
	@NamedQuery(name=Fertilizante.FIND_ALL, query="SELECT o FROM Fertilizante o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Fertilizante.FIND_NAME, query="SELECT o FROM Fertilizante o where o.nombre = :name") ,
}) 
public class Fertilizante extends Producto implements Comparable<Fertilizante>{
	public static final String SUPERFOSFATO_TRIPLE_SPT = "Superfosfato Triple (SPT)";
	public static final String SUPERFOSFATO_SIMPLE = "Superfosfato Simple";
	public static final String FOSFATO_MONOAMONICO_MAP = "Fosfato Monoamonico (MAP)";
	public static final String FOSFATO_DIAMONICO_DAP = "Fosfato Diamonico (DAP)";//se usa en fertilizacion labor para seleccionar el default
	
	//public static final Double porcN_NO3=0.2259;//necesario para convertir de ppm Nitratos NO3 a kgN
	//public static final Double porcP_PO4=0.3231;// la informacion que estoy ingresando esta en ppm P no P204 
	
	public static final Double porcN_MO=0.04;
	public static final Double porcMO_DISP_Campania=0.02;
	public static final Double porcMO_DISP_Estival=2d/3d;
	public static final Double porcMO_DISP_Invernal=1d/3d;
	
	public static final String FIND_ALL="Fertilizante.findAll";
	public static final String FIND_NAME="Fertilizante.findName";
	
	public static Map<String,Fertilizante> getFertilizantesDefault(){
		Map<String,Fertilizante> fertilizantes = new HashMap<String,Fertilizante>();
		//Nitrogenados
		fertilizantes.put("Amoniaco anhidro",new Fertilizante("Amoniaco anhidro",82,0.0,0.0,0.0));//ok
		fertilizantes.put("Nitrato de amonio",new Fertilizante("Nitrato de amonio",35,0.0,0.0,0.0));//ok
		fertilizantes.put("Sulfato de amonio",new Fertilizante("Sulfato de amonio",20.5,0.0,0.0,24.0));//ok
		fertilizantes.put("UAN",new Fertilizante("UAN",31,0.0,0.0,0.0));//ok
		fertilizantes.put("Urea",new Fertilizante("Urea",46,0.0,0.0,0.0));//ok
		//Fosfatados
		//acido fosforico
		fertilizantes.put("Fosfato diamonico",new Fertilizante("Fosfato diamónico",18,20,0.0,0));//ok
		fertilizantes.put("Fosfato monoamonico",new Fertilizante("Fosfato monoamonico",11,23,0.0,0));//ok
		fertilizantes.put("Fosfato monopotasico",new Fertilizante("Fosfato monopotasico",0,23,29,0));//ok
		fertilizantes.put("Superfosfato simple",new Fertilizante("Superfosfato simple",0.0,9,0.0,12));//ok
		fertilizantes.put("Superfosfato triple",new Fertilizante("Superfosfato triple",0.0,20,0.0,0.0));//ok
		//Potasicos
		fertilizantes.put("Cloruro de potasio",new Fertilizante("Cloruro de potasio",0.0,0.0,50,0.0));//ok
		fertilizantes.put("Nitrato de potasio",new Fertilizante("Nitrato de potasio",13,0.0,36,0.0));//ok
		fertilizantes.put("Sulfato de potasio",new Fertilizante("Sulfato de potasio",0.0,0.0,42,18));//ok
		//Azufrados
		fertilizantes.put("Yeso Agricola",new Fertilizante("Yeso Agricola",0.0,0.0,0,17));//ok S=17 Ca=22
		//Calcicos
		//fertilizantes.put("Nitrato de calcio",new Fertilizante("Nitrato de calcio",15,0.0,0.0,0.0));19Ca 15N
		//fertilizantes.put("Superfosfato de calcio",new Fertilizante("Superfosfato de calcio",0.0,17,0.0,0.0));

		return fertilizantes;
	}
	
	double porcN= 0.0;
	double porcP= 0.0;
	double porcK= 0.0;
	double porcS= 0.0;
	double porcCa, porcMg, porcB, porcCl, porcCo, porcCu, porcFe, porcMn, porcMo, porcZn;
	
	@Transient
	private Map<SueloParametro,Double> cNutrientes = null;
	
	
	public Fertilizante() {
		super();

	}
	
	public Fertilizante(String nom) {
		super(nom);

		
	}
	
	public Fertilizante(String nom, double n,double p,double k, double s) {
		super(nom);
		 porcN= n;
		 porcP= p;
		 porcK= k;
		 porcS= s;
		
	}
	
	public Map<SueloParametro,Double> getCNutrientes(){
		if(cNutrientes==null) {
			 cNutrientes = new ConcurrentHashMap<SueloParametro,Double>();
			 cNutrientes.put(SueloParametro.Nitrogeno, porcN);
			 cNutrientes.put(SueloParametro.Fosforo, porcP);
			 cNutrientes.put(SueloParametro.Potasio, porcK);
			 cNutrientes.put(SueloParametro.Azufre, porcS);
			 cNutrientes.put(SueloParametro.Calcio, porcCa);
			 cNutrientes.put(SueloParametro.Magnecio, porcMg);
			 cNutrientes.put(SueloParametro.Boro, porcB);
			 cNutrientes.put(SueloParametro.Cloro, porcCl);
			 cNutrientes.put(SueloParametro.Cobalto, porcCo);
			 cNutrientes.put(SueloParametro.Cobre, porcCu);
			 cNutrientes.put(SueloParametro.Hierro, porcFe);
			 cNutrientes.put(SueloParametro.Manganeso, porcMn);
			 cNutrientes.put(SueloParametro.Molibdeno, porcMo);
			 cNutrientes.put(SueloParametro.Zinc, porcZn);
		}
		return cNutrientes;
	}


	@Override
	public String toString() {
		return this.nombre;
	}
	@Override
	public int compareTo(Fertilizante p) {
		return super.compareTo(p);	
	}
}

