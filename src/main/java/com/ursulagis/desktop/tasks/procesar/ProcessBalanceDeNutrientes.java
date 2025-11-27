package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.DoubleStream;

import org.geotools.api.data.FeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Nutriente;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearSueloMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;
/**
 * proceso que no contemplaba potacio ni azufre
 */
@Deprecated
public class ProcessBalanceDeNutrientes extends ProcessMapTask<SueloItem,Suelo> {
	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	private int featureCount=0;
	private int featureNumber=0;

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
		updateProgress(featureNumber, featureCount);
		double ancho = labor.getConfigLabor().getAnchoGrilla();

		List<Labor<?>> labores = new LinkedList<Labor<?>>();
		labores.addAll(cosechas);
		labores.addAll(fertilizaciones);
		labores.addAll(suelos);
		ReferencedEnvelope unionEnvelope = labores.parallelStream().collect(
				()->new ReferencedEnvelope(),
				(env, labor) ->{		
					ReferencedEnvelope b = labor.outCollection.getBounds();
					System.out.println("agregando al envelope "+b );
					env.expandToInclude(b);
				},	(env1, env2) -> env1.expandToInclude(env2));
		
		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);
		System.out.println("construi una grilla con "+grilla.size()+" elementos");//construi una grilla con 5016 elementos
		//obtener una lista con todas las geometrias de las labores
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
		
		
		
		//unir las geometrias de todas las labores para obtener un poligono de contorno
		
		//GeometryCollection activasCollection = GeometryHelper.toGeometryCollection(geometriasActivas);
		//Geometry cover =  activasCollection.buffer(0);
		Geometry cover = GeometryHelper.unirGeometrias(geometriasActivas);
		System.out.println("el area del cover es: "+GeometryHelper.getHas(cover));//el area del cover es: 3.114509320893096E-12
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
		System.out.println("grilla cover size "+featureCount);
		
		grillaCover.parallelStream().forEach(g->{		
				SueloItem sueloItem = createSueloForPoly(g);		
				if(sueloItem!=null){
					labor.insertFeature(sueloItem);
				}
				featureNumber++;
				updateProgress(featureNumber, featureCount);			
		});
//		for(Geometry geometry :grillaCover){//TODO usar streams paralelos
//			SueloItem sueloItem = createSueloForPoly(geometry);		
//			if(sueloItem!=null){
//				labor.insertFeature(sueloItem);
//			}
//			featureNumber++;
//			updateProgress(featureNumber, featureCount);
//		}


		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}
	
