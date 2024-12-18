package dao.siembra;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;

import org.opengis.feature.simple.SimpleFeature;

import dao.LaborItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
public class SiembraItem extends LaborItem {	
	private Double dosisHa =new Double(0);	
	private Double dosisML =new Double(0);	//dosis metro lineal
	private Double precioInsumo= new Double(0);	
	private Double costoLaborHa=new Double(0);	
	private Double importeHa=new Double(0);	
	private Double dosisFertLinea=new Double(0);
	private Double dosisFertCostado=new Double(0);

	public SiembraItem(SimpleFeature harvestFeature) {
		super(harvestFeature);
	}

	public SiembraItem() {
		super();
	}

	public Double getDosisHa() {
		return dosisHa;
	}

	public void setDosisHa(Double kgHa) {
		this.dosisHa = kgHa;
	}

	public Double getPrecioInsumo() {
		return precioInsumo;
	}

	public void setPrecioInsumo(Double precio) {
		this.precioInsumo = precio;
	}

	public Double getImporteHa() {
		this.importeHa =  (dosisHa * precioInsumo + costoLaborHa);
		return importeHa;
	}

	public void setImporteHa(Double doubleFromObj) {
		this.importeHa = doubleFromObj;	
	}
	
	
	
	/**
	 * @return the dosisFertLinea
	 */
	public Double getDosisFertLinea() {
		return dosisFertLinea;
	}

	/**
	 * @param dosisFertLinea the dosisFertLinea to set
	 */
	public void setDosisFertLinea(Double dosisFertLinea) {
		this.dosisFertLinea = dosisFertLinea;
	}

	/**
	 * @return the dosisFertCostado
	 */
	public Double getDosisFertCostado() {
		return dosisFertCostado;
	}

	/**
	 * @param dosisFertCostado the dosisFertCostado to set
	 */
	public void setDosisFertCostado(Double dosisFertCostado) {
		this.dosisFertCostado = dosisFertCostado;
	}

	@Override
	public Double getAmount() {
		return getDosisHa();
	}

	public void setAmount(Double amount) {		
		setDosisHa(amount);		
}
	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getDosisML(),
				getDosisHa(),
				getDosisFertLinea(),
				getDosisFertCostado(),
				getPrecioInsumo(),
				getCostoLaborHa(),
				getImporteHa()
		};
		return elements;
	}
	

	/**
	 * @return the precioPasada
	 */
	public Double getCostoLaborHa() {
		return costoLaborHa;
	}



	/**
	 * @param precioPasada the precioPasada to set
	 */
	public void setCostoLaborHa(Double precioPasada) {
		this.costoLaborHa = precioPasada;
	}
}
