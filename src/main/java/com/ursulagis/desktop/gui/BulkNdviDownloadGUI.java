package com.ursulagis.desktop.gui;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
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
import org.locationtech.jts.geom.Point;

import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.gui.utils.SmartTableView;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.ursulagis.desktop.tasks.GetNdviForLaborTask4;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.ExcelHelper;

public class BulkNdviDownloadGUI {
	private static final int MAX_TRYS = 3;
	private static int MAX_DATES_PER_QUERY = 4;//con 20 tarda 7.3 min para mes para 208 poligonos//con 40 en 2 poligonos 6 meses tira service unavailable 503; con 20 y 2 poli 6 meses parece andar bien
	private static final String YYYY_MM_DD = "yyyy-MM-dd";
	private static final String URSULA_GIS_TOKEN = "ursulaGIS"+JFXMain.VERSION;//"ursulaGISv23";
	private static final String TOKEN = "token";
	private static final String END = "end";
	private static final String BEGIN = "begin"; //Data availability (time)	Jun 23, 2015 - Apr 18, 2017
	private static final String PATH2 = "path2";
	private static final String DATA = "data";

	private static final String BASE_URL = "http://gee-api-helper.herokuapp.com";
	private static final String HTTP_GEE_API_HELPER_HEROKUAPP_COM_NDVI_V3 = BASE_URL+"/ndvi_mean_v4";//"/ndvi_v4";//+"/gndvi_v4_SR";//"/ndvi_v3";//ndvi_v5

	private static final String GEE_POLYGONS_GET_REQUEST_KEY = "polygons";

	ObservableList<Poligono> availablePoligons =null;
	ObservableList<Ndvi> availableNdvi =null;
	ObservableList<Poligono> selected =null;
	DateTimeFormatter format1 = DateTimeFormatter.ofPattern(YYYY_MM_DD);	
	
	//SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	DecimalFormat bdf = new DecimalFormat("#,###.00");

	Stage tablaStage = null;
			
	Map<String,Object[]> datesPoligonNdvi = new ArrayMap<String,Object[]>();
	private DoubleProperty progressProp;
	private LocalDate finalLd;
	private LocalDate startLd;
	private double cantImagenesADescargar=0.0;
	private int n = 0;

