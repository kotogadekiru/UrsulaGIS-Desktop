package com.ursulagis.desktop.dao.siembra;

import java.util.function.ToDoubleFunction;

import com.ursulagis.desktop.utils.ProyectionConstants;

public class SiembraHelper {
	public static final ToDoubleFunction<SiembraItem> getSemillaCantMethod() {
		ToDoubleFunction<SiembraItem> getCantSemilla=new ToDoubleFunction<SiembraItem>() {
			@Override
			public double applyAsDouble(SiembraItem item) {
				Double has = ProyectionConstants.A_HAS(item.getGeometry().getArea());
				Double cant = item.getDosisHa();
				return has*cant;
			}			    	
		};
		return getCantSemilla;
	}
	
	public static ToDoubleFunction<SiembraItem> getFertLCantMethod() {
		ToDoubleFunction<SiembraItem> getCantFertL=new ToDoubleFunction<SiembraItem>() {
			@Override
			public double applyAsDouble(SiembraItem item) {
				Double has = ProyectionConstants.A_HAS(item.getGeometry().getArea());
				Double cant = item.getDosisFertLinea();
				System.out.println("getFertLCantMethod="+has*cant);
				return has*cant;
			}			    	
		};
		return getCantFertL;
	}
	
	public static ToDoubleFunction<SiembraItem> getFertCCantMethod() {
		ToDoubleFunction<SiembraItem> getCantFertC=new ToDoubleFunction<SiembraItem>() {
			@Override
			public double applyAsDouble(SiembraItem item) {
				Double has = ProyectionConstants.A_HAS(item.getGeometry().getArea());
				Double cant = item.getDosisFertCostado();
				return has*cant;
			}			    	
		};
		return getCantFertC;
	}
}
