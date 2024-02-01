package edu.curtin.saed.assignment1;

import javafx.scene.canvas.*;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.application.Platform;

import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.util.*;

/**
 * A JavaFX GUI element that displays a grid on which you can draw images, text and lines.
 */
public class JFXArena extends Pane
{
    // Represents an image to draw, retrieved as a project resource.
    private static final String ROBOT_FILE = "1554047213.png";
    private static final String CITADEL_FILE = "citadel.png";
    private static final String WALL_FILE = "wall.png";
    private static final String BROKEN_WALL_FILE = "bWall.png";
    private Image wall;
    private Image brokenWall;
    private Image robot1;
    private Image citadel;
    
    // The following values are arbitrary, and you may need to modify them according to the 
    // requirements of your application.
    private int gridWidth = 9;
    private int gridHeight = 9;
    private Map<String, Robot> myMap = new ConcurrentHashMap<>();
    private Map<Integer, Wall> walls = new ConcurrentHashMap<>();
    private String[][] map;

    private double gridSquareSize; // Auto-calculated
    private Canvas canvas; // Used to provide a 'drawing surface'.
    private Object mutex = new Object();
    private Thread myThread = null; 
    private boolean GameOver = false;

    private List<ArenaListener> listeners = null;
    
    /**
     * Creates a new arena object, loading the robot image and initialising a drawing surface.
     */
    public JFXArena()
    {
        // Here's how (in JavaFX) you get an Image object from an image file that's part of the 
        // project's "resources". If you need multiple different images, you can modify this code 
        // accordingly.
        
        // (NOTE: _DO NOT_ use ordinary file-reading operations here, and in particular do not try
        // to specify the file's path/location. That will ruin things if you try to create a 
        // distributable version of your code with './gradlew build'. The approach below is how a 
        // project is supposed to read its own internal resources, and should work both for 
        // './gradlew run' and './gradlew build'.)
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(ROBOT_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + ROBOT_FILE);
            }
            robot1 = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + ROBOT_FILE, e);
        }
        
        try(InputStream is = getClass().getClassLoader().getResourceAsStream(CITADEL_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + CITADEL_FILE);
            }
            citadel = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + CITADEL_FILE, e);
        }

        try(InputStream is = getClass().getClassLoader().getResourceAsStream(WALL_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + WALL_FILE);
            }
            wall = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + WALL_FILE, e);
        }

        try(InputStream is = getClass().getClassLoader().getResourceAsStream(BROKEN_WALL_FILE))
        {
            if(is == null)
            {
                throw new AssertionError("Cannot find image file " + BROKEN_WALL_FILE);
            }
            brokenWall = new Image(is);
        }
        catch(IOException e)
        {
            throw new AssertionError("Cannot load image file " + BROKEN_WALL_FILE, e);
        }

        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        getChildren().add(canvas);
    }
  
    public void setMap(String[][] map)
    {
        this.map = map;
        requestLayout();
    }

    public void addWall(Wall wall)
    {
        walls.put(wall.getNo(), wall);
        requestLayout();
    }

    public void updateWall(int index)
    {
        
        Wall w = walls.get(index);
        w.hit();
        walls.put(w.getNo(), w);

        requestLayout(); 
    }

    public void clearWall(int index)
    {
        walls.remove(index);
        requestLayout(); 
    }

    public void setGameOver()
    {
        GameOver = true;

        for (Robot robo : myMap.values()) {
            robo.die();
        }
    }

    public void addToRoboList(Robot robo, boolean remove)
    {
        myMap.put(robo.getTag(), robo);
        if(remove && !GameOver)
        {
            myMap.remove(robo.getTag());
            robo.die();
        }
        else if(GameOver)
        {
            robo.die();
        }

        requestLayout();  
    }

    /**
     * Adds a callback for when the user clicks on a grid square within the arena. The callback 
     * (of type ArenaListener) receives the grid (x,y) coordinates as parameters to the 
     * 'squareClicked()' method.
     */
    public void addListener(ArenaListener newListener)
    {
        if(listeners == null)
        {
            listeners = new LinkedList<>();
            setOnMouseClicked(event ->
            {
                int gridX = (int)(event.getX() / gridSquareSize);
                int gridY = (int)(event.getY() / gridSquareSize);
                
                if(gridX < gridWidth && gridY < gridHeight)
                {
                    for(ArenaListener listener : listeners)
                    {   
                        listener.squareClicked(gridX, gridY);
                    }
                }
            });
        }
        listeners.add(newListener);
    }
        
    /**
     * This method is called in order to redraw the screen, either because the user is manipulating 
     * the window, OR because you've called 'requestLayout()'.
     *
     * You will need to modify the last part of this method; specifically the sequence of calls to
     * the other 'draw...()' methods. You shouldn't need to modify anything else about it.
     */
    @Override
    public void layoutChildren()
    {
        super.layoutChildren(); 
        GraphicsContext gfx = canvas.getGraphicsContext2D();
        gfx.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
        
        // First, calculate how big each grid cell should be, in pixels. (We do need to do this
        // every time we repaint the arena, because the size can change.)
        gridSquareSize = Math.min(
            getWidth() / (double) gridWidth,
            getHeight() / (double) gridHeight);
            
        double arenaPixelWidth = gridWidth * gridSquareSize;
        double arenaPixelHeight = gridHeight * gridSquareSize;
            
            
        // Draw the arena grid lines. This may help for debugging purposes, and just generally
        // to see what's going on.
        gfx.setStroke(Color.DARKGREY);
        gfx.strokeRect(0.0, 0.0, arenaPixelWidth - 1.0, arenaPixelHeight - 1.0); // Outer edge

        for(int gridX = 1; gridX < gridWidth; gridX++) // Internal vertical grid lines
        {
            double x = (double) gridX * gridSquareSize;
            gfx.strokeLine(x, 0.0, x, arenaPixelHeight);
        }
        
        for(int gridY = 1; gridY < gridHeight; gridY++) // Internal horizontal grid lines
        {
            double y = (double) gridY * gridSquareSize;
            gfx.strokeLine(0.0, y, arenaPixelWidth, y);
        }

        // Invoke helper methods to draw things at the current location.
        // ** You will need to adapt this to the requirements of your application. **
        //___________________________________________________________________________
        drawImage(gfx, citadel, 4, 4);

        for (Robot robo : myMap.values()) 
        {
            Coordinates curr = robo.getCurrC();
            drawImage(gfx, robot1, curr.getX(), curr.getY());
            drawLabel(gfx, robo.getTag(), curr.getX(), curr.getY());
        }

        for (Wall wallObj : walls.values()) 
        {
            Coordinates curr = wallObj.getCoords();
            if(wallObj.hitStatus())
            {
                drawImage(gfx, brokenWall, curr.getX(), curr.getY());
            }
            else
            {
                drawImage(gfx, wall, curr.getX(), curr.getY());
            }   
        }
    }

  
    private void drawImage(GraphicsContext gfx, Image image, double gridX, double gridY)
    {
        // Get the pixel coordinates representing the centre of where the image is to be drawn. 
        double x = (gridX + 0.5) * gridSquareSize;
        double y = (gridY + 0.5) * gridSquareSize;
        
        // We also need to know how "big" to make the image. The image file has a natural width 
        // and height, but that's not necessarily the size we want to draw it on the screen. We 
        // do, however, want to preserve its aspect ratio.
        double fullSizePixelWidth = robot1.getWidth();
        double fullSizePixelHeight = robot1.getHeight();
        
        double displayedPixelWidth, displayedPixelHeight;
        if(fullSizePixelWidth > fullSizePixelHeight)
        {
            // Here, the image is wider than it is high, so we'll display it such that it's as 
            // wide as a full grid cell, and the height will be set to preserve the aspect 
            // ratio.
            displayedPixelWidth = gridSquareSize;
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth;
        }
        else
        {
            // Otherwise, it's the other way around -- full height, and width is set to 
            // preserve the aspect ratio.
            displayedPixelHeight = gridSquareSize;
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight;
        }

        // Actually put the image on the screen.
        gfx.drawImage(image,
            x - displayedPixelWidth / 2.0,  // Top-left pixel coordinates.
            y - displayedPixelHeight / 2.0, 
            displayedPixelWidth,              // Size of displayed image.
            displayedPixelHeight);
    }
    
    private void drawLabel(GraphicsContext gfx, String label, double gridX, double gridY)
    {
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setTextBaseline(VPos.TOP);
        gfx.setStroke(Color.BLUE);
        gfx.strokeText(label, (gridX + 0.5) * gridSquareSize, (gridY + 1.0) * gridSquareSize);
    }
    
    private void drawLine(GraphicsContext gfx, double gridX1, double gridY1, 
                                               double gridX2, double gridY2)
    {
        gfx.setStroke(Color.RED);
        
        // Recalculate the starting coordinate to be one unit closer to the destination, so that it
        // doesn't overlap with any image appearing in the starting grid cell.
        final double radius = 0.5;
        double angle = Math.atan2(gridY2 - gridY1, gridX2 - gridX1);
        double clippedGridX1 = gridX1 + Math.cos(angle) * radius;
        double clippedGridY1 = gridY1 + Math.sin(angle) * radius;
        
        gfx.strokeLine((clippedGridX1 + 0.5) * gridSquareSize, 
                       (clippedGridY1 + 0.5) * gridSquareSize, 
                       (gridX2 + 0.5) * gridSquareSize, 
                       (gridY2 + 0.5) * gridSquareSize);
    }
}

