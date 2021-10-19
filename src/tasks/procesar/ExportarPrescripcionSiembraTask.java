package tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.LaborItem;
import dao.config.Configuracion;
import dao.fertilizacion.FertilizacionItem;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gui.Messages;
import tasks.ProgresibleTask;
import utils.FileHelper;
import utils.PolygonValidator;


/**
 * para el cufia la semilla es la 3ra culumna, los datos tienen que estar en enteros
 * diferencia entre AGFusion y 
 * FGS (mas nuevo: permite mas flexibilidad en cantidad de zonas y cantidad de columnas. 
 * permite cargar las prescripciones en diferentes mapas) 
 * @author quero
 *
 */

public class ExportarPrescripcionSiembraTask extends ProgresibleTask<File>{
	private SiembraLabor laborToExport=null;
	private File shapeFile=null;
	private String unidad=null;
	
	public  ExportarPrescripcionSiembraTask(SiembraLabor _laborToExport,File _shapeFile,String _unidad) {	
		laborToExport=_laborToExport;
		shapeFile=_shapeFile;
		unidad=_unidad;
		
	}
	
	public File call() {
		SimpleFeatureType type = null;
		//*the_geom:    												:srid=4326,
		//	String typeDescriptor = Messages.getString("JFXMain.341")+Polygon.class.getCanonicalName()+Messages.getString("JFXMain.342") //$NON-NLS-1$ //$NON-NLS-2$
		//
		//		+ SiembraLabor.COLUMNA_DOSIS_LINEA + Messages.getString("JFXMain.343") //$NON-NLS-1$ :java.lang.Long,
		//		+ SiembraLabor.COLUMNA_DOSIS_COSTADO + Messages.getString("JFXMain.344") //$NON-NLS-1$ :java.lang.Long,
		//		//seeding
		//		+ Messages.getString("JFXMain.345") + Messages.getString("JFXMain.346"); //$NON-NLS-1$ :java.lang.Long

		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":srid=4326,"//$NON-NLS-1$
				+ SiembraLabor.COLUMNA_DOSIS_LINEA + ":java.lang.Long,"//$NON-NLS-1$
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO +":java.lang.Long,"//$NON-NLS-1$
				+unidad+":java.lang.Long";//$NON-NLS-1$ semilla siempre tiene que ser la 3ra columna

		System.out.println("creando type con: "+typeDescriptor); //$NON-NLS-1$ the_geom:Polygon:srid=4326,Fert L:java.lang.Long,Fert C:java.lang.Long,seeding:java.lang.Long
		System.out.println("Long.SIZE="+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
		try {
			//type = DataUtilities.createType(Messages.getString("JFXMain.349"), typeDescriptor); //$NON-NLS-1$
			type = DataUtilities.createType("PrescType", typeDescriptor); //$NON-NLS-1$
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		System.out.println("PrescType: "+DataUtilities.spec(type));//PrescType: the_geom:Polygon,Rate:java.lang.Long //$NON-NLS-1$


		List<LaborItem> items = new ArrayList<LaborItem>();

		SimpleFeatureIterator it = laborToExport.outCollection.features();
		while(it.hasNext()){
			SiembraItem fi = laborToExport.constructFeatureContainerStandar(it.next(),false);
			items.add(fi);
		}
		it.close();

		int zonas = items.size();

		if(zonas>=100) {
			reabsorverZonasChicas(items);
		}

		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok

		for(LaborItem i:items) {//(it.hasNext()){
			SiembraItem fi=(SiembraItem) i;
			Geometry itemGeometry=fi.getGeometry();
			List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
			for(Polygon p : flatPolygons){
				fb.add(p);
//				availableColums.add(SiembraLabor.COLUMNA_SEM_10METROS);//("Sem10ml");
//				availableColums.add(SiembraLabor.COLUMNA_DOSIS_SEMILLA);//("kgSemHa");
//				availableColums.add(SiembraLabor.COLUMNA_MILES_SEM_HA);//("MilSemHa");
				Double semilla = fi.getDosisML()*10;///XXX aca hago magia para convertir de plantas por metro a plantas cada 10 metros
				if(SiembraLabor.COLUMNA_DOSIS_SEMILLA.equals(unidad)) {
					semilla = fi.getDosisHa();
				} else if(SiembraLabor.COLUMNA_MILES_SEM_HA.equals(unidad)) {
					semilla = fi.getDosisML()*(10/laborToExport.getEntreSurco());
				}
				Double linea = fi.getDosisFertLinea();
				Double costado = fi.getDosisFertCostado();

				//System.out.println("presc Dosis ="+semilla); //$NON-NLS-1$
				fb.add(linea);
				fb.add(costado);
				fb.add(Math.round(semilla));

				SimpleFeature exportFeature = fb.buildFeature(fi.getId().toString());
				exportFeatureCollection.add(exportFeature);
			}
		}
		//it.close();

		ShapefileDataStore newDataStore = FileHelper.createShapefileDataStore(shapeFile,type);//aca el type es GeometryDescriptorImpl the_geom <MultiPolygon:MultiPolygon> nillable 0:1 
		SimpleFeatureSource featureSource = null;
		try {
			String typeName = newDataStore.getTypeNames()[0];
			featureSource = newDataStore.getFeatureSource(typeName);
		} catch (IOException e) {

			e.printStackTrace();
		}


		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;//aca es de tipo polygonFeature(the_geom:MultiPolygon,Rate:Rate)
			Transaction transaction = new DefaultTransaction("create"); //$NON-NLS-1$
			featureStore.setTransaction(transaction);

			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so we use the
			 * ListFeatureCollection class to wrap our list of features.
			 */

			try {
				featureStore.setFeatures(exportFeatureCollection.reader());
				try {
					transaction.commit();
				} catch (Exception e1) {
					e1.printStackTrace();
				}finally {
					try {
						transaction.close();
						//System.out.println("closing transaction");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}		

		System.out.println(Messages.getString("JFXMain.355")+ shapeFile); //$NON-NLS-1$
		Configuracion config = Configuracion.getInstance();
		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
		config.save();
		
		return shapeFile;
	}


	public static void reabsorverZonasChicas( List<LaborItem> items) {
		//TODO reabsorver zonas mas chicas a las mas grandes vecinas
		System.out.println("tiene mas de 100 zonas, reabsorviendo..."); //$NON-NLS-1$
		//TODO tomar las 100 zonas mas grandes y reabsorver las otras en estas



		items.sort((i1,i2)->-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea()));					
		List<LaborItem> itemsAgrandar =items.subList(0,100-1);
		Quadtree tree=new Quadtree();
		for(LaborItem ar : itemsAgrandar) {
			Geometry gAr =ar.getGeometry();
			tree.insert(gAr.getEnvelopeInternal(), ar);
		}
		List<LaborItem> itemsAReducir =items.subList(100, items.size()-1);
		int n=0;
		while(itemsAReducir.size()>0 || n>10) {
			List<LaborItem> done = new ArrayList<LaborItem>();		
			for(LaborItem ar : itemsAReducir) {
				Geometry gAr =ar.getGeometry();
				List<LaborItem> vecinos =(List<LaborItem>) tree.query(gAr.getEnvelopeInternal());

				if(vecinos.size()>0) {
					Optional<LaborItem> opV = vecinos.stream().reduce((v1,v2)->{
						boolean v1i = gAr.intersects(v1.getGeometry());
						boolean v2i = gAr.intersects(v2.getGeometry());
						return v1i&&v2i?(v1.getGeometry().getArea()>v2.getGeometry().getArea()?v1:v2):(v1i?v1:v2);

					});
					if(opV.isPresent()) {
						LaborItem v = opV.get();
						Geometry g = v.getGeometry();
						tree.remove(g.getEnvelopeInternal(), v);
						Geometry union = g.union(gAr);
						v.setGeometry(union);
						tree.insert(union.getEnvelopeInternal(), v);
						done.add(ar);
					}
				}
			}
			n++;
			itemsAReducir.removeAll(done);
		}
		items.clear();
		items.addAll((List<LaborItem>)tree.queryAll());
	}

}
