package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.sound.ModSounds;

import java.util.function.Supplier;

/**
 * Client-to-server packet to request a perk unlock.
 */
public class UnlockPerkPacket {
    private final int perkId;

    public UnlockPerkPacket(int perkId) {
        this.perkId = perkId;
    }

    public static void encode(UnlockPerkPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.perkId);
    }

    public static UnlockPerkPacket decode(FriendlyByteBuf buf) {
        return new UnlockPerkPacket(buf.readVarInt());
    }

    public static void handle(UnlockPerkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            Perk perk = Perk.byId(packet.perkId);
            if (perk == null) return;

            CapabilityHelper.withStats(player, stats -> {
                int statLevel = stats.getLevel(perk.stat.index);

                CapabilityHelper.withPerks(player, perkManager -> {
                    boolean success = perkManager.unlockPerk(perk, statLevel);
                    if (success) {
                        player.level().playSound(null, player.blockPosition(),
                            ModSounds.PERK_UNLOCK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                        int[] unlockedIds = perkManager.getUnlockedPerks().stream()
                            .mapToInt(Integer::intValue)
                            .toArray();
                        NetworkHandler.sendToPlayer(
                            new SyncPerksPacket(unlockedIds, perkManager.getAvailablePoints()),
                            player
                        );
                    }
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
