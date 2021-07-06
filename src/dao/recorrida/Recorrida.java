package dao.recorrida;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import dao.Labor;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * clase que representa una observacion
 * @author quero
 *
 */
@Getter
@Setter(value = AccessLevel.PUBLIC)

@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Recorrida.FIND_ALL, query="SELECT o FROM Recorrida o"),
	@NamedQuery(name=Recorrida.FIND_NAME, query="SELECT o FROM Recorrida o where o.nombre = :name") ,
	@NamedQuery(name=Recorrida.FIND_ID, query="SELECT o FROM Recorrida o where o.id = :id") ,
}) 

public class Recorrida {
	//public static final String FIND_ALL = "Recorrida.findAll";
	public static final String CLASS_NAME= "Recorrida";
	public static final String FIND_ALL = CLASS_NAME+".findAll";
	public static final String FIND_NAME = CLASS_NAME+".findName";
	public static final String FIND_ID = CLASS_NAME+".findID";
	
	@Id @GeneratedValue//(strategy = GenerationType.IDENTITY)
	private Long id;
	
	public String nombre=new String();
	public String observacion=new String();
	
	//public String posicion=new String();//json {long,lat}
	public Double latitude= new Double(0.0);
	public Double longitude=new Double(0.0);
	
	@OneToMany(cascade=CascadeType.ALL, mappedBy="recorrida",orphanRemoval=true)
	@OrderBy("id ASC")
	public List<Muestra> muestras =new ArrayList<Muestra>();
	
	public Recorrida() {
		
//		Recorrida r=this;
//		muestras.addListener(new ListChangeListener<Muestra>() {
//			List<Muestra> added = new ArrayList<Muestra>();
//			@Override
//			public void onChanged(Change<? extends Muestra> c) {
//				if(!c.next()) {//me aseguro de estar en el ultimo cambio
//				System.out.println("muestras changed "+c);
//				
//				added.addAll(c.getAddedSubList());
//				added.stream().forEach(m->m.setRecorrida(r));
//				added.clear();
//				
//				c.getRemoved().stream().forEach(m->m.setRecorrida(null));
//				}
//			}
//			
//		});
	}
	
	
	@Override
	public String toString() {
		return nombre;
	}
}
