package com.ursulagis.desktop.gui.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;

public class WeakBinder {
	private final List<Object> hardRefs = new ArrayList<>();
	private final Map<ObservableValue<?>, WeakInvalidationListener> listeners = new HashMap<>();

	public void unbindAll() {
		for(ObservableValue<?> observableValue : listeners.keySet()) {
			observableValue.removeListener(listeners.get(observableValue));
		}

		hardRefs.clear();
		listeners.clear();
	}


	public <T> void addChangeListener(final InvalidationListener invalidationListener, final ObservableValue<? extends T> dest) {
		WeakInvalidationListener weakInvalidationListener = new WeakInvalidationListener(invalidationListener);
		listeners.put(dest, weakInvalidationListener);
		dest.addListener(invalidationListener);
		hardRefs.add(dest);
		hardRefs.add(invalidationListener);

	}

	public <T> void bind(final Property<T> property, final ObservableValue<? extends T> dest) {
		InvalidationListener invalidationListener = new InvalidationListener() {
			@Override
			public void invalidated(Observable observable) {
				property.setValue(dest.getValue());
			}
		};

		WeakInvalidationListener weakInvalidationListener = new WeakInvalidationListener(invalidationListener);

		listeners.put(dest, weakInvalidationListener);

		dest.addListener(weakInvalidationListener);
		property.setValue(dest.getValue());

		hardRefs.add(dest);
		hardRefs.add(invalidationListener);
	}

	public static void main(String[] args){
		//test invalidationListeners
		StringProperty subject = new SimpleStringProperty("hola");
		WeakReference<InvalidationListener> ref = new  WeakReference<InvalidationListener>(
				new InvalidationListener(){
					@Override
					public void invalidated(Observable observable) {
						SimpleStringProperty sp=(SimpleStringProperty)observable;
						System.out.println("subject invalidated value is: "+sp.getValue());			
					}		  
				}
				);
		subject.addListener(ref.get());

		WeakChangeListener<String> wcr = new WeakChangeListener<String>(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				SimpleStringProperty sp=(SimpleStringProperty)observable;
				System.out.println("subject changed value is: "+newValue);					
			}
		});

		subject.addListener(wcr);
		for(int i =0;i<1000;i++) {
			subject.set(("hola "+i));
		}
		subject=null;
		System.gc();
		for(int i =0;i<10;i++) {
			System.out.println("Invlistener es: "+ref.get());		
			System.out.println("Changelistener fue collected: "+wcr.wasGarbageCollected());			

		}
	}
}