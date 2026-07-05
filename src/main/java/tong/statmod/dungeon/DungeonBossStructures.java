package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import tong.statmod.STATMod;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mission M6 — Import des <b>arènes de boss</b> d'autres mods (2026-07-05).
 *
 * <p>Quand un boss d'étage vient d'un mod qui fournit sa propre <b>structure d'arène</b> (NBT dans
 * son datapack), on la charge et on la pose <b>centrée sur l'île</b> — le boss combat ainsi dans sa
 * vraie arène plutôt que dans l'arène générique. Actuellement : <b>Bosses of Mass Destruction</b>
 * (Gauntlet, Obsidilith, Lich). Résolution douce : si le mod/structure est absent, on retombe sur
 * {@link DungeonBossArenaFloor}.
 *
 * <p><b>yOffset</b> : décalage vertical de pose, à affiner en jeu si le sol de l'arène ne tombe pas
 * au niveau de l'île (une seule constante par structure à ajuster).
 */
public final class DungeonBossStructures {

    /** Une arène importée : structure principale + éventuelle pièce empilée au-dessus (tour). */
    private record ArenaDef(String mainPath, int yOffset, String topPath) {}

    /** Boss id (roster) → arène importée. */
    private static final Map<String, ArenaDef> ARENAS = Map.of(
            "bosses_of_mass_destruction:gauntlet",
            new ArenaDef("bosses_of_mass_destruction:gauntlet_arena/base", 0, null),
            "bosses_of_mass_destruction:obsidilith",
            new ArenaDef("bosses_of_mass_destruction:obsidilith_arena/base", 0, null),
            "bosses_of_mass_destruction:lich",
            new ArenaDef("bosses_of_mass_destruction:lich_tower/bottom", 0,
                    "bosses_of_mass_destruction:lich_tower/top")
    );

    private DungeonBossStructures() {}

    /** Id du premier boss du roster pour {@code floor}. */
    private static String bossIdFor(int floor) {
        List<DungeonBossRoster.BossEntry> roster = DungeonBossRoster.forFloor(floor);
        if (roster.isEmpty()) return "";
        String id = roster.get(0).entityId();
        int comma = id.indexOf(',');
        return comma >= 0 ? id.substring(0, comma) : id;
    }

    /** {@code true} si l'étage a une arène importée disponible (mod + structure présents). */
    public static boolean hasArena(ServerLevel lv, int floor) {
        ArenaDef def = ARENAS.get(bossIdFor(floor));
        return def != null && lv.getStructureManager().get(rl(def.mainPath())).isPresent();
    }

    /** {@code true} si l'étage MAPPE une arène importée (sans vérifier le chargement — sans level). */
    public static boolean hasArenaDef(int floor) {
        return ARENAS.containsKey(bossIdFor(floor));
    }

    /**
     * Pose l'arène importée centrée sur {@code islandCenter} si le boss de l'étage en a une.
     * Retourne le centre monde où poser l'autel/spawn (sol de l'arène), ou vide → arène générique.
     */
    public static Optional<BlockPos> tryPlace(ServerLevel lv, BlockPos islandCenter, int floor) {
        ArenaDef def = ARENAS.get(bossIdFor(floor));
        if (def == null) return Optional.empty();

        Optional<StructureTemplate> main = lv.getStructureManager().get(rl(def.mainPath()));
        if (main.isEmpty()) return Optional.empty();

        StructureTemplate tpl = main.get();
        Vec3i size = tpl.getSize();
        BlockPos corner = islandCenter.offset(-size.getX() / 2, def.yOffset(), -size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        boolean ok = tpl.placeInWorld(lv, corner, corner, settings, lv.getRandom(), 2);
        if (!ok) return Optional.empty();

        // Tour à deux pièces (Lich) : empile la partie haute au-dessus de la base.
        if (def.topPath() != null) {
            lv.getStructureManager().get(rl(def.topPath())).ifPresent(top -> {
                BlockPos topCorner = corner.offset(0, size.getY(), 0);
                top.placeInWorld(lv, topCorner, topCorner, new StructurePlaceSettings(), lv.getRandom(), 2);
            });
        }

        STATMod.LOGGER.info("[TrialDungeon] Arène importée '{}' posée à l'étage {} (taille {}×{}×{})",
                def.mainPath(), floor, size.getX(), size.getY(), size.getZ());
        // Sol de l'arène ≈ niveau de l'île + yOffset → autel/spawn juste au-dessus.
        return Optional.of(islandCenter.offset(0, def.yOffset() + 1, 0));
    }

    private static ResourceLocation rl(String id) {
        return ResourceLocation.parse(id);
    }
}
