package org.OGFishing;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public enum FishType {
    COOKED_COD(Material.COOKED_COD),
    COD(Material.COD),
    SALMON(Material.SALMON),
    COOKED_SALMON(Material.COOKED_SALMON),
    TROPICAL_FISH(Material.TROPICAL_FISH),
    PUFFERFISH(Material.PUFFERFISH);

    private final Material material;
    private static final Map<String, FishType> MATERIAL_MAP = new HashMap<>();

    static
    {
        for (FishType type : values())
        {
            MATERIAL_MAP.put(type.material.name(), type);
        }
    }

    FishType(Material material)
    {
        this.material = material;
    }

    public Material getMaterial()
    {
        return material;
    }

    public static FishType getMaterialFromStr(String material)
    {
        return MATERIAL_MAP.getOrDefault(material.toUpperCase(), null);
    }
}

