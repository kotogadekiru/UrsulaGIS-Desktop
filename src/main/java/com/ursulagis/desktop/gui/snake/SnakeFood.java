package com.ursulagis.desktop.gui.snake;

import java.awt.Color;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Box;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwindx.examples.util.ShapeUtils;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class SnakeFood {
	private static final double MATCH_DISTANCE = 1;
//	public double x;
//	public double y;
	public int foodValue;
	public Position pos;
	Box foodBox =new Box();//sections box
	//ShapeAttributes attrs = new BasicShapeAttributes();


	public SnakeFood(double x, double y,double h) {
		Material mat = new Material(Color.green);


        ShapeAttributes attrs = new BasicShapeAttributes();
        attrs.setInteriorMaterial(mat);
        attrs.setOutlineMaterial(Material.DARK_GRAY);
        attrs.setDrawOutline(false);
        attrs.setInteriorOpacity(1);
        attrs.setOutlineOpacity(1);
        attrs.setOutlineWidth(2);
        attrs.setEnableLighting(true);
        
		pos=Position.fromDegrees(y, x,2*10000);
		foodBox.setEastWestRadius(2*10000);
		foodBox.setNorthSouthRadius(2*10000);
		foodBox.setVerticalRadius(2*10000);
		foodBox.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);       
		foodBox.setAttributes(attrs);
		foodBox.setValue(AVKey.DISPLAY_NAME, "snake food");
		foodBox.setCenterPosition(pos);
	}

	//	public SnakeFood(double x, double y,double azimut,double h) {
	//		this.x = x;
	//		this.y = y;
	//		this.heading=Angle.fromDegrees(azimut);
	//		pos=Position.fromDegrees(y, x,h);
	//		foodBox.setEastWestRadius(2*Snake.scale);
	//		foodBox.setNorthSouthRadius(2*Snake.scale);
	//		foodBox.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);       
	//		foodBox.setAttributes(attrs);
	//		foodBox.setValue(AVKey.DISPLAY_NAME, "snake food");
	//		
	//		foodBox.setCenterPosition(pos);
	//
	//	}

	public boolean match(Snake snake) {
		Position snakePos = snake.snakeHead.getCenterPosition();
		//ProyectionConstants.getDistancia(null, null)
		Angle dist = Position.greatCircleDistance(pos, snakePos);
		return dist.degrees<MATCH_DISTANCE;
	}
	
	public void render(DrawContext dc) {
		this.foodBox.render(dc);		
	}
	
}
