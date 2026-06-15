package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record BatchSyncPayload(int[] levels, int[] xp, int[] perStatPoints, int[] perkIds, int soulLevel)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BatchSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "batch_sync"));

    public static final StreamCodec<ByteBuf, BatchSyncPayload> CODEC =
            StreamCodec.composite(
                    NetCodecs.INT_ARRAY, BatchSyncPayload::levels,
                    NetCodecs.INT_ARRAY, BatchSyncPayload::xp,
                    NetCodecs.INT_ARRAY, BatchSyncPayload::perStatPoints,
                    NetCodecs.INT_ARRAY, BatchSyncPayload::perkIds,
                    ByteBufCodecs.VAR_INT, BatchSyncPayload::soulLevel,
                    BatchSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
