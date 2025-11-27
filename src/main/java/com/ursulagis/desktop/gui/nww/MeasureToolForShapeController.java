package com.ursulagis.desktop.gui.nww;

import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.*;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import com.ursulagis.desktop.gui.nww.MeasureToolForShape.ControlPoint;

/**
 * Controller for the MeasureToolForShape class.
 * Handles mouse interactions for editing surface shapes with inner boundaries.
 *
 * @author UrsulaGIS Team
 * @version 1.0
 */
public class MeasureToolForShapeController extends MouseAdapter implements SelectListener, PositionListener, RenderingListener{
    protected MeasureToolForShape measureToolForShape;
    //protected boolean creationMode = false;//permite agregar o borrar puntos
    // protected boolean active = false;
    protected boolean moving = false;
    protected MeasureToolForShape.ControlPoint movingTarget;
    protected MeasureToolForShape.ControlPoint lastPickedObject;
    // Removed BasicDragger - using direct position updates instead

    public MeasureToolForShapeController(){
        super();
    }

    /**
     * Set the MeasureToolForShape that this controller will be operating on.
     *
     * @param measureToolForShape the MeasureToolForShape that this controller will be operating on.
     */
    public void setMeasureToolForShape(MeasureToolForShape measureToolForShape) {
        if (measureToolForShape == null) {
            throw new IllegalArgumentException("MeasureToolForShape is null");
        }
        this.measureToolForShape = measureToolForShape;
        WorldWindow wwd = measureToolForShape.wwd;
        wwd.getInputHandler().addMouseListener(this);
        wwd.getInputHandler().addMouseMotionListener(this);
        wwd.addPositionListener(this);
        wwd.addSelectListener(this);
        wwd.addRenderingListener(this);        

    }  

    private boolean isCreationMode() {
        return this.measureToolForShape.isCreationMode();
    }

    @Override
    /**
     * handle movingTarget and add position if in creation mode
     */
    public void mousePressed(MouseEvent mouseEvent) {
        //System.out.println("mousePressed");
        if (this.measureToolForShape == null) {
            return;
        }

        if (mouseEvent.getButton() != MouseEvent.BUTTON1) {
            //si el boton no es el 1, no hacer nada
            //TODO permitir borrar un punto si es click derecho y esta en modo creacion
            return;
        }

        // Check if we're picking a control point
        if (this.lastPickedObject != null && this.lastPickedObject instanceof MeasureToolForShape.ControlPoint) {
            this.movingTarget = this.lastPickedObject;
            this.moving = true;
            // this.active = true;
            mouseEvent.consume();
            return;
        }

        // If not picking a control point, only add new positions if we're in creation mode (armed)
        if (this.isCreationMode() && this.measureToolForShape.getWwd() != null) {
            Position position = this.measureToolForShape.getWwd().getCurrentPosition();
            if (position != null) {
                this.measureToolForShape.addPosition(position);
                // this.active = true;
                measureToolForShape.wwd.redraw();
                mouseEvent.consume();
            }
        }
    }
   
    /**
     * handrle moveControlPoint if moving
     */
    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        //System.out.println("mouseDragged");
        if (this.measureToolForShape == null)  {
            return;
        }

