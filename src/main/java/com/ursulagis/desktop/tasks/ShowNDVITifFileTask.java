package com.ursulagis.desktop.tasks;
import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;



import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.api.geometry.Position;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import com.ursulagis.desktop.dao.Clasificador;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.data.BasicDataRasterReaderFactory;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.data.ByteBufferRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.formats.tiff.GeoTiff;
import gov.nasa.worldwind.formats.tiff.Tiff;
import gov.nasa.worldwind.formats.tiff.Tiff.PlanarConfiguration;
import gov.nasa.worldwind.formats.tiff.TiffIFDEntry;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
//import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.coords.UTMCoord;
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
import com.ursulagis.desktop.gui.Messages;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import lombok.extern.java.Log;
import mil.nga.tiff.FieldType;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.FileDirectoryEntry;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffReader;
import com.ursulagis.desktop.utils.ProyectionConstants;

@Log //private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(LogExample.class.getName());
public class ShowNDVITifFileTask extends Task<Layer>{
	public static final double WATER_RENDER_VALUE = 0.09; //para que quede sobre el nivel del suelo
	public static final double CLOUD_RENDER_VALUE = 2;// 2.2;
	private static final int WATER_VALUE = -2;
	private static final int CLOUD_VALUE = 2;
	public static final double MAX_VALUE = 1.0;//1.0;//con soja en floracion no pasa de 0.9
	public static final double MIN_VALUE =0.1;// 0.2;
	public static final double TRANSPARENT_VALUE = 0.000000001;// 0.2;
	private static final String YYYY_MM_DD = "dd-MM-yyyy";//"yyyy-MM-dd";
	/*
	-The values between �1 and 0 correspond to non-plant surfaces that have a reflectance in the Red that is greater than the
	reflectance in the Near Infrared: water, snow, or even clouds. 
	-Soil has an NDVI value close to 0. 
	-With their substantial reflectance in the Near Infrared, plants have an NDVI value from 0.1 to nearly 1.0;
 	the higher the value, the greater the plant density 
	 */
	//	private File file=null;
	private Ndvi ndvi=null;
	private Poligono ownerPoli = null;
	public ShowNDVITifFileTask(File f){		
		//TODO  leer el archivo y crear el ndvi
		ndvi=constructNdviFromFile(f);

		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		log.addHandler(consoleHandler);
	}

	//metodo que se invoca cuando se carga un archivo drag and drop o en importar NDVI
	private Ndvi constructNdviFromFile(File f) {
		String fileName = GetNdviForLaborTask4.extractNameFromFileName(f.getName());
		LocalDate date = GetNdviForLaborTask4.getAssetDate(f.getName().replace(".tif", ""));
		//en este punto fileName tiene la fecha en formato 2017-03-28 es decir dd-MM-yyyy

		//String fechaString = new String (fileName);



		Ndvi ndvi = new Ndvi();
		ndvi.setNombre(fileName);
		//ndvi.setF(tiffFile.toFile());
		ndvi.updateContent(f);
		//ndvi.setContent((byte[])nameBytes[1]);
		//	ndvi.setContorno(contornoP);

		ndvi.setFecha(date);
		//ndvi.setMeanNDVI(meanNDVI.doubleValue());
		//ndvi.setPorcNubes(porcNubes.doubleValue());
		return ndvi;
	}



	public ShowNDVITifFileTask(Ndvi _ndvi){
		//file =f;
		ndvi=_ndvi;

		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		log.addHandler(consoleHandler);
	}


	class RasterWraperApache{
		public int width = Tiff.Undefined;
		public int height = Tiff.Undefined;

		//private int y_resolution = Tiff.Undefined;
		//private int x_resolution = Tiff.Undefined;

		public int samplesPerPixel = Tiff.Undefined;
		public int photometric = Tiff.Photometric.Undefined;
		public int rowsPerStrip = Tiff.Undefined;
		public int planarConfig = Tiff.Undefined;
		public int minSampleValue;
		public int maxSampleValue;

		public int[] sampleFormat = null;
		public int[] bitsPerSample = null;

		public String displayName = null;
		public String imageDescription = null;
		public String softwareVersion = null;
		public String dateTime = null;

		public int[] stripOffsets = null;//long[] stripOffsets = null;
		public byte[][] cmap = null;
		public int[] stripCounts = null;//public long[] stripCounts = null;
		private Sector sector=null;


		Rasters image=null;
		Set<FileDirectoryEntry> metadata=null;
		private double noData;
		private double xPixelScale=Tiff.Undefined;
		private double yPixelScale=Tiff.Undefined;
		private double minLatitude=Tiff.Undefined, maxLatitude=Tiff.Undefined, minLongitude=Tiff.Undefined, maxLongitude=Tiff.Undefined;


