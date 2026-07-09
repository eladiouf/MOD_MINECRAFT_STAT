package tong.statmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record LearnBookPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LearnBookPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "learn_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LearnBookPayload> CODEC =
            StreamCodec.unit(new LearnBookPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
