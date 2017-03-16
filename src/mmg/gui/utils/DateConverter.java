package mmg.gui.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.util.StringConverter;

public class DateConverter extends StringConverter<LocalDate>
{
	// Default Date Pattern
	private String pattern = "dd/MM/yyyy";
	// The Date Time Converter
	private DateTimeFormatter dtFormatter;

	public DateConverter() 
	{
		dtFormatter = DateTimeFormatter.ofPattern(pattern);
	}

	public DateConverter(String pattern) 
	{
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
	public String toString(LocalDate date) 
	{
		String text = null;

		if (date != null) 
		{
			text = dtFormatter.format(date);
		}

		return text;
	}	
}