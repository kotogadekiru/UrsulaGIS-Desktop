package com.ursulagis.desktop.dao;


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
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

import com.ursulagis.desktop.dao.config.Lote;

import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import com.ursulagis.desktop.gui.Messages;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.ProyectionConstants;

@Data
@EqualsAndHashCode(exclude="lote")
@Entity @Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Poligono.FIND_ALL, query="SELECT c FROM Poligono c ORDER BY lower(c.nombre)") ,
	@NamedQuery(name=Poligono.FIND_NAME, query="SELECT o FROM Poligono o where o.nombre = :name") ,
	@NamedQuery(name=Poligono.FIND_ACTIVOS, query="SELECT o FROM Poligono o where o.activo = true ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Poligono.FIND_BY_LOTE, query="SELECT o FROM Poligono o where o.lote = :lote") ,
}) 
public class Poligono implements Comparable<Poligono>{
	private static final String COORDINATE_CLOSE = "}";
	private static final String COORDINATE_OPEN = "{";
	private static final String COORDITANTE_SEPARATOR = ",";
	public static final String FIND_ALL="Poligono.findAll";
	public static final String FIND_NAME = "Poligono.findName";
	public static final String FIND_ACTIVOS = "Poligono.findActivos";
    public static final String FIND_BY_LOTE = "Poligono.findByLote";

	//@Id @GeneratedValue
	private Long id=null;
	private String nombre="";
	private Lote lote=null;
	private double area=-1;
	/**
	 * indica si se muestra al inicio
	 */
	private boolean activo =false;
	private String positionsString="";
	private String text="";//resultado de toText de la geometria
	//un poligono tiene LineString shell y LineString[] holes  #ver PolygonValidator
	
	//@Transient //transient va en el metodo getPositions()
	private List<Position> positions = new ArrayList<Position>();
	//@Transient
	private List<List<Position>> huecos = new ArrayList<List<Position>>();
	//@Transient
	private Geometry geometry=null;
	//@Transient
	private Layer layer =null;
	
	private List<Ndvi> imagenesPoligono =null;


	private static DecimalFormat lonLatFormat = null;
	static {//tiene que estar en ingles porque de lo contrario no anda bien descargar ndvi
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		lonLatFormat = (DecimalFormat)nf;
		//lonLatFormat = new DecimalFormat("#0.00000000;-#0.00000000");
		//System.out.println("inicializando lonLanFormat");
		lonLatFormat.getDecimalFormatSymbols().setDecimalSeparator('.');
		lonLatFormat.getDecimalFormatSymbols().setGroupingSeparator(',');
		lonLatFormat.setMinimumFractionDigits(8);
	}

	public Poligono(){
		
	}

	@Id @GeneratedValue
	public Long getId(){
		return this.id;
	}

	@OneToMany(cascade=CascadeType.ALL, mappedBy="contorno",orphanRemoval=true)
	private List<Ndvi> getImagenesPoligono(){
		return this.imagenesPoligono;
	}


	/** 
	 * construir positionsString a partir de las posiciones
	 */
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
			if(s.length()<=2)return;
			s=s.substring(1, s.length()-2);//descarto el primer "{" y el ultimo "}"
			String[] parts = s.split("\\{");
			for (int i = 0; i < parts.length; i++) {
				String p = parts[i];
				if(p.contains(COORDITANTE_SEPARATOR)){
					String[] latlon = p.substring(0,p.length()-2).split(COORDITANTE_SEPARATOR);
					String lat = latlon[0];
					String lon = latlon[1];
					try{
						//FIXME ava.lang.NumberFormatException: For input string: ".336706456E2336706456E2"
						Double dLat = lonLatFormat.parse(lat).doubleValue();// Double.parseDouble(lon);
						Double dLon = lonLatFormat.parse(lon).doubleValue();// Double.parseDouble(lon);

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

			//XXX comento esto porque tengo miedo que me este borrando puntos reales.
			//		Position anterior=null,actual =null;
			//		List <Position> aRemover = new ArrayList<Position>();
			//		for(int i = 1;positions.size()>1 && i<positions.size();i++){
			//			anterior = positions.get(i-1);
			//			actual = positions.get(i);
			//			if(anterior.equals(actual)){
			//				aRemover.add(actual);				
			//			}			
			//		}
			//		//System.out.println("Eliminando duplicados "+aRemover);
			//		positions.removeAll(aRemover);

			Position p0 = positions.get(0);
			Position pn = positions.get(positions.size()-1);
			if(!p0.equals(pn)){
				positions.add(positions.get(0));
				//	System.out.println("completando el poligono para que sea cerrado");
			}

			GeometryFactory fact = new GeometryFactory();
			Coordinate[] shell = new Coordinate[positions.size()];
			for(int i =0; i<positions.size();i++){
				Position pos=positions.get(i);
				shell[i]=new Coordinate(pos.getLongitude().degrees,pos.getLatitude().degrees);

			}
			Polygon p = fact.createPolygon(shell);
			this.setArea(ProyectionConstants.A_HAS(p.getArea()));
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public String getText(){
		Geometry g = this.geometry;//this.toGeometry();
		if(g==null)return "";
		this.text = g.toText();
		return this.text;
	}

	public void setText(String s){
		this.text = s;
		if(s==null || s.isEmpty())return;
		WKTReader reader = new WKTReader(ProyectionConstants.getGeometryFactory());
		try {
			//System.out.println("readding geometry from text "+s);
			Geometry g = reader.read(s);//org.locationtech.jts.io.ParseException: Expected word but found End-of-Stream (line 1)
			this.setPositions(GeometryHelper.geometryToPositions(g.getGeometryN(0)));
			if(huecos==null){
				huecos = new ArrayList<List<Position>>();
			}
			huecos.clear();
			for(int i=1;i<g.getNumGeometries();i++){
				huecos.add(GeometryHelper.geometryToPositions(g.getGeometryN(i)));
			}
			//TODO set holes
		} catch (Exception e) {
			System.out.println("error al leer el poligono desde el texto "+s);
			e.printStackTrace();
		}
	}
	
	@Column( columnDefinition="DECIMAL(32,15)")
	public double getArea() {
		return this.area;
	}
	

	public void setNombre(String n){
		this.nombre=n;
		if(this.layer!=null){
			NumberFormat dc = Messages.getNumberFormat();
			String formated = dc.format(this.area)+Messages.getString("PoligonLayerFactory.4"); //$NON-NLS-1$

			layer.setName(nombre+" "+formated);
		}
	}
	@Transient
	public void setLayer(Layer l){
		this.layer=l;
		NumberFormat dc = Messages.getNumberFormat();
		layer.setName(nombre+" "+dc.format(area)+Messages.getString("PoligonLayerFactory.4"));
	}

	@Transient
	public Layer getLayer(){
		return this.layer;
	}

	@Transient
	public List<Position> getPositions(){
		return this.positions;
	}
	@Transient
	public List<List<Position>> getHuecos(){
		return this.huecos;
	}
	@Transient
	public Geometry getGeometry(){
		if(this.geometry==null){
			this.geometry = this.toGeometry();
		}
		return this.geometry;
	}
	@Transient
	public void setGeometry(Geometry g){
		//TODO update positions and huecos, area, text
		this.geometry = g;
		try {
			//System.out.println("readding geometry from text "+s);
		
			this.setPositions(GeometryHelper.geometryToPositions(g.getGeometryN(0)));
			if(huecos==null){
				huecos = new ArrayList<List<Position>>();
			}
			huecos.clear();
			for(int i=1;i<g.getNumGeometries();i++){
				huecos.add(GeometryHelper.geometryToPositions(g.getGeometryN(i)));
			}
			//TODO set holes
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.setArea(ProyectionConstants.A_HAS(g.getArea()));
		this.text = g.toText();
	}

	public void setArea(double a){
		this.area =a;
		if(this.layer!=null){
			NumberFormat dc = Messages.getNumberFormat();
			layer.setName(nombre+" "+dc.format(area)+Messages.getString("PoligonLayerFactory.4"));
		}
	}

	public boolean getActivo(){
		return activo;
	}

	public String toString(){
		return this.getNombre();
	}

	public Geometry toGeometry(){
		return GeometryHelper.poligonotoGeometry(this);
	}

	@Override
	public int compareTo(Poligono p) {
		if(p==null || p.getNombre()==null )return -1;
		return this.getNombre().compareToIgnoreCase(p.getNombre());
	}

	//	@Override
	//	public boolean equals(Object o) {
	//		if(o!= null || ! (o instanceof Poligono)) return false;
	//		return this.getPoligonoToString().equals(((Poligono)o).positionsString);
	//	}

	/**
	 * metodo que devuelve el string necesario para consultar el ndvi
	 * @return
	 */
	public String getPoligonoToStringOld() {
		List<? extends Position> positions = this.getPositions();

		StringBuilder sb = new StringBuilder();
		sb.append("[[[");
		for(Position p:positions){	
			Angle lon= p.getLongitude();
			Angle lat = p.getLatitude();
			sb.append("["+lon.degrees+","+lat.degrees+"],");
		}
		sb.deleteCharAt(sb.length()-1);
//TODO agregar los huecos
		sb.append("]]]");
		String polygons=sb.toString();
		return polygons;
	}

	public String getPoligonoToString() {
		List<? extends Position> positions = this.getPositions();

		StringBuilder sb = new StringBuilder();
		//sb.append("[");//multi poligono
		sb.append("[");//poligon
		sb.append("[");//shell
		for(Position p:positions){	
			Angle lon= p.getLongitude();
			Angle lat = p.getLatitude();
			sb.append("["+lon.degrees+","+lat.degrees+"],");
		}	
		sb.deleteCharAt(sb.length()-1);
		sb.append("]");//close shell
		if(huecos!=null||huecos.size()>0){
			sb.append(",");//huecos
			for(List<Position> hole:huecos){
				sb.append("[");//hole
				for(Position p:hole){
					Angle lon= p.getLongitude();
					Angle lat = p.getLatitude();
					sb.append("["+lon.degrees+","+lat.degrees+"],");
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append("],");//close hole
			}
			sb.deleteCharAt(sb.length()-1);
			//sb.append("]");//close huecos
		}
		sb.append("]");//close poligon
		//sb.append("]");//close multi poligono
//TODO agregar los huecos
		
		String polygons=sb.toString();
		return polygons;
	}
}
