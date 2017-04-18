package dao.config;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;
@Data
public class Fertilizante{
	public static final String SUPERFOSFATO_TRIPLE_SPT = "Superfosfato Triple (SPT)";
	public static final String SUPERFOSFATO_SIMPLE = "Superfosfato Simple";
	public static final String FOSFATO_MONOAMONICO_MAP = "Fosfato Monoamonico (MAP)";
	public static final String FOSFATO_DIAMONICO_DAP = "Fosfato Diamonico (DAP)";
	
	public static Map<String,Fertilizante> fertilizantes = new HashMap<String,Fertilizante>();
	//http://semillastodoterreno.com/2013/03/concentracion-de-n-p-y-k-en-los-fertilizantes/
	static{																		//N-P-K-S kg cada 100kg de producto 
//		fertilizantes.put(FOSFATO_DIAMONICO_DAP,new Fertilizante(FOSFATO_DIAMONICO_DAP,0.1472,0.0));	//18-46-0-2
//		fertilizantes.put(FOSFATO_MONOAMONICO_MAP,new Fertilizante(FOSFATO_MONOAMONICO_MAP,0.1664,0.0));//11-52-0-2
//		fertilizantes.put(SUPERFOSFATO_SIMPLE,new Fertilizante(SUPERFOSFATO_SIMPLE,0.576,0.0));		//0-(18~21)-0-0
//		fertilizantes.put(SUPERFOSFATO_TRIPLE_SPT,new Fertilizante(SUPERFOSFATO_TRIPLE_SPT,0.1472,0.0));//0-(44~53)-0-0
		
//		%N		%P
//Amoníaco anhidro 							82	
//Nitrato de amonio							33.5	
//Sulfato de amonio							21	
//Fosfato diamónico			 18 a 21		21		20
//Fosfato monoamónico							11		23
//Nitrato de Calcio							15	
//Cianamida cálcica	 20 a 22				22	
//Nitrato de Potasio							13	
//Nitrato de Sodio							16	
//Urea										46	
//Urea - Amonio Nitrato (UAN)					32				
fertilizantes.put("Urea",new Fertilizante("Urea",46,0.0,0.0,0.0));
fertilizantes.put("Sulfato de amonio",new Fertilizante("Sulfato de amonio",20.5,0.0,0.0,24.0));
fertilizantes.put("Amoníaco anhidro",new Fertilizante("Amoníaco anhidro",82,0.0,0.0,0.0));
fertilizantes.put("Fosfato diamónico",new Fertilizante("Fosfato diamónico",18,46,0.0,2));
fertilizantes.put("Fosfato monoamónico",new Fertilizante("Fosfato monoamónico",11,52,0.0,2));
fertilizantes.put("Nitrato de amonio",new Fertilizante("Nitrato de amonio",35,0.0,0.0,0.0));
fertilizantes.put("Nitrato de calcio",new Fertilizante("Nitrato de calcio",15,0.0,0.0,0.0));
fertilizantes.put("Nitrato de potasio",new Fertilizante("Nitrato de potasio",13,0.0,44,0.0));
fertilizantes.put("Superfosfato de calcio",new Fertilizante("Superfosfato de calcio",0.0,17,0.0,0.0));
fertilizantes.put("Superfosfato triple",new Fertilizante("Superfosfato triple",0.0,48.5,0.0,0.0));
fertilizantes.put("Superfosfato simple",new Fertilizante("Superfosfato simple",0.0,19.5,0.0,0.0));
fertilizantes.put("Fosfato dicálcico",new Fertilizante("Fosfato dicálcico",0.0,38,0.0,0.0));
fertilizantes.put("Cloruro de potasio",new Fertilizante("Cloruro de potasio",0.0,0.0,60,0.0));
fertilizantes.put("Sulfato de potasio",new Fertilizante("Sulfato de potasio",0.0,0.0,50,18));
	}
	
	String nombre = new String();
	double porcN= 0.0;
	double porcP= 0.0;
	double porcK= 0.0;
	double porcS= 0.0;
	
	
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


	
	
}

