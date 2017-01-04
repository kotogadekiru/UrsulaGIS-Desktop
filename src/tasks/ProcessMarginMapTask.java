package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.FeatureContainer;
import dao.Labor;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.margen.MargenItem;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraLabor;
import utils.ProyectionConstants;

public class ProcessMarginMapTask extends ProcessMapTask<MargenItem,Margen> {
	//	public Group map = new Group();

	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	Quadtree geometryTree = null;
	//Quadtree featureTree = null;

	private int featureCount;
	private int featureNumber;

	private List<FertilizacionLabor> fertilizaciones;
	private List<SiembraLabor> siembras;
	private List<CosechaLabor> cosechas;
	private List<PulverizacionLabor> pulverizaciones;
	//ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();
	Double costoFijoHa;

	public ProcessMarginMapTask(Margen margen, List<PulverizacionLabor> pulverizaciones, List<FertilizacionLabor> fertilizaciones, List<SiembraLabor> siembras,List<CosechaLabor> cosechas) {
		super(margen);
		this.fertilizaciones = fertilizaciones;
		this.pulverizaciones = pulverizaciones;
		this.siembras = siembras;
		this.cosechas = cosechas;

		this.costoFijoHa = margen.costoFijoHaProperty.getValue();

		System.out.println("inicializando ProcessMarginMapTask con costo Fijo = "+ costoFijoHa);
	}


	public void doProcess() throws IOException {
		//	this.featureTree = new Quadtree();
		featureNumber = 0;

		List<Polygon> geometryList = construirGrilla(getBounds());

		featureCount = geometryList.size();
		List<MargenItem> itemsToShow = new ArrayList<MargenItem>();
		geometryList.parallelStream().forEach(polygon->{	
			featureNumber++;
			updateProgress(featureNumber, featureCount);	
			MargenItem renta = createRentaForPoly(polygon);			
			if(renta.getCostoPorHa()>0||renta.getImporteCosechaHa()>0){
				labor.insertFeature(renta);
				itemsToShow.add(renta);
			}
		});

		labor.constructClasificador();
		runLater(itemsToShow);
		updateProgress(0, featureCount);	

	}


	private BoundingBox getBounds() {
		List<Labor<?>> labores = new ArrayList<Labor<?>>();
		labores.addAll(cosechas);
		labores.addAll(siembras);
		labores.addAll(fertilizaciones);
		labores.addAll(pulverizaciones);

		ReferencedEnvelope unionEnvelope = labores.stream()
				.filter((labor)->labor.layer.isEnabled())
				.map((l)->l.outCollection.getBounds())
				.reduce(new ReferencedEnvelope(),
						(e1,e2)->{
							e1.expandToInclude(e2);
							return e1;	
						});

		//new ReferencedEnvelope(),//new
		//(u1,u2)->,
		//(e1,e2)->e1.expandToInclude(e2)//combine
		//);

		return unionEnvelope;
	}


