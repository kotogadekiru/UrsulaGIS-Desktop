package com.ursulagis.desktop.dao;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.ursulagis.desktop.api.OrdenPulverizacion;
import lombok.Getter;

/*
 @Entity
public class Element
{
   @Id
   @GeneratedValue
   @Type(type = "uuid-binary") // This is pg-uuid by default for PostgreSQL82Dialect and higher
   private UUID id;

   // ...
}
 */

@Getter
@MappedSuperclass
public abstract class AbstractBaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	//@Id
	//@Column(name = "UUID", updatable = false, nullable = false)
	private String uuid;

	public AbstractBaseEntity() {
		this.uuid = UUID.randomUUID().toString();
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractBaseEntity)) {
			return false;
		}
		AbstractBaseEntity other = (AbstractBaseEntity) obj;
		if(getUuid()!=null) {
			return getUuid().equals(other.getUuid());
		}else return false;
	}
	
	public static void main(String[] args) {
		System.out.println("testing uuid");
		for(int i =0 ; i<30;i++) {
			OrdenPulverizacion e = new OrdenPulverizacion();
			System.out.println("i="+i+" "+e.getUuid());
		}
	}
}
