package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/**
 * Client → server : demande de changement de la branche de départ choisie.
 * Le serveur valide que la branche est dans les {@code naturalAffinities} de la race
 * du joueur avant d'appliquer. Si invalide, no-op silencieux (le serveur est l'autorité).
 *
 * @param branchOrdinal ordinal de la {@link tong.statmod.magic.MagicBranch} choisie
 */
public record ChangeStartBranchPayload(int branchOrdinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChangeStartBranchPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "change_start_branch"));

    public static final StreamCodec<ByteBuf, ChangeStartBranchPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ChangeStartBranchPayload::branchOrdinal,
                    ChangeStartBranchPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
