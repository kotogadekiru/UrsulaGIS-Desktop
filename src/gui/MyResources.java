package gui;

import java.util.ListResourceBundle;

public class MyResources extends ListResourceBundle {

	@Override
	protected Object[][] getContents() {
		
		return new Object[][] {
            // LOCALIZE THE SECOND STRING OF EACH ARRAY (e.g., "OK")
            {"OkKey", "OK"},
            {"CancelKey", "Cancel"},
            {"JFXMain.importar","Importar"},
            // END OF MATERIAL TO LOCALIZE
       };
	}

}
