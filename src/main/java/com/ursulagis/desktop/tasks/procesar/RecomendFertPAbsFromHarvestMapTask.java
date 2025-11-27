package com.ursulagis.desktop.tasks.procesar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.DoubleStream;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearFertilizacionMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;
/**
 * Task que toma el suelo y recomienda cuanto fertilizante aplicar para llegar al requerimiento de absorcion del cultivo
 */
public class RecomendFertPAbsFromHarvestMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	private CosechaLabor cosecha;
	private List<Suelo> suelos;
	private List<FertilizacionLabor> fertilizaciones;
	private Double minFert=null;
	private Double maxFert=null;
	private Double ppmObj=null;

	public RecomendFertPAbsFromHarvestMapTask(FertilizacionLabor labor,CosechaLabor cosechaEstimada, List<Suelo> _suelos, List<FertilizacionLabor> _fert) {
		super(labor);
		this.cosecha =cosechaEstimada;
		this.suelos=_suelos;
		this.fertilizaciones=_fert;
	}

	public void doProcess() {	
		try {
			featureCount = cosecha.outCollection.size();
			//List<FertilizacionItem> itemsToShow = new ArrayList<FertilizacionItem>();
			Cultivo cultivo = cosecha.getCultivo();
			Fertilizante fert = this.labor.fertilizante;

			//TODO calcular el balance de nitrogeno en el suelo luego de la fertilizacion 
			//y completar lo que haga falta para la cosecha estimada;
			List<CosechaItem> cItems = new ArrayList<CosechaItem>();
			FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
			while (reader.hasNext()) {
				SimpleFeature simpleFeature = reader.next();
				CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);
				cItems.add(ci);
			}
			reader.close();
			ForkJoinPool myPool = new ForkJoinPool(4);			
			myPool.submit(() ->{

				cItems.parallelStream().forEach(cItem->{
					try {
						FertilizacionItem fi = new FertilizacionItem();			
						//synchronized(labor){							
						fi.setId(labor.getNextID());
						labor.setPropiedadesLabor(fi);
						//}
						Geometry geom = PolygonValidator.validate(cItem.getGeometry());
						if(geom==null) {
							System.out.println("item geom es null");
							return;
						}
						Double areaGeom =  ProyectionConstants.A_HAS(geom.getArea());
						
						fi.setGeometry(geom);
						double absP = cItem.getRindeTnHa()*cultivo.getAbsP();
						double dispPSueloKg = getPDisponibleSuelo(geom);//incluye el organico a mineralizar.

						//System.out.println("kg disponible suelo: " + dispNSuelo+ "areaGeom:"+areaGeom);
						double dispPSueloKgHa = dispPSueloKg / areaGeom;

						double dispNFertKgHa = getPDisponibleFert(geom) / areaGeom;
						//System.out.println("absN="+absN+" dispSuelo="+dispNSuelo+" dispNFert="+dispNFert);
						double nAAplicar= absP-dispPSueloKgHa-dispNFertKgHa;


						nAAplicar = Math.max(0, nAAplicar);
						double reposicionN = fert.getPorcP()>0?nAAplicar / (fert.getPorcP()/100):0;

						//	System.out.println("Rinde "+cItem.getAmount()+" Nreq "+absN+" nAAplicar="+nAAplicar+" "+fert.getNombre()+" "+reposicionN);

						if(this.minFert != null && this.minFert>0 &&this.minFert > reposicionN) {
							reposicionN = minFert;
						}
						if(this.maxFert != null && this.maxFert>0 && this.maxFert < reposicionN) {
							reposicionN = maxFert;
						}
						fi.setDosistHa(reposicionN);
						fi.setElevacion(10d);
						labor.setPropiedadesLabor(fi);
						//segun el cultivo de la cosecha

						//System.out.println("insertando "+fi);
						labor.insertFeature(fi);
						//itemsToShow.add(fi);
						featureNumber++;
						//System.out.println("inserte la feature "+featureNumber+" "+fi);
						updateProgress(featureNumber, featureCount);
					}catch(Exception e) {
						System.err.println("error al procesar el item "+cItem);
						e.printStackTrace();
					}
				});
			}
			//list.parallelStream().forEach(/* Do Something */);
					).get();
			System.out.println("construyendo clasificador");
			labor.constructClasificador();
			runLater(getItemsList());
			updateProgress(0, featureCount);	
		}catch(Exception e) {
			e.printStackTrace();
		}
	}


	private Double getPDisponibleFert(Geometry geometry) {
		Double kgPFert = 0.0;
		kgPFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Fertilizante fertilizante = fertilizacion.fertilizante;
			return items.parallelStream().flatMapToDouble(item->{
				Double kgPHa = (Double) item.getDosistHa() * fertilizante.getPorcP()/100;
				Geometry fertGeom = item.getGeometry();				
				fertGeom = PolygonValidator.validate(fertGeom);//puede ser null
				if(fertGeom == null || !fertGeom.intersects(geometry))return DoubleStream.of(0.0);
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(fertGeom, geometry);
					//Geometry inteseccionGeom = geometry.intersection(fertGeom);// Computes a
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgPHa * area);				
			});
		}).sum();
		//			System.out.println("la cantidad de ppmP acumulado en el suelo es = "
		//					+ kPFert);

		return kgPFert;
	}

