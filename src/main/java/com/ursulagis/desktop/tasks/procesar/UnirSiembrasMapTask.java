package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.tasks.crear.ConvertirASiembraTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class UnirSiembrasMapTask extends ProcessMapTask<SiembraItem,SiembraLabor> {
	/**
	 * la lista de las cosechas a unir
	 */
	private List<SiembraLabor> siembras;
	//private boolean calibrar;

	public UnirSiembrasMapTask(List<SiembraLabor> _siembras){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		this.siembras=new ArrayList<SiembraLabor>();
		for(SiembraLabor l:_siembras){
			if(l.getLayer().isEnabled()){
				this.siembras.add(l);
			}
		};
		SiembraLabor sRef = this.siembras.get(0);
		super.labor = new SiembraLabor();
		//TODO asignar las columnas a  los valores estanar
		labor.colAmount.set(SiembraLabor.COLUMNA_KG_SEMILLA);
		labor.colDosisSemilla.set(SiembraLabor.COLUMNA_KG_SEMILLA);
		labor.colAncho.set(SiembraLabor.COLUMNA_ANCHO);
		labor.colCurso.set(SiembraLabor.COLUMNA_CURSO);
		labor.colDistancia.set(SiembraLabor.COLUMNA_DISTANCIA);
		labor.colElevacion.set(SiembraLabor.COLUMNA_ELEVACION);
		//labor.colVelocidad.set(CosechaLabor.COLUMNA_VELOCIDAD);
		//labor.colPasada.set(CosechaLabor.COLUMNA_ANCHO);
		labor.setSemilla(sRef.getSemilla());
		labor.setEntreSurco(sRef.getEntreSurco());
		labor.setPrecioLabor(sRef.getPrecioLabor());
		labor.setPrecioInsumo(sRef.getPrecioInsumo());
		
		labor.getConfiguracion().valorMetrosPorUnidadDistanciaProperty().set(1.0);
		//labor.getConfiguracion().correccionFlowToRindeProperty().setValue(false);
		String nombreProgressBar = "clonar siembras";
		if(_siembras.size()>1){
			nombreProgressBar = "unir siembras";
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
		String prefijo = "Grilla";
		if(siembras.size()>1){
			prefijo = "Union";
		}
		int featuresInsertadas=0;
		//TODO agregar una etapa de calibracion de cosechas
		//buscar las cosechas que mayor superposicion tienen
		//tirar puntos al azar dentro del area superpuesta y calcular el promedio de los puntos de las 2 cosechas en ese area y el coeficiente de conversion entre una cosecha y la otra.
	
		
		for(SiembraLabor c:siembras){
			if(nombre == null){
				nombre=prefijo+" "+c.getNombre();	
			}else {
				nombre+=" - "+c.getNombre();
			}
			//TODO preguntar si se desea calibrar las cosechas
		//	Double coeficienteConversion =calcCoeficientesConversion(c);	//XXX calibrar elevacion tambien?
			
			
			FeatureReader<SimpleFeatureType, SimpleFeature> reader = c.outCollection.reader();
			while(reader.hasNext()){
				SimpleFeature f = reader.next();
				SiembraItem ci = labor.constructFeatureContainerStandar(f,true);
				//TODO multiplicar ci.rinde por el coeficiente de conversion
			
				ci.setDosisML(ci.getDosisML()*10);//XXX verificar que ande para otras unidades
				//SimpleFeature nf=ci.getFeature(labor.getFeatureBuilder());
				labor.insertFeature(ci);
				//boolean ret = labor.outCollection.add(nf);
				featuresInsertadas++;			
			}
			
			reader.close();
		}
		
		System.out.println("inserte "+featuresInsertadas+" elementos");
		int elementosContiene = labor.outCollection.getCount();
		System.out.println("la labor contiene "+elementosContiene+" elementos");
		if(featuresInsertadas != elementosContiene){
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


		
		siembras.forEach((c)->c.getLayer().setEnabled(false));


		runLater(this.getItemsList());
		updateProgress(0, featureCount);
		long time=System.currentTimeMillis()-init;
		System.out.println("tarde "+time+" milisegundos en unir las cosechas.");
	}

	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = poly.getArea() *ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = ConvertirASiembraTask.buildTooltipText(siembraFeature, area,labor);
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,tooltipText,renderablePolygon);	
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