private SueloItem createSueloForPoly(Geometry geomQuery) {	
	
		Double areaQuery =GeometryHelper.getHas(geomQuery);//ProyectionConstants.A_HAS( geomQuery.getArea());
		if(!(areaQuery>0)) {
			System.out.println("salteando la geometria "+geomQuery+" porque el area no es >0");
			return null;//si el area no es mayor a cero paso a la siguiente
		}
		//	* ProyectionConstants.A_HAS();
		//Double pesoSueloQuery = getKgSuelo(geomQuery);
		
		//Double kgPSuelo = getKgPSuelo(geomQuery);	
		//Double kgPFert = getKgPFertilizacion(geomQuery);
		//Double kgNSuelo = getKgNSuelo(geomQuery);	
		//Double kgNFert = getKgNFertilizacion(geomQuery);
		System.out.println("obteniendo los nutrientes de los suelos");
		Map<SueloParametro,Double> kgNutrienteSuelos = getKgNutrientesSuelos(geomQuery);
		Map<SueloParametro,Double> kgNutrienteFerts = getKgNutrientesFertilizaciones(geomQuery);
		
//		Double kgNFert = kgNutrienteFerts.get(SueloParametro.Nitrogeno);	
//		Double kgPFert = kgNutrienteFerts.get(SueloParametro.Fosforo);
//		Double kgKFert = kgNutrienteFerts.get(SueloParametro.Potasio);
//		Double kgSFert = kgNutrienteFerts.get(SueloParametro.Azufre);
//		
//		Double kgNSuelo = kgNutrienteSuelos.get(SueloParametro.Nitrogeno);	
//		Double kgPSuelo = kgNutrienteSuelos.get(SueloParametro.Fosforo);
//		Double kgKSuelo = kgNutrienteSuelos.get(SueloParametro.Potasio);
//		Double kgSSuelo = kgNutrienteSuelos.get(SueloParametro.Azufre);
//		Double kgMoSuelo =  kgNutrienteSuelos.get(SueloParametro.MateriaOrganica);//getKgMoSuelo(geomQuery);
		
		Map<SueloParametro,Double> kgNutrientesCosechas = getKgNutrientesCosechas(geomQuery);
		
		//Double kgPCosecha = getPpmPCosecha(geomQuery);
		//Double kgNCosecha = getPpmNCosecha(geomQuery);		
		
//		Double kgNCosecha = kgNutrientesCosechas.get(SueloParametro.Nitrogeno);
//		Double kgPCosecha = kgNutrientesCosechas.get(SueloParametro.Fosforo);
//		Double kgKCosecha = kgNutrientesCosechas.get(SueloParametro.Potasio);
//		Double kgSCosecha = kgNutrientesCosechas.get(SueloParametro.Azufre);
//		Double kgMOCosecha = kgNutrientesCosechas.get(SueloParametro.MateriaOrganica);
//		Double elevCosecha = kgNutrientesCosechas.get(SueloParametro.Elevacion);
		
		//Double kgNOrganicoSuelo = getKgNOrganicoSuelo(geomQuery); //no tomo el n organico porque lo calculo al momento de hacer la recomendacion y no quiero duplicaciones
	
	
	//	Double kgMoCosecha = getKgMoCosecha(geomQuery);
		
	//	Double 	elev = getElevCosecha(geomQuery);
		

//		if(kgPFert==0 
//			&& kgPSuelo==0
//			&& kgPCosecha==0 
//			&& kgNCosecha==0
//			&& kgNSuelo==0
//			&& kgNFert==0
//			&& kgMoSuelo == 0
//			&& kgMoCosecha == 0
//			&& elev == 10)return null;//descarto los que no tienen valores
		System.out.println("uniendo los parametros de los suelos");
		joinParametrosSuelosMaps(kgNutrienteSuelos,kgNutrienteFerts);
		joinParametrosSuelosMaps(kgNutrienteSuelos,kgNutrientesCosechas);
		
		Double sumaAreas = kgNutrienteSuelos.get(SueloParametro.Area);
		if(sumaAreas==null || sumaAreas==0) {
			System.out.println("salteando el item porque no tiene areas");
			return null;
		}
		
		System.out.println("sumaAreas "+sumaAreas);
		Double elev =  kgNutrienteSuelos.get(SueloParametro.Elevacion)/sumaAreas;
		Double newDensSuelo = kgNutrienteSuelos.get(SueloParametro.Densidad)/sumaAreas;// y si hay superposicion entre los suelo item? no deberia
		//N
		//no tomo el n organico porque lo calculo al momento de hacer la recomendacion y no quiero duplicaciones
		Double newKgHaNSuelo = 	kgNutrienteSuelos.get(SueloParametro.Nitrogeno)/ areaQuery;
		//P
		Double newKgHaPSuelo = kgNutrienteSuelos.get(SueloParametro.Fosforo)/ areaQuery;
		
		
		//System.out.println("newKgHaNSuelo "+newKgHaNSuelo);
		//MO
		Double newKgHaMOSuelo = kgNutrienteSuelos.get(SueloParametro.MateriaOrganica)/ areaQuery;

//		System.out.println("\ncantFertilizante en el suelo= " + kgPSuelo);
//		System.out.println("cantFertilizante agregada= " + kgPFert);
//		System.out.println("cantFertilizante absorvida= " + kgPCosecha);
//		System.out.println("newKgHaPSuelo = "+newKgHaPSuelo);
		Double newPpmPsuelo=labor.calcPpm_0_20(newDensSuelo,newKgHaPSuelo);///(labor.getDensidad()/2);
	//	System.out.println("newPpmPsuelo = "+newPpmPsuelo);
		Double newPpmNsuelo=labor.calcPpmNHaKg(newDensSuelo,newKgHaNSuelo);//labor.getDensidad();
		
		Double newPMoSuelo = labor.calcPorcMoHaKg(newDensSuelo,newKgHaMOSuelo);
		SueloItem sueloItem = new SueloItem();
		synchronized(labor){
			//fi= new FertilizacionItem();					
			sueloItem.setId(labor.getNextID());
		}
		sueloItem.setGeometry(geomQuery);
		
		sueloItem.setDensAp(newDensSuelo);		
		sueloItem.setPpmP(newPpmPsuelo);
		sueloItem.setPpmNO3(newPpmNsuelo);
		sueloItem.setPorcMO(newPMoSuelo);
		sueloItem.setElevacion(elev);//10.0);//para que aparezca en el mapa
		

		return sueloItem;
	}

