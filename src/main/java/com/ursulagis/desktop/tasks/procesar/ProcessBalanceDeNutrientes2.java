package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.geometry.jts.ReferencedEnvelope;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Nutriente;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import javafx.beans.property.SimpleStringProperty;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearSueloMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ProcessBalanceDeNutrientes2 extends ProcessMapTask<SueloItem,Suelo> {
	double distanciaAvanceMax = 0;
	double anchoMax = 0;

	private int featureCount=0;
	private int featureNumber=0;

	private List<Suelo> suelos;
	private List<CosechaLabor> cosechas;
	private List<FertilizacionLabor> fertilizaciones;

	public ProcessBalanceDeNutrientes2(List<Suelo> suelos,List<CosechaLabor> cosechas,List<FertilizacionLabor> fertilizaciones) {
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
					//System.out.println("agregando al envelope "+b );
					env.expandToInclude(b);
				},	(env1, env2) -> env1.expandToInclude(env2));

		// 2 generar una grilla de ancho ="ancho" que cubra bounds
		List<Polygon>  grilla = GrillarCosechasMapTask.construirGrilla(unionEnvelope, ancho);
		//System.out.println("construi una grilla con "+grilla.size()+" elementos");//construi una grilla con 5016 elementos
		//obtener una lista con todas las geometrias de las labores
		/*
		List<Geometry> geometriasActivas = labores.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(activas, labor) ->{		
					try {
						//Geometry lContorno = labor.getContorno().toGeometry();
						ReferencedEnvelope bounds = labor.outCollection.getBounds();
						//System.out.println("outCollectionBounds "+bounds);
						Geometry cascadedUnion = GeometryHelper.unirCascading(labor,bounds);
						if(cascadedUnion!=null) {
							activas.add(cascadedUnion);
						}else {
							System.err.println("no se pudo extraer el contorno de "+labor.getNombre());
						}
					}catch(Exception e ) {
						e.printStackTrace();
						System.err.println("no se pudo extraer el contorno de "+labor.getNombre());						
					}
				},	(l1, l2) -> l1.addAll(l2));		

		//unir las geometrias de todas las labores para obtener un poligono de contorno
		Geometry cover = GeometryHelper.unirGeometrias(geometriasActivas);
		//FIXME cover no se hace bien
		//System.out.println("el area del cover es: "+GeometryHelper.getHas(cover));//el area del cover es: 3.114509320893096E-12
		//intersectar la grilla con el contorno
		if(cover==null) {
			System.err.println("no se pudo unir el contorno de las labores a procesar "+cover);
			return;
		}
	
		List<Geometry> grillaCover = grilla.parallelStream().collect(
				()->new ArrayList<Geometry>(),
				(intersecciones, poly) ->{			
					try {
					Geometry intersection = GeometryHelper.getIntersection(poly, cover); 
					if(intersection!=null) {
						intersecciones.add(intersection);
					}
					}catch(Exception e) {
						e.printStackTrace();
					}
				},	(env1, env2) -> env1.addAll(env2));

	*/
		featureCount = grilla.size();
		//System.out.println("grilla cover size "+featureCount);

		grilla.parallelStream().forEach(g->{		
			SueloItem sueloItem = createSueloForPoly(g);		
			if(sueloItem!=null){
				labor.insertFeature(sueloItem);
			}
			featureNumber++;
			updateProgress(featureNumber, featureCount);			
		});

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	private SueloItem createSueloForPoly(Geometry geomQuery) {	

		Double areaQuery =GeometryHelper.getHas(geomQuery);//ProyectionConstants.A_HAS( geomQuery.getArea());
		if(!(areaQuery>0)) {
			//System.out.println("salteando la geometria "+geomQuery+" porque el area no es >0");
			return null;//si el area no es mayor a cero paso a la siguiente
		}

		//System.out.println("obteniendo los nutrientes de los suelos");
		NutrienteSuelo kgNutrienteSuelos = getKgNutrientesSuelos(geomQuery);
		Double areaSuperpuestItem =0.0;
		Double sumaAreasSuelos = kgNutrienteSuelos.parametros.get(SueloParametro.Area);
		if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.Area)) {
			areaSuperpuestItem+=sumaAreasSuelos;
		}
		Double elev =  10.0;
		Double newDensSuelo =SueloItem.DENSIDAD_SUELO_KG;
		if(sumaAreasSuelos!=null && sumaAreasSuelos>0) {
			if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.Elevacion)) {
				elev= kgNutrienteSuelos.parametros.get(SueloParametro.Elevacion)/sumaAreasSuelos;
			}
			
		
			if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.Densidad)) {
				//FIXME solo deberia dividir por las areas de los suelos; en el caso normal solo habria un suelo
				newDensSuelo = kgNutrienteSuelos.parametros.get(SueloParametro.Densidad)/sumaAreasSuelos;//aca tiro una excepcion
			}
		}

		//System.out.println("sumaAreas "+sumaAreas);
	

		
		//System.out.println("obteniendo los nutrientes de las fertilizaciones");
		NutrienteSuelo kgNutrienteFerts = getKgNutrientesFertilizaciones(geomQuery);	
		if(kgNutrienteFerts.parametros.containsKey(SueloParametro.Area)) {
		areaSuperpuestItem+=kgNutrienteFerts.parametros.get(SueloParametro.Area);
		}
		//System.out.println("obteniendo los nutrientes de las cosechas");
		NutrienteSuelo kgNutrientesCosechas = getKgNutrientesCosechas(geomQuery);
		if(kgNutrientesCosechas.parametros.containsKey(SueloParametro.Area)) {
		areaSuperpuestItem+=kgNutrientesCosechas.parametros.get(SueloParametro.Area);
		}
		if(!(areaSuperpuestItem>0)) {
			return null;
		}

		//System.out.println("uniendo los parametros de los suelos y fertilizaciones");
		kgNutrienteSuelos.join(kgNutrienteFerts);
		//System.out.println("uniendo los parametros de los suelos y cosechas");
		kgNutrienteSuelos.join(kgNutrientesCosechas);

		Geometry geomSup = GeometryHelper.unirGeometrias(kgNutrienteSuelos.geoms);
		Double areaSup=0.0;
		if(geomSup!=null) {
			areaSup=ProyectionConstants.A_HAS(geomSup.getArea());
		}
		if(!(areaSup>0)) {
			System.err.println("descargando "+geomSup);
			return null;
		}else {
			areaQuery=areaSup;
		}

		// y si hay superposicion entre los suelo item? no deberia
		//N
		//no tomo el n organico porque lo calculo al momento de hacer la recomendacion y no quiero duplicaciones
		Double newKgHaNSuelo = 	0.0;
		if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.Nitrogeno)) {
			newKgHaNSuelo=kgNutrienteSuelos.parametros.get(SueloParametro.Nitrogeno)/ areaQuery;
		}
		//P
		Double newKgHaPSuelo = 0.0;
		if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.Fosforo)) {
			newKgHaPSuelo = kgNutrienteSuelos.parametros.get(SueloParametro.Fosforo)/ areaQuery;
		}
		
		Double newKgHaKsuelo = 0.0;
		if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.Potasio)) {
			newKgHaKsuelo = kgNutrienteSuelos.parametros.get(SueloParametro.Potasio)/ areaQuery;
		}

		Double newKgHaSsuelo = 0.0;
		if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.Azufre)) {
			newKgHaSsuelo = kgNutrienteSuelos.parametros.get(SueloParametro.Azufre)/ areaQuery;
		}
		
		Double newKgHaMOSuelo =0.0;
		if(kgNutrienteSuelos.parametros.containsKey(SueloParametro.MateriaOrganica)) {
			newKgHaMOSuelo= kgNutrienteSuelos.parametros.get(SueloParametro.MateriaOrganica)/ (areaQuery);
		}
		Double newPpmNsuelo=labor.calcPpmNHaKg(newDensSuelo,newKgHaNSuelo);
		Double newPpmPsuelo=labor.calcPpm_0_20(newDensSuelo,newKgHaPSuelo);
		Double newPpmKsuelo=labor.calcPpm_0_20(newDensSuelo,newKgHaKsuelo);
		Double newPpmSsuelo=labor.calcPpmSHaKg(newDensSuelo,newKgHaSsuelo);//dice isabel que va 0-60 es SO4		
		
		Double newPMoSuelo = labor.calcPorcMoHaKg(newDensSuelo,newKgHaMOSuelo);
		
		SueloItem sueloItem = new SueloItem();
		synchronized(labor){
			//fi= new FertilizacionItem();					
			sueloItem.setId(labor.getNextID());
		}
		sueloItem.setGeometry(geomSup);

		sueloItem.setDensAp(newDensSuelo);	
		sueloItem.setPpmNO3(newPpmNsuelo);
		sueloItem.setPpmP(newPpmPsuelo);
		sueloItem.setPpmK(newPpmKsuelo);
		sueloItem.setPpmS(newPpmSsuelo);
	
		List<SueloParametro> microNutrientes = Nutriente.getMicroNutrientes();
		final double areaQueryF = areaQuery;
		final double newDensSueloF = newDensSuelo;
		microNutrientes.forEach((sp)->{
			Nutriente n = Nutriente.getNutrientesDefault().get(sp);
			Double kgNutrienteSuelo = kgNutrienteSuelos.parametros.get(sp);
			if(kgNutrienteSuelo!=null) {
			double newKgHaNutriente= kgNutrienteSuelo/ (areaQueryF);			
			double newPpmNutriente =  Suelo.kgToPpm(newDensSueloF, newKgHaNutriente,n.getProfundidad());
			Suelo.setPpm(sp, sueloItem, newPpmNutriente);
			}
		});
		
		sueloItem.setPorcMO(newPMoSuelo);
		sueloItem.setElevacion(elev>10?elev:10.0);//10.0);//para que aparezca en el mapa
		return sueloItem;
	}	

	private NutrienteSuelo getKgNutrientesSuelos(Geometry geometry) {
		NutrienteSuelo nutrientesGeom = suelos.parallelStream().collect( 
				() -> new NutrienteSuelo(), 
				(n, suelo)->getKgNutrientesSuelo(geometry, n, suelo)			 
				,
				(n1,n2)->n1.join(n2)
				);
		return nutrientesGeom;
	}
	private void getKgNutrientesSuelo(Geometry geometry, NutrienteSuelo nutriente, Suelo suelo) {		
		List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		NutrienteSuelo nutrientesSuelo = items.stream().collect( 
				() -> new NutrienteSuelo(), 
				(n, item)->{
					Double area = 0.0;
					try {
						Geometry itemG = item.getGeometry();		
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, itemG);//geometry.intersection(fertGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);
						n.geoms.add(inteseccionGeom);
					} catch (Exception e) {
						e.printStackTrace();
					}

					Map<SueloParametro,Double> nutrientesHaItem= Suelo.getKgNutrientes(item);// (Double) item.getPpmP()*suelo.getDensidad()/2;//TODO multiplicar por la densidad del suelo

					for(SueloParametro k :nutrientesHaItem.keySet()) {					
						addValueToMap(n.parametros,k,nutrientesHaItem.get(k)*area);
					}		
					//if(item.getElevacion()>1) {
					//calcular la mineralizacion N de la materia organica. no eso va en recomendacion
					addValueToMap(n.parametros,SueloParametro.Densidad,item.getDensAp()*area);			
					addValueToMap(n.parametros,SueloParametro.Elevacion,item.getElevacion()*area);	
					
					Double pesoMOHa =item.getPorcMO()*item.getDensAp()*0.2*ProyectionConstants.METROS2_POR_HA/100;
					addValueToMap(n.parametros,SueloParametro.MateriaOrganica,pesoMOHa*area);		
					addValueToMap(n.parametros,SueloParametro.Area,area);
					//						}else {
					//							System.out.println("la elevacion no es > 1");
					//						}

				}, 
				(n1,n2)->n1.join(n2)
				);
		nutriente.join(nutrientesSuelo);
	}
	
	/*
	private Map<SueloParametro,Double> getKgNutrientesSuelos(Geometry geometry) {
		Map<SueloParametro,Double> nutrientesGeom = suelos.parallelStream().collect( 
				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
				(mapG, suelo)->getKgNutrientesSuelo(geometry, mapG, suelo)			 
				,
				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
				);
		return nutrientesGeom;
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
					//calcular la mineralizacion N de la materia organica. no eso va en recomendacion
					addValueToMap(map,SueloParametro.Densidad,item.getDensAp()*area);			
					addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);	
					
					Double pesoMOHa =item.getPorcMO()*item.getDensAp()*0.2*ProyectionConstants.METROS2_POR_HA/100;
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
*/
	private NutrienteSuelo getKgNutrientesCosechas(Geometry geometry) {
		NutrienteSuelo nutrientesGeom = cosechas.parallelStream().collect( 
				() -> new NutrienteSuelo(), 
				(n, cosecha)->getKgNutrientesCosecha(geometry, n, cosecha),
				(n1,n2)->n1.join(n2)
				);
		return nutrientesGeom;
	}

	/**
	 * metodo que toma un suelo item y le descuenta los nutrientes extraidos por la cosecha dentro del mismo.
	 * une las geometrias de la cosecha a la geometria del item
	 * @param geometry
	 * @param item
	 * @param cosecha
	 */
	private void getKgNutrientesCosecha(Geometry geometry, NutrienteSuelo item, CosechaLabor cosecha) {		
		Cultivo cultivo = cosecha.getCultivo();		

		List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		NutrienteSuelo nutrientesCosecha = items.parallelStream().collect( 
				() -> new NutrienteSuelo(), 
				(sueloItem, cosechaItem)->{
					Geometry itemG = cosechaItem.getGeometry();				
					Double area = 0.0;
					try {						
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, itemG);//geometry.intersection(pulvGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);
						sueloItem.geoms.add(inteseccionGeom);
					} catch (Exception e) {
						e.printStackTrace();
					}					
					Double rinde = cosechaItem.getRindeTnHa();
					
					addValueToMap(sueloItem.parametros,SueloParametro.Nitrogeno,-1*rinde * cultivo.getExtN() * area);
					addValueToMap(sueloItem.parametros,SueloParametro.Fosforo,-1*rinde * cultivo.getExtP() * area);
					addValueToMap(sueloItem.parametros,SueloParametro.Potasio,-1*rinde * cultivo.getExtK() * area);
					addValueToMap(sueloItem.parametros,SueloParametro.Azufre,-1*rinde * cultivo.getExtS() * area);
					try {
					addValueToMap(sueloItem.parametros,SueloParametro.Calcio,-1*rinde * cultivo.getExtCa() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Magnecio,-1*rinde * cultivo.getExtMg() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Boro,-1*rinde * cultivo.getExtB() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Cloro,-1*rinde * cultivo.getExtCl() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Cobalto,-1*rinde * cultivo.getExtCo() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Cobre,-1*rinde * cultivo.getExtCu() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Hierro,-1*rinde * cultivo.getExtFe() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Manganeso,-1*rinde * cultivo.getExtMn() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Molibdeno,-1*rinde * cultivo.getExtMo() * area);
					}catch(Exception e ) {}try {
					addValueToMap(sueloItem.parametros,SueloParametro.Zinc,-1*rinde * cultivo.getExtZn() * area);
					}catch(Exception e ) {}				
					
					addValueToMap(sueloItem.parametros,SueloParametro.MateriaOrganica,rinde * cultivo.getAporteMO() * area);	
					//if(item.getElevacion()>1) {
					addValueToMap(sueloItem.parametros,SueloParametro.Elevacion,cosechaItem.getElevacion() * area);				
					addValueToMap(sueloItem.parametros,SueloParametro.Area,area);
					//	}


				}, 
				(nutrientes1,nutrientes2)->{
					nutrientes1.join(nutrientes2);
				}
				);
		item.join(nutrientesCosecha);
	}
	
