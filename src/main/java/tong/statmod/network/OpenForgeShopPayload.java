package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record OpenForgeShopPayload(long balance) implements CustomPacketPayload {
    public static final Type<OpenForgeShopPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_forge_shop"));
    public static final StreamCodec<ByteBuf, OpenForgeShopPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, OpenForgeShopPayload::balance,
            OpenForgeShopPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
