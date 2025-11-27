package com.ursulagis.desktop.gui.utils;

import java.util.Optional;

import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.JFXMain;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;

public class NumberInputDialog {
    private static Double value = 0.0;
    /**
     * 
     * @param title titulo del dialog
     * @param header texto que aparece arriba describiendo la accion a realizar
     * @param label nombre de la variable a ingresar
     * @param prompt valor por defecto de la variable
     * @param tooltip mensaje de error que se muestra si se uso mal el separador de decimales
     * @return el Double ingresado por el usuario
     */
    public static Double showAndWait(String title, String header, String label, String prompt, String tooltip) {
    	boolean inputIsValid = false;
		
		TextInputDialog dialog = new TextInputDialog(prompt);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(label);
		dialog.initOwner(JFXMain.stage);
		
		// Tooltip
		TextField textField = (TextField) dialog.getEditor();
        Tooltip tooltipText = new Tooltip(tooltip);
        textField.setTooltip(tooltipText);
		
		while(!inputIsValid) {
			Optional<String> result = dialog.showAndWait();
			// Validavion que sea un Double  
			if (result.isPresent()) {					
					Number number = PropertyHelper.parseDouble(result.get()) ;
					value = number.doubleValue();
					
					inputIsValid = true;
					return value;
					
//				}catch (ParseException e) {
//					Alert inputFieldAlert = new Alert(AlertType.INFORMATION,Messages.getString("JFXMain.IngreseNumValido")); 
//					inputFieldAlert.showAndWait();
//				}
			}else {
				return Double.NaN;
			}
		}
		return value;
    }
	
}
		