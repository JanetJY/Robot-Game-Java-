package edu.curtin.saed.assignment1;

import java.util.Arrays;
import java.util.Comparator;

// FILE:      Wall.java
// AUTHOR:    Janet Joy
// PURPOSE:   a data structure to hold data about Wall
// Last Mod:  11th Sept

public class Wall
{
    boolean hit = false;
    int no;
    Coordinates currCoords = null;

    public Wall(int no, double x, double y)
    {
        this.no = no;
        currCoords = new Coordinates(x, y);
    }

    public int getNo()
    {
        return no;
    }

    public Coordinates getCoords()
    {
        return currCoords;
    }

    public void hit()
    {
        hit = true;
    }

    public boolean hitStatus()
    {
        return hit;
    }
}