package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

class StatsSnapshotMessageTest {
    @Test
    void roundTripsByStableStringId() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.MAGIC_RESISTANCE, 17);
        stats.addXp(StatType.MAGIC_RESISTANCE, 30);
        StatsSnapshotMessage original = StatsSnapshotMessage.from(stats);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        StatsSnapshotMessage.encode(original, buffer);
        StatsSnapshotMessage decoded = StatsSnapshotMessage.decode(buffer);

        assertEquals(original.values(), decoded.values());
        assertEquals(original.activePerkIds(), decoded.activePerkIds());
    }

    @Test
    void roundTripsOnlyCanonicalKnownPerkIds() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.RAPIDITE, 50);
        StatsSnapshotMessage original = StatsSnapshotMessage.from(stats);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        StatsSnapshotMessage.encode(original, buffer);
        StatsSnapshotMessage decoded = StatsSnapshotMessage.decode(buffer);

        assertEquals(21, StatsSnapshotMessage.MAX_PERKS);
        assertEquals(java.util.List.of(
                "statmod:rapidite_25", "statmod:rapidite_50"),
                decoded.activePerkIds());
    }

    @Test
    void rejectsOversizedPerkPayloadBeforeAllocation() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(0);
        buffer.writeVarInt(StatsSnapshotMessage.MAX_PERKS + 1);

        assertThrows(IllegalArgumentException.class,
                () -> StatsSnapshotMessage.decode(buffer));
    }
}
