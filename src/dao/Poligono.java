package dao;

import java.awt.Component;
import java.util.ArrayList;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import lombok.Data;
@Data
public class Poligono {
	ArrayList<? extends Position> positions = null;
	Layer layer =null;
}
