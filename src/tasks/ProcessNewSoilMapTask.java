package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.DoubleStream;

import javafx.beans.property.Property;
import javafx.scene.Group;
import javafx.scene.shape.Path;
import mmg.gui.nww.LaborLayer;

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

import dao.Labor;
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.MargenItem;
import dao.pulverizacion.PulverizacionItem;
import dao.siembra.SiembraItem;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;

public class ProcessNewSoilMapTask extends ProcessMapTask<SueloItem,Suelo> {
	//public Group map = new Group();

	
	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	private int featureCount;
	private int featureNumber;



	//private Suelo nuevoSuelo;
	private List<Suelo> suelos;
	private List<CosechaLabor> cosechas;
	private List<FertilizacionLabor> fertilizaciones;


	public ProcessNewSoilMapTask(List<Suelo> suelos,List<CosechaLabor> cosechas,List<FertilizacionLabor> fertilizaciones) {
		this.suelos=suelos;
		this.fertilizaciones=fertilizaciones;
		this.cosechas =cosechas;

		Suelo suelo = new Suelo();
		suelo.setLayer(new LaborLayer());
		StringBuilder sb = new StringBuilder();
		sb.append("Balance P ");
		cosechas.forEach((c)->sb.append(c.getNombreProperty().get()+" "));
		suelo.getNombreProperty().setValue(sb.toString());
		suelo.setLayer(new LaborLayer());
		super.labor=suelo;

	}

	public void doProcess() throws IOException {
		//TODO establecer los bounds de los inputs
		//TODO crear una grilla cubriendo los bounds
		//TODO para cada item de la grilla calcular el balance de nutrientes y producir el nuevo suelo
		//TODO crear el clasificador
		//TODO crear los paths
		//TODO devolver el nuevo suelo
		featureNumber = 0;

		double ancho = labor.getConfigLabor().getAnchoGrilla();

		List<Labor<?>> labores = new LinkedList<Labor<?>>();
		labores.addAll(cosechas);
		labores.addAll(fertilizaciones);
		labores.addAll(suelos);
		ReferencedEnvelope unionEnvelope = labores.parallelStream().collect(
				()->new ReferencedEnvelope(),
				(env, labor) ->{		
					ReferencedEnvelope b = labor.outCollection.getBounds();
					env.expandToInclude(b);
				},	(env1, env2) -> env1.expandToInclude(env2));


		
		//TODO 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);


		featureCount = grilla.size();
		List<SueloItem> itemsToShow = new ArrayList<SueloItem>();
		for(Geometry geometry :grilla){//TODO usar streams paralelos
			SueloItem sueloItem = createSueloForPoly(geometry);

			//	SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(SueloItem.getType());
			//pathTooltips.add(0, getPathTooltip( (Polygon) geometry, sueloItem));
			//	nuevoSuelo.insertFeature(sueloItem.getFeature(featureBuilder));			
			if(sueloItem!=null){
				labor.insertFeature(sueloItem);
				itemsToShow.add(sueloItem);
			}
			featureNumber++;
			updateProgress(featureNumber, featureCount);
		}

		for(Labor<?> c:labores){
			c.clearCache();
		}

		//		List<SueloItem> itemsToShow = new ArrayList<SueloItem>();
		//		SimpleFeatureIterator it = labor.outCollection.features();
		//		while(it.hasNext()){
		//			SimpleFeature f=it.next();
		//			itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
		//		}
		//		it.close();
		labor.constructClasificador();
		runLater(itemsToShow);
		updateProgress(0, featureCount);


	}
private SueloItem createSueloForPoly(Geometry geomQuery) {	
		Double areaQuery =ProyectionConstants.A_HAS( geomQuery.getArea());
		//	* ProyectionConstants.A_HAS();
		Double kgPSuelo = getKgPSuelo(geomQuery);	
		Double kgPFert = getKgPFertilizacion(geomQuery);
		Double 	kgPCosecha = getPpmCosecha(geomQuery);
		


		if(kgPFert==0&&kgPSuelo==0&& kgPCosecha==0)return null;//descarto los que no tienen valores
		Double newKgHaPSuelo = (kgPFert + kgPSuelo - kgPCosecha)
				/ areaQuery;

		System.out.println("\ncantFertilizante en el suelo= " + kgPSuelo);
		System.out.println("cantFertilizante agregada= " + kgPFert);
		System.out.println("cantFertilizante absorvida= " + kgPCosecha);
		System.out.println("newKgHaPSuelo = "+newKgHaPSuelo);
		Double newPpmPsuelo=newKgHaPSuelo/labor.getDensidad();
		System.out.println("newPpmPsuelo = "+newPpmPsuelo);
		
		SueloItem sueloItem = new SueloItem();
		synchronized(labor){
			//fi= new FertilizacionItem();					
			sueloItem.setId(labor.getNextID());
		}
		
		sueloItem.setGeometry(geomQuery);
		sueloItem.setPpmP(newPpmPsuelo);
		sueloItem.setElevacion(10.0);//para que aparezca en el mapa


		return sueloItem;
	}

	private double getPpmCosecha(Geometry geometry) {
		Double kgPCosecha = new Double(0);
		kgPCosecha = cosechas.parallelStream().flatMapToDouble(cosecha->{
			List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Cultivo cultivo =cosecha.producto.getValue();
			return items.parallelStream().flatMapToDouble(item->{
				//double rindeItem = (Double) item.getRindeTnHa();
				//double extraccionP = item.getRindeTnHa()*cultivo.getExtP();
				Double kgPAbsHa = item.getRindeTnHa()*cultivo.getExtP();//rindeItem * cultivo.getExtP();//solo me interesa reponer lo que extraje
				Geometry pulvGeom = item.getGeometry();				
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = geometry.intersection(pulvGeom);// Computes a
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgPAbsHa * area);				
			});
		}).sum();

		//		double ppmCosechasGeom = 0.0;
		//
		//		for(CosechaLabor cosecha:this.cosechas){				
		//			Cultivo producto = cosecha.producto.getValue();
		//
		//			DoubleStream ppmPStream=cosecha.outStoreQuery(geometry.getEnvelopeInternal()).stream().flatMapToDouble(cItem->{
		//				double rindeItem = (Double) cItem.getRindeTnHa();
		//
		//				Double ppmPabsorvida = rindeItem * producto.getAbsP();
		//				Geometry cosechaGeom = cItem.getGeometry();
		//
		//				Geometry inteseccionGeom = geometry
		//						.intersection(cosechaGeom);// Computes a
		//				// Geometry
		//
		//				Double area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
		//
		//				return DoubleStream.of( ppmPabsorvida * area);
		//			});					
		//			Double ppmCosecha = ppmPStream.sum();
		//			ppmCosechasGeom+=ppmCosecha;			
		//		}

//		System.out.println("Los kg P absorvidos por las cosechas"
//				+ "correspondientes a la query son = "
//				+ kgPCosecha);
		return kgPCosecha;
	}

	//busco todos los items de los mapas de suelo
	//calculo cuantas ppm aporta al cultivo,
	//las sumo
	//y devuelve la suma de las ppm existentes en el suelo para esa geometria
	//XXX tener en cuenta que ppm es una unidad de concentracion y no creo que se puedad sumar directamente. 
	//habria que pasar a gramos o promediar por la superficie total
	private Double getKgPSuelo(Geometry geometry) {
		Double kgPSuelo = new Double(0);
		kgPSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{
				Double kgPHa= (Double) item.getPpmP()*suelo.getDensidad();//TODO multiplicar por la densidad del suelo
				Geometry geom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = geometry.intersection(geom);// Computes a
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgPHa * area);				
			});
		}).sum();
//		System.out
//		.println("la cantidad de ppmP acumulado en el suelo es = "
//				+ kgPSuelo);
		return kgPSuelo;
	}

	private Double getKgPFertilizacion(Geometry geometry) {
		Double kPFert = new Double(0);
		kPFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Fertilizante fertilizante = fertilizacion.fertilizante.getValue();
			return items.parallelStream().flatMapToDouble(item->{
				Double kgPHa = (Double) item.getCantFertHa() * fertilizante.getPorcP()/100;
				Geometry fertGeom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = geometry.intersection(fertGeom);// Computes a
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgPHa * area);				
			});
		}).sum();
//		System.out.println("la cantidad de ppmP acumulado en el suelo es = "
//				+ kPFert);
		return kPFert;
	}


	@Override
	protected void getPathTooltip(Geometry poly, SueloItem si) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		DecimalFormat df = new DecimalFormat("#.00");
		String tooltipText = new String(
				" PpmFosforo/Ha: "+ df.format(si.getPpmP()) +"\n"
				);

		if(area<1){
			tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
		} else {
			tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
		}

		super.getRenderPolygonFromGeom(poly, si,tooltipText);


	}

	//	private ArrayList<Object> getPathTooltip(Polygon poly, SueloItem objSuelo) {
	//
	//		Path path = getExrudedPolygonFromGeom(poly, objSuelo);
	//
	//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
	//		DecimalFormat df = new DecimalFormat("#.00");
	//		String tooltipText = new String(
	//
	//				" PpmFosforo/Ha: " + df.format(objSuelo.getPpmP()) + "\n"
	//				// + "Sup: "
	//				// + df.format(area * ProyectionConstants.METROS2_POR_HA)
	//				// + " m2\n"
	//				// +"feature: " + featureNumber
	//				);
	//
	//		if (area < 1) {
	//			tooltipText = tooltipText.concat("Sup: "
	//					+ df.format(area * ProyectionConstants.METROS2_POR_HA)
	//					+ "m2\n");
	//		} else {
	//			tooltipText = tooltipText.concat("Sup: " + df.format(area)
	//			+ "Has\n");
	//		}
	//
	//		ArrayList<Object> ret = new ArrayList<Object>();
	//		ret.add(path);
	//		ret.add(tooltipText);
	//		return ret;
	//	}

	protected int getAmountMin() {
		return 100;
	}

	protected int gerAmountMax() {
		return 500;
	}

}
