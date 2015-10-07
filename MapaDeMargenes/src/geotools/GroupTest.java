package geotools;
import javafx.application.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.stage.*;

import java.util.*;


public class GroupTest extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {
        Pane pane = new Pane();

        Group root = new Group();
        // NOTE: removing these two setScale* lines stops the undesirable behavior
//        root.setScaleX(.2); 
//        root.setScaleY(.2);
//        root.setTranslateX(100);
//        root.setTranslateY(100);

        root.layoutXProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                System.out.println("root layout: " + root.getLayoutX() + ", " + root.getLayoutY());
            }
        });

        root.getChildren().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> change) {
                System.out.println("root: " + root.getBoundsInParent());
                System.out.println("root: " + root.getBoundsInLocal());
                System.out.println("root: " + root.getLayoutBounds());
                System.out.println("root: " + root.getLayoutX() + ", " + root.getLayoutY());
            }
        });

        pane.getChildren().add(root);
        Scene scene = new Scene(pane, 500, 500);
        stage.setScene(scene);
        stage.show();
        
//        root.getChildren().add(new Circle(100,100,100));
//        root.getChildren().add(new Circle(220,100,100));
    

        new Thread(() -> {
            Random r = new Random();
            try {
            	   for(int i=0;i<500/20;i++){
                   // expand = expand * 1.1;
                	
                    Thread.sleep(700);
                    Platform.runLater(() -> {                     
                       	 root.getChildren().add(new Circle(20*expand-10,10,10));
                       	 root.getChildren().add(new Circle(10,20*expand-10,10));
                       	root.getChildren().add(new Circle(250-10,250-10,10));
                      
                        //root.getChildren().add(new Circle(r.nextInt((int)(1000*expand)) - 500*expand, r.nextInt((int)(1000*expand)) - 500*expand, r.nextInt(50)+30));
                    });
                    expand++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    static double expand = 0.0;
}