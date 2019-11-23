package gui.nww;

import java.util.function.Function;

import gov.nasa.worldwind.layers.Layer;

public class LayerAction implements Function<Layer, String>{
	public String name;
	public Function<Layer, String> predicate;
	public int minElementsRequired=0;

	public LayerAction(Function<Layer, String> _predicate){
		this.predicate=_predicate;
		
	}
	public LayerAction(Function<Layer, String> _predicate,int minElements){
		this.predicate=_predicate;
		this.minElementsRequired=minElements;
		
	}

	@Override
	public String apply(Layer t) {
		return predicate.apply(t);
	}


}
