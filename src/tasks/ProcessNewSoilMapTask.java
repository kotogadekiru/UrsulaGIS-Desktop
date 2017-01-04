package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import javafx.beans.property.Property;
import javafx.scene.Group;
import javafx.scene.shape.Path;

import org.geotools.data.FileDataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.FilterFactory;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import utils.ProyectionConstants;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Suelo;
import dao.SueloItem;
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.MargenItem;
import dao.pulverizacion.PulverizacionItem;
import dao.siembra.SiembraItem;

public class ProcessNewSoilMapTask extends ProcessMapTask<Suelo>{
	//public Group map = new Group();

	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	private int featureCount;
	private int featureNumber;



	private Suelo nuevoSuelo;
	private List<Suelo> suelos;
	private List<CosechaLabor> cosechas;
	private List<FertilizacionLabor> fertilizaciones;


	public ProcessNewSoilMapTask(List<Suelo> suelos,List<CosechaLabor> cosechas,List<FertilizacionLabor> fertilizaciones) {
		this.suelos=suelos;
		this.fertilizaciones=fertilizaciones;
		this.cosechas =cosechas;
	
		
	}

	public void doProcess() throws IOException {
		//TODO establecer los bounds de los inputs
		//TODO crear una grilla cubriendo los bounds
		//TODO para cada item de la grilla calcular el balance de nutrientes y producir el nuevo suelo
		//TODO crear el clasificador
		//TODO crear los paths
		//TODO devolver el nuevo suelo




		



		featureNumber = 0;



		List<Geometry> grilla = new ArrayList<Geometry>();
		featureCount = grilla.size();
		Suelo nuevoSuelo = new Suelo();
		for(Geometry geometry :grilla){
				SueloItem sueloItem = createSueloForPoly(geometry);
			
				SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(SueloItem.getType());
				pathTooltips.add(0, getPathTooltip( (Polygon) geometry, sueloItem));
				nuevoSuelo.insertFeature(sueloItem.getFeature(featureBuilder));			
				updateProgress(featureNumber, featureCount);
		}
	

		//constructHistogram();



		runLater();


		updateProgress(0, featureCount);


	}

	private SueloItem createSueloForPoly(Geometry harvestPolygon) {	
		Double areaMargen = harvestPolygon.getArea()
				* ProyectionConstants.A_HAS;
		Double ppmSuelo = getPpmSuelo(harvestPolygon);
		System.out.println("cantFertilizante en el suelo= " + ppmSuelo);
		
		Double ppmFert = getPpmFertilizacion(harvestPolygon);
		System.out.println("cantFertilizante agregada= " + ppmFert);

		
		Double 	ppmCosecha = getPpmCosecha(harvestPolygon);
		System.out.println("cantFertilizante absorvida= " + ppmCosecha);



		Double margenPorHa = (ppmFert + ppmSuelo - ppmCosecha)
				/ areaMargen;

		SueloItem objSuelo = new SueloItem();
		objSuelo.setGeometry(harvestPolygon);
		objSuelo.setPpmP(margenPorHa);

		return objSuelo;
	}

	private double getPpmCosecha(Geometry geometry) {	
			double ppmCosechasGeom = 0.0;
		
			for(CosechaLabor cosecha:this.cosechas){				
				Cultivo producto = cosecha.producto.getValue();
			
				DoubleStream ppmPStream=	cosecha.outStoreQuery(geometry.getEnvelopeInternal()).stream().flatMapToDouble(cItem->{
					double costoHa = (Double) cItem.getRindeTnHa();

					Double ppmPabsorvida = costoHa * producto.getReqP();
					Geometry cosechaGeom = cItem.getGeometry();
					
						Geometry inteseccionGeom = geometry
								.intersection(cosechaGeom);// Computes a
															// Geometry

						Double area = inteseccionGeom.getArea()
								* ProyectionConstants.A_HAS;
						return DoubleStream.of( ppmPabsorvida * area);
				});					
				Double ppmCosecha = ppmPStream.sum();
				ppmCosechasGeom+=ppmCosecha;			
			}
		
		System.out.println("Las Ppm absorvidas de las cosechas"
				+ "correspondientes a la query son = "
						+ ppmCosechasGeom);
		return ppmCosechasGeom;
	}

	private Double getPpmSuelo(Geometry geometry) {
		Double importeSiembra = new Double(0);
		if (!(geometry instanceof Point) && soilTree != null) {
			@SuppressWarnings("rawtypes")
			List siembras = soilTree.query(geometry.getEnvelopeInternal());
			for (Object siembraObj : siembras) {
				if (siembraObj instanceof SueloItem) {

					SueloItem suelo = (SueloItem) siembraObj;
					Double costoHa = (Double) suelo.getPpmP();

					Object sueloGeom = suelo.getGeometry();
					if (sueloGeom instanceof Geometry) {
						Geometry pulvGeom = (Geometry) sueloGeom;
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
										+ sueloGeom.getClass());
					}
				} else {
					System.err.println("me perdi en getppmPSuelo");
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
				if (fertObj instanceof FertilizacionItem) {
					FertilizacionItem fert = (FertilizacionItem) fertObj;
					Double ppmP = (Double) fert.getCantFertHa()
							*
							fertilizante.getPpmP();

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


	private ArrayList<Object> getPathTooltip(Polygon poly, SueloItem objSuelo) {

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
