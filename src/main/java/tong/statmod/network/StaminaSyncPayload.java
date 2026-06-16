package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record StaminaSyncPayload(float currentStamina, float fatigueDebt, boolean meditating)
        implements CustomPacketPayload {
    public static final Type<StaminaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "stamina_sync"));

    public static final StreamCodec<ByteBuf, StaminaSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, StaminaSyncPayload::currentStamina,
                    ByteBufCodecs.FLOAT, StaminaSyncPayload::fatigueDebt,
                    ByteBufCodecs.BOOL, StaminaSyncPayload::meditating,
                    StaminaSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
