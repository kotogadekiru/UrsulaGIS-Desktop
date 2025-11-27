package com.ursulagis.desktop.gui.nww;

import gov.nasa.worldwind.globes.EllipsoidalGlobe;
import gov.nasa.worldwind.terrain.ZeroElevationModel;

public class ZeroElevationsEarth extends EllipsoidalGlobe {
    public static final double WGS84_EQUATORIAL_RADIUS = 6378137.0;
    public static final double WGS84_POLAR_RADIUS = 6356752.3;
    public static final double WGS84_ES = 0.00669437999013;
    public static final double ELEVATION_MIN = -11000.0;
    public static final double ELEVATION_MAX = 8500.0;
 
    public ZeroElevationsEarth() {
       super(6378137.0, 6356752.3, 0.00669437999013,new ZeroElevationModel());
    }
 
    public String toString() {
       return "Earth";
    }
 }