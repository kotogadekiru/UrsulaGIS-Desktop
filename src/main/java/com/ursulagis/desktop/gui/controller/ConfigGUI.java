package com.ursulagis.desktop.gui.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.controlsfx.control.HyperlinkLabel;
import org.geotools.api.data.FileDataStore;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import com.ursulagis.desktop.api.OrdenFertilizacion;
import com.ursulagis.desktop.api.OrdenPulverizacion;
import com.ursulagis.desktop.api.OrdenSiembra;
import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.config.Asignacion;
import com.ursulagis.desktop.dao.config.Campania;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Empresa;
import com.ursulagis.desktop.dao.config.Establecimiento;
import com.ursulagis.desktop.dao.config.Fertilizante;
import com.ursulagis.desktop.dao.config.Lote;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.cosecha.CosechaLabor;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.margen.Margen;
import com.ursulagis.desktop.dao.ordenCompra.OrdenCompra;
import com.ursulagis.desktop.dao.ordenCompra.OrdenCompraItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import gov.nasa.worldwind.geom.Position;
import com.ursulagis.desktop.gui.CorrelacionarCapas;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.MargenConfigDialogController;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.gui.snake.SnakesLayer;
import com.ursulagis.desktop.gui.utils.DoubleTableColumn;
import com.ursulagis.desktop.gui.utils.LoggingOutputStream;
import com.ursulagis.desktop.gui.utils.OpenBrowserUrl;
import com.ursulagis.desktop.gui.utils.SmartTableView;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.ursulagis.desktop.tasks.CotizarOdenDeCompraOnlineTask;
import com.ursulagis.desktop.tasks.GoogleGeocodingHelper;
import com.ursulagis.desktop.tasks.UpdateTask;
import com.ursulagis.desktop.tasks.crear.GenerarOrdenCompraTask;
import com.ursulagis.desktop.tasks.importar.OpenMargenMapTask;
import com.ursulagis.desktop.tasks.procesar.ProcessMarginMapTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.ExcelHelper;
import com.ursulagis.desktop.utils.FileHelper;

public class ConfigGUI extends AbstractGUIController{
	private static final String DD_MM_YYYY = "dd/MM/yyyy";
	private boolean snakeIsActive=false;


	public ConfigGUI(JFXMain _main) {
		super(_main);		
	}

	public static String getBuildInfo() {
		//Esta aplicacion fue compilada el 11 de Febrero de 2020.

		LocalDate compileDate;
		try {
			// Try to parse Maven timestamp format first (ISO format)
			DateTimeFormatter mavenFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			compileDate = LocalDate.parse(JFXMain.buildDate, mavenFormatter);
		} catch (Exception e) {
			// Fallback to old format (dd/MM/yyyy)
			DateTimeFormatter inDTFormater = DateTimeFormatter.ofPattern(DD_MM_YYYY);
			compileDate = LocalDate.parse(JFXMain.buildDate, inDTFormater);
		}	
		//DateFormat outDTFormater = DateFormat.getDateInstance(DateFormat.FULL, Messages.getLocale());

		DateTimeFormatter dft= DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
		dft = dft.withLocale(Messages.getLocale());

		//String localizedBuidDate = outDTFormater.format(DateConverter.asDate(compileDate));
		String localizedBuidDate =  dft.format(compileDate);
		//Esta aplicacion fue compilada el 11 de Febrero de 2020.
		return Messages.getString("JFXMain.info1")+localizedBuidDate //TODO crear una dateTimeFomrater y localizar el formato segun el locale Elegido
				+Messages.getString("JFXMain.info2") // version compilada el 
				+Messages.getString("JFXMain.inf3") //
				+Messages.getString("JFXMain.info3") //
				+Messages.getString("JFXMain.info4"); //
	}