        if (this.moving) {
            // Update position during drag
            Position position = this.measureToolForShape.getWwd().getCurrentPosition();
            if (position != null && this.movingTarget != null) {
                ControlPoint controlPoint = (ControlPoint) this.movingTarget;
                Integer index = (Integer) controlPoint.getValue("INDEX");
				List<Position> list = (List<Position>) controlPoint.getValue("LIST");
                if (index != null && list != null) {
                    this.measureToolForShape.setPositionAtIndex(list, index, position);
                    //this.measureToolForShape.
                }
            }
            mouseEvent.consume();
        }
    }

    /**
     * finalize moveControlPoint if moving
     */
    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        //System.out.println("mouseReleased");
        if (this.measureToolForShape == null) {
            return;
        }

        if (this.moving) {
            // Final position update
            Position position = this.measureToolForShape.getWwd().getCurrentPosition();
            if (position != null && this.movingTarget != null) {
				//if(this.movingTarget instanceof MeasureToolForShape.ControlPoint){					
				//MeasureToolForShape.ControlPoint controlPoint = (MeasureToolForShape.ControlPoint) this.movingTarget;
					
				Integer index = (Integer) movingTarget.getValue("INDEX");
				List<Position> list = (List<Position>) movingTarget.getValue("LIST");
				if (index != null && list != null) {
					this.measureToolForShape.setPositionAtIndex(list, index, position);
				}
				//}
                
            }
            
            this.moving = false;
            this.movingTarget = null;
            mouseEvent.consume();
        }
    }


    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        //System.out.println("mouseClicked");
        if (!this.isCreationMode() || this.measureToolForShape == null) {
            return;
        }

        // Handle right-click to remove last position
        if (mouseEvent.getButton() == MouseEvent.BUTTON3){
            //TODO implement borrar ultimo punto agregado
            // if (this.measureToolForShape.getPositions().size() > 0)
            // {
            //     this.measureToolForShape.removePosition(this.measureToolForShape.getPositions().size() - 1);
            //     mouseEvent.consume();
            // }
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        if (!this.isCreationMode() || this.measureToolForShape == null) {
            return;
        }

        // Update cursor based on what's under the mouse
        PickedObjectList pickedObjects = this.measureToolForShape.getWwd().getObjectsAtCurrentPosition();
        if (pickedObjects != null && pickedObjects.size() > 0) {
            PickedObject pickedObject = pickedObjects.getTopPickedObject();
            if (pickedObject != null && pickedObject.getObject() instanceof MeasureToolForShape.ControlPoint)  {
                System.out.println("mouseMoved over control point");
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }
        }
        this.setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void selected(SelectEvent event) {
        if (this.measureToolForShape == null) {
            return;
        }

        // Handle selection events for control points
        if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
            this.highlight(event.getTopObject());
           //this.measureToolForShape.getWwd().redraw();
        }
    }

    @Override
    public void moved(PositionEvent event) {
        if (!this.isCreationMode() || this.measureToolForShape == null) {
            return;
        }

        // Update position if we're moving a control point
        if (this.moving && this.movingTarget != null) {
            Position newPosition = event.getPosition();
            if (newPosition != null) {
				if(this.movingTarget instanceof MeasureToolForShape.ControlPoint){
					MeasureToolForShape.ControlPoint controlPoint = (MeasureToolForShape.ControlPoint) this.movingTarget;
					controlPoint.setPosition(newPosition);
					Integer index = (Integer) controlPoint.getValue("INDEX");
					List<Position> list = (List<Position>) controlPoint.getValue("LIST");
					if (index != null && list != null){
						this.measureToolForShape.setPositionAtIndex(list, index, newPosition);
					}
				}
                // Find the index of the control point and update the corresponding position
               
            }
        }
    }

    @Override
    public void stageChanged(RenderingEvent event) {
        if (!this.isCreationMode() || this.measureToolForShape == null) {
            return;
        }

        // Update the shape during rendering
        if (event.getStage().equals(RenderingEvent.BEFORE_RENDERING)) {
            //this.measureToolForShape.updateShape();
           // this.measureToolForShape.renderShape();
            // Get current mouse position for annotation
            //Position currentPosition = this.measureToolForShape.getWwd().getCurrentPosition();
          //  this.measureToolForShape.updateAnnotation(currentPosition);
        }
    }


    /**
     * Highlight control points on hover.
     *
     * @param o the object to highlight
     */
    protected void highlight(Object o) {
        // Manage highlighting of control points
        if (this.lastPickedObject == o)
            return; // Same thing selected

        // Turn off highlight if on.
        if (this.lastPickedObject != null) {
            this.lastPickedObject.getAttributes().setHighlighted(false);
            this.lastPickedObject.getAttributes().setBackgroundColor(null); // use default
            this.lastPickedObject = null;
            this.setCursor(null);
            
            // Hide annotation when control point is no longer highlighted
            this.measureToolForShape.updateAnnotation(null);
        }

        // Turn on highlight if object selected is a control point and belongs to this controller's MeasureToolForShape.
        if (this.lastPickedObject == null && o instanceof MeasureToolForShape.ControlPoint &&
            ((MeasureToolForShape.ControlPoint) o).getParent() == this.measureToolForShape) {
            this.lastPickedObject = (MeasureToolForShape.ControlPoint) o;
            this.lastPickedObject.getAttributes().setHighlighted(true);
            // Highlight using text color
            this.lastPickedObject.getAttributes().setBackgroundColor(
                    this.lastPickedObject.getAttributes().getTextColor());
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            // Show annotation at control point position
            this.measureToolForShape.updateAnnotation(this.lastPickedObject.getPosition());
        }
    }

    /**
     * Set the cursor for the WorldWindow.
     *modifica el puntero del mouse
     * @param cursor the cursor to set
     */
    protected void setCursor(Cursor cursor) {
        if (this.measureToolForShape != null && this.measureToolForShape.getWwd() != null)
        {
            ((Component) this.measureToolForShape.getWwd()).setCursor(cursor != null ? cursor : Cursor.getDefaultCursor());
        }
    }
}
