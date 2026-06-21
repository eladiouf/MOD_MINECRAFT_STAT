package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/**
 * Client → server request to open the virtual Iron's Spellbooks inscription menu
 * (the block-less variant filtered to the player's learned spells).
 */
public record OpenVirtualInscriptionPayload() implements CustomPacketPayload {
    public static final Type<OpenVirtualInscriptionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_virtual_inscription"));

    public static final StreamCodec<ByteBuf, OpenVirtualInscriptionPayload> CODEC =
            StreamCodec.unit(new OpenVirtualInscriptionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
