package tong.statmod.network;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.client.ClientStatsCache;

public final class StatNetwork {
    private static final String PROTOCOL = "1";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(StatMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private StatNetwork() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        CHANNEL.registerMessage(0, StatsSnapshotMessage.class,
                StatsSnapshotMessage::encode,
                StatsSnapshotMessage::decode,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientStatsCache.replace(message.values())));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendSnapshot(ServerPlayer player) {
        player.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(stats ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        StatsSnapshotMessage.from(stats)));
    }
}
