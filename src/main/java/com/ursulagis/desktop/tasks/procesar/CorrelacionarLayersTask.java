package com.ursulagis.desktop.tasks.procesar;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;

import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.GeometryHelper;

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
			 super.updateTitle(taskName+" construyendo grilla");
			List<Geometry> grilla = construirGrilla(laborX,laborY);
			 super.updateTitle(taskName+" procesando");
			Integer totalWork = grilla.size();
			this.updateProgress(1, totalWork);
			AtomicInteger workDone = new AtomicInteger(1);
 			List<XYChart.Data<Number, Number>> xyData = grilla.parallelStream().collect(
					()->new ArrayList<XYChart.Data<Number, Number>>(),
					(xy, g) ->{		
						double x=0,y=0;
						List<SimpleFeature> xFeatures = cachedOutStoreQuery(g,laborX);
						OptionalDouble xOpt = xFeatures.stream().mapToDouble(f->{
						Object objValue= f.getAttribute(columnX);
						return LaborItem.getDoubleFromObj(objValue);
						
						}).average();
						//TODO ver si valela pena hacer un promedio ponderado por la superficie superpuesta
						if(xOpt.isPresent())x=xOpt.getAsDouble();
						
						List<SimpleFeature> yFeatures = cachedOutStoreQuery(g,laborY);					
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
		

		List<Geometry> contornos = new ArrayList<Geometry>();
		contornos.addAll(grilla);
		return contornos;
/*
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
*/
	}
	
	/**
	 * metodo para probar cual de las 2 implementaciones es mas rapida. 
	 * pero cualquiera de las 2 es mas rapida que esto
	 * @param g
	 * @param labor
	 * @return
	 */
	public  List<SimpleFeature> query(Geometry g,Labor<? extends LaborItem> labor){
		List<SimpleFeature> ret =null;
		  CompletableFuture<List<SimpleFeature>> service1 = CompletableFuture.supplyAsync(() -> {
	          return cachedOutStoreQuery(g,labor);
	        });
		  CompletableFuture<List<SimpleFeature>> service2 = CompletableFuture.supplyAsync(() -> {
	          return outStoreQuery(g,labor);
	        });
		  
		  CompletableFuture<Object> firstResponse = CompletableFuture.anyOf(service1, service2);
		  try {
			
	       ret = (List<SimpleFeature>)firstResponse.get();
	       service1.cancel(true);
	       service2.cancel(true);
	       
		  }catch(Exception e) {
			  e.printStackTrace();
		  }
		  return ret;
	}

	
	public  List<SimpleFeature> cachedOutStoreQuery(Geometry g,Labor<? extends LaborItem> labor){
		List<? extends LaborItem> res = labor.outStoreQuery(g.getEnvelopeInternal());
		
		List<SimpleFeature> objects = res.parallelStream()
				.map(item->
				item.getFeature(
						labor.getFeatureBuilder()
						)
				).collect(Collectors.toList());
		return objects;
		
	}
	/**
	 * 
	 * @param g
	 * @param labor
	 * @return List<SimpleFeature> de la labor que coincida con la geometria g
	 */
	public  List<SimpleFeature> outStoreQuery(Geometry g,Labor<? extends LaborItem> labor){
		//XXX usar labor.cachedOutStoreQuery(g.getEnvelopeInternal()); ??
		List<SimpleFeature> objects = new ArrayList<>();
		//TODO tratar de cachear todo lo posible para evitar repetir trabajo en querys consecutivas.
		//una udea es cachear un sector del out collection y solo hacer la query si el envelope esta fuera de lo cacheado
		if(labor.outCollection.getBounds().intersects(g.getEnvelopeInternal())){//solo hago la query si el bounds esta dentro del mapa
			FilterFactory ff = CommonFactoryFinder.getFilterFactory( GeoTools.getDefaultHints() );
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
