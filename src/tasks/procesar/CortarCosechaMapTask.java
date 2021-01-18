package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Poligono;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import gui.nww.LaborLayer;
import tasks.ProcessMapTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;

public class CortarCosechaMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private CosechaLabor cosecha=null;
	private List<Poligono> poligonos=null;

	public CortarCosechaMapTask(CosechaLabor cosechaACortar,List<Poligono> _poligonos){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.cosecha=cosechaACortar;
		this.poligonos=_poligonos;

		super.labor = new CosechaLabor();
		//TODO asignar las columnas a  los valores estanar
		labor.colAmount.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
		labor.colRendimiento.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
		labor.colAncho.set(CosechaLabor.COLUMNA_ANCHO);
		labor.colCurso.set(CosechaLabor.COLUMNA_CURSO);
		labor.colDistancia.set(CosechaLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(CosechaLabor.COLUMNA_ELEVACION);
		//labor.colVelocidad.set(CosechaLabor.COLUMNA_VELOCIDAD);
		//labor.colPasada.set(CosechaLabor.COLUMNA_ANCHO);

		labor.getConfiguracion().valorMetrosPorUnidadDistanciaProperty().set(1.0);
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		//	String nombreProgressBar = Messages.getString("JFXMain.cortarCosechaAction");

		List<String> nombres =this.poligonos.stream().map(p->p.getNombre()).collect(Collectors.toList());

		labor.setNombre(cosechaACortar.getNombre()+"-"+String.join("-", nombres));//este es el nombre que se muestra en el progressbar
	}

	/**
	 * proceso que toma una lista de cosechas y las une sin tener en cuenta superposiciones ni nada
	 */
	@Override
	protected void doProcess() throws IOException {	

		FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.cosecha.outCollection.reader();
		while(reader.hasNext()){
			SimpleFeature f = reader.next();
			CosechaItem ci = labor.constructFeatureContainerStandar(f,true);
			Geometry g = ci.getGeometry();

			/*
			 * calcula las intesecciones entre la geometria del cosechaitem y los poligonos seleccionados
			 */
			 List<Geometry> intersecciones = poligonos.stream().map(pol->{
				 Geometry ret = GeometryHelper.getIntersection(pol.toGeometry(), g);
				//System.out.println("intersection is "+ret);
				return ret;// ? pol.toGeometry().intersection(g):null;
				}).filter(inter->inter!=null).collect(Collectors.toList());

			if(intersecciones.size()>0) {
				GeometryFactory fact = intersecciones.get(0).getFactory();
				Geometry[] geomArray = new Geometry[intersecciones.size()];
				GeometryCollection colectionCat = fact.createGeometryCollection(intersecciones.toArray(geomArray));

				Geometry buffered = null;
				double bufer= ProyectionConstants.metersToLongLat(0.25);
				try{
					buffered = colectionCat.union();
					buffered =buffered.buffer(bufer);
				}catch(Exception e){
					System.out.println(Messages.getString("ProcessHarvestMapTask.10")); //$NON-NLS-1$
					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					try{
					buffered= EnhancedPrecisionOp.buffer(colectionCat, bufer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}
				
				ci.setGeometry(buffered);
				SimpleFeature nf=ci.getFeature(labor.featureBuilder);

				boolean ret = labor.outCollection.add(nf);
				//featuresInsertadas++;
				if(!ret){
					System.out.println("no se pudo agregar la feature "+f);
				}
			}
		}

		reader.close();
		labor.setLayer(new LaborLayer());
		labor.constructClasificador();

		List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();

		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f=it.next();
			itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
		}
		it.close();

		runLater(itemsToShow);
		updateProgress(0, featureCount);
	}

	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
		//	System.out.println("getPathTooltip(); "+System.currentTimeMillis());
		//List<SurfacePolygon>  paths = getSurfacePolygons(poly, cosechaFeature);//
		//	List<gov.nasa.worldwind.render.Polygon>  paths = super.getPathFromGeom2D(poly, cosechaFeature);
		//ExtrudedPolygon  path = super.getPathFromGeom2D(poly, cosechaFeature);

		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$

		String tooltipText = new String(Messages.getString("ProcessHarvestMapTask.23") //$NON-NLS-1$
				+ df.format(cosechaItem.getAmount()) + Messages.getString("ProcessHarvestMapTask.24") //$NON-NLS-1$
				//	+ "Area: "+ df.format(area * ProyectionConstants.METROS2_POR_HA)+ " m2\n" + 

				);

		tooltipText=tooltipText.concat(Messages.getString("ProcessHarvestMapTask.25")+df.format(cosechaItem.getElevacion() ) + Messages.getString("ProcessHarvestMapTask.26")); //$NON-NLS-1$ //$NON-NLS-2$

		tooltipText=tooltipText.concat(Messages.getString("ProcessHarvestMapTask.27")+df.format(cosechaItem.getAncho() ) + Messages.getString("ProcessHarvestMapTask.28")); //$NON-NLS-1$ //$NON-NLS-2$
		tooltipText=tooltipText.concat(Messages.getString("ProcessHarvestMapTask.29")+df.format(cosechaItem.getRumbo() ) + Messages.getString("ProcessHarvestMapTask.30")); //$NON-NLS-1$ //$NON-NLS-2$
		tooltipText=tooltipText.concat(Messages.getString("ProcessHarvestMapTask.31")+cosechaItem.getId() + Messages.getString("ProcessHarvestMapTask.32")); //$NON-NLS-1$ //$NON-NLS-2$
		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessHarvestMapTask.33")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessHarvestMapTask.34")); //$NON-NLS-1$ //$NON-NLS-2$
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessHarvestMapTask.35")+df.format(area ) + Messages.getString("ProcessHarvestMapTask.36")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		//super.getRenderPolygonFromGeom(poly, cosechaItem,tooltipText);
		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText, renderablePolygon);
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
