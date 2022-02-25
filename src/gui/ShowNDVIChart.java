package gui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.Labor;
import dao.Ndvi;
import dao.Poligono;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gui.nww.LayerPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import utils.ExcelHelper;
//agrege que extende de vbox 
public class ShowNDVIChart extends VBox {
	private WorldWindow wwd;
	private LineChart<Number,Number> lineChart =null;

	public ShowNDVIChart(WorldWindow _wwd) {
		super ();//sueper
		this.wwd=_wwd;
		//	this.layerPanel=_lP;

	}

	public void doShowNDVIChart() {
		//TODO agregar grafico con la evolucion del ndvi promedio, la superficie de nubes agua y cultivo
		List<SurfaceImageLayer> ndviLayers = extractLayers();

		//System.out.println("mostrar grafico");
		final NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel("Fecha");
		xAxis.setTickLabelFormatter(new StringConverter<Number>() {
			@Override
			public String toString(Number epochDay) {
				return LocalDate.ofEpochDay(epochDay.longValue()).toString();
			}

			@Override
			public Number fromString(String string) {
				return LocalDate.parse(string).toEpochDay();
			
			}			
		});
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLabel("NDVI");
		//line chart syntax
		lineChart = new LineChart<Number, Number>(xAxis, yAxis);				

		//SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy");
		//junto los ndvi segun fecha para hacer la evolucion correctamente.

		Map<Poligono, List<SurfaceImageLayer>>  contornoMap = ndviLayers.stream().collect(
				Collectors.groupingBy((l2)->{
					Ndvi lNdvi = (Ndvi)l2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
					return lNdvi.getContorno();// fecha me devuelve siempre hoy por eso no hace la animacion
				}));

		contornoMap.keySet().stream().forEach((c)->{
			XYChart.Series<Number,Number> sr = new XYChart.Series<Number,Number>(); 
			sr.setName(c.getNombre() );
			contornoMap.get(c).stream().map(
					(layer)->(Ndvi)layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR)).sorted((n1,n2)->n1.compareTo(n2)).forEachOrdered(lNdvi->{
				//Ndvi lNdvi = (Ndvi)layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);

				sr.getData().add(new XYChart.Data<Number, Number>(lNdvi.getFecha().toEpochDay(), lNdvi.getMeanNDVI().doubleValue()));
			});
			lineChart.getData().add(sr);	
		});



		VBox vbox = new VBox(lineChart);
		
		VBox right = new VBox();
		Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //$NON-NLS-1$
		exportButton.setOnAction(a->{doExportarExcell();});
		right.getChildren().add(exportButton);
		BorderPane bottom = new BorderPane();
		//bottom.setCenter(left);
		bottom.setRight(right);//getChildren().addAll(left,right);
		bottom.setPadding(new Insets(5,5,5,5));
		vbox.getChildren().add(bottom);
		this.getChildren().add(vbox);
		

	
		System.out.println("Mostre grafico");


	}

	private void doExportarExcell() {
		ExcelHelper xHelper = new ExcelHelper();
		xHelper.exportSeriesList(this.lineChart.getData());
	}
	public List<SurfaceImageLayer> extractLayers() {
		List<SurfaceImageLayer> ndviLayers = new ArrayList<SurfaceImageLayer>();
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			Object o = l.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			if (l.isEnabled() && o instanceof Ndvi){
				//l.setEnabled(false);
				ndviLayers.add((SurfaceImageLayer) l);
			}
		}	

		//System.out.println("mostrando la evolucion de "+ndviLayers.size()+" layers");
		ndviLayers.sort(new NdviLayerComparator());
		return ndviLayers;
	}

	public class NdviLayerComparator implements Comparator<Layer>{
		DateTimeFormatter df =null;// DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		public NdviLayerComparator() {
			df = DateTimeFormatter.ofPattern("dd-MM-yyyy"); //$NON-NLS-1$
		}

		@Override
		public int compare(Layer c1, Layer c2) {			
			String l1Name =c1.getName();
			String l2Name =c2.getName();

			Object labor1 = c1.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);
			Object labor2 = c2.getValue(Labor.LABOR_LAYER_IDENTIFICATOR);


			if(labor1 != null && labor1 instanceof Ndvi && 
					labor2 != null && labor2 instanceof Ndvi ){
				Ndvi ndvi1 = (Ndvi)labor1;
				Ndvi ndvi2 = (Ndvi)labor2;

				try{
					return ndvi1.getFecha().compareTo(ndvi2.getFecha());
				} catch(Exception e){
					//System.err.println("no se pudo comparar las fechas de los ndvi. comparando nombres"); //$NON-NLS-1$
				}
				// comparar por el valor del layer en vez del nombre del layer
				try{
					LocalDate d1 = LocalDate.parse(l1Name.substring(l1Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					LocalDate d2 = LocalDate.parse(l2Name.substring(l2Name.length()-"dd-MM-yyyy".length()),df); //$NON-NLS-1$
					return d1.compareTo(d2);
				} catch(Exception e){
					//no se pudo parsear como fecha entonces lo interpreto como string.
					e.printStackTrace();
				}
			}
			return l1Name.compareToIgnoreCase(l2Name);
		}
	}
}
