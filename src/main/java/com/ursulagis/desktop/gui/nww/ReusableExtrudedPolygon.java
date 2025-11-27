package com.ursulagis.desktop.gui.nww;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.ExtrudedPolygon;

public class ReusableExtrudedPolygon extends ExtrudedPolygon{
	public void clearBoundarys() {
		if(boundaries!=null && boundaries.size()>0) {			
			for(List<? extends LatLon> b:boundaries) {
				b.clear();
			}
		}
		//this.boundaries.clear();	
	}
	
	public List<? extends LatLon> getBoundary(){
		if(boundaries.size()==0) {		
			this.boundaries.add(new ArrayList<LatLon>()); // placeholder for outer boundary
		}
		return boundaries.get(0);
	}

}
