package com.ursulagis.desktop.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
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
import java.util.stream.Collectors;

import javafx.collections.ObservableList;
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

import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Agroquimico;
import com.ursulagis.desktop.dao.config.Configuracion;
import com.ursulagis.desktop.dao.ordenCompra.OrdenCompra;
import com.ursulagis.desktop.dao.ordenCompra.OrdenCompraItem;
import com.ursulagis.desktop.dao.pulverizacion.PulverizacionLabor;
import com.ursulagis.desktop.gui.JFXMain;
import com.ursulagis.desktop.gui.Messages;
import com.ursulagis.desktop.gui.utils.DateConverter;



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
		

		File file = fileChooser.showSaveDialog(JFXMain.stage);
		if(file != null) {
		 config.setProperty(Configuracion.LAST_FILE,file.getParent());
		 config.save();
		}
		System.out.println("archivo seleccionado para guardar "+file);

		return file;
	}
	
	public void readAgroquimicosFile() {
		try {
			InputStream excelStream = PulverizacionLabor.class.getClassLoader()
					.getResourceAsStream("./dao/pulverizacion/agroquimicos.xlsx");
			if(excelStream==null) System.out.println("stream es null");
//			FileInputStream file = new FileInputStream(new File(
//					"/dao/pulverizacion/agroquimicos.xlsx"));

			// Create Workbook instance holding reference to .xlsx file
			XSSFWorkbook workbook = new XSSFWorkbook(excelStream);
			// workbook.getSheet("nombreDeLaHoja");

			// Get first/desired sheet from the workbook
			XSSFSheet sheet = workbook.getSheetAt(0);

			// Iterate through each rows one by one
			Iterator<Row> rowIterator = sheet.iterator();
//			Row row5 = sheet.getRow(5);
//			row5.getCell(0);

			for (int i =0;rowIterator.hasNext();i++) {
				Row row = rowIterator.next();
				if(i==0)continue;//salteo las cabederas
				
				// For each row, iterate through all the columns
				Iterator<Cell> cellIterator = row.cellIterator();
				Agroquimico a = new Agroquimico();				
				
				a.setUnidadDosis("lts");
				a.setUnidadStock("lts");
				//0) N� registro 
											
				a.setNumRegistro(getStringValueFromCell(row.getCell(0)));
				
				//1) Marca
				
				a.setNombre(getStringValueFromCell(row.getCell(1)));
				//2) Empresa
				
				a.setEmpresa(getStringValueFromCell(row.getCell(2)));
				//3) Activos
				
				a.setActivos(getStringValueFromCell(row.getCell(3)));
				//4) Banda tox				
				a.setBandaToxicologica(getStringValueFromCell(row.getCell(4)));
//				System.out.println("reg:"+a.getNumRegistro()+" "
//						+"nombre:"+a.getNombre()+" "
//						+"empresa:"+a.getEmpresa()+" "
//						+"activos:"+a.getActivos()+" "
//						+"banda:"+a.getBandaToxicologica()+" ");
				
				Agroquimico r = DAH.findAgroquimico(a.getNumRegistro());
				if(r==null) {
					DAH.save(a);
					//System.out.println("no existe, lo guardo");
				} else {
					//System.out.println("existe, no lo guardo");
					continue;
				}
		
//				while (cellIterator.hasNext()) {
//					Cell cell = cellIterator.next();
//					// Check the cell type and format accordingly
//					switch (cell.getCellType()) {
//					case Cell.CELL_TYPE_NUMERIC:
//						System.out.print(cell.getNumericCellValue() + "num");
//						break;
//					case Cell.CELL_TYPE_STRING:
//						System.out.print(cell.getStringCellValue() + "string");
//						break;
//					}
//				}
//				System.out.println("");
			}
			excelStream.close();
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getStringValueFromCell(Cell cell) {
		if(cell==null)return "";
		switch (cell.getCellType()) {
		case Cell.CELL_TYPE_NUMERIC:
			double reg = cell.getNumericCellValue();
			return Messages.getNumberFormat().format(reg);//39.936,00 deberia ser 39936
			//System.out.print(cell.getNumericCellValue() + "num");
			//break;
		case Cell.CELL_TYPE_STRING:
			return cell.getStringCellValue();
//			System.out.print(cell.getStringCellValue() + "string");
//			break;
		default:
			return "";
		}
	}

	public static void main(String[] args) {
		ExcelHelper h = new ExcelHelper();
		h.readAgroquimicosFile();
	}

	public void readExcelFile() {

		try {
			FileInputStream file = new FileInputStream(new File(
					"myFile.xlsx"));

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
//			Double ret = 0.0;
//			String stringValue = null;
//			//	try {
//			stringValue = getStringValue(row, col);
//			if ("".equals(stringValue) || stringValue == null) {
//				stringValue = "0.0";
//			}
//			ret = Double.parseDouble(stringValue);
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
//			Double ret = 0.0;
//			String stringValue = null;
//			//	try {
//			stringValue = getStringValue(cell);
//			if ("".equals(stringValue) || stringValue == null) {
//				stringValue = "0.0";
//			}
//			ret = Double.parseDouble(stringValue);
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
					if (obj instanceof String) {
						cell.setCellValue((String) obj);
					} else if (obj instanceof Double) {
						cell.setCellValue((Double) obj);					
					} else if (obj instanceof Calendar){
						Date date = ((Calendar)obj).getTime();

						CellStyle dateCellStyle = workbook.createCellStyle();
						CreationHelper createHelper = workbook.getCreationHelper();
						dateCellStyle.setDataFormat(
								createHelper.createDataFormat().getFormat("dd/mm/yy"));
						cell.setCellStyle(dateCellStyle);
						cell.setCellValue(date);
					} else if(obj instanceof LocalDate) {
						Date date = DateConverter.asDate((LocalDate)obj);
						CellStyle dateCellStyle = workbook.createCellStyle();
						CreationHelper createHelper = workbook.getCreationHelper();
						dateCellStyle.setDataFormat(
								createHelper.createDataFormat().getFormat("dd/MM/yy"));
						cell.setCellStyle(dateCellStyle);
						cell.setCellValue(date);
					}
				}
			}
		}

		public void exportSeriesList(ObservableList<Series<Number, Number>> observableList) {//OK!
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

			Series<Number, Number> s1= observableList.get(0);
			String sheetName = s1.getName();
			if(sheetName ==null){
				sheetName="Data";
			}
			String xName = s1.getChart().getXAxis().getLabel();
			String YName = s1.getChart().getYAxis().getLabel();
			XSSFSheet sheet = workbook.createSheet(sheetName);

			// This data needs to be written (Object[])
			Map<String, Object[]> data = new TreeMap<String, Object[]>();
			
			data.put("0", new Object[] {"",	YName, ""});
			List<String> labels = new ArrayList<String>();
			labels.add(xName);
			labels.addAll(observableList.stream().map(s->s.getName()).collect(Collectors.toList()));
			data.put("1",labels.toArray());		
			
			for(int j=0;j<observableList.size();j++) {
				Series<Number, Number> s=observableList.get(j);
				List<Data<Number,Number>> datos =s.getData();			

				for(int i =1;i<datos.size();i++){
					String sFecha = LocalDate.ofEpochDay(datos.get(i).getXValue().longValue()).toString();
					//Number rinde = 0.0;
					Number yVal = datos.get(i).getYValue();
				
					Object[] d = data.get(sFecha);
					if(d==null) {
						d = new Object[observableList.size()+1];					
						
						d[0]=sFecha;					
					}else {
						data.remove(sFecha);
					}
						d[j+1]=yVal;
						data.put(sFecha,d);
						//data.m
					
				}
			}

			// Iterate over data and write to sheet
			writeDataToSheet( sheet, data);

			try {
				// Write the workbook in file system
				FileOutputStream out = new FileOutputStream(outFile);
				workbook.write(out);
				out.close();
				workbook.close();
				System.out.println("el archivo excel fue guardado con exito.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void exportNumberSeries(Series<Number, Number> series) {//OK!
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
				sheetName="Data";
			}
			XSSFSheet sheet = workbook.createSheet(sheetName);

			// This data needs to be written (Object[])
			Map<String, Object[]> data = new TreeMap<String, Object[]>();

			List<Data<Number,Number>> datos =series.getData();
			String xName = series.getChart().getXAxis().getLabel();
			String YName = series.getChart().getYAxis().getLabel();
			data.put("0", new Object[] {
					xName,
					YName
			});
			
			for(int i = 0;i<datos.size();i++){
				Number xVal  = datos.get(i).getXValue();				
				Number yVal = datos.get(i).getYValue();
					Object[] d = new Object[2];	
					d[0]=xVal;
					d[1]=yVal;
					data.put(String.valueOf(i+1),d);			
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
					"Cantidad"
			});


			for(int i =0;i<datos.size();i++){
				Number rinde = 0.0;
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

		public void exportSeriesNdvi(Series<String, Number> series) {//OK!
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
					"Ndvi"
			});


			for(int i =0;i<datos.size();i++){
				Number rinde = 0.0;
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
				System.out.println("el backup del fue guardado con exito.");
			} catch (Exception e) {
				e.printStackTrace();
			}

			
		}
	}
