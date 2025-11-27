package com.ursulagis.desktop.dao.cosecha;

import java.util.List;
import java.util.Optional;

import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborConfig;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.ordenCompra.ProductoLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.ursulagis.desktop.utils.CultivoHelper;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.ProyectionConstants;


@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity @Access(AccessType.FIELD)//variable (el default depende de donde pongas el @Id)
@NamedQueries({
	@NamedQuery(name=CosechaLabor.CosechaLaborConstants.FIND_ALL, query="SELECT c FROM CosechaLabor c") ,
	@NamedQuery(name=CosechaLabor.CosechaLaborConstants.FIND_NAME, query="SELECT o FROM CosechaLabor o where o.nombre = :name") ,
	@NamedQuery(name=CosechaLabor.CosechaLaborConstants.FIND_ACTIVOS, query="SELECT o FROM CosechaLabor o where o.activo = true") ,
}) 
public class CosechaLabor extends Labor<CosechaItem> {	
	public static class CosechaLaborConstants{
	public static final String FIND_ALL="CosechaLabor.findAll";
	public static final String FIND_NAME = "CosechaLabor.findName";
	public static final String FIND_ACTIVOS = "CosechaLabor.findActivos";
	
	public static final int KG_POR_TN = 1000;
	public static final double FEET_TO_METERS = 0.3048;
	public static final double KG_POR_LIBRA = 0.453592;

	public static final String COLUMNA_VELOCIDAD = "Velocidad";
	public static final String COLUMNA_HUMEDAD_KEY = "Humedad";
	public static final String COLUMNA_RENDIMIENTO = "Rendimient";
	public static final String COLUMNA_DESVIO_REND = "DesvRendim";

	public static final String COLUMNA_PRECIO = "precio_gra";
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";


	public static final String PRECIO_GRANO = "precioGrano";
	public static final String CORRECCION_COSECHA = "CORRECCION_COSECHA";
	public static final String COSTO_COSECHA_TN = "COSTO_COSECHA_TN";
	public static final String COSTO_COSECHA_HA = "COSTO_COSECHA_HA";

	public static final String MAX_RINDE_KEY = "MAX_RINDE";
	public static final String MIN_RINDE_KEY = "MIN_RINDE";

	public static final String PRODUCTO_DEFAULT = "CultivoDefault";
	public static final String COLUMNA_COSTO_LB_HA = "CostoLbTn";
	public static final String COLUMNA_COSTO_LB_TN = "CostoLbHa";
	}
	
	public StringProperty colRendimiento= new SimpleStringProperty();
	
	
	
	public Cultivo cultivo= null;//FIXME producto no se puede guardar como una property
	public Double costoCosechaTn = 0.0;
	//public Double precioGrano = 0.0;
	//public DoubleProperty precioGranoProperty= new SimpleDoubleProperty();	 
	//public DoubleProperty costoCosechaTnProperty= new SimpleDoubleProperty();

	//TODO mover a tasks separados de la cosecha	 
	public DoubleProperty correccionCosechaProperty= new SimpleDoubleProperty();//es el porcentaje de 0-100 por que que hay que multiplicar el rinde 
	public DoubleProperty maxRindeProperty= new SimpleDoubleProperty();
	public DoubleProperty minRindeProperty= new SimpleDoubleProperty();


	/**
	 * constructor que sirve para crear una cosecha artificial cuando no tiene
	 * un datastore que la represente
	 */
	public CosechaLabor() {
		super();
		initConfig();
	}
	
	public CosechaLabor(CosechaLabor c) {
		super();
		initConfig();
		setCultivo(c.getCultivo());
		setFecha(c.getFecha());
		setPrecioInsumo(c.getPrecioInsumo());
		setPrecioLabor(c.getPrecioLabor());
		setCostoCosechaTn(c.getCostoCosechaTn());
		setClasificador(c.getClasificador().clone());
	}

	public CosechaLabor(FileDataStore store) {
		super(store);
		initConfig();
	}

