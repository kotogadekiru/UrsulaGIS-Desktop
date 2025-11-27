package com.ursulagis.desktop.tasks;

import com.ursulagis.desktop.dao.recorrida.Recorrida;
import gov.nasa.worldwind.layers.RenderableLayer;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.tasks.procesar.GenerarRecorridaDirigidaTask;

public class ShowRecorridaDirigidaTask extends Task<RenderableLayer> {


	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";
//	int MAX_URL_LENGHT = 4443;//2048 segun un stackoverflow //4443 segun pruevas con chrome// corresponde a 129 puntos
//	protected int featureCount=0;
//	protected int featureNumber=0;
//	protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();

	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;
	
	/**
	 * la lista de las cosechas a unir
	 */
	
	
	
	//private double cantidadMaximaDeMuestrasTotal=Double.MAX_VALUE;
	
	
	
	private Recorrida recorrida =null;

	public ShowRecorridaDirigidaTask(Recorrida recorrida) {
		this.recorrida=recorrida;
		}

	/**
	 *Proceso que genera una lista de puntos al azar dentro de cada zona de acuerdo a la frecuencia minima especificada
	 */
	@Override
	public RenderableLayer call()  {		
		RenderableLayer layer = new RenderableLayer();		
		GenerarRecorridaDirigidaTask.renderRecorrida(layer,recorrida);		
		return layer;
	}



	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Recorrida "+this.recorrida.getNombre());
		progressBarLabel.setTextFill(Color.BLACK);

		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("Cancelando GenerarRecorridaDirigidaTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(MMG_GUI_EVENT_CLOSE_PNG));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}
	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}


}

