package com.ursulagis.desktop.gui.nww.replacementLayers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

import gov.nasa.worldwind.avlist.AVKey;



public class GoogleLayer extends BasicMercatorTiledImageLayer {
	 private static long lastReq=0;
    public enum Type {
        ROADMAP("Google road map", "m", ".png", false),
        //ROADMAP2("Google road map 2", "r", ".png", false),
        TERRAIN("Google map w/ terrain", "p", ".png",false),
        //TERRAIN_ONLY("Google terrain only", "t", ".png", false),
        //HYBRID("Google hybrid", "y", ".png", false),
        SATELLITE("Google satellite", "s", ".jpg",false),
        ROADS("Google roads", "h", ".png", true),
        TRAFFIC("Google traffic", "h,traffic&style=15", ".png",true),
        ;

        Type(String name, String lyrs, String suffix, boolean overlay) {
            this.name = name;
            this.lyrs = lyrs;
            this.suffix = suffix;
            this.overlay = overlay;
        }

        private final String name;
        private final String lyrs;
        private final String suffix;
        private final boolean overlay;

    }

    public GoogleLayer(Type type) {
        super(type.name, 22, 256, type.overlay, type.suffix, new URLBuilder(type.lyrs).setFirstLevelOffset(3));
    	//this.setName("GoogleLayer");
    	 //this.setValue(AVKey.DATA_CACHE_NAME,"GoogleLayer");
    }

    private static class URLBuilder extends MercatorTileUrlBuilder {
        private String lyrs;
       

        URLBuilder(String lyrs) {
            this.lyrs = lyrs;
        }

        @Override
        protected URL getMercatorURL(int x, int y, int z) throws MalformedURLException {
        	//http://b.andy.sandbox.cloudmade.com/tiles/cycle/
        	//https://maps.googleapis.com/maps/api/staticmap?center=Berkeley,CA&zoom=14&size=256x256&key=yourkey
        	long now = System.currentTimeMillis();	
        	long elapsed = now-GoogleLayer.lastReq;
        	if(elapsed>1000) {
        		GoogleLayer.lastReq=now;
                
        		URI uri = URI.create("https://mt.google.com/vt/lyrs="+lyrs+"&x="+x+"&y="+y+"&z="+z+"&hl="+Locale.getDefault().getLanguage());
        		//System.out.println("elapsed= "+elapsed +" downloading url "+url);        	
        		return uri.toURL();
        	}
        	return null;
        }
    }

}