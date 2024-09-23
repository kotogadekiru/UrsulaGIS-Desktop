package tasks;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

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
import com.vividsolutions.jts.geom.Geometry;

import api.OrdenSiembra;
import api.OrdenSiembraItem;
import api.StandardResponse;
import dao.LaborItem;
import dao.Poligono;
import dao.config.Configuracion;
import dao.config.Cultivo;
import dao.config.Semilla;
import dao.ordenCompra.Producto;
import dao.siembra.SiembraLabor;
import gui.Messages;
import gui.OrdenSiembraPaneController;
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
import tasks.procesar.ExportarPrescripcionSiembraTask;
import utils.DAH;
import utils.FileHelper;
import utils.GeometryHelper;
import utils.JsonUtil;
import utils.ProyectionConstants;
import utils.TarjetaHelper;
import utils.UnzipUtility;

public class CompartirSiembraLaborTask extends Task<String> {
	//private static final String GET_RECORRIDAS_BY_ID_URL = "https://www.ursulagis.com/api/recorridas/id/";
	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";
	public static final String BASE_URL = "https://www.ursulagis.com";
	//public static final String BASE_URL = "http://localhost:5000";
	//public static final String BASE_URL = "https://sheltered-mesa-69562-dev-514e4d674053.herokuapp.com";
	public static final String INSERT_URL = BASE_URL+"/api/orden_siembra/insert/";
	
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	private SiembraLabor siembraLabor = null;
	private OrdenSiembra ordenSiembra = null;

	public CompartirSiembraLaborTask(SiembraLabor siembraLabor,OrdenSiembra orden) {
		this.siembraLabor = siembraLabor;
		this.ordenSiembra = orden;
		System.out.println("compartiendo SiembraLabor "+siembraLabor);
		//System.out.println("items "+siembraLabor.getItems().size());
	}

