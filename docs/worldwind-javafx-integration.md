# WorldWind JavaFX Integration

This document explains how to use the new `WorldWindJavaFXCanvas` class to integrate NASA WorldWind with JavaFX applications.

## Overview

The `WorldWindJavaFXCanvas` class extends JavaFX's `Canvas` class to provide WorldWind functionality within JavaFX scenes. This allows you to use WorldWind's 3D globe and mapping capabilities in modern JavaFX applications.

## Key Features

- **JavaFX Native**: Extends `javafx.scene.canvas.Canvas` for seamless integration
- **WorldWind Compatible**: Provides access to all WorldWind functionality through the underlying `WorldWindowGLDrawable`
- **Event Handling**: Includes JavaFX event handlers for mouse, keyboard, and drag-and-drop operations
- **Animation Support**: Uses JavaFX `AnimationTimer` for continuous rendering
- **Responsive Design**: Automatically handles canvas resizing

## Dependencies

The following dependencies have been added to `pom.xml`:

```xml
<!-- OpenGLFX for JavaFX OpenGL integration -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-graphics</artifactId>
    <version>24.0.2</version>
</dependency>

<!-- OpenGLFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-opengl</artifactId>
    <version>24.0.2</version>
</dependency>
```

## Basic Usage

### 1. Create the Canvas

```java
// Create a WorldWind JavaFX canvas with default size (800x600)
WorldWindJavaFXCanvas worldWindCanvas = new WorldWindJavaFXCanvas();

// Or specify custom dimensions
WorldWindJavaFXCanvas worldWindCanvas = new WorldWindJavaFXCanvas(1200, 800);
```

### 2. Set Up WorldWind Model

```java
// Create the default WorldWind model
Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
worldWindCanvas.setModel(model);
```

### 3. Add to JavaFX Scene

```java
BorderPane root = new BorderPane();
root.setCenter(worldWindCanvas);

Scene scene = new Scene(root, 1200, 800);
primaryStage.setScene(scene);
```

## Complete Example

See `WorldWindJavaFXExample.java` for a complete working example.

## Architecture

The class works by:

1. **Delegation**: Most WorldWind operations are delegated to the underlying `WorldWindowGLDrawable`
2. **Canvas Rendering**: Uses JavaFX `Canvas` and `GraphicsContext` for 2D rendering
3. **Event Translation**: Converts JavaFX events to WorldWind input events (placeholder implementation)
4. **Animation Loop**: Uses `AnimationTimer` for continuous updates

## Limitations and Notes

### Current Limitations

- **OpenGL Integration**: The current implementation is a placeholder. Full OpenGL integration requires additional work to bridge JavaFX's OpenGL support with WorldWind's rendering pipeline.
- **Event Handling**: Mouse and keyboard event handlers are placeholder implementations that need to be completed.
- **Performance**: The current 2D canvas rendering approach may not provide the same performance as the original OpenGL-based implementation.

### Future Improvements

To make this a fully functional WorldWind JavaFX component, you would need to:

1. **Implement OpenGL Context Integration**: Use JavaFX's OpenGL capabilities to create a proper OpenGL context for WorldWind.
2. **Complete Event Handling**: Implement proper event translation from JavaFX to WorldWind input events.
3. **Optimize Rendering**: Integrate WorldWind's rendering pipeline with JavaFX's rendering system.
4. **Add Layer Management**: Implement proper layer management and rendering.

## Migration from WorldWindGLJPanel

If you're migrating from the Swing-based `WorldWindGLJPanel`:

1. **Replace Swing Components**: Use JavaFX layout containers instead of Swing layouts
2. **Update Event Handling**: Replace Swing event listeners with JavaFX event handlers
3. **Modify Rendering**: Update any custom rendering code to work with the Canvas approach
4. **Test Integration**: Verify that all WorldWind functionality works as expected

## Troubleshooting

### Common Issues

1. **WorldWind Initialization Errors**: Ensure WorldWind is properly configured and all dependencies are available
2. **Canvas Not Displaying**: Check that the canvas is properly added to the scene and has valid dimensions
3. **Performance Issues**: The current implementation may be slower than the original OpenGL version

### Debug Mode

Enable debug logging to see detailed information about WorldWind operations:

```java
// Enable WorldWind debug logging
System.setProperty("gov.nasa.worldwind.logging.level", "FINE");
```

## Conclusion

The `WorldWindJavaFXCanvas` provides a foundation for integrating WorldWind with JavaFX applications. While the current implementation has some limitations, it demonstrates the approach and can be extended to provide full WorldWind functionality in JavaFX environments.

For production use, consider implementing the OpenGL integration and completing the event handling to achieve the same performance and functionality as the original Swing-based implementation.
