package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.PointExchangeScreen;

import java.util.function.Supplier;

public final class OpenExchangeMessage {
    private final int points;
    private final long coins;
    private final float rate;

    public OpenExchangeMessage(int points, long coins, float rate) {
        this.points = points;
        this.coins = coins;
        this.rate = rate;
    }

    public static void encode(OpenExchangeMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.points);
        buffer.writeLong(message.coins);
        buffer.writeFloat(message.rate);
    }

    public static OpenExchangeMessage decode(FriendlyByteBuf buffer) {
        return new OpenExchangeMessage(buffer.readInt(), buffer.readLong(), buffer.readFloat());
    }

    public static void handle(OpenExchangeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            PointExchangeScreen.openOrRefresh(message.points, message.coins, message.rate);
        }));
        context.setPacketHandled(true);
    }
}
