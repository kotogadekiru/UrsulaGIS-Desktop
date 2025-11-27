package com.ursulagis.desktop.gui;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;

public class ShowNDVIEvolution {
	private WorldWindow wwd;
	private LayerPanel layerPanel;

	public ShowNDVIEvolution(WorldWindow _wwd,LayerPanel _lP) {
		this.wwd=_wwd;
		this.layerPanel=_lP;
	}

	public void doShowNDVIEvolution() {
		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
		//	executorPool.execute(()->{
		List<SurfaceImageLayer> ndviLayers = extractLayers();


		//junto los ndvi segun fecha para hacer la evolucion correctamente.
		Map<LocalDate, List<SurfaceImageLayer>>  fechaMap = ndviLayers.stream().collect(
				Collectors.groupingBy((l2)->{
					Ndvi lNdvi = (Ndvi)l2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
				//	System.out.println("agregando el layer con la fecha "+lNdvi.getFecha());
					return lNdvi.getFecha();// fecha me devuelve siempre hoy por eso no hace la animacion
				}));

//		System.out.println("recorriendo los layers en orden de fecha con "+fechaMap.keySet().size()+" fechas");

		List<LocalDate> dates= fechaMap.keySet().stream().distinct().sorted().collect(Collectors.toList());


		Timeline timeline = new Timeline();
		timeline.setCycleCount(1);
		timeline.setAutoReverse(false);	

		double s =0;

		KeyFrame startKF = new KeyFrame(Duration.seconds(s),(t)->{
			//System.out.println("cambiando opacity a "+n);
			for(SurfaceImageLayer l: ndviLayers){					
				//l.setOpacity(n.doubleValue());				
				l.setEnabled(false);	
				//l.setOpacity(0);
			}
			this.layerPanel.update(wwd);
			wwd.redraw();		
	//		System.out.println("termine de apagar los layers");
		});
		timeline.getKeyFrames().add(startKF);
		//s+=0.1;//wait

		for(LocalDate date:dates){
		//	System.out.println("creando el keyFrame para la date "+date);
			ObjectProperty<LocalDate> antesP = new SimpleObjectProperty<LocalDate>(null);
			int dIndex = dates.indexOf(date);

			if(dIndex>0){
				antesP.set(dates.get(dIndex-1));
			}
			if(antesP.get()==null){		
				//s+=1;
				//System.out.println("agregando antesKeyFrame con s="+s);
				KeyFrame antesKeyFrame = new KeyFrame(Duration.seconds(s),(t)->{	
			//		System.out.println("apagando todos los layers");
					List<SurfaceImageLayer> layerList = fechaMap.get(date);
					for(SurfaceImageLayer l: layerList){					
						//l.setOpacity(1);
						if(l.isEnabled()){
							l.setEnabled(false);
						}
					}
					//	Platform.runLater(()->{
					this.layerPanel.update(wwd);
					wwd.redraw();
					//	});
				});
				timeline.getKeyFrames().add(antesKeyFrame);	
			}else{			
				List<SurfaceImageLayer> layerList = fechaMap.get(date);
				List<SurfaceImageLayer> antesLayerList = fechaMap.get(antesP.get());

				long days=Period.between(antesP.get(),date).getDays(); //endDateExclusive)(date.getTime()-antesP.get().getTime())/(1000*60*60*24);
				//System.out.println("days= "+days);
				double avanceDia =0.2;//0.2seg por dia ->2 seg por imagen
				double totalTime = days*avanceDia;			
				//System.out.println("agregando dateFrame con s="+s);

				s+=totalTime;		//1						
				timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(s),(t)->{		

					for(SurfaceImageLayer l: layerList){
						l.setEnabled(true);
						//l.setOpacity(0.2);					
					}
					for(Layer l: antesLayerList){	
						l.setEnabled(false);
						//l.setOpacity(0.8);
					}
					//	Platform.runLater(()->{
					this.layerPanel.update(wwd);
					wwd.redraw();
					//	});
				}));		

			}
		}
		s+=4;
		System.out.println("agregando endKF con s="+s);
		// KeyFrame endKF = new KeyFrame(Duration.seconds(s),(t)->{
		// 	for(Layer l: ndviLayers){
		// 		l.setEnabled(true);
		// 	}
		// 	this.layerPanel.update(wwd);
		// 	wwd.redraw();
		// 	//System.out.println("termine de habilitar todos los layers endKF "+System.currentTimeMillis());
		// });

		//timeline.getKeyFrames().add(endKF);
		System.out.println("playing animation");
		timeline.play();
	}

	public List<SurfaceImageLayer> extractLayers() {
		List<SurfaceImageLayer> ndviLayers = new ArrayList<SurfaceImageLayer>();
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Ndvi){
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
