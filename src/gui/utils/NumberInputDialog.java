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
import lombok.Getter;

public class NumberInputDialog {
	private TextInputDialog dialog;
    private Double value;
    
    public static Double showAndWait(String message) {
    	boolean inputIsValid = false;
		Double dosis = 0.0;
		
		TextInputDialog dosisDialog = new TextInputDialog(Messages.getString("JFXMain.250"));
		dosisDialog.setTitle(Messages.getString("JFXMain.251"));
		dosisDialog.setContentText(Messages.getString("JFXMain.252"));
		dosisDialog.initOwner(JFXMain.stage);
		
		while(!inputIsValid) {
			Optional<String> dosisOptional = dosisDialog.showAndWait();
			// Validavion que sea un Double 
			try {
				DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Messages.getLocale());
				DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
				char sep =symbols.getDecimalSeparator();
				
				Number number = format.parse(dosisOptional.get());
				dosis = number.doubleValue();
				
				// y mayor a 0
				if (dosis >= 0.0) {
					System.out.println("Con locale: " + Messages.getLocale() + " el rinde es: " + dosis);
					inputIsValid = true;
					return dosis;
				}
				else {
					throw new ParseException(null, sep);
				}
			}
			catch (ParseException e) {
				Alert inputFieldAlert = new Alert(AlertType.INFORMATION,Messages.getString("JFXMain.IngreseNumValido")); 
				inputFieldAlert.showAndWait();
			}
		}
		return dosis;
    }
	
}




//Validacion de input correcto de dosis
		