package tasks;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.media.opengl.GL2;

import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.jogamp.common.nio.Buffers;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Clasificador;
import dao.Labor;
import dao.LaborItem;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolygons;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.SurfaceTileDrawContext;
import gov.nasa.worldwind.util.VecBuffer;
import gov.nasa.worldwind.util.VecBufferSequence;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import javafx.application.Platform;
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
import utils.PolygonValidator;
import utils.ProyectionConstants;
//import org.opengis.filter.FilterFactory2;

public abstract class ProcessMapTask<FC extends LaborItem,E extends Labor<FC>> extends Task<E>{
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";
	protected int featureCount=0;
	protected int featureNumber=0;
	protected E labor;

	protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();

	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	public ProcessMapTask() {
	}

	public ProcessMapTask(E labor2) {
		labor=labor2;
	}

	@Override
	protected E call() throws Exception {

		try {
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
	protected gov.nasa.worldwind.render.ExtrudedPolygon  getExtrudedPolygonFromGeom(Geometry poly, FC dao,String tooltipText) {	
		gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon = new gov.nasa.worldwind.render.ExtrudedPolygon ();
		// Set the coordinates (in degrees) to draw your polygon
		// To radians just change the method the class Position
		// to fromRadians().

		Color currentColor = null;
		try{
			currentColor = labor.getClasificador().getColorFor(dao);
		}catch(Exception e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}
		java.awt.Color awtColor = new java.awt.Color((float) currentColor.getRed(),(float) currentColor.getGreen(),(float) currentColor.getBlue());
		Material material = new Material(awtColor);

		ShapeAttributes outerAttributes = new BasicShapeAttributes();

		outerAttributes.setOutlineWidth(0.01);
		outerAttributes.setOutlineOpacity(0.01);
		outerAttributes.setDrawInterior(true);
		outerAttributes.setDrawOutline(false);
		outerAttributes.setInteriorOpacity(1);
		outerAttributes.setInteriorMaterial(material);
		//normalAttributes.setOutlineMaterial(material);

		ShapeAttributes sideAttributes = new BasicShapeAttributes();
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
				List<Position> exteriorPositions = coordinatesToPositions(coordinates);

				
				renderablePolygon.setOuterBoundary(exteriorPositions);
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
		getExtrudedPolygonFromGeom(inputGeom, dao, tooltipText);
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
				float elev=0;
				double amount=0;
				for(FC it : indexItems){
					elev+= it.getElevacion()-minElev;
					amount+=it.getAmount();
				}

				int n = indexItems.size();
				Color color = labor.getClasificador().getColorFor(amount/n);
				float 	r=(float) color.getRed();//0.99607843
				float g=(float) color.getGreen();
				float b=(float) color.getBlue();
				java.awt.Color rgbaColor = new java.awt.Color(r,g,b,1);//IllegalArgumentException - if r, g b or a are outside of the range 0.0 to 1.0, inclusive
				newGridPoint  =  AnalyticSurface.createGridPointAttributes(elev/n, rgbaColor);
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
		DecimalFormat legendLabelFormat = new DecimalFormat("0.00");
		Color colorMin = labor.getClasificador().getColorFor(labor.minAmount);// Clasificador.colors[0];
		Color colorMax =labor.getClasificador().getColorFor(labor.maxAmount);// Clasificador.colors[Clasificador.colors.length-1];
		double HUE_MIN = colorMin.getHue()/360d;//0d / 360d;
		double HUE_MAX = colorMax.getHue()/360d;//240d / 360d;

		final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(
				labor.minAmount,labor.maxAmount,
				HUE_MIN, HUE_MAX,
				AnalyticSurfaceLegend.createDefaultColorGradientLabels(labor.minAmount, labor.maxAmount, legendLabelFormat),
				AnalyticSurfaceLegend.createDefaultTitle(labor.getNombreProperty().get()));
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
	 * @return
	 */
	protected List<Polygon> getPolygons(FC dao){
		List<Polygon> polygons = new ArrayList<Polygon>();
		Object geometry = dao.getGeometry();
		//	System.out.println("obteniendo los poligonos de "+geometry);

		if (geometry instanceof MultiPolygon) {		
			MultiPolygon mp = (MultiPolygon) geometry;
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry g = mp.getGeometryN(i);
				if(g instanceof Polygon){
					polygons.add((Polygon) g);
				}				
			}

		} else if (geometry instanceof Polygon) {
			polygons.add((Polygon) geometry);
		} else if(geometry instanceof Point){ 
			//si es una capa de puntos lo cambio por una capa de cuadrados de lado 5mts
			Point p = (Point) geometry;
			GeometryFactory fact = p.getFactory();
			Double r = 5*ProyectionConstants.metersToLat();

			Coordinate D = new Coordinate(p.getX() - r , p.getY() + r ); // x-l-d
			Coordinate C = new Coordinate(p.getX() + r , p.getY()+ r);// X+l-d
			Coordinate B = new Coordinate(p.getX() + r , p.getY() - r );// X+l+d
			Coordinate A = new Coordinate(p.getX() - r , p.getY() -r );// X-l+d

			Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.

			// PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
			// fact= new GeometryFactory(pm);

			LinearRing shell = fact.createLinearRing(coordinates);
			LinearRing[] holes = null;
			Polygon poly = new Polygon(shell, holes, fact);

			polygons.add(poly);
			System.out.println("creando polygon default");//Las geometrias son POINT. que hago?
			//TODO crear un poligono default

		}
		//System.out.println("devolviendo los polygons "+polygons);
		return polygons;
	}
	protected abstract gov.nasa.worldwind.render.ExtrudedPolygon getPathTooltip(Geometry p, FC  fc);


	private void updateStatsLabor(Collection<FC> itemsToShow){
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		Iterator<FC> it = itemsToShow.iterator();
		while(it.hasNext()){//int i =0;i<itemsToShow.size();i++){
			FC fc=	it.next();
			Geometry g = fc.getGeometry();
			//geomArray[i]=g;
			min=Math.min(min,fc.getAmount());
			max=Math.max(max,fc.getAmount());

			labor.minElev=Math.min(labor.minElev,fc.getElevacion());
			labor.maxElev=Math.max(labor.maxElev,fc.getElevacion());

			//TODO aproximar el envelope con un rectangulo
			//arribaIzq es la coordenada x,y del que tenga max y? 

			Point centroid = g.getCentroid();//si la geometria es grande esto falla?


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

			//	labor.minX = Math.min(labor.minX, envelopeInternal.getMinX());
			//	labor.minY = Math.min(labor.minY, envelopeInternal.getMinY());
			//	labor.maxX = Math.max(labor.maxX, envelopeInternal.getMaxX());
			//	labor.maxY = Math.max(labor.maxY, envelopeInternal.getMaxY());
		}


		labor.minAmount=min;
		labor.maxAmount=max;
		System.out.println("(min,max) = ("+min+" , "+max+")");
	}

	protected void runLater(Collection<FC> itemsToShow) {	
		//System.out.println("runlater "+itemsToShow.size());
		updateStatsLabor(itemsToShow);

		labor.getLayer().removeAllRenderables();//tiene que estar antes de runLaterInternall para que no se propagen los poligonos viejos
		//TODO crear un renderable layer que dibuje el layer de los poligonos si esta suficientemente cerca
		//o el analyticSurface si esta lejos
		//if(itemsToShow.size()<10000){
		RenderableLayer extrudedPolygonsLayer = createExtrudedPolygonsLayer(itemsToShow);//XXX ojo! si son muchos esto me puede tomar toda la memoria.
		//}else{
		RenderableLayer analyticSurfaceLayer = createAnalyticSurface(itemsToShow);
		//}

		analyticSurfaceLayer.setPickEnabled(false);//ya es false de fabrica

		//		RenderableLayer altitudeDependingLayer =  new RenderableLayer(){
		//			public void render(DrawContext dc){
		//				if(!this.isEnabled())return;
		//			//	Extent extent =  Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), sector );
		//				//if (extent.intersects(dc.getView().getCurrentEyePosition().elevation.getFrustumInModelCoordinates()))
		//				//	return;
		//
		//				int screenPixelsSectorMinSize=500;
		//				double eyeElevation = dc.getView().getCurrentEyePosition().elevation;
		//				if ( itemsToShow.size()<10000 || eyeElevation < screenPixelsSectorMinSize ){
		//					//System.out.println("dibujando para screenSize = " +sectorPixelSizeInWindow);
		//					extrudedPolygonsLayer.setEnabled(true);
		//					analyticSurfaceLayer.setEnabled(false);
		//					extrudedPolygonsLayer.render(dc);
		//					
		//				}else{
		//					analyticSurfaceLayer.setEnabled(true);
		//					//extrudedPolygonsLayer.setEnabled(false);					
		//					analyticSurfaceLayer.render(dc);					
		//				}				
		//			}
		//			
		//			@Override
		//			public void pick(DrawContext dc, java.awt.Point point) {//pick(DrawContext dc, Point p){
		//				extrudedPolygonsLayer.pick(dc, point);
		//			}
		//		};

		labor.getLayer().removeAllRenderables();
		labor.getLayer().setAnalyticSurfaceLayer(analyticSurfaceLayer);
		labor.getLayer().setExtrudedPolygonsLayer(extrudedPolygonsLayer);
		labor.getLayer().setElementsCount(itemsToShow.size());
		//	labor.getLayer().dispose();
		//labor.setLayer(altitudeDependingLayer);//esto funciona bien pero no se actualiza al editar
		//labor.getLayer().addRenderable(altitudeDependingLayer);
		//labor.getLayer().setPickEnabled(false);//si hago esto no puedo pasar el pick a extrudedPolygonsLayers

		installPlaceMark();
	}

	private RenderableLayer createExtrudedPolygonsLayer(Collection<FC> itemsToShow) {	
		//this.pathTooltips.clear();
		//labor.getLayer().removeAllRenderables();
		//labor.getLayer().setPickEnabled(false);//evita que se muestre el tooltip
		double min = labor.minAmount;//Double.MAX_VALUE;
		double max = labor.maxAmount;//-Double.MAX_VALUE;

		//		double min=0;
		//		double max=10000;

		Color colorMin = labor.getClasificador().getColorFor(min);// Clasificador.colors[0];
		Color colorMax =labor.getClasificador().getColorFor(max);// Clasificador.colors[Clasificador.colors.length-1];


//		int workDone = 0;
//		for(FC c:itemsToShow){
//			this.updateProgress(workDone, itemsToShow.size());
//			workDone++;
//			//			double amount = c.getAmount();
//			//			if( min>amount){
//			//				min=amount;
//			//				colorMin = labor.getClasificador().getColorFor(c);
//			//			}
//			//			if( max<amount){
//			//				max=amount;
//			//				colorMax = labor.getClasificador().getColorFor(c);
//			//			}
//
//			Geometry g = c.getGeometry();
//			if(g instanceof Polygon){
//				//	pathTooltips.add(
//				getPathTooltip((Polygon)g,c);
//				//		);	
//			} else if(g instanceof MultiPolygon){
//				MultiPolygon mp = (MultiPolygon)g;			
//				for(int i=0;i<mp.getNumGeometries();i++){
//					Polygon p = (Polygon) (mp).getGeometryN(i);
//					getPathTooltip(p,c);//se construye el poligono y se agrega al layer
//					//	pathTooltips.add(getPathTooltip(p,c));	
//				}
//
//			}
//		}

		double HUE_MIN = colorMin.getHue()/ 360d; //0d / 360d;
		double HUE_MAX = colorMax.getHue()/ 360d;//240d / 360d;


		//Format legendLabelFormat = new DecimalFormat() ;
		DecimalFormat legendLabelFormat = new DecimalFormat("0.00");
		final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(min, max,
				HUE_MIN, HUE_MAX,
				AnalyticSurfaceLegend.createDefaultColorGradientLabels(min, max, legendLabelFormat),
				AnalyticSurfaceLegend.createDefaultTitle(labor.getNombreProperty().get()));
		legend.setOpacity(0.6);
		legend.setScreenLocation(new java.awt.Point(100, 400));

		ReferencedEnvelope bounds = labor.outCollection.getBounds();//null pointer
		Sector sector =  Sector.fromDegrees(bounds.getMinY(), bounds.getMaxY(),bounds.getMinX() ,bounds.getMaxX());
		Renderable analiticLegendrenderable =  new Renderable(){
			public void render(DrawContext dc){
				//FIXME 2017-01-02T18:30:35.649-0300  SEVERE  Exception while picking Renderable
				// 2017-01-02T18:30:35.828-0300  SEVERE  Exception while rendering Renderable
				// java.util.ConcurrentModificationException
				Extent extent =  Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), sector );
				if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
					return;
				if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300)
					return;
				legend.render(dc);
			}
		};

