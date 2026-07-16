package tong.statmod.network;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tong.statmod.StatMod;
import tong.statmod.StatModRuntime;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.ClientBookStudyEffects;
import tong.statmod.client.hunter.ClientHunterPerception;
import tong.statmod.client.notice.ClientProgressNotices;
import tong.statmod.event.EnchantedBookStudySessions;
import tong.statmod.stats.StatType;
import tong.statmod.network.BookStudyInputMessage.Action;

public final class StatNetwork {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(StatMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> StatModRuntime.NETWORK_PROTOCOL)
            .clientAcceptedVersions(StatModRuntime.NETWORK_PROTOCOL::equals)
            .serverAcceptedVersions(StatModRuntime.NETWORK_PROTOCOL::equals)
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
                            () -> () -> ClientStatsCache.replace(
                                    message.values(), message.activePerkIds())));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, StatProgressNoticeMessage.class,
                StatProgressNoticeMessage::encode,
                StatProgressNoticeMessage::decode,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientProgressNotices.offer(message)));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, BookStudyInputMessage.class,
                BookStudyInputMessage::encode,
                BookStudyInputMessage::decode,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        context.enqueueWork(() -> EnchantedBookStudySessions.input(
                                sender, message.action(), message.hand(),
                                sender.serverLevel().getGameTime()));
                    }
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, BookStudyCompletionMessage.class,
                BookStudyCompletionMessage::encode,
                BookStudyCompletionMessage::decode,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientBookStudyEffects.show(message)));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, TrackedPreyMessage.class,
                TrackedPreyMessage::encode,
                TrackedPreyMessage::decode,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientHunterPerception.accept(message)));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendSnapshot(ServerPlayer player) {
        player.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(stats ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        StatsSnapshotMessage.from(stats)));
    }

    public static void sendProgressNotice(ServerPlayer player, StatType stat,
            int awardedXp, int newLevel, int levelsGained) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new StatProgressNoticeMessage(stat, awardedXp, newLevel, levelsGained));
    }

    public static void sendBookStudyInput(Action action, InteractionHand hand) {
        CHANNEL.sendToServer(new BookStudyInputMessage(action, hand));
    }

    public static void sendBookStudyCompletion(ServerPlayer player, ItemStack book) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new BookStudyCompletionMessage(book));
    }

    public static void sendTrackedPrey(
            ServerPlayer player, int entityId, int durationTicks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TrackedPreyMessage(entityId, durationTicks));
    }

    public static void sendTrackedPreyClear(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                TrackedPreyMessage.clear());
    }
}
