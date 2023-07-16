package gui.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import dao.config.Configuracion;
import dao.cosecha.CosechaLabor;
import dao.cosecha.CosechaLabor.CosechaLaborConstants;
import dao.fertilizacion.FertilizacionLabor;
import dao.pulverizacion.PulverizacionLabor;
import dao.recorrida.Camino;
import dao.siembra.SiembraLabor;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import dao.utils.PropertyHelper;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gui.FertilizacionConfigDialogController;
import gui.HarvestConfigDialogController;
import gui.JFXMain;
import gui.Messages;
import gui.NDVIDatePickerDialog;
import gui.PathLayerFactory;
import gui.PoligonLayerFactory;
import gui.PoligonoDialog;
import gui.PulverizacionConfigDialogController;
import gui.SiembraConfigDialogController;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import gui.nww.LayerPanel;
import gui.utils.DateConverter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import tasks.GetNdviForLaborTask4;
import tasks.crear.CrearCosechaMapTask;
import tasks.crear.CrearFertilizacionMapTask;
import tasks.crear.CrearPulverizacionMapTask;
import tasks.crear.CrearSiembraMapTask;
import tasks.crear.CrearSueloMapTask;
import tasks.procesar.ExtraerPoligonosDeLaborTask;
import tasks.procesar.SimplificarCaminoTask;
import tasks.procesar.UnirCosechasMapTask;
import utils.DAH;
import utils.FileHelper;
import utils.GeometryHelper;
import utils.ProyectionConstants;

public class PoligonoGUIController {
	JFXMain main=null;
	Pane progressBox=null;
	private ExecutorService executorPool=null;

	public PoligonoGUIController(JFXMain _main) {
		this.main=_main;	
		this.progressBox=main.getProgressBox();
		this.executorPool = JFXMain.executorPool;
	}
	private LayerPanel getLayerPanel() {
		return main.getLayerPanel();
	}

	private WorldWindow getWwd() {
		return main.getWwd();
	}
	
	private void viewGoTo(Labor<?> labor) {
		main.viewGoTo(labor);
		
	}
	private void viewGoTo(Position pos) {
		main.viewGoTo(pos);		
	}
//	private void insertBeforeCompass(WorldWindow wwd, LaborLayer layer) {
//		JFXMain.insertBeforeCompass(wwd, layer);		
//	}
	private void insertBeforeCompass(WorldWindow wwd, RenderableLayer applicationLayer) {
		JFXMain.insertBeforeCompass(wwd, applicationLayer);		
	}
	private void playSound() {
		main.playSound();		
	}
	
//	public List<Poligono> getPoligonosActivos() {
//		List<Poligono> geometriasActivas = new ArrayList<Poligono>();
//		
//		//1 obtener los poligonos activos
//		LayerList layers = this.getWwd().getModel().getLayers();
//		for (Layer l : layers) {
//			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
//			if (l.isEnabled() && o instanceof Poligono){
//				Poligono p = (Poligono)o;
//				geometriasActivas.add(p);
//			}
//		}
//		return geometriasActivas;
//	}
	
	@SuppressWarnings("unchecked")
	public List<Poligono> getEnabledPoligonos() {
		return ((List<Poligono>)main.getObjectFromLayersOfClass(Poligono.class)).stream()
				.filter((p)-> p.getLayer().isEnabled())
				.collect(Collectors.toList());
	}
	
	public void addPoligonosRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();

		
		rootNodeP.add(new LayerAction((layer)->{
			doMedirDistancia();
			return "distancia";	
		},Messages.getString("JFXMain.distancia")));

		//crear poligono
		rootNodeP.add(new LayerAction((layer)->{
			doCrearPoligono();
			return "superficie";	
		},Messages.getString("JFXMain.superficie")));

		//unir
		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.unirPoligonos"),(layer)->{
			doUnirPoligonos();
			return "unidos";	
		},2));

		//intersectar
		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.intersectarPoligonos"),(layer)->{
			doIntersectarPoligonos();
			return "intersectados";	
		},2));
		//convertir a siembra
		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.poligonToSiembraAction"),(layer)->{
			doConvertirPoligonosASiembra();

			return "converti a Siembra";	
		},1));
		//convertir a fertilizacion
		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.poligonToFertAction"),(layer)->{
			doConvertirPoligonosAFertilizacion();

			return "converti a fertilizacion";	
		},1));
		//convertir a cosecha
		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.poligonToHarvestAction"),(layer)->{
			doConvertirPoligonosACosecha();

			return "converti a Cosecha";	
		},1));

		//		rootNodeP.add(constructPredicate(Messages.getString("JFXMain.poligonToFertAction"),(layer)->{
		//			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
		//			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){	
		//				doCrearFertilizacion((Poligono) layerObject);
		//			}
		//			return "converti a Fertilizacion"; //$NON-NLS-1$
		//		}));

		//addMenuItem(Messages.getString("JFXMain.poligonos"),(a)->doImportarPoligonos(null),menuImportar); //$NON-NLS-1$
		//importar poligonos
		rootNodeP.add(new LayerAction((layer)->{
			doImportarPoligonos(null);
			return "importados";	
		},Messages.getString("JFXMain.importar")));

		//guardar poligono
		rootNodeP.add(new LayerAction((layer)->{
			executorPool.submit(()->{
				try {
					LayerList layers = this.getWwd().getModel().getLayers();
					for (Layer l : layers) {
						Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
						if (l.isEnabled() && o instanceof Poligono){
							Poligono p = (Poligono)o;
							doGuardarPoligono(p);
						}
					}

				}catch(Exception e) {
					System.err.println("Error al guardar los poligonos"); //$NON-NLS-1$
					e.printStackTrace();
				}
			});

			return "Guarde los poligonos"; //$NON-NLS-1$
		},Messages.getString("JFXMain.saveAction")));


		//obtener ndvi
		rootNodeP.add(new LayerAction((layer)->{
			//executorPool.submit(()->{
			try {
				List<Poligono> poligonos= new ArrayList<Poligono>();
				LayerList layers = this.getWwd().getModel().getLayers();
				for (Layer l : layers) {
					Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					if (l.isEnabled() && o instanceof Poligono){
						Poligono p = (Poligono)o;
						//doGuardarPoligono(p);
						poligonos.add(p);

					}
				}
				doGetNdviTiffFiles(poligonos);
			}catch(Exception e) {
				System.err.println("Error al guardar los poligonos"); //$NON-NLS-1$
				e.printStackTrace();
			}
			//});

			return "ndvi obtenidos"; //$NON-NLS-1$
		},Messages.getString("JFXMain.downloadNDVIAction")));

		//		rootNodeP.add(constructPredicate(Messages.getString("JFXMain.downloadNDVIAction"),(layer)->{
		//			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
		//			if(o instanceof Poligono){
		//				doGetNdviTiffFile(o);
		//			}
		//			
		//			return "ndvi obtenido" + layer.getName();	 //$NON-NLS-1$
		//		}));

		getLayerPanel().addAccionesClase(rootNodeP,Poligono.class);
	}
	
	public void addAccionesPoligonos(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> poligonosP = new ArrayList<LayerAction>();
		predicates.put(Poligono.class, poligonosP);

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editarLayer"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				doEditarPoligono(layerObject);
			}
			return "edite poligono"; //$NON-NLS-1$
		}));
		
		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.clonar"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				doClonarPoligono((Poligono)layerObject);
			}
			return "clone poligono"; //$NON-NLS-1$
		}));
		
		//XXX simplificar poligono action. sirve para probar alinear los puntos que estan en un margen 
		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.simplificar"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				doSimplificarPoligono((Poligono)layerObject);
			}
			return "simplifique poligono"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.poligonToSiembraAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				//doConvertirASiembra((Polygon) layerObject);

				doCrearSiembra(Collections.singletonList((Poligono) layerObject));
			}
			return "converti a Siembra"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.poligonToFertAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){	
				doCrearFertilizacion(Collections.singletonList((Poligono) layerObject));
			}
			return "converti a Fertilizacion"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.poligonToPulvAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				doCrearPulverizacion((Poligono) layerObject);
			}
			return "converti a Pulverizacion"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.poligonToHarvestAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				doCrearCosecha(Collections.singletonList((Poligono) layerObject));
			}
			return "converti a Cosecha"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.poligonToSoilAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				doCrearSuelo((Poligono) layerObject);
				layer.setEnabled(false);
			}
			return "converti a Suelo"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.saveAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
				doGuardarPoligono((Poligono) layerObject);
			}
			return "Guarde Guarde"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.goToPoligonoAction"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (layerObject==null){
			}else if(Poligono.class.isAssignableFrom(layerObject.getClass())){
				Poligono poli = (Poligono)layerObject;
				Point c = poli.toGeometry().getCentroid();
				Position pos =Position.fromDegrees(c.getY(), c.getX());
				viewGoTo(pos);
			}
			return "went to " + layer.getName(); //$NON-NLS-1$
		}));
		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.downloadNDVIAction"),(layer)->{
			Object o =  layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);			
			if(o instanceof Poligono){
				doGetNdviTiffFile(o);
			}
			return "ndvi obtenido" + layer.getName();	 //$NON-NLS-1$
		}));

		Collections.sort(poligonosP);
	}
	
	public void addAccionesCaminos(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> poligonosP = new ArrayList<LayerAction>();
		predicates.put(Camino.class, poligonosP);

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editarLayer"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Camino.class.isAssignableFrom(layerObject.getClass())){
				//mostrar un dialogo para editar el nombre del poligono
				Camino p =(Camino)layerObject;
				TextInputDialog nombreDialog = new TextInputDialog(p.getNombre());
				nombreDialog.initOwner(main.stage);
				nombreDialog.setTitle(Messages.getString("JFXMain.editarLayerDialogTitle")); //$NON-NLS-1$
				nombreDialog.setContentText(Messages.getString("JFXMain.editarLayerPoligonName")); //$NON-NLS-1$

				Optional<String> nombreOptional = nombreDialog.showAndWait();
				if(nombreOptional.isPresent()){
					p.setNombre(nombreOptional.get());
					this.getLayerPanel().update(this.getWwd());
				}
			}
			return "edite camino"; //$NON-NLS-1$
		}));

		poligonosP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.acortarCamino"),(layer)->{
			Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if(layerObject!=null && Camino.class.isAssignableFrom(layerObject.getClass())){
				layer.setEnabled(false);
				//mostrar un dialogo para editar el nombre del poligono
				Camino camino =(Camino)layerObject;
				Camino cNuevo = new Camino();
				cNuevo.setPositionsString(camino.getPositionsString());
				JFXMain.executorPool.submit(()->{
					SimplificarCaminoTask t = new SimplificarCaminoTask(cNuevo);
					t.run();				
					MeasureTool measureTool = PathLayerFactory.createCaminoLayer(cNuevo, this.getWwd(), this.getLayerPanel());
					measureTool.setArmed(false);
					Platform.runLater(()->{
						insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
						this.getLayerPanel().update(this.getWwd());
					});					
				});


			}
			return "acorte camino"; //$NON-NLS-1$
		}));
	}

	public void doEditarPoligono(Object layerObject) {
		//mostrar un dialogo para editar el nombre del poligono
		Poligono p =(Poligono)layerObject;

		PoligonoDialog pd = new PoligonoDialog(p,true);
		Optional<Poligono> op = pd.showAndWait();
		if(op.isPresent()) {
			p = op.get();
			System.out.println("p edited");
			this.getLayerPanel().update(this.getWwd());
		}
	}
	private void doCrearSiembra(List<Poligono> polis) {
		SiembraLabor labor = new SiembraLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		labor.setNombre(polis.get(0).getNombre()+" "+Messages.getString("JFXMain.255")); //$NON-NLS-1$ //$NON-NLS-2$
		Optional<SiembraLabor> siembraConfigured= SiembraConfigDialogController.config(labor);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.257")); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.258")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.259")); //$NON-NLS-1$
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = PropertyHelper.parseDouble(anchoOptional.get()).doubleValue();//Double.valueOf(anchoOptional.get());

		CrearSiembraMapTask umTask = new CrearSiembraMapTask(labor,polis,rinde);
		umTask.installProgressBar(main.getProgressBox());

		umTask.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			//siembras.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			polis.stream().forEach(p->p.getLayer().setEnabled(false));
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.260")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	private void doCrearFertilizacion(List<Poligono> polis) {
		FertilizacionLabor labor = new FertilizacionLabor();
		labor.setNombre(polis.get(0).getNombre()+" "+Messages.getString("JFXMain.fertilizacion")); //$NON-NLS-1$ //$NON-NLS-2$
		//labor.setNombre(poli.getNombre()+" "+Messages.getString("JFXMain.fertilizacion")); //$NON-NLS-1$ //$NON-NLS-2$
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.261")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.262")); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.263")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.264")); //$NON-NLS-1$
		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = PropertyHelper.parseDouble(anchoOptional.get()).doubleValue();//Double.valueOf(anchoOptional.get());

		CrearFertilizacionMapTask umTask = new CrearFertilizacionMapTask(labor,polis,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			//fertilizaciones.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			//poli.getLayer().setEnabled(false);
			polis.stream().forEach(p->p.getLayer().setEnabled(false));
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.265")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	private void doCrearPulverizacion(Poligono poli) {
		PulverizacionLabor labor = new PulverizacionLabor();
		//labor.setNombre(poli.getNombre());
		labor.setNombre(poli.getNombre()+" "+Messages.getString("JFXMain.pulverizacion")); //$NON-NLS-1$ //$NON-NLS-2$
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);

		//TODO modificar el dialog controler para poder ingresar el caldo
		Optional<PulverizacionLabor> cosechaConfigured= PulverizacionConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.249")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}							

		TextInputDialog anchoDialog = new TextInputDialog(Messages.getString("JFXMain.250")); //$NON-NLS-1$
		anchoDialog.setTitle(Messages.getString("JFXMain.251")); //$NON-NLS-1$
		anchoDialog.setContentText(Messages.getString("JFXMain.252")); //$NON-NLS-1$
		anchoDialog.initOwner(JFXMain.stage);

		Optional<String> anchoOptional = anchoDialog.showAndWait();
		Double rinde = PropertyHelper.parseDouble(anchoOptional.get()).doubleValue();//Double.valueOf(anchoOptional.get());
		CrearPulverizacionMapTask umTask = new CrearPulverizacionMapTask(labor,poli,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			PulverizacionLabor ret = (PulverizacionLabor)handler.getSource().getValue();
			//pulverizaciones.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			poli.getLayer().setEnabled(false);
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.253")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

		
	private void doCrearSuelo(Poligono poli) {
		Suelo labor = new Suelo();
		labor.setNombre(poli.getNombre());

		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);

		//		Optional<Suelo> cosechaConfigured= SueloConfigDialogController.config(labor);
		//		if(!cosechaConfigured.isPresent()){//
		//			System.out.println("el dialogo termino con cancel asi que no continuo con la cosecha");
		//			labor.dispose();//libero los recursos reservados
		//			return;
		//		}							

		TextInputDialog ppmPDialog = new TextInputDialog(Messages.getString("JFXMain.228")); //$NON-NLS-1$
		ppmPDialog.initOwner(JFXMain.stage);
		ppmPDialog.setTitle(Messages.getString("JFXMain.229")); //$NON-NLS-1$
		ppmPDialog.setContentText(Messages.getString("JFXMain.230")); //$NON-NLS-1$
		Optional<String> ppmPOptional = ppmPDialog.showAndWait();
		Double ppmP = PropertyHelper.parseDouble(ppmPOptional.get()).doubleValue();//Double.valueOf(ppmPOptional.get());

		TextInputDialog ppmNDialog = new TextInputDialog(Messages.getString("JFXMain.231")); //$NON-NLS-1$
		ppmNDialog.initOwner(JFXMain.stage);
		ppmNDialog.setTitle(Messages.getString("JFXMain.232")); //$NON-NLS-1$
		ppmNDialog.setContentText(Messages.getString("JFXMain.233")); //$NON-NLS-1$
		Optional<String> ppmNOptional = ppmNDialog.showAndWait();
		Double ppmN = PropertyHelper.parseDouble(ppmNOptional.get()).doubleValue();

		TextInputDialog pMODialog = new TextInputDialog(Messages.getString("JFXMain.234")); //$NON-NLS-1$
		pMODialog.initOwner(JFXMain.stage);
		pMODialog.setTitle(Messages.getString("JFXMain.235")); //$NON-NLS-1$
		pMODialog.setContentText(Messages.getString("JFXMain.236")); //$NON-NLS-1$
		Optional<String> pMOOptional = pMODialog.showAndWait();
		Double pMO = PropertyHelper.parseDouble(pMOOptional.get()).doubleValue();// Double.valueOf(pMOOptional.get());

		TextInputDialog densidaDialog = new TextInputDialog(Messages.getNumberFormat().format(SueloItem.DENSIDAD_SUELO_KG)); //$NON-NLS-1$
		densidaDialog.initOwner(JFXMain.stage);
		densidaDialog.setTitle("Configure la densidad"); //$NON-NLS-1$
		densidaDialog.setContentText(SueloItem.DENSIDAD); //$NON-NLS-1$
		Optional<String> dOptional = densidaDialog.showAndWait();
		Double densidad = PropertyHelper.parseDouble(dOptional.get()).doubleValue();// Double.valueOf(pMOOptional.get());
		System.out.println("ingrese densidad "+densidad);
		CrearSueloMapTask umTask = new CrearSueloMapTask(labor,poli,ppmP,ppmN,pMO,densidad);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			Suelo ret = (Suelo)handler.getSource().getValue();
			//suelos.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.237")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}
	
	private void doGuardarPoligono(Poligono layerObject){
		layerObject.setActivo(true);
		DAH.save(layerObject);
	}
	
	/**
	 * descargar los tiff correspondientes a un polygono y mostrarlos como ndvi
	 * @param placementObject
	 */
	@SuppressWarnings("unchecked")
	private void doGetNdviTiffFile(Object placementObject) {//ndvi2
		final Object plo=placementObject;
		LocalDate fin =null;
		if(placementObject !=null && Labor.class.isAssignableFrom(placementObject.getClass())){
			fin= DateConverter.asLocalDate((Date)((Labor<?>)placementObject).getFecha());
		} 
		//fin = dateChooser(fin);
		NDVIDatePickerDialog ndviDpDLG = new NDVIDatePickerDialog(JFXMain.stage);
		LocalDate ret = ndviDpDLG.ndviDateChooser(fin);
		if(ret ==null)return;//seleccionar fecha termino en cancel.
		//System.out.println(Messages.getString("JFXMain.212")+ndviDpDLG.initialDate+Messages.getString("JFXMain.213")+ndviDpDLG.finalDate); //$NON-NLS-1$ //$NON-NLS-2$
		//Begin: 2018-02-28 End: 2018-03-28
		//fin = ndviDpDLG.finalDate;


		if(ndviDpDLG.finalDate != null){
		//	File downloadLocation=null;
//			try {
//				downloadLocation = File.createTempFile(Messages.getString("JFXMain.214"), Messages.getString("JFXMain.215")).getParentFile(); //$NON-NLS-1$ //$NON-NLS-2$
//			} catch (IOException e) {
//
//				e.printStackTrace();
//			}//directoryChooser();
		//	if(downloadLocation == null) return;
			ObservableList<Ndvi> observableList = FXCollections.observableArrayList(new ArrayList<Ndvi>());
			observableList.addListener((ListChangeListener<Ndvi>) c -> {
				System.out.println(Messages.getString("JFXMain.216")); //$NON-NLS-1$
				if(c.next()){
					c.getAddedSubList().forEach((ndvi)->{
						main.ndviGUIController.doShowNDVI(ndvi);
					});//fin del foreach
				}			
			});
			if(placementObject !=null && Labor.class.isAssignableFrom(placementObject.getClass())){
				Labor<?> l =(Labor<?>)placementObject;
				
				ReferencedEnvelope bounds = l.getOutCollection().getBounds();
				Polygon pol = GeometryHelper.constructPolygon(bounds);
				placementObject =GeometryHelper.constructPoligono(pol);
				
			} 
			GetNdviForLaborTask4 task = new GetNdviForLaborTask4((Poligono)placementObject, observableList);
			task.setBeginDate(ndviDpDLG.initialDate);
			task.setFinDate(ndviDpDLG.finalDate);
			task.setIgnoreNDVI((List<Ndvi>) main.getObjectFromLayersOfClass(Ndvi.class));

			System.out.println("procesando los datos entre "+ndviDpDLG.initialDate+" y "+ ndviDpDLG.finalDate);//hasta aca ok!
			task.installProgressBar(progressBox);
			task.setOnSucceeded(handler -> {
				if(plo instanceof Poligono){
					((Poligono)plo).getLayer().setEnabled(false);
				}
				task.uninstallProgressBar();
				System.out.println("termine de descargar todos los ndvi de "+plo);
			});
			JFXMain.executorPool.submit(task);
		}
	}
	
	/**
	 * descargar los tiff correspondientes a un polygono y mostrarlos como ndvi
	 * @param placementObject
	 */
	@SuppressWarnings("unchecked")
	private void doGetLatestNdviForPoligono(Poligono poligono) {
		
		LocalDate fin =DateConverter.asLocalDate(new Date());
		LocalDate begin = fin.minus(1, ChronoUnit.MONTHS);		
			
			//aca empieza el codigo una vez que la fecha esta configurada
			ObservableList<Ndvi> observableList = FXCollections.observableArrayList(new ArrayList<Ndvi>());
			observableList.addListener((ListChangeListener<Ndvi>) c -> {
				
				if(c.next()){
					c.getAddedSubList().forEach((ndvi)->{
						main.ndviGUIController.showNdvi(poligono, ndvi, false);
					});//fin del foreach
				}			
			});				
	
			GetNdviForLaborTask4 task = new GetNdviForLaborTask4(poligono, observableList);
			task.setBeginDate(begin);
			task.setFinDate(fin);			
			task.setIgnoreNDVI((List<Ndvi>) main.getObjectFromLayersOfClass(Ndvi.class));
			System.out.println("procesando los datos entre "+begin+" y "+fin);//hasta aca ok!
			task.installProgressBar(progressBox);
			task.setOnSucceeded(handler -> {			
				task.uninstallProgressBar();			
			});
			JFXMain.executorPool.submit(task);		
	}
	
	/**
	 * metodo que toma un poligono lo clona y lo agrega a los layers de main
	 */
	private void doClonarPoligono(Poligono p) {
		Poligono clon = new Poligono();
		clon.setNombre(p.getNombre()+" clon");
		clon.setArea(p.getArea());
		clon.getPositions().addAll(p.getPositions());
		MeasureTool measureTool = PoligonLayerFactory.createPoligonMeasureTool(clon, this.getWwd(), this.getLayerPanel());		
		insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
		this.getLayerPanel().update(this.getWwd());	
	}
	
	
	/**
	 * metodo que reemplaza los puntos por una version interpolada
	 */
	private void doSimplificarPoligono(Poligono p) {
		//TODO en vez de mover los puntos agregar los puntos que hagan que las lineas sean suaves
		//que significa que una linea sea suave? 
		//h1: que su radio del circulo tangente sea mayor a R
		//h2: que los puntos alineados se reemplacen por sus extremos
		//-> reemplazar cada grupo de puntos por un segmento de recta siempre que el error sea menor a e=E/L
		main.executorPool.submit(()->{
		Geometry g = p.toGeometry();
		//g=g.buffer(ProyectionConstants.metersToLongLat(10));
		
		g=GeometryHelper.removeClosePoints(g, ProyectionConstants.metersToLongLat(2));
		g=GeometryHelper.removeSinglePoints(g, ProyectionConstants.metersToLongLat(2));
		g=GeometryHelper.reduceAlignedPoints(g, 0.2);
		Poligono pol =GeometryHelper.constructPoligono(g);

		MeasureTool measureTool = (MeasureTool) p.getLayer().getValue(PoligonLayerFactory.MEASURE_TOOL);
		measureTool.setPositions((ArrayList<? extends Position>) pol.getPositions());
		});
//		List<? extends Position> positions = measureTool.getPositions();//p.getPositions();
//		List<Position> interpolated = new ArrayList<Position>();
//		System.out.println("poligon size "+positions.size());
//		positions.remove(0);
//		positions.remove(positions.size()-1);
//		System.out.println("poligon size "+positions.size());
//		Position last = positions.get(positions.size()-1);
//		for(int i=0;i<positions.size();i++) {
//			Position este = positions.get(i);
//			double dist = Position.linearDistance(este, last).degrees;
//			//int nextIndex = i+1 < positions.size() ? i+1 : 0;
//			System.out.println("este "+i);
//			//Position next = positions.get(nextIndex);
//			
//			//Position interp = last.subtract(este);//Position.interpolate(1.3, last, este);
//			//double deltaLat = este.latitude.degrees-last.latitude.degrees;
//			//double deltaLon = este.longitude.degrees-last.longitude.degrees;
//			//Position interp=Position.fromDegrees(deltaLat*1/3, deltaLon*1/3);
//			//interp=este.subtract(interp);
//			Position interp = Position.interpolate(2/3, last, este);
//			interpolated.add(interp);
//			interpolated.add(este);
//			
//			last=este;
//		}
//		interpolated.add(interpolated.get(0));
		
		//measureTool.setPositions((ArrayList<? extends Position>) interpolated);
		
		//p.setPositions(interpolated);
	}
	
	
	public void doCrearPoligono(){
		Poligono poli = new Poligono();
		MeasureTool measureTool = PoligonLayerFactory.createPoligonMeasureTool(poli, this.getWwd(), this.getLayerPanel());
		measureTool.setArmed(true);

		insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
		this.getLayerPanel().update(this.getWwd());

		PoligonoDialog pd = new PoligonoDialog(poli,false);

		//Optional<Poligono> op = 
		pd.show();
		pd.setOnHidden((event)->{			
			measureTool.setArmed(false);
			Poligono op = pd.getResult();
			if(op!=null) {
				//p = op.get();
				//System.out.println("p created");
				//measureTool.setArmed(false);

			} else {
				this.getWwd().getModel().getLayers().remove(measureTool.getApplicationLayer());
				//measureTool.getApplicationLayer().dispose();//custom iterable exception
				measureTool.dispose();
			}
			this.getLayerPanel().update(this.getWwd());
		});
	}
	

	/**
	 * metodo que toma los poligonos seleccionados y los une si se intersectan
	 */
	private void doUnirPoligonos(){		
		@SuppressWarnings("unchecked")
		List<Poligono> pActivos = (List<Poligono>) this.getEnabledPoligonos();
		StringJoiner joiner = new StringJoiner("-");
		//joiner.add(Messages.getString("JFXMain.poligonUnionNamePrefixText"));

		List<Geometry> gActivas = pActivos.stream().map(p->{
			p.getLayer().setEnabled(false);
			joiner.add(p.getNombre());
			return p.toGeometry();
		}).collect(Collectors.toList());


		Geometry union = GeometryHelper.unirGeometrias(gActivas);

		double has = ProyectionConstants.A_HAS(union.getArea());

		Poligono poli = ExtraerPoligonosDeLaborTask.geometryToPoligono(union);
		poli.setArea(has);
		poli.setNombre(joiner.toString()); //$NON-NLS-1$

		MeasureTool measureTool = PoligonLayerFactory.createPoligonMeasureTool(poli, this.getWwd(), this.getLayerPanel());		
		insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
		this.getLayerPanel().update(this.getWwd());		
	}
	

	public void doMedirDistancia(){
		Camino camino = new Camino();
		MeasureTool measureTool = PathLayerFactory.createCaminoLayer(camino, this.getWwd(), this.getLayerPanel());
		measureTool.setArmed(true);

		JFXMain.insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
		this.getLayerPanel().update(this.getWwd());	

		Alert supDialog = new Alert(Alert.AlertType.INFORMATION);
		supDialog.initOwner(JFXMain.stage);
		supDialog.setTitle(Messages.getString("JFXMain.medirDistancia")); //$NON-NLS-1$
		supDialog.setHeaderText(Messages.getString("JFXMain.medirDistanciaHeaderText")); //$NON-NLS-1$

		//Text t = new Text();
		TextField nombreTF = new TextField();
		nombreTF.setPromptText(Messages.getString("JFXMain.medirDistanciaHeaderText")); //$NON-NLS-1$
		VBox vb = new VBox();
		vb.getChildren().addAll(nombreTF);
		supDialog.setGraphic(vb);
		supDialog.initModality(Modality.NONE);
		nombreTF.textProperty().addListener((obj,old,n)->{
			camino.setNombre(n);
			// es importante para que se modifique el layerPanel con el nombre actualizado
			this.getLayerPanel().update(this.getWwd());
		});

		supDialog.show();
		supDialog.setOnHidden((event)->{			
			measureTool.setArmed(false);
			this.getLayerPanel().update(this.getWwd());
		});
	}
	
	/**
	 * metodo que toma los poligonos seleccionados calcula la inteseccion y agrega
	 * los poligonos intesectados
	 * a int b
	 * a - (a int b)
	 * b - (a int b)
	 * 
	 */
	private void doIntersectarPoligonos(){
		JFXMain.executorPool.submit(()->{
			try {
				List<Geometry> geometriasActivas = new ArrayList<Geometry>();
				//1 obtener los poligonos activos
				String nombre = Messages.getString("JFXMain.poligonIntersectionNamePrefix");
				LayerList layers = this.getWwd().getModel().getLayers();
				for (Layer l : layers) {
					Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					if (l.isEnabled() && o instanceof Poligono){
						Poligono p = (Poligono)o;
						geometriasActivas.add(p.toGeometry());
						l.setEnabled(false);
						p.setActivo(false);
						nombre=nombre+" "+p.getNombre();
					}
				}

				Set<Geometry> geometriasOutput = GeometryHelper.obtenerIntersecciones(geometriasActivas);

				geometriasOutput = geometriasOutput.stream().map(g->{
					Densifier densifier = new Densifier(g);
					densifier.setDistanceTolerance(ProyectionConstants.metersToLongLat(10));
					g=densifier.getResultGeometry();
					return  g;			
				}).collect(Collectors.toSet());

				int num=0;
				for(Geometry g : geometriasOutput){
					Poligono poli = ExtraerPoligonosDeLaborTask.geometryToPoligono(g);
					if(poli ==null)continue;
					MeasureTool measureTool = PoligonLayerFactory.createPoligonMeasureTool(poli, this.getWwd(), this.getLayerPanel());
					double has = ProyectionConstants.A_HAS(g.getArea());
					poli.setArea(has);
					poli.setNombre(nombre+" ["+num+"]");num++; 
					insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
				}

				this.getLayerPanel().update(this.getWwd());
			}catch(Exception e) {
				System.err.println("Error al intesectar los poligonos"); //$NON-NLS-1$
				e.printStackTrace();
			}
		});
	}
	
	

	private void doConvertirPoligonosACosecha() {
		List<Poligono> geometriasActivas = new ArrayList<Poligono>();
		LayerList layers = this.getWwd().getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Poligono){
				Poligono p = (Poligono)o;
				geometriasActivas.add(p);
			}
		}

		doCrearCosecha(geometriasActivas);
	}

	private void doConvertirPoligonosASiembra() {
		List<Poligono> geometriasActivas = new ArrayList<Poligono>();
		//1 obtener los poligonos activos
		//String nombre = Messages.getString("JFXMain.poligonIntersectionNamePrefix");
		LayerList layers = this.getWwd().getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Poligono){
				Poligono p = (Poligono)o;
				geometriasActivas.add(p);
				//l.setEnabled(false);
				//p.setActivo(false);
				//nombre=nombre+" "+p.getNombre();
			}
		}
		System.out.println("generando siembra para "+geometriasActivas.size()+" poligonos");
		doCrearSiembra(geometriasActivas);
		//		Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
		//		if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
		//			//
		//			//List<Poligono> geometriasActivas = new ArrayList<Poligono>();
		//			//1 obtener los poligonos activos
		//			LayerList layers = this.getWwd().getModel().getLayers();
		//			for (Layer l : layers) {
		//				Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
		//				if (l.isEnabled() && o instanceof Poligono){
		//					Poligono p = (Poligono)o;
		//					geometriasActivas.add(p);
		//				}
		//			}
		//			System.out.println("convirtiendo poligonos a siembra");
		//			//doConvertirASiembra((Polygon) layerObject);
		//			doCrearSiembra(geometriasActivas);
		//		}

	}

	private void doConvertirPoligonosAFertilizacion() {
		List<Poligono> geometriasActivas = new ArrayList<Poligono>();
		//1 obtener los poligonos activos
		//String nombre = Messages.getString("JFXMain.poligonIntersectionNamePrefix");
		LayerList layers = this.getWwd().getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Poligono){
				Poligono p = (Poligono)o;
				geometriasActivas.add(p);
				//l.setEnabled(false);
				//p.setActivo(false);
				//nombre=nombre+" "+p.getNombre();
			}
		}
		System.out.println("generando fertilizacion para "+geometriasActivas.size()+" poligonos");
		doCrearFertilizacion(geometriasActivas);
		//		Object layerObject = layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
		//		if(layerObject!=null && Poligono.class.isAssignableFrom(layerObject.getClass())){
		//			//
		//			//List<Poligono> geometriasActivas = new ArrayList<Poligono>();
		//			//1 obtener los poligonos activos
		//			LayerList layers = this.getWwd().getModel().getLayers();
		//			for (Layer l : layers) {
		//				Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
		//				if (l.isEnabled() && o instanceof Poligono){
		//					Poligono p = (Poligono)o;
		//					geometriasActivas.add(p);
		//				}
		//			}
		//			System.out.println("convirtiendo poligonos a siembra");
		//			//doConvertirASiembra((Polygon) layerObject);
		//			doCrearSiembra(geometriasActivas);
		//		}

	}

	

	

	private void doCrearCosecha(List<Poligono> polis) {
		CosechaLabor labor = new CosechaLabor();
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
		//labor.setNombre(poli.getNombre());
		labor.setNombre(polis.get(0).getNombre()+" "+Messages.getString("JFXMain.cosecha")); //$NON-NLS-1$ //$NON-NLS-2$
		Optional<CosechaLabor> cosechaConfigured= HarvestConfigDialogController.config(labor);
		if(!cosechaConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.266")); //$NON-NLS-1$
			labor.dispose();//libero los recursos reservados
			return;
		}		
		Double rindeEsperado = cosechaConfigured.get().getCultivo().getRindeEsperado();
		TextInputDialog rindeDialog = new TextInputDialog(Messages.getNumberFormat().format(rindeEsperado));//Messages.getString("JFXMain.272")); 
			
		//TextInputDialog rindeDialog = new TextInputDialog(Messages.getString("JFXMain.267")); //$NON-NLS-1$
		rindeDialog.setTitle(Messages.getString("JFXMain.268")); //$NON-NLS-1$
		rindeDialog.setContentText(Messages.getString("JFXMain.269")); //$NON-NLS-1$
		rindeDialog.initOwner(JFXMain.stage);
		Optional<String> anchoOptional = rindeDialog.showAndWait();
		if(!anchoOptional.isPresent())return;
		Double rinde = PropertyHelper.parseDouble(anchoOptional.get()).doubleValue();//Double.valueOf(anchoOptional.get());

		CrearCosechaMapTask umTask = new CrearCosechaMapTask(labor,polis,rinde);
		umTask.installProgressBar(progressBox);

		umTask.setOnSucceeded(handler -> {
			CosechaLabor ret = (CosechaLabor)handler.getSource().getValue();
			//cosechas.add(ret);
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());
			//poli.getLayer().setEnabled(false);
			polis.stream().forEach(p->p.getLayer().setEnabled(false));
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			umTask.uninstallProgressBar();
			System.out.println(Messages.getString("JFXMain.270")); //$NON-NLS-1$
			playSound();
		});//fin del OnSucceeded
		JFXMain.executorPool.execute(umTask);		
	}

	/**
	 * descargar los tiff correspondientes a un polygono y mostrarlos como ndvi
	 * @param placementObject
	 */
	@SuppressWarnings("unchecked")
	private void doGetNdviTiffFiles(List<Poligono> poligonos) {//ndvi2
		LocalDate fin =null;

		//fin = dateChooser(fin);
		NDVIDatePickerDialog ndviDpDLG = new NDVIDatePickerDialog(JFXMain.stage);
		LocalDate ret = ndviDpDLG.ndviDateChooser(fin);
		if(ret ==null)return;//seleccionar fecha termino en cancel.
		//System.out.println(Messages.getString("JFXMain.212")+ndviDpDLG.initialDate+Messages.getString("JFXMain.213")+ndviDpDLG.finalDate); //$NON-NLS-1$ //$NON-NLS-2$
		//Begin: 2018-02-28 End: 2018-03-28
		//fin = ndviDpDLG.finalDate;


		if(ndviDpDLG.finalDate != null){
			File downloadLocation=null;
			try {
				downloadLocation = File.createTempFile(Messages.getString("JFXMain.214"), Messages.getString("JFXMain.215")).getParentFile(); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(downloadLocation == null) return;
			for(Poligono p : poligonos) {
				ObservableList<Ndvi> observableList = FXCollections.observableArrayList(new ArrayList<Ndvi>());
				observableList.addListener((ListChangeListener<Ndvi>) c -> {					
					if(c.next()){
						c.getAddedSubList().forEach((ndvi)->{
							main.ndviGUIController.doShowNDVI(ndvi);
						});//fin del foreach
					}			
				});

				GetNdviForLaborTask4 task = new GetNdviForLaborTask4(p,observableList);
				task.setBeginDate(ndviDpDLG.initialDate);
				task.setFinDate(ndviDpDLG.finalDate);
				task.setIgnoreNDVI((List<Ndvi>) main.getObjectFromLayersOfClass(Ndvi.class));


				System.out.println("procesando los datos entre "+ndviDpDLG.initialDate+" y "+ ndviDpDLG.finalDate);//hasta aca ok!
				task.installProgressBar(progressBox);
				task.setOnSucceeded(handler -> {
					if(p instanceof Poligono){
						((Poligono)p).getLayer().setEnabled(false);
					}
					task.uninstallProgressBar();
				});

				System.out.println(Messages.getString("JFXMain.217")); //$NON-NLS-1$
				executorPool.submit(task);
			}
		}
	}
	
	public void showPoligonos(List<Poligono> poligonos) {
		
		Platform.runLater(()->{
			for(Poligono poli : poligonos){
			MeasureTool measureTool = PoligonLayerFactory.createPoligonMeasureTool(poli, this.getWwd(), this.getLayerPanel());
		
				insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
				this.getLayerPanel().update(this.getWwd());//ponerlo fuera del for?
			
		}
		});
	}
	
	public void showPoligonosActivos() {
		List<Poligono> poligonos = DAH.getPoligonosActivos();
		showPoligonos(poligonos);
		//XXX obtener todos los ndvi de los poligonos activos no es bueno
		//TODO marcar poligonos como a actualizar o actualizar los ndvi de
		//los contornos de los lotes
		//poligonos.stream().forEach(p->this.doGetLatestNdviForPoligono(p));		
	}

	
	/**
	 * @see doExtraerPoligonos(Labor<?>) para ver como optimizar este codigo
	 * @ metodo que lee todas las geometrias de todos los archivos seleccionados y los carga como poligonos en memoria
	 * @param files
	 */
	public void doImportarPoligonos(List<File> files) {
		List<FileDataStore> stores = FileHelper.chooseShapeFileAndGetMultipleStores(files);
		executorPool.submit(()->{
			if (stores != null) {for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				System.out.println(Messages.getString("JFXMain.312")); //$NON-NLS-1$
				try {
					String storeName = store.getNames().get(0).getLocalPart().replace("%20", " ");
					//System.out.println(storeName);
					SimpleFeatureSource	source = store.getFeatureSource();

					SimpleFeatureIterator iterator = source.getFeatures().features();
					int i=0;
					while(iterator.hasNext()){
						SimpleFeature feature = iterator.next();	

						double has = ProyectionConstants.A_HAS(((Geometry)feature.getDefaultGeometry()).getArea());
						//						if(has<0.02) {
						//							System.out.println("descarto poligono por ser menor a 0.2has");
						//							continue;//cada poli mayor a 10m2
						//						}
						Poligono poli = ExtraerPoligonosDeLaborTask.featureToPoligono(feature);
						poli.setNombre(storeName+" ["+Integer.toString(i)+"]");
						i++;
						try {
							//intento tomar el nombre del atributo si tiene la columna Name
							String name = (String) feature.getAttribute("Name");
							if(name!=null) {
								poli.setNombre(name);
							}
						}catch(Exception e) {
							e.printStackTrace();

						}

						MeasureTool measureTool = PoligonLayerFactory.createPoligonMeasureTool(poli, this.getWwd(), this.getLayerPanel());
						poli.setArea(has);
						insertBeforeCompass(this.getWwd(), measureTool.getApplicationLayer());
					}//fin del while sobre las features
					this.getLayerPanel().update(this.getWwd());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}//fin del for stores
			}//if stores != null
		});
	}
}
