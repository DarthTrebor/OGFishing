package org.OGFishing.Models;

public class Reward
{
    private String message;
    private double chance;
    private String size;
    private String name;
    private String fishType;

    public Reward(String message, double chance, String size, String name, String fishType)
    {
        this.message = message;
        this.chance = chance;
        this.size = size;
        this.name = name;
        this.fishType = fishType;
    }

    public String getFishType()
    {
        return fishType;
    }

    public String getName()
    {
        return name;
    }

    public String getMessage()
    {
        return this.message;
    }

    public double getChance()
    {
        return this.chance;
    }

    public String getSize()
    {
        return size;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setChance(double chance)
    {
        this.chance = chance;
    }
}
