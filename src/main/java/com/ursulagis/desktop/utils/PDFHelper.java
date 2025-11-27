package com.ursulagis.desktop.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.ursulagis.desktop.gui.JFXMain;
import javafx.application.Application;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/*
 * <dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.4</version>
</dependency>
 */
/**
 * 
 * @author quero
 *
 */
public class PDFHelper {
	public static void main(String[] args) {
		//testInsertText();
		
		//testInsertImage();
		
		Application.launch(PDFHelperAPP.class, args);

	}
	
	public static void testInsertText() {
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		document.addPage(page);

		PDPageContentStream contentStream;
		try {
			contentStream = new PDPageContentStream(document, page);

			//contentStream.setFont(PDType1Font.COURIER, 12);
			contentStream.beginText();
			contentStream.showText("Hola Ursula");
			contentStream.endText();
			contentStream.close();

			document.save("pdfBoxHelloWorld.pdf");
			document.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	public static void testInsertImage() {
		PDDocument document2 = new PDDocument();
		PDPage page2 = new PDPage();
		document2.addPage(page2);

		Path path2;
		try {
			path2 = Paths.get(ClassLoader.getSystemResource("./gui/ursula_logo_2020.png").toURI());

			PDPageContentStream contentStream2 = new PDPageContentStream(document2, page2);
			PDImageXObject image = PDImageXObject.createFromFile(
					path2.toAbsolutePath().toString(),
					document2);
			contentStream2.drawImage(image, 0, 0);
			contentStream2.close();

			document2.save("pdfBoxImage.pdf");
			document2.close();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void printToPDF(Node yourNode,Stage stage) {
		PrinterJob job = PrinterJob.createPrinterJob();
		 if(job != null){
		
		
		   job.showPrintDialog(stage); // Window must be your main Stage
		   job.printPage(yourNode);
		   job.endJob();
		 }
	}
	class PDFHelperAPP extends Application{
		public PDFHelperAPP() {
			super();
		}
		@Override
		public void start(Stage primaryStage) throws Exception {
			 Node node = new Circle(100, 200, 200);
			 PDFHelper.printToPDF(node,primaryStage);
		}
		
	}
}
