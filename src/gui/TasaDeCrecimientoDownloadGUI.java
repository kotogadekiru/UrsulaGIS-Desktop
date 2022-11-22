package gui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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

import dao.Ndvi;
import dao.Poligono;
import dao.config.Asignacion;
import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Lote;
import gui.utils.DateConverter;
import gui.utils.SmartTableView;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Data;
import tasks.GetNdviForLaborTask4;
import utils.DAH;
import utils.ExcelHelper;

public class TasaDeCrecimientoDownloadGUI {
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
	private static final String HTTP_GEE_API_HELPER_HEROKUAPP_COM_MEAN_NDVI_V4 = BASE_URL+"/ndvi_mean_v4";//"/ndvi_v4";//+"/gndvi_v4_SR";//"/ndvi_v3";//ndvi_v5

	private static final String GEE_POLYGONS_GET_REQUEST_KEY = "polygons";

	ObservableList<Asignacion> availableAsignaciones =null;
	ObservableList<TasaCrecimientoData> availableData =null;
	ObservableList<Asignacion> selected =null;
	DateTimeFormatter format1 = DateTimeFormatter.ofPattern(YYYY_MM_DD);

	DecimalFormat bdf = new DecimalFormat("#,###.00");

	Stage tablaStage = null;

	//Map<String,Object[]> datesPoligonNdvi = new ArrayMap<String,Object[]>();
	private DoubleProperty progressProp;

	private double cantImagenesADescargar=0.0;
	private int n = 0;
	Double[] radInc = new Double[] {24.79 ,24.74 ,24.68 ,24.63 ,24.57 ,24.51 ,24.44 ,24.38 ,24.31 ,24.24 ,24.16 ,24.09 ,24.01 ,23.93 ,23.84 ,23.76 ,23.67 ,23.58 ,23.49 ,23.39 ,23.30 ,23.20 ,23.10 ,23.00 ,22.90 ,22.79 ,22.68 ,22.58 ,22.47 ,22.36 ,22.24 ,22.13 ,22.01 ,21.90 ,21.78 ,21.66 ,21.54 ,21.42 ,21.29 ,21.17 ,21.04 ,20.92 ,20.79 ,20.66 ,20.53 ,20.40 ,20.27 ,20.14 ,20.01 ,19.87 ,19.74 ,19.61 ,19.47 ,19.34 ,19.20 ,19.06 ,18.93 ,18.79 ,18.65 ,18.51 ,18.38 ,18.24 ,18.10 ,17.96 ,17.82 ,17.68 ,17.54 ,17.40 ,17.26 ,17.13 ,16.99 ,16.85 ,16.71 ,16.57 ,16.43 ,16.29 ,16.15 ,16.02 ,15.88 ,15.74 ,15.61 ,15.47 ,15.33 ,15.20 ,15.06 ,14.93 ,14.79 ,14.66 ,14.53 ,14.40 ,14.27 ,14.13 ,14.00 ,13.88 ,13.75 ,13.62 ,13.49 ,13.37 ,13.24 ,13.12 ,12.99 ,12.87 ,12.75 ,12.63 ,12.51 ,12.39 ,12.27 ,12.16 ,12.04 ,11.93 ,11.81 ,11.70 ,11.59 ,11.48 ,11.37 ,11.27 ,11.16 ,11.06 ,10.95 ,10.85 ,10.75 ,10.65 ,10.55 ,10.46 ,10.36 ,10.27 ,10.18 ,10.09 ,10.00 ,9.91 ,9.82 ,9.74 ,9.66 ,9.57 ,9.49 ,9.42 ,9.34 ,9.26 ,9.19 ,9.12 ,9.05 ,8.98 ,8.91 ,8.85 ,8.78 ,8.72 ,8.66 ,8.60 ,8.54 ,8.49 ,8.44 ,8.38 ,8.33 ,8.29 ,8.24 ,8.19 ,8.15 ,8.11 ,8.07 ,8.03 ,8.00 ,7.97 ,7.93 ,7.90 ,7.87 ,7.85 ,7.82 ,7.80 ,7.78 ,7.76 ,7.74 ,7.73 ,7.72 ,7.70 ,7.70 ,7.69 ,7.68 ,7.68 ,7.68 ,7.68 ,7.68 ,7.68 ,7.69 ,7.69 ,7.70 ,7.71 ,7.73 ,7.74 ,7.76 ,7.78 ,7.80 ,7.82 ,7.84 ,7.87 ,7.90 ,7.93 ,7.96 ,7.99 ,8.03 ,8.07 ,8.11 ,8.15 ,8.19 ,8.23 ,8.28 ,8.33 ,8.38 ,8.43 ,8.48 ,8.54 ,8.60 ,8.65 ,8.72 ,8.78 ,8.84 ,8.91 ,8.97 ,9.04 ,9.11 ,9.19 ,9.26 ,9.34 ,9.41 ,9.49 ,9.57 ,9.66 ,9.74 ,9.83 ,9.91 ,10.00 ,10.09 ,10.18 ,10.28 ,10.37 ,10.47 ,10.56 ,10.66 ,10.76 ,10.86 ,10.97 ,11.07 ,11.18 ,11.29 ,11.39 ,11.50 ,11.61 ,11.73 ,11.84 ,11.95 ,12.07 ,12.19 ,12.31 ,12.43 ,12.55 ,12.67 ,12.79 ,12.91 ,13.04 ,13.17 ,13.29 ,13.42 ,13.55 ,13.68 ,13.81 ,13.94 ,14.07 ,14.21 ,14.34 ,14.47 ,14.61 ,14.75 ,14.88 ,15.02 ,15.16 ,15.30 ,15.43 ,15.57 ,15.71 ,15.86 ,16.00 ,16.14 ,16.28 ,16.42 ,16.57 ,16.71 ,16.85 ,17.00 ,17.14 ,17.28 ,17.43 ,17.57 ,17.72 ,17.86 ,18.01 ,18.15 ,18.29 ,18.44 ,18.58 ,18.73 ,18.87 ,19.01 ,19.16 ,19.30 ,19.44 ,19.59 ,19.73 ,19.87 ,20.01 ,20.15 ,20.29 ,20.43 ,20.57 ,20.71 ,20.85 ,20.98 ,21.12 ,21.25 ,21.39 ,21.52 ,21.66 ,21.79 ,21.92 ,22.05 ,22.18 ,22.30 ,22.43 ,22.55 ,22.68 ,22.80 ,22.92 ,23.04 ,23.16 ,23.27 ,23.39 ,23.50 ,23.61 ,23.72 ,23.83 ,23.94 ,24.04 ,24.15 ,24.25 ,24.35 ,24.44 ,24.54 ,24.63 ,24.72 ,24.81 ,24.90 ,24.98 ,25.06 ,25.14 ,25.22 ,25.29 ,25.37 ,25.43 ,25.50 ,25.57 ,25.63 ,25.68 ,25.74 ,25.79 ,25.84 ,25.89 ,25.93 ,25.97};
	private LocalDate finalLd;
	private LocalDate startLd;

