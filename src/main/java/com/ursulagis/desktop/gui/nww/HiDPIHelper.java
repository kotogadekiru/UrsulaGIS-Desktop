package com.ursulagis.desktop.gui.nww;

//import com.sun.prism.impl.PrismSettings;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class HiDPIHelper {
	private static Boolean isHiDPI=null;
	public static boolean isHiDPI(){
		return false;
		//PrismSettings.allowHiDPIScaling;
//		
//		if(isHiDPI==null){
//		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
//		System.out.println("screen "+primaryScreenBounds);
//		//width=1536.0, height=824.0]
//		isHiDPI = new Boolean( primaryScreenBounds.getWidth()>1000);
//		}
//		return isHiDPI;
	}
}
