package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.network.ManaSyncPayload;
import tong.statmod.storage.ModAttachments;

public final class IronSpellManaSyncBridge {
    private IronSpellManaSyncBridge() {}

    public static void restoreToMax(ServerPlayer player) {
        if (player == null) {
            return;
        }
        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana <= 0.0d) {
            return;
        }
        syncMana(player, (float) maxMana);
    }

    public static void addMana(ServerPlayer player, float deltaMana) {
        if (player == null || Math.abs(deltaMana) < 0.01f) {
            return;
        }
        MagicData magicData = MagicData.getPlayerMagicData(player);
        syncMana(player, magicData.getMana() + deltaMana);
    }

    public static void syncMana(ServerPlayer player, float targetMana) {
        syncMana(player, targetMana, false);
    }

    public static void syncMana(ServerPlayer player, float targetMana, boolean forceClientSync) {
        if (player == null) return;

        double maxMana = player.getAttributeValue(AttributeRegistry.MAX_MANA);
        MagicData magicData = MagicData.getPlayerMagicData(player);

        if (maxMana > 0) targetMana = Mth.clamp(targetMana, 0.0f, (float) maxMana);
        targetMana = Math.max(0.0f, targetMana);

        float current = magicData.getMana();
        boolean manaChanged = Math.abs(targetMana - current) >= 0.01f;
        boolean shouldBroadcast = shouldBroadcastClientSync(current, targetMana, forceClientSync);

        player.getData(ModAttachments.STATS).setStoredMana(targetMana);

        if (manaChanged) {
            magicData.setMana(targetMana);
        }
        if (!shouldBroadcast) return;

        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(targetMana, (float) maxMana));
    }

    static boolean shouldBroadcastClientSync(float liveMana, float targetMana, boolean forceClientSync) {
        return forceClientSync || Math.abs(targetMana - liveMana) >= 0.01f;
    }
}