//	private void getKgNutrientesCosecha(Geometry geometry, Map<SueloParametro, Double> mapG, CosechaLabor cosecha) {		
//		Cultivo cultivo = cosecha.getCultivo();		
//
//		List<CosechaItem> items = cosecha.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//		Map<SueloParametro,Double> nutrientesCosechas = items.parallelStream().collect( 
//				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
//				(map, item)->{
//					Geometry itemG = item.getGeometry();				
//					Double area = 0.0;
//					try {						
//						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, itemG);//geometry.intersection(pulvGeom);// Computes a
//						area=GeometryHelper.getHas(inteseccionGeom);					
//					} catch (Exception e) {
//						e.printStackTrace();
//					}					
//					Double rinde = item.getRindeTnHa();
//					addValueToMap(map,SueloParametro.Nitrogeno,-1*rinde * cultivo.getExtN() * area);
//					addValueToMap(map,SueloParametro.Fosforo,-1*rinde * cultivo.getExtP() * area);
//					addValueToMap(map,SueloParametro.Potasio,-1*rinde * cultivo.getExtK() * area);
//					addValueToMap(map,SueloParametro.Azufre,-1*rinde * cultivo.getExtS() * area);
//
//					addValueToMap(map,SueloParametro.MateriaOrganica,rinde * cultivo.getAporteMO() * area);	
//					//if(item.getElevacion()>1) {
//					addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);				
//					addValueToMap(map,SueloParametro.Area,area);
//					//	}
//
//
//				}, 
//				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
//				);
//		joinParametrosSuelosMaps(mapG, nutrientesCosechas);
//	}

	private void addValueToMap(Map<SueloParametro,Double> map,SueloParametro p,Double value) {		
		Double kgMap = map.get(p);
		//System.out.println("agregando "+value+" al parametro "+p+" del map. era "+kgMap);
		if(kgMap!=null)value+=kgMap;
		//System.out.println("queda "+value);
		map.put(p, value);	
	}


	private NutrienteSuelo getKgNutrientesFertilizaciones(Geometry geometry) {
		NutrienteSuelo nutrientesGeom = fertilizaciones.parallelStream().collect( 
				() -> new NutrienteSuelo(), 
				(n, fert)->getKgNutrientesFertilizacion(geometry, n, fert),
				(n1,n2)->n1.join(n2)				
				);
		return nutrientesGeom;
	}

