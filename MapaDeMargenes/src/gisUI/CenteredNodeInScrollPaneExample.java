package gisUI;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
 
/** 
 * Example of centering a various kinds of nodes in a scrollpane 
 * applying effects and transforms to them and
 * reporting changes in their various bounds types.
 */
public class CenteredNodeInScrollPaneExample extends Application  {
  // define some controls.
  final ToggleButton stroke     = new ToggleButton("Add Border");
  final ToggleButton effect     = new ToggleButton("Add Effect");
  final ToggleButton translate  = new ToggleButton("Translate");
  final ToggleButton rotate     = new ToggleButton("Rotate");
  final ToggleButton scale      = new ToggleButton("Scale");
  final ToggleGroup  alignmentToggle = new ToggleGroup();
  final RadioButton  center     = new RadioButton("Center");
  final RadioButton  topLeft    = new RadioButton("Top Left");
  final ToggleGroup  nodeToggle = new ToggleGroup();
  final RadioButton  showSquare = new RadioButton("Square");
  final RadioButton  showGraph  = new RadioButton("Graph");
 
  // the node to be manipulated.
  final ScrollPane scrollPane = new ScrollPane();
  final StackPane nodeContainer = new StackPane();
  Node node = createSquare();
  
  private void reset() {
    stroke.setSelected(false);
    effect.setSelected(false);
    translate.setSelected(false);
    rotate.setSelected(false);
    scale.setSelected(false);
    center.setSelected(true);
  }
 
  public static void main(String[] args) { launch(args); }
  @Override public void start(final Stage stage) throws Exception {
    // show the effect of a stroke.
    stroke.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (node instanceof Region) {
          if (stroke.isSelected()) {
            node.setStyle("-fx-border-color: firebrick; -fx-border-width: 10; -fx-border-style: solid outside line-join miter;");
          } else {
            node.setStyle("-fx-border-width: 10;");
          }
        } else {
          if (stroke.isSelected()) {
            node.setStyle("-fx-stroke: firebrick; -fx-stroke-width: 10; -fx-stroke-type: outside;");
          } else {
            node.setStyle("-fx-stroke-width: 0;");
          }
        }
        reportBounds(node);
      }
    });
 
    // show the effect of an effect.
    effect.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (effect.isSelected()) {
          node.setEffect(new DropShadow());
        } else {
          node.setEffect(null);
        }
        reportBounds(node);
      }
    });
 
    // show the effect of a translation.
    translate.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (translate.isSelected()) {
          node.setTranslateX(100); node.setTranslateY(60);
        } else {
          node.setTranslateX(0); node.setTranslateY(0);
        }
        reportBounds(node);
      }
    });
 
    // show the effect of a rotation.
    rotate.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (rotate.isSelected()) {
          node.setRotate(45);
        } else {
          node.setRotate(0);
        }
        reportBounds(node);
      }
    });
 
    // show the effect of a scale.
    scale.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (scale.isSelected()) {
          node.setScaleX(3); node.setScaleY(3);
        } else {
          node.setScaleX(1); node.setScaleY(1);
        }
        Platform.runLater(new Runnable() {
          @Override public void run() {
            nodeContainer.setPrefSize(
              Math.max(nodeContainer.getBoundsInParent().getMaxX(), scrollPane.getViewportBounds().getWidth()),
              Math.max(nodeContainer.getBoundsInParent().getMaxY(), scrollPane.getViewportBounds().getHeight())
            );
          }
        });
        
        reportBounds(node);
      }
    });
 
    // align the node.
    topLeft.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (topLeft.isSelected()) {
          StackPane.setAlignment(node, Pos.TOP_LEFT);
        }
      }
    });
    topLeft.setToggleGroup(alignmentToggle);
    center.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (center.isSelected()) {
          StackPane.setAlignment(node, Pos.CENTER);
        }
      }
    });
    center.setToggleGroup(alignmentToggle);
    center.fire();
 
    // choose which node to show.
    showSquare.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (showSquare.isSelected()) {
          reset();
          setNode(createSquare());
          stroke.setDisable(false);
        }
      }
    });
    showSquare.setToggleGroup(nodeToggle);
    showGraph.setOnAction(new EventHandler<ActionEvent>() {
      @Override public void handle(ActionEvent actionEvent) {
        if (showGraph.isSelected()) {
          reset();
          setNode(createGraph());
          stroke.setDisable(true);
        }
      }
    });
    showGraph.setToggleGroup(nodeToggle);
    showSquare.fire();
 
    // layout the scene.
    nodeContainer.setStyle("-fx-background-color: cornsilk;");
    nodeContainer.getChildren().add(node);
