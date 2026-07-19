package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TrackedPreyMessageTest {
    @Test
    void roundTripsMarkAndClear() {
        assertRoundTrip(new TrackedPreyMessage(42, 280));
        assertRoundTrip(TrackedPreyMessage.clear());
    }

    @Test
    void rejectsMalformedBoundsBeforeCacheMutation() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrackedPreyMessage(-2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TrackedPreyMessage(3, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new TrackedPreyMessage(3, 401));
        assertThrows(IllegalArgumentException.class,
                () -> new TrackedPreyMessage(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new TrackedPreyMessage(3, 0));
    }

    private static void assertRoundTrip(TrackedPreyMessage original) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        TrackedPreyMessage.encode(original, buffer);
        assertEquals(original, TrackedPreyMessage.decode(buffer));
    }
}
