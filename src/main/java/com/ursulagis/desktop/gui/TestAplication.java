package com.ursulagis.desktop.gui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import javax.swing.JOptionPane;

import com.ursulagis.desktop.api.OrdenPulverizacion;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class TestAplication extends Application {
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Hola Mundo");
		StackPane pane = new StackPane();
		Button b = new Button("clicky");
		b.setOnAction((action)->{
			System.out.println("clicked");
			//HarvestConfigDialogController.config(new CosechaLabor()); 
			//XXX no olvidarse de setear el controller en el fxml		
			OrdenPulverizacionPaneController.config(new OrdenPulverizacion());
			
			//myLoader.setResources(Messages.getBoundle());
		
		});
		pane.getChildren().add(b);
		Scene scene = new Scene(pane,800,600);//, Color.White);
		primaryStage.setScene(scene);
		primaryStage.show();
	//	Optional<OrdenPulverizacion> retOp = OrdenPulverizacionPaneController.config(new OrdenPulverizacion());				
	}	

	public static void main(String[] args) {	
		try	{
			System.setProperty("prism.order", "es2");
			Application.launch(TestAplication.class, args);
		}catch (Exception e){
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,"hola: " + e.getMessage());
			try	{
				PrintWriter pw = new PrintWriter(new File("ursula_error.log"));
				e.printStackTrace(pw);
				pw.close();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}	
	}


}