		RasterWraperApache(Rasters rasters, Set<FileDirectoryEntry> fieldList){
			image=rasters;
			metadata=fieldList;

			this.height=image.getHeight();
			this.width=image.getWidth();


			for (FileDirectoryEntry entry : metadata){
				//System.out.println(entry.getFieldTag().toString()+" "+entry.getValues());
				try
				{

					switch (entry.getFieldTag().getId())
					{
					// base TIFF tags
					case Tiff.Tag.IMAGE_WIDTH:
						width = (int) entry.getValues();//getIntValue();
						break;

					case Tiff.Tag.IMAGE_LENGTH:
						height = (int) entry.getValues();
						break;

					case GeoTiff.Tag.MODEL_PIXELSCALE:
						//ModelPixelScale [8.983152841195215E-5, 8.983152841195215E-5, 0.0]
						double[] scales = toDoubleArray(entry);
						xPixelScale = scales[0];
						yPixelScale = scales[1];
						break;

					case GeoTiff.Tag.MODEL_TIEPOINT:
						//System.out.println("ModelTiepoint es "+ entry.getValues());
						//		ModelTiepoint [0.0, 0.0, 0.0, -61.97279515778072, -33.962875441291985, 0.0]
						double[] tiepoint = toDoubleArray(entry);
						minLongitude = tiepoint[3];
						minLatitude = tiepoint[4];
						//double minLatitude, maxLatitude, minLongitude, maxLongitude;
						//minLatitude=lat;

						break;

					case GeoTiff.Tag.MODEL_TRANSFORMATION://34264:// "ModelTransformationTag":
						//System.out.println("model transformation es "+ entry.getValues());
						//34264 (0x85d8: ModelTransformationTag): 8.983152841195215E-5, 0.0, 0.0, -62.11778324463761
						double[] transformationValues = toDoubleArray(entry);
						xPixelScale = transformationValues[0];
						yPixelScale = -transformationValues[5];
						minLongitude = transformationValues[3];
						minLatitude = transformationValues[7];
						//System.out.println(Arrays.toString(transformationValues));
						break;

					case 42113://Tiff.Tag.:	GDALNoData
						this.noData = (double)entry.getValues();
						break;

					case Tiff.Tag.DOCUMENT_NAME:
						displayName =(String)entry.getValues();// tiffReader.readString(entry);
						break;

					case Tiff.Tag.IMAGE_DESCRIPTION:
						imageDescription = (String) entry.getValues();//getDescriptionWithoutValue();//tiffReader.readString(entry);
						break;

					case Tiff.Tag.SOFTWARE_VERSION:
						softwareVersion = (String) entry.getValues();//tiffReader.readString(entry);
						break;

					case Tiff.Tag.DATE_TIME:
						dateTime = (String) entry.getValues();//tiffReader.readString(entry);
						break;

					case Tiff.Tag.SAMPLES_PER_PIXEL:
						samplesPerPixel = (int) entry.getValues();
						break;

					case Tiff.Tag.PHOTO_INTERPRETATION:
						photometric = (int) entry.getValues();
						break;

					case Tiff.Tag.ROWS_PER_STRIP:
						rowsPerStrip = (int) entry.getValues();
						break;

					case Tiff.Tag.PLANAR_CONFIGURATION:
						planarConfig = (int) entry.getValues();
						break;

					case Tiff.Tag.SAMPLE_FORMAT:
						//System.out.println("sample format entry class "+entry.getValues().getClass());

						sampleFormat =null;// list.toArray(ints);//((java.util.ArrayList)entry.getValues());//java.util.ArrayList
						break;

					case Tiff.Tag.BITS_PER_SAMPLE:

						bitsPerSample = toArray(entry);
						break;

					case Tiff.Tag.MIN_SAMPLE_VALUE:
						minSampleValue = (int)entry.getValues();
						break;

					case Tiff.Tag.MAX_SAMPLE_VALUE:
						maxSampleValue = (int)entry.getValues();//entry.getIntValue();
						break;
					case Tiff.Tag.STRIP_OFFSETS:
						stripOffsets = toArray(entry);//(int[])entry.getValues();//entry.getIntArrayValue();
						//System.out.println("offsets= "+Arrays.toString(stripOffsets));
						//[384, 8232, 16080][462, 8222, 15982, 23742, 31502, 39262, 47022, 54782, 62542, 70302, 78062, 85822, 93582, 101342, 109102, 116862][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600][396, 8396, 16396, 24396, 32396][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600][384, 8232, 16080][384, 8232, 16080][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600][462, 8222, 15982, 23742, 31502, 39262, 47022, 54782, 62542, 70302, 78062, 85822, 93582, 101342, 109102, 116862][384, 8232, 16080][504, 8372, 16240, 24108, 31976, 39844, 47712, 55580, 63448, 71316, 79184, 87052, 94920, 102788, 110656, 118524, 126392, 134260, 142128, 149996, 157864, 165732, 173600]
						break;

					case Tiff.Tag.STRIP_BYTE_COUNTS:
						stripCounts = toArray(entry);//(int[])entry.getValues();//entry.getIntArrayValue();
						//System.out.println("stripCounts= "+Arrays.toString(stripCounts));
						break;

					case Tiff.Tag.COLORMAP:
						//cmap = entry.readColorMap(entry);
						break;
					}


				}catch (Exception e)	{
					Logging.logger().finest(e.toString());
				}
			}

			minLatitude-=(height)*yPixelScale;

			maxLatitude=minLatitude+(height)*yPixelScale;
			maxLongitude=minLongitude+width*xPixelScale;

			//			System.out.println(
			//							"minLatitude "+minLatitude+"\n"+
			//							"maxLatitude "+maxLatitude+"\n"+
			//							"minLongitude "+minLongitude+"\n"+
			//							"maxLongitude "+maxLongitude+"\n"
			//					
			//					);
			sector = Sector.fromDegrees(minLatitude, 
					maxLatitude,
					minLongitude,
					maxLongitude
					);
		}
	}

