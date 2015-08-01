package tasks;

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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.CosechaItem;
import dao.Fertilizacion;
import dao.Fertilizante;
import dao.Producto;
import dao.Pulverizacion;
import dao.Rentabilidad;
import dao.Siembra;
import dao.Suelo;

public class ProcessNewSoilMapTask extends ProcessMapTask {
	//public Group map = new Group();

	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	Quadtree geometryTree = null;

	private int featureCount;
	private int featureNumber;

	private Quadtree soilTree;
	private Quadtree fertTree;

	private Quadtree harvestTree;
	private Producto producto;
	private Fertilizante fertilizante;

	// private Quadtree pulvTree;

	// ArrayList<ArrayList<Object>> pathTooltips = new
	// ArrayList<ArrayList<Object>>();

	public ProcessNewSoilMapTask(FileDataStore store, Group map, Quadtree soilTree,
			Quadtree fertTree, Quadtree harvestTree, Producto objProducto,Fertilizante fertilizante) {
		this.store = store;
		super.map = map;

		this.soilTree = soilTree;
		this.fertTree = fertTree;
		this.harvestTree = harvestTree;
		this.producto = objProducto;
		this.fertilizante = fertilizante;
	}

	public void doProcess() throws IOException {
		this.featureTree = new Quadtree();

		SimpleFeatureSource featureSource = store.getFeatureSource();

		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featuresIterator = featureCollection.features();

		// TODO si harvestTree es null crear grilla adecuada y calcular los
		// costos
		// @SuppressWarnings("unchecked")
		// List<Cosecha> features = this.harvestTree.queryAll();
		// Iterator<SimpleFeature> featuresIterator = features.iterator();

		featureCount = featureCollection.size();
		featureNumber = 0;

		List<Suelo> itemsByIndex = new ArrayList<Suelo>();
		List<Suelo> itemsByAmount = new ArrayList<Suelo>();

		// for (Cosecha cosehaItem : features) {
		while (featuresIterator.hasNext()) {
			SimpleFeature simpleFeature = featuresIterator.next();
			Object geometry = simpleFeature.getDefaultGeometry();// cosehaItem.getGeometry();

			// Double importeCosecha = new Double(0);
			// Double importePulv = new Double(0);
			// Double importeFert = new Double(0);
			// Double importeSiembra = new Double(0);

			if (geometry instanceof Polygon) {

				Polygon harvestPolygon = (Polygon) geometry;

				Suelo renta = createRentaForPoly(harvestPolygon);

				itemsByIndex.add(renta);
				featureTree.insert(harvestPolygon.getEnvelopeInternal(), renta);
			} else if (geometry instanceof MultiPolygon) {
				MultiPolygon multipolygon = (MultiPolygon) geometry;
				for (int indicePolygono = 0; indicePolygono < multipolygon
						.getNumGeometries(); indicePolygono++) {

					Polygon p = (Polygon) multipolygon
							.getGeometryN(indicePolygono);
					Suelo renta = createRentaForPoly(p);

					itemsByIndex.add(renta);
					featureTree.insert(p.getEnvelopeInternal(), renta);

				}
				System.out.println("geometry es Multipolygon");
			}
		}
		itemsByAmount.addAll(itemsByIndex);
		constructHistogram(itemsByAmount);

		for (Suelo sueloItem : itemsByIndex) {
			// simpleFeature = featuresIterator.next();

			featureNumber++;
			updateProgress(featureNumber, featureCount);

			// System.out.println("Feature " + featureNumber + " of "
			// + featureCount);

			Geometry geometry = sueloItem.getGeometry();

			// Double areaCosecha = geometry.getArea() *
			// ProyectionConstants.A_HAS;

			// System.out.println("harvest geometry = " + geometry);

			if (geometry instanceof MultiPolygon) {
				System.err.println("geometry is MultiPolygon");

			} else if (geometry instanceof Polygon) {
				Polygon harvestPolygon = (Polygon) geometry;

				// importePulv = getImportePulv(harvestPolygon);
				// importeFert = getImporteFert(harvestPolygon);
				// importeSiembra = getImporteSiembra(harvestPolygon);
				//
				// Double margenPorHa = (importeCosechaPorHa * areaCosecha
				// - importePulv - importeFert - importeSiembra)
				// / areaCosecha;
				//
				// Rentabilidad renta = new Rentabilidad();
				// renta.setGeometry(geometry);
				// renta.setImportePulvHa(importePulv/ areaCosecha);
				// renta.setImporteFertHa(importeFert/ areaCosecha);
				// renta.setImporteSiembraHa(importeSiembra/ areaCosecha);
				// renta.setImporteCosechaHa(importeCosechaPorHa);
				// renta.setMargenPorHa(margenPorHa);

				// System.out.println("El margen/ha para la cosecha "
				// + featureNumber + " es = " + margenPorHa);

				pathTooltips.add(0, getPathTooltip(harvestPolygon, sueloItem));

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

	private Suelo createRentaForPoly(Polygon harvestPolygon) {
		Double ppmCosecha;
		Double ppmFert;
		Double ppmSuelo;
		
		Double areaMargen = harvestPolygon.getArea()
				* ProyectionConstants.A_HAS;

		ppmCosecha = getPpmCosecha(harvestPolygon);
		System.out.println("ingreso por cosecha=" + ppmCosecha);

		// importePulv = getImportePulv(harvestPolygon);
		ppmFert = getPpmFertilizacion(harvestPolygon);
		ppmSuelo = getPpmSuelo(harvestPolygon);

		Double margenPorHa = (ppmFert + ppmSuelo - ppmCosecha)
				/ areaMargen;

		Suelo objSuelo = new Suelo();
		objSuelo.setGeometry(harvestPolygon);
		objSuelo.setPpmP(margenPorHa);

		return objSuelo;
	}

	private double getPpmCosecha(Geometry geometry) {
		// cosehaItem.getImporteHa();

		double importeCosecha = 0.0;
		if (!(geometry instanceof Point) && harvestTree != null) {
			@SuppressWarnings("rawtypes")
			List cosechas = harvestTree.query(geometry.getEnvelopeInternal());
			for (Object cosechaObject : cosechas) {
				if (cosechaObject instanceof CosechaItem) {

					CosechaItem cosecha = (CosechaItem) cosechaObject;
					double costoHa = (Double) cosecha.getRindeTnHa();

					Double ppmPabsorvida = costoHa * 
							producto.getReqP();
					
					Object cosechaGeomObject = cosecha.getGeometry();

					if (cosechaGeomObject instanceof Geometry) {
						Geometry cosechaGeom = (Geometry) cosechaGeomObject;
						try {
							Geometry inteseccionGeom = geometry
									.intersection(cosechaGeom);// Computes a
																// Geometry

							Double area = inteseccionGeom.getArea()
									* ProyectionConstants.A_HAS;
							importeCosecha += ppmPabsorvida * area;
						} catch (Exception e) {
							System.out
									.println("Error al instersectar la geometria query con la geometria de la cosecha");
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

	private Double getPpmSuelo(Geometry geometry) {
		Double importeSiembra = new Double(0);
		if (!(geometry instanceof Point) && soilTree != null) {
			@SuppressWarnings("rawtypes")
			List siembras = soilTree.query(geometry.getEnvelopeInternal());
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

							Double area = inteseccionGeom.getArea()
									* ProyectionConstants.A_HAS;
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

	private Double getPpmFertilizacion(Geometry geometry) {
		Double ppmPTotal = new Double(0);
		if (!(geometry instanceof Point) && fertTree != null) {
			@SuppressWarnings("rawtypes")
			List ferts = fertTree.query(geometry.getEnvelopeInternal());

			// System.out.println("encontre " + ferts.size()
			// + " fertilizaciones en contacto con " + geometry);
			for (Object fertObj : ferts) {
				if (fertObj instanceof Fertilizacion) {
					Fertilizacion fert = (Fertilizacion) fertObj;
					Double ppmP = (Double) fert.getCantFertHa()*fertilizante.getPpmP();

					Object pulvGeomObject = fert.getGeometry();
					if (pulvGeomObject instanceof Geometry) {
						// Double harvestArea = geometry.getArea();
						Geometry pulvGeom = (Geometry) pulvGeomObject;
						try {
							Geometry inteseccionGeom = geometry
									.intersection(pulvGeom);// Computes a
															// Geometry

							Double area = inteseccionGeom.getArea()
									* ProyectionConstants.A_HAS;
							ppmPTotal += ppmP * area;
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
						+ ppmPTotal);
		return ppmPTotal;
	}

	/*
	 * private Double getImportePulv(Geometry geometry) { Double importePulv =
	 * new Double(0); if (!(geometry instanceof Point) && pulvTree != null) {
	 * 
	 * @SuppressWarnings("rawtypes") List pulves =
	 * pulvTree.query(geometry.getEnvelopeInternal());
	 * 
	 * System.out.println("encontre " + pulves.size() +
	 * " pulverizaciones en contacto con " + geometry); for (Object pulvObj :
	 * pulves) { if (pulvObj instanceof Pulverizacion) { // System.out //
	 * .println("calculando el costo de la pulverizacion: " // + pulvObj);
	 * Pulverizacion pulv = (Pulverizacion) pulvObj; Double costoHa =
	 * pulv.getImporteHa(); // .getAttribute(COLUMNA_COSTO_HA_PULV);
	 * 
	 * System.out .println(
	 * "el costo por ha de la pulverizacion que se cruza con la geometria es=" +
	 * costoHa);
	 * 
	 * Object pulvGeomObject = pulv.getGeometry(); if (pulvGeomObject instanceof
	 * Geometry) { // Double harvestArea = geometry.getArea(); Geometry pulvGeom
	 * = (Geometry) pulvGeomObject; try { Geometry inteseccionGeom = geometry
	 * .intersection(pulvGeom);// Computes a // Geometry
	 * 
	 * Double area = inteseccionGeom.getArea() ProyectionConstants.A_HAS;
	 * importePulv += costoHa * area; } catch (Exception e) {
	 * e.printStackTrace(); } } else { System.out
	 * .println("la geometria de pulv no es de tipo Geometry es: " +
	 * pulvGeomObject.getClass()); } } else {
	 * System.out.println("me perdi en getImportePulv"); } } } System.out
	 * .println
	 * ("el importe de la pulverizacion correspondiente a la cosecha es = " +
	 * importePulv); return importePulv; }
	 */

	// getPathFromGeom(importeFert/areaCosecha,importePulv/areaCosecha,importeSiembra/areaCosecha,importeCosechaPorHa,margenPorHa,
	// harvestPolygon);
	private ArrayList<Object> getPathTooltip(Polygon poly, Suelo objSuelo) {

		Path path = getPathFromGeom(poly, objSuelo);

		double area = poly.getArea() * ProyectionConstants.A_HAS;// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String(

		" PpmFosforo/Ha: " + df.format(objSuelo.getPpmP()) + "\n"
		// + "Sup: "
		// + df.format(area * ProyectionConstants.METROS2_POR_HA)
		// + " m2\n"
		// +"feature: " + featureNumber
		);

		if (area < 1) {
			tooltipText = tooltipText.concat("Sup: "
					+ df.format(area * ProyectionConstants.METROS2_POR_HA)
					+ "m2\n");
		} else {
			tooltipText = tooltipText.concat("Sup: " + df.format(area)
					+ "Has\n");
		}

		ArrayList<Object> ret = new ArrayList<Object>();
		ret.add(path);
		ret.add(tooltipText);
		return ret;
	}

	protected int getAmountMin() {
		return 100;
	}

	protected int gerAmountMax() {
		return 500;
	}

}
