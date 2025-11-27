package com.ursulagis.desktop.gui;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;
import com.ursulagis.desktop.tasks.ShowNDVITifFileTask;
import com.ursulagis.desktop.utils.ExcelHelper;

public class ExportNDVIToExcel {
	private WorldWindow wwd;
	private LayerPanel layerPanel;
	private static final String YYYY_MM_DD = "yyyy-MM-dd";
	DateTimeFormatter format1 = DateTimeFormatter.ofPattern(YYYY_MM_DD);

	/**
	 * clase que toma todos los ndvi cargados y los exporta en un unico excel
	 * @param _wwd
	 * @param _lP
	 */
	public ExportNDVIToExcel(WorldWindow _wwd,LayerPanel _lP) {
		this.wwd=_wwd;
		this.layerPanel=_lP;
	}

	public void exportToExcel() {
		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
		//	executorPool.execute(()->{
		List<SurfaceImageLayer> ndviLayers = extractLayers();

		//junto los ndvi segun fecha para hacer la evolucion correctamente.
		Map<LocalDate, List<SurfaceImageLayer>>  fechaMap = ndviLayers.stream().collect(
				Collectors.groupingBy((l2)->{
					Ndvi lNdvi = (Ndvi)l2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					return lNdvi.getFecha();// fecha me devuelve siempre hoy por eso no hace la animacion
				}));

		List<LocalDate> dates= fechaMap.keySet().stream().distinct().sorted().collect(Collectors.toList());

		Map<String, Object[]> data = new TreeMap<String, Object[]>();
		//			fecha1	fecha2	fecha3	fecha4	...
		//poligono1	p1f1	p1f2	p1f3	p1f4	...
		//poligono2	p2f1	p2f2	p2f3	p2f4	...
		//...

		List<String> headers = new ArrayList<String>();
		//headers.add(Messages.getString("BulkNdviDownloadGUI.poligonoNameExcelColumn"));//"Poligono");
		headers.add(Messages.getString("BulkNdviDownloadGUI.latitudExcelColumn"));//"Latitud");
		headers.add(Messages.getString("BulkNdviDownloadGUI.longitudExcelColumn"));//"Longitud");

		for(LocalDate fecha :dates) {						
			headers.add(format1.format(fecha));
		}

		data.put("0", headers.toArray());

		Map<Position, Object[]> positionsMap=new HashMap<Position,Object[]>();

		//TODO por cada punto crear un vector con los valores a exportar
		//headers long,lat date1, date2 ... date n
		for(LocalDate date:dates){
			List<SurfaceImageLayer> layerList = fechaMap.get(date);
			for(SurfaceImageLayer l:layerList) {
				//SurfaceImageLayer l = layerList.get(0);
				Ndvi ndvi = (Ndvi) l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				ExportableAnalyticSurface surface = ndvi.getSurfaceLayer();
				Iterable<? extends GridPointAttributes> attributesList = surface.getValues();
				Sector sector =  surface.getSector();
				
				int[] dim = surface.getDimensions();
				System.out.println("dimensions " + Arrays.toString(dim));
				
				final int width = dim[0];
				final int height = dim[1];
				double latStep = -sector.getDeltaLatDegrees() / (double) (height-1);//-1
				double lonStep = sector.getDeltaLonDegrees() / (double) (width-1);
				
				double minLat = sector.getMaxLatitude().degrees;
				double minLon = sector.getMinLongitude().degrees;
				
				Iterator<? extends GridPointAttributes> it = attributesList.iterator();
				for (int y = 0; y < height; y++){
					double lat = minLat + y * latStep;
					for (int x = 0; x < width; x++)	{
						double lon = minLon+x*lonStep;
						GridPointAttributes attr = it.hasNext() ? it.next() : null;
						if(attr==null)continue;
						double value = attr.getValue();
						
						Position pos = Position.fromDegrees(lat,lon);
						if(positionsMap.keySet().contains(pos)) {
							Object[] array = positionsMap.get(pos);
							array[dates.indexOf(date)+2] = value;
							positionsMap.put(pos, array);
						} else {
							Object[] array=new Object[dates.size()+2];
							array[0]=lat;
							array[1]=lon;
							array[dates.indexOf(date)+2] = value;
							positionsMap.put(pos, array);
						}
					}
				}
			}
		}

		int i=1;
		for(Object[] p : positionsMap.values()) {

			//System.out.println(Arrays.toString(p));
			//TODO eliminar los puntos que sean todos no data
			//ShowNDVITifFileTask.TRANSPARENT_VALUE
			boolean insert=false;
			for(int j=2;j<p.length;j++) {
				Object o =p[j];
				if(o!=null) {
					double d=(double)o;
					insert=insert||(ShowNDVITifFileTask.TRANSPARENT_VALUE<d);
				}
			}
			if(insert) {
				data.put(String.valueOf(i),p);
				i++;
			}
		}

		System.out.println("creando excel con "+data.size()+" lineas");
		ExcelHelper excelHelper=new ExcelHelper();
		excelHelper.exportData("ndvi", data);
	}



	public List<SurfaceImageLayer> extractLayers() {
		List<SurfaceImageLayer> ndviLayers = new ArrayList<SurfaceImageLayer>();
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Ndvi){
				//Ndvi ndvi=(Ndvi) o;
				//l.setEnabled(false);
				ndviLayers.add((SurfaceImageLayer) l);
			}
		}	

		//System.out.println("mostrando la evolucion de "+ndviLayers.size()+" layers");
		ndviLayers.sort(new NdviLayerComparator());
		return ndviLayers;
	}

	public class NdviLayerComparator implements Comparator<Layer>{
		DateTimeFormatter df =null;// DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		public NdviLayerComparator() {
			df = DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		}

		@Override
		public int compare(Layer c1, Layer c2) {			
			String l1Name =c1.getName();
			String l2Name =c2.getName();

			Object labor1 = c1.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			Object labor2 = c2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);


			if(labor1 != null && labor1 instanceof Ndvi && 
					labor2 != null && labor2 instanceof Ndvi ){
				Ndvi ndvi1 = (Ndvi)labor1;
				Ndvi ndvi2 = (Ndvi)labor2;

				try{
					return ndvi1.getFecha().compareTo(ndvi2.getFecha());
				} catch(Exception e){
					//System.err.println("no se pudo comparar las fechas de los ndvi. comparando nombres"); //$NON-NLS-1$
				}
				// comparar por el valor del layer en vez del nombre del layer
				try{
					LocalDate d1 = LocalDate.parse(l1Name.substring(l1Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					LocalDate d2 = LocalDate.parse(l2Name.substring(l2Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					return d1.compareTo(d2);
				} catch(Exception e){
					//no se pudo parsear como fecha entonces lo interpreto como string.
					e.printStackTrace();
				}
			}
			return l1Name.compareToIgnoreCase(l2Name);
		}
	}
}
