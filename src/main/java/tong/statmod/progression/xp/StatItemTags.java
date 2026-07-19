package tong.statmod.progression.xp;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import tong.statmod.StatMod;

public final class StatItemTags {
    public static final TagKey<Item> HEAVY_WEAPONS = create("heavy_weapons");
    public static final TagKey<Item> BLADE_WEAPONS = create("blade_weapons");
    public static final TagKey<Item> PRECISION_WEAPONS = create("precision_weapons");
    public static final TagKey<Item> FORGEABLE_EQUIPMENT = create("forgeable_equipment");

    private StatItemTags() {
    }

    private static TagKey<Item> create(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(StatMod.MOD_ID, path));
    }
}