	public TasaDeCrecimientoDownloadGUI() {
		availableAsignaciones =	FXCollections.observableArrayList( DAH.getAllAsignaciones() );
		selected =	FXCollections.observableArrayList();
		availableData = FXCollections.observableArrayList();

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
		SmartTableView<Asignacion> tabla1 = new SmartTableView<Asignacion>(availableAsignaciones,	Arrays.asList("Id","PositionsString","PoligonoToString","Activo","Area"),
				Arrays.asList("Nombre"));
		tabla1.setEditable(false);

		tabla1.getSelectionModel().setSelectionMode(
				SelectionMode.MULTIPLE
				);

		SmartTableView<Asignacion> tabla2 = new SmartTableView<Asignacion>(selected,	Arrays.asList("Id","PositionsString","PoligonoToString","Activo","Area"),
				Arrays.asList("Nombre"));
		tabla2.setEditable(false);
		tabla2.getSelectionModel().setSelectionMode(
				SelectionMode.MULTIPLE
				);

		Button addSelectedButton= new Button();
		addSelectedButton.setMaxWidth(Double.MAX_VALUE);


		addSelectedButton.setText(Messages.getString("doBulkNDVIDownload.addSelectedButton")+" -->");
		addSelectedButton.setOnAction((ae)->{
			ObservableList<Asignacion> list = tabla1.getSelectionModel().getSelectedItems();
			selected.addAll(list);
			availableAsignaciones.removeAll(list);
			tabla1.refresh();
			tabla2.refresh();
		});

		Button remSelectedButton= new Button();
		remSelectedButton.setMaxWidth(Double.MAX_VALUE);
		remSelectedButton.setText("<-- "+Messages.getString("doBulkNDVIDownload.remSelectedButton"));
		remSelectedButton.setOnAction((ae)->{
			//getSelected from tabla 1 and add them to selected list
			ObservableList<Asignacion> list = tabla2.getSelectionModel().getSelectedItems();
			//	System.out.println("removing "+list.size()+" items");
			selected.removeAll(list);
			availableAsignaciones.addAll(list);//FIXME no los vuelve a poner en la lista
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
		Label creditos = new Label("Esta funcionalidad fue desarrollada junto a Federico del Pino, \n");
		//Image image = new Image(getClass().getResourceAsStream("ravit_logo400x400.jpg"));
		//ImageView iv = new ImageView(image);
		//iv.setPreserveRatio(true);
		//iv.setFitWidth(100);

		//creditos.setGraphic(new ImageView(image));
		creditos.setWrapText(true);
		VBox v = new VBox(/*iv,*/addSelectedButton,remSelectedButton,showNDVITablaButton,creditos);
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

		tablaStage.getIcons().add(new Image(JFXMain.ICON));
		tablaStage.setTitle(Messages.getString("JFXMain.381")); //$NON-NLS-1$
		tablaStage.setScene(scene);
		tablaStage.show();			

	}

	private Double getRadInc(LocalDate fecha) {
		int diaJul =fecha.getDayOfYear(); 
		double rad = radInc[diaJul];
		System.out.println("calculando raciacion para "+diaJul+" de "+fecha+" => "+rad);
		return rad;			 
	}

	private TasaCrecimientoData getTasaCrecimientoNDVI(Ndvi ndvi, Asignacion asig) {
		Cultivo cult= asig.getCultivo();
		//PP:6,532+0
		//CN:2,669+4,36
		//VI:9,579+0
		double pendienteNdviRinde= cult.getTasaCrecimientoPendiente();
		double origenNdviRinde=cult.getTasaCrecimientoOrigen();

		//		double pendienteNdviRinde = 6.532;		
		//		double origenNdviRinde = 0;			
		double radInc=getRadInc(ndvi.getFecha());//20.8466480856667;

		double ndviGreenSeeker=0.8913*ndvi.getMeanNDVI();//ndvi greenseeker
		double fPar=ndviGreenSeeker*1.51-0.29;
		//PAR (Rad Inc)(MJ/m2/dia)

		double RFAA=0.48*radInc;
		double crecimientoDia = fPar*RFAA*pendienteNdviRinde+origenNdviRinde;

		System.out.println("taza crecimiento para ndvi "+ndvi+" => "+crecimientoDia);
		TasaCrecimientoData tc = new TasaCrecimientoData();
		tc.setLote(asig.getLote());
		tc.setCampo(tc.getLote().getEstablecimiento());
		tc.setEmpresa(tc.getCampo().getEmpresa());
		tc.setNdvi(ndvi);
		tc.setRecurso(asig.getCultivo());
		tc.setFecha(ndvi.getFecha());
		tc.setAnio(tc.getFecha().getYear());
		tc.setMes(tc.getFecha().getMonthValue());
		tc.setContorno(ndvi.getContorno());
		tc.setRadInc(radInc);
		tc.setFPar(fPar);
		tc.setRfaa(RFAA);
		tc.setRadInc(radInc);		
		tc.setCrecimiento(crecimientoDia);

		return tc;

	}

	public void showNdviValues() {

		SmartTableView<TasaCrecimientoData> tabla1 = new SmartTableView<TasaCrecimientoData>(availableData,	
				Arrays.asList("Id","Contorno"),
				Arrays.asList("Empresa","Campo","Lote","Recurso","Fecha","Ndvi","FPar","RadInc","Rfaa","Crecimiento"));
		tabla1.setEditable(false);

		tabla1.getSelectionModel().setSelectionMode(
				SelectionMode.SINGLE
				);			

		Button export =new Button(Messages.getString("doBulkNDVIDownload.exportSelectedButton"));//"Exportar");
		export.setOnAction(a->{
			ExcelHelper h=new ExcelHelper();
			h.exportData("NDVI",crearSerie());
		});

		ProgressBar progressBarTask = new ProgressBar();	
		Tooltip t = new Tooltip();
		progressBarTask.progressProperty().addListener(c->{
			t.setText(availableData.size()+" / "+(int)cantImagenesADescargar);
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

		availableData.addListener((Change<? extends TasaCrecimientoData> c)->{
			//System.out.println("refreshing.."+availableNdvi.size());
			//System.out.println("progress = "+this.progressProp);

			//Platform.runLater(()->

			this.progressProp.set(availableData.size()/cantImagenesADescargar);
			tabla1.refresh();
			//);

		});
		Scene scene = new Scene(v, 800, 600);
		tablaStage.getIcons().add(new Image(JFXMain.ICON));
		tablaStage.setTitle(Messages.getString("BulkNdviDownloadGUI.stageNDVITitle")); //$NON-NLS-1$
		tablaStage.setScene(scene);
		tablaStage.show();	 		
	}

	//TODO el excel tiene que tener el ndvi el cultivo la tasa de crecimiento la empresa y el lote
	private  Map<String, Object[]> crearSerie() {		
		
		DateTimeFormatter dma = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		Map<String, Object[]> data = new TreeMap<String, Object[]>();

		List<String> headers = new ArrayList<String>();			
		headers.add("Empresa");
		headers.add("Campo");
		headers.add("Lote");
		headers.add("Recurso");
		headers.add("Fecha");
		headers.add("Mes");
		headers.add("Año");
		headers.add("NDVI");
		headers.add("fPar");
		headers.add("radInc");
		headers.add("RFAA");
		headers.add("crecimiento (Kgs/ha/día)");//Crecimiento (Kgs/ha/día)

		data.put("0", headers.toArray());

		List<Object> values = new ArrayList<Object>();
		int i=1;
		NumberFormat nf = Messages.getNumberFormat();
		//junto todos los resultados por fecha
		for(TasaCrecimientoData tc : availableData) {
			values.add(tc.getEmpresa().getNombre());
			values.add(tc.getCampo().getNombre());
			values.add(tc.getLote().getNombre());
			values.add(tc.getRecurso().getNombre());
			values.add(tc.getFecha());
			values.add(new Double(tc.getFecha().getMonthValue()));
			values.add(new Double(tc.getFecha().getYear()));			
			values.add(tc.getNdvi().getMeanNDVI());
			values.add(tc.getFPar());
			values.add(tc.getRadInc());
			values.add(tc.getRfaa());
			values.add(tc.getCrecimiento());
			
			System.out.println(Arrays.toString(values.toArray()));
			data.put(String.valueOf(i),values.toArray());
			values.clear();
			i++;
		}
		return data;
	}

	public void processSelectedPoligonsNDVI(ObservableList<Asignacion> selected2) {
		NDVIDatePickerDialog ndviDpDLG = new NDVIDatePickerDialog(null);
		LocalDate ret = ndviDpDLG.ndviDateChooser(null);
		if(ret!=null) { 
			this.finalLd = ndviDpDLG.finalDate;
			this.startLd = ndviDpDLG.initialDate;
			JFXMain.executorPool.execute(()->	{
				selected2.parallelStream().forEach(	(asig) ->{
					requestNDVIForAsigDateByDate(asig);//ejecuta las querys de a una						
				});
				Platform.runLater(()->{
					Alert a = new Alert(Alert.AlertType.INFORMATION, Messages.getString("BulkNdviDownloadGUI.allDoneAlertMessage"), ButtonType.OK);
					a.initOwner(tablaStage);
					a.setContentText("Se descargaron "+availableData.size()+" datos");
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

	public 	void requestNDVIForAsigDateByDate( Asignacion asig) {
		LocalDate iniAsig = DateConverter.asLocalDate(asig.getCampania().getInicio());
		LocalDate finAsig = DateConverter.asLocalDate(asig.getCampania().getFin());
	
		if(iniAsig.isAfter(finalLd))return;//la asignacion empieza despues de la fecha seleccionada
		if(finAsig.isBefore(startLd))return;//la asignacion termina antes de la fecha seleccionada
		LocalDate ini = this.startLd.isAfter(iniAsig)?this.startLd:iniAsig;
		LocalDate fin = this.finalLd.isBefore(finAsig)?this.finalLd:finAsig;
		//if(fin.isBefore(iniAsig))return;//
		GetNdviForLaborTask4 task = new GetNdviForLaborTask4(null,null);
		task.setFinDate(fin);
		task.setBeginDate(ini);

		List<LocalDate> fechasP = task.getSentinellAssets(asig.getContorno());

		this.cantImagenesADescargar=(this.cantImagenesADescargar*n+ fechasP.size()*selected.size())/(n+1);//fechasP.size();
		n++;

		//	if(fechasP.size()>5) {
		Map<LocalDate,LocalDate> beginEndMap = new HashMap<>();
		if(fechasP.size()==0)return;
		LocalDate fechaInit = fechasP.get(0);
		for(int i=MAX_DATES_PER_QUERY;i<fechasP.size();i+=MAX_DATES_PER_QUERY) {
			LocalDate fechaHasta = fechasP.get(i);
			beginEndMap.put(fechaInit, fechaHasta);
			fechaInit=fechaHasta;
		}

		beginEndMap.put(fechaInit, fechasP.get(fechasP.size()-1).plusDays(1));

		beginEndMap.keySet().parallelStream().forEach(init->{
			LocalDate end = beginEndMap.get(init);
			String sEnd = format1.format(end);//ld.plusDays(1));
			String sBegin = format1.format(init);//ld);
			List<Ndvi> values= requestNDVIForPolyDate(sEnd,sBegin,asig.getContorno());
			List<TasaCrecimientoData> tcValues = new ArrayList<TasaCrecimientoData>();
			for(Ndvi ndvi:values) {
				if(ndvi.getPorcNubes()<50) {
					tcValues.add(getTasaCrecimientoNDVI(ndvi,asig));
				} else {
					cantImagenesADescargar--;
					System.out.println("rechazando ndvi por nublado >0.5 "+ndvi.getPorcNubes());
				}
			}
			//	ret.addAll(values);
			Platform.runLater(()->	{	
				availableData.addAll(tcValues);		
				this.progressProp.set(availableData.size()/cantImagenesADescargar);
			});
		});			
		//		} else {			
		//			
		//			List<Ndvi> values=requestNDVIForPolyDate(sEnd,sBegin,asig.getContorno());
		//			for(Ndvi ndvi:values) {
		//				ndvi.setMeanNDVI(getTasaCrecimientoNDVI(ndvi,asig));
		//			}
		//			Platform.runLater(()->	{
		//				availableNdvi.addAll(values);					
		//			});
		//		}

	}

	public List<Ndvi> requestNDVIForPolyDate(String sEnd, String sBegin, Poligono p) {
		GenericUrl url = new GenericUrl(HTTP_GEE_API_HELPER_HEROKUAPP_COM_MEAN_NDVI_V4);

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
				ndvi.setMeanNDVI(new Double(v[1]));//number format exception empty string
			}catch(Exception e) {
				ndvi.setMeanNDVI(0.0);
				e.printStackTrace();
			}
			try {
				ndvi.setPorcNubes(new Double(v[2]));//number format exception empty string
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
	@Data
	public class TasaCrecimientoData{
		private Empresa empresa;
		private Establecimiento campo;
		private Lote lote;
		private Poligono contorno;
		private Cultivo recurso;
		private LocalDate fecha;
		private int mes;
		private int anio;
		private Ndvi ndvi;
		private Double fPar;
		private Double radInc;
		private Double rfaa;
		private Double crecimiento;//Crecimiento (Kgs/ha/día)
	}
}
