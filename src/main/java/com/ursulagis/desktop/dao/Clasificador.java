package com.ursulagis.desktop.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.JenksNaturalBreaksFunction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.Messages;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import lombok.Data;
import com.ursulagis.desktop.utils.NaturalBreaks;
import com.ursulagis.desktop.utils.ProyectionConstants;
import com.ursulagis.desktop.utils.UrsulaJenksNaturalBreaksFunction;
@Data
public class Clasificador {
	private static final String CLASIFICADOR_MIN_AREA_JENKINS = "Clasificador.MIN_AREA_JENKINS";
	public static final String NUMERO_CLASES_CLASIFICACION = "NUMERO_CLASES_CLASIFICACION";
	private static final String CLASIFICADOR_JENKINS = "Jenkins";
	private static final String CLASIFICADOR_DESVIOSTANDAR = "Desvio Standar";
	public static final String TIPO_CLASIFICADOR = "CLASIFICADOR";

	public static final  String[] clasficicadores = {CLASIFICADOR_DESVIOSTANDAR,CLASIFICADOR_JENKINS};

	//paleta de colores del logo:
	//verde oscuro : Color.rgb(59,136,67),
	//verde claro : Color.rgb(116,182,71),
	//azul : Color.rgb(19,119,165),
	//azul oscuro : Color.rgb(35,39,91),
	
	public static Color[] colors = {//9 colores del 0 al 8 
			//Color.rgb(158,1,66),//0
			//Color.rgb(213,62,79),//1
			Color. rgb(244,109,67),//0 desde min a -inf
			Color. rgb(253,174,97),//1 min a min+1
			Color. rgb(254,224,139),//2 min +1 min +2
			Color. rgb(255,255,191),//3 min +2 a min +3
			Color. rgb(230,245,152)	,//4 min +3 a min +4
			Color. rgb(171,221,164),//5 de min +4 a min +5
			Color.rgb(102,194,165),//6 de min +5 a min +6
			Color.rgb(50,136,189),//7 de min + 6 a min +7
			Color.DARKBLUE};	  //8 de min + 7 a +inf
	//		Color.rgb(94,79,162)
	//		};
	
	public static Map<Color,java.awt.Color> awtColorMap = new HashMap<Color,java.awt.Color>();
	public static char[] abc = "ABCDEFGHIJKLM".toCharArray();
	public static String cba = "HGFEDCBA";

	private  Double[] histograma=null;// es static para poder hacer constructHistograma static para usarlo en el grafico de Histograma
	private  Classifier clasifier=null;
	public StringProperty tipoClasificadorProperty;
	public IntegerProperty clasesClasificadorProperty;
	private boolean initialized=false;

	public Clasificador(){
		tipoClasificadorProperty = new SimpleStringProperty();		

	}

	public Clasificador clone() {
		Clasificador clon = new Clasificador();
		clon.clasesClasificadorProperty = new SimpleIntegerProperty(this.clasesClasificadorProperty.get());
		clon.tipoClasificadorProperty= new SimpleStringProperty(this.tipoClasificadorProperty.get());
		clon.initialized=this.initialized;

		if(this.histograma!=null) {
			clon.histograma=this.histograma.clone();
		}else {
			clon.clasifier=this.clasifier;
		}
		return clon;
	}