//	private Map<SueloParametro,Double> getKgNutrientesFertilizaciones(Geometry geometry) {
//		Map<SueloParametro,Double> nutrientesGeom = fertilizaciones.parallelStream().collect( 
//				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
//				(mapG, fert)->getKgNutrientesFertilizacion(geometry, mapG, fert),
//				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
//				);
//		return nutrientesGeom;
//	}

	public void getKgNutrientesFertilizacion(Geometry geometry, NutrienteSuelo item, FertilizacionLabor fert) {
		Fertilizante fertilizante = fert.fertilizante;
		List<FertilizacionItem> items = fert.cachedOutStoreQuery(geometry.getEnvelopeInternal());
		NutrienteSuelo nutrientesFertilizacion = items.stream().collect( 
				() -> new NutrienteSuelo(), 
				(n, fertItem)->{	
					Double area = 0.0;
					try {
						Geometry fertGeom = fertItem.getGeometry();		
						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
						area=GeometryHelper.getHas(inteseccionGeom);	
						n.geoms.add(inteseccionGeom);
					} catch (Exception e) {
						e.printStackTrace();
					}

					Map<SueloParametro,Double> cFert = fertilizante.getCNutrientes();//%				
					Double dosis = fertItem.getDosistHa();//kg/ha
					if(cFert!=null) {
					for(SueloParametro k :cFert.keySet()) {					
						addValueToMap(n.parametros,k,dosis*cFert.get(k)*area/100);				
//						if(SueloParametro.Zinc==k) {
//							System.out.println("el contenido de zinc de la fertilizacion es "+n.parametros.get(k));
//						}
					}				
					}else {
						System.out.println("cFert es null para "+fertilizante.getNombre());
					}
				//	if(item.getElevacion()>10) {
						addValueToMap(n.parametros,SueloParametro.Elevacion,fertItem.getElevacion()*area);				
						addValueToMap(n.parametros,SueloParametro.Area,area);
				//	}

				}, 
				(n1,n2)->n1.join(n2)
				);
		item.join(nutrientesFertilizacion);
		
	}
