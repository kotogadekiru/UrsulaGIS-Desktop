package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
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
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;


/**
 * 
 * @author quero
 * task que toma una labor y devuelve una version resumida segun el clasificador
 */

public class ResumirLaborMapTask extends ProcessMapTask<LaborItem,Labor<LaborItem>> {
	private Labor<?> aResumir=null;
	//private Map<Class,Function<LaborItem,String>> tooltipCreator = ClonarLaborMapTask.constructTooltipCreator();
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

	public LaborItem resumirItems(List<LaborItem> catItems) {
		LaborItem resumido = null;
		if(aResumir instanceof CosechaLabor) {
			List<CosechaItem> items =catItems.stream().map(item->(CosechaItem)item).collect(Collectors.toList());
			resumido = resumirCosItems(items);			
			((CosechaLabor)aResumir).setPropiedadesLabor((CosechaItem)resumido);
		} else 	if(aResumir instanceof SiembraLabor) {			
			List<SiembraItem> items =catItems.stream().map(item->(SiembraItem)item).collect(Collectors.toList());
			resumido = resumirSiembraItems(items);	
			((SiembraLabor)aResumir).setPropiedadesLabor((SiembraItem)resumido);
		} else 	if(aResumir instanceof FertilizacionLabor) {
			resumido = resumirFertItems(catItems);		
			((FertilizacionLabor)aResumir).setPropiedadesLabor((FertilizacionItem)resumido);
		} else 	if(aResumir instanceof PulverizacionLabor) {
			List<PulverizacionItem> items =catItems.stream().map(item->(PulverizacionItem)item).collect(Collectors.toList());
			resumido = resumirPulvItems(items);
			((PulverizacionLabor)aResumir).setPropiedadesLabor((PulverizacionItem)resumido);			
		} else 	if(aResumir instanceof Suelo) {
			List<SueloItem> items =catItems.stream().map(item->(SueloItem)item).collect(Collectors.toList());
			resumido = resumirSueloItems(items);//
			((Suelo)aResumir).setPropiedadesLabor((SueloItem)resumido);
		} else if(aResumir instanceof Margen) {
			List<MargenItem> items =catItems.stream().map(item->(MargenItem)item).collect(Collectors.toList());
			resumido = resumirMargenItems(items);//
			
			((Margen)aResumir).setPropiedadesLabor((MargenItem)resumido);
		}
		
		return resumido;
	} 
	
	
	public static LaborItem resumirCosItems(List<CosechaItem> cosechasPoly) {
		if(cosechasPoly.size()<1){
			return null;
		}
		
		List<Geometry> geoms = new ArrayList<Geometry>();
		Double hasTotal=Double.valueOf(0.0);
		Double _id=null;
		double rinde=0,desv=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;
		for(CosechaItem item : cosechasPoly) {
			Geometry g = item.getGeometry();
			Double hasItem = ProyectionConstants.A_HAS(g.getArea());
			if(_id==null) {
				_id=item.getId();
			}

			rinde+=item.getRindeTnHa()*hasItem;
			desv+=item.getDesvioRinde()*hasItem;
			ancho+=item.getAncho()*hasItem;
			distancia+=item.getDistancia()*hasItem;
			elev+=item.getElevacion()*hasItem;
			rumbo+=item.getRumbo()*hasItem;
			
			hasTotal+=hasItem;
			geoms.add(item.getGeometry());
		}
		if(hasTotal>0) {
			rinde = rinde / hasTotal;
			desv = desv / hasTotal;
			ancho = ancho / hasTotal;
			distancia = distancia / hasTotal;
			elev = elev / hasTotal;
			rumbo = rumbo / hasTotal;
		}	

		CosechaItem c = null;
			c = new CosechaItem();
			c.setId(_id);
			c.setGeometry(GeometryHelper.unirGeometrias(geoms));
			c.setRindeTnHa(rinde);
			c.setDesvioRinde(desv);
			c.setAncho(ancho);
			c.setDistancia(distancia);
			c.setElevacion(elev);
			c.setRumbo(rumbo);		
		return c;
	}
	
