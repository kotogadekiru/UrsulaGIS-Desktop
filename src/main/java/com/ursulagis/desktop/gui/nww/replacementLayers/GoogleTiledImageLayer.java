/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package com.ursulagis.desktop.gui.nww.replacementLayers;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.mercator.*;
import gov.nasa.worldwind.util.*;

import java.net.*;

/**
 * @version $Id: OSMCycleMapLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GoogleTiledImageLayer extends BasicMercatorTiledImageLayer
{
	public GoogleTiledImageLayer()
	{
		super(makeLevels());
    }

	private static LevelSet makeLevels(){
		AVList params = new AVListImpl();

		params.setValue(AVKey.TILE_WIDTH, 256);
		params.setValue(AVKey.TILE_HEIGHT, 256);
		params.setValue(AVKey.DATA_CACHE_NAME, "Earth/Google");
		//params.setValue(AVKey.SERVICE, "http://b.andy.sandbox.cloudmade.com/tiles/cycle/");
		params.setValue(AVKey.DATASET_NAME, "satellite");
		params.setValue(AVKey.FORMAT_SUFFIX, ".png");
		params.setValue(AVKey.NUM_LEVELS, 20);//1 a 20
		params.setValue(AVKey.NUM_EMPTY_LEVELS,0);//                              22.5d                     45d
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
		params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0,	Angle.NEG180, Angle.POS180));
		params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());

		return new LevelSet(params);
	}

	private static class URLBuilder implements TileUrlBuilder
	{
		public URL getURL(Tile tile, String imageFormat)
				throws MalformedURLException
		{
			LatLon centroid = tile.getSector().getCentroid();
	    	
	     //   return getURL(centroid.longitude.degrees, centroid.latitude.degrees, tile.getLevelNumber() + firstLevelOffset);
			//"https://maps.googleapis.com/maps/api/staticmap?
	        //center=Berkeley,CA&zoom=14&size=400x400&key=AIzaSyAjaTBN4kTCJ7TZCzDZ15Eqs7NdYexoGRA"
			int size= 256;///tile.getLevelNumber();
			URL url = new URL("https://maps.googleapis.com/maps/api/staticmap?"
					+ "center="+centroid.latitude.degrees+","+centroid.longitude.degrees
					+"&zoom="+ (tile.getLevelNumber()+3)
					+"&size="+size+"x"+size
					+"&maptype=hybrid"//satellite terrain roadmap hybrid
					+"&key="+ "[YOUR KEY]");
			//			URL url = new URL(tile.getLevel().getService()
//					+ (tile.getLevelNumber() + 3)
//					+"/"+ tile.getColumn()
//					+"/"+ ((1 << (tile.getLevelNumber()) + 3) - 1 - tile.getRow()) + ".png");
			System.out.println("calling url "+url);

			return url;		}
	}

	@Override
	public String toString()
	{
		return "GoogleTiledImageLayer";
	}
}
