package tong.statmod.dungeon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.ModList;
import tong.statmod.StatMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pool de mobs moddés pour le Trial Dungeon (Forge 1.20.1).
 *
 * <p>Basé sur les mods RÉELLEMENT présents dans le pack.
 * Mods disponibles : slu, irons_spellbooks, epic_mobs, mutantmonsters,
 * mowziesmobs, bosses_of_mass_destruction et les aventuriers STAT Mod.
 *
 * <p>Mods RETIRÉS du pack : block_factorys_bosses, deeperdarker,
 * darkdoppelganger, born_in_chaos_v1, tensura, iceandfire.
 */
public final class ModdedMobPool {

    private ModdedMobPool() {}

    public static List<EntityType<?>> getModdedMobs(FloorPalette tier) {
        List<EntityType<?>> modded = new ArrayList<>();

        addDungeonMages(modded, tier);
        addSLUMobs(modded, tier);
        addIronsSpellbooksMobs(modded, tier);
        addEpicMobs(modded, tier);
        addMutantMobs(modded, tier);
        addMowzieMobs(modded, tier);

        return modded;
    }

    private static void addDungeonMages(List<EntityType<?>> pool, FloorPalette tier) {
        switch (tier) {
            case EARLY -> {
                addIfAvailable(pool, "statmod:adventurer");
                addIfAvailable(pool, "statmod:pyromancer_mob");
            }
            case MID -> {
                addIfAvailable(pool, "statmod:adventurer");
                addIfAvailable(pool, "statmod:pyromancer_mob");
                addIfAvailable(pool, "statmod:cryomancer_mob");
                addIfAvailable(pool, "statmod:electromancer_mob");
                addIfAvailable(pool, "statmod:cleric_mob");
            }
            case LATE -> {
                addIfAvailable(pool, "statmod:adventurer");
                addIfAvailable(pool, "statmod:pyromancer_mob");
                addIfAvailable(pool, "statmod:cryomancer_mob");
                addIfAvailable(pool, "statmod:electromancer_mob");
                addIfAvailable(pool, "statmod:wither_mage_mob");
                addIfAvailable(pool, "statmod:cleric_mob");
                addIfAvailable(pool, "statmod:hollow_witch_mob");
                addIfAvailable(pool, "statmod:mage_knight_mob");
            }
            case ABYSS -> {
                addIfAvailable(pool, "statmod:adventurer");
                addIfAvailable(pool, "statmod:pyromancer_mob");
                addIfAvailable(pool, "statmod:cryomancer_mob");
                addIfAvailable(pool, "statmod:electromancer_mob");
                addIfAvailable(pool, "statmod:wither_mage_mob");
                addIfAvailable(pool, "statmod:cleric_mob");
                addIfAvailable(pool, "statmod:hollow_witch_mob");
                addIfAvailable(pool, "statmod:mage_knight_mob");
                addIfAvailable(pool, "statmod:void_knight_mob");
            }
        }
    }

    private static void addSLUMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("slu")) return;

        switch (tier) {
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

        switch (tier) {
            case EARLY -> {}
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
                addIfAvailable(pool, "irons_spellbooks:cursed_armor_stand");
            }
            case ABYSS -> {
                addIfAvailable(pool, "irons_spellbooks:archevoker");
                addIfAvailable(pool, "irons_spellbooks:necromancer");
                addIfAvailable(pool, "irons_spellbooks:priest");
                addIfAvailable(pool, "irons_spellbooks:frozen_humanoid");
            }
        }
    }

    /** Epic Mobs — 20+ mobs de toutes difficultés (frost, bronze, radiant, tech). */
    private static void addEpicMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("epic_mobs")) return;

        switch (tier) {
            case EARLY -> {
                addIfAvailable(pool, "epic_mobs:bronze_guard");
                addIfAvailable(pool, "epic_mobs:bronze_spinner");
                addIfAvailable(pool, "epic_mobs:bronzer");
            }
            case MID -> {
                addIfAvailable(pool, "epic_mobs:frostguard");
                addIfAvailable(pool, "epic_mobs:frostrogue");
                addIfAvailable(pool, "epic_mobs:frost_knight");
                addIfAvailable(pool, "epic_mobs:frostsentinel");
                addIfAvailable(pool, "epic_mobs:frostspear");
                addIfAvailable(pool, "epic_mobs:radiant_guard");
                addIfAvailable(pool, "epic_mobs:radiant_archer");
            }
            case LATE -> {
                addIfAvailable(pool, "epic_mobs:frostgeneral");
                addIfAvailable(pool, "epic_mobs:radiant_smasher");
                addIfAvailable(pool, "epic_mobs:tech_knight");
                addIfAvailable(pool, "epic_mobs:infernal_eye");
                addIfAvailable(pool, "epic_mobs:hypo_fight");
                addIfAvailable(pool, "epic_mobs:frost_knight_reborn");
            }
            case ABYSS -> {
                addIfAvailable(pool, "epic_mobs:tech_knight");
                addIfAvailable(pool, "epic_mobs:the_knight");
                addIfAvailable(pool, "epic_mobs:infernal_eye");
                addIfAvailable(pool, "epic_mobs:hypo_fight");
                addIfAvailable(pool, "epic_mobs:micky");
                addIfAvailable(pool, "epic_mobs:phoenix_fight");
                addIfAvailable(pool, "epic_mobs:karin");
            }
        }
    }

    private static void addMutantMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("mutantmonsters")) return;

        switch (tier) {
            case MID -> addIfAvailable(pool, "mutantmonsters:mutant_zombie");
            case LATE -> {
                addIfAvailable(pool, "mutantmonsters:mutant_skeleton");
                addIfAvailable(pool, "mutantmonsters:mutant_creeper");
            }
            case ABYSS -> {
                addIfAvailable(pool, "mutantmonsters:mutant_enderman");
                addIfAvailable(pool, "mutantmonsters:mutant_zombie");
            }
        }
    }

    private static void addMowzieMobs(List<EntityType<?>> pool, FloorPalette tier) {
        if (!ModList.get().isLoaded("mowziesmobs")) return;

        switch (tier) {
            case EARLY, MID -> {
                addIfAvailable(pool, "mowziesmobs:foliaath");
                addIfAvailable(pool, "mowziesmobs:umvuthana_raptor");
                addIfAvailable(pool, "mowziesmobs:umvuthana");
            }
            case LATE -> {
                addIfAvailable(pool, "mowziesmobs:umvuthana_crane");
                addIfAvailable(pool, "mowziesmobs:grottol");
                addIfAvailable(pool, "mowziesmobs:lantern");
                addIfAvailable(pool, "mowziesmobs:bluff");
            }
            case ABYSS -> {
                addIfAvailable(pool, "mowziesmobs:naga");
                addIfAvailable(pool, "mowziesmobs:umvuthi");
                addIfAvailable(pool, "mowziesmobs:elokosa");
                addIfAvailable(pool, "mowziesmobs:elokosa_howler");
            }
        }
    }

    private static void addIfAvailable(List<EntityType<?>> pool, String entityId) {
        EntityType<?> type = resolve(entityId);
        if (type != null) {
            pool.add(type);
            StatMod.LOGGER.debug("[TrialDungeon] Added modded mob: {}", entityId);
        }
    }

    public static EntityType<?> resolve(String entityId) {
        if (entityId == null) return null;
        if ("statmod:pyromancer_mob".equals(entityId)) {
            return EntityType.ZOMBIE;
        }
        if ("statmod:cryomancer_mob".equals(entityId)) {
            return EntityType.SKELETON;
        }
        if ("statmod:electromancer_mob".equals(entityId)) {
            return EntityType.SKELETON;
        }
        if ("statmod:wither_mage_mob".equals(entityId)) {
            return EntityType.WITHER_SKELETON;
        }
        if ("statmod:cleric_mob".equals(entityId)) {
            return EntityType.ZOMBIE;
        }
        if ("statmod:hollow_witch_mob".equals(entityId)) {
            return EntityType.WITCH;
        }
        if ("statmod:mage_knight_mob".equals(entityId)) {
            return EntityType.ZOMBIE;
        }
        if ("statmod:void_knight_mob".equals(entityId)) {
            return EntityType.WITHER_SKELETON;
        }

        try {
            ResourceLocation id = new ResourceLocation(entityId);
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return BuiltInRegistries.ENTITY_TYPE.get(id);
            }
        } catch (Exception e) {
            StatMod.LOGGER.debug("[TrialDungeon] Failed to resolve mob: {}", entityId);
        }
        return null;
    }

    public static List<EntityType<?>> resolveAll(List<String> ids) {
        List<EntityType<?>> out = new ArrayList<>();
        for (String id : ids) {
            EntityType<?> t = resolve(id);
            if (t != null) out.add(t);
        }
        return out;
    }

    public static List<EntityType<?>> getCombinedPool(int floor) {
        DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
        List<EntityType<?>> themed = resolveAll(theme.adds());
        if (!themed.isEmpty()) return themed;

        FloorPalette tier = FloorPalette.forFloor(floor);
        List<EntityType<?>> vanilla = DungeonMasterpiece.mobPool(tier);
        List<EntityType<?>> modded = getModdedMobs(tier);
        if (modded.isEmpty()) return vanilla;

        int moddedAllowed = vanilla.size();
        List<EntityType<?>> combined = new ArrayList<>(vanilla);
        if (modded.size() <= moddedAllowed) {
            combined.addAll(modded);
        } else {
            Collections.shuffle(modded);
            combined.addAll(modded.subList(0, moddedAllowed));
        }
        return combined;
    }
}
