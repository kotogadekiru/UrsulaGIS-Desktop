package dao;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import dao.config.Cultivo;

import javax.persistence.AccessType;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import lombok.Data;

@Data
@Entity @Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Poligono.FIND_ALL, query="SELECT c FROM Poligono c") ,
	@NamedQuery(name=Poligono.FIND_NAME, query="SELECT o FROM Poligono o where o.nombre = :name") ,
}) 
public class Poligono {
	public static final String FIND_ALL="Poligono.findAll";
	public static final String FIND_NAME = "Poligono.findName";
	
	@Id @GeneratedValue
	private long id;
	private String nombre="";
	private double area;
	private String positionsString="";
	@Transient
	private List<Position> positions = new ArrayList<Position>();
	@Transient
	private Layer layer =null;
	
	public Poligono(){
		//this.setPositionsString("{{-35.462175934426305,-61.5357421901391}{-35.462175934426305,-61.5357421901391}{-35.5221036194563,-61.54692846191018}{-35.51801142905433,-61.48036800329617}{-35.462175934426305,-61.5357421901391}}");
	}
	
	public String getPositionsString(){
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for(Position p:positions){
			sb.append("{"+p.getLatitude().degrees+","+p.getLongitude().degrees+"}");
			
		}
		sb.append("}");
		positionsString=sb.toString();
		//System.out.println(positionsString);
		return positionsString;
	}
	
	public void setPositionsString(String s){
		try{
		positionsString=s.substring(1, s.length()-2);//descarto el primer { y el ultimo }
		String[] parts = s.split("\\{");
		for (int i = 0; i < parts.length; i++) {
			String p = parts[i];
			if(p.contains(",")){
			String[] latlon = p.substring(0,p.length()-2).split(",");
			Position pos = Position.fromDegrees(new Double(latlon[0]), new Double(latlon[1]));
			positions.add(pos);
			}
		}
		positions.remove(positions.size()-1);
		positions.add(positions.get(0));
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
	
	public void setLayer(Layer l){
		this.layer=l;
		DecimalFormat dc = new DecimalFormat("0.00");
		dc.setGroupingSize(3);
		dc.setGroupingUsed(true);
		layer.setName(nombre+" "+dc.format(area)+" Ha");
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
}
