package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;
import tong.statmod.network.BookStudyInputMessage.Action;

class BookStudyInputMessageTest {
    @Test
    void roundTripsEveryActionAndHand() {
        for (Action action : Action.values()) {
            for (InteractionHand hand : InteractionHand.values()) {
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                new BookStudyInputMessage(action, hand).encode(buffer);
                assertEquals(new BookStudyInputMessage(action, hand),
                        BookStudyInputMessage.decode(buffer));
            }
        }
    }
}
