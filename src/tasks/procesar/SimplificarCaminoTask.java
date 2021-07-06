package tasks.procesar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dao.recorrida.Camino;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import javafx.concurrent.Task;
import utils.ProyectionConstants;

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
		System.out.println("antes "+camino.getLongitud());
		this.ordenarRecorrido(this.camino);
		System.out.println("despues "+camino.getLongitud());
		return camino;
	}
	

	
	public int ordenarRecorrido(Camino c){
	// The tricky part of this code is not testing 2 adjacents edges for
	// crosses.
	// You need to be careful when edge lands on 0. (temp helps this check)
		int crosses = 0;
		int temp=0;
		int tries=0;
		do {
		for (int i = 0; i < positions.size(); i++) {//ordena el camino entre el primero y el ultimo para que sea lo mas corto posible
			temp = (i > 0) ? 0 : 1;
			for (int j = i ; j < positions.size()-1- temp; j++)
				crosses+=swapIfCrossed(i,j,positions);
		}
		tries++;
		}while(crosses>0 && tries <1000);
		
		return crosses;
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
	

	public double distanciaPuntosGrados(int i, int j,List<Position> positions) {
		if(i==j){
			System.out.println("calculando la distancia entre 2 elementos de igual indice");
			return 0;
		}
		Position di = positions.get(i);
		Position dj = positions.get(j);			
		return Position.greatCircleDistance(
				new LatLon(di.getLatitude(),di.getLongitude()),
				new LatLon(dj.getLatitude(),dj.getLongitude())).degrees;
	}

}
