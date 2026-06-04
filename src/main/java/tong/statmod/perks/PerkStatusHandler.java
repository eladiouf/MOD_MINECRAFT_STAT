package tong.statmod.perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class PerkStatusHandler {

    @SubscribeEvent
    public static void onEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event instanceof net.minecraftforge.event.entity.living.MobEffectEvent.Added added)) return;

        UUID uuid = player.getUUID();
        if (PerkState.effectProcessing.contains(uuid)) return;

        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(Perk.WILL_FOCUS)) {
                var inst = added.getEffectInstance();
                if (inst.getEffect().isBeneficial()) return;
                int newDur = (int) (inst.getDuration() * 0.85f);
                if (newDur > 0) {
                    PerkState.effectProcessing.add(uuid);
                    player.removeEffect(inst.getEffect());
                    player.addEffect(new MobEffectInstance(inst.getEffect(), newDur, inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                    PerkState.effectProcessing.remove(uuid);
                }
            }
        });
    }
}
