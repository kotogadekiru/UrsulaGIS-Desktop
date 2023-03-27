package dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import dao.OrdenDeCompra.Producto;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper=false)
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
	
	public static final Double porcN_NO3=0.2259;//necesario para convertir de ppm Nitratos NO3 a kgN
	//public static final Double porcP_PO4=0.3231;// la informacion que estoy ingresando esta en ppm P no P204 
	
	public static final Double porcN_MO=0.04;
	public static final Double porcMO_DISP_Campania=0.02;
	public static final Double porcMO_DISP_Estival=2d/3d;
	public static final Double porcMO_DISP_Invernal=1d/3d;
	
	public static final String FIND_ALL="Fertilizante.findAll";
	public static final String FIND_NAME="Fertilizante.findName";
	
	public static Map<String,Fertilizante> fertilizantes = new HashMap<String,Fertilizante>();
	//http://semillastodoterreno.com/2013/03/concentracion-de-n-p-y-k-en-los-fertilizantes/
	static{																		//N-P-K-S kg cada 100kg de producto 
//		fertilizantes.put(FOSFATO_DIAMONICO_DAP,new Fertilizante(FOSFATO_DIAMONICO_DAP,0.1472,0.0));	//18-46-0-2
//		fertilizantes.put(FOSFATO_MONOAMONICO_MAP,new Fertilizante(FOSFATO_MONOAMONICO_MAP,0.1664,0.0));//11-52-0-2
//		fertilizantes.put(SUPERFOSFATO_SIMPLE,new Fertilizante(SUPERFOSFATO_SIMPLE,0.576,0.0));		//0-(18~21)-0-0
//		fertilizantes.put(SUPERFOSFATO_TRIPLE_SPT,new Fertilizante(SUPERFOSFATO_TRIPLE_SPT,0.1472,0.0));//0-(44~53)-0-0
		
//								%N			%P
//Amoníaco anhidro 							82	
//Nitrato de amonio							33.5	
//Sulfato de amonio							21	
//Fosfato diamónico			 18 a 21		21		20
//Fosfato monoamónico			11			23
//Nitrato de Calcio				15	
//Cianamida cálcica			 20 a 22		22	
//Nitrato de Potasio			13	
//Nitrato de Sodio				16	
//Urea							46	
//Urea - Amonio Nitrato (UAN)	32				
		
		//Nitrogenados
fertilizantes.put("Amoníaco anhidro",new Fertilizante("Amoníaco anhidro",82,0.0,0.0,0.0));//ok
fertilizantes.put("Nitrato de amonio",new Fertilizante("Nitrato de amonio",35,0.0,0.0,0.0));//ok
fertilizantes.put("Sulfato de amonio",new Fertilizante("Sulfato de amonio",20.5,0.0,0.0,24.0));//ok
fertilizantes.put("UAN",new Fertilizante("UAN",31,0.0,0.0,0.0));//ok
fertilizantes.put("Urea",new Fertilizante("Urea",46,0.0,0.0,0.0));//ok
		//Fosfatados
//acido fosforico
fertilizantes.put("Fosfato diamónico",new Fertilizante("Fosfato diamónico",18,20,0.0,0));//ok
fertilizantes.put("Fosfato monoamónico",new Fertilizante("Fosfato monoamónico",11,23,0.0,0));//ok
fertilizantes.put("Fosfato monopotásico",new Fertilizante("Fosfato monopotásico",0,23,29,0));//ok
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
	}
	
//	@Id @GeneratedValue
//	private Long id=null;
	
	String nombre = new String();
	
	double porcN= 0.0;
	double porcP= 0.0;
	double porcK= 0.0;
	double porcS= 0.0;
	
	
	public Fertilizante() {}
	
	public Fertilizante(String nom) {
		super();
		this.nombre=nom;
	}
	
	public Fertilizante(String nom, double n,double p,double k, double s) {
		this.nombre=nom;
		 porcN= n;
		 porcP= p;
		 porcK= k;
		 porcS= s;
	}

	@Override
	public String toString() {
		return nombre;
	}

	@Override
	public int compareTo(Fertilizante o) {
		return (int) (this.getId()-o.getId());
	}
}

