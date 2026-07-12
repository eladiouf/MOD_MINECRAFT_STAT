package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record BuyForgeItemPayload(String itemId) implements CustomPacketPayload {
    public static final Type<BuyForgeItemPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "buy_forge_item"));
    public static final StreamCodec<ByteBuf, BuyForgeItemPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BuyForgeItemPayload::itemId,
            BuyForgeItemPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
