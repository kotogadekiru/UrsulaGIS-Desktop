package tasks;

public class ProyectionConstants {
	

	public static final int RADIO_TERRESTRE_METROS = 6378137;//6378400;// 6371000;//Radio
	public static final double METROS2_POR_HA = 10000;
	public static double metersToLongLat = 360 / (2 * Math.PI * RADIO_TERRESTRE_METROS);// para
	//getArea() - area returned in the same units as the coordinates (be careful of lat/lon data!)
	public static final double A_HAS =1/(metersToLongLat*metersToLongLat*METROS2_POR_HA);// 1000000;//*1.24;
}
