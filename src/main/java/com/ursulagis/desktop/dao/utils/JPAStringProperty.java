package com.ursulagis.desktop.dao.utils;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

import javafx.beans.property.SimpleStringProperty;

@Embeddable 
@Access(AccessType.PROPERTY)
public class JPAStringProperty extends SimpleStringProperty{
	public String getString(){
		return this.get();
	}
	public void setString(String s){
		this.set(s);
	}

}
