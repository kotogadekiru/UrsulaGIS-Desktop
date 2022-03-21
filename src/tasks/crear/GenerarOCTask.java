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
import dao.config.Cultivo;
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
		Map<Producto,OrdenCompraItem> prodCantidadMap = new HashMap<Producto,OrdenCompraItem>();
		StringBuilder description = new StringBuilder();
		this.siembras.forEach(l->{
			description.append(l.getNombre());
			Producto producto =l.getSemilla();
			Cultivo cultivo = l.getSemilla().getCultivo();
			//TODO resolver el problema de la unidad de compra. kg o bolsas
			//si cultivo es maiz que la unidad sea bolsas. 
			
			Double cantidadItem = l.getCantidadInsumo();
			putItem(prodCantidadMap, producto, cantidadItem,l.getPrecioInsumo());
			putItem(prodCantidadMap, l.getProductoLabor(), l.getCantidadLabor(),l.getPrecioLabor());
			
		});
		this.fertilizaciones.forEach(l->{
			description.append(l.getNombre());
			Producto producto =l.getFertilizanteProperty().getValue();
			
			Double cantidadItem = l.getCantidadInsumo();
			putItem(prodCantidadMap, producto, cantidadItem,l.getPrecioInsumo());
			putItem(prodCantidadMap, l.getProductoLabor(), l.getCantidadLabor(),l.getPrecioLabor());
		});
		this.pulverizaciones.forEach(l->{
			description.append(l.getNombre());
			Producto producto =l.getAgroquimico().getValue();
			
			Double cantidadItem = l.getCantidadInsumo();
			putItem(prodCantidadMap, producto, cantidadItem,l.getPrecioInsumo());
			putItem(prodCantidadMap, l.getProductoLabor(), l.getCantidadLabor(),l.getPrecioLabor());
		});
		
		//reduzco la list
//		List<OrdenCompraItem> itemsOC = new ArrayList<>();
//		prodCantidadMap.forEach((producto,cantidad)->{
//			OrdenCompraItem item = new OrdenCompraItem(producto,cantidad);
//			itemsOC.add(item);
//		});
	
		OrdenCompra oc=new OrdenCompra();		
		oc.setDescription(description.toString());
		oc.setItems(new ArrayList<OrdenCompraItem>(prodCantidadMap.values()));
		//oc.getItems().stream().forEach((item)->item.setOrdenCompra(oc));
		
		return oc;
	}
	
	private void putItem(Map<Producto, OrdenCompraItem> items, Producto producto, Double cantidad,Double precio) {
		if(items.containsKey(producto)) {
			OrdenCompraItem existente = items.get(producto);
			existente.setPrecio((existente.getImporte()+precio*cantidad)/(existente.getCantidad()+cantidad));
			existente.setCantidad(existente.getCantidad()+cantidad);
			items.put(producto, existente);
		} else {
			OrdenCompraItem item =new OrdenCompraItem(producto,cantidad);
			item.setPrecio(precio);
			items.put(producto,item);
		}
	}
	

//	private Double getOrdenCompraLabor(final Labor<?> labor) {
//		Double cantidad = new Double(0.0);
//		Double area = new Double(0.0);
//		SimpleFeatureIterator it = labor.outCollection.features();
//		while(it.hasNext()){
//			SimpleFeature f = it.next();
//			Double rinde = LaborItem.getDoubleFromObj(f.getAttribute(labor.colAmount.get()));//labor.colAmount.get()
//			Geometry geometry = (Geometry) f.getDefaultGeometry();
//			Double a = geometry.getArea() * ProyectionConstants.A_HAS();
//			area+=a;
//			cantidad+=rinde*a;
//			
//		}
//		it.close();
//		labor.setCantidadLabor(area);
//		labor.setCantidadInsumo(cantidad);
//		
//		return cantidad;
//	}
	
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