	public void addMenuesToMenuBar(MenuBar menuBar) {
		/*Menu Importar*/
		final Menu menuImportar = new Menu(Messages.getString("JFXMain.importar")); 
		addMenuItem(Messages.getString("JFXMain.NDVI"),(a)->main.ndviGUIController.doOpenNDVITiffFiles(),menuImportar); 
		addMenuItem(Messages.getString("JFXMain.imagen"),(a)->main.importImagery(),menuImportar); 
		addMenuItem(Messages.getString("JFXMain.suelo"),(a)->main.sueloGUIController.doOpenSoilMap(null),menuImportar); 
		addMenuItem(Messages.getString("RecorridaGUIController.recorrida"),(a)->main.recorridaGUIController.doOpenRecorridaMap(null),menuImportar);
		addMenuItem(Messages.getString("JFXMain.margen"),(a)->doOpenMarginMap(),menuImportar); 
		addMenuItem(Messages.getString("JFXMain.poligonos"),(a)->main.poligonoGUIController.doImportarPoligonos(null),menuImportar); 
		/*Menu herramientas*/
		final Menu menuHerramientas = new Menu(Messages.getString("JFXMain.herramientas")); 		
		addMenuItem(Messages.getString("JFXMain.distancia"),(a)->main.poligonoGUIController.doMedirDistancia(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.superficie"),(a)->main.poligonoGUIController.doCrearPoligono(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.unirShapes"),(a)->main.genericGUIController.doJuntarShapefiles(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.rentabilidad"),(a)->doProcessMargin(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.balanceNutrientes"),(a)->main.sueloGUIController.doProcesarBalanceNutrientes(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.generarOrdenCompra"),(a)->doGenerarOrdenDeCompra(),menuHerramientas); 
		addMenuItem(Messages.getString("JFXMain.goTo"),(a)->showGoToDialog(),menuHerramientas);
		addMenuItem(Messages.getString("JFXMain.bulk_ndvi_download"),(a)->main.ndviGUIController.doBulkNDVIDownload(),menuHerramientas);
		addMenuItem(Messages.getString("JFXMain.correlacionarCapas"),(a)->doCorrelacionarCapas(),menuHerramientas);
		//addMenuItem(Messages.getString("JFXMain.JugarSnake"),(a)->doJugarSnake(),menuHerramientas);
		/*Menu Exportar*/
		final Menu menuExportar = new Menu(Messages.getString("JFXMain.exportar"));		 
		addMenuItem(Messages.getString("JFXMain.exportarPantallaMenuItem"),(a)->main.doSnapshot(),menuExportar);
		/*Menu Configuracion*/
		final Menu menuConfiguracion = contructConfigMenu();		
		menuBar.getMenus().addAll(menuImportar,menuHerramientas, menuExportar,menuConfiguracion);
	}

	public Menu contructConfigMenu() {
		/*Menu Configuracion*/
		final Menu menuConfiguracion = new Menu(Messages.getString("JFXMain.configuracionMenu")); //
		addMenuItem(Messages.getString("JFXMain.cultivosMenuItem"),(a)->doConfigCultivo(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.fertilizantesMenuItem"),(a)->doConfigFertilizantes(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.agroquimicosMenuItem"),(a)->doConfigAgroquimicos(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configSemillasMenuItem"),(a)->doConfigSemillas(),menuConfiguracion); //
		//	addMenuItem(Messages.getString("JFXMain.Caldo"),(a)->doConfigCaldos(),menuConfiguracion); //


		addMenuItem(Messages.getString("JFXMain.configEmpresaMI"),(a)->doConfigEmpresa(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configEstablecimientoMI"),(a)->doConfigEstablecimiento(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configLoteMi"),(a)->doConfigLote(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configCampaniaMI"),(a)->doConfigCampania(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configAsignacionMI"),(a)->doConfigAsignacion(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configPoligonosMI"),(a)->doConfigPoligonos(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configNDVIMI"),(a)->doShowNdviTable(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configRecorridaMI"),(a)->doShowRecorridaTable(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.OrdenCompra"),(a)->doShowOrdenesCompra(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.362"),(a)->doShowLaboresTable(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configPulverizacionMI"),(a)->doShowOrdenesPulverizacionTable(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configFertilizacionMI"),(a)->doShowOrdenesFertilizacionTable(),menuConfiguracion); //
		addMenuItem(Messages.getString("JFXMain.configSiembraMI"),(a)->doShowOrdenesSiembraTable(),menuConfiguracion); //

		addMenuItem(Messages.getString("JFXMain.configConfigMI"),(a)->doShowConfiguracionTable(),menuConfiguracion); //

		addMenuItem(Messages.getString("JFXMain.configIdiomaMI"),(a)->doChangeLocale(),menuConfiguracion); 
		addMenuItem(Messages.getString("JFXMain.configHelpMI"),(a)->doShowAcercaDe(),menuConfiguracion);
		addMenuItem(Messages.getString("JFXMain.configLogMI"),(a)->doShowLog(),menuConfiguracion);

		addMenuItem(Messages.getString("ConfigGUI.changeProject"),(a)->doSelectDB(),menuConfiguracion);	

		
		checkIfactualizarMIEnabled(menuConfiguracion);

		return menuConfiguracion;
	}





	public void checkIfactualizarMIEnabled(final Menu menuConfiguracion) {
		JFXMain.executorPool.submit(()->{
			if(UpdateTask.isUpdateAvailable()){
				MenuItem actualizarMI=addMenuItem(Messages.getString("JFXMain.configUpdate"),null,menuConfiguracion); 
				actualizarMI.setOnAction((a)->doUpdate());
				actualizarMI.setVisible(true);
				actualizarMI.getStyleClass().add("menu-item:focused"); 
				actualizarMI.setStyle("-fx-background: -fx-accent;" 
						+"-fx-background-color: -fx-selection-bar;" 
						+ "-fx-text-fill: -fx-selection-bar-text;" 
						+ "fx-text-fill: white;"); 
				//menuConfiguracion.show();
				menuConfiguracion.setStyle("-fx-background: -fx-accent;" 
						+"-fx-background-color: -fx-selection-bar;" 
						+ "-fx-text-fill: -fx-selection-bar-text;" 
						+ "fx-text-fill: white;"); 
			}
		});
	}	
	//TODO seleccionar capas y columnas para armar un grafico de x,y
	private void doCorrelacionarCapas() {
		//TODO seleccionar labor x
		CorrelacionarCapas gui = new CorrelacionarCapas(main);
		gui.show();		
	}

	public void doJugarSnake() {
		if(!snakeIsActive) {
			snakeIsActive = true;
			SnakesLayer layer = new SnakesLayer(getWwd());	     
			insertBeforeCompass(getWwd(), layer);
		}
	}

	public void doCerrarSnake() {
		snakeIsActive=false;
		//SnakesLayer layer = new SnakesLayer(getWwd());	     
		//insertBeforeCompass(getWwd(), layer);
		System.out.println("cerrando snake");
		List<SnakesLayer> snakes = (List<SnakesLayer>)main.getLayersOfClass(SnakesLayer.class);
		if(snakes.size()==0) {
			System.out.println("No encontre snakesLayer");
		}
		for(SnakesLayer s:snakes) {
			s.stop();

		}	  	
	}

	public void startKeyBoardListener() {
		JFXMain.stage.getScene().setOnKeyReleased(event->{
			KeyCode key=event.getCode();
			//System.out.println(key+" typed");
			switch(key) {
			case S:{
				if(event.isControlDown()) {
					doJugarSnake();
				}
				break;
			}
			case C:{
				if(event.isControlDown()) {
					doCerrarSnake();
				}
				break;
			}			
			default :break;		  
			}
		});
	}

	/**
	 * metodo que toma las labores activas de siembra fertilizacion y pulverizacion y hace una lista con los insumos y cantidades para
	 * cotizar precios online. Permite exporta a excel y cotizar precios online y guardar
	 */
	public void doGenerarOrdenDeCompra() {
		GenerarOrdenCompraTask gOCTask = new GenerarOrdenCompraTask(
				main.getSiembrasSeleccionadas(),
				main.getFertilizacionesSeleccionadas(),
				main.getPulverizacionesSeleccionadas());
		gOCTask.installProgressBar(progressBox);
		gOCTask.setOnSucceeded(handler -> {
			OrdenCompra ret = (OrdenCompra)handler.getSource().getValue();
			gOCTask.uninstallProgressBar();
			playSound();
			doShowOrdenCompraItems(ret);
			System.out.println("SiembraFertTask succeded"); 
		});
		executorPool.execute(gOCTask);
	}




	public void doProcessMargin() {		
		System.out.println(Messages.getString("JFXMain.319")); 

		Margen margen = new Margen();
		margen.setLayer(new LaborLayer());

		//todo pasar el filtrado por visibles aca y pasar nuevas listas solo con las visibles
		List<PulverizacionLabor> pulvEnabled =  main.getPulverizacionesSeleccionadas();//pulverizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<FertilizacionLabor> fertEnabled = main.getFertilizacionesSeleccionadas();//fertilizaciones.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());
		List<SiembraLabor> siemEnabled =  main.getSiembrasSeleccionadas();//t());
		List<CosechaLabor> cosechasEnabled =  main.getCosechasSeleccionadas();//cosechas.stream().filter((l)->l.getLayer().isEnabled()).collect(Collectors.toList());

		margen.setFertilizaciones(fertEnabled);
		margen.setPulverizaciones(pulvEnabled);
		margen.setSiembras(siemEnabled);
		margen.setCosechas(cosechasEnabled);

		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("JFXMain.320")); //Rentabilidades
		cosechasEnabled.forEach((c)->sb.append(c.getNombre()+Messages.getString("JFXMain.321"))); 
		margen.setNombre(sb.toString());

		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.322")); 
			return;
		}							

		ProcessMarginMapTask uMmTask = new ProcessMarginMapTask(margen);

		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			Margen ret = (Margen)handler.getSource().getValue();
			uMmTask.uninstallProgressBar();			
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			playSound();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.323")); 
		});
		executorPool.execute(uMmTask);
	}

	public void doOpenMarginMap() {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(null);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Margen labor = new Margen(store);
				labor.setLayer(new LaborLayer());
				Optional<Margen> cosechaConfigured= MargenConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.317")); 
					continue;
				}							

				OpenMargenMapTask umTask = new OpenMargenMapTask(labor);
				umTask.installProgressBar(progressBox);

				//	testLayer();
				umTask.setOnSucceeded(handler -> {
					Margen ret = (Margen)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);
					System.out.println(Messages.getString("JFXMain.318")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}

	public void showGoToDialog() {
		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.goToExample")); 
		anchoDialog.setTitle(Messages.getString("JFXMain.goToDialogTitle")); 
		anchoDialog.setHeaderText(Messages.getString("JFXMain.goToDialogHeader")); 
		anchoDialog.initOwner(JFXMain.stage);
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		if(anchoOptional.isPresent()){
			Position pos = GoogleGeocodingHelper.obtenerPositionDirect(anchoOptional.get());
			if(pos!=null){
				main.viewGoTo(pos);
			}				
		} else{
			return;
		}
	}

	/**
	 * metodo que permite crear una base de datos en una ubicacion deseada
	 * @return
	 */
	private String doSelectDB() {	
		Configuracion c = JFXMain.config;

		String proyectoActual = c.getPropertyOrDefault(DAH.PROJECT_URL_KEY, null);
		File f = FileHelper.chooseFile(proyectoActual, "*.h2");
		if(f!=null && !f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(f!=null && f.exists()) {
			c.setProperty(DAH.PROJECT_URL_KEY, f.getAbsolutePath());
			c.save();
		}
		Alert alert = new Alert(
				AlertType.INFORMATION,
				"Debe Reiniciar para que se efectuen los cambios"); 
		alert.initOwner(JFXMain.stage);
		alert.showAndWait();
		// TODO Auto-generated method stub
		return "nuevo proyecto seleccionado. debe reiniciar";
	}

	/**
	 * 
	 */
	public void doShowAcercaDe() {
		Alert acercaDe = new Alert(AlertType.INFORMATION);
		acercaDe.titleProperty().set(Messages.getString("JFXMain.363")+JFXMain.TITLE_VERSION); //
		acercaDe.initOwner(JFXMain.stage);
		//acercaDe.setHeaderText(this.TITLE_VERSION+"\n"+this.BUILD_INFO+"\nVisitar www.ursulagis.com");
		//acercaDe.contentTextProperty().set();
		String content =   "<b>"+JFXMain.TITLE_VERSION+"</b><br>" // //$NON-NLS-2$
				+"<b>Dispositivo: "+JFXMain.config.getPropertyOrDefault("USER", "NOT SET")+"</b><br>"
				+ ConfigGUI.getBuildInfo()
				+ "<br><b>" +Messages.getString("JFXMain.visitarUrsulaGIS.com")+"</b>"; // //$NON-NLS-2$ //$NON-NLS-3$

		WebView webView = new WebView();
		webView.getEngine().loadContent("<html>"+content+"</html>"); // //$NON-NLS-2$


		//   webView.setPrefSize(150, 60);
		//acercaDe.setHeaderText(""); //
		//acercaDe.setGraphic(null);
		acercaDe.getDialogPane().setHeader(null);

		acercaDe.getDialogPane().setContent(webView);;
		//  alert.showAndWait();
		acercaDe.setResizable(true);
		acercaDe.show();
	}

	/**
	 * open an alert with te console log dump into a text area
	 */
	private void doShowLog() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.initOwner(JFXMain.stage);
		alert.setTitle("Log");
		alert.setHeaderText(null);
		TextArea textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setWrapText(true);
		
		// Store original streams
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		
		// Create custom output streams that capture to textArea
		PrintStream customOut = new PrintStream(new OutputStream() {
			private StringBuilder buffer = new StringBuilder();
			
			@Override
			public void write(int b) throws IOException {
				char c = (char) b;
				if (c == '\n') {
					String line = buffer.toString();
					buffer = new StringBuilder();
					Platform.runLater(() -> {
						textArea.appendText(line + "\n");
						textArea.setScrollTop(Double.MAX_VALUE);
					});
					// Also write to original stream
					originalOut.println(line);
				} else if (c != '\r') {
					buffer.append(c);
				}
			}
		});
		
		PrintStream customErr = new PrintStream(new OutputStream() {
			private StringBuilder buffer = new StringBuilder();
			
			@Override
			public void write(int b) throws IOException {
				char c = (char) b;
				if (c == '\n') {
					String line = buffer.toString();
					buffer = new StringBuilder();
					Platform.runLater(() -> {
						textArea.appendText(line + "\n");
						textArea.setScrollTop(Double.MAX_VALUE);
					});
					// Also write to original stream
					originalErr.println(line);
				} else if (c != '\r') {
					buffer.append(c);
				}
			}
		});
		
		// Redirect System.out and System.err to our custom streams
		System.setOut(customOut);
		System.setErr(customErr);
		
		// Add some test messages
		System.out.println("Log viewer opened - capturing console output");
		System.err.println("This error message should appear in the log");
		
		// Set up the dialog
		alert.getDialogPane().setContent(textArea);
		alert.getDialogPane().setPrefSize(800, 600);
		alert.setResizable(true);
		
		// Restore original streams when dialog is closed
		alert.setOnCloseRequest(e -> {
			System.setOut(originalOut);
			System.setErr(originalErr);
		});
		
		alert.initModality(Modality.NONE);
		alert.show();
	}

	public static void doConfigCultivo() {
		Platform.runLater(()->{
			final ObservableList<Cultivo> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllCultivos()
							);

			SmartTableView<Cultivo> table = new SmartTableView<Cultivo>(dataLotes,
					Arrays.asList("Id","TasaCrecimientoPendiente","TasaCrecimientoOrigen"),
					Arrays.asList("Nombre","Estival",
							"AbsN","ExtN",
							"AbsP","ExtP",
							"AbsK","ExtK",
							"AbsS","ExtS",
							"AporteMO","RindeEsperado"
							)
					);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Cultivo(Messages.getString("JFXMain.372"))); //

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			//tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.373")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	public static void doConfigFertilizantes() {
		Platform.runLater(()->{

			//			ArrayList<Fertilizante> ciLista = new ArrayList<Fertilizante>();
			//			System.out.println("Comenzando a cargar la los datos de la tabla");
			//
			//			ciLista.addAll(Fertilizante.fertilizantes.values());
			//			final ObservableList<Fertilizante> dataLotes =
			//					FXCollections.observableArrayList(
			//							ciLista
			//							);

			final ObservableList<Fertilizante> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllFertilizantes()
							);

			SmartTableView<Fertilizante> table = new SmartTableView<Fertilizante>(dataLotes,
					Arrays.asList("Id"),//rejected
					Arrays.asList("Nombre","PorcN","PorcP","PorcK","PorcS")//order
					);//,dataLotes);
			table.setEditable(true);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setOnDoubleClick(()->new Fertilizante(Messages.getString("JFXMain.374"))); //
			table.setEliminarAction(
					list->{
						Platform.runLater(()->{		
							try {
								System.out.println("removing fertilizantes "+list.size());
								List<Object> objs = new ArrayList<>(list);
								DAH.removeAll(objs);
								DAH.commitTransaction();
							}catch(Exception e) {
								e.printStackTrace();
								DAH.rollbackTransaction();
							}
						});
					}
					);
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.375")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});
	}

	public static void doConfigAgroquimicos() {
		Platform.runLater(()->{
			final ObservableList<Agroquimico> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllAgroquimicos()
							);
			SmartTableView<Agroquimico> table = new SmartTableView<Agroquimico>(dataLotes,
					Arrays.asList("Id"),     //rejected
					Arrays.asList("Activo","NumRegistro","Nombre","Empresa","Activos","BandaToxicologica"));//order
			table.setEditable(true);
			table.getSelectionModel().setSelectionMode(
					SelectionMode.MULTIPLE
					);
			table.setEliminarAction(list->{
				//System.out.println("eliminando agroquimicos "+list);
				List<Object> toRemove = new ArrayList<Object>();
				//System.out.println("agregando a toRemove "+list);
				toRemove.addAll(list);
				//JFXMain.executorPool.execute(()->{
				try {
					DAH.beginTransaction();

					//System.out.println("items en toRemove "+toRemove);
					DAH.removeAll(toRemove);
					DAH.commitTransaction();
					//System.out.println("termine de eliminar "+toRemove);
				}catch(Exception e) {					
					//System.out.println("no se pudo borrar");
					DAH.rollbackTransaction();
					e.printStackTrace();
				}
			});


			table.setOnDoubleClick(()->new Agroquimico(Messages.getString("JFXMain.376"))); //

			table.addSecondaryClickConsumer(Messages.getString("SmartTableView.Activar"),(r)-> {
				doToggleAgroquimico(r);
			});

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.377")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	public static void doToggleAgroquimico(Agroquimico r) {
		//		System.out.println("Activando agroquimicos "+r);
		//		List<Object> toToggleActivate = new ArrayList<Object>();
		//		System.out.println("agregando a toToggleActivate "+r);
		//		toToggleActivate.add(r);
		try {
			// System.out.println("Toggleando activo " + r);
			DAH.beginTransaction();
			r.toggleActivo();
			DAH.commitTransaction();
			// System.out.println("termine de activar/desactivar " + r);
		}
		catch(Exception e) {					
			// System.out.println("no se pudo cambiar el estado del item " + r);
			DAH.rollbackTransaction();
			e.printStackTrace();
		}
	}

	public static void doConfigCampania() {
		Platform.runLater(()->{
			final ObservableList<Campania> data =
					FXCollections.observableArrayList(
							DAH.getAllCampanias()							
							);
			if(data.size()==0){
				data.add(new Campania(Messages.getString("JFXMain.378")));//TODO obtener el anio actual y armar 16/17 //
			}
			SmartTableView<Campania> table = new SmartTableView<Campania>(data,
					Arrays.asList("Id"),
					Arrays.asList("Nombre","Inicio","Fin")
					);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Campania(Messages.getString("JFXMain.379"))); //
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.380")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}

	public void doConfigPoligonos() {
		Platform.runLater(()->{
			final ObservableList<Poligono> data =
					FXCollections.observableArrayList(
							DAH.getAllPoligonos()
							);

			SmartTableView<Poligono> table = new SmartTableView<Poligono>(data,
					Arrays.asList("Id","PoligonoToString"),
					Arrays.asList("Activo","Nombre","Lote","Area","PositionsString")
					);
			table.setEditable(true);
			table.setEliminarAction(list->{
				//JFXMain.executorPool.execute(()->{
				try {
					//decidir que hacer con los ndvi. borralos o dejarlos huerfanos
					DAH.beginTransaction();
					list.stream().forEach(p->{
						p.setLote(null);
						List<Ndvi> ndviPoli = DAH.getNdvi(p);
						ndviPoli.stream().forEach(n->{
							n.setContorno(null);
							DAH.save(n);
							System.out.println("quitando poligono de "+n);
						});
						System.out.println("eliminando "+p);

					});
					List<Object> toRemove = new ArrayList<Object>();
					toRemove.addAll(list);
					DAH.removeAll(toRemove);
					DAH.commitTransaction();
				}catch(Exception e) {

					System.out.println("no se pudo borrar");
					e.printStackTrace();					
				}
				//});
			}
					);
			table.getSelectionModel().setSelectionMode(
					SelectionMode.MULTIPLE
					);
			table.setOnDoubleClick(()->new Poligono());

			//PoligonoGUIController controller =  new PoligonoGUIController(main);
			table.setOnShowClick((poli)->{
				poli.setActivo(true);
				main.poligonoGUIController.showPoligonos(Collections.singletonList(poli));
				//controller.showPoligonos(Collections.singletonList(poli));
				if(poli.getPositions().size()>0) {
					Position pos =poli.getPositions().get(0);
					main.viewGoTo(pos);
					Platform.runLater(()->{
						DAH.save(poli);
					});
				}
			});



			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.381")); //
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});
			try {
				tablaStage.show();	 
			}catch(Exception e){
				//aca lanza un error de que no encuentra el column header. probablemente porque en java 10 no tiene permiso para acceder al internal api
				//java.lang.NoSuchMethodException: javafx.scene.control.skin.TableViewSkin.getTableHeaderRow()

			}
		});	
	}

	public void doShowRecorridaTable() {
		main.recorridaGUIController.doShowRecorridaTable(DAH.getAllRecorridas());

	}

	/**
	 * metodo que toma una cosecha y permite asignar valores para cada clase de la cosecha
	 * @param cosecha
	 * @return devuelve el map con los valores asignados
	 */
	public Map<String,Double[]> doAsignarValoresCosecha(CosechaLabor cosecha,String[] column_Valores) {
		//List<Muestra> muestras = cosecha.getMuestras();
		//Map<String, List<Muestra>> nombresMuestraMap = muestras.stream().collect(Collectors.groupingBy(Muestra::getNombre));

		List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();


		//for(String nombre : nombresMuestraMap.keySet()) {
		// Map<String,Number> props =null;
		String column_Nombre = 	Messages.getString("ProcessMapTask.categoria");//"Clase";
		//String column_Valor = "Densidad pl/m2";
		int clases = cosecha.getClasificador().getNumClasses();
		///System.out.println("clases "+clases);
		for(int i = clases-1;  i>-1; i--) {	 
			//System.out.print("categoria "+i);
			String nombre = cosecha.getClasificador().getLetraCat(i);		 
			//System.out.println(" letra "+nombre);
			Map<String,Object> initialD = new LinkedHashMap<String,Object>();
			initialD.put(column_Nombre, nombre);
			for(String column_Valor : column_Valores) {
				initialD.put(column_Valor, 0.0);
			}
			data.add(initialD);
		}

		TableView<Map<String,Object>> tabla = new TableView<Map<String,Object>>( FXCollections.observableArrayList(data));
		tabla.setEditable(true);

		TableColumn<Map<String,Object>,String> columnNombre = new TableColumn<Map<String,Object>,String>(column_Nombre);
		columnNombre.setEditable(false);
		columnNombre.setCellFactory(TextFieldTableCell.forTableColumn());
		columnNombre.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
						stringValue =(String)  cellData.getValue().get(column_Nombre);						
						return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
						//System.out.println("La creacion de SimpleStringProperty en getStringColumn "+name +" con valor: "+stringValue);

						return new SimpleStringProperty("sin datos");
					}
				});
		columnNombre.setOnEditCommit(cellEditingEvent -> { 
			int row = cellEditingEvent.getTablePosition().getRow();
			Map<String,Object> p = cellEditingEvent.getTableView().getItems().get(row);
			try {
				p.put(column_Nombre, cellEditingEvent.getNewValue());
				tabla.refresh();
			} catch (Exception e) {	e.printStackTrace();}
		});		

		tabla.getColumns().add(columnNombre);

		for(String column_Valor : column_Valores) {
			DoubleTableColumn<Map<String,Object>> dColumn = new DoubleTableColumn<Map<String,Object>>(column_Valor,
					(p)->{	try {
						Number n =(Number) p.get(column_Valor);
						if(n!=null) {
							return n.doubleValue();
						} else {
							return 0.0;
						}
					} catch (Exception e) {	e.printStackTrace();}
					return null;
					},(p,d)->{ try {

						p.put(column_Valor,d);
						tabla.refresh();
					} catch (Exception e) {	e.printStackTrace();}
					});
			dColumn.setEditable(true);
			tabla.getColumns().add(dColumn);			
		}

		BorderPane bp = new BorderPane();
		bp.setCenter(tabla);
		Button accept =new Button(Messages.getString("Recorrida.Aceptar"));
		bp.setBottom(accept);
		Scene scene = new Scene(bp, 400, 300);
		Stage tablaStage = new Stage();
		//tablaStage.getIcons().addAll(JFXMain.stage.getIcons());		
		tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
		tablaStage.setTitle(Messages.getString("Recorrida.asignarValores")); //
		tablaStage.setScene(scene);

		accept.setOnAction((e)->{tablaStage.close();});


		tablaStage.showAndWait();	 
		Map<String,Double[]> ret = new HashMap<String,Double[]>();
		for(Map<String,Object> ma : data) {
			String k = (String) ma.get(column_Nombre);
			//for(String column_Valor : column_Valores) {
			Double [] valor = new  Double[column_Valores.length];
			for(int i=0;i<column_Valores.length;i++) {
				Double d = (Double) ma.get(column_Valores[i]);
				valor[i]=d;
			}
			ret.put(k, valor);

		}
		return ret;
	}





	public void doShowOrdenCompraItems(OrdenCompra oc) {
		Platform.runLater(()->{
			final ObservableList<OrdenCompraItem> data =
					FXCollections.observableArrayList(
							oc.getItems()
							);

			SmartTableView<OrdenCompraItem> table = new SmartTableView<OrdenCompraItem>(data,
					Arrays.asList("Id"),//rejected
					Arrays.asList("Producto","Cantidad","Precio","Importe")//order
					);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			//			table.setOnShowClick((ndvi)->{
			//				//poli.setActivo(true);
			//				doShowNDVI(ndvi);
			//
			//			});

			VBox v=new VBox();
			VBox.setVgrow(table, Priority.ALWAYS);
			v.getChildren().add(table);
			HBox hBoxTotal = new HBox();

			/*You use a container if you need add many nodes in CustomTextField*/
			//HBox box=new HBox();
			//box.getChildren().add(myLabel);
			//			TextField total=new TextField();
			//			total.setEditable(false);

			oc.calcImporteTotal();
			Double total = oc.getImporteTotal();
			if(total == null) {
				System.out.println("total es null");
				total=0.0;
			}
			DoubleProperty dp = new SimpleDoubleProperty();
			dp.set(total);

			NumberFormat numberFormat = Messages.getNumberFormat();
			// Dejar 2 decimales nada mas
			numberFormat.setMaximumFractionDigits(2);

			String totalString = numberFormat.format(total);
			Label importeTotalLabel = new Label("Importe total: "+totalString);			
			//TODO actualizar el importe total cuando se cambia el precio o la cantidad
			importeTotalLabel.setPadding(new Insets(5,5,5,5));//ar,d,ab,izq
			hBoxTotal.getChildren().add(importeTotalLabel);		
			v.getChildren().add(hBoxTotal);

			HBox h = new HBox();

			Button guardarB = new Button(Messages.getString("JFXMain.saveAction"));//TODO traducir
			guardarB.setOnAction(actionEvent->{
				//System.out.println("implementar GuardarOrden de compra action");
				DAH.save(oc);
			});

			Button exportarB = new Button(Messages.getString("JFXMain.exportar"));//TODO traducir
			exportarB.setOnAction(actionEvent->{
				ExcelHelper helper = new ExcelHelper();
				helper.exportOrdenCompra(oc);
			});

			Button cotizarOblineB = new Button(Messages.getString("ShowConfigGUI.cotizarOnline"));//"Cotizar OnLine");//TODO traducir
			cotizarOblineB.setOnAction(actionEvent->{
				//preguntar mail para enviar presupuestos
				TextInputDialog tDialog = new TextInputDialog("tomas@ursulagis.com");//"tomas@ursulagis.com"
				tDialog.setContentText("e-mail");				
				tDialog.getEditor().setPromptText("tomas@ursulagis.com");
				tDialog.initOwner(JFXMain.stage);
				tDialog.setTitle(Messages.getString("ShowConfigGUI.cotizarOnlineMailTitle"));
				tDialog.showAndWait();

				String mail = tDialog.getResult();

				oc.setMail(mail);
				CotizarOdenDeCompraOnlineTask cotTask= new CotizarOdenDeCompraOnlineTask(oc); 
				cotTask.setOnSucceeded((wse)->{
					String url=oc.getUrl();
					if(url==null) {
						url= "https://www.ursulagis.com/api/orden_compra/show/"+oc.getUuid()+"/";
					}
					showQR(url);	
				});

				JFXMain.executorPool.execute(cotTask);
				// enviar orden de compra a la nube. preguntar mail de contacto y subir la orden de compra a la nube


			});
			h.getChildren().addAll(guardarB,exportarB,cotizarOblineB);

			v.getChildren().add(h);
			Scene scene = new Scene(v, 400, 300);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.OrdenCompra")); //
			tablaStage.setScene(scene);
			tablaStage.heightProperty().addListener((obj,old,nu)->{
				v.setPrefHeight(nu.doubleValue());
			});
			tablaStage.widthProperty().addListener((obj,old,nu)->{
				v.setPrefWidth(nu.doubleValue());
			});
			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});

			tablaStage.show();	 
		});	

	}

