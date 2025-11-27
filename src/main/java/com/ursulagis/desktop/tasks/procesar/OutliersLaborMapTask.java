package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.function.Function;

import org.geotools.api.data.FeatureReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class OutliersLaborMapTask extends ProcessMapTask<LaborItem,Labor<LaborItem>> {
	/**
	 * la lista de las cosechas a unir
	 */
	//private Labor<LaborItem> laborAFiltrar=null;
	private double anchoFiltroOuliers=50;
	private double minRinde=Double.MIN_VALUE;
	private double maxRinde=Double.MAX_VALUE;;
	private double toleranciaCoeficienteVariacion=0;
	
	public OutliersLaborMapTask(Labor<LaborItem> _laborACortar,Double ancho, Double min, Double max, Double tol){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(_laborACortar);
		//this.laborAFiltrar=_laborACortar;		
		if(ancho!=null)this.anchoFiltroOuliers=ancho;
		if(min!=null)this.minRinde=min;
		if(max!=null)this.maxRinde=max;
		if(tol!=null)this.toleranciaCoeficienteVariacion=tol;
	
		Map<Class, Function<Labor, Labor>> constructor = ClonarLaborMapTask.laborConstructor();
		
//		this.tooltipCreator = constructTooltipCreator();
	
		//this.labor=constructor.get(this.laborAFiltrar.getClass()).apply(laborAFiltrar);	

		labor.setNombre(_laborACortar.getNombre()+" out");//este es el nombre que se muestra en el progressbar
	}

	

	/**
	 * proceso que toma una cosecha y selecciona los items que estan dentro de los poligonos seleccionados
	 */
	@Override
	protected void doProcess() throws IOException {

		
		corregirOutlayersParalell();
		//labor.setLayer(new LaborLayer());
		labor.constructClasificador();

		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	/**
	 * metodo que toma los elementos en outCollection y los cambia por el promedio
	 * de su entorno si es un outlayer.
	 * el entorno esta difinido por un circulo de radio igual al ancho outlayers configurado y centrado en el elemento
	 * se define como outlayer si el desvio entre el valor del elemento y el promedio de su entorno es mayor a la tolerancia configurada
	 * el metodo realiza la tarea en forma paralelizada
	 */
	private void corregirOutlayersParalell() {
		//1) crear un circulo de radio a definir y centro en el centroide de la cosecha
		double ancho = this.anchoFiltroOuliers;

		int initOutCollectionSize = labor.outCollection.size();
		final DoubleProperty done=new SimpleDoubleProperty(0);
		SimpleFeature[] arrayF = new SimpleFeature[labor.outCollection.size()];
		labor.outCollection.toArray(arrayF);
		List<SimpleFeature> outFeatures = Arrays.asList(arrayF);
		List<SimpleFeature>  filteredFeatures = outFeatures.parallelStream().collect(
				()-> new  ArrayList<SimpleFeature>(),
				(list, pf) ->{		
					try{
						LaborItem cosechaFeature = labor.constructFeatureContainerStandar(pf,false);
						Point X = cosechaFeature.getGeometry().getCentroid();	
						//Polygon poly= GeometryHelper.constructPolygon(new Coordinate(ancho,0), new Coordinate(0,ancho), X);
						Polygon poly = createGeomPoint(X,ancho,ancho,0,0);
						List<LaborItem> features = labor.cachedOutStoreQuery(poly.getEnvelopeInternal());
						if(features.size()>0){						
							outlayerCV(cosechaFeature, poly,features);		
							//XXX porque creo un featureBuilder por cada feature? sera para que pueda hacerlo en paralelo?
							SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(labor.getType());
							SimpleFeature f = cosechaFeature.getFeature(fBuilder);
							list.add(f);	
							//This method is safe to be called from any thread.	
							done.set(done.get()+1);
							updateProgress(done.get(), initOutCollectionSize);
						} else{
							System.out.println("la query devolvio cero elementos"); //$NON-NLS-1$
						}
					}catch(Exception e){
						System.err.println("error en corregirOutliersParalell"); //$NON-NLS-1$
						e.printStackTrace();
					}
				},	(list1, list2) -> list1.addAll(list2));
		// esto termina bien. filteredFeatures tiene 114275 elementos como corresponde

		DefaultFeatureCollection newOutcollection =  new DefaultFeatureCollection("internal",labor.getType());		 //$NON-NLS-1$
		boolean res = newOutcollection.addAll(filteredFeatures);
		if(!res){
			System.out.println("fallo el addAll(filteredFeatures)"); 
		}

		labor.clearCache();

		int endtOutCollectionSize = newOutcollection.size();
		if(initOutCollectionSize !=endtOutCollectionSize){
			System.err.println("se perdieron elementos al hacer el filtro de outlayers. init="
					+initOutCollectionSize
					+" end="+endtOutCollectionSize); 
		}
		labor.setOutCollection(newOutcollection);
		featureCount=labor.outCollection.size();
	}
	
	/**
	 * 
	 * @param cosechaFeature
	 * @param poly es el area dentro de la que se calcula el outlayer
	 * @param features
	 * @return true si cosechaFeature fue modificada
	 */
	private boolean outlayerCV(LaborItem cosechaFeature, Polygon poly,	List<LaborItem> features) {
		boolean ret = false;
		Point geo = cosechaFeature.getGeometry().getCentroid();
		double rindeCosechaFeature = cosechaFeature.getAmount();
		double sumatoriaRinde = 0;			
		double sumatoriaAltura = 0;				
		double divisor = 0;
		// cambiar el promedio directo por el metodo de kriging de interpolacion. ponderando los rindes por su distancia al cuadrado de la muestra
		double ancho = this.anchoFiltroOuliers;
		//la distancia no deberia ser mayor que 2^1/2*ancho, me tomo un factor de 10 por seguridad e invierto la escala para tener mejor representatividad
		//en vez de tomar de 0 a inf, va de ancho*(10-2^1/2) a 0
		ancho = Math.sqrt(2)*ancho;


		ProyectionConstants.setLatitudCalculo(geo.getY());
		for(LaborItem cosecha : features){
			double cantidadCosecha = cosecha.getAmount();	
			Point geo2 = cosecha.getGeometry().getCentroid();
			
			double distancia =geo.distance(geo2)/ProyectionConstants.metersToLat();

			double distanciaInvert = (ancho-distancia);
			if(distanciaInvert<0) {
				distanciaInvert=0;
				System.out.println(Messages.getString("ProcessHarvestMapTask.19")+distanciaInvert); //$NON-NLS-1$
			}
			//los pesos van de ~ancho^2 para los mas cercanos a 0 para los mas lejanos
			double weight =  Math.pow(distanciaInvert,2);	
			if(isBetweenMaxMin(cantidadCosecha)){
				sumatoriaAltura+=cosecha.getElevacion()*weight;
				sumatoriaRinde+=cantidadCosecha*weight;
				divisor+=weight;		
			}			
		}
		boolean rindeEnRango = isBetweenMaxMin(rindeCosechaFeature);

		double promedioRinde = 0.0;
		double promedioAltura = 0.0;
		if(divisor>0){
			promedioRinde = sumatoriaRinde/divisor;
			//			promedioRinde = Math.min(promedioRinde,labor.maxRindeProperty.doubleValue());
			//			promedioRinde = Math.max(promedioRinde,labor.minRindeProperty.doubleValue());
			promedioAltura = sumatoriaAltura/divisor;
		}else{
			System.out.println(Messages.getString("ProcessHarvestMapTask.20")+ divisor); //$NON-NLS-1$
			System.out.println(Messages.getString("ProcessHarvestMapTask.21")+sumatoriaRinde); //$NON-NLS-1$
		}
		//4) obtener la varianza (LA DIF ABSOLUTA DEL DATO Y EL PROM DE LA MUESTRA) (EJ. ABS(10-9.3)/9.3 = 13%)
		//SI 13% ES MAYOR A TOLERANCIA CV% REEMPLAZAR POR PROMEDIO SINO NO

		if(!(promedioRinde==0)){
			double coefVariacionCosechaFeature = Math.abs(rindeCosechaFeature-promedioRinde)/promedioRinde;
			//cosechaFeature.setDesvioRinde(coefVariacionCosechaFeature);

			if(coefVariacionCosechaFeature > toleranciaCoeficienteVariacion ||!rindeEnRango){//si el coeficiente de variacion es mayor al 20% no es homogeneo
				//El valor esta fuera de los parametros y modifico el valor por el promedio
					System.out.println("reemplazo "+cosechaFeature.getAmount()+" por "+promedioRinde);
				cosechaFeature.setAmount(promedioRinde);

				cosechaFeature.setElevacion(promedioAltura);
				ret=true;
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param center centro del poligono a dibujar
	 * @param ancho
	 * @param distancia
	 * @param rumbo
	 * @param elevacion
	 * @return
	 */
	private Polygon createGeomPoint(Point center,double ancho, double distancia,double rumbo,double elevacion) {
		try {
			GeometryFactory fact = ProyectionConstants.getGeometryFactory();
			Polygon ret=null;
			//partir de center y calcular distancia/2 
			Point d = ProyectionConstants.getPoint(center,  rumbo, distancia/2);
			Point a = ProyectionConstants.getPoint(center,  (rumbo+90)%360, ancho/2);

			//       adelante
			// A ^^^^^^^^^^^^^^^ B
			//          |
			// D ^^^^^^^^^^^^^^^ C
			//        atras

			//center+(distancia-center)-(ancho-center)=center-distancia+ancho
			Coordinate A = new Coordinate(center.getX()+d.getX()-a.getX(),
					center.getY()+d.getY()-a.getY(),elevacion);
			//center+(distancia-center)+(ancho-center)=-center+distancia+ancho
			Coordinate B =  new Coordinate(-center.getX()+d.getX()+a.getX(),
					-center.getY()+d.getY()+a.getY(),elevacion);
			//center-(distancia-center)+(ancho-center)=center-distancia+ancho
			Coordinate C = new Coordinate(center.getX()-d.getX()+a.getX(),
					center.getY()-d.getY()+a.getY(),elevacion);
			//center-(distancia-center)-(ancho-center)=3*center-distancia+ancho
			Coordinate D = new Coordinate(3*center.getX()-d.getX()-a.getX(),
					3*center.getY()-d.getY()-a.getY(),elevacion);

			Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.
			ret = fact.createPolygon(coordinates);
			return ret;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean isBetweenMaxMin(double cantidadCosecha) {
		boolean ret = minRinde<=cantidadCosecha && cantidadCosecha<=maxRinde;
		if(!ret){
			//	System.out.println(cantidadCosecha+">"+labor.maxRindeProperty.doubleValue()+" o <"+labor.minRindeProperty.doubleValue());
		}
		return ret;
	}
	
	@Override
	protected int getAmountMin() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		// TODO Auto-generated method stub
		return 0;
	}
}
