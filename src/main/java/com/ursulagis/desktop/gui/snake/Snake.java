package com.ursulagis.desktop.gui.snake;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.Wedge;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class Snake {	  
	//	  static final int SCREEN_SIZE_X=40;         // In units of snake sections.
	//	  static final int SCREEN_SIZE_Y=30;
	public static double scale = 10000;
	//private static final double METROS_AVANCE = 1*scale;
	//public static double altitud =20;
	//final int MAX_SNAKE_LENGTH = 1000;

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

	//int snakeLength = 50;                      // Start snakes with length 5.
	Angle heading=null;
	Deque<SnakeSection>  snakeSecs = new ArrayDeque<SnakeSection> ();//[MAX_SNAKE_LENGTH];

	// These variables represent the direction the snake is going.
	// Each time step, the snake moves in the direction represented by these
	// variables. The program does this by adding these values to the previous
	// head position of the snake. For example, the snake goes left initially, 
	// since by adding -1 to the x value (dirX = -1) and adding 0 to the y value
	// (dirY=0), the head of the snake moves one square to the left.

	ShapeAttributes headAttrs = new BasicShapeAttributes();
	ShapeAttributes attrs = new BasicShapeAttributes();
	Wedge snakeHead = null;

	public Snake(Position pos ,Angle heading,Color color) {
		// Set the color of the snake based upon the formal parameter.    
		//this.color=color;
		this.heading=heading;
		constructSnakeHead(pos, heading, color);		
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

		//		snakeSecs.add(startPos);
				grow(1);

		//		snakeLength=1;
		//		for (int i=0; i<50; i++) {			
		//			move();
		//			snakeLength++;
		//		}
		//snakeSecs[i]=new SnakeSection(startPos.x+i*dx,startPos.y+i*dy,altitud);
	}

	public void constructSnakeHead(Position pos, Angle heading, Color color) {
		Material mat = new Material(color);
		attrs.setInteriorMaterial(mat);
		attrs.setInteriorOpacity(1);
		attrs.setEnableLighting(true);
		attrs.setOutlineMaterial(mat);
		attrs.setOutlineWidth(2d);
		attrs.setDrawInterior(true);
		attrs.setDrawOutline(true);

		headAttrs.setInteriorMaterial(Material.WHITE);
		headAttrs.setInteriorOpacity(1);
		headAttrs.setEnableLighting(true);
		headAttrs.setOutlineMaterial(mat);
		headAttrs.setOutlineWidth(2d);
		headAttrs.setDrawInterior(true);
		headAttrs.setDrawOutline(false);

		Double verticalR = 2*scale;

		Double northSouthR = 1*scale;		
		Double eastWestR = 3*scale;
		snakeHead = new Wedge(
				pos, 
				Angle.fromDegrees(180),//angulo del arco
				northSouthR, verticalR, eastWestR,
				heading.addDegrees(-90),//Angle heading,
				Angle.fromDegrees(0),// Angle tilt,
				Angle.fromDegrees(0));// Angle roll);
		snakeHead.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
		snakeHead.setAttributes(headAttrs);
		snakeHead.setValue(AVKey.DISPLAY_NAME, "Snake Head");
	}

	// This method returns true if EITHER the head or the body of a snake matches the given coordinates (x,y).
	public boolean contains(double x,double y) {  
		boolean contains=false;
		Position pos = Position.fromDegrees(y, x);
		for(SnakeSection s: snakeSecs) {
			Angle dist = Position.greatCircleDistance(pos, s.pos);
			if(dist.degrees<SnakeSection.MATCH_DISTANCE) {
				return true;
			}
		}
		return contains;
	}

	// This method returns true if any snake section in the body of a snake matches the given SnakeSection s.
	public boolean checkBodyPositions(SnakeSection s) {
		for (SnakeSection bSection : snakeSecs) {
			if (s.match(bSection))
				return true;//return at once
		}
		return false;
	}

	public void move() {		
		Angle heading = this.heading;

		Position pos = snakeHead.getCenterPosition();
		int size = snakeSecs.size();
		size=(int)Math.log10(size)+1;
		Position nPos = movePosition(pos,heading.degrees,size*scale);

		snakeHead.setCenterPosition(nPos);//move head ahead
		//		double newX=nPos.getLongitude().degrees;
		//		double newY=nPos.getLatitude().degrees;
		//newY =
		//		if(newY <= -89 ) {
		//		//	System.out.println("newY < -90");
		//			newX=(newX+180)%180;// : newY > 90 ? -90 : newY;
		//			heading=Angle.fromDegrees((heading.degrees-180)%180);
		//		}
		//		if(newY >= 89) {
		//			newX=(newX+180)%180;// : newY > 90 ? -90 : newY;
		//			heading=Angle.fromDegrees(-(heading.degrees));
		//		}
		if(snakeSecs.size()>0) {
			SnakeSection last =snakeSecs.pollLast();
			last.pos=pos;
			last.heading=heading;
			//altitud no cambia
			snakeSecs.addFirst(last);
		}
	}

	public static Point posToPoint(Position pos) {
		GeometryFactory fact = ProyectionConstants.getGeometryFactory();
		Point dest = fact.createPoint(
				new Coordinate(pos.getLongitude().degrees,pos.getLatitude().degrees,pos.elevation));		
		return dest;
	}
	public static Position pointToPos(Point p) {
		Coordinate c = p.getCoordinate();
		return Position.fromDegrees(c.y,c.x,c.z);
	}

	public void turnRight() {
		turnSnake(10);	
	}

	public void turnLeft() {
		turnSnake(-10);		
	}

	public void turnSnake(int degrees) {		
		Angle newHeading = this.heading.addDegrees(degrees);	
		//TODO validate new hading between -180 and +180
		int ofset=0;
		if(newHeading.degrees>180 || newHeading.degrees<-180) {		
			System.err.println("new heading error");
			//turning snake -10 from -180.0� newHeading -190.0�
			ofset = newHeading.degrees>0?-360:360;
			newHeading = newHeading.addDegrees(ofset);	
		}
		System.out.println("turning snake "+degrees+" from "+heading+" newHeading "+newHeading);
		this.heading=newHeading;
		Angle head = snakeHead.getHeading(); 		
		Angle newHeadHeading=head.addDegrees(degrees+ofset);		
		snakeHead.setHeading(newHeadHeading);
	}


	public void grow(int foodValue) {
		if(snakeSecs.size()>0) {
			SnakeSection last = snakeSecs.getLast();
			for(int i=0;i<foodValue;i++) {
				Angle heading = last.heading;
				Position pos = last.pos;
				Position nPos = movePosition(pos,heading.degrees+180,3*scale);
				snakeSecs.addLast(new SnakeSection(nPos,heading));//add new head
			}		
		}else {
			//Angle heading = heading;
			Position pos = posWithElev(snakeHead.getCenterPosition(),scale);			
			Position nPos = movePosition(pos,heading.degrees+180,2*scale);
			snakeSecs.addLast(new SnakeSection(nPos,heading));//add new head

		}
	}
	
	public static Position posWithElev(Position pos,Double elev) {
		return Position.fromDegrees(pos.latitude.degrees, pos.longitude.degrees,elev);
	}

	public static Position movePosition(Position pos,Double heading, Double dist) {
		Point nPoint = ProyectionConstants.getPoint(posToPoint(pos),
				heading,//quiero que el nuevo punto aparezca atras del ultimo 
				dist);//avanza 5000mts
		//Position nPos = pointToPos(nPoint);
		//nPos=Position.fromDegrees(nPos.latitude.degrees, nPos.longitude.degrees,pos.elevation);
		return posWithElev(pointToPos(nPoint), pos.elevation);
	}

	public void render(DrawContext dc) {
		snakeHead.render(dc);
		for(SnakeSection s:snakeSecs) {//concurrent mod exception
			s.render(dc);
		}
	}

}

