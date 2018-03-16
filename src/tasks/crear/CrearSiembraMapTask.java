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
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class CrearSiembraMapTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	Double amount = new Double(0);
	Poligono poli=null;

	public CrearSiembraMapTask(SiembraLabor labor,Poligono _poli,Double _amount){
		super(labor);
		amount=_amount;
		poli=_poli;

	}

	public void doProcess() throws IOException {
		SiembraItem ci = new SiembraItem();
		ci.setDosisHa(amount);

		labor.setPropiedadesLabor(ci);
		GeometryFactory fact = new GeometryFactory();
		List<? extends Position> positions = poli.getPositions();
		Coordinate[] coordinates = new Coordinate[positions.size()];
		for(int i=0;i<positions.size();i++){
			Position p = positions.get(i);	
			Coordinate c = new Coordinate(p.getLongitude().getDegrees(),p.getLatitude().getDegrees(),p.getElevation());
			
			coordinates[i]=c;
		}
	//	if(coordinates[0]!=coordinates[coordinates.length-1]){
			coordinates[coordinates.length-1]=coordinates[0];//en caso de que la geometria no este cerrada
	//	}
		Polygon poly = fact.createPolygon(coordinates);	

		ci.setGeometry(poly);
		
		labor.insertFeature(ci);
				
		labor.constructClasificador();

		List<SiembraItem> itemsToShow = new ArrayList<SiembraItem>();
		itemsToShow.add(ci);
		runLater(itemsToShow);
		updateProgress(0, featureCount);

	}


	@Override
	public  ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature) {
	//	Path path = getPathFromGeom(poly,siembraFeature);		
		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00"); 
		String tooltipText = new String(
				"Densidad: "+ df.format(siembraFeature.getDosisHa()) + " Bolsa/Ha\n\n"
				+"Costo: " + df.format(siembraFeature.getImporteHa()) + " U$S/Ha\n"				
			//	+"Sup: " +  df.format(area*ProyectionConstants.METROS2_POR_HA) + " m2\n"
		//		+"feature: " + featureNumber						
		);
		
		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText);
	//	return ret;		
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task