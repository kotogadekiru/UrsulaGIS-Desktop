package gui.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Optional;

import gui.JFXMain;
import gui.Messages;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;

public class NumberInputDialog {
    private static Double value = 0.0;
    
    public static Double showAndWait(String message) {
    	boolean inputIsValid = false;
		
		TextInputDialog dialog = new TextInputDialog(message);
		dialog.setTitle(Messages.getString("JFXMain.251"));
		dialog.setContentText(Messages.getString("JFXMain.252"));
		dialog.initOwner(JFXMain.stage);
		dialog.getDialogPane().setMinWidth(300);
		
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
		