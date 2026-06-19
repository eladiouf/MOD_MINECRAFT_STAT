package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record PerkFeedbackPayload(String title, String message) implements CustomPacketPayload {
    public static final Type<PerkFeedbackPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "perk_feedback"));

    public static final StreamCodec<ByteBuf, PerkFeedbackPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PerkFeedbackPayload::title,
                    ByteBufCodecs.STRING_UTF8, PerkFeedbackPayload::message,
                    PerkFeedbackPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
