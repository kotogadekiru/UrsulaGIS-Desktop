package com.ursulagis.desktop.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.util.SystemOutLogger;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.ArrayMap;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Poligono;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.utils.DateConverter;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.utils.UnzipUtility;

@Deprecated
public class GetNDVI2ForLaborTask extends Task<List<File>>{
	//private static final String URSULA_TOKEN = "ursulaToken";

	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";
	int MAX_URL_LENGHT = 4443;//2048 segun un stackoverflow //4443 segun pruevas con chrome// corresponde a 129 puntos
	protected int featureCount=0;
	protected int featureNumber=0;
	protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();

	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	private static final String URSULA_GIS_TOKEN = "ursulaGIS"+JFXMain.VERSION;//"ursulaGISv23";
	private static final String TOKEN = "token";


	private static final String BASE_URL = "https://gee-api-helper.herokuapp.com";
	//private static final String BASE_URL = "https://gee-api-helper-staging.herokuapp.com";
	//private static final String BASE_URL = "http://www.ursulagis.com/api/ndvi/v4/SR/";
	//private static final String BASE_URL = "http://localhost:5001";

	//private static final String HTTP_GEE_API_HELPER_HEROKUAPP_COM_NDVI_V3 = BASE_URL+"/chloroi_v1_SR";//"/ndvi_v4";//+"/gndvi_v4_SR";//"/ndvi_v3";//ndvi_v5
	private static final String HTTP_GEE_API_HELPER_HEROKUAPP_COM_NDVI_V3 = BASE_URL+"/ndvi_v4";//+"/chloroi_v1_SR";//+"/gndvi_v4_SR";//"/ndvi_v3";//ndvi_v5
	private static final String HTTP_GEE_API_HELPER_HEROKUAPP_COM_NDVI_V3PNG = BASE_URL+"/ndvi_v4PNG";
	private static final String HTTPS_GEE_API_HELPER_HEROKUAPP_COM_S2_PRODUCT_FINDER = BASE_URL+"/s2_product_finder_v3";
	private static final String GEE_POLYGONS_GET_REQUEST_KEY = "polygons";
	//private static final String GEE_ASSETS_GET_REQUEST_KEY = "assets";
	private static final String ID = "id";
	//private static final String FEATURES = "features";
	private static final String DATA = "data";
	private static final String END = "end";
	private static final String BEGIN = "begin"; //Data availability (time)	Jun 23, 2015 - Apr 18, 2017
	private static final String PATH2 = "path2";

	private static final String COPERNICUS_S2 = "COPERNICUSS2";

	Object placementObject = null;
	private LocalDate end;
	private LocalDate begin;
	private File downloadDir=null;
	
	private List<File> observableList =null;
	
	private DecimalFormat bdf = new DecimalFormat("#,###.00");
	
	public GetNDVI2ForLaborTask(Object labor, File downloadDirectory,List<File> _observableList ) {
		this.placementObject=labor;
		downloadDir=downloadDirectory;
		observableList=_observableList;
	}

