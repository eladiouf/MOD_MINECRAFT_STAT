package tong.statmod.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class MobScalingHandler {

    @SubscribeEvent
    public static void onMobSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        if (mob.getPersistentData().getBoolean("statmod_scaled")) return;
        mob.getPersistentData().putBoolean("statmod_scaled", true);

        Player nearest = mob.level().getNearestPlayer(mob, 64);
        if (!(nearest instanceof ServerPlayer player)) return;

        int[] globalLevel = {0};
        CapabilityHelper.withStats(player, stats -> {
            int sum = 0;
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) sum += stats.getLevel(i);
            globalLevel[0] = sum / PlayerStats.STAT_COUNT;
        });

        int gLevel = globalLevel[0];
        float healthScale = computeHealthScale(gLevel, (float) Config.mobHealthScaleMax);
        float damageScale = computeDamageScale(gLevel, (float) Config.mobDamageScaleMax);

        if (healthScale > 1.0f) {
            var healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(healthAttr.getBaseValue() * healthScale);
                mob.setHealth(mob.getMaxHealth());
            }
        }

        if (damageScale > 1.0f) {
            var damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damageAttr != null) {
                damageAttr.setBaseValue(damageAttr.getBaseValue() * damageScale);
            }
        }

        mob.getPersistentData().putFloat("statmod_xp_multiplier",
            1.0f + gLevel * 0.01f);

        // Show mob level on name tag
        if (gLevel > 10) {
            String tier = gLevel >= 80 ? "§c" : gLevel >= 50 ? "§6" : gLevel >= 25 ? "§e" : "§7";
            mob.setCustomName(Component.literal(tier + "Lv." + gLevel + " §f" + mob.getName().getString()));
            mob.setCustomNameVisible(true);
        }
    }

    /** Returns scale in [1.0, maxScale] for health. Level 100 → ×1.25 max. */
    static float computeHealthScale(int globalLevel, float maxScale) {
        if (globalLevel <= 10) return 1.0f;
        float raw = 1.0f + globalLevel * 0.0025f;
        return Math.min(raw, maxScale);
    }

    /** Returns scale in [1.0, maxScale] for damage. Level 100 → ×1.15 max. */
    static float computeDamageScale(int globalLevel, float maxScale) {
        if (globalLevel <= 10) return 1.0f;
        float raw = 1.0f + globalLevel * 0.0015f;
        return Math.min(raw, maxScale);
    }

    public static float getXpMultiplier(Mob mob) {
        return mob.getPersistentData().getFloat("statmod_xp_multiplier");
    }
}
