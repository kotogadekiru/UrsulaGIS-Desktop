package com.ursulagis.desktop.dao.suelo;

import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.LaborItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

//Los par�metros necesarios para cada tipo de suelo son: 
//o Capacidad de campo
//o Punto de marchitez permanente
//o Tipo de escurrimiento
//o Porcentaje de arcilla
//o Porcentaje de limo
//o Porcentaje de arena
//o Porcentaje de materia org�nica
//o Profundidad m�xima de exploraci�n radicular

@Data
@EqualsAndHashCode(callSuper=true)//si no pones esto todos los hashmaps andan mal y grillar cosecha no anda
public class SueloItem extends LaborItem { //suelo item no es labor item. le sobra: rumbo, ancho y distancia
	public static final String PPM = "PPM";
	public static final String PPM_N =PPM+" NO3";
	public static final String PPM_FOSFORO = PPM+" P";
	public static final String PPM_POTASIO= PPM+" K";
	public static final String PPM_ASUFRE = PPM+" S";//dejar en PPM S porque sino no importa las recorridas anteriores
	public static final String PC_MO = "PC MO";
	
	public static final String PROF_NAPA= "Prof Napa";
	public static final String AGUA_PERFIL= "Agua Perf";
	public static final String DENSIDAD = "kg/m3";
	public static final String ELEVACION ="Elevacion";
	
	public static final String Calcio = PPM+" Ca";
	public static final String Magnecio = PPM+" Mg";
	public static final String Boro = PPM+" B";
	public static final String Cloro = PPM+" Cl";
	public static final String Cobalto = PPM+" Co";
	public static final String Cobre = PPM+" Cu";
	public static final String Hierro = PPM+" Fe";
	public static final String Manganeso = PPM+" Mn";
	public static final String Molibdeno = PPM+" Mo";
	public static final String Zinc = PPM+" Zn";
	public static final String Textura = "Textura";
	public static final String Porosidad = "Porosidad";
	public static final String CapacidadCampo = "mm CC";
	public static final String Area = "Superficie";
	
	/**
	 *  Arenosos de 1,65 g cm-3;
	 *  Franco arenoso, 1,5 g cm-3;
	 *  Franco, 1,4 g cm-3;
	 *  Franco Arcilloso, 1,33 g cm-3; 
	 *  Arcillo Arenoso, 1,3 g cm-3 y 
	 *  Arcillosos, 1,25 g cm-3.
	 *  densidad en kg/m3
	 */
	public static final double DENSIDAD_SUELO_KG = 1.4*1000;//+-0.4 Arenoso 1650, franco 1400, arcilloso 1250


	//los ingenieros usan 2.6 para pasar de ppm a kg/ha. deben tomar la densidad en 1.3 en vez de 2
	//para pasar de Ppm a kg/ha hay que multiplicar por 2.6. 
	//es por que hay 2600tns en cada ha de 20cm de suelo.
	//ppm=x/1.000.000 => ppm/ha=X(kg/ha)/2.600.000(kg/ha)=(1/2.6)
	private Double ppmNO3=0.0;	
	private Double ppmP=0.0;	
	private Double ppmK=0.0;	
	private Double ppmS=0.0;	
	private Double ppmCa,ppmMg,ppmB,ppmCl,ppmCo,ppmCu,ppmFe,ppmMn,ppmMo,ppmZn;
	
	
	private Double porcMO=0.0;	//puede ser labil o permanente
	private Double densAp=DENSIDAD_SUELO_KG;//Densidad aparente 0-60
	/**
	 * La profundidad en cm hasta la napa
	 */
	private Double profNapa=0.0;	
	private Double aguaPerfil=0.0;	
	
	private String textura ="";
	private Double porosidad = 0.0;
	/**
	 * Capacidad de campo
	 */
	private Double porcCC = 0.0;//Capacidad de campo

	
	public SueloItem(SimpleFeature fertFeature) {
		super(fertFeature);
	}
	
	public SueloItem() {
		super();
	}

	@Override
	public Double getAmount() {
		return getPpmP();//FIXME depende de que se este dibujando
	}
	
	public void setAmount(Double amount) {		
		setPpmP(amount);		
	}
	
	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getPpmNO3(),
				getPpmP(),
				getPpmK(),
				getPpmS(),
				
				getPpmCa(),
				getPpmMg(),
				getPpmB(),
				getPpmCl(),
				getPpmCo(),
				getPpmCu(),
				getPpmFe(),
				getPpmMn(),
				getPpmMo(),
				getPpmZn(),	
				
				getPorcMO(),
				getDensAp(),
				
				getProfNapa(),
				getAguaPerfil(),
				
				getTextura(),
				getPorosidad(),
				getPorcCC()
		};
		return elements;
	}

	@Override
	public Double getImporteHa() {//podriamos devolver una valuacion del suelo de acuerdo a sus propiedades
		return 0.0;
	}
}
