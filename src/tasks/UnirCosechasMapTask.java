package tasks;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;

import java.io.IOException;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import utils.ProyectionConstants;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import dao.CosechaItem;
import dao.CosechaLabor;

public class UnirCosechasMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<CosechaLabor> cosechas;

	public UnirCosechasMapTask(List<CosechaLabor> cosechas){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.cosechas=new ArrayList<CosechaLabor>();
		for(CosechaLabor l:cosechas){
			if(l.getLayer().isEnabled()){
				this.cosechas.add(l);
			}
		};

		super.labor = new CosechaLabor();
		//TODO asignar las columnas a  los valores estanar
		labor.colAmount.set(CosechaLabor.COLUMNA_RENDIMIENTO);
		labor.colRendimiento.set(CosechaLabor.COLUMNA_RENDIMIENTO);
		labor.colAncho.set(CosechaLabor.COLUMNA_ANCHO);
		labor.colCurso.set(CosechaLabor.COLUMNA_CURSO);
		labor.colDistancia.set(CosechaLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(CosechaLabor.COLUMNA_ELEVACION);
		labor.colVelocidad.set(CosechaLabor.COLUMNA_VELOCIDAD);
		//labor.colPasada.set(CosechaLabor.COLUMNA_ANCHO);

		labor.getConfiguracion().valorMetrosPorUnidadDistanciaProperty().set(1.0);
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);

		labor.getNombreProperty().setValue("union de cosechas");//este es el nombre que se muestra en el progressbar
	}

	/**
	 * proceso que toma una lista de cosechas y las une sin tener en cuenta superposiciones ni nada
	 */
	@Override
	protected void doProcess() throws IOException {
		long init = System.currentTimeMillis();
		// TODO 1 obtener el bounds general que cubre a todas las cosechas
		//	ReferencedEnvelope unionEnvelope = null;
		double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();
		String nombre =null;
		for(CosechaLabor c:cosechas){
			if(nombre == null){
				nombre="union "+c.getNombreProperty().get();	
			}else {
				nombre+=" - "+c.getNombreProperty().get();
			}
			FeatureReader<SimpleFeatureType, SimpleFeature> reader = c.outCollection.reader();
			while(reader.hasNext()){
				SimpleFeature f = reader.next();
				CosechaItem ci = labor.constructFeatureContainerStandar(f,true);
				SimpleFeature nf=ci.getFeature(labor.featureBuilder);
				boolean ret = labor.outCollection.add(nf);
				if(!ret){
					System.out.println("no se pudo agregar la feature "+f);
				}
			}
			reader.close();
		}

		labor.nombreProperty.set(nombre);
		labor.setLayer(new RenderableLayer());


		//TODO 4 mostrar la cosecha sintetica creada
		labor.constructClasificador();

		List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();

			SimpleFeatureIterator it = labor.outCollection.features();
			while(it.hasNext()){
				SimpleFeature f=it.next();
				itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
			}
			it.close();
		

		//this.pathTooltips.clear();
//		labor.getLayer().removeAllRenderables();
//		for(CosechaItem c:itemsToShow){
//			Geometry g = c.getGeometry();
//			if(g instanceof Polygon){
//						getPathTooltip((Polygon)g,c);
//			} else if(g instanceof MultiPolygon){
//				MultiPolygon mp = (MultiPolygon)g;			
//				for(int i=0;i<mp.getNumGeometries();i++){
//					Polygon p = (Polygon) (mp).getGeometryN(i);
//					getPathTooltip(p,c);
//				}
//
//			}
//		}
		runLater(itemsToShow);
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde "+time+" milisegundos en unir las cosechas. es "+time+" milisegundos");
	}

	protected void getPathTooltip(Geometry poly,
			CosechaItem cosechaFeature) {
		//	System.out.println("getPathTooltip(); "+System.currentTimeMillis());
		//	List<gov.nasa.worldwind.render.Polygon>  paths = super.getPathFromGeom2D(poly, cosechaFeature);
		//ExtrudedPolygon  path = super.getPathFromGeom2D(poly, cosechaFeature);

		double area = poly.getArea() * ProyectionConstants.A_HAS;// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("#.00");

		String tooltipText = new String("Rinde: "
				+ df.format(cosechaFeature.getAmount()) + " Tn/Ha\n"
				//	+ "Area: "+ df.format(area * ProyectionConstants.METROS2_POR_HA)+ " m2\n" + 

				);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
			tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		tooltipText=tooltipText.concat("Pasada: "+df.format(cosechaFeature.getPasada() ) + "\n");
		tooltipText=tooltipText.concat("feature: "+cosechaFeature.getId() + "\n");

		 super.getPathFromGeom2D(poly, cosechaFeature,tooltipText);
	}

	@Override
	protected int getAmountMin() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		// TODO Auto-generated method stub
		return 0;
	}

}
