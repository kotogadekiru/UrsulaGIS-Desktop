package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import dao.Ndvi;
import dao.Poligono;
import dao.OrdenDeCompra.OrdenCompra;
import dao.OrdenDeCompra.OrdenCompraItem;
import dao.config.Configuracion;



public class ExcelHelper {

	//	private Workbook workbook;
	//	private Sheet sheet = null;

	public ExcelHelper() {

	}


	private  File getNewExcelFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Guardar ShapeFile");
		fileChooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("XLSX", "*.xlsx"));

		File lastFile = null;
		Configuracion config = Configuracion.getInstance();
		String lastFileName = config.getPropertyOrDefault(Configuracion.LAST_FILE,null);
		if(lastFileName != null){
			lastFile = new File(lastFileName);
		}
		if(lastFile ==null || ! lastFile.exists()) {
			lastFile=File.listRoots()[0];
		} 
		//if(lastFile != null && lastFile.exists()){
		String initFileName = lastFile.getName();
		if(initFileName.contains(".")) {
			initFileName=initFileName.substring(0, initFileName.lastIndexOf('.'));
		}
		fileChooser.setInitialDirectory(lastFile.getParentFile());
		fileChooser.setInitialFileName(initFileName);
				
		config.setProperty(Configuracion.LAST_FILE, lastFile.getAbsolutePath());
		

		File file = fileChooser.showSaveDialog(new Stage());
		 config.setProperty(Configuracion.LAST_FILE,file.getParent());
		 config.save();

		System.out.println("archivo seleccionado para guardar "+file);

		return file;
	}
	


	public void readExcelFile() {

		try {
			FileInputStream file = new FileInputStream(new File(
					"howtodoinjava_demo.xlsx"));

			// Create Workbook instance holding reference to .xlsx file
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			// workbook.getSheet("nombreDeLaHoja");

			// Get first/desired sheet from the workbook
			XSSFSheet sheet = workbook.getSheetAt(0);

			// Iterate through each rows one by one
			Iterator<Row> rowIterator = sheet.iterator();
			Row row5 = sheet.getRow(5);
			row5.getCell(0);

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				// For each row, iterate through all the columns
				Iterator<Cell> cellIterator = row.cellIterator();

				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					// Check the cell type and format accordingly
					switch (cell.getCellType()) {
					case Cell.CELL_TYPE_NUMERIC:
						System.out.print(cell.getNumericCellValue() + "t");
						break;
					case Cell.CELL_TYPE_STRING:
						System.out.print(cell.getStringCellValue() + "t");
						break;
					}
				}
				System.out.println("");
			}
			file.close();
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



//	public static void main(String[] args) {
//		ExcelHelper xlsH = new ExcelHelper();
//		String filename = "Monitor - LA TAPERA-14-15.xlsx";
//		if(args.length>0){
//			for(int i=0;i<args.length;i++){
//				filename=args[i];
//				System.out.println(filename);
//				xlsH.importarMonitorExcelFile(filename);
//			}
//
//		}else{
//			xlsH.importarMonitorExcelFile(filename);
//		}
//		xlsH.writeNewExcelFile();
//		// xlsH.readExcelFile();
//
//	}



//		public String getStringValue(int row, int col) {
//			String ret = new String();
//			//try {
//			Cell cell = sheet.getRow(row).getCell(col);
//
//			ret = getStringValue(cell);
//			//		} catch (NullPointerException e) {
//			//			System.err
//			//			.println("Error al tratar de obtener un dato numerico en la celda (fila,col)= ("
//			//					+ row + "," + col + ") =>" +ret);
//			//
//			//		}
//			return ret;
//		}

//		private String getStringValue( Cell cell) {
//			String ret =  new String();
//			FormulaEvaluator evaluator = workbook.getCreationHelper()
//					.createFormulaEvaluator();
//			if (cell != null) {
//				switch (evaluator.evaluateInCell(cell).getCellType()) {
//				case Cell.CELL_TYPE_NUMERIC:
//					ret = String.valueOf(cell.getNumericCellValue());
//					break;
//				case Cell.CELL_TYPE_STRING:
//					ret = cell.getStringCellValue();
//					break;
//				}
//			}
//			return ret;
//		}

//		public Double getDoubleValue(int row, int col) {
//			Double ret = new Double(0);
//			String stringValue = null;
//			//	try {
//			stringValue = getStringValue(row, col);
//			if ("".equals(stringValue) || stringValue == null) {
//				stringValue = "0.0";
//			}
//			ret = new Double(stringValue);
//			//		} catch (Exception e) {
//			//			// aca el le ponia la string "s/d"
//			//			System.err
//			//			.println("Error al tratar de obtener un dato numerico en la celda (fila,col)= ("
//			//					+ row + "," + col + ") =>" + stringValue);
//			//			e.printStackTrace();
//			//		}
//			return ret;
//		}

//		public Double getDoubleValue(Cell cell){
//			Double ret = new Double(0);
//			String stringValue = null;
//			//	try {
//			stringValue = getStringValue(cell);
//			if ("".equals(stringValue) || stringValue == null) {
//				stringValue = "0.0";
//			}
//			ret = new Double(stringValue);
//			//		} catch (Exception e) {
//			//			// aca el le ponia la string "s/d"
//			//			System.err
//			//			.println("Error al tratar de obtener un dato numerico en la celda "
//			//					+cell+ " =>" + stringValue);
//			//		//	e.printStackTrace();
//			//		}
//			return ret;
//		}




		private void writeDataToSheet(XSSFSheet sheet,
				Map<String, Object[]> data) {
			XSSFWorkbook workbook = sheet.getWorkbook();

			Set<String> keyset = data.keySet();
			int rownum = 0;
			for (String key : keyset) {
				Row row = sheet.createRow(rownum++);
				Object[] objArr = data.get(key);
				int cellnum = 0;
				for (Object obj : objArr) {
					Cell cell = row.createCell(cellnum++);
					if (obj instanceof String)
						cell.setCellValue((String) obj);
					else if (obj instanceof Double)
						cell.setCellValue((Double) obj);
					else if (obj instanceof Calendar){
						Date date = ((Calendar)obj).getTime();


						CellStyle dateCellStyle = workbook.createCellStyle();
						CreationHelper createHelper = workbook.getCreationHelper();
						dateCellStyle.setDataFormat(
								createHelper.createDataFormat().getFormat("dd-mm-yy"));
						cell.setCellStyle(dateCellStyle);
						cell.setCellValue(date);

					}
				}
			}
		}


		public void exportSeries(Series<String, Number> series) {//OK!
			File outFile = getNewExcelFile();


			XSSFWorkbook workbook = new XSSFWorkbook();				
			//				Calendar periodoCalendar = Calendar.getInstance();
			//				int sec = periodoCalendar.get(Calendar.SECOND);
			//				int min = periodoCalendar.get(Calendar.MINUTE);
			//				int hour = periodoCalendar.get(Calendar.HOUR_OF_DAY);
			//				int day = periodoCalendar.get(Calendar.DAY_OF_MONTH);
			//				int mes = periodoCalendar.get(Calendar.MONTH);//, Calendar.SHORT_FORMAT, Locale.getDefault());
			//				int anio = periodoCalendar.get(Calendar.YEAR);//, Calendar.SHORT_FORMAT, Locale.getDefault());
			//
			//				String periodoName = String.valueOf(anio)+"-"+String.valueOf(mes)+"-"+String.valueOf(day)+"-"+String.valueOf(hour)+String.valueOf(min)+String.valueOf(sec);
			//				// Create a blank sheet

			String sheetName = series.getName();
			if(sheetName ==null){
				sheetName="Histograma";
			}
			XSSFSheet sheet = workbook.createSheet(sheetName);

			// This data needs to be written (Object[])
			Map<String, Object[]> data = new TreeMap<String, Object[]>();

			List<Data<String,Number>> datos =series.getData();
			data.put("0", new Object[] {
					"Rango",
					"Superficie",
					"Rinde"

			});


			for(int i =0;i<datos.size();i++){
				Number rinde = new Double(0);
				Number superficie = datos.get(i).getYValue();
				Number produccion = (Number) datos.get(i).getExtraValue();
				if(superficie!=null
						&&produccion!=null 
						&& superficie.doubleValue() > 0 
						&& produccion.doubleValue() > 0){				
					rinde = produccion.doubleValue()/superficie.doubleValue();
				}
				data.put(String.valueOf(i+1),
						new Object[] {
					datos.get(i).getXValue(),
					superficie,
					rinde
				});
			}

			// Iterate over data and write to sheet
			writeDataToSheet( sheet, data);

			try {
				// Write the workbook in file system
				FileOutputStream out = new FileOutputStream(outFile);
				workbook.write(out);
				out.close();
				workbook.close();
				System.out
				.println("el backup del fue guardado con exito.");
			} catch (Exception e) {
				e.printStackTrace();
			}


		}

		public void exportData(String sheetName ,Map<String, Object[]> data) {//OK!
			File outFile = getNewExcelFile();

			XSSFWorkbook workbook = new XSSFWorkbook();							
		//	String sheetName = nombre;
			
			XSSFSheet sheet =null;
			if(sheetName!=null) {
				sheet =  workbook.createSheet(sheetName);	
			}else {
				sheet = workbook.createSheet();
			}
			

			// Iterate over data and write to sheet
			writeDataToSheet( sheet, data);
			
			try {
				// Write the workbook in file system
				FileOutputStream out = new FileOutputStream(outFile);
				workbook.write(out);
				out.close();
				workbook.close();
				System.out
				.println("el backup del fue guardado con exito.");
			} catch (Exception e) {
				e.printStackTrace();
			}


		}

		public void exportOrdenCompra(OrdenCompra oc) {
			File outFile = getNewExcelFile();

			XSSFWorkbook workbook = new XSSFWorkbook();				
			//				Calendar periodoCalendar = Calendar.getInstance();
			//				int sec = periodoCalendar.get(Calendar.SECOND);
			//				int min = periodoCalendar.get(Calendar.MINUTE);
			//				int hour = periodoCalendar.get(Calendar.HOUR_OF_DAY);
			//				int day = periodoCalendar.get(Calendar.DAY_OF_MONTH);
			//				int mes = periodoCalendar.get(Calendar.MONTH);//, Calendar.SHORT_FORMAT, Locale.getDefault());
			//				int anio = periodoCalendar.get(Calendar.YEAR);//, Calendar.SHORT_FORMAT, Locale.getDefault());
			//
			//				String periodoName = String.valueOf(anio)+"-"+String.valueOf(mes)+"-"+String.valueOf(day)+"-"+String.valueOf(hour)+String.valueOf(min)+String.valueOf(sec);
			//				// Create a blank sheet

			String sheetName = oc.getDescription();
			if(sheetName ==null){
				sheetName="OrdenCompra";
			}
			XSSFSheet sheet = workbook.createSheet(sheetName);

			// This data needs to be written (Object[])
			Map<String, Object[]> data = new TreeMap<String, Object[]>();

			List<OrdenCompraItem> datos =oc.getItems();
			data.put("0", new Object[] {
					"Producto",
					"Cantidad",
					"Precio",
					"Importe"
			});

			
			for(int i =0;i<datos.size();i++){
				OrdenCompraItem item = datos.get(i);
				String productoNombre = item.getProducto().getNombre();
				Number cantidad = item.getCantidad();
				Number precio = item.getPrecio();
				Number importe = item.getImporte();
				
				data.put(String.valueOf(i+1),
						new Object[] {
					productoNombre,
					cantidad,
					precio,
					importe
				});
			}
			
			data.put(String.valueOf(datos.size()+1), new Object[] {
					"Total",
					"",
					"",
					oc.getImporteTotal()

			});
			
			// Iterate over data and write to sheet
			writeDataToSheet( sheet, data);

			try {
				// Write the workbook in file system
				FileOutputStream out = new FileOutputStream(outFile);
				workbook.write(out);
				out.close();
				workbook.close();
				System.out
				.println("el backup del fue guardado con exito.");
			} catch (Exception e) {
				e.printStackTrace();
			}

			
		}
	}
