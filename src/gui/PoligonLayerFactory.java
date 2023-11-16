package gui;

import java.awt.Component;
import java.awt.Cursor;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import dao.Labor;
import dao.Poligono;
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
import utils.ProyectionConstants;

public class PoligonLayerFactory {

	
	public static final String MEASURE_TOOL = "MeasureTool";

	public static MeasureTool createMeasureTool(WorldWindow wwd, RenderableLayer surfaceLayer) {
		MeasureTool measureTool = new MeasureTool(wwd,surfaceLayer);
		measureTool.setController(new MeasureToolController());
		measureTool.setMeasureShapeType(MeasureTool.SHAPE_POLYGON);
		measureTool.setPathType(AVKey.GREAT_CIRCLE);
		measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.METERS);
		measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.HECTARE);
		measureTool.getUnitsFormat().setShowDMS(false);
		measureTool.setFollowTerrain(true);
		measureTool.setShowControlPoints(true);
		measureTool.setArmed(false);
		return measureTool;
	}
	
	@SuppressWarnings("unchecked")
	static public MeasureTool createPoligonMeasureTool(Poligono poli, WorldWindow wwd,LayerPanel layerPanel){
		RenderableLayer surfaceLayer = new RenderableLayer();
//		{
//		    public void dispose() {
//		    	//todo MeasureTool clear or dispose
//		    	super.dispose();
//		    }
//		};
		poli.setLayer(surfaceLayer);
		surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
		surfaceLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, poli.getClass());
		MeasureTool measureTool = createMeasureTool(wwd, surfaceLayer);
		surfaceLayer.setValue(MEASURE_TOOL, measureTool);
		List<Position> positions = poli.getPositions();

		measureTool.setPositions((ArrayList<? extends Position>) positions);
	
				
		DoubleProperty valueProperty= new SimpleDoubleProperty();
		valueProperty.setValue( poli.getArea());

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
					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE)){//&& measureTool.isArmed()
//				DecimalFormat dc = new DecimalFormat("0.00");
//				dc.setGroupingSize(3);
//				dc.setGroupingUsed(true);					
				double area =  measureTool.getArea()/ProyectionConstants.METROS2_POR_HA;//esto lo hace despues de cada cuadro. no puedo volver a medir el area
			
//					String formated = dc.format(area)+" Ha";
//					t.textProperty().set(formated);
					
					poli.getPositions().clear();
					poli.getPositions().addAll((List<Position>) measureTool.getPositions());
					
					//poli.setPositions( (List<Position>) measureTool.getPositions());
					poli.setArea(area);	
					//poli.setLayer(surfaceLayer);
					//surfaceLayer.setName(poli.getNombre()+" "+formated);
					if( valueProperty.get()!=area && area > 0){
					surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
					valueProperty.setValue(area);
					layerPanel.update(wwd);				//XXX esto hace que se re calcule todo el arbol varias veces?? si pero esta ok.
				}                	                  
			}
		});	
		//measureTool.setArmed(false);//XXX trato de evitar que measure tool se coma toda la memoria
		return measureTool;
	}
	
	@SuppressWarnings("unchecked")
	static public MeasureTool createCircleMeasureTool(Poligono poli, WorldWindow wwd,LayerPanel layerPanel){
		RenderableLayer surfaceLayer = new RenderableLayer();

		poli.setLayer(surfaceLayer);
		surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
		surfaceLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, poli.getClass());
		MeasureTool measureTool = createMeasureTool(wwd, surfaceLayer);
		surfaceLayer.setValue(MEASURE_TOOL, measureTool);
		List<Position> positions = poli.getPositions();

		measureTool.setPositions((ArrayList<? extends Position>) positions);
	
				
		DoubleProperty valueProperty= new SimpleDoubleProperty();
		valueProperty.setValue( poli.getArea());

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
					event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE)){//&& measureTool.isArmed()
//				DecimalFormat dc = new DecimalFormat("0.00");
//				dc.setGroupingSize(3);
//				dc.setGroupingUsed(true);					
				double area =  measureTool.getArea()/ProyectionConstants.METROS2_POR_HA;//esto lo hace despues de cada cuadro. no puedo volver a medir el area
			
//					String formated = dc.format(area)+" Ha";
//					t.textProperty().set(formated);

					poli.setPositions( (List<Position>) measureTool.getPositions());
					poli.setArea(area);	
					if(valueProperty.get()!=area && area > 0){
					//poli.setLayer(surfaceLayer);
					//surfaceLayer.setName(poli.getNombre()+" "+formated);
					surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
					valueProperty.setValue(area);
					layerPanel.update(wwd);				//XXX esto hace que se re calcule todo el arbol varias veces?? si pero esta ok.
				}                	                  
			}
		});	
		
		return measureTool;
	}
	
//	private void doMedirSuperfice(Poligono poli, WorldWindow wwd,LayerPanel layerPanel) {
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
//			poli.setNombre(n);
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
//				double	value = measureTool.getArea()/ProyectionConstants.METROS2_POR_HA;
//				if(value != valueProperty.doubleValue() && value > 0){
//					String formated = dc.format(value)+Messages.getString("PoligonLayerFactory.4"); //$NON-NLS-1$
//					t.textProperty().set(formated);
//
//
//					poli.setPositions( (List<Position>) measureTool.getPositions());
//					poli.setArea(value);
//					//poli.setNombre(dc.format(value/ProyectionConstants.METROS2_POR_HA));
//
//					//System.out.println("nombre poli :"+poli.getNombre());
//					poli.setLayer(surfaceLayer);
//					surfaceLayer.setName(poli.getNombre()+" "+formated); //$NON-NLS-1$
//					//ArrayList<? extends Position> positions = measureTool.getPositions();
//					surfaceLayer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, poli);
//					surfaceLayer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, poli.getClass());
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
}
