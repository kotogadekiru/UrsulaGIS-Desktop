package com.ursulagis.desktop.dao.suelo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Transient;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborConfig;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Nutriente;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.ProyectionConstants;

@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity
public class Suelo extends Labor<SueloItem>{
	
	private static final String DOUBLE_TYPE = ":Double,";
	//utilizando una densidad aparente promedio para todos los sitios de 1,3. 
	//los nombres de las columnas estandar
	public static final String COLUMNA_N = "PPM_N";
	public static final String COLUMNA_P = "PPM_P";
	public static final String COLUMNA_K = "PPM_K";
	public static final String COLUMNA_S = "PPM_S";
	public static final String COLUMNA_MO = "PORC_MO";
	
	public static final String COLUMNA_DENSIDAD = "Densidad";
	public static final String COLUMNA_PROF_NAPA= "Prof_Nap";
	public static final String COLUMNA_AGUA_PERFIL= "Agua_Pe";
	
	public static final String COLUMNA_TEXTURA = "Textura";
	public static final String COLUMNA_POROSIDAD = "Porosidad";
	public static final String COLUMNA_CAPACIDAD_CAMPO = "Capacidad_Campo";
	
	public static enum SueloParametro{Nitrogeno,Fosforo,Potasio,Azufre,
		Calcio, Magnecio, Boro, Cloro, Cobalto, Cobre, Hierro, Manganeso, Molibdeno, Zinc,
		MateriaOrganica,Densidad,
		Napa,Agua,Textura,Porosidad,CapacidadCampo,Elevacion, 
		Area};

	//las propiedades que le permiten al usuario definir el nombre de sus columnas
	@Transient
	public StringProperty colNProperty;
	@Transient
	public StringProperty colPProperty;
	@Transient
	public StringProperty colKProperty;
	@Transient
	public StringProperty colSProperty;
	@Transient
	public StringProperty colMOProperty;
	@Transient
	public StringProperty colProfNapaProperty;
	@Transient
	public StringProperty colDensidadProperty;
	@Transient
	public StringProperty colAguaPerfProperty;
	
	public Suelo() {
		initConfig();
	}

	public Suelo(FileDataStore store) {
		super(store);
		initConfig();
	}


	@Override
	protected Double initPrecioLaborHa() {
		return 0.0;
	}
	
	@Override
	protected Double initPrecioInsumo() {
		return 0.0;
	}

	@Override
	public String getTypeDescriptors() {
		String type = Suelo.COLUMNA_N + DOUBLE_TYPE
				+ Suelo.COLUMNA_P + DOUBLE_TYPE
				+ Suelo.COLUMNA_K + DOUBLE_TYPE
				+ Suelo.COLUMNA_S + DOUBLE_TYPE
				
				+ SueloItem.Calcio.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Magnecio.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Boro.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Cloro.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Cobalto.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Cobre.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Hierro.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Manganeso.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Molibdeno.replace(" ","_") + DOUBLE_TYPE
				+ SueloItem.Zinc.replace(" ","_") + DOUBLE_TYPE
				
				+ Suelo.COLUMNA_MO + DOUBLE_TYPE		
				+ Suelo.COLUMNA_DENSIDAD + DOUBLE_TYPE
				
				+ Suelo.COLUMNA_PROF_NAPA + DOUBLE_TYPE
				+ Suelo.COLUMNA_AGUA_PERFIL + DOUBLE_TYPE
		
				+ Suelo.COLUMNA_TEXTURA + ":String,"//getTextura(),
				+ Suelo.COLUMNA_POROSIDAD + DOUBLE_TYPE
				+ Suelo.COLUMNA_CAPACIDAD_CAMPO + DOUBLE_TYPE;//	getPorcCC()
		System.out.println("cree suelo type "+type);
		return type;
	}

