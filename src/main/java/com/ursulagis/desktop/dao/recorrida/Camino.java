package com.ursulagis.desktop.dao.recorrida;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import com.ursulagis.desktop.gui.Messages;
import lombok.Data;
import com.ursulagis.desktop.utils.ProyectionConstants;

@Data
@Entity @Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Camino.FIND_ALL, query="SELECT c FROM Camino c ORDER BY lower(c.nombre)") ,
	@NamedQuery(name=Camino.FIND_NAME, query="SELECT o FROM Camino o where o.nombre = :name") ,
	@NamedQuery(name=Camino.FIND_ACTIVOS, query="SELECT o FROM Camino o where o.activo = true ORDER BY lower(o.nombre)") ,
}) 
public class Camino implements Comparable<Camino>{
	private static final String COORDINATE_CLOSE = "}";
	private static final String COORDINATE_OPEN = "{";
	private static final String COORDITANTE_SEPARATOR = ",";
	public static final String FIND_ALL="Camino.findAll";
	public static final String FIND_NAME = "Camino.findName";
	public static final String FIND_ACTIVOS = "Camino.findActivos";

	private Long id=null;
	private String nombre="";
	private double longitud=-1;

	/**
	 * indica si se muestra al inicio
	 */
	private boolean activo =false;
	private String positionsString="";
	//un poligono tiene LineString shell y LineString[] holes  #ver PolygonValidator
	@Transient
	private List<Position> positions = new ArrayList<Position>();
	@Transient
	private Layer layer =null;


	private static DecimalFormat lonLatFormat = null;
	static {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		lonLatFormat = (DecimalFormat)nf;
		//lonLatFormat = new DecimalFormat("#0.00000000;-#0.00000000");
		//System.out.println("inicializando lonLanFormat");
		lonLatFormat.getDecimalFormatSymbols().setDecimalSeparator('.');
		lonLatFormat.getDecimalFormatSymbols().setGroupingSeparator(',');
		lonLatFormat.setMinimumFractionDigits(8);
	}


	//private List<Position> positions=null;
//	@Transient
//	private Position origen=null;
//	@Transient
//	private Position destino=null;

	public Camino() {
		
	}
	public Camino(List<Position> puntos) {
		this.positions = puntos;
	}

	@Id @GeneratedValue
	public Long getId(){
		return this.id;
	}

	public String getPositionsString(){
		StringBuilder sb = new StringBuilder();
		sb.append(COORDINATE_OPEN);
		for(Position p:positions){
			Double dLat = p.getLatitude().degrees;
			Double dLon= p.getLongitude().degrees;
			String sLat =lonLatFormat.format(dLat);
			String sLon = lonLatFormat.format(dLon);

			//			if(!sLon.equals(dLon.toString())){
			//				System.out.println("hubo un error al serializar el poligono! "+sLon+ " != "+dLon);
			//			}
			String s = COORDINATE_OPEN+sLat+COORDITANTE_SEPARATOR+sLon+COORDINATE_CLOSE;
			sb.append(s);// {-33,00000000,91375176,00000000}
			//	System.out.println("agregando al double de positions => "+ COORDINATE_OPEN+dLat+COORDITANTE_SEPARATOR+dLon+COORDINATE_CLOSE);
			//	System.out.println("agregando al string de positions => "+ s);

		}
		sb.append(COORDINATE_CLOSE);
		positionsString=sb.toString();
		//System.out.println(positionsString);
		return positionsString;
	}

