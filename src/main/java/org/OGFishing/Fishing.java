package org.OGFishing;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.SkinTrait;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.util.concurrent.AtomicDouble;

import org.OGFishing.Commands.Commands;
import org.OGFishing.Models.CustomRods;
import org.OGFishing.Models.FishingPool;
import org.OGFishing.Models.Reward;
import org.OGFishing.Models.Selection;

public class Fishing extends JavaPlugin implements Listener
{
   private Map<String, FishingPool> fishingPools;
   private Map<UUID, Selection> playerSelections;
   private Map<Player, FishingGame> activeGames;
   private ConcurrentHashMap<UUID, AtomicDouble> playerKilogramsCaught;
   private Map<String, Reward> rewards;
   private String fishEscapeMessage;
   private String fishCaughtMessage;
   private boolean randomSpeed;
   private int fixedSpeed;
   private int minSpeed;
   private int maxSpeed;
   private String barTexture;
   private String gameDisplayType;
   Economy economy;
   private AtomicDouble biggestFishCaught;

   BukkitRunnable leaderboardUpdater = null;

   private double multiplier;

    public void updateLeaderboardHologram()
   {
       if (playerKilogramsCaught == null) return;
       List<Map.Entry<UUID, AtomicDouble>> sortedList = new ArrayList<>(playerKilogramsCaught.entrySet());
       sortedList.sort((a, b) -> Double.compare(b.getValue().get(), a.getValue().get()));

       List<Map.Entry<UUID, AtomicDouble>> top5Fishermen = sortedList.stream().limit(5).toList();

       String leaderboardName = "fishing_hologram";
       Hologram hologram = DHAPI.getHologram(leaderboardName);

       if (hologram == null) return;

       int rank = 1;
       DHAPI.setHologramLine(hologram,0, ChatColor.YELLOW + "Best 5 fishermen");
       for (Map.Entry<UUID, AtomicDouble> entry : top5Fishermen)
       {
           String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            double kilograms = entry.getValue().get();
            DHAPI.setHologramLine(hologram, rank, "§e#" + (rank++) + " §b" + playerName + " §7- " + String.format("%.2f", kilograms) + " kg");
        }

        updateBestFisherman();
   }

