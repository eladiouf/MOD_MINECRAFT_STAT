package tong.statmod.dungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import tong.statmod.StatMod;

public final class DungeonDimensions {

    public static final ResourceKey<Level> TRIAL_DUNGEON = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(StatMod.MOD_ID, "trial_dungeon"));

    public static final ResourceKey<DimensionType> TRIAL_DUNGEON_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation(StatMod.MOD_ID, "trial_dungeon"));

    private DungeonDimensions() {}
}
