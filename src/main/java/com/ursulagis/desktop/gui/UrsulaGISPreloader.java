package com.ursulagis.desktop.gui;

import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.application.Preloader.StateChangeNotification.Type;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import com.ursulagis.desktop.tasks.UpdateTask;

/**
 * Preloader for UrsulaGIS Desktop application
 * Shows a branded loading screen while the main application initializes
 */
public class UrsulaGISPreloader extends Preloader {
    
    private static final String ICON_PATH = "U_nueva_3_256x256_verde.png";
    //private static final String FALLBACK_ICON_PATH = "gui/U_nueva_256x256_verde.png";
    
    private Stage preloaderStage;
    private ProgressBar progressBar;
    //private Label statusLabel;
    //private Label versionLabel;
    String message = "";
    @Override
    public void init() throws Exception {
        message = UpdateTask.checkForUpdate();
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;
        
        // Create the preloader UI
       // VBox root = createPreloaderUI();
       VBox webView = createPreloaderWebView();

       double height = webView.getPrefHeight();
        double width = webView.getPrefWidth();
        progressBar=new ProgressBar();
        progressBar.setProgress(0);
        progressBar.setPrefWidth(300);
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(webView);
       // borderPane.setBottom(progressBar);
       
        Scene scene = new Scene(borderPane, width-500,height-100);
        
        // Configure stage
        primaryStage.setScene(scene);

		primaryStage.setTitle(Messages.getString("UpdateTaskWelcome.Title"));//"Bienvenido!");
        //primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        
        // Set application icon
        setApplicationIcon(primaryStage);
        
        primaryStage.setOnHiding((e)->{
            System.out.println("preloaderStage onHiding");
        });
        // Show the preloader
        primaryStage.show();
    }
    
    private VBox createPreloaderWebView() {       
        WebView webView = new WebView();		
		webView.autosize();
		WebEngine engine = webView.getEngine();
		engine.loadContent(message);

		VBox v = new VBox(20);
		VBox.setVgrow(webView, Priority.ALWAYS);
		VBox.setMargin(webView, new Insets(10,10,10,10));
		v.getChildren().add(webView);
        return v;
    }
    
    
    private void setApplicationIcon(Stage stage) {
        try {
            Image icon = new Image(getClass().getResourceAsStream(ICON_PATH));
            stage.getIcons().add(icon);
        } catch (Exception e) {
          e.printStackTrace();
        }
    }

    
    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification) {
            ProgressNotification progressInfo = (ProgressNotification) info;
            double progress = progressInfo.getProgress();
            System.out.println("handleApplicationNotification progress: " + progress);

            if(progress>0.8){
                Stage stage = JFXMain.stage;
              
                if(stage!=null ){
                    System.out.println("showing stage");   
                    stage.show();	
                    //&& stage.isShowing()
                    this.preloaderStage.toFront();
                };
            }

         //   Platform.runLater(() -> {
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                    System.out.println("Progress bar updated to: " + progress);
                }
           // });

        } else if (info instanceof StateChangeNotification) {
            StateChangeNotification stateInfo = (StateChangeNotification) info;
            if (stateInfo.getType() == Type.BEFORE_INIT) {
              System.out.println("BEFORE_INIT");
            } else if (stateInfo.getType() == Type.BEFORE_LOAD) {
                System.out.println("BEFORE_LOAD");
            } else if (stateInfo.getType() == Type.BEFORE_START) {
                System.out.println("BEFORE_START");
            }
        }
    }

}
