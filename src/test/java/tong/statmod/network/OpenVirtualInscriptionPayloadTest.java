package tong.statmod.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test: empty payload round-trips through its StreamCodec without
 * corrupting the buffer, and carries the expected resource id.
 */
class OpenVirtualInscriptionPayloadTest {
    @Test
    void payload_type_has_expected_resource() {
        OpenVirtualInscriptionPayload p = new OpenVirtualInscriptionPayload();
        assertNotNull(p.type());
        assertEquals("statmod:open_virtual_inscription", p.type().id().toString());
    }

    @Test
    void payload_round_trips_through_codec() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenVirtualInscriptionPayload original = new OpenVirtualInscriptionPayload();
        OpenVirtualInscriptionPayload.CODEC.encode(buf, original);
        OpenVirtualInscriptionPayload decoded = OpenVirtualInscriptionPayload.CODEC.decode(buf);
        assertNotNull(decoded);
        assertEquals(original.type().id(), decoded.type().id());
    }
}
