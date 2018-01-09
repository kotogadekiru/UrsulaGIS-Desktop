package dao;


import java.text.DecimalFormat;
import java.text.NumberFormat;
//import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.Access;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;


import javax.persistence.AccessType;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import lombok.Data;

@Data
@Entity @Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Poligono.FIND_ALL, query="SELECT c FROM Poligono c") ,
	@NamedQuery(name=Poligono.FIND_NAME, query="SELECT o FROM Poligono o where o.nombre = :name") ,
	@NamedQuery(name=Poligono.FIND_ACTIVOS, query="SELECT o FROM Poligono o where o.activo = true") ,
}) 
public class Poligono {
	private static final String COORDINATE_CLOSE = "}";
	private static final String COORDINATE_OPEN = "{";
	private static final String COORDITANTE_SEPARATOR = ",";
	public static final String FIND_ALL="Poligono.findAll";
	public static final String FIND_NAME = "Poligono.findName";
	public static final String FIND_ACTIVOS = "Poligono.findActivos";
	
//	@Id @GeneratedValue
	private Long id=null;
	private String nombre="";
	private double area=-1;
	/**
	 * indica si se muestra al inicio
	 */
	private boolean activo =false;
	private String positionsString="";
	@Transient
	private List<Position> positions = new ArrayList<Position>();
	@Transient
	private Layer layer =null;
	

	private static DecimalFormat lonLatFormat = null;
	static {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		lonLatFormat = (DecimalFormat)nf;
		//lonLatFormat = new DecimalFormat("#0.00000000;-#0.00000000");
		System.out.println("inicializando lonLanFormat");
		lonLatFormat.getDecimalFormatSymbols().setDecimalSeparator('.');
		lonLatFormat.getDecimalFormatSymbols().setGroupingSeparator(',');
		lonLatFormat.setMinimumFractionDigits(8);
	}
	
	public Poligono(){
	
		//this.setPositionsString("{{-35.462175934426305,-61.5357421901391}{-35.462175934426305,-61.5357421901391}{-35.5221036194563,-61.54692846191018}{-35.51801142905433,-61.48036800329617}{-35.462175934426305,-61.5357421901391}}");
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
				Double dLat = lonLatFormat.parse(lat).doubleValue();// new Double(lon);
				Double dLon = lonLatFormat.parse(lon).doubleValue();// new Double(lon);
			
//				if(!lon.equals(dLon.toString())){
//					System.out.println("orig lon, parsed lon "+lon+" , "+dLon);
//					System.out.println("no son iguales");
//				}
			Position pos = Position.fromDegrees(dLat,dLon);
			positions.add(pos);
			}catch(Exception e){
				System.out.println("error al des serializar el poligono");
				e.printStackTrace();
			}
			}
		}
	//	positions.remove(positions.size()-1);
	
		Position anterior=null,actual =null;
		List <Position> aRemover = new ArrayList<Position>();
		for(int i = 1;positions.size()>1 && i<positions.size();i++){
			anterior = positions.get(i-1);
			actual = positions.get(i);
			if(anterior.equals(actual)){
				aRemover.add(actual);				
			}			
		}
		//System.out.println("Eliminando duplicados "+aRemover);
		positions.removeAll(aRemover);
		Position p0 = positions.get(0);
		Position pn = positions.get(positions.size()-1);
		if(!p0.equals(pn)){
			positions.add(positions.get(0));
		//	System.out.println("completando el poligono para que sea cerrado");
		}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void setNombre(String n){
		this.nombre=n;
		if(this.layer!=null){
			DecimalFormat dc = new DecimalFormat("0.00");
			dc.setGroupingSize(3);
			dc.setGroupingUsed(true);
			layer.setName(nombre+" "+dc.format(area)+" Ha");
		}
	}
	@Transient
	public void setLayer(Layer l){
		this.layer=l;
		DecimalFormat dc = new DecimalFormat("0.00");
		dc.setGroupingSize(3);
		dc.setGroupingUsed(true);
		layer.setName(nombre+" "+dc.format(area)+" Ha");
	}
	@Transient
	public Layer getLayer(){
		return this.layer;
	}
	
	@Transient
	public List<Position> getPositions(){
		return this.positions;
	}
	
	public void setArea(double a){
		this.area =a;
		if(this.layer!=null){
			DecimalFormat dc = new DecimalFormat("0.00");
			dc.setGroupingSize(3);
			dc.setGroupingUsed(true);
			layer.setName(nombre+" "+dc.format(area)+" Ha");
		}
	}
	
	public boolean getActivo(){
		return activo;
	}
	
	public String toString(){
		return this.getNombre();
	}
}