	/**
	 * Returns a new iterable populated with {@link GridPointAttributes} computed by invoking {@link
	 * #createColorGradientAttributes(double, double, double, double, double)} for each double value in the speicfied
	 * {@link BufferWrapper}. Values equivalent to the specified <code>missingDataSignal</code> are replaced with the
	 * specified <code>minValue</code>.
	 *
	 * @param values            the buffer of values.
	 * @param missingDataSignal the number indicating a specific value to ignore.
	 * @param minValue          the minimum value.
	 * @param maxValue          the maximum value.
	 * @param minHue            the mimimum color hue, corresponding to the minimum value.
	 * @param maxHue            the maximum color hue, corresponding to the maximum value.
	 *
	 * @return an iiterable GridPointAttributes defined by the specified buffer of values.
	 */
	public static Iterable<? extends AnalyticSurface.GridPointAttributes> createColorGradientValues(
			BufferWrapper values, double missingDataSignal, double minValue, double maxValue, double minHue, double maxHue){
		if (values == null)
		{
			String message = Logging.getMessage("nullValue.BufferIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		ArrayList<AnalyticSurface.GridPointAttributes> attributesList
		= new ArrayList<AnalyticSurface.GridPointAttributes>();
		Map<Double,GridPointAttributes> gpMap = new HashMap<>();
		for (int i = 0; i < values.length(); i++) {
			double value = values.getDouble(i);
			if (Double.compare(value, missingDataSignal) == 0)  value = minValue;
			GridPointAttributes gp=null;

			if(gpMap.containsKey(value)) {
				System.out.println("ndvi gpMapContains "+value);
				gp = gpMap.get(value);
			} else {
				gp =AnalyticSurface.createColorGradientAttributes(value, minValue, maxValue, minHue, maxHue); 
				gpMap.put(value, gp);
			}

			attributesList.add(gp);
		}

		return attributesList;
	}
	
	

	public int[] toArray(FileDirectoryEntry entry) {
		ArrayList<Integer> list = (ArrayList<Integer>) entry.getValues();
		int[] ints = new int[list.size()];
		for(int i =0; i<list.size();i++) {
			ints[i]=list.get(i).intValue();
		}
		return ints;
	}

	public double[] toDoubleArray(FileDirectoryEntry entry) {		
		FieldType type = entry.getFieldType();
		double[] ints = new double[(int)entry.sizeOfValues()];

		if(type == FieldType.DOUBLE) {
			ArrayList<Double> list = (ArrayList<Double>) entry.getValues();

			for(int i =0; i<list.size();i++) {
				ints[i]=list.get(i).doubleValue();
			}
		} else if(type == FieldType.LONG) {
			ArrayList<Long> list = (ArrayList<Long>) entry.getValues();

			for(int i =0; i<list.size();i++) {
				ints[i]=list.get(i).doubleValue();
			}
		}
		return ints;
	}

	public Layer call() {	
		//		if(ndvi==null) {
		//			ndvi=new Ndvi();
		//			//ndvi.setF(this.file);
		//			ndvi.updateContent(this.file);
		//			
		//			System.out.println("loading ndvi from file");
		//		}
		try{
			System.out.println("mostrando el ndvi"+ndvi.getNombre());
			RasterWraperApache wrapper = loadRaster(ndvi);
			if(wrapper ==null ) {
				System.out.println("no se pudo cargar el wrapper");
				return null;
			}

			String fileName="";
			String fechaString="";
			//trato de leer la fecha desde el nombre del archivo 
			//			if(file!=null) {
			//				fileName = file.getName();
			//				fileName= fileName.replace(".tif", "");
			//				//COPERNICUSS220170328T140051_20170328T140141_T20HNH.nd.tif
			//				if(fileName.contains("COPERNICUSS2")){
			//					fileName=fileName.replace("COPERNICUSS2", "");
			//					fileName=fileName.substring(0, "20170328".length());//anio
			//					fileName=fileName.substring("201703".length(), fileName.length())//dia
			//							+"-"+fileName.substring("2017".length(), "201703".length())//mes
			//							+"-"+fileName.substring(0, "2017".length());//anio
			//
			//				} else if(fileName.contains("LANDSATLC08C01T1_TOALC08_XXXXXX_")){
			//					fileName=fileName.replace("LANDSATLC08C01T1_TOALC08_XXXXXX_", "");
			//					fileName=fileName.substring(0, "20170328".length());
			//					fileName=fileName.substring("201703".length(), fileName.length())
			//							+"-"+fileName.substring("2017".length(), "201703".length())
			//							+"-"+fileName.substring(0, "2017".length());
			//				}
			//				//en este punto fileName tiene la fecha en formato 2017-03-28 es decir dd-MM-yyyy
			//
			//				fechaString = new String (fileName);
			//			}

			//			if(ownerPoli !=null){
			//				System.out.println("mosntrando un ndvi con owner poli"+ ownerPoli);
			//				fileName = ownerPoli.getNombre() +" "+ fileName;
			//			}
			if(ndvi!=null && ndvi.getNombre()!=null) {
				fileName = ndvi.getNombre();
			}

			final ExportableAnalyticSurface surface = new ExportableAnalyticSurface();
			Sector sector =wrapper.sector;// Sector.fromDegrees(wrapper.image.getMinX()+wrapper.width, wrapper.image.getMinY()+wrapper.height,wrapper.image.getMinX(), wrapper.image.getMinY());
			surface.setSector(sector);
			surface.setDimensions(wrapper.width,wrapper.height);//(int)envelope.getWidth(),(int)envelope.height);
			//surface.setExportImageHeight(wrapper.height);
			//surface.setExportImageWidth(wrapper.width);
			//					surface.setExportImageName("ndviExportedImage");
			//					surface.setExportImagePath("/exportedImagePath");
			//					OutputStream outStream = null;					
			//					KMZDocumentBuilder kmzB = new KMZDocumentBuilder(outStream);
			//					kmzB.writeObject(surface);
			//					Object writer = writer = XMLOutputFactory.newInstance().createXMLStreamWriter(this.zipStream);
			//					surface.export(KMLConstants.KML_MIME_TYPE, writer);//mimeType, output);

			double HUE_MIN = Clasificador.colors[0].getHue()/360d;//0d / 360d;
			double HUE_MAX = Clasificador.colors[Clasificador.colors.length-1].getHue()/360d;//240d / 360d;

			int width = wrapper.width;//(int)envelope.getWidth();//coverage.getWidth();
			int height = wrapper.height;//(int)envelope.getHeight();
			double dLat =sector.getDeltaLatDegrees();
			double dLon =sector.getDeltaLonDegrees();

			//Sector sector = coverage.getSector();
			double latProm = (sector.getMaxLatitude().degrees+sector.getMinLatitude().degrees)/2;
			ProyectionConstants.setLatitudCalculo(latProm);
			double pixelArea = ProyectionConstants.A_HAS(((dLat*dLon)/(width*height)));//12... =~ 10
			//System.out.println("el area calculada del pixel es "+pixelArea);

			Rasters image =wrapper.image;//null;// coverage.getRenderedImage().getData();

			//acoto los valores entre -2.2 y 2.2

			AVList elev32 = new AVListImpl();
			elev32.setValue(AVKey.SECTOR, sector);
			//elev32.setValue(AVKey.WIDTH, width);
			//elev32.setValue(AVKey.HEIGHT, height);
			elev32.setValue(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_GEOGRAPHIC);
			elev32.setValue(AVKey.PIXEL_FORMAT, AVKey.ELEVATION);
			elev32.setValue(AVKey.DATA_TYPE, AVKey.FLOAT32);
			elev32.setValue(AVKey.ELEVATION_UNIT, AVKey.UNIT_METER);
			elev32.setValue(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN);
			elev32.setValue(AVKey.MISSING_DATA_SIGNAL, -8388608);//wrapper.noData


			BufferWrapper bufferWrapper = BufferWrapper.DoubleBufferWrapper.wrap(ByteBuffer.allocate(image.size()*Float.BYTES),elev32);

			for(int x=0;x<width;x++) {
				for(int y =0;y<height;y++) {



					Number value = image.getFirstPixelSample(x, y);
					int i = y*width+x;

					//double value = 0.7;//((double)i)/size;// buffer.getElem(i);//getDouble(i);
					//value= values[i].getDouble();
					//.MAX_VALUEvalues[i]
					//log.info("agregando value "+value);

					if(Double.isNaN(value.doubleValue()) || Double.isInfinite(value.doubleValue())) {					
						//log.fine("agregando Nan a transparente");
						//System.out.println("agregando value a transparente" +value);
						value=TRANSPARENT_VALUE;
					} else {
						//if(value.doubleValue()>0)System.out.println("agregando value " +value);
						//	value =0.000001;
						//  value = Math.max(value, -2.2);
						//  value = Math.min(value, 2.2);
						//System.out.println(i+"= ("+x+" , "+y+") = "+value);
					}

					bufferWrapper.putDouble(i, value.doubleValue());


				}
			}


			@SuppressWarnings("unchecked")
			ArrayList<AnalyticSurface.GridPointAttributes> attributesList
			= (ArrayList<GridPointAttributes>) AnalyticSurface.createColorGradientValues(bufferWrapper, 55, MIN_VALUE, MAX_VALUE, HUE_MIN, HUE_MAX);
			if(attributesList.size()==0)return null;

			GridPointAttributes cloud = AnalyticSurface.createGridPointAttributes(CLOUD_RENDER_VALUE,Color.white);
			GridPointAttributes water = AnalyticSurface.createGridPointAttributes(WATER_RENDER_VALUE,Color.CYAN);

			IntegerProperty cloudCount = new SimpleIntegerProperty(0);
			IntegerProperty totalCount = new SimpleIntegerProperty(0);
			IntegerProperty cultivoCount = new SimpleIntegerProperty(0);
			DoubleProperty ndviSuma = new SimpleDoubleProperty(0);
			attributesList.replaceAll((gpa)->{
				double value = gpa.getValue();
				//System.out.println("value "+value);
				if(value == CLOUD_VALUE) {

					cloudCount.set(cloudCount.getValue()+1);
					totalCount.set(totalCount.getValue()+1);

					return cloud;
				} else if (value == WATER_VALUE){
					totalCount.set(totalCount.getValue()+1);

					return water;
				} else {
					if(value >= ShowNDVITifFileTask.MIN_VALUE && value <= ShowNDVITifFileTask.MAX_VALUE ) {
						ndviSuma.set(ndviSuma.getValue()+value);
						cultivoCount.set(cultivoCount.getValue()+1);
						totalCount.set(totalCount.getValue()+1);
					}

					//totalCount.add(1);
					return gpa;
				}

			});

			double porcNubes=cloudCount.doubleValue()/totalCount.doubleValue();
			double ndviProm=ndviSuma.doubleValue()/cultivoCount.doubleValue();
			//System.out.println("ndviProm "+ndviProm);

			if(porcNubes>0.9) {
				System.out.println("ignorando layer por nublado porcNubes = "+porcNubes);
				return null;
			}
			//				surface.setValues(AnalyticSurface.createColorGradientValues(
			//						raster.getBuffer(), transparentValue, MIN_VALUE, MAX_VALUE, HUE_MIN, HUE_MAX));
			surface.setValues(attributesList);
			// surface.setVerticalScale(5e3);
			surface.setVerticalScale(100);
			//surface.setAltitude(-10);


			AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
			attr.setDrawOutline(false);
			attr.setDrawShadow(false);
			attr.setInteriorOpacity(1);
			surface.setSurfaceAttributes(attr);


			NumberFormat legendLabelFormat=Messages.getNumberFormat();
			final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(
					MAX_VALUE,
					MIN_VALUE,//FIXME valores invertidos funciona en una version de world wind y no en otra
					
					HUE_MIN, HUE_MAX,
					AnalyticSurfaceLegend.createDefaultColorGradientLabels(MIN_VALUE, MAX_VALUE, legendLabelFormat),
					AnalyticSurfaceLegend.createDefaultTitle(fileName));
			legend.setOpacity(1);
			legend.setScreenLocation(new Point(100, 400));


			LatLon ori = sector.getCentroid();
			 
			gov.nasa.worldwind.geom.Position pointPosition = gov.nasa.worldwind.geom.Position.fromDegrees(ori.latitude.degrees, ori.longitude.degrees);			
			PointPlacemark pmStandard = new PointPlacemark(pointPosition);
			PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
			pointAttribute.setImageColor(java.awt.Color.red);
			//		if(HiDPIHelper.isHiDPI()){
			//			pointAttribute.setLabelFont(java.awt.Font.decode("Verdana-Bold-50"));
			//		}
			pointAttribute.setLabelMaterial(Material.DARK_GRAY);
			pmStandard.setLabelText(fileName);
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

			NumberFormat df=Messages.getNumberFormat();
			if(porcNubes>0) {
				layer.setName(fileName+" "+df.format(porcNubes*100)+"% "+Messages.getString("ShowNDVITifFileTask.nublado"));
			}else {
				layer.setName(fileName);
			}
			layer.setPickEnabled(false);
			layer.addRenderable(surface);
			layer.addRenderable(renderable);

			//			if(ndvi==null){
			//				ndvi = new Ndvi();
			//				ndvi.setNombre(fileName);
			//				//ndvi.setF(file);	
			//				ndvi.updateContent(file);
			//				ndvi.setContorno(ownerPoli);
			//
			//				//04-01-2018
			//				//SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
			//				//System.out.println("convirtiendo fechaString "+fechaString);
			//				//convirtiendo fechaString 04-01-2018
			//				LocalDate fecha = null;
			//				try{
			//					if(fechaString.length()<"2017-03-28".length()) {
			//						String ymd = fechaString.substring(0,"2017-03-28".length());
			//						System.out.println("formateando la fecha con string "+ymd);
			//						DateTimeFormatter format1 = DateTimeFormatter.ofPattern(YYYY_MM_DD);	
			//						//formateando la fecha con string 06-02-2020
			//						fecha = LocalDate.parse(ymd, format1);//.parse(fechaString);//java.text.ParseException: Unparseable date: "Jag 20 30-08-20175528033450897731504"
			//						ndvi.setFecha(fecha);
			//					}
			//				}catch(Exception e){
			//					e.printStackTrace();
			//					System.err.println("no se pudo cargar la fecha del ndvi para "+fechaString);
			//				}
			//			}
			ndvi.setPorcNubes(Double.valueOf(porcNubes));
			ndvi.setMeanNDVI(ndviProm);
			ndvi.setPixelArea(pixelArea);//pixelArea);
			ndvi.setSurfaceLayer(surface);
			ndvi.setLayer(layer);



			// creando un ndvi con fecha
			//System.out.println("creando un ndvi con fecha "+fecha);
			//creando un ndvi con fecha Thu Jan 04 00:00:00 ART 2018

			layer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, ndvi);
			layer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, ndvi.getClass());

			layer.setValue(ProcessMapTask.ZOOM_TO_KEY, pointPosition);		

			return layer;

		} catch (Exception e)     {
			e.printStackTrace();
		}
		return null;
	}

