package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

class StatsSnapshotMessageTest {
    @Test
    void roundTripsByStableStringId() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.FIRE_AFFINITY, 17);
        stats.addXp(StatType.FIRE_AFFINITY, 30);
        StatsSnapshotMessage original = StatsSnapshotMessage.from(stats);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        StatsSnapshotMessage.encode(original, buffer);
        StatsSnapshotMessage decoded = StatsSnapshotMessage.decode(buffer);

        assertEquals(original.values(), decoded.values());
    }
}
