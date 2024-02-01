package edu.curtin.saed.assignment1;
import java.util.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import java.util.Random;

// FILE:      RoboManager.java
// AUTHOR:    Janet Joy
// PURPOSE:   manages interactions between robots, runs robots on threads
// COMMENTS:  uses thread pool and a blocking queue
// Last Mod:  11th Sept


public class RoboManager
{
    private String[][] roboTracker;
    private Object mutex = new Object();
    private int noOfRobots;
    private Map map;
    private ExecutorService executor;
    private TextArea textArea;

    public RoboManager(Map map, TextArea textArea)
    {
        this.textArea = textArea;
        this.map = map;
        roboTracker = map.getMap();
        noOfRobots = 0;
        this.executor = Executors.newFixedThreadPool(50);
        create();
    }


    //will be called by multiple robo threads
    public void validRoboMove(Robot robo, Coordinates[] pMoves)
    {
        Coordinates curr = robo.getCurrC();
        Coordinates[] moves = pMoves;
        
        for(int x = 0; x < moves.length; x++)
        {
            int pX = (int)moves[x].getX();
            int pY = (int)moves[x].getY();

            if(pX >= 0 && pX < 9 )
            {
                if (pY >=0 && pY < 9)
                {
                    synchronized(mutex)
                    {
                        if(!(roboTracker[pY][pX].contains("r")))
                        {
                            robo.setFutuCoords(pX, pY);
                            break;
                        }
                    }
                }
            }
        }

        if(!curr.theSame(robo.getFutuC()))
        {
            map.moveRobo(robo);
        }

    }

    public void create() //creates robots and add them to the queue
    {
        Random random = new Random();
        Coordinates[] startPoints = new Coordinates[4];
        startPoints[0] = new Coordinates(0, 0);
        startPoints[1] = new Coordinates(8, 8);
        startPoints[2] = new Coordinates(0, 8);
        startPoints[3] = new Coordinates(8, 0);

        executor.execute(() -> // This is a Runnable.
        {
            while(!map.getGameOStatus())
            {
                try
                {
                    Thread.sleep(1500); //1500
                    int delay = 500 + random.nextInt(3000);
                    int index = random.nextInt(4);
                    int x = (int) startPoints[index].getX();
                    int y = (int) startPoints[index].getY();

                    synchronized(mutex)
                    {
                        if(!roboTracker[y][x].contains("r"))
                        {
                            noOfRobots = noOfRobots + 1;
                            String tag = String.valueOf(noOfRobots);
                            Robot minion = new Robot(tag, ((double) x), ((double) y), delay, this);
                            map.addRobo(minion);
                            //queue.put(minion);
                            executor.submit(minion);
                            Platform.runLater(() -> {
                                textArea.appendText(("Robo " + noOfRobots + " has started at (" + x + ", " + y + ")\n"));
                            });
                        }
                    }
                }
                catch(InterruptedException ex)
                {}
            }

            executor.shutdown();
        });
    }
}