	//XXX ver como los listeners de las propiedades me afectan el archivo de properties y 
	//el controller de la configuracion. creo que setea las variables pero nunca las graba a menos 
	//que las grabe el controller
	protected void initConfig() {
		this.productoLabor = DAH.getProductoLabor(ProductoLabor.LABOR_DE_COSECHA);
		//System.out.println("iniciando la configuracion de CosechLabor");
		List<String> availableColums = this.getAvailableColumns();		
		Configuracion properties = getConfiguracion().getConfigProperties();

		colRendimiento = PropertyHelper.initStringProperty(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO, properties, availableColums);
		//como detecto que es una cosecha default evito hacer correcciones de flow y de distancia
		if(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO.equals(colRendimiento.get())){
			this.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
			this.getConfiguracion().correccionRindeProperty().setValue(false);
			this.getConfiguracion().valorMetrosPorUnidadDistanciaProperty().set(1);
		}
		colAmount= new SimpleStringProperty(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

		correccionCosechaProperty = PropertyHelper.initDoubleProperty(CosechaLabor.CosechaLaborConstants.CORRECCION_COSECHA, "100", properties);
		minRindeProperty = PropertyHelper.initDoubleProperty(CosechaLabor.CosechaLaborConstants.MIN_RINDE_KEY, "0", properties);
		maxRindeProperty = PropertyHelper.initDoubleProperty(CosechaLabor.CosechaLaborConstants.MAX_RINDE_KEY, "20", properties);
		
		Double precioGrano = PropertyHelper.initDouble(CosechaLabor.CosechaLaborConstants.PRECIO_GRANO, "0", properties);
		this.setPrecioInsumo(precioGrano);
		costoCosechaTn = PropertyHelper.initDouble(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_TN, "0", properties);

		String productoKEY = properties.getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.PRODUCTO_DEFAULT, CultivoHelper.MAIZ);
		Cultivo cultDefault = null;
		try {
			cultDefault = DAH.getCultivo(productoKEY);
			//cultDefault = Cultivo.cultivos.get(productoKEY);//DAH.getCultivo(productoKEY);
		}catch(Exception e) {
			e.printStackTrace();
		}
		//productoProperty = new SimpleObjectProperty<Cultivo>(cultDefault);//values().iterator().next());
		cultivo=cultDefault;
//		cultivo.addListener((obs, bool1, bool2) -> {
//			//this.cultivo=bool2;
//			if(bool2!=null)properties.setProperty(CosechaLabor.PRODUCTO_DEFAULT,bool2.getNombre());
//		});
	}

	@Override
	protected Double initPrecioLaborHa(){
		try {
		return PropertyHelper.getDoubleConverter().parse(
				config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_HA,"0")
				).doubleValue();
		}catch(Exception e) {
			return 0.0;
		}
		//return Double.parseDouble(config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.COSTO_COSECHA_HA,"0"));
		//return PropertyHelper.initDoubleProperty(CosechaLabor.COSTO_COSECHA_HA,"0",config.getConfigProperties());
	} 
	
	@Override
	protected Double initPrecioInsumo() {
		try {
			return PropertyHelper.getDoubleConverter().parse(
					config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.PRECIO_GRANO,"0")
					).doubleValue();
		}catch(Exception e) {
			return 0.0;
		}	
		//return Double.parseDouble(config.getConfigProperties().getPropertyOrDefault(CosechaLabor.CosechaLaborConstants.PRECIO_GRANO,"0"));
		//return PropertyHelper.initDoubleProperty(CosechaLabor.PRECIO_GRANO,  "0", config.getConfigProperties());
	//	return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}

	
	
	@Override
	@Transient
	public String getTypeDescriptors() {
		String type = CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO + ":Double,"	
				+CosechaLabor.CosechaLaborConstants.COLUMNA_DESVIO_REND + ":Double,"	
				+CosechaLabor.CosechaLaborConstants.COLUMNA_COSTO_LB_HA + ":Double,"	
				+CosechaLabor.CosechaLaborConstants.COLUMNA_COSTO_LB_TN + ":Double,"	
				//+CosechaLabor.COLUMNA_VELOCIDAD + ":Double,"	
				+ CosechaLabor.CosechaLaborConstants.COLUMNA_PRECIO + ":Double,"
				+CosechaLabor.CosechaLaborConstants.COLUMNA_IMPORTE_HA+":Double";
		return type;
	}

	@Override
	public CosechaItem constructFeatureContainer(SimpleFeature harvestFeature) {
		CosechaConfig config = (CosechaConfig) super.config;
		CosechaItem ci = new CosechaItem(harvestFeature);
		super.constructFeatureContainer(ci,harvestFeature);

		Double rindeDouble = LaborItem.getDoubleFromObj(harvestFeature.getAttribute(colRendimiento.get()));

		if(this.getConfiguracion().correccionAnchoEnabled() 
				&& ci.getAncho()!=0.0){//si el ancho era cero no lo expando			
			ci.setAncho(this.anchoDefaultProperty.doubleValue());
		} 

		if (config.correccionFlowToRindeProperty().getValue()) {
			

			/*
			 * mass_flow[kg/s]=0.4535*lb/s ancho[m] velocidad[m/s] rinde[kg/ha] =
			 * 10000*mass_flow/(ancho*velocidad)
			 */

			// ("Mass_Flow_" *0.453592) /((("Width" *2.54/100)*
			// ("Distance"*2.54/100))/10000)
			/*
			 * convertir el rinde que es un flow en libras por segundo a kg por //o es en bushel??
			 * ha. para eso hay que usar la formula rinde = flow*[kg por
			 * libra]*[1 segundo]*[m2 por Ha]/(width*distance)
			 */
			double constantes = ProyectionConstants.METROS2_POR_HA
					* CosechaLabor.CosechaLaborConstants.KG_POR_LIBRA/CosechaLabor.CosechaLaborConstants.KG_POR_TN;// XXX asume un dato por segundo
			//si la distancia o el ancho son cero esto da infinito
			double divisor = ci.getDistancia() * ci.getAncho();
			if(divisor>0){
				rindeDouble = rindeDouble * constantes / (divisor);
			} 
			//TODO si existe el campo Moisture_s tomarlo encuenta para secar el grano		
			List<AttributeDescriptor> descriptors = harvestFeature.getType().getAttributeDescriptors();
		//	String moistureColumn =null;
			String col_moisture =this.config.getConfigProperties().getPropertyOrDefault(CosechaLaborConstants.COLUMNA_HUMEDAD_KEY, "Moisture_s");
			Optional<String> opt = descriptors.stream()
					.map(att->att.getName().toString())
					.filter(att->att.equalsIgnoreCase(col_moisture))
					.findAny();

			
			if(opt.isPresent()){
				Double moisture =LaborItem.getDoubleFromObj(harvestFeature.getAttribute(opt.get()));
				Double rindeH = ci.getRindeTnHa();
				Double k = 100/(100+moisture);
				ci.setRindeTnHa(rindeH*k);
				//ci.setPrecioTnGrano(this.precioGrano);
			}
			
			//CAMBIO LA ELEVACION DE PIES A METROS
			String convertir =this.config.getConfigProperties().getPropertyOrDefault("ConvertirElevacionPiesAMetros", "true");
			if("true".equals(convertir)){
				//double feetToMeters =;
				ci.setElevacion(ci.getElevacion()*CosechaLabor.CosechaLaborConstants.FEET_TO_METERS);				
			}
		}

		double correccionRinde = correccionCosechaProperty.doubleValue()/100;
		ci.rindeTnHa = rindeDouble * (correccionRinde);		
		setPropiedadesLabor(ci);
		return ci;
	}

	public void setPropiedadesLabor(CosechaItem ci){
		ci.precioTnGrano = this.getPrecioInsumo();//precioGranoProperty.get();
		ci.costoLaborHa=this.getPrecioLabor();
		ci.costoLaborTn=this.getCostoCosechaTn();
	}
	


	
	@Override
	public  CosechaItem constructFeatureContainerStandar(
			SimpleFeature harvestFeature,boolean newIDS) {
		CosechaItem ci = new CosechaItem(harvestFeature);
		super.constructFeatureContainerStandar(ci,harvestFeature,newIDS);
		ci.rindeTnHa = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO));
		ci.desvioRinde = LaborItem.getDoubleFromObj(harvestFeature
				.getAttribute(CosechaLabor.CosechaLaborConstants.COLUMNA_DESVIO_REND));

		setPropiedadesLabor(ci);//para que se actualice si se edito la labor
//		ci.precioTnGrano = LaborItem.getDoubleFromObj(harvestFeature
//				.getAttribute(CosechaLabor.COLUMNA_PRECIO));
//
//		ci.costoLaborHa=LaborItem.getDoubleFromObj(harvestFeature
//				.getAttribute(CosechaLabor.COLUMNA_COSTO_LB_HA));
//		ci.costoLaborTn=LaborItem.getDoubleFromObj(harvestFeature
//				.getAttribute(CosechaLabor.COLUMNA_COSTO_LB_TN));

		return ci;
	}

	//se usa en los tasks
	public CosechaConfig getConfiguracion() {
		if(config==null){
			config = new CosechaConfig();
		}
		return (CosechaConfig) config;
	}

	@Override
	public LaborConfig getConfigLabor() {
		return getConfiguracion();
	}
	
	@ManyToOne
	public Cultivo getCultivo() {
		return this.cultivo;
	}
	
	public void setCultivo(Cultivo c) {
		//System.out.println("camniando el cultivo de "+getCultivo()+" a "+c);
//		if(productoProperty==null) {
//			productoProperty = new SimpleObjectProperty<Cultivo>();
//		}
		this.cultivo=c;
		//System.out.println("el cultivo quedo en "+getCultivo());
	}


}
