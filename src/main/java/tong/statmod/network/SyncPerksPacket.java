package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientPerkCache;

import java.util.function.Supplier;

public class SyncPerksPacket {
    private final int[] unlockedPerkIds;
    private final int[] perStatPoints;

    public SyncPerksPacket(int[] unlockedPerkIds, int[] perStatPoints) {
        this.unlockedPerkIds = NetworkPayloadRules.requirePerkArray(unlockedPerkIds, "unlockedPerkIds");
        this.perStatPoints = perStatPoints;
    }

    public static void encode(SyncPerksPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.unlockedPerkIds);
        buf.writeVarIntArray(packet.perStatPoints);
    }

    public static SyncPerksPacket decode(FriendlyByteBuf buf) {
        return new SyncPerksPacket(
            buf.readVarIntArray(NetworkPayloadRules.MAX_PERK_COUNT),
            buf.readVarIntArray(23)
        );
    }

    public static void handle(SyncPerksPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientPerkCache.update(packet.unlockedPerkIds, packet.perStatPoints));
        ctx.get().setPacketHandled(true);
    }
}
