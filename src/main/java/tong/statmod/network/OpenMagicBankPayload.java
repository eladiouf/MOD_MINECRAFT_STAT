package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record OpenMagicBankPayload(long balance, long physical) implements CustomPacketPayload {
    public static final Type<OpenMagicBankPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_magic_bank"));
    public static final StreamCodec<ByteBuf, OpenMagicBankPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, OpenMagicBankPayload::balance,
            ByteBufCodecs.VAR_LONG, OpenMagicBankPayload::physical,
            OpenMagicBankPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
