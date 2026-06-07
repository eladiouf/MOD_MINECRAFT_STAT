package tong.statmod.stats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.reload.MobStatReloadListener;

public class MobStatInitializer {

    /** Initialise mob stats from JSON defaults (no L2H integration). */
    public static void applyDefaults(Mob mob) {
        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityType == null) return;

        mob.getCapability(MobStatsProvider.MOB_STATS).ifPresent(stats -> {
            MobStatReloadListener.INSTANCE.getDefaults(entityType).ifPresent(defaults -> {
                stats.reset();
                for (int i = 0; i < defaults.length && i < MobStats.STAT_COUNT; i++) {
                    stats.setLevel(i, defaults[i]);
                }
            });
        });
    }
}
