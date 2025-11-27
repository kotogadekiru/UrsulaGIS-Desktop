package com.ursulagis.desktop.dao.utils;

import java.sql.Date;
import java.time.LocalDate;


import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

@Converter(autoApply = true)
public class JPALocalDatePropertyConverter implements AttributeConverter<Property<LocalDate>, Date> {
	

//	public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {
		
	    @Override
	    public Date convertToDatabaseColumn(Property<LocalDate> locDate) {
	    	return (locDate == null ? null : Date.valueOf(locDate.getValue()));
	    }

	    @Override
	    public Property<LocalDate> convertToEntityAttribute(Date sqlDate) {
	    	return (sqlDate == null ? null : new SimpleObjectProperty<LocalDate>(sqlDate.toLocalDate()));
	    }
	

}
