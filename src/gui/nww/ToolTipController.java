/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gui.nww;

import dao.LaborItem;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gui.JFXMain;
import gui.LaborItemGUIController;
import javafx.scene.control.Dialog;
import tasks.ProcessMapTask;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.util.*;

/**
 * Controls display of tool tips on picked objects. Any shape implementing {@link AVList} can participate. Shapes
 * provide tool tip text in their AVList for either or both of hover and rollover events. The keys associated with the
 * text are specified to the constructor.
 *
 * @author tag
 * @version $Id: ToolTipController.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ToolTipController implements SelectListener, Disposable
{
    protected WorldWindow wwd;
    protected String hoverKey = AVKey.HOVER_TEXT;
    protected String rolloverKey = AVKey.ROLLOVER_TEXT;
    protected Object lastRolloverObject;
    protected Object lastHoverObject;
    protected AnnotationLayer layer;
    protected ToolTipAnnotation annotation;
	private Object lastRightClickObject;
	protected JFXMain main;

    /**
     * Create a controller for a specified {@link WorldWindow} that displays tool tips on hover and/or rollover.
     *
     * @param wwd         the World Window to monitor.
     * @param rolloverKey the key to use when looking up tool tip text from the shape's AVList when a rollover event
     *                    occurs. May be null, in which case a tool tip is not displayed for rollover events.
     * @param hoverKey    the key to use when looking up tool tip text from the shape's AVList when a hover event
     *                    occurs. May be null, in which case a tool tip is not displayed for hover events.
     * @param main 
     */
    public ToolTipController(WorldWindow wwd, String rolloverKey, String hoverKey, JFXMain _main){
        this.wwd = wwd;
        this.hoverKey = hoverKey;
        this.rolloverKey = rolloverKey;
        this.main=_main;
        this.wwd.addSelectListener(this);
    }

    /**
     * Create a controller for a specified {@link WorldWindow} that displays "DISPLAY_NAME" on rollover.
     *
     * @param wwd         the World Window to monitor.
     */
    public ToolTipController(WorldWindow wwd) {
        this.wwd = wwd;
        this.rolloverKey = AVKey.DISPLAY_NAME;

        this.wwd.addSelectListener(this);        
    }

    public void dispose(){
        this.wwd.removeSelectListener(this);
    }

    protected String getHoverText(SelectEvent event)  {
        return event.getTopObject() != null && event.getTopObject() instanceof AVList ?
            ((AVList) event.getTopObject()).getStringValue(this.hoverKey) : null;
    }

    protected String getRolloverText(SelectEvent event) {
    	Object obj = event.getTopObject();
    	String ret=null;
    	if(obj instanceof AVList) {
    		ret = ((AVList) obj).getStringValue(this.rolloverKey);
    	}
    	return ret;
//        return event.getTopObject() != null && event.getTopObject() instanceof AVList ?
//            ((AVList) event.getTopObject()).getStringValue(this.rolloverKey) : null;
    }

    public void selected(SelectEvent event) {
    	//System.out.println("tooltip selec listener event "+event.getEventAction());
        try {
            if (event.isRollover() && this.rolloverKey != null) {
            	
            } else if (event.isHover() ) {//&& this.hoverKey != null) {
            	// System.out.println("event.isHover");
            	this.handleRollover(event);
            }
                
           if(event.isRightClick())    {//este evento no se lanza
        	   System.out.println("right click!");
        	   this.handleRigthClick(event);
           }
            
        } catch (Exception e) {
            // Wrap the handler in a try/catch to keep exceptions from bubbling up
            Logging.logger().warning(e.getMessage() != null ? e.getMessage() : e.toString());
        }

    }

