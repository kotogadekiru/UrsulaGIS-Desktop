package dao.cosecha;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.opengis.feature.simple.SimpleFeature;

import dao.FeatureContainer;

public class CosechaItem extends FeatureContainer{	
	//private static final int KG_POR_TN = 1000;

	//	public static Double CosechaID =new Double(0);//XXX este id no es global sino que depende de la labor
	//TODO grear un generador de ids para la labor
	//double toMetros=1;//HarvestFiltersConfig.getInstance().getMetrosPorUnidadDistancia();	
	//private static Double correccionRinde = new Double(100);

	//Double distancia =new Double(0);
	//Double rumbo=new Double(0);
	//Double ancho=new Double(0);
	Double rindeTnHa=new Double(0);
	Double desvioRinde=new Double(0);
	//Double elevacion=new Double(0);
	Double precioTnGrano=new Double(0);
	Double importeHa=new Double(0);
	//	Double pasada=new Double(0);

	Double velocidad=new Double(0);



	public CosechaItem(SimpleFeature feature){
		super(feature);
		//		synchronized(CosechaItem.CosechaID){//esto parece innecesario. el problema de los bloques faltantes esta al exportar
		//	this.id=getNextID();
		//		}
	}
	//	@Deprecated
	//	public CosechaItem(SimpleFeature harvestFeature, Double precioGrano) {
	//		super(harvestFeature);
	//		precioTnGrano=precioGrano;
	//		
	////	//	double toMetros=HarvestFiltersConfig.getInstance().getMetrosPorUnidadDistancia();
	////		
	////		String distColumn = getColumn(COLUMNA_DISTANCIA);//TODO pasar el constructor de cosechaItem a la labor
	////		try{
	////			distancia = new Double(distColumn);
	////		}catch(Exception e){
	////			Object distAttribute = harvestFeature.getAttribute(distColumn);
	////		
	////		distancia =
	////				super.getDoubleFromObj(distAttribute)*toMetros;		
	////		}
	////		
	////		rumbo =super.getDoubleFromObj(harvestFeature.getAttribute(getColumn(COLUMNA_CURSO)));
	////		
	////		pasada =super.getDoubleFromObj(harvestFeature.getAttribute(getColumn(COLUMNA_PASADA)));
	////		
	////		String identifier = harvestFeature.getIdentifier().getID();
	////		String[] split = identifier.split("\\.");
	////	
	////		id =super.getDoubleFromObj(split[split.length-1]);
	////		
	////
	////		String anchoColumn = getColumn(COLUMNA_ANCHO);
	//////		try{
	//////			ancho = new Double(anchoColumn);
	//////		}catch(Exception e){
	//////			Object distAttribute = harvestFeature.getAttribute(distColumn);
	//////		
	//////			ancho =
	//////				super.getDoubleFromObj(distAttribute)
	//////				*toMetros;		
	//////		}
	////		
	////		ancho = super.getDoubleFromObj(harvestFeature.getAttribute(anchoColumn));
	////		ancho=ancho*toMetros;
	////			
	////		Double rindeDouble =  super.getDoubleFromObj(harvestFeature.getAttribute(getColumn( COLUMNA_RENDIMIENTO)));
	////		
	////		if(rindeDouble>100){//esta en kilogramos
	////			rindeTnHa = rindeDouble *(correccionRinde/100);//KG_POR_TN ;
	////		} else if(rindeDouble <1){ //esta en decenas de toneladas?
	////		
	////				rindeTnHa = rindeDouble *(correccionRinde/100);// *4.28;
	////		}else{
	////			rindeTnHa = rindeDouble *(correccionRinde/100) ;
	////		}
	////	
	////
	////		elevacion = super.getDoubleFromObj(harvestFeature.getAttribute(getColumn(COLUMNA_VELOCIDAD)))*toMetros;
	////		this.precioTnGrano = precioGrano;
	////		this.importeHa = rindeTnHa * this.precioTnGrano;
	//	}



	public CosechaItem() {
		//this.id=nextId;
		//	this.id=getNextID();
	}

	//	private synchronized Double getNextID(){		
	//		Double ret = CosechaItem.CosechaID;
	//		CosechaItem.CosechaID++;
	//		//System.out.println("devolviendo el nextID para la cosecha = "+ret);
	//		return new Double(ret);
	//	}

//	public Double getDistancia() {
//		return distancia;
//	}
//
//	public void setDistancia(Double distancia) {
//		this.distancia = distancia;
//	}
//
//	public Double getRumbo() {
//		return rumbo;
//	}

//	public void setRumbo(Double rumbo) {
//		this.rumbo = rumbo;
//	}
//
//	public Double getAncho() {
//		return ancho;
//	}

