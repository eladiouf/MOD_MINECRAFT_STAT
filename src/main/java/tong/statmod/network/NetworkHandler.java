package tong.statmod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tong.statmod.STATMod;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, SyncAllStatsPacket.class,
            SyncAllStatsPacket::encode, SyncAllStatsPacket::decode, SyncAllStatsPacket::handle);
        CHANNEL.registerMessage(packetId++, StatUpdatePacket.class,
            StatUpdatePacket::encode, StatUpdatePacket::decode, StatUpdatePacket::handle);
        CHANNEL.registerMessage(packetId++, FatiguePacket.class,
            FatiguePacket::encode, FatiguePacket::decode, FatiguePacket::handle);
        CHANNEL.registerMessage(packetId++, ThirstPacket.class,
            ThirstPacket::encode, ThirstPacket::decode, ThirstPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncPerksPacket.class,
            SyncPerksPacket::encode, SyncPerksPacket::decode, SyncPerksPacket::handle);
        CHANNEL.registerMessage(packetId++, UnlockPerkPacket.class,
            UnlockPerkPacket::encode, UnlockPerkPacket::decode, UnlockPerkPacket::handle);
        CHANNEL.registerMessage(packetId++, BatchSyncPacket.class,
            BatchSyncPacket::encode, BatchSyncPacket::decode, BatchSyncPacket::handle);
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
