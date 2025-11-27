package com.ursulagis.desktop.tasks.procesar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.tasks.ProgresibleTask;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.PolygonValidator;
import com.ursulagis.desktop.utils.ProyectionConstants;

/**
 * Task para exportar una prescripción de pulverización en formato JSON
 * compatible con el formato de prescripción de drones XAG
 * @author quero
 */
public class ExportarPrescripcionPulverizacionJSONTask extends ProgresibleTask<File> {
	private PulverizacionLabor laborToExport = null;
	private File outFile = null;
	public boolean guardarConfig = true;
	private double cellSize = 5.0; // Tamaño de celda en metros
	
	// Clases internas para la estructura JSON
	public static class PrescripcionJSON {
		public String borderWKT;
		public double cellSize;
		public int columns;
		public int dataType = 3;
		public List<DataTypeLevel> dataTypeLevel = new ArrayList<>();
		public String guid;
		public String name;
		public double originEndLat;
		public double originEndLng;
		public double originLat;
		public double originLng;
		public double rotation = 0;
		public int rows;
		public String source = "UrsulaGIS";
		public int version = 1;
		public List<Integer> weightData = new ArrayList<>();
		public int workType = 1;
	}
	
	public static class DataTypeLevel {
		public double dosage;
		public int level;
	}
	
	public ExportarPrescripcionPulverizacionJSONTask(PulverizacionLabor laborToExport, File jsonFile) {
		super();
		this.laborToExport = laborToExport;
		this.outFile = jsonFile;
		super.updateTitle(taskName);
		this.taskName = laborToExport.getNombre();
	}
	
	public ExportarPrescripcionPulverizacionJSONTask(PulverizacionLabor laborToExport, File jsonFile, double cellSize) {
		this(laborToExport, jsonFile);
		this.cellSize = cellSize;
	}
	
