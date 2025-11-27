package com.ursulagis.desktop.gui.snake;

import java.awt.Color;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicShapeAttributes;
//import com.ursulagis.desktop.utils.ProyectionConstants;
import gov.nasa.worldwind.render.Box;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;

public class SnakeSection {

	static final double MATCH_DISTANCE = 1;

	public Position pos=null;
	public Angle heading=null;//de 0 a 360
	private Box bodyBox =new Box();//sections box

	
	public SnakeSection(Position _pos,Angle _heading) {
		pos=_pos;
		heading=_heading;
		
		Material mat = new Material(Color.red);
        ShapeAttributes attrs = new BasicShapeAttributes();
        attrs.setInteriorMaterial(mat);
        attrs.setOutlineMaterial(Material.DARK_GRAY);
        attrs.setDrawOutline(false);
        attrs.setInteriorOpacity(1);
        attrs.setOutlineOpacity(1);
        attrs.setOutlineWidth(2);
        attrs.setEnableLighting(true);
        
        Double verticalR = 2*Snake.scale;
		
		Double northSouthR = 1*Snake.scale;		
		Double eastWestR = 1*Snake.scale;
        
		//pos=Position.fromDegrees(y, x,2*10000);
		bodyBox.setEastWestRadius(eastWestR);
		bodyBox.setNorthSouthRadius(northSouthR);
		bodyBox.setVerticalRadius(verticalR);
		bodyBox.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);       
		bodyBox.setAttributes(attrs);
		bodyBox.setValue(AVKey.DISPLAY_NAME, "snake body");
		bodyBox.setCenterPosition(pos);
	}



	public boolean match(SnakeSection s) {
		Angle dist = Position.greatCircleDistance(pos, s.pos);
		return dist.degrees<MATCH_DISTANCE;

	}

	public void render(DrawContext dc) {
		bodyBox.setCenterPosition(pos);//heigt);
		bodyBox.setHeading(heading);
		bodyBox.render(dc);
		
	}
}