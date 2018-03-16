package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
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

		List<PulverizacionItem> itemsToShow = new ArrayList<PulverizacionItem>();
		itemsToShow.add(ci);
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}


	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, PulverizacionItem pulv) {
	//	Path path = getPathFromGeom(poly, pulv);

		double area = poly.getArea() * ProyectionConstants.A_HAS();

		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String("Costo Agroquimicos: "
				+ df.format(pulv.getDosis()) + " U$S/Ha\n"
				+ "Pulverizacion: " + df.format(pulv.getImporteHa())
				+ " U$S/Ha\n" 
		// +"feature: " + featureNumber
		);
		
		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		return super.getExtrudedPolygonFromGeom(poly, pulv,tooltipText);
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task