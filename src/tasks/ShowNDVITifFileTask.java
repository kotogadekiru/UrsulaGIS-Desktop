package tasks;
import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import dao.Clasificador;
import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import lombok.experimental.var;
import lombok.extern.java.Log;
import utils.ProyectionConstants;
@Log //private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(LogExample.class.getName());
public class ShowNDVITifFileTask extends Task<Layer>{
	public static final double WATER_RENDER_VALUE = 0.09; //para que quede sobre el nivel del suelo
	public static final double CLOUD_RENDER_VALUE = 2;// 2.2;
	private static final int WATER_VALUE = -2;
	private static final int CLOUD_VALUE = 2;
	public static final double MAX_VALUE = 1.0;//1.0;//con soja en floracion no pasa de 0.9
	public static final double MIN_VALUE =0.1;// 0.2;
	public static final double TRANSPARENT_VALUE = 0.000000001;// 0.2;
	/*
	-The values between –1 and 0 correspond to non-plant surfaces that have a reflectance in the Red that is greater than the
	reflectance in the Near Infrared: water, snow, or even clouds. 
	-Soil has an NDVI value close to 0. 
	-With their substantial reflectance in the Near Infrared, plants have an NDVI value from 0.1 to nearly 1.0;
 	the higher the value, the greater the plant density 
	 */
	private File file=null;
	private Ndvi ndvi=null;
	private Poligono ownerPoli = null;
	public ShowNDVITifFileTask(File f,Ndvi _ndvi){
		file =f;
		ndvi=_ndvi;
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		log.addHandler(consoleHandler);
	}
	public Layer call() {	
		try{
			BufferWrapperRaster raster = ShowNDVITifFileTask.loadRasterFile(file);
			if (raster == null){
				return null;
			}

			String fileName = file.getName();
			fileName= fileName.replace(".tif", "");
			//COPERNICUSS220170328T140051_20170328T140141_T20HNH.nd.tif
			if(fileName.contains("COPERNICUSS2")){
				fileName=fileName.replace("COPERNICUSS2", "");
				fileName=fileName.substring(0, "20170328".length());//anio
				fileName=fileName.substring("201703".length(), fileName.length())//dia
						+"-"+fileName.substring("2017".length(), "201703".length())//mes
						+"-"+fileName.substring(0, "2017".length());//anio
			
			} else if(fileName.contains("LANDSATLC08C01T1_TOALC08_XXXXXX_")){
				fileName=fileName.replace("LANDSATLC08C01T1_TOALC08_XXXXXX_", "");
				fileName=fileName.substring(0, "20170328".length());
				fileName=fileName.substring("201703".length(), fileName.length())
						+"-"+fileName.substring("2017".length(), "201703".length())
						+"-"+fileName.substring(0, "2017".length());
			}
			//en este punto fileName tiene la fecha en formato 20170328 es decir YYMMdd
			
			String fechaString = new String (fileName);

			if(ownerPoli !=null){
				fileName = ownerPoli.getNombre() +" "+ fileName;
			}
			if(ndvi!=null) {
				fileName = ndvi.getNombre();
			}
			
			final ExportableAnalyticSurface surface = new ExportableAnalyticSurface();
			surface.setSector(raster.getSector());
			surface.setDimensions(raster.getWidth(), raster.getHeight());

			//					surface.setExportImageName("ndviExportedImage");
			//					surface.setExportImagePath("/exportedImagePath");
			//					OutputStream outStream = null;					
			//					KMZDocumentBuilder kmzB = new KMZDocumentBuilder(outStream);
			//					kmzB.writeObject(surface);
			//					Object writer = writer = XMLOutputFactory.newInstance().createXMLStreamWriter(this.zipStream);
			//					surface.export(KMLConstants.KML_MIME_TYPE, writer);//mimeType, output);

			double HUE_MIN = Clasificador.colors[0].getHue()/360d;//0d / 360d;
			double HUE_MAX = Clasificador.colors[Clasificador.colors.length-1].getHue()/360d;//240d / 360d;
			//TRANSPARENT_VALUE =raster.getTransparentValue();
		//	System.out.println("ndvi transparent value = "+transparentValue);
			//double transparentValue =extremes[0];
			//TRANSPARENT_VALUE=0;//para que pueda interpretar los valores clipeados como transparente
			
			int width = raster.getWidth();
			int height = raster.getHeight();
			double dLat = raster.getSector().getDeltaLatDegrees();
			double lon = raster.getSector().getDeltaLonDegrees();
			
			Sector sector = raster.getSector();
			double latProm = (sector.getMaxLatitude().degrees+sector.getMinLatitude().degrees)/2;
			ProyectionConstants.setLatitudCalculo(latProm);
			double pixelArea = ProyectionConstants.A_HAS(((dLat*lon)/(width*height)));//12... =~ 10
			//System.out.println("el area calculada del pixel es "+pixelArea);
			
			//acoto los valores entre -2.2 y 2.2
			BufferWrapper buffer = raster.getBuffer();
			//log.setLevel(Level.ALL);
			
			for(int i=0;i<buffer.length();i++){
				double value = buffer.getDouble(i);
				//log.info("agregando value "+value);
				//System.out.println("agregando value " +value);
				if(Double.isNaN(value) || Double.isInfinite(value)) {					
					//log.fine("agregando Nan a transparente");
					//System.out.println("agregando value a transparente" +value);
					value=TRANSPARENT_VALUE;
				} else {
				//	value =0.000001;
				//  value = Math.max(value, -2.2);
				//  value = Math.min(value, 2.2);
				}
				  buffer.putDouble(i, value);
			}
			
			 @SuppressWarnings("unchecked")
			ArrayList<AnalyticSurface.GridPointAttributes> attributesList
	            = (ArrayList<GridPointAttributes>) AnalyticSurface.createColorGradientValues(raster.getBuffer(), 55, MIN_VALUE, MAX_VALUE, HUE_MIN, HUE_MAX);
			 if(attributesList.size()==0)return null;
			 attributesList.replaceAll((gpa)->{
				 double value = gpa.getValue();
					if(value == CLOUD_VALUE) {
						GridPointAttributes cloud = AnalyticSurface.createGridPointAttributes(CLOUD_RENDER_VALUE,Color.white);
						return cloud;
					} else if (value == WATER_VALUE){
						GridPointAttributes water = AnalyticSurface.createGridPointAttributes(WATER_RENDER_VALUE,Color.CYAN);
						return water;
					} else {
						return gpa;
					}
				
				 });
			
//			surface.setValues(AnalyticSurface.createColorGradientValues(
//					raster.getBuffer(), transparentValue, MIN_VALUE, MAX_VALUE, HUE_MIN, HUE_MAX));
			surface.setValues(attributesList);
			// surface.setVerticalScale(5e3);
			surface.setVerticalScale(100);
			//surface.setAltitude(-10);

			
			AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
			attr.setDrawOutline(false);
			attr.setDrawShadow(false);
			attr.setInteriorOpacity(1);
			surface.setSurfaceAttributes(attr);

			Format legendLabelFormat = new DecimalFormat() ;
			final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(MIN_VALUE,MAX_VALUE,
					HUE_MIN, HUE_MAX,
					AnalyticSurfaceLegend.createDefaultColorGradientLabels(MIN_VALUE, MAX_VALUE, legendLabelFormat),
					AnalyticSurfaceLegend.createDefaultTitle(fileName + " NDVI Values"));
			legend.setOpacity(1);
			legend.setScreenLocation(new Point(100, 400));

			
			LatLon ori = sector.getCentroid();
			Position pointPosition = Position.fromDegrees(ori.latitude.degrees, ori.longitude.degrees);			
			PointPlacemark pmStandard = new PointPlacemark(pointPosition);
			PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
			pointAttribute.setImageColor(java.awt.Color.red);
			//		if(HiDPIHelper.isHiDPI()){
			//			pointAttribute.setLabelFont(java.awt.Font.decode("Verdana-Bold-50"));
			//		}
			pointAttribute.setLabelMaterial(Material.DARK_GRAY);
			pmStandard.setLabelText(file.getName());
			pmStandard.setAttributes(pointAttribute);
		
			
			Renderable renderable =  new Renderable()	{
				public void render(DrawContext dc)
				{
					Extent extent = surface.getExtent(dc);
					if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
						return;

					if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300){
						pmStandard.render(dc);
						return;
					}

					legend.render(dc);
				}
			};
			SurfaceImageLayer layer = new SurfaceImageLayer(){
				@Override
				public void setOpacity(double opacity){
					//System.out.println("setting opacity en SurfaceImageLayer"+opacity);
					AnalyticSurfaceAttributes attributes = surface.getSurfaceAttributes();
					attributes.setInteriorOpacity(opacity);
					surface.setSurfaceAttributes(attributes);
					legend.setOpacity(opacity);
					
				}
				
			};
			
		
			layer.setName(fileName);
			layer.setPickEnabled(false);
			layer.addRenderable(surface);
			layer.addRenderable(renderable);
			if(ndvi==null){
				ndvi = new Ndvi();
				ndvi.setNombre(fileName);
				ndvi.setF(file);				
				ndvi.setContorno(ownerPoli);
				
				//04-01-2018
				SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
				//System.out.println("convirtiendo fechaString "+fechaString);
				//convirtiendo fechaString 04-01-2018
				Date fecha = null;
				try{
					fecha = format1.parse(fechaString);//java.text.ParseException: Unparseable date: "Jag 20 30-08-20175528033450897731504"
					ndvi.getFecha().setTime(fecha);
				}catch(Exception e){
					
				}
			}
			ndvi.setPixelArea(pixelArea);//pixelArea);
			ndvi.setSurfaceLayer(surface);
		
		
			
			// creando un ndvi con fecha
			//System.out.println("creando un ndvi con fecha "+fecha);
			//creando un ndvi con fecha Thu Jan 04 00:00:00 ART 2018
			
			layer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, ndvi);
			layer.setValue(ProcessMapTask.ZOOM_TO_KEY, pointPosition);		
			
			
		//	Sector s = raster.getSector();
		//	BufferWrapper buffer = raster.getBuffer();
//			int nFilas =raster.getHeight();
//			int nCols =raster.getWidth();//buffer.length()/raster.getWidth();
//			int filaV=0,colV = 0;
//			for(int col =0;col<nCols;col++){
//			for(int fila =0;fila<nFilas;fila++){
//				int index = fila*raster.getWidth()+col;
//				double value = buffer.getDouble(index);
//				//System.out.println("raster value for "+fila+","+col+" : "+value);
//				if(value > 0.2 ){//si no hay dato lee 0.0
//					filaV = fila;
//					colV=col;
//					break;
//				}				
//			}
//			if(filaV>0)break;
//			}
//		//	System.out.println("fila= "+filaV);
//			double latDelta = dLat*filaV/raster.getHeight();
//			double lonDelta = lon*colV/raster.getWidth();
		//	System.out.println("latDelta= "+latDelta);
			//TODO en vez de usar sector usar el metodo de labor para encontrar un vertice
	