	public static LaborItem resumirSiembraItems(List<SiembraItem> items) {
		if(items.size()<1){
			return null;
		}
		
		List<Geometry> geoms = new ArrayList<Geometry>();
		Double hasTotal=Double.valueOf(0.0);
		Double _id=null;
		double dosisS=0,dosisML=0,fertL=0,fertC=0,ancho=0,distancia=0,elev=0,rumbo=0;// , pesos=0;
		for(SiembraItem item : items) {
			Geometry g = item.getGeometry();
			Double hasItem = ProyectionConstants.A_HAS(g.getArea());
			if(_id==null) {
				_id=item.getId();
			}

			dosisS+=item.getDosisHa()*hasItem;
			dosisML+=item.getDosisML()*hasItem;
			fertL+=item.getDosisFertLinea()*hasItem;
			fertC+=item.getDosisFertCostado()*hasItem;
			ancho+=item.getAncho()*hasItem;
			distancia+=item.getDistancia()*hasItem;
			elev+=item.getElevacion()*hasItem;
			rumbo+=item.getRumbo()*hasItem;
			
			hasTotal+=hasItem;
			geoms.add(item.getGeometry());
		}
		if(hasTotal>0) {
			dosisS = dosisS / hasTotal;
			dosisML = dosisML / hasTotal;
			fertL = fertL / hasTotal;
			fertC = fertC / hasTotal;
			ancho = ancho / hasTotal;
			distancia = distancia / hasTotal;
			elev = elev / hasTotal;
			rumbo = rumbo / hasTotal;
		}	

		SiembraItem c = null;
			c = new SiembraItem();
			c.setId(_id);
			c.setGeometry(GeometryHelper.unirGeometrias(geoms));
			c.setDosisHa(dosisS);
			c.setDosisML(dosisML);
			c.setDosisFertLinea(fertL);
			c.setDosisFertCostado(fertC);
			c.setAncho(ancho);
			c.setDistancia(distancia);
			c.setElevacion(elev);
			c.setRumbo(rumbo);		
		return c;
	}
	
	
	public static LaborItem resumirFertItems(List<LaborItem> catItems) {
		
		Double _id=null;
		Double amountTotal = Double.valueOf(0.0);
		Double hasTotal=Double.valueOf(0.0);
		List<Geometry> geoms = new ArrayList<Geometry>();
	
		for(LaborItem item : catItems) {
			Geometry g = item.getGeometry();
			Double hasItem = ProyectionConstants.A_HAS(g.getArea());
			if(_id==null) {
				_id=item.getId();
			}

			amountTotal+=item.getAmount()*hasItem;
			hasTotal+=hasItem;
			geoms.add(item.getGeometry());
		}
		if(hasTotal>0) {
			amountTotal = amountTotal / hasTotal;
		}

		
		FertilizacionItem ret = new FertilizacionItem();
		ret.setId(_id);
		ret.setDosistHa(amountTotal);		
		ret.setGeometry(GeometryHelper.unirGeometrias(geoms));
		
		return ret;
	}
	
public static LaborItem resumirPulvItems(List<PulverizacionItem> catItems) {
		
		Double _id=null;
		Double amountTotal = Double.valueOf(0.0);
		Double hasTotal=Double.valueOf(0.0);
		List<Geometry> geoms = new ArrayList<Geometry>();
	
		for(PulverizacionItem item : catItems) {
			Geometry g = item.getGeometry();
			Double hasItem = ProyectionConstants.A_HAS(g.getArea());
			if(_id==null) {
				_id=item.getId();
			}

			amountTotal+=item.getAmount()*hasItem;
			hasTotal+=hasItem;
			geoms.add(item.getGeometry());
		}
		if(hasTotal>0) {
			amountTotal = amountTotal / hasTotal;
		}

		
		PulverizacionItem ret = new PulverizacionItem();
		ret.setId(_id);
		ret.setAmount(amountTotal);		
		ret.setGeometry(GeometryHelper.unirGeometrias(geoms));
		
		return ret;
	}

public static LaborItem resumirSueloItems(List<SueloItem> aResumir) {
	//return ResumirSoilMapTask.resumirItems(catItems);
	SueloItem ret = new SueloItem();
	Double _id=null;

	Double hasTotal=Double.valueOf(0.0);
	Double elevacion=Double.valueOf(0.0);
	
	Double masaDeSuelo=Double.valueOf(0.0);
	Double kgP=Double.valueOf(0.0);
	Double kgN=Double.valueOf(0.0);
	Double kgK=Double.valueOf(0.0);
	Double kgS=Double.valueOf(0.0);
	Double kgMO=Double.valueOf(0.0);
	Double profNapa=Double.valueOf(0.0);
	Double mmAgua=Double.valueOf(0.0);
	
	Double porosidad = 0.0;
	Double porcCC = 0.0;//Capacidad de campo
	
	List<Geometry> geoms = new ArrayList<Geometry>(); 
	
	for(SueloItem item : aResumir) {
		Geometry g = item.getGeometry();
		Double hasItem = ProyectionConstants.A_HAS(g.getArea());
		if(_id==null) {
			_id=item.getId();
		}
		double dap = item.getDensAp();
		masaDeSuelo+=dap*hasItem;
		
		kgN += item.getPpmNO3()*dap*hasItem;
		kgP += item.getPpmP()*dap*hasItem;
		kgK += item.getPpmK()*dap*hasItem;
		kgS += item.getPpmS()*dap*hasItem;
		kgMO += item.getPorcMO()*dap*hasItem;
		profNapa+= item.getProfNapa()*hasItem;
		mmAgua += item.getAguaPerfil()*hasItem;			
		porosidad +=item.getPorosidad()*hasItem;
		porcCC +=item.getPorcCC()*hasItem;

		//TODO resumir otros elementos del suelo
		elevacion+=item.getElevacion()*hasItem;
		hasTotal+=hasItem;
		geoms.add(item.getGeometry());		
	}

	ret.setId(_id);
	
	//System.out.println("devolviendo un suelo item con id="+_id);
	ret.setDensAp(masaDeSuelo/hasTotal);
	ret.setPpmNO3(kgN/masaDeSuelo);
	ret.setPpmP(kgP/masaDeSuelo);
	ret.setPpmK(kgK/masaDeSuelo);
	ret.setPpmS(kgS/masaDeSuelo);
	ret.setPorcMO(kgMO/(hasTotal*ret.getDensAp()));
	ret.setProfNapa(mmAgua/hasTotal);
	ret.setAguaPerfil(mmAgua/hasTotal);
	ret.setPorosidad(porosidad/hasTotal);
	ret.setPorcCC(porcCC/hasTotal);
	
	ret.setElevacion(elevacion/hasTotal);
	ret.setGeometry(GeometryHelper.unirGeometrias(geoms));
	
	return ret;
}

public static LaborItem resumirMargenItems(List<MargenItem> aResumir) {
	//return  ResumirMargenMapTask.resumirItems(items);
	MargenItem ret = new MargenItem();
	Double _id=null;
	Double importeCosecha=Double.valueOf(0.0);
	Double importeFert=Double.valueOf(0.0);
	Double importePulv=Double.valueOf(0.0);
	Double importeSiembra=Double.valueOf(0.0);
	Double hasTotal=Double.valueOf(0.0);
	List<Geometry> geoms = new ArrayList<Geometry>(); 
	
	for(MargenItem item : aResumir) {
		Geometry g = item.getGeometry();
		Double hasItem = ProyectionConstants.A_HAS(g.getArea());
		if(_id==null) {
			_id=item.getId();
		}
		importeCosecha+=item.getImporteCosechaHa()*hasItem;
		importeFert+=item.getImporteFertHa()*hasItem;
		importePulv+=item.getImportePulvHa()*hasItem;
		importeSiembra+=item.getImporteSiembraHa()*hasItem;
		hasTotal+=hasItem;
		geoms.add(item.getGeometry());
		
		//TODO sumar todos los importes y dividirlos por la nueva superficie
	}
	importeCosecha = importeCosecha / hasTotal;
	importeFert = importeFert / hasTotal;
	importePulv = importePulv / hasTotal;
	importeSiembra = importeSiembra / hasTotal;
	
	ret.setId(_id);
	ret.setImporteCosechaHa(importeCosecha);		
	ret.setImporteFertHa(importeFert);
	ret.setImportePulvHa(importePulv);
	ret.setImporteSiembraHa(importeSiembra);
	
	ret.setGeometry(GeometryHelper.unirGeometrias(geoms));
	
	return ret;
}

	
	protected ExtrudedPolygon getPathTooltip( Geometry poly,LaborItem item,ExtrudedPolygon  renderablePolygon) {
		//double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();

		//String tooltipText2 = null;//tooltipCreator.get(this.labor.getClass()).apply(renta);
		String tooltipText2 = super.createTooltipForLaborItem(poly,item);
		//String tooltipText= buildTooltipText(renta,area);
		return super.getExtrudedPolygonFromGeom(poly, item,tooltipText2,renderablePolygon);
	//	super.getRenderPolygonFromGeom(poly, renta,tooltipText);
	}

	
	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}

}// fin del task