	@Override
	protected String call()  {
		this.updateProgress(0, 10);
		//TODO exportar prescripcion!
		String ordenUrl = uploadLaborFile(this.siembraLabor);
		this.ordenSiembra.setOrdenShpZipUrl(ordenUrl);
		this.updateProgress(1, 10);
		try {
			// TODO call www.ursulagis.com/api/recorridas/insert/
			GenericUrl url = new GenericUrl(INSERT_URL);	

			Gson gson = getGson();

			System.out.println("convirtiendo SiembraLabor a json "+this.ordenSiembra);
			String json_body = gson.toJson(this.ordenSiembra, OrdenSiembra.class);
			System.out.println("sending SiembraLabor "+ json_body);

			final HttpContent content = new ByteArrayContent("application/json", json_body.getBytes("UTF8") );

			this.updateProgress(2, 10);
			HttpResponse response = makePostRequest(url,content);
			this.updateProgress(3, 10);
			InputStream resContent = response.getContent();
			Reader reader = new InputStreamReader(resContent);
			this.updateProgress(4, 10);
			StandardResponse standarResponse =  new Gson().fromJson(reader, StandardResponse.class);
			System.out.println("standarResponse = "+standarResponse);
			this.updateProgress(5, 10);
			//StandardResponse standarResponse = response.parseAs(StandardResponse.class);
			//Recorrida r = new Gson().fromJson((String) resContent.get("data"), Recorrida.class);
			StandardResponse.StatusResponse status = standarResponse.getStatus();
			System.out.println("response status = "+status);
			if(StandardResponse.StatusResponse.SUCCESS.equals(status)) {
				this.updateProgress(6, 10);
				//com.google.api.client.util.ArrayMap data =(ArrayMap) resContent.get("data");
				JsonElement data = standarResponse.getData();
				//Map<String,String> message = (Map<String, String>) resContent.get("data");
				//System.out.println("message "+message);
				if(data !=null) {
					this.updateProgress(7, 10);
					OrdenSiembra dbSiembraLabor = gson.fromJson(data, OrdenSiembra.class);
					//DAH.save(dbSiembraLabor);//merge local recorrida
					//	Long id = dbRecorrida.getId();
					String dbUrl = dbSiembraLabor.getUrl();
					this.ordenSiembra.setUrl(dbUrl);
					this.updateProgress(8, 10);
					System.out.println("guardando siembra");
					long ini = System.currentTimeMillis();
					DAH.save(ordenSiembra);
					long time = System.currentTimeMillis()-ini;
					System.out.println("guarde siembra en "+time+"ms");
					this.updateProgress(9, 10);
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
					String urlGoto = dbUrl;// GET_RECORRIDAS_BY_ID_URL+id+"/";
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

	public static OrdenSiembra constructOrdenSiembra(SiembraLabor siembra) {	
		OrdenSiembra orden = new OrdenSiembra();
		orden.setNombre(siembra.getNombre());
		Locale loc = new Locale("en", "US");
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, loc);
		orden.setFecha(dateFormat.format(siembra.getFecha()));		
		orden.setSuperficie(Messages.getNumberFormat().format(siembra.getCantidadLabor()));
		
		
		/*   viene de orden compra  */
		Semilla producto =siembra.getSemilla();
		
		Cultivo cultivo = producto.getCultivo();
		Double semBolsa = cultivo.getSemPorBolsa();
		double kgBolsa=1;
		if(semBolsa!=null && semBolsa>0) {
			Double pMil = producto.getPesoDeMil();
			kgBolsa=semBolsa*pMil/1000000;			
		}
		
		double fertCTotal=0;
		double fertLTotal=0;
		double insumoTotal=0;
		double laborTotal=0;
		SimpleFeatureIterator it = siembra.outCollection.features();
		//fertCTotal =  siembra.getCantidadFertilizanteCostado();
		//fertLTotal = siembra.getCantidadFertilizanteLinea();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			Double rinde = LaborItem.getDoubleFromObj(f.getAttribute(siembra.colAmount.get()));//labor.colAmount.get()
			Double fertL = LaborItem.getDoubleFromObj(f.getAttribute(SiembraLabor.COLUMNA_DOSIS_LINEA));
			Double fertC = LaborItem.getDoubleFromObj(f.getAttribute(SiembraLabor.COLUMNA_DOSIS_COSTADO));
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Double area = ProyectionConstants.A_HAS(geometry.getArea());			
			insumoTotal+=rinde*area;
			fertLTotal+=fertL*area;
			fertCTotal+=fertC*area;
			
			laborTotal+=area;
		}
		it.close();			
		//putItem(prodCantidadMap, siembra.getProductoLabor(), laborTotal,siembra.getPrecioLabor());
		OrdenSiembraItem itemLabor = new OrdenSiembraItem();
		itemLabor.setProducto(siembra.getProductoLabor());		
		itemLabor.setCantidad(laborTotal);
		itemLabor.setDosisHa(1.0);
		orden.getItems().add(itemLabor);
		
		NumberFormat nf = Messages.getNumberFormat();
		//putItem(prodCantidadMap, producto, insumoTotal/kgBolsa,siembra.getPrecioInsumo());
		OrdenSiembraItem itemSemilla = new OrdenSiembraItem();
		itemSemilla.setProducto(producto);		
		itemSemilla.setCantidad(insumoTotal/kgBolsa);//kgBolsa no es cero
		itemSemilla.setDosisHa(itemSemilla.getCantidad()/laborTotal);
		itemSemilla.setObservaciones("Cantidad en bolsas de "
									+nf.format(semBolsa)+" semilas y "
									+nf.format(kgBolsa)+"Kg");
		orden.getItems().add(itemSemilla);
		
		//putItem(prodCantidadMap, siembra.getFertLinea(), fertLTotal,0.0);
		OrdenSiembraItem itemFertL = new OrdenSiembraItem();
		itemFertL.setProducto(siembra.getFertLinea());		
		//TODO fix no fertLineaProducto
		itemFertL.setCantidad(fertLTotal);//kgBolsa no es cero	
		itemFertL.setDosisHa(fertLTotal/laborTotal);
		if(fertLTotal>0) {
			orden.getItems().add(itemFertL);
		}
		
		//putItem(prodCantidadMap, siembra.getFertCostado(), fertCTotal,0.0);
		OrdenSiembraItem itemFertC = new OrdenSiembraItem();
		itemFertC.setProducto(siembra.getFertCostado());		
		itemFertC.setCantidad(fertCTotal);
		itemFertC.setDosisHa(fertCTotal/laborTotal);
		if(fertCTotal>0) {
			orden.getItems().add(itemFertC);
		}
		
		/**/
/*
		OrdenSiembraItem itemSemilla = new OrdenSiembraItem();
		Double has =siembra.getCantidadLabor();		
		System.out.println("cantidad labor oc= "+has);
		itemSemilla.setProducto(siembra.getSemilla());
		itemSemilla.setCantidad(siembra.getCantidadInsumo());				
		if(has>0) {
			itemSemilla.setDosisHa(itemSemilla.getCantidad()/has);
		}				
		orden.getItems().add(itemSemilla);

		OrdenSiembraItem costado = new OrdenSiembraItem();								
		costado.setProducto(siembra.getFertLinea());
		costado.setCantidad(siembra.getCantidadFertilizanteLinea());			
		if(has>0) {
			costado.setDosisHa(itemSemilla.getCantidad()/has);
		}
		if(costado.getProducto()!=null) {
			orden.getItems().add(costado);				
		}					

		OrdenSiembraItem linea = new OrdenSiembraItem();								
		linea.setProducto(siembra.getFertLinea());
		linea.setCantidad(siembra.getCantidadFertilizanteLinea());				
		if(has>0) {
			linea.setDosisHa(itemSemilla.getCantidad()/has);
		}				
		if(linea.getProducto()!=null) {
			orden.getItems().add(linea);				
		}
*/

	
		Optional<OrdenSiembra> retOp = OrdenSiembraPaneController.config(orden);
		
		if(retOp.isPresent()) {
			OrdenSiembra ret = retOp.get();
			Platform.runLater(()->{				
				Geometry contornoG = GeometryHelper.extractContornoGeometry(siembra);
				
				System.out.println("contorno siembra es "+contornoG.toText());
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
	 * @param pl la labor de siembra
	 * @return la url de destino en la nube de la labor
	 */
	private String uploadLaborFile(SiembraLabor pl) {
		//TODO exportar siembra a prescripcion
		File zipFile = zipLaborToTmpDir(pl);//ok funciona
		//subir el zipFile a la tarjeta del usuario
		TarjetaHelper.uploadFile(zipFile, "/labores");
		return "/labores/"+zipFile.getName();
	}

	private File zipLaborToTmpDir(SiembraLabor labor) {
		//1 crear un directorio temporal
		Path dir = FileHelper.createTempDir("toUpload");
		//2 crear un archivo shape dentro del directorio para subir
		File shpFile = FileHelper.getNewShapeFileAt(dir,"labor.shp");
		//2 exportar la labor al directorio
		ExportarPrescripcionSiembraTask export = new ExportarPrescripcionSiembraTask(labor,shpFile,ordenSiembra.getUnidad());
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
					System.out.println("buscando producto local con nombre "+productoNombre);
					Producto p = DAH.findProducto(productoNombre);
					System.out.println("encontre "+p);
					//javax.persistence.NoResultException: getSingleResult() did not retrieve any entities.
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
		progressBarLabel = new Label("Compartiendo SiembraLabor "+this.siembraLabor.getNombre());
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
