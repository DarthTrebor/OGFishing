package org.OGFishing.Models;

import org.OGFishing.Fishing;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomRods
{
    List<ShapedRecipe> recipes;
    private final Fishing plugin;

    public CustomRods(Fishing plugin)
    {
        this.plugin = plugin;
    }

    public CustomRods loadRecipes()
    {
        recipes = new ArrayList<>();
        recipes.add(createRobertsFishingRod());

        return this;
    }

    public ShapedRecipe createRobertsFishingRod()
    {
        ItemStack fishingRod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = fishingRod.getItemMeta();
        if (meta != null)
        {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Gilgamesh's Fishing Rod");
            meta.setLore(Arrays.asList(
                    "",
                    "§bFish Catcher I",
                    "§bLuck of the Sea IV",
                    "",
                    "§7Catches fishes §c2x§7 faster!"
            ));
            fishingRod.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "gilgameshs_fishing_rod");
        ShapedRecipe recipe = new ShapedRecipe(key, fishingRod);
        recipe.shape("SCS", "CCC", "SCS");
        recipe.setIngredient('C', Material.PUFFERFISH);
        recipe.setIngredient('S', Material.STICK);
        
        return recipe;
    }

    public List<ShapedRecipe> getRecipes()
    {
        return recipes;
    }
}
