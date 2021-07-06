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
				
				si.setDosisHa(dosis);//solo queda poner la formula para guardar el valor de la dosis 
					
					
					
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
		protected ExtrudedPolygon getPathTooltip(Geometry poly, SiembraItem fertFeature,ExtrudedPolygon  renderablePolygon) {

			double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
			//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
			DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$

			String tooltipText = new String(// TODO ver si se puede instalar un
					// boton
					// que permita editar el dato
					"Densidad Fertilizante: " + df.format(fertFeature.getDosisFertCostado())
					+ " Kg/Ha\n" + "Densidad Siembra: "
					+ df.format(fertFeature.getDosisHa()) + " \n"
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
			return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText, renderablePolygon);

			//return null;
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