		//labor.getLayer().addRenderable(analiticLegendrenderable);
		RenderableLayer layer = new RenderableLayer(){
			private Sector s = null;
			public void render(DrawContext dc){

//				Position p1 = dc.getView().computePositionFromScreenPoint(0, 0);
//				Position p2 = dc.getView().computePositionFromScreenPoint(-1920, -1080);
				Sector visibleSector = dc.getVisibleSector();
				if(s==null || !s.equals(visibleSector)){
					s=visibleSector;
				this.clearRenderables();
				//	System.out.println("dibujando entre p1= "+p1+" y p2="+p2);
				Envelope env = new Envelope(visibleSector.getMinLongitude().degrees,visibleSector.getMinLatitude().degrees,visibleSector.getMaxLongitude().degrees,visibleSector.getMaxLatitude().degrees);//p1.getLongitude().degrees, p1.getLatitude().degrees,p2.getLongitude().degrees, p2.getLatitude().degrees);

				List<FC> features = labor.cachedOutStoreQuery(env);
				//System.out.println("los features en vista son "+features.size());
				features.stream().forEach((c)->{
					Geometry g = c.getGeometry();
					if(g instanceof Polygon){

						gov.nasa.worldwind.render.ExtrudedPolygon extPoly=	getPathTooltip((Polygon)g,c);
					//	System.out.println("dibujando "+extPoly);
						if(extPoly!=null)this.addRenderable(extPoly);//extPoly.render(dc);

					} else if(g instanceof MultiPolygon){
						MultiPolygon mp = (MultiPolygon)g;			
						for(int i=0;i<mp.getNumGeometries();i++){
							Polygon p = (Polygon) (mp).getGeometryN(i);

							gov.nasa.worldwind.render.ExtrudedPolygon extPoly=	getPathTooltip(p,c);
					//		System.out.println("dibujando "+extPoly);
							if(extPoly!=null)this.addRenderable(extPoly);

						}
					}
				});//for each feature create renderable
				
				}
				analiticLegendrenderable.render(dc);
				this.getRenderables().forEach((r)->r.render(dc));
				}};
	
			
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
			pmStandard.setLabelText(labor.nombreProperty.get());
			pmStandard.setAttributes(pointAttribute);
			labor.getLayer().addRenderable(pmStandard);
			labor.getLayer().setValue(ZOOM_TO_KEY, pointPosition);		
		}
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

			return ob1;// (Geometry) crsTransform( ob1);
		} else {// hay mas de un objeto para unir
			//System.out.println( "tratando de unir "+ objects.size()+" geometrias");
			ArrayList<Geometry> geomList = new ArrayList<Geometry>();
			Point zero = fact.createPoint(new Coordinate (0,0));
			/*
			 *recorro todas las cosechas y si su geometria interna se cruza con la query la agrego a la lista de geometrias 
			 */


			int maxGeometries = 	labor.getConfigLabor().getMAXGeometries();//labor.getConfiguracion().getMAXGeometries();
			//	Envelope queryEnvelope = query.getEnvelopeInternal();		
			for (LaborItem o : objects) {			
				Geometry g =o.getGeometry();
				//Geometry g =crsTransform( o.getGeometry());
				try{
					if (g.intersects(query)) {//acelera mucho el proceso //g.getEnvelopeInternal().intersects(query) 

						boolean contains = g.touches(zero);
						if(!contains
								&&geomList.size()<maxGeometries
								&& g.isValid()
								){//limito la cantidad de geometrias a unir arbitrariamente por una cuestion de performance 100 parece ser un techo 
							g = makeGood(g);
							geomList.add(g);
							//System.out.println("agregue "+g+" a la lista para unir");
						} else {
							//System.out.println("not valid geometry "+g);
							g = makeGood(g);

							//System.out.println(	g.isValid()+" valid geometry "+g);
							geomList.add(g);

						}
						//						else{
						//							System.out.println("contains zero o hay mas de 100 geometrias en superposicion");
						//						}
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


	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}

	public void start() {
		Platform.runLater(this);
	}

	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label(labor.nombreProperty.get());
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(TASK_CLOSE_ICON));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}


}