//    private void handleRigthClickOld(SelectEvent event) {
//    	this.lastRightClickObject = event.getTopObject();
//    	if(this.lastRightClickObject != null && this.lastRightClickObject instanceof AVList) {
//    		LaborItem item = ((LaborItem)  ((AVList) this.lastRightClickObject).getValue(ProcessMapTask.LABOR_ITEM_AVKey) );	
//    		this.showToolTip(event, "Borrar item "+item.getId()+"?"); 
//    		this.wwd.redraw();    		
//    	}
//	}

	protected void handleRigthClick(SelectEvent event)  {
//        if (this.lastRightClickObject != null) {
//            if (this.lastRightClickObject == event.getTopObject() && !WWUtil.isEmpty(getRolloverText(event)))
//                return;
//
//            this.hideToolTip();
//            this.lastRightClickObject = null;
//            this.wwd.redraw();
//        }

        if (event.getTopObject() != null && event.getTopObject() instanceof AVList) {
            this.lastRightClickObject = event.getTopObject();
            
            LaborItem item = ((LaborItem)  ((AVList) this.lastRightClickObject).getValue(ProcessMapTask.LABOR_ITEM_AVKey) );	
          

            
            LaborItemGUIController controller = new LaborItemGUIController(main);
            controller.showDialog(item);

        }
    }
    
	/**
	 * metodo que se usa para mostrar los tooltips de los layers
	 * @param event
	 */
	protected void handleRollover(SelectEvent event)  {
        if (this.lastRolloverObject != null) {
            if (this.lastRolloverObject == event.getTopObject() && !WWUtil.isEmpty(getRolloverText(event)))
                return;

            this.hideToolTip();
            this.lastRolloverObject = null;
            //this.wwd.redraw();
        }
        Object rolloverObject =event.getTopObject();
        String rolloverText = getRolloverText(event);
        if(rolloverObject instanceof gui.nww.ReusableExtrudedPolygon) {
        	
        	ReusableExtrudedPolygon renderablePolygon = (ReusableExtrudedPolygon)rolloverObject;
        	LaborItem dao = (LaborItem) renderablePolygon.getValue(ProcessMapTask.LABOR_ITEM_AVKey);
        	rolloverText = ProcessMapTask.createTooltipForLaborItem(dao.getGeometry(),dao);        	
        }    
     
       
        if (rolloverText != null)
        {
            this.lastRolloverObject = rolloverObject;
            this.showToolTip(event, rolloverText.replace("\\n", "\n"));
            //this.wwd.redraw();
        }
        this.wwd.redraw();
    }

    protected void handleHover(SelectEvent event) {
    	//System.out.println("hover");
        if (this.lastHoverObject != null){
            if (this.lastHoverObject == event.getTopObject())
                return;

            this.hideToolTip();
            this.lastHoverObject = null;
            //this.wwd.redraw();
        }

        if (getHoverText(event) != null) {
            this.lastHoverObject = event.getTopObject();
            this.showToolTip(event, getHoverText(event).replace("\\n", "\n"));
            //this.wwd.redraw();
        }
        this.wwd.redraw();
    }

    protected void showToolTip(SelectEvent event, String text) {
       
        if (layer == null) {
            layer = new AnnotationLayer();
            layer.setPickEnabled(false);
            this.addLayer(layer);
        }else {
        	layer.setEnabled(true);
        }

        //layer.removeAllAnnotations();
        
        if (annotation != null) {
            annotation.setText(text);           
        }  else  {
            annotation = new ToolTipAnnotation(text);
            System.out.println("creando nuevo tooltip");
            layer.addAnnotation(annotation);
        }
        annotation.setScreenPoint(event.getPickPoint());
    }

    protected void hideToolTip() {
        if (this.layer != null)
        {
        	layer.setEnabled(false);
//            this.layer.removeAllAnnotations();
//            this.removeLayer(this.layer);
//            this.layer.dispose();
//            this.layer = null;
        }

//        if (this.annotation != null)
//        {
//            this.annotation.dispose();
//            this.annotation = null;
//        }
    }

    protected void addLayer(Layer layer)
    {
        if (!this.wwd.getModel().getLayers().contains(layer))
            ApplicationTemplate.insertBeforeCompass(this.wwd, layer);
    }

    protected void removeLayer(Layer layer)
    {
        this.wwd.getModel().getLayers().remove(layer);
    }
}
