package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tong.statmod.network.SyncHelper;
import tong.statmod.perks.PerkPointAllocator;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public class RespecStoneItem extends Item {
    public RespecStoneItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return super.use(level, player, hand);
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int totalRefund = PerkPointAllocator.refundPaidUnlockedPerks(data);

        data.clearUnlockedPerks();
        player.sendSystemMessage(Component.literal("All perks reset! " + totalRefund + " points refunded."));
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            SyncHelper.syncPerks(serverPlayer);
        }
        player.getItemInHand(hand).shrink(1);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
