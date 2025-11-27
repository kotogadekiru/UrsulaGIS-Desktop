package com.ursulagis.desktop.tasks;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Optional;

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
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.api.OrdenCosecha;
import com.ursulagis.desktop.api.OrdenPulverizacion;
import com.ursulagis.desktop.api.OrdenPulverizacionItem;
import com.ursulagis.desktop.api.StandardResponse;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.ordenCompra.Producto;
import com.ursulagis.desktop.dao.pulverizacion.CaldoItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.OrdenPulverizacionPaneController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.tasks.procesar.ExportarPrescripcionPulverizacionTask;
import com.ursulagis.desktop.tasks.procesar.ExportarPrescripcionSiembraTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.FileHelper;
import com.ursulagis.desktop.utils.GeometryHelper;
import com.ursulagis.desktop.utils.JsonUtil;
import com.ursulagis.desktop.utils.TarjetaHelper;
import com.ursulagis.desktop.utils.UnzipUtility;


public class CompartirPulverizacionLaborTask extends Task<String> {

	//private static final String GET_RECORRIDAS_BY_ID_URL = "https://www.ursulagis.com/api/recorridas/id/";
	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";

	public static final String INSERT_URL = "https://www.ursulagis.com/api/orden_pulverizacion/insert/";
	//public static final String INSERT_URL = "http://localhost:5000/api/recorridas/insert/";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;


	private PulverizacionLabor pulverizacionLabor =null;
	private OrdenPulverizacion ordenPulverizacion;


	public CompartirPulverizacionLaborTask(PulverizacionLabor pulverizacionLabor,OrdenPulverizacion ordenPulv) {
		this.pulverizacionLabor = pulverizacionLabor;
		this.ordenPulverizacion = ordenPulv;
		System.out.println("compartiendo PulverizacionLabor "+pulverizacionLabor);
		System.out.println("items "+pulverizacionLabor.getItems().size());
	}

