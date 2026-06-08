package tong.statmod.integration;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

/**
 * Safe wrapper around L2Hostility XP scaling. The {@link #doGetMultiplier} method
 * directly references {@code MobTraitCap}; isolating it behind the
 * {@code ModList.isLoaded} guard keeps STAT Mod startable without L2Hostility
 * (the JVM only loads {@code doGetMultiplier} when actually invoked).
 */
public final class L2HostilityScaling {

    private L2HostilityScaling() {}

    public static float getXpMultiplier(LivingEntity entity) {
        if (!ModList.get().isLoaded("l2hostility")) return 1.0f;
        return doGetMultiplier(entity);
    }

    private static float doGetMultiplier(LivingEntity entity) {
        dev.xkmc.l2hostility.content.capability.mob.MobTraitCap cap =
            dev.xkmc.l2hostility.content.capability.mob.MobTraitCap.HOLDER.get(entity);
        if (cap == null) return 1.0f;
        return 1.0f + cap.lv * 0.05f;
    }
}
