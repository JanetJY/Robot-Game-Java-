package edu.curtin.saed.assignment1;
import java.math.BigDecimal;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

// FILE:      Map.java
// AUTHOR:    Janet Joy
// PURPOSE:   manages interactions walls, robots holds all the game logic
// COMMENTS:  uses a blocking queue
// Last Mod:  11th Sept


public class Map
{
    private String[][] map;
    private Object mutex = new Object();
    private Object scoreMutex = new Object();
    private boolean GameOver = false;
    private JFXArena arena;
    private int noOfWall, totalWall;
    private TextArea tA;
    private Thread wallExecute = null;
    private Thread timeScorer;
    private BlockingQueue<Coordinates> queue = new ArrayBlockingQueue<>(10);
    int score = 0;
    private Label q;
    private Label scoreL;

    public Map(JFXArena arena, TextArea tA, Label q, Label scoreL)
    {
        this.arena = arena;
        this.scoreL = scoreL;
        this.tA = tA;
        this.q = q;
        map = new String[9][9];
        noOfWall = 0;
        totalWall = 0;
        fill();
        timeScore();
    }

    private void fill()
    {
        // Fill the array with spaces
        for (int i = 0; i < 9; i++) 
        {
            for (int j = 0; j < 9; j++) 
            {
                map[i][j] = " ";
            }
        }

        map[4][4] = "c";

        arena.setMap(map);
    }

    private void timeScore()
    {
        //adds the +10 points to score every 1 second 
        Runnable myTask = () ->
        {
            try
            {
                while(true)
                {
                    Thread.sleep(1000);
                    synchronized(scoreMutex)
                    {
                        score = score + 10;
                    }

                    Platform.runLater(() -> {
                        scoreL.setText("Score: " + score +"");
                    });

                }
            }
            catch(InterruptedException ex){}
        };
        timeScorer = new Thread(myTask, "my-thread");
        timeScorer.start();
    }

    public String[][] getMap()
    {
        return map;
    }

    public void endWalls()
    {
        if(wallExecute != null)
        {
            wallExecute.interrupt();
        }
    }

    public void addWall(double x, double y)
    {
        if(noOfWall < 10 && queue.size() < 10)
        {
            try
            {
                if(wallExecute == null)
                {
                    takeBlueprints();
                }

                Platform.runLater(() -> {
                    q.setText("Queue: " + queue.size() +"");
                });
                queue.put(new Coordinates(x, y));
            }
            catch(InterruptedException ex)
            {}
        }
    }

    private void takeBlueprints()
    {
        Runnable myTask = () ->
        {
            try
            {
                while(!(Thread.interrupted()))
                {
                    if(noOfWall < 10)
                    {
                        Thread.sleep(2000);
                        int x, y;
                        do
                        {
                            if(Thread.interrupted())
                            {
                                throw new InterruptedException();
                            }
                            Platform.runLater(() -> {
                                q.setText("Queue: " + queue.size() +"");
                            });
                            Coordinates w = queue.take();
                            x = (int) w.getX();
                            y = (int) w.getY();

                        } while(!createWall(x, y) && !queue.isEmpty());
                    }
                }
            }
            catch(InterruptedException ex)
            {
                System.out.println("Error end thread");
            }
            
        };
        wallExecute = new Thread(myTask, "my-thread");
        wallExecute.start();
    }

    public boolean createWall(int x, int y)
    {
        boolean proceed = true;
        synchronized(mutex)
        {
            if( map[y][x].contains("w") || map[y][x].contains("c") || map[y][x].contains("r"))
            {
                //space is occupied 
                proceed = false;
            }
            else 
            {
                noOfWall = noOfWall + 1;
                totalWall = totalWall + 1;
                String info = "w" + totalWall;
                map[y][x] = info;
            }
        }

        if(proceed)
        {
            Platform.runLater(() -> {
                tA.appendText(("Wall " + totalWall+ " has been created at (" + x + ", " + y + ")\n"));
            });
            arena.addWall(new Wall(totalWall, x, y));
        }
        
        return proceed;
    }

    public boolean getGameOStatus()
    {
        return GameOver;
    }

    public void addRobo(Robot robo)
    {
        Coordinates curr = robo.getCurrC();
        int x = (int)curr.getX();
        int y = (int)curr.getY();

        synchronized(mutex)
        {
            if(map[y][x].contains("w"))
            {
                robo.setKillStatus();
                arena.addToRoboList(robo, true);
                wallLogic(x, y);
            }
            else
            {
                map[y][x] = "r";
                arena.addToRoboList(robo, false);
            }
        }
        
    }

    public void moveRobo(Robot robo)
    {
        Coordinates curr = robo.getCurrC();
        Coordinates future = robo.getFutuC();

        int x1 = (int)curr.getX();
        int y1 = (int)curr.getY();
        int x2 = (int)future.getX();
        int y2 = (int)future.getY();

        synchronized(mutex)
        {
            map[y1][x1] = "rOut";

            if(map[y2][x2].contains("w")) //when robo is facing a wall
            {
                robo.setKillStatus();
                synchronized(scoreMutex)
                {
                    score = score + 100;
                }

            }
            else if(y2 == 4 && x2 == 4) //when robo is headed to citadel
            {
                arena.setGameOver();
                GameOver = true;
                robo.setKillStatus();
                endWalls();
                timeScorer.interrupt();
                map[y2][x2] = "rIn";
            }
            else //open area
            {
                map[y2][x2] = "rIn";
            }
        }

        if (y1 == y2)
        {
            if(x1 < x2) //go right
            {
                moveRoboHorizontal(robo, "+0.1");
            }
            else //go left
            {
                moveRoboHorizontal(robo, "-0.1");
            }
        }
        else
        {
            if(y1 < y2) //go down
            {
                moveRoboVertical(robo, "+0.1");
            }
            else //go up
            {
                moveRoboVertical(robo, "-0.1");
            }
        }
    
    }

    public void moveRoboHorizontal(Robot robo, String direction) 
    {
        Coordinates curr = robo.getCurrC();
        Coordinates future = robo.getFutuC();

        int x = (int) curr.getX();
        int y = (int) curr.getY();  //old coords
        int x2 = (int)future.getX();
        int y2 = (int)future.getY(); //new coords

        BigDecimal currX = new BigDecimal(curr.getX());
        BigDecimal increment = new BigDecimal(direction);

        while(curr.getX() != future.getX() )
        {
            currX = currX.add(increment);
            curr.setX(currX.doubleValue());

            try 
            {
                Thread.sleep(40);

                if(curr.getX() == future.getX() && robo.getKillStatus() && !GameOver)
                {
                    arena.addToRoboList(robo, true);
                    wallLogic(x2, y2);
                }
                else
                {
                    arena.addToRoboList(robo, false);
                }
            } 
            catch (InterruptedException e) {}

            if(curr.getX() == future.getX())
            {
                synchronized(mutex)
                {
                    if(robo.getKillStatus())
                    {
                        map[y][x] = " "; //2D takes x,y differently
                    }
                    else
                    {
                        map[y][x] = " ";
                        map[y2][x2] = "r";
                    }
                }
            } 
        }
    }


    public void moveRoboVertical(Robot robo, String direction) 
    {
        Coordinates curr = robo.getCurrC();
        Coordinates future = robo.getFutuC();

        int x = (int) curr.getX();
        int y = (int) curr.getY();  //old coords
        int x2 = (int)future.getX();
        int y2 = (int)future.getY(); //future coords

        BigDecimal currY = new BigDecimal(curr.getY());
        BigDecimal increment = new BigDecimal(direction);

        while(curr.getY() != future.getY() )
        {
            currY = currY.add(increment);
            curr.setY(currY.doubleValue());

            try 
            {
                Thread.sleep(40);
                if(curr.getY() == future.getY() && robo.getKillStatus() && !GameOver)
                {
                    arena.addToRoboList(robo, true);
                    wallLogic(x2, y2);
                }
                else
                {
                    arena.addToRoboList(robo, false);
                }
            } 
            catch (InterruptedException e) {}

            if(curr.getY() == future.getY())
            {
                synchronized(mutex)
                {
                    if(robo.getKillStatus())
                    {
                        map[y][x] = " ";
                    }
                    else
                    {
                        map[y][x] = " ";
                        map[y2][x2] = "r";
                    }
                }
            } 
        }
    }

    private void wallLogic(int x2, int y2)
    {
        String wall;
        synchronized(mutex)
        {
            wall = new String(map[y2][x2]);
        }

        Platform.runLater(() -> {
            tA.appendText(("wall at ("+ x2 + ", " + y2 + ") has ben impacted\n"));
            scoreL.setText("Score: " + score +"");
        });

        if(wall.contains("b"))
        {
            int wallIndex = Integer.parseInt(wall.replace("bw", ""));
            synchronized(mutex)
            {
                map[y2][x2] = " ";
                noOfWall = noOfWall -1;
            }
            arena.clearWall(wallIndex);
        }
        else
        {
            int wallIndex = Integer.parseInt(wall.replace("w", ""));
            synchronized(mutex)
            {
                map[y2][x2] = map[y2][x2].replace("w", "bw");
            }
            arena.updateWall(wallIndex);
        }
    }

}