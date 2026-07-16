package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tong.statmod.StatModRuntime;
import tong.statmod.stats.StatType;

class StatProgressNoticeMessageTest {
    @Test
    void roundTripsStableIdAndNumbers() {
        var original = new StatProgressNoticeMessage(StatType.AGILITY, 25, 7, 1);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        StatProgressNoticeMessage.encode(original, buffer);

        assertEquals(original, StatProgressNoticeMessage.decode(buffer));
        assertTrue(original.valid());
    }

    @Test
    void malformedPayloadIsBoundedAndUnknownIdIsRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("missing_stat");
        buffer.writeVarInt(Integer.MAX_VALUE);
        buffer.writeVarInt(Integer.MAX_VALUE);
        buffer.writeVarInt(Integer.MAX_VALUE);

        var decoded = StatProgressNoticeMessage.decode(buffer);

        assertFalse(decoded.valid());
        assertEquals(StatProgressNoticeMessage.MAX_AWARDED_XP, decoded.awardedXp());
        assertEquals(100, decoded.newLevel());
        assertEquals(100, decoded.levelsGained());
    }

    @Test
    void bookStudyInputUsesTheCurrentWireProtocol() {
        assertEquals("8", StatModRuntime.NETWORK_PROTOCOL);
    }
}
