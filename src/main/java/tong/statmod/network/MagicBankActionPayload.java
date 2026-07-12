package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record MagicBankActionPayload(int action, long amount) implements CustomPacketPayload {
    public static final int DEPOSIT_ALL = 0;
    public static final int WITHDRAW = 1;
    public static final Type<MagicBankActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "magic_bank_action"));
    public static final StreamCodec<ByteBuf, MagicBankActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MagicBankActionPayload::action,
            ByteBufCodecs.VAR_LONG, MagicBankActionPayload::amount,
            MagicBankActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