	public BulkNdviDownloadGUI() {
		availablePoligons =	FXCollections.observableArrayList( DAH.getAllPoligonos() );
		selected =	FXCollections.observableArrayList();
		availableNdvi = FXCollections.observableArrayList();
		
		bdf.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("EN")));
		
		String maxDates= JFXMain.config.getPropertyOrDefault("MAX_DATES_PER_QUERY", Integer.toString(MAX_DATES_PER_QUERY));
		if(maxDates==null) {
			maxDates=Integer.toString(MAX_DATES_PER_QUERY);
		}
		MAX_DATES_PER_QUERY=new Integer(maxDates).intValue();
		JFXMain.config.setProperty("MAX_DATES_PER_QUERY", Integer.toString(MAX_DATES_PER_QUERY));
		JFXMain.config.save();
	}

	public void show() {
			SmartTableView<Poligono> tabla1 = new SmartTableView<Poligono>(availablePoligons,	Arrays.asList("Id","PositionsString","PoligonoToString","Activo","Area"),
					Arrays.asList("Nombre"));
			tabla1.setEditable(false);

			tabla1.getSelectionModel().setSelectionMode(
					SelectionMode.MULTIPLE
					);

			SmartTableView<Poligono> tabla2 = new SmartTableView<Poligono>(selected,	
					Arrays.asList("Id","PositionsString","PoligonoToString","Activo","Area"),
					Arrays.asList("Nombre"));
			tabla2.setEditable(false);
			tabla2.getSelectionModel().setSelectionMode(
					SelectionMode.MULTIPLE
					);

			Button addSelectedButton= new Button();
			addSelectedButton.setMaxWidth(Double.MAX_VALUE);
		
			
			addSelectedButton.setText(Messages.getString("doBulkNDVIDownload.addSelectedButton")+" -->");
			addSelectedButton.setOnAction((ae)->{
				ObservableList<Poligono> list = tabla1.getSelectionModel().getSelectedItems();
				selected.addAll(list);
				availablePoligons.removeAll(list);
				tabla1.refresh();
				tabla2.refresh();
			});

			Button remSelectedButton= new Button();
			remSelectedButton.setMaxWidth(Double.MAX_VALUE);
			remSelectedButton.setText("<-- "+Messages.getString("doBulkNDVIDownload.remSelectedButton"));
			remSelectedButton.setOnAction((ae)->{
				//getSelected from tabla 1 and add them to selected list
				ObservableList<Poligono> list = tabla2.getSelectionModel().getSelectedItems();
				//	System.out.println("removing "+list.size()+" items");
				selected.removeAll(list);
				availablePoligons.addAll(list);//FIXME no los vuelve a poner en la lista
				tabla1.refresh();
				tabla2.refresh();
			});

			Button showNDVITablaButton= new Button();
			showNDVITablaButton.setMaxWidth(Double.MAX_VALUE);
			showNDVITablaButton.setText(Messages.getString("doBulkNDVIDownload.showNDVITablaButton"));
			

			tabla1.setPrefWidth(300);
			tabla2.setPrefWidth(300);

			BorderPane v2 = new BorderPane();
		
			v2.setPrefWidth(120);
			//v2.setTop(addSelectedButton);
			Label creditos = new Label("Esta funcionalidad fue desarrollada junto a RAVIT, \nRed Agropecuaria de Vigilancia Tecnol�gica\nTwitter: @ravitagro");
			 Image image = new Image(getClass().getResourceAsStream("ravit_logo400x400.jpg"));
			 ImageView iv = new ImageView(image);
			 iv.setPreserveRatio(true);
			 iv.setFitWidth(100);
			 
			//creditos.setGraphic(new ImageView(image));
			creditos.setWrapText(true);
			VBox v = new VBox(iv,addSelectedButton,remSelectedButton,showNDVITablaButton,creditos);
			v.setSpacing(10);
			v.setPadding(new Insets(0, 10, 10, 10)); 
			v.setAlignment(Pos.CENTER); 
			v2.setCenter(v);
			//v2.setBottom(showNDVITablaButton);

			SplitPane h = new SplitPane(tabla1,v2,tabla2);
			h.setDividerPositions(0.4,0.55);

			//	h.getChildren().addAll(tabla1,v2,tabla2);
			Scene scene = new Scene(h, 800, 600);
			h.setPrefSize(scene.getWidth(),scene.getHeight());
			tablaStage = new Stage();

			showNDVITablaButton.setOnAction((ae)->{
				//TODO make requests and show ndvi tabla 
				//System.out.println("downloading and showing ndvi values for "+selected.size()+" elements");	
				
				//tablaStage.close();
				showNdviValues();
				//availableNdvi.addAll(
				processSelectedPoligonsNDVI(selected);
			});
			
			//tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.381")); //$NON-NLS-1$
			tablaStage.setScene(scene);
			tablaStage.show();	 
			
		
	}
	
	public void showNdviValues() {
		
			SmartTableView<Ndvi> tabla1 = new SmartTableView<Ndvi>(availableNdvi,	Arrays.asList("Id","PositionsString","Activo","PixelArea"),
					Arrays.asList("Nombre"));
			tabla1.setEditable(false);

			tabla1.getSelectionModel().setSelectionMode(
					SelectionMode.SINGLE
					);
			
			tabla1.setOnShowClick((ndvi)->{
				//poli.setActivo(true);
				//TODO descargar el path y mostrarlo en el mapa
				//	doShowNDVI(ndvi);

			});
			
			Button export =new Button(Messages.getString("doBulkNDVIDownload.exportSelectedButton"));//"Exportar");
			export.setOnAction(a->{
				ExcelHelper h=new ExcelHelper();
				h.exportData("NDVI",crearSerie());
			});
			
			ProgressBar progressBarTask = new ProgressBar();	
			Tooltip t = new Tooltip();
			progressBarTask.progressProperty().addListener(c->{
				t.setText(availableNdvi.size()+" / "+(int)cantImagenesADescargar);
			});
			
			progressBarTask.setTooltip(t);
			progressBarTask.setProgress(0);

			this.progressProp = progressBarTask.progressProperty();
			
			VBox v = new VBox();
			VBox.setVgrow(tabla1, Priority.ALWAYS);
			v.getChildren().add(tabla1);
			
			BorderPane bp=new BorderPane();
		
		//	h.setPadding(new Insets(10, 0, 10, 0)); 
			
			BorderPane.setAlignment(progressBarTask,Pos.CENTER); 
			
		//	h.setAlignment(Pos.CENTER_LEFT);
			bp.setLeft(progressBarTask);
			bp.setRight(export);
			VBox.setVgrow(bp, Priority.NEVER);
			v.getChildren().add(bp);
			
			availableNdvi.addListener((Change<? extends Ndvi> c)->{
				//System.out.println("refreshing.."+availableNdvi.size());
				//System.out.println("progress = "+this.progressProp);
				
				//Platform.runLater(()->
				
				this.progressProp.set(availableNdvi.size()/cantImagenesADescargar);
				tabla1.refresh();
				//);
				
				});
			Scene scene = new Scene(v, 800, 600);
			//tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("BulkNdviDownloadGUI.stageNDVITitle")); //$NON-NLS-1$
			tablaStage.setScene(scene);
			tablaStage.show();	 		
	}

		private  Map<String, Object[]> crearSerie() {				 
			 SortedSet<LocalDate> fechasUnicas = new TreeSet<>(this.availableNdvi.stream().map(n->n.getFecha()).sorted()
		    		  .collect(Collectors.toList()));
			
			
			Map<String, Object[]> data = new TreeMap<String, Object[]>();
			//			fecha1	fecha2	fecha3	fecha4	...
			//poligono1	p1f1	p1f2	p1f3	p1f4	...
			//poligono2	p2f1	p2f2	p2f3	p2f4	...
			//...
			
			List<String> headers = new ArrayList<String>();
			headers.add(Messages.getString("BulkNdviDownloadGUI.poligonoNameExcelColumn"));//"Poligono");
			headers.add(Messages.getString("BulkNdviDownloadGUI.latitudExcelColumn"));//"Latitud");
			headers.add(Messages.getString("BulkNdviDownloadGUI.longitudExcelColumn"));//"Longitud");
		//	DateTimeFormatter simpleDateFormat = DateTimeFormatter.ofPattern(YYYY_MM_DD);
			for(LocalDate fecha :fechasUnicas) {			
				//System.out.println("creando columna para fecha "+fecha +" con header "+format1.format(fecha));
				
				headers.add(format1.format(fecha));
			}
			data.put("0", headers.toArray());
			
			 List<Object> values = new ArrayList<Object>();
			int i=1;
			for(Poligono p : selected) {
			 Map<LocalDate, Ndvi> poliNdviFechaMap =  (Map<LocalDate, Ndvi>)availableNdvi.stream().filter(n->n.getContorno().equals(p)).
					 collect(Collectors.toMap(n->n.getFecha(), n->n,(prev,curr)->{
						// System.out.println("resolviendo colision entre "+prev.getMeanNDVI()+" "+curr.getMeanNDVI()+" para "+prev.getFecha());
						 return prev; 
					 }));//toList());
			
				values.add(p.getNombre());
				Point centroid=p.toGeometry().getCentroid();
				values.add(centroid.getY());
				values.add(centroid.getX());
				for(LocalDate fecha : fechasUnicas) {
					Ndvi n =poliNdviFechaMap.get(fecha);
					if(n == null || n.getPorcNubes()>50) {//no exporto los puntos nublados
						values.add("");
					} else {
						values.add(n.getMeanNDVI());
					}
				}
				data.put(String.valueOf(i),values.toArray());
				values.clear();
				i++;
			}
			return data;
		}
	
		public void processSelectedPoligonsNDVI(ObservableList<Poligono> poligons) {
			NDVIDatePickerDialog ndviDpDLG = new NDVIDatePickerDialog(null);
			LocalDate ret = ndviDpDLG.ndviDateChooser(null);
			if(ret!=null) { 
				this.finalLd = ndviDpDLG.finalDate;
				this.startLd = ndviDpDLG.initialDate;
				
				JFXMain.executorPool.execute(()->	{
					poligons.parallelStream().forEach(	(p) ->{			
					//	Poligono env = ExtraerPoligonosDeLaborTask.geometryToPoligono(p.toGeometry().getEnvelope());
					//	System.out.println(env.getPositionsString());
					//	env.setNombre(p.getNombre());
						 requestNDVIForPolyDateDateByDate(p);//ejecuta las querys de a una						
					});
					Platform.runLater(()->{
						Alert a = new Alert(Alert.AlertType.INFORMATION, Messages.getString("BulkNdviDownloadGUI.allDoneAlertMessage"), ButtonType.OK);
						a.initOwner(tablaStage);
						a.setContentText("Se descargaron "+availableNdvi.size()+" datos");
						a.setTitle(Messages.getString("BulkNdviDownloadGUI.stageNDVITitle"));
						a.show();
					});
					System.out.println("termine de procesar todos los poligonos");
				});
				//return resNDVI;
				//recorrer los poligonos buscando ndvi para las fechas seleccionadas
			} else {
				//return null;
			}
		}
		
	public 	List<Ndvi> requestNDVIForPolyDateDateByDate( Poligono p) {
		List<Ndvi> ret = new ArrayList<Ndvi>();
		GetNdviForLaborTask4 task = new GetNdviForLaborTask4(null,null);
		task.setFinDate(this.finalLd);
		task.setBeginDate(this.startLd);
		List<LocalDate> fechasP = task.getSentinellAssets(p);
		
		this.cantImagenesADescargar=(this.cantImagenesADescargar*n+ fechasP.size()*selected.size())/(n+1);//fechasP.size();
		n++;
		
		if(fechasP.size()>5) {
			Map<LocalDate,LocalDate> beginEndMap = new HashMap<>();
			LocalDate fechaInit = fechasP.get(0);
			for(int i=MAX_DATES_PER_QUERY;i<fechasP.size();i+=MAX_DATES_PER_QUERY) {
				LocalDate fechaHasta = fechasP.get(i);
				beginEndMap.put(fechaInit, fechaHasta);
				fechaInit=fechaHasta;
			}

			//LocalDate fechaHasta = fechasP.get(fechasP.size()-1);
			beginEndMap.put(fechaInit, fechasP.get(fechasP.size()-1).plusDays(1));

			beginEndMap.keySet().parallelStream().forEach(init->{
				LocalDate end = beginEndMap.get(init);
				String sEnd = format1.format(end);//ld.plusDays(1));
				String sBegin = format1.format(init);//ld);
				List<Ndvi> values=requestNDVIForPolyDate(sEnd,sBegin,p);
				ret.addAll(values);
				Platform.runLater(()->	{
					availableNdvi.addAll(values);		
					this.progressProp.set(availableNdvi.size()/cantImagenesADescargar);
				});
			});			
		} else {			
			String sEnd = format1.format(this.finalLd);//ld.plusDays(1));
			String sBegin = format1.format(this.startLd);//ld);
			List<Ndvi> values=requestNDVIForPolyDate(sEnd,sBegin,p);
			ret.addAll(values);
			Platform.runLater(()->	{
				availableNdvi.addAll(values);					
			});
		}
		return ret;
	}

	public List<Ndvi> requestNDVIForPolyDate(String sEnd, String sBegin, Poligono p) {
		GenericUrl url = new GenericUrl(HTTP_GEE_API_HELPER_HEROKUAPP_COM_NDVI_V3);

		Map<String, String> req_data = new HashMap<String, String>();
		req_data.put(GEE_POLYGONS_GET_REQUEST_KEY, p.getPoligonoToString());
		req_data.put(BEGIN, sBegin);
		req_data.put(END, sEnd);
		req_data.put(TOKEN, URSULA_GIS_TOKEN);

		final HttpContent req_content = new JsonHttpContent(new JacksonFactory(), req_data);

		/*
		assets=["COPERNICUS/S2/20161221T141042_20161221T142209_T20HLG"]
		polygons=[[[[-64.69101905822754,-34.860017354204885],[-64.69058990478516,-34.86705989785682],[-64.67016220092773,-34.86515847050267],[-64.67265129089355,-34.86198932721536]]]]
		 */

		HttpResponse response = makePostRequest(url,req_content);
		List<String[]> values = parseNDVIResponse(response);//date, ndvi, path2
		List<Ndvi> ndviValues = values.stream().map(v->{
			Ndvi ndvi = new Ndvi();
			ndvi.setNombre(p.getNombre()+" "+v[0]);
//			File f = new File(v[3]);
//			ndvi.updateContent(f);
//			f.delete();
			//ndvi.setF(new File(v[3]));//pongo path en file				
			ndvi.setContorno(p);
			try {
				ndvi.setMeanNDVI(Double.parseDouble(v[1]));//number format exception epty string
			}catch(Exception e) {
				ndvi.setMeanNDVI(0.0);
				e.printStackTrace();
			}
			try {
				ndvi.setPorcNubes(Double.parseDouble(v[2]));//number format exception epty string
			}catch(Exception e) {
				ndvi.setPorcNubes(0.0);
				e.printStackTrace();
			}
			
			try{							
				LocalDate fecha = LocalDate.parse(v[0], format1);
				ndvi.setFecha(fecha);
			}catch(Exception e){
				e.printStackTrace();
			}
			return ndvi; 
		}).collect(Collectors.toList());
		return ndviValues;
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
					}
				});
		int trys = 0;
		do {
			trys++;
			try {
			
				HttpRequest request = requestFactory.buildPostRequest(url, req_content);//(url);
				response= request.execute();
				if(trys>1) {
					System.err.println("Exito luego de "+(trys)+" intentos");
				}
			} catch (Exception e) {
				/*
				 * Jan 27 06:48:30 gee-api-helper heroku/router: at=error code=H12 desc="Request timeout" method=POST path="/ndvi_v4PNG" host=gee-api-helper.herokuapp.com request_id=d76a92ee-aba7-4fa5-bcb1-80967f2af845 fwd="200.89.183.43" dyno=web.1 connect=1ms service=30000ms status=503 bytes=0 protocol=http
				 * Jan 27 06:48:30 gee-api-helper heroku/router: at=error code=H12 desc="Request timeout" method=POST path="/ndvi_v4PNG" host=gee-api-helper.herokuapp.com request_id=95163f31-034f-4de8-83d2-1eadaf5fe5b2 fwd="200.89.183.43" dyno=web.1 connect=0ms service=30000ms status=503 bytes=0 protocol=http
				 * Jan 27 06:49:05 gee-api-helper heroku/router: at=error code=H12 desc="Request timeout" method=POST path="/ndvi_v4PNG" host=gee-api-helper.herokuapp.com request_id=11d4d1a9-300b-457c-a271-6a35988e0ac5 fwd="200.89.183.43" dyno=web.1 connect=0ms service=30000ms status=503 bytes=0 protocol=http
				 */
				
				//e.printStackTrace();
				String message = e.getClass().getName();
				String[] lines = e.getMessage().split("\n");
				if(lines.length>0)message = message.concat(" "+lines[0] );
				System.err.println(message);
				if(trys<MAX_TRYS) {
					System.err.println("Reintentando "+(MAX_TRYS-trys)+" vececes mas");
				}else {
					System.err.println("Fallo el request "+(trys)+" vececes. Abandonando.");
				}
				response = null;
//				try {
//					Thread.sleep(100L);//wait 1second to retry
//				} catch (InterruptedException e1) {
//					e1.printStackTrace();
//				}
			}	
		}while(response == null && trys < MAX_TRYS );
		return response;
	}

	@SuppressWarnings("unchecked")
	private  List<String[]> parseNDVIResponse(HttpResponse response) {
		List<String[]> values = new ArrayList<String[]>();
		if(response==null)return values;
		try {
		
			//System.out.println("response:\n"+response.parseAsString());
			GenericJson content = response.parseAs(GenericJson.class);
			if(content==null)return values;
			/*
			 * {"data":[{
			 * 			"date":"2019-12-28",
			 * 			 "id":"COPERNICUSS220191228T140049_20191228T140047_T20",
			 * 			 "path1":"https://earthengine.googleapis.com/api/download?docid=d0e2f71cd8235106453f5486dd2b0487&token=095ccd6438a1eb2a32fe6d47e1b28889",
			 * 			 "path2":"https://earthengine.googleapis.com/api/download?docid=dcc20e13c90bcf36acbecaf7cad92b92&token=4628eac19fdaf824da2c55c81a662c23",
			 * 			 "path3":"",
			 * 			 "metadata":{"meanNDVI":2.0}}]}
			 */
			//System.out.println("ndvi content "+ content);//XXX ndvi content {"data":[]}
			Object features = content.get(DATA);
			if(features == null || ! (features instanceof List) )return values;	
				for(ArrayMap<String,?> feature : (List<ArrayMap<String,String>>)features){
					String date = (String) feature.get("date");//2018-03-25
					ArrayMap<String,Object> metadata = (ArrayMap<String, Object>) feature.get("metadata");
					Object meanObject = metadata.get("meanNDVI");
					String mean = "0.0";
					if(meanObject instanceof BigDecimal) {
						BigDecimal meanNDVI = (BigDecimal)meanObject;
						mean = bdf.format(meanNDVI);
					} else {
						System.out.println("no se de que clase es meanNDVI "+meanObject.getClass().getCanonicalName());
					}
					
					Object cloudsObject = metadata.get("porcNubes");
					String clouds = "0.0";
					if(meanObject instanceof BigDecimal) {
						BigDecimal cloudsBD = (BigDecimal)cloudsObject;
						clouds = bdf.format(cloudsBD);
					} else {
						System.out.println("no se de que clase es meanNDVI "+meanObject.getClass().getCanonicalName());
					}
					//System.out.println(date+" "+mean);//2020-01-15 2.00 {meanNDVI:"0.24"}
					String path2 = (String) feature.get(PATH2);
					values.add(new String[]{date,mean,clouds,path2});
				}
				return values;
		} catch (Exception e) {
			e.printStackTrace();
			return values;
		}
	}
}
