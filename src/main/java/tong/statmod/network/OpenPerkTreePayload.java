package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record OpenPerkTreePayload() implements CustomPacketPayload {
    public static final Type<OpenPerkTreePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_perk_tree"));

    public static final StreamCodec<ByteBuf, OpenPerkTreePayload> CODEC =
            StreamCodec.unit(new OpenPerkTreePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
