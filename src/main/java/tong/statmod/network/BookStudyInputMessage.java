package tong.statmod.network;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

public record BookStudyInputMessage(Action action, InteractionHand hand) {
    public BookStudyInputMessage {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(hand, "hand");
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeEnum(hand);
    }

    public static BookStudyInputMessage decode(FriendlyByteBuf buffer) {
        return new BookStudyInputMessage(
                buffer.readEnum(Action.class), buffer.readEnum(InteractionHand.class));
    }

    public enum Action {
        BEGIN,
        HEARTBEAT,
        RELEASE
    }
}
