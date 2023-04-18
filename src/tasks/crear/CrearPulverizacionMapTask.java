package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.Poligono;
import dao.pulverizacion.PulverizacionItem;
import dao.pulverizacion.PulverizacionLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class CrearPulverizacionMapTask extends ProcessMapTask<PulverizacionItem,PulverizacionLabor> {
	Double amount = new Double(0);
	Poligono poli=null;

	public CrearPulverizacionMapTask(PulverizacionLabor labor,Poligono _poli,Double _amount){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(labor);
		amount=_amount;
		poli=_poli;
	}

	public void doProcess() throws IOException {
		labor.setContorno(poli);
		PulverizacionItem ci = new PulverizacionItem();
		ci.setDosis(amount);
		labor.setPropiedadesLabor(ci);
		GeometryFactory fact = new GeometryFactory();
		List<? extends Position> positions = poli.getPositions();
		Coordinate[] coordinates = new Coordinate[positions.size()];
		for(int i=0;i<positions.size();i++){
			Position p = positions.get(i);	
			Coordinate c = new Coordinate(p.getLongitude().getDegrees(),p.getLatitude().getDegrees(),p.getElevation());
			
			coordinates[i]=c;
		}
		coordinates[coordinates.length-1]=coordinates[0];//en caso de que la geometria no este cerrada
		Polygon poly = fact.createPolygon(coordinates);	

		ci.setGeometry(poly);
		
		labor.insertFeature(ci);
				
		labor.constructClasificador();

		
		runLater(this.getItemsList());;
		updateProgress(0, featureCount);

	}


	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, PulverizacionItem pulv,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();
		String tooltipText = CrearPulverizacionMapTask.buildTooltipText(pulv, area);
		return super.getExtrudedPolygonFromGeom(poly, pulv,tooltipText,renderablePolygon);
	}

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