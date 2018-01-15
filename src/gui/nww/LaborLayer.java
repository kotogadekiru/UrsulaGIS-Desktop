package gui.nww;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;

public class LaborLayer extends RenderableLayer {
	private RenderableLayer analyticSurfaceLayer=null;
	private RenderableLayer extrudedPolygonsLayer=null;
	private int elementsCount=0;
	private boolean showOnlyExtudedPolygons=false;
	private int screenPixelsSectorMinSize=1000;//2000 queda bueno
	private static int MAX_EXTRUDED_ELEMENTS=10000;
	long vS =0;
	private boolean extrudedRendered=false;

//	private long lastRendered=System.currentTimeMillis();
//	public void preRender(DrawContext dc){
//		if(analyticSurfaceLayer!=null){
//			analyticSurfaceLayer.render(dc);			
//			
//		}
//	}
	
//	public void preRender(DrawContext dc){
//		GLContext glContext = dc.getGLContext();
//		System.out.println("pre rendering LaborLayer");
//		extrudedPolygonsLayer.getRenderables().forEach((r)->{
//			if(r instanceof ExtrudedPolygon){
//				
//				((ExtrudedPolygon)r).render(dc);
//			}
//			
//		});
//		
//	//	extrudedPolygonsLayer.preRender(dc);
//		
//		dc.setGLContext(glContext);
//	}
	
	public void render(DrawContext dc){
		if(!this.isEnabled())return;
		long vsNow =dc.getView().getViewStateID();
		//long tNow = System.currentTimeMillis();
		double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
		if( //(this.vS==vsNow  &&
				//extrudedPolygonsLayer != null && (
				//elementsCount<MAX_EXTRUDED_ELEMENTS || 
				eyeElevation < screenPixelsSectorMinSize ){
			
			extrudedPolygonsLayer.render(dc);
			analyticSurfaceLayer.setEnabled(false);
		} else //if(analyticSurfaceLayer!=null)
		{
			this.vS=vsNow;
			analyticSurfaceLayer.setEnabled(true);
			analyticSurfaceLayer.render(dc);	
			if(!extrudedRendered){
				extrudedPolygonsLayer.render(dc);
				
				extrudedRendered=true;
			}
			//this.lastRendered=tNow;
		}
		
//		double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
//		if(extrudedPolygonsLayer!=null && analyticSurfaceLayer!=null){
//			if (showOnlyExtudedPolygons || elementsCount<MAX_EXTRUDED_ELEMENTS || eyeElevation < screenPixelsSectorMinSize ){
//				//System.out.println("dibujando para screenSize = " +sectorPixelSizeInWindow);
//				//extrudedPolygonsLayer.setEnabled(true);
//				//analyticSurfaceLayer.setEnabled(false);
//				try{
//					extrudedPolygonsLayer.render(dc);
//					//showOnlyExtudedPolygons=false;
//				}catch(Exception e){
//					System.out.println("no se pudo dibujar el extudedPolygonsLayer");
//					e.printStackTrace();
//				}
//
//			}else{
//				//analyticSurfaceLayer.setEnabled(true);
//				//extrudedPolygonsLayer.setEnabled(false);					
//				analyticSurfaceLayer.render(dc);	
//
//				Timer t = new Timer();
//				t.schedule(new TimerTask(){
//
//					@Override
//					public void run() {					
//						showOnlyExtudedPolygons=true;
//					//	extrudedPolygonsLayer.setEnabled(true);
//					//	analyticSurfaceLayer.setEnabled(false);
//
//						System.out.println("tratando de abilitar extudedPolygonsLayer");
//					}
//
//				}, 1000);
//
//
//			}				
//		}
//		
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
		this.extrudedRendered=false;
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
