package tasks;

import gov.nasa.worldwind.render.ExtrudedPolygon;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.shape.Path;

import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

import utils.ProyectionConstants;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Configuracion;
import dao.CosechaItem;
import dao.Costos;
import dao.FertilizacionItem;
import dao.CosechaConfig;
import dao.PulverizacionItem;
import dao.RentabilidadItem;
import dao.Siembra;

public class ProcessMarginMapTask extends ProcessMapTask {
	//	public Group map = new Group();

	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	Quadtree geometryTree = null;
	//Quadtree featureTree = null;

	private int featureCount;
	private int featureNumber;

	private Quadtree fertTree;
	private Quadtree siembraTree;
	private Quadtree harvestTree;
	private Quadtree pulvTree;
	//ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();
	Double costoFijoHa;


	public ProcessMarginMapTask(FileDataStore store, Group map, Quadtree pulvTree, Quadtree fertTree,
			Quadtree siembraTree, Quadtree harvestTree) {
//		this.store = store;
//		this.layer = map;
		this.fertTree = fertTree;
		this.pulvTree = pulvTree;
		this.siembraTree = siembraTree;
		this.harvestTree = harvestTree;
		this.costoFijoHa = Costos.getInstance().costoFijoHaProperty.getValue();

		System.out.println("inicializando ProcessMarginMapTask con costo Fijo = "+ costoFijoHa);
	}