	public String getCategoryNameFor(int index) {		
		String rangoIni = "";
		NumberFormat df = Messages.getNumberFormat();
//		DecimalFormat df = new DecimalFormat("0.00");
//		df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("EN")));
//		df.setGroupingSize(3);
//		df.setGroupingUsed(true);
		if(histograma != null ){
			//	Double delta = histograma[1]-histograma[0];
			if(histograma.length==0) {// se creo el histograma pero no hay nigun limite porque hay una sola clase
				rangoIni = "-inf ~ +inf";//java.lang.ArrayIndexOutOfBoundsException: 0
			}else if(index == 0){
				rangoIni = "-inf ~ "+ df.format(histograma[0]);//java.lang.ArrayIndexOutOfBoundsException: 0
			}else if(index < histograma.length ){
				rangoIni = df.format(histograma[index-1])+" ~ "+ df.format(histograma[index]);
			}else 	if(index == histograma.length){
				rangoIni = df.format(histograma[index-1])+" ~ +inf";//+(histograma[index]+delta);
			}
			return rangoIni;
		} else if(clasifier != null){			
			rangoIni = clasifier.getTitle(index);	
			String [] partesIni = rangoIni.split("\\.\\.");			
			return df.format(Double.parseDouble(partesIni[0]))+" ~ "+df.format(Double.parseDouble(partesIni[1]));// +"-"+histograma[j+1];
		}
		return "error";
		//		if(rangoIni!=null){
		//		
		//		} else{
		//			return "error";
		//		}
		//		System.err.println("Error no hay un clasificador seleccionado");
		//		return label;
	}

	/**
	 * metodo con el cual se se asigna la categoria al labor item
	 * @param rinde
	 * @return la categoria en la que cae el rinde
	 */
	public  Integer getCategoryFor(Double rinde) {				

		if(histograma != null){
			int absColor = getColorByHistogram(rinde, histograma);
			return absColor;
			//			System.out.println("obteniendo la clase para el ");
			//			return absColor*(colors.length-1)/clasesClasificadorProperty.get();

		} else if(clasifier != null){
			int absColor = getColorByJenks(rinde);
			return absColor;
			//	return absColor*(colors.length-1)/clasesClasificadorProperty.get();
		}
		//System.err.println("Error no hay un clasificador seleccionado");
		return 0;
		//return getColorByHue(rinde, rindeMin, rindeMax, porcent);
	}


	/**
	 * metodo con el cual se asigna la categoria si se asigno un histograma
	 * un elemento pertenece a una categoria si es menor o igual al limite en el histograma de esa categoria. 
	 * puede devolver valores entre 0 y histo.length()+1
	 * 
	 * @param rinde
	 * @param histo arreglo de los limites de las categorias
	 * @return
	 */
	public static int getColorByHistogram(Double rinde, Double[] histo) {
		int colorIndex = histo.length;
		try {
		//	BigDecimal bd = new BigDecimal(rinde);//java.lang.NumberFormatException: Infinite or NaN
		//	bd = bd.setScale(2, RoundingMode.HALF_UP);
		//	rinde = bd.doubleValue();
			for (int i = histo.length-1; i > -1 ; i--) {
				double histoMax = histo[i];
				if (rinde <= histoMax) {
					colorIndex = i;
				}
			}

			//	 System.out.println("Histograma color Index for rinde "+rinde+" is "+colorIndex);

			return colorIndex;
		} catch (Exception e) {
			//	System.err.println("getColorsByHistogram "+rinde);
			//e.printStackTrace();
			return 0;
		}
	}


	public  Classifier constructUrsulaJenksClasifier(SimpleFeatureCollection collection,String amountColumn){
		histograma = null;
		//TODO usar config. ancho grilla^2/10000
		//Sup minima relevante 10m^2
		Configuracion config = Configuracion.getInstance();
		DecimalFormat dc = PropertyHelper.getDoubleConverter();		
		Number anchoGrilla = 70;
		try {
			anchoGrilla = dc.parse(config.getPropertyOrDefault(CLASIFICADOR_MIN_AREA_JENKINS,"70"));
		} catch (ParseException e) {		
			e.printStackTrace();
		}

		double minArea = anchoGrilla.doubleValue()/(ProyectionConstants.METROS2_POR_HA*ProyectionConstants.A_HAS());//area en longLat
		NaturalBreaks func = new NaturalBreaks(amountColumn,this.getNumClasses(),minArea);
		//UrsulaJenksNaturalBreaksFunction func = new UrsulaJenksNaturalBreaksFunction(amountColumn,this.getNumClasses(),minArea);


		//TODO construir una colleccion equivalente pero donde cada feature tenga la misma superficie
		//10m^2 para que tengan el mismo peso relativo
		if(collection.size()>0){
			System.out.println("evaluando la colleccion para poder hacer jenkins");


			clasifier = (Classifier) func.evaluate(collection);//XXX esto demora unos segundos!

			System.out.println(Arrays.toString(clasifier.getTitles())+" size: "+clasifier.getSize());
		} else{
			System.out.println("no se pudo evaluar jenkins porque la coleccion de datos es de tamanio cero");
		}
		if(clasifier == null){
			System.out.println("No se pudo evaluar la colleccion de features con el metodo de Jenkins");

		}
		//  int clase =   clasifier.classify(arg0)
		return clasifier;
	}
	
