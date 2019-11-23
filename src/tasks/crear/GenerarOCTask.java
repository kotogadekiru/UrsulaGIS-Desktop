package tasks.crear;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import dao.Labor;
import dao.LaborItem;
import dao.OrdenDeCompra.OrdenCompra;
import dao.OrdenDeCompra.OrdenCompraItem;
import dao.OrdenDeCompra.Producto;
import dao.config.Semilla;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionLabor;
import dao.pulverizacion.PulverizacionLabor;
import dao.siembra.SiembraConfig;
import dao.siembra.SiembraLabor;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import utils.ProyectionConstants;

public class GenerarOCTask  extends Task<OrdenCompra>{
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;
	
	private List<SiembraLabor> siembras=null;
	private List<FertilizacionLabor> fertilizaciones=null;
	private List<PulverizacionLabor> pulverizaciones=null;
	
	public GenerarOCTask(List<SiembraLabor> _siembras,List<FertilizacionLabor> _fertilizaciones, List<PulverizacionLabor> _pulverizaciones){
		this.siembras=_siembras;
		this.fertilizaciones=_fertilizaciones;
		this.pulverizaciones=_pulverizaciones;
	}
	public OrdenCompra call()  {
		
		
		Map<Producto,Double> prodCantidadMap = new HashMap<Producto,Double>();
		
		this.siembras.forEach(l->{
			Producto producto =l.getSemilla();
			Double cantidadItem = getOrdenCompraLabor(l);
			putItem(prodCantidadMap, producto, cantidadItem);
			
		});
		this.fertilizaciones.forEach(l->{
			Producto producto =l.getFertilizanteProperty().getValue();
			Double cantidadItem = getOrdenCompraLabor(l);
			putItem(prodCantidadMap, producto, cantidadItem);
		});
		this.pulverizaciones.forEach(l->{
			Producto producto =l.getAgroquimico().getValue();
			Double cantidadItem = getOrdenCompraLabor(l);
			putItem(prodCantidadMap, producto, cantidadItem);
		});
		
		List<OrdenCompraItem> itemsOC = new ArrayList<>();
		prodCantidadMap.forEach((producto,cantidad)->{
			itemsOC.add(new OrdenCompraItem(producto,cantidad));
		});
	
		OrdenCompra oc=new OrdenCompra();		
		oc.setItems(itemsOC);
		
		return oc;
	}
	
	private void putItem(Map<Producto, Double> items, Producto producto, Double cantidad) {
		if(items.containsKey(producto)) {
			Double existente = items.get(producto);
			items.put(producto, existente+cantidad);
		} else {
			items.put(producto,cantidad);
		}
	}
	

	private Double getOrdenCompraLabor(final Labor<?> labor) {
		Double cantidad = new Double(0.0);
		SimpleFeatureIterator it = labor.outCollection.features();
		while(it.hasNext()){
			SimpleFeature f = it.next();
			Double rinde = LaborItem.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));//labor.colAmount.get()
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Double area = geometry.getArea() * ProyectionConstants.A_HAS();
		
			cantidad+=rinde*area;
			
		}
		it.close();
		
		return cantidad;
	}
	
	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Generar Orden de Compra");
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(TASK_CLOSE_ICON));
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