	public void run(PulverizacionLabor laborToExport, File jsonFile) {
		// Obtener items de la labor
		List<LaborItem> items = new ArrayList<LaborItem>();
	
		SimpleFeatureIterator it = laborToExport.outCollection.features();
		while(it.hasNext()){
			PulverizacionItem fi = laborToExport.constructFeatureContainerStandar(it.next(), false);
			items.add(fi);
		}
		it.close();
		
		int zonas = items.size();
		updateProgress(0, zonas);
		
		if(zonas >= 100) {
			items = resumirGeometrias(laborToExport);
			reabsorverZonasChicas(items);
		}
		
		super.updateTitle("extrayendo contorno");
		updateProgress(0, 100);
		
		// Extraer contorno
		Geometry contorno = GeometryHelper.extractContornoGeometry(laborToExport);
		if(contorno == null) {
			throw new RuntimeException("No se pudo extraer el contorno de la labor");
		}
		
		// Convertir contorno a WKT
		String borderWKT = contorno.toText();
		
		// Obtener bounds
		ReferencedEnvelope bounds = laborToExport.outCollection.getBounds();
		double minLng = bounds.getMinX();
		double minLat = bounds.getMinY();
		double maxLng = bounds.getMaxX();
		double maxLat = bounds.getMaxY();
		
		// Calcular dimensiones del grid
		double cellSizeLong = ProyectionConstants.metersToLongLat(cellSize);
		double cellSizeLat = ProyectionConstants.metersToLongLat(cellSize);
		
		int columns = (int) Math.ceil((maxLng - minLng) / cellSizeLong);
		int rows = (int) Math.ceil((maxLat - minLat) / cellSizeLat);
		
		super.updateTitle("creando grid y mapeando dosis");
		updateProgress(0, rows);
		
		// Crear índice espacial de las zonas
		Quadtree zoneIndex = new Quadtree();
		for(LaborItem item : items) {
			Geometry geom = item.getGeometry();
			zoneIndex.insert(geom.getEnvelopeInternal(), item);
		}
		
		// Crear mapas de dosis a nivel
		Map<Double, Integer> dosisToLevel = new HashMap<>();
		List<DataTypeLevel> dataTypeLevels = new ArrayList<>();
		int levelCounter = 1;
		
		// Recopilar todas las dosis únicas
		for(LaborItem item : items) {
			PulverizacionItem pi = (PulverizacionItem) item;
			Double dosis = pi.getDosis();
			if(dosis != null && !dosisToLevel.containsKey(dosis)) {
				DataTypeLevel dtl = new DataTypeLevel();
				dtl.dosage = dosis;
				dtl.level = levelCounter;
				dataTypeLevels.add(dtl);
				dosisToLevel.put(dosis, levelCounter);
				levelCounter++;
			}
		}
		
		// Si no hay dosis, crear un nivel por defecto
		if(dataTypeLevels.isEmpty()) {
			DataTypeLevel dtl = new DataTypeLevel();
			dtl.dosage = 0.0;
			dtl.level = 1;
			dataTypeLevels.add(dtl);
			dosisToLevel.put(0.0, 1);
		}
		
		// Generar weightData
		List<Integer> weightData = new ArrayList<>();
		GeometryFactory fact = new GeometryFactory();
		
		for(int row = 0; row < rows; row++) {
			double lat = maxLat - (row + 0.5) * cellSizeLat;
			for(int col = 0; col < columns; col++) {
				double lng = minLng + (col + 0.5) * cellSizeLong;
				
				Point cellCenter = fact.createPoint(new Coordinate(lng, lat));
				
				// Verificar si el punto está dentro del contorno
				if(!contorno.contains(cellCenter) && !contorno.covers(cellCenter)) {
					weightData.add(0);
					continue;
				}
				
				// Buscar la zona que contiene o intersecta con este punto
				@SuppressWarnings("unchecked")
				List<LaborItem> candidates = (List<LaborItem>) zoneIndex.query(cellCenter.getEnvelopeInternal());
				
				LaborItem bestMatch = null;
				double bestDosis = 0.0;
				
				for(LaborItem candidate : candidates) {
					Geometry candidateGeom = candidate.getGeometry();
					if(candidateGeom.contains(cellCenter) || candidateGeom.covers(cellCenter)) {
						PulverizacionItem pi = (PulverizacionItem) candidate;
						Double dosis = pi.getDosis();
						if(dosis != null) {
							bestMatch = candidate;
							bestDosis = dosis;
							break; // Preferir el primero que contenga el punto
						}
					}
				}
				
				// Si no hay coincidencia exacta, usar el más cercano
				if(bestMatch == null && !candidates.isEmpty()) {
					double minDistance = Double.MAX_VALUE;
					for(LaborItem candidate : candidates) {
						Geometry candidateGeom = candidate.getGeometry();
						double distance = candidateGeom.distance(cellCenter);
						if(distance < minDistance) {
							minDistance = distance;
							PulverizacionItem pi = (PulverizacionItem) candidate;
							bestDosis = pi.getDosis() != null ? pi.getDosis() : 0.0;
						}
					}
				}
				
				// Determinar el nivel
				Integer level = dosisToLevel.get(bestDosis);
				if(level == null) {
					// Buscar el nivel más cercano
					double minDiff = Double.MAX_VALUE;
					Integer closestLevel = 1;
					for(Map.Entry<Double, Integer> entry : dosisToLevel.entrySet()) {
						double diff = Math.abs(entry.getKey() - bestDosis);
						if(diff < minDiff) {
							minDiff = diff;
							closestLevel = entry.getValue();
						}
					}
					level = closestLevel;
				}
				
				weightData.add(level);
			}
			updateProgress(row + 1, rows);
		}
		
		super.updateTitle("escribiendo archivo JSON");
		
		// Crear objeto JSON
		PrescripcionJSON prescripcion = new PrescripcionJSON();
		prescripcion.borderWKT = borderWKT;
		prescripcion.cellSize = cellSize;
		prescripcion.columns = columns;
		prescripcion.dataTypeLevel = dataTypeLevels;
		prescripcion.guid = UUID.randomUUID().toString();
		prescripcion.name = laborToExport.getNombre();
		// Origen: esquina sur-oeste (según el formato del ejemplo)
		prescripcion.originLat = minLat;
		prescripcion.originLng = minLng;
		// Fin: esquina noreste
		prescripcion.originEndLat = maxLat;
		prescripcion.originEndLng = maxLng;
		prescripcion.rows = rows;
		prescripcion.weightData = weightData;
		
		// Escribir JSON
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (FileWriter writer = new FileWriter(jsonFile)) {
			gson.toJson(prescripcion, writer);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error al escribir el archivo JSON", e);
		}
		
		if(guardarConfig) {
			Configuracion config = Configuracion.getInstance();
			config.loadProperties();
			config.setProperty(Configuracion.LAST_FILE, jsonFile.getAbsolutePath());
			config.save();
		}
		
		updateProgress(100, 100);
	}
	
	private List<LaborItem> resumirGeometrias(PulverizacionLabor labor) {
		super.updateTitle("resumir geometrias");
		updateProgress(0, 100);

		List<List<org.geotools.api.feature.simple.SimpleFeature>> colections = new ArrayList<List<org.geotools.api.feature.simple.SimpleFeature>>();
		for(int i = 0; i < labor.clasificador.getNumClasses(); i++){
			colections.add(i, new ArrayList<org.geotools.api.feature.simple.SimpleFeature>());
		}
		
		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			org.geotools.api.feature.simple.SimpleFeature f = it.next();
			PulverizacionItem ci = labor.constructFeatureContainerStandar(f, false);
			int cat = labor.getClasificador().getCategoryFor(ci.getAmount());
			colections.get(cat).add(f);
		}
		it.close();
		updateProgress(1, 100);
		
