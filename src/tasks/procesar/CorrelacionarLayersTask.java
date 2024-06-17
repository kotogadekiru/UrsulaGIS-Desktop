package tasks.procesar;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import dao.Labor;
import dao.LaborItem;

import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import tasks.ProgresibleTask;
import utils.GeometryHelper;

public class CorrelacionarLayersTask extends ProgresibleTask<XYChart.Series<Number, Number>> {
	Labor<?> laborX=null;
	String columnX=null;
	Labor<?> laborY=null;
	String columnY=null;

	public CorrelacionarLayersTask(Labor<?> _laborX, String _columnX, Labor<?> _laborY, String _columnY) {
		laborX=_laborX;
		columnX=_columnX;
		laborY=_laborY;
		columnY=_columnY;
		this.taskName="Correlacion";//+_laborX.getNombre()+" "+_laborY.getNombre();
		 super.updateTitle(taskName);
		// this.taskName= laborToExport.getNombre();
	}
	
	@Override
	protected Series<Number, Number> call()  {
		try {
			this.updateProgress(0, 100);
			List<Geometry> grilla = construirGrilla(laborX,laborY);
			
			Integer totalWork = grilla.size();
			this.updateProgress(1, totalWork);
			AtomicInteger workDone = new AtomicInteger(1);
 			List<XYChart.Data<Number, Number>> xyData = grilla.parallelStream().collect(
					()->new ArrayList<XYChart.Data<Number, Number>>(),
					(xy, g) ->{		
						double x=0,y=0;
						List<SimpleFeature> xFeatures = outStoreQuery(g,laborX);
						OptionalDouble xOpt = xFeatures.stream().mapToDouble(f->{
						Object objValue= f.getAttribute(columnX);
						return LaborItem.getDoubleFromObj(objValue);
						
						}).average();
						//TODO ver si valela pena hacer un promedio ponderado por la superficie superpuesta
						if(xOpt.isPresent())x=xOpt.getAsDouble();
						
						List<SimpleFeature> yFeatures = outStoreQuery(g,laborY);					
						OptionalDouble yOpt = yFeatures.stream().mapToDouble(f->{
						Object objValue= f.getAttribute(columnY);
						return LaborItem.getDoubleFromObj(objValue);
						
						}).average();
						if(yOpt.isPresent())y=yOpt.getAsDouble();
						if(xFeatures.size()>0 && yFeatures.size()>0) {
							//solo agrego los datos si hay features de las 2 capas
						Data<Number, Number> data = new XYChart.Data<Number, Number>(x, y);
						xy.add(data);
						}
						this.updateProgress(workDone.incrementAndGet(), totalWork);						
					},	(xy1, xy2) -> xy1.addAll(xy2));		
			 XYChart.Series<Number, Number> series = new XYChart.Series<>();
			 System.out.println("creando el grafico con "+xyData.size()+" elementos");
			 series.getData().addAll(xyData);
			 return series;
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<Geometry> construirGrilla(Labor<?> lx, Labor<?> ly) {
		double ancho = lx.getConfigLabor().getAnchoGrilla();
		ReferencedEnvelope unionEnvelope = new ReferencedEnvelope();
		unionEnvelope.expandToInclude(lx.outCollection.getBounds());
		unionEnvelope.expandToInclude(ly.outCollection.getBounds());

		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);
		//System.out.println("construi una grilla con "+grilla.size()+" elementos");//construi una grilla con 5016 elementos
		//obtener una lista con todas las geometrias de las labores
		List<Geometry> contornos = new ArrayList<Geometry>();
		Geometry lxContorno = GeometryHelper.extractContornoGeometry(lx);
		Geometry lyContorno = GeometryHelper.extractContornoGeometry(ly);
		contornos.add(lxContorno);
		contornos.add(lyContorno);
	

		//unir las geometrias de todas las labores para obtener un poligono de contorno
		Geometry cover = GeometryHelper.unirGeometrias(contornos);
		//System.out.println("el area del cover es: "+GeometryHelper.getHas(cover));//el area del cover es: 3.114509320893096E-12
		//intersectar la grilla con el contorno
		List<Geometry> grillaCover = grilla.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(intersecciones, poly) ->{					
					Geometry intersection = GeometryHelper.getIntersection(poly, cover); 
					if(intersection!=null) {
						intersecciones.add(intersection);
					}
				},	(env1, env2) -> env1.addAll(env2));
		return grillaCover;
	}

	/**
	 * 
	 * @param g
	 * @param labor
	 * @return List<SimpleFeature> de la labor que coincida con la geometria g
	 */
	public synchronized List<SimpleFeature> outStoreQuery(Geometry g,Labor<? extends LaborItem> labor){
		//XXX usar labor.cachedOutStoreQuery(g.getEnvelopeInternal()); ??
		List<SimpleFeature> objects = new ArrayList<>();
		//TODO tratar de cachear todo lo posible para evitar repetir trabajo en querys consecutivas.
		//una udea es cachear un sector del out collection y solo hacer la query si el envelope esta fuera de lo cacheado
		if(labor.outCollection.getBounds().intersects(g.getEnvelopeInternal())){//solo hago la query si el bounds esta dentro del mapa
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
			FeatureType schema = labor.outCollection.getSchema();

			// usually "THE_GEOM" for shapefiles
			String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
			CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

			ReferencedEnvelope bbox = new ReferencedEnvelope(g.getEnvelopeInternal(),targetCRS);		

			BBOX filter = ff.bbox(ff.property(geometryPropertyName), bbox);

			SimpleFeatureCollection features = labor.outCollection.subCollection(filter);//OK!! esto funciona
			// System.out.println("encontre "+features.size()+" que se intersectan con "+ bbox );

			//Polygon boundsPolygon = GeometryHelper.constructPolygon(bbox);

			SimpleFeatureIterator featuresIterator = features.features();
			while(featuresIterator.hasNext()){
				SimpleFeature next = featuresIterator.next();
				Object obj = next.getDefaultGeometry();

				Geometry defaultGeom = null;
				if(obj instanceof Geometry){					
					defaultGeom =(Geometry)obj;					 
				} 

				boolean intersects = false;
				if(defaultGeom!=null){
					intersects = defaultGeom.intersects(g );
				}
				if(intersects && defaultGeom!=null && g!=null ){
					Geometry intersection = GeometryHelper.getIntersection(g, defaultGeom);
					if(intersection!=null) {
						//next.setDefaultGeometry(intersection);
						//XXX Verificar que no se esta pisando la informacion original
						objects.add(next);
					}
				}
			}
			featuresIterator.close();
		}

		return objects;
	}
}
