package com.ursulagis.desktop.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;



import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.ogc.kml.KMLConstants;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import gov.nasa.worldwindx.examples.kml.KMZDocumentBuilder;
import com.ursulagis.desktop.gui.nww.LayerPanel;
import javafx.stage.FileChooser;
import com.ursulagis.desktop.utils.FileHelper;

public class ExportNDVIToKMZ {
	private WorldWindow wwd;
	private LayerPanel layerPanel;
	private static final String YYYY_MM_DD = "yyyy-MM-dd";
	DateTimeFormatter format1 = DateTimeFormatter.ofPattern(YYYY_MM_DD);

	/**
	 * clase que toma todos los ndvi cargados y los exporta en un unico excel
	 * @param _wwd
	 * @param _lP
	 */
	public ExportNDVIToKMZ(WorldWindow _wwd,LayerPanel _lP) {
		this.wwd=_wwd;
		this.layerPanel=_lP;
	}

	

	
	public void exportToKMZ(Ndvi ndvi){
		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
		//	executorPool.execute(()->{
		
			ExportableAnalyticSurface surface = ndvi.getSurfaceLayer();
	
			
			//FileChooser chooser = new FileChooser();
			
			File selected =FileHelper.getNewFile(ndvi.getNombre()+".kml","kml");
			if(selected!=null) {
				surface.setExportImageName(selected.getName()+".png");
				surface.setExportImagePath(selected.getParent());
				
				
				
				OutputStream outStream;
				try {
					outStream = new FileOutputStream(selected);
						
				
				
				  /**
			     * Export's this surface's color values as a KML GroundOverlay. Only this surface's color values, outline color and
			     * outline width are used to create the ground overly image. The image is exported as clamp-to-ground. The following
			     * fields of this surface must be set prior to calling this method: exportImagePath and exportImageName. Optionally
			     * the exportImageWidth and exportImageHeight fields may be set to indicate the size of the exported ground overlay
			     * image. These values are both 1024 by default. The surface's opacity attributes are ignored, but any opacity
			     * values specified in the surface's color values are captured in the ground overlay image.
			     * <p>
			     * If color values have not been specified for the surface then the interior if the output image is blank. The image
			     * outline is exported only if the surface's drawOutline file is true.
			     *
			     * @param mimeType Desired export format. Only "application/vnd.google-earth.kml+xml" is supported.
			     * @param output   Object that will receive the exported data. The type of this object depends on the export format.
			     *                 All formats should support {@code java.io.OutputStream}. Text based format (for example, XML
			     *                 formats) should also support {@code java.io.Writer}. Certain formats may also support other
			     *                 object types.
			     *
			     * @throws java.io.IOException if an error occurs while writing the output file or its image.
			     * @see #setExportImageName(String)
			     * @see #setExportImagePath(String)
			     * @see #setExportImageWidth(int)
			     * @see #setExportImageHeight(int)
			     */
			    surface.export("application/vnd.google-earth.kml+xml", outStream);
			    outStream.flush();
			    outStream.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	 catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//KMZDocumentBuilder kmzB = new KMZDocumentBuilder(outStream);
				//kmzB.writeObject(surface);
				//kmzB.close();
			//	Object writer = XMLOutputFactory.newInstance().createXMLStreamWriter(this.zipStream);
			//	surface.export(KMLConstants.KML_MIME_TYPE, writer);//mimeType, output);

				
				
				
//				surface.setExportImageName(selected.getName());
//				surface.setExportImagePath(selected.getParent());
//				
//				OutputStream outStream = new FileOutputStream(selected);					
//				KMZDocumentBuilder kmzB = new KMZDocumentBuilder(outStream);
//				kmzB.writeObject(surface);
//				kmzB.close();
//				outStream.close();
			}



	}
}
