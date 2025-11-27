package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.precision.EnhancedPrecisionOp;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;
@Deprecated
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
	 * proceso que toma una cosecha y selecciona los items que estan dentro de los poligonos seleccionados
	 */
	@Override
	protected void doProcess() throws IOException {
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.cosecha.outCollection.reader();
		this.featureCount=this.cosecha.outCollection.size();
		updateProgress(0, featureCount);
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
				//	buffered = colectionCat.union();
					buffered =colectionCat.buffer(bufer);
				}catch(Exception e){
					System.out.println(Messages.getString("ProcessHarvestMapTask.10")); //$NON-NLS-1$
					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					try{
					buffered= EnhancedPrecisionOp.buffer(colectionCat, bufer);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}
				try{	
					buffered = TopologyPreservingSimplifier.simplify(buffered, ProyectionConstants.metersToLongLat(0.25));
					//g =g.buffer(0);		
					
				}catch(Exception e){
					e.printStackTrace();
				}				
				ci.setGeometry(buffered);
				SimpleFeature nf=ci.getFeature(labor.getFeatureBuilder());

				boolean ret = labor.outCollection.add(nf);
				//featuresInsertadas++;
				if(!ret){
					System.out.println("no se pudo agregar la feature "+f);
				}
				updateProgress(this.featureNumber++, featureCount);
			}
		}

		reader.close();
		labor.setLayer(new LaborLayer());
		labor.constructClasificador();

	//	List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();

//		SimpleFeatureIterator it = labor.outCollection.features();
//		while(it.hasNext()){
//			SimpleFeature f=it.next();
//		//	itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
//		}
//		it.close();

		//runLater(itemsToShow);
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//	}

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
