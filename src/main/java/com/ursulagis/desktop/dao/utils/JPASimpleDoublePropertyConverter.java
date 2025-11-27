package com.ursulagis.desktop.dao.utils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import javafx.beans.property.SimpleDoubleProperty;

@Converter(autoApply = true)
public class JPASimpleDoublePropertyConverter implements AttributeConverter<SimpleDoubleProperty, Double> {

    public Double convertToDatabaseColumn(SimpleDoubleProperty toDB) {
    	return (toDB == null ? null : toDB.get());
    }

    @Override
    public SimpleDoubleProperty convertToEntityAttribute(Double fromDB) {
    	return (fromDB == null ? null : new SimpleDoubleProperty(fromDB));
    }
}
