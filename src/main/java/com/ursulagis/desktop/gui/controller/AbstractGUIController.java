package com.ursulagis.desktop.gui.controller;

import java.util.concurrent.ExecutorService;

import com.ursulagis.desktop.dao.Labor;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import com.ursulagis.desktop.gui.nww.WWPanel;
import javafx.scene.layout.Pane;

public abstract class AbstractGUIController {
	public JFXMain main=null;
	public Pane progressBox;
	public ExecutorService executorPool;
	//public WWPanel wwjPanel;
	public LayerPanel layerPanel;

	public AbstractGUIController(JFXMain _main) {
		this.main=_main;		
		this.progressBox=main.progressBox;
		this.executorPool=JFXMain.executorPool;		
	}
	public void insertBeforeCompass(WorldWindow wwd, RenderableLayer applicationLayer) {
		JFXMain.insertBeforeCompass(wwd, applicationLayer);		
	}
	public void insertBeforeCompass(WorldWindow wwd, LaborLayer layer) {
		JFXMain.insertBeforeCompass(wwd, layer);		
	}
	
	public void insertBeforeCompass(WorldWindow wwd, Layer layer) {
		JFXMain.insertBeforeCompass(wwd, layer);		
	}

	public LayerPanel getLayerPanel() {		
		return main.getLayerPanel();
	}

	public WorldWindow getWwd() {		
		return main.getWwd();
	}

	
	public void viewGoTo(Layer ndviLayer) {
		main.viewGoTo(ndviLayer);		
	}
	
	public void viewGoTo(Labor<?> ret) {
		main.viewGoTo(ret);		
	}
	public void viewGoTo(Position pos) {
		main.viewGoTo(pos);		
	}

	public void playSound() {
		main.playSound();
		
	}
	
}
