package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record BookStudyCompletionMessage(ItemStack book) {
    public BookStudyCompletionMessage {
        book = book == null || book.isEmpty() ? ItemStack.EMPTY : book.copyWithCount(1);
    }

    public boolean valid() {
        return book.is(Items.ENCHANTED_BOOK);
    }

    public static void encode(BookStudyCompletionMessage message, FriendlyByteBuf buffer) {
        buffer.writeItem(message.book);
    }

    public static BookStudyCompletionMessage decode(FriendlyByteBuf buffer) {
        return new BookStudyCompletionMessage(buffer.readItem());
    }
}
