package com.ursulagis.desktop.tasks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.ordenCompra.OrdenCompra;
import com.ursulagis.desktop.dao.ordenCompra.Producto;
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
import com.ursulagis.desktop.utils.JsonUtil;


public class CotizarOdenDeCompraOnlineTask extends Task<String> {

	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";

	public static final String BASE_URL = "https://www.ursulagis.com";
	//public static final String BASE_URL = "http://localhost:5000";
	public static final String INSERT_URL = BASE_URL+"/api/orden_compra/insert/";
	//public static final String INSERT_URL = "http://localhost:5000/api/recorridas/insert/";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;


	private OrdenCompra ordenCompra =null;	

	public CotizarOdenDeCompraOnlineTask(OrdenCompra orden) {
		this.ordenCompra = orden;		
		System.out.println("compartiendo OrdenCompra "+orden);
		System.out.println("items "+orden.getItems().size());
	}

	@Override
	protected String call()  {
		try {
			// TODO call www.ursulagis.com/api/recorridas/insert/
			GenericUrl url = new GenericUrl(INSERT_URL);	

			Gson gson = getGson();//new GsonBuilder().serializeNulls().setExclusionStrategies( getJSonStrategy()).create();
		
			System.out.println("convirtiendo OrdenCompra a json "+ordenCompra);

			String json_body = gson.toJson(ordenCompra, OrdenCompra.class);
			System.out.println("sending OrdenCompra "+ json_body);			

			final HttpContent content = new ByteArrayContent("application/json", json_body.getBytes("UTF8") );

			//final HttpContent req_content = new JsonHttpContent(new JacksonFactory(), content);

			HttpResponse response = makePostRequest(url,content);
			InputStream resContent = response.getContent();
			Reader reader = new InputStreamReader(resContent);

			StandardResponse standarResponse =  new Gson().fromJson(reader, StandardResponse.class);
			System.out.println("standarResponse = "+standarResponse);
				
			StandardResponse.StatusResponse status = standarResponse.getStatus();
			System.out.println("response status = "+status);
			if(StandardResponse.StatusResponse.SUCCESS.equals(status)) {				
				JsonElement data = standarResponse.getData();
				System.out.println("orden compra subida correctamente");
				if(data !=null) {
					// Failed to invoke public dao.OrdenDeCompra.Producto() with no args
					//FIXME cambiar estrategia de producto a JoinedTable y crear un descerializador que convierta de producto a productoGenerico o fertilizacion etc.
					OrdenCompra dbOrdenCompra = gson.fromJson(data, OrdenCompra.class);
					//Long id = dbOrdenCompra.getId();
					//DAH.save(dbOrdenCompra);//merge local recorrida
					//	Long id = dbRecorrida.getId();
					String dbUrl = dbOrdenCompra.getUrl();
					this.ordenCompra.setUrl(dbUrl);
					DAH.save(this.ordenCompra);
					String urlGoto =dbUrl;// "https://www.ursulagis.com/api/orden_compra/id/"+id+"/";
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

	public static  JsonDeserializer<Producto> getProductoDeserializer() {
		return new JsonDeserializer<Producto>() {

			@Override
			public Producto deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				System.out.println("des serializando "+json+" type "+typeOfT);
				/*
				des serializando {"nombre":"Urea","porcN":46.0,"porcP":0.0,"porcK":0.0,"porcS":0.0,"id":29954} type class models.OrdenDeCompra.Producto
				des serializando {"nombre":"Labor de Fertilizacion","id":8553} type class models.OrdenDeCompra.Producto
				 * */
				String productoNombre = json.getAsJsonObject().get("nombre").getAsString();
			try {
				Producto p = DAH.findProducto(productoNombre);
				return p;
			}catch(Exception e) {
				e.printStackTrace();
				return null;
			}
//				if(p==null) {
//					p=new Producto(productoNombre);
//					DAH.save(p);
//				}
				

			}
			
		};
	}
	public static Gson getGson() {
		return new GsonBuilder()
				.registerTypeAdapter(Producto.class, getProductoDeserializer())
				  .addSerializationExclusionStrategy(getJSonStrategy())
				  .create();
	}
	
	private static ExclusionStrategy getJSonStrategy() {
		ExclusionStrategy strategy = new ExclusionStrategy() {
			@Override
			public boolean shouldSkipClass(Class<?> arg0) {
				return false;
			}
			@Override
			public boolean shouldSkipField(FieldAttributes arg0) {
				if (arg0.getAnnotation(JsonUtil.Exclude.class) != null)return true;

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
	private HttpResponse makeGetRequest(GenericUrl url,HttpContent req_content){
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
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);//(url);
			response= request.execute();
		} catch (Exception e) {			
			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
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
		progressBarLabel = new Label("Compartiendo Recorrida "+this.ordenCompra.getDescription());
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
