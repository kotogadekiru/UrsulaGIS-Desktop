package gui.snake;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
//import utils.ProyectionConstants;

public class SnakeSection {

	private static final double MATCH_DISTANCE = 0.0001;
	public double x;
	public double y;
	public Position pos;
	public Angle heading=Angle.fromDegrees(45);//de 0 a 360

	public SnakeSection(double x, double y,double h) {
		this.x = x;
		this.y = y;
		pos=Position.fromDegrees(y, x,h);
	}

	public SnakeSection(double x, double y,double azimut,double h) {
		this.x = x;
		this.y = y;
		this.heading=Angle.fromDegrees(azimut);
		pos=Position.fromDegrees(y, x,h);
	}

	public boolean match(SnakeSection s) {
		Angle dist = Position.greatCircleDistance(pos, s.pos);
		//ProyectionConstants.getDistancia(null, null)
		return dist.degrees<MATCH_DISTANCE;
		//return this.x==s.x && this.y==s.y;
	}
}