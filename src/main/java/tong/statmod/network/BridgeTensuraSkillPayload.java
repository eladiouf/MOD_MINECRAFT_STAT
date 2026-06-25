package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/**
 * Server → client trigger asking the player's client to simulate a Tensura skill
 * keybind press or release. The client handler calls the matching ManasCore
 * {@code SkillAPI} packet so Tensura's native client+server flow runs end-to-end:
 * magic circle rendering, cast time, charge accumulation, projectile spawn — all
 * unchanged from a normal keybind cast.
 *
 * <p>This is the "door" — Iron's grimoire only initiates and ends the cycle,
 * Tensura owns the whole UX in between.
 */
public record BridgeTensuraSkillPayload(String tensuraSkillId, boolean release)
        implements CustomPacketPayload {

    public static final Type<BridgeTensuraSkillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "bridge_tensura_skill"));

    public static final StreamCodec<ByteBuf, BridgeTensuraSkillPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BridgeTensuraSkillPayload::tensuraSkillId,
            ByteBufCodecs.BOOL, BridgeTensuraSkillPayload::release,
            BridgeTensuraSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