	public List<Polygon> construirGrilla(BoundingBox bounds) {
		System.out.println("construyendo grilla");
		List<Polygon> polygons = new ArrayList<Polygon>();
		//	 = getBounds();
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat;
		Double ancho=new CosechaConfig().getAnchoFiltroOutlayers()/4;
		for(int x=0;(minX+x*ancho)<maxX;x++){
			Double x0=minX+x*ancho;
			Double x1=minX+(x+1)*ancho;
			for(int y=0;(minY+y*ancho)<maxY;y++){
				Double y0=minY+y*ancho;
				Double y1=minY+(y+1)*ancho;


				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong, y0*ProyectionConstants.metersToLat); 
				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong, y0*ProyectionConstants.metersToLat);
				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong, y1*ProyectionConstants.metersToLat);
				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong, y1*ProyectionConstants.metersToLat);

				/**
				 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
				 * carro--B
				 * 
				 */
				Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
				// Empezar y terminar en
				// el mismo punto.
				// sentido antihorario

				//			GeometryFactory fact = X.getFactory();
				GeometryFactory fact = new GeometryFactory();


				//				DirectPosition upper = positionFactory.createDirectPosition(new double[]{-180,-90});
				//				DirectPosition lower = positionFactory.createDirectPosition(new double[]{180,90});
				//	Envelope envelope = geometryFactory.createEnvelope( upper, lower );

				LinearRing shell = fact.createLinearRing(coordinates);
				LinearRing[] holes = null;
				Polygon poly = new Polygon(shell, holes, fact);			
				polygons.add(poly);
			}
		}
		return polygons;
	}


	private MargenItem createRentaForPoly(Polygon harvestPolygon) {
		Double importeCosecha;
		Double importePulv;
		Double importeFert;
		Double importeSiembra;
		Double areaMargen = harvestPolygon.getArea() * ProyectionConstants.A_HAS;
		Double importeFijo = costoFijoHa*areaMargen;

		importeCosecha = getImporteCosecha(harvestPolygon);
		//System.out.println("ingreso por cosecha=" + importeCosecha);

		importePulv = getImportePulv(harvestPolygon);
		importeFert = getImporteFert(harvestPolygon);
		importeSiembra = getImporteSiembra(harvestPolygon);


		Double margenPorHa = (importeCosecha
				- importePulv - importeFert - importeSiembra-importeFijo)
				/ areaMargen;

		MargenItem renta = new MargenItem();
		renta.setGeometry(harvestPolygon);
		renta.setImportePulvHa(importePulv/ areaMargen);
		renta.setImporteFertHa(importeFert/ areaMargen);
		renta.setImporteSiembraHa(importeSiembra/ areaMargen);
		renta.setCostoFijoPorHa(costoFijoHa);
		renta.setImporteCosechaHa(importeCosecha/ areaMargen);
		renta.setMargenPorHa(margenPorHa);
		return renta;
	}


	private Double getImporteCosecha(Geometry geometry) {
		return getImporteLabores(geometry,cosechas);
	}
	private Double getImporteSiembra(Geometry geometry) {
		return getImporteLabores(geometry,siembras);
	}
	private Double getImporteFert(Geometry geometry) {
		return getImporteLabores(geometry,fertilizaciones);
	}
	private Double getImportePulv(Geometry geometry) {
		return getImporteLabores(geometry,pulverizaciones);
	}

	private Double getImporteLabores(Geometry geometry, List<? extends Labor<?>> labores){
		return labores.stream().filter((l)->l.getLayer().isEnabled())
				.mapToDouble((labor)->{
					List<? extends FeatureContainer> cItems = labor.outStoreQuery(geometry.getEnvelopeInternal());

					Double importeCosecha =	cItems.stream().mapToDouble((ci)->{
						Double costoHa = (Double) ci.getImporteHa();
						Geometry cosechaGeom = ci.getGeometry();
						Geometry interseccionGeom = geometry.intersection(cosechaGeom);// Computes a
						Double area = interseccionGeom.getArea() * ProyectionConstants.A_HAS;
						//importeCosecha+=costoHa*area;
						return costoHa*area;
					}).sum();

					return importeCosecha;
				}).sum();
	}


	// getPathFromGeom(importeFert/areaCosecha,importePulv/areaCosecha,importeSiembra/areaCosecha,importeCosechaPorHa,margenPorHa,
	// harvestPolygon);
	@Override
	protected void getPathTooltip( Geometry poly,MargenItem renta) {

	//	gov.nasa.worldwind.render.Polygon path = getPathFromGeom2D(poly, renta);

		double area = poly.getArea() * ProyectionConstants.A_HAS;// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();


		DecimalFormat df = new DecimalFormat("#.00");

		String tooltipText = new String(
				"Rentabilidad: "+ df.format(renta.getRentabilidadHa())+ "%\n\n" 
						+"Margen: "+ df.format(renta.getMargenPorHa())	+ "U$S/Ha\n" 
						+ "Costo: "	+ df.format(renta.getCostoPorHa())		+ "U$S/Ha\n\n"
						+ "Fertilizacion: "	+ df.format(renta.getImporteFertHa())+ "U$S/Ha\n" 
						+ "Pulverizacion: "	+ df.format(renta.getImportePulvHa())	+ "U$S/Ha\n"
						+ "Siembra: "	+ df.format(renta.getImporteSiembraHa())+ "U$S/Ha\n"
						+ "Fijo: "	+ df.format(renta.getCostoFijoPorHa())+ "U$S/Ha\n"
						+ "Cosecha: "	+ df.format(renta.getImporteCosechaHa()) + "U$S/Ha\n" 
						//		+ df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n"
						// +"feature: " + featureNumber
				);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

//		ArrayList<Object> ret = new ArrayList<Object>();
//		ret.add(path);
//		ret.add(tooltipText);
		//return ret;
		super.getPathFromGeom2D(poly, renta,tooltipText);
	}




	protected  int getAmountMin(){return 100;} 
	protected  int gerAmountMax() {return 500;}




}
