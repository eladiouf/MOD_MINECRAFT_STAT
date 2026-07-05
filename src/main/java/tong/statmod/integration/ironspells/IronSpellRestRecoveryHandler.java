package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.network.PacketDistributor;

final class IronSpellRestRecoveryHandler {
    private IronSpellRestRecoveryHandler() {}

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreManaAfterSleep(player);
        }
    }

    static void restoreManaAfterSleep(ServerPlayer player) {
        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana <= 0.0d) {
            return;
        }

        MagicData magicData = MagicData.getPlayerMagicData(player);
        float targetMana = (float) maxMana;
        if (Math.abs(magicData.getMana() - targetMana) < 0.01f) {
            return;
        }

        magicData.setMana(targetMana);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }
}
