package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.geotools.api.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.precision.EnhancedPrecisionOp;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.margen.MargenItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.ConvertirASiembraTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.tasks.crear.CrearFertilizacionMapTask;
import com.ursulagis.desktop.tasks.crear.CrearPulverizacionMapTask;
import com.ursulagis.desktop.tasks.crear.CrearSueloMapTask;
import com.ursulagis.desktop.tasks.importar.OpenMargenMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class CortarLaborMapTask extends ProcessMapTask<LaborItem,Labor<LaborItem>> {
	/**
	 * la lista de las cosechas a unir
	 */
	private Labor<?> laborACortar=null;
	private List<Poligono> poligonos=null;
	private Map<Class, Function<LaborItem, String>> tooltipCreator;


	
	public CortarLaborMapTask(Labor<?> _laborACortar,List<Poligono> _poligonos){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.laborACortar=_laborACortar;
		this.poligonos=_poligonos;
		
	
		Map<Class, Function<Labor, Labor>> constructor = laborConstructor();
		
		this.tooltipCreator = constructTooltipCreator();
	
		this.labor=constructor.get(this.laborACortar.getClass()).apply(laborACortar);

		List<String> nombres =this.poligonos.stream().map(p->p.getNombre()).collect(Collectors.toList());

		labor.setNombre(_laborACortar.getNombre()+"-"+String.join("-", nombres));//este es el nombre que se muestra en el progressbar
	}

	public static Map<Class, Function<Labor, Labor>> laborConstructor() {
		Map<Class,Function<Labor,Labor>> constructor = new HashMap<Class,Function<Labor,Labor>>();
		constructor.put(CosechaLabor.class, l->{
			return new CosechaLabor();
		});
		constructor.put(SiembraLabor.class, l->{
			SiembraLabor os = (SiembraLabor)l;
			SiembraLabor ns =  new SiembraLabor();
			ns.setEntreSurco(os.getEntreSurco());
			ns.setSemilla(os.getSemilla());
			ns.setPrecioInsumo(os.getPrecioInsumo());
			ns.setPrecioLabor(os.getPrecioLabor());
			ns.setFecha(os.getFecha());
			ns.setFertLinea(os.getFertLinea());
			ns.setFertCostado(os.getFertCostado());
			return ns;
		});
		constructor.put(FertilizacionLabor.class, l->{
			return new FertilizacionLabor();
		});
		constructor.put(PulverizacionLabor.class, l->{
			return new PulverizacionLabor();
		});
		constructor.put(Suelo.class, l->{
			return new Suelo();
		});
		constructor.put(Margen.class, l->{
			Margen ol =(Margen)l;
			Margen newl = new Margen();
			newl.setFecha(ol.getFecha());
			newl.getCostoFleteProperty().setValue(ol.getCostoFleteProperty().getValue());
			newl.getCostoFijoHaProperty().setValue(ol.getCostoFijoHaProperty().getValue());
			newl.getCostoTnProperty().setValue(ol.getCostoTnProperty().getValue());
			//set col amout
			newl.colAmount.set(newl.colAmount.get());			
			return newl;
		});
		return constructor;
	}

	public static Map<Class,Function<LaborItem,String>> constructTooltipCreator() {
		Map<Class,Function<LaborItem,String>> tooltipCreator = new HashMap<Class,Function<LaborItem,String>>();
		tooltipCreator.put(CosechaLabor.class, li->{
			Geometry poly = li.getGeometry();
			double area = poly.getArea() * ProyectionConstants.A_HAS();
			return CrearCosechaMapTask.buildTooltipText((CosechaItem)li, area);			
		});
		tooltipCreator.put(SiembraLabor.class, li->{
			Geometry poly = li.getGeometry();
			double area = poly.getArea() * ProyectionConstants.A_HAS();
			return ConvertirASiembraTask.buildTooltipText((SiembraItem)li, area);	
		});
		tooltipCreator.put(FertilizacionLabor.class, li->{
			Geometry poly = li.getGeometry();
			double area = poly.getArea() * ProyectionConstants.A_HAS();
			return CrearFertilizacionMapTask.buildTooltipText((FertilizacionItem)li, area);	
		});
		tooltipCreator.put(PulverizacionLabor.class, li->{
			Geometry poly = li.getGeometry();
			double area = poly.getArea() * ProyectionConstants.A_HAS();
			return CrearPulverizacionMapTask.buildTooltipText((PulverizacionItem)li, area);	
		});
		tooltipCreator.put(Suelo.class, li->{
			Geometry poly = li.getGeometry();
			double area = poly.getArea() * ProyectionConstants.A_HAS();
			return CrearSueloMapTask.buildTooltipText((SueloItem)li, area);	
		});
		tooltipCreator.put(Margen.class, li->{
			Geometry poly = li.getGeometry();
			double area = poly.getArea() * ProyectionConstants.A_HAS();
			return OpenMargenMapTask.buildTooltipText((MargenItem)li, area);	
		});
		return tooltipCreator;
	}

	/**
	 * proceso que toma una cosecha y selecciona los items que estan dentro de los poligonos seleccionados
	 */
	@Override
	protected void doProcess() throws IOException {
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.laborACortar.outCollection.reader();
		this.featureCount=this.laborACortar.outCollection.size();
		updateProgress(0, featureCount);
		while(reader.hasNext()){
			SimpleFeature f = reader.next();
			//CosechaItem ci = labor.constructFeatureContainerStandar(f,true);
			Geometry g = (Geometry)f.getDefaultGeometry();//ci.getGeometry();

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
				//double bufer= ProyectionConstants.metersToLongLat(0.25);//esto agranda la superficie. porque?
				try{
				//	buffered = colectionCat.union();
					buffered =colectionCat.buffer(0);
				}catch(Exception e){
					System.out.println(Messages.getString("ProcessHarvestMapTask.10")); //$NON-NLS-1$
					//java.lang.IllegalArgumentException: Comparison method violates its general contract!
					try{
					buffered= EnhancedPrecisionOp.buffer(colectionCat, 0);//java.lang.IllegalArgumentException: Comparison method violates its general contract!
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
//				ci.setGeometry(buffered);
//				SimpleFeature nf=ci.getFeature(labor.featureBuilder);
				
				SimpleFeature nf=SimpleFeatureBuilder.copy(f);
				nf.setDefaultGeometry(buffered);

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

		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	LaborItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		
//		String tooltipText = tooltipCreator.get(this.labor.getClass()).apply(cosechaItem);
//				//CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
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
