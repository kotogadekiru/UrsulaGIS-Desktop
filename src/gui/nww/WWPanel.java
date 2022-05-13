package gui.nww;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.globes.ElevationModel;
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
import gui.nww.replacementLayers.GIBS_PlaceLabels;
import gui.nww.replacementLayers.GoogleLayer;
import gui.nww.replacementLayers.GoogleTiledImageLayer;
import gui.nww.replacementLayers.Sentinel2Layer;
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

	public WWPanel(Dimension canvasSize, boolean includeStatusBar) {
		super(new BorderLayout());

		this.wwd =new WorldWindowGLJPanel();// this.createWorldWindow();

		//	((Component) this.wwd).setSize((int)canvasSize.getWidth()/4,(int) canvasSize.getHeight()/4);
		((Component) this.wwd).setPreferredSize(canvasSize);

		// Create the default model as described in the current worldwind
		// properties.
		Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
		//		final ElevationModel elevationModel = new ZeroElevationModel(){
		//		m.getGlobe().setElevationModel(elevationModel);

		//m.getLayers().clear();//TODO remover esto que es debug
		AVList crs = new AVListImpl();
		crs.setValue(AVKey.COORDINATE_SYSTEM, "EPSG:4326");


		//		
		//

		//		https://emxsys.net/worldwind25/wms
		//			https://emxsys.net/worldwind26/elev
		//			https://emxsys.net/worldwind27/wms/virtualearth

		//m.getLayers().add(new GIBS_PlaceLabels());
		try {
			m.getLayers().add(	new WMSTiledImageLayer(
					WWXML.openDocumentFile("gui/nww/replacementLayers/GIBS_BlueMarble.xml", null),
					null));
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
		double transicion =3*1000;

		//		GoogleTiledImageLayer sat = new GoogleTiledImageLayer();
		//		sat.setValue(AVKey.DATA_CACHE_NAME, "/Earth/Google/Satellite");
		//		sat.setMinActiveAltitude(transicion);
		//		sat.setMaxActiveAltitude(2*transicion);
		//		m.getLayers().add(sat);

		GoogleLayer sat = new GoogleLayer(GoogleLayer.Type.SATELLITE);
		sat.setValue(AVKey.DATA_CACHE_NAME, "/Earth/Google/Satelite");
		sat.setMinActiveAltitude(transicion+1);
		sat.setMaxActiveAltitude(2*transicion);
		m.getLayers().add(sat);

		try {
			WMSTiledImageLayer bing = new WMSTiledImageLayer(
					WWXML.openDocumentFile("gui/nww/replacementLayers/BingImageryEmxsys.xml", null),
					null);
			bing.setMaxActiveAltitude(transicion);
			bing.setMinActiveAltitude(0);
			m.getLayers().add(bing);
		}catch(Exception e) {
			System.out.println("fallo la carga del layer en gui/nww/replacementLayers/BingImageryEmxsys.xml");
			e.printStackTrace();
		}

		GoogleLayer roads = new GoogleLayer(GoogleLayer.Type.ROADS);
		roads.setValue(AVKey.DATA_CACHE_NAME, "/Earth/Google/Roads");
		roads.setMinActiveAltitude(0);
		roads.setMaxActiveAltitude(2*transicion);
		m.getLayers().add(roads);


		//		m.getLayers().add(
		//				new WMSTiledImageLayer(
		//						WWXML.openDocumentFile("gui/nww/replacementLayers/GIBS_PlaceLabels.xml", null),
		//				crs.copy()));//add city names and such
		//		m.getLayers().add(
		//				new WMSTiledImageLayer(
		//						WWXML.openDocumentFile("gui/nww/replacementLayers/GIBS_ReferenceFeatures.xml", null),
		//				null));//add roads and political boundaries

		//		m.getLayers().add(	new BasicMercatorTiledImageLayer(WWXML.openDocumentFile("gui/nww/replacementLayers/GoogleTiledImage.xml", null),
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
				AVKey.DISPLAY_NAME, null);
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