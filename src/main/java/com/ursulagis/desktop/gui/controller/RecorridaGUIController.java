package com.ursulagis.desktop.gui.controller;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.geotools.api.data.FileDataStore;

import com.google.gson.Gson;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.recorrida.Muestra;
import com.ursulagis.desktop.dao.recorrida.Recorrida;
import com.ursulagis.desktop.dao.suelo.Suelo;
import com.ursulagis.desktop.dao.suelo.SueloItem;
import com.ursulagis.desktop.dao.utils.PropertyHelper;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import com.ursulagis.desktop.gui.FertilizacionConfigDialogController;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.nww.LaborLayer;
import com.ursulagis.desktop.gui.nww.LayerAction;
import com.ursulagis.desktop.gui.utils.DoubleTableColumn;
import com.ursulagis.desktop.gui.utils.SmartTableView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.ursulagis.desktop.tasks.CompartirRecorridaTask;
import com.ursulagis.desktop.tasks.ShowRecorridaDirigidaTask;
import com.ursulagis.desktop.tasks.UpdateRecorridaTask;
import com.ursulagis.desktop.tasks.importar.ImportarRecorridaTask;
import com.ursulagis.desktop.tasks.importar.ProcessFertMapTask;
import com.ursulagis.desktop.tasks.procesar.ExportarRecorridaTask;
import com.ursulagis.desktop.tasks.procesar.GenerarRecorridaDirigidaTask;
import com.ursulagis.desktop.tasks.procesar.InterpolarRecorridaMapTask;
import com.ursulagis.desktop.utils.DAH;
import com.ursulagis.desktop.utils.FileHelper;

public class RecorridaGUIController extends AbstractGUIController {
	public RecorridaGUIController(JFXMain _main) {
		super(_main);
	}

	public void addAccionesRecorridas(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> recorridasP = new ArrayList<LayerAction>();
		predicates.put(Recorrida.class, recorridasP);

		//editar Recorrida
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editarLayer"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;				
				doShowRecorridaTable(Collections.singletonList(recorrida));
				layer.setName(recorrida.getNombre());		
				GenerarRecorridaDirigidaTask.renderRecorrida((RenderableLayer)layer,recorrida);		
				this.getLayerPanel().update(this.getWwd());
			}
			return "edite recorrida"; 
		}));

		//Guardar Recorrida
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.guardarNDVIAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				DAH.save(recorrida);
			}
			return "guarde recorrida"; 
		}));

		//Tomar una recorrida y un poligono y convertir a mapa de suelo interpolando los datos con krigging
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("RecorridaGUIController.interpolarASuelo"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				doInterpolarRecorrida(recorrida,layer);
			}
			return "interpole recorrida"; 
		}));	

		//Compartir Recorrida
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.compartir"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				//updload to server and show url to access
				doCompartirRecorrida(recorrida);
			}
			return "comparti una recorrida"; 
		}));

		/**
		 * metodo que permite asignar los resultados de los analisis a los muestreos con mismo nombre
		 */
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("Recorrida.asignarValores"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Recorrida.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Recorrida recorrida =(Recorrida)layerObject;
				doAsignarValoresRecorrida(recorrida);
			}
			return "asigne los valores de una recorrida"; 
		}));

		/**
		 * Accion permite exportar la recorrida como shp
		 */
		recorridasP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportLaborAction"),(layer)->{
			doExportRecorrida((Recorrida) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "recorrida Exportada" + layer.getName(); 
		}));
	}


	/**
	 * metodo que toma las muestras de una recorrida y interpola los puntos en 
	 * una grilla contenida dentro de un poligono seleccionado
	 * @param recorrida
	 * @param layer 
	 */
	private void doInterpolarRecorrida(Recorrida recorrida, Layer layer) {		
		List<Poligono> contornos = main.poligonoGUIController.getEnabledPoligonos();
		if(contornos==null || contornos.size()<1) {
			Alert enDesarrollo = new Alert(AlertType.INFORMATION,"Debe seleccionar un poligono "); 
			enDesarrollo.showAndWait();
			return;
		}
		//TODO crear un task que tome un poligono y una recorrida 
		//y devuelva un suelo con los valores de la recorrida interpolados por krigging
		InterpolarRecorridaMapTask imTask = new InterpolarRecorridaMapTask(recorrida,contornos);
		imTask.installProgressBar(main.progressBox);
		imTask.setOnSucceeded(handler -> {
			layer.setEnabled(false);
			contornos.forEach(c->c.getLayer().setEnabled(false));
			Suelo ret = (Suelo)handler.getSource().getValue();
			imTask.uninstallProgressBar();			
			
			JFXMain.insertBeforeCompass(main.getWwd(), ret.getLayer());
			main.getLayerPanel().update(main.getWwd());

			main.playSound();
			main.viewGoTo(ret);
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(imTask);
	}

	public void doShowRecorrida(Recorrida recorrida) {
		ShowRecorridaDirigidaTask umTask = new ShowRecorridaDirigidaTask(recorrida);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			RenderableLayer ret = (RenderableLayer)handler.getSource().getValue();
			JFXMain.insertBeforeCompass(getWwd(), ret);
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			main.viewGoTo(ret);
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);	
	}

	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	public void doOpenRecorridaMap(List<File> files) {
		List<FileDataStore> stores =FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				Recorrida labor = new Recorrida(store);

				//				Optional<Recorrida> configured= RecorridaConfigDialogController.config(labor);
				//				if(!configured.isPresent()){//
				//					System.out.println(Messages.getString("JFXMain.308")); 
				//					continue;
				//				}							

				ImportarRecorridaTask umTask = new ImportarRecorridaTask(labor,store);
				umTask.installProgressBar(progressBox);

				umTask.setOnSucceeded(handler -> {
					Recorrida ret = (Recorrida)handler.getSource().getValue();
					doShowRecorrida(ret);
					umTask.uninstallProgressBar();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}



	/**
	 *  updload recorrida to server and show url to access
	 * @param recorrida
	 */
	public void doCompartirRecorrida(Recorrida recorrida) {		
		if(recorrida.getUrl()!=null && recorrida.getUrl().length()>0) {			
			main.configGUIController.showQR(recorrida.getUrl());
			//XXX editar la recorrida remota con la informacion actualizada de la local?
			//XXX recupero la recorrida remota?
			return;
		}
		Configuracion config = Configuracion.getInstance();
		NumberFormat nf = Messages.getNumberFormat();
		nf.setMaximumFractionDigits(0);
		try {
			Number nroRec = nf.parse(config.getPropertyOrDefault("Recorrida.NumeroKey", "0"));
			recorrida.setNumero(nroRec.longValue()+1);
		} catch (ParseException e) {			
			e.printStackTrace();
		}
		CompartirRecorridaTask task = new CompartirRecorridaTask(recorrida);
		//System.out.println("procesando los datos entre "+ndviDpDLG.initialDate+" y "+ ndviDpDLG.finalDate);//hasta aca ok!
		task.installProgressBar(progressBox);
		task.setOnSucceeded(handler -> {
			String ret = (String)handler.getSource().getValue();
			recorrida.setUrl(ret);
			DAH.save(recorrida);
			if(ret!=null) {
				main.configGUIController.showQR(ret);
			}
			//XXX agregar boton de actualizar desde la nube?
			task.uninstallProgressBar();			
		});
		System.out.println("ejecutando Compartir Recorrida"); 
		executorPool.execute(task);
	}
	
	/**
	 *  download recorrida from server and update local copy
	 * @param recorrida
	 */
	public void doUpdateRecorrida(Recorrida recorrida) {		
//		if(recorrida.getUrl()!=null && recorrida.getUrl().length()>0) {			
//			main.configGUIController.showQR(recorrida.getUrl());
//			//XXX editar la recorrida remota con la informacion actualizada de la local?
//			//XXX recupero la recorrida remota?
//			return;
//		}
		UpdateRecorridaTask task = new UpdateRecorridaTask(recorrida);
		//System.out.println("procesando los datos entre "+ndviDpDLG.initialDate+" y "+ ndviDpDLG.finalDate);//hasta aca ok!
		task.installProgressBar(progressBox);
		task.setOnSucceeded(handler -> {
			String ret = (String)handler.getSource().getValue();
			recorrida.setUrl(ret);
			DAH.save(recorrida);
//			if(ret!=null) {
//				main.configGUIController.showQR(ret);
//			}
			//XXX agregar boton de actualizar desde la nube?
			task.uninstallProgressBar();			
		});
		System.out.println("ejecutando Compartir Recorrida"); 
		executorPool.execute(task);
	}

//	// junta las muestras con mismo nombre y permite completar los datos de las objervaciones
//	public void doAsignarValoresRecorrida(Recorrida recorrida) {
//		doAsignarValoresRecorrida(recorrida);
//	}	
	
	public void doShowRecorridaTable(List<Recorrida> recorridas) {
	//	Platform.runLater(()->{
			final ObservableList<Recorrida> data = FXCollections.observableArrayList(recorridas);

			
			SmartTableView<Recorrida> table = new SmartTableView<Recorrida>(data,
					Arrays.asList("Id","Posicion"),
					Arrays.asList("Nombre","Observacion","Latitude","Longitude","JsonAmb"),//orden
					Arrays.asList("Nombre","Observacion","Latitud","Longitud","geoms")//nombres					
					//,Arrays.asList(Messages.getString("Recorrida.Nombre",,,)
					);
			table.getSelectionModel().setSelectionMode(	SelectionMode.MULTIPLE	);
			table.setEditable(true);
			//			table.setOnDoubleClick(()->new Poligono());
			table.setOnShowClick((recorrida)->{
				//poli.setActivo(true);
				main.recorridaGUIController.doShowRecorrida(recorrida);
			});
			
			table.addSecondaryClickConsumer(Messages.getString("JFXMain.editarLayer"),(r)-> {
				doShowMuestrasTable(r.getMuestras());
			});
			//TODO add to messages.properties Update
			table.addSecondaryClickConsumer("Update",(r)-> {
				doUpdateRecorrida(r);
			});

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.configRecorridaMI")); //
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
				//getWwd().redraw();
			});

			tablaStage.showAndWait();
	//	});	

	}

	public void doShowMuestrasTable(List<Muestra> muestras) {
		Platform.runLater(()->{
			final ObservableList<Muestra> data = FXCollections.observableArrayList(muestras);

			SmartTableView<Muestra> table = new SmartTableView<Muestra>(data,
					Arrays.asList("Id","Posicion"),
					Arrays.asList("Nombre","SubNombre","Latitude","Longitude","Observacion"),
					Arrays.asList("Nombre","SubNombre","Latitud","Longitud","Observacion")
					//TODO agregar la lista de nombres traducidos para mostrar
					//,Arrays.asList(Messages.getString("Recorrida.Nombre",,,)
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
							List<Object> objs = new ArrayList<Object>(list);
							DAH.removeAll(objs);
							DAH.commitTransaction();
						}catch(Exception e) {
							DAH.rollbackTransaction();
						}
					}
					);
			table.setEditable(true);

			Scene scene = new Scene(table, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
			tablaStage.setTitle(Messages.getString("JFXMain.configRecorridaMI")); //
			tablaStage.setScene(scene);

			tablaStage.onHiddenProperty().addListener((o,old,n)->{
				main.getLayerPanel().update(main.getWwd());
			});
			tablaStage.show();	 
		});
	}
	
	public void doAsignarValoresRecorrida(Recorrida recorrida) {
		List<Muestra> muestras = recorrida.getMuestras();
		Map<String, List<Muestra>> nombresMuestraMap = muestras.stream().collect(Collectors.groupingBy(Muestra::getNombre));

		List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();

		Map<String,Number> props =null;
		for(String nombre : nombresMuestraMap.keySet()) {//obtengo los nombres de las columnas de cada categoria
			Muestra m0 = nombresMuestraMap.get(nombre).get(0);
			//"{\"PPM P\":\"\",\"PPM K\":\"\",\"Agua Perf\":\"\",\"PPM N\":\"\",\"Prof Napa\":\"\",\"PPM MO\":\"\",\"PPM S\":\"\"}"
			String obs = m0.getObservacion();

			@SuppressWarnings("unchecked")
			Map<String,String> map = new Gson().fromJson(obs, Map.class);	 
			if(map == null) {
				System.out.println("salteando "+m0.nombre+" porque obs es null");
				continue;
			}
			props = new LinkedHashMap<String,Number>();//convierto el valor de obs a number
			for(String k : map.keySet()) {
				Object value = map.get(k);
				if(String.class.isAssignableFrom(value.getClass())) {				
					Double dValue = 0.0;
					try { 
						dValue = PropertyHelper.parseDouble((String)value).doubleValue();
						//dValue=Messages.getNumberFormat().parse((String)value).doubleValue();
						//dValue=Double.parseDouble((String)value);
					}catch(Exception e) {
						System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");
					}
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

		//XXX porque no uso smartTable?
		TableView<Map<String,Object>> tabla = new TableView<Map<String,Object>>( 
				FXCollections.observableArrayList(data)//,
				//Arrays.asList("Id","PoligonoToString"),
				//Arrays.asList("Activo","Nombre","Lote","Area","PositionsString")
				);
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
				cellEditingEvent.getTableView().refresh();
			} catch (Exception e) {	e.printStackTrace();}
		});		

		tabla.getColumns().add(columnNombre);
		List<String> orderColumns = Arrays.asList("Nombre","SubNombre",SueloItem.PPM_N,SueloItem.PPM_FOSFORO);
		List<String> nombresColumns = props.keySet().stream().sorted(
				(a,b)->{

					String nameA =a;//.getName();
					String nameB =  b;//b.getName();
					if(orderColumns.contains(nameA)&&orderColumns.contains(nameB)) {
						return Integer.compare(orderColumns.indexOf(nameA),orderColumns.indexOf(nameB));
					} else 	if(orderColumns.contains(nameA)&&!orderColumns.contains(nameB)) {
						return -1;
					}  else if(!orderColumns.contains(nameA)&&orderColumns.contains(nameB)) {
						return 1;
					} 
					return nameA.compareToIgnoreCase(nameB);
				}
				).collect(Collectors.toList());
		
		for(String k : nombresColumns) {

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
						//tabla.refresh();//se hace en el edit de DoubleTableColumn
					} catch (Exception e) {	e.printStackTrace();}
					});
			dColumn.setEditable(true);
			tabla.getColumns().add(dColumn);			
		}

		Scene scene = new Scene(tabla, 800, 600);
		Stage tablaStage = new Stage();
		tablaStage.getIcons().addAll(JFXMain.stage.getIcons());
		tablaStage.setTitle(Messages.getString("Recorrida.asignarValores")); //
		tablaStage.setScene(scene);

		tablaStage.showAndWait();	 

		//Al terminar de editar recoger la informacion y guardar los cambios
		for(Map<String,Object> ma : data) {
			String k = (String) ma.get("Nombre");
			//System.out.println("persisting changes for "+k);
			List<Muestra> muestrasNombre = nombresMuestraMap.get(k);
			for(Muestra m : muestrasNombre) {
				ma.remove("Nombre");
				m.setObservacion(new Gson().toJson(ma));
				//System.out.println("setting observaciones "+m.getObservacion());
				try {
					if(m.getId()!=null) {
						DAH.save(m);
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void doExportRecorrida(Recorrida recorrida) {
		String nombre = recorrida.getNombre();
		File shapeFile = FileHelper.getNewShapeFile(nombre);
		ExportarRecorridaTask task = new ExportarRecorridaTask(recorrida,shapeFile);
		task.installProgressBar(progressBox);
		task.setOnSucceeded(handler -> {
			playSound();
			task.uninstallProgressBar();
		});
		executorPool.execute(task);
	}


}
