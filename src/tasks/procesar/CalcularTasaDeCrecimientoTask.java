package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.geotools.data.FeatureReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import dao.Ndvi;
import dao.Poligono;
import dao.config.Configuracion;
import dao.config.Cultivo;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import dao.utils.PropertyHelper;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import tasks.ProcessMapTask;
import tasks.ShowNDVITifFileTask;
import tasks.crear.CrearCosechaMapTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;


public class CalcularTasaDeCrecimientoTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
		private static  double NDVI_RINDE_CERO = ShowNDVITifFileTask.MIN_VALUE;//0.2;

		Ndvi ndvi=null;
		
		Double[] radInc = new Double[] {24.79 ,24.74 ,24.68 ,24.63 ,24.57 ,24.51 ,24.44 ,24.38 ,24.31 ,24.24 ,24.16 ,24.09 ,24.01 ,23.93 ,23.84 ,23.76 ,23.67 ,23.58 ,23.49 ,23.39 ,23.30 ,23.20 ,23.10 ,23.00 ,22.90 ,22.79 ,22.68 ,22.58 ,22.47 ,22.36 ,22.24 ,22.13 ,22.01 ,21.90 ,21.78 ,21.66 ,21.54 ,21.42 ,21.29 ,21.17 ,21.04 ,20.92 ,20.79 ,20.66 ,20.53 ,20.40 ,20.27 ,20.14 ,20.01 ,19.87 ,19.74 ,19.61 ,19.47 ,19.34 ,19.20 ,19.06 ,18.93 ,18.79 ,18.65 ,18.51 ,18.38 ,18.24 ,18.10 ,17.96 ,17.82 ,17.68 ,17.54 ,17.40 ,17.26 ,17.13 ,16.99 ,16.85 ,16.71 ,16.57 ,16.43 ,16.29 ,16.15 ,16.02 ,15.88 ,15.74 ,15.61 ,15.47 ,15.33 ,15.20 ,15.06 ,14.93 ,14.79 ,14.66 ,14.53 ,14.40 ,14.27 ,14.13 ,14.00 ,13.88 ,13.75 ,13.62 ,13.49 ,13.37 ,13.24 ,13.12 ,12.99 ,12.87 ,12.75 ,12.63 ,12.51 ,12.39 ,12.27 ,12.16 ,12.04 ,11.93 ,11.81 ,11.70 ,11.59 ,11.48 ,11.37 ,11.27 ,11.16 ,11.06 ,10.95 ,10.85 ,10.75 ,10.65 ,10.55 ,10.46 ,10.36 ,10.27 ,10.18 ,10.09 ,10.00 ,9.91 ,9.82 ,9.74 ,9.66 ,9.57 ,9.49 ,9.42 ,9.34 ,9.26 ,9.19 ,9.12 ,9.05 ,8.98 ,8.91 ,8.85 ,8.78 ,8.72 ,8.66 ,8.60 ,8.54 ,8.49 ,8.44 ,8.38 ,8.33 ,8.29 ,8.24 ,8.19 ,8.15 ,8.11 ,8.07 ,8.03 ,8.00 ,7.97 ,7.93 ,7.90 ,7.87 ,7.85 ,7.82 ,7.80 ,7.78 ,7.76 ,7.74 ,7.73 ,7.72 ,7.70 ,7.70 ,7.69 ,7.68 ,7.68 ,7.68 ,7.68 ,7.68 ,7.68 ,7.69 ,7.69 ,7.70 ,7.71 ,7.73 ,7.74 ,7.76 ,7.78 ,7.80 ,7.82 ,7.84 ,7.87 ,7.90 ,7.93 ,7.96 ,7.99 ,8.03 ,8.07 ,8.11 ,8.15 ,8.19 ,8.23 ,8.28 ,8.33 ,8.38 ,8.43 ,8.48 ,8.54 ,8.60 ,8.65 ,8.72 ,8.78 ,8.84 ,8.91 ,8.97 ,9.04 ,9.11 ,9.19 ,9.26 ,9.34 ,9.41 ,9.49 ,9.57 ,9.66 ,9.74 ,9.83 ,9.91 ,10.00 ,10.09 ,10.18 ,10.28 ,10.37 ,10.47 ,10.56 ,10.66 ,10.76 ,10.86 ,10.97 ,11.07 ,11.18 ,11.29 ,11.39 ,11.50 ,11.61 ,11.73 ,11.84 ,11.95 ,12.07 ,12.19 ,12.31 ,12.43 ,12.55 ,12.67 ,12.79 ,12.91 ,13.04 ,13.17 ,13.29 ,13.42 ,13.55 ,13.68 ,13.81 ,13.94 ,14.07 ,14.21 ,14.34 ,14.47 ,14.61 ,14.75 ,14.88 ,15.02 ,15.16 ,15.30 ,15.43 ,15.57 ,15.71 ,15.86 ,16.00 ,16.14 ,16.28 ,16.42 ,16.57 ,16.71 ,16.85 ,17.00 ,17.14 ,17.28 ,17.43 ,17.57 ,17.72 ,17.86 ,18.01 ,18.15 ,18.29 ,18.44 ,18.58 ,18.73 ,18.87 ,19.01 ,19.16 ,19.30 ,19.44 ,19.59 ,19.73 ,19.87 ,20.01 ,20.15 ,20.29 ,20.43 ,20.57 ,20.71 ,20.85 ,20.98 ,21.12 ,21.25 ,21.39 ,21.52 ,21.66 ,21.79 ,21.92 ,22.05 ,22.18 ,22.30 ,22.43 ,22.55 ,22.68 ,22.80 ,22.92 ,23.04 ,23.16 ,23.27 ,23.39 ,23.50 ,23.61 ,23.72 ,23.83 ,23.94 ,24.04 ,24.15 ,24.25 ,24.35 ,24.44 ,24.54 ,24.63 ,24.72 ,24.81 ,24.90 ,24.98 ,25.06 ,25.14 ,25.22 ,25.29 ,25.37 ,25.43 ,25.50 ,25.57 ,25.63 ,25.68 ,25.74 ,25.79 ,25.84 ,25.89 ,25.93 ,25.97};
		private boolean isPP;
		private boolean isCN;

		private boolean isVI;

		public CalcularTasaDeCrecimientoTask(CosechaLabor cosechaLabor,Ndvi _ndvi){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
			super(cosechaLabor);
			
			ndvi=_ndvi;

		}

		public void doProcess() throws IOException {
			
			Iterable<? extends GridPointAttributes> values = ndvi.getSurfaceLayer().getValues();
			Iterator<? extends GridPointAttributes> it = values.iterator();
			
			double sum =0;
			int size =0;
			while(it.hasNext()){
				GridPointAttributes gpa = it.next();
				Double value = new Double(gpa.getValue());
				//OJO cuando se trabaja con el jar la version de world wind que esta levantando no es la que usa eclipse.
				// es por eso que el jar permite valores Nan y en eclipse no.
				if(value < ShowNDVITifFileTask.MIN_VALUE || value > ShowNDVITifFileTask.MAX_VALUE || value == 0 || value.isNaN()){
						continue;
				} else{
				//System.out.println("calculando el promedio para el valor "+value+" suma parcial "+sum+" size "+size);
				sum+=value;
				size++;
				}
			}
			if(size == 0){
				System.err.println("no hay puntos iterables para obtener el promedio");
				return;
			}
			this.featureCount=size;
			Double averageNdvi = new Double(sum/size);
			System.out.println("el promedio de los ndvi es "+averageNdvi);
			// si el rinde promedio es >0.5 => NDVI_RINDE_CERO es 0.3
			//si el rinde promedio es <0.5 => NDVI_RINDE_CERO es 0.1
			//chequear el minimo ndvi segun el cultivo? para el trigo inicial en macollo el minimo de 0.5 es malo
			Cultivo cult =super.labor.getCultivo();
			if(cult.isEstival()) {
				//ver si la imagen es del final del ciclo o del principio?
				NDVI_RINDE_CERO= averageNdvi>0.5?0.2:0.1;//depende del cultivo?
			}
			if(averageNdvi.isNaN()) averageNdvi = 1.0;
			 it = values.iterator();		 
			 
			ExportableAnalyticSurface exportableSurface = ndvi.getSurfaceLayer();//.getSurfaceAttributes();
			Sector sector = exportableSurface.getSector();
			int[] dimensions = exportableSurface.getDimensions();//raster.getWidth(), raster.getHeight()
			
			final int width = dimensions[0];
			final int height = dimensions[1];
			
			GeometryFactory fact = new GeometryFactory();
			
			double latStep = -sector.getDeltaLatDegrees() / (double) (height);//-1
			double lonStep = sector.getDeltaLonDegrees() / (double) (width);
			
			
			List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();
			
			double elev = 1;
			double minLat = sector.getMaxLatitude().degrees;
			double minLon = sector.getMinLongitude().degrees;
			
			List<SimpleFeature> features = Collections.synchronizedList(new ArrayList<SimpleFeature>());
			SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(
					labor.getType());
			
			double id=0;
			
			//Configuracion config = Configuracion.getInstance();
			//String useSigmoidString = config.getPropertyOrDefault(this.getClass().getName()+".USE_SIGMOID", "false");
			//System.out.println(this.getClass().getName()+".USE_SIGMOID="+useSigmoidString);
			double pendiente=1;
			double origen=0;
			if(isPP) {//pastura consociada
				//productividad pasturas (y pi): fPAR*RFA*6,532
			 pendiente =6.532; 
			 origen = 0;
			}else if(isCN) {
				//productividad CN: fPAR*RFA*2,669+4,36
			 pendiente = 2.669;
			 origen = 4.36;
			} else if(isVI) {
				//productividad verdeos invierno: fPAR*RFA*9,579
				 pendiente = 9.579;
				 origen = 0;
			}
			Function<Double, Double> calcRinde = getRineForNDVILinealFunction(pendiente,origen);
	
		
			Poligono contorno = this.ndvi.getContorno();
			Geometry contornoGeom =null;
			if(contorno !=null) {
				contornoGeom = contorno.toGeometry();
			}
			
			for (int y = 0; y < height; y++){
				double lat = minLat + y * latStep;
				for (int x = 0; x < width; x++)	{
					double lon = minLon+x*lonStep;
					
					GridPointAttributes attr = it.hasNext() ? it.next() : null;
					double ndvi = attr.getValue();
					if((ndvi<=NDVI_RINDE_CERO)){//&&(x<3 || y<3||x>width-3||y>height-3)){
						continue;//me salteo la primera fila de cada costado
					}
					
					
					if(ndvi <= ShowNDVITifFileTask.MAX_VALUE && ndvi >= ShowNDVITifFileTask.MIN_VALUE && ndvi !=0){
						CosechaItem ci = new CosechaItem();
						ci.setId(id);
						ci.setElevacion(elev);
						ProyectionConstants.setLatitudCalculo(lat);
						double ancho =lonStep / ProyectionConstants.metersToLong();
						double distancia  = latStep /ProyectionConstants.metersToLat();
						ci.setAncho(ancho);
						ci.setDistancia(distancia);
						
						//Double rindeNDVI = new Double(pendienteNdviRinde*ndvi+origenNdviRinde);//aproximacion lineal
						Double rindeNDVI = new Double(calcRinde.apply(ndvi));//aproximacion logaritmica
						//System.out.println("creado nueva cosecha con rinde "+rindeNDVI+" para el ndvi "+value+" rinde prom "+rinde+" ndvi promedio "+average);
						ci.setRindeTnHa(rindeNDVI);
						labor.setPropiedadesLabor(ci);
						
						Coordinate[] coordinates = new Coordinate[5];
						coordinates[0]= new Coordinate(lon,lat,elev);
						coordinates[1]= new Coordinate(lon+lonStep,lat,elev);
						coordinates[2]= new Coordinate(lon+lonStep,lat+latStep,elev);
						coordinates[3]= new Coordinate(lon,lat+latStep,elev);
						coordinates[4]= new Coordinate(lon,lat,elev);
						
						Geometry poly = fact.createPolygon(coordinates);	
					
						// recortar si esta afuera del contorno del ndvi
						if(contornoGeom!=null && !contornoGeom.covers(poly)) {//OK! funciona. no introducir poligonos empty!
							try {
							poly= GeometryHelper.getIntersection(poly, contornoGeom);//poly.intersection(contornoGeom);
							if(poly==null || poly.isEmpty())continue;
							//System.out.println("el contorno no cubre el polygono y la interseccion es: "+poly.toText());
							}catch(Exception e) {//com.vividsolutions.jts.geom.TopologyException: Found null DirectedEdge
								e.printStackTrace();//com.vividsolutions.jts.geom.TopologyException: side location conflict [ (-61.920393510481325, -33.66456750394795, NaN) ]
							}
						}
						ci.setGeometry(poly);
						
						SimpleFeature f = ci.getFeature(fBuilder);
						boolean res = features.add(f);
						if(!res){
							System.out.println("no se pudo agregar la feature "+ci);
						}
					//	labor.insertFeature(ci);
						itemsToShow.add(ci);
						id++;
						updateProgress(id, featureCount);

				}
					
					
				//	lon += lonStep;
				}
			//lat += latStep;
			}
			
		
			if(labor.inCollection == null){
				labor.inCollection = new DefaultFeatureCollection("internal",labor.getType());
			}
			labor.inCollection.addAll(features);
			boolean ret= labor.outCollection.addAll(features);
			if(!ret){//XXX si esto falla es provablemente porque se estan creando mas de una feature con el mismo id
				System.out.println("no se pudieron agregar las features al outCollection");
			}					
			labor.constructClasificador();	
			runLater(itemsToShow);
			updateProgress(0, featureCount);
		}
		private Double getRadInc() {
			int diaJul =ndvi.getFecha().getDayOfYear(); 
			double rad = radInc[diaJul];
			System.out.println("calculando raciacion para "+diaJul+" de "+ndvi.getFecha()+" => "+rad);
			return rad;			 
		}

		private Function<Double, Double> getRineForNDVILinealFunction(double pendienteNdviRinde,double origenNdviRinde) {
			
//			double pendienteNdviRinde = 6.532;		
//			double origenNdviRinde = 0;			
			double radInc=getRadInc();//20.8466480856667;
			Function<Double,Double> tazaPP = (ndvi)->{				
				double modisNdvi=0.8913*ndvi;//ndvi greenseeker
				double fPar=modisNdvi*1.51-0.29;
				//PAR (Rad Inc)(MJ/m2/dia)
				
				double RFAA=0.48*radInc;
				double crecimientoDia = fPar*RFAA*pendienteNdviRinde+origenNdviRinde;
				System.out.println("taza crecimiento para ndvi "+ndvi+" => "+crecimientoDia);
				return crecimientoDia;
			};
			
//			Function<Double,Double> tazaCN = (ndvi)->{				
//				double modisNdvi=0.8913*ndvi;
//				double fPar=modisNdvi*1.51-0.29;			
//				double RFAA=0.48*radInc;
//				double crecimientoDia = fPar*RFAA*2.669+4.36;
//				return crecimientoDia;
//			};			
					
			return tazaPP;
		}		

			
		@Override
		protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
			double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
			String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
			return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);

		}

		protected int getAmountMin() {
			return 3;
		}

		protected int gerAmountMax() {
			return 15;
		}

		public void isPP(boolean b) {
			this.isPP=b;			
		}

		public void isCN(boolean b) {
			this.isCN=b;			
		}
		public void isVI(boolean b) {
			this.isVI=b;			
		}
	}// fin del task