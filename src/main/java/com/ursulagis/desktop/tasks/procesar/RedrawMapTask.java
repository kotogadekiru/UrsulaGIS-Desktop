package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.margen.MargenItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionItem;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.ConvertirASiembraTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.tasks.crear.CrearFertilizacionMapTask;
import com.ursulagis.desktop.tasks.crear.CrearPulverizacionMapTask;
import com.ursulagis.desktop.tasks.crear.CrearSueloMapTask;
import com.ursulagis.desktop.tasks.importar.OpenMargenMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

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
