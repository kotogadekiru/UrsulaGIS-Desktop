package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.geometry.BoundingBox;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.margen.MargenItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ProcessMarginMapTask extends ProcessMapTask<MargenItem,Margen> {
	//	public Group map = new Group();

	double distanciaAvanceMax = 0;
	double anchoMax = 0;

//	Quadtree geometryTree = null;
	//Quadtree featureTree = null;

	private int featureCount;
	private int featureNumber;

	private List<FertilizacionLabor> fertilizaciones;
	private List<SiembraLabor> siembras;
	private List<CosechaLabor> cosechas;
	private List<PulverizacionLabor> pulverizaciones;
	//ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();
	Double costoFijoHa;
	boolean showMargen = false;
	
	public ProcessMarginMapTask(Margen margen) {
		super(margen);
		this.fertilizaciones = margen.getFertilizaciones();
		this.pulverizaciones = margen.getPulverizaciones();
		this.siembras = margen.getSiembras();
		this.cosechas = margen.getCosechas();

		if(labor.outCollection!=null)labor.outCollection.clear();
		
		this.costoFijoHa = margen.costoFijoHaProperty.getValue();
		showMargen = Margen.COLUMNA_MARGEN.equals(labor.colAmount.get());
		System.out.println("showMargen es "+showMargen);
		System.out.println("inicializando ProcessMarginMapTask con costo Fijo = "+ costoFijoHa);
	}


	public void doProcess() throws IOException {
		//	this.featureTree = new Quadtree();
		featureNumber = 0;
		
		List<Labor<?>> labores = new LinkedList<Labor<?>>();
		labores.addAll(fertilizaciones);
		labores.addAll(pulverizaciones);
		labores.addAll(siembras);
		labores.addAll(cosechas);	
		updateProgress(1, 100);	
	System.out.println("Construyendo grilla labores");
		ReferencedEnvelope unionEnvelope = getBounds(labores);
		List<Polygon> grilla = construirGrilla(unionEnvelope);
		updateProgress(2, 100);	
		System.out.println("extrayendo geometrias activas");
		List<Geometry> geometriasActivas = labores.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(activas, labor) ->{		
					Geometry contorno = GeometryHelper.extractContornoGeometry(labor);
					activas.add(contorno);
//					@SuppressWarnings("unchecked")
//					List<LaborItem> features = (List<LaborItem>) labor.outStoreQuery(unionEnvelope);
//					activas.addAll(
//							features.parallelStream().collect(
//							()->new ArrayList<Geometry>(),
//							(list, f) -> list.add((Geometry) f.getGeometry()),
//							(env1, env2) -> env1.addAll(env2))
//							);
				},	(env1, env2) -> env1.addAll(env2));
		
		GeometryCollection activasCollection = GeometryHelper.toGeometryCollection(geometriasActivas);
		Geometry cover =  activasCollection.buffer(0);
		//intersectar la grilla con el contorno
		List<Geometry> grillaCover = grilla.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(activas, poly) ->{					
					Geometry intersection = GeometryHelper.getIntersection(poly, cover); 
					if(intersection!=null) {
						activas.add(intersection);
					}
				},	(env1, env2) -> env1.addAll(env2));
		
		
		featureCount = grillaCover.size();
	//	List<MargenItem> itemsToShow = new ArrayList<MargenItem>();)
		List<MargenItem> itemsToShow = 	grillaCover.parallelStream().collect(	
				() -> new  LinkedList<MargenItem>(),
				(list, polygon) -> {//	).forEach(polygon->{	
			featureNumber++;
			updateProgress(featureNumber, featureCount);	
			MargenItem renta = createRentaForPoly(polygon);			
			if(renta.getImporteFertHa()>0 ||
					renta.getImportePulvHa()>0 ||
					renta.getImporteSiembraHa()>0 ||
					renta.getImporteCosechaHa() != 0){//solo lo descarto si no tienen costos variables
				labor.insertFeature(renta);
				list.add(renta);
			}
		},
		(list1, list2) -> list1.addAll(list2));

		pulverizaciones.forEach((l)->{
			l.clearCache();
			l.getLayer().setEnabled(false);
		});
		fertilizaciones.forEach((l)->{
			l.clearCache();
			l.getLayer().setEnabled(false);
		});
		cosechas.forEach((l)->{
			l.clearCache();
			l.getLayer().setEnabled(false);
		});
		siembras.forEach((l)->{
			l.clearCache();
			l.getLayer().setEnabled(false);
		});
		System.out.println("clasificador antes de constructClasificador es "+labor.clasificador.tipoClasificadorProperty.get());
		labor.constructClasificador();//labor.clasificador.tipoClasificadorProperty.get());
		runLater(itemsToShow);
		updateProgress(0, featureCount);	

	}


	private ReferencedEnvelope getBounds(List<Labor<?>> labores) {
//		List<Labor<?>> labores = new ArrayList<Labor<?>>();
//		labores.addAll(cosechas);
//		labores.addAll(siembras);
//		labores.addAll(fertilizaciones);
//		labores.addAll(pulverizaciones);

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

	/**
	 * 
	 * @param bounds en long/lat
	 * @param ancho en metros
	 * @return una lista de poligonos que representa una grilla con un 100% de superposiocion 10mts de ancho
	 */
	private List<Polygon> construirGrilla(BoundingBox bounds) {
		Double ancho=10d;//new CosechaConfig().getAnchoFiltroOutlayers()/3;
		
		System.out.println("construyendo grilla");
		List<Polygon> polygons = new ArrayList<Polygon>();
		//convierte los bounds de longlat a metros
		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong() - ancho/2;
		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat() - ancho/2;
		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong() + ancho/2;
		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat() + ancho/2;
		Double x0=minX;
		for(int x=0;(x0)<maxX;x++){
			x0=minX+x*ancho;
			Double x1=minX+(x+1)*ancho;
			for(int y=0;(minY+y*ancho)<maxY;y++){
				Double y0=minY+y*ancho;
				Double y1=minY+(y+1)*ancho;


				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());

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
	
//	public List<Polygon> construirGrilla(BoundingBox bounds) {
//		System.out.println("construyendo grilla");
//		List<Polygon> polygons = new ArrayList<Polygon>();
//		//	 = getBounds();
//		Double minX = bounds.getMinX()/ProyectionConstants.metersToLong();
//		Double minY = bounds.getMinY()/ProyectionConstants.metersToLat();
//		Double maxX = bounds.getMaxX()/ProyectionConstants.metersToLong();
//		Double maxY = bounds.getMaxY()/ProyectionConstants.metersToLat();
//		Double ancho=new CosechaConfig().getAnchoFiltroOutlayers()/4;
//		for(int x=0;(minX+x*ancho)<maxX;x++){
//			Double x0=minX+x*ancho;
//			Double x1=minX+(x+1)*ancho;
//			for(int y=0;(minY+y*ancho)<maxY;y++){
//				Double y0=minY+y*ancho;
//				Double y1=minY+(y+1)*ancho;
//
//
//				Coordinate D = new Coordinate(x0*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat()); 
//				Coordinate C = new Coordinate(x1*ProyectionConstants.metersToLong(), y0*ProyectionConstants.metersToLat());
//				Coordinate B = new Coordinate(x1*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
//				Coordinate A =  new Coordinate(x0*ProyectionConstants.metersToLong(), y1*ProyectionConstants.metersToLat());
//
//				/**
//				 * D-- ancho de carro--C ^ ^ | | avance ^^^^^^^^ avance | | A-- ancho de
//				 * carro--B
//				 * 
//				 */
//				Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
//				// Empezar y terminar en
//				// el mismo punto.
//				// sentido antihorario
//
//				//			GeometryFactory fact = X.getFactory();
//				GeometryFactory fact = new GeometryFactory();
//
//
//				//				DirectPosition upper = positionFactory.createDirectPosition(new double[]{-180,-90});
//				//				DirectPosition lower = positionFactory.createDirectPosition(new double[]{180,90});
//				//	Envelope envelope = geometryFactory.createEnvelope( upper, lower );
//
//				LinearRing shell = fact.createLinearRing(coordinates);
//				LinearRing[] holes = null;
//				Polygon poly = new Polygon(shell, holes, fact);			
//				polygons.add(poly);
//			}
//		}
//		return polygons;
//	}


	private MargenItem createRentaForPoly(Geometry g) {
		Double importeCosecha;
		Double importePulv;
		Double importeFert;
		Double importeSiembra;
		Double areaMargen = g.getArea() * ProyectionConstants.A_HAS();
		Double importeFijo = costoFijoHa*areaMargen;

		importeCosecha = getImporteCosecha(g);
		//System.out.println("ingreso por cosecha=" + importeCosecha);

		importePulv = getImportePulv(g);
		importeFert = getImporteFert(g);
		importeSiembra = getImporteSiembra(g);
		


		Double margenPorHa = (importeCosecha
				- importePulv - importeFert - importeSiembra-importeFijo)
				/ areaMargen;

		MargenItem renta = new MargenItem();
		synchronized(labor){
			//c = new CosechaItem();
			renta.setId(labor.getNextID());
		}
		renta.setGeometry(g);
		renta.setImportePulvHa(importePulv/ areaMargen);
		renta.setImporteFertHa(importeFert/ areaMargen);
		renta.setImporteSiembraHa(importeSiembra/ areaMargen);
		renta.setCostoFijoPorHa(costoFijoHa);
		renta.setImporteCosechaHa(importeCosecha/ areaMargen);
		renta.setMargenPorHa(margenPorHa);
		
		renta.setShowMargen(showMargen);
		renta.setAncho(0d);
		renta.setDistancia(0d);
		renta.setElevacion(1d);
		renta.setRumbo(0d);
		return renta;
	}


	private Double getImporteCosecha(Geometry geometry) {
		//TODO agregar al importe de cosecha el importe de flete y comercializacion
		double fletePorTN =this.labor.costoFleteProperty.getValue().doubleValue();
		double costoTN = this.labor.costoTnProperty.getValue().doubleValue();
		//double cantidadCosechas = getCantidadLabores(geometry,cosechas);
		
		return getImporteLabores(geometry,cosechas,(-fletePorTN-costoTN));
	}
	private Double getImporteSiembra(Geometry geometry) {
		return getImporteLabores(geometry,siembras,null);
	}
	private Double getImporteFert(Geometry geometry) {
		return getImporteLabores(geometry,fertilizaciones,null);
	}
	private Double getImportePulv(Geometry geometry) {
		return getImporteLabores(geometry,pulverizaciones,null);
	}

	
	private Double getImporteLabores(Geometry geometry, List<? extends Labor<?>> labores,Double costoVariable){
		Double ret =  labores.stream().filter((l)->l.getLayer().isEnabled())
				.mapToDouble((labor)->{
					List<? extends LaborItem> lItems = labor.cachedOutStoreQuery(geometry.getEnvelopeInternal());
					//List<? extends LaborItem> lItems = labor.cachedOutStoreQueryOLD(geometry.getEnvelopeInternal());
					
					Double importeLabor =	lItems.stream().mapToDouble((li)->{
						Geometry query = geometry;
						
						Double costoHa = (Double) li.getImporteHa();
						Geometry liGeom = li.getGeometry();
						//getAmount es el amount/ha
						Double costoVariableHa = costoVariable!=null?li.getAmount()*costoVariable:0.0;
						Geometry interseccionGeom =getIntersection(liGeom,query);// geometry.intersection(liGeom);// Computes a
						if(interseccionGeom == null)return 0;
						Double area = interseccionGeom.getArea() * ProyectionConstants.A_HAS();
						if(area>0){
							return (costoHa+costoVariableHa)*area;
						}
						//importeCosecha+=costoHa*area;
//						System.out.println("importeHa:"+costoHa
//								+"\ncantHa:"+li.getAmount()
//								+"\ncostoVariable:"+costoVariable
//								+"\ncantHa*costoVariable:"+costoVariableHa
//								+"\nimporteHa+importeVariable:"+(costoHa+costoVariableHa));
						return 0.0;
					}).sum();

					return importeLabor;
				}).sum();
		return ret;
	}
	
	private Geometry getIntersection(Geometry g, Geometry g2){
		Geometry intersection = GeometryHelper.getIntersection(g, g2);
//	g=makeGood(g);
//	g2=makeGood(g2);
//		Geometry intersection =g;
//		try {			
//			if (g2 != null //&& g2.isValid()
//					){
//				Geometry polyG =g;
//				intersection = polyG.intersection(g2);// Computes a Geometry//found non-noded intersection between LINESTRING ( -61.9893807883
//			}
//			intersection = makeGood(intersection);
//			// .out.println("tarde "+(fin-init)+" milisegundos en insertar");
//		} catch (Exception te) {//org.locationtech.jts.geom.TopologyException: found non-noded intersection between LINESTRING ( -62.008963817544945 -33.872771283412874, -62.0089897286684 -33.87272023486772 ) and LINESTRING ( -62.00902014621838 -33.87282073965304, -62.00893906917324 -33.872626066467696 ) [ (-62.008983444498256, -33.87273261556376, NaN) ]
//			try{//
//				intersection = EnhancedPrecisionOp.difference(g, g2);
//			}catch(Exception e){
//			//	e.printStackTrace();
//				intersection=null;
//			}
//		}
		return intersection;
	}
	
	private Double getCantidadLabores(Geometry geometry, List<? extends Labor<?>> labores){
		Double ret =  labores.stream().filter((l)->l.getLayer().isEnabled())
				.mapToDouble((labor)->{
					List<? extends LaborItem> cItems = labor.cachedOutStoreQuery(geometry.getEnvelopeInternal());

					Double importeCosecha =	cItems.stream().mapToDouble((ci)->{
						Double costoHa = (Double) ci.getAmount();
						Geometry cosechaGeom = ci.getGeometry();
						Geometry interseccionGeom = geometry.intersection(cosechaGeom);// Computes a
						Double area = interseccionGeom.getArea() * ProyectionConstants.A_HAS();
						//importeCosecha+=costoHa*area;
						return costoHa*area;
					}).sum();

					return importeCosecha;
				}).sum();
		return ret;
	}


	// getPathFromGeom(importeFert/areaCosecha,importePulv/areaCosecha,importeSiembra/areaCosecha,importeCosechaPorHa,margenPorHa,
	// harvestPolygon);
//	@Override
//	protected ExtrudedPolygon getPathTooltip( Geometry poly,MargenItem renta,ExtrudedPolygon  renderablePolygon) {
//
//	//	gov.nasa.worldwind.render.Polygon path = getPathFromGeom2D(poly, renta);
//
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//
//
////		DecimalFormat df = new DecimalFormat("0.00");
////		df.setGroupingSize(3);
////		
////		df.setGroupingUsed(true);
////
////		String tooltipText = new String(
////				"Rentabilidad: "+ df.format(renta.getRentabilidadHa())+ "%\n\n" 
////						+"Margen: "+ df.format(renta.getMargenPorHa())	+ "U$S/Ha\n" 
////						+ "Costo: "	+ df.format(renta.getCostoPorHa())		+ "U$S/Ha\n\n"
////						+ "Fertilizacion: "	+ df.format(renta.getImporteFertHa())+ "U$S/Ha\n" 
////						+ "Pulverizacion: "	+ df.format(renta.getImportePulvHa())	+ "U$S/Ha\n"
////						+ "Siembra: "	+ df.format(renta.getImporteSiembraHa())+ "U$S/Ha\n"
////						+ "Fijo: "	+ df.format(renta.getCostoFijoPorHa())+ "U$S/Ha\n"
////						+ "Cosecha: "	+ df.format(renta.getImporteCosechaHa()) + "U$S/Ha\n" 
////						//		+ df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n"
////						// +"feature: " + featureNumber
////				);
////
////		if(area<1){
////			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
////		} else {
////			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
////		}
//
////		ArrayList<Object> ret = new ArrayList<Object>();
////		ret.add(path);
////		ret.add(tooltipText);
//		//return ret;
//		NumberFormat df = Messages.getNumberFormat();
//
//		String tooltipText = new String(
//				Messages.getString("OpenMargenMapTask.1")+ df.format(renta.getRentabilidadHa())+ Messages.getString("OpenMargenMapTask.2")  //$NON-NLS-1$ //$NON-NLS-2$
//						+Messages.getString("OpenMargenMapTask.3")+ df.format(renta.getMargenPorHa())	+ Messages.getString("OpenMargenMapTask.4")  //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.5")	+ df.format(renta.getCostoPorHa())		+ Messages.getString("OpenMargenMapTask.6") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.7")	+ df.format(renta.getImporteFertHa())+ Messages.getString("OpenMargenMapTask.8")  //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.9")	+ df.format(renta.getImportePulvHa())	+ Messages.getString("OpenMargenMapTask.10") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.11")	+ df.format(renta.getImporteSiembraHa())+ Messages.getString("OpenMargenMapTask.12") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.13")	+ df.format(renta.getCostoFijoPorHa())+ Messages.getString("OpenMargenMapTask.14") //$NON-NLS-1$ //$NON-NLS-2$
//						+ Messages.getString("OpenMargenMapTask.15")	+ df.format(renta.getImporteCosechaHa()) + Messages.getString("OpenMargenMapTask.16")  //$NON-NLS-1$ //$NON-NLS-2$
//						//		+ df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n"
//						// +"feature: " + featureNumber
//				);
//
//		if(area<1){
//			tooltipText=tooltipText.concat( Messages.getString("OpenMargenMapTask.17")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("OpenMargenMapTask.18")); //$NON-NLS-1$ //$NON-NLS-2$
//		} else {
//			tooltipText=tooltipText.concat(Messages.getString("OpenMargenMapTask.19")+df.format(area ) + Messages.getString("OpenMargenMapTask.20")); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		return super.getExtrudedPolygonFromGeom(poly, renta,tooltipText,renderablePolygon);
//	}




	protected  int getAmountMin(){return 100;} 
	protected  int gerAmountMax() {return 500;}




}
