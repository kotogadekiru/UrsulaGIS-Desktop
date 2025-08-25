package tasks.procesar;

import java.io.IOException;

import com.vividsolutions.jts.geom.Geometry;

import dao.Labor;
import dao.LaborItem;
import dao.cosecha.CosechaItem;
import dao.fertilizacion.FertilizacionItem;
import dao.margen.MargenItem;
import dao.pulverizacion.PulverizacionItem;
import dao.siembra.SiembraItem;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.JFXMain;
import tasks.ProcessMapTask;
import tasks.crear.ConvertirASiembraTask;
import tasks.crear.CrearCosechaMapTask;
import tasks.crear.CrearFertilizacionMapTask;
import tasks.crear.CrearPulverizacionMapTask;
import tasks.crear.CrearSueloMapTask;
import tasks.importar.OpenMargenMapTask;
import utils.ProyectionConstants;

public class RedrawMapTask extends ProcessMapTask<LaborItem,Labor<LaborItem>>{
	public RedrawMapTask(Labor<LaborItem> cosechaLabor){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(cosechaLabor);
		labor.clearCache();
	}

	@Override
	protected void doProcess() throws IOException {
		runLater(this.getItemsList());		
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

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry p, LaborItem fc, ExtrudedPolygon renderablePolygon) {
//		double area = p.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = "";
//		if(fc instanceof CosechaItem) {
//			tooltipText = CrearCosechaMapTask.buildTooltipText((dao.cosecha.CosechaItem)fc, area);			
//		} else 	if(fc instanceof SiembraItem) {
//			tooltipText = ConvertirASiembraTask.buildTooltipText((SiembraItem)fc, area); 
//		} else 	if(fc instanceof FertilizacionItem) {
//			tooltipText = CrearFertilizacionMapTask.buildTooltipText((FertilizacionItem)fc, area); 
//		} else 	if(fc instanceof PulverizacionItem) {
//			tooltipText = CrearPulverizacionMapTask.buildTooltipText((PulverizacionItem)fc, area);
//		} else 	if(fc instanceof SueloItem) {
//			tooltipText = CrearSueloMapTask.buildTooltipText((SueloItem)fc, area);
//		}else 	if(fc instanceof MargenItem) {
//			tooltipText = OpenMargenMapTask.buildTooltipText((MargenItem)fc, area);
//		}
//		return super.getExtrudedPolygonFromGeom(p, fc,tooltipText,renderablePolygon);
//	}

	public static void redraw(Labor<LaborItem> l) {
		RedrawMapTask task = new RedrawMapTask( l);
		JFXMain.executorPool.execute(task);
	}
}