	/**
	 * 
	 * metodo que construye un SueloItem teniendo en cuenta las columnas estandar
	 */
	@Override
	public SueloItem constructFeatureContainerStandar(SimpleFeature next, boolean newIDS) {
		Labor<SueloItem> lab=this;
		SueloItem si = new SueloItem(next) {
			@Override
			public Double getAmount() {		
				
				return LaborItem.getDoubleFromObj(next.getAttribute(lab.getColAmount().get()));				
			}
		};
		super.constructFeatureContainerStandar(si,next,newIDS);
		si.setPpmNO3(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_N)));
		si.setPpmP(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_P)));
		si.setPpmK(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_K)));
		si.setPpmS(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_S)));
		
		si.setPpmCa(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Calcio.replace(" ","_"))));//los attributes estan en null
		si.setPpmMg(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Magnecio.replace(" ","_"))));
		si.setPpmB(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Boro.replace(" ","_"))));
		si.setPpmCl(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Cloro.replace(" ","_"))));
		si.setPpmCo(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Cobalto.replace(" ","_"))));
		si.setPpmCu(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Cobre.replace(" ","_"))));
		si.setPpmFe(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Hierro.replace(" ","_"))));
		si.setPpmMn(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Manganeso.replace(" ","_"))));
		si.setPpmMo(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Molibdeno.replace(" ","_"))));
		si.setPpmZn(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Zinc.replace(" ","_"))));	
		
		si.setPorcMO(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_MO)));
		si.setDensAp(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_DENSIDAD)));
		if(!(si.getDensAp()>0)) {
			
			si.setDensAp(SueloItem.DENSIDAD_SUELO_KG);//Densidad aparente 0-60);
		}
		
		si.setProfNapa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PROF_NAPA)));
		si.setAguaPerfil(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_AGUA_PERFIL)));

		si.setTextura((String) next.getAttribute(COLUMNA_TEXTURA));
		si.setPorosidad(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_POROSIDAD)));
		si.setPorcCC(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_CAPACIDAD_CAMPO)));
	

		return si;
	}

	/**
	 * metodo que construye un SueloItem teniendo en cuenta las columnas informadas
	 */
	@Override
	public SueloItem constructFeatureContainer(SimpleFeature next) {
		Labor<SueloItem> lab=this;
		SueloItem si = new SueloItem(next) {
			@Override
			public Double getAmount() {		
				
				return LaborItem.getDoubleFromObj(next.getAttribute(lab.getColAmount().get()));				
			}
		};
		super.constructFeatureContainer(si,next);
		si.setPpmNO3(LaborItem.getDoubleFromObj(next.getAttribute(this.colNProperty.get())));
		si.setPpmP(LaborItem.getDoubleFromObj(next.getAttribute(this.colPProperty.get())));
		si.setPpmK(LaborItem.getDoubleFromObj(next.getAttribute(this.colKProperty.get())));
		si.setPpmS(LaborItem.getDoubleFromObj(next.getAttribute(this.colSProperty.get())));
		
		si.setPpmCa(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Calcio.replace(" ","_"))));
		si.setPpmMg(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Magnecio.replace(" ","_"))));
		si.setPpmB(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Boro.replace(" ","_"))));
		si.setPpmCl(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Cloro.replace(" ","_"))));
		si.setPpmCo(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Cobalto.replace(" ","_"))));
		si.setPpmCu(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Cobre.replace(" ","_"))));
		si.setPpmFe(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Hierro.replace(" ","_"))));
		si.setPpmMn(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Manganeso.replace(" ","_"))));
		si.setPpmMo(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Molibdeno.replace(" ","_"))));
		si.setPpmZn(LaborItem.getDoubleFromObj(next.getAttribute(SueloItem.Zinc.replace(" ","_"))));	
		
		si.setPorcMO(LaborItem.getDoubleFromObj(next.getAttribute(this.colMOProperty.get())));
		si.setDensAp(LaborItem.getDoubleFromObj(next.getAttribute(this.colDensidadProperty.get())));
		if(!(si.getDensAp()>0)) {
			si.setDensAp(SueloItem.DENSIDAD_SUELO_KG);//Densidad aparente 0-60);
		}
		
		si.setAguaPerfil(LaborItem.getDoubleFromObj(next.getAttribute(this.colAguaPerfProperty.get())));
		si.setProfNapa(LaborItem.getDoubleFromObj(next.getAttribute(this.colProfNapaProperty.get())));

		si.setTextura((String) next.getAttribute(COLUMNA_TEXTURA));
		si.setPorosidad(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_POROSIDAD)));
		si.setPorcCC(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_CAPACIDAD_CAMPO)));
		
		
		return si;
	}

	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new SueloConfig();
		}
		return config;
	}

	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		

		Configuracion properties = getConfigLabor().getConfigProperties();

		colNProperty = PropertyHelper.initStringProperty(COLUMNA_N, properties, availableColums);
		colPProperty = PropertyHelper.initStringProperty(COLUMNA_P, properties, availableColums);
		colKProperty = PropertyHelper.initStringProperty(COLUMNA_K, properties, availableColums);
		colSProperty = PropertyHelper.initStringProperty(COLUMNA_S, properties, availableColums);
		colMOProperty = PropertyHelper.initStringProperty(COLUMNA_MO, properties, availableColums);
		
		colAguaPerfProperty = PropertyHelper.initStringProperty(COLUMNA_AGUA_PERFIL, properties, availableColums);
		colProfNapaProperty = PropertyHelper.initStringProperty(COLUMNA_PROF_NAPA, properties, availableColums);
		colDensidadProperty = PropertyHelper.initStringProperty(COLUMNA_DENSIDAD, properties, availableColums);
		//colAmount= colPProperty;
		colAmount= new SimpleStringProperty(COLUMNA_P);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		
	}

//	private Double getDensidad() {
//		return DENSIDAD_SUELO_KG;
//	}

	public void setPropiedadesLabor(SueloItem ci) {
//		ci.precioTnGrano =this.precioGranoProperty.get();
//		ci.costoLaborHa=this.precioLaborProperty.get();
//		ci.costoLaborTn=this.costoCosechaTnProperty.get();
		
	}

	/**
	 * metodo para convertir de partes por millon a kg en 0 a determinada profundidad
	 * @param ppm la densidad a convertir
	 * @param prof la profundidad de suelo a considerar la densidad
	 * @return la cantidad de kg que representa la densidad en la profundidad de suelo determinada
	 */
	public static double ppmToKg(double densidad, double ppm,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*densidad;
		Double kgNHa= (Double)ppm*kgSueloHa/1000000;//divido por un millon
		return kgNHa;
	}
	
	public static double kgToPpm(double densidad,double kg,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*densidad;
		Double ppm= (Double) kg*1000000/(kgSueloHa);//por un millon
		return ppm;
	}
	/**
	 * metodo para convertir de porcentaje a kg en 0 a determinada profundidad
	 * @param porc la densidad a convertir
	 * @param prof la profundidad de suelo a considerar la densidad
	 * @return la cantidad de kg que representa la densidad en la profundidad de suelo determinada
	 */
	public double porcToKg(double densidad, double porc,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*densidad;
		Double kgNHa= (Double)porc*kgSueloHa/100;//divido por cien
		return kgNHa;
	}
	
	public double kgToPorc(double densidad, double kgHa,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*densidad;
		Double porc= (Double) kgHa*100/(kgSueloHa);//por 100
		return porc;
	}
	
	
	//NITROGENO
	
	/**
	 * 
	 * @param sueloItem
	 * @return devuelve los kg de N elemento disponible mas los que se van a mineralizar durante la campania por materia organica
	 */
	public static double getKgNHa(SueloItem item) {
		//double kgSueloHa0_20 = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//double kgNorganicoHa = item.getPpmMO()*kgSueloHa0_20*Fertilizante.porcN_MO*Fertilizante.porcMO_DISP_Campania*1000/100;//ver factor estacionalidad
		
		//double kgNorganicoHa=getKgMoHa(item)*Fertilizante.porcN_MO*Fertilizante.porcMO_DISP_Campania;
		
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.6*this.getDensidad();
		//Double kgNHa= (Double) item.getPpmN()*kgSueloHa*Fertilizante.porcN_NO3/1000000;
		
		Double kgNHa= ppmToKg(item.getDensAp(),item.getPpmNO3(),0.6)*Nutriente.porcN_NO3;
		return kgNHa;		
	}
	
	/**
	 * convierte de kg de S por ha aplicados a densidad en ppm 0-20cm
	 * @param kgNHa
	 * @return la densidad en ppm para 0-20cm en el suelo 
	 */
	public double calcPpmNHaKg(Double densidad, Double kgNHa) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.6*this.getDensidad();
		//Double ppmN= (Double) kgNHa*1000000/(kgSueloHa*Fertilizante.porcN_NO3);
		return kgToPpm(densidad,kgNHa,0.6)/Nutriente.porcN_NO3;//convierto de n elemento a N03 para poder comparar con los analisis de laboratorio		
	}
	
	/**
	 * convierte de kg de N por ha aplicados a densidad en ppm 0-60cm
	 * @param kgNHa
	 * @return la densidad en ppm para 0-60cm en el suelo 
	 */
	public double calcPpmSHaKg(Double densidad, Double kgNHa) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.6*this.getDensidad();
		//Double ppmN= (Double) kgNHa*1000000/(kgSueloHa*Fertilizante.porcN_NO3);
		return kgToPpm(densidad,kgNHa,0.2)/Nutriente.porcS_SO4;//convierto de n elemento a N03 para poder comparar con los analisis de laboratorio		
	}
	
	
	
	//MATERIA ORGANICA
	public double getKgMoHa(SueloItem item) {
		//double kgSueloHa0_20 = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double kgNHa= (Double) item.getPpmMO()*kgSueloHa0_20/1000000;//no es un por millon
		//Double kgNHa= (Double) item.getPpmMO()*kgSueloHa0_20/100;//es un porcentaje
		return porcToKg(item.getDensAp(),item.getPorcMO(), 0.2);		
	}
	
	public double calcPorcMoHaKg(Double densidad,Double kgMoHa) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double ppmN= (Double) kgMoHa*100/(kgSueloHa);
		return kgToPorc(densidad,kgMoHa, 0.2);
	}
	
	public double getKgNOrganicoHa(SueloItem item) {
		double kgNorganicoHa=getKgMoHa(item)*Fertilizante.porcN_MO*Fertilizante.porcMO_DISP_Campania;
		return kgNorganicoHa;
	}
	
	// no dividir por el peso del pentoxido. todos los pesos son de kg de P
	// FOSFORO
	public static double getKgPHa(SueloItem item) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double kgNHa= (Double) item.getPpmP()*kgSueloHa*Fertilizante.porcP_PO4/1000000;
		//la densidad se corrije al crear el elemento en el task
		return ppmToKg(item.getDensAp(),item.getPpmP(),0.2);//*Fertilizante.porcP_PO4;		
	}
	
	/**
	 * 
	 * @param item
	 * @return un map con los kg de nutrientes por ha del item
	 */
	public static Map<SueloParametro,Double> getKgNutrientes(SueloItem item) {
		Map<SueloParametro,Double> nutrientesSuelo = new ConcurrentHashMap<SueloParametro,Double>();
		//Nutriente nitrogeno = Nutriente.getNutriente(Nutriente.NITROGENO);
		//Double kgNHa= ppmToKg(item.getDensAp(),item.getPpmNO3(),0.6)*Fertilizante.porcN_NO3;
		//nutrientes.put(nitrogeno,  ppmToKg(item.getDensAp(),item.getPpmNO3(),nitrogeno.getProfundidad())*nitrogeno.getPorcNutrienteEnMolecula());
		Map<SueloParametro,Nutriente> nutrientesMap = Nutriente.getNutrientesDefault();
		for(SueloParametro p : nutrientesMap.keySet()) {
		//nutrientesMap.keySet().forEach(p->{
			Nutriente n = nutrientesMap.get(p);
			if(n==null) {
				System.out.println("el nutriente para el parametro "+p+" es null");
				continue;}
			//System.out.println("Obteniendo los kg de Nutriente para "+n.getNombre());
			Double ppm =Suelo.getPpm(p, item);
			if(ppm!=null) {
			nutrientesSuelo.put(p,  
					ppmToKg(item.getDensAp(),
							ppm,
							n.getProfundidad())
					* n.getPorcNutrienteEnMolecula()
					);//n es null
			}
		}
	
		return nutrientesSuelo;
	}
	
	public static Double getPpm(SueloParametro p,SueloItem item) {
		Double ppm=0.0;
		switch(p) {
		case Nitrogeno: return item.getPpmNO3();
		case Fosforo: return item.getPpmP();
		case Potasio: return item.getPpmK();
		case Azufre: return item.getPpmS();
		case Calcio: return item.getPpmCa();
		case Magnecio: return item.getPpmMg();
		case Boro: return item.getPpmB();
		case Cloro: return item.getPpmCl();
		case Cobalto: return item.getPpmCo();
		case Cobre: return item.getPpmCu();
		case Hierro: return item.getPpmFe();
		case Manganeso: return item.getPpmMn();
		case Molibdeno: return item.getPpmMo();
		case Zinc: return item.getPpmZn();
	
		
		default: break;		
		}
		//XXX si se agregan nuevos nutrientes a suelo agregarlos a este switch
		return ppm;
	}
	
	public static void setPpm(SueloParametro p,SueloItem item,Double ppm) {		
		switch(p) {
		case Nitrogeno:  item.setPpmNO3(ppm);
		case Fosforo:  item.setPpmP(ppm);
		case Potasio:  item.setPpmK(ppm);
		case Azufre:  item.setPpmS(ppm);
		case Calcio:  item.setPpmCa(ppm);
		case Magnecio:  item.setPpmMg(ppm);
		case Boro:  item.setPpmB(ppm);
		case Cloro:  item.setPpmCl(ppm);
		case Cobalto:  item.setPpmCo(ppm);
		case Cobre:  item.setPpmCu(ppm);
		case Hierro:  item.setPpmFe(ppm);
		case Manganeso:  item.setPpmMn(ppm);
		case Molibdeno:  item.setPpmMo(ppm);
		case Zinc:  item.setPpmZn(ppm);	

		default: break;		
		}		
	}
	
	public double calcPpm_0_20(Double densidad,Double kgPHa) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double ppmP= (Double) kgPHa*1000000/(kgSueloHa*Fertilizante.porcP_PO4);
		return  kgToPpm(densidad, kgPHa,0.2);// /Fertilizante.porcP_PO4;	
	}
	/**
	 * metodo usado para relacionar una propiedad de una muestra con un SueloParametro
	 * @param s el nombre del sueloItem a encontrar
	 * @return devuelve el SueloParametro que correponde al sueloItem cuyo nombre sea s
	 */
	public static SueloParametro getSueloParametro(String s) {		
		switch(s) {
		case SueloItem.PPM_N: return SueloParametro.Nitrogeno;
		case SueloItem.PPM_FOSFORO: return SueloParametro.Fosforo;
		case SueloItem.PPM_POTASIO: return SueloParametro.Potasio;
		case SueloItem.PPM_ASUFRE: return SueloParametro.Azufre;
		
		case SueloItem.Calcio: return SueloParametro.Calcio;
		case SueloItem.Magnecio: return SueloParametro.Magnecio;
		case SueloItem.Boro: return SueloParametro.Boro;
		case SueloItem.Cloro: return SueloParametro.Cloro;
		case SueloItem.Cobalto: return SueloParametro.Cobalto;
		case SueloItem.Cobre: return SueloParametro.Cobre;
		case SueloItem.Hierro: return SueloParametro.Hierro;
		case SueloItem.Manganeso: return SueloParametro.Manganeso;
		case SueloItem.Molibdeno: return SueloParametro.Molibdeno;
		case SueloItem.Zinc: return SueloParametro.Zinc;
		
		case SueloItem.PC_MO: return SueloParametro.MateriaOrganica;
		case SueloItem.DENSIDAD: return SueloParametro.Densidad;
		case SueloItem.PROF_NAPA: return SueloParametro.Napa;
		case SueloItem.AGUA_PERFIL: return SueloParametro.Agua;
		case SueloItem.Textura: return SueloParametro.Textura;
		case SueloItem.Porosidad: return SueloParametro.Porosidad;
		case SueloItem.CapacidadCampo: return SueloParametro.CapacidadCampo;
		case SueloItem.ELEVACION: return SueloParametro.Elevacion;
		case SueloItem.Area: return SueloParametro.Area;		

		default: return null;		
		}
		//XXX si se agregan nuevos nutrientes a suelo agregarlos a este switch
		
		
		
	}
}