			return layer;

		} catch (Exception e)     {
			e.printStackTrace();
		}
		return null;
	}
	
	public static BufferWrapperRaster loadRasterFile(File file){
//		if(!file.exists()){	
//			//TODO si el recurso es web podemos bajarlo a 
//			// Download the data and save it in a temp file.
//			String path = file.getAbsolutePath();
//			file = ExampleUtil.saveResourceToTempFile(path, "." + WWIO.getSuffix(path));
//		}



		// Create a raster reader for the file type.
		DataRasterReaderFactory readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
				AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
		DataRasterReader reader = readerFactory.findReaderFor(file, null);

		try{
			// Before reading the raster, verify that the file contains elevations.
			AVList metadata = reader.readMetadata(file, null);
			metadata.setValue(AVKey.PIXEL_FORMAT, AVKey.ELEVATION);
			 String pixelFormat = metadata.getStringValue(AVKey.PIXEL_FORMAT);
			//metadata.setValue(AVKE, value)
			if (metadata == null || !AVKey.ELEVATION.equals(pixelFormat))
			{
				Platform.runLater(()->{
					Alert imagenAlert = new Alert(Alert.AlertType.ERROR);
					//imagenAlert.initOwner(stage);
					imagenAlert.initModality(Modality.NONE);
					imagenAlert.setTitle("Archivo no compatible");
					imagenAlert.setContentText("El archivo no continen informacion ndvi. Por favor seleccione un archivo con solo una capa y valores decimales");
					imagenAlert.show();
				});
				String msg = Logging.getMessage("ElevationModel.SourceNotElevations", file.getAbsolutePath());
				Logging.logger().severe(msg);
				throw new IllegalArgumentException(msg);
			}

			// Read the file into the raster.
			DataRaster[] rasters = reader.read(file, null);
			if (rasters == null || rasters.length == 0)	{
				String msg = Logging.getMessage("ElevationModel.CannotReadElevations", file.getAbsolutePath());
				Logging.logger().severe(msg);
				throw new WWRuntimeException(msg);
			}

			// Determine the sector covered by the elevations. This information is in the GeoTIFF file or auxiliary
			// files associated with the elevations file.
			Sector sector = (Sector) rasters[0].getValue(AVKey.SECTOR);
			if (sector == null)
			{
				String msg = Logging.getMessage("DataRaster.MissingMetadata", AVKey.SECTOR);
				Logging.logger().severe(msg);
				throw new IllegalArgumentException(msg);
			}

			// Request a sub-raster that contains the whole file. This step is necessary because only sub-rasters
			// are reprojected (if necessary); primary rasters are not.
			int width = rasters[0].getWidth();
			int height = rasters[0].getHeight();
			//System.out.println("height: "+height+" width: "+width);

			DataRaster subRaster = rasters[0].getSubRaster(width, height, sector, rasters[0]);
			//System.out.println("subRaster: "+subRaster);
			// Verify that the sub-raster can create a ByteBuffer, then create one.
			if (!(subRaster instanceof BufferWrapperRaster)){
				String msg = Logging.getMessage("ElevationModel.CannotCreateElevationBuffer", file.getName());
				Logging.logger().severe(msg);
				throw new WWRuntimeException(msg);
			}

			return (BufferWrapperRaster) subRaster;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public void setPoligono(Poligono p){
		if(ndvi!=null) {
			this.ndvi.setContorno(p);
		}
		this.ownerPoli=p;
	}
	
}
