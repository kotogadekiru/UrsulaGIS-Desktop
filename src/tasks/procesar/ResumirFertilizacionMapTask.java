package tasks.procesar;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import dao.LaborItem;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.margen.MargenItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import gui.nww.LaborLayer;
import tasks.ProcessMapTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;


/**
 * 
 * @author quero
 * task que toma un store y devuelve una capa de suelo a partir del store
 */

public class ResumirFertilizacionMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	private FertilizacionLabor aResumir=null;
	private Map<Class,Function<LaborItem,String>> tooltipCreator = ClonarLaborMapTask.constructTooltipCreator();
	public ResumirFertilizacionMapTask(FertilizacionLabor _aResumir) {
//		FertilizacionLabor margen = new FertilizacionLabor();
//		margen.setLayer(new LaborLayer());
//		this.labor=margen;		
		aResumir=_aResumir;
		this.labor=(FertilizacionLabor)ClonarLaborMapTask.laborConstructor()
				.get(aResumir.getClass())
				.apply(aResumir);
		
		
		labor.setNombre(aResumir.getNombre()+Messages.getString("ResumirMargenMapTask.resumido"));
	//	labor.setContorno(aResumir.getContorno());
//		labor.getCostoFijoHaProperty().setValue(aResumir.getCostoFijoHaProperty().getValue());
//		labor.getCostoFleteProperty().setValue(aResumir.getCostoFleteProperty().getValue());
//		labor.getCostoTnProperty().setValue(aResumir.getCostoTnProperty().getValue());
		labor.setClasificador(aResumir.getClasificador().clone());
		//aResumir.getLayer().setEnabled(false);
	}
	
	public void doProcess() throws IOException {
		List<FertilizacionItem> resumidas = resumirPorCategoria(this.labor);
		if(labor.outCollection!=null)labor.outCollection.clear();
		labor.treeCache=null;
		labor.treeCacheEnvelope=null;
		resumidas.stream().forEach(renta->
			labor.insertFeature(renta)
		);
		
		labor.constructClasificador();
		runLater(resumidas);
		updateProgress(0, featureCount);

	}

//	public static String buildTooltipText(FertilizacionItem renta,double area) {
//		
//		
//		NumberFormat df = Messages.getNumberFormat();
//
//		String tooltipText = new String(
//						  Messages.getString("OpenMargenMapTask.1") + df.format(renta.getRentabilidadHa()) + Messages.getString("OpenMargenMapTask.2")  //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.3") + df.format(renta.getMargenPorHa()) + Messages.getString("OpenMargenMapTask.4")  //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.5")	+ df.format(renta.getCostoPorHa()) + Messages.getString("OpenMargenMapTask.6") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.7")	+ df.format(renta.getImporteFertHa()) + Messages.getString("OpenMargenMapTask.8")  //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.9")	+ df.format(renta.getImportePulvHa()) + Messages.getString("OpenMargenMapTask.10") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.11")+ df.format(renta.getImporteSiembraHa()) + Messages.getString("OpenMargenMapTask.12") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.13")+ df.format(renta.getCostoFijoPorHa()) + Messages.getString("OpenMargenMapTask.14") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.15")+ df.format(renta.getImporteCosechaHa()) + Messages.getString("OpenMargenMapTask.16")  //$NON-NLS-1$ //$NON-NLS-2$
//				);
//
//		if(area<1){
//			tooltipText=tooltipText.concat( Messages.getString("OpenMargenMapTask.17")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("OpenMargenMapTask.18")); //$NON-NLS-1$ //$NON-NLS-2$
//		} else {
//			tooltipText=tooltipText.concat(Messages.getString("OpenMargenMapTask.19")+df.format(area ) + Messages.getString("OpenMargenMapTask.20")); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		return tooltipText;
//	}
	
	/**
	 * metodo que toma la labor y devuelve una lista de los items agrupados por categoria
	 * @param labor
	 * @return
	 */
	private List<FertilizacionItem> resumirPorCategoria(FertilizacionLabor labor) {
		//TODO antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		//XXX inicializo la lista de las features por categoria
		List<List<FertilizacionItem>> itemsByCat = new ArrayList<List<FertilizacionItem>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			itemsByCat.add(i, new ArrayList<FertilizacionItem>());
		}
		
		//XXX recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = aResumir.outCollection.features();

		while(it.hasNext()){
			SimpleFeature f = it.next();
			FertilizacionItem ci = aResumir.constructFeatureContainerStandar(f, false);
		
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			System.out.println("cat for "+ci.getAmount()+" es "+cat);
			itemsByCat.get(cat).add(ci);
		}
		it.close();
		updateProgress(1, 100);
		
		
		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<FertilizacionItem> itemsCategoria = new ArrayList<FertilizacionItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		//XXX por cada categoria 
		
			for(List<FertilizacionItem> catItems : itemsByCat) {
				System.out.println("resumiendo "+catItems.size());
				if(catItems.size()>0) {
					itemsCategoria.add(resumirItems(catItems));
				}				
			}
			
		System.out.println("items resumidos "+itemsCategoria.size());
		return itemsCategoria;
	}

	public static FertilizacionItem resumirItems(List<FertilizacionItem> catItems) {
		FertilizacionItem ret = new FertilizacionItem();
		Double _id=null;
		Double amountTotal = new Double(0.0);
//		Double importeCosecha=new Double(0.0);
//		Double importeFert=new Double(0.0);
//		Double importePulv=new Double(0.0);
//		Double importeSiembra=new Double(0.0);
		Double hasTotal=new Double(0.0);
		List<Geometry> geoms = new ArrayList<Geometry>(); 
		
		for(FertilizacionItem item : catItems) {
			Geometry g = item.getGeometry();
			Double hasItem = ProyectionConstants.A_HAS(g.getArea());
			if(_id==null) {
				_id=item.getId();
			}
//			importeCosecha+=item.getImporteCosechaHa()*hasItem;
//			importeFert+=item.getImporteFertHa()*hasItem;
//			importePulv+=item.getImportePulvHa()*hasItem;
//			importeSiembra+=item.getImporteSiembraHa()*hasItem;
			amountTotal+=item.getAmount()*hasItem;
			hasTotal+=hasItem;
			geoms.add(item.getGeometry());
			
			//TODO sumar todos los importes y dividirlos por la nueva superficie
		}
		amountTotal = amountTotal / hasTotal;
//		importeFert = importeFert / hasTotal;
//		importePulv = importePulv / hasTotal;
//		importeSiembra = importeSiembra / hasTotal;
		
		ret.setId(_id);
		ret.setDosistHa(amountTotal);
//		ret.setImporteCosechaHa(importeCosecha);		
//		ret.setImporteFertHa(importeFert);
//		ret.setImportePulvHa(importePulv);
//		ret.setImporteSiembraHa(importeSiembra);
		
		ret.setGeometry(GeometryHelper.unirGeometrias(geoms));
		
		return ret;
	}
	
	
	protected ExtrudedPolygon getPathTooltip( Geometry poly,FertilizacionItem renta,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();

		String tooltipText2 = tooltipCreator.get(FertilizacionLabor.class).apply(renta);
		//String tooltipText= buildTooltipText(renta,area);
		return super.getExtrudedPolygonFromGeom(poly, renta,tooltipText2,renderablePolygon);
	//	super.getRenderPolygonFromGeom(poly, renta,tooltipText);
	}

	
	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}

}// fin del task

