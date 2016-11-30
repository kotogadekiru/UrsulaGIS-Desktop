package mmg.gui.test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.data.ows.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.MapContext;
import org.geotools.map.WMSLayer;
import org.geotools.ows.ServiceException;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.wms.WMSChooser;
import org.geotools.swing.wms.WMSLayerChooser;
/*
 row/path landsat la union 227/83 227/84
 "CTRLAT" <-30 and 
 "CTRLAT" > -34 and 
 "CTRLON" < -60 and  
 "CTRLON" >-62 and  "MODE_" ='D'
 */
public class WMSL8Layer {
	public WMSL8Layer(){
		URL url;
		try {
			//http://earthexplorer.usgs.gov/cgi-bin/landsat_8?sceneid=LC82280852016076LGN00&amp;srs=EPSG:4326&amp;bbox=-65.320416,-37.313309,-62.215464,-34.6669465&
			/*
			 WMS requests can be placed for the following datasets:
				EO-1 ALI GLS2005 L4-5 TM L7 ETM+ SLC-on (1999-2003)
				EO-1 Hyperion GLS2000 L1-5 MSS L7 ETM+ SLC-off (2003-present) 
			 */
			//http://earthexplorer.usgs.gov/wms/custom/0101504235741?
			//url = new URL("http://atlas.gc.ca/cgi-bin/atlaswms_en?VERSION=1.1.1&Request=GetCapabilities&Service=WMS");
			//url = new URL("http://landsatlook.usgs.gov/arcgis/services/LandsatLook/ImageServer/WMSServer?request=GetCapabilities&service=WMS");
			//http://landsat2.arcgis.com/arcgis/rest/services/Landsat8_Views/ImageServer
			//url = new URL("http://map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?SERVICE=WMTS&request=GetCapabilities");
			url = new URL("http://map1.vis.earthdata.nasa.gov/wmts-geo/wmts.cgi?SERVICE=WMTS&request=GetCapabilities");
			//http://map1.vis.earthdata.nasa.gov/twms-geo/twms.cgi
			url = new URL("http://map1.vis.earthdata.nasa.gov/twms-geo/twms.cgi");
			WebMapServer wms = new WebMapServer(url);
			WMSCapabilities capabilities = wms.getCapabilities();

			System.out.println(capabilities.toString());
			// gets all the layers in a flat list, in the order they appear in
			// the capabilities document (so the rootLayer is at index 0)
			List<Layer> layers = capabilities.getLayerList();
			System.out.println(layers);
			
			GetMapRequest request = wms.createGetMapRequest();
			request.setFormat("image/tiff");
			//request.setDimensions("583", "420"); //sets the dimensions to be returned from the server
			//request.setTransparent(true);
			//request.setSRS("EPSG:4326");
			//bbox=-65.320416,-37.313309,-62.215464,-34.6669465&
			request.setBBox("-65.320416,-37.313309,-62.215464,-34.6669465");
			request.addLayer(layers.get(0));

			GetMapResponse response = (GetMapResponse) wms.issueRequest(request);
			BufferedImage image = ImageIO.read(response.getInputStream());
			System.out.println(image);
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	
	public static void main(String[] args){
		
		  URL capabilitiesURL = WMSChooser.showChooseWMS();
	        if( capabilitiesURL == null ){
	            System.exit(0); // canceled
	        }
	        WebMapServer wms;
			try {
				wms = new WebMapServer( capabilitiesURL );
		
	        List<Layer> wmsLayers = WMSLayerChooser.showSelectLayer( wms );
	        if( wmsLayers == null ){
	            JOptionPane.showMessageDialog(null, "Could not connect - check url");
	            System.exit(0);
	        }
	
	        MapContext mapcontent = new MapContext();
	        mapcontent.setTitle( wms.getCapabilities().getService().getTitle() );
	        
	        for( Layer wmsLayer : wmsLayers ){
	        	WMSLayer displayLayer = new WMSLayer(wms, wmsLayer );
	            mapcontent.addLayer(displayLayer);
	        }
	        // Now display the map
	     
	        JMapFrame.showMap( mapcontent);
			} catch (ServiceException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}        
	        
		//new WMSL8Layer();
	}
}
