package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import tasks.ProcessMapTask;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class RecomendFertNFromHarvestMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	private CosechaLabor cosecha;
	private List<Suelo> suelos;
	private List<FertilizacionLabor> fertilizaciones;
	public RecomendFertNFromHarvestMapTask(FertilizacionLabor labor,CosechaLabor cosechaEstimada, List<Suelo> _suelos, List<FertilizacionLabor> _fert) {
		super(labor);
		this.cosecha =cosechaEstimada;
		this.suelos=_suelos;
		this.fertilizaciones=_fert;
	}
		public void doProcess() throws IOException {
			FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
			featureNumber=cosecha.outCollection.size();
			List<FertilizacionItem> itemsToShow = new ArrayList<FertilizacionItem>();
			Cultivo cultivo = cosecha.producto.getValue();
			Fertilizante fert = this.labor.fertilizanteProperty.getValue();
			
			//TODO calcular el balance de nitrogeno en el suelo luego de la fertilizacion y completar lo que haga falta para la cosecha estimada;
			while (reader.hasNext()) {
				SimpleFeature simpleFeature = reader.next();
				CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);
				FertilizacionItem fi =null;
				synchronized(labor){
					fi= new FertilizacionItem();					
					fi.setId(labor.getNextID());
					labor.setPropiedadesLabor(fi);
				}
				Geometry geom = PolygonValidator.validate(ci.getGeometry());
				Double areaGeom =  ProyectionConstants.A_HAS(geom.getArea());
				fi.setGeometry(geom);
				double absN = ci.getRindeTnHa()*cultivo.getAbsN();
				double dispNSuelo = getNDisponibleSuelo(geom)/areaGeom;
				double dispNFert = getNDisponibleFert(geom)/areaGeom;
			//	System.out.println("absN="+absN+" dispSuelo="+dispNSuelo+" dispNFert="+dispNFert);
				double nAAplicar= absN-dispNSuelo-dispNFert;
			//	System.out.println("nAAplicar="+nAAplicar);
				nAAplicar = Math.max(0, nAAplicar);
				double reposicionN = nAAplicar/(fert.getPorcN()/100);
				fi.setDosistHa(reposicionN);
				fi.setElevacion(10d);
				labor.setPropiedadesLabor(fi);
				//segun el cultivo de la cosecha
				
			
				labor.insertFeature(fi);
				itemsToShow.add(fi);
				featureNumber++;
				updateProgress(featureNumber, featureCount);
		}
			reader.close();

		
		
			labor.constructClasificador();
			runLater(itemsToShow);
			updateProgress(0, featureCount);	
	}
		
	
		private Double getNDisponibleFert(Geometry geometry) {
			Double kgNFert = new Double(0);
			kgNFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
				List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
				Fertilizante fertilizante = fertilizacion.fertilizanteProperty.getValue();
				return items.parallelStream().flatMapToDouble(item->{
					Double kgNHa = (Double) item.getDosistHa() * fertilizante.getPorcN()/100;
					Geometry fertGeom = item.getGeometry();				
					fertGeom= PolygonValidator.validate(fertGeom);
					Double area = 0.0;
					try {
						//XXX posible punto de error/ exceso de demora/ inneficicencia
						Geometry inteseccionGeom = geometry.intersection(fertGeom);// Computes a
						area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
						// Geometry
					} catch (Exception e) {
						e.printStackTrace();
					}				
					return DoubleStream.of( kgNHa * area);				
				});
			}).sum();
//			System.out.println("la cantidad de ppmP acumulado en el suelo es = "
//					+ kPFert);
		
			return kgNFert;
		}
		
		private Double getNDisponibleSuelo(Geometry geometry) {
			Double kgNSuelo = new Double(0);
			kgNSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
				List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
				return items.parallelStream().flatMapToDouble(item->{
					Double kgNHa= (Double) item.getPpmN()*suelo.getDensidad();//TODO multiplicar por la densidad del suelo
					Geometry geom = item.getGeometry();				
					geom= PolygonValidator.validate(geom);
					Double area = 0.0;
					try {
						//XXX posible punto de error/ exceso de demora/ inneficicencia
						Geometry inteseccionGeom = geometry.intersection(geom);// Computes a
						area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
						// Geometry
					} catch (Exception e) {
						//e.printStackTrace();
					}				
					return DoubleStream.of( kgNHa * area);				
				});
			}).sum();
//			System.out
//			.println("la cantidad de ppmP acumulado en el suelo es = "
//					+ kgPSuelo);
		
			return kgNSuelo;
		}
		
		@Override
		protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature) {

			double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
			//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
			DecimalFormat df = new DecimalFormat("#.00");

			String tooltipText = new String(// TODO ver si se puede instalar un
					// boton
					// que permita editar el dato
					"Densidad: " + df.format(fertFeature.getDosistHa())
					+ " Kg/Ha\n" + "Costo: "
					+ df.format(fertFeature.getImporteHa()) + " U$S/Ha\n"
					//+ "Sup: "
					//+ df.format(area * ProyectionConstants.METROS2_POR_HA)
					//+ " m2\n"
					// +"feature: " + featureNumber
					);
			if(area<1){
				tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
				//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
			} else {
				tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
			}

			//List  paths = 
		return	super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText);

			//return null;
		}
		protected int getAmountMin() {
			return 0;
		}

		protected int gerAmountMax() {
			return 1000;
		}
}
