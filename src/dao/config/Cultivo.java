package dao.config;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;


//La información acerca del cultivo modelado debe contener:
//o Fecha de siembra
//o Fecha estimada de cosecha
//o Duración aproximada de cada etapa fenológica
//o Profundidad radicular en cada etapa fenológica
//o Consumo hídrico en cada etapa fenológica
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
	private Double absN=new Double(0);
	private Double absP=new Double(0);
	private Double absK=new Double(0);
	private Double absS=new Double(0);
	private Double absCa=new Double(0), 
			absMg=new Double(0), 
			absB=new Double(0), 
			absCl=new Double(0),
			absCo=new Double(0),
			absCu=new Double(0),
			absFe=new Double(0),
			absMn=new Double(0),
			absMo=new Double(0),
			absZn=new Double(0);
	
	
	//mm absorvidos de agua por tn de grano producido
	private Double absAgua=new Double(0);
	private Double aporteMO=new Double(0);
	
	//es lo que se lleva el grano por cada TN 
	private Double extN=new Double(0);
	private Double extP=new Double(0);
	private Double extK=new Double(0);
	private Double extS=new Double(0);
	private Double extCa=new Double(0),
			extMg=new Double(0), 
			extB=new Double(0),
			extCl=new Double(0), 
			extCo=new Double(0), 
			extCu=new Double(0), 
			extFe=new Double(0), 
			extMn=new Double(0),
			extMo=new Double(0),
			extZn=new Double(0);
	
	private Double rindeEsperado=new Double(0);
	private Double ndviRindeCero=new Double(0);
	
	private Boolean estival = true;
	private Double semPorBolsa = 1.0;
	
//	private Double tasaCrecimientoPendiente=new Double(0);
//	private Double tasaCrecimientoOrigen=new Double(0);
	

	public Cultivo() {
		aporteMO=new Double(0);
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

