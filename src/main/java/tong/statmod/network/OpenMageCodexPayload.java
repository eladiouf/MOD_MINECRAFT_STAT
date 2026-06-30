package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/**
 * Server → client : ouvre le {@link tong.statmod.client.codex.MageCodexScreen} sur le client
 * du joueur ciblé. Émis par la commande {@code /magic codex} (et plus tard par une keybind).
 *
 * <p>Pas de payload — c'est un signal pur.
 */
public record OpenMageCodexPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenMageCodexPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_mage_codex"));

    public static final StreamCodec<ByteBuf, OpenMageCodexPayload> CODEC =
            StreamCodec.unit(new OpenMageCodexPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