//	private Double getElevCosecha(Geometry geometry) {
//	//	OptionalDouble elevCosecha
//		DoubleStream cosechasElevationsStream = cosechas.parallelStream().flatMapToDouble(cosecha->{
//			List<CosechaItem> items 
//					 = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			return items.parallelStream().flatMapToDouble(item->{	
//				return DoubleStream.of(item.getElevacion());				
//			});
//		});//.average();
//		
//	//	OptionalDouble elevSuelo = 
//				DoubleStream suelosElevationsStream =suelos.parallelStream().flatMapToDouble(suelo->{
//			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			return items.parallelStream().flatMapToDouble(item->{	
//				return DoubleStream.of(item.getElevacion());				
//			});
//		});//.average();
//		
//	//	OptionalDouble elevFert = 
//				DoubleStream fertilizacionesElevationsStream =	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
//			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			return items.parallelStream().flatMapToDouble(item->{	
//				return DoubleStream.of(item.getElevacion());				
//			});
//		});//.average();
//				DoubleStream elevations=DoubleStream.concat(DoubleStream.concat(cosechasElevationsStream,suelosElevationsStream)
//				,fertilizacionesElevationsStream);
//				
//		
//		return elevations.average().orElse(10);
//}

//	private double getPpmNCosecha(Geometry geometry) {
//		Double kgPCosecha = 0.0;
//		kgPCosecha = cosechas.parallelStream().flatMapToDouble(cosecha->{
//			List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			Cultivo cultivo =cosecha.getCultivo();
//			return items.parallelStream().flatMapToDouble(item->{
//				//double rindeItem = (Double) item.getRindeTnHa();
//				//double extraccionP = item.getRindeTnHa()*cultivo.getExtP();
//				Double kgPAbsHa = item.getRindeTnHa()*cultivo.getAbsN();//rindeItem * cultivo.getExtP();//solo me interesa reponer lo que extraje
//				Geometry cGeom = item.getGeometry();				
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, cGeom);//geometry.intersection(pulvGeom);// Computes a
//					//interseccionGeom no deberia ser null porque viene de cachedOutStoreQuery
//					if(inteseccionGeom!=null) {
//						area=GeometryHelper.getHas(inteseccionGeom);
//					} 
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgPAbsHa * area);				
//			});
//		}).sum();
//		return kgPCosecha;
//	}
	
//	private double getKgMoCosecha(Geometry geometry){
//		Double kgMoCosecha = 0.0;
//		kgMoCosecha = cosechas.parallelStream().flatMapToDouble(cosecha->{
//			List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			Cultivo cultivo =cosecha.getCultivo();
//			return items.parallelStream().flatMapToDouble(item->{
//				Double kgMoApHa = item.getRindeTnHa()*cultivo.getAporteMO();
//				Geometry itGeom = item.getGeometry();				
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom =GeometryHelper.getIntersection(geometry, itGeom);// geometry.intersection(itGeom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgMoApHa * area);				
//			});
//		}).sum();
//		return kgMoCosecha;
//	}
	
//	private double getPpmPCosecha(Geometry geometry) {
//		Double kgPCosecha = 0.0;
//		kgPCosecha = cosechas.parallelStream().flatMapToDouble(cosecha->{
//			List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			Cultivo cultivo = cosecha.getCultivo();
//			return items.parallelStream().flatMapToDouble(item->{
//				//double rindeItem = (Double) item.getRindeTnHa();
//				//double extraccionP = item.getRindeTnHa()*cultivo.getExtP();
//				Double kgPAbsHa = item.getRindeTnHa()*cultivo.getExtP();//rindeItem * cultivo.getExtP();//solo me interesa reponer lo que extraje
//				Geometry pulvGeom = item.getGeometry();				
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, pulvGeom);//geometry.intersection(pulvGeom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgPAbsHa * area);				
//			});
//		}).sum();
//
//		//		double ppmCosechasGeom = 0.0;
//		//
//		//		for(CosechaLabor cosecha:this.cosechas){				
//		//			Cultivo producto = cosecha.producto.getValue();
//		//
//		//			DoubleStream ppmPStream=cosecha.outStoreQuery(geometry.getEnvelopeInternal()).stream().flatMapToDouble(cItem->{
//		//				double rindeItem = (Double) cItem.getRindeTnHa();
//		//
//		//				Double ppmPabsorvida = rindeItem * producto.getAbsP();
//		//				Geometry cosechaGeom = cItem.getGeometry();
//		//
//		//				Geometry inteseccionGeom = geometry
//		//						.intersection(cosechaGeom);// Computes a
//		//				// Geometry
//		//
//		//				Double area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
//		//
//		//				return DoubleStream.of( ppmPabsorvida * area);
//		//			});					
//		//			Double ppmCosecha = ppmPStream.sum();
//		//			ppmCosechasGeom+=ppmCosecha;			
//		//		}
//
////		System.out.println("Los kg P absorvidos por las cosechas"
////				+ "correspondientes a la query son = "
////				+ kgPCosecha);
//		return kgPCosecha;
//	}
	
