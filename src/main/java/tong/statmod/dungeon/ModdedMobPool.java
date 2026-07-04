package tong.statmod.dungeon;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;

/**
 * Mission M6 — Mobs moddés contrôlés dans le donjon.
 *
 * <p>Détecte les mods disponibles et ajoute certains mobs tiers au pool vanilla
 * pour une expérience plus variée tout en gardant le contrôle.
 *
 * <p><b>Mods supportés</b> : SLU (modId réel {@code slu}, boss exclus — réservés au roster
 * {@link DungeonBossRoster}), Iron's Spellbooks (casters/hostiles, hors boss). Overgeared n'a
 * aucune entité de combat (juste des flèches de forge) donc n'est pas intégré ici.
 *
 * <p><b>Vérifié depuis les jars réels</b> (2026-07-04) : les IDs précédents utilisaient un modId
 * inventé ({@code souls_like} au lieu de {@code slu}) et des noms d'entités qui n'existent pas
 * dans les jars → {@code getModdedMobs} retournait toujours une liste vide, d'où l'absence
 * totale de mobs moddés en jeu malgré le code présent.
 */
public final class ModdedMobPool {

    private ModdedMobPool() {}

    /**
     * Retourne les mobs moddés disponibles pour le tier donné.
     * Les mobs sont sélectionnés selon la difficulté et la disponibilité du mod.
     */
    public static List<EntityType<?>> getModdedMobs(FloorPalette tier) {
        List<EntityType<?>> modded = new ArrayList<>();

        // SLU (Souls-Like Universe, modId="slu") — trash mobs contrôlés par difficulté
        addSLUMobs(modded, tier);

        // Iron's Spellbooks — casters et hostiles (hors boss)
        addIronsSpellbooksMobs(modded, tier);

        // Tensura — immense bestiaire (gobelins, orcs, daemons, colosses, élémentaux)
        addTensuraMobs(modded, tier);

        // Block Factory's Bosses — pirates, morts-vivants d'âme, gardiens de dragon
        addBfbMobs(modded, tier);

        return modded;
    }

