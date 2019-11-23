package dao.OrdenDeCompra;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Inheritance;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
@Inheritance(strategy=javax.persistence.InheritanceType.TABLE_PER_CLASS)
public abstract class Producto {
	
	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	
	public abstract String getNombre();
	public abstract void setNombre(String nombre);

}
