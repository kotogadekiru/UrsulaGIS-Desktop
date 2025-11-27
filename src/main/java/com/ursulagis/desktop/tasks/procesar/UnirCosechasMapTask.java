package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.geotools.api.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.CrearCosechaMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class UnirCosechasMapTask extends ProcessMapTask<CosechaItem,CosechaLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<CosechaLabor> cosechas;
	private boolean calibrar;

	public UnirCosechasMapTask(List<CosechaLabor> cosechas){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.cosechas=new ArrayList<CosechaLabor>();
		for(CosechaLabor l:cosechas){
			if(l.getLayer().isEnabled()){
				this.cosechas.add(l);
			}
		};

		super.labor = new CosechaLabor();
		//TODO asignar las columnas a  los valores estanar
		labor.colAmount.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
		labor.colRendimiento.set(CosechaLabor.CosechaLaborConstants.COLUMNA_RENDIMIENTO);
		labor.colAncho.set(CosechaLabor.COLUMNA_ANCHO);
		labor.colCurso.set(CosechaLabor.COLUMNA_CURSO);
		labor.colDistancia.set(CosechaLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(CosechaLabor.COLUMNA_ELEVACION);
		//labor.colVelocidad.set(CosechaLabor.COLUMNA_VELOCIDAD);
		//labor.colPasada.set(CosechaLabor.COLUMNA_ANCHO);

		labor.getConfiguracion().valorMetrosPorUnidadDistanciaProperty().set(1.0);
		labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		String nombreProgressBar = "clonar cosecha";
		if(cosechas.size()>1){
			nombreProgressBar = "unir cosechas";
		}
		labor.setNombre(nombreProgressBar);//este es el nombre que se muestra en el progressbar
	}

	/**
	 * proceso que toma una lista de cosechas y las une sin tener en cuenta superposiciones ni nada
	 */
	@Override
	protected void doProcess() throws IOException {
		long init = System.currentTimeMillis();
		// TODO 1 obtener el bounds general que cubre a todas las cosechas
		//	ReferencedEnvelope unionEnvelope = null;
		//double ancho = labor.getConfiguracion().getAnchoFiltroOutlayers();
		String nombre =null;
		String prefijo = "Clon";
		if(cosechas.size()>1){
			prefijo = "Union";
		}
		int featuresInsertadas=0;
		//TODO agregar una etapa de calibracion de cosechas
		//buscar las cosechas que mayor superposicion tienen
		//tirar puntos al azar dentro del area superpuesta y calcular el promedio de los puntos de las 2 cosechas en ese area y el coeficiente de conversion entre una cosecha y la otra.
	
		cosechas.sort((c1,c2)->{//ordenar segun cuantas superposiciones tenga
			int sup1 = calcNSup(c1);
			int sup2 = calcNSup(c2);
			
			return Integer.compare(sup1,sup2);
		});
		
		for(CosechaLabor c:cosechas){
			if(nombre == null){
				nombre=prefijo+" "+c.getNombre();	
			}else {
				nombre+=" - "+c.getNombre();
			}
			//TODO preguntar si se desea calibrar las cosechas
			Double coeficienteConversion =calcCoeficientesConversion(c);	//XXX calibrar elevacion tambien?
			
			
			FeatureReader<SimpleFeatureType, SimpleFeature> reader = c.outCollection.reader();
			while(reader.hasNext()){
				SimpleFeature f = reader.next();
				CosechaItem ci = labor.constructFeatureContainerStandar(f,true);
				//TODO multiplicar ci.rinde por el coeficiente de conversion
			
				ci.setRindeTnHa(ci.getRindeTnHa()*coeficienteConversion);
				SimpleFeature nf=ci.getFeature(labor.getFeatureBuilder());
				
				boolean ret = labor.outCollection.add(nf);
				featuresInsertadas++;
				if(!ret){
					System.out.println("no se pudo agregar la feature "+f);
				}
			}
			
			reader.close();
			//mantengo los valores configurados al clonar
			labor.setPrecioInsumo(c.getPrecioInsumo());
			labor.setCostoCosechaTn(c.getCostoCosechaTn());
			labor.setPrecioLabor(c.getPrecioLabor());
			labor.setCultivo(c.getCultivo());
			labor.setFecha(c.getFecha());
			labor.getMinRindeProperty().set(c.getMinRindeProperty().get());
			labor.getMaxRindeProperty().set(c.getMaxRindeProperty().get());
			labor.getConfiguracion().toleranciaCVProperty().set(c.getConfiguracion().toleranciaCVProperty().get());
			labor.getConfiguracion().anchoFiltroOutlayersProperty().set(c.getConfiguracion().anchoFiltroOutlayersProperty().get());
			labor.anchoDefaultProperty.set(c.getAnchoDefaultProperty().get());
			labor.getConfiguracion().supMinimaProperty().set(c.getConfiguracion().supMinimaProperty().get());
			
			
			
		}
		
		System.out.println("inserte "+featuresInsertadas+" elementos");
		int elementosContiene = labor.outCollection.getCount();
		System.out.println("la labor contiene "+elementosContiene+" elementos");
		if(featuresInsertadas!=elementosContiene){
			System.out.println("no se insertaron todos los elementos con exito.");
		}
		labor.setNombre(nombre);
		labor.setLayer(new LaborLayer());


		//TODO 4 mostrar la cosecha sintetica creada
//		if(cosechas.size()==1){
//			CosechaLabor original = cosechas.get(0);
//			Clasificador co=original.getClasificador();
//			 labor.clasificador=co.clone();
//			
//		} else{
			labor.constructClasificador();
//		}

		List<CosechaItem> itemsToShow = new ArrayList<CosechaItem>();

			SimpleFeatureIterator it = labor.outCollection.features();
			while(it.hasNext()){
				SimpleFeature f=it.next();
				itemsToShow.add(labor.constructFeatureContainerStandar(f,false));
			}
			it.close();
		
			cosechas.forEach((c)->c.getLayer().setEnabled(false));

		//this.pathTooltips.clear();
//		labor.getLayer().removeAllRenderables();
//		for(CosechaItem c:itemsToShow){
//			Geometry g = c.getGeometry();
//			if(g instanceof Polygon){
//						getPathTooltip((Polygon)g,c);
//			} else if(g instanceof MultiPolygon){
//				MultiPolygon mp = (MultiPolygon)g;			
//				for(int i=0;i<mp.getNumGeometries();i++){
//					Polygon p = (Polygon) (mp).getGeometryN(i);
//					getPathTooltip(p,c);
//				}
//
//			}
//		}
		runLater(itemsToShow);
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde "+time+" milisegundos en unir las cosechas.");
	}

	/**
	 * metodo que calcula la cantidad de superposiciones de la cosecha con las otras cosechas
	 * @param c
	 * @return
	 */
	private int calcNSup(CosechaLabor cosecha) {
		int nSup =0;
		Envelope cosechaEnvelope = cosecha.outCollection.getBounds();
		for(CosechaLabor c : cosechas) {
			if(c.outCollection.getBounds().intersects(cosechaEnvelope)) {
				nSup++;
			}			
		}
		
		return nSup;
	}
	
	/**
	 * metodo que genera puntos al azar sobre la interseccion para calcular el coeficiente de conversion entre la cosecha y la labor conjunta
	 * @param cosecha
	 * @return
	 */
	private Double calcCoeficientesConversion(CosechaLabor cosecha) {
		Double coeficienteConversion=1.0;

		if(!calibrar)return coeficienteConversion;
		Envelope b1 = cosecha.outCollection.getBounds();
		Envelope b2 = labor.outCollection.getBounds();		

		if(b1.intersects(b2)) {
			Envelope i = b1.intersection(b2);

			//TODO subdividir i en envelopes mas chicos para hacer menos cuentas
			List<CosechaItem> itemsCosecha = cosecha.outStoreQuery(i);



			double sumCoef=0;
			int count =0;
			double N = Math.sqrt(itemsCosecha.size());
			for(int vez=0;vez<N;vez++) {
				int randIndex = (int) (Math.random()*(itemsCosecha.size()-1));
				CosechaItem itemCosecha =itemsCosecha.get(randIndex);
				Envelope itemEnvelope = itemCosecha.getGeometry().getEnvelopeInternal();
				itemEnvelope.expandBy(ProyectionConstants.metersToLongLat(10));
				List<CosechaItem> itemsLabor = labor.outStoreQuery(itemEnvelope);
				//	OptionalDouble promedioRindeCosecha = itemsCosecha.stream().mapToDouble((item)-> item.getRindeTnHa()).average();
				OptionalDouble promedioRindeLabor = itemsLabor.stream().mapToDouble((item)-> item.getRindeTnHa()).average();

				if(promedioRindeLabor.isPresent() && itemCosecha.getRindeTnHa()>0) {
					sumCoef += promedioRindeLabor.getAsDouble()/itemCosecha.getRindeTnHa();
					count++;
				}

			}
			if(count>0)	coeficienteConversion=sumCoef/count;
		}
		//TODO remover comentario
		System.out.println("el coeficiente de conversion para "+cosecha.getNombre()+" es "+coeficienteConversion);
		return coeficienteConversion;
	}
	
//	@Override
//	protected ExtrudedPolygon getPathTooltip(Geometry poly,	CosechaItem cosechaItem,ExtrudedPolygon  renderablePolygon) {
//		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
//		String tooltipText = CrearCosechaMapTask.buildTooltipText(cosechaItem, area);
//		return super.getExtrudedPolygonFromGeom(poly, cosechaItem,tooltipText,renderablePolygon);
//	}

	@Override
	protected int getAmountMin() {
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		return 0;
	}

	public void setCalibrar(boolean calibrar) {
		this.calibrar=calibrar;
		
	}

}