	public static StringConverter<String> clasificadorStringConverter() {
		return	new StringConverter<String>() {
			@Override
			public String fromString(String arg0) {				
				return null;
			}

			@Override
			public String toString(String arg0) {
				String s =(String)arg0;
				String key="Clasificador";
				switch (s) {
				case Clasificador.CLASIFICADOR_DESVIOSTANDAR:
					key="Clasificador.CLASIFICADOR_DESVIOSTANDAR";
					break;
				case Clasificador.CLASIFICADOR_JENKINS:
					key="Clasificador.CLASIFICADOR_JENKINS";
					break;
				}
				return Messages.getString(key);				
			}			
		};
	}

	public  Classifier constructJenksClasifier(SimpleFeatureCollection collection,String amountColumn){
		//JenksFunctionTest test = new JenksFunctionTest("jenksTest");
		histograma = null;
		FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

		//Literal classes = ff.literal(colors.length);
		Literal classes = ff.literal(this.getNumClasses());
		PropertyName expr = ff.property(amountColumn);

		JenksNaturalBreaksFunction func = (JenksNaturalBreaksFunction) ff.function("Jenks", expr,
				classes);



		//TODO construir una colleccion equivalente pero donde cada feature tenga la misma superficie
		//10m^2 para que tengan el mismo peso relativo
		if(collection.size()>0){
			System.out.println("evaluando la colleccion para poder hacer jenkins");
			//double areaLongLat = 100/ProyectionConstants.A_HAS();

			clasifier = (Classifier) func.evaluate(collection);//XXX esto demora unos segundos!
		} else{
			System.out.println("no se pudo evaluar jenkins porque la coleccion de datos es de tamanio cero");
		}
		if(clasifier == null){
			System.out.println("No se pudo evaluar la colleccion de features con el metodo de Jenkins");

		}
		//  int clase =   clasifier.classify(arg0)
		return clasifier;
	}

	//	private SimpleFeatureCollection splitByArea(SimpleFeatureCollection collection,double area){
	//		List<SimpleFeature> splitted = new ArrayList<SimpleFeature>();
	//		//FXCollections.synchronizedObservableList(list) //Usar esto si hay proyblemas de paralelismo
	//		//TODO por cada Simplefeature dividirlo por el area y volver a agregarlo tantas veces como entre el area
	//		collection.parallelStream().forEach(sf->{
	//			Geometry g = (Geometry) sf.getDefaultGeometry();
	//			double times = g.getArea()/area;
	//			for(int i=0;i<=times;i++) {
	//				splitted.add(sf);
	//			}
	//			
	//		});
	//		return splitted;
	//	}

	private  int getColorByJenks(Double double1) {
		try{
			int colorIndex = clasifier.classify(double1);

			if(colorIndex<0||colorIndex>colors.length){
				//System.out.println("el color de jenks es: "+colorIndex+" para el rinde "+double1);//rinde es 0.0
				colorIndex=0;
			}
			return colorIndex;
		}catch(Exception e){
			e.printStackTrace();
			return 0;
		}
	}

