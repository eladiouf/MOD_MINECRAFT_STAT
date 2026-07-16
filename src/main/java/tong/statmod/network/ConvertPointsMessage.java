package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.config.Config;
import tong.statmod.dungeon.DungeonExchanger;
import tong.statmod.dungeon.PointExchange;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.stats.PlayerStats;

import java.util.function.Supplier;

public final class ConvertPointsMessage {
    private final int amount;

    public ConvertPointsMessage(int amount) {
        this.amount = amount;
    }

    public static void encode(ConvertPointsMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.amount);
    }

    public static ConvertPointsMessage decode(FriendlyByteBuf buffer) {
        return new ConvertPointsMessage(buffer.readInt());
    }

    public static void handle(ConvertPointsMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                if (DungeonExchanger.canConvert(sender)) {
                    PlayerStats data = StatCapabilities.get(sender);
                    int points = data.getDungeonPoints();
                    double rate = Config.getPointToCoinRate();
                    PointExchange.Result r = PointExchange.compute(message.amount, points, rate);
                    if (r.converted() > 0) {
                        net.minecraft.world.item.ItemStack emeralds = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, (int) r.coins());
                        boolean added = sender.getInventory().add(emeralds);
                        if (!added || emeralds.getCount() > 0) {
                            sender.drop(emeralds, false);
                        }
                        data.addDungeonPoints(-r.converted());
                        SyncHelper.syncStats(sender);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
