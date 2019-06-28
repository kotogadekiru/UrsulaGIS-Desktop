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
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class CrearSueloMapTask extends ProcessMapTask<SueloItem,Suelo> {
	Double ppmP = new Double(0);
	Double ppmN = new Double(0);
	Double pMO = new Double(0);
	Poligono poli=null;

	public CrearSueloMapTask(Suelo labor,Poligono _poli,Double _amount, Double _ppmN, Double _pMO){
		super(labor);
		ppmP=_amount;
		ppmN=_ppmN;
		pMO=_pMO;
		poli=_poli;
		labor.setNombre(poli.getNombre());

	}

	public void doProcess() throws IOException {
		SueloItem si = new SueloItem();
		si.setPpmP(ppmP);
		si.setPpmN(ppmN);
		si.setPorcMO(pMO);

		labor.setPropiedadesLabor(si);
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

		si.setGeometry(poly);
		
		labor.insertFeature(si);
				
		labor.constructClasificador();

		List<SueloItem> itemsToShow = new ArrayList<SueloItem>();
		itemsToShow.add(si);
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}


	@Override
	public  ExtrudedPolygon  getPathTooltip( Geometry poly,SueloItem si) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String(
				Messages.getString("CrearSueloMapTask.fosforo")+": " +df.format(si.getPpmP()) +"Ppm\n"
				+Messages.getString("CrearSueloMapTask.nitrogeno")+": "+ df.format(si.getPpmN()) +"Ppm\n"
				);

		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat(Messages.getString("CrearSueloMapTask.sup")+": "+df.format(area ) + "Has\n");
		}

		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText);
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task