package edu.curtin.saed.assignment1;

import java.util.Arrays;
import java.util.Comparator;

// FILE:      Robot.java
// AUTHOR:    Janet Joy
// PURPOSE:   a data structure to hold data about Robot
// Last Mod:  11th Sept

public class Robot implements Runnable
{
    String tag;
    Coordinates currCoords = null;
    Coordinates futuCoords = null;
    int delay;
    boolean pleaseKillSoon = false;
    boolean die = false;
    Coordinates[] nextMoves = new Coordinates[4]; //(x1, y1) (x2, y2) (x3, y3) (x4, y4)
    RoboManager manager;

    public Robot(String tag, double x, double y, int delay, RoboManager manager)
    {
        this.tag = tag;
        currCoords = new Coordinates(x, y);
        futuCoords = new Coordinates(-1, -1);
        this.delay = delay;
        this.manager = manager;
    }

    public void setCoords(double x, double y)
    {
        currCoords.setX(x);
        currCoords.setY(y);
    }

    public void setFutuCoords(double x, double y)
    {
        futuCoords.setX(x);
        futuCoords.setY(y);
    }

    public void switchToFuture()
    {
        setCoords(futuCoords.getX(), futuCoords.getY());
        futuCoords = null;
    }

    public Coordinates getCurrC()
    {
        return currCoords;
    }

    public Coordinates getFutuC()
    {
        return futuCoords;
    }

    public Coordinates[] getNextMoves()
    {
        return nextMoves;
    }

    public String getTag()
    {
        return tag;
    }

    public void die()
    {
        die = true;
    }

    public void setKillStatus()
    {
        pleaseKillSoon = true;
    }

    public boolean getKillStatus()
    {
        return pleaseKillSoon;
    }

    @Override
    public void run()
    {
        try 
        {
            while(!die)
            {
                Thread.sleep(delay);
                possibleMoves();
                manager.validRoboMove(this, nextMoves);
            }
        } 
        catch (InterruptedException e) {}
    }

    private void possibleMoves()
    {
        //right
        nextMoves[2] = new Coordinates((currCoords.getX() + 1), currCoords.getY());

        //left
        nextMoves[1] = new Coordinates((currCoords.getX() - 1), currCoords.getY());

        //up
        nextMoves[3] = new Coordinates(currCoords.getX(), (currCoords.getY() - 1));

        //down
        nextMoves[0] = new Coordinates(currCoords.getX(), (currCoords.getY() + 1));

        sortByDistance();
        
    }

    private void sortByDistance()
    {
        //coordinate -> lambda function that takes in a Coordinate object (int x) so coordinate = x
        //coordinate.getD... is applying our comparison method to it

        Comparator<Coordinates> comparator = Comparator.comparingInt(coordinate ->
        coordinate.getDistanceFromCitadel());

        Arrays.sort(nextMoves, comparator);
    }
}