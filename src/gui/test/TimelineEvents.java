package gui.test;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.Lighting;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class TimelineEvents extends Application {

	//main timeline
	private Timeline timeline;
	private AnimationTimer timer;

	//variable for storing actual frame
	private Integer i=0;

	@Override public void start(Stage stage) {




		//create a circle with effect
		final Circle circle = new Circle(20,  Color.rgb(156,216,255));
		circle.setEffect(new Lighting());
		//create a text inside a circle
		final Text text = new Text (i.toString());
		text.setStroke(Color.BLACK);
		//create a layout for circle with text inside
		final StackPane stack = new StackPane();
		stack.getChildren().addAll(circle, text);
		stack.setLayoutX(30);
		stack.setLayoutY(30);

		Group p = new Group();
		p.setTranslateX(80);
		p.setTranslateY(80);
		p.getChildren().add(stack);
		Scene scene = new Scene(p);
		stage.setScene(scene);
		stage.setWidth(500);
		stage.setHeight(500);
		stage.show();

		//create a timeline for moving the circle
		timeline = new Timeline();
		timeline.setCycleCount(0);
		timeline.setAutoReverse(false);
	

		//You can add a specific action when each frame is started.
		timer = new AnimationTimer() {
			@Override
			public void handle(long l) {
				text.setText(i.toString());
				i++;
			}
		};

		
	

		//add the keyframe to the timeline
//		timeline.getKeyFrames().add(new KeyFrame(Duration.millis(0),
//				new KeyValue(stack.scaleXProperty(), 2),
//				new KeyValue(stack.scaleYProperty(), 2) ));
//		timeline.getKeyFrames().add(new KeyFrame(Duration.millis(2000),
//				new KeyValue(stack.scaleXProperty(), 1),
//				new KeyValue(stack.scaleYProperty(), 1) ));
		long init = System.currentTimeMillis();
		timeline.getKeyFrames().add(new KeyFrame(Duration.millis(0),(t)->{ 
			long exTime = System.currentTimeMillis()-init;
			System.out.println("se deberia ejecutar en 0ms "+exTime);
			}));
		Duration time = Duration.millis(2000);
		timeline.getKeyFrames().add(new KeyFrame(time,(t)->{ 
			long exTime = System.currentTimeMillis()-init;
			System.out.println("se deberia ejecutar en 7000ms "+exTime);
			}));
		time =time.add(Duration.millis(3000));
		timeline.getKeyFrames().add(new KeyFrame(time,(t)->{ 
			long exTime = System.currentTimeMillis()-init;
			System.out.println("se deberia ejecutar en 10000ms "+exTime);
			}));
		System.out.println(time+"antes");
		time =time.add(Duration.millis(5000));
		System.out.println(time+" despues");
		timeline.getKeyFrames().add(new KeyFrame(time,(t)->{ 
			long exTime = System.currentTimeMillis()-init;
			System.out.println("se deberia ejecutar en "+5000+"ms "+exTime);
			}));
		timeline.getKeyFrames().add(new KeyFrame(Duration.millis(1),(t)->{ 
			long exTime = System.currentTimeMillis()-init;
			System.out.println("se deberia ejecutar en 1ms "+exTime);
			}));
		timeline.play();
		//timer.start();
	}


	public static void main(String[] args) {
		Application.launch(args);
	}
} 