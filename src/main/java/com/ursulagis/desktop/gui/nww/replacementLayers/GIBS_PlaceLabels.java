package com.ursulagis.desktop.gui.nww.replacementLayers;

/**
 * 
 */

import org.w3c.dom.Document;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;


public class GIBS_PlaceLabels extends WMSTiledImageLayer
{
  static AVListImpl crs;
  static {
    crs = new AVListImpl();
    crs.setValue(AVKey.COORDINATE_SYSTEM, "EPSG:4326");
  }
  public GIBS_PlaceLabels()
  {
      super(getConfigurationDocument(), crs);
  }
  
  protected static Document getConfigurationDocument()
  {
	  
	  return WWXML.openDocumentFile("com/ursulagis/desktop/gui/nww/replacementLayers/GIBS_PlaceLabels.xml", null);
      //return WWXML.openDocumentFile("config/earth/EOX_Sentinel2Cloudless2016.xml", null);
  }
}
