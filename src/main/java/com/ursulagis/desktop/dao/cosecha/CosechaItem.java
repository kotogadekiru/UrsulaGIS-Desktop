package com.ursulagis.desktop.dao.cosecha;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.persistence.Entity;

import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.LaborItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)//si no pones esto todos los hashmaps andan mal y grillar cosecha no anda
@Entity
public class CosechaItem extends LaborItem {	
	Double rindeTnHa=0.0;
	Double desvioRinde=0.0;
	Double precioTnGrano=0.0;
	Double importeHa=0.0;
	//Double velocidad=0.0;
    Double costoLaborHa=0.0;
    Double costoLaborTn=0.0;
	
	public CosechaItem() {
		super();
	}
	public CosechaItem(SimpleFeature feature){
		super(feature);
	}

	public Double getRindeTnHa() {
		return rindeTnHa;
	}

	public void setRindeTnHa(Double rindeTnHa) {
		this.rindeTnHa = rindeTnHa;
	}
	
	public void setAmount(Double amount) {
		this.rindeTnHa = amount;
	}

	public Double getPrecioTnGrano() {
		return precioTnGrano;
	}

	public void setPrecioTnGrano(Double precioTnGrano) {
		this.precioTnGrano = precioTnGrano;
	}

	public Double getImporteHa() {
		this.importeHa =  this.rindeTnHa *(this.precioTnGrano-this.costoLaborTn)-costoLaborHa;
		
		return importeHa;
	}


	public void setImporteHa(Double importeHa) {
		this.importeHa = importeHa;
	}


	@Override
	public Double getAmount() {
		return getRindeTnHa();
	}


	

	/*
	 * type = DataUtilities.createType("Cosecha","*geom:Polygon,"
							+ CosechaLabor.COLUMNA_DISTANCIA+":Double,"
							+ CosechaLabor.COLUMNA_CURSO+":Double,"
							+ CosechaLabor.COLUMNA_ANCHO+":Double,"
							+ CosechaLabor.COLUMNA_RENDIMIENTO+":Double,"
							+ CosechaLabor.COLUMNA_VELOCIDAD+":Double,"
							+ CosechaLabor.COLUMNA_ELEVACION+":Double,"
							+ CosechaLabor.COLUMNA_PRECIO+":Double,"
							+CosechaLabor.COLUMNA_IMPORTE_HA+":Double"
	 */




	/**
	 * @return the costoLaborHa
	 */
	public Double getCostoLaborHa() {
		return costoLaborHa;
	}




	/**
	 * @param costoLaborHa the costoLaborHa to set
	 */
	public void setCostoLaborHa(Double costoLaborHa) {
		this.costoLaborHa = costoLaborHa;
	}




	/**
	 * @return the costoLaborTn
	 */
	public Double getCostoLaborTn() {
		return costoLaborTn;
	}




	/**
	 * @param costoLaborTn the costoLaborTn to set
	 */
	public void setCostoLaborTn(Double costoLaborTn) {
		this.costoLaborTn = costoLaborTn;
	}




	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getRindeTnHa(),
				getDesvioRinde(),
				getCostoLaborHa(),
				getCostoLaborTn(),
				//getVelocidad(),	
				getPrecioTnGrano(),
				getImporteHa()
		};
		return elements;
	}

	/**
	 * @return the desvioRinde
	 */
	public Double getDesvioRinde() {
		return desvioRinde;
	}



	/**
	 * @param desvioRinde the desvioRinde to set
	 */
	public void setDesvioRinde(Double desvioRinde) {
		this.desvioRinde = desvioRinde;
	}



	/**
	 * 
	 * @param d
	 * @return devuelve el numero ingresado redondeado a 3 decimales
	 */
	private double round(double d){
		try {
			BigDecimal bd = new BigDecimal(d);//java.lang.NumberFormatException: Infinite or NaN
			bd = bd.setScale(3, RoundingMode.HALF_UP);
			return bd.doubleValue();
		} catch (Exception e) {
			System.err.println("CosechaItem::round "+d);
			//e.printStackTrace();
			return 0;
		}

	}
	@Override
	public String toString() {
		return "CosechaItem [distancia=" + distancia + ", rumbo=" + rumbo
				+ ", ancho=" + ancho + ", rindeTnHa=" + rindeTnHa + ", elevacion="
				+ elevacion + ", precioTnGrano=" + precioTnGrano + ", importeHa="
				+ importeHa + ", id=" + id + ""
				+ "]";
	}
	
//	public int hashCode() {
//		return super.hashCode();
//	}
//	
//	public boolean equals(Object o) {
//		return super.equals(o);
//	}
}