	/**
	 * metodo que toma una labor, ejecuta un request a la api de gee y devuelve los assets que coinciden con la labor
	 * @param placementObject
	 * @return
	 */
	public List<LocalDate> getSentinellAssets(Object placementObject){			
		String polygons =getPolygonsFromLabor(placementObject);

		if(placementObject instanceof Labor){
			Labor<?> c =(Labor<?>)placementObject;
			LocalDate endDate = DateConverter.asLocalDate((Date) c.getFecha());//fechaProperty.getValue(); // "1/8/2016" ->
			//System.out.println("parseando la fecha "+endDate);			
			end= endDate;
			begin=endDate.minusMonths(1);
		}

		DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String sEnd = format1.format(this.end);
		String sBegin = format1.format(this.begin);
		//System.out.println("buscando los ndvi entre "+sBegin+" y "+sEnd);

		GenericUrl url = new GenericUrl(HTTPS_GEE_API_HELPER_HEROKUAPP_COM_S2_PRODUCT_FINDER);

		Map<String, String> req_data = new HashMap<String, String>();
		req_data.put(GEE_POLYGONS_GET_REQUEST_KEY, polygons);		
		req_data.put(BEGIN, sBegin);
		req_data.put(END, sEnd);
		req_data.put(TOKEN, URSULA_GIS_TOKEN);

		final HttpContent req_content = new JsonHttpContent(new JacksonFactory(), req_data);

		HttpResponse response = makePostRequest(url,req_content);//response pueded ser null
		if(response==null)return  new ArrayList<LocalDate>();
		try {

			GenericJson content = response.parseAs(GenericJson.class);

			//@SuppressWarnings("unchecked")
			//ArrayMap<String,Object> data = ((ArrayMap<String, Object>) content.get(DATA));
			//	System.out.println("assets data response: "+ content);
			response.disconnect();
			return parseAssetsData(content);

			//asset obtenido ={"data": {"properties": {"thumb": "https://mw1.google.com/ges/dd/images/s2_thumb.png", "title": "Sentinel-2: MultiSpectral Instrument (MSI), Level-1C", "date_range": [1435017600000.0, 1487116800000.0], "system:visualization_0_max": 3000, "system:visualization_0_name": "RGB", "period": 0, "sample": "https://mw1.google.com/ges/dd/images/s2_sample.png", "system:visualization_0_min": 0, "description": "<p>SENTINEL-2 is a wide-swath, high-resolution, multi-spectral imaging mission supporting Copernicus Land Monitoring studies, including the monitoring of vegetation, soil and water cover, as well as observation of inland waterways and coastal areas.</p>  <p>The SENTINEL-2 data contain 13 UINT16 spectral bands representing TOA reflectance scaled by 10000: <table> <th>Band</th> <th>Use</th> <th>Wavelength</th> <th>Resolution</th> <tr> <td>B1</td> <td>Aerosols</td> <td>443nm</td> <td>60m</td> </tr> <tr> <td>B2</td> <td>Blue</td> <td>490nm</td> <td>10m</td> </tr> <tr> <td>B3</td> <td>Green</td> <td>560nm</td> <td>10m</td> </tr> <tr> <td>B4</td> <td>Red</td> <td>665nm</td> <td>10m</td> </tr> <tr> <td>B5</td> <td>Red Edge 1</td> <td>705nm</td> <td>20m</td> </tr> <tr> <td>B6</td> <td>Red Edge 2</td> <td>740nm</td> <td>20m</td> </tr> <tr> <td>B7</td> <td>Red Edge 3</td> <td>783nm </td> <td>20m</td> </tr> <tr> <td>B8</td> <td>NIR</td> <td>842nm</td> <td>10m</td> </tr> <tr> <td>B8a</td> <td>Red Edge 4</td> <td>865nm</td> <td>20m</td> </tr> <tr> <td>B9</td> <td>Water vapor</td> <td>940nm</td> <td>60m</td> </tr> <tr> <td>B10</td> <td>Cirrus</td> <td>1375nm</td> <td>60m</td> </tr> <tr> <td>B11</td> <td>SWIR 1</td> <td>1610nm</td> <td>20m</td> </tr> <tr> <td>B12</td> <td>SWIR 2</td> <td>2190nm</td> <td>20m</td> </tr> </table></p>  <p>See <a href='https://sentinel.esa.int/documents/247904/685211/Sentinel-2_User_Handbook'>Sentinel 2 User Handbook</a> for details. In addition, the following bands are present:  <ul> <li>QA10: currently always empty</li> <li>QA20: currently always empty</li> <li>QA60: bit mask band with cloud mask information. Bit 10 is set if the corresponding 60m pixel has been marked as OPAQUE. Bit 11 is set if the corresponding 60m pixel has been marked as CIRRUS. <a href='https://sentinel.esa.int/web/sentinel/technical-guides/sentinel-2-msi/level-1c/land-water-cloud-masks'> See the full explanation of how cloud masks are computed.</a></li> </ul> </p>  <p>Each Sentinel 2 product (zip archive) contains multiple granules. Each granule becomes a separate Earth Engine asset. EE asset ids for Sentinel 2 assets look like this: COPERNICUS/S2/20151128T002653_20151128T102149_T56MNN. Here the first numeric part represents the sensing date and time, the second numeric part represents the product generation date and time, and the final 6-character string is a unique granule identifier indicating its UTM grid reference (see <a href='https://en.wikipedia.org/wiki/Military_grid_reference_system'>MGRS</a>).  <p>Several Sentinel-specific metadata fields are taken from the original metadata, including: <ul> <li>CLOUDY_PIXEL_PERCENTAGE: granule-specific cloudy pixel percentage.</li> <li>CLOUD_COVERAGE_ASSESSMENT: cloudy pixel percentage for the whole archive that contains this granule.</li> </ul>  Also, each S2 Earth Engine asset has a reference to the archive name for the product that the asset's granule was taken from: <ul> <li>PRODUCT_ID; the full id of the original Sentinel 2 product.</li> </ul>  <p>The use of Sentinel data is governed by the <a href='https://scihub.copernicus.eu/twiki/pub/SciHubWebPortal/TermsConditions/Sentinel_Data_Terms_and_Conditions.pdf'>Copernicus Sentinel Data Terms and Conditions</a>.</p> ", "provider_url": "https://sentinel.esa.int/web/sentinel/user-guides/sentinel-2-msi", "tags": ["eu", "esa", "copernicus", "sentinel", "msi", "radiance"], "provider": "European Union/ESA/Copernicus", "system:visualization_0_bands": "B04,B03,B02"}, "id": "COPERNICUS/S2", "features": [{"bands": [{"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B1", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B2", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B3", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B4", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B5", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B6", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B7", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B8", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B8A", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B9", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B10", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B11", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B12", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"id": "QA10", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [10980, 10980], "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}}, {"id": "QA20", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [5490, 5490], "data_type": {"max": 4294967295, "precision": "int", "min": 0, "type": "PixelType"}}, {"id": "QA60", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [1830, 1830], "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}}], "id": "COPERNICUS/S2/20170127T140051_20170127T140834_T20HNH", "version": 1485761178555000, "properties": {"system:footprint": {"coordinates": [[-61.819239281830036, -33.43332561633586], [-61.81925703184291, -33.4333229726925], [-62.7746120563906, -33.43873908682031], [-62.77465299938557, -33.438769352168926], [-62.77470493139525, -33.43878368029093], [-62.7747155148453, -33.438804371468905], [-62.82444952313311, -33.583921379464265], [-62.8578943898261, -33.692198834352375], [-62.89917044363287, -33.8264463517792], [-62.99772043096514, -34.15009214479483], [-63.000325754689314, -34.166066307453725], [-63.000326773383755, -34.42914148092107], [-63.000282437423245, -34.42918294702473], [-63.000242412121544, -34.429231470325604], [-61.80552256701574, -34.423398198210464], [-61.80547283084268, -34.42336114547148], [-61.805417864017265, -34.423329518661504], [-61.805414664132634, -34.42331469839915], [-61.8123697515832, -33.92838012999751], [-61.81915701214183, -33.43341201759283], [-61.81920139487949, -33.43337097074325], [-61.819239281830036, -33.43332561633586]], "type": "LinearRing"}, "PRODUCT_ID": "S2A_MSIL1C_20170127T140051_N0204_R067_T20HNH_20170127T140834", "FORMAT_CORRECTNESS_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B8": 7.72369869995664, "GENERAL_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B10": 101.441555665013, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B11": 101.487425281538, "REFLECTANCE_CONVERSION_CORRECTION": 1.0320757775284, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B9": 101.599178300475, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B4": 101.400753687056, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B5": 101.458357038042, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B6": 101.480437533335, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B7": 101.52117418076, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B1": 101.598688890412, "SENSING_ORBIT_DIRECTION": "DESCENDING", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B3": 101.36258430796, "SPACECRAFT_NAME": "Sentinel-2A", "SOLAR_IRRADIANCE_B9": 813.04, "GRI_FILENAME": "S2A_OPER_AUX_GRI065_PDMC_20130621T120000_S20130101T000000", "ECMWF_DATA_REF": "S2__OPER_AUX_ECMWFD_PDMC_20170127T000000_V20170127T120000_20170128T000000", "SOLAR_IRRADIANCE_B3": 1822.61, "SOLAR_IRRADIANCE_B2": 1941.63, "SOLAR_IRRADIANCE_B1": 1913.57, "RADIOMETRIC_QUALITY_FLAG": "PASSED", "SOLAR_IRRADIANCE_B7": 1163.19, "SOLAR_IRRADIANCE_B6": 1288.32, "SOLAR_IRRADIANCE_B5": 1425.56, "SOLAR_IRRADIANCE_B4": 1512.79, "DATATAKE_TYPE": "INS-NOBS", "IERS_BULLETIN_FILENAME": "S2__OPER_AUX_UT1UTC_PDMC_20170126T000000_V20170127T000000_20180126T000000", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B2": 101.321174817635, "MEAN_SOLAR_AZIMUTH_ANGLE": 70.0220193260797, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B8A": 101.516340540985, "SENSING_ORBIT_NUMBER": 67.0, "CLOUDY_PIXEL_PERCENTAGE": 0.0, "SOLAR_IRRADIANCE_B8A": 955.19, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B12": 101.566848891123, "GRANULE_ID": "L1C_T20HNH_A008358_20170127T140834", "MEAN_INCIDENCE_ZENITH_ANGLE_B11": 7.81300137594528, "MEAN_INCIDENCE_ZENITH_ANGLE_B10": 7.74970340853725, "MEAN_INCIDENCE_ZENITH_ANGLE_B12": 7.87339825732469, "system:time_end": 1485526114471, "CLOUD_COVERAGE_ASSESSMENT": 0.0, "SENSOR_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B8A": 7.88983721750514, "MEAN_INCIDENCE_ZENITH_ANGLE_B6": 7.82238134216798, "PRODUCT_URI": "S2A_MSIL1C_20170127T140051_N0204_R067_T20HNH_20170127T140834.SAFE", "SOLAR_IRRADIANCE_B12": 85.25, "SOLAR_IRRADIANCE_B11": 245.59, "SOLAR_IRRADIANCE_B10": 367.15, "MGRS_TILE": "20HNH", "system:asset_size": 1170449747, "MEAN_INCIDENCE_ZENITH_ANGLE_B5": 7.79660845295558, "MEAN_INCIDENCE_ZENITH_ANGLE_B4": 7.77761510886907, "MEAN_INCIDENCE_ZENITH_ANGLE_B7": 7.85483966043568, "GEOMETRIC_QUALITY_FLAG": "FAILED", "MEAN_INCIDENCE_ZENITH_ANGLE_B1": 7.92359025137159, "MEAN_INCIDENCE_ZENITH_ANGLE_B3": 7.74262301650035, "MEAN_INCIDENCE_ZENITH_ANGLE_B2": 7.71545546679167, "system:index": "20170127T140051_20170127T140834_T20HNH", "DATATAKE_IDENTIFIER": "GS2A_20170127T140051_008358_N02.04", "MEAN_INCIDENCE_ZENITH_ANGLE_B9": 7.96070843029955, "DATASTRIP_ID": "S2A_OPER_MSI_L1C_DS_SGS__20170127T185456_S20170127T140834_N02.04", "SOLAR_IRRADIANCE_B8": 1036.39, "GENERATION_TIME": 1485526114000, "MEAN_SOLAR_ZENITH_ANGLE": 32.9714392274947, "PRODUCTION_DEM_TYPE": "S2__OPER_DEM_GLOBEF_PDMC_19800101T000000_S19800101T000000", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B8": 101.353346878156, "DEGRADED_MSI_DATA_PERCENTAGE": 0.0, "system:time_start": 1485526114471, "PROCESSING_BASELINE": "02.04"}, "type": "Image"}, {"bands": [{"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B1", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B2", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B3", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B4", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B5", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B6", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B7", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B8", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B8A", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B9", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B10", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B11", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B12", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"id": "QA10", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [10980, 10980], "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}}, {"id": "QA20", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [5490, 5490], "data_type": {"max": 4294967295, "precision": "int", "min": 0, "type": "PixelType"}}, {"id": "QA60", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [1830, 1830], "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}}], "id": "COPERNICUS/S2/20170206T140051_20170206T140632_T20HNH", "version": 1486706209342000, "properties": {"system:footprint": {"coordinates": [[-61.81923928182774, -33.43332561633631], [-61.81925703184165, -33.43332297269265], [-62.77558030361335, -33.43874084271251], [-62.77561810019611, -33.43876878168642], [-62.77566736896839, -33.4387780040758], [-62.77568034311826, -33.438797724802875], [-62.77825030667524, -33.44421783185332], [-62.80648036921582, -33.52328271663303], [-62.98211104948312, -34.09056328781995], [-62.99511728777705, -34.13926899208452], [-63.000325743232615, -34.16308811150741], [-63.000326773383755, -34.429141480921075], [-63.000282437423245, -34.42918294702473], [-63.00024241212153, -34.429231470325604], [-61.80552256701574, -34.423398198210464], [-61.80547283084268, -34.42336114547148], [-61.805417864017265, -34.423329518661504], [-61.805414664132634, -34.42331469839915], [-61.8123697515832, -33.92838012999751], [-61.81915701214183, -33.43341201759283], [-61.81920139487894, -33.433370970743766], [-61.81923928182774, -33.43332561633631]], "type": "LinearRing"}, "PRODUCT_ID": "S2A_MSIL1C_20170206T140051_N0204_R067_T20HNH_20170206T140632", "FORMAT_CORRECTNESS_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B8": 7.72087418841092, "GENERAL_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B10": 101.417775155007, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B11": 101.488376304212, "REFLECTANCE_CONVERSION_CORRECTION": 1.02948514449663, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B9": 101.593664605364, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B4": 101.38735125322, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B5": 101.411184503444, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B6": 101.431716209234, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B7": 101.437042822351, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B1": 101.539413807944, "SENSING_ORBIT_DIRECTION": "DESCENDING", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B3": 101.332561694526, "SPACECRAFT_NAME": "Sentinel-2A", "SOLAR_IRRADIANCE_B9": 813.04, "GRI_FILENAME": "S2A_OPER_AUX_GRI065_PDMC_20130621T120000_S20130101T000000", "ECMWF_DATA_REF": "S2__OPER_AUX_ECMWFD_PDMC_20170206T000000_V20170206T120000_20170207T000000", "SOLAR_IRRADIANCE_B3": 1822.61, "SOLAR_IRRADIANCE_B2": 1941.63, "SOLAR_IRRADIANCE_B1": 1913.57, "RADIOMETRIC_QUALITY_FLAG": "PASSED", "SOLAR_IRRADIANCE_B7": 1163.19, "SOLAR_IRRADIANCE_B6": 1288.32, "SOLAR_IRRADIANCE_B5": 1425.56, "SOLAR_IRRADIANCE_B4": 1512.79, "DATATAKE_TYPE": "INS-NOBS", "IERS_BULLETIN_FILENAME": "S2__OPER_AUX_UT1UTC_PDMC_20170202T000000_V20170203T000000_20180202T000000", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B2": 101.290247482918, "MEAN_SOLAR_AZIMUTH_ANGLE": 66.2098468436741, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B8A": 101.458755074051, "SENSING_ORBIT_NUMBER": 67.0, "CLOUDY_PIXEL_PERCENTAGE": 2.5373, "SOLAR_IRRADIANCE_B8A": 955.19, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B12": 101.540713188865, "GRANULE_ID": "L1C_T20HNH_A008501_20170206T140632", "MEAN_INCIDENCE_ZENITH_ANGLE_B11": 7.79556715616444, "MEAN_INCIDENCE_ZENITH_ANGLE_B10": 7.74753418873838, "MEAN_INCIDENCE_ZENITH_ANGLE_B12": 7.86677157145221, "system:time_end": 1486389992802, "CLOUD_COVERAGE_ASSESSMENT": 2.5373, "SENSOR_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B8A": 7.87987176428214, "MEAN_INCIDENCE_ZENITH_ANGLE_B6": 7.82373704697061, "PRODUCT_URI": "S2A_MSIL1C_20170206T140051_N0204_R067_T20HNH_20170206T140632.SAFE", "SOLAR_IRRADIANCE_B12": 85.25, "SOLAR_IRRADIANCE_B11": 245.59, "SOLAR_IRRADIANCE_B10": 367.15, "MGRS_TILE": "20HNH", "system:asset_size": 1213908597, "MEAN_INCIDENCE_ZENITH_ANGLE_B5": 7.79795327088808, "MEAN_INCIDENCE_ZENITH_ANGLE_B4": 7.77079911743578, "MEAN_INCIDENCE_ZENITH_ANGLE_B7": 7.84877227015588, "GEOMETRIC_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B1": 7.91366869340992, "MEAN_INCIDENCE_ZENITH_ANGLE_B3": 7.73590158219813, "MEAN_INCIDENCE_ZENITH_ANGLE_B2": 7.70871523495296, "system:index": "20170206T140051_20170206T140632_T20HNH", "DATATAKE_IDENTIFIER": "GS2A_20170206T140051_008501_N02.04", "MEAN_INCIDENCE_ZENITH_ANGLE_B9": 7.95064209431493, "DATASTRIP_ID": "S2A_OPER_MSI_L1C_DS_SGS__20170206T203517_S20170206T140632_N02.04", "SOLAR_IRRADIANCE_B8": 1036.39, "GENERATION_TIME": 1486389992000, "MEAN_SOLAR_ZENITH_ANGLE": 34.9190601366047, "PRODUCTION_DEM_TYPE": "S2__OPER_DEM_GLOBEF_PDMC_19800101T000000_S19800101T000000", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B8": 101.310060902374, "DEGRADED_MSI_DATA_PERCENTAGE": 0.0, "system:time_start": 1486389992802, "PROCESSING_BASELINE": "02.04"}, "type": "Image"}, {"bands": [{"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B1", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B2", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B3", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B4", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B5", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B6", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B7", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B8", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "dimensions": [10980, 10980], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B8A", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B9", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B10", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "dimensions": [1830, 1830], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B11", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"properties": {"system:nodata_value": 0.0}, "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}, "id": "B12", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "dimensions": [5490, 5490], "crs": "EPSG:32720"}, {"id": "QA10", "crs_transform": [10.0, 0.0, 499980.0, 0.0, -10.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [10980, 10980], "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}}, {"id": "QA20", "crs_transform": [20.0, 0.0, 499980.0, 0.0, -20.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [5490, 5490], "data_type": {"max": 4294967295, "precision": "int", "min": 0, "type": "PixelType"}}, {"id": "QA60", "crs_transform": [60.0, 0.0, 499980.0, 0.0, -60.0, 6300040.0], "crs": "EPSG:32720", "dimensions": [1830, 1830], "data_type": {"max": 65535, "precision": "int", "min": 0, "type": "PixelType"}}], "id": "COPERNICUS/S2/20170216T140051_20170216T140238_T20HNH", "version": 1487646635752000, "properties": {"system:footprint": {"coordinates": [[-63.00028243742321, -34.42918294702477], [-63.000242412121544, -34.429231470325604], [-61.80552256701574, -34.423398198210464], [-61.80547283084268, -34.42336114547148], [-61.80541448214799, -34.42332757280011], [-61.8123697515832, -33.92838012999751], [-61.81915701214183, -33.43341201759283], [-61.81920139487929, -33.43337097074343], [-61.81924161261129, -33.433322826158644], [-62.774950791025965, -33.438739702254736], [-62.774983185362125, -33.43874933657661], [-62.77562798712367, -33.43902109622738], [-62.77564097731928, -33.43904820645398], [-62.77567389415508, -33.43905810431031], [-62.7756838158894, -33.43907902257399], [-62.78560985861644, -33.47129689582426], [-62.99902051875921, -34.15901563315487], [-62.99999808688343, -34.16226624991174], [-63.00032574635743, -34.16390045635071], [-63.000326773383755, -34.429141480921075], [-63.00028243742321, -34.42918294702477]], "type": "LinearRing"}, "PRODUCT_ID": "S2A_MSIL1C_20170216T140051_N0204_R067_T20HNH_20170216T140238", "FORMAT_CORRECTNESS_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B8": 7.72191935006344, "GENERAL_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B10": 101.419348636653, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B11": 101.489648631237, "REFLECTANCE_CONVERSION_CORRECTION": 1.02601887961432, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B9": 101.594288578126, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B4": 101.377103557168, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B5": 101.412544469152, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B6": 101.432939695596, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B7": 101.467039398032, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B1": 101.540180966808, "SENSING_ORBIT_DIRECTION": "DESCENDING", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B3": 101.334278322561, "SPACECRAFT_NAME": "Sentinel-2A", "SOLAR_IRRADIANCE_B9": 813.04, "GRI_FILENAME": "S2A_OPER_AUX_GRI065_PDMC_20130621T120000_S20130101T000000", "ECMWF_DATA_REF": "S2__OPER_AUX_ECMWFD_PDMC_20170216T000000_V20170216T090000_20170216T210000", "SOLAR_IRRADIANCE_B3": 1822.61, "SOLAR_IRRADIANCE_B2": 1941.63, "SOLAR_IRRADIANCE_B1": 1913.57, "RADIOMETRIC_QUALITY_FLAG": "PASSED", "SOLAR_IRRADIANCE_B7": 1163.19, "SOLAR_IRRADIANCE_B6": 1288.32, "SOLAR_IRRADIANCE_B5": 1425.56, "SOLAR_IRRADIANCE_B4": 1512.79, "DATATAKE_TYPE": "INS-NOBS", "IERS_BULLETIN_FILENAME": "S2__OPER_AUX_UT1UTC_PDMC_20170209T000000_V20170210T000000_20180209T000000", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B2": 101.292179741435, "MEAN_SOLAR_AZIMUTH_ANGLE": 61.8693047313385, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B8A": 101.459692977312, "SENSING_ORBIT_NUMBER": 67.0, "CLOUDY_PIXEL_PERCENTAGE": 46.001, "SOLAR_IRRADIANCE_B8A": 955.19, "MEAN_INCIDENCE_AZIMUTH_ANGLE_B12": 101.522984693803, "GRANULE_ID": "L1C_T20HNH_A008644_20170216T140238", "MEAN_INCIDENCE_ZENITH_ANGLE_B11": 7.79661591458585, "MEAN_INCIDENCE_ZENITH_ANGLE_B10": 7.74858088008228, "MEAN_INCIDENCE_ZENITH_ANGLE_B12": 7.87202797543515, "system:time_end": 1487253758304, "CLOUD_COVERAGE_ASSESSMENT": 46.001, "SENSOR_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B8A": 7.88091971370358, "MEAN_INCIDENCE_ZENITH_ANGLE_B6": 7.82478554723881, "PRODUCT_URI": "S2A_MSIL1C_20170216T140051_N0204_R067_T20HNH_20170216T140238.SAFE", "SOLAR_IRRADIANCE_B12": 85.25, "SOLAR_IRRADIANCE_B11": 245.59, "SOLAR_IRRADIANCE_B10": 367.15, "MGRS_TILE": "20HNH", "system:asset_size": 1175517360, "MEAN_INCIDENCE_ZENITH_ANGLE_B5": 7.79900116247969, "MEAN_INCIDENCE_ZENITH_ANGLE_B4": 7.77607041217818, "MEAN_INCIDENCE_ZENITH_ANGLE_B7": 7.85318622478758, "GEOMETRIC_QUALITY_FLAG": "PASSED", "MEAN_INCIDENCE_ZENITH_ANGLE_B1": 7.91471610436124, "MEAN_INCIDENCE_ZENITH_ANGLE_B3": 7.7369473590697, "MEAN_INCIDENCE_ZENITH_ANGLE_B2": 7.70975907970575, "system:index": "20170216T140051_20170216T140238_T20HNH", "DATATAKE_IDENTIFIER": "GS2A_20170216T140051_008644_N02.04", "MEAN_INCIDENCE_ZENITH_ANGLE_B9": 7.95168886480531, "DATASTRIP_ID": "S2A_OPER_MSI_L1C_DS_SGS__20170216T185746_S20170216T140238_N02.04", "SOLAR_IRRADIANCE_B8": 1036.39, "GENERATION_TIME": 1487253758000, "MEAN_SOLAR_ZENITH_ANGLE": 37.0241680190277, "PRODUCTION_DEM_TYPE": "S2__OPER_DEM_GLOBEF_PDMC_19800101T000000_S19800101T000000", "MEAN_INCIDENCE_AZIMUTH_ANGLE_B8": 101.311911153513, "DEGRADED_MSI_DATA_PERCENTAGE": 0.0, "system:time_start": 1487253758304, "PROCESSING_BASELINE": "02.04"}, "type": "Image"}], "bands": [], "version": 1487648348026000, "type": "ImageCollection"}}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<LocalDate> parseAssetsData(GenericJson content){//data.get("features")
		List<LocalDate> assets = new ArrayList<LocalDate>();
		if(content == null)return assets;
		//	System.out.println("productFinderv5 content = "+content);

		//@SuppressWarnings("unchecked")
		//ArrayMap<String,Object> data = ((ArrayMap<String, Object>) content;
		Object features = content.get(DATA);
		//	System.out.println("features:"+features);

		if(features instanceof List){
			int i=0;
			for(ArrayMap<String,Object> feature:(List<ArrayMap<String,Object>>)features ){
				i++;
				updateProgress(i, ((List<?>)features).size());
				if(feature instanceof Map){
					String assetString = (String)((Map<?,?>)feature).get(ID);
					//String dateString = (String)((Map<?,?>)feature).get("date");
					//System.out.println("dateString "+dateString);//dateString 2018-03-08

					LocalDate assetDate = getAssetDate(assetString);

					//System.out.println("dateString "+dateString);//dateString 2018-03-08

					/*
					 * "metadata": { "meanNDVI": '+str( meanNDVI)+', "porcNubes": '+str( cpp)+'}}'
					 */
					BigDecimal porcNubes=new BigDecimal(0);
					try {
						Map<?,?> metadata = (Map<?,?>)((Map<?,?>)feature).get("metadata");
						porcNubes = (BigDecimal)metadata.get("porcNubes");
						System.out.println("porcNubes "+metadata.get("porcNubes"));//dateString 2018-03-08
					}catch(Exception e) {
						e.printStackTrace();
					}
					

					if(!assets.contains(assetDate)&&porcNubes.doubleValue()<90){
						assets.add(assetDate);
					}
				}
			}
		} else{
			System.out.println("content no es list");
		}

		return assets;
	}

	@SuppressWarnings("unchecked")
	private  List<File> parseNDVIResponse(HttpResponse response) throws IOException {
		List<File> files = new ArrayList<File>();
		//String s = response.parseAsString();		
		//System.out.println("ndvi content as string"+ s);//XXX ndvi content {"data":[]}

		GenericJson content = response.parseAs(GenericJson.class);
		if(content == null)return files;
		System.out.println("ndvi content "+ content);//XXX ndvi content {"data":[]}
		Object features = content.get(DATA);

		if(features == null)return files;

		if(features instanceof List){
			//int i=0;
			for(ArrayMap<String,String> feature:(List<ArrayMap<String,String>>)features){
				//ArrayMap<String,Object> values = (ArrayMap<String,Object>) data.get(key);
				BigDecimal porcNubes=new BigDecimal(0);
				try {
					Map<?,?> metadata = (Map<?,?>)((Map<?,?>)feature).get("metadata");
					porcNubes = (BigDecimal)metadata.get("porcNubes");
					System.out.println("porcNubes "+metadata.get("porcNubes"));//dateString 2018-03-08
					
					
					Object meanObject = metadata.get("meanNDVI");
					String mean = "0.0";
					if(meanObject instanceof BigDecimal) {
						BigDecimal meanNDVI = (BigDecimal)meanObject;
						mean = bdf.format(meanNDVI);
					} else {
						System.out.println("no se de que clase es meanNDVI "+meanObject.getClass().getCanonicalName());
					}
					
				}catch(Exception e) {
					e.printStackTrace();
				}

				String path2 = feature.get(PATH2);
				if(path2!=""&&porcNubes.doubleValue()<90){
					System.out.println("path2 "+path2);

					File tiffFile = downloadGoogleTifFile(path2);

					//esto no funciona porque los nombres duplicados se le agrega un numero.
					//	String filePath2 = filePath.substring(0,dotIndex)+"("+i+")"+filePath.substring(dotIndex);
					if(tiffFile!=null) {
						//					String baseName = tiffFile.getName().split("\\(")[0];
						//					if(!files.stream().anyMatch(f->f.getName().startsWith(baseName))) {

						files.add(tiffFile);
						//observableList.add(tiffFile);
						//					} else {						
						//						System.out.println(tiffFile +" ya existe en la lista de files!");
						//					}
					}
				}else {//path2 es ""
					System.out.println("path1 "+feature.get("path1"));
				}
				//updateProgress(observableList.size(), data.size());
			}}
		//System.out.println(data);
		//"path2": "https://earthengine.googleapis.com/api/download?docid=0d32b479051b60a92f40ade16f8bacf4&token=1677640d60ae8b508e17d6089fa091dd"
		//es un zip con el tif a dentro. hay que descargarlo, descomprimirlo y mostrarlo :)
		return files;

	}

	public static LocalDate getAssetDate(String assetID){
		String assetDateString=assetID;
		if(assetID.contains(COPERNICUS_S2)){
			assetID=assetID.replace(COPERNICUS_S2, "");
			assetDateString=assetID.substring(0, "20170328".length());
		} else	if(assetID.contains("LANDSATLC08C01T1_TOALC08")){//LANDSATLC08C01T1_TOALC08_227083_20180923
			//System.out.println("obteniendo la fecha para un asset de landsat "+assetID);//obteniendo la fecha para un asset de landsat LANDSATLC08C01T1_TOALC08_227083_20180923
			//Margarita Trigo 1819   LANDSATLC08C01T1_TOALC08_XXXXXX_20180923.nd(2)
			//LANDSATLC08C01T1_TOALC08_XXXXXX_
			//assetID=assetID.replace("LANDSATLC08C01T1_TOALC08_XXXXXX_", "");
			assetID=assetID.substring("LANDSATLC08C01T1_TOALC08_XXXXXX_".length(), assetID.length());
			assetDateString=assetID.substring(0, "20170328".length());
			//assetDateString=assetID.substring(assetID.length()-"20170328".length(), assetID.length());
		}
		DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate assetDate = null;
		//assetDateString = format1.format(assetDate);
		//System.out.println("now se formatea a: "+assetDateString);
		try{
			assetDate =format1.parse(assetDateString,LocalDate::from);
		}catch(Exception e){
			e.printStackTrace();// Text '"2018030' could not be parsed at index 0
		}
		//System.out.println("devolviendo el assetDate "+assetDate);
		return assetDate;
	}


	private List<File> getNdviTiffFiles(Object labor){
		updateProgress(0, 3);
		//List<File> tiffFiles = new ArrayList<File>();
		String polygons = getPolygonsFromLabor(labor);		
		List<LocalDate> uniqueDates = getSentinellAssets(labor);//assents tiene la forma ["COPERNICUS/S2/20161221T141042_20161221T142209_T20HLG","COPERNICUS/S2/20161221T141042_20161221T142209_T20HLG"]
		updateProgress(0, uniqueDates.size());
		//String subAssets = assets.substring(1,assets.length()-1);//elimino los corchetes
		//List<String> idsList =  Arrays.asList(subAssets.split(","));
		//List<LocalDate> uniqueDates =idsList.stream().map(GetNDVI2ForLaborTask::getAssetDate).distinct().collect(Collectors.toList());

		System.out.println("procesando los dates unicos "+uniqueDates);
		DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");	

		//IntegerProperty i=new SimpleIntegerProperty(0);
		List<LocalDate> processed = Collections.synchronizedList(new ArrayList<LocalDate>());
		List<File>  resFiles = uniqueDates.stream().collect( ()->new  ArrayList<File>(),
				(tiffFiles, assetDate) ->{
					String sEnd = format1.format(assetDate.plusDays(1));
					String sBegin = format1.format(assetDate.minusDays(1));
					System.out.println("buscando los ndvi entre "+sBegin+" y "+sEnd);

					GenericUrl url = new GenericUrl(HTTP_GEE_API_HELPER_HEROKUAPP_COM_NDVI_V3);

					Map<String, String> req_data = new HashMap<String, String>();
					req_data.put(GEE_POLYGONS_GET_REQUEST_KEY, polygons);
					req_data.put(BEGIN, sBegin);
					req_data.put(END, sEnd);
					req_data.put(TOKEN, URSULA_GIS_TOKEN);

					final HttpContent req_content = new JsonHttpContent(new JacksonFactory(), req_data);

					/*
					assets=["COPERNICUS/S2/20161221T141042_20161221T142209_T20HLG"]
					polygons=[[[[-64.69101905822754,-34.860017354204885],[-64.69058990478516,-34.86705989785682],[-64.67016220092773,-34.86515847050267],[-64.67265129089355,-34.86198932721536]]]]
					 */
					System.out.println("calling url: "+url);
					//for(int i=0;i<3;i++) {
					HttpResponse response = makePostRequest(url,req_content);


					try {
						List<File> tiffResponse = parseNDVIResponse(response);

						response.disconnect();
						if(tiffResponse.size()>0) {
							//observableList.addAll(tiffFiles);
							//tiffFiles.addAll(observableList);

							observableList.addAll(tiffResponse);//agrego a la lista de observables para que se vayan mostrando
							tiffFiles.addAll(tiffResponse);//agrego a la coleccion final
							processed.add(assetDate);
							updateProgress(processed.size(), uniqueDates.size());

							//		break;//salgo del for retry
						}else {//retry
							System.out.println("no hay files para agregar");
							// no descargo porque estaba nublada
							//Thread.sleep(1000);//esperar a seguir operando
						}

					} catch (IOException e) {
						e.printStackTrace();

					} 
					//	}

				},
				(list1, list2) -> list1.addAll(list2)
				);
		//		
		//		for(LocalDate assetDate : uniqueDates){
		//			updateProgress(i, uniqueDates.size());i++;
		//			
		//			String sEnd = format1.format(assetDate.plusDays(1));
		//			String sBegin = format1.format(assetDate.minusDays(1));
		//			System.out.println("buscando los ndvi entre "+sBegin+" y "+sEnd);
		//			
		//			GenericUrl url = new GenericUrl(HTTP_GEE_API_HELPER_HEROKUAPP_COM_NDVI_V3);
		//			
		//			Map<String, String> req_data = new HashMap<String, String>();
		//			req_data.put(GEE_POLYGONS_GET_REQUEST_KEY, polygons);
		//			req_data.put(BEGIN, sBegin);
		//			req_data.put(END, sEnd);
		//			req_data.put("token", "ursulaGISv23");
		//
		//			final HttpContent req_content = new JsonHttpContent(new JacksonFactory(), req_data);
		//
		//			/*
		//			assets=["COPERNICUS/S2/20161221T141042_20161221T142209_T20HLG"]
		//			polygons=[[[[-64.69101905822754,-34.860017354204885],[-64.69058990478516,-34.86705989785682],[-64.67016220092773,-34.86515847050267],[-64.67265129089355,-34.86198932721536]]]]
		//			 */
		//			//System.out.println("calling url: "+url);
		//			HttpResponse response = makePostRequest(url,req_content);
		//
		//			try {
		//				tiffFiles.addAll(parseNDVIResponse(response));
		//				response.disconnect();
		//			} catch (IOException e) {
		//				e.printStackTrace();
		//				return null;
		//			}		
		//		}//fin del for
		return resFiles;
	}

	private String getPolygonsFromLabor(Object labor) {
		if(labor instanceof Labor){
			Labor<?> c = (Labor<?>)labor;
			//ReferencedEnvelope bounds = c.outCollection.getBounds();
			double witdh = c.maxX.longitude.degrees-c.minX.longitude.degrees;
			double height = c.maxY.latitude.degrees-c.minY.latitude.degrees;
			double bufferX=witdh*0.05;
			double bufferY=height*0.05;

			StringBuilder sb = new StringBuilder();
			sb.append("[[[");
			sb.append("["+(c.minX.longitude.degrees-bufferX)+","+c.minX.latitude.degrees+"],");
			sb.append("["+c.minY.longitude.degrees+","+(c.minY.latitude.degrees-bufferY)+"],");
			sb.append("["+(c.maxX.longitude.degrees+bufferX)+","+c.maxX.latitude.degrees+"],");
			sb.append("["+c.maxY.longitude.degrees+","+(c.maxY.latitude.degrees+bufferY)+"]");
			sb.append("]]]");
			String polygons=sb.toString();
			return polygons;
		} else if(labor instanceof Poligono){
			List<? extends Position> positions = ((Poligono)labor).getPositions();

			StringBuilder sb = new StringBuilder();
			sb.append("[[[");
			for(Position p:positions){	
				Angle lon= p.getLongitude();
				Angle lat = p.getLatitude();
				sb.append("["+lon.degrees+","+lat.degrees+"],");
			}
			sb.deleteCharAt(sb.length()-1);

			sb.append("]]]");
			String polygons=sb.toString();
			return polygons;
		}
		return null;
	}

	/**
	 * metodo que toma la url path2 y descarga el zip con el tiff finalmente
	 * @param path
	 * @return
	 */
	public  File downloadGoogleTifFile(String path){
		int tries = 0;
		while(tries<5){
			try{
				List<String> filePaths=null;
				GenericUrl url = new GenericUrl(path);
				HttpResponse response = makeGetRequest(url);
				if(response == null){
					System.err.println("no se pudo descargar el archivo de google... reintentando");
					continue;//retry
				}
				try {
					InputStream is = response.getContent();
					filePaths = UnzipUtility.unzip(is, downloadDir.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
				List<File> tifFiles = filePaths.stream().map((s)->new File(s)).filter((f)->f.getName().endsWith(".tif")).collect(Collectors.toList());
				return tifFiles.get(0);
			}catch(Exception e){
				System.err.println("no se pudo descargar el archivo de google... reintentando");
				e.printStackTrace();
			}
			try {
				Thread.sleep(tries*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tries++;
		}
		System.err.println("saliendo despues de intentar 5 veces sin exito");
		return null;

	}

	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private HttpResponse makePostRequest(GenericUrl url,HttpContent req_content){
		HttpResponse response = null;

		//		 HttpTransport transport = GoogleTransport.create();
		//	        // add default headers
		//	        GoogleHeaders defaultHeaders = new GoogleHeaders();
		//	        transport.defaultHeaders = defaultHeaders;
		//	        transport.defaultHeaders.put("Content-Type", "application/json");
		//	        transport.addParser(new JsonHttpParser());

		//java.net.SocketException: Address family not supported by protocol family: connect
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);


					}
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);//(url);
			response= request.execute();

		} catch (Exception e) {
			//	System.out.println("no se pudo ejecutar el request "+builtUrl);
			//			Platform.runLater(()->{
			//				Alert a = new Alert(AlertType.INFORMATION);
			//				a.setTitle("Info");
			//				a.setHeaderText("Hubo un problema con la descarga");
			//				a.setContentText("Puede ser que no haya imagenes para el periodo o que el poligono sea demasiado grande. Pruebe con otra fecha o un poligono mas simple.");
			//				a.show();
			//				uninstallProgressBar();
			//			});

			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
		}	
		return response;
	}

	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private HttpResponse makeGetRequest(GenericUrl url){
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);

					}
				});

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			response= request.execute();
		} catch (Exception e) {
			//	System.out.println("no se pudo ejecutar el request "+builtUrl);
			//			Platform.runLater(()->{
			//				Alert a = new Alert(AlertType.INFORMATION);
			//				a.setTitle("Info");
			//				a.setHeaderText("Hubo un problema con la descarga");
			//				a.setContentText("Puede ser que no haya imagenes para el periodo o que el poligono sea demasiado grande. Pruebe con otra fecha o un poligono mas simple.");
			//				a.show();
			//				uninstallProgressBar();
			//			});
			System.err.println("Fallo el getUrl "+url);
			e.printStackTrace();

			return null;
		}	
		return response;
	}


	public List<File> call() {	
		return	getNdviTiffFiles(placementObject);
	}

	public void setFinDate(LocalDate date) {
		this.end=date;

	}

	public void setBeginDate(LocalDate initialDate) {
		this.begin=initialDate;

	}

	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("NDVI2 para "+this.end);
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(MMG_GUI_EVENT_CLOSE_PNG));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}
	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}


}
