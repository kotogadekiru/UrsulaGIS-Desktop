package com.ursulagis.desktop.tasks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.api.StandardResponse;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.utils.DAH;


public class UpdateRecorridaTask extends Task<String> {


	//private static final String GET_RECORRIDAS_BY_ID_URL = "https://www.ursulagis.com/api/recorridas/id/";
	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";

	//public static final String BASE_URL="https://sheltered-mesa-69562-dev-514e4d674053.herokuapp.com/";
	//public static final String BASE_URL="http://localhost:5000/";
	public static final String BASE_URL="https://www.ursulagis.com";
	private static final String API_RECORRIDAS_UUID = "/api/recorridas/uuid/";
	//public static final String DOWNLOAD_URL = BASE_URL+API_RECORRIDAS_UUID;
	//public static final String INSERT_URL = "http://localhost:5000/api/recorridas/insert/";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;


	private Recorrida recorrida =null;


	public UpdateRecorridaTask(Recorrida recorrida) {
		this.recorrida = recorrida;

		System.out.println("actualizando recorrida "+recorrida);
		System.out.println("muestras "+recorrida.getMuestras().size());
	}

	@Override
	protected String call()  {
		try {
			// TODO call www.ursulagis.com/api/recorridas/insert/
			String baseUlr = getBaseUrl();

			GenericUrl url = new GenericUrl(baseUlr+API_RECORRIDAS_UUID+this.recorrida.getUuid());	

			HttpResponse response = makeGetRequest(url);
			InputStream resContent = response.getContent();
			Reader reader = new InputStreamReader(resContent);

			StandardResponse standarResponse =  new Gson().fromJson(reader, StandardResponse.class);
			System.out.println("standarResponse = "+standarResponse);

			StandardResponse.StatusResponse status = standarResponse.getStatus();
			System.out.println("response status = "+status);
			if(StandardResponse.StatusResponse.SUCCESS.equals(status)) {
				//com.google.api.client.util.ArrayMap data =(ArrayMap) resContent.get("data");
				JsonElement data = standarResponse.getData();

				if(data !=null) {
					Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies( getJSonStrategy()).create();
					Recorrida dbRecorrida = gson.fromJson(data, Recorrida.class);

					String dbUrl = dbRecorrida.getUrl();
					recorrida.setUrl(dbUrl);
					recorrida.setNombre(dbRecorrida.getNombre());
					recorrida.setJsonAmb(dbRecorrida.getJsonAmb());
					recorrida.setLatitude(dbRecorrida.getLatitude());
					recorrida.setLongitude(dbRecorrida.getLongitude());
					recorrida.setObservacion(dbRecorrida.getObservacion());
					//FIXME las muestras de dbRecorrida no estan en la base de datos
					recorrida.setMuestras(dbRecorrida.getMuestras());
					//TODO asignar dbRecorrida a this.recorrida
					//DAH.save(this.recorrida);// se guarda en el controller

					String urlGoto =dbUrl;// GET_RECORRIDAS_BY_ID_URL+id+"/";
					return urlGoto;
				}
				return "status Success but data null";
			} else {//status is not Success
				String message =standarResponse.getMessage();
				return status+" "+message;
			}
		}catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public String getBaseUrl() {
		String baseUlr=BASE_URL;
		try {
			URL requestUrl = new URL(recorrida.getUrl());			
			String portString = requestUrl.getPort() == -1 ? "" : ":" + requestUrl.getPort();
			baseUlr = requestUrl.getProtocol() + "://" + requestUrl.getHost() +portString;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return baseUlr;
	}

	private ExclusionStrategy getJSonStrategy() {
		ExclusionStrategy strategy = new ExclusionStrategy() {
			@Override
			public boolean shouldSkipClass(Class<?> arg0) {
				return false;
			}
			@Override
			public boolean shouldSkipField(FieldAttributes arg0) {
				if (arg0.getAnnotation(ManyToOne.class) != null)return true;//no subo referencias circulares
				if (arg0.getAnnotation(Id.class) != null)return true;//no subo las ids locales al servidor

				return false;
			}
		};
		return strategy;
	}


	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private static HttpResponse makeGetRequest(GenericUrl url){
		System.out.println("calling get "+url);
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
					}
				});

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			response= request.execute();
		} catch (Exception e) {			
			System.err.println("Fallo el getUrl "+url);
			e.printStackTrace();

			return null;
		}	
		return response;
	}



	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private HttpResponse makePostRequest(GenericUrl url,HttpContent req_content){
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
						HttpHeaders headers = request.getHeaders();//USER=693,468
						headers.set("USER", Configuracion.getInstance().getPropertyOrDefault("USER", "nonefound"));


					}
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);//(url);
			//request.getHeaders().set("USER", getUser());
			response= request.execute();
		} catch (Exception e) {			
			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
		}	
		return response;
	}


	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Compartiendo Recorrida "+this.recorrida.getNombre());
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
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
