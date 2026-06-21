package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record UnlockMagicNodePayload(String nodeId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlockMagicNodePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "unlock_magic_node"));

    public static final StreamCodec<ByteBuf, UnlockMagicNodePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, UnlockMagicNodePayload::nodeId,
                    UnlockMagicNodePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
