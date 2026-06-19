package commonnetwork.api;

import commonnetwork.networking.data.Side;
import net.minecraft.network.protocol.PacketFlow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkCompatTest {
    @Test
    void packetDirectionFollowsElementalsPackageConvention() {
        assertEquals(Network.PacketDirection.CLIENTBOUND,
                Network.packetDirection("dev.saperate.elementals.network.packets.S2C.SyncChiPacket"));
        assertEquals(Network.PacketDirection.SERVERBOUND,
                Network.packetDirection("dev.saperate.elementals.network.packets.C2S.MouseClickPacket"));
        assertEquals(Network.PacketDirection.BIDIRECTIONAL,
                Network.packetDirection("dev.saperate.elementals.network.packets.common.SyncLevelPacket"));
    }

    @Test
    void packetSideMapsFromPacketFlow() {
        assertEquals(Side.CLIENT, Network.packetSide(PacketFlow.CLIENTBOUND));
        assertEquals(Side.SERVER, Network.packetSide(PacketFlow.SERVERBOUND));
    }
}
