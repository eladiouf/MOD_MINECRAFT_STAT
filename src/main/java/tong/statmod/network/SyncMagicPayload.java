package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record SyncMagicPayload(String[] magicNodes, String[] learnedSpells, int arcanePoints,
                               int[] schoolPoints, int raceOrdinal, int startBranchOrdinal)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMagicPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "sync_magic"));

    public static final StreamCodec<ByteBuf, SyncMagicPayload> CODEC =
            StreamCodec.composite(
                    NetCodecs.STRING_ARRAY, SyncMagicPayload::magicNodes,
                    NetCodecs.STRING_ARRAY, SyncMagicPayload::learnedSpells,
                    ByteBufCodecs.VAR_INT, SyncMagicPayload::arcanePoints,
                    NetCodecs.INT_ARRAY, SyncMagicPayload::schoolPoints,
                    ByteBufCodecs.VAR_INT, SyncMagicPayload::raceOrdinal,
                    ByteBufCodecs.VAR_INT, SyncMagicPayload::startBranchOrdinal,
                    SyncMagicPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
