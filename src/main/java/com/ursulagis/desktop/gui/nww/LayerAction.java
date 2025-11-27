package com.ursulagis.desktop.gui.nww;

import java.util.function.Function;

import gov.nasa.worldwind.layers.Layer;

public class LayerAction implements Function<Layer, String>, Comparable<LayerAction>{
	public String name;
	public Function<Layer, String> predicate;
	public int minElementsRequired = 0;

	public LayerAction(Function<Layer, String> _predicate){
		this.predicate=_predicate;
	}
	
	public LayerAction(Function<Layer, String> _predicate, String name){
		this.predicate=_predicate;
		this.name=name;
	}
	
	public LayerAction(String name, Function<Layer, String> _predicate, int minElements){
		this.predicate = _predicate;
		this.name=name;
		this.minElementsRequired = minElements;		
	}
	
	public LayerAction(Function<Layer, String> _predicate, int minElements){
		this.predicate = _predicate;
		this.minElementsRequired = minElements;		
	}

	@Override
	public String apply(Layer t) {
		if(t==null)return this.name;
		return predicate.apply(t);
	}
	@Override
	public int compareTo(LayerAction o) {
		return this.name.compareTo(o.name);
	}

	public static LayerAction constructPredicate(String name, Function<Layer, String> action) {
		LayerAction lAction=new LayerAction(action);
		lAction.name=name;
		return lAction;
	}


}