//	def calcDosisCallBack():
//	    deseado = float(E1.get().replace(",", "."))
//	    actual = float(E2.get().replace(",", "."))
//	    arcilla = float(Slider.get())
//	    faltante = deseado - actual
//	    Kg_Ha = faltante * ((108.08 + 0.91 * arcilla) / 30)
//	    Kg_P205 = Kg_Ha * 2.29  # Coef. fijo
//	    dosis = Kg_P205 / float(combo_dict.get((combo.get())) / 100)
//	    print("{0:.2f}".format(dosis))
//	    tkinter.messagebox.showinfo("Dosis a aplicar", "{0:.2f}".format(dosis) + " litros por ha")
//	    
	
	private Double getPDisponibleSuelo(Geometry geometry) {
		Double kgPSuelo = 0.0;
		//boolean estival = this.cosecha.getCultivo().isEstival();
		kgPSuelo=	suelos.stream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			//System.out.println("hay "+items.size()+" items en el suelo "+suelo.nombre);
			return items.parallelStream().flatMapToDouble(item->{
				//descuento el ppmObj para solo devolver el ppm disponible sobre el obj
				Double kgPHa=Suelo.ppmToKg(item.getDensAp(),item.getPpmP()-ppmObj,0.2);
						//suelo.getKgPHa(item);// (Double) item.getPpmN()*kgSueloHa*Fertilizante.porcN_NO3/1000000;
//				double porcOrgDisponible = (1d/3);// esto era 0.0 porque estaba inicializado con la division entera de 1/3 que es 0. //que feo!
//				if(estival)porcOrgDisponible=(2d/3);
//				Double kgNOrganicoHa= suelo.getKgNOrganicoHa(item)*porcOrgDisponible;//divido la mineralizacion anual a la mitad. pero depende del cultivo

				Geometry geom = item.getGeometry();				
				geom = PolygonValidator.validate(geom);
				if(!geom.intersects(geometry))return DoubleStream.of(0.0);
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geom, geometry);
					//Geometry inteseccionGeom = geometry.intersection(geom);// Computes a
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				//System.out.println("los kgN/ha para ppmN: "+item.getPpmN()+" es "+kgNHa+ " has es " +area);
				return DoubleStream.of( (kgPHa)* area);				
			});
		}).sum();
		//					System.out
		//					.println("la cantidad de kgNSuelo acumulado en el suelo es = "
		//							+ kgNSuelo+" en "+ ProyectionConstants.A_HAS(geometry.getArea()));

		return kgPSuelo;
	}

//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearFertilizacionMapTask.buildTooltipText(fertFeature, area); 
//		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
//	}

	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 1000;
	}

	public void setMinFert(Double _minFert) {
		this.minFert=_minFert;

	}
	public void setMaxFert(Double _maxFert) {
		this.maxFert=_maxFert;

	}

	public void setPpmPObj(Double _ppmObjD) {
		if(_ppmObjD==null) {
			this.ppmObj=0.0;
		}
		this.ppmObj=_ppmObjD;		
	}
}
