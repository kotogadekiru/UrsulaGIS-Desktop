package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import dao.Labor;
import dao.Poligono;
import dao.recorrida.Camino;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.UnitsFormat;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwind.util.measure.MeasureToolController;
import gui.nww.LayerPanel;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;

public class PathLayerFactory {

	public static MeasureTool createMeasureTool(WorldWindow wwd, RenderableLayer surfaceLayer) {
		MeasureTool measureTool = new MeasureTool(wwd,surfaceLayer);
		measureTool.setController(new MeasureToolController());
		measureTool.setMeasureShapeType(MeasureTool.SHAPE_PATH);
		measureTool.setPathType(AVKey.GREAT_CIRCLE);
		measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.METERS);
		measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.HECTARE);
		measureTool.getUnitsFormat().setShowDMS(false);
		measureTool.setFollowTerrain(true);
		measureTool.setShowControlPoints(true);
		measureTool.setArmed(false);
		measureTool.setLineColor(Color.WHITE);
		return measureTool;
	}

	@SuppressWarnings("unchecked")
	static public MeasureTool createCaminoLayer(Camino camino, WorldWindow wwd,LayerPanel layerPanel){
		RenderableLayer surfaceLayer = new RenderableLayer();
		camino.setLayer(surfaceLayer);
		//	poli.setLayer(surfaceLayer);		
		surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, camino);
		surfaceLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, camino.getClass());
		MeasureTool measureTool = createMeasureTool(wwd, surfaceLayer);

		List<Position> positions = camino.getPositions();

		measureTool.setPositions((ArrayList<? extends Position>) positions);
		camino.setLongitud(measureTool.getLength());

		DoubleProperty valueProperty= new SimpleDoubleProperty();
		valueProperty.setValue( camino.getLongitud());

		measureTool.addPropertyChangeListener((event)->{
			// Add, remove or change positions
			if(event.getPropertyName().equals(MeasureTool.EVENT_ARMED)){
				if (measureTool.isArmed()) {
					((Component) wwd).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				} else {                    //al cerrar el alert se hace un setArmed(false);
					((Component) wwd).setCursor(Cursor.getDefaultCursor());
				}
			}
			//event released as position moved, position added or position removed			
			else if(event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REPLACE) ||
					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_ADD) ||
					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE)){

				
				double length =  measureTool.getLength();//esto lo hace despues de cada cuadro. no puedo volver a medir el area
				if(valueProperty.get()!=length && length > 0 
						&& Math.abs(camino.getLongitud()-length)>2){//solo actualizo si la diferencia es mayor a 2m
				//	DecimalFormat dc = new DecimalFormat("0.00"); //$NON-NLS-1$
				//	dc.setGroupingSize(3);
				//	dc.setGroupingUsed(true);
				//	String formated = dc.format(length)+Messages.getString("JFXMain.metrosAbrevSufix"); //$NON-NLS-1$
				//	t.textProperty().set(formated);
					//System.out.println(event.getPropertyName()+" pos size"+measureTool.getPositions().size());
					camino.getPositions().clear();
					camino.getPositions().addAll((List<Position>) measureTool.getPositions());
					//if(camino.getPositions().size()>2)	camino.getPositions().remove(1);//quito el primero que pone por duplicado
					camino.setLongitud(length);	
					camino.setNombre(camino.getNombre());//asigna el nombre al layer
					//surfaceLayer.setName(camino.getNombre()+" "+formated); //$NON-NLS-1$
					surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, camino);
					surfaceLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, Camino.class);
					valueProperty.setValue(length);
					layerPanel.update(wwd);
				}                	                  
			}
		});

		return measureTool;
	}
}
//
//	private void doMedirSuperfice(Camino camino, WorldWindow wwd,LayerPanel layerPanel) {
//		RenderableLayer surfaceLayer = new RenderableLayer();
//
//		MeasureTool measureTool = createMeasureTool(wwd, surfaceLayer);
//		// measureTool.setMeasureShape(new SurfacePolygon());
//		measureTool.clear();
//		measureTool.setArmed(true);
//
//		//insertBeforeCompass(wwd, surfaceLayer);
//		//Poligono poli =new Poligono();
//
//		Alert supDialog = new Alert(Alert.AlertType.INFORMATION);
//		//supDialog.initOwner(this.stage);
//		supDialog.setTitle(Messages.getString("PoligonLayerFactory.title")); //$NON-NLS-1$
//		supDialog.setHeaderText(Messages.getString("PoligonLayerFactory.texto")); //$NON-NLS-1$
//		Text t = new Text();
//		TextField nombreTF = new TextField();
//		nombreTF.setPromptText(Messages.getString("PoligonLayerFactory.2texto2")); //$NON-NLS-1$
//		VBox vb = new VBox();
//		vb.getChildren().addAll(nombreTF,t);
//		supDialog.setGraphic(vb);
//		supDialog.initModality(Modality.NONE);
//		nombreTF.textProperty().addListener((o,old,n)->{
//			camino.setNombre(n);
//			//surfaceLayer.setName(n);
//		});
//
//		DoubleProperty valueProperty = new SimpleDoubleProperty();
//		measureTool.addPropertyChangeListener((event)->{
//			//System.out.println(event.getPropertyName());
//			// Add, remove or change positions
//			if(event.getPropertyName().equals(MeasureTool.EVENT_ARMED)){
//				if (measureTool.isArmed()) {
//					((Component) wwd).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
//				} else {                    //al cerrar el alert se hace un setArmed(false);
//					((Component)wwd).setCursor(Cursor.getDefaultCursor());
//				}
//			}
//			// Metric changed - sent after each render frame
//			else if(event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REPLACE) ||
//					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_ADD) ||
//					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE) ){// no sirve EVENT_POSITION_REPLACE porque al agregar un punto no se dispara la actualizacion
//				DecimalFormat dc = new DecimalFormat("0.00"); //$NON-NLS-1$
//				dc.setGroupingSize(3);
//				dc.setGroupingUsed(true);
//				double	value = measureTool.getLength();
//				if(value != valueProperty.doubleValue() && value > 0){
//					String formated = dc.format(value)+Messages.getString("JFXMain.metrosAbrevSufix"); //$NON-NLS-1$
//					t.textProperty().set(formated);
//
//
//					camino.setPositions( (List<Position>) measureTool.getPositions());
//					camino.setLongitud(value);
//					//poli.setNombre(dc.format(value/ProyectionConstants.METROS2_POR_HA));
//
//					//System.out.println("nombre poli :"+poli.getNombre());
//					camino.setLayer(surfaceLayer);
//					surfaceLayer.setName(camino.getNombre()+" "+formated); //$NON-NLS-1$
//					//ArrayList<? extends Position> positions = measureTool.getPositions();
//					surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, camino);
//					surfaceLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, camino.getClass());
//					//surfaceLayer.setValue("POSITIONS", poli.getPositions());
//
//					valueProperty.setValue(value);
//					layerPanel.update(wwd);
//					//  System.out.println("lengh="+measureTool.getLength());
//				}                	                  
//				//updateMetric();
//			}
//		});
//
//
//		supDialog.show();
//		supDialog.setOnHidden((event)->{			
//			measureTool.setArmed(false);
//			//surfaceLayer.setName(poli.getNombre()+" "+	t.textProperty().get());
//			//((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
//			layerPanel.update(wwd);
//		}); 
//
//	}


