package gui;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import dao.Ndvi;
import dao.Poligono;
import dao.config.Agroquimico;
import dao.config.Campania;
import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Fertilizante;
import dao.config.Lote;
import dao.config.Semilla;
import gov.nasa.worldwind.geom.Position;
import gui.utils.DateConverter;
import gui.utils.SmartTableView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import utils.DAH;

public class ShowConfigGUI {
	private static final String DD_MM_YYYY = "dd/MM/yyyy";
	JFXMain main=null;
	
	public ShowConfigGUI(JFXMain _main) {
		this.main=_main;		
	}

	public static String getBuildInfo() {
		//Esta aplicacion fue compilada el 11 de Febrero de 2020.
	
	
		DateTimeFormatter inDTFormater = DateTimeFormatter.ofPattern(DD_MM_YYYY);
		LocalDate compileDate =  LocalDate.parse(JFXMain.buildDate, inDTFormater);	
		//DateFormat outDTFormater = DateFormat.getDateInstance(DateFormat.FULL, Messages.getLocale());
		
		DateTimeFormatter dft= DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
		dft = dft.withLocale(Messages.getLocale());
		
		//String localizedBuidDate = outDTFormater.format(DateConverter.asDate(compileDate));
		String localizedBuidDate =  dft.format(compileDate);
	//Esta aplicacion fue compilada el 11 de Febrero de 2020.
		 return Messages.getString("JFXMain.info1")+localizedBuidDate //TODO crear una dateTimeFomrater y localizar el formato segun el locale Elegido
					+Messages.getString("JFXMain.info2") //$NON-NLS-1$ version compilada el 
					+Messages.getString("JFXMain.inf3") //$NON-NLS-1$
					+Messages.getString("JFXMain.info3") //$NON-NLS-1$
					+Messages.getString("JFXMain.info4"); //$NON-NLS-1$
	}
	
	public Menu contructConfigMenu() {
		/*Menu Configuracion*/
		final Menu menuConfiguracion = new Menu(Messages.getString("JFXMain.configuracionMenu")); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.cultivosMenuItem"),(a)->doConfigCultivo(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.fertilizantesMenuItem"),(a)->doConfigFertilizantes(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.agroquimicosMenuItem"),(a)->doConfigAgroquimicos(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configSemillasMenuItem"),(a)->doConfigSemillas(),menuConfiguracion); //$NON-NLS-1$

		addMenuItem(Messages.getString("JFXMain.configEmpresaMI"),(a)->doConfigEmpresa(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configEstablecimientoMI"),(a)->doConfigEstablecimiento(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configLoteMi"),(a)->doConfigLote(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configCampaniaMI"),(a)->doConfigCampania(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configPoligonosMI"),(a)->doConfigPoligonos(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configNDVIMI"),(a)->doShowNdviTable(),menuConfiguracion); //$NON-NLS-1$
		//addMenuItem("Labores",(a)->doShowLaboresTable(),menuConfiguracion);

		addMenuItem(Messages.getString("JFXMain.configIdiomaMI"),(a)->doChangeLocale(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.configHelpMI"),(a)->doShowAcercaDe(),menuConfiguracion); //$NON-NLS-1$
		return menuConfiguracion;
	}
	/**
	 * 
	 */
	public void doShowAcercaDe() {
		Alert acercaDe = new Alert(AlertType.INFORMATION);
		acercaDe.titleProperty().set(Messages.getString("JFXMain.363")+JFXMain.TITLE_VERSION); //$NON-NLS-1$
		acercaDe.initOwner(JFXMain.stage);
		//acercaDe.setHeaderText(this.TITLE_VERSION+"\n"+this.BUILD_INFO+"\nVisitar www.ursulagis.com");
		//acercaDe.contentTextProperty().set();
		String content =   "<b>"+JFXMain.TITLE_VERSION+"</b><br>" //$NON-NLS-1$ //$NON-NLS-2$
		+ ShowConfigGUI.getBuildInfo()
		+ "<br><b>" +Messages.getString("JFXMain.visitarUrsulaGIS.com")+"</b>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		WebView webView = new WebView();
		webView.getEngine().loadContent("<html>"+content+"</html>"); //$NON-NLS-1$ //$NON-NLS-2$


		//   webView.setPrefSize(150, 60);
		//acercaDe.setHeaderText(""); //$NON-NLS-1$
		//acercaDe.setGraphic(null);

		acercaDe.getDialogPane().setContent(webView);;
		//  alert.showAndWait();
		acercaDe.setResizable(true);
		acercaDe.show();
	}

	public static void doConfigCultivo() {
		Platform.runLater(()->{
			final ObservableList<Cultivo> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllCultivos()
							);

		//	SmartTableView<Cultivo> table = new SmartTableView<Cultivo>(dataLotes);//,dataLotes);
			SmartTableView<Cultivo> table = new SmartTableView<Cultivo>(dataLotes,
					Arrays.asList("Id"),
					Arrays.asList("AbsN","AbsK","AbsP")
					);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Cultivo(Messages.getString("JFXMain.372"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.373")); //$NON-NLS-1$
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

			SmartTableView<Fertilizante> table = new SmartTableView<Fertilizante>(dataLotes);//,dataLotes);
			table.setEditable(true);

			table.setOnDoubleClick(()->new Fertilizante(Messages.getString("JFXMain.374"))); //$NON-NLS-1$


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.375")); //$NON-NLS-1$
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	public static void doConfigAgroquimicos() {
		Platform.runLater(()->{

			//			ArrayList<Fertilizante> ciLista = new ArrayList<Fertilizante>();
			//			System.out.println("Comenzando a cargar la los datos de la tabla");
			//
			//			ciLista.addAll(Fertilizante.fertilizantes.values());
			//			final ObservableList<Fertilizante> dataLotes =
			//					FXCollections.observableArrayList(
			//							ciLista
			//							);

			final ObservableList<Agroquimico> dataLotes =
					FXCollections.observableArrayList(
							DAH.getAllAgroquimicos()
							);

			SmartTableView<Agroquimico> table = new SmartTableView<Agroquimico>(dataLotes);//,dataLotes);
			table.setEditable(true);

			table.setOnDoubleClick(()->new Agroquimico(Messages.getString("JFXMain.376"))); //$NON-NLS-1$


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.377")); //$NON-NLS-1$
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});

	}

	public static void doConfigCampania() {
		Platform.runLater(()->{
			final ObservableList<Campania> data =
					FXCollections.observableArrayList(
							DAH.getAllCampanias()
							);
			if(data.size()==0){
				data.add(new Campania(Messages.getString("JFXMain.378")));//TODO obtener el anio actual y armar 16/17 //$NON-NLS-1$
			}
			SmartTableView<Campania> table = new SmartTableView<Campania>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Campania(Messages.getString("JFXMain.379"))); //$NON-NLS-1$
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.380")); //$NON-NLS-1$
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

			SmartTableView<Poligono> table = new SmartTableView<Poligono>(data);
			table.setEditable(true);
			table.getSelectionModel().setSelectionMode(
					SelectionMode.MULTIPLE
					);
			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((poli)->{
				poli.setActivo(true);
				main.showPoligonos(Collections.singletonList(poli));
				Position pos =poli.getPositions().get(0);
				main.viewGoTo(pos);
				Platform.runLater(()->{
					DAH.save(poli);
				});
			});


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.381")); //$NON-NLS-1$
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

	public void doShowNdviTable() {
		Platform.runLater(()->{
			final ObservableList<Ndvi> data =
					FXCollections.observableArrayList(
							DAH.getAllNdvi()
							);

			SmartTableView<Ndvi> table = new SmartTableView<Ndvi>(data);
			table.getSelectionModel().setSelectionMode(
					SelectionMode.MULTIPLE
					);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((ndvi)->{
				//poli.setActivo(true);
				main.doShowNDVI(ndvi);
			});


			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.382")); //$NON-NLS-1$
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
		dialog.setTitle(Messages.getString("JFXMain.383")); //$NON-NLS-1$
		dialog.setHeaderText(Messages.getString("JFXMain.384")); //$NON-NLS-1$
		dialog.setContentText(Messages.getString("JFXMain.385")); //$NON-NLS-1$
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

			SmartTableView<Establecimiento> table = new SmartTableView<Establecimiento>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Establecimiento(Messages.getString("JFXMain.386"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.387")); //$NON-NLS-1$
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
			SmartTableView<Lote> table = new SmartTableView<Lote>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Lote(Messages.getString("JFXMain.388"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.389")); //$NON-NLS-1$
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
				data.add(new Empresa(Messages.getString("JFXMain.390"))); //$NON-NLS-1$
			}
			SmartTableView<Empresa> table = new SmartTableView<Empresa>(data);//,data);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Empresa(Messages.getString("JFXMain.391"))); //$NON-NLS-1$

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.392")); //$NON-NLS-1$
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
			SmartTableView<Semilla> table = new SmartTableView<Semilla>(dataLotes);//,dataLotes);
			table.setEditable(true);
			table.setOnDoubleClick(()->new Semilla(Messages.getString("JFXMain.393"),DAH.getAllCultivos().get(0))); //$NON-NLS-1$
			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.394")); //$NON-NLS-1$
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
}