		List<LaborItem> itemsCategoria = new ArrayList<LaborItem>();
		
		for(int i = 0; i < labor.clasificador.getNumClasses(); i++){
			List<Geometry> geometriesCat = new ArrayList<Geometry>();
			updateProgress(i + 1, labor.clasificador.getNumClasses());
			
			Double sumRinde = 0.0;
			Double sumatoriaAltura = 0.0;
			int n = 0;
			for(org.geotools.api.feature.simple.SimpleFeature f : colections.get(i)){
				Object geomObj = f.getDefaultGeometry();
				geometriesCat.add((Geometry)geomObj);
				sumRinde += LaborItem.getDoubleFromObj(f.getAttribute(PulverizacionLabor.COLUMNA_DOSIS));
				sumatoriaAltura += LaborItem.getDoubleFromObj(f.getAttribute(com.ursulagis.desktop.dao.Labor.COLUMNA_ELEVACION));
				n++;
			} 
			double rindeProm = sumRinde / n;
			double elevProm = sumatoriaAltura / n;
			
			if(n > 0){
				GeometryFactory fact = geometriesCat.get(0).getFactory();
				Geometry[] geomArray = new Geometry[geometriesCat.size()];
				org.locationtech.jts.geom.GeometryCollection colectionCat = fact.createGeometryCollection(geometriesCat.toArray(geomArray));

				Geometry buffered = null;
				double bufer = ProyectionConstants.metersToLongLat(0.25);
				try{
					buffered = colectionCat.union();
					buffered = buffered.buffer(bufer);
				}catch(Exception e){
					System.out.println(Messages.getString("ProcessHarvestMapTask.10"));
					try{
						buffered = org.locationtech.jts.precision.EnhancedPrecisionOp.buffer(colectionCat, bufer);
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}

				org.geotools.api.feature.simple.SimpleFeature fIn = colections.get(i).get(0);
				for(int igeom = 0; buffered != null && igeom < buffered.getNumGeometries(); igeom++){
					Geometry g = buffered.getGeometryN(igeom);
				
					PulverizacionItem ci = labor.constructFeatureContainerStandar(fIn, true);
					ci.setDosis(rindeProm);
					ci.setElevacion(elevProm);
					ci.setGeometry(g);

					itemsCategoria.add(ci);
				}
			}	
		}
		
		return itemsCategoria;
	}
	
	public void reabsorverZonasChicas(List<LaborItem> items) {
		System.out.println("tiene mas de 100 zonas, reabsorviendo...");
		
		items.sort((i1, i2)
				-> (-1 * Double.compare(i1.getGeometry().getArea(), i2.getGeometry().getArea())));
		List<LaborItem> itemsAgrandar = items.subList(0, Math.min(100, items.size()));
		Quadtree tree = new Quadtree();
		for(LaborItem ar : itemsAgrandar) {
			Geometry gAr = ar.getGeometry();
			tree.insert(gAr.getEnvelopeInternal(), ar);
		}
		List<LaborItem> itemsAReducir = items.subList(Math.min(100, items.size()), items.size());
		int n = 0;
		int i = itemsAReducir.size();
		super.updateTitle("reabsorver zonas chicas");
		updateProgress(0, i);
		while(itemsAReducir.size() > 0 && n < 100) {
			List<LaborItem> done = new ArrayList<LaborItem>();
			for(LaborItem ar : itemsAReducir) {
				Geometry gAr = ar.getGeometry();
				@SuppressWarnings("unchecked")
				List<LaborItem> vecinos = (List<LaborItem>) tree.query(gAr.getEnvelopeInternal());

				if(vecinos.size() > 0) {
					java.util.Optional<LaborItem> opV = vecinos.stream().reduce((v1, v2) -> {
						boolean v1i = gAr.intersects(v1.getGeometry());
						boolean v2i = gAr.intersects(v2.getGeometry());
						return (v1i && v2i) 
								? (v1.getGeometry().getArea() > v2.getGeometry().getArea() ? v1 : v2) 
								: (v1i ? v1 : v2);
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
				updateProgress(done.size(), itemsAReducir.size());
			}
			updateProgress(i - itemsAReducir.size(), i);
			n++;
			itemsAReducir.removeAll(done);
		}
		
		items.clear();
		@SuppressWarnings("unchecked")
		List<LaborItem> allItems = (List<LaborItem>) tree.queryAll();
		items.addAll(allItems);
	}

	@Override
	public File call() {
		this.run(this.laborToExport, this.outFile);
		return outFile;
	}
}
