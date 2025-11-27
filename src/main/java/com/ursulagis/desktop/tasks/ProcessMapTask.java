package com.ursulagis.desktop.tasks;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.geotools.api.data.FeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.geometry.BoundingBox;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.precision.EnhancedPrecisionOp;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.margen.MargenItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionItem;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.ReusableExtrudedPolygon;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import com.ursulagis.desktop.tasks.crear.ConvertirASiembraTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.tasks.crear.CrearFertilizacionMapTask;
import com.ursulagis.desktop.tasks.crear.CrearPulverizacionMapTask;
import com.ursulagis.desktop.tasks.crear.CrearSueloMapTask;
import com.ursulagis.desktop.tasks.importar.OpenMargenMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;
//import org.geotools.api.filter.FilterFactory2;
//TODO change extend to ProgresibleTask<E>
public abstract class ProcessMapTask<FC extends LaborItem,E extends Labor<FC>> extends ProgresibleTask<E>{
	public static final String LABOR_ITEM_AVKey = "LABOR_ITEM";
	private static final int TARGET_LOW_RES_TIME = 2000;
	//private static final String TASK_CLOSE_ICON = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";
//	/**
//	 * cantidad de features a procesar
//	 */
//	protected int featureCount=0;
//	/**
//	 * cantidad de features procesadas
//	 */
//	protected int featureNumber=0;
	protected E labor;

	//protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();

//	private ProgressBar progressBarTask;
//	private Pane progressPane;
//	private Label progressBarLabel;
//	private HBox progressContainer;

	public ProcessMapTask() {
	}

	public ProcessMapTask(E labor2) {
		labor=labor2;
	}

	@Override
	protected E call() throws Exception {

		try {
			labor.clearCache();//remember to clear your cache!!!
			doProcess();
		} catch (Exception e1) {
			System.err.println("Error al procesar el task");
			e1.printStackTrace();
		}

		return labor;
	}

	protected abstract void doProcess()throws IOException ;

	protected abstract int getAmountMin() ;
	protected abstract int gerAmountMax() ;

	//@Deprecated
	protected gov.nasa.worldwind.render.ExtrudedPolygon  getExtrudedPolygonFromGeom(Geometry poly, FC dao,String tooltipText,gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon) {	
		//		gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon = new gov.nasa.worldwind.render.ExtrudedPolygon ();
		// Set the coordinates (in degrees) to draw your polygon
		// To radians just change the method the class Position
		// to fromRadians().

//		Color currentColor = null;
//		try{
//			currentColor = labor.getClasificador().getAwtColorFor(dao.getAmount());
//		}catch(Exception e){
//			e.printStackTrace();
//			currentColor = Color.WHITE;
//		}
//		java.awt.Color awtColor = new java.awt.Color((float) currentColor.getRed(),(float) currentColor.getGreen(),(float) currentColor.getBlue());
		Material material = new Material(labor.getClasificador().getAwtColorFor(dao.getAmount()));

		ShapeAttributes outerAttributes = renderablePolygon.getAttributes();
		if(outerAttributes==null) {
			outerAttributes=new BasicShapeAttributes();
		}		

		outerAttributes.setOutlineWidth(0.01);
		outerAttributes.setOutlineOpacity(0.01);
		outerAttributes.setDrawInterior(true);
		outerAttributes.setDrawOutline(false);
		outerAttributes.setInteriorOpacity(1);
		outerAttributes.setInteriorMaterial(material);
		//normalAttributes.setOutlineMaterial(material);

		//ShapeAttributes sideAttributes = new BasicShapeAttributes();
		ShapeAttributes sideAttributes = renderablePolygon.getSideAttributes();
		if(sideAttributes==null) {
			sideAttributes=new BasicShapeAttributes();
		}
		sideAttributes.setOutlineWidth(0.01);
		sideAttributes.setOutlineOpacity(0.01);
		sideAttributes.setDrawInterior(true);
		sideAttributes.setDrawOutline(false);
		sideAttributes.setInteriorOpacity(1);
		sideAttributes.setInteriorMaterial(Material.BLACK);

		for(int geometryN=0;geometryN<poly.getNumGeometries();geometryN++){
			Geometry forGeom = poly.getGeometryN(geometryN);
			if(forGeom instanceof Polygon){
				Polygon forPolygon = (Polygon)forGeom;
				//	ArrayList<Position> positions = new ArrayList<Position>();     
				/**
				 * recorro el vector de puntos que contiene el poligono gis y creo un
				 * path para dibujarlo
				 */
				if(forPolygon.getNumPoints()==0){
					System.err.println("dibujando un path con cero puntos "+ forPolygon);
					return null;
				}
				//				for (int i = 0; i < forPolygon.getNumPoints(); i++) {
				//					Coordinate coord = forPolygon.getCoordinates()[i];
				//
				//					double z = coord.z>=1?5*(coord.z+1-labor.minElev):1;//XXX hacer que dibujar las coordenadas sea opcional o mover las alturas al model (mas difici)
				//				
				//					if(z<1)z=1;
				//					Position pos = Position.fromDegrees(coord.y,coord.x,z);
				//				
				//					positions.add(pos);
				//				}
				Coordinate[] coordinates = forPolygon.getExteriorRing().getCoordinates();

				double scale=1; 
				if(labor.maxElev>0){
					scale = 5*(labor.minElev/labor.maxElev);
				}
				for(Coordinate c:coordinates){

					//	if(c.z<=labor.minElev){
					c.z=(dao.getElevacion()-labor.minElev)*scale;
					//	} else{
					/*al quitar las superposiciones
					 * 2017-04-10T18:42:33.681-0300  SEVERE  Matrix is not symmetric (NaN, NaN, NaN, 0.0, 
							NaN, NaN, NaN, 0.0, 
							NaN, NaN, NaN, 0.0, 
							0.0, 0.0, 0.0, 0.0)
					 */
					//		c.z=(c.z-labor.minElev)*scale;
					//	}

					if(c.z<1){
						//System.out.println("corrigiendo la elevacion porque era menor a 1 elev="+c.z);
						c.z=1;
					}


				}
				ReusableExtrudedPolygon reusable = (ReusableExtrudedPolygon)renderablePolygon;
				reusable.clearBoundarys();
				@SuppressWarnings("unchecked")
				List<Position> boundary = (List<Position>) reusable.getBoundary();
				//List<Position> exteriorPositions = 
				coordinatesToPositions(coordinates,boundary);


				//renderablePolygon.setOuterBoundary(exteriorPositions);
				//	forPolygon= (Polygon) makeGood(forPolygon);
				//XXX si no pongo esto se superponene los poligonos.
				//XXX pero si lo pongo y el poligono tiene algun error no se muestra correctamente 

				for(int interiorN=0;interiorN<forPolygon.getNumInteriorRing();interiorN++){
					Coordinate[] interiorCoordinates = forPolygon.getInteriorRingN(interiorN).getCoordinates();					
					if(labor.maxElev>0){
						scale = 5*(labor.minElev/labor.maxElev);
					}
					for(Coordinate c:interiorCoordinates){
						c.z=(dao.getElevacion()-labor.minElev)*scale;
						if(c.z<1){
							//System.out.println("corrigiendo la elevacion porque era menor a 1 elev="+c.z);
							c.z=1;
						}
					}
					List<Position> interiorPositions = coordinatesToPositions(interiorCoordinates);

					//List<Position> interiorPositions = coordinatesToPositions(forPolygon.getInteriorRingN(interiorN).getCoordinates());
					renderablePolygon.addInnerBoundary(interiorPositions);
				}

				renderablePolygon.setAttributes(outerAttributes);
				renderablePolygon.setSideAttributes(sideAttributes);
				renderablePolygon.setAltitudeMode(WorldWind.ABSOLUTE);
				renderablePolygon.setValue(AVKey.DISPLAY_NAME, tooltipText);// el tooltip se muestra con el nww.ToolTipAnnotation
				renderablePolygon.setValue(LABOR_ITEM_AVKey, dao);// el tooltip se muestra con el nww.ToolTipAnnotation
				renderablePolygon.setEnableBatchRendering(true);//XXX saco esto para ver si causa el problema del rendering
				//	labor.getLayer().addRenderable(renderablePolygon);




				//				/**old*/
				//				List<Position> exteriorPositions = coordinatesToPositions(forPolygon.getExteriorRing().getCoordinates());
				//				renderablePolygon.setOuterBoundary(exteriorPositions);
				//
				//				for(int interiorN=0;interiorN<forPolygon.getNumInteriorRing();interiorN++){
				//					List<Position> interiorPositions = coordinatesToPositions(forPolygon.getInteriorRingN(interiorN).getCoordinates());
				//					renderablePolygon.addInnerBoundary(interiorPositions);
				//				}



			} else {
				System.err.println("tratando de crear un extruded poligon sin un poligono");
			}
		}//termine de recorrer el multipoligono

		return renderablePolygon;
	}

