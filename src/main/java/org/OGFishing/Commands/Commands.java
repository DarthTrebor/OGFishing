package org.OGFishing.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.util.concurrent.AtomicDouble;
import org.OGFishing.Fishing;
import org.OGFishing.Messages;
import org.OGFishing.Permissions;
import org.OGFishing.Models.Selection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Commands implements CommandExecutor, TabCompleter
{
   private final Fishing plugin;

   public Commands(Fishing plugin)
   {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
   {
      if (args.length == 0)
      {
         return false;
      }
      else
      {
          String pCommand = args[0].toLowerCase();

          return switch (pCommand)
          {
              case "reload" -> this.handleReloadCommand(sender);
              case "wand" -> this.handleWandCommand(sender);
              case "setpool" -> this.handleSetPoolCommand(sender, args);
              case "resetkilograms" -> this.handleResetKilograms(sender);
              case "resetbiggestfish" -> this.handleResetBiggestFish(sender);
              case "forcestart" -> this.handleForceStart(sender, args);
              case "forcestop" -> this.handleForceStop(sender, args);
              default -> false;
          };
      }
   }

   public boolean handleForceStop(CommandSender sender, String[] args)
   {
      if (!sender.hasPermission(Permissions.PERMISSION_RESET.getPermission()))
      {
         sender.sendMessage(Messages.NO_PERMISSION_MESSAGE.getMessage());
      }

      if (args.length < 2)
      {
         sender.sendMessage(Messages.INCOMPLETE_FORCESTOP_MESSAGE.getMessage());
      }
      else
      {
         String pool = args[1];
         plugin.disableFishingForPool(Bukkit.getPlayer(sender.getName()), pool);
      }
      return true;
   }

   public boolean handleForceStart(CommandSender sender, String[] args)
   {
      if (!sender.hasPermission(Permissions.PERMISSION_RESET.getPermission()))
      {
         sender.sendMessage(Messages.NO_PERMISSION_MESSAGE.getMessage());
      }
      if (args.length < 2)
      {
         sender.sendMessage(Messages.INCOMPLETE_FORCESTART_MESSAGE.getMessage());
      }
      else
      {
         String pool = args[1];
         plugin.allowFishingForPool(Bukkit.getPlayer(sender.getName()), pool);
      }
      return true;
   }

   public boolean handleResetBiggestFish(CommandSender sender)
   {
      if (!sender.hasPermission(Permissions.PERMISSION_RESET.getPermission()))
      {
         sender.sendMessage(Messages.NO_PERMISSION_MESSAGE.getMessage());
      }
      else
      {
         plugin.setBiggestFishCaught(new AtomicDouble(0));
         sender.sendMessage(Messages.RESET_BIGGESTFISH_MESSAGE.getMessage());
      }
      return true;
   }

   public boolean handleResetKilograms(CommandSender sender)
   {
      if (!sender.hasPermission(Permissions.PERMISSION_RESET.getPermission()))
      {
         sender.sendMessage(Messages.NO_PERMISSION_MESSAGE.getMessage());
      }
      else
      {
         plugin.setPlayerKilogramsCaught(new ConcurrentHashMap<>());
         sender.sendMessage(Messages.RESET_LEADERBOARD_MESSAGE.getMessage());
      }
      return true;
   }

   private boolean handleWandCommand(CommandSender sender)
   {
      if (!sender.hasPermission(Permissions.PERMISSION_WAND.getPermission()))
      {
         sender.sendMessage(Messages.NO_PERMISSION_MESSAGE.getMessage());
      }
      else if (!(sender instanceof Player))
      {
         sender.sendMessage(Messages.ONLY_PLAYER_COMMAND.getMessage());
      }
      else
      {
         Player player = (Player) sender;

         ItemStack wand = new ItemStack(Material.STICK);
         ItemMeta meta = wand.getItemMeta();
         meta.setDisplayName(ChatColor.GREEN + "Pool Wand");
         wand.setItemMeta(meta);

         player.getInventory().addItem(wand);

         player.sendMessage(ChatColor.WHITE + "You received a " + ChatColor.GREEN +  "Pool Wand" + ChatColor.GREEN + "!");
      }
      return true;
   }

   private boolean handleSetPoolCommand(CommandSender sender, String[] args)
   {
      if (args.length != 2)
      {
         sender.sendMessage(Messages.INCOMPLETE_SETPOOL_COMMAND.getMessage());
      }
      else if (!sender.hasPermission(Permissions.PERMISSION_SETPOOL.getPermission()))
      {
         sender.sendMessage(Messages.NO_PERMISSION_MESSAGE.getMessage());
      }
      else if (!(sender instanceof Player))
      {
         sender.sendMessage(Messages.ONLY_PLAYER_COMMAND.getMessage());
      }
      else
      {
         Player player = (Player)sender;
         Selection selection = this.plugin.getSelection(player);

         if (selection != null && selection.getPos1() != null && selection.getPos2() != null)
         {
            this.plugin.setPool(args[1], selection.getPos1(), selection.getPos2());
            sender.sendMessage(ChatColor.GREEN + "Fishing pool '" + args[1] + "' set!");
            this.plugin.clearSelection(player);
         }
         else
         {
            sender.sendMessage(Messages.EXACTLY_TWOPOS_COMMAND.getMessage());
         }
      }
      return true;
   }

   private boolean handleReloadCommand(CommandSender sender)
   {
      if (!sender.hasPermission(Permissions.PERMISSION_RELOAD.getPermission()))
      {
         sender.sendMessage(Messages.NO_PERMISSION_MESSAGE.getMessage());
      }
      else
      {
         this.plugin.loadConfig();
         sender.sendMessage(Messages.CONFIG_RELOADED_MESSAGE.getMessage());
      }
      return true;
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
   {
      List<String> completions = new ArrayList<>();

      if (args.length == 1)
      {
         completions.addAll(Arrays.asList("wand", "setpool", "reload", "resetBiggestFish", "resetKilograms", "forcestart", "forcestop"));
      }
      else if (args.length == 2 && args[0].equalsIgnoreCase("forcestart"))
      {
         completions.addAll(this.plugin.getPoolNames());
      }
      else if (args.length == 2 && args[0].equalsIgnoreCase("forcestop"))
      {
         completions.addAll(this.plugin.getPoolNames());
      }

      return completions.stream().filter((completion) -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
   }
}
