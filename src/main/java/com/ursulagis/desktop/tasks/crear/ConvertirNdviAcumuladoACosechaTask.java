package com.ursulagis.desktop.tasks.crear;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.ShowNDVITifFileTask;
import com.ursulagis.desktop.tasks.procesar.SumarCosechasMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * Task que toma una lista de ndvis y los convierte a cosecha haciendo la acumulacion de ndvi por su fecha
 */
public class ConvertirNdviAcumuladoACosechaTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
//	private static  double NDVI_RINDE_CERO = ShowNDVITifFileTask.MIN_VALUE;//0.2;
	Double diasNdviPorTn = 0.0;
	List<Ndvi> ndvis = null;

	public ConvertirNdviAcumuladoACosechaTask(CosechaLabor cosechaLabor, List<Ndvi> _ndvis, Double _diasNdviPorTn){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		diasNdviPorTn=_diasNdviPorTn;
		ndvis=_ndvis;
//		try {
//			NDVI_RINDE_CERO=
//					cosechaLabor.getCultivo().getNdviRindeCero();
//		}catch(Exception e) {
//			e.printStackTrace();
//			NDVI_RINDE_CERO=ShowNDVITifFileTask.MIN_VALUE;
//		}

	}
	
	public void doProcess() {
		ndvisToCosecha();
		labor.constructClasificador();	
		
		runLater(getItemsList());
	}
	
	/**
	 * convertir cada ndvi a cosecha y luego grillar las cosechas sumando como en unirFertilizaciones
	 */
	public void ndvisToCosecha() {
		LocalDate lastFecha =null;
		updateProgress(0, ndvis.size());
		List<CosechaLabor> cosechasASumar = new ArrayList<CosechaLabor>();
		int i=0;
		//XXX esto da error si mezclo ndvi de diferentes lotes. podria agrupar por contorno antes.
		ndvis.sort((n1,n2)->n1.compareTo(n2));
		for(Ndvi ndvi : ndvis) {
			if(ndvi.getMeanNDVI()<0.2)continue;//solo procesar los datos con el surco cerrado
			LocalDate fecha = ndvi.getFecha();
			long dias=5;//esto no asigna valor a la primera imagen porque multiplica por cero
			if(lastFecha!=null) {								
				dias = java.time.temporal.ChronoUnit.DAYS.between(lastFecha, fecha);				
			}
			lastFecha=fecha;
		
			CosechaLabor cosechaNdvi = new CosechaLabor();
			cosechaNdvi.setNombre(ndvi.getNombre());
			cosechaNdvi.setCultivo(labor.getCultivo());
			Date date = localDateToDate(fecha);

			cosechaNdvi.setFecha(date);			
			cosechaNdvi.setLayer(new LaborLayer());
			
			double ndviProm = ndvi.getMeanNDVI();
			//System.out.println("MeanNDVI "+ndviProm);
			//ndviProm = getAverageNdvi(ndvi.getSurfaceLayer().getValues());
			//System.out.println("/***************   fecha "+date+" "+lastFecha+" dias "+dias+" ndvi "+ndviProm);
			Double newValue = ndviProm * dias/diasNdviPorTn;		
			ConvertirNdviACosechaTask umTask = new ConvertirNdviACosechaTask(cosechaNdvi,ndvi,newValue);
			umTask.setOnSucceeded(handler -> {
				CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();		
				cosechasASumar.add(ret);				
			});//fin del OnSucceeded
			umTask.run();
			updateProgress(i++, ndvis.size()+1);
		}
		
		SumarCosechasMapTask umTask = new SumarCosechasMapTask(cosechasASumar);						
		umTask.run();
		
		updateProgress(i++, ndvis.size()+1);
		try {
			CosechaLabor cosechaSuma = umTask.get();
			
			cosechaSuma.setNombre(labor.getNombre());
			super.labor=cosechaSuma;		
		} catch (InterruptedException e) {		
			e.printStackTrace();
		} catch (ExecutionException e) {		
			e.printStackTrace();
		}		
	}

	public Date localDateToDate(LocalDate fecha) {
		return java.util.Date.from(fecha.atStartOfDay()
				.atZone(ZoneId.systemDefault())
				.toInstant());
	}



	public Double getAverageNdvi(Iterable<? extends GridPointAttributes> values) {
		double NDVI_RINDE_CERO = ShowNDVITifFileTask.MIN_VALUE;//0.2;
		Iterator<? extends GridPointAttributes> it = values.iterator();
		double sum =0;
		int size =0;
		while(it.hasNext()){
			GridPointAttributes gpa = it.next();
			Double value = Double.valueOf(gpa.getValue());
			//OJO cuando se trabaja con el jar la version de world wind que esta levantando no es la que usa eclipse.
			// es por eso que el jar permite valores Nan y en eclipse no.
			if(value < NDVI_RINDE_CERO || value > ShowNDVITifFileTask.MAX_VALUE || value == 0 || value.isNaN()){
				continue;
			} else{
				//System.out.println("calculando el promedio para el valor "+value+" suma parcial "+sum+" size "+size);
				sum+=value;
				size++;
			}
		}
		if(size == 0){
			System.err.println("no hay puntos iterables para obtener el promedio");
			return 0.0;
		}
		this.featureCount=size;
		Double averageNdvi = Double.valueOf(sum/size);
		System.out.println("el promedio de los ndvi es "+averageNdvi);
		return averageNdvi;
	}

//	public void addNdviValuesToCosecha(CosechaLabor cosecha) {
//		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
//		LocalDate[] lastFecha =new LocalDate[1];
//
//
//		ndvis.stream()	
//		.sorted((n1,n2)->n1.compareTo(n2))
//		.forEachOrdered(lNdvi->{
//			try {
//				LocalDate fecha = lNdvi.getFecha();
//				long dias=5;//esto no asigna valor a la primera imagen porque multiplica por cero
//				if(lastFecha[0]==null) {
//					lastFecha[0]=fecha;					
//				} else {					
//					dias = java.time.temporal.ChronoUnit.DAYS.between(lastFecha[0], fecha);
//					lastFecha[0]=fecha;
//				}
//				//acumulo el ndvi	
//				//TODO hacer esto por cada pixel del ndvi
//				Double ndviValue = 0.0;
//				Double oldValue = 0.0;//obtener oldValue del pixel para la cosecha
//				Double newValue = oldValue + ndviValue * dias;			
//
//			}catch(Exception e) {
//				System.err.println("Excepcion para "+lNdvi.getNombre());
//				e.printStackTrace();
//			}
//		});	//termine de recorrer todos los ndvi en sentido ordenado	
//	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task