	private Double[] constructValoresHisto(Set<Double> valores) {
		System.out.println("creando histograma para un set menor o igual a la cantidad de clases del sistema valores.size() "+ valores.size());
		if(valores.size()==1) {
			histograma=new Double[1];
			this.clasesClasificadorProperty.set(2);
			histograma=valores.toArray(histograma);
		}else {
		int numLimites = valores.size()-1;//esto es porque el histograma se extiende hacia el infinito por lo que gana una clase
		histograma=new Double[numLimites];//2 clases 1 limite, 3 clases 2 limites...
		this.clasesClasificadorProperty.set(numLimites+1);
		List<Double> sorted = valores.stream().sorted().collect(Collectors.toList());
		for(int i=1;i<sorted.size();i++) {
			double s0=sorted.get(i-1);
			double s1=sorted.get(i);
			double average =(s0+s1)/2;
			histograma[i-1]=average;
		}
		}
//		//if(numLimites>1) {
//			for(int i = 0; i < numLimites; i++){	
//				histograma[i] = sorted.get(i);// java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
//				System.out.println("i: "+i+" -> "+histograma[i]);
//				//histograma[i] = average  + desvioEstandar * (i- 1/numLimites );
//			}
////		} else if(numLimites==1){
////			histograma[0]=average;
////		}

		this.initialized=true;

		return histograma;
	}

	/**
	 * 
	 * @param elementos Lista de FeatureContainer
	 * @return 
	 */
	public  Double[] constructHistogram(List<? extends LaborItem> elementosItem){

		//1 ordeno los elementos de menor a mayor
		//2 bsuco el i*size/12 elemento y anoto si amount en la posicion i del vector de rangos

		//		List<Dao> elementos = new LinkedList<Dao>(elementosItem);
		//		elementos.sort((e1, e2) -> e1.getAmount().compareTo(e2.getAmount()));//sort ascending


		Double min =Double.MAX_VALUE;
		Double max =Double.MIN_VALUE;
		
		Double average = 0.0;
		Double sup=0.0;
		Double amount= 0.0;
		for(LaborItem dao: elementosItem){
			Double area = dao.getGeometry().getArea()*ProyectionConstants.A_HAS();
			sup += area;
			amount += dao.getAmount()*area;		
			
			min=Math.min(min, dao.getAmount());
			max=Math.max(max, dao.getAmount());
		}
		average= sup > 0 ? amount/(sup) : 0.0;

		//		average = elementosItem
		//				.stream().mapToDouble( FeatureContainer::getAmount)
		//				.average().getAsDouble();//no such value???

		Double desvioEstandar =Double.MAX_VALUE; 
				//0.0;
		if(elementosItem.size()>0){

			double desvios = 0.0;
			for(LaborItem dao: elementosItem){
				Double area = dao.getGeometry().getArea()*ProyectionConstants.A_HAS();
				desvios += area * Math.abs(dao.getAmount()-average);
			}
			desvioEstandar = sup > 0 ? desvios/(sup) : 0.0;
		}

		//	System.out.println("termine de ordenar los elementos en constructHistogram");
		int numLimites = getNumClasses()-1;//esto es porque el histograma se extiende hacia el infinito por lo que gana una clase
		
		//si tiene una sola clase, tiene cero limites
		histograma=new Double[numLimites];//2 clases 1 limite, 3 clases 2 limites...


		//y=ax+b
		if(numLimites>1) {
			for(int i = 0; i < numLimites; i++){	
				histograma[i] = average + (-1+i*2d/(numLimites-1))*desvioEstandar;
			}
		} else if(numLimites==1){
			histograma[0]=average;
		}		
	
		this.initialized=true;
		return histograma;
	}

	public Color getColorFor(double amount) {
		int absCat = getCategoryFor(amount);//entre 0 y numClases-1
		return getColorForCategoria(absCat);
		//		int length =colors.length-1;
		//		int clases =getNumClasses()-1;
		//		int colorIndex = absCat*(length/clases);
		//		//	System.out.println(absCat+"*"+length+"/"+clases+" = "+colorIndex+" colorIndex");
		//		if(colorIndex>length){
		//			colorIndex=length;
		//		}
		//		return colors[colorIndex];
	}

