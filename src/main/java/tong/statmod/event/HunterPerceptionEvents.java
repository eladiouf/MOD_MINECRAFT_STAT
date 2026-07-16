package tong.statmod.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.effects.HunterTrackingService;
import tong.statmod.progression.xp.XpAwardService;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class HunterPerceptionEvents {
    private HunterPerceptionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        float amount = event.getAmount();
        LivingEntity target = event.getEntity();
        if (!Float.isFinite(amount) || amount <= 0F
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !(target instanceof Enemy)
                || !XpAwardService.isEligible(player)) {
            return;
        }
        HunterTrackingService.mark(player, target);
    }
}
