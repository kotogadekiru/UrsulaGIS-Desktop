package com.ursulagis.desktop.gui.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import com.ursulagis.desktop.gui.JFXMain;
import javafx.scene.input.KeyCode;
import com.ursulagis.desktop.utils.ProyectionConstants;


public class SnakesLayer extends RenderableLayer {
	Timer timer= new Timer();
	AdvanceTheSnakeTask advanceTask = new AdvanceTheSnakeTask(this);
	final double MIN_TIME_INTERVAL=5;

	int millisecs=200;

	Snake redSnake =null;
	int redCrashes=0;	
	List<SnakeFood> food=new ArrayList<SnakeFood>();

	private WorldWindow ww=null;

	private boolean stop=false;

	public SnakesLayer(WorldWindow worldWindow) {   
		this.ww = worldWindow;
		createSnakes();
		createStartFood();

		// First thing to do: Start up the periodic task:
		System.out.println("About to start the snake.");
		startTimer(millisecs);   // Argument is number of milliseconds per snake move.
		System.out.println("Snake started.");


		JFXMain.stage.getScene().setOnKeyPressed(event->{
			KeyCode key=event.getCode();
			//System.out.println(key+" typed");
			switch(key) {
			case A:redSnakeLeft();break;
			case D:redSnakeRight();break;
			default :break;		  
			}
		});
	}

	public void createStartFood() {
		for(int i=0;i<1000;i++) {
			food.add(getNewFood());
		}
		System.out.println("start food created "+food.size());
	}
	
	public void createSnakes() {
		//millisecs=200;    
		View view = ww.getView();
		Position start = view.getEyePosition();
		start = Snake.posWithElev(start, Snake.scale);
		int viewportWith = view.getViewport().width;
		Angle heading = view.getHeading();
		//Snake.scale=viewportWith/10;
		redSnake = new Snake(start,heading,Color.red);  
	}

	/**
	 * 
	 * @param milliseconds time in milliseconds between successive task executions
	 */
	public void startTimer(int milliseconds) {
		if(stop) return;
		long delay = 1000;
		timer.schedule(advanceTask, delay, milliseconds);
	}

	class AdvanceTheSnakeTask extends TimerTask {
		SnakesLayer layer=null;
		private int totalTimeSteps;
		public AdvanceTheSnakeTask(SnakesLayer _layer) {
			super();
			layer = _layer;
			System.out.println("AdvanceTheSnakeTask Constructor");
		}
		public void run() {
			// Put stuff here that should happen during every advance.
			redSnake.move();             

			Position snakePos = redSnake.snakeHead.getCenterPosition();
			View view = ww.getView();
			if( !view.isAnimating()) {
				//double scale = view.getViewport().getWidth()/10;
				//Snake.scale=10000;
				Position eyePos = view.getEyePosition();				
				
//				double eyeElev=eyePos.elevation;
//				Point newEyePoint = ProyectionConstants.getPoint(
//						Snake.posToPoint(snakePos),
//						redSnake.heading.degrees+180, 
//						eyeElev);
//				double lon=snakePos.getLongitude().degrees;
//				double lat=snakePos.getLatitude().degrees;
//				Position newCenterPos=Position.fromDegrees(lat, lon,eyeElev/3);
//				Position newEyePos=Position.fromDegrees(newEyePoint.getY(),newEyePoint.getX(),eyeElev);
			//	view.setOrientation(newEyePos, newCenterPos);
			
				//view.setHeading(redSnake.heading);
				ww.redrawNow();
			}

			// Here, we check to see if the snakes have collided with each other.
			// This is done by checking whether the head of one snake (SnakeSection 0)
			// is equal in position to one of the SnakeSections of the competitor's snake.
			//
			// To make the game a little more interesting, we will define a collision to 
			// be the intersection of a snake's head with another snake's body. In particular,
			// we will NOT count it as a collision if the two snakes' heads move to the same
			// position at the same time. This will allow the snakes to pass through each other!
			// 
			// It is possible for both snakes to "crash" simultaneously, if, during the same
			// time step, each snake hits the other snake's body. In this case, neither player
			// is awarded any points.

			if(redSnake.snakeSecs.size()>0) {
				SnakeSection first = redSnake.snakeSecs.getFirst();
				boolean redHitsRed = redSnake.checkBodyPositions(first);

//				if (redHitsRed) {           // true if EITHER snake crashes.
//					redCrashes++;
//					createSnakes();
//				}

				if (redCrashes==5) {      // game ends after one player has crashed 5 times.
					stop=true;
				}
			}
			// Here, we check to see if the snake has eaten the current food.
			// Note that we will only check to see if the head of the snake (SnakeSection 0)
			// is in the same place as the food.
			//
			// Note that if both snakes get to the food simultaneously, they both
			// get to eat it.


			SnakeFood foodColission=null;
			for(SnakeFood f:food) {			
				if (f.match(redSnake)) {					
					foodColission=f;
					break;
				}
			}
			if (foodColission!=null) {
				redSnake.grow(foodColission.foodValue);
		
				food.remove(foodColission);
				food.add(getNewFood());
			}


//			totalTimeSteps++;      
//			if (totalTimeSteps%50 == 0 && timer!=null) {                 // Update speed every 50 time steps.
//				timer.cancel();                             // Cancel previous periodic events.
//				if (millisecs>MIN_TIME_INTERVAL) 
//					millisecs = (int) (millisecs * .9);       // Reduce current delay by 10%.  
//				//	System.out.print(millisecs+" ");            // Diagnostic.				
//				startTimer(millisecs);
//			}
		}
	}//fin del timmer task
	
	public SnakeFood getNewFood() {    
		// Now we need to find a position for the food that is not already on one of the snakes.
		// We try different positions until we find one that is not part of a snake.
		boolean acceptable=false;
		double newFoodX=0,newFoodY=0;
		while (!acceptable) {
			newFoodX=(double) (Math.random()*2*180-180);          
			newFoodY=(double) (Math.random()*2*90-90);
			//			if (!redSnake.contains(newFoodX,newFoodY) )
							acceptable=true;
		}
		// Now that we have an acceptable position for the food, put the food in that position.  
		//int foodValue = 3;   // Initial food value.
		int foodValue=(int) (Math.random()*8+1);       // Food has value from 1 to 9.
		SnakeFood f = new SnakeFood(newFoodX,newFoodY,2*Snake.scale);
		f.foodValue=foodValue;
		return f;
	}

	public void redSnakeLeft() {
		System.out.println("turning left");
		redSnake.turnLeft();
	}
	
	public void redSnakeRight() {
		System.out.println("turning right");
		redSnake.turnRight();
	}
	
	public void render(DrawContext dc){
		if(redSnake!=null) {
			redSnake.render(dc);    

			for(SnakeFood f:food) {
				f.render(dc);
			}
		}
	}

	@Override
	public void pick(DrawContext dc, java.awt.Point point) {
		//TODO pick
	}

	public void stop() {
		System.out.println("stopping timer in SnakeLayer");
		this.stop=true;
		if(timer!=null) {
			timer.cancel();
			timer=null;
			this.ww.getModel().getLayers().remove(this);
		}

	}

}
