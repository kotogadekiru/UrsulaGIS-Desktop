package gui.snake;
import java.awt.*;
import javax.swing.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Box;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.Wedge;
import utils.ProyectionConstants;

import java.awt.event.*; // needed for event handling
import java.util.ArrayList;

public class Snake {	  
	//	  static final int SCREEN_SIZE_X=40;         // In units of snake sections.
	//	  static final int SCREEN_SIZE_Y=30;
	public static double scale =10000;
	public static double altitud =20;
	final int MAX_SNAKE_LENGTH = 1000;

	// While a snake is created with a very large number of snake sections,
	// determined by the constant variable MAX_SNAKE_LENGTH, the actual
	// apparent length of the snake in the game will be controlled by the
	// variable snakeLength. At each time step, the program will only draw
	// snakeLength snake sections for each snake. 
	//
	// Each time the snake eats some "food" (a yellow box on the screen), 
	// the snakeLength for that snake will
	// grow by the value of the food, which is printed in the box representing the
	// food. 

	int snakeLength = 50;                      // Start snakes with length 5.
	SnakeSection [] snakeSecs = new SnakeSection[MAX_SNAKE_LENGTH];

	// These variables represent the direction the snake is going.
	// Each time step, the snake moves in the direction represented by these
	// variables. The program does this by adding these values to the previous
	// head position of the snake. For example, the snake goes left initially, 
	// since by adding -1 to the x value (dirX = -1) and adding 0 to the y value
	// (dirY=0), the head of the snake moves one square to the left.

	int dirX=1;
	int dirY=1;
	ShapeAttributes headAttrs = new BasicShapeAttributes();
	ShapeAttributes attrs = new BasicShapeAttributes();
	Wedge snakeHead = null;
	//Box box =null;//sections box

	Color color;               // Holds the color of the snake.
	private int dy;
	private int dx;

	public Snake(SnakeSection startPos,int dx,int dy,Color color) {
		this.dy=dy;
		this.dx=dx;
		// Here, we are creating a large number of snake sections (1000 of them) so
		// that we don't have to worry about creating them later.
		//	    for (int i=0; i<MAX_SNAKE_LENGTH; i++) 
		//	      snakeSecs[i]=new SnakeSection(0,0,0);

		// Set the color of the snake based upon the formal parameter.    
		this.color=color;
		Material mat = new Material(color);
		attrs.setInteriorMaterial(mat);
		attrs.setInteriorOpacity(1);
		attrs.setEnableLighting(true);
		attrs.setOutlineMaterial(mat);
		attrs.setOutlineWidth(2d);
		attrs.setDrawInterior(true);
		attrs.setDrawOutline(false);

		headAttrs.setInteriorMaterial(Material.WHITE);
		headAttrs.setInteriorOpacity(1);
		headAttrs.setEnableLighting(true);
		headAttrs.setOutlineMaterial(mat);
		headAttrs.setOutlineWidth(2d);
		headAttrs.setDrawInterior(true);
		headAttrs.setDrawOutline(false);
		
		snakeHead = new Wedge(
				Position.fromDegrees(startPos.x,startPos.y, altitud), 
				Angle.fromDegrees(180),
				1*scale, 2*scale, 3*scale,
				Angle.fromDegrees(180),//Angle heading,
				Angle.fromDegrees(0),// Angle tilt,
				Angle.fromDegrees(0));// Angle roll);
		snakeHead.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
		snakeHead.setAttributes(headAttrs);
		snakeHead.setValue(AVKey.DISPLAY_NAME, "Snake Head");

		//box = new Box();
		//	  Position.fromDegrees(snakeSecs[i].x,//long
		//	  					   snakeSecs[i].y, //lat
		//	  					   2*scale),//heigt
		//	  2*scale,//widthx
		//	  2*scale,//widthy
		//	  2*scale,//height
		//	  Angle.fromDegrees(180),//Angle heading,
		//      Angle.fromDegrees(0),// Angle tilt,
		//      Angle.fromDegrees(0));// Angle roll);
		

		//box.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);       
		//box.setAttributes(attrs);
		//box.setValue(AVKey.DISPLAY_NAME, "snake body");


		// Here, we create and INITIALIZE the snake sections that are going to be visible
		// at the beginning. We give these locations using a starting position, and offsets
		// from the start position.
		//
		// NOTE: Strictly speaking, it is unnecessary to CREATE the snake sections below
		//       using the new command, since these snake sections have already been created
		//       in the code just above, and all we really want to do is initialize the coordinates
		//       of the snake sections. However, in this case, it is easiest to use the
		//       constructor to initialize the snake sections to the values we want. In order
		//       to use the constructor, we must call new, and thus create the same snake
		//       sections again. It is a little bit of wasted effort, but it won't hurt anything.

		snakeSecs[0]=startPos;
		snakeLength=1;
		for (int i=0; i<50; i++) {			
			move();
			snakeLength++;
		}
			//snakeSecs[i]=new SnakeSection(startPos.x+i*dx,startPos.y+i*dy,altitud);
	}

