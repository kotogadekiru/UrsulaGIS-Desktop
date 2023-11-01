package gui.controller;

import java.util.concurrent.Executor;

import dao.Labor;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.WorldWindow;
import gui.JFXMain;
import gui.nww.LaborLayer;
import gui.nww.LayerPanel;
import gui.nww.WWPanel;
import javafx.scene.layout.Pane;

public abstract class AbstractGUIController {
	public JFXMain main=null;
	public Pane progressBox;
	public Executor executorPool;
	public WWPanel wwjPanel;
	public LayerPanel layerPanel;

	public AbstractGUIController(JFXMain _main) {
		this.main=_main;		
		this.progressBox=main.progressBox;
		this.executorPool=JFXMain.executorPool;
		this.wwjPanel=main.wwjPanel;
		this.layerPanel=main.getLayerPanel();
	}
	
	public void insertBeforeCompass(WorldWindow wwd, LaborLayer layer) {
		JFXMain.insertBeforeCompass(wwd, layer);		
	}

	public LayerPanel getLayerPanel() {		
		return main.getLayerPanel();
	}

	public WorldWindow getWwd() {		
		return main.getWwd();
	}

	public void viewGoTo(Labor<?> ret) {
		main.viewGoTo(ret);		
	}

	public void playSound() {
		main.playSound();
		
	}
	
}
