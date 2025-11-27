package com.ursulagis.desktop.gui.nww;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;

public class LaborLayer extends RenderableLayer {
	private RenderableLayer analyticSurfaceLayer=null;
	private RenderableLayer extrudedPolygonsLayer=null;
	private int elementsCount=0;
	private boolean showOnlyExtudedPolygons=false;
	private int screenPixelsSectorMinSize=3000;//2000 queda bueno
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
	
	
	@Override
	public void setOpacity(double op) {		
	    /**
	     * Opacity is not applied to layers of this type because each renderable typically has its own opacity control.
	     *
	     * @param opacity the current opacity value, which is ignored by this layer.
	     */
		super.setOpacity(op);
		System.out.println("setting opacity para labor layer "+op);
		if(extrudedPolygonsLayer!=null) {
			extrudedPolygonsLayer.setOpacity(op);
			
			
			for(Renderable r:extrudedPolygonsLayer.getRenderables()) {
				if(ExtrudedPolygon.class.isAssignableFrom(r.getClass())) {					
					ExtrudedPolygon renderablePolygon = (ExtrudedPolygon)r;
				
					//gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon;
				
		
					ShapeAttributes sideAttributes = renderablePolygon.getSideAttributes();
//					if(sideAttributes==null) {
//						sideAttributes=new BasicShapeAttributes();
//					}
//					sideAttributes.setOutlineWidth(0.01);
//					sideAttributes.setOutlineOpacity(0.01);
//					sideAttributes.setDrawInterior(true);
//					sideAttributes.setDrawOutline(false);
					sideAttributes.setInteriorOpacity(op);					
					renderablePolygon.setSideAttributes(sideAttributes);
					
					ShapeAttributes outerAttributes = renderablePolygon.getAttributes();
//					outerAttributes.setOutlineWidth(0.01);
//					outerAttributes.setOutlineOpacity(0.01);
//					outerAttributes.setDrawInterior(true);
//					outerAttributes.setDrawOutline(false);
					outerAttributes.setInteriorOpacity(op);
					renderablePolygon.setAttributes(outerAttributes);
			
				}
			}
			
			
		}
		if(analyticSurfaceLayer!=null) {
			SurfaceImageLayer imageLayer = (SurfaceImageLayer)analyticSurfaceLayer;
			for(Renderable r:imageLayer.getRenderables()) {
				if(ExportableAnalyticSurface.class.isAssignableFrom(r.getClass())) {					
					ExportableAnalyticSurface s = (ExportableAnalyticSurface)r;
					AnalyticSurfaceAttributes att = s.getSurfaceAttributes();
					att.setInteriorOpacity(op);
					s.setSurfaceAttributes(att);
				}
			}
	
		}		
	}

	@Override
	public void dispose() {
		System.out.println("disposing of LaborLayer");
		if(extrudedPolygonsLayer!=null) {
			extrudedPolygonsLayer.dispose();
			extrudedPolygonsLayer=null;
		}
		if(analyticSurfaceLayer!=null) {
			analyticSurfaceLayer.dispose();
			analyticSurfaceLayer=null;
		}
		super.dispose();
	}

	public void render(DrawContext dc){
		if(!this.isEnabled())return;
		long vsNow =dc.getView().getViewStateID();

		double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
		//(this.vS==vsNow  &&
		//extrudedPolygonsLayer != null && (
		//elementsCount<MAX_EXTRUDED_ELEMENTS || //si comento esto cuando es una labor sintentica no se muestra
		if(	analyticSurfaceLayer==null ||
				(eyeElevation < screenPixelsSectorMinSize && this.vS==vsNow)){//primer render o debajo de la altura minima
			if(extrudedPolygonsLayer!=null)extrudedPolygonsLayer.render(dc);//null pointer exception
			if(analyticSurfaceLayer!=null)analyticSurfaceLayer.setEnabled(false);
		} else 	{//if(analyticSurfaceLayer!=null)

			if(!extrudedRendered){
				extrudedPolygonsLayer.render(dc);				
				extrudedRendered=true;
			}
			analyticSurfaceLayer.setEnabled(true);
			analyticSurfaceLayer.render(dc);	
		}
		this.vS=vsNow;
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
			//TODO cambiar esto por una consulta a la base y mostrar un baloon
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
		if(this.extrudedPolygonsLayer!=null) {
			this.renderables.remove(this.extrudedPolygonsLayer);

			this.extrudedPolygonsLayer.removeAllRenderables();
			this.extrudedPolygonsLayer.dispose();
			System.out.println("removing old extrudedPolygonsLayer");

		}
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
