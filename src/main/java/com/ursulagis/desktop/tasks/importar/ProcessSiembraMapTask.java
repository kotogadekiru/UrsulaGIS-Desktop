package com.ursulagis.desktop.tasks.importar;
import java.io.IOException;
import java.util.function.Function;

import org.geotools.api.data.FeatureReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.siembra.SiembraConfig;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.ConvertirASiembraTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class ProcessSiembraMapTask extends ProcessMapTask<SiembraItem,SiembraLabor> {	

	public ProcessSiembraMapTask(SiembraLabor siembra) {
		super(siembra);
		//		this.layer=map1;
		//		this.store = store;
		//		
		//		precioPasada = precioPasada1;
		//		this.precioBolsaSemilla = precioBolsaSemilla;

	}


	public void doProcess() throws IOException {
		//		System.out.println("doProcess(); "+System.currentTimeMillis());

		FeatureReader<SimpleFeatureType, SimpleFeature> reader =null;
		//	CoordinateReferenceSystem storeCRS =null;
		if(labor.getInStore()!=null){
			if(labor.outCollection!=null)labor.outCollection.clear();
			reader = labor.getInStore().getFeatureReader();
			//		 storeCRS = labor.getInStore().getSchema().getCoordinateReferenceSystem();
			//convierto los features en cosechas
			featureCount=labor.getInStore().getFeatureSource().getFeatures().size();
		} else {//editando
			if(labor.getInCollection() == null){//solo cambio la inCollection por la outCollection la primera vez
				labor.setInCollection(labor.outCollection);
				labor.outCollection=  new DefaultFeatureCollection("internal",labor.getType()); //$NON-NLS-1$
			}
			// cuando es una grilla los datos estan en outstore y instore es null
			// si leo del outCollection y luego escribo en outCollection me quedo sin memoria
			reader = labor.getInCollection().reader();
			labor.outCollection.clear();
			featureCount=labor.getInCollection().size();
		}

		int divisor = 1;

		SiembraConfig.Unidad dosisUnit = labor.getConfiguracion().dosisUnitProperty().get();
		Function<Double,Double> dosisToKgHa = (dosis)->dosis*2;

		switch (dosisUnit) {
		case kgHa: 
			dosisToKgHa = (dosis)->dosis;
			break;
		case milPlaHa:
			dosisToKgHa = (milPlHa)->{
				//convertir de miles de semillas por Ha a kg de semilla por Ha
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return (milPlHa)*labor.getSemilla().getPesoDeMil()/1000;
			};
			break;
		case pla10MtLineal:
			dosisToKgHa = (pla10MtLineal)->{
				//convertir de semillas cada 10metros lineales a kg de semilla por Ha
				//1) divido por 10 para obtener semillas por metro lineal
				double plML= pla10MtLineal/10;
				double plm2= plML/labor.getEntreSurco();
				double plHa = plm2*ProyectionConstants.METROS2_POR_HA;
				double gramHa = plHa*labor.getSemilla().getPesoDeMil()/1000;
				double kgHa= gramHa/1000;
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return kgHa;
			};
			break;
		case pla1MtLineal:
			dosisToKgHa = (pla1MtLineal)->{
				double plML= pla1MtLineal;
				double plm2= plML/labor.getEntreSurco();
				double plHa = plm2*ProyectionConstants.METROS2_POR_HA;
				double gramHa = plHa*labor.getSemilla().getPesoDeMil()/1000;
				double kgHa= gramHa/1000;
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return kgHa;
			};
			break;
		case plaMetroCuadrado:
			dosisToKgHa = (plm2)->{
				//convertir de semillas cada metro cuadrado a kg de semilla por Ha
				//1) divido por 10 para obtener semillas por metro lineal

				double plHa = plm2*ProyectionConstants.METROS2_POR_HA;
				double gramHa = plHa*labor.getSemilla().getPesoDeMil()/1000;
				double kgHa= gramHa/1000;
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return kgHa;
			};
			break;

		}

		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			SiembraItem si = labor.constructFeatureContainer(simpleFeature);
			
			si.setDosisHa(dosisToKgHa.apply(si.getDosisHa()));

			Double kgM2 = si.getDosisHa()/ProyectionConstants.METROS2_POR_HA;//kg/m2
			double semM2= (1000*1000*kgM2)/labor.getSemilla().getPesoDeMil();//sem/m2
			si.setDosisML(semM2*labor.getEntreSurco());// 1/entresurco=ml/m2 => sem/m2

			featureNumber++;
			updateProgress(featureNumber/divisor, featureCount);
			Object geometry = si.getGeometry();

			/**
			 * si la geometria es un point procedo a poligonizarla
			 */
			if (geometry instanceof Point) {
				//					Point longLatPoint = (Point) geometry;
				//
				//				
				//					if(	lastX!=null && labor.getConfiguracion().correccionDistanciaEnabled()){
				//						
				//						double aMetros=1;// 1/ProyectionConstants.metersToLongLat;
				//					//	BigDecimal x = new BigDecimal();
				//						double deltaY = longLatPoint.getY()*aMetros-lastX.getY()*aMetros;
				//						double deltaX = longLatPoint.getX()*aMetros-lastX.getX()*aMetros;
				//						if(deltaY==0.0 && deltaX ==0.0|| lastX.equals(longLatPoint)){
				//							puntosEliminados++;
				//						//	System.out.println("salteando el punto "+longLatPoint+" porque tiene la misma posicion que el punto anterior "+lastX);
				//							continue;//ignorar este punto
				//						}
				//					
				//						double tan = deltaY/deltaX;//+Math.PI/2;
				//						rumbo = Math.atan(tan);
				//						rumbo = Math.toDegrees(rumbo);//como esto me da entre -90 y 90 le sumo 90 para que me de entre 0 180
				//						rumbo = 90-rumbo;
				//
				//						/**
				//						 * 
				//						 * deltaX=0.0 ;deltaY=0.0
				//						 *	rumbo1=NaN
				//						 *	rumbo0=310.0
				//						 */
				//					
				//						if(rumbo.isNaN()){//como el avance en x es cero quiere decir que esta yerndo al sur o al norte
				//							if(deltaY>0){
				//								rumbo = 0.0;
				//							}else{
				//								rumbo=180.0;
				//							}
				//						}
				//
				//						if(deltaX<0){//si el rumbo estaba en el cuadrante 3 o 4 sumo 180 para volverlo a ese cuadrante
				//							rumbo = rumbo+180;
				//						}
				//						ci.setRumbo(rumbo);
				//
				//					}
				//				
				//					lastX=longLatPoint;
				//					Double alfa  = Math.toRadians(rumbo) + Math.PI / 2;
				//
				//					// convierto el ancho y la distancia a verctores longLat poder
				//					// operar con la posicion del dato
				//					Coordinate anchoLongLat = constructCoordinate(alfa,ancho);
				//					Coordinate distanciaLongLat = constructCoordinate(alfa+ Math.PI / 2,distancia);
				//
				//
				//					if(labor.getConfiguracion().correccionDemoraPesadaEnabled()){
				//						Double corrimientoPesada =	labor.getConfiguracion().getCorrimientoPesada();
				//						Coordinate corrimientoLongLat =constructCoordinate(alfa + Math.PI / 2,corrimientoPesada);
				//						// mover el punto 3.5 distancias hacia atras para compenzar el retraso de la pesada
				//
				//						longLatPoint = longLatPoint.getFactory().createPoint(new Coordinate(longLatPoint.getX()+corrimientoPesada*distanciaLongLat.x,longLatPoint.getY()+corrimientoPesada*distanciaLongLat.y));
				//						//utmPoint = utmPoint.getFactory().createPoint(new Coordinate(utmPoint.getX()-corrimientoLongLat.x,utmPoint.getY()-corrimientoLongLat.y));
				//					}
				//
				//					/**
				//					 * creo la geometria que corresponde a la feature tomando en cuenta si esta activado el filtro de distancia y el de superposiciones
				//					 */				
				//					//				Geometry utmGeom = createGeometryForHarvest(anchoLongLat,
				//					//						distanciaLongLat, utmPoint,pasada,altura,ci.getRindeTnHa());		
				//					Geometry longLatGeom = createGeometryForHarvest(anchoLongLat,
				//							distanciaLongLat, longLatPoint,pasada,altura,ci.getRindeTnHa());
				//					if(longLatGeom == null 
				//							//			|| geom.getArea()*ProyectionConstants.A_HAS*10000<labor.config.supMinimaProperty().doubleValue()
				//							){//con esto descarto las geometrias muy chicas
				//						//System.out.println("geom es null, ignorando...");
				//						continue;
				//					}
				//
				//					/**
				//					 * solo ingreso la cosecha al arbol si la geometria es valida
				//					 */
				//					boolean empty = longLatGeom.isEmpty();
				//					boolean valid = longLatGeom.isValid();
				//					boolean big = (longLatGeom.getArea()*ProyectionConstants.A_HAS>supMinimaHas);
				//					if(!empty
				//							&&valid
				//							&&big//esta fallando por aca
				//							){
				//
				//						//Geometry longLatGeom =	crsAntiTransform(utmGeom);//hasta aca se entrega la geometria correctamente
				//
				//						ci.setGeometry(longLatGeom);//FIXME aca ya perdio la orientacion pero tiene la forma correcta
				//					//	corregirRinde(ci);
				//
				//						labor.insertFeature(ci);//featureTree.insert(geom.getEnvelopeInternal(), cosechaFeature);
				//					} else{
				//						//	System.out.println("no inserto el feature "+featureNumber+" porque tiene una geometria invalida empty="+empty+" valid ="+valid+" area="+big+" "+geom);
				//					}

			} else { // no es point. Estoy abriendo una siembra de poligonos.
				labor.insertFeature(si);
			}
		}// fin del for que recorre las cosechas por indice
		reader.close();

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = ConvertirASiembraTask.buildTooltipText(siembraFeature, area,labor);
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	
	}

	protected  int getAmountMin(){return 0;} 
	protected  int gerAmountMax() {return 1;} 

}// fin del task