	public void doProcess() throws IOException {
	//	this.featureTree = new Quadtree();

	

		// TODO si harvestTree es null crear grilla adecuada y calcular los
		// costos
		//		@SuppressWarnings("unchecked")
		//		List<Cosecha> features = this.harvestTree.queryAll();
		// Iterator<SimpleFeature> featuresIterator = features.iterator();

	
		featureNumber = 0;

		List<RentabilidadItem> itemsByIndex = new ArrayList<RentabilidadItem>();
		List<RentabilidadItem> itemsByAmount = new ArrayList<RentabilidadItem>();
		List<Polygon> geometryList = null;
		if(Configuracion.getInstance().getGenerarMapaRentabilidadFromShp()){
			geometryList = new ArrayList<Polygon>();

			SimpleFeatureSource featureSource = store.getFeatureSource();
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();
			SimpleFeatureIterator featuresIterator = featureCollection.features();
			while (featuresIterator.hasNext()) {
				SimpleFeature simpleFeature = featuresIterator.next();
				Object geometry = simpleFeature.getDefaultGeometry();//cosehaItem.getGeometry();

				if (geometry instanceof Polygon) {		
					Polygon polygon = (Polygon) geometry;			
					geometryList.add(polygon);	
				} else if(geometry instanceof MultiPolygon){
					MultiPolygon multipolygon = (MultiPolygon) geometry;
					for(int indicePolygono=0; indicePolygono<multipolygon.getNumGeometries();indicePolygono++){
						Polygon p = (Polygon) multipolygon.getGeometryN(indicePolygono);
						geometryList.add(p);	
					}
				}
			}	
		}else{
			geometryList = construirGrilla(getBounds());
		}


		featureCount = geometryList.size();
		
		geometryList.parallelStream().forEach(polygon->{			
			RentabilidadItem renta = createRentaForPoly(polygon);			
			if(renta.getCostoPorHa()>0||renta.getImporteCosechaHa()>0){
			itemsByIndex.add(renta);
			featureTree.insert(polygon.getEnvelopeInternal(), renta);
			}

		});

		itemsByAmount.addAll(itemsByIndex);
		constructHistogram(itemsByAmount);

		for (RentabilidadItem rentaItem : itemsByIndex) {
			featureNumber++;
			updateProgress(featureNumber, featureCount);

			Geometry geometry = rentaItem.getGeometry();

			if (geometry instanceof MultiPolygon) {
				System.err.println("geometry is MultiPolygon");

			} else if (geometry instanceof Polygon) {				
				Polygon harvestPolygon = (Polygon) geometry;

				pathTooltips.add(
						0,
						getPathTooltip(harvestPolygon,rentaItem));

			} else if (geometry instanceof Geometry) {
				System.err.println("geometry is Geometry");

			} else if (geometry instanceof Point) {
				System.err.println("geometry is Point");

			} else {
				System.out.println("no se que es la geometry " + geometry);
			}

		}// fin del while

		runLater();

		// saveFeaturesToNewShp(destinationFeatures);
		updateProgress(0, featureCount);
		// System.out.println("min: (" + minX + "," + minY + ") max: (" + maxX
		// + "," + maxY + ")");

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


	private RentabilidadItem createRentaForPoly(Polygon harvestPolygon) {
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

		RentabilidadItem renta = new RentabilidadItem();
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
		//	cosehaItem.getImporteHa();

		Double importeCosecha = new Double(0);
		if (!(geometry instanceof Point) && harvestTree != null) {
			@SuppressWarnings("rawtypes")
			List cosechas = harvestTree.query(geometry.getEnvelopeInternal());
			for (Object cosechaObject : cosechas) {
				if (cosechaObject instanceof CosechaItem) {

					CosechaItem cosecha = (CosechaItem) cosechaObject;
					Double costoHa = (Double) cosecha.getImporteHa();


					Object cosechaGeomObject = cosecha.getGeometry();
					if (cosechaGeomObject instanceof Geometry) {
						Geometry cosechaGeom = (Geometry) cosechaGeomObject;
						try {
							Geometry inteseccionGeom = geometry
									.intersection(cosechaGeom);// Computes a
							// Geometry

							Double area = inteseccionGeom.getArea() * ProyectionConstants.A_HAS;
							importeCosecha += costoHa * area;
						} catch (Exception e) {
							System.out.println("Error al instersectar la geometria query con la geometria de la cosecha");
							e.printStackTrace();
						}
					} else {
						System.err
						.println("la geometria de cosecha no es de tipo Geometry es: "
								+ cosechaGeomObject.getClass());
					}
				} else {
					System.err.println("me perdi en getImporteCosecha");
				}
			}
		}
		System.out
		.println("el importe de la cosecha correspondiente a la query es = "
				+ importeCosecha);
		return importeCosecha;
	}

	private Double getImporteSiembra(Geometry geometry) {
		Double importeSiembra = new Double(0);
		if (!(geometry instanceof Point) && siembraTree != null) {
			@SuppressWarnings("rawtypes")
			List siembras = siembraTree.query(geometry.getEnvelopeInternal());
			for (Object siembraObj : siembras) {
				if (siembraObj instanceof Siembra) {

					Siembra siembra = (Siembra) siembraObj;
					Double costoHa = (Double) siembra.getImporteHa();
					
					Object pulvGeomObject = siembra.getGeometry();
					if (pulvGeomObject instanceof Geometry) {
						Geometry pulvGeom = (Geometry) pulvGeomObject;
						try {
							Geometry inteseccionGeom = geometry
									.intersection(pulvGeom);// Computes a
							// Geometry

							Double area = inteseccionGeom.getArea() * ProyectionConstants.A_HAS;
							importeSiembra += costoHa * area;
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						System.err
						.println("la geometria de fert no es de tipo Geometry es: "
								+ pulvGeomObject.getClass());
					}
				} else {
					System.err.println("me perdi en getImporteSiembra");
				}
			}
		}
		System.out
		.println("el importe de la siembra correspondiente a la cosecha es = "
				+ importeSiembra);
		return importeSiembra;
	}

	private Double getImporteFert(Geometry geometry) {
		Double importeFert = new Double(0);
		if (!(geometry instanceof Point) && fertTree != null) {
			@SuppressWarnings("rawtypes")
			List ferts = fertTree.query(geometry.getEnvelopeInternal());

			// System.out.println("encontre " + ferts.size()
			// + " fertilizaciones en contacto con " + geometry);
			for (Object fertObj : ferts) {
				if (fertObj instanceof FertilizacionItem) {			
					FertilizacionItem fert = (FertilizacionItem) fertObj;
					Double costoHa = (Double) fert.getImporteHa();		

					Object pulvGeomObject = fert.getGeometry();
					if (pulvGeomObject instanceof Geometry) {
						//	Double harvestArea = geometry.getArea();
						Geometry pulvGeom = (Geometry) pulvGeomObject;
						try {
							Geometry inteseccionGeom = geometry
									.intersection(pulvGeom);// Computes a
							// Geometry

							Double area = inteseccionGeom.getArea() * ProyectionConstants.A_HAS;
							importeFert += costoHa * area;
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						System.out
						.println("la geometria de fert no es de tipo Geometry es: "
								+ pulvGeomObject.getClass());
					}
				} else {
					System.out.println("me perdi en getImporteFert");
				}
			}
		}
		System.out
		.println("el importe de la fertilizacion correspondiente a la cosecha es = "
				+ importeFert);
		return importeFert;
	}

	private Double getImportePulv(Geometry geometry) {
		Double importePulv = new Double(0);
		if (!(geometry instanceof Point) && pulvTree != null) {
			@SuppressWarnings("rawtypes")
			List pulves = pulvTree.query(geometry.getEnvelopeInternal());

//			System.out.println("encontre " + pulves.size()
//					+ " pulverizaciones en contacto con " + geometry);
			for (Object pulvObj : pulves) {
				if (pulvObj instanceof PulverizacionItem) {
					// System.out
					// .println("calculando el costo de la pulverizacion: "
					// + pulvObj);
					PulverizacionItem pulv = (PulverizacionItem) pulvObj;
					Double costoHa = pulv.getImporteHa();
					//.getAttribute(COLUMNA_COSTO_HA_PULV);

				//	System.out
//					.println("el costo por ha de la pulverizacion que se cruza con la geometria es="
//							+ costoHa);

					Object pulvGeomObject = pulv.getGeometry();
					if (pulvGeomObject instanceof Geometry) {
						// Double harvestArea = geometry.getArea();
						Geometry pulvGeom = (Geometry) pulvGeomObject;
						try {
							Geometry inteseccionGeom = geometry
									.intersection(pulvGeom);// Computes a
							// Geometry

							Double area = inteseccionGeom.getArea() * ProyectionConstants.A_HAS;
							importePulv += costoHa * area;
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						System.out
						.println("la geometria de pulv no es de tipo Geometry es: "
								+ pulvGeomObject.getClass());
					}
				} else {
					System.out.println("me perdi en getImportePulv");
				}
			}
		}
		System.out
		.println("el importe de la pulverizacion correspondiente a la cosecha es = "
				+ importePulv);
		return importePulv;
	}

	// getPathFromGeom(importeFert/areaCosecha,importePulv/areaCosecha,importeSiembra/areaCosecha,importeCosechaPorHa,margenPorHa,
	// harvestPolygon);
	private ArrayList<Object> getPathTooltip( Polygon poly,RentabilidadItem renta) {

		gov.nasa.worldwind.render.Polygon path = getPathFromGeom2D(poly, renta);

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

		ArrayList<Object> ret = new ArrayList<Object>();
		ret.add(path);
		ret.add(tooltipText);
		return ret;
	}




	protected  int getAmountMin(){return 100;} 
	protected  int gerAmountMax() {return 500;}

}
