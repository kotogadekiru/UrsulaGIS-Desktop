package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.data.FeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.LaborItem;
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.config.Semilla;
import dao.cosecha.CosechaItem;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import gui.nww.LaborLayer;
import tasks.ProcessMapTask;
import utils.PolygonValidator;
import utils.ProyectionConstants;

//public class CrearSiembraDesdeFertilizacionTask {

	
//	package tasks.procesar;

	
	public class CrearSiembraDesdeFertilizacionTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
		/**
		 * la lista de las cosechas a unir
		 */
		//private List<CosechaLabor> cosechas;
		private SiembraLabor siembra;
		private FertilizacionLabor fertilizacion;
		private double dosisXha;
		private double dosisXhaMin;
		private double dosisXhaMax;
		private boolean relDirecta;
		//	private SimpleFeatureType type = null;


		public CrearSiembraDesdeFertilizacionTask(SiembraLabor _siembra, FertilizacionLabor _fertilizacion, double dosis, double min, double max, boolean relDirecta_){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
			super( _siembra);
			this.siembra=_siembra;
			this.fertilizacion=_fertilizacion;
			this.dosisXha = dosis;
			relDirecta = relDirecta_;
			dosisXhaMin = min;
			if (max > 0) dosisXhaMax= max;
			else dosisXhaMax = 1000000;
			
			labor.setNombre("SiembraFertilizada "+siembra.getNombre()+"-"+fertilizacion.getNombre());//este es el nombre que se muestra en el progressbar
		}

		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected void doProcess() throws IOException {

			FeatureReader<SimpleFeatureType, SimpleFeature> reader =fertilizacion.outCollection.reader();
			featureNumber=fertilizacion.outCollection.size();
		//	List<SiembraItem> itemsToShow = new ArrayList<SiembraItem>();
		
			
			int clasesA = fertilizacion.clasificador.getNumClasses();
			double totalFerti = fertilizacion.getCantidadInsumo();
			double totalHa = fertilizacion.getCantidadLabor();
		    double promedio = totalFerti/totalHa;
			System.out.println("Area: " + totalHa + " Dosis: " + totalFerti + " Promedio: " + promedio);
			Semilla semilla = labor.getSemilla();
			System.out.println("semilla es "+semilla);
			double entresurco = labor.getEntreSurco();
			double pmil = semilla.getPesoDeMil();
			double pg = semilla.getPG();
			double metrosLinealesHa = ProyectionConstants.METROS2_POR_HA/entresurco;//23809 a 0.42
			//System.out.println("metrosLinealesHa "+metrosLinealesHa);//metrosLinealesHa 52631.57894736842 ok!
			// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas

			while (reader.hasNext()) {
				SimpleFeature simpleFeature = reader.next();
				FertilizacionItem fi = fertilizacion.constructFeatureContainerStandar(simpleFeature,false);
				SiembraItem si =null;
				synchronized(labor){
					si= new SiembraItem();					
					si.setId(labor.getNextID());
					labor.setPropiedadesLabor(si);	
				}
				si.setGeometry(fi.getGeometry());
				//asigno fertilizante
				double kgFerti = fi.getDosistHa();
				double variador = kgFerti/promedio;
				//reviso de no tener fertilizante cero
				double dosis = 0;
				if (variador>0) {
					//asigno el fertilizante
					si.setDosisFertCostado(fi.getDosistHa());
						//asigno dosis de semilla 
						
						if (relDirecta == true)
							{
							dosis = dosisXha*variador;
											}
						else {
							dosis = dosisXha/variador;
							
							
							}
						
				}
				else  {
					if (relDirecta == true)
						{
							if (dosisXhaMin == 0 ) dosis = dosisXha/0.30;
							else dosis = dosisXhaMin;
					    }
					else {
						if (dosisXhaMax == 0 ) dosis = dosisXha*0.30;
						else dosis = dosisXhaMax;
						}		
				
				}
				System.out.println("la dosis antes max y min es : " + dosis );
				
				if (dosis <= dosisXhaMin ) dosis=dosisXhaMin;
				
				if (dosis >= dosisXhaMax  ) dosis= dosisXhaMax;
								
				
				System.out.println("la dosis final es : " + dosis );
				
				double semillasHa = ProyectionConstants.METROS2_POR_HA*dosis/pg;
				
				si.setDosisHa(semillasHa*pmil/(1000*1000));//solo queda poner la formula para guardar el valor de la dosis 
				//si.setDosisML(dosis);	
				
					
					labor.setPropiedadesLabor(si);
				//segun el cultivo de la cosecha
				
			
				labor.insertFeature(si);
		//		itemsToShow.add(si);
				featureNumber++;
				updateProgress(featureNumber, featureCount);
		} 
			
			reader.close();

		
		
			labor.constructClasificador();
		//	runLater(itemsToShow);
			runLater(this.getItemsList());
			updateProgress(0, featureCount);	
	}
		
		@Override
		protected ExtrudedPolygon getPathTooltip(Geometry poly, SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {
			double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
			DecimalFormat df = new DecimalFormat("#,###.##");//$NON-NLS-2$
			df.setGroupingUsed(true);
			df.setGroupingSize(3);
			//densidad seeds/metro lineal
			String tooltipText = new String(Messages.getString("ProcessSiembraMapTask.1")+ df.format(siembraFeature.getDosisML()) + Messages.getString("ProcessSiembraMapTask.2")); //$NON-NLS-1$ //$NON-NLS-2$

			Double seedsSup= siembraFeature.getDosisML()/labor.getEntreSurco();
			if(seedsSup<100) {//plantas por ha
				tooltipText=tooltipText.concat(df.format(seedsSup*ProyectionConstants.METROS2_POR_HA) + " s/"+ Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$

			}else {
				tooltipText=tooltipText.concat(df.format(seedsSup) + " s/"+Messages.getString("ProcessSiembraMapTask.10")); //s/m2
			}
			//kg semillas por ha
			tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.3") + df.format(siembraFeature.getDosisHa()) + Messages.getString("ProcessSiembraMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
			//fert l
			tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.5") + df.format(siembraFeature.getDosisFertLinea()) + Messages.getString("ProcessSiembraMapTask.6")		); //$NON-NLS-1$ //$NON-NLS-2$
			//fert costo
			tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.7") + df.format(siembraFeature.getImporteHa()) + Messages.getString("ProcessSiembraMapTask.8")		); //$NON-NLS-1$ //$NON-NLS-2$

			if(area<1){
				tooltipText=tooltipText.concat( Messages.getString("ProcessSiembraMapTask.9")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessSiembraMapTask.10")); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				tooltipText=tooltipText.concat(Messages.getString("ProcessSiembraMapTask.11")+df.format(area ) + Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	

		}
		
			@Override
		protected int getAmountMin() {
			// TODO Auto-generated method stub
			return 0;
		}


		@Override
		protected int gerAmountMax() {
			// TODO Auto-generated method stub
			return 0;
		}

		

//	}


	
	
	
}