	//	public Layer call_old() {	
	//		try{
	//			BufferWrapperRaster raster = ShowNDVITifFileTask.loadRasterFile(file);
	//			if (raster == null){
	//				return null;
	//			}
	//
	//			String fileName = file.getName();
	//			fileName= fileName.replace(".tif", "");
	//			//COPERNICUSS220170328T140051_20170328T140141_T20HNH.nd.tif
	//			if(fileName.contains("COPERNICUSS2")){
	//				fileName=fileName.replace("COPERNICUSS2", "");
	//				fileName=fileName.substring(0, "20170328".length());//anio
	//				fileName=fileName.substring("201703".length(), fileName.length())//dia
	//						+"-"+fileName.substring("2017".length(), "201703".length())//mes
	//						+"-"+fileName.substring(0, "2017".length());//anio
	//
	//			} else if(fileName.contains("LANDSATLC08C01T1_TOALC08_XXXXXX_")){
	//				fileName=fileName.replace("LANDSATLC08C01T1_TOALC08_XXXXXX_", "");
	//				fileName=fileName.substring(0, "20170328".length());
	//				fileName=fileName.substring("201703".length(), fileName.length())
	//						+"-"+fileName.substring("2017".length(), "201703".length())
	//						+"-"+fileName.substring(0, "2017".length());
	//			}
	//			//en este punto fileName tiene la fecha en formato 2017-03-28 es decir dd-MM-yyyy
	//
	//			String fechaString = new String (fileName);
	//
	//			if(ownerPoli !=null){
	//				fileName = ownerPoli.getNombre() +" "+ fileName;
	//			}
	//			if(ndvi!=null) {
	//				fileName = ndvi.getNombre();
	//			}
	//
	//			final ExportableAnalyticSurface surface = new ExportableAnalyticSurface();
	//			surface.setSector(raster.getSector());
	//			surface.setDimensions(raster.getWidth(), raster.getHeight());
	//
	//			//					surface.setExportImageName("ndviExportedImage");
	//			//					surface.setExportImagePath("/exportedImagePath");
	//			//					OutputStream outStream = null;					
	//			//					KMZDocumentBuilder kmzB = new KMZDocumentBuilder(outStream);
	//			//					kmzB.writeObject(surface);
	//			//					Object writer = writer = XMLOutputFactory.newInstance().createXMLStreamWriter(this.zipStream);
	//			//					surface.export(KMLConstants.KML_MIME_TYPE, writer);//mimeType, output);
	//
	//			double HUE_MIN = Clasificador.colors[0].getHue()/360d;//0d / 360d;
	//			double HUE_MAX = Clasificador.colors[Clasificador.colors.length-1].getHue()/360d;//240d / 360d;
	//			//TRANSPARENT_VALUE =raster.getTransparentValue();
	//			//	System.out.println("ndvi transparent value = "+transparentValue);
	//			//double transparentValue =extremes[0];
	//			//TRANSPARENT_VALUE=0;//para que pueda interpretar los valores clipeados como transparente
	//
	//			int width = raster.getWidth();
	//			int height = raster.getHeight();
	//			double dLat = raster.getSector().getDeltaLatDegrees();
	//			double dLon = raster.getSector().getDeltaLonDegrees();
	//
	//			Sector sector = raster.getSector();
	//			double latProm = (sector.getMaxLatitude().degrees+sector.getMinLatitude().degrees)/2;
	//			ProyectionConstants.setLatitudCalculo(latProm);
	//			double pixelArea = ProyectionConstants.A_HAS(((dLat*dLon)/(width*height)));//12... =~ 10
	//			//System.out.println("el area calculada del pixel es "+pixelArea);
	//
	//			//acoto los valores entre -2.2 y 2.2
	//			BufferWrapper buffer = raster.getBuffer();
	//			//log.setLevel(Level.ALL);
	//
	//			for(int i=0;i<buffer.length();i++){
	//				double value = buffer.getDouble(i);
	//				//log.info("agregando value "+value);
	//				//System.out.println("agregando value " +value);
	//				if(Double.isNaN(value) || Double.isInfinite(value)) {					
	//					//log.fine("agregando Nan a transparente");
	//					//System.out.println("agregando value a transparente" +value);
	//					value=TRANSPARENT_VALUE;
	//				} else {
	//					//	value =0.000001;
	//					//  value = Math.max(value, -2.2);
	//					//  value = Math.min(value, 2.2);
	//				}
	//				buffer.putDouble(i, value);
	//			}
	//
	//			@SuppressWarnings("unchecked")
	//			ArrayList<AnalyticSurface.GridPointAttributes> attributesList
	//			= (ArrayList<GridPointAttributes>) AnalyticSurface.createColorGradientValues(raster.getBuffer(), 55, MIN_VALUE, MAX_VALUE, HUE_MIN, HUE_MAX);
	//			if(attributesList.size()==0)return null;
	//
	//			GridPointAttributes cloud = AnalyticSurface.createGridPointAttributes(CLOUD_RENDER_VALUE,Color.white);
	//			GridPointAttributes water = AnalyticSurface.createGridPointAttributes(WATER_RENDER_VALUE,Color.CYAN);
	//
	//			IntegerProperty cloudCount = new SimpleIntegerProperty(0);
	//			IntegerProperty totalCount = new SimpleIntegerProperty(0);
	//			IntegerProperty cultivoCount = new SimpleIntegerProperty(0);
	//			DoubleProperty ndviSuma = new SimpleDoubleProperty(0);
	//			attributesList.replaceAll((gpa)->{
	//				double value = gpa.getValue();
	//				if(value == CLOUD_VALUE) {
	//
	//					cloudCount.set(cloudCount.getValue()+1);
	//					totalCount.set(totalCount.getValue()+1);
	//
	//					return cloud;
	//				} else if (value == WATER_VALUE){
	//					totalCount.set(totalCount.getValue()+1);
	//
	//					return water;
	//				} else {
	//					if(value >= ShowNDVITifFileTask.MIN_VALUE && value <= ShowNDVITifFileTask.MAX_VALUE ) {
	//						ndviSuma.set(ndviSuma.getValue()+value);
	//						cultivoCount.set(cultivoCount.getValue()+1);
	//						totalCount.set(totalCount.getValue()+1);
	//					}
	//
	//					//totalCount.add(1);
	//					return gpa;
	//				}
	//
	//			});
	//
	//			double porcNubes=cloudCount.doubleValue()/totalCount.doubleValue();
	//			double ndviProm=ndviSuma.doubleValue()/cultivoCount.doubleValue();
	//
	//			if(porcNubes>0.9) {
	//				System.out.print("ignorando layer por nublado");
	//				return null;
	//			}
	//			//			surface.setValues(AnalyticSurface.createColorGradientValues(
	//			//					raster.getBuffer(), transparentValue, MIN_VALUE, MAX_VALUE, HUE_MIN, HUE_MAX));
	//			surface.setValues(attributesList);
	//			// surface.setVerticalScale(5e3);
	//			surface.setVerticalScale(100);
	//			//surface.setAltitude(-10);
	//
	//
	//			AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
	//			attr.setDrawOutline(false);
	//			attr.setDrawShadow(false);
	//			attr.setInteriorOpacity(1);
	//			surface.setSurfaceAttributes(attr);
	//
	//			Format legendLabelFormat = new DecimalFormat() ;
	//			final AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(MIN_VALUE,MAX_VALUE,
	//					HUE_MIN, HUE_MAX,
	//					AnalyticSurfaceLegend.createDefaultColorGradientLabels(MIN_VALUE, MAX_VALUE, legendLabelFormat),
	//					AnalyticSurfaceLegend.createDefaultTitle(fileName));
	//			legend.setOpacity(1);
	//			legend.setScreenLocation(new Point(100, 400));
	//
	//
	//			LatLon ori = sector.getCentroid();
	//			Position pointPosition = Position.fromDegrees(ori.latitude.degrees, ori.longitude.degrees);			
	//			PointPlacemark pmStandard = new PointPlacemark(pointPosition);
	//			PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
	//			pointAttribute.setImageColor(java.awt.Color.red);
	//			//		if(HiDPIHelper.isHiDPI()){
	//			//			pointAttribute.setLabelFont(java.awt.Font.decode("Verdana-Bold-50"));
	//			//		}
	//			pointAttribute.setLabelMaterial(Material.DARK_GRAY);
	//			pmStandard.setLabelText(fileName);
	//			pmStandard.setAttributes(pointAttribute);
	//
	//
	//			Renderable renderable =  new Renderable()	{
	//				public void render(DrawContext dc)
	//				{
	//					Extent extent = surface.getExtent(dc);
	//					if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
	//						return;
	//
	//					if (WWMath.computeSizeInWindowCoordinates(dc, extent) < 300){
	//						pmStandard.render(dc);
	//						return;
	//					}
	//
	//					legend.render(dc);
	//				}
	//			};
	//			SurfaceImageLayer layer = new SurfaceImageLayer(){
	//				@Override
	//				public void setOpacity(double opacity){
	//					//System.out.println("setting opacity en SurfaceImageLayer"+opacity);
	//					AnalyticSurfaceAttributes attributes = surface.getSurfaceAttributes();
	//					attributes.setInteriorOpacity(opacity);
	//					surface.setSurfaceAttributes(attributes);
	//					legend.setOpacity(opacity);
	//
	//				}
	//
	//			};
	//
	//			DecimalFormat df = new DecimalFormat(Messages.getString("GenerarMuestreoDirigidoTask.5")); //$NON-NLS-1$
	//			//XXX quito la informacion de nublado porque me rompe el ordenamiento y evoluvion de ndvi
	//			layer.setName(fileName);//+" "+df.format(porcNubes*100)+"% "+Messages.getString("ShowNDVITifFileTask.nublado"));
	//			layer.setPickEnabled(false);
	//			layer.addRenderable(surface);
	//			layer.addRenderable(renderable);
	//			if(ndvi==null){
	//				ndvi = new Ndvi();
	//				ndvi.setNombre(fileName);
	//				//ndvi.setF(file);			
	//				ndvi.updateContent(file);
	//				ndvi.setContorno(ownerPoli);
	//
	//				//04-01-2018
	//				//SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
	//				//System.out.println("convirtiendo fechaString "+fechaString);
	//				//convirtiendo fechaString 04-01-2018
	//				LocalDate fecha = null;
	//				try{
	//					if(fechaString.length()<"2017-03-28".length()) {
	//						String ymd = fechaString.substring(0,"2017-03-28".length());
	//						System.out.println("formateando la fecha con string "+ymd);
	//						DateTimeFormatter format1 = DateTimeFormatter.ofPattern(YYYY_MM_DD);	
	//						//formateando la fecha con string 06-02-2020
	//						fecha = LocalDate.parse(ymd, format1);//.parse(fechaString);//java.text.ParseException: Unparseable date: "Jag 20 30-08-20175528033450897731504"
	//						ndvi.setFecha(fecha);
	//					}
	//				}catch(Exception e){
	//					e.printStackTrace();
	//					System.err.println("no se pudo cargar la fecha del ndvi para "+fechaString);
	//				}
	//			}
	//			ndvi.setPorcNubes(Double.parseDouble(porcNubes));
	//			ndvi.setMeanNDVI(ndviProm);
	//			ndvi.setPixelArea(pixelArea);//pixelArea);
	//			ndvi.setSurfaceLayer(surface);
	//			ndvi.setLayer(layer);
	//
	//
	//
	//			// creando un ndvi con fecha
	//			//System.out.println("creando un ndvi con fecha "+fecha);
	//			//creando un ndvi con fecha Thu Jan 04 00:00:00 ART 2018
	//
	//			layer.setValue(Labor.LABOR_LAYER_IDENTIFICATOR, ndvi);
	//			layer.setValue(Labor.LABOR_LAYER_CLASS_IDENTIFICATOR, ndvi.getClass());
	//
	//			layer.setValue(ProcessMapTask.ZOOM_TO_KEY, pointPosition);		
	//
	//
	//			//	Sector s = raster.getSector();
	//			//	BufferWrapper buffer = raster.getBuffer();
	//			//			int nFilas =raster.getHeight();
	//			//			int nCols =raster.getWidth();//buffer.length()/raster.getWidth();
	//			//			int filaV=0,colV = 0;
	//			//			for(int col =0;col<nCols;col++){
	//			//			for(int fila =0;fila<nFilas;fila++){
	//			//				int index = fila*raster.getWidth()+col;
	//			//				double value = buffer.getDouble(index);
	//			//				//System.out.println("raster value for "+fila+","+col+" : "+value);
	//			//				if(value > 0.2 ){//si no hay dato lee 0.0
	//			//					filaV = fila;
	//			//					colV=col;
	//			//					break;
	//			//				}				
	//			//			}
	//			//			if(filaV>0)break;
	//			//			}
	//			//		//	System.out.println("fila= "+filaV);
	//			//			double latDelta = dLat*filaV/raster.getHeight();
	//			//			double lonDelta = lon*colV/raster.getWidth();
	//			//	System.out.println("latDelta= "+latDelta);
	//			//TODO en vez de usar sector usar el metodo de labor para encontrar un vertice
	//
	//			return layer;
	//
	//		} catch (Exception e)     {
	//			e.printStackTrace();
	//		}
	//		return null;
	//	}

