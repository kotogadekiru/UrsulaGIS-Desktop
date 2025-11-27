package com.ursulagis.desktop.dao.utils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

@Converter(autoApply = true)
public class JPADoublePropertyConverter  implements AttributeConverter<DoubleProperty, Double> {

	
    @Override
    public Double convertToDatabaseColumn(DoubleProperty toDB) {
    	return (toDB == null ? null : toDB.get());
    }

    @Override
    public DoubleProperty convertToEntityAttribute(Double fromDB) {
    	return (fromDB == null ? null : new SimpleDoubleProperty(fromDB));
    }
}