	public void doShowOrdenesCompra() {		
		Platform.runLater(()->{		
			final ObservableList<OrdenCompra> data =
					FXCollections.observableArrayList(DAH.getAllOrdenesCompra());

			SmartTableView<OrdenCompra> table = new SmartTableView<OrdenCompra>(data,
					Arrays.asList("Id","Url","Uuid"),
					Arrays.asList("Description","ImporteTotal","Mail")
					);
			table.setEditable(true);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setOnShowClick((oc)->{
				this.doShowOrdenCompraItems(oc);				

			});

			table.setEliminarAction(
					list->{
						Platform.runLater(()->{		
							try {
								//System.out.println("removing ordenes de compra "+list.size());
								DAH.beginTransaction();						
								//							list.stream().forEach(oc->{
								//								DAH.removeAll(oc.getItems());	
								//							});
								//							DAH.save(list.get(0).getRecorrida());
								List<Object> objs = new ArrayList<Object>(list);
								DAH.removeAll(objs);
								DAH.commitTransaction();
							}catch(Exception e) {
								e.printStackTrace();
								DAH.rollbackTransaction();
							}
						});
					}
					);


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.OrdenCompra")); //
			tablaStage.setScene(scene);

			//			tablaStage.onHiddenProperty().addListener((o,old,n)->{
			//				main.getLayerPanel().update(main.getWwd());				
			//			});

			tablaStage.show();	 
		});
	}


