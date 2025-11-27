package com.ursulagis.desktop.tasks.crear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.api.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.dao.config.Semilla;
import com.ursulagis.desktop.dao.cosecha.CosechaItem;
import com.ursulagis.desktop.dao.fertilizacion.FertilizacionLabor;
import com.ursulagis.desktop.dao.ordenCompra.OrdenCompra;
import com.ursulagis.desktop.dao.ordenCompra.OrdenCompraItem;
import com.ursulagis.desktop.dao.ordenCompra.Producto;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.dao.siembra.SiembraHelper;
import com.ursulagis.desktop.dao.siembra.SiembraItem;
import com.ursulagis.desktop.dao.siembra.SiembraLabor;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import com.ursulagis.desktop.utils.ProyectionConstants;

public class GenerarOrdenCompraTask  extends Task<OrdenCompra>{
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	private List<SiembraLabor> siembras=null;
	private List<FertilizacionLabor> fertilizaciones=null;
	private List<PulverizacionLabor> pulverizaciones=null;

	public GenerarOrdenCompraTask(List<SiembraLabor> _siembras,List<FertilizacionLabor> _fertilizaciones, List<PulverizacionLabor> _pulverizaciones){
		this.siembras=_siembras;
		this.fertilizaciones=_fertilizaciones;
		this.pulverizaciones=_pulverizaciones;
	}
	public OrdenCompra call()  {
		try {
		//TODO agregar contorno a ordenCompra
		Map<Producto,OrdenCompraItem> prodCantidadMap = new HashMap<Producto,OrdenCompraItem>();
		StringBuilder description = new StringBuilder();

		
		this.siembras.forEach(l->{
			description.append(l.getNombre());
			Semilla producto =l.getSemilla();
			
			Cultivo cultivo = l.getSemilla().getCultivo();
			Double semBolsa = cultivo.getSemPorBolsa();
			double kgBolsa=1;
			if(semBolsa!=null) {
				Double pMil = l.getSemilla().getPesoDeMil();
				kgBolsa=semBolsa*pMil/1000000;			
			}
			
			double fertCTotal=0;
			double fertLTotal=0;
			double insumoTotal=0;
			double laborTotal=0;
			SimpleFeatureIterator it = l.outCollection.features();
//			fertCTotal =  l.getCantidadFertilizanteCostado();
//			fertLTotal = l.getCantidadFertilizanteLinea();
			while(it.hasNext()){
				SimpleFeature f = it.next();
				Double kgHa = LaborItem.getDoubleFromObj(f.getAttribute(l.colAmount.get()));//labor.colAmount.get()
				Double fertL = LaborItem.getDoubleFromObj(f.getAttribute(SiembraLabor.COLUMNA_DOSIS_LINEA));
				Double fertC = LaborItem.getDoubleFromObj(f.getAttribute(SiembraLabor.COLUMNA_DOSIS_COSTADO));
				Geometry geometry = (Geometry) f.getDefaultGeometry();
				Double area = geometry.getArea() * ProyectionConstants.A_HAS();		
				fertLTotal+=fertL*area;
				fertCTotal+=fertC*area;
				insumoTotal+=kgHa*area;
				laborTotal+=area;
			}
			it.close();			
			if(kgBolsa==0) {
				putItem(prodCantidadMap, producto, insumoTotal,l.getPrecioInsumo());
			}else {
			putItem(prodCantidadMap, producto, insumoTotal/kgBolsa,l.getPrecioInsumo());
			}
			putItem(prodCantidadMap, l.getProductoLabor(), laborTotal,l.getPrecioLabor());
			
			putItem(prodCantidadMap, l.getFertLinea(), fertLTotal,0.0);
			putItem(prodCantidadMap, l.getFertCostado(), fertCTotal,0.0);
		});
		
//		this.siembras.forEach(l->{
//			description.append(l.getNombre());
//			Producto producto =l.getSemilla();
//			Cultivo cultivo = l.getSemilla().getCultivo();
//			Double semBolsa = cultivo.getSemPorBolsa();
//			double kgBolsa=1;
//			if(semBolsa!=null) {
//				Double pMil = l.getSemilla().getPesoDeMil();
//				kgBolsa=semBolsa*pMil/1000000;
//			}
//			
//			//TODO resolver el problema de la unidad de compra. kg o bolsas
//			//si cultivo es maiz que la unidad sea bolsas. 			
//
//
//			Double cantSemilla = l.getCantLabor(SiembraHelper.getSemillaCantMethod());
//			Double cantidadFertL = l.getCantLabor(SiembraHelper.getFertLCantMethod());
//			Double cantidadFertC = l.getCantLabor(SiembraHelper.getFertCCantMethod());
//			cantSemilla = cantSemilla/kgBolsa;
//			
//			putItem(prodCantidadMap, l.getFertCostado(), cantidadFertC,0.0);
//			putItem(prodCantidadMap, l.getFertLinea(), cantidadFertL,0.0);
//			putItem(prodCantidadMap, producto, cantSemilla,kgBolsa/l.getPrecioInsumo());
//			putItem(prodCantidadMap, l.getProductoLabor(), l.getCantidadLabor(),l.getPrecioLabor());
//			
//			if(l.getFertCostado()!=null) {
//				putItem(prodCantidadMap, l.getFertCostado(), l.getCantidadFertilizanteCostado(),0.0);
//			}
//			if(l.getFertLinea()!=null) {
//				putItem(prodCantidadMap, l.getFertLinea(), l.getCantidadFertilizanteLinea(),0.0);
//			}
//
//		});
		
		this.fertilizaciones.forEach(l->{
			description.append(l.getNombre());
			Producto producto =l.getFertilizante();
			
			double insumoTotal=0;
			double laborTotal=0;
			SimpleFeatureIterator it = l.outCollection.features();
			while(it.hasNext()){
				SimpleFeature f = it.next();
				Double rinde = LaborItem.getDoubleFromObj(f.getAttribute(l.colAmount.get()));//labor.colAmount.get()
				Geometry geometry = (Geometry) f.getDefaultGeometry();
				Double area = geometry.getArea() * ProyectionConstants.A_HAS();			
				insumoTotal+=rinde*area;
				laborTotal+=area;
			}
			it.close();			
			putItem(prodCantidadMap, producto, insumoTotal,l.getPrecioInsumo());
			putItem(prodCantidadMap, l.getProductoLabor(), laborTotal,l.getPrecioLabor());
		});
		
		this.pulverizaciones.forEach(l->{
			description.append(l.getNombre());
			//Producto producto =l.getAgroquimico().getValue();
			Double cantidadItem = l.getCantidadInsumo();
			l.getItems().forEach((caldoItem)->{
				putItem(prodCantidadMap, caldoItem.getProducto(),
						cantidadItem*caldoItem.getDosisHa(),
						l.getPrecioInsumo());
			});
			//Double cantidadItem = l.getCantidadInsumo();
			//putItem(prodCantidadMap, producto, cantidadItem,l.getPrecioInsumo());
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
		oc.getItems().stream().forEach((item)->item.setOrdenCompra(oc));

		return oc;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void putItem(Map<Producto, OrdenCompraItem> items, Producto producto, Double cantidad, Double precio) {
		if(cantidad==0)return;
		if(items.containsKey(producto)) {
			OrdenCompraItem existente = items.get(producto);
			existente.calcImporte();
			Double nuevaCantidad = existente.getCantidad()+cantidad;
			Double nuevoPrecio = nuevaCantidad>0?(existente.getImporte()+precio*cantidad)
					/(nuevaCantidad):0;
			existente.setPrecio(nuevoPrecio);
			existente.setCantidad(nuevaCantidad);
			items.put(producto, existente);
		} else {
			OrdenCompraItem item =new OrdenCompraItem(producto,cantidad);
			item.setPrecio(precio);
			items.put(producto,item);
		}
	}


	//	private Double getOrdenCompraLabor(final Labor<?> labor) {
	//		Double cantidad = Double.parseDouble(0.0);
	//		Double area = Double.parseDouble(0.0);
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
