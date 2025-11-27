package com.ursulagis.desktop.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;



public class ReadJDHarvestLog  extends Task<File>{
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	private File fdl=null;
	public ReadJDHarvestLog(File fdl){
		this.fdl=fdl;
	}

	public File call()  {
		String fddFileName = readFdlFile();
		String dir = fdl.getParent();
		String fddPath = dir+File.separator+fddFileName;
		System.out.println("fdd File path = "+fddPath);
		File fddFile = new File(fddPath);

		int fileLength = (int)fddFile.length();
		System.out.println("fileLength ="+fileLength);

		byte[] result = new byte[fileLength];
		//  Map<String,Integer> headers = new HashMap<>();
		try { //lo necesito para InputStream
			InputStream input = null;
			try {
				int totalBytesRead = 0;
				input = new BufferedInputStream(new FileInputStream(fddFile));

				while(totalBytesRead < fileLength){
					int bytesRemaining = result.length - totalBytesRead;
					//input.read() returns -1, 0, or more :
					int bytesRead = input.read(result, totalBytesRead, bytesRemaining); 
					//System.out.print(DatatypeConverter.printHexBinary(result));
					totalBytesRead+=bytesRead;
				}


				//   System.out.println(headers);
				log("Num bytes read: " + totalBytesRead);
			} catch(Exception e){
				e.printStackTrace();
			}
			finally {
				log("Closing input stream.");
				input.close();
			}
		}
		catch (FileNotFoundException ex) {
			log("File not found.");
		}
		catch (IOException ex) {
			log(ex);
		}
		navigateByteArray(result,0);
		return fdl;
	}

	private static void log(Object aThing){
		System.out.println(String.valueOf(aThing));
	}
	
	//Caracteres de control
//	Binario	Decimal	Hex	Abreviatura	Repr	AT	Nombre/Significado
//	0000 0000	0	00	NUL		^@	Car�cter Nulo
//	0000 0001	1	01	SOH		^A	Inicio de Encabezado
//	0000 0010	2	02	STX		^B	Inicio de Texto
//	0000 0011	3	03	ETX		^C	Fin de Texto
//	0000 0100	4	04	EOT		^D	Fin de Transmisi�n
//	0000 0101	5	05	ENQ		^E	Consulta
//	0000 0110	6	06	ACK		^F	Acuse de recibo
//	0000 0111	7	07	BEL		^G	Timbre
//	0000 1000	8	08	BS		^H	Retroceso
//	0000 1001	9	09	HT		^I	Tabulaci�n horizontal
//	0000 1010	10	0A	LF		^J	Salto de l�nea
//	0000 1011	11	0B	VT		^K	Tabulaci�n Vertical
//	0000 1100	12	0C	FF		^L	Avance de p�gina
//	0000 1101	13	0D	CR		^M	Retorno de carro
//	0000 1110	14	0E	SO		^N	Desactivar may�sculas
//	0000 1111	15	0F	SI		^O	Activar may�sculas
//	0001 0000	16	10	DLE		^P	Escape v�nculo de datos
//	0001 0001	17	11	DC1		^Q	Control de dispositivo 1 (XON)
//	0001 0010	18	12	DC2		^R	Control de dispositivo 2
//	0001 0011	19	13	DC3		^S	Control de dispositivo 3 (XOFF)
//	0001 0100	20	14	DC4		^T	Control de dispositivo 4
//	0001 0101	21	15	NAK		^U	Acuse de recibo negativo
//	0001 0110	22	16	SYN		^V	S�ncron�a en espera
//	0001 0111	23	17	ETB		^W	Fin del bloque de transmisi�n
//	0001 1000	24	18	CAN		^X	Cancelar
//	0001 1001	25	19	EM		^Y	Fin del medio
//	0001 1010	26	1A	SUB		^Z	Substituci�n
//	0001 1011	27	1B	ESC		^[ o ESC	Escape
//	0001 1100	28	1C	FS		^\	Separador de archivo
//	0001 1101	29	1D	GS		^]	Separador de grupo
//	0001 1110	30	1E	RS		^^	Separador de registro
//	0001 1111	31	1F	US		^_	Separador de unidad
//	0111 1111	127	7F	DEL		^? o DEL	Suprimir
			

	//metodo que divide el array segun los caracteres de control
	//10 00 en Little Endian es Inicio Encabezado
	//02 00 en Little Endian es Inicio de texto
	private void navigateByteArray(byte[] result, int cursor){
		System.out.println();
		
		if(result.length<4){
			System.out.println("parcial < 4: {\n"+new String(result)+"\n}");
			return;
		}
		//	while(cursor<result.length && cursor<50){
		System.out.println("cursor="+cursor);
		
		int headerLength = 0;// Short.toUnsignedInt(bb.getShort(0));
		while(headerLength<2 || headerLength==256 ){
			//todo leer el siguiente short
			ByteBuffer bb2 = ByteBuffer.allocate(2);
			bb2.order(ByteOrder.LITTLE_ENDIAN);
			bb2.put(result[cursor]);
			bb2.put(result[cursor+1]);
			headerLength =  Short.toUnsignedInt(bb2.getShort(0));
			cursor+=2;
			System.out.println("cursor="+cursor);
		}
		if(headerLength>256){
			System.out.println("parcial > 255: {\n"+new String(Arrays.copyOfRange(result, cursor, result.length))+"\n}");
			return;
		}
		int begin = cursor;//0+2=2
		int end = begin + headerLength+1;//2+1=3
		cursor=end;//3+1=4
		System.out.println("begin="+begin+" headerLength="+headerLength+" end="+end+" cursor+1="+cursor);
		try{
			byte[] headerB = Arrays.copyOfRange(result, begin, end);//2,3
			byte[] headerB2 = Arrays.copyOfRange(result, end, result.length);
			//String header = new String(headerB);
			//System.out.println("parcial1: "+header);
			//System.out.println("parcial2: "+new String(headerB2));
			navigateByteArray(headerB,0);
			navigateByteArray(headerB2,0);

		}catch(Exception e){e.printStackTrace();}

		//}

	}



	private String readFdlFile() {
		try {
			//File fXmlFile = new File("/Users/mkyong/staff.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fdl);

			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			/**
				 <RCDLogFile>
				 	<RCDLogBlock>
				 		<ConfigBlock>
							 <LogDataBlock logDataProcessor="EvenByteDelta">
								<EvenByteDelta filePathName="50b2706b-0000-1000-7fdb-e1e1e114c450.fdd"
									xmlns="urn:schemas-johndeere-com:LogDataBlock:EvenByteDelta"
									filePosition="0" />
							</LogDataBlock>
			 */

			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());//Root element :RCDLogFile

			NodeList files = doc.getElementsByTagName("EvenByteDelta");
			Element fileElement = (Element) files.item(0);
			String fddFileName = fileElement.getAttribute("filePathName");
			System.out.println("filePathName= : " + fddFileName);//ok!!


			NodeList clients = doc.getElementsByTagName("rcdsetup:Client");
			Element element = (Element) clients.item(0);
			if(element!=null)
				System.out.println("cliente= : " + element.getAttribute("name"));//ok!!
			element=null;	
			NodeList operators = doc.getElementsByTagName("rcdsetup:Operator");
			element = (Element) operators.item(0);
			if(element!=null)System.out.println("operador= : " + element.getAttribute("name"));//ok!!
			element=null;	
			NodeList farms = doc.getElementsByTagName("rcdsetup:Farm");
			element = (Element) farms.item(0);
			if(element!=null)System.out.println("campo= : " + element.getAttribute("name"));//ok!!
			element=null;	

			NodeList fields = doc.getElementsByTagName("rcdsetup:Field");
			element = (Element) fields.item(0);
			if(element!=null)System.out.println("lote= : " + element.getAttribute("name"));//ok!!
			element=null;	
			NodeList crops = doc.getElementsByTagName("rcdsetup:Crop");
			element = (Element) crops.item(0);
			if(element!=null)System.out.println("cultivo= : " + element.getAttribute("name"));//ok!!
			element=null;	
			System.out.println("");
			return fddFileName;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("JDHarvest");
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
