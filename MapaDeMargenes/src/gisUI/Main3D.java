package gisUI;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.AmbientLight;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;

import java.util.stream.*;

public class Main3D extends Application {
	private static final double DIVIDER_POSITION = 0.9;

	private static final String TITLE_VERSION = "Margin Map Viewer 3D Ver: 0.0.0)";
	private static final String ICON = "gisUI/1-512.png";

	final static float minX = -10;
	final static float minY = -10;

	final static float maxX = 10;
	final static float maxY = 10;


	//private Stage stage;
	  private final Translate translate = new Translate(0, 0, -100);
	   // private final Translate translateZ = new Translate(0, 0, -100);
	    private final Rotate rotateX = new Rotate(-120, Rotate.X_AXIS);//new Rotate(-120, 0, 0, 0, Rotate.X_AXIS);
	    private final Rotate rotateY =new Rotate(180, Rotate.Y_AXIS);//new Rotate(180, 0, 0, 0, Rotate.Y_AXIS);
	    private final Translate translateY = new Translate(0, 0, 0);

	
		


	@Override
	public void start(Stage primaryStage) throws Exception {
		//	this.stage = primaryStage;
		primaryStage.setTitle(TITLE_VERSION);
		primaryStage.getIcons().add(new Image(ICON));

		// primaryStage.setMaximized(true);

		Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();
		Scene scene = new Scene(createContent2(), bounds.getWidth() * 9 / 10,
				bounds.getHeight() * 9 / 10);// , 1500, 800);//, Color.White);
		// primaryStage.setMaxHeight(bounds.getHeight()*2/3);
		// primaryStage.setMaxWidth(bounds.getWidth()*2/3);


		primaryStage.setScene(scene);
		primaryStage.show();


	}