	public Color getColorForCategoria(Integer absCat) {
		//int absCat = getCategoryFor(amount);//entre 0 y numClases-1
		//		int length =colors.length-1;
		//		int clases =getNumClasses()-1;
		//		int colorIndex = absCat*(length/clases);
		//		//	System.out.println(absCat+"*"+length+"/"+clases+" = "+colorIndex+" colorIndex");
		//		if(colorIndex>length){
		//			colorIndex=length;
		//		}
		return getColorForCategoria(absCat,getNumClasses());
	}

	public java.awt.Color getAwtColorFor(Double amount){
		int absCat = getCategoryFor(amount);//entre 0 y numClases-1
		return getAwtColorForCategoria(absCat);
	}
	
	public java.awt.Color getAwtColorForCategoria(Integer absCat){
		Color colorKey = getColorForCategoria(absCat);
		if(awtColorMap.containsKey(colorKey)) {
			return awtColorMap.get(colorKey);
		}
		int red = Double.valueOf(colorKey.getRed()*255).intValue();
		int green = Double.valueOf(colorKey.getGreen()*255).intValue();
		int blue = Double.valueOf(colorKey.getBlue()*255).intValue();
		java.awt.Color awtColor =new java.awt.Color(red,green,blue);
		awtColorMap.put(colorKey, awtColor);
		return awtColor;
	}
	/**
	 * 
	 * @param absCat
	 * @param classCount: es el numero de clases
	 * @return
	 */
	public static Color getColorForCategoria(Integer absCat,Integer classCount) {
		//int absCat = getCategoryFor(amount);//entre 0 y numClases-1
		int length =colors.length-1;//8
		int clases =classCount-1;//1
		if(clases==0)return colors[colors.length-1];//si clases es cero devuelvo el ultimo color
		int colorIndex = absCat * (length/clases); // 7 * (8/1)
		//	System.out.println(absCat+"*"+length+"/"+clases+" = "+colorIndex+" colorIndex");
		if(colorIndex>length){
			colorIndex=length;
		}
		return colors[colorIndex];
	}

	public String getLetraCat(Integer categoria) {
		int size = this.getNumClasses();
		String nombre =""+Clasificador.abc[size-categoria-1];
		return nombre;
	}

	public Color getColorFor(LaborItem dao) {	
		return getColorFor(dao.getAmount());

	}

	public int getNumClasses() {
		int numClases = clasesClasificadorProperty.intValue();
		//return 3;
		if(numClases > colors.length|| numClases < 1){
			System.err.println("la configuracion de "+NUMERO_CLASES_CLASIFICACION+" no puede ser mayor a "+(colors.length));
			numClases=colors.length;
		}
		return numClases;
	}

	public boolean isInitialized(){return initialized;}

	public void constructClasificador(String nombreClasif, Labor<?> labor) {
		System.out.println("constructClasificador "+nombreClasif);
		if (Clasificador.CLASIFICADOR_JENKINS.equalsIgnoreCase(nombreClasif)) {
			System.out.println("construyendo clasificador jenkins "+labor.colAmount.get());

			//*** nuevo codigo para tomar en cuenta el area de los poligonos
			//			SimpleFeatureCollection areaInvariantCol = new DefaultFeatureCollection("internal",labor.getType());
			//			
			//			FeatureReader<SimpleFeatureType, SimpleFeature> reader=null;
			//			try {
			//				reader = labor.getInCollection().reader();
			//			} catch (IOException e) {
			//				
			//				e.printStackTrace();
			//			}
			//			
			//			SimpleFeatureBuilder fb = labor.featureBuilder;
			//			
			//			int id=0;
			//			final double minArea=10;
			//			while (ProcessMapTask.readerHasNext(reader)) {
			//				SimpleFeature feature=null;
			//				try {
			//					feature = reader.next();
			//					
			//					Double area = ProyectionConstants.getHasFeature(feature)							
			//							*ProyectionConstants.METROS2_POR_HA;
			//					System.out.println("multiplicando el feature con area original "+ area+" id "+id);//16.11?
			//					while(area > 0) {
			//						areaInvariantCol.add(fb.buildFeature("\\."+id, feature.getAttributes().toArray()));
			//						area=area-minArea;
			//						id++;
			//					}
			//					
			//				} catch (Exception e) {
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}				
			//			}
			//			System.out.println("haciendo jenkins con la nueva collection de "+id+" features");
			//			this.constructJenksClasifier(areaInvariantCol,labor.colAmount.get());
			//			areaInvariantCol.clear();
			//** fin del nuevo codigo para tomar en cuenta el area de los poligonos

			this.constructUrsulaJenksClasifier(labor.outCollection,labor.colAmount.get());//dan el mismo histograma
			//this.constructJenksClasifier(labor.outCollection,labor.colAmount.get());

		} else {//if(Clasificador.CLASIFICADOR_DESVIOSTANDAR.equalsIgnoreCase(nombreClasif)) {
			System.out.println("no hay jenks Classifier falling back to histograma");
			List<LaborItem> items = new ArrayList<LaborItem>();

			SimpleFeatureIterator ocReader = labor.outCollection.features();
			Set<Double> valores=new HashSet<Double>();
			while (ocReader.hasNext()) {
				LaborItem i = labor.constructFeatureContainerStandar(ocReader.next(),false);
				items.add(i);
				
				valores.add(round(i.getAmount(),5));
			}
			ocReader.close();
			if(valores.size()<=getNumClasses()) {
				constructValoresHisto(valores);
			} else {
				this.constructHistogram(items);
			}
		}


		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection(Messages.getString("ProcessHarvestMapTask.9"),labor.getType());

		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature fIn = it.next();

			LaborItem li=labor.constructFeatureContainerStandar(fIn,false);
			li.setCategoria(this.getCategoryFor(li.getAmount()));
			SimpleFeature f = li.getFeature(labor.getFeatureBuilder());
			boolean res = newOutcollection.add(f);

		}
		labor.setOutCollection(newOutcollection);

	}
	
	public static double round(double n, int decimals) {
	    return Math.floor(n * Math.pow(10, decimals)) / Math.pow(10, decimals);
	}
	
	//	public Clasificador clone(){
	//		Clasificador cn = new Clasificador();
	//		cn.setClasesClasificadorProperty(new SimpleIntegerProperty(this.getClasesClasificadorProperty().get()));
	//		cn.getTipoClasificadorProperty().set(this.getTipoClasificadorProperty().get());
	//		//XXX si el tipo de clasificador es jenkins hay que volver a constriur el clasificador
	//		cn.setHistograma(this.getHistograma().clone());
	//		cn.setInitialized(this.isInitialized());
	//		return cn;
	//	}

	//	/**
	//	 * Metodo que busca los limites de las alturas despues hay que buscar los elementos que estan dentro de un entorno y agregarlos a una lista para dibujarlos
	//	 * @param elementos Lista de Dao ordenados por Elevacion de menor a mayor
	//	 * @return 
	//	 */
	//	public static Double[] constructHeightstogram(List<? extends CosechaItem> elementosItem){
	//		double average = elementosItem
	//				.stream().mapToDouble( CosechaItem::getElevacion)
	//				.average().getAsDouble();
	//		double desvios = 0.0;
	//		for(CosechaItem dao: elementosItem){
	//			desvios += Math.abs(dao.getElevacion()-average);
	//		}
	//		double desvioEstandar= desvios/elementosItem.size();
	//		heightstogram=Double.parseDouble[colors.length];
	//
	//		int desviosEstandar = 8;
	//		Double deltaForColour =(desviosEstandar*desvioEstandar)/colors.length;
	//
	//		for(int i = 0;i<colors.length;i++){	
	//			heightstogram[i]=(average-(desviosEstandar/2)*desvioEstandar)+deltaForColour*(i+1);
	//		}
	//		return heightstogram;
	//	}
}
