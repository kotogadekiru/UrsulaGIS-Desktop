package com.ursulagis.desktop.tasks;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.io.InputStream;

public abstract class ProgresibleTask<E> extends Task<E>{
	
	private static final String TASK_CLOSE_ICON = "gui/event-close.png";
	//public static final String ZOOM_TO_KEY = "ZOOM_TO";
	
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;
	
	protected String taskName="";
	/**
	 * cantidad de features a procesar
	 */
	protected int featureCount=0;
	/**
	 * cantidad de features procesadas
	 */
	protected int featureNumber=0;
	

	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}

	/**
	 * Metodo que checkea si se cancelo el task y tira una excepcion para cerrar el thread
	 * Llamar a este metodo periodicamente dentro de los loops largos para permitir cortar el proceso
	 * @throws InterruptedException
	 */
	protected void checkCancelled() throws InterruptedException {
		if(this.isCancelled()) {
			throw new InterruptedException("User Cancelled Exception");
		}
	}

	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label(taskName);
		progressBarLabel.setTextFill(Color.BLACK);
		progressBarLabel.textProperty().bind(this.titleProperty());


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		try {
			System.out.println("Attempting to load image: " + TASK_CLOSE_ICON);
			InputStream imageStream = getClass().getClassLoader().getResourceAsStream(TASK_CLOSE_ICON);
			if (imageStream != null) {
				System.out.println("Successfully found image resource: " + TASK_CLOSE_ICON);
				Image imageDecline = new Image(imageStream);
				cancel.setGraphic(new ImageView(imageDecline));
			} else {
				System.err.println("Could not find image resource: " + TASK_CLOSE_ICON);
				cancel.setText("X");
			}
		} catch (Exception e) {
			System.err.println("Error loading image: " + e.getMessage());
			e.printStackTrace();
			cancel.setText("X");
		}

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}

}
