package dao.suelo;

import java.util.List;

import javax.persistence.Transient;

import org.geotools.data.FileDataStore;
import org.opengis.feature.simple.SimpleFeature;

import dao.Labor;
import dao.LaborConfig;
import dao.LaborItem;
import dao.config.Configuracion;
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.utils.PropertyHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import utils.ProyectionConstants;

@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity
public class Suelo extends Labor<SueloItem>{
	private static final double DENSIDAD_SUELO_KG = 1.2*1000;//+-0.4 Arenoso 1650, franco 1400, arcilloso 1250
	//utilizando una densidad aparente promedio para todos los sitios de 1,3. 
	//los nombres de las columnas estandar
	public static final String COLUMNA_N = "PPM_N";
	public static final String COLUMNA_P = "PPM_P";
	public static final String COLUMNA_K = "PPM_K";
	public static final String COLUMNA_S = "PPM_S";
	public static final String COLUMNA_MO = "PORC_MO";
	
	public static final String COLUMNA_PROF_NAPA= "Prof_Nap";
	public static final String COLUMNA_AGUA_PERFIL= "Agua_Pe";

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
		String type = Suelo.COLUMNA_N + ":Double,"
				+ Suelo.COLUMNA_P + ":Double,"
				+ Suelo.COLUMNA_K + ":Double,"
				+ Suelo.COLUMNA_S + ":Double,"
				+ Suelo.COLUMNA_MO + ":Double,"
				+ Suelo.COLUMNA_PROF_NAPA + ":Double,"
				+ Suelo.COLUMNA_AGUA_PERFIL + ":Double";

		return type;
	}

	@Override
	public SueloItem constructFeatureContainerStandar(SimpleFeature next, boolean newIDS) {
		SueloItem si = new SueloItem(next);
		super.constructFeatureContainerStandar(si,next,newIDS);
		si.setPpmNO3(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_N)));
		si.setPpmP(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_P)));
		si.setPpmK(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_K)));
		si.setPpmS(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_S)));
		si.setPorcMO(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_MO)));
		si.setAguaPerfil(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_AGUA_PERFIL)));
		si.setProfNapa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PROF_NAPA)));
		return si;
	}

	@Override
	public SueloItem constructFeatureContainer(SimpleFeature next) {
		SueloItem si = new SueloItem(next);
		super.constructFeatureContainer(si,next);
		si.setPpmNO3(LaborItem.getDoubleFromObj(next.getAttribute(this.colNProperty.get())));
		si.setPpmP(LaborItem.getDoubleFromObj(next.getAttribute(this.colPProperty.get())));
		si.setPpmK(LaborItem.getDoubleFromObj(next.getAttribute(this.colKProperty.get())));
		si.setPpmS(LaborItem.getDoubleFromObj(next.getAttribute(this.colSProperty.get())));
		si.setPorcMO(LaborItem.getDoubleFromObj(next.getAttribute(this.colMOProperty.get())));
		si.setAguaPerfil(LaborItem.getDoubleFromObj(next.getAttribute(this.colAguaPerfProperty.get())));
		si.setProfNapa(LaborItem.getDoubleFromObj(next.getAttribute(this.colProfNapaProperty.get())));
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

		//colAmount= colPProperty;
		colAmount= new SimpleStringProperty(COLUMNA_P);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		
	}

	private Double getDensidad() {
		return DENSIDAD_SUELO_KG;
	}

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
	public double ppmToKg(double ppm,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*this.getDensidad();
		Double kgNHa= (Double)ppm*kgSueloHa/1000000;//divido por un millon
		return kgNHa;
	}
	
	public double kgToPpm(double kg,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*this.getDensidad();
		Double ppm= (Double) kg*1000000/(kgSueloHa);//por un millon
		return ppm;
	}
	/**
	 * metodo para convertir de porcentaje a kg en 0 a determinada profundidad
	 * @param ppm la densidad a convertir
	 * @param prof la profundidad de suelo a considerar la densidad
	 * @return la cantidad de kg que representa la densidad en la profundidad de suelo determinada
	 */
	public double porcToKg(double ppm,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*this.getDensidad();
		Double kgNHa= (Double)ppm*kgSueloHa/100;//divido por cien
		return kgNHa;
	}
	
	public double kgToPorc(double kg,double prof) {
		double kgSueloHa = ProyectionConstants.METROS2_POR_HA*prof*this.getDensidad();
		Double ppm= (Double) kg*100/(kgSueloHa);//por un millon
		return ppm;
	}
	
	
	//NITROGENO
	
	/**
	 * 
	 * @param sueloItem
	 * @return devuelve los kg de N elemento disponible mas los que se van a mineralizar durante la campania por materia organica
	 */
	public double getKgNHa(SueloItem item) {
		//double kgSueloHa0_20 = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//double kgNorganicoHa = item.getPpmMO()*kgSueloHa0_20*Fertilizante.porcN_MO*Fertilizante.porcMO_DISP_Campania*1000/100;//ver factor estacionalidad
		
		//double kgNorganicoHa=getKgMoHa(item)*Fertilizante.porcN_MO*Fertilizante.porcMO_DISP_Campania;
		
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.6*this.getDensidad();
		//Double kgNHa= (Double) item.getPpmN()*kgSueloHa*Fertilizante.porcN_NO3/1000000;
		
		Double kgNHa= ppmToKg(item.getPpmNO3(),0.6)*Fertilizante.porcN_NO3;
		return kgNHa;		
	}
	
	/**
	 * convierte de kg de N por ha aplicados a densidad en ppm 0-60cm
	 * @param kgNHa
	 * @return la densidad en ppm para 0-60cm en el suelo 
	 */
	public double calcPpmNHaKg(Double kgNHa) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.6*this.getDensidad();
		//Double ppmN= (Double) kgNHa*1000000/(kgSueloHa*Fertilizante.porcN_NO3);
		return kgToPpm(kgNHa,0.6)/Fertilizante.porcN_NO3;//convierto de n elemento a N03 para poder comparar con los analisis de laboratorio		
	}
	
	//MATERIA ORGANICA
	public double getKgMoHa(SueloItem item) {
		//double kgSueloHa0_20 = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double kgNHa= (Double) item.getPpmMO()*kgSueloHa0_20/1000000;//no es un por millon
		//Double kgNHa= (Double) item.getPpmMO()*kgSueloHa0_20/100;//es un porcentaje
		return porcToKg(item.getPorcMO(), 0.2);		
	}
	
	public double calcPorcMoHaKg(Double kgMoHa) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double ppmN= (Double) kgMoHa*100/(kgSueloHa);
		return kgToPorc(kgMoHa, 0.2);
	}
	
	public double getKgNOrganicoHa(SueloItem item) {
		double kgNorganicoHa=getKgMoHa(item)*Fertilizante.porcN_MO*Fertilizante.porcMO_DISP_Campania;
		return kgNorganicoHa;
	}
	
	//FIXME no dividir por el peso del pentoxido. todos los pesos son de kg de P
	// FOSFORO
	public double getKgPHa(SueloItem item) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double kgNHa= (Double) item.getPpmP()*kgSueloHa*Fertilizante.porcP_PO4/1000000;
		return ppmToKg(item.getPpmP(),0.2);//*Fertilizante.porcP_PO4;		
	}
	
	public double calcPpmPHaKg(Double kgPHa) {
		//double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.2*this.getDensidad();
		//Double ppmP= (Double) kgPHa*1000000/(kgSueloHa*Fertilizante.porcP_PO4);
		return  kgToPpm(kgPHa,0.2);// /Fertilizante.porcP_PO4;	
	}
}
