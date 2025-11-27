package com.ursulagis.desktop.utils;

import java.util.HashMap;
import java.util.Map;

import com.ursulagis.desktop.dao.config.Cultivo;

public class CultivoHelper {
	public static final String GIRASOL = "Girasol";
	public static final String SOJA = "Soja";
	public static final String TRIGO = "Trigo";
	public static final String MAIZ = "Maiz";
	public static final String SORGO = "Sorgo";
	public static final String CEBADA = "Cebada";
	
	public static Map<String,Cultivo> getCultivosDefault(){
		 Map<String,Cultivo> cultivos = new HashMap<String,Cultivo>();
							//String _nombre, Double _absP, Double _extP,Double rinde
				cultivos.put(MAIZ, getMaiz());
				cultivos.put(TRIGO,getTrigo());
				cultivos.put(SOJA, getSoja());
				cultivos.put(CEBADA,getCebada());
				cultivos.put(SORGO,getSorgo());
				cultivos.put(GIRASOL,getGirasol());
			return cultivos;
		}
	
	
//	-	(kg ton-1)	(%)
//	N	22	0.68
//	P	4	0.76
//	K	19	0.21
//	Ca	3	0.07
//	Mg	3	0.53
//	S	4	0.35
//	B	0.02	0.25
//	Cl	0.444	0.06
//	Cu	0.013	0.29
//	Fe	0.125	0.36
//	Mn	0.189	0.17
//	Mo	0.001	0.63
//	Zn	0.053	0.5
//	Ni	-	-
	private static Cultivo getMaiz(){
		Cultivo c = new Cultivo(MAIZ);
		c.setAbsN(22d);
		c.setAbsP(4d);
		c.setAbsK(19d);
		c.setAbsS(5d);
		
		c.setExtN(c.getAbsN()*0.68);
		c.setExtP(c.getAbsP()*0.76);
		c.setExtK(c.getAbsK()*0.21);
		c.setExtS(c.getAbsS()*0.35);
		
		c.setRindeEsperado(10d);
		c.setAbsAgua(1000/12d);
		c.setAporteMO(1000*(-1+c.getAbsN()/c.getExtN()));//kg por tn;  estimacion en base a la extraccion de n vs absorcion de n
		c.setEstival(true);// se puede usar el porcentaje de los grados dias de la campania que el cultivo esta activo
		c.setSemPorBolsa(80000.0);
		return c;
	}
	
//	NUTRIENTE	REQUERIMIENTO	IC
//	-	(kg ton-1)	(%)
//	N	30	0.66
//	P	4.4	0.82
//	K	20.8	0.19
//	Ca	-	-
//	Mg	4.5	0.29
//	S	3.75	0.57
//	B	-	-
//	Cl	-	-
//	Cu	-	-
//	Fe	-	-
//	Mn	-	-
//	Mo	-	-
//	Zn	-	-
//	Ni	-	-
	private static Cultivo getSorgo(){
		Cultivo c = new Cultivo(SORGO);
		c.setAbsN(30d);
		c.setAbsP(4.4d);
		c.setAbsK(20.8d);
		c.setAbsS(3.75d);
		
		c.setExtN(c.getAbsN()*0.66);
		c.setExtP(c.getAbsP()*0.82);
		c.setExtK(c.getAbsK()*0.19);
		c.setExtS(c.getAbsS()*0.57);
		c.setRindeEsperado(10d);
		c.setAbsAgua(1000/12d);
		c.setAporteMO(1000*(-1+c.getAbsN()/c.getExtN()));//kg por tn;  estimacion en base a la extraccion de n vs absorcion de n
		return c;
	}
	
//	N	30	0.69
//	P	5	0.8
//	K	19	0.21
//	Ca	3	0.14
//	Mg	4	0.63
//	S	5	0.34
//	B	0.025	0.5
//	Cl	-	-
//	Cu	0.01	0.75
//	Fe	0.137	0.99
//	Mn	0.07	0.17
//	Mo	-	-
//	Zn	0.052	0.5
//	Ni	-	-
	private static Cultivo getTrigo(){
		Cultivo c = new Cultivo(TRIGO);
		c.setAbsN(30d);
		c.setAbsP(5d);
		c.setAbsK(19d);
		c.setAbsS(5d);
		
		c.setExtN(c.getAbsN()*0.69);
		c.setExtP(c.getAbsP()*0.8);
		c.setExtK(c.getAbsK()*0.21);
		c.setExtS(c.getAbsS()*0.34);
		c.setRindeEsperado(4d);
		c.setAbsAgua(1000/12d);
		c.setEstival(false);
		c.setAporteMO(1000*(-1+c.getAbsN()/c.getExtN()));//kg por tn;  estimacion en base a la extraccion de n vs absorcion de n
		return c;
	}
	
//	NUTRIENTE	REQUERIMIENTO	IC
//	-	(kg ton-1)	(%)
//	N	26.3	0.68
//	P	4	0.76
//	K	19	0.21
//	Ca	19.7	0.07
//	Mg	-	-
//	S	4.15	0.48
//	B	-	-
//	Cl	-	-
//	Cu	-	-
//	Fe	-	-
//	Mn	-	-
//	Mo	-	-
//	Zn	-	-
//	Ni	-	-
	private static Cultivo getCebada(){
		Cultivo c = new Cultivo(CEBADA);
		c.setAbsN(26d);
		c.setAbsP(4d);
		c.setAbsK(19d);
		c.setAbsS(4.15d);
		
		c.setExtN(c.getAbsN()*0.68);
		c.setExtP(c.getAbsP()*0.76);
		c.setExtK(c.getAbsK()*0.21);
		c.setExtS(c.getAbsS()*0.48);
		c.setRindeEsperado(4d);
		c.setAbsAgua(1000/12d);
		c.setEstival(false);
		c.setAporteMO(1000*(-1+c.getAbsN()/c.getExtN()));//kg por tn;  estimacion en base a la extraccion de n vs absorcion de n
		return c;
	}
	
//	NUTRIENTE	REQUERIMIENTO	IC
//	-	(kg ton-1)	(%)
//	N	30	0.69
//	P	5	0.8
//	K	19	0.21
//	Ca	3	0.14
//	Mg	4	0.63
//	S	5	0.34
//	B	0.025	0.5
//	Cl	-	-
//	Cu	0.01	0.75
//	Fe	0.137	0.99
//	Mn	0.07	0.17
//	Mo	-	-
//	Zn	0.052	0.5
//	Ni	-	-
	private static Cultivo getSoja(){
		Cultivo c = new Cultivo(SOJA);
		c.setAbsN(30d);
		c.setAbsP(5d);
		c.setAbsK(19d);
		c.setAbsS(5d);
		
		c.setExtN(c.getAbsN()*0.69);
		c.setExtP(c.getAbsP()*0.8);
		c.setExtK(c.getAbsK()*0.21);
		c.setExtS(c.getAbsS()*0.34);
		c.setRindeEsperado(4d);
		//para producir 1 Tn de grano se necesita una l�mina de agua 125 � 160 mm (Ing. Marta Castiglione)
		c.setAbsAgua(142.5);
		c.setEstival(true);
		c.setAporteMO(1000*(-1+c.getAbsN()/c.getExtN()));//kg por tn;  estimacion en base a la extraccion de n vs absorcion de n
		//135 dias de desarrollo para una Soja P
		return c;
	}
	
//	NUTRIENTE	REQUERIMIENTO	IC
//	-	(kg ton-1)	(%)
//	N	22.2	0.66
//	P	4	0.84
//	K	26.2	0.1
//	Ca	2.8	0.04
//	Mg	2.4	0.42
//	S	0.94	0.64
//	B	0.016	0.5
//	Cl	-	-
//	Cu	0.027	0.92
//	Fe	0.35	0.57
//	Mn	0.37	0.16
//	Mo	-	-
//	Zn	0.04	0.5
//	Ni	-	-
	private static Cultivo getGirasol(){
		Cultivo c = new Cultivo(GIRASOL);
		c.setAbsN(22.2);
		c.setAbsP(4d);
		c.setAbsK(26.2);
		c.setAbsS(0.94);
		
		c.setExtN(c.getAbsN()*0.66);
		c.setExtP(c.getAbsP()*0.84);
		c.setExtK(c.getAbsK()*0.1);
		c.setExtS(c.getAbsS()*0.64);
		c.setRindeEsperado(6d);
		c.setAbsAgua(1000/12d);
		c.setEstival(true);
		c.setAporteMO(1000*(-1+c.getAbsN()/c.getExtN()));//kg por tn;  estimacion en base a la extraccion de n vs absorcion de n
		return c;
	}
	
}
