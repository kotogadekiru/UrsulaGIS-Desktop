package com.ursulagis.desktop.tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class CrearPulverizacionMapTask extends ProcessMapTask<PulverizacionItem,PulverizacionLabor> {
	Double amount = 0.0;
	List<Poligono> polis=null;
	//Poligono poli=null;

	public CrearPulverizacionMapTask(PulverizacionLabor labor,List<Poligono> _poli,Double _amount){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(labor);
		amount=_amount;
		//poli=_poli;
		polis=_poli;
	}

	public void doProcess() throws IOException {
		//labor.setContorno(poli);
		for(Poligono pol : this.polis) {
			PulverizacionItem pi = new PulverizacionItem();
			pi.setDosis(amount);
			labor.setPropiedadesLabor(pi);


			Geometry g = GeometryHelper.simplificarContorno(pol.toGeometry());
			pi.setGeometry(g);

			//ci.setGeometry(poly);
			pi.setId(labor.getNextID());
			pi.setElevacion(10.0);
			labor.insertFeature(pi);
		}
		labor.constructClasificador();


		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, PulverizacionItem pulv,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();
//		String tooltipText = CrearPulverizacionMapTask.buildTooltipText(pulv, area);
//		return super.getExtrudedPolygonFromGeom(poly, pulv,tooltipText,renderablePolygon);
//	}

	public static String buildTooltipText(PulverizacionItem pulv, double area) {
		NumberFormat nf = Messages.getNumberFormat();

		//DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$
		String tooltipText = new String(Messages.getString("ProcessPulvMapTask.1") //$NON-NLS-1$
				+Messages.getString("PulvConfigDialog.dosisLabel")+": "+nf.format(pulv.getDosis())+"\n"
				+ nf.format(pulv.getPrecioInsumo()*pulv.getDosis()) + Messages.getString("ProcessPulvMapTask.2") //$NON-NLS-1$
				+ Messages.getString("ProcessPulvMapTask.3") + nf.format(pulv.getImporteHa()) //$NON-NLS-1$
				+ Messages.getString("ProcessPulvMapTask.4")  //$NON-NLS-1$
				// +"feature: " + featureNumber
				);

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessPulvMapTask.5")+nf.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessPulvMapTask.6")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessPulvMapTask.7")+nf.format(area ) + Messages.getString("ProcessPulvMapTask.8")); //$NON-NLS-1$ //$NON-NLS-2$
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