package org.OGFishing.Models;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class FishingPool
{
    private final Location pos1;
    private final Location pos2;
    private final Map<String, Reward> rewards;
    private boolean allowFishing;

    public FishingPool(Location pos1, Location pos2, Map<String, Reward> rewards, boolean allowFishing)
    {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.rewards = (rewards != null ? rewards : new HashMap());
        this.allowFishing = allowFishing;
    }

    public void setAllowFishing(boolean allowFishing)
    {
        this.allowFishing = allowFishing;
    }

    public boolean getAllowFishing()
    {
        return this.allowFishing;
    }

    public boolean isInPool(Location location)
    {
        World world = this.pos1.getWorld();
        if (world != null && world.equals(location.getWorld()))
        {
            double minX = Math.min(this.pos1.getX(), this.pos2.getX());
            double minY = Math.min(this.pos1.getY(), this.pos2.getY());
            double minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
            double maxX = Math.max(this.pos1.getX(), this.pos2.getX());
            double maxY = Math.max(this.pos1.getY(), this.pos2.getY());
            double maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());
            return location.getX() >= minX && location.getX() <= maxX && location.getY() >= minY && location.getY() <= maxY && location.getZ() >= minZ && location.getZ() <= maxZ;
        }
        else
        {
            return false;
        }
    }

    public Map<String, Reward> getRewards()
    {
        return this.rewards;
    }
}
