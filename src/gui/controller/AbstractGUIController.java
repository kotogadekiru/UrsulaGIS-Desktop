package gui.controller;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import dao.Labor;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gui.JFXMain;
import gui.nww.LaborLayer;
import gui.nww.LayerPanel;
import gui.nww.WWPanel;
import javafx.scene.layout.Pane;

public abstract class AbstractGUIController {
	public JFXMain main=null;
	public Pane progressBox;
	public ExecutorService executorPool;
	public WWPanel wwjPanel;
	public LayerPanel layerPanel;

	public AbstractGUIController(JFXMain _main) {
		this.main=_main;		
		this.progressBox=main.progressBox;
		this.executorPool=JFXMain.executorPool;
		this.wwjPanel=main.wwjPanel;
		
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
