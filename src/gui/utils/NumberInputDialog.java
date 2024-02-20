package gui.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Optional;

import gui.JFXMain;
import gui.Messages;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;

public class NumberInputDialog {
    private static Double value = 0.0;
    
    public static Double showAndWait(String title, String content, String prompt, String tooltip) {
    	boolean inputIsValid = false;
		
		TextInputDialog dialog = new TextInputDialog(prompt);
		dialog.setTitle(title);
		dialog.setContentText(content);
		dialog.initOwner(JFXMain.stage);
		
		// Tooltip
		TextField textField = (TextField) dialog.getEditor();
        Tooltip tooltipText = new Tooltip(tooltip);
        textField.setTooltip(tooltipText);
		
		while(!inputIsValid) {
			Optional<String> result = dialog.showAndWait();
			// Validavion que sea un Double  
			if (result.isPresent())
				try {
					DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Messages.getLocale());
					DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
					char sep =symbols.getDecimalSeparator();
					
					Number number = format.parse(result.get());
					value = number.doubleValue();
					
					// y mayor a 0
					if (value >= 0.0) {
						inputIsValid = true;
						return value;
					}
					else {
						throw new ParseException(null, sep);
					}
				}
				catch (ParseException e) {
					Alert inputFieldAlert = new Alert(AlertType.INFORMATION,Messages.getString("JFXMain.IngreseNumValido")); 
					inputFieldAlert.showAndWait();
				}
			else
				return Double.NaN;
		}
		return value;
    }
	
}
		