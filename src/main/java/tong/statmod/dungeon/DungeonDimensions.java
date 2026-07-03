package tong.statmod.dungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import tong.statmod.STATMod;

/**
 * Mission M6 — Phase α (Trial Dungeon Dimension).
 *
 * <p>Dimension {@code statmod:trial_dungeon} : un void absolu dans lequel des îles volantes
 * seront générées à la volée par {@code IslandGenerator} lorsque le joueur téléporte sur un
 * étage pour la première fois. Aucun terrain, aucun bedrock — seulement les îles.
 *
 * <p>La dimension est déclarée entièrement en datapack (dimension.json + dimension_type.json).
 * Cette classe ne fait qu'exposer les {@link ResourceKey} nécessaires pour :
 * <ul>
 *   <li>téléporter les joueurs via {@code MinecraftServer.getLevel(TRIAL_DUNGEON)},</li>
 *   <li>tester la dimension côté serveur (event listeners qui filtrent par dimension).</li>
 * </ul>
 */
public final class DungeonDimensions {

    /** Clé de la dimension elle-même (résolvable en {@code ServerLevel}). */
    public static final ResourceKey<Level> TRIAL_DUNGEON = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "trial_dungeon"));

    /** Clé du dimension_type associé (rendering, height, ambient_light). */
    public static final ResourceKey<DimensionType> TRIAL_DUNGEON_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "trial_dungeon"));

    private DungeonDimensions() {}
}