	public RasterWraperApache loadRaster(Ndvi target) throws IOException {
		//System.out.println("loading raster target "+target.getName());

		if(!(target.getContent().length>0)) {
			System.out.println("el content de "+target.getNombre()+" es cero");
			return null;
		}
		TIFFImage tiffImage = TiffReader.readTiff(target.getContent());

		List<FileDirectory> directories = tiffImage.getFileDirectories();
		FileDirectory directory = directories.get(0);

		Rasters rasters = directory.readRasters();

		Set<FileDirectoryEntry> entries = directory.getEntries();

		RasterWraperApache wrapper = new RasterWraperApache(rasters,entries);

		return wrapper;
	}


	public RasterWraperApache loadRaster(File target) throws IOException {
		//System.out.println("loading raster target "+target.getName());


		TIFFImage tiffImage = TiffReader.readTiff(target);

		List<FileDirectory> directories = tiffImage.getFileDirectories();
		FileDirectory directory = directories.get(0);

		Rasters rasters = directory.readRasters();

		Set<FileDirectoryEntry> entries = directory.getEntries();

		RasterWraperApache wrapper = new RasterWraperApache(rasters,entries);

		return wrapper;
	}


	public static BufferWrapperRaster loadRasterFile(File file){
		//		if(!file.exists()){	
		//			//TODO si el recurso es web podemos bajarlo a 
		//			// Download the data and save it in a temp file.
		//			String path = file.getAbsolutePath();
		//			file = ExampleUtil.saveResourceToTempFile(path, "." + WWIO.getSuffix(path));
		//		}






		// Create a raster reader for the file type.
		//		DataRasterReaderFactory readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
		//				AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
		BasicDataRasterReaderFactory readerFactory = new BasicDataRasterReaderFactory();
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
			DataRaster[] rasters = reader.read(file, null);// BufferUnderflowException - If there are fewer than eight bytes remaining in this buffer
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
