package com.ursulagis.desktop.gui.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import javafx.util.StringConverter;

public class DateConverter extends StringConverter<LocalDate>
{
	// Default Date Pattern
	private String pattern = "dd/MM/yyyy";
	// The Date Time Converter
	private DateTimeFormatter dtFormatter;

	public DateConverter() {
		dtFormatter = DateTimeFormatter.ofPattern(pattern);
	}

	public DateConverter(String pattern) {
		this.pattern = pattern;
		dtFormatter = DateTimeFormatter.ofPattern(pattern);
	}

	// Change String to LocalDate
	public LocalDate fromString(String text) 
	{
		LocalDate date = null;

		if (text != null && !text.trim().isEmpty()) 
		{
			try{
				date = LocalDate.parse(text, dtFormatter);
			}catch(Exception e){
				System.out.println("no se pudo parsear "+text);
				date = LocalDate.now();
				e.printStackTrace();
			}
		}

		return date;
	}

	// Change LocalDate to String
	public String toString(LocalDate date) {
		String text = null;

		if (date != null) 
		{
			text = dtFormatter.format(date);
		}

		return text;
	}

	public static Date asDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Date asDate(LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static LocalDate asLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}
	
	public static LocalDate asLocalDate(Calendar cal ) {
		Date date = cal.getTime();
		return asLocalDate(date);
		//return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public static LocalDateTime asLocalDateTime(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
}