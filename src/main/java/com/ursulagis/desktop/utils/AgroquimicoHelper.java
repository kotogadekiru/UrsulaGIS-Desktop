package com.ursulagis.desktop.utils;

import java.util.HashMap;
import java.util.Map;

import com.ursulagis.desktop.dao.config.Agroquimico;

public class AgroquimicoHelper {
	public static Map<String,Agroquimico> getAgroquimicosDefault(){
		HashMap<String,Agroquimico> agroquimicos = new HashMap<String,Agroquimico>();
		agroquimicos.put("RoundUp(lts)",new Agroquimico("RoundUp(lts)"));	
		agroquimicos.put("Superwet(lts)",new Agroquimico("Superwet(lts)"));
		agroquimicos.put("Atrazina(lts)",new Agroquimico("Atrazina(lts)"));
		agroquimicos.put("Cletodim(lts)",new Agroquimico("Cletodim(lts)"));
		
		agroquimicos.put("Rizospray extremo(lts)",new Agroquimico("Rizospray extremo(lts)"));
		agroquimicos.put("Benazolin(lts)",new Agroquimico("Benazolin(lts)"));
		agroquimicos.put("Fomesafen(lts)",new Agroquimico("Fomesafen(lts)"));
		agroquimicos.put("Glifosato 66%(lts)",new Agroquimico("Glifosato 66%(lts)"));
		agroquimicos.put("Dinotefuran(lts)",new Agroquimico("Dinotefuran(lts)"));
		agroquimicos.put("Abamectina 1,8(lts)",new Agroquimico("Abamectina 1,8(lts)"));
		agroquimicos.put("Coragen(lts)",new Agroquimico("Coragen(lts)"));
		agroquimicos.put("Opera(lts)",new Agroquimico("Opera(lts)"));		
		agroquimicos.put("Haloxifop 90% (Galant max)(lts)",new Agroquimico("Haloxifop 90% (Galant max)(lts)"));	
		return agroquimicos;
	}	

}
