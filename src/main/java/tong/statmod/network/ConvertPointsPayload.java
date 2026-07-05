package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/** C2S : demande de convertir {@code amount} points en coins. */
public record ConvertPointsPayload(int amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConvertPointsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "convert_points"));

    public static final StreamCodec<ByteBuf, ConvertPointsPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ConvertPointsPayload::amount,
                    ConvertPointsPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
