package tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import dao.Labor;
import dao.LaborItem;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;


/**
 * 
 * @author quero
 * task que toma un store y devuelve una capa de suelo a partir del store
 */

public class ResumirLaborMapTask extends ProcessMapTask<LaborItem,Labor<LaborItem>> {
	private Labor<?> aResumir=null;
	private Map<Class,Function<LaborItem,String>> tooltipCreator = ClonarLaborMapTask.constructTooltipCreator();
	public ResumirLaborMapTask(Labor<?> _aResumir) {	
		aResumir=_aResumir;
		this.labor=ClonarLaborMapTask.laborConstructor()
				.get(aResumir.getClass())
				.apply(aResumir);		
		
		labor.setNombre(aResumir.getNombre()+" "+Messages.getString("ResumirMargenMapTask.resumido"));
	//	labor.setContorno(aResumir.getContorno());
//		labor.getCostoFijoHaProperty().setValue(aResumir.getCostoFijoHaProperty().getValue());
//		labor.getCostoFleteProperty().setValue(aResumir.getCostoFleteProperty().getValue());
//		labor.getCostoTnProperty().setValue(aResumir.getCostoTnProperty().getValue());
		labor.setClasificador(aResumir.getClasificador().clone());
		//aResumir.getLayer().setEnabled(false);
	}
	
	public void doProcess() throws IOException {
		List<LaborItem> resumidas = resumirPorCategoria(this.labor);
		if(labor.outCollection!=null)labor.outCollection.clear();
		labor.treeCache=null;
		labor.treeCacheEnvelope=null;
		resumidas.stream().forEach(renta->
			labor.insertFeature(renta)
		);
		
		labor.constructClasificador();
		runLater(resumidas);//FIXME al resumir una siembra layer es null y da null pointer 
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
	private List<LaborItem> resumirPorCategoria(Labor<?> labor) {
		//TODO antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		//XXX inicializo la lista de las features por categoria
		List<List<LaborItem>> itemsByCat = new ArrayList<List<LaborItem>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			itemsByCat.add(i, new ArrayList<LaborItem>());
		}
		
		//XXX recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = aResumir.outCollection.features();

		while(it.hasNext()){
			SimpleFeature f = it.next();
			LaborItem ci = aResumir.constructFeatureContainerStandar(f, false);
		
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			System.out.println("cat for "+ci.getAmount()+" es "+cat);
			itemsByCat.get(cat).add(ci);
		}
		it.close();
		updateProgress(1, 100);
		
		
		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<LaborItem> itemsCategoria = new ArrayList<LaborItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		//XXX por cada categoria 
		
			for(List<LaborItem> catItems : itemsByCat) {
				System.out.println("resumiendo "+catItems.size());
				if(catItems.size()>0) {
					itemsCategoria.add(resumirItems(catItems));
				}				
			}
			
		System.out.println("items resumidos "+itemsCategoria.size());
		return itemsCategoria;
	}

	public static LaborItem resumirItems(List<LaborItem> catItems) {
		FertilizacionItem ret = new FertilizacionItem();
		Double _id=null;
		Double amountTotal = new Double(0.0);
//		Double importeCosecha=new Double(0.0);
//		Double importeFert=new Double(0.0);
//		Double importePulv=new Double(0.0);
//		Double importeSiembra=new Double(0.0);
		Double hasTotal=new Double(0.0);
		List<Geometry> geoms = new ArrayList<Geometry>(); 
		
		//FIXME cambiar este metodo para que tome en cuenta el tipo de labor item
		for(LaborItem item : catItems) {
			Geometry g = item.getGeometry();
			Double hasItem = ProyectionConstants.A_HAS(g.getArea());
			if(_id==null) {
				_id=item.getId();
			}

			amountTotal+=item.getAmount()*hasItem;
			hasTotal+=hasItem;
			geoms.add(item.getGeometry());
			
			//TODO sumar todos los importes y dividirlos por la nueva superficie
		}
		amountTotal = amountTotal / hasTotal;

		
		ret.setId(_id);
		ret.setDosistHa(amountTotal);		
		ret.setGeometry(GeometryHelper.unirGeometrias(geoms));
		
		return ret;
	}
	
	
	protected ExtrudedPolygon getPathTooltip( Geometry poly,LaborItem renta,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();

		String tooltipText2 = tooltipCreator.get(this.labor.getClass()).apply(renta);
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

