package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/**
 * Sync server → client de l'état magique du joueur. Refondu sous Mission J pour porter
 * {@code magicPoints} (monnaie unifiée) + {@code masteryProgress[]} (par école) qui
 * remplacent les anciens {@code arcanePoints} + {@code schoolPoints[]} legacy.
 */
public record SyncMagicPayload(String[] magicNodes, String[] learnedSpells, int magicPoints,
                               int[] masteryProgress, int raceOrdinal, int startBranchOrdinal)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMagicPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "sync_magic"));

    public static final StreamCodec<ByteBuf, SyncMagicPayload> CODEC =
            StreamCodec.composite(
                    NetCodecs.STRING_ARRAY, SyncMagicPayload::magicNodes,
                    NetCodecs.STRING_ARRAY, SyncMagicPayload::learnedSpells,
                    ByteBufCodecs.VAR_INT, SyncMagicPayload::magicPoints,
                    NetCodecs.INT_ARRAY, SyncMagicPayload::masteryProgress,
                    ByteBufCodecs.VAR_INT, SyncMagicPayload::raceOrdinal,
                    ByteBufCodecs.VAR_INT, SyncMagicPayload::startBranchOrdinal,
                    SyncMagicPayload::new
            );

    /** @deprecated Wire-compat shim — préférer {@link #magicPoints()}. */
    @Deprecated
    public int arcanePoints() { return magicPoints; }

    /** @deprecated Wire-compat shim — préférer {@link #masteryProgress()}. */
    @Deprecated
    public int[] schoolPoints() { return masteryProgress; }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
