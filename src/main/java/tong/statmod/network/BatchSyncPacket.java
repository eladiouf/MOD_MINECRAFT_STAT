package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;
import java.util.function.Supplier;

public class BatchSyncPacket {
    private final int[] statLevels;
    private final int[] statXp;
    private final float fatigue;
    private final int maxFatigue;
    private final float thirst;
    private final float mana;
    private final int[] perkIds;
    private final int perkPoints;

    public BatchSyncPacket(int[] levels, int[] xp, float fatigue, int maxFatigue, float thirst,
                           float mana, int[] perkIds, int perkPoints) {
        this.statLevels = levels;
        this.statXp = xp;
        this.fatigue = fatigue;
        this.maxFatigue = maxFatigue;
        this.thirst = thirst;
        this.mana = mana;
        this.perkIds = perkIds;
        this.perkPoints = perkPoints;
    }

    public static void encode(BatchSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarIntArray(msg.statLevels);
        buf.writeVarIntArray(msg.statXp);
        buf.writeFloat(msg.fatigue);
        buf.writeInt(msg.maxFatigue);
        buf.writeFloat(msg.thirst);
        buf.writeFloat(msg.mana);
        buf.writeVarIntArray(msg.perkIds);
        buf.writeInt(msg.perkPoints);
    }

    public static BatchSyncPacket decode(FriendlyByteBuf buf) {
        return new BatchSyncPacket(
            buf.readVarIntArray(), buf.readVarIntArray(),
            buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readFloat(),
            buf.readVarIntArray(), buf.readInt());
    }

    public static void handle(BatchSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientStatsCache.updateAll(msg.statLevels, msg.statXp);
            ClientStatsCache.updateFatigue(msg.fatigue, msg.maxFatigue);
            ClientStatsCache.updateThirst(msg.thirst);
            ClientStatsCache.updateMana(msg.mana);
            ClientStatsCache.updatePerks(msg.perkIds, msg.perkPoints);
        });
        ctx.get().setPacketHandled(true);
    }
}