//private void doMedirDistancia() {		 
////RenderableLayer layer = new RenderableLayer();
////layer.setName(Messages.getString("JFXMain.medirDistanciaLayerName")); //$NON-NLS-1$
//RenderableLayer layer = new RenderableLayer();
//MeasureTool measureTool = new MeasureTool(getWwd(),layer);
//measureTool.setController(new MeasureToolController());
//// MeasureToolPanel  measurePanel=  new MeasureToolPanel(getWwd(), measureTool);
//
//measureTool.setMeasureShapeType(MeasureTool.SHAPE_PATH);
//measureTool.setPathType(AVKey.GREAT_CIRCLE);
//measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.METERS);
//measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.HECTARE);
//measureTool.getUnitsFormat().setShowDMS(false);
//measureTool.setFollowTerrain(true);
//measureTool.setShowControlPoints(true);
//// measureTool.setMeasureShape(new SurfacePolygon());
//measureTool.clear();
//measureTool.setArmed(true);
//
//Alert distanciaDialog = new Alert(Alert.AlertType.INFORMATION);
//distanciaDialog.initOwner(JFXMain.stage);
//Text t = new Text();
//distanciaDialog.setTitle(Messages.getString("JFXMain.medirDistancia")); //$NON-NLS-1$
//distanciaDialog.setHeaderText(Messages.getString("JFXMain.medirDistanciaHeaderText")); //$NON-NLS-1$
//distanciaDialog.setGraphic(t);
//distanciaDialog.initModality(Modality.NONE);
//
//DoubleProperty valueProperty = new SimpleDoubleProperty();
//measureTool.addPropertyChangeListener((event)->{
//	//System.out.println(event.getPropertyName());
//	// Add, remove or change positions
//	if(event.getPropertyName().equals(MeasureTool.EVENT_ARMED)){
//		if (measureTool.isArmed()) {
//			((Component) getWwd()).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
//		} else {                    
//			((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
//		}
//	}
//	// Metric changed - sent after each render frame
//	else if(event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REPLACE) ||
//			event.getPropertyName().equals(MeasureTool.EVENT_POSITION_ADD) ||
//			event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE)){
//		double	value = measureTool.getLength();
//		DecimalFormat dc = new DecimalFormat("0.00"); //$NON-NLS-1$
//		dc.setGroupingSize(3);
//		dc.setGroupingUsed(true);
//		if(value != valueProperty.doubleValue() && value > 0){
//			String formated = dc.format(value)+Messages.getString("JFXMain.metrosAbrevSufix"); //$NON-NLS-1$
//			t.textProperty().set(formated);
//			measureTool.getLayer().setName(formated);
//			measureTool.getLayer().setValue(Labor.LABOR_LAYER_IDENTIFICATOR, Messages.getString("JFXMain.medirLayerTypeName")); //$NON-NLS-1$
//			measureTool.getLayer().setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, Camino.class); //$NON-NLS-1$
//			valueProperty.setValue(value);
//			this.getLayerPanel().update(this.getWwd());
//		}                	                  
//	}
//});
//
//
//distanciaDialog.show();
//distanciaDialog.setOnHidden((event)->{
//	measureTool.setArmed(false);
//	((Component) getWwd()).setCursor(Cursor.getDefaultCursor());
//	this.getLayerPanel().update(this.getWwd());
//});
//
//distanciaDialog.getResult();               
//}

