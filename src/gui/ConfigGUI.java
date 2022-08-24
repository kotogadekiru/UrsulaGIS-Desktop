package gui;

import java.awt.image.BufferedImage;
import java.text.DateFormat;
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
import java.util.stream.Collectors;

import javax.persistence.EntityTransaction;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import dao.OrdenDeCompra.OrdenCompra;
import dao.OrdenDeCompra.OrdenCompraItem;
import dao.config.Agroquimico;
import dao.config.Campania;
import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Fertilizante;
import dao.config.Lote;
import dao.config.Semilla;
import dao.cosecha.CosechaLabor;
import dao.recorrida.Muestra;
import dao.recorrida.Recorrida;
import gov.nasa.worldwind.geom.Position;
import gui.utils.DateConverter;
import gui.utils.DoubleTableColumn;
import gui.utils.SmartTableView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tasks.CotizarOdenDeCompraOnlineTask;
import tasks.procesar.ExportarPrescripcionSiembraTask;
import utils.DAH;
import utils.ExcelHelper;

public class ConfigGUI {
	private static final String DD_MM_YYYY = "dd/MM/yyyy";
	JFXMain main=null;

	public ConfigGUI(JFXMain _main) {
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
		addMenuItem(Messages.getString("JFXMain.configRecorridaMI"),(a)->doShowRecorridaTable(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.OrdenCompra"),(a)->doShowOrdenesCompra(),menuConfiguracion); //$NON-NLS-1$
		addMenuItem(Messages.getString("JFXMain.362"),(a)->doShowLaboresTable(),menuConfiguracion); //$NON-NLS-1$

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
				+ ConfigGUI.getBuildInfo()
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
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setOnDoubleClick(()->new Fertilizante(Messages.getString("JFXMain.374"))); //$NON-NLS-1$
			table.setEliminarAction(
					list->{
						Platform.runLater(()->{		
							try {
								System.out.println("removing fertilizantes "+list.size());
								List<Object> objs = new ArrayList(list);
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

			SmartTableView<Poligono> table = new SmartTableView<Poligono>(data,
					Arrays.asList("Id","PoligonoToString"),
					Arrays.asList("Activo","Nombre","Area","PositionsString")
					);
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

	public void doShowRecorridaTable() {
		doShowRecorridaTable(DAH.getAllRecorridas());

	}

	/**
	 * metodo que toma una cosecha y permite asignar valores para cada clase de la cosecha
	 * @param cosecha
	 * @return devuelve el map con los valores asignados
	 */
	public Map<String,Double> doAsignarValoresCosecha(CosechaLabor cosecha,String column_Valor) {
		//List<Muestra> muestras = cosecha.getMuestras();
		//Map<String, List<Muestra>> nombresMuestraMap = muestras.stream().collect(Collectors.groupingBy(Muestra::getNombre));

		List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();


		//for(String nombre : nombresMuestraMap.keySet()) {
		// Map<String,Number> props =null;
		String column_Nombre = 	Messages.getString("JFXMain.clase");//"Clase";
		//String column_Valor = "Densidad pl/m2";
		int clases = cosecha.getClasificador().getNumClasses();
		///System.out.println("clases "+clases);
		for(int i = clases-1;  i>-1; i--) {	 
			//System.out.print("categoria "+i);
			String nombre = cosecha.getClasificador().getLetraCat(i);		 
			//System.out.println(" letra "+nombre);
			Map<String,Object> initialD = new LinkedHashMap<String,Object>();
			initialD.put(column_Nombre, nombre);
			initialD.put(column_Valor, 0.0);
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


		Scene scene = new Scene(tabla, 800, 600);
		Stage tablaStage = new Stage();
		tablaStage.getIcons().add(new Image(JFXMain.ICON));
		tablaStage.setTitle(Messages.getString("Recorrida.asignarValores")); //$NON-NLS-1$
		tablaStage.setScene(scene);



		tablaStage.showAndWait();	 
		Map<String,Double> ret = new HashMap<String,Double>();
		for(Map<String,Object> ma : data) {
			String k = (String) ma.get(column_Nombre);
			Double valor = (Double) ma.get(column_Valor);
			ret.put(k, valor);

		}
		return ret;
	}


	public void doAsignarValoresRecorrida(Recorrida recorrida) {
		List<Muestra> muestras = recorrida.getMuestras();
		Map<String, List<Muestra>> nombresMuestraMap = muestras.stream().collect(Collectors.groupingBy(Muestra::getNombre));

		List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();

		Map<String,Number> props =null;
		for(String nombre : nombresMuestraMap.keySet()) {
			Muestra m0 = nombresMuestraMap.get(nombre).get(0);
			//"{\"PPM P\":\"\",\"PPM K\":\"\",\"Agua Perf\":\"\",\"PPM N\":\"\",\"Prof Napa\":\"\",\"PPM MO\":\"\",\"PPM S\":\"\"}"
			String obs = m0.getObservacion();

			@SuppressWarnings("unchecked")
			Map<String,String> map = new Gson().fromJson(obs, Map.class);	 

			props = new LinkedHashMap<String,Number>();
			for(String k : map.keySet()) {
				Object value = map.get(k);
				if(String.class.isAssignableFrom(value.getClass())) {				
					Double dValue = new Double(0);
					try { dValue=new Double((String)value);
					}catch(Exception e) {System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");}
					props.put(k, dValue);//ojo number format exception
				} else if(Number.class.isAssignableFrom(value.getClass())) {
					props.put(k, (Number)value);
				}			
			}

			Map<String,Object> initialD = new LinkedHashMap<String,Object>();
			//System.out.println("agregando la observacion con nombre "+nombre);
			initialD.put("Nombre", nombre);

			initialD.putAll(props);

			data.add(initialD);
		}

		TableView<Map<String,Object>> tabla = new TableView<Map<String,Object>>( FXCollections.observableArrayList(data));
		tabla.setEditable(true);
		TableColumn<Map<String,Object>,String> columnNombre = new TableColumn<Map<String,Object>,String>("Nombre");
		columnNombre.setEditable(false);
		columnNombre.setCellFactory(TextFieldTableCell.forTableColumn());
		columnNombre.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
						stringValue =(String)  cellData.getValue().get("Nombre");						
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
				p.put("Nombre", cellEditingEvent.getNewValue());
				tabla.refresh();
			} catch (Exception e) {	e.printStackTrace();}
		});		

		tabla.getColumns().add(columnNombre);
		for(String k : props.keySet()) {

			DoubleTableColumn<Map<String,Object>> dColumn = new DoubleTableColumn<Map<String,Object>>(k,
					(p)->{	try {
						Number n =(Number) p.get(k);
						if(n!=null) {
							return n.doubleValue();
						} else {
							return 0.0;
						}
					} catch (Exception e) {	e.printStackTrace();}
					return null;
					},(p,d)->{ try {

						p.put(k,d);
						tabla.refresh();
					} catch (Exception e) {	e.printStackTrace();}
					});
			dColumn.setEditable(true);
			tabla.getColumns().add(dColumn);			
		}

		Scene scene = new Scene(tabla, 800, 600);
		Stage tablaStage = new Stage();
		tablaStage.getIcons().add(new Image(JFXMain.ICON));
		tablaStage.setTitle(Messages.getString("Recorrida.asignarValores")); //$NON-NLS-1$
		tablaStage.setScene(scene);

		//			tablaStage.onHiddenProperty().addListener((o,old,n)->{
		//				this.getLayerPanel().update(this.getWwd());
		//				
		//				//getWwd().redraw();
		//			});

		tablaStage.showAndWait();	 
		//System.out.println("hidding tablastage");
		//Al terminar de editar recoger la informacion y guardar los cambios
		for(Map<String,Object> ma : data) {
			String k = (String) ma.get("Nombre");
			//System.out.println("persisting changes for "+k);
			List<Muestra> muestrasNombre = nombresMuestraMap.get(k);
			for(Muestra m : muestrasNombre) {
				ma.remove("Nombre");
				m.setObservacion(new Gson().toJson(ma));
				//System.out.println("setting observaciones "+m.getObservacion());
				DAH.save(m);
			}
		}
	}

	public void doShowOrdenCompraItems(OrdenCompra ret) {
		Platform.runLater(()->{
			final ObservableList<OrdenCompraItem> data =
					FXCollections.observableArrayList(
							ret.getItems()
							);

			SmartTableView<OrdenCompraItem> table = new SmartTableView<OrdenCompraItem>(data,
					Arrays.asList("Id"),//rejected
					Arrays.asList("Producto","Cantidad")//order
					);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			//			table.setOnShowClick((ndvi)->{
			//				//poli.setActivo(true);
			//				doShowNDVI(ndvi);
			//
			//			});

			VBox v=new VBox();
			v.getChildren().add(table);
			HBox h = new HBox();

			Button guardarB = new Button(Messages.getString("JFXMain.saveAction"));//TODO traducir
			guardarB.setOnAction(actionEvent->{
				//System.out.println("implementar GuardarOrden de compra action");
				DAH.save(ret);
			});

			Button exportarB = new Button(Messages.getString("JFXMain.exportar"));//TODO traducir
			exportarB.setOnAction(actionEvent->{
				ExcelHelper helper = new ExcelHelper();
				helper.exportOrdenCompra(ret);
			});

			Button cotizarOblineB = new Button(Messages.getString("ShowConfigGUI.cotizarOnline"));//"Cotizar OnLine");//TODO traducir
			cotizarOblineB.setOnAction(actionEvent->{
				//TODO preguntar mail para enviar presupuestos
				TextInputDialog tDialog = new TextInputDialog("tomas@ursulagis.com");
				tDialog.initOwner(JFXMain.stage);
				tDialog.setTitle(Messages.getString("ShowConfigGUI.cotizarOnlineMailTitle"));
				tDialog.showAndWait();

				String mail = tDialog.getResult();

				ret.setMail(mail);
				CotizarOdenDeCompraOnlineTask cotTask= new CotizarOdenDeCompraOnlineTask(ret); 

				JFXMain.executorPool.execute(cotTask);
				//TODO enviar orden de compra a la nube. preguntar mail de contacto y subir la orden de compra a la nube


			});
			h.getChildren().addAll(guardarB,exportarB,cotizarOblineB);

			v.getChildren().add(h);
			Scene scene = new Scene(v, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.OrdenCompra")); //$NON-NLS-1$
			tablaStage.setScene(scene);

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
					Arrays.asList("Id","Url","Uuid","ImporteTotal2"),
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
								List<Object> objs = new ArrayList(list);
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
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.OrdenCompra")); //$NON-NLS-1$
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
		//			tablaStage.getIcons().add(new Image(JFXMain.ICON));
		//			tablaStage.setTitle(Messages.getString("JFXMain.configRecorridaMI")); //$NON-NLS-1$
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

	public void doShowRecorridaTable(List<Recorrida> recorridas) {
		Platform.runLater(()->{
			final ObservableList<Recorrida> data = FXCollections.observableArrayList(recorridas);

			SmartTableView<Recorrida> table = new SmartTableView<Recorrida>(data,
					Arrays.asList("Id","Posicion"),
					Arrays.asList("Nombre","Observacion","Latitude","Longitude")
					);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((recorrida)->{
				//poli.setActivo(true);
				main.doShowRecorrida(recorrida);
			});

			table.addSecondaryClickConsumer("Editar",(r)-> {
				doShowMuestrasTable(r.getMuestras());
			});

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.configRecorridaMI")); //$NON-NLS-1$
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});

			tablaStage.show();	 
		});	

	}

	public void doShowMuestrasTable(List<Muestra> muestras) {
		Platform.runLater(()->{
			final ObservableList<Muestra> data = FXCollections.observableArrayList(muestras);

			SmartTableView<Muestra> table = new SmartTableView<Muestra>(data,
					Arrays.asList("Id","Posicion"),
					Arrays.asList("Nombre","Latitude","Longitude")
					);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setEliminarAction(
					list->{

						try {
							DAH.beginTransaction();						
							list.stream().forEach(m->{
								m.getRecorrida().getMuestras().remove(m);	
							});
							DAH.save(list.get(0).getRecorrida());
							List<Object> objs = new ArrayList(list);
							DAH.removeAll(objs);
							DAH.commitTransaction();
						}catch(Exception e) {
							DAH.rollbackTransaction();
						}

					}
					);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			//			table.setOnShowClick((recorrida)->{
			//				//poli.setActivo(true);
			//				main.doShowRecorrida(recorrida);
			//			});
			//			
			//			table.addSecondaryClickConsumer("editarRecorrida",(r)->
			//			{
			//				
			//				
			//			});

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(Messages.getString("JFXMain.configRecorridaMI")); //$NON-NLS-1$
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

	public  void showQR(String ret) {
		BufferedImage qr = ConfigGUI.generateQR(ret);
		Image image = SwingFXUtils.toFXImage(qr, null);

		Alert a = new Alert(AlertType.INFORMATION);
		ImageView view = new ImageView();
		a.initOwner(this.main.stage);
		a.setTitle("Qr Code");
		view.setImage(image);
		VBox v = new VBox();
		v.getChildren().add(view);
		TextField link = new TextField(ret);
		link.setEditable(false);
		v.getChildren().add(link);

		a.setResizable(true);
		a.setGraphic(v);
		a.setHeaderText("");
		//a.setWidth(400);
		a.getDialogPane().setPrefWidth(image.getWidth());
		//a.setContentText(ret);
		a.show();
	}



	public static BufferedImage generateQR(String code) {
		QRCodeWriter barcodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix;
		try {
			bitMatrix = barcodeWriter.encode(code, BarcodeFormat.QR_CODE, 200, 200);
			return MatrixToImageWriter.toBufferedImage(bitMatrix);
		} catch (WriterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}


	}
}
