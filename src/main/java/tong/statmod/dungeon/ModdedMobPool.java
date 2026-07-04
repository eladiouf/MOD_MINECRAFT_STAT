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

        return modded;
    }

    private static void addSLUMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("slu")) return;

        switch(tier) {
            case EARLY -> {
                addIfAvailable(pool, "slu:hollow");
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
                addIfAvailable(pool, "slu:ghost_samurai");
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

    /** Ajoute un EntityType si le mod est chargé et le mob existe dans le registre. */
    private static void addIfAvailable(List<EntityType<?>> pool, String entityId) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(entityId);
            if (id == null) return;

            // Vérifier que l'EntityType existe
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                STATMod.LOGGER.debug("[TrialDungeon] Modded mob not found in registry: {}", entityId);
                return;
            }

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type != null) {
                pool.add(type);
                STATMod.LOGGER.debug("[TrialDungeon] Added modded mob: {}", entityId);
            }
        } catch (Exception e) {
            // Ignorer les erreurs de chargement de mobs moddés
            STATMod.LOGGER.debug("[TrialDungeon] Failed to load modded mob {}: {}", entityId, e.getMessage());
        }
    }

    /**
     * Combine les pools vanilla et moddés pour le tier donné.
     * Garantit que les mobs moddés ne représentent pas plus de 50% du pool total.
     */
    public static List<EntityType<?>> getCombinedPool(FloorPalette tier) {
        List<EntityType<?>> vanilla = DungeonMasterpiece.mobPool(tier);
        List<EntityType<?>> modded = getModdedMobs(tier);

        // Si pas de mobs moddés, retourner vanilla pur
        if (modded.isEmpty()) return vanilla;

        // Combiner en garantissant max 50% de mobs moddés
        int vanillaCount = vanilla.size();
        int moddedAllowed = vanillaCount; // 50% max

        if (modded.size() <= moddedAllowed) {
            // Tous les mods rentrent
            List<EntityType<?>> combined = new ArrayList<>(vanilla);
            combined.addAll(modded);
            return combined;
        } else {
            // Sélection aléatoire de mobs moddés
            List<EntityType<?>> combined = new ArrayList<>(vanilla);
            java.util.Collections.shuffle(modded);
            combined.addAll(modded.subList(0, moddedAllowed));
            return combined;
        }
    }
}