package tong.statmod.integration;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.stats.MobStatEffectApplier;
import tong.statmod.stats.StatType;

/**
 * Only instantiated when l2hostility is loaded (checked in STATMod.commonSetup()).
 * Reads MobTraitCap.lv + active traits → computes MobStats.
 */
public class L2HostilityMobSync {

    private static final float SCALE_FACTOR = 2.0f;

    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        if (mob.getPersistentData().getBoolean("statmod_stats_initialized")) return;
        mob.getPersistentData().putBoolean("statmod_stats_initialized", true);

        MobTraitCap traitCap = MobTraitCap.HOLDER.get(mob);
        if (traitCap == null) return;

        mob.getCapability(MobStatsProvider.MOB_STATS).ifPresent(stats -> {
            int l2hLevel = traitCap.lv;
            applyBaseScaling(stats, l2hLevel);
            applyTraitBonuses(stats, traitCap);
        });
        MobStatEffectApplier.applyAllBonuses(mob);
    }

    private static void applyBaseScaling(MobStats stats, int l2hLevel) {
        for (StatType stat : new StatType[]{
            StatType.BRUTE_FORCE, StatType.BLADE_TECHNIQUE, StatType.RAPIDITE,
            StatType.AGILITY, StatType.PHYSICAL_RESISTANCE,
            StatType.PHYSICAL_ENDURANCE, StatType.PRECISION
        }) {
            stats.setLevel(stat.index, Math.round(l2hLevel * SCALE_FACTOR));
        }
        for (StatType stat : new StatType[]{
            StatType.ARCANE_POWER, StatType.WATER_AFFINITY, StatType.EARTH_AFFINITY,
            StatType.FIRE_AFFINITY, StatType.AIR_AFFINITY,
            StatType.MAGIC_RESISTANCE, StatType.CASTING_SPEED,
            StatType.MANA_POOL, StatType.ERUDITION
        }) {
            stats.setLevel(stat.index, Math.round(l2hLevel * SCALE_FACTOR * 0.7f));
        }
        for (StatType stat : new StatType[]{
            StatType.TRACKING, StatType.KEEN_SENSES,
            StatType.INTIMIDATION, StatType.WILLPOWER
        }) {
            stats.setLevel(stat.index, Math.round(l2hLevel * SCALE_FACTOR * 0.5f));
        }
    }

    private static void applyTraitBonuses(MobStats stats, MobTraitCap traitCap) {
        for (var entry : traitCap.traits.entrySet()) {
            applyTraitBonus(stats, entry.getKey());
        }
    }

    private static void applyTraitBonus(MobStats stats, MobTrait trait) {
        ResourceLocation id = trait.getRegistryName();
        if (id == null) return;
        switch (id.getPath()) {
            case "fiery"        -> stats.addToLevel(StatType.FIRE_AFFINITY.index,      30);
            case "regen"        -> stats.addToLevel(StatType.PHYSICAL_ENDURANCE.index, 25);
            case "reflect"      -> stats.addToLevel(StatType.PHYSICAL_RESISTANCE.index, 20);
            case "invisible"    -> { stats.addToLevel(StatType.AGILITY.index, 20);
                                     stats.addToLevel(StatType.KEEN_SENSES.index, 15); }
            case "gravity"      -> stats.addToLevel(StatType.BRUTE_FORCE.index,        15);
            case "aura"         -> stats.addToLevel(StatType.INTIMIDATION.index,       25);
            case "shulker"      -> stats.addToLevel(StatType.AGILITY.index,            20);
            case "drain"        -> { stats.addToLevel(StatType.ARCANE_POWER.index, 30);
                                     stats.addToLevel(StatType.MANA_POOL.index,    20); }
            case "adapting"     -> stats.addToLevel(StatType.PHYSICAL_RESISTANCE.index, 10);
            case "arena"        -> stats.addToLevel(StatType.INTIMIDATION.index,       30);
            case "corrosion"    -> stats.addToLevel(StatType.ALCHEMY.index,            10);
            case "growth"       -> { stats.addToLevel(StatType.PHYSICAL_ENDURANCE.index, 30);
                                     stats.addToLevel(StatType.BRUTE_FORCE.index,        20); }
            case "reprint"      -> stats.addToLevel(StatType.WILLPOWER.index,          25);
            case "split"        -> { stats.addToLevel(StatType.WILLPOWER.index, 20);
                                     stats.addToLevel(StatType.AGILITY.index,   15); }
            case "killer_aura"  -> { stats.addToLevel(StatType.ARCANE_POWER.index,  40);
                                     stats.addToLevel(StatType.INTIMIDATION.index, 30); }
            case "undying"      -> { stats.addToLevel(StatType.WILLPOWER.index,            50);
                                     stats.addToLevel(StatType.PHYSICAL_ENDURANCE.index,   30); }
            case "dementor"     -> { stats.addToLevel(StatType.MAGIC_RESISTANCE.index, 40);
                                     stats.addToLevel(StatType.WILLPOWER.index,        35); }
            case "dispell"      -> { stats.addToLevel(StatType.ARCANE_POWER.index, 35);
                                     stats.addToLevel(StatType.ERUDITION.index,    25); }
            case "master"       -> { stats.addToLevel(StatType.INTIMIDATION.index, 40);
                                     stats.addToLevel(StatType.WILLPOWER.index,    30); }
            case "ragnarok"     -> { stats.addToLevel(StatType.BRUTE_FORCE.index,  40);
                                     stats.addToLevel(StatType.ARCANE_POWER.index, 30); }
            default -> { /* unknown trait: no bonus */ }
        }
    }
}
