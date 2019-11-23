package dao.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Nutriente.FIND_ALL, query="SELECT o FROM Nutriente o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Nutriente.FIND_NAME, query="SELECT o FROM Nutriente o where o.nombre = :name") ,
}) 

public class Nutriente {
	
	public static final String FIND_ALL="Nutriente.findAll";
	public static final String FIND_NAME="Nutriente.findName";
	
	
	@Id @GeneratedValue
	private Long id=null;
	
	public String nombre;
	public String simbolo;
	public Double pesoMolecular;
	

}
