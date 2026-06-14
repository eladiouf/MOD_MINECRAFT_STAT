package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record SyncPerksPayload(int[] perkIds, int[] perStatPoints) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPerksPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "sync_perks"));

    public static final StreamCodec<ByteBuf, SyncPerksPayload> CODEC =
            StreamCodec.composite(
                    NetCodecs.INT_ARRAY, SyncPerksPayload::perkIds,
                    NetCodecs.INT_ARRAY, SyncPerksPayload::perStatPoints,
                    SyncPerksPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
