package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/** S2C : ouvre/rafraîchit l'écran d'échange avec les points + coins courants et le taux de conversion. */
public record OpenExchangePayload(int points, long coins, float rate) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenExchangePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_exchange"));

    public static final StreamCodec<ByteBuf, OpenExchangePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenExchangePayload::points,
                    ByteBufCodecs.VAR_LONG, OpenExchangePayload::coins,
                    ByteBufCodecs.FLOAT, OpenExchangePayload::rate,
                    OpenExchangePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
