package dao.suelo;

import org.opengis.feature.simple.SimpleFeature;

import dao.LaborItem;
import lombok.Data;

//Los parámetros necesarios para cada tipo de suelo son: 
//o Capacidad de campo
//o Punto de marchitez permanente
//o Tipo de escurrimiento
//o Porcentaje de arcilla
//o Porcentaje de limo
//o Porcentaje de arena
//o Porcentaje de materia orgánica
//o Profundidad máxima de exploración radicular

@Data
public class SueloItem extends LaborItem { //suelo item no es labor item. le sobra: rumbo, ancho y distancia
	public static final String PPM_N = "PPM N";
	public static final String PPM_FOSFORO = "PPM P";
	public static final String PPM_POTASIO= "PPM K";
	public static final String PPM_ASUFRE = "PPM S";
	public static final String PPM_MO = "PPM MO";
	
	public static final String PROF_NAPA= "Prof Napa";
	public static final String AGUA_PERFIL= "Agua Perf";

	
	//los ingenieros usan 2.6 para pasar de ppm a kg/ha. deben tomar la densidad en 1.3 en vez de 2
	//para pasar de Ppm a kg/ha hay que multiplicar por 2.6. 
	//es por que hay 2600tns en cada ha de 20cm de suelo.
	//ppm=x/1.000.000 => ppm/ha=X(kg/ha)/2.600.000(kg/ha)=(1/2.6)
	private Double ppmN=new Double(0);	
	private Double ppmP=new Double(0);	
	private Double ppmK=new Double(0);	
	private Double ppmS=new Double(0);	
	private Double ppmMO=new Double(0);	//puede ser labil o permanente
	/*La profundidad en cm hasta la napa*/
	private Double profNapa=new Double(0);	
	private Double aguaPerfil=new Double(0);	

	
	public SueloItem(SimpleFeature fertFeature) {
		super(fertFeature);
	}
	
	public SueloItem() {
		super();
	}

	@Override
	public Double getAmount() {
		return getPpmP();
	}
	
	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getPpmN(),
				getPpmP(),
				getPpmK(),
				getPpmS(),
				getPpmMO(),
				getProfNapa(),
				getAguaPerfil()				
		};
		return elements;
	}

	@Override
	public Double getImporteHa() {//podriamos devolver una valuacion del suelo de acuerdo a sus propiedades
		return 0.0;
	}
	

}
