package com.ursulagis.desktop.gui;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ApplicationTest  extends Application{

	@Override
	public void start(Stage primaryStage) throws Exception {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setContentText("application funcniona");
		a.show();
		
	}
	public static void main(String[] args) {
		System.out.println("Starting main app");
		System.setProperty("prism.order", "es2");
		
		Application.launch(ApplicationTest.class, args);//si comento application anda
		//Application.launch(JFXMain.class, args);//si comento application anda
	}

}
