package com.ursulagis.desktop.dao.utils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

@Converter(autoApply = true)
public class JPAObjectPropertyConverter  implements AttributeConverter<Property<Object>, Object> {	
    @Override
    public Object convertToDatabaseColumn(Property<Object> toDB) {
    	return (toDB == null ? null : toDB.getBean());
    }

    @Override
    public Property<Object> convertToEntityAttribute(Object fromDB) {
    	return (fromDB == null ? null :  new SimpleObjectProperty<Object>(fromDB));
    }
}
