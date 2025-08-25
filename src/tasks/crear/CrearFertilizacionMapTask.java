package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import dao.Poligono;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.utils.PropertyHelper;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.GeometryHelper;
import utils.ProyectionConstants;

public class CrearFertilizacionMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	Double amount = new Double(0);
	List<Poligono> polis=null;

	public CrearFertilizacionMapTask(FertilizacionLabor labor,List<Poligono> _polis,Double _amount){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(labor);
		amount=_amount;
		polis=_polis;

	}

	public void doProcess() throws IOException {
		//labor.setContorno(GeometryHelper.unirPoligonos(polis));
		//GeometryFactory fact = new GeometryFactory();
		for(Poligono pol : this.polis) {
			FertilizacionItem ci = new FertilizacionItem();
			ci.setDosistHa(amount);
//			ci.setPrecioInsumo(labor.precioInsumoProperty.get());
//			ci.setCostoLaborHa(labor.precioLaborProperty.get());
			labor.setPropiedadesLabor(ci);
			//dosis sembradora va en semillas cada 10mts
			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg

			ci.setGeometry(pol.toGeometry());
			ci.setId(labor.getNextID());
			ci.setElevacion(10.0);
			//labor.setNombre(poli.getNombre());
			labor.insertFeature(ci);
		}
				
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

	public static String buildTooltipText(FertilizacionItem fertFeature, double area) {
		String tooltipText = new String(
				Messages.getString("ProcessFertMapTask.2") + PropertyHelper.formatDouble(fertFeature.getDosistHa()) 
				+ Messages.getString("ProcessFertMapTask.3") + Messages.getString("ProcessFertMapTask.4") 
				+ PropertyHelper.formatDouble(fertFeature.getImporteHa()) + Messages.getString("ProcessFertMapTask.5") 
				);
		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessFertMapTask.6")
					+PropertyHelper.formatDouble(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessFertMapTask.7")); //$NON-NLS-1$ //$NON-NLS-2$
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessFertMapTask.8")
					+PropertyHelper.formatDouble(area ) + Messages.getString("ProcessFertMapTask.9")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return tooltipText;
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task