	public void doShowLaboresTable() {
		List<? extends Labor<?>> recorridas = DAH.getAllLabores();
		//		Platform.runLater(()->{
		//			final ObservableList<? extends Labor<?>> data = FXCollections.observableArrayList(recorridas);
		//
		//			SmartTableView<? extends Labor<?>> table = new SmartTableView<Labor>(data,
		//					Arrays.asList("Id","Posicion"),
		//					Arrays.asList("Nombre","Observacion","Latitude","Longitude")
		//					);
		//			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
		//			table.setEditable(true);
		//			//			table.setOnDoubleClick(()->new Poligono());
		//			table.setOnShowClick((recorrida)->{
		//				//poli.setActivo(true);
		//				//main.doShowRecorrida(recorrida);
		//			});
		//			
		//			table.addSecondaryClickConsumer("Editar",(r)-> {
		//				//doShowMuestrasTable(r.getMuestras());
		//			});
		//
		//			Scene scene = new Scene(table, 800, 600);
		//			Stage tablaStage = new Stage();
		//			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
		//			tablaStage.setTitle(Messages.getString("JFXMain.configRecorridaMI")); //
		//			tablaStage.setScene(scene);
		//
		//			tablaStage.onHiddenProperty().addListener((o,old,n)->{
		//				main.getLayerPanel().update(main.getWwd());
		//				//getWwd().redraw();
		//			});
		//
		//			tablaStage.show();	 
		//		});	

	}



