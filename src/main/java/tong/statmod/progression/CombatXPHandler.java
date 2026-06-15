package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

@EventBusSubscriber(modid = STATMod.MODID)
public class CombatXPHandler {

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        LivingEntity target = event.getEntity();
        float health = target.getMaxHealth();
        int xp = Math.max(1, Math.round(health * 1.5f));

        PlayerStatData data = player.getData(ModAttachments.STATS);
        boolean leveled = RaceEffectApplier.addScaledXp(player, StatType.BRUTE_FORCE.index, xp, data);
        leveled |= RaceEffectApplier.addScaledXp(player, StatType.BLADE_TECHNIQUE.index, xp / 2, data);
        leveled |= RaceEffectApplier.addScaledXp(player, StatType.RAPIDITE.index, xp / 4, data);
        SyncHelper.syncStats((ServerPlayer) player);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
    }
}