	// This method returns true if EITHER the head or the body of a snake matches the given coordinates (x,y).

	public boolean contains(double x,double y) {  
		SnakeSection s=new SnakeSection(x,y,0);
		return s.match(snakeSecs[0]) || checkBodyPositions(s);
	}

	// This method returns true if any snake section in the body of a snake matches the given SnakeSection s.
	public boolean checkBodyPositions(SnakeSection s) {
		//boolean collision=false;
		for (int i=1; i<snakeLength; i++) {
			if (s.match(snakeSecs[i]))
				return true;//return at once
		}
		return false;
	}

	public void move() {
		//shift all sections one to the tail
		for (int i=snakeLength-1; i>=0; i--) {
			//System.out.println("moviendo "+i+" a "+(i+1));
			snakeSecs[i+1]=snakeSecs[i];
		}
		
		Angle heading = snakeSecs[1].heading;
	//	System.out.println("heading "+heading);
		if(heading.degrees>360) {
			heading=Angle.fromDegrees(heading.degrees%360);
		}
		if(heading.degrees<0) {
			heading=Angle.fromDegrees(heading.degrees+360);
		}
		Position pos = snakeSecs[1].pos;
		//LatLon nPos =  Position.rhumbEndPosition(pos, heading, Angle.fromDegrees(0.15));
		Point nPoint = ProyectionConstants.getPoint(posToPoint(pos),heading.degrees, 5*1000);
		Position nPos = pointToPos(nPoint);
		double newX=nPos.getLongitude().degrees;
		double newY=nPos.getLatitude().degrees;
		//newY =
		if(newY <= -89 ) {
			System.out.println("newY < -90");
			newX=(newX+180)%180;// : newY > 90 ? -90 : newY;
			heading=Angle.fromDegrees((heading.degrees-180)%180);
		}
		if(newY >= 89) {
			System.out.println(newY+" >= 89");
//			newX=(-60);// : newY > 90 ? -90 : newY;
//			heading=Angle.fromDegrees(180);
			System.out.println("old heading "+heading);
			newX=(newX+180)%180;// : newY > 90 ? -90 : newY;
			heading=Angle.fromDegrees(-(heading.degrees));
			System.out.println("new heading "+heading);
		}
//		if(newX >= 180) {
//			System.out.println("newX > 180");
//			newX=(newX-360);// : newY > 90 ? -90 : newY;			
//		}
//		if(newX <= -180) {
//			System.out.println("newX < -180");
//			newX=(newX+360);// : newY > 90 ? -90 : newY;			
//		}
		snakeSecs[0]=new SnakeSection(newX,newY,heading.degrees,altitud);//add new head
	}

	public Point posToPoint(Position pos) {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Point dest = fact.createPoint(
				new Coordinate(pos.getLongitude().degrees,pos.getLatitude().degrees));		
		return dest;
	}
	public Position pointToPos(Point p) {
		Coordinate c = p.getCoordinate();
		return Position.fromDegrees(c.y,c.x);
	}
	// A snake is painted by drawing a square for each snake section. Each square is 20 by 20 pixels.
	public void paint(Graphics g) { 
		for (int i=1; i<snakeLength; i++) {
			g.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
			g.drawRect((int)snakeSecs[i].x*20,(int)snakeSecs[i].y*20,20,20);
		}
	}
	
	public void paint(DrawContext dc) { 
		
		snakeHead.setCenterPosition(snakeSecs[0].pos);
		snakeHead.setHeading(snakeSecs[0].heading.addDegrees(-90));
				
		snakeHead.render(dc);

		for (int i=1; i<snakeLength; i++) {
			//g.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
			//g.drawRect(snakeSecs[i].x*20,snakeSecs[i].y*20,20,20);
			Box box = new Box();
			//	  Position.fromDegrees(snakeSecs[i].x,//long
			//	  					   snakeSecs[i].y, //lat
			//	  					   2*scale),//heigt
			//	  2*scale,//widthx
			//	  2*scale,//widthy
			//	  2*scale,//height
			//	  Angle.fromDegrees(180),//Angle heading,
			//      Angle.fromDegrees(0),// Angle tilt,
			//      Angle.fromDegrees(0));// Angle roll);
			
			box.setEastWestRadius(scale);
			box.setNorthSouthRadius(scale);
			box.setVerticalRadius(2*scale);
			box.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);       
			box.setAttributes(attrs);
			box.setValue(AVKey.DISPLAY_NAME, "snake body");


			box.setCenterPosition(snakeSecs[i].pos);//heigt);
			box.setHeading(snakeSecs[i].heading);
			box.render(dc);
		}
	}

}

