package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.api.data.FeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.geometry.BoundingBox;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.ConvertirASiembraTask;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;

//public class CrearSiembraDesdeFertilizacionTask {

	
//	package com.ursulagis.desktop.tasks.procesar;

	
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
		
		public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
			double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
			String tooltipText = ConvertirASiembraTask.buildTooltipText(siembraFeature, area,labor);
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
