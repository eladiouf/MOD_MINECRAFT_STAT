package tong.statmod.integration.ironspells;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

final class IronSpellRestRecoveryHandler {
    private IronSpellRestRecoveryHandler() {}

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreManaAfterSleep(player);
        }
    }

    static void restoreManaAfterSleep(ServerPlayer player) {
        IronSpellManaSyncBridge.restoreToMax(player);
    }
}
