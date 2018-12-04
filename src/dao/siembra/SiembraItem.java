package dao.siembra;
import org.opengis.feature.simple.SimpleFeature;
import dao.LaborItem;

public class SiembraItem extends LaborItem {
//	private Double elevacion = new Double(0);	
	private Double dosisHa =new Double(0);	
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

	public void setDosisHa(Double rindeTnHa) {
		this.dosisHa = rindeTnHa;
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

	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
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
