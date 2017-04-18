package tasks;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import utils.ProyectionConstants;

public class RecomendFertPFromHarvestMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	private CosechaLabor cosecha;

	public RecomendFertPFromHarvestMapTask(FertilizacionLabor labor,CosechaLabor c) {
		super(labor);
		this.cosecha =c;
	}
		public void doProcess() throws IOException {
			FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
			featureNumber=cosecha.outCollection.size();
			List<FertilizacionItem> itemsToShow = new ArrayList<FertilizacionItem>();
			Cultivo cultivo = cosecha.producto.getValue();
			Fertilizante fert = this.labor.fertilizante.getValue();
			
			while (reader.hasNext()) {
				SimpleFeature simpleFeature = reader.next();
				CosechaItem ci = cosecha.constructFeatureContainer(simpleFeature);
				FertilizacionItem fi =null;
				synchronized(labor){
					fi= new FertilizacionItem();					
					fi.setId(labor.getNextID());
				}
				
				fi.setGeometry(ci.getGeometry());
				double extraccionP = ci.getRindeTnHa()*cultivo.getExtP();
				double reposicionP = extraccionP/(fert.getPorcP()/100);
				fi.setCantFertHa(reposicionP);
				fi.setElevacion(10d);
				fi.setPrecioFert(labor.precioInsumoProperty.get());
				fi.setPrecioPasada(labor.precioLaborProperty.get());
				//segun el cultivo de la cosecha
				
				//TODO terminar de crear el FertilizacionItem
				labor.insertFeature(fi);
				itemsToShow.add(fi);
				featureNumber++;
				updateProgress(featureNumber, featureCount);
		}
			reader.close();

		
		
			labor.constructClasificador();
			runLater(itemsToShow);
			updateProgress(0, featureCount);	
	}
		
		@Override
		protected void getPathTooltip(Geometry poly, FertilizacionItem fertFeature) {

			double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
			//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
			DecimalFormat df = new DecimalFormat("#.00");

			String tooltipText = new String(// TODO ver si se puede instalar un
					// boton
					// que permita editar el dato
					"Densidad: " + df.format(fertFeature.getCantFertHa())
					+ " Kg/Ha\n" + "Costo: "
					+ df.format(fertFeature.getImporteHa()) + " U$S/Ha\n"
					//+ "Sup: "
					//+ df.format(area * ProyectionConstants.METROS2_POR_HA)
					//+ " m2\n"
					// +"feature: " + featureNumber
					);
			if(area<1){
				tooltipText=tooltipText.concat( "Sup: "+df.format(area * ProyectionConstants.METROS2_POR_HA) + "m2\n");
				//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
			} else {
				tooltipText=tooltipText.concat("Sup: "+df.format(area ) + "Has\n");
			}

			//List  paths = 
			super.getRenderPolygonFromGeom(poly, fertFeature,tooltipText);

			//return null;
		}
		protected int getAmountMin() {
			return 0;
		}

		protected int gerAmountMax() {
			return 1000;
		}
}
