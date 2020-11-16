package gui.nww;

import java.util.ArrayList;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.ExtrudedPolygon;

public class ReusableExtrudedPolygon extends ExtrudedPolygon{
	public void clearBoundarys() {
		this.boundaries.clear();
		this.boundaries.add(new ArrayList<LatLon>()); // placeholder for outer boundary
	}

}
