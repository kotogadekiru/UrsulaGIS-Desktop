package com.ursulagis.desktop.tests;

import java.util.Map;

import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.Suelo.SueloParametro;
import com.ursulagis.desktop.dao.suelo.SueloItem;

public class SueloTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SueloItem item = new SueloItem();
		item.setDensAp(1400.0);
		item.setPpmNO3(60.0);
		Double kgN = Suelo.getKgNHa(item);
		System.out.println("para 60ppm de NO3 me da "+kgN+"kgN/Ha");
		//para 60ppm de NO3 me da 113.8536 con 1400kg/m3
		//para 60ppm de NO3 me da 97.58879999999999 con 1200kg/m3
		Map<SueloParametro, Double> nutrientesSuelo = Suelo.getKgNutrientes(item);
		Double kgNHa = nutrientesSuelo.get(SueloParametro.Nitrogeno);
		System.out.println("kgNHa es "+kgNHa);
	}
}