	/**
	 * 
	 * @param inputGeom
	 * @param dao
	 * @param tooltipText 
	 * @return devuelve el objeto rendereable que se agrega en la coleccion de pathaTooltips y se muestra en runlater()
	 */
	protected List<gov.nasa.worldwind.render.AbstractShape>  getRenderPolygonFromGeom(Geometry inputGeom, FC dao, String tooltipText) {
		getExtrudedPolygonFromGeom(inputGeom, dao, tooltipText,new gov.nasa.worldwind.render.ExtrudedPolygon ());
		return  new ArrayList<>();
		//				if(inputGeom.getNumPoints()==0){
		//					System.err.println("dibujando un path con cero puntos "+ inputGeom);
		//					return null;
		//				}
		//		
		//				Color currentColor = null;
		//				try{
		//					currentColor = labor.getClasificador().getColorFor(dao);
		//				}catch(Exception e){
		//					//e.printStackTrace();
		//					currentColor = Color.WHITE;
		//				}
		//				java.awt.Color awtColor = new java.awt.Color((float) currentColor.getRed(),(float) currentColor.getGreen(),(float) currentColor.getBlue());
		//				Material material = new Material(awtColor);
		//		
		//				ShapeAttributes outerAttributes = new BasicShapeAttributes();
		//				outerAttributes.setOutlineWidth(0.01);
		//				outerAttributes.setOutlineOpacity(0.01);
		//				outerAttributes.setDrawInterior(true);
		//				outerAttributes.setDrawOutline(false);
		//				outerAttributes.setInteriorOpacity(0.8);
		//				outerAttributes.setInteriorMaterial(material);
		//		
		//				//		outerAttributes.setFont(new Font("Serif",Font.PLAIN,24));
		//				//		
		//				//		BasicBalloonAttributes highlightAttrs = new BasicBalloonAttributes();
		//				//		highlightAttrs.setFont(new Font("Serif",Font.PLAIN,24));
		//				//		
		//		
		//				List<gov.nasa.worldwind.render.AbstractShape> renderablePolygons = new ArrayList<>();
		//		
		//				for(int geometryN=0;geometryN<inputGeom.getNumGeometries();geometryN++){
		//					Geometry forGeom = inputGeom.getGeometryN(geometryN);
		//					if(forGeom instanceof Polygon){
		//						gov.nasa.worldwind.render.Polygon  renderablePolygon = new gov.nasa.worldwind.render.Polygon();
		//						renderablePolygon.setAttributes(outerAttributes);
		//		
		//						Polygon forPolygon = (Polygon)forGeom;
		//		
		//						List<Position> exteriorPositions = coordinatesToPositions(forPolygon.getExteriorRing().getCoordinates());
		//						renderablePolygon.setOuterBoundary(exteriorPositions);
		//		
		//						for(int interiorN=0;interiorN<forPolygon.getNumInteriorRing();interiorN++){
		//							List<Position> interiorPositions = coordinatesToPositions(forPolygon.getInteriorRingN(interiorN).getCoordinates());
		//							renderablePolygon.addInnerBoundary(interiorPositions);
		//						}
		//						renderablePolygon.setValue(AVKey.DISPLAY_NAME, tooltipText);// el tooltip se muestra con el nww.ToolTipAnnotation
		//						//renderablePolygon.setValue(AVKey., value)
		//		
		//						//				renderablePolygon.setHighlightAttributes(highlightAttrs);
		//		
		//						renderablePolygon.setEnableBatchRendering(false);//XXX saco esto para ver si causa el problema del rendering
		//						labor.getLayer().addRenderable(renderablePolygon);
		//						//renderablePolygons.add(renderablePolygon);
		//		
		//					}				
		//				}//termine de recorrer el multipoligono
		//		
		//				return renderablePolygons;
	}

	//	@Deprecated
	//	protected List<SurfacePolygon>  getSurfacePolygons(Geometry inputGeom, FC dao) {
	//		if(inputGeom.getNumPoints()==0){
	//			System.err.println("dibujando un path con cero puntos "+ inputGeom);
	//			return null;
	//		}
	//
	//		Color currentColor = null;
	//		try{
	//			currentColor = labor.getClasificador().getColorFor(dao);
	//		}catch(Exception e){
	//			e.printStackTrace();
	//			currentColor = Color.WHITE;
	//		}
	//		java.awt.Color awtColor = new java.awt.Color((float) currentColor.getRed(),(float) currentColor.getGreen(),(float) currentColor.getBlue());
	//		Material material = new Material(awtColor);
	//
	//		ShapeAttributes outerAttributes = new BasicShapeAttributes();
	//		outerAttributes.setOutlineWidth(0.01);
	//		outerAttributes.setOutlineOpacity(0.01);
	//		outerAttributes.setDrawInterior(true);
	//		outerAttributes.setDrawOutline(false);
	//		outerAttributes.setInteriorOpacity(0.8);
	//		outerAttributes.setInteriorMaterial(material);
	//
	//		List<SurfacePolygon> renderablePolygons = new ArrayList<>();
	//
	//
	//
	//
	//		for(int geometryN=0;geometryN<inputGeom.getNumGeometries();geometryN++){
	//			Geometry forGeom = inputGeom.getGeometryN(geometryN);
	//			if(forGeom instanceof Polygon){
	//				//				List<LatLon> locations = Arrays.asList(
	//				//		                LatLon.fromDegrees(20, -170),
	//				//		                LatLon.fromDegrees(15, 170),
	//				//		                LatLon.fromDegrees(10, -175),
	//				//		                LatLon.fromDegrees(5, 170),
	//				//		                LatLon.fromDegrees(0, -170),
	//				//		                LatLon.fromDegrees(20, -170));
	//				SurfacePolygon renderablePolygon = new SurfacePolygon();
	//
	//				//    shape.setAttributes(outerAttributes);
	//
	//				//	gov.nasa.worldwind.render.Polygon  renderablePolygon = new gov.nasa.worldwind.render.Polygon();
	//				renderablePolygon.setAttributes(outerAttributes);
	//
	//				Polygon forPolygon = (Polygon)forGeom;
	//
	//				List<Position> exteriorPositions = coordinatesToPositions(forPolygon.getExteriorRing().getCoordinates());
	//				renderablePolygon.setOuterBoundary(exteriorPositions);
	//
	//				for(int interiorN=0;interiorN<forPolygon.getNumInteriorRing();interiorN++){
	//					List<Position> interiorPositions = coordinatesToPositions(forPolygon.getInteriorRingN(interiorN).getCoordinates());
	//					renderablePolygon.addInnerBoundary(interiorPositions);
	//				}
	//
	//				renderablePolygons.add(renderablePolygon);
	//
	//			}			
	//
	//		}//termine de recorrer el multipoligono
	//
	//		return renderablePolygons;
	//	}

