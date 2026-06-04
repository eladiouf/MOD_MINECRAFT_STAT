package tong.statmod.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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

        if (globalLevel[0] <= 10) return;

        float scale = 1.0f + globalLevel[0] * 0.005f;

        var healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(healthAttr.getBaseValue() * scale);
            mob.setHealth(mob.getMaxHealth());
        }

        var damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * (1.0f + globalLevel[0] * 0.003f));
        }

        mob.getPersistentData().putFloat("statmod_xp_multiplier", 1.0f + globalLevel[0] * 0.01f);
    }

    public static float getXpMultiplier(Mob mob) {
        return mob.getPersistentData().getFloat("statmod_xp_multiplier");
    }
}