	public VBox createContent2() {

		final PhongMaterial redMaterial = new PhongMaterial();
		redMaterial.setSpecularColor(Color.ORANGE);
		redMaterial.setDiffuseColor(Color.RED);

		final PhongMaterial blueMaterial = new PhongMaterial();
		blueMaterial.setDiffuseColor(Color.BLUE);
		blueMaterial.setSpecularColor(Color.LIGHTBLUE);

		final PhongMaterial greenMaterial = new PhongMaterial();
	//	greenMaterial.setDiffuseColor(Color.DARKGREEN);
	//	greenMaterial.setSpecularColor(Color.GREEN);
		greenMaterial.setDiffuseMap(new Image(getClass().getResourceAsStream("colors.png")));

		double width = 5.0;
		double height = 5.0;
		double depth = 5.0;
		Box myBox = new Box(width, height, depth);
		myBox.setMaterial(redMaterial);

		Box xyBox = new Box(1, 1, 20);
		xyBox.setMaterial(redMaterial);
		xyBox.setTranslateZ(10);

		Box xzBox = new Box(1, 20, 1);
		xzBox.setMaterial(greenMaterial);
		xzBox.setTranslateY(10);

		Box zyBox = new Box(20, 1, 1);
		zyBox.setMaterial(blueMaterial);
		zyBox.setTranslateX(10);
		//		blueBox.getTransforms().addAll (
		//	                new Rotate(-20, Rotate.Y_AXIS),
		//	                new Rotate(-20, Rotate.X_AXIS),
		//	                new Translate(-2.5/2, 2.5/2, 0));




		Sphere sol = new Sphere(5);
		sol.setMaterial(redMaterial);
		double radius = 2.0;
		Cylinder myCylinder = new Cylinder(radius, height);
		myCylinder.setMaterial(new PhongMaterial(Color.GREY));
		myCylinder.setRotationAxis(new Point3D(1,0,0));
		myCylinder.setRotate(90);

		TriangleMesh mesh;// = new TriangleMesh();

		// Define the set of points, which are the vertices of the mesh.
		//							//{x1,y1,z1,x2,y2,z2...,xn,yn,zn}
		//		float points[] = { 2, 3, 5 };
		//		mesh.getPoints().addAll(points);
		//		//{dx0,dy0,dx1,dy1,...dxn,dyn}//valores entre 0 y 1
		//		float textureCoords[] = { 2, 3, 5 };
		//		mesh.getTexCoords().addAll(textureCoords);
		//		// Using the vertices, build the Faces, which are triangles that
		//		// describe the topology.
		//		int faces[] = { 0, 1, 2 };
		//		mesh.getFaces().addAll(faces);
		//
		//		// Define the smoothing group to which each face belongs.
		//		int smoothingGroups[] = { 0 };
		//		mesh.getFaceSmoothingGroups().addAll(smoothingGroups);

		mesh = buildTriangleMesh(15, 15, 1);
		
		MeshView   meshView = new MeshView(mesh);
		//meshView.setCullFace(CullFace.NONE);
		meshView.setMaterial(greenMaterial);

		PerspectiveCamera camera = new PerspectiveCamera(true);             

//		camera.getTransforms().addAll (
//				new Rotate(-20, Rotate.X_AXIS),
//				new Rotate(0, Rotate.Y_AXIS),
//				new Rotate(0, Rotate.Z_AXIS),
//				new Translate(0, 0, -300)
//				);


		PointLight light2 = new PointLight();
		Rotate rz = new Rotate(-90, Rotate.Z_AXIS);
		light2.getTransforms().addAll (
				new Rotate(-90, Rotate.X_AXIS),
				new Rotate(0, Rotate.Y_AXIS),
				rz,
				new Translate(0, 0, -100)
				);
//		light2.setTranslateX(400);
//		light2.setTranslateY(0);



		AmbientLight ambientLight = new AmbientLight(Color.WHITE);
		ambientLight.setOpacity(0.5);

		Group objects = new Group();
		//  objects.getChildren().add(myCylinder);			
			        objects.getChildren().add(xzBox);
			        objects.getChildren().add(zyBox);
			        objects.getChildren().add(xyBox);
		objects.getChildren().add(meshView);
		
		

//		objects.getTransforms().addAll (              
//				new Rotate(90, Rotate.X_AXIS),
//				new Rotate(20, Rotate.Y_AXIS),
//				new Rotate(-90, Rotate.Z_AXIS)
//
//				);
		Group subRoot = new Group();
		subRoot.getChildren().add(camera);
	//	subRoot.getChildren().add(ambientLight);
		subRoot.getChildren().add(light2);
		subRoot.getChildren().add(sol);
		subRoot.getChildren().add(objects);


		Rotate rx = new Rotate(0, Rotate.Y_AXIS);
		objects.getTransforms().addAll ( rx	);
	
		
		 final Timeline timeline = new Timeline();
		 timeline.setCycleCount(Animation.INDEFINITE);
		
		 timeline.getKeyFrames().add(new KeyFrame(Duration.minutes(0.2),
		   new KeyValue (rx.angleProperty(), 360)));
		

		 timeline.play();
		
		 
		 final Animation animation = new Transition() {
		     {
		         setCycleDuration(Duration.minutes(0.3));
		     }
		 
		     protected void interpolate(double frac) {
		    	 light2.setTranslateX(100*Math.cos(2*Math.PI*frac));
		    	 light2.setTranslateY(100*Math.sin(2*Math.PI*frac));
		    	 sol.setTranslateX(50*Math.cos(2*Math.PI*frac));
		    	 sol.setTranslateY(50*Math.sin(2*Math.PI*frac));		    	 
		     }
		 
		 };
		 animation.setCycleCount(Animation.INDEFINITE);
		 animation.play();
	

		SubScene subScene = new SubScene(subRoot, 1080, 920);
		subScene.setFill(Color.ALICEBLUE);
		subScene.setCamera(camera);
		
		  Translate centerTranslate = new Translate();
	        centerTranslate.xProperty().bind(subScene.widthProperty().divide(2));
	        centerTranslate.yProperty().bind(subScene.heightProperty().divide(2));
	        
	        camera.getTransforms().addAll(centerTranslate, translate, rotateX, rotateY, translateY);
	        
	        DragSupport dragSupport = new DragSupport(subScene, null, MouseButton.SECONDARY, Orientation.VERTICAL, translate.zProperty(), -3);
	        DragSupport dragSupport1 = new DragSupport(subScene, null, Orientation.HORIZONTAL, camera.rotateProperty());
	        DragSupport dragSupport2 = new DragSupport(subScene, null, Orientation.VERTICAL, rotateX.angleProperty());
	        DragSupport dragSupport3 = new DragSupport(subScene, null, MouseButton.MIDDLE, Orientation.HORIZONTAL, translate.xProperty());
	        DragSupport dragSupport4 = new DragSupport(subScene, null, MouseButton.MIDDLE, Orientation.VERTICAL, translate.yProperty());
	        

	        Label ltx = new Label("camera x:");
	        Label tx = new Label();
	        tx.textProperty().bindBidirectional(translate.xProperty(),new NumberStringConverter());
	        Label lty = new Label("camera y:");
	        Label ty = new Label();
	        ty.textProperty().bindBidirectional(translate.yProperty(),new NumberStringConverter());
	        Label ltz = new Label("camera z:");
	        Label tz = new Label();
	        tz.textProperty().bindBidirectional(translate.zProperty(),new NumberStringConverter());
	        
	        Label lrx = new Label("camera rotateProperty:");
	        Label rotateLabel = new Label();
	        rotateLabel.textProperty().bindBidirectional(  camera.rotateProperty(),new NumberStringConverter());
	       
	        HBox hBox = new HBox(ltx,tx,lty,ty,ltz,tz,lrx,rotateLabel);
	        VBox root = new VBox();
	        
	        root.getChildren().add(hBox);
		root.getChildren().add(subScene);


		//camera.setFieldOfView(20);//fishEye
//		camera.setNearClip(0.5);
		camera.setFarClip(500);
		// In JavaFX, the camera's coordinate system is Y-down, which means X
		// axis points to the right, Y axis is pointing down, Z axis is pointing
		// away from the viewer or into the screen.
		return root;
	}

	
	//TODO construir un mesh a partir de un shp de poligonos
	//1) leo el shp y obtengo todos los vertices con sus alturas
	//2)
	static TriangleMesh buildTriangleMesh(int subDivX, int subDivY, float scale) {

		final int pointSize = 3;
		final int texCoordSize = 2;
		// 3 point indices and 3 texCoord indices per triangle
		final int faceSize = 6;
		int numDivX = subDivX + 1;
		int numVerts = (subDivY + 1) * numDivX;
		float points[] = new float[numVerts * pointSize];
		float textureCoords[] = new float[numVerts * texCoordSize];
		int faceCount = subDivX * subDivY*subDivX* 2;
		int faces[] = new int[faceCount * faceSize];
		int smoothingGroups[] = new int[faceCount ];

		// Create points and texCoords
		//y es la subdivision en el eje y para la que estamos creando los puntos
		for (int y = 0; y <= subDivY; y++) {
			//dy es el porcentaje de avance entre minY y maxY; va entre 0 y 1
			float dy = (float) y / subDivY;
			//fy es la posicion en el eje Y del punto
			double fy = (1 - dy) * minY + dy * maxY;

			for (int x = 0; x <= subDivX; x++) {
				float dx = (float) x / subDivX;
				double fx = (1 - dx) * minX + dx * maxX;

				int index = y * numDivX * pointSize + (x * pointSize);
				points[index] = (float) fx * scale;
				points[index + 1] = (float)(fy*fy + 5*Math.sin(fx/2*Math.PI))/10; 
				points[index + 2] =(float) fy * scale;

				index = y * numDivX * texCoordSize + (x * texCoordSize);
				textureCoords[index] = dx;//si me paso de 1 vuelvo a empezar
				textureCoords[index + 1] = dy;
			}
		}

		// Create faces
		for (int y = 0; y < subDivY; y++) {
			for (int x = 0; x < subDivX; x++) {
				int p00 = y * numDivX + x;
				int p01 = p00 + 1;
				int p10 = p00 + numDivX;
				int p11 = p10 + 1;

				int tc00 = y * numDivX + x;
				int tc01 = tc00 + 1;
				int tc10 = tc00 + numDivX;
				int tc11 = tc10 + 1;

				int index = (y * subDivX * faceSize + (x * faceSize)) * 2;
//			        faces[index + 0] = p00;
//	                faces[index + 1] = tc00;
//	                faces[index + 2] = p10;
//	                faces[index + 3] = tc10;
//	                faces[index + 4] = p11;
//	                faces[index + 5] = tc11;
//	             
//	                index += faceSize;
	                faces[index + 0] = p00;
	                faces[index + 1] = tc00;
	                faces[index + 2] = p11;
	                faces[index + 3] = tc11;
	                faces[index + 4] = p10;
	                faces[index + 5] = tc10;

//	                index += faceSize;
//	                faces[index + 0] = p11;
//	                faces[index + 1] = tc11;
//	                faces[index + 2] = p01;
//	                faces[index + 3] = tc01;
//	                faces[index + 4] = p00;
//	                faces[index + 5] = tc00;
	                
	                index += faceSize;
	                faces[index + 0] = p11;
	                faces[index + 1] = tc11;
	                faces[index + 2] = p00;
	                faces[index + 3] = tc00;
	                faces[index + 4] = p01;
	                faces[index + 5] = tc01;
			}
		}

		
		for(int i = 0 ; i<faces.length/6;i++){
			
				smoothingGroups[i]=i;
			
		}
		
		TriangleMesh triangleMesh = new TriangleMesh();
		triangleMesh.getPoints().setAll(points);
		triangleMesh.getTexCoords().setAll(textureCoords);
		triangleMesh.getFaces().setAll(faces);
		System.out.println("faces ="+faces.length);
		System.out.println("smoothingGroups ="+smoothingGroups.length);
		triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);
		return triangleMesh;
	}

	public Parent createContent() throws Exception {

		// Box
		Box testBox = new Box(5, 5, 5);
		testBox.setMaterial(new PhongMaterial(Color.RED));
		//   testBox.setDrawMode(DrawMode.LINE);

		// Create and position camera
		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.getTransforms().addAll (
				new Rotate(-20, Rotate.Y_AXIS),
				new Rotate(-20, Rotate.X_AXIS),
				new Translate(0, 0, -15));

		// Build the Scene Graph
		Group subRoot = new Group();       
		subRoot.getChildren().add(camera);
		subRoot.getChildren().add(testBox);

		// Use a SubScene       
		SubScene subScene = new SubScene(subRoot, 300,300);
		subScene.setFill(Color.ALICEBLUE);
		subScene.setCamera(camera);

		Group root = new Group();
		root.getChildren().add(subScene);
		return root;
	}
	
	public void colorPalete(){
		Image imgPalette = new WritableImage(40, 40);
		PixelWriter pw = ((WritableImage)imgPalette).getPixelWriter();
		AtomicInteger count = new AtomicInteger();
		IntStream.range(0, 40).boxed()
		        .forEach(y->IntStream.range(0, 40).boxed()
		                .forEach(x->pw.setColor(x, y, Color.hsb(count.getAndIncrement()/1600*360,1,1))));

	}
	
	public DoubleStream getTextureLocation(int iPoint){
	    int y = iPoint/40; 
	    int x = iPoint-40*y;
	    return DoubleStream.of((((float)x)/40f),(((float)y)/40f));
	}
	 
//	public float[] getTexturePaletteArray(){
//	    return IntStream.range(0,colors).boxed()
//	        .flatMapToDouble(palette::getTextureLocation)
//	        .collect(()->new FloatCollector(2*colors),
//	        		FloatCollector::add, FloatCollector::join)
//	        .toArray();
//	}
	 
	//mesh.getTexCoords().setAll(getTexturePaletteArray());

	@FunctionalInterface
	public interface DensityFunction<T> {
	    Double eval(T p);
	}
	 
	private DensityFunction<Point3D> density;
	
	private double min, max;
	 
	public void updateExtremes(List<Point3D> points){
	    max=points.parallelStream().mapToDouble(density::eval).max().orElse(1.0);
	    min=points.parallelStream().mapToDouble(density::eval).min().orElse(0.0);
	    if(max==min){
	        max=1.0+min;
	    }
	}
	public int mapDensity(Point3D p){
	    int f=(int)((density.eval(p)-min)/(max-min)*colors);
	    if(f<0){
	        f=0;
	    }
	    if(f>=colors){
	        f=colors-1;
	    }
	    return f;
	}
	 
	public int[] updateFacesWithDensityMap(List<Point3D> points, List<Point3D> faces){
	    return faces.parallelStream().map(f->{
	            int p0=(int)f.getX(); int p1=(int)f.getY(); int p2=(int)f.getZ();
	            int t0=mapDensity(points.get(p0));
	            int t1=mapDensity(points.get(p1));
	            int t2=mapDensity(points.get(p2));
	            return IntStream.of(p0, t0, p1, t1, p2, t2);
	        }).flatMapToInt(i->i).toArray();
	}
	 
	//mesh.getFaces().setAll(updateFacesWithDensityMap(listVertices, listFaces));
	public static void main(String[] args) {
		Application.launch(Main3D.class, args);
	}
}
