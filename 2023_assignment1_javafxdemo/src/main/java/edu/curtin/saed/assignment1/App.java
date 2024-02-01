package edu.curtin.saed.assignment1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application 
{
    private TextArea logger;
    private Label score;
    private Label q;
    public static void main(String[] args) 
    {
        launch();        
    }
    
    @Override
    public void start(Stage stage) 
    {
        logger = new TextArea();
        score = new Label("Score: 0");
        q = new Label("Queue: 0");
        stage.setTitle("Robo Annihilator");

        JFXArena arena = new JFXArena();
        Map map = new Map(arena, logger, q, score);
        RoboManager rM = new RoboManager(map, logger);
        arena.addListener((x, y) ->
        {
            map.addWall(x, y);

        });

        

        ToolBar toolbar = new ToolBar();
        toolbar.getItems().addAll(score, q);
      
        
        //____________________MAIN SCREEN LOGGER___________________________
 
        
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(arena, logger);
        arena.setMinWidth(300.0);
        
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);
        
        Scene scene = new Scene(contentPane, 800, 800);
        stage.setScene(scene);
        stage.show();

    }
}
