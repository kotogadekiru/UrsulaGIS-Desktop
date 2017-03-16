package mmg.gui.nww;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;

public class LaborLayer extends RenderableLayer {
	private RenderableLayer analyticSurfaceLayer=null;
	private RenderableLayer extrudedPolygonsLayer=null;
	private int elementsCount=0;
	private boolean showOnlyExtudedPolygons=false;
	private int screenPixelsSectorMinSize=500;
	private static int MAX_EXTRUDED_ELEMENTS=10000;

//	public void preRender(DrawContext dc){
//		if(analyticSurfaceLayer!=null){
//			analyticSurfaceLayer.render(dc);			
//			
//		}
//	}
	
	public void render(DrawContext dc){
		if(!this.isEnabled())return;
		double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
		if(extrudedPolygonsLayer!=null && analyticSurfaceLayer!=null){
			if (showOnlyExtudedPolygons || elementsCount<MAX_EXTRUDED_ELEMENTS || eyeElevation < screenPixelsSectorMinSize ){
				//System.out.println("dibujando para screenSize = " +sectorPixelSizeInWindow);
				extrudedPolygonsLayer.setEnabled(true);
				analyticSurfaceLayer.setEnabled(false);
				try{
					extrudedPolygonsLayer.render(dc);
				}catch(Exception e){
					System.out.println("no se pudo dibujar el extudedPolygonsLayer");
					e.printStackTrace();
				}

			}else{
				analyticSurfaceLayer.setEnabled(true);
				//extrudedPolygonsLayer.setEnabled(false);					
				analyticSurfaceLayer.render(dc);					
			}				
		}
		super.render(dc);
	}

	@Override
	public void pick(DrawContext dc, java.awt.Point point) {//pick(DrawContext dc, Point p){
		if(this.isEnabled()&&extrudedPolygonsLayer!=null ){
			extrudedPolygonsLayer.pick(dc, point);
		} else{
			super.pick(dc, point);
		}
	}

	/**
	 * @return the analyticSurfaceLayer
	 */
	public RenderableLayer getAnalyticSurfaceLayer() {
		return analyticSurfaceLayer;
	}

	/**
	 * @param analyticSurfaceLayer the analyticSurfaceLayer to set
	 */
	public void setAnalyticSurfaceLayer(RenderableLayer analyticSurfaceLayer) {
		this.analyticSurfaceLayer = analyticSurfaceLayer;
	}

	/**
	 * @return the extrudedPolygonsLayer
	 */
	public RenderableLayer getExtrudedPolygonsLayer() {
		return extrudedPolygonsLayer;
	}

	/**
	 * @param extrudedPolygonsLayer the extrudedPolygonsLayer to set
	 */
	public void setExtrudedPolygonsLayer(RenderableLayer extrudedPolygonsLayer) {
		this.extrudedPolygonsLayer = extrudedPolygonsLayer;
		this.elementsCount=extrudedPolygonsLayer.getNumRenderables();
	}

	/**
	 * @return the elementsCount
	 */
	public int getElementsCount() {
		return elementsCount;
	}

	/**
	 * @param elementsCount the elementsCount to set
	 */
	public void setElementsCount(int elementsCount) {
		this.elementsCount = elementsCount;
	}

	/**
	 * @return the showOnlyExtudedPolygons
	 */
	public boolean isShowOnlyExtudedPolygons() {
		return showOnlyExtudedPolygons;
	}

	/**
	 * @param showOnlyExtudedPolygons the showOnlyExtudedPolygons to set
	 */
	public void setShowOnlyExtudedPolygons(boolean showOnlyExtudedPolygons) {
		this.showOnlyExtudedPolygons = showOnlyExtudedPolygons;
	}

}
