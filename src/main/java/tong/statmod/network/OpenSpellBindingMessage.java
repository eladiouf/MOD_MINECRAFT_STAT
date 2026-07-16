package tong.statmod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import tong.statmod.integration.ironspells.VirtualInscriptionMenuProvider;

public record OpenSpellBindingMessage() {
    public static void encode(OpenSpellBindingMessage message, FriendlyByteBuf buffer) {
    }

    public static OpenSpellBindingMessage decode(FriendlyByteBuf buffer) {
        return new OpenSpellBindingMessage();
    }

    public static void handle(
            OpenSpellBindingMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                long gameTime = player.serverLevel().getGameTime();
                if (SpellBindingRequestThrottle.allow(player.getUUID(), gameTime)) {
                    NetworkHooks.openScreen(player, new VirtualInscriptionMenuProvider());
                }
            });
        }
        context.setPacketHandled(true);
    }
}