	//	/**
	//	 * este metodo anda bien pero no se muestra nada en la pantalla ???
	//	 * @param inputGeom
	//	 * @param attrs
	//	 * @return
	//	 */
	//	private SurfacePolygons createSurfacePolygons(Geometry inputGeom, ShapeAttributes attrs){
	//
	//		//record.getCompoundPointBuffer()
	//		//(ShapefileRecordPolygon) record).getBoundingRectangle()
	//		Envelope envelope = inputGeom.getEnvelopeInternal();
	//		double minLongitude=envelope.getMinX();
	//		double minLatitude=envelope.getMaxX();
	//		double maxLatitude=envelope.getMinY();
	//		double maxLongitude=envelope.getMaxY();
	//		Sector sector =  Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude);
	//
	//
	//
	//		Coordinate[] coords = inputGeom.getCoordinates();
	//
	//		DoubleBuffer doubleBuffer = Buffers.newDirectDoubleBuffer(2 * coords.length);
	//		VecBufferSequence compoundVecBuffer =new VecBufferSequence(
	//				new VecBuffer(2, new BufferWrapper.DoubleBufferWrapper(doubleBuffer)));
	//
	//		for(int i =0;i<coords.length;i++){
	//			Coordinate c = coords[i];
	//			// doubleBuffer.put(new double[]{c.x,c.y});
	//			DoubleBuffer pointBuffer =DoubleBuffer.wrap(new double[]{c.x,c.y});//aca pongo las coordenadas del punto
	//			VecBuffer vecBuffer = new VecBuffer(2, new BufferWrapper.DoubleBufferWrapper(pointBuffer));
	//
	//			compoundVecBuffer.append(vecBuffer);
	//		}
	//
	//		SurfacePolygons surfacePolygons = new SurfacePolygons(sector,compoundVecBuffer){
	//			protected void drawInterior(DrawContext dc, SurfaceTileDrawContext sdc){
	//
	//				// Exit immediately if the polygon has no coordinate data.
	//				if (this.buffer.size() == 0)
	//					return;
	//
	//				Position referencePos = this.getReferencePosition();
	//				if (referencePos == null)
	//					return;
	//
	//				// Attempt to tessellate the polygon's interior if the polygon's interior display list is uninitialized, or if
	//				// the polygon is marked as needing tessellation.
	//				int[] dlResource = (int[]) dc.getGpuResourceCache().get(this.interiorDisplayListCacheKey);
	//				if (dlResource == null || this.needsInteriorTessellation)
	//					dlResource = this.tessellateInterior(dc, referencePos);
	//
	//				// Exit immediately if the polygon's interior failed to tessellate. The cause has already been logged by
	//				// tessellateInterior().
	//				if (dlResource == null)
	//					return;
	//
	//				GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
	//				this.applyInteriorState(dc, sdc, this.getActiveAttributes(), this.getTexture(), referencePos);
	//				gl.glCallList(dlResource[0]);
	//
	//				if (this.crossesDateLine)
	//				{
	//					gl.glPushMatrix();
	//					try
	//					{
	//						// Apply hemisphere offset and draw again
	//						double hemisphereSign = Math.signum(referencePos.getLongitude().degrees);
	//						gl.glTranslated(360 * hemisphereSign, 0, 0);
	//						gl.glCallList(dlResource[0]);
	//					}
	//					finally
	//					{
	//						gl.glPopMatrix();
	//					}
	//				}
	//			}
	//		};
	//		surfacePolygons.setAttributes(attrs);
	//		// Configure the SurfacePolygons as a single large polygon.
	//		// Configure the SurfacePolygons to correctly interpret the Shapefile polygon record. Shapefile polygons may
	//		// have rings defining multiple inner and outer boundaries. Each ring's winding order defines whether it's an
	//		// outer boundary or an inner boundary: outer boundaries have a clockwise winding order. However, the
	//		// arrangement of each ring within the record is not significant; inner rings can precede outer rings and vice
	//		// versa.
	//		//
	//		// By default, SurfacePolygons assumes that the sub-buffers are arranged such that each outer boundary precedes
	//		// a set of corresponding inner boundaries. SurfacePolygons traverses the sub-buffers and tessellates a new
	//		// polygon each  time it encounters an outer boundary. Outer boundaries are sub-buffers whose winding order
	//		// matches the SurfacePolygons' windingRule property.
	//		//
	//		// This default behavior does not work with Shapefile polygon records, because the sub-buffers of a Shapefile
	//		// polygon record can be arranged arbitrarily. By calling setPolygonRingGroups(new int[]{0}), the
	//		// SurfacePolygons interprets all sub-buffers as boundaries of a single tessellated shape, and configures the
	//		// GLU tessellator's winding rule to correctly interpret outer and inner boundaries (in any arrangement)
	//		// according to their winding order. We set the SurfacePolygons' winding rule to clockwise so that sub-buffers
	//		// with a clockwise winding ordering are interpreted as outer boundaries.
	//		surfacePolygons.setWindingRule(AVKey.CLOCKWISE);
	//		surfacePolygons.setPolygonRingGroups(new int[] {0});
	//		surfacePolygons.setPolygonRingGroups(new int[] {0});
	//		return surfacePolygons;// layer.addRenderable(shape);
	//	}

	/*
	 * metodo que toma una lista de Features y los convierte a puntos en una superficie analitica para mostrar en la pantalla
	 */
	private RenderableLayer createAnalyticSurface(Collection<FC> items){
		/* creo los datos para el layer*/
		double resolution = 10*ProyectionConstants.metersToLong();//si aumento la resolucion aumento los lugares sin informacion; entonces deberia hacer un interpolado para que no se vea tan feo

		double minX = labor.minX.getLongitude().degrees;
		double minY = labor.minY.getLatitude().degrees;
		double maxX = labor.maxX.getLongitude().degrees;;
		double maxY = labor.maxY.getLatitude().degrees;

		System.out.println("creando analyticSurface con minX="+minX+
				" minY="+minY+" maxX="+maxX+" maxY="+maxY);
		//creando analyticSurface con minX=0.0 minY=0.0 maxX=-1.0 maxY=-1.0

		Double maxElev = labor.maxElev;
		Double minElev = labor.minElev;

		int offset = 3;//para que quede un lugar a cada lado mas el desplazamiento
		int  width=(int) ((maxX-minX)/resolution)+offset;
		int  height=(int) ((maxY-minY)/resolution)+offset;
		int maxIndex =  width*height;

		//indexar las features de acuerdo a su ubicacion
		ConcurrentMap<Integer, List<FC>> indexMap = items.parallelStream().collect(Collectors.groupingByConcurrent((f)->{
			Point center = f.getGeometry().getCentroid();
			Coordinate coord = center.getCoordinate();// si la geometria es grande esto es insuficiente

			int col= (int)((coord.x-minX) / resolution)+1;
			int fila = (int)((-coord.y+maxY) / resolution)+1;
			int index = (col+fila*width);

			return index;
		}));

		AnalyticSurface.GridPointAttributes transparent  =  AnalyticSurface.createGridPointAttributes(0, new java.awt.Color(0,0,0,0));

		LinkedList<AnalyticSurface.GridPointAttributes> attributesList = new LinkedList<AnalyticSurface.GridPointAttributes>();
		Map<String,GridPointAttributes> gpMap=new HashMap<String,GridPointAttributes>();
		for(int index=0;index<maxIndex;index++){
			GridPointAttributes newGridPoint  = transparent;
			List<FC> indexItems = indexMap.getOrDefault(index, null);		

			//rellenando los huecos con el promedio de los vecinos
			if(indexItems==null){
				List<FC> average = new ArrayList<FC>();
				ArrayList<FC> defaultL = new ArrayList<FC>();

				int w =1;
				for(int ofset =-w;ofset<w+1;ofset++){
					average.addAll(indexMap.getOrDefault(index+ofset, defaultL ));//solo promedia con los de los costados
					average.addAll(indexMap.getOrDefault(index+ofset*width, defaultL ));//arriba y abajo
					defaultL.clear();
				}
				indexItems=average;
			}
			if(indexItems!=null && indexItems.size()>0){
				//float r =0,g = 0,b=0,
				double elev=0;
				double amount=0;
				for(FC it : indexItems){
					elev+= it.getElevacion()-minElev;
					amount+=it.getAmount();
				}

				int n = indexItems.size();
//				Color color = labor.getClasificador().getColorFor(amount/n);
//				float r=(float) color.getRed();//0.99607843
//				float g=(float) color.getGreen();
//				float b=(float) color.getBlue();
//				java.awt.Color rgbaColor = new java.awt.Color(r,g,b,1);//IllegalArgumentException - if r, g b or a are outside of the range 0.0 to 1.0, inclusive
				String kpKey = getGPKey(elev/n,amount/n);
				if(gpMap.containsKey(kpKey)) {
					newGridPoint = gpMap.get(kpKey);
				} else {
				java.awt.Color rgbaColor = labor.getClasificador().getAwtColorFor(amount/n);//new java.awt.Color(r,g,b,1);//IllegalArgumentException - if r, g b or a are outside of the range 0.0 to 1.0, inclusive
				
				newGridPoint  =  AnalyticSurface.createGridPointAttributes(elev/n, rgbaColor);
				gpMap.put(kpKey, newGridPoint);
				}
				//newGridPoint  =  AnalyticSurface.createGridPointAttributes(elev/n, labor.getClasificador().getAwtColorFor(amount/n));
			}
			//	System.out.println("agregando el elemento "+index);
			attributesList.add(index,newGridPoint);

		}
		//		System.out.println("attributesList size = "+attributesList.size());
		//		System.out.println("deberia ser "+(maxIndex+1));

		/*   creo la superficie  */
		AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
		attr.setDrawOutline(false);
		attr.setDrawShadow(false);
		attr.setInteriorOpacity(1);

		final ExportableAnalyticSurface surface = new ExportableAnalyticSurface();
		Sector sector = Sector.fromDegrees(
				minY,maxY,minX,maxX);//+/- 90 degrees latitude
		surface.setSector(sector);
		surface.setDimensions((int)width ,(int)  height );


		//		Material material = new Material(java.awt.Color.blue);		
		//		surface.getSurfaceAttributes().setInteriorMaterial(material);

		surface.setValues(attributesList);//.subList(0, width*height));
		double scale = 	(maxElev)/minElev;
		surface.setVerticalScale(5/scale);		
		surface.setSurfaceAttributes(attr);
		surface.setAltitude(1);


		/*   Creo la leyenda   */
		//	Format legendLabelFormat = new DecimalFormat() ;
		NumberFormat legendLabelFormat=Messages.getNumberFormat();
		Color colorMin = labor.getClasificador().getColorFor(labor.minAmount);// Clasificador.colors[0];
		Color colorMax =labor.getClasificador().getColorFor(labor.maxAmount);// Clasificador.colors[Clasificador.colors.length-1];
		double HUE_MIN = colorMin.getHue()/360d;//0d / 360d;
		double HUE_MAX = colorMax.getHue()/360d;//240d / 360d;

		final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(
				labor.minAmount,labor.maxAmount,
				HUE_MIN, HUE_MAX,
				AnalyticSurfaceLegend.createDefaultColorGradientLabels(labor.minAmount, labor.maxAmount, legendLabelFormat),
				AnalyticSurfaceLegend.createDefaultTitle(labor.getNombre())
				);
		legend.setOpacity(1);
		legend.setScreenLocation(new java.awt.Point(100, 400));

		Renderable legendRenderable =  new Renderable()	{
			public void render(DrawContext dc){
				Extent extent = surface.getExtent(dc);
				if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
					return;
				//TODO usar esto para cambiar entre el rendering de analitic surface y Polygons
				if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300)//limite de elevacion
					return;
				legend.render(dc);
			}
		};


		//RenderableLayer layer = new RenderableLayer();
		SurfaceImageLayer layer = new SurfaceImageLayer();
		layer.addRenderable(surface);
		layer.addRenderable(legendRenderable);
		layer.setPickEnabled(false);

		return layer;
	}


	/*
	 * metodo que toma una lista de Features y los convierte a puntos en una superficie analitica para mostrar en la pantalla
	 */
	private RenderableLayer createAnalyticSurfaceFromQuery(int milis){		
		ReferencedEnvelope bounds = labor.outCollection.getBounds();
		//	System.out.println("createAnalyticSurfaceFromQuery");
		//System.out.println("bounds = "+bounds);
		double res=  Math.sqrt(bounds.getArea()/(milis));//antes dividia por 10000 cuando eran segundos
		double	resolution =res;// Math.sqrt(bounds.getArea()/40000)>1?;//como el tiempo por item es 0.1 limito el tiempo de rendering a 2seg
		double	ancho = resolution / ProyectionConstants.metersToLong();
		//	System.out.println("ancho "+ancho);
		double minX = bounds.getMinX();
		double minY = bounds.getMinY();
		double maxX = bounds.getMaxX();
		double maxY = bounds.getMaxY();
		//System.out.println("bounds: "+bounds);
		//	System.out.println("creando analyticSurface con segs="+segs);

		Double maxElev = Math.max(labor.maxElev,1.0);
		Double minElev = Math.min(labor.minElev,1.0);

		//System.out.println("maxElev,minElev= "+maxElev+", "+minElev);

		int offset = 3;//para que quede un lugar a cada lado mas el desplazamiento
		int width=Math.max((int) ((maxX-minX)/resolution)+offset,1);
		int height=Math.max((int) ((maxY-minY)/resolution)+offset,1);
		int maxIndex =  width*height;

		//System.out.println("width="+width+" height="+height+" maxIndex="+maxIndex);
		Map<String,GridPointAttributes> gpMap=new HashMap<String,GridPointAttributes>();
		AnalyticSurface.GridPointAttributes transparent  =  AnalyticSurface.createGridPointAttributes(0, new java.awt.Color(0,0,0,0));
		LinkedList<AnalyticSurface.GridPointAttributes> attributesList = new LinkedList<AnalyticSurface.GridPointAttributes>();
		for(int i = 0;i<maxIndex;i++){
			attributesList.add(transparent);
		}

		//ReferencedEnvelope unionEnvelope = labor.outCollection.getBounds();

		Consumer<Polygon> polygonConsumer = new Consumer<Polygon>(){

			@Override
			public void accept(Polygon p) {
				Point center = p.getCentroid();
				Coordinate coord = center.getCoordinate();
				//calculo el indice en el que tiene que ir el nuevo dato
				int col= (int)((coord.x-minX) / resolution)+1;
				int fila = (int)((-coord.y+maxY) / resolution)+1;//da negativo cuando y esta fuera de min max
				int index = (col+fila*width);//index me da negativo

				if(index<0 ||index>=maxIndex) {
					System.err.println("fila="+fila+" col="+col+" index="+index);
					//System.err.println("index out of range for "+center+" bounds = "+bounds);
					return;
				} else { 
					//System.out.println("procesando index= "+index);
				}
				Envelope envelope = p.getEnvelopeInternal();
				//envelope.expandBy(2*resolution);//esto me trae algunos problemas 
				List<FC> fueaturesToAdd = labor.cachedOutStoreQuery(envelope);

				//promediar
				GridPointAttributes newGridPoint  = transparent;

				if(fueaturesToAdd!=null && fueaturesToAdd.size()>0){
					//float r =0,g = 0,b=0,
					double elev=0;
					double amount=0;

					for(FC it : fueaturesToAdd){
						elev+= it.getElevacion()-minElev;
						amount+=it.getAmount();
					}

					int n = fueaturesToAdd.size();
//					Color color = labor.getClasificador().getColorFor(amount/n);
//					
//					float r=(float) color.getRed();//0.99607843
//					float g=(float) color.getGreen();
//					float b=(float) color.getBlue();
					String kpKey = getGPKey(elev/n,amount/n);
					if(gpMap.containsKey(kpKey)) {
						newGridPoint = gpMap.get(kpKey);
					} else {
					java.awt.Color rgbaColor = labor.getClasificador().getAwtColorFor(amount/n);//new java.awt.Color(r,g,b,1);//IllegalArgumentException - if r, g b or a are outside of the range 0.0 to 1.0, inclusive
					
					newGridPoint  =  AnalyticSurface.createGridPointAttributes(elev/n, rgbaColor);
					gpMap.put(kpKey, newGridPoint);
					}
				} else {
					//System.out.println("no hay features para fila,columna= "+fila+","+col);
				}
				try{
					attributesList.set(index,newGridPoint);				
				}catch(Exception e){
					System.out.println("excepcion tratando de agregar el index "+index+" size="+attributesList.size());
				}
			}
		};
		construirGrilla(bounds, ancho,polygonConsumer);

		/*   creo la superficie  */
		AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
		attr.setDrawOutline(false);
		attr.setDrawShadow(false);
		attr.setInteriorOpacity(1);

		final ExportableAnalyticSurface surface = new ExportableAnalyticSurface();
		Sector sector = Sector.fromDegrees(
				minY,maxY,minX,maxX);//+/- 90 degrees latitude
		surface.setSector(sector);
		surface.setDimensions((int)width ,(int)  height );

		surface.setValues(attributesList);//.subList(0, width*height));

		double scale =1.0;
		if(minElev>0){
			scale = 	(maxElev)/minElev;// 1/1=1 en cambio 0/0=inf
		}
		surface.setVerticalScale(5/scale);		
		surface.setSurfaceAttributes(attr);
		surface.setAltitude(1);

		/*   Creo la leyenda   */
		NumberFormat legendLabelFormat=Messages.getNumberFormat();

		Color colorMin = labor.getClasificador().getColorFor(labor.minAmount);// Clasificador.colors[0];
		Color colorMax =labor.getClasificador().getColorFor(labor.maxAmount);// Clasificador.colors[Clasificador.colors.length-1];
		double HUE_MIN = colorMin.getHue()/360d;//0d / 360d;
		double HUE_MAX = colorMax.getHue()/360d;//240d / 360d;

		final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(
				labor.maxAmount,
				labor.minAmount,//FIXME valores invertidos funciona en una version de world wind y no en otra
				HUE_MIN, HUE_MAX,
				AnalyticSurfaceLegend.createDefaultColorGradientLabels(labor.minAmount, labor.maxAmount, legendLabelFormat),
				AnalyticSurfaceLegend.createDefaultTitle(labor.getNombre())
				);
		legend.setOpacity(1);
		legend.setScreenLocation(new java.awt.Point(100, 400));

		Renderable legendRenderable =  new Renderable()	{
			public void render(DrawContext dc){
				Extent extent = surface.getExtent(dc);
				if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
					return;
				//TODO usar esto para cambiar entre el rendering de analitic surface y Polygons
				if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300)//limite de elevacion
					return;
				legend.render(dc);
			}
		};

		SurfaceImageLayer layer = new SurfaceImageLayer();
		layer.addRenderable(surface);
		layer.addRenderable(legendRenderable);
		layer.setPickEnabled(false);

		return layer;
	}
	private String getGPKey(Double v,Double v2) {
		String ret =null;
		String sv = Messages.getNumberFormat().format(v);
		String sv2 = Messages.getNumberFormat().format(v2);		
		ret =sv+"-"+sv2;
		return ret;
	}
	public static boolean readerHasNext(FeatureReader<SimpleFeatureType, SimpleFeature> reader) {
		try{
			return reader.hasNext();
		}catch(Exception e ){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion
	 */
	public static void construirGrilla(BoundingBox bounds,double ancho,Consumer<Polygon> lambda) {
		//System.out.println("construyendo grilla");
		//List<Polygon> polygons = new ArrayList<Polygon>();
		//convierte los bounds de longlat a metros
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong();
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat();
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong();
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat();
		Double x0=minX,x1=minX;
		Double y0=minY,y1=minY;


		GeometryFactory fact = new GeometryFactory();
		Coordinate A = new Coordinate(); 
		Coordinate B = new Coordinate(); 
		Coordinate C = new Coordinate();
		Coordinate D = new Coordinate();
		//Coordinate[] coordinates = new Coordinate[5];
		Coordinate[] coordinates = { A, B, C, D, A };
		for(int x=0;(x0)<maxX;x++){
			x0=minX+x*ancho;
			//Double
			x1=minX+(x+1)*ancho;
			for(int y=0;(minY+y*ancho)<maxY;y++){
				//Double 
				y0=minY+y*ancho;
				//Double 
				y1=minY+(y+1)*ancho;

				//Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
				D.x=x0*ProyectionConstants.metersToLong(); D.y=y0*ProyectionConstants.metersToLat();
				//Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
				C.x=x1*ProyectionConstants.metersToLong(); C.y=y0*ProyectionConstants.metersToLat();
				//Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
				B.x=x1*ProyectionConstants.metersToLong(); B.y= y1*ProyectionConstants.metersToLat();
				//Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
				A.x=x0*ProyectionConstants.metersToLong(); A.y=y1*ProyectionConstants.metersToLat();

				//System.out.println(A.y+" "+B.y+" "+C.y+" "+D.y);

				/**
				 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
				 * carro--B
				 * 
				 */
				//Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
				//				coordinates[0]=A;
				//				coordinates[1]=B;
				//				coordinates[2]=C;
				//				coordinates[3]=D;
				//				coordinates[4]=A;

				// Empezar y terminar en
				// el mismo punto.
				// sentido antihorario

				//			GeometryFactory fact = X.getFactory();



				//				DirectPosition upper = positionFactory.createDirectPosition(new double[]{-180,-90});
				//				DirectPosition lower = positionFactory.createDirectPosition(new double[]{180,90});
				//	Envelope envelope = geometryFactory.createEnvelope( upper, lower );

				//LinearRing shell = fact.createLinearRing(coordinates);
				//LinearRing[] holes = null;
				Polygon poly =	fact.createPolygon(coordinates);// new Polygon(shell, holes, fact);
				//executorPool.execute(()->lambda.accept(poly));
				lambda.accept(poly);

				//polygons.add(poly);
			}
		}

	}

	private void coordinatesToPositions(Coordinate[] coordinates,List<Position> positions ){
		//ArrayList<Position> positions = new ArrayList<Position>();     
		for (int i = 0; i < coordinates.length; i++) {
			Coordinate coord = coordinates[i];	

			//double z =1;//coord.z>=0?coord.z:0;//XXX hacer que dibujar las coordenadas sea opcional o mover las alturas al model (mas difici)
			//			double z = coord.z>=1?5*(coord.z+1-labor.minElev):1;//XXX hacer que dibujar las coordenadas sea opcional o mover las alturas al model (mas difici)
			//		
			//			if(z<1)z=1;
			Position pos = Position.fromDegrees(coord.y,coord.x,coord.z);
			positions.add(pos);
		}

		//return positions;
	}

	private List<Position> coordinatesToPositions(Coordinate[] coordinates){
		ArrayList<Position> positions = new ArrayList<Position>();     
		for (int i = 0; i < coordinates.length; i++) {
			Coordinate coord = coordinates[i];			
			//double z =1;//coord.z>=0?coord.z:0;//XXX hacer que dibujar las coordenadas sea opcional o mover las alturas al model (mas difici)
			//			double z = coord.z>=1?5*(coord.z+1-labor.minElev):1;//XXX hacer que dibujar las coordenadas sea opcional o mover las alturas al model (mas difici)
			//		
			//			if(z<1)z=1;
			Position pos = Position.fromDegrees(coord.y,coord.x,coord.z);
			positions.add(pos);
		}
		return positions;
	}

	@Deprecated
	protected Path getPathFromGeom(Geometry poly, Integer colorIndex) {			
		Path path = new Path();		
		/**
		 * recorro el vector de puntos que contiene el poligono gis y creo un
		 * path para dibujarlo
		 */
		if(poly.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ poly);
			return null;
		}
		for (int i = 0; i < poly.getNumPoints(); i++) {
			Coordinate coord = poly.getCoordinates()[i];
			// como las coordenadas estan en long/lat las convierto a metros
			// para dibujarlas con menos error.
			double x = coord.x / ProyectionConstants.metersToLong();
			double y = coord.y /ProyectionConstants.metersToLat();
			if (i == 0) {
				path.getElements().add(new MoveTo(x, y)); // primero muevo el
			}
			path.getElements().add(new LineTo(x, y));// dibujo una linea desde
		}

		Paint currentColor = null;
		try{
			currentColor = Clasificador.colors[colorIndex];
		}catch(Exception e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}

		path.setFill(currentColor);
		path.setStrokeWidth(0.05);

		path.getStyleClass().add(currentColor.toString());//esto me permite luego asignar un estilo a todos los objetos con la clase "currentColor.toString()"
		return path;
	}

	/**
	 * metodo usado por las capas de siembra fertilizacion, pulverizacion y suelo para obtener los poligonos
	 * @param dao
	 * @return una lista de Polygon simples
	 */
	protected List<Polygon> getPolygons(FC dao){
		return PolygonValidator.geometryToFlatPolygons(dao.getGeometry());
//		List<Polygon> polygons = new ArrayList<Polygon>();
//		Object geometry = dao.getGeometry();
//		//	System.out.println("obteniendo los poligonos de "+geometry);
//
//		if (geometry instanceof MultiPolygon) {		
//			MultiPolygon mp = (MultiPolygon) geometry;
//			for (int i = 0; i < mp.getNumGeometries(); i++) {
//				Geometry g = mp.getGeometryN(i);
//				if(g instanceof Polygon){//aca fallaba porque la geometrias podia ser multipoligon
//					polygons.add((Polygon) g);
//				}				
//			}
//
//		} else if (geometry instanceof Polygon) {
//			polygons.add((Polygon) geometry);
//		} else if(geometry instanceof Point){ 
//			//si es una capa de puntos lo cambio por una capa de cuadrados de lado 5mts
//			Point p = (Point) geometry;
//			GeometryFactory fact = p.getFactory();
//			Double r = 100*ProyectionConstants.metersToLat();
//
//			Coordinate D = new Coordinate(p.getX() - r , p.getY() + r ); // x-l-d
//			Coordinate C = new Coordinate(p.getX() + r , p.getY()+ r);// X+l-d
//			Coordinate B = new Coordinate(p.getX() + r , p.getY() - r );// X+l+d
//			Coordinate A = new Coordinate(p.getX() - r , p.getY() -r );// X-l+d
//
//			Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
//
//			// PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
//			// fact= new GeometryFactory(pm);
//
//			LinearRing shell = fact.createLinearRing(coordinates);
//			LinearRing[] holes = null;
//			Polygon poly = new Polygon(shell, holes, fact);
//
//			polygons.add(poly);
//			System.out.println("creando polygon default");//Las geometrias son POINT. que hago?
//			//TODO crear un poligono default
//
//		}
//		//System.out.println("devolviendo los polygons "+polygons);
//		return polygons;
	}
	
	//protected abstract gov.nasa.worldwind.render.ExtrudedPolygon getPathTooltip(Geometry p, FC  fc,gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon);

	
	private ExtrudedPolygon getPathTooltip(Geometry p, FC fc, ExtrudedPolygon renderablePolygon) {
		String tooltipText = null;// createTooltipForLaborItem(p, fc);//creo el tooltip al crear el anotation. no antes
		return getExtrudedPolygonFromGeom(p, fc,tooltipText,renderablePolygon);
	}

	public static String createTooltipForLaborItem(Geometry p, LaborItem fc) {
		double area = p.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = "";
		if(fc instanceof CosechaItem) {
			tooltipText = CrearCosechaMapTask.buildTooltipText((CosechaItem)fc, area);			
		} else 	if(fc instanceof SiembraItem) {
			tooltipText = ConvertirASiembraTask.buildTooltipText((SiembraItem)fc, area); 
		} else 	if(fc instanceof FertilizacionItem) {
			tooltipText = CrearFertilizacionMapTask.buildTooltipText((FertilizacionItem)fc, area); 
		} else 	if(fc instanceof PulverizacionItem) {
			tooltipText = CrearPulverizacionMapTask.buildTooltipText((PulverizacionItem)fc, area);
		} else 	if(fc instanceof SueloItem) {
			tooltipText = CrearSueloMapTask.buildTooltipText((SueloItem)fc, area);
		}else 	if(fc instanceof MargenItem) {
			tooltipText = OpenMargenMapTask.buildTooltipText((MargenItem)fc, area);
		}
		return tooltipText;
	} 
	
	protected List<FC> getItemsList(){
		List<FC> cItems = new ArrayList<FC>();
		try {
			FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.labor.outCollection.reader();

			while (reader.hasNext()) {
				SimpleFeature simpleFeature = reader.next();
				FC ci = this.labor.constructFeatureContainerStandar(simpleFeature,false);
				cItems.add(ci);
			}
			reader.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("devolviendo itemList size "+cItems.size());
		return cItems;
	}

	private void updateStatsLabor(Collection<FC> itemsToShow){
		labor.minAmount = Double.MAX_VALUE;
		labor.maxAmount = -Double.MAX_VALUE;

		labor.setCantidadLabor(Double.valueOf(0.0));
		labor.setCantidadInsumo(Double.valueOf(0.0));


		
		itemsToShow.parallelStream().forEach(fc->{
			try {
			Geometry g = fc.getGeometry();

			Double rinde = fc.getAmount();//labor.colAmount.get()

			Double a = GeometryHelper.getHas(g);//.getArea() * ProyectionConstants.A_HAS();

			labor.setCantidadLabor(labor.getCantidadLabor()+a);
			labor.setCantidadInsumo(labor.getCantidadInsumo()+rinde*a);

			labor.minAmount=Math.min(labor.minAmount,fc.getAmount());
			labor.maxAmount=Math.max(labor.maxAmount,fc.getAmount());

			labor.minElev=Math.min(labor.minElev,fc.getElevacion());
			labor.maxElev=Math.max(labor.maxElev,fc.getElevacion());

			// aproximar el envelope con un rectangulo
			//arribaIzq es la coordenada x,y del que tenga max y? 

			if(g instanceof Point){
				Point centroid =(Point) g;
				if(labor.minX==null || labor.minX.getLongitude().degrees>centroid.getX()){
					labor.minX=Position.fromDegrees(centroid.getY(), centroid.getX());
				}
				if(labor.minY==null ||labor.minY.getLatitude().degrees>centroid.getY()){
					labor.minY=Position.fromDegrees(centroid.getY(), centroid.getX());
				}
				if(labor.maxX==null ||labor.maxX.getLongitude().degrees<centroid.getX()){
					labor.maxX=Position.fromDegrees(centroid.getY(), centroid.getX());
				}
				if(labor.maxY==null ||labor.maxY.getLatitude().degrees<centroid.getY()){
					labor.maxY=Position.fromDegrees(centroid.getY(), centroid.getX());
				}
			}else{
				Envelope envelope = g.getEnvelopeInternal();//si la geometria es grande esto falla?

				if(labor.minX==null || labor.minX.getLongitude().degrees>envelope.getMinX()){
					labor.minX=Position.fromDegrees(envelope.centre().y, envelope.getMinX());
				}
				if(labor.minY==null ||labor.minY.getLatitude().degrees>envelope.getMinY()){
					labor.minY=Position.fromDegrees(envelope.getMinY(), envelope.centre().x);
				}
				if(labor.maxX==null ||labor.maxX.getLongitude().degrees<envelope.getMaxX()){
					labor.maxX=Position.fromDegrees(envelope.centre().y, envelope.getMaxX());
				}
				if(labor.maxY==null ||labor.maxY.getLatitude().degrees<envelope.getMaxY()){
					labor.maxY=Position.fromDegrees(envelope.getMaxY(), envelope.centre().x);
				}

			}
			}catch(Exception e ) {
				e.printStackTrace();
				System.err.println("Excepcion en updateStatsLabor");
			}
		});//

		//		Iterator<FC> it = itemsToShow.iterator();
		//		while(it.hasNext()){//int i =0;i<itemsToShow.size();i++){
		//			FC fc=	it.next();
		//			Geometry g = fc.getGeometry();
		//			
		//			Double rinde = fc.getAmount();//labor.colAmount.get()
		//			
		//			Double a = g.getArea() * ProyectionConstants.A_HAS();
		//			area+=a;
		//			cantidad+=rinde*a;
		//			
		//			//geomArray[i]=g;
		//			min=Math.min(min,fc.getAmount());
		//			max=Math.max(max,fc.getAmount());
		//
		//			labor.minElev=Math.min(labor.minElev,fc.getElevacion());
		//			labor.maxElev=Math.max(labor.maxElev,fc.getElevacion());
		//
		//			// aproximar el envelope con un rectangulo
		//			//arribaIzq es la coordenada x,y del que tenga max y? 
		//		
		//			if(g instanceof Point){
		//				Point centroid =(Point) g;
		//				if(labor.minX==null || labor.minX.getLongitude().degrees>centroid.getX()){
		//					labor.minX=Position.fromDegrees(centroid.getY(), centroid.getX());
		//				}
		//				if(labor.minY==null ||labor.minY.getLatitude().degrees>centroid.getY()){
		//					labor.minY=Position.fromDegrees(centroid.getY(), centroid.getX());
		//				}
		//				if(labor.maxX==null ||labor.maxX.getLongitude().degrees<centroid.getX()){
		//					labor.maxX=Position.fromDegrees(centroid.getY(), centroid.getX());
		//				}
		//				if(labor.maxY==null ||labor.maxY.getLatitude().degrees<centroid.getY()){
		//					labor.maxY=Position.fromDegrees(centroid.getY(), centroid.getX());
		//				}
		//			}else{
		//				Envelope envelope = g.getEnvelopeInternal();//si la geometria es grande esto falla?
		//				
		//				if(labor.minX==null || labor.minX.getLongitude().degrees>envelope.getMinX()){
		//					labor.minX=Position.fromDegrees(envelope.centre().y, envelope.getMinX());
		//				}
		//				if(labor.minY==null ||labor.minY.getLatitude().degrees>envelope.getMinY()){
		//					labor.minY=Position.fromDegrees(envelope.getMinY(), envelope.centre().x);
		//				}
		//				if(labor.maxX==null ||labor.maxX.getLongitude().degrees<envelope.getMaxX()){
		//					labor.maxX=Position.fromDegrees(envelope.centre().y, envelope.getMaxX());
		//				}
		//				if(labor.maxY==null ||labor.maxY.getLatitude().degrees<envelope.getMaxY()){
		//					labor.maxY=Position.fromDegrees(envelope.getMaxY(), envelope.centre().x);
		//				}
		//				
		//			}
		//			
		//			//System.out.println("actualizando las estadisticas con "+centroid);
		//
		//	
		//
		//			//	labor.minX = Math.min(labor.minX, envelopeInternal.getMinX());
		//			//	labor.minY = Math.min(labor.minY, envelopeInternal.getMinY());
		//			//	labor.maxX = Math.max(labor.maxX, envelopeInternal.getMaxX());
		//			//	labor.maxY = Math.max(labor.maxY, envelopeInternal.getMaxY());
		//		}

		System.out.println("(maxElev, minElev)= ("+labor.maxElev+" , "+labor.minElev+")");
		System.out.println("(min, max) = ("+labor.minAmount+" , "+labor.maxAmount+")");//(min,max) = (203.0 , 203.0)
	}

	protected void runLater(Collection<FC> itemsToShow) {	
		//labor.setContorno(null);
		updateStatsLabor(itemsToShow);
		RenderableLayer extrudedPolygonsLayer = createExtrudedPolygonsLayer(itemsToShow);//XXX ojo! si son muchos esto me puede tomar toda la memoria.	

		//Configuracion config = Configuracion.getInstance();
		int lowRes= TARGET_LOW_RES_TIME;//Integer.parseInt(config.getPropertyOrDefault(FAST_LAYER_PROCESS_TIME, Integer.toString(TARGET_LOW_RES_TIME)));
		//lowRes = 1000000;
		long start = System.currentTimeMillis();
		//		System.out.println("creando analyticSurface lowRes");
		RenderableLayer analyticSurfaceLayer = createAnalyticSurfaceFromQuery(lowRes);//21ms
		long end = System.currentTimeMillis();
		long actualTime= end-start;
		//		System.out.println("lowRes Rendering Time = "+actualTime);

		if(actualTime > 0) {
			lowRes=new Long(TARGET_LOW_RES_TIME*TARGET_LOW_RES_TIME/actualTime).intValue();
		}

		analyticSurfaceLayer.setPickEnabled(false);//ya es false de fabrica

		labor.getLayer().removeAllRenderables();
		labor.getLayer().setAnalyticSurfaceLayer(analyticSurfaceLayer);
		labor.getLayer().setExtrudedPolygonsLayer(extrudedPolygonsLayer);
		labor.getLayer().setElementsCount(itemsToShow.size());

		//System.out.println("low res rendering milis: "+lowRes);

		//int medRes=5*lowRes;
		//	System.out.println("mid res rendering milis: "+medRes);
		int highRes=Math.min(10*lowRes,30000);
		System.out.println("lowRes= "+lowRes);
		System.out.println("highRes= "+highRes);
		installPlaceMark();
		//

		if( highRes > TARGET_LOW_RES_TIME*2 && highRes < 60000) {//solo si es menor a un minuto
			CompletableFuture.runAsync(() -> {
				//				System.out.println("corriendo analyticSurfaceLayerMD");
				//				RenderableLayer analyticSurfaceLayerMD = createAnalyticSurfaceFromQuery(medRes);//10
				//				analyticSurfaceLayerMD.setPickEnabled(false);//ya es false de fabrica
				//				labor.getLayer().setAnalyticSurfaceLayer(analyticSurfaceLayerMD);
				//				System.out.println("termine analyticSurfaceLayerMD");
				//			}).handle((r,e) -> {
				//				if (e != null) e.printStackTrace();		
				//				return CompletableFuture.runAsync(()->{});
				//			}).thenRun(
				//					()->{
//				if(labor.getContorno()==null) {
//					extractContorno();//FIXME consume mucha memoria si son muchos puntos
//				}
				System.out.println("corriendo analyticSurfaceLayerHD");
				RenderableLayer analyticSurfaceLayerHD = createAnalyticSurfaceFromQuery(highRes);//30
				analyticSurfaceLayerHD.setPickEnabled(false);//ya es false de fabrica
				labor.getLayer().setAnalyticSurfaceLayer(analyticSurfaceLayerHD);
				System.out.println("termine analyticSurfaceLayerHD");
			}).handle((r,e) -> {
				if (e != null) e.printStackTrace();		
				return null;
			});
		}// else {
		//			System.out.println("no corro analyticSurfaceLayerHD");
		//		}
	}

//	public void extractContorno() {
//		GeometryHelper.extractContorno(labor);
//	}



	private RenderableLayer createExtrudedPolygonsLayer(Collection<FC> itemsToShow) {	

		double min = labor.minAmount;//Double.MAX_VALUE;
		double max = labor.maxAmount;//-Double.MAX_VALUE;

		Color colorMin = labor.getClasificador().getColorFor(min);// Clasificador.colors[0];
		Color colorMax =labor.getClasificador().getColorFor(max);// Clasificador.colors[Clasificador.colors.length-1];

		double HUE_MIN = colorMin.getHue()/ 360d; //0d / 360d;
		double HUE_MAX = colorMax.getHue()/ 360d;//240d / 360d;

		NumberFormat legendLabelFormat=Messages.getNumberFormat();
		final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(min, max,
				HUE_MIN, HUE_MAX,
				AnalyticSurfaceLegend.createDefaultColorGradientLabels(min, max, legendLabelFormat),
				AnalyticSurfaceLegend.createDefaultTitle(labor.getNombre()));
		legend.setOpacity(0.6);
		legend.setScreenLocation(new java.awt.Point(100, 400));

		ReferencedEnvelope bounds = labor.outCollection.getBounds();//null pointer
		Sector sector =  Sector.fromDegrees(bounds.getMinY(), bounds.getMaxY(),bounds.getMinX() ,bounds.getMaxX());
		//		Renderable analiticLegendrenderable =  new Renderable(){
		//			public void render(DrawContext dc){
		//				//FIXME 2017-01-02T18:30:35.649-0300  SEVERE  Exception while picking Renderable
		//				// 2017-01-02T18:30:35.828-0300  SEVERE  Exception while rendering Renderable
		//				// java.util.ConcurrentModificationException
		//				Extent extent =  Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), sector );
		//				if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
		//					return;
		//				if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300)
		//					return;
		//				legend.render(dc);
		//			}
		//		};

		//System.out.println("antes de crear el layer "+(System.currentTimeMillis()-time)); 
		//labor.getLayer().addRenderable(analiticLegendrenderable);
		RenderableLayer layer = new RenderableLayer(){
			private long id = -1;
			private long lastStateRendered=-1;
			private Sector s = null;
			private boolean finished = false;
			private Envelope env=null;
			private List<Renderable> renderablesPool=new ArrayList<Renderable>();
			private boolean created =false;

			private ExtrudedPolygon getFreeRenderable() {
				gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon =null;
				if(renderablesPool==null)renderablesPool=new ArrayList<Renderable>();
				if(this.renderablesPool.size()>0) {//java.lang.NullPointerException
					renderablePolygon=(ExtrudedPolygon) renderablesPool.get(0);
					renderablesPool.remove(0);
				}else {
					renderablePolygon = new ReusableExtrudedPolygon();
				}
				return renderablePolygon;
			}

			public void render(DrawContext dc){//no se ejecuta hasta que se muestra el layer. como corresponde
				Extent extent =  Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), sector );
				if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
					return;
				if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300)
					return;

				long init = System.currentTimeMillis();
				long stateID = dc.getView().getViewStateID();

				if(created) {
					//TODO continuar dibujando los rendereables
					//System.out.println("rendering same scene");
					
				} else {
					//toBeRendered.clear();
					Sector visibleSector = dc.getVisibleSector();
					if(s==null || !s.equals(visibleSector)){
						s=visibleSector;
						
						this.renderablesPool.addAll(renderables);	
						renderables.clear();
						

						double maxX = visibleSector.getMaxLongitude().degrees;
						double minX = visibleSector.getMinLongitude().degrees;
						double maxY = visibleSector.getMaxLatitude().degrees;
						double minY = visibleSector.getMinLatitude().degrees;
						double dX = 0; //(maxX - minX)/200;
						double dY = 0;//(maxY - minY)/200;
						if(env == null) {
							env = new Envelope(minX+dX,maxX-dX,minY+dY,maxY-dY);
						}else {
							env.init(minX+dX,maxX-dX,minY+dY,maxY-dY);
						}				

						List<FC> features = labor.cachedOutStoreQuery(env);//java.util.ConcurrentModificationException
						char[] abc = "ABCDEFGHIJKLM".toCharArray();
						int size = labor.getClasificador().getNumClasses()-1;

						features.stream().forEach((c)->{
							Geometry g = c.getGeometry();
							if(g instanceof Point){
								Point center =(Point)g;
								try{
									Position pointPosition = Position.fromDegrees(center.getY(),center.getX());

									PointPlacemark pmStandard = new PointPlacemark(pointPosition);
									pmStandard.setLabelText(Messages.getString("ProcessMapTask.categoria")+": "+abc[size-c.getCategoria()]);//

									PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();								
									pointAttribute.setImageColor(getAwtColor(c));
									pmStandard.setAttributes(pointAttribute);

									if(pmStandard!=null)this.addRenderable(pmStandard);//extPoly.render(dc);

								}catch(Exception e){
									System.out.println("error al tratar de contruir un poligono desde un punto");
									e.printStackTrace();
								}
							} else if(g instanceof Polygon){

								gov.nasa.worldwind.render.ExtrudedPolygon extPoly=	getPathTooltip((Polygon)g,c,this.getFreeRenderable());
								//	System.out.println("dibujando "+extPoly);
								if(extPoly!=null)this.addRenderable(extPoly);//extPoly.render(dc);

							} else if(g instanceof MultiPolygon){
								MultiPolygon mp = (MultiPolygon)g;			
								for(int i=0;i<mp.getNumGeometries();i++){
									Polygon p = (Polygon) (mp).getGeometryN(i);

									gov.nasa.worldwind.render.ExtrudedPolygon extPoly=	getPathTooltip((Polygon)p,c,this.getFreeRenderable());

									if(extPoly!=null)this.addRenderable(extPoly);

								}
							}
						});//for each feature create renderable
						//toBeRendered.addAll(this.renderables);

					} 
					created=true;
				}
				renderables.stream().forEach(r->{
					if(System.currentTimeMillis()-init<500
							||(!finished && id==stateID)
							) {
						//System.out.println("rendering "+r);
						r.render(dc);
					} else {
						this.finished=false;
						//break;
					}
					finished=true;
				
				});
				//System.out.println(toBeRendered.size()+" items to be rendered"); 
				//for(Renderable r:renderables) {
				
//					if(System.currentTimeMillis()-init<500
//							||(!finished && id==stateID)
//							) {
//						//System.out.println("rendering "+r);
//						r.render(dc);
//					} else {
//						this.finished=false;
//						break;
//					}
//					finished=true;
//				}
				id=stateID;
				//	System.out.println("tarde "+(System.currentTimeMillis()-init)+" milis en renderear los "+ dp.get()+" rendereables");
			}

			public java.awt.Color getAwtColor(FC c) {
				return labor.getClasificador().getAwtColorForCategoria(c.getCategoria());
				//				Color color = labor.getClasificador().getColorForCategoria(c.getCategoria());
				//				int red = Double.parseDouble(color.getRed()*255).intValue();
				//				int green = Double.parseDouble(color.getGreen()*255).intValue();
				//				int blue = Double.parseDouble(color.getBlue()*255).intValue();
				//				java.awt.Color awtColor =new java.awt.Color(red,green,blue);
				//				return awtColor;
			}
			@Override
			public void dispose() {
				System.out.println("disposing of extrudedPoligonsLayer");
				env=null;
				renderables.stream().forEach(r->{
					if(r instanceof ReusableExtrudedPolygon) {
						
//					((ReusableExtrudedPolygon)r).setAttributes(null);
//					((ReusableExtrudedPolygon)r).setSideAttributes(null);
					((ReusableExtrudedPolygon)r).clearBoundarys();
					((ReusableExtrudedPolygon)r).clearList();
					}
				});
				renderablesPool.stream().forEach(r->{
					if(r instanceof ReusableExtrudedPolygon) {
					((ReusableExtrudedPolygon)r).clearBoundarys();
					((ReusableExtrudedPolygon)r).clearList();
					}
				});
				renderablesPool.clear();
				renderablesPool=null;
				this.renderables.clear();
				super.dispose();
			}
			};

			

			//layer.addRenderables(labor.getLayer().getRenderables());
			//	layer.addRenderable(analiticLegendrenderable);		
			layer.setPickEnabled(false);
			return layer;
	}

	private void installPlaceMark() {
		if(labor.outCollection!=null){
			Position pointPosition = Position.fromDegrees(
					labor.minY.getLatitude().getDegrees(),
					labor.minY.getLongitude().getDegrees());
			PointPlacemark pmStandard = new PointPlacemark(pointPosition){
				public void render(DrawContext dc){
					double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
					if (  eyeElevation > 1000 ){
						super.render(dc);
					}
				}
			};
			PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
			pointAttribute.setImageColor(java.awt.Color.red);
			//		if(HiDPIHelper.isHiDPI()){
			//			pointAttribute.setLabelFont(java.awt.Font.decode("Verdana-Bold-50"));
			//		}

			pointAttribute.setLabelMaterial(Material.DARK_GRAY);
			pmStandard.setLabelText(labor.getNombre());
			pmStandard.setAttributes(pointAttribute);
			labor.getLayer().addRenderable(pmStandard);

			Coordinate centre = labor.outCollection.getBounds().centre();
			Position centerPosition = Position.fromDegrees(
					centre.y,centre.x);
			labor.getLayer().setValue(ZOOM_TO_KEY, centerPosition);		
		}
	}

	/**
	 * 
	 * @param c geometria a ser elevada hasta la altura de query
	 * @param query geometria de la que se toma la altura para elevar a c
	 * @return la geometria c elevada a la altura de query
	 */
	public static Geometry flatenGeometry(Geometry c, Geometry query) {
		Geometry g = (Geometry)c.clone();
		Coordinate cero = query.getCoordinates()[0];
		g.apply( new CoordinateFilter() {
			@Override
			public void filter(Coordinate c) {
				c.z=cero.z;					
			}});
		return g;
	}

	/**
	 * @Description Metodo recomendado para unir varios poligonos rapido
	 */
	public Geometry getUnion(GeometryFactory fact, List<? extends LaborItem> objects, Geometry query) {

		//		System.out.println("getUnion(); "+System.currentTimeMillis());
		if (objects == null || objects.size() < 1) {
			return null;
		} else if (objects.size() == 1) {
			Geometry ob1 =(Geometry) objects.get(0).getGeometry();
			return flatenGeometry(ob1,query);// (Geometry) crsTransform( ob1);
		} else {// hay mas de un objeto para unir
			//System.out.println( "tratando de unir "+ objects.size()+" geometrias");
			ArrayList<Geometry> geomList = new ArrayList<Geometry>();
			Point zero = fact.createPoint(new Coordinate (0,0));
			/*
			 *recorro todas las cosechas y si su geometria interna se cruza con la query la agrego a la lista de geometrias 
			 */

			int maxGeometries = 	labor.getConfigLabor().getMAXGeometries();//labor.getConfiguracion().getMAXGeometries();
			for (LaborItem o : objects) {			
				Geometry g= o.getGeometry();
				Geometry flatG =flatenGeometry(g,query);
				try{
					if (flatG.intersects(query)) {//acelera mucho el proceso //g.getEnvelopeInternal().intersects(query) 
						boolean contains = flatG.touches(zero);
						if(!contains
								&&geomList.size()<maxGeometries
								&& flatG.isValid()
								){ 
							flatG = makeGood(flatG);
							geomList.add(flatG);
						} else {
							flatG = makeGood(flatG);
							geomList.add(flatG);
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}

			if (geomList == null || geomList.size() < 1) {
				return null;
			}

			Geometry union = null;

			Geometry[] geomArray = geomList.toArray(new Geometry[geomList.size()]);

			GeometryCollection polygonCollection = fact
					.createGeometryCollection(geomArray);

			Long antes = System.currentTimeMillis();
			//System.out.println("antes de hacer buffer(0) "+antes);
			/*uno las geometrias para sacar la interseccion. deberia funcionar bien con pocas geometrias*/
			try {
				union = polygonCollection.union();//buffer(0); 
				union = makeGood(union);

				Long despues = System.currentTimeMillis();
				Long demora = despues - antes;
				if(demora > 1000){
					System.out.println("tardo mas de 1 segundos en unir "+ polygonCollection.getNumGeometries());
					System.out.println("tarde "+demora/1000+"s en hacer buffer(0)");
					System.err.println("probable error de la configuracion de metros por unidad de distancia, probar con 0.0254 para pulgadas. terminando el proceso");
					//labor.config.valorMetrosPorUnidadDistanciaProperty().set(0.0254);
					//super.cancel();
					//					 tardo mas de 10 segundos en unir 1390
					//					 despues de hacer buffer(0) 110850
				}
				// System.out.println("tarde "+demora+" en hacer buffer(0)");


			} catch (Exception e) {
				union= 	EnhancedPrecisionOp.buffer(polygonCollection, 0);
				//e.printStackTrace();
				/*java.lang.IllegalArgumentException: Ring has fewer than 3 points, so orientation cannot be determined*/
				//	union = null;
			}

			return union;
		}


	}

	public Geometry makeGood(Geometry g) {

		//		if(g instanceof Polygon){
		//			Polygon poly = (Polygon)g;
		//			if(poly.getNumPoints()>3){
		//				g = JTSUtilities.makeGoodShapePolygon(poly);
		//
		//			} else {
		//				//System.out.println("ring has fewer than 3 points");
		//				//g = null;
		//			}
		//
		//		} else if(g instanceof MultiPolygon){
		//			g = JTSUtilities.makeGoodShapeMultiPolygon((MultiPolygon)g);
		//		}

		//		if(g instanceof Polygon){
		//			g = JTSUtilities.makeGoodShapePolygon((Polygon)g);
		//		} else if(g instanceof MultiPolygon){
		//			g = JTSUtilities.makeGoodShapeMultiPolygon((MultiPolygon)g);
		//		}
		return PolygonValidator.validate(g);//g;
	}


//	public void uninstallProgressBar() {		
//		progressPane.getChildren().remove(progressContainer);
//	}
//
//	//	public void start() {
//	//		Platform.runLater(this);
//	//	}
//
//	public void installProgressBar(Pane progressBox) {
//		this.progressPane= progressBox;
//		progressBarTask = new ProgressBar();			
//		progressBarTask.setProgress(0);
//
//		progressBarTask.progressProperty().bind(this.progressProperty());
//		progressBarLabel = new Label(labor.getNombre());
//		progressBarLabel.setTextFill(Color.BLACK);
//
//
//		Button cancel = new Button();
//		cancel.setOnAction(ae->{
//			System.out.println("cancelando el ProcessMapTask");
//			this.cancel();
//			this.uninstallProgressBar();
//		});
//		Image imageDecline = new Image(getClass().getResourceAsStream(TASK_CLOSE_ICON));
//		cancel.setGraphic(new ImageView(imageDecline));
//
//		//progressBarLabel.setStyle("-fx-color: black");
//		progressContainer = new HBox();
//		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
//		progressBox.getChildren().add(progressContainer);
//
//
//	}


}

