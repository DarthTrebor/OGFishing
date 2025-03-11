package org.OGFishing;

import org.bukkit.ChatColor;

public enum Messages
{
    NO_PERMISSION_MESSAGE(ChatColor.RED + "You don't have permission to use this command"),
    INCOMPLETE_FORCESTART_MESSAGE(ChatColor.RED + "Incomplete command, use /ogfishing forcestart <pool_name>"),
    INCOMPLETE_FORCESTOP_MESSAGE(ChatColor.RED + "Incomplete command, use /ogfishing forcestop <pool_name>"),
    RESET_BIGGESTFISH_MESSAGE(ChatColor.GREEN + "Now the biggest fish caught has 0kg."),
    RESET_LEADERBOARD_MESSAGE(ChatColor.GREEN + "You have successfully reset the leaderboard."),
    INCOMPLETE_SETPOOL_COMMAND(ChatColor.RED + "Incomplete command, use /ogfishing setpool <pool_name>"),
    ONLY_PLAYER_COMMAND(ChatColor.RED + "This command can only be used by players!"),
    EXACTLY_TWOPOS_COMMAND(ChatColor.RED + "You must select 2 positions in order to create a pool!"),
    CONFIG_RELOADED_MESSAGE(ChatColor.GREEN + "Config has been reloaded!"),
    UNDEFINED_POOL_MESSAGE(ChatColor.RED + "This fishing pool does not exist!"),
    POSITIONONE_SET_MESSAGE(ChatColor.GREEN + "First position set"),
    POSITIONSECOND_SET_MESSAGE(ChatColor.GREEN + "Second position set");

    private final String value;

    Messages(String value)
    {
        this.value = value;
    }

    public String getMessage()
    {
        return value;
    }
}