//	private Double getKgMoSuelo(Geometry geometry) {
//		Double kgMoSuelo = 0.0;
//		kgMoSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
//			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			return items.parallelStream().flatMapToDouble(item->{
//				Double kgMoHa= suelo.getKgMoHa(item);//(Double) item.getPpmN()*suelo.getDensidad();//TODO multiplicar por la densidad del suelo
//				Geometry geom = item.getGeometry();				
//
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					//area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgMoHa * area);				
//			});
//		}).sum();
//		return kgMoSuelo;
//	}
	
//	private Double getKgNSuelo(Geometry geometry) {
//		Double kgNSuelo = 0.0;
//		kgNSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
//			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			return items.parallelStream().flatMapToDouble(item->{
//				Double kgNHa= suelo.getKgNHa(item);//(Double) item.getPpmN()*suelo.getDensidad();//TODO multiplicar por la densidad del suelo
//				Geometry geom = item.getGeometry();				
//
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia					
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgNHa * area);				
//			});
//		}).sum();
//		return kgNSuelo;
//	}

//	private Double getKgNOrganicoSuelo(Geometry geometry) {
//		Double kgNSuelo = 0.0;
//		kgNSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
//			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			return items.parallelStream().flatMapToDouble(item->{
//				Double kgNHa= suelo.getKgNOrganicoHa(item);//divido por 2 porque es la mitad de la campania. si supiera si es invierno o verano puedo hacer 1/3 2/3 respectivamente
//				Geometry geom = item.getGeometry();				
//
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgNHa * area);				
//			});
//		}).sum();
//		return kgNSuelo;
//	}
	
	//busco todos los items de los mapas de suelo
	//calculo cuantas ppm aporta al cultivo,
	//las sumo
	//y devuelve la suma de las ppm existentes en el suelo para esa geometria
	//habria que pasar a gramos o promediar por la superficie total
//	private Double getKgSuelo(Geometry geomQuery) {
//		Double kgSuelo = 0.0;
//		Double kgSueloOP = suelos.parallelStream().flatMapToDouble(suelo->{
//			List<SueloItem> items = suelo.cachedOutStoreQuery(geomQuery.getEnvelopeInternal());
//			//System.out.println("obteniendo el peso del suelo de "+items.size()+" items");
//			double kgSueloMap = items.parallelStream().flatMapToDouble(item->{
//				Double dens= item.getDensAp();
//				Geometry geomItem = item.getGeometry();				
//
//				Double hasInterseccion = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geomQuery, geomItem);
//					hasInterseccion=GeometryHelper.getHas(inteseccionGeom);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( dens * hasInterseccion);				
//			}
//			).sum();//sumo dentro de un mismo suelo
//			return DoubleStream.of(kgSueloMap);			
//		}).filter(kg->kg>0).sum();//promedio entre 2 suelos distintos
////		if(kgSueloOP.isPresent()) {
//			//kgSuelo =kgSueloOP.getAsDouble();
//			kgSuelo = kgSueloOP;
////		}else {
////			System.out.println("average not present");
////		}
//		if(kgSuelo==0) {
//			kgSuelo=SueloItem.DENSIDAD_SUELO_KG*ProyectionConstants.A_HAS(geomQuery.getArea());
//		}
//		return kgSuelo;
//	} 
	//busco todos los items de los mapas de suelo
	//calculo cuantas ppm aporta al cultivo,
	//las sumo
	//y devuelve la suma de las ppm existentes en el suelo para esa geometria
	//XXX tener en cuenta que ppm es una unidad de concentracion y no creo que se puedad sumar directamente. 
	//habria que pasar a gramos o promediar por la superficie total
