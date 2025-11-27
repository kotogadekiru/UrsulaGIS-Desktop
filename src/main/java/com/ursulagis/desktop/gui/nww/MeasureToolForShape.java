package com.ursulagis.desktop.gui.nww;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * A specialized measure tool for surface shapes that supports inner boundaries.
 * This tool is designed to work specifically with SurfacePolygon shapes and allows
 * editing, rendering, and measuring complex polygons with holes.
 * 
 * @author UrsulaGIS Team
 * @version 1.0
 */
public class MeasureToolForShape extends AVListImpl implements Disposable {

    // Event constants
    public static final String EVENT_POSITION_ADD = "MeasureToolForShape.AddPosition";
    public static final String EVENT_POSITION_REMOVE = "MeasureToolForShape.RemovePosition";
    public static final String EVENT_POSITION_REPLACE = "MeasureToolForShape.ReplacePosition";
    public static final String EVENT_METRIC_CHANGED = "MeasureToolForShape.MetricChanged";
    public static final String EVENT_ARMED = "MeasureToolForShape.Armed";
    //public static final String EVENT_INNER_BOUNDARY_ADD = "MeasureToolForShape.AddInnerBoundary";
    //public static final String EVENT_INNER_BOUNDARY_REMOVE = "MeasureToolForShape.RemoveInnerBoundary";

    // Control point types
    //public static final String CONTROL_TYPE_LOCATION_INDEX = "MeasureToolForShape.ControlTypeLocationIndex";
   // public static final String CONTROL_TYPE_INNER_BOUNDARY = "MeasureToolForShape.ControlTypeInnerBoundary";

    // Label constants
    public static final String AREA_LABEL = "MeasureToolForShape.AreaLabel";
    public static final String PERIMETER_LABEL = "MeasureToolForShape.PerimeterLabel";
    public static final String LATITUDE_LABEL = "MeasureToolForShape.LatitudeLabel";
    public static final String LONGITUDE_LABEL = "MeasureToolForShape.LongitudeLabel";

    // Core components
    protected WorldWindow wwd;
    protected MeasureToolForShapeController controller;
    protected SurfacePolygon surfaceShape;//the shape to be edited
    protected ScreenAnnotation annotation;

    // Shape data
    // protected ArrayList<Position> positions = new ArrayList<>();
    // protected List<List<Position>> innerBoundaries = new ArrayList<>();
    protected ArrayList<Renderable> controlPoints = new ArrayList<>();

    // Layers
    protected RenderableLayer applicationLayer;//layer provided by the caller
    //protected CustomRenderableLayer layer;
    protected CustomRenderableLayer controlPointsLayer;
    protected CustomRenderableLayer shapeLayer;

    // Visual attributes
    protected Color lineColor = Color.YELLOW;
    protected Color fillColor = new Color(.6f, .6f, .4f, .5f);
    protected double lineWidth = 2;
    protected AnnotationAttributes controlPointsAttributes;
    protected AnnotationAttributes controlPointWithLeaderAttributes;
    protected ShapeAttributes leaderAttributes;
    protected AnnotationAttributes annotationAttributes;

    // Measurement
    protected UnitsFormat unitsFormat = new UnitsFormat();
    protected boolean creationMode = false;
    protected boolean showControlPoints = true;

    /**
     * Construct a new measure tool for surface shapes.
     *
     * @param wwd the WorldWindow to attach to
     */
    public MeasureToolForShape(final WorldWindow wwd) {
        this(wwd, null);
    }

    /**
     * Construct a new measure tool for surface shapes with an application layer.
     *
     * @param wwd the WorldWindow to attach to
     * @param applicationLayer the layer to add shapes to
     */
    public MeasureToolForShape(final WorldWindow wwd, RenderableLayer applicationLayer) {
        if (wwd == null) {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            System.err.println(msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = wwd;
        this.applicationLayer = applicationLayer; // can be null
        this.initializeLayers();
        this.initializeAttributes();
       // this.initializeController();
    }

    /**
     * Initialize the renderable layers.
     */
    protected void initializeLayers() {
        //this.layer = new CustomRenderableLayer();
        this.controlPointsLayer = new CustomRenderableLayer();
        this.shapeLayer = new CustomRenderableLayer();

        // Set initial state of control points layer
        this.controlPointsLayer.setEnabled(this.showControlPoints);
        this.controlPointsLayer.setPickEnabled(true); // Enable picking for control points
        
        // Add layers to the main layer
       
        
        // Add the main layer to the application layer or model
        if (this.applicationLayer != null) {
            this.applicationLayer.addRenderable((Renderable) this.shapeLayer);
            this.applicationLayer.addRenderable((Renderable) this.controlPointsLayer);
            //this.applicationLayer.addRenderable((Renderable) this.layer);
        } 
    }  

    /**
     * Initialize visual attributes.
     */
    protected void initializeAttributes() {
        // Control points attributes - using standard styling
        this.controlPointsAttributes = new AnnotationAttributes();
        this.controlPointsAttributes.setFrameShape(AVKey.SHAPE_RECTANGLE);
        this.controlPointsAttributes.setLeader(AVKey.SHAPE_NONE);
        this.controlPointsAttributes.setAdjustWidthToText(AVKey.SIZE_FIXED);
        this.controlPointsAttributes.setSize(new Dimension(8, 8));
        this.controlPointsAttributes.setDrawOffset(new Point(0, -4));
        this.controlPointsAttributes.setInsets(new Insets(0, 0, 0, 0));
        this.controlPointsAttributes.setBorderWidth(0);
        this.controlPointsAttributes.setCornerRadius(0);
        this.controlPointsAttributes.setBackgroundColor(Color.BLUE);    // Normal color
        this.controlPointsAttributes.setTextColor(Color.GREEN);         // Highlighted color
        this.controlPointsAttributes.setHighlightScale(1.2);
        this.controlPointsAttributes.setDistanceMaxScale(1);            // No distance scaling
        this.controlPointsAttributes.setDistanceMinScale(1);
        this.controlPointsAttributes.setDistanceMinOpacity(1);

        // Control point with leader attributes - using standard styling
        this.controlPointWithLeaderAttributes = new AnnotationAttributes();
        this.controlPointWithLeaderAttributes.setDefaults(this.controlPointsAttributes);
        this.controlPointWithLeaderAttributes.setFrameShape(AVKey.SHAPE_ELLIPSE);
        this.controlPointWithLeaderAttributes.setSize(new Dimension(10, 10));
        this.controlPointWithLeaderAttributes.setDrawOffset(new Point(0, -5));
        this.controlPointWithLeaderAttributes.setBackgroundColor(Color.LIGHT_GRAY);

        // Leader attributes
        this.leaderAttributes = new BasicShapeAttributes();
        this.leaderAttributes.setOutlineMaterial(new Material(this.getLineColor()));
        this.leaderAttributes.setOutlineOpacity(this.getLineColor().getAlpha() / 255d);
        this.leaderAttributes.setOutlineWidth(this.getLineWidth());

        // Annotation attributes
        this.annotationAttributes = new AnnotationAttributes();
        this.annotationAttributes.setFrameShape(AVKey.SHAPE_RECTANGLE);
        this.annotationAttributes.setBackgroundColor(new Color(1f, 1f, 1f, 0.8f));
        this.annotationAttributes.setBorderColor(new Color(0.4f, 0.4f, 0.4f, 1f));
        this.annotationAttributes.setBorderWidth(1);
        this.annotationAttributes.setSize(new Dimension(200, 0));
        this.annotationAttributes.setLeaderGapWidth(0);
        this.annotationAttributes.setCornerRadius(2);
    }

    /**
     * Initialize the controller.
     */
    // protected void initializeController() {
    //     this.controller = new MeasureToolForShapeController();
    //     this.controller.setMeasureToolForShape(this);
    // }

    /**
     * Set the controller for this measure tool.
     *
     * @param controller the controller to use
     */
    public void setController(MeasureToolForShapeController controller) {
        if (this.controller != null) {//remove old controller
            this.wwd.getInputHandler().removeMouseListener(this.controller);
            this.wwd.getInputHandler().removeMouseMotionListener(this.controller);
            this.wwd.removePositionListener(this.controller);
            this.wwd.removeSelectListener(this.controller);
            this.wwd.removeRenderingListener(this.controller);
        }

        this.controller = controller;
        if (this.controller != null) {//add new controller
            this.controller.setMeasureToolForShape(this);

        }
    }

    /**
     * Get the controller for this measure tool.
     *
     * @return the controller
     */
    // public MeasureToolForShapeController getController() {
    //     return this.controller;
    // }

    /**
     * Set the surface shape to work with.
     *
     * @param surfaceShape the surface shape
     */
    public void setSurfaceShape(SurfacePolygon surfaceShape) {
        if (surfaceShape == null) {
            String msg = Logging.getMessage("nullValue.Shape");
            System.err.println(msg);
            throw new IllegalArgumentException(msg);
        }

        // Don't disarm the tool when setting surface shape - let the caller decide
        // setArmed(false);
        this.clear();

        this.surfaceShape = surfaceShape;
        
        // Set tooltip properties for hover functionality
        //shape.setValue("NAME", poli.getNombre());
        
        
        this.shapeLayer.addRenderable(surfaceShape);
        this.shapeLayer.setPickEnabled(true);
        this.shapeLayer.setEnabled(true);
        // this.updatePositionsFromShape();
        this.createControlPoints();
       // this.updateShape();
       // this.updateAnnotation();
    }

    /**
     * Get the current surface shape.
     *
     * @return the surface shape
     */
    public SurfacePolygon getSurfaceShape() {
        return this.surfaceShape;
    }

    /**
     * Add a position to the outer boundary.
     *
     * @param position the position to add
     */
    public void addPosition(Position position) {
        if (position == null) {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            System.err.println(msg);
            throw new IllegalArgumentException(msg);
        }
        try{
            @SuppressWarnings("unchecked")
            List<Position> outerBoundary = (List<Position>) surfaceShape.getOuterBoundary();
            //outerBoundary.add(position);
            if(outerBoundary.size() == 0){
                outerBoundary.add(position);
                outerBoundary.add(outerBoundary.size() - 1, position);
            }else{
                outerBoundary.add(outerBoundary.size() - 2, position);//0,1,2 size=3 -> new position 3-2=1
            }
            this.surfaceShape.setOuterBoundary(outerBoundary);
        }catch(Exception e){
            e.printStackTrace();
        }
        
        this.createControlPoints();
        this.shapeLayer.removeAllRenderables();
        this.shapeLayer.addRenderable(this.surfaceShape);
       
        // this.updateShape();
        //this.updateAnnotation();
        this.firePropertyChange(EVENT_POSITION_ADD, null, position);
    }

    /**
     * Remove a position from the outer boundary.
     *
     * @param index the index of the position to remove
     */
    // public void removePosition(int index) {
    //     try{
    //         @SuppressWarnings("unchecked")
    //         List<Position> outerBoundary = (List<Position>) surfaceShape.getOuterBoundary();         
          
    //     if (index >= 0 && index < outerBoundary.size()) {            
    //         Position removed = outerBoundary.remove(index);
    //         this.createControlPoints();
    //        // this.updateShape();
    //         //this.updateAnnotation();
    //         this.firePropertyChange(EVENT_POSITION_REMOVE, removed, null);
    //     }
    //         }catch(Exception e){
    //             e.printStackTrace();
    //         }

    // }

    /**
     * Replace a position in the outer boundary.
     *
     * @param index the index of the position to replace
     * @param position the new position
     */
    // public void replacePosition(int index, Position position) {
    //     if (position == null) {
    //         String msg = Logging.getMessage("nullValue.PositionIsNull");
    //         System.err.println(msg);
    //         throw new IllegalArgumentException(msg);
    //     }
    //     @SuppressWarnings("unchecked")
    //     List<Position> outerBoundary = (List<Position>) surfaceShape.getOuterBoundary();  
    //     if (index >= 0 && index < outerBoundary.size()) {
    //         Position oldPosition = outerBoundary.set(index, position);
    //         this.createControlPoints();
    //         // this.updateShape();
    //         //this.updateAnnotation();
    //         this.firePropertyChange(EVENT_POSITION_REPLACE, oldPosition, position);
    //     }
    // }

    /**
     * Get the position at a specific index.
     *
     * @param index the index
     * @return the position
     */
    // public Position getPositionAtIndex(int index) {
    //     if (index < this.positions.size()) {
    //         return this.positions.get(index);
    //     } else {
    //         // Check inner boundaries
    //         int innerIndex = index - this.positions.size();
    //         for (List<Position> innerBoundary : this.innerBoundaries) {
    //             if (innerIndex < innerBoundary.size()) {
    //                 return innerBoundary.get(innerIndex);
    //             }
    //             innerIndex -= innerBoundary.size();
    //         }
    //     }
    //     return null;
    // }

    /**
     * Set the position at a specific index in the list
     * @param list 
     *
     * @param index the index
     * @param position the new position
     */
    public void setPositionAtIndex(List<Position> list, int index, Position position) {
        if (position == null) {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            System.err.println(msg);
            throw new IllegalArgumentException(msg);
        }

       
            // Outer boundary (excluding last point which is same as first for closed polygons)
			list.set(index, position);
            // Also update the last point if it's the first point (for closed polygons)
            if (index == 0) {
				list.set(list.size() - 1, position);
            }
       refreshChangedSurfaceShape();
        //this.setSurfaceShape(this.surfaceShape);//force update shape
        // Update the specific control point position
        this.updateControlPointPosition(list,index, position);
        
        // Update tooltip properties when shape is modified
        this.updateTooltipProperties();
       //this.updateShape();
        //this.updateAnnotation();
        // if (this.wwd != null) {
        //     System.out.println("redrawing wwd on setPositionAtIndex");
        //     this.wwd.redraw();
        // }
    }

    private void refreshChangedSurfaceShape() {
        Iterable<? extends LatLon> outerBoundary = surfaceShape.getOuterBoundary();       
           this.surfaceShape.setOuterBoundary(outerBoundary);
    }

    /**
     * Update the position of a specific control point.
     *
     * @param index the index of the control point to update
     * @param position the new position
     */
    protected void updateControlPointPosition(List<Position> list, int index, Position position) {
        // Find the control point with the matching index
        //boolean found = false;
        for (Renderable renderable : this.controlPoints) {
            if (renderable instanceof ControlPoint) {
                ControlPoint controlPoint = (ControlPoint) renderable;
                Integer controlIndex = (Integer) controlPoint.getValue("INDEX");
				//List<Position> positions = (List<Position>) controlPoint.getValue("LIST");
                if (controlIndex != null && controlIndex == index) {
                    controlPoint.setPosition(position);
                    //found = true;
                    break;
                }
            }
        }
        
        // If not found, recreate control points to ensure consistency
        // if (!found) {
        //     this.createControlPoints();
        // }
        
        // Force redraw to ensure the changes are visible
        if (this.wwd != null) {
            this.wwd.redraw();
        }
    }

    /**
     * Arm or disarm the measure tool.
     *
     * @param armed true to arm, false to disarm
     */
    public void setCreationMode(boolean armed) {
        boolean wasArmed = this.creationMode;
        this.creationMode = armed;
        if (wasArmed != this.creationMode) {
            this.firePropertyChange(EVENT_ARMED, wasArmed, this.creationMode);
        }
    }

    /**
     * Check if the measure tool is armed.
     *
     * @return true if armed
     */
    public boolean isCreationMode() {
        return this.creationMode;
    }

    /**
     * Get the measured area.
     *
     * @return the area in square meters
     */
    public double getArea() {
        if (this.surfaceShape == null) {
            return -1;
        }

        Globe globe = this.wwd.getModel().getGlobe();
        return this.surfaceShape.getArea(globe);
    }

    /**
     * Get the measured perimeter.
     *
     * @return the perimeter in meters
     */
    public double getPerimeter() {
        if (this.surfaceShape == null) {
            return -1;
        }

        Globe globe = this.wwd.getModel().getGlobe();
        return this.surfaceShape.getPerimeter(globe);
    }

    /**
     * Get the units format.
     *
     * @return the units format
     */
    public UnitsFormat getUnitsFormat() {
        return this.unitsFormat;
    }

    /**
     * Set the units format.
     *
     * @param unitsFormat the units format
     */
    public void setUnitsFormat(UnitsFormat unitsFormat) {
        this.unitsFormat = unitsFormat;
    }

    /**
     * Get the line color.
     *
     * @return the line color
     */
    public Color getLineColor() {
        return this.lineColor;
    }

    /**
     * Set the line color.
     *
     * @param lineColor the line color
     */
    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
        // this.updateShape();
    }

    /**
     * Get the fill color.
     *
     * @return the fill color
     */
    public Color getFillColor() {
        return this.fillColor;
    }

    /**
     * Set the fill color.
     *
     * @param fillColor the fill color
     */
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
        // this.updateShape();
    }

    /**
     * Get the line width.
     *
     * @return the line width
     */
    public double getLineWidth() {
        return this.lineWidth;
    }

    /**
     * Set the line width.
     *
     * @param lineWidth the line width
     */
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        // this.updateShape();
    }

    /**
     * Get the layer containing the measure tool shapes.
     *
     * @return the layer
     */
    // public RenderableLayer getLayer() {
    //     return this.layer;
    // }

    /**
     * Get the WorldWindow associated with this measure tool.
     *
     * @return the WorldWindow
     */
    public WorldWindow getWwd() {
        return this.wwd;
    }



    /**
     * Create control points for all positions.
     */
    protected void createControlPoints() {
        this.controlPointsLayer.removeAllRenderables();
        this.controlPoints.clear();

        // Only create control points if they should be shown
        if (!this.showControlPoints) {
            return;
        }

        this.surfaceShape.getBoundaries().forEach(boundary -> {
            @SuppressWarnings("unchecked")
            List<Position> list = (List<Position>) boundary;
            createControlPointsForList(list);
        });

        // createControlPointsForList(this.positions);

        // // Create control points for inner boundaries
        // for (List<Position> innerBoundary : this.innerBoundaries) {
        //     createControlPointsForList(innerBoundary);
        // }
        
        // Force redraw to ensure control points are visible
        // if (this.wwd != null) {
        //     this.wwd.redraw();
        // }
    }

	private void createControlPointsForList(List<Position> iterable) {
        int i=0;
		for (Position p:iterable) {
		    this.addControlPoint(p, iterable, i);
            i++;
		}
	}

    /**
     * Add a control point.
     *
     * @param position the position
     * @param controlType the control type
     * @param index the index
     */
    protected void addControlPoint(Position position, Iterable<Position> positions, int index) {
        ControlPoint controlPoint = new ControlPoint(position, this.controlPointsAttributes, this);
		controlPoint.setValue("LIST", positions);
        controlPoint.setValue("INDEX", index);
        //controlPoint.setValue("CONTROL_TYPE", controlType);
        controlPoint.setPickEnabled(true); // Enable picking for this control point
        this.controlPoints.add(controlPoint);
        this.controlPointsLayer.addRenderable(controlPoint);
        
        // Update tooltip properties when shape is modified
        this.updateTooltipProperties();
    }


	public ShapeAttributes getBasicShapeAttributes() {
		ShapeAttributes attr = new BasicShapeAttributes();
		attr.setInteriorMaterial(new Material(this.getFillColor()));
		attr.setInteriorOpacity(this.getFillColor().getAlpha() / 255d);
		attr.setOutlineMaterial(new Material(this.getLineColor()));
		attr.setOutlineOpacity(this.getLineColor().getAlpha() / 255d);
		attr.setOutlineWidth(this.getLineWidth());
		return attr;
	}

    /**
     * Update tooltip properties for the surface shape
     */
    public void updateTooltipProperties() {
        if (this.surfaceShape != null) {
            // Update area and perimeter information for tooltips
            double area =this.getArea()/ ProyectionConstants.METROS2_POR_HA;
            //double area = this.getArea();
            double perimeter = this.getPerimeter();
            
            this.surfaceShape.setValue("AREA", String.format("%.2f Has", area));
            this.surfaceShape.setValue("PERIMETER", String.format("%.2f m", perimeter));
            
            // Update display name if not set
  
        }
    }

    /**
     * Update the annotation with measurement information and position.
     */
    protected void updateAnnotation(Position position) {
        // Only remove the annotation if it exists
        if (this.annotation != null) {
            this.applicationLayer.removeRenderable(this.annotation);
        }

        if (this.surfaceShape != null ) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.surfaceShape.getValue("NAME")).append("\n");
            sb.append(this.unitsFormat.area("Area", this.getArea())).append("\n");
            sb.append(this.unitsFormat.length("Perimeter", this.getPerimeter()));
            if(annotation==null){
                this.annotation = new ScreenAnnotation(sb.toString(), new Point(0, 0));
                this.annotation.setAttributes(this.annotationAttributes);
            }else{
                this.annotation.setText(sb.toString());                
            }            
            
            // Set position if provided
            if (position != null) {
                this.annotation.setPosition(position);
                this.annotation.getAttributes().setVisible(true);
            } else {
                System.out.println("setting annotation with null position");
                this.annotation.getAttributes().setVisible(false);
            }
            
            this.applicationLayer.addRenderable(this.annotation);
        }
    }

    /**
     * Clear all data.
     */
    public void clear() {
        this.controlPointsLayer.removeAllRenderables();
        this.shapeLayer.removeAllRenderables();
        this.controlPoints.clear();
        this.surfaceShape = null;
        
        // Remove annotation if it exists
        if (this.annotation != null) {
            this.applicationLayer.removeRenderable(this.annotation);
            this.annotation = null;
        }
    }

    /**
     * Dispose of resources.
     */
    @Override
    public void dispose() {
        if (this.controller != null) {
            this.wwd.getInputHandler().removeMouseListener(this.controller);
            this.wwd.getInputHandler().removeMouseMotionListener(this.controller);
            this.wwd.removePositionListener(this.controller);
            this.wwd.removeSelectListener(this.controller);
            this.wwd.removeRenderingListener(this.controller);
        }
        
        // Remove layer from application layer or model
        if (this.applicationLayer != null) { 
            this.wwd.getModel().getLayers().remove(this.applicationLayer);
        }
        
        this.clear();
    }

    /**
     * Control point class for editing.
     */
    public static class ControlPoint extends ScreenAnnotation {
        protected MeasureToolForShape parent;

        public ControlPoint(Position position, AnnotationAttributes attributes, MeasureToolForShape parent) {
            super("", new Point(0, 0), attributes);
            this.parent = parent;
            // Set the position after construction
            this.setPosition(position);
        }

        public MeasureToolForShape getParent() {
            return this.parent;
        }
    }

    /**
     * Get whether control points are shown.
     *
     * @return true if control points are shown
     */
    public boolean isShowControlPoints() {
        return this.showControlPoints;
    }

    /**
     * Set whether control points should be shown.
     *
     * @param showControlPoints true to show control points, false to hide them
     */
    public void setShowControlPoints(boolean showControlPoints) {
        this.showControlPoints = showControlPoints;
        if (this.controlPointsLayer != null) {
            this.controlPointsLayer.setEnabled(showControlPoints);
            // Recreate control points based on new setting
          //  this.createControlPoints();
        }
        // if (this.wwd != null) {
        //     this.wwd.redraw();
        // }
    }

    /**
     * Get the application layer that this measure tool is attached to.
     *
     * @return the application layer, or null if not attached to a specific layer
     */
    public RenderableLayer getApplicationLayer() {
        return this.applicationLayer;
    }

        /**
     * Custom renderable layer that properly handles visibility.
     */
    protected static class CustomRenderableLayer extends RenderableLayer implements PreRenderable, Renderable {
        @Override
        public void render(DrawContext dc) {
            // if (dc.isPickingMode() && !this.isPickEnabled()) {
            //     return;
            // }
            // if (!this.isEnabled()) {
            //     return;
            // }
            super.render(dc);
        }
        
        @Override
        public void preRender(DrawContext dc) {

            // if (!this.isEnabled()) {
            //     return;
            // }
            super.preRender(dc);
        }
    }
}
