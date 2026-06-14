package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record UnlockPerkPayload(int perkId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlockPerkPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "unlock_perk"));

    public static final StreamCodec<ByteBuf, UnlockPerkPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, UnlockPerkPayload::perkId,
                    UnlockPerkPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