//	
//	public void getKgNutrientesFertilizacion(Geometry geometry, Map<SueloParametro, Double> mapG, FertilizacionLabor fert) {
//		Fertilizante fertilizante = fert.fertilizanteProperty.getValue();
//		List<FertilizacionItem> items = fert.cachedOutStoreQuery(geometry.getEnvelopeInternal());
//		Map<SueloParametro,Double> nutrientesFertilizacion = items.stream().collect( 
//				() -> new ConcurrentHashMap<SueloParametro,Double>(), 
//				(map, item)->{	
//					Double area = 0.0;
//					try {
//						Geometry fertGeom = item.getGeometry();		
//						Geometry inteseccionGeom = GeometryHelper.getIntersection(geometry, fertGeom);//geometry.intersection(fertGeom);// Computes a
//						area=GeometryHelper.getHas(inteseccionGeom);						
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//
//					Map<SueloParametro,Double> cFert = fertilizante.getCNutrientes();//%				
//					Double dosis = item.getDosistHa();//kg/ha
//					if(cFert!=null) {
//					for(SueloParametro k :cFert.keySet()) {					
//						addValueToMap(map,k,dosis*cFert.get(k)*area/100);						
//					}				
//					}else {
//						System.out.println("cFert es null para "+fertilizante.getNombre());
//					}
//				//	if(item.getElevacion()>10) {
//						addValueToMap(map,SueloParametro.Elevacion,item.getElevacion()*area);				
//						addValueToMap(map,SueloParametro.Area,area);
//				//	}
//
//				}, 
//				(map1,map2)->joinParametrosSuelosMaps(map1,map2)
//				);
//		joinParametrosSuelosMaps(mapG, nutrientesFertilizacion);
//	}	


	
	/**
	 * clase que representa un proto suelo item pero con valores en kg en vez de ppm
	 */
	private class NutrienteSuelo{
		/**
		 * coleccion de geometrias tomadas para este objeto
		 */
		public List<Geometry> geoms =  Collections.synchronizedList(new ArrayList<>());;
		public Map<SueloParametro,Double> parametros = new ConcurrentHashMap<SueloParametro,Double>();

		/**
		 * add to this nutrientes suelo n
		 * @param n
		 */
		public void join(NutrienteSuelo n) {
			joinParametrosSuelosMaps(this.parametros,n.parametros);
			this.geoms.addAll(n.geoms);
			
		}
		
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
	}


//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();
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