//	private Double getKgPSuelo(Geometry geometry) {
//		Double kgPSuelo = 0.0;
//		kgPSuelo=	suelos.parallelStream().flatMapToDouble(suelo->{
//			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			return items.parallelStream().flatMapToDouble(item->{
//				Double kgPHa= Suelo.getKgPHa(item);// (Double) item.getPpmP()*suelo.getDensidad()/2;//TODO multiplicar por la densidad del suelo
//				Geometry geom = item.getGeometry();				
//
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, geom);//geometry.intersection(geom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgPHa * area);				
//			});
//		}).sum();
//		return kgPSuelo;
//	}
	
//	private Double getKgNFertilizacion(Geometry geometry) {
//		Double kNFert = 0.0;
//		kNFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
//			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			Fertilizante fertilizante = fertilizacion.fertilizanteProperty.getValue();
//			return items.parallelStream().flatMapToDouble(item->{
//				Double kgNHa = (Double) item.getDosistHa() * fertilizante.getPorcN()/100;
//				Geometry fertGeom = item.getGeometry();				
//
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgNHa * area);				
//			});
//		}).sum();
//		return kNFert;
//	}

//	private Double getKgPFertilizacion(Geometry geometry) {
//		Double kPFert = 0.0;
//		kPFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
//			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//			Fertilizante fertilizante = fertilizacion.fertilizanteProperty.getValue();
//			return items.parallelStream().flatMapToDouble(item->{
//				Double kgPHa = (Double) item.getDosistHa() * fertilizante.getPorcP()/100;
//				Geometry fertGeom = item.getGeometry();				
//
//				Double area = 0.0;
//				try {
//					//XXX posible punto de error/ exceso de demora/ inneficicencia
//					Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
//					area=GeometryHelper.getHas(inteseccionGeom);
//					// Geometry
//				} catch (Exception e) {
//					e.printStackTrace();
//				}				
//				return DoubleStream.of( kgPHa * area);				
//			});
//		}).sum();
////		System.out.println("la cantidad de ppmP acumulado en el suelo es = "
////				+ kPFert);
//		return kPFert;
//	}
	
	private Map<SueloParametro,Double> getKgNutrientesSuelos(Geometry geometry) {
		 Map<SueloParametro,Double> nutrientesGeom = suelos.parallelStream().collect( 
					() -> new ConcurrentHashMap<SueloParametro,Double>(), 
					(mapG, suelo)->getKgNutrientesSuelo(geometry, mapG, suelo)			 
					,
					(map1,map2)->joinParametrosSuelosMaps(map1,map2)
					);
		return nutrientesGeom;
	}
	
	private Map<SueloParametro,Double> getKgNutrientesCosechas(Geometry geometry) {
		 Map<SueloParametro,Double> nutrientesGeom = cosechas.parallelStream().collect( 
					() -> new ConcurrentHashMap<SueloParametro,Double>(), 
					(mapG, cosecha)->getKgNutrientesCosecha(geometry, mapG, cosecha)			 
					,
					(map1,map2)->joinParametrosSuelosMaps(map1,map2)
					);
		return nutrientesGeom;
	}
	
	private void getKgNutrientesCosecha(Geometry geometry, Map<SueloParametro, Double> mapG, CosechaLabor cosecha) {		
		Cultivo cultivo = cosecha.getCultivo();		
		
		List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		Map<SueloParametro,Double> nutrientesFertilizacion = items.parallelStream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(map, item)->{
					Geometry itemG = item.getGeometry();				
					Double area = 0.0;
					try {
						//XXX posible punto de error/ exceso de demora/ inneficicencia
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, itemG);//geometry.intersection(pulvGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);
						// Geometry
					} catch (Exception e) {
						e.printStackTrace();
					}					
					Double rinde = item.getRindeTnHa();
					addValueToMap(map,SueloParametro.Nitrogeno,-1*rinde * cultivo.getExtN() * area);
					addValueToMap(map,SueloParametro.Fosforo,-1*rinde * cultivo.getExtP() * area);
					addValueToMap(map,SueloParametro.Potasio,-1*rinde * cultivo.getExtK() * area);
					addValueToMap(map,SueloParametro.Azufre,-1*rinde * cultivo.getExtS() * area);
					
					addValueToMap(map,SueloParametro.MateriaOrganica,rinde * cultivo.getAporteMO() * area);	
					//if(item.getElevacion()>1) {
					addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);				
					addValueToMap(map,SueloParametro.Area,area);
					//	}
						
													
				}, 
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
		);
		joinParametrosSuelosMaps(mapG, nutrientesFertilizacion);
	}
	
	private void addValueToMap(Map<SueloParametro,Double> map,SueloParametro p,Double value) {		
		Double kgMap = map.get(p);
		//System.out.println("agregando "+value+" al parametro "+p+" del map. era "+kgMap);
		if(kgMap!=null)value+=kgMap;
		//System.out.println("queda "+value);
		map.put(p, value);	
	}
	
	private void getKgNutrientesSuelo(Geometry geometry, Map<SueloParametro, Double> mapG, Suelo suelo) {		
		List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		Map<SueloParametro,Double> nutrientesFertilizacion = items.stream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(map, item)->{
					Double area = 0.0;
					try {
						Geometry itemG = item.getGeometry();		
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, itemG);//geometry.intersection(fertGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					Map<SueloParametro,Double> nutrientesHaItem= Suelo.getKgNutrientes(item);// (Double) item.getPpmP()*suelo.getDensidad()/2;//TODO multiplicar por la densidad del suelo
						
					for(SueloParametro k :nutrientesHaItem.keySet()) {					
						addValueToMap(map,k,nutrientesHaItem.get(k)*area);
					}		
					//if(item.getElevacion()>1) {
						//TODO calcular la mineralizacion N de la materia organica
						addValueToMap(map,SueloParametro.Densidad,item.getDensAp()*area);			
						addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);	
						Double pesoMOHa =item.getPorcMO()*item.getDensAp()*0.2/100;
						addValueToMap(map,SueloParametro.MateriaOrganica,pesoMOHa*area);		
						addValueToMap(map,SueloParametro.Area,area);
//						}else {
//							System.out.println("la elevacion no es > 1");
//						}
				
				}, 
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
		);
		joinParametrosSuelosMaps(mapG, nutrientesFertilizacion);
	}
	
	
	private Map<SueloParametro,Double> getKgNutrientesFertilizaciones(Geometry geometry) {
		 Map<SueloParametro,Double> nutrientesGeom = fertilizaciones.parallelStream().collect( 
					() -> new ConcurrentHashMap<SueloParametro,Double>(), 
					(mapG, fert)->getKgNutrientesFertilizacion(geometry, mapG, fert)			 
					,
					(map1,map2)->joinParametrosSuelosMaps(map1,map2)
					);
		return nutrientesGeom;
	}

	public void getKgNutrientesFertilizacion(Geometry geometry, Map<SueloParametro, Double> mapG, FertilizacionLabor fert) {
		Fertilizante fertilizante = fert.fertilizante;
		List<FertilizacionItem> items = fert.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		Map<SueloParametro,Double> nutrientesFertilizacion = items.stream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(map, item)->{	
					Double area = 0.0;
					try {
						Geometry fertGeom = item.getGeometry();		
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					Map<SueloParametro,Double> cFert = fertilizante.getCNutrientes();				
					Double dosis = item.getDosistHa();
					for(SueloParametro k :cFert.keySet()) {					
						addValueToMap(map,k,dosis*cFert.get(k)*area);						
					}				
					if(item.getElevacion()>10) {
					addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);				
					addValueToMap(map,SueloParametro.Area,area);
					}
														
				}, 
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
		);
		joinParametrosSuelosMaps(mapG, nutrientesFertilizacion);
	}

//	private void addNutrientValueToMap(Map<SueloParametro,Double> map,
//			SueloParametro p,
//			Map<SueloParametro,Double> cFert,			
//			FertilizacionItem item,
//			Double area) {
//		Double kgHa =  (Double) item.getDosistHa()*cFert.get(p)/100;
//		Double kgMap = map.get(p);
//		if(kgMap==null)kgMap=0.0;						
//		map.put(p, kgMap+ kgHa * area);
//	}

//	private void joinNutrientMaps(Map<Nutriente, Double> map1, Map<Nutriente, Double> map2) {			
//			map2.keySet().forEach(k->{
//				Double kg=0.0;
//				Double kg2 = map2.get(k);
//				if(kg2!=null)kg+=kg2;
//				Double kg1 = map1.get(k);
//				if(kg1!=null)kg+=kg1;
//				map1.put(k,kg);
//			});		
//	}
	
	private void joinParametrosSuelosMaps(Map<SueloParametro, Double> map1, Map<SueloParametro, Double> map2) {			
		map2.keySet().forEach(k->{
			Double kg=0.0;
			Double kg2 = map2.get(k);
			if(kg2!=null)kg+=kg2;
			Double kg1 = map1.get(k);
			if(kg1!=null)kg+=kg1;
			map1.put(k,kg);
		});		
}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		
//		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor,si,area);
//	
//		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 100;
	}

	protected int gerAmountMax() {
		return 500;
	}
}