	public void doShowOrdenesSiembraTable() {
		Platform.runLater(()->{
			List<OrdenSiembra> ordenes = DAH.getAllOrdenesSiembra();
			final ObservableList<OrdenSiembra> data = FXCollections.observableArrayList(ordenes);

			SmartTableView<OrdenSiembra> table = new SmartTableView<OrdenSiembra>(data,
					Arrays.asList("Id","PoligonoString","Uuid","Url","OrdenShpZipUrl","Owner","Items"),
					Arrays.asList("NumeroOrden",
							"Fecha",
							"Nombre",
							"Description",
							"Productor",
							"Establecimiento",
							"NombreIngeniero",
							"Contratista",
							"Cultivo",
							"Estado","Superficie"							
							),
					Arrays.asList("Numero",
							"Fecha",
							"Nombre",
							"Descripcion",
							"Productor",
							"Establecimiento",
							"Ingeniero",
							"Contratista",
							"Cultivo",
							"Estado","Superficie"							
							)
					);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((recorrida)->{
				showQR(recorrida.url);
				//TODO descargar el archivo e importarlo
				//poli.setActivo(true);
				//main.doShowRecorrida(recorrida);
			});

			table.addSecondaryClickConsumer("Editar",(r)-> {
				//doShowMuestrasTable(r.getMuestras());
			});

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.configSiembraMI")); //
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});

			tablaStage.show();	 
		});	
	}

	private void doShowConfiguracionTable() {
		List<Map<String,String>> data = new ArrayList<Map<String,String>>();
		String CLAVE_COLUMN_NOMBRE=Messages.getString("ConfigGUI.configClave");//"Clave";
		String VALOR_COLUMN_NOMBRE=Messages.getString("ConfigGUI.configValor");//"Valor";
		Configuracion config = Configuracion.getInstance();
		for(String key : config.getAllPropertyNames()) {
			Map<String,String> map = new HashMap<String,String>();
			map.put(CLAVE_COLUMN_NOMBRE, key);
			map.put(VALOR_COLUMN_NOMBRE, config.getPropertyOrDefault(key, ""));
			data.add(map);
		}


		TableView<Map<String,String>> tabla = new TableView<Map<String,String>>( FXCollections.observableArrayList(data));
		tabla.setEditable(true);

		TableColumn<Map<String,String>,String> columnNombre = new TableColumn<Map<String,String>,String>(CLAVE_COLUMN_NOMBRE);
		columnNombre.setEditable(false);
		columnNombre.setCellFactory(TextFieldTableCell.forTableColumn());
		columnNombre.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
						stringValue =(String)  cellData.getValue().get(CLAVE_COLUMN_NOMBRE);						
						return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
						//System.out.println("La creacion de SimpleStringProperty en getStringColumn "+name +" con valor: "+stringValue);

						return new SimpleStringProperty("sin datos");
					}
				});
		columnNombre.setOnEditCommit(cellEditingEvent -> { 
			int row = cellEditingEvent.getTablePosition().getRow();
			Map<String,String> p = cellEditingEvent.getTableView().getItems().get(row);
			try {
				p.put(CLAVE_COLUMN_NOMBRE, cellEditingEvent.getNewValue());
				tabla.refresh();
			} catch (Exception e) {	e.printStackTrace();}
		});		

		tabla.getColumns().add(columnNombre);

		TableColumn<Map<String,String>,String> columnValor = new TableColumn<Map<String,String>,String>(VALOR_COLUMN_NOMBRE);
		columnValor.setEditable(false);
		columnValor.setCellFactory(TextFieldTableCell.forTableColumn());
		columnValor.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
						stringValue =(String)  cellData.getValue().get(VALOR_COLUMN_NOMBRE);						
						return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
						//System.out.println("La creacion de SimpleStringProperty en getStringColumn "+name +" con valor: "+stringValue);

						return new SimpleStringProperty("sin datos");
					}
				});
		columnValor.setOnEditCommit(cellEditingEvent -> { 
			int row = cellEditingEvent.getTablePosition().getRow();
			Map<String,String> p = cellEditingEvent.getTableView().getItems().get(row);
			try {
				p.put(VALOR_COLUMN_NOMBRE, cellEditingEvent.getNewValue());
				tabla.refresh();
			} catch (Exception e) {	e.printStackTrace();}
		});		
		columnValor.setEditable(true);

		tabla.getColumns().add(columnValor);		


		BorderPane bp = new BorderPane();
		bp.setCenter(tabla);
		Button accept =new Button(Messages.getString("Recorrida.Aceptar"));
		bp.setBottom(accept);
		Scene scene = new Scene(bp, 400, 300);
		Stage tablaStage = new Stage();
		tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
		tablaStage.setTitle(Messages.getString("Recorrida.asignarValores")); //
		tablaStage.setScene(scene);

		accept.setOnAction((e)->{tablaStage.close();});


		tablaStage.showAndWait();	 

		for(Map<String,String> ma : data) {
			String k = (String) ma.get(CLAVE_COLUMN_NOMBRE);
			String v = (String) ma.get(VALOR_COLUMN_NOMBRE);
			config.setProperty(k, v);
		}
		config.save();

	}

	public void doShowOrdenesPulverizacionTable() {
		Platform.runLater(()->{
			List<OrdenPulverizacion> ordenes = DAH.getAllOrdenesPulverizacion();
			final ObservableList<OrdenPulverizacion> data = FXCollections.observableArrayList(ordenes);

			SmartTableView<OrdenPulverizacion> table = new SmartTableView<OrdenPulverizacion>(data,
					Arrays.asList("Id","PoligonoString","Uuid","Url","OrdenShpZipUrl","Owner","Items"),
					Arrays.asList("NumeroOrden",
							"Fecha",
							"Nombre",
							"Description",
							"Productor",
							"Establecimiento",
							"NombreIngeniero",
							"Contratista",
							"Cultivo",
							"Estado","Superficie"							
							),
					Arrays.asList("Numero",
							"Fecha",
							"Nombre",
							"Descripcion",
							"Productor",
							"Establecimiento",
							"Ingeniero",
							"Contratista",
							"Cultivo",
							"Estado","Superficie"							
							)
					);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((op)->{
				showQR(op.url);
				//TODO descargar el archivo e importarlo
				//poli.setActivo(true);
				//main.doShowRecorrida(recorrida);
			});

			table.addSecondaryClickConsumer("Editar",(r)-> {
				//doShowMuestrasTable(r.getMuestras());
			});

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.configPulverizacionMI")); //
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});

			tablaStage.show();	 
		});	
	}


	public void doShowOrdenesFertilizacionTable() {
		Platform.runLater(()->{
			List<OrdenFertilizacion> ordenes = DAH.getAllOrdenesFertilizacion();
			final ObservableList<OrdenFertilizacion> data = FXCollections.observableArrayList(ordenes);

			SmartTableView<OrdenFertilizacion> table = new SmartTableView<OrdenFertilizacion>(data,
					Arrays.asList("Id","PoligonoString","Uuid","Url","OrdenShpZipUrl","Owner","Items"),
					Arrays.asList("NumeroOrden",
							"Fecha",
							"Nombre",
							"Description",
							"Productor",
							"Establecimiento",
							"NombreIngeniero",
							"Contratista",
							"Cultivo",
							"Estado","Superficie"							
							),
					Arrays.asList("Numero",
							"Fecha",
							"Nombre",
							"Descripcion",
							"Productor",
							"Establecimiento",
							"Ingeniero",
							"Contratista",
							"Cultivo",
							"Estado","Superficie"							
							)
					);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((op)->{
				showQR(op.url);
				//TODO descargar el archivo e importarlo
				//poli.setActivo(true);
				//main.doShowRecorrida(recorrida);
			});

			//FIXME cambiar Editar por un mensaje con traduccion
			//			table.addSecondaryClickConsumer("Editar",(r)-> {
			//				//doShowMuestrasTable(r.getMuestras());
			//			});

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.configFertilizacionMI")); //
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});

			tablaStage.show();	 
		});	
	}




	public void doShowNdviTable() {
		Platform.runLater(()->{
			final ObservableList<Ndvi> data =
					FXCollections.observableArrayList(
							DAH.getAllNdvi()
							);

			SmartTableView<Ndvi> table = new SmartTableView<Ndvi>(data,
					Arrays.asList("Id","PixelArea","FileName"),
					Arrays.asList("Activo","Contorno","Fecha","Nombre","MeanNdvi","PorcNubes")
					);
			table.getSelectionModel().setSelectionMode(
					SelectionMode.MULTIPLE
					);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((ndvi)->{
				ndvi.setActivo(true);
				main.ndviGUIController.doShowNDVI(ndvi);
			});


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.382")); //
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});

			tablaStage.show();	 
		});	
	}

	public static void doChangeLocale() {
		List<Locale> locales = Messages.getLocales();

		Function<Locale,String> capitalizeLocale = (Locale loc) ->{
			String key = loc.getDisplayLanguage(Messages.getLocale());
			key = key.substring(0, 1).toUpperCase() + key.substring(1);
			return key;
		};

		Map<String, Locale> displayLocales =
				locales.stream().collect(Collectors.toMap(capitalizeLocale,
						(Locale loc) -> loc));
		Locale actual = Messages.getLocale();

		ChoiceDialog<String> dialog = new ChoiceDialog<>(capitalizeLocale.apply(actual), displayLocales.keySet());
		dialog.setTitle(Messages.getString("JFXMain.383")); //
		dialog.setHeaderText(Messages.getString("JFXMain.384")); //
		dialog.setContentText(Messages.getString("JFXMain.385")); //
		dialog.initOwner(JFXMain.stage);

		Optional<String> result = dialog.showAndWait();
		// The Java 8 way to get the response value (with lambda expression).
		result.ifPresent(newLocale -> {
			Locale selected = displayLocales.get(newLocale);
			Messages.setLocale(selected);	
		});

		//TODO redibujar la ventana principal con el nuevo locale
	}

	public static void doConfigEstablecimiento() {
		Platform.runLater(()->{

			final ObservableList<Establecimiento> data =
					FXCollections.observableArrayList(
							DAH.getAllEstablecimientos()
							);
			if(data.size()<1){
				data.add(new Establecimiento());
			}

			SmartTableView<Establecimiento> table = new SmartTableView<Establecimiento>(data,
					Arrays.asList("Id"),     //rejected
					Arrays.asList("Nombre","Contorno","Empresa","SuperficieTotal","SuperficieAgricola","SuperficieGanadera","SuperficieDesperdicio"));//order
			table.setEditable(true);
			table.setOnDoubleClick(()->new Establecimiento(Messages.getString("JFXMain.386"))); //

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.387")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}


	public static void doConfigLote() {
		Platform.runLater(()->{
			final ObservableList<Lote> data =
					FXCollections.observableArrayList(
							DAH.getAllLotes()
							);
			if(data.size()<1){
				data.add(new Lote());
			}
			SmartTableView<Lote> table = new SmartTableView<Lote>(data,
					Arrays.asList("Id"),                 //rejected
					Arrays.asList("Nombre","Contorno"));//order
			table.setEditable(true);
			table.setOnDoubleClick(()->new Lote(Messages.getString("JFXMain.388"))); //

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.389")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}

	public static void doConfigAsignacion() {
		Platform.runLater(()->{
			final ObservableList<Asignacion> data =
					FXCollections.observableArrayList(
							DAH.getAllAsignaciones()
							);
			if(data.size()<1){
				data.add(new Asignacion());
			}
			SmartTableView<Asignacion> table = new SmartTableView<Asignacion>(data,
					Arrays.asList("Id"),                 //rejected
					Arrays.asList("Lote","Campania","Cultivo","Contorno","Superficie"));//order
			table.setEditable(true);
			table.setOnDoubleClick(()->new Asignacion()); //

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.configAsignacionMI")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}

	public static void doConfigEmpresa() {
		Platform.runLater(()->{
			final ObservableList<Empresa> data =
					FXCollections.observableArrayList(
							DAH.getAllEmpresas()
							);
			if(data.size()<1){
				data.add(new Empresa(Messages.getString("JFXMain.390"))); //
			}
			SmartTableView<Empresa> table = new SmartTableView<Empresa>(data,
					Arrays.asList("Id"),     //rejected
					Arrays.asList("Nombre"));//order
			table.setEditable(true);
			table.setOnDoubleClick(()->new Empresa(Messages.getString("JFXMain.391"))); //

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.392")); //
			tablaStage.setScene(scene);
			tablaStage.show();	 

		});	
	}

	public static void doConfigSemillas() {
		Platform.runLater(()->{

			//ArrayList<Semilla> ciLista = new ArrayList<Semilla>();
			//System.out.println("Comenzando a cargar la los datos de la tabla");

			//ciLista.addAll(Semilla.semillas.values());
			final ObservableList<Semilla> dataLotes =
					FXCollections.observableArrayList(	DAH.getAllSemillas());
			//System.out.println("mostrando la tabla de las semillas con "+dataLotes);
			SmartTableView<Semilla> table = new SmartTableView<Semilla>(dataLotes,
					Arrays.asList("Id"),     //rejected
					Arrays.asList("Nombre","Cultivo","PesoDeMill","PG"));//order

			table.setEditable(true);
			table.setOnDoubleClick(()->new Semilla(Messages.getString("JFXMain.393"),DAH.getAllCultivos().get(0))); //
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.394"));
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});
	}

	/**
	 * Funcion util para vincular un metodo con un item en un menu
	 * @param name nombre para mostrar en el menu
	 * @param action metodo a vincular con el item
	 * @param parent menu en el que se va a insertar el menuItem
	 */
	private static MenuItem addMenuItem(String name, EventHandler<ActionEvent> action, Menu parent){
		MenuItem menuItemProductos = new MenuItem(name);
		menuItemProductos.setOnAction(action);
		parent.getItems().addAll(menuItemProductos);
		return menuItemProductos;
	}

	public  void showQR(String ret) {
		BufferedImage qr = ConfigGUI.generateQR(ret);
		Image image = SwingFXUtils.toFXImage(qr, null);

		Alert a = new Alert(AlertType.INFORMATION);
		a.getButtonTypes().clear();
		Window window = a.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(e -> a.hide());         
            
		ImageView view = new ImageView();
		a.initOwner(JFXMain.stage);
		a.setTitle("Qr Code");
		view.setImage(image);
		VBox v = new VBox();
		v.getChildren().add(view);
		Hyperlink link = new Hyperlink("link");
		link.setOnAction(new EventHandler<ActionEvent>() {
		    public void handle(ActionEvent event) {
				
		        //Hyperlink link = (Hyperlink)event.getSource();
				OpenBrowserUrl.browseTo(ret);
		        //final String str = link == null ? "" : link.getText();
		
		     }
		});

		//TextField link = new TextField(ret);
		//link.setEditable(false);
		v.getChildren().add(link);

		a.setResizable(true);
		a.setGraphic(v);
		a.setHeaderText("");
		//a.setWidth(400);
		a.getDialogPane().setPrefWidth(image.getWidth());
		//a.setContentText(ret);
		a.show();
	}

	private void doUpdate() {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setContentText(Messages.getString("JFXMain.doUpdateText")); 
		alert.initOwner(JFXMain.stage);
		alert.showAndWait();
		if(ButtonType.OK.equals(alert.getResult())){
			UpdateTask uTask = new UpdateTask();
			uTask.installProgressBar(main.progressBox);
			uTask.setOnSucceeded(handler -> {
				File newVersion = (File) handler.getSource().getValue();	
				if(newVersion==null){
					Alert error = new Alert(Alert.AlertType.ERROR);
					error.setContentText(Messages.getString("JFXMain.doUpdateErrorText")); 
					error.initOwner(JFXMain.stage);
					error.showAndWait();
				} else{
					Alert error = new Alert(Alert.AlertType.CONFIRMATION);
					error.setContentText(Messages.getString("JFXMain.doUpdateSuccessText")); 
					error.initOwner(JFXMain.stage);
					error.showAndWait();
				}			
				uTask.uninstallProgressBar();
			});
			JFXMain.executorPool.submit(uTask);
		}//fin del if OK	
	}

	public static BufferedImage generateQR(String code) {
		QRCodeWriter barcodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix;
		try {
			bitMatrix = barcodeWriter.encode(code, BarcodeFormat.QR_CODE, 200, 200);
			return MatrixToImageWriter.toBufferedImage(bitMatrix);
		} catch (WriterException e) {
			e.printStackTrace();
			return null;
		}


	}


}
