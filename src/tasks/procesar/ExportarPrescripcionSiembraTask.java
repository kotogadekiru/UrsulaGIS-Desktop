package tasks.procesar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.LaborItem;
import dao.config.Configuracion;
import dao.fertilizacion.FertilizacionItem;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gui.Messages;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import tasks.ProgresibleTask;
import utils.FileHelper;
import utils.GeometryHelper;
import utils.PolygonValidator;
import utils.ProyectionConstants;


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
	public boolean guardarConfig=true;
	
	public  ExportarPrescripcionSiembraTask(SiembraLabor _laborToExport,File _shapeFile,String _unidad) {	
		laborToExport=_laborToExport;
		shapeFile=_shapeFile;
		unidad=_unidad;		
	}
	
	public File call() {
		try {
		SimpleFeatureType type = null;
		//*the_geom:    												:srid=4326,
		//	String typeDescriptor = Messages.getString("JFXMain.341")+Polygon.class.getCanonicalName()+Messages.getString("JFXMain.342") //$NON-NLS-1$ //$NON-NLS-2$
		//
		//		+ SiembraLabor.COLUMNA_DOSIS_LINEA + Messages.getString("JFXMain.343") //$NON-NLS-1$ :java.lang.Long,
		//		+ SiembraLabor.COLUMNA_DOSIS_COSTADO + Messages.getString("JFXMain.344") //$NON-NLS-1$ :java.lang.Long,
		//		//seeding
		//		+ Messages.getString("JFXMain.345") + Messages.getString("JFXMain.346"); //$NON-NLS-1$ :java.lang.Long

		
//		Map<String,String> availableColums = new LinkedHashMap<String,String>();
//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_10METROS"),SiembraLabor.COLUMNA_SEM_10METROS);//("Sem10ml");
//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_DOSIS_SEMILLA"),SiembraLabor.COLUMNA_KG_SEMILLA);//("kgSemHa");
//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_MILES_SEM_HA"),SiembraLabor.COLUMNA_MILES_SEM_HA);//("MilSemHa");
//		availableColums.put(Messages.getString("SiembraLabor.COLUMNA_SEM_ML"),SiembraLabor.COLUMNA_SEM_ML);//("semML");
		
		String dosisClass = "java.lang.Long";
		if(SiembraLabor.COLUMNA_SEM_ML.equals(unidad)) {
			dosisClass = "java.lang.Float";
		}
		
		String typeDescriptor = "*the_geom:"+Polygon.class.getCanonicalName()+":srid=4326,"//$NON-NLS-1$
				+ SiembraLabor.COLUMNA_DOSIS_LINEA +":java.lang.Long,"//java.lang.Long,"//$NON-NLS-1$
				+ SiembraLabor.COLUMNA_DOSIS_COSTADO +":java.lang.Long,"//$NON-NLS-1$
				+unidad+":"+dosisClass;//$NON-NLS-1$ semilla siempre tiene que ser la 3ra columna

		System.out.println("creando type con: "+typeDescriptor); //$NON-NLS-1$ the_geom:Polygon:srid=4326,Fert L:java.lang.Long,Fert C:java.lang.Long,seeding:java.lang.Long
		//System.out.println("Long.SIZE="+Long.SIZE);//64bits=16bytes. ok!! //$NON-NLS-1$
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

		//TODO si una geometria tiene mas de 50 sub zonas descartar las chicas
		//TODO simplificar las geometrias con presicion de 0.0001grados
		for(LaborItem item:items) {
		
			if(item.getGeometry().getNumGeometries()>50){
				System.out.println("procesando geometry con "+item.getGeometry().getNumGeometries()+" geometrias");
				Geometry g50 = item.getGeometry();
				System.err.println("reduciendo item "+item+ "porque tiene mas de 50 partes "+g50.toString());
				//TODO reducir geometry
				List<Geometry> simples = new ArrayList<Geometry>();
				for(int i=0;i<g50.getNumGeometries();i++) {
					Geometry g = g50.getGeometryN(i);
					simples.add(g);//GeometryHelper.simplify(g));
				}
				simples.sort((g1,g2)->{					
					return Double.compare(g1.getArea(),g2.getArea());
				});
				List<Geometry> simplesGrandes =simples.subList(0,50-1);
				GeometryCollection col = new GeometryCollection(simplesGrandes.toArray(new Geometry[simplesGrandes.size()]),g50.getFactory());
				Geometry buf =  col.buffer(ProyectionConstants.metersToLongLat(0.25));
				item.setGeometry(buf);
			}
		}
		
		DefaultFeatureCollection exportFeatureCollection =  new DefaultFeatureCollection("PrescType",type); //$NON-NLS-1$
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);//ok
		Integer id = 0;
		for(LaborItem i:items) {//(it.hasNext()){
			SiembraItem fi=(SiembraItem) i;
			Geometry itemGeometry=fi.getGeometry();
			List<Polygon> flatPolygons = PolygonValidator.geometryToFlatPolygons(itemGeometry);
			if(flatPolygons.size()>1) {
				System.out.println("el item "+i.getId()+" tiene "+flatPolygons.size()+" poligonos "+itemGeometry.toText());
			}
			
			for(Polygon p : flatPolygons){
				int partes = p.getNumGeometries();
				System.out.println("exportando item con "+partes+" partes "+p.toText());
				if(partes>50) {
					System.err.println("error tiene mas de 50 partes!!");
				}
//				fb.add(p);
//				availableColums.add(SiembraLabor.COLUMNA_SEM_10METROS);//("Sem10ml");
//				availableColums.add(SiembraLabor.COLUMNA_DOSIS_SEMILLA);//("kgSemHa");
//				availableColums.add(SiembraLabor.COLUMNA_MILES_SEM_HA);//("MilSemHa");
				Double semilla = Math.rint(fi.getDosisML()*10);///XXX aca hago magia para convertir de plantas por metro a plantas cada 10 metros
				if(SiembraLabor.COLUMNA_KG_SEMILLA.equals(unidad)) {
					semilla = Math.rint(fi.getDosisHa());
				} else if(SiembraLabor.COLUMNA_MILES_SEM_HA.equals(unidad)) {
					semilla = Math.rint(fi.getDosisML()*(10/laborToExport.getEntreSurco()));
				} else if(SiembraLabor.COLUMNA_SEM_ML.equals(unidad)) {
					semilla = fi.getDosisML();
					System.out.println("Exportando prescripcion con dosis ML "+semilla);
				}
				Double linea = fi.getDosisFertLinea();
				Double costado = fi.getDosisFertCostado();

				//System.out.println("presc Dosis ="+semilla); //$NON-NLS-1$
				
				
//				fb.add(linea);
//				fb.add(costado);
//				fb.add(semilla);
				id++;
				//SimpleFeature exportFeature = fb.buildFeature(id.toString());//aca pierdo una geometria porque dublico el id
				SimpleFeature exportFeature = fb.buildFeature(null, new Object[]{p,linea,costado,semilla});
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
		try {
			long bytes = Files.size(shapeFile.toPath());
			long kilobytes = bytes/1024;
			if(kilobytes > 500) {
				
				Platform.runLater(()->//esto hace que quede abajo del dialogo de importar				
				{
					Alert a = new Alert(Alert.AlertType.ERROR);
					a.setContentText(String.format("El archivo generado pesa %,dKB,  en algunos monitores 512KB es lo maximo", kilobytes));
					a.showAndWait();
					});
				
			}
	        System.out.println(String.format("%,d kilobytes", bytes / 1024));//61 kilobytes
		}catch(Exception e) {e.printStackTrace();}
		
		if(guardarConfig) {
			//TODO guardar un archivo txt con la configuracion de la labor para que quede como registro de las operaciones
			 Configuracion config = Configuracion.getInstance();
			 	config.loadProperties();
				config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
				config.save();
			}
//		System.out.println(Messages.getString("JFXMain.355")+ shapeFile); //$NON-NLS-1$
//		Configuracion config = Configuracion.getInstance();
//		config.setProperty(Configuracion.LAST_FILE, shapeFile.getAbsolutePath());
//		config.save();
		
		return shapeFile;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public static void reabsorverZonasChicas( List<LaborItem> items) {
		// reabsorver zonas mas chicas a las mas grandes vecinas
		System.out.println("tiene mas de 100 zonas, reabsorviendo..."); //$NON-NLS-1$
		// tomar las 100 zonas mas grandes y reabsorver las otras en estas

		items.sort((i1,i2)->-1*Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea()));					
		List<LaborItem> itemsAgrandar =items.subList(0,100-1);
		Quadtree tree=new Quadtree();
		for(LaborItem ar : itemsAgrandar) {
			Geometry gAr =ar.getGeometry();
			tree.insert(gAr.getEnvelopeInternal(), ar);
		}
		List<LaborItem> itemsAReducir =items.subList(99, items.size()-1);//99 es el indice de la zona numero 100
		int n=0;
		while(itemsAReducir.size()>0 || n>10) {
			List<LaborItem> done = new ArrayList<LaborItem>();		
			for(LaborItem ar : itemsAReducir) {
				Geometry gAr =ar.getGeometry();
				@SuppressWarnings("unchecked")
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
