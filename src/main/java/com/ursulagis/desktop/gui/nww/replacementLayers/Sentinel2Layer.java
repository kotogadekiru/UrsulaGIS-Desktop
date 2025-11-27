package com.ursulagis.desktop.gui.nww.replacementLayers;

/**
 * 
 */

import org.w3c.dom.Document;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;


public class Sentinel2Layer extends WMSTiledImageLayer
{
  static AVListImpl crs;
  static {
    crs = new AVListImpl();
    crs.setValue(AVKey.COORDINATE_SYSTEM, "EPSG:4326");
  }
  public Sentinel2Layer() {
      super(getConfigurationDocument(), crs);
      this.setName("Sentinel-2 2016");
  }
  
  protected static Document getConfigurationDocument()
  {
	
	  return WWXML.openDocumentFile("com/ursulagis/desktop/gui/nww/replacementLayers/EOX_Sentinel2Cloudless2016.xml", null);
      //return WWXML.openDocumentFile("config/earth/EOX_Sentinel2Cloudless2016.xml", null);
  }
}
