package com.ursulagis.desktop.gui.nww;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.WorldWindowGLDrawable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
//import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.globes.ElevationModel;
//import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.layers.mercator.BasicMercatorTiledImageLayer;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;
import gov.nasa.worldwindx.examples.util.HighlightController;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.nww.replacementLayers.GIBS_PlaceLabels;
import com.ursulagis.desktop.gui.nww.replacementLayers.GoogleLayer;
import com.ursulagis.desktop.gui.nww.replacementLayers.GoogleTiledImageLayer;
import com.ursulagis.desktop.gui.nww.replacementLayers.Sentinel2Layer;
import com.ursulagis.desktop.gui.nww.ZeroElevationsEarth;
import javafx.scene.layout.BorderPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;

//FIXME cambiar a javaFX SwingNode
public class WWPanel extends JPanel {

	private static final long serialVersionUID = -7158127157119827058L;
	protected WorldWindow wwd;
	protected StatusBar statusBar;
	protected ToolTipController toolTipController;
	protected HighlightController highlightController;
	protected JFXMain main;

	public WWPanel(boolean includeStatusBar, JFXMain jfxMain) {
		super(new BorderLayout());
		this.main=jfxMain;
		this.wwd =new WorldWindowGLJPanel(); // WorldWindowGLJPanel no esta disponible en 2.0.0
		// Try to create WorldWindow using the available configuration

		// Check if WorldWindow was created successfully
		// if (this.wwd == null) {
		// 	throw new RuntimeException("Failed to create WorldWindow component. WorldWind configuration may be incomplete.");
		// }
		
		//	((Component) this.wwd).setSize((int)canvasSize.getWidth()/4,(int) canvasSize.getHeight()/4);
		//((Component) this.wwd).setPreferredSize(canvasSize);

		// Create the default model as described in the current worldwind
		// properties.
		ZeroElevationsEarth earth = new ZeroElevationsEarth();
		BasicModel m = new BasicModel(earth,null);//no tiene minimap ni rosa de los vientos
		//Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
		//m.setGlobe(earth);

		//m.getLayers().clear();//TODO remover esto que es debug
		// AVList crs = new AVListImpl();
		// crs.setValue(AVKey.COORDINATE_SYSTEM, "EPSG:4326");


		//		
		//

		//		https://emxsys.net/worldwind25/wms
		//			https://emxsys.net/worldwind26/elev
		//			https://emxsys.net/worldwind27/wms/virtualearth

		//m.getLayers().add(new GIBS_PlaceLabels());
		try {
			WMSTiledImageLayer blueMarbleLayer = new WMSTiledImageLayer(
				WWXML.openDocumentFile("com/ursulagis/desktop/gui/nww/replacementLayers/GIBS_BlueMarble.xml", null),
				null);
			blueMarbleLayer.setName("Blue Marble");			
			m.getLayers().add(blueMarbleLayer);
		}catch(Exception e) {
			System.out.println("fallo la carga del layer en gui/nww/replacementLayers/GIBS_BlueMarble.xml");
			e.printStackTrace();
		}
		try {
			m.getLayers().add(new Sentinel2Layer());
		}catch(Exception e) {
			System.out.println("fallo la carga del layer en Sentinel2Layer");
			e.printStackTrace();
		}
	//	double transicion =3*1000;
//		GoogleTiledImageLayer sat = new GoogleTiledImageLayer();
//		sat.setValue(AVKey.DATA_CACHE_NAME, "/Earth/Google/Satellite");
//		sat.setMinActiveAltitude(transicion);
//		sat.setMaxActiveAltitude(2*transicion);
//		m.getLayers().add(sat);

		try {
			WMSTiledImageLayer bing = new WMSTiledImageLayer(
					WWXML.openDocumentFile("com/ursulagis/desktop/gui/nww/replacementLayers/BingImageryEmxsys.xml", null),
					null);
			bing.setName("Bing Imagery EXMSYS");
			//bing.setMaxActiveAltitude(transicion);
		//	bing.setMinActiveAltitude(0);
			m.getLayers().add(bing);
		}catch(Exception e) {
			System.out.println("fallo la carga del layer en gui/nww/replacementLayers/BingImageryEmxsys.xml");
			e.printStackTrace();
		}
		
//		GoogleLayer sat = new GoogleLayer(GoogleLayer.Type.SATELLITE);
//		sat.setValue(AVKey.DATA_CACHE_NAME, "/Earth/Google/Satelite");
//		sat.setMinActiveAltitude(0);
//		sat.setMaxActiveAltitude(transicion);
//		m.getLayers().add(sat);

		

		GoogleLayer roads = new GoogleLayer(GoogleLayer.Type.ROADS);
		roads.setValue(AVKey.DATA_CACHE_NAME, "/Earth/Google/Roads");
		roads.setMinActiveAltitude(0);
		roads.setName("Google Roads");
		//roads.setMaxActiveAltitude(200*transicion);
		m.getLayers().add(roads);


		//		m.getLayers().add(
		//				new WMSTiledImageLayer(
		//						WWXML.openDocumentFile("gui/nww/replacementLayers/GIBS_PlaceLabels.xml", null),
		//				crs.copy()));//add city names and such
		//		m.getLayers().add(
		//				new WMSTiledImageLayer(
		//						WWXML.openDocumentFile("gui/nww/replacementLayers/GIBS_ReferenceFeatures.xml", null),
		//				null));//add roads and political boundaries

		//		m.getLayers().add(	new BasicMercatorTiledImageLayer(WWXML.openDocumentFile("com/ursulagis/desktop/gui/nww/replacementLayers/GoogleTiledImage.xml", null),
		//				null));//add roads and political boundaries

		//m.getLayers().add(new CompassLayer());		
		this.wwd.setModel(m);



		// Setup a select listener for the worldmap click-and-go feature
		this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

		this.add((Component) this.wwd, BorderLayout.CENTER);
		
		//this.setCenter(wwd);
		if (includeStatusBar) {
			this.statusBar = new StatusBar();
			//this.setBottom(statusBar);
			this.add(statusBar, BorderLayout.PAGE_END);
			this.statusBar.setEventSource(wwd);
		}

		// Add controllers to manage highlighting and tool tips.
		this.toolTipController = new ToolTipController(this.getWwd(),
				AVKey.DISPLAY_NAME, null,this.main);
		this.highlightController = new HighlightController(this.getWwd(),
				SelectEvent.ROLLOVER);
	}

	//@Override
	public void setPreferredSize(Dimension dim){
		//	super.setPrefSize(dim.getWidth(),dim.getHeight());
		super.setPreferredSize(dim);
		((Component) this.wwd).setPreferredSize(dim);
		//this.wwd.redraw();

	}

	//	protected WorldWindow createWorldWindow() {//Este es el unico metodo de cambie de AppPanel porque sino no andaba con JavaFX
	//		//return new WorldWindowGLCanvas();
	//		return new WorldWindowGLJPanel();
	//		//return new WorldWindow();
	//	}

	public WorldWindow getWwd() {
		return wwd;
	}

	public StatusBar getStatusBar() {
		return statusBar;
	}
}//FIN DE AppPanel