   public void allowFishingForPool(Player player, String pool)
   {
      if (fishingPools.containsKey(pool))
      {
         fishingPools.get(pool).setAllowFishing(true);
         leaderboardUpdater = new BukkitRunnable() {
            public void run()
            {
               updateLeaderboardHologram();
            }
         };
         leaderboardUpdater.runTaskTimer(this, 0L, 200L); // 200L = 10 seconds (20 ticks per second)
         Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(this.getConfig().getString("event_forcestart_message"))));
      }
      else
      {
         player.sendMessage(Messages.UNDEFINED_POOL_MESSAGE.getMessage());
      }
   }

   public void disableFishingForPool(Player player, String pool)
   {
      if (fishingPools.containsKey(pool))
      {
         fishingPools.get(pool).setAllowFishing(false);
         if (leaderboardUpdater != null)
         {
            leaderboardUpdater.cancel();
         }
         Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(this.getConfig().getString("event_forcestop_message"))));
      }
      else
      {
         player.sendMessage(Messages.UNDEFINED_POOL_MESSAGE.getMessage());
      }
   }

   public void setBiggestFishCaught(AtomicDouble biggestFishCaught)
   {
      this.biggestFishCaught = biggestFishCaught;
   }

   public void setPlayerKilogramsCaught(ConcurrentHashMap<UUID, AtomicDouble> playerKilogramsCaught)
   {
      this.playerKilogramsCaught = playerKilogramsCaught;
   }

   public void onEnable()
   {
      this.saveDefaultConfig();
      this.loadConfig();
      this.getServer().getPluginManager().registerEvents(this, this);

      Commands commands = new Commands(this);
      Objects.requireNonNull(this.getCommand("ogfishing")).setExecutor(commands);
      Objects.requireNonNull(this.getCommand("ogfishing")).setTabCompleter(commands);

      this.biggestFishCaught = new AtomicDouble(0);
      this.economy = Objects.requireNonNull(getServer().getServicesManager().getRegistration(Economy.class)).getProvider();
      this.activeGames = new HashMap<>();
      this.playerSelections = new HashMap<>();
      this.multiplier = this.getConfig().getDouble("pools.multiplier");

      new CustomRods(this).loadRecipes().getRecipes().forEach(Bukkit::addRecipe);
   }

   public void onDisable()
   {
       for (FishingGame game : this.activeGames.values())
       {
           game.end();
       }
   }

   public void loadConfig()
   {
      this.reloadConfig();
      FileConfiguration config = this.getConfig();

      this.fishingPools = new HashMap<>();
      this.gameDisplayType = this.getConfig().getString("game_display.type", "subtitle").toLowerCase();
      this.fishEscapeMessage = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(config.getString("messages.fish_escape")));
      this.randomSpeed = config.getBoolean("settings.random_speed", false);
      this.fixedSpeed = config.getInt("settings.speed", 1);
      this.minSpeed = config.getInt("settings.min_speed", 1);
      this.maxSpeed = config.getInt("settings.max_speed", 4);
      this.barTexture = config.getString("settings.bar_texture", "█");

      ConfigurationSection poolsSection = config.getConfigurationSection("pools");
      if (poolsSection != null)
      {
          for (String poolName : poolsSection.getKeys(false))
          {
              ConfigurationSection poolSection = poolsSection.getConfigurationSection(poolName);
              if (poolSection != null)
              {
                  Location pos1 = this.locationFromString(Objects.requireNonNull(poolSection.getString("pos1")));
                  Location pos2 = this.locationFromString(Objects.requireNonNull(poolSection.getString("pos2")));
                  Map<String, Reward> rewards = this.loadRewards(poolSection.getConfigurationSection("rewards"));
                  this.fishingPools.put(poolName, new FishingPool(pos1, pos2, rewards, poolSection.getBoolean("allowFishing")));
              }
          }
      }
   }

   private Map<String, Reward> loadRewards(ConfigurationSection rewardsSection)
   {
      Map<String, Reward> rewards = new HashMap<>();
      if (rewardsSection != null) {
          for (String key : rewardsSection.getKeys(false))
          {
              ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(key);
              if (rewardSection != null)
              {
                  rewards.put(
                          key,
                          new Reward(
                              rewardSection.getString("message", "You won a reward!").replace('&', '§'),
                              rewardSection.getDouble("chance", 0.1D),
                              rewardSection.getString("kilograms"),
                              rewardSection.getString("name"),
                              rewardSection.getString("type")
                          )
                  );
              }
          }
      }
      return rewards;
   }

   @EventHandler
   public void onPlayerFish(PlayerFishEvent event)
   {
      if (event.getState() == State.CAUGHT_FISH)
      {
         Player player = event.getPlayer();
         Location hookLocation = event.getHook().getLocation();
         FishingPool pool = this.getPoolAtLocation(hookLocation);
         if (pool != null && pool.getAllowFishing())
         {
            this.getLogger().info(player.getName() + " caught a fish caught in pool");
            event.setCancelled(true);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> this.startFishingGame(player, pool), 3L);
         }
      }
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event)
   {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();

      if (item != null && item.getType() == Material.STICK && item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).getDisplayName().equals(ChatColor.GREEN + "Pool Wand"))
      {
         event.setCancelled(true);
         Selection selection = this.playerSelections.computeIfAbsent(player.getUniqueId(), (k) -> new Selection());
         if (event.getAction() == Action.LEFT_CLICK_BLOCK)
         {
            selection.setPos1(Objects.requireNonNull(event.getClickedBlock()).getLocation());
            player.sendMessage(Messages.POSITIONONE_SET_MESSAGE.getMessage());
         }
         else if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
         {
            selection.setPos2(Objects.requireNonNull(event.getClickedBlock()).getLocation());
            player.sendMessage(Messages.POSITIONONE_SET_MESSAGE.getMessage());
         }
      }

      FishingGame game = this.activeGames.get(player);
      if (game != null)
      {
         game.onInteract();
      }
   }

   @EventHandler
   public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
   {
      if (CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked()))
      {
         NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
         if (Objects.equals(npc.getName(), "Gilgamesh"))
         {
            Inventory gui = org.bukkit.Bukkit.createInventory(null, 27, "Do you want to sell your fish?");

            ItemStack redWool = new ItemStack(Material.RED_CONCRETE);
            ItemMeta redMeta = redWool.getItemMeta();
            if (redMeta != null)
            {
               redMeta.setDisplayName(ChatColor.RED + "Back");
               redMeta.getPersistentDataContainer().set(new NamespacedKey("ogfishing", "back_button"), PersistentDataType.STRING, "ogfishing_redwool");
               redWool.setItemMeta(redMeta);
            }

            ItemStack greenWool = new ItemStack(Material.GREEN_CONCRETE);
            ItemMeta greenMeta = greenWool.getItemMeta();
            if (greenMeta != null)
            {
               greenMeta.setDisplayName(ChatColor.GREEN + "Sell all fish");
               greenMeta.getPersistentDataContainer().set(new NamespacedKey("ogfishing", "sell_button"), PersistentDataType.STRING, "ogfishing_greenwool");
               greenWool.setItemMeta(greenMeta);
            }

            ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta glassMeta = blackGlass.getItemMeta();
            if (glassMeta != null)
            {
               glassMeta.getPersistentDataContainer().set(new NamespacedKey("ogfishing", "glass"), PersistentDataType.STRING, "ogfishing_glass");
               blackGlass.setItemMeta(glassMeta);
            }

            gui.setItem(16, redWool);
            gui.setItem(10, greenWool);

            for (int i = 0; i < gui.getSize(); i++)
            {
               if (gui.getItem(i) == null)
               {
                  gui.setItem(i, blackGlass);
               }
            }

            event.getPlayer().openInventory(gui);

            event.setCancelled(true);
         }
      }
   }

   private FishingPool getPoolAtLocation(Location location)
   {
      Iterator var2 = this.fishingPools.values().iterator();

      FishingPool pool;
      do
      {
         if (!var2.hasNext())
         {
            return null;
         }

         pool = (FishingPool)var2.next();
      }
      while(!pool.isInPool(location));

      return pool;
   }

   private void startFishingGame(Player player, FishingPool pool)
   {
      if (this.activeGames.containsKey(player))
      {
         this.activeGames.get(player).end();
      }

      FishingGame game = new FishingGame(player, pool);
      this.activeGames.put(player, game);

      game.start();
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getView().getTitle().equals("Do you want to sell your fish?"))
      {
         event.setCancelled(true);

         ItemStack clickedItem = event.getCurrentItem();
         if (clickedItem != null && clickedItem.hasItemMeta()) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null)
            {
               if (meta.getPersistentDataContainer().has(new NamespacedKey("ogfishing", "back_button"), PersistentDataType.STRING))
               {
                  event.getWhoClicked().closeInventory();
               }
               else if (meta.getPersistentDataContainer().has(new NamespacedKey("ogfishing", "sell_button"), PersistentDataType.STRING))
               {
                  Inventory inventory = event.getWhoClicked().getInventory();
                  NamespacedKey kgKey = new NamespacedKey("ogfishing", "kg_size");

                  double totalKg = 0;

                  for (ItemStack item : inventory.getContents()) {
                     if (item == null || !item.hasItemMeta()) continue;

                     ItemMeta itemMeta = item.getItemMeta();
                     if (itemMeta != null && itemMeta.getPersistentDataContainer().has(kgKey, PersistentDataType.DOUBLE)) {
                        double kg = itemMeta.getPersistentDataContainer().get(kgKey, PersistentDataType.DOUBLE);
                        int amount = item.getAmount();
                        event.getWhoClicked().getInventory().remove(item);
                        totalKg += kg * amount;
                     }
                  }
                  economy.depositPlayer(event.getWhoClicked().getName(), totalKg * multiplier);
                  if (totalKg > 0)
                  {
                     event.getWhoClicked().sendMessage(ChatColor.GREEN + "You sold: " + ChatColor.GOLD + String.format("%.2f", totalKg) + " kg of fish");
                     event.getWhoClicked().sendMessage(ChatColor.WHITE + "You received " + ChatColor.GREEN + String.format("%.2f", (totalKg * multiplier)) + "$");
                  }
                  else
                  {
                     event.getWhoClicked().sendMessage(ChatColor.RED + "You don't have any fish to sell");
                  }
                  event.getWhoClicked().closeInventory();
               }
            }
         }
      }
   }

   public static double getRandomDoubleBetween(double min, double max)
   {
      Random random = new Random();
      double value = min + (max - min) * random.nextDouble();
      return Math.round(value * 100.0) / 100.0; // Rounds to 2 decimal places
   }

   private void giveReward(Player player, FishingPool pool)
   {
      Map<String, Reward> rewards = pool.getRewards();
      if (rewards != null && !rewards.isEmpty())
      {
         double random = Math.random();
         double cumulativeChance = 0.0D;
         Iterator var8 = rewards.entrySet().iterator();

         Reward reward;
         do
         {
            if (!var8.hasNext())
            {
               return;
            }
            Entry<String, Reward> entry = (Entry)var8.next();
            reward = entry.getValue();
            cumulativeChance += reward.getChance();
         }
         while(!(random <= cumulativeChance));

         player.sendMessage(reward.getMessage());

         ItemStack fish = new ItemStack(FishType.getMaterialFromStr(reward.getFishType()).getMaterial());

         ItemMeta fishMeta = fish.getItemMeta();
         if (fishMeta != null)
         {
            fishMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', reward.getName()));
            String kilograms = reward.getSize().replace("[", "").replace("]", "");
            double kg = getRandomDoubleBetween(
                    Double.parseDouble(kilograms.split("\\.\\.")[0]),
                    Double.parseDouble(kilograms.split("\\.\\.")[1])
            );

            if (kg > biggestFishCaught.get())
            {
               Bukkit.broadcastMessage(ChatColor.RED + player.getName() + ChatColor.WHITE +" caught " + ChatColor.translateAlternateColorCodes('&', reward.getName()) + ChatColor.WHITE + " a " + kg + " fish!");
               try {
                  CitizensAPI.getNPCRegistry().forEach(npc -> {
                     if ("biggest_fish".equals(npc.data().get("ogfishing")))
                     {
                        SkinnableEntity skinnable = (SkinnableEntity) npc.getEntity();
                        skinnable.setSkinName(player.getName(), true);
                        npc.getOrAddTrait(SkinTrait.class).setShouldUpdateSkins(true);
                        npc.setName(player.getName());
                     }
                  });
               }
               catch (Exception e)
               {
                  e.printStackTrace();
               }
               biggestFishCaught.set(kg);
            }

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + " ");
            lore.add(ChatColor.GOLD + "Greutate: " + ChatColor.WHITE + kg + " kg");
            lore.add(ChatColor.WHITE + " ");
            lore.add(ChatColor.WHITE + "Acest pește este pescuit din lacul Sf. Ana.");
            fishMeta.setLore(lore);

            if (playerKilogramsCaught == null)
            {
               playerKilogramsCaught = new ConcurrentHashMap<>();
            }

            if (playerKilogramsCaught.containsKey(player.getUniqueId()))
            {
               AtomicDouble kgCaught = playerKilogramsCaught.get(player.getUniqueId());
               playerKilogramsCaught.get(player.getUniqueId()).set(kgCaught.get() + kg);
            }
            else
            {
               playerKilogramsCaught.put(player.getUniqueId(), new AtomicDouble(kg));
            }

            NamespacedKey key = new NamespacedKey("ogfishing", "kg_size");
            fishMeta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, kg);

            fish.setItemMeta(fishMeta);
         }
         else
         {
            player.sendMessage("Null meta");
         }
         player.getInventory().addItem(fish);
      }
      else
      {
         this.getLogger().warning("No rewards found for pool");
      }
   }

   public void updateBestFisherman()
   {
      Map.Entry<UUID, AtomicDouble> topPlayer = playerKilogramsCaught.entrySet().stream()
              .max(Comparator.comparingDouble(entry -> entry.getValue().get()))
              .orElse(null);

      if (topPlayer == null) return;

      CitizensAPI.getNPCRegistry().forEach(npc -> {
         if ("kg_caught".equals(npc.data().get("ogfishing")))
         {
            String playerName = Bukkit.getOfflinePlayer(topPlayer.getKey()).getName();
            SkinnableEntity skinnable = (SkinnableEntity) npc.getEntity();
            skinnable.setSkinName(playerName, true);
            npc.getOrAddTrait(SkinTrait.class).setShouldUpdateSkins(true);
            npc.setName(playerName);
         }
      });
   }

   public void setPool(String name, Location pos1, Location pos2)
   {
      FishingPool pool = new FishingPool(pos1, pos2, new HashMap<>(), false);
      this.fishingPools.put(name, pool);
      FileConfiguration config = this.getConfig();
      String path = "pools." + name + ".";
      config.set(path + "pos1", this.locationToString(pos1));
      config.set(path + "pos2", this.locationToString(pos2));
      this.saveConfig();
   }

   public Selection getSelection(Player player)
   {
      return this.playerSelections.get(player.getUniqueId());
   }

   public void clearSelection(Player player)
   {
      this.playerSelections.remove(player.getUniqueId());
   }

   private String locationToString(Location loc)
   {
      return String.format("%.2f,%.2f,%.2f,%s", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
   }

   private Location locationFromString(String str)
   {
      String[] parts = str.split(",");
      World world = Bukkit.getWorld(parts[3]);
      return new Location(world, Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
   }

   public List<String> getPoolNames()
   {
      return new ArrayList(this.fishingPools.keySet());
   }

   private class FishingGame
   {
      private final Player player;
      private BossBar bossBar;
      private BukkitRunnable gameTask;
      private int position;
      private int count;
      private boolean increasing;
      private int gameSpeed;
      private final FishingPool pool;

      public FishingGame(Player player, FishingPool pool)
      {
         this.player = player;
         this.pool = pool;

         if (Fishing.this.gameDisplayType.equals("bossbar"))
         {
            this.bossBar = Bukkit.createBossBar(this.getBarText(), BarColor.GREEN, BarStyle.SOLID);
            this.bossBar.addPlayer(player);
         }

         if (Fishing.this.randomSpeed)
         {
            Random random = new Random();
            this.gameSpeed = random.nextInt(Fishing.this.maxSpeed - Fishing.this.minSpeed + 1) + Fishing.this.minSpeed;
         }
         else
         {
            this.gameSpeed = Fishing.this.fixedSpeed;
         }

      }

      public void start()
      {
         this.position = 0;
         this.increasing = true;

         this.gameTask = new BukkitRunnable() {
            public void run() {
               if (FishingGame.this.count >= 80)
               {
                  FishingGame.this.end();

                  if (Fishing.this.fishEscapeMessage != null && !Fishing.this.fishEscapeMessage.isEmpty())
                  {
                     FishingGame.this.player.sendMessage(Fishing.this.fishEscapeMessage);
                  }

                  FishingGame.this.player.playSound(FishingGame.this.player.getLocation(), Sound.ENTITY_FISH_SWIM, 1.0F, 1.0F);
               }

               if (FishingGame.this.increasing)
               {
                  FishingGame.this.position++;
                  FishingGame.this.count++;

                  if (FishingGame.this.position >= 20)
                  {
                     FishingGame.this.increasing = false;
                  }
               }
               else
               {
                  FishingGame.this.position--;
                  FishingGame.this.count++;

                  if (FishingGame.this.position <= 0)
                  {
                     FishingGame.this.increasing = true;
                  }
               }

               FishingGame.this.updateBar();
            }
         };
         this.gameTask.runTaskTimer(Fishing.this, 0L, this.gameSpeed);
      }

      public void onInteract()
      {
         if (this.position != 9 && this.position != 10 && this.position != 11)
         {
            this.end();
            if (Fishing.this.fishEscapeMessage != null && !Fishing.this.fishEscapeMessage.isEmpty())
            {
               this.player.sendMessage(Fishing.this.fishEscapeMessage);
            }

            this.player.playSound(this.player.getLocation(), Sound.ENTITY_FISH_SWIM, 1.0F, 1.0F);
         } else
         {
            this.end();
            Fishing.this.giveReward(this.player, this.pool);

            if (Fishing.this.fishCaughtMessage != null && !Fishing.this.fishCaughtMessage.isEmpty())
            {
               this.player.sendMessage(Fishing.this.fishCaughtMessage);
            }

            this.player.playSound(this.player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
         }

      }

      public void end()
      {
         if (this.gameTask != null) {
            this.gameTask.cancel();
         }

         this.player.getWorld().getEntities().stream()
                 .filter((entity) -> entity instanceof FishHook)
                 .map((entity) -> (FishHook)entity)
                 .filter((fishHook) -> fishHook.getShooter() instanceof Player)
                 .filter((fishHook) -> fishHook.getShooter().equals(this.player))
                 .forEach(Entity::remove);

         if (this.bossBar != null)
         {
            this.bossBar.removeAll();
         }

         if (Fishing.this.gameDisplayType.equals("subtitle"))
         {
            this.player.sendTitle("", "", 0, 1, 0);
         }

         Fishing.this.activeGames.remove(this.player);
      }

      private void updateBar()
      {
         String barText = this.getBarText();
         String barCode = Fishing.this.gameDisplayType;

         switch (barCode)
         {
            case "subtitle":
               this.player.sendTitle("", barText, 0, 20, 0);
               break;
            case "bossbar":
               if (this.bossBar != null) {
                  this.bossBar.setTitle(barText);
               }
               break;
            case "actionbar":
               this.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(barText));
         }
      }

      private String getBarText()
      {
         StringBuilder barText = new StringBuilder("§7");

         for(int i = 0; i < 21; ++i)
         {
            if (i == this.position)
            {
               barText.append("§a").append(Fishing.this.barTexture);
            }
            else if (i != 9 && i != 10 && i != 11)
            {
               barText.append("§7").append(Fishing.this.barTexture);
            }
            else
            {
               barText.append("§2").append(Fishing.this.barTexture);
            }
         }

         return barText.toString();
      }
   }
}
