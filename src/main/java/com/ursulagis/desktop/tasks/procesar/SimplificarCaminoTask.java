package com.ursulagis.desktop.tasks.procesar;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.recorrida.Camino;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.Messages;
import javafx.concurrent.Task;

public class SimplificarCaminoTask extends Task<Camino>{
	private Camino camino=null;
	private List<Position> positions=null;

	public SimplificarCaminoTask(Camino c) {
		this.camino = c;
		this.positions=this.camino.getPositions();
	}

	@Override
	protected Camino call() throws Exception {
		// TODO simplificar camino y devolverlo
		NumberFormat nf = Messages.getNumberFormat();
		System.out.println("antes "+nf.format(camino.getLongitud()));
		//SimplificarCaminoTask.ordenarTriadas(camino);
		ponerInicioYFin(camino);
		ordenarRecorrido(camino);
		quitarInicioYFin(camino);
		//this.ordenarRecorrido2(this.camino);
		System.out.println("despues "+nf.format(camino.getLongitud()));
		return camino;
	}
	public void quitarInicioYFin(Camino c) {
		//TODO encontrar los puntos mas alejados entre si
		List<Position> list = c.getPositions();
		
		list.remove(0);
		list.remove(list.size()-1);		
	}
	public void ponerInicioYFin(Camino c) {
		//TODO encontrar los puntos mas alejados entre si
		List<Position> list = c.getPositions();
		Envelope e = new Envelope();
		for(Position p:list) {
			e.expandToInclude(p.getLongitude().degrees, p.getLatitude().degrees);
		}
//		Coordinate ce =e.centre();
		Position ini = Position.fromDegrees(e.getMinX()-e.getWidth(), e.getMinY()-e.getHeight());
		Position fin = Position.fromDegrees(e.getMaxX()+e.getWidth(), e.getMaxY()+e.getHeight());
		list.add(0,ini);
		list.add(fin);		
	}
	


	//TODO tomar grupos de 3 
	//y ver si estan ordenados de modo que circulen por los 2 lados menores y no uno mayor y uno menor
	public static int ordenarTriadas(Camino camino) {
		List<Position> positions = camino.getPositions();
		if(positions.size()<3)return 0;
		int crosses = 0;		
		do {
			crosses = 0;		
			for (int i = 0; i < positions.size()-2; i++) {
				Double ab = distanciaPuntosGrados(i, i+1, positions);
				Double bc = distanciaPuntosGrados(i+1, i+2, positions);
				Double ac = distanciaPuntosGrados(i, i+2, positions);
				if(ab<bc && ac<bc) {
					//a esta en el medio de los 2 lados cortos.
					//cambiar como primer punto a b
					swap(i,i+1,positions);
					System.out.println("cambiando a por b como primer punto");
					crosses++;
				}else if(ac<bc) {//a esta en una de las puntas
					swap(i+1,i+2,positions);
					crosses++;
					System.out.println("cambiando b por c");
				}				
			}			
		}while(crosses>0 );
		return crosses;
	}

	
	public int ordenarRecorrido(Camino c){
		// The tricky part of this code is not testing 2 adjacents edges for
		// crosses.
		// You need to be careful when edge lands on 0. (temp helps this check)
		int crosses = 1;
		int temp=0;

		for(int tries2 =0;tries2<1000 && crosses>0;tries2++) {
			crosses=0;
			temp=0;
			for (int i = 0; i < positions.size(); i++) {
				//ordena el camino entre el primero y el ultimo para que sea lo mas corto posible
				temp = (i > 0) ? 0 : 1;
				for (int j = i ; j < positions.size() -1 -temp; j++) {
					//cruzo i con todos los siguientes excepto el ultimo y el primero
					crosses+=swapIfCrossed(i,j,positions);
				}
			}
		}
		return crosses;
	}


	public static void swap(int i, int j, List<Position> positions) {
		System.out.println("swapping "+i+" "+j);
		i=i%positions.size();
		j=j%positions.size();
		Position posI= positions.get(i);
		Position posJ= positions.get(j);
		positions.remove(i);
		positions.add(i,posJ);
		positions.remove(j);
		positions.add(j,posI);
	}