//    nodeContainer.setManaged(false);
 
    // add a scrollpane and size it's content to fit the pane (if it can).
    scrollPane.setContent(nodeContainer);
    scrollPane.viewportBoundsProperty().addListener(
      new ChangeListener<Bounds>() {
      @Override public void changed(ObservableValue<? extends Bounds> observableValue, Bounds oldBounds, Bounds newBounds) {
        nodeContainer.setPrefSize(
          Math.max(node.getBoundsInParent().getMaxX(), newBounds.getWidth()),
          Math.max(node.getBoundsInParent().getMaxY(), newBounds.getHeight())
        );
      }
    });
 
    // layout the scene.
    VBox controlPane = new VBox(10);
    controlPane.setStyle("-fx-background-color: linear-gradient(to bottom, gainsboro, silver); -fx-padding: 10;");
    controlPane.getChildren().addAll(
      HBoxBuilder.create().spacing(10).children(stroke, effect).build(),
      HBoxBuilder.create().spacing(10).fillHeight(false).children(translate, rotate, scale).build(),
      HBoxBuilder.create().spacing(10).fillHeight(false).children(center, topLeft).build(),
      HBoxBuilder.create().spacing(10).fillHeight(false).children(showSquare, showGraph).build()
    );
 
    VBox layout = new VBox();
    VBox.setVgrow(scrollPane, Priority.ALWAYS);
    layout.getChildren().addAll(scrollPane, controlPane);
    
    setNode(createSquare());
 
    // show the scene.
    final Scene scene = new Scene(layout, 300, 300);
    stage.setScene(scene);
    stage.show();
 
    reportBounds(node);
  }
 
  private void setNode(Node newNode) {
    Pane parent = this.node != null ? (Pane) this.node.getParent() : null;
    if (parent != null) {
      parent.getChildren().remove(this.node);
      parent.getChildren().add(newNode);
    }
    this.node = newNode;
    reset();
    
    node.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {
      @Override public void changed(ObservableValue<? extends Bounds> observableValue, Bounds oldBounds, Bounds newBounds) {
        nodeContainer.setPrefSize(
          Math.max(newBounds.getMaxX(), scrollPane.getViewportBounds().getWidth()),
          Math.max(newBounds.getMaxY(), scrollPane.getViewportBounds().getHeight())
        );
      }
    });
  }
 
  private Node createGraph() { // create a graph node to be acted on by the controls.
    // setup graph
    NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("X Axis");
    NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("Y Axis");
    LineChart<Number,Number> graph = new LineChart<Number,Number>(xAxis,yAxis);
    graph.setTitle("Test Graph");
    graph.setCreateSymbols(false);
 
    // add starting data
    XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();
    series.setName("Squares");
    for (int x = 0; x <= 10; x++) series.getData().add(new XYChart.Data<Number,Number>(x, x * x));
    graph.getData().add(series);
 
    graph.setMinSize(300, 260);
    graph.setMaxSize(300, 260);
 
    return graph;
  }
 
  private Rectangle createSquare() { // create a square to be acted on by the controls.
    final Rectangle square = new Rectangle(0, 0, 100, 100);
    square.setStyle("-fx-fill: linear-gradient(to right, darkgreen, forestgreen)");
    return square;
  }
 
  /** output the squares bounds. */
  private void reportBounds(final Node n) {
    StringBuilder description = new StringBuilder();
    if (stroke.isSelected())       description.append("Stroke 10 : ");
    if (effect.isSelected())       description.append("Dropshadow Effect : ");
    if (translate.isSelected())    description.append("Translated : ");
    if (rotate.isSelected())       description.append("Rotated 45 degrees : ");
    if (scale.isSelected())        description.append("Scale 3 : ");
    if (description.length() == 0) description.append("Unchanged : ");
 
    System.out.println(description.toString());
    System.out.println("Layout Bounds:    " + n.getLayoutBounds());
    System.out.println("Bounds In Local:  " + n.getBoundsInLocal());
    System.out.println("Bounds In Parent: " + n.getBoundsInParent());
    System.out.println();
  }
}