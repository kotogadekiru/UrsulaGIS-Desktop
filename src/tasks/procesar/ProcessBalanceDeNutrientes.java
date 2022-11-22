package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;

import org.geotools.data.FeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;

import dao.Labor;
import dao.LaborItem;
import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import gui.nww.LaborLayer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import tasks.ProcessMapTask;
import tasks.crear.CrearSueloMapTask;
import utils.GeometryHelper;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class ProcessBalanceDeNutrientes extends ProcessMapTask<SueloItem,Suelo> {
	//public Group map = new Group();

	
	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	private int featureCount;
	private int featureNumber;



	//private Suelo nuevoSuelo;
	private List<Suelo> suelos;
	private List<CosechaLabor> cosechas;
	private List<FertilizacionLabor> fertilizaciones;


	public ProcessBalanceDeNutrientes(List<Suelo> suelos,List<CosechaLabor> cosechas,List<FertilizacionLabor> fertilizaciones) {
		this.suelos=suelos;
		this.fertilizaciones=fertilizaciones;
		this.cosechas =cosechas;

		Suelo suelo = new Suelo();
		suelo.colDensidadProperty=new SimpleStringProperty("Densidad");
		suelo.setLayer(new LaborLayer());
		StringBuilder sb = new StringBuilder();
		sb.append("Balance de Nutrientes ");
		cosechas.forEach((c)->sb.append(c.getNombre()+" "));
		suelo.setNombre(sb.toString());
		suelo.setLayer(new LaborLayer());
		super.labor=suelo;

	}

	public void doProcess() throws IOException {
		// establecer los bounds de los inputs
		// crear una grilla cubriendo los bounds
		// para cada item de la grilla calcular el balance de nutrientes y producir el nuevo suelo
		// crear el clasificador
		// crear los paths
		// devolver el nuevo suelo
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
		
		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);
		//obtener una lista con todas las geometrias de las labores
		List<Geometry> geometriasActivas = labores.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(activas, labor) ->{		
					List<LaborItem> features = (List<LaborItem>) labor.outStoreQuery(unionEnvelope);
					activas.addAll(
							features.parallelStream().collect(
							()->new ArrayList<Geometry>(),
							(list, f) -> list.add((Geometry) f.getGeometry()),
							(env1, env2) -> env1.addAll(env2))
							);
				},	(env1, env2) -> env1.addAll(env2));
		
		
		//unir las geometrias de todas las labores para obtener un poligono de contorno
		GeometryCollection activasCollection = ProyectionConstants.getGeometryFactory().createGeometryCollection( geometriasActivas.toArray(new Geometry[geometriasActivas.size()]));
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
		
		//este codigo comentado permitia hacer el balance sobre los ambientes 
		//en vez de la grilla
		//Set<Geometry> parts = GeometryHelper.obtenerIntersecciones(geometriasActivas);
		//List<Polygon>  grilla = new ArrayList<Polygon>();
		
		//for(Geometry g:parts) {
		//		grilla.addAll(PolygonValidator.geometryToFlatPolygons(g));
		//}

		featureCount = grillaCover.size();

		for(Geometry geometry :grillaCover){//TODO usar streams paralelos
			SueloItem sueloItem = createSueloForPoly(geometry);		
			if(sueloItem!=null){
				labor.insertFeature(sueloItem);
			//	itemsToShow.add(sueloItem);
			}
			featureNumber++;
			updateProgress(featureNumber, featureCount);
		}

//		for(Labor<?> c:labores){
//			c.clearCache();
//		}
		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);


	}
private SueloItem createSueloForPoly(Geometry geomQuery) {	
	
		Double areaQuery =GeometryHelper.getHas(geomQuery);//ProyectionConstants.A_HAS( geomQuery.getArea());
		if(!(areaQuery>0))return null;//si el area no es mayor a cero paso a la siguiente
		//	* ProyectionConstants.A_HAS();
		Double pesoSueloQuery = getKgSuelo(geomQuery);
		Double kgPSuelo = getKgPSuelo(geomQuery);	
		Double kgPFert = getKgPFertilizacion(geomQuery);
		Double 	kgPCosecha = getPpmPCosecha(geomQuery);
		
		Double kgNSuelo = getKgNSuelo(geomQuery);	
		//Double kgNOrganicoSuelo = getKgNOrganicoSuelo(geomQuery); //no tomo el n organico porque lo calculo al momento de hacer la recomendacion y no quiero duplicaciones
		Double kgNFert = getKgNFertilizacion(geomQuery);
		Double kgNCosecha = getPpmNCosecha(geomQuery);
		
		
		Double kgMoSuelo = getKgMoSuelo(geomQuery);
		Double kgMoCosecha = getKgMoCosecha(geomQuery);
		
		Double 	elev = getElevCosecha(geomQuery);


		if(kgPFert==0 
				&&kgPSuelo==0
				&& kgPCosecha==0 
				&& kgNCosecha==0
				&& kgNSuelo==0
				&& kgNFert==0
				&& kgMoSuelo == 0
				&& kgMoCosecha == 0
				&& elev == 10)return null;//descarto los que no tienen valores
		
		Double newDensSuelo = pesoSueloQuery/areaQuery;// y si hay superposicion entre los suelo item? no deberia
		
		//P
		Double newKgHaPSuelo = (kgPFert + kgPSuelo - kgPCosecha) / areaQuery;
		//no tomo el n organico porque lo calculo al momento de hacer la recomendacion y no quiero duplicaciones
		//N
		Double newKgHaNSuelo = (kgNFert + kgNSuelo  - kgNCosecha)	/ areaQuery;
		//System.out.println("newKgHaNSuelo "+newKgHaNSuelo);
		//MO
		Double newKgHaMOSuelo = (kgMoSuelo + kgMoCosecha)/ areaQuery;

//		System.out.println("\ncantFertilizante en el suelo= " + kgPSuelo);
//		System.out.println("cantFertilizante agregada= " + kgPFert);
//		System.out.println("cantFertilizante absorvida= " + kgPCosecha);
//		System.out.println("newKgHaPSuelo = "+newKgHaPSuelo);
		Double newPpmPsuelo=labor.calcPpmPHaKg(newDensSuelo,newKgHaPSuelo);///(labor.getDensidad()/2);
	//	System.out.println("newPpmPsuelo = "+newPpmPsuelo);
		Double newPpmNsuelo=labor.calcPpmNHaKg(newDensSuelo,newKgHaNSuelo);//labor.getDensidad();
		
		Double newPMoSuelo = labor.calcPorcMoHaKg(newDensSuelo,newKgHaMOSuelo);
		SueloItem sueloItem = new SueloItem();
		synchronized(labor){
			//fi= new FertilizacionItem();					
			sueloItem.setId(labor.getNextID());
		}
		sueloItem.setDensAp(newDensSuelo);
		sueloItem.setGeometry(geomQuery);
		sueloItem.setPpmP(newPpmPsuelo);
		sueloItem.setPpmNO3(newPpmNsuelo);
		sueloItem.setPorcMO(newPMoSuelo);
		sueloItem.setElevacion(elev);//10.0);//para que aparezca en el mapa


		return sueloItem;
	}

	private Double getElevCosecha(Geometry geometry) {
	//	OptionalDouble elevCosecha
		DoubleStream cosechasElevationsStream = cosechas.parallelStream().flatMapToDouble(cosecha->{
			List<CosechaItem> items 
					 = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{	
				return DoubleStream.of(item.getElevacion());				
			});
		});//.average();
		
	//	OptionalDouble elevSuelo = 
				DoubleStream suelosElevationsStream =suelos.parallelStream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{	
				return DoubleStream.of(item.getElevacion());				
			});
		});//.average();
		
	//	OptionalDouble elevFert = 
				DoubleStream fertilizacionesElevationsStream =	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{	
				return DoubleStream.of(item.getElevacion());				
			});
		});//.average();
				DoubleStream elevations=DoubleStream.concat(DoubleStream.concat(cosechasElevationsStream,suelosElevationsStream)
				,fertilizacionesElevationsStream);
				
		
		return elevations.average().orElse(10);
}

	private double getPpmNCosecha(Geometry geometry) {
		Double kgPCosecha = new Double(0);
		kgPCosecha = cosechas.parallelStream().flatMapToDouble(cosecha->{
			List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Cultivo cultivo =cosecha.getCultivo();
			return items.parallelStream().flatMapToDouble(item->{
				//double rindeItem = (Double) item.getRindeTnHa();
				//double extraccionP = item.getRindeTnHa()*cultivo.getExtP();
				Double kgPAbsHa = item.getRindeTnHa()*cultivo.getAbsN();//rindeItem * cultivo.getExtP();//solo me interesa reponer lo que extraje
				Geometry cGeom = item.getGeometry();				
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, cGeom);//geometry.intersection(pulvGeom);// Computes a
					//interseccionGeom no deberia ser null porque viene de cachedOutStoreQuery
					if(inteseccionGeom!=null) {
						area=GeometryHelper.getHas(inteseccionGeom);
					} 
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgPAbsHa * area);				
			});
		}).sum();
		return kgPCosecha;
	}
	
	private double getKgMoCosecha(Geometry geometry){
		Double kgMoCosecha = new Double(0);
		kgMoCosecha = cosechas.parallelStream().flatMapToDouble(cosecha->{
			List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Cultivo cultivo =cosecha.getCultivo();
			return items.parallelStream().flatMapToDouble(item->{
				Double kgMoApHa = item.getRindeTnHa()*cultivo.getAporteMO();
				Geometry itGeom = item.getGeometry();				
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom =GeometryHelper.getIntersection(geometry, itGeom);// geometry.intersection(itGeom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgMoApHa * area);				
			});
		}).sum();
		return kgMoCosecha;
	}
	
	private double getPpmPCosecha(Geometry geometry) {
		Double kgPCosecha = new Double(0);
		kgPCosecha = cosechas.parallelStream().flatMapToDouble(cosecha->{
			List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Cultivo cultivo =cosecha.getCultivo();
			return items.parallelStream().flatMapToDouble(item->{
				//double rindeItem = (Double) item.getRindeTnHa();
				//double extraccionP = item.getRindeTnHa()*cultivo.getExtP();
				Double kgPAbsHa = item.getRindeTnHa()*cultivo.getExtP();//rindeItem * cultivo.getExtP();//solo me interesa reponer lo que extraje
				Geometry pulvGeom = item.getGeometry();				
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, pulvGeom);//geometry.intersection(pulvGeom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
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
	
	private Double getKgMoSuelo(Geometry geometry) {
		Double kgMoSuelo = new Double(0);
		kgMoSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{
				Double kgMoHa= suelo.getKgMoHa(item);//(Double) item.getPpmN()*suelo.getDensidad();//TODO multiplicar por la densidad del suelo
				Geometry geom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
					//area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgMoHa * area);				
			});
		}).sum();
		return kgMoSuelo;
	}
	
	private Double getKgNSuelo(Geometry geometry) {
		Double kgNSuelo = new Double(0);
		kgNSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{
				Double kgNHa= suelo.getKgNHa(item);//(Double) item.getPpmN()*suelo.getDensidad();//TODO multiplicar por la densidad del suelo
				Geometry geom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia					
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgNHa * area);				
			});
		}).sum();
		return kgNSuelo;
	}

	private Double getKgNOrganicoSuelo(Geometry geometry) {
		Double kgNSuelo = new Double(0);
		kgNSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{
				Double kgNHa= suelo.getKgNOrganicoHa(item);//divido por 2 porque es la mitad de la campania. si supiera si es invierno o verano puedo hacer 1/3 2/3 respectivamente
				Geometry geom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgNHa * area);				
			});
		}).sum();
		return kgNSuelo;
	}
	
	//busco todos los items de los mapas de suelo
	//calculo cuantas ppm aporta al cultivo,
	//las sumo
	//y devuelve la suma de las ppm existentes en el suelo para esa geometria
	//XXX tener en cuenta que ppm es una unidad de concentracion y no creo que se puedad sumar directamente. 
	//habria que pasar a gramos o promediar por la superficie total
	private Double getKgSuelo(Geometry geomQuery) {
		Double kgPSuelo = new Double(0);
		kgPSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geomQuery.getEnvelopeInternal());
			return items.parallelStream().flatMapToDouble(item->{
				Double dens= item.getDensAp();// (Double) item.getPpmP()*suelo.getDensidad()/2;//TODO multiplicar por la densidad del suelo
				Geometry geomItem = item.getGeometry();				

				Double hasInterseccion = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geomQuery, geomItem);//geometry.intersection(geom);// Computes a
					hasInterseccion=GeometryHelper.getHas(inteseccionGeom);
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( dens * hasInterseccion);				
			});
		}).sum();
		return kgPSuelo;
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
				Double kgPHa= suelo.getKgPHa(item);// (Double) item.getPpmP()*suelo.getDensidad()/2;//TODO multiplicar por la densidad del suelo
				Geometry geom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgPHa * area);				
			});
		}).sum();
		return kgPSuelo;
	}
	
	private Double getKgNFertilizacion(Geometry geometry) {
		Double kNFert = new Double(0);
		kNFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Fertilizante fertilizante = fertilizacion.fertilizanteProperty.getValue();
			return items.parallelStream().flatMapToDouble(item->{
				Double kgNHa = (Double) item.getDosistHa() * fertilizante.getPorcN()/100;
				Geometry fertGeom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgNHa * area);				
			});
		}).sum();
		return kNFert;
	}

	private Double getKgPFertilizacion(Geometry geometry) {
		Double kPFert = new Double(0);
		kPFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Fertilizante fertilizante = fertilizacion.fertilizanteProperty.getValue();
			return items.parallelStream().flatMapToDouble(item->{
				Double kgPHa = (Double) item.getDosistHa() * fertilizante.getPorcP()/100;
				Geometry fertGeom = item.getGeometry();				

				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
					area=GeometryHelper.getHas(inteseccionGeom);
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
	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		
		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor,si,area);
	
		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);
	}

	protected int getAmountMin() {
		return 100;
	}

	protected int gerAmountMax() {
		return 500;
	}
}