	/**
	 * verifica  si la distancia  (i, j) mas (i2, j2) es menor que la distancia (i, i2) mas (j,j2)  
	 * esto tiene sentido por que 4 puntos forman un paralelogramo y las diagonales siempre son mas largas que los costados
	 * @param i
	 * @param j
	 * @return 1 si estan crusados 0 si no
	 */
	public int swapIfCrossed(int i, int j,List<Position> positions) {
		if (i == j)	return 0;
		//		double dij= distance(i, j);
		//		if(dij>2*this.resolucion){
		//			return false;//no permito caminos mas largos del doble de la resolucion
		//		}
		//i2 es el indice del siguiente nodo despues de i
		int i2 = (i + 1) % positions.size(); // may be wasteful, but safer
		int j2 = (j + 1) % positions.size();

		double sumaDiags =distanciaPuntosGrados(i, j,positions) + distanciaPuntosGrados(i2, j2,positions);
		double sumaLados =distanciaPuntosGrados(i, i2,positions)+ distanciaPuntosGrados(j, j2,positions);
		if(  sumaDiags	< sumaLados) {//controlo que esten cruzados
			//tengo que cambiar los puntos cruzados y dar vuelta el sentido de recorrido de todos los puntos intermedios
			//0  1  2  3  4 i=0 j=4
			//A->B->C->D->E si B y E estan cruzados se convierte en A->E->D->C->B
			List<Position> sub = positions.subList(i2,j+1);//desde hasta exclusivo por eso sumo 1
			Collections.reverse(sub);//como sub es una view de positions los cambios impactan sobre positions

			//			for(int k=0;k<sub.size();k++) {
			//				positions.set(i+1+k, sub.get(k));
			//			}			
			//			for (int k = 0; k <= (j - i - 1) / 2; k++) { 
			//				//hago el swap de todos los elementos de k hasta j
			//				
			//				Position c = positions.get(i + 1 + k);
			//				positions.set(i,positions.get( j - k));
			//				positions.set(j,c);
			//			}
			return 1;
		}
		return 0;
		//return areCrossed(i, j);	
	}

	public static void main(String[] args) {
		Camino c = new Camino();
		Configuracion config = Configuracion.getInstance();

		Double iLong =0.0;// Double.parseDouble(config.getPropertyOrDefault("gov.nasa.worldwind.avkey.InitialLongitude", "-61.97"));
		Double iLat =0.0;// Double.parseDouble(config.getPropertyOrDefault("gov.nasa.worldwind.avkey.InitialLatitude","-33"));
		c.getPositions().add(Position.fromDegrees(iLat-1,iLong));
		c.getPositions().add(Position.fromDegrees(iLat+1,iLong+1));
		c.getPositions().add(Position.fromDegrees(iLat+1,iLong+2));
		c.getPositions().add(Position.fromDegrees(iLat-1,iLong+2));
		c.getPositions().add(Position.fromDegrees(iLat-1,iLong+1));
		c.getPositions().add(Position.fromDegrees(iLat+1,iLong));
		printCamino(c);
		SimplificarCaminoTask t = new SimplificarCaminoTask(c);
		try {
			t.call();
			printCamino(c);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public static void printCamino(Camino c) {
		StringBuilder sb = new StringBuilder();

		List<Position> list = c.getPositions();
		for(int i=0;i<list.size();i++) {
			Position p = list.get(i);
			sb.append("\n["+i+"] ("+p.getLatitude()+", "+p.getLongitude()+"), ");
		}
		//String s = c.getPositions().stream().map(p->p.toString()).collect(Collectors.joining(", ","",""));
		System.out.println(sb);
	}

	public static double distanciaPuntosGrados(int i, int j,List<Position> positions) {
		if(i==j){
			System.out.println("calculando la distancia entre 2 elementos de igual indice");
			return 0;
		}
		Position di = positions.get(i%positions.size());
		Position dj = positions.get(j%positions.size());			
		return Position.greatCircleDistance(di,dj).degrees;
		//				new LatLon(di.getLatitude(),di.getLongitude()),
		//				new LatLon(dj.getLatitude(),dj.getLongitude())).degrees;
	}

}
