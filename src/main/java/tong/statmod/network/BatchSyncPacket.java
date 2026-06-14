package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientPerkCache;
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
    private final int[] perStatPoints;

    public BatchSyncPacket(int[] levels, int[] xp, float fatigue, int maxFatigue, float thirst,
                           float mana, int[] perkIds, int[] perStatPoints) {
        this.statLevels = NetworkPayloadRules.requireStatArray(levels, "statLevels");
        this.statXp = NetworkPayloadRules.requireStatArray(xp, "statXp");
        NetworkPayloadRules.requireMatchingStatArrays(this.statLevels, this.statXp, "batch stat sync");
        this.fatigue = fatigue;
        this.maxFatigue = maxFatigue;
        this.thirst = thirst;
        this.mana = mana;
        this.perkIds = NetworkPayloadRules.requirePerkArray(perkIds, "perkIds");
        this.perStatPoints = perStatPoints;
    }

    public static void encode(BatchSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarIntArray(msg.statLevels);
        buf.writeVarIntArray(msg.statXp);
        buf.writeFloat(msg.fatigue);
        buf.writeInt(msg.maxFatigue);
        buf.writeFloat(msg.thirst);
        buf.writeFloat(msg.mana);
        buf.writeVarIntArray(msg.perkIds);
        buf.writeVarIntArray(msg.perStatPoints);
    }

    public static BatchSyncPacket decode(FriendlyByteBuf buf) {
        return new BatchSyncPacket(
            buf.readVarIntArray(NetworkPayloadRules.MAX_STAT_COUNT),
            buf.readVarIntArray(NetworkPayloadRules.MAX_STAT_COUNT),
            buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readFloat(),
            buf.readVarIntArray(NetworkPayloadRules.MAX_PERK_COUNT),
            buf.readVarIntArray(23));
    }

    public static void handle(BatchSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientStatsCache.updateAll(msg.statLevels, msg.statXp);
            ClientStatsCache.updateFatigue(msg.fatigue, msg.maxFatigue);
            ClientStatsCache.updateThirst(msg.thirst);
            ClientStatsCache.updateMana(msg.mana);
            ClientPerkCache.update(msg.perkIds, msg.perStatPoints);
        });
        ctx.get().setPacketHandled(true);
    }
}
