package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record StatUpdatePayload(int[] levels, int[] xp, int soulLevel) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StatUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "stat_update"));

    public static final StreamCodec<ByteBuf, StatUpdatePayload> CODEC =
            StreamCodec.composite(
                    NetCodecs.INT_ARRAY, StatUpdatePayload::levels,
                    NetCodecs.INT_ARRAY, StatUpdatePayload::xp,
                    ByteBufCodecs.VAR_INT, StatUpdatePayload::soulLevel,
                    StatUpdatePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
