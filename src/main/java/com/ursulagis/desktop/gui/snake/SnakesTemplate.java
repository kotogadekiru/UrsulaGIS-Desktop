package com.ursulagis.desktop.gui.snake;

import gov.nasa.worldwindx.examples.ApplicationTemplate;

public class SnakesTemplate  extends ApplicationTemplate{
	  public static class AppFrame extends ApplicationTemplate.AppFrame{
	        public AppFrame(){
	            this.makeShapes();
	        }
	        public void makeShapes() {	        	
	        	SnakesLayer layer = new SnakesLayer(getWwd());	        	
	        	insertBeforePlacenames(getWwd(), layer);	        	
	        }
	    }
	  
    public static void main(String[] args) {
        ApplicationTemplate.start("SnakesTemplate", AppFrame.class);
    }
}