	//	public Double getPasada() {
	//		return pasada;
	//	}





	public Double getRindeTnHa() {
		return rindeTnHa;
	}

	public void setRindeTnHa(Double rindeTnHa) {
		this.rindeTnHa = rindeTnHa;
	}



	public Double getPrecioTnGrano() {
		return precioTnGrano;
	}

	public void setPrecioTnGrano(Double precioTnGrano) {
		this.precioTnGrano = precioTnGrano;
	}

	public Double getImporteHa() {
		this.importeHa = rindeTnHa * this.precioTnGrano;
		return importeHa;
	}

	private double getVelocidad() {
		return velocidad;
	}

	public void setImporteHa(Double importeHa) {
		this.importeHa = importeHa;
	}

	//	@Deprecated //use CosechaLabor.getRequieredcolumns
	//	public static List<String> getRequieredColumns() {
	//		List<String> requiredColumns = new ArrayList<String>();
	//		requiredColumns.add(COLUMNA_VELOCIDAD);		
	//		requiredColumns.add(COLUMNA_RENDIMIENTO);	
	//		requiredColumns.add(COLUMNA_ANCHO);	
	//		requiredColumns.add(COLUMNA_CURSO);	
	//		requiredColumns.add(COLUMNA_DISTANCIA);	
	//		requiredColumns.add(COLUMNA_PASADA);	
	//	//	requiredColumns.add(COLUMNA_ID);	
	//		return requiredColumns;
	//	}

	@Override
	public Double getAmount() {
		return getRindeTnHa();
	}




	//	@Override
	//	protected Map<String, String> getColumnsMap() {
	//		return CosechaItem.columnsMap;
	//	}
	//
	//	public static String getColumnName(String internalName){
	//		return CosechaItem.columnsMap.get(internalName);
	//	}


	//	public static void setColumnsMap(Map<String, String> columns) {
	//		CosechaItem.columnsMap.clear();
	//		CosechaItem.columnsMap.putAll(columns);		
	//		
	//		columns.forEach(new BiConsumer<String, String>(){
	//			@Override
	//			public void accept(String key, String value) {
	//				Configuracion.getInstance().setProperty(key, value);				
	//			}
	//			
	//		});
	//		
	//	}



	//	public static void setCorreccionRinde(Double correccionRinde1) {
	//		//System.out.println("nuevo correccion rinde es "+correccionRinde1);
	//		correccionRinde  = correccionRinde1;
	//	}


	/*
	 * type = DataUtilities.createType("Cosecha","*geom:Polygon,"
							+ CosechaLabor.COLUMNA_DISTANCIA+":Double,"
							+ CosechaLabor.COLUMNA_CURSO+":Double,"
							+ CosechaLabor.COLUMNA_ANCHO+":Double,"
							+ CosechaLabor.COLUMNA_RENDIMIENTO+":Double,"
							+ CosechaLabor.COLUMNA_VELOCIDAD+":Double,"
							+ CosechaLabor.COLUMNA_ELEVACION+":Double,"
							+ CosechaLabor.COLUMNA_PRECIO+":Double,"
							+CosechaLabor.COLUMNA_IMPORTE_HA+":Double"*/




	@Override
	public Object[] getSpecialElementsArray() {
		Object[] elements = new Object[]{
				getRindeTnHa(),
				getDesvioRinde(),
				round(getVelocidad()),	
				round(getPrecioTnGrano()),
				round(getImporteHa())
		};
		return elements;
	}

//	public SimpleFeature getPointFeature(SimpleFeatureBuilder featureBuilder) {
//		return super.getFeatureAsPoint(featureBuilder);
//		
////		featureBuilder.addAll(new Object[]{
////				super.getGeometry().getCentroid(),
////				round(super.distancia),
////				super.rumbo,
////				round(super.ancho),
////				round(super.elevacion),
////				getCategoria(),
////				getRindeTnHa(),
////				round(getVelocidad()),	
////				round(getPrecioTnGrano()),
////				round(getImporteHa())
////				});	
////
////		//System.out.println("construyendo el simplefeature para el id:"+this.getId());//construuendo el simplefeature para el id:0.0
////		SimpleFeature feature = featureBuilder.buildFeature("\\."+this.getId().intValue());
////
////		return feature;
//	}

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
		//return d;
		//return Math.round(d*100)/100;
		//	
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
				+ importeHa + ", id=" + id + ", velocidad="
				+ velocidad + "]";
	}
}