	/**
	 * metodo que toma un string conf formato {{{lat,long},{lat}}}
	 * y crea la lista de posiciones del poligono
	 * @param s
	 */
	public void setPositionsString(String s){
		positions.clear();
		try{
			positionsString=s.substring(1, s.length()-2);//descarto el primer "{" y el ultimo "}"
			String[] parts = s.split("\\{");
			for (int i = 0; i < parts.length; i++) {
				String p = parts[i];
				if(p.contains(COORDITANTE_SEPARATOR)){
					String[] latlon = p.substring(0,p.length()-2).split(COORDITANTE_SEPARATOR);
					String lat = latlon[0];
					String lon = latlon[1];
					try{
						Double dLat = lonLatFormat.parse(lat).doubleValue();// Double.parseDouble(lon);
						Double dLon = lonLatFormat.parse(lon).doubleValue();// Double.parseDouble(lon);
						Position pos = Position.fromDegrees(dLat,dLon);
						positions.add(pos);
					}catch(Exception e){
						System.out.println("error al des serializar el camino");
						e.printStackTrace();
					}
				}
			}

//			Position p0 = positions.get(0);
//			Position pn = positions.get(positions.size()-1);
//			if(!p0.equals(pn)){
//				positions.add(positions.get(0));
//				//	System.out.println("completando el poligono para que sea cerrado");
//			}

			GeometryFactory fact = new GeometryFactory();
			Coordinate[] shell = new Coordinate[positions.size()];
			for(int i =0; i<positions.size();i++){
				Position pos=positions.get(i);
				shell[i]=new Coordinate(pos.getLongitude().degrees,pos.getLatitude().degrees);

			}
			LineString p = fact.createLineString(shell);
			this.setLongitud(p.getLength()/ProyectionConstants.metersToLat());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public double getLongitud() {
		//private double calcLongitud(Camino c) {
			Double d = Double.valueOf(0.0);
			Position last = null;
			for(Position p : this.getPositions()) {
				if(last!=null) {
					d+= Position.greatCircleDistance(last,p).degrees/ProyectionConstants.metersToLat();
				}
				last=p;
			}		
			return d;
		//}
	}
	public void setNombre(String n){
		this.nombre=n;
		if(this.layer!=null){			
			NumberFormat dc = Messages.getNumberFormat();
			String formated = dc.format(this.getLongitud())+" "+Messages.getString("JFXMain.metrosAbrevSufix"); //$NON-NLS-1$
			
			layer.setName(nombre+" "+formated);
		}
	}
	@Transient
	public void setLayer(Layer l){
		this.layer=l;
		NumberFormat dc = Messages.getNumberFormat();
		layer.setName(nombre+" "+dc.format(longitud)+" "+Messages.getString("JFXMain.metrosAbrevSufix"));
	}
	@Transient
	public Layer getLayer(){
		return this.layer;
	}

	@Transient
	public List<Position> getPositions(){
		return this.positions;
	}

	public void setLongitud(double a){
		this.longitud =a;
		if(this.layer!=null){
			NumberFormat dc = Messages.getNumberFormat();
			layer.setName(nombre+" "+dc.format(longitud)+" "+Messages.getString("JFXMain.metrosAbrevSufix"));
		}
	}

	public boolean getActivo(){
		return activo;
	}

	public String toString(){
		return this.getNombre();
	}

	public Geometry toGeometry(){
		GeometryFactory fact = new GeometryFactory();
		List<? extends Position> positions = this.getPositions();
		Coordinate[] coordinates = new Coordinate[positions.size()];
		for(int i=0;i<positions.size();i++){
			Position p = positions.get(i);	
			Coordinate c = new Coordinate(p.getLongitude().getDegrees(),p.getLatitude().getDegrees(),p.getElevation());

			coordinates[i]=c;
		}
		coordinates[coordinates.length-1]=coordinates[0];//en caso de que la geometria no este cerrada
		LinearRing poly = fact.createLinearRing(coordinates);	
		return poly;
	}

	@Override
	public int compareTo(Camino p) {
		if(p==null || p.getNombre()==null )return -1;
		return this.getNombre().compareToIgnoreCase(p.getNombre());
	}

	public String getPoligonoToString() {
		List<? extends Position> positions = this.getPositions();

		StringBuilder sb = new StringBuilder();
		sb.append("[[[");
		for(Position p:positions){	
			Angle lon= p.getLongitude();
			Angle lat = p.getLatitude();
			sb.append("["+lon.degrees+","+lat.degrees+"],");
		}
		sb.deleteCharAt(sb.length()-1);

		sb.append("]]]");
		String polygons=sb.toString();
		return polygons;
	}

//	double largoCaminoGrados(List<Position> positions) {
//		double d = 0;
//		for (int i = 0; i < positions.size()-1; i++){
//			Position di = positions.get(i);
//			Position dj =positions.get(i + 1);	
//
//			d += Position.greatCircleDistance(
//					new LatLon(di.getLatitude(),di.getLongitude()),
//					new LatLon(dj.getLatitude(),dj.getLongitude())).degrees;
//		}
//		return d;//+ distance(0, Positions.size() - 1);
//	}
//
//	public static double distanciaPuntosGrados(int i, int j,List<Position> positions) {
//		if(i==j){
//			System.out.println("calculando la distancia entre 2 elementos de igual indice");
//			return 0;
//		}
//		Position di = positions.get(i);
//		Position dj = positions.get(j);			
//		return Position.greatCircleDistance(
//				new LatLon(di.getLatitude(),di.getLongitude()),
//				new LatLon(dj.getLatitude(),dj.getLongitude())).degrees;
//	}	
//
//	/**
//	 * verifica  si la distancia  (i, j) mas (i2, j2) es menor que la distancia (i, i2) mas (j,j2)  
//	 * esto tiene sentido por que 4 puntos forman un paralelogramo y las diagonales siempre son mas largas que los costados
//	 * @param i
//	 * @param j
//	 * @return 1 si estan crusados 0 si no
//	 */
//	public static int swapIfCrossed(int i, int j,List<Position> positions) {
//		if (i == j)	return 0;
//		//		double dij= distance(i, j);
//		//		if(dij>2*this.resolucion){
//		//			return false;//no permito caminos mas largos del doble de la resolucion
//		//		}
//		//i2 es el indice del siguiente nodo despues de i
//		int i2 = (i + 1) % positions.size(); // may be wasteful, but safer
//		int j2 = (j + 1) % positions.size();
//
//
//		if( distanciaPuntosGrados(i, j,positions) + distanciaPuntosGrados(i2, j2,positions) 
//		< distanciaPuntosGrados(i, i2,positions)+ distanciaPuntosGrados(j, j2,positions)) {//controlo que esten cruzados
//			for (int k = 0; k <= (j - i - 1) / 2; k++) {
//				//hago el swap de todos los elementos de k hasta j
//				Position c = positions.get(i + 1 + k);
//				positions.set(i,positions.get( j - k));
//				positions.set(j,c);
//			}
//			return 1;
//		}
//		return 0;
//		//return areCrossed(i, j);	
//	}
//
//	public static int ordenarRecorrido(List<Position> positions){
//		// The tricky part of this code is not testing 2 adjacents edges for
//		// crosses.
//		// You need to be careful when edge lands on 0. (temp helps this check)
//
//		int crosses = 0;
//		int temp;
//
//		for (int i = 0; i < positions.size() - 1; i++) {//ordena el camino entre el primero y el ultimo para que sea lo mas corto posible
//			temp = (i > 0) ? 0 : 1;
//			for (int j = i ; j < positions.size()-1- temp; j++)
//				crosses+=swapIfCrossed(i,j,positions);
//		}
//		return crosses;
//	}
//
//
//
//	public void encontrarCaminoMinimo(Consumer<Camino> mostrar){
//		//		System.out.println("cantidad de Positions antes de recorrer "+Positions.size());
//		//		System.out.println("camino inicial: "+Positions);
//		ArrayList<Position> oldPositions = new ArrayList<Position>();
//		oldPositions.addAll(positions);
//		ArrayList<Double> largos = new ArrayList<Double>();
//		//	double oLengh = this.length();	
//		double nLengh = 0;	
//		double lowest = this.largoCaminoGrados(positions);
//		//	int cuenta = 0;
//		do {			
//			//		oLengh = nLengh;	
//			//			 oldPositions.clear();
//			//			 oldPositions.addAll(Positions);
//			int swaps = ordenarRecorrido(positions);	
//
//			nLengh = largoCaminoGrados(positions);
//			largos.add(Double.parseDouble(nLengh));
//			if(nLengh == lowest) break;
//			if(nLengh < lowest) {
//
//				lowest= nLengh;
//				oldPositions.clear();
//				oldPositions.addAll(positions);
//			} else {
//				//	cuenta++;
//				if(largos.contains((Double.parseDouble(nLengh)))){
//					this.positions.clear();
//					positions.addAll(oldPositions);
//					break;//
//				}
//
//			}
//			System.out.println( (int)nLengh+" lowest="+lowest);
//			//			if(nLengh> oLengh){
//			//				System.out.println("Swapping");
//			////				this.Positions.clear();
//			////				this.Positions.addAll(oldPositions);
//			//				double swap = oLengh;
//			//				oLengh=nLengh;
//			//				nLengh=swap;
//			//				//System.exit(1);
//			//			}
//
//		}while (true);		
//		//		System.out.println("cantidad de Positions despues de recorrer "+Positions.size());
//		//System.out.println("camino minimo: "+Positions);	
//	}
	@Transient
	public void setOrigen(Position origen2) {	
		//positions.remove(origen);
		//this.origen=origen2;
		this.positions.remove(0);
		this.positions.add(0, origen2);		
	}
	
	@Transient
	public void setDestino(Position newPosition) {
		positions.remove(positions.size()-1);
		//this.destino=newPosition;
		this.positions.add(newPosition);		

	}
}
