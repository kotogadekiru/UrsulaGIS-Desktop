package com.ursulagis.desktop.dao.utils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Converter(autoApply = true)
public class JPAStringPropertyConverter implements AttributeConverter<StringProperty, String> {

	
    @Override
    public String convertToDatabaseColumn(StringProperty toDB) {
    	return (toDB == null ? null : toDB.get());
    }

    @Override
    public StringProperty convertToEntityAttribute(String fromDB) {
    	return (fromDB == null ? null : new SimpleStringProperty(fromDB));
    }

}
