package dao.pulverizacion;

import java.math.BigDecimal;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;

import dao.config.Agroquimico;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
//@Entity @Access(AccessType.FIELD)
public class CaldoItem {
	public static final String FIND_ALL="CaldoItem.findAll";
	public static final String FIND_NAME = "CaldoItem.findName";

	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	
	//@ManyToOne
	private PulverizacionLabor labor =null;
	
	//@ManyToOne//(cascade= {CascadeType.DETACH})
	private Agroquimico producto =null;
	
	private Double dosisHa = 0.0;
	private String observaciones =  null;
	@Override
	public String toString() {
		return "CaldoItem:{"+producto+", "+dosisHa+", "+observaciones+"}";
	}
}