    private static void addSLUMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("slu")) return;

        switch(tier) {
            case EARLY -> {
                addIfAvailable(pool, "slu:hollow");
                addIfAvailable(pool, "slu:armed_hollow");
                addIfAvailable(pool, "slu:thief");
                addIfAvailable(pool, "slu:hollow_soldier_sword");
                addIfAvailable(pool, "slu:hollow_soldier_spear");
                addIfAvailable(pool, "slu:twisted_souls");
            }
            case MID -> {
                addIfAvailable(pool, "slu:hollow_knight");
                addIfAvailable(pool, "slu:knight");
                addIfAvailable(pool, "slu:dark_spirit");
                addIfAvailable(pool, "slu:castle_guard");
                addIfAvailable(pool, "slu:dungeon_knight");
                addIfAvailable(pool, "slu:nightmare_knight");
                addIfAvailable(pool, "slu:ghost_samurai");
            }
            case LATE -> {
                addIfAvailable(pool, "slu:elite_knight");
                addIfAvailable(pool, "slu:noble_knight");
                addIfAvailable(pool, "slu:dark_knight");
                addIfAvailable(pool, "slu:ringed_knight");
                addIfAvailable(pool, "slu:temple_guard");
                addIfAvailable(pool, "slu:shadow_assassin");
                addIfAvailable(pool, "slu:mad_knight");
                addIfAvailable(pool, "slu:wither_skeleton_knight");
                addIfAvailable(pool, "slu:monster_successor");
                addIfAvailable(pool, "slu:clone_abyss_watcher");
            }
            case ABYSS -> {
                addIfAvailable(pool, "slu:monster_crucible_knight");
                addIfAvailable(pool, "slu:monster_crucible_knight_2");
                addIfAvailable(pool, "slu:monster_tower_knight");
                addIfAvailable(pool, "slu:monster_godrick_knight");
                addIfAvailable(pool, "slu:monster_godrick_soldier");
                addIfAvailable(pool, "slu:monster_blasphemy_knight");
                addIfAvailable(pool, "slu:monster_crusader");
                addIfAvailable(pool, "slu:magma_giant");
                addIfAvailable(pool, "slu:bad_omen_giant");
                addIfAvailable(pool, "slu:executor");
                addIfAvailable(pool, "slu:white_phantom");
                addIfAvailable(pool, "slu:havel");
            }
        }
    }

    private static void addIronsSpellbooksMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("irons_spellbooks")) return;

        switch(tier) {
            case EARLY -> {
                // Les casters Iron's Spellbooks sont trop dangereux pour l'étage 1-9.
            }
            case MID -> {
                addIfAvailable(pool, "irons_spellbooks:cultist");
                addIfAvailable(pool, "irons_spellbooks:catacombs_zombie");
                addIfAvailable(pool, "irons_spellbooks:ice_spider");
            }
            case LATE -> {
                addIfAvailable(pool, "irons_spellbooks:pyromancer");
                addIfAvailable(pool, "irons_spellbooks:cryomancer");
                addIfAvailable(pool, "irons_spellbooks:magehunter_vindicator");
                addIfAvailable(pool, "irons_spellbooks:apothecarist");
            }
            case ABYSS -> {
                addIfAvailable(pool, "irons_spellbooks:archevoker");
                addIfAvailable(pool, "irons_spellbooks:necromancer");
                addIfAvailable(pool, "irons_spellbooks:priest");
            }
        }
    }

    /** Mobs Tensura par tier (immense bestiaire : gobelins → daemons → colosses). */
    private static void addTensuraMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("tensura")) return;

        switch (tier) {
            case EARLY -> {
                addIfAvailable(pool, "tensura:goblin");
                addIfAvailable(pool, "tensura:direwolf");
                addIfAvailable(pool, "tensura:giant_bat");
                addIfAvailable(pool, "tensura:horned_rabbit");
            }
            case MID -> {
                addIfAvailable(pool, "tensura:orc");
                addIfAvailable(pool, "tensura:lizardman");
                addIfAvailable(pool, "tensura:black_spider");
                addIfAvailable(pool, "tensura:army_wasp");
                addIfAvailable(pool, "tensura:giant_bear");
            }
            case LATE -> {
                addIfAvailable(pool, "tensura:lesser_daemon");
                addIfAvailable(pool, "tensura:knight_spider");
                addIfAvailable(pool, "tensura:basilisk");
                addIfAvailable(pool, "tensura:barghest");
                addIfAvailable(pool, "tensura:bone_golem");
            }
            case ABYSS -> {
                addIfAvailable(pool, "tensura:greater_daemon");
                addIfAvailable(pool, "tensura:arch_daemon");
                addIfAvailable(pool, "tensura:evil_centipede");
                addIfAvailable(pool, "tensura:elemental_colossus");
                addIfAvailable(pool, "tensura:charybdis");
            }
        }
    }

    /** Mobs Block Factory's Bosses (pirates, soul skeletons, dragon guards) — tiers hauts. */
    private static void addBfbMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("block_factorys_bosses")) return;

        switch (tier) {
            case EARLY, MID -> {
                addIfAvailable(pool, "block_factorys_bosses:soul_skeleton");
                addIfAvailable(pool, "block_factorys_bosses:crossbow_pirate");
                addIfAvailable(pool, "block_factorys_bosses:frozen_skeleton");
            }
            case LATE -> {
                addIfAvailable(pool, "block_factorys_bosses:pirate_rook");
                addIfAvailable(pool, "block_factorys_bosses:soul_knight_wither_skeleton");
                addIfAvailable(pool, "block_factorys_bosses:flaming_skeleton_guard_sword");
            }
            case ABYSS -> {
                addIfAvailable(pool, "block_factorys_bosses:pirate_captain");
                addIfAvailable(pool, "block_factorys_bosses:dragon_guard_sword");
                addIfAvailable(pool, "block_factorys_bosses:underworld_knight");
            }
        }
    }

    /** Ajoute un EntityType si le mod est chargé et le mob existe dans le registre. */
    private static void addIfAvailable(List<EntityType<?>> pool, String entityId) {
        EntityType<?> type = resolve(entityId);
        if (type != null) {
            pool.add(type);
            STATMod.LOGGER.debug("[TrialDungeon] Added modded mob: {}", entityId);
        }
    }

    /** Résout un EntityType par son ID, ou {@code null} si absent (mod non installé, ID inconnu). */
    public static EntityType<?> resolve(String entityId) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(entityId);
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
            return BuiltInRegistries.ENTITY_TYPE.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Résout la liste d'IDs en EntityTypes présents (les absents sont ignorés). Utilisé par les
     * étages à thème ({@link DungeonTheme}).
     */
    public static List<EntityType<?>> resolveAll(List<String> ids) {
        List<EntityType<?>> out = new ArrayList<>();
        for (String id : ids) {
            EntityType<?> t = resolve(id);
            if (t != null) out.add(t);
        }
        return out;
    }

    /**
     * Pool de mobs de la vague pour un étage.
     *
     * <p>Étage à <b>thème</b> ({@link DungeonTheme#forFloor}) : renvoie UNIQUEMENT les mobs du thème
     * (horde d'orcs pure, etc.) — le mini-boss est spawné séparément par {@link DungeonMobSpawner}.
     * Si le thème n'a aucun mob disponible (mod absent), on retombe sur le pool mixte normal.
     *
     * <p>Étage normal : mélange vanilla + moddés (Tensura/SLU/Iron's Spellbooks), plafonné à 50 %
     * de moddés.
     */
    public static List<EntityType<?>> getCombinedPool(int floor) {
        DungeonTheme theme = DungeonTheme.forFloor(floor);
        if (theme != null) {
            List<EntityType<?>> themed = resolveAll(theme.addIds());
            if (!themed.isEmpty()) return themed; // pool 100 % thématique
        }

        FloorPalette tier = FloorPalette.forFloor(floor);
        List<EntityType<?>> vanilla = DungeonMasterpiece.mobPool(tier);
        List<EntityType<?>> modded = getModdedMobs(tier);
        if (modded.isEmpty()) return vanilla;

        int moddedAllowed = vanilla.size(); // 50 % max
        List<EntityType<?>> combined = new ArrayList<>(vanilla);
        if (modded.size() <= moddedAllowed) {
            combined.addAll(modded);
        } else {
            java.util.Collections.shuffle(modded);
            combined.addAll(modded.subList(0, moddedAllowed));
        }
        return combined;
    }
}