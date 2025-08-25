package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.Clasificador;
import dao.Poligono;
import dao.config.Semilla;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;


/**
 * task que convierte una cosecha a una fertilizacion
 * @author quero
 *
 */
public class ConvertirAFertilizacionTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	Map<String,Double[]> dosisMap = null;//new Double(0);
	CosechaLabor cosecha=null;

	public ConvertirAFertilizacionTask(CosechaLabor _cosecha,FertilizacionLabor labor,Map<String,Double[]> valores){
		super(labor);
		dosisMap=valores;
		cosecha=_cosecha;

	}

	public void doProcess() throws IOException {
		//labor.setContorno(cosecha.getContorno());
		
		//Semilla semilla = labor.getSemilla();
		//System.out.println("semilla es "+semilla);
		//double entresurco = labor.getEntreSurco();
		//double pmil = semilla.getPesoDeMil();
		//double pg = semilla.getPG();
		//double metrosLinealesHa = ProyectionConstants.METROS2_POR_HA/entresurco;//23809 a 0.42
		//System.out.println("metrosLinealesHa "+metrosLinealesHa);//metrosLinealesHa 52631.57894736842 ok!
		//double semillasHa = ProyectionConstants.METROS2_POR_HA*plantasM2Objetivo/pg;// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas

		
		//System.out.println("semillasMetroLineal "+semillasMetroLineal);//semillasMetroLineal 38.0 ok!
		//List<CosechaItem> cItems = new ArrayList<CosechaItem>();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
		Clasificador cl = cosecha.getClasificador();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);			
			String nombre = cl.getLetraCat(cl.getCategoryFor(ci.getRindeTnHa()));
			double dosis = dosisMap.get(nombre)[0];
			//double semillasHa = ProyectionConstants.METROS2_POR_HA*plantasM2Objetivo/pg;// si pg ==1 semillas= plantas. si pg es <1 => semillas>plantas
			//double semillasMetroLineal = semillasHa/metrosLinealesHa;//si es trigo va en plantas /m2 si es maiz o soja va en miles de plantas por ha
			
			FertilizacionItem si = new FertilizacionItem();
			
			si.setDosistHa(dosis);//1000semillas*1000gramos para pasar a kg/ha

		//	si.setDosisML(semillasMetroLineal);
			//dosis sembradora va en semillas cada 10mts
			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg
			
			labor.setPropiedadesLabor(si);

			si.setGeometry(ci.getGeometry());
			si.setId(labor.getNextID());
			si.setElevacion(10.0);
			labor.insertFeature(si);
		}
		reader.close();
		
//		for(Poligono pol : this.polis) {
//			SiembraItem si = new SiembraItem();
//					
//			si.setDosisHa(semillasHa*pmil/(1000*1000));//1000semillas*1000gramos para pasar a kg/ha
//
//			si.setDosisML(semillasMetroLineal);
//			//dosis sembradora va en semillas cada 10mts
//			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg
//			
//			labor.setPropiedadesLabor(si);
//
//			si.setGeometry(pol.toGeometry());
//			si.setId(labor.getNextID());
//
//			labor.insertFeature(si);
//		}
		labor.constructClasificador();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearFertilizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task