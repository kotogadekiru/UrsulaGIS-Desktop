package gui.controller;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.data.FileDataStore;

import api.OrdenCosecha;
import dao.Labor;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.margen.Margen;
import dao.pulverizacion.PulverizacionLabor;
import dao.recorrida.Muestra;
import dao.recorrida.Recorrida;
import dao.suelo.Suelo;
import dao.utils.PropertyHelper;
import gov.nasa.worldwind.layers.RenderableLayer;
import gui.AmountVsElevacionChart;
import gui.FertilizacionConfigDialogController;
import gui.HarvestConfigDialogController;
import gui.HarvestSelectDialogController;
import gui.JFXMain;
import gui.MargenConfigDialogController;
import gui.Messages;
import gui.PulverizacionConfigDialogController;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import gui.utils.NumberInputDialog;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tasks.CompartirCosechaLaborTask;
import tasks.crear.ConvertirAFertilizacionTask;
import tasks.crear.ConvertirAPulverizacionTask;
import tasks.crear.ConvertirASueloTask;
import tasks.importar.OpenMargenMapTask;
import tasks.importar.ProcessHarvestMapTask;
import tasks.procesar.ExportarCosechaDePuntosTask;
import tasks.procesar.GenerarRecorridaDirigidaTask;
import tasks.procesar.GrillarCosechasMapTask;
import tasks.procesar.RecomendFertNFromHarvestMapTask;
import tasks.procesar.RecomendFertPAbsFromHarvestMapTask;
import tasks.procesar.RecomendFertPFromHarvestMapTask;
import tasks.procesar.SumarCosechasMapTask;
import tasks.procesar.UnirCosechasMapTask;
import tasks.procesar.UnirFertilizacionesMapTask;
import utils.DAH;
import utils.FileHelper;

public class MargenGUIController extends AbstractGUIController {


	public MargenGUIController(JFXMain _main) {
		super(_main);
	}

	public void addMargenRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();
//		rootNodeP.add(new LayerAction(
//				(layer)->{	this.doOpenCosecha(null);
//				return "opened";	
//				},Messages.getString("JFXMain.importar")));
//
//		rootNodeP.add(new LayerAction(
//				Messages.getString("JFXMain.unirCosechas"),
//				(layer)->{
//					this.doUnirCosechas(null);
//					return "joined";	
//				},
//				2));
//		rootNodeP.add(new LayerAction(
//				Messages.getString("JFXMain.sumarCosechas"),
//				(layer)->{
//					this.doSumarCosechas();
//					return "joined";	
//				},
//				2));

		getLayerPanel().addAccionesClase(rootNodeP,Margen.class);
	}

	public void addAccionesMargen(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> margenesP = new ArrayList<LayerAction>();
		predicates.put(Margen.class, margenesP);
		/**
		 *Accion que permite editar un mapa de rentabilidad
		 */
		margenesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editMargenAction"),(layer)->{	
			doEditMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "margen editado" + layer.getName(); 
		}));
		
		/**
		 *Accion que permite resumir por categoria un mapa de rentabilidad
		 */
		margenesP.add(LayerAction.constructPredicate(Messages.getString("ResumirMargenMapTask.resumirAction"),(layer)->{	
			main.configGUIController.doResumirMargin((Margen) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "margen resumido" + layer.getName(); 
		}));
	}
	
	private void doEditMargin(Margen margen) {		
		System.out.println(Messages.getString("JFXMain.324")); 
		Optional<Margen> margenConfigured= MargenConfigDialogController.config(margen);
		if(!margenConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.325")); 
			return;
		}							
		OpenMargenMapTask uMmTask = new OpenMargenMapTask(margen);
		uMmTask.installProgressBar(progressBox);
		uMmTask.setOnSucceeded(handler -> {
			this.getLayerPanel().update(this.getWwd());
			uMmTask.uninstallProgressBar();
			this.wwjPanel.repaint();
			System.out.println(Messages.getString("JFXMain.326")); 
			playSound();
		});
		executorPool.execute(uMmTask);
	}



	


}