	@Override
	protected String call()  {
		this.updateProgress(0, 10);
		//OrdenPulverizacion ordenPulv = constructOrdenPulverizacion(this.pulverizacionLabor);
		String ordenUrl = uploadLaborFile(this.pulverizacionLabor);
		this.ordenPulverizacion.setOrdenShpZipUrl(ordenUrl);
		this.updateProgress(1, 10);
		try {
			// TODO call www.ursulagis.com/api/recorridas/insert/
			GenericUrl url = new GenericUrl(INSERT_URL);	

			Gson gson = getGson();

			System.out.println("convirtirndo PulverizacionLabor a json "+this.ordenPulverizacion);
			String json_body = gson.toJson(this.ordenPulverizacion, OrdenPulverizacion.class);
			System.out.println("sending PulverizacionLabor "+ json_body);

			final HttpContent content = new ByteArrayContent("application/json", json_body.getBytes("UTF8") );

			this.updateProgress(2, 10);
			HttpResponse response = makePostRequest(url,content);
			this.updateProgress(3, 10);
			InputStream resContent = response.getContent();
			Reader reader = new InputStreamReader(resContent);

			StandardResponse standarResponse =  new Gson().fromJson(reader, StandardResponse.class);
			System.out.println("standarResponse = "+standarResponse);
			//StandardResponse standarResponse = response.parseAs(StandardResponse.class);
			//Recorrida r = new Gson().fromJson((String) resContent.get("data"), Recorrida.class);
			StandardResponse.StatusResponse status = standarResponse.getStatus();
			System.out.println("response status = "+status);
			if(StandardResponse.StatusResponse.SUCCESS.equals(status)) {
				//com.google.api.client.util.ArrayMap data =(ArrayMap) resContent.get("data");
				JsonElement data = standarResponse.getData();
				//Map<String,String> message = (Map<String, String>) resContent.get("data");
				//System.out.println("message "+message);
				if(data !=null) {
					OrdenPulverizacion dbPulverizacionLabor = gson.fromJson(data, OrdenPulverizacion.class);
					//DAH.save(dbPulverizacionLabor);//merge local recorrida
					//	Long id = dbRecorrida.getId();
					String dbUrl = dbPulverizacionLabor.getUrl();
					this.ordenPulverizacion.setUrl(dbUrl);
					DAH.save(ordenPulverizacion);
					//java.math.BigDecimal id = (java.math.BigDecimal) data.get("id");
					//String prettyresponse = resContent.toPrettyString();
					//System.out.println("prettyresponse "+prettyresponse);

					/*
			prettyresponse {
		  "status" : "SUCCESS",
		  "data" : {
		    "id" : 4,
		    "nombre" : "",
		    "observacion" : "",
		    "posicion" : "",
		    "latitude" : 0.0,
		    "longitude" : 0.0,
		    "muestras" : [ ]
		  }
		}
					 */
					//String urlGoto = "https://www.ursulagis.com/api/recorridas/4/";
					//TODO cambiar esta url por una url mobile que permita hacer la recorrida via web.
					String urlGoto =dbUrl;// GET_RECORRIDAS_BY_ID_URL+id+"/";
					this.updateProgress(10, 10);
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

	public static OrdenPulverizacion constructOrdenPulverizacion(PulverizacionLabor pl) {	
				OrdenPulverizacion op = new OrdenPulverizacion();
				op.setNombre(pl.getNombre());
				Locale loc = new Locale("en", "US");
				DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, loc);
				op.setFecha(dateFormat.format(pl.getFecha()));		
				op.setSuperficie(Messages.getNumberFormat().format(pl.getCantidadLabor()));
				for(CaldoItem item:pl.getItems()) {//.forEach(item->{
					OrdenPulverizacionItem i = new OrdenPulverizacionItem();
					i.setDosisHa(item.getDosisHa());
					i.setProducto(item.getProducto());
					// Seteo cantidad a 1 por si no se cargo el insumo bien
					if(i.getDosisHa()*pl.getCantidadInsumo() < 1) {
						i.setCantidad(1.0);
					}
					else {
						i.setCantidad(i.getDosisHa()*pl.getCantidadInsumo());
					}
					//System.out.println("seteando cantidad item "+i.getCantidad());
					i.setObservaciones(item.getObservaciones());
					//System.out.println("seteando observaciones item "+item.getObservaciones());

					op.getItems().add(i);
				}//);
		
				Optional<OrdenPulverizacion> retOp = OrdenPulverizacionPaneController.config(op);
	

				if(retOp.isPresent()) {
					OrdenPulverizacion ret = retOp.get();
					Platform.runLater(()->{
						
						Geometry contornoG = GeometryHelper.extractContornoGeometry(pl);
						Poligono contornoP =GeometryHelper.constructPoligono(contornoG);
						if(contornoP!=null) {
							ret.setPoligonoString(contornoP.getPositionsString());
						} else {
							System.out.println("no se pudo extraer el contorno de la cosecha");
						}
					});
					return ret;
				} else {return null;}
		
	}

	/**
	 * metodo que comprime la labor, la sube a la nube, y devuelve la url
	 * @param pl la labor de pulverizacion
	 * @return la url de destino en la nube de la labor
	 */
	private String uploadLaborFile(PulverizacionLabor pl) {
		File zipFile = zipLaborToTmpDir(pl);//ok funciona
		//TODO subir el zipFile a la tarjeta del usuario
		TarjetaHelper.uploadFile(zipFile, "/labores");
		return "/labores/"+zipFile.getName();
	}

	private File zipLaborToTmpDir(PulverizacionLabor labor) {
		//1 crear un directorio temporal
		Path dir = FileHelper.createTempDir("toUpload");
		//2 crear un archivo shape dentro del directorio para subir
		File shpFile = FileHelper.getNewShapeFileAt(dir,"labor.shp");
		//2 exportar la labor al directorio
		ExportarPrescripcionPulverizacionTask export = new ExportarPrescripcionPulverizacionTask(labor,shpFile);
		export.guardarConfig=false;//como es un temp dir no quiero guardar LAST_FILE
		export.call();
		File zipFile = UnzipUtility.zipFiles(FileHelper.selectAllFiles(dir),dir.toFile());
		return zipFile;
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
		progressBarLabel = new Label("Compartiendo PulverizacionLabor "+this.pulverizacionLabor.getNombre());
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
