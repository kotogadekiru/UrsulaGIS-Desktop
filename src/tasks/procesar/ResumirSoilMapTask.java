package tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import gui.nww.LaborLayer;
import tasks.ProcessMapTask;
import tasks.crear.CrearSueloMapTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;


/**
 * 
 * @author quero
 * task que toma un store y devuelve una capa de suelo a partir del store
 */

public class ResumirSoilMapTask extends ProcessMapTask<SueloItem,Suelo> {
	Suelo aResumir=null;
	public ResumirSoilMapTask(Suelo _aResumir) {
		Suelo margen = new Suelo();
		margen.setLayer(new LaborLayer());
		this.labor=margen;
		aResumir=_aResumir;
		labor.setNombre(aResumir.getNombre()+Messages.getString("ResumirSoilMapTask.resumido"));
		//labor.setContorno(aResumir.getContorno());
//		labor.getCostoFijoHaProperty().setValue(aResumir.getCostoFijoHaProperty().getValue());
//		labor.getCostoFleteProperty().setValue(aResumir.getCostoFleteProperty().getValue());
//		labor.getCostoTnProperty().setValue(aResumir.getCostoTnProperty().getValue());
		labor.setClasificador(aResumir.getClasificador().clone());
	//	aResumir.getLayer().setEnabled(false);
	}
	
	public void doProcess() throws IOException {
		List<SueloItem> resumidas = resumirPorCategoria(this.labor);
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

	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si, ExtrudedPolygon renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor, si, area);//buildTooltipText(this.labor,si,area);

		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);		
	}
	
	/**
	 * metodo que toma la labor y devuelve una lista de los items agrupados por categoria
	 * @param labor
	 * @return
	 */
	private List<SueloItem> resumirPorCategoria(Suelo labor) {
		//TODO antes de proceder a dibujar las features
		//agruparlas por clase y hacer un buffer cero
		//luego crear un feature promedio para cada poligono individual
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		//XXX inicializo la lista de las features por categoria
		List<List<SueloItem>> itemsByCat = new ArrayList<List<SueloItem>>();
		for(int i=0;i<labor.clasificador.getNumClasses();i++){
			itemsByCat.add(i, new ArrayList<SueloItem>());
		}
		
		//XXX recorro las features y segun la categoria las voy asignando las features a cada lista de cada categoria
		SimpleFeatureIterator it = aResumir.outCollection.features();

		while(it.hasNext()){
			SimpleFeature f = it.next();
			SueloItem ci = aResumir.constructFeatureContainerStandar(f, false);
		
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());//LaborItem.getDoubleFromObj(f.getAttribute(labor.colRendimiento.get())));
			itemsByCat.get(cat).add(ci);
		}
		it.close();
		updateProgress(1, 100);
		
		
		// ahora que tenemos las colecciones con las categorias solo hace falta juntar las geometrias y sacar los promedios	
		List<SueloItem> itemsCategoria = new ArrayList<SueloItem>();//es la lista de los items que representan a cada categoria y que devuelvo
		//XXX por cada categoria 
		
			for(List<SueloItem> catItems : itemsByCat) {
				System.out.println("resumiendo "+catItems.size());
				if(catItems.size()>0) {
					itemsCategoria.add(resumirItems(catItems));
				}
			}
			
		System.out.println("items resumidos "+itemsCategoria.size());
		return itemsCategoria;
	}

	public static SueloItem resumirItems(List<SueloItem> aResumir) {
		SueloItem ret = new SueloItem();
		Double _id=null;

		Double hasTotal=new Double(0.0);
		Double elevacion=new Double(0.0);
		
		Double masaDeSuelo=new Double(0.0);
		Double kgP=new Double(0.0);
		Double kgN=new Double(0.0);
		Double kgK=new Double(0.0);
		Double kgS=new Double(0.0);
		Double kgMO=new Double(0.0);
		Double profNapa=new Double(0.0);
		Double mmAgua=new Double(0.0);
		
		Double porosidad = new Double(0);
		Double porcCC = new Double(0);//Capacidad de campo
		
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
	
			elevacion+=item.getElevacion()*hasItem;
			hasTotal+=hasItem;
			geoms.add(item.getGeometry());
			
			//TODO sumar todos los importes y dividirlos por la nueva superficie
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

	
	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 200;
	}
	
	public static void main(String[] args) {
		
		Double uno = new Double(1);
		
		Geometry g1 = GeometryHelper.constructPolygon(new Envelope(0d,1d,0d,1d));
		SueloItem s1 = new SueloItem();
		s1.setId(uno);
		s1.setGeometry(g1);
		s1.setDensAp(1300.0);
		s1.setPpmNO3(20d);
		s1.setPpmP(30d);
		s1.setPpmK(20d);
		s1.setPpmS(15d);
		s1.setPorcMO(2.5);
		s1.setProfNapa(1.5);
		s1.setAguaPerfil(40d);
		s1.setPorosidad(50d);
		s1.setPorcCC(50d);
		
		Double dos = new Double(3);
		Geometry g2 = GeometryHelper.constructPolygon(new Envelope(0.5,1d,0d,1d));
		SueloItem s2 = new SueloItem();
		s2.setId(dos);
		s2.setGeometry(g2);
		s2.setDensAp(1400.0);
		s2.setPpmNO3(40d);
		s2.setPpmP(50d);
		s2.setPpmK(40d);
		s2.setPpmS(25d);
		s2.setPorcMO(3d);		
		s2.setProfNapa(200d);
		s2.setAguaPerfil(100d);
		s2.setPorosidad(60d);
		s2.setPorcCC(70d);
		
		List<SueloItem> aResumir = new ArrayList<SueloItem>();
		aResumir.add(s1);
		aResumir.add(s2);
		
		SueloItem res = ResumirSoilMapTask.resumirItems(aResumir);
		System.out.println("has g1 "+ProyectionConstants.A_HAS(g1.getArea()));
		System.out.println("has g2 "+ProyectionConstants.A_HAS(g2.getArea()));
		System.out.println("has res "+ProyectionConstants.A_HAS(res.getGeometry().getArea()));
		System.out.println(s1.toString());
		System.out.println(s2.toString());
		System.out.println(res.toString());
	}

}// fin del task

