package edu.curtin.saed.assignment1;

// FILE:      Coodinates.java
// AUTHOR:    Janet Joy
// PURPOSE:   a data structure to hold x and y values hence coordinates
// COMMENTS:  makes it convient to get and return coordinates
// Last Mod:  11th Sept

public class Coordinates
{
    private double x;
    private double y;

    public Coordinates(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public int getDistanceFromCitadel()
    {
        int cX = 4;
        int cY = 4;

        int distanceX = Math.abs(cX - ((int)x));
        int distanceY = Math.abs(cX - ((int)y));

        return distanceX + distanceY;
    }

    public boolean theSame(Coordinates compare)
    {
        boolean same = false;
        if(x == compare.getX() && y == compare.getY())
        {
            same = true;
        }
        
        return same;
    }

    @Override
    public String toString()
    {
        return("(" + x + ", " + y + ")");
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
       this.x = x; 
    }

    public void setY(double y) {
        this.y = y;
    }
}