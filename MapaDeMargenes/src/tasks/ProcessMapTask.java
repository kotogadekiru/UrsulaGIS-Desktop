package tasks;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.DoubleStream;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.stage.Screen;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Filter;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.JenksNaturalBreaksFunction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;

import sun.java2d.DestSurfaceProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Configuracion;
import dao.CosechaItem;
import dao.FeatureContainer;
import dao.Producto;
//import org.opengis.filter.FilterFactory2;

public abstract class ProcessMapTask extends Task<Quadtree>{
	protected static  Double  MINIMA_SUP_HAS =  new Double(Configuracion.getInstance().getPropertyOrDefault("SUP_MINIMA_M2", "10"))/ProyectionConstants.METROS2_POR_HA;//0.001
	protected Group map = null;//new Group();
	protected Quadtree featureTree;
	protected FileDataStore store = null;
	
	//store donde se almacenan los features mientras se estan editando
	protected FileDataStore output = null;
	protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();
	public static Color[] colors = {
		Color.rgb(158,1,66),
		Color.rgb(213,62,79),
		Color. rgb(244,109,67),
		Color. rgb(253,174,97),
		Color. rgb(254,224,139),
		Color. rgb(255,255,191),
		Color. rgb(230,245,152),
		Color. rgb(171,221,164),
		Color.rgb(102,194,165),
		Color.rgb(50,136,189),
		Color.DARKBLUE};
	//	Color.rgb(94,79,162)};
	private static Double[] histograma=null;// es static para poder hacer constructHistograma static para usarlo en el grafico de Histograma
	private static Double[] heightstogram=null;//es una lista de las alturas asociadas a
	
	public static Classifier clasifier=null;

	public static Producto producto;

	protected int featureCount;
	protected int featureNumber;


	@Override
	protected Quadtree call() throws Exception {
		MINIMA_SUP_HAS =  new Double(Configuracion.getInstance().getPropertyOrDefault("SUP_MINIMA_M2", "10"))/ProyectionConstants.METROS2_POR_HA;//0.001
		try {
			doProcess();
		} catch (Exception e1) {
			System.err.println("Error al procesar el Shape de cosechas");
			e1.printStackTrace();
		}

		return featureTree;
	}

	protected abstract void doProcess()throws IOException ;

	protected abstract int getAmountMin() ;
	protected abstract int gerAmountMax() ;

	private Paint getColorFor(Double rinde) {				
		//		int rindeMin=getAmountMin(),rindeMax=gerAmountMax();
		//		
		//		if(rinde < rindeMin)return colors[0];
		//		if(rinde > rindeMax)return colors[colors.length-1];
		//		
		//		
		//		int rango= rindeMax-rindeMin;
		//		double delta=rinde-rindeMin;
		//		double porcent = delta/rango;


		//		if(histograma != null){
		//			return colors[getColorByHistogram(rinde, histograma)];
		//		} else if(clasifier != null){
		//			return  colors[getColorByJenks(rinde)];
		//		}
		//		System.err.println("Error no hay un clasificador seleccionado");
		return colors[getCategoryFor(rinde)];
		//return getColorByHue(rinde, rindeMin, rindeMax, porcent);

	}

	public static Integer getCategoryFor(Double rinde) {				
		//		int rindeMin=getAmountMin(),rindeMax=gerAmountMax();
		//		
		//		if(rinde < rindeMin)return colors[0];
		//		if(rinde > rindeMax)return colors[colors.length-1];
		//		
		//		
		//		int rango= rindeMax-rindeMin;
		//		double delta=rinde-rindeMin;
		//		double porcent = delta/rango;


		if(histograma != null){
			return getColorByHistogram(rinde, histograma);
		} else if(clasifier != null){
			return getColorByJenks(rinde);
		}
		System.err.println("Error no hay un clasificador seleccionado");
		return null;
		//return getColorByHue(rinde, rindeMin, rindeMax, porcent);
	}

	public static String getCategoryNameFor(int index) {		
		String rangoIni = null;
		if(histograma != null){
			Double delta = histograma[1]-histograma[0];

			if(index<histograma.length){
				rangoIni = (histograma[index]-delta)+".."+histograma[index];
			}else {
				rangoIni = histograma[index]+".."+(histograma[index]+delta);
			}
		} else if(clasifier != null){			
			rangoIni = clasifier.getTitle(index);		
		}

		String [] partesIni = rangoIni.split("\\.\\.");
		DecimalFormat df = new DecimalFormat("0.00");
		return df.format(new Double(partesIni[0]))+"~"+df.format(new Double(partesIni[1]));// +"-"+histograma[j+1];

		//		System.err.println("Error no hay un clasificador seleccionado");
		//		return label;
	}

	private static int getColorByHistogram(Double rinde, Double[] histo) {
		int colorIndex = histo.length-1;
		try {
			BigDecimal bd = new BigDecimal(rinde);//java.lang.NumberFormatException: Infinite or NaN
			bd = bd.setScale(2, RoundingMode.HALF_UP);
			rinde = bd.doubleValue();
			for (int i = histo.length-1; i > -1 ; i--) {
				double histoMax = histo[i];
				if (rinde <= histoMax) {
					colorIndex = i;
				}
			}

			//	 System.out.println("Histograma color Index for rinde "+rinde+" is "+colorIndex);
			return colorIndex;
		} catch (Exception e) {
			System.err.println("getColorsByHistogram "+rinde);
			e.printStackTrace();
			return 0;
		}
	}


	public static Classifier constructJenksClasifier(SimpleFeatureCollection collection,String amountColumn){
		//JenksFunctionTest test = new JenksFunctionTest("jenksTest");
		histograma = null;
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

		Literal classes = ff.literal(colors.length);
		//Literal classes = ff.literal(3);
		PropertyName expr = ff.property(amountColumn);
		JenksNaturalBreaksFunction func = (JenksNaturalBreaksFunction) ff.function("Jenks", expr,
				classes);

		if(collection.size()>0){
			System.out.println("evaluando la colleccion para poder hacer jenkins");
			clasifier = (Classifier) func.evaluate(collection);//XXX esto demora unos segundos!
		} else{
			System.out.println("no se pudo evaluar jenkins porque la coleccion de datos es de tamaño cero");
		}
		if(clasifier == null){
			System.out.println("No se pudo evaluar la colleccion de features con el metodo de Jenkins");

		}
		//  int clase =   clasifier.classify(arg0)
		return clasifier;
	}

	private static int getColorByJenks(Double double1) {
		try{
			int colorIndex = clasifier.classify(double1);

			if(colorIndex<0||colorIndex>colors.length){
				System.out.println("el color de jenks es: "+colorIndex+" para el rinde "+double1);
				colorIndex=0;
			}
			return colorIndex;
		}catch(Exception e){
			e.printStackTrace();
			return 0;
		}
	}

	
	/**
	 * Metodo que busca los limites de las alturas despues hay que buscar los elementos que estan dentro de un entorno y agregarlos a una lista para dibujarlos
	 * @param elementos Lista de Dao ordenados por Elevacion de menor a mayor
	 * @return 
	 */
	public static Double[] constructHeightstogram(List<? extends CosechaItem> elementosItem){
		double average = elementosItem
				.stream().mapToDouble( CosechaItem::getElevacion)
				.average().getAsDouble();
		double desvios = new Double(0);
		for(CosechaItem dao: elementosItem){
			desvios += Math.abs(dao.getElevacion()-average);
		}
		double desvioEstandar= desvios/elementosItem.size();
		heightstogram=new Double[colors.length];

		int desviosEstandar = 8;
		Double deltaForColour =(desviosEstandar*desvioEstandar)/colors.length;

		for(int i = 0;i<colors.length;i++){	
			heightstogram[i]=(average-(desviosEstandar/2)*desvioEstandar)+deltaForColour*(i+1);
		}
		return heightstogram;
	}
	
	
	/**
	 * 
	 * @param elementos Lista de FeatureContainer
	 * @return 
	 */
	public static Double[] constructHistogram(List<? extends FeatureContainer> elementosItem){
	
		//1 ordeno los elementos de menor a mayor
		//2 bsuco el i*size/12 elemento y anoto si amount en la posicion i del vector de rangos

		//		List<Dao> elementos = new LinkedList<Dao>(elementosItem);
		//		elementos.sort((e1, e2) -> e1.getAmount().compareTo(e2.getAmount()));//sorg ascending

		double average = elementosItem
				.stream().mapToDouble( FeatureContainer::getAmount)
				.average().getAsDouble();
		
		double desvios = new Double(0);
		for(FeatureContainer dao: elementosItem){
			desvios += Math.abs(dao.getAmount()-average);
		}
		double desvioEstandar= desvios/elementosItem.size();


		System.out.println("termine de ordenar los elementos en constructHistogram");
		histograma=new Double[colors.length];


		//Set.add() returns false if the item was already in the set.

		//	if(elementosItem.size()>colors.length){//FIXME lo importante no son los elementos sino los diferentes valores
		System.out.println("hay mas elementos que colores");
		//Double rindeMin=elementosItem.get(0).getAmount();
		//Double rindeMax = elementosItem.last().getAmount();				//el problema es que el max es 90 y no es representativo

		//			System.out.println("rindeMin ="+rindeMin);
		//			System.out.println("rindeMax ="+rindeMax);
		//			Double rindeMin=0.0;
		//			Double maxAmount = 20.0;	
		//			if(producto!=null){
		//				maxAmount = producto.getRindeEsperado().getValue()*1.5;
		//			}

		int desviosEstandar = 8;
		Double deltaForColour =(desviosEstandar*desvioEstandar)/colors.length;

		for(int i = 0;i<colors.length;i++){	
			histograma[i]=(average-(desviosEstandar/2)*desvioEstandar)+deltaForColour*(i+1);
		}


		//		} else if(diferent.size()>0){
		//			Double rindeMin=diferent.first().getAmount();
		//			Double rindeMax = diferent.last().getAmount();			
		//			double rango= rindeMax-rindeMin;
		//
		//			double delta = rango/colors.length;// si rango es cero delta es cero
		//
		//			for(int i = 0;i<colors.length;i++){		
		//				histograma[i]=rindeMin+delta*i;
		//			}	
		//		}	
		//		System.out.print("histograma [");
		//		for(Double histo:histograma){
		//			System.out.print(histo+",");
		//		}
		//		System.out.println("]");
		//		
		//		ArrayList<Double> doubles = new ArrayList<Double>();
		//		for(Dao dao : elementos){
		//			doubles.add(dao.getAmount());
		//		}
		//		Double[] jenks = NaturalBreaks.getJenksBreaks(doubles, colors.length);
		//		System.out.println("jenks array " );
		//		
		//		System.out.print("jenks array [");
		//		for(Double jen:jenks){
		//			System.out.print(jen+",");
		//		}
		//		System.out.println("]");
		//		
		//		histograma=jenks;
		return histograma;
	}

	private Paint getColorByHue(Double rinde, int rindeMin, int rindeMax,
			double porcent) {
		double redHue = Color.RED.getHue();
		double greenHue = Color.GREEN.getHue();

		double colorHue = 0;
		double brightness= Color.BLACK.getBrightness();
		if(rinde<= rindeMax && rinde>=rindeMin){
			colorHue = redHue+porcent*(greenHue-redHue);

			brightness=Color.WHITE.getBrightness()+porcent*(Color.BLACK.getBrightness()-Color.WHITE.getBrightness());
		} else{
			System.out.println("rinde fuera de rango "+rinde);
		}

		return 	Color.hsb(colorHue, Color.RED.getSaturation(), brightness);
	}
	
	  public Paint getColorRGB (double i, double j) {
		  double alpha = 50;//frecuencia x
		  double beta = 50;//frecuencia y
		    return Color.rgb(255*((int)(0.5 + 0.5 * Math.sin (i * alpha))),
		                    255*((int)(.5 - .5 * Math.cos (j * beta))), 0);
		  }


	protected Path getPathFromGeom(Geometry poly, FeatureContainer dao) {			
		Path path = new Path();		
		/**
		 * recorro el vector de puntos que contiene el poligono gis y creo un
		 * path para dibujarlo
		 */
		if(poly.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ poly);
			return null;
		}
		for (int i = 0; i < poly.getNumPoints(); i++) {
			Coordinate coord = poly.getCoordinates()[i];
			// como las coordenadas estan en long/lat las convierto a metros
			// para dibujarlas con menos error.
			double x = coord.x / ProyectionConstants.metersToLongLat;
			double y = coord.y /ProyectionConstants.metersToLongLat;
			if (i == 0) {
				path.getElements().add(new MoveTo(x, y)); // primero muevo el
			}
			path.getElements().add(new LineTo(x, y));// dibujo una linea desde
		}

		Paint currentColor = null;
		try{
			currentColor = getColorFor(dao.getAmount());
		}catch(Exception e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}


		//		if(Color.BLACK.equals(currentColor)){
		//			System.out.println("Infinite or NaN "+((Cosecha)dao).getId());
		//		}
		path.setFill(currentColor);
		path.setStrokeWidth(0.05);
		//		path.setCache(false);
		path.getStyleClass().add(currentColor.toString());//esto me permite luego asignar un estilo a todos los objetos con la clase "currentColor.toString()"
		//path.setStyle("-fx-fill: white;");//ok anda
		//		String pathClass=null;
		//		if(pathClass==null){
		//			pathClass = currentColor.toString();
		//			System.out.println("pathClass = "+pathClass);
		//			map.setStyle("0xe6f598ff.{"
		//				    +"-fx-fill: white;"
		//				+"}");
		//}
		/*
		 * pathClass = 0x3288bdff
pathClass = 0x3288bdff
pathClass = 0x3288bdff
pathClass = 0x66c2a5ff
pathClass = 0xe6f598ff
pathClass = 0xffffbfff
pathClass = 0xfee08bff
pathClass = 0xf46d43ff
pathClass = 0xd53e4fff
pathClass = 0x9e0142ff
		 */


		return path;
	}
	
	protected Path getPathFromGeom(Geometry poly, Integer colorIndex) {			
		Path path = new Path();		
		/**
		 * recorro el vector de puntos que contiene el poligono gis y creo un
		 * path para dibujarlo
		 */
		if(poly.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ poly);
			return null;
		}
		for (int i = 0; i < poly.getNumPoints(); i++) {
			Coordinate coord = poly.getCoordinates()[i];
			// como las coordenadas estan en long/lat las convierto a metros
			// para dibujarlas con menos error.
			double x = coord.x / ProyectionConstants.metersToLongLat;
			double y = coord.y /ProyectionConstants.metersToLongLat;
			if (i == 0) {
				path.getElements().add(new MoveTo(x, y)); // primero muevo el
			}
			path.getElements().add(new LineTo(x, y));// dibujo una linea desde
		}

		Paint currentColor = null;
		try{
			currentColor = colors[colorIndex];
		}catch(Exception e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}

		path.setFill(currentColor);
		path.setStrokeWidth(0.05);
	
		path.getStyleClass().add(currentColor.toString());//esto me permite luego asignar un estilo a todos los objetos con la clase "currentColor.toString()"
		return path;
	}

	protected List<Polygon> getPolygons(FeatureContainer dao){
		List<Polygon> polygons = new ArrayList<Polygon>();
		Object geometry = dao.getGeometry();
	//	System.out.println("obteniendo los poligonos de "+geometry);

		if (geometry instanceof MultiPolygon) {		
			MultiPolygon mp = (MultiPolygon) geometry;
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry g = mp.getGeometryN(i);
				if(g instanceof Polygon){
					polygons.add((Polygon) g);
				}				
			}

		} else if (geometry instanceof Polygon) {
			polygons.add((Polygon) geometry);
		} else if(geometry instanceof Point){
			Point p = (Point) geometry;
			GeometryFactory fact = p.getFactory();
			Double r = 5*ProyectionConstants.metersToLongLat;
			
			Coordinate D = new Coordinate(p.getX() - r , p.getY() + r ); // x-l-d
			Coordinate C = new Coordinate(p.getX() + r , p.getY()+ r);// X+l-d
			Coordinate B = new Coordinate(p.getX() + r , p.getY() - r );// X+l+d
			Coordinate A = new Coordinate(p.getX() - r , p.getY() -r );// X-l+d
			
				Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
			
				// PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
				// fact= new GeometryFactory(pm);

				LinearRing shell = fact.createLinearRing(coordinates);
				LinearRing[] holes = null;
				Polygon poly = new Polygon(shell, holes, fact);

				polygons.add(poly);
			System.out.println("creando polygon default");//Las geometrias son POINT. que hago?
			//TODO crear un poligono default
			
		}
		//System.out.println("devolviendo los polygons "+polygons);
		return polygons;
	}

	protected void runLater() {	
		ArrayList<Path> pathsToAdd = new ArrayList<Path>();

		int i =0;
		for (ArrayList<Object> pathTooltip : pathTooltips) {
			updateProgress(i, pathTooltips.size());
			i++;
			Path path = (Path) pathTooltip.get(0);
			String tooltipText = (String) pathTooltip.get(1);

			pathsToAdd.add(path);

			path.setOnMouseEntered(e -> {

				Tooltip t = new Tooltip(tooltipText);
				Font f = t.getFont();

				t.setFont(new Font(f.getName(), 32));//FIXME seguro esto me proboca el error del tamaño del texto
				Tooltip.install(path, t);

			});						

		}

		
		Platform.runLater(new Runnable() {
				@Override
			public void run() {				
				map.getChildren().addAll(pathsToAdd);//antes tenia setAll que es mas rapido pero me borraba los nodos que habia antes si los habia
				//	map.getChildren().setAll(pathsToAdd);//antes tenia setAll que es mas rapido pero me borraba los nodos que habia antes si los habia

				//				SnapshotParameters params = new SnapshotParameters();
				//				params.setFill(Color.TRANSPARENT);
				//				
				//				  WritableImage image = map.snapshot(params, null);
				//				  gc.drawImage(image, 0, 0);
				//	  map.getChildren().clear();
				//	map.getChildren().add(canvas);
			}
		});

	}

	protected void canvasRunLater() {		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Screen screen = Screen.getPrimary();
				Rectangle2D bounds = screen.getVisualBounds();
				double size = Math.sqrt(pathTooltips.size());
				int lado = 5;
				Canvas canvas = new Canvas(bounds.getWidth(),bounds.getHeight());//size*lado+10,size*lado+10);
				GraphicsContext gc = canvas.getGraphicsContext2D();		
				//6898688,-6898688


				BoundingBox bounds2 = getBounds();

				System.out.println("bounds2 "+bounds2);

				int i = 0;
				for (ArrayList<Object> pathTooltip : pathTooltips) {
					updateProgress(i, pathTooltips.size());
					i++;
					Object obj =pathTooltip.get(0);
					if (obj instanceof Path ) {
						Path path = (Path) obj;

						gc.setFill(path.getFill());
						gc.setStroke(path.getStroke());

						ObservableList<PathElement> elements = path.getElements();

						double[] xCoords = new double[elements.size()-1];
						double[] yCoords = new double[elements.size()-1];


						for (int j =0; j<elements.size()-1;j++ ) {
							PathElement pe = elements.get(j);

							if (pe instanceof MoveTo ) {
								MoveTo mt = (MoveTo) pe;

								xCoords[j]=mt.getX()-bounds2.getMinX()/ProyectionConstants.metersToLongLat;//+7074000;
								yCoords[j]=mt.getY()-bounds2.getMinY()/ProyectionConstants.metersToLongLat;//3967057;

							} else if (pe instanceof LineTo ) {
								LineTo lt = (LineTo) pe;

								xCoords[j]=lt.getX()-bounds2.getMinX()/ProyectionConstants.metersToLongLat;//7074000;
								yCoords[j]=lt.getY()-bounds2.getMinY()/ProyectionConstants.metersToLongLat;//3967057;
							}
						}
						System.out.println("fillPolygon "+Arrays.toString(xCoords) + " "+ Arrays.toString(yCoords) );

						gc.fillPolygon(xCoords,yCoords,xCoords.length);
					}//no se que hacer si no es un Path

				}//fin del for
				//double size = Math.sqrt(pathTooltips.size());


				//	map.getChildren().add(new Label("HolaGroup"));
				map.getChildren().add(canvas);
			}


		});
	}
	public BoundingBox getBounds() {
		BoundingBox bounds2=null;
		try {
			bounds2 = store.getFeatureSource().getBounds();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return bounds2;
	}
	
	public void insertFeature(SimpleFeature f){
		 SimpleFeatureStore store2;
		try {
			store2 = (SimpleFeatureStore) store.getFeatureSource( f.getType().getName() );
			
			  List<SimpleFeature> list = new ArrayList<SimpleFeature>();
			    list.add(f);
			  //  list.add( build.buildFeature("fid2", new Object[]{ geom.point(2,3), "martin" } ) );
			    SimpleFeatureCollection collection = new ListFeatureCollection( f.getType(), list);

			    Transaction transaction = new DefaultTransaction("insertFeatureTransaction");
			    store2.setTransaction( transaction );
			    try {
			        store2.addFeatures( collection );
			        transaction.commit(); // actually writes out the features in one go
			    }
			    catch( Exception problem){
			    	problem.printStackTrace();
					try {
						transaction.rollback();
						//	System.out.println("transaction rolledback");
					} catch (IOException e) {

						e.printStackTrace();
					}

				} finally {
					try {
						transaction.close();
						//System.out.println("closing transaction");
					} catch (IOException e) {

						e.printStackTrace();
					}
				}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		 
			
	}
}

