package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tong.statmod.perks.Perk;
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
        int[] unlocked = data.getUnlockedPerks();

        int[] refunds = new int[PlayerStatData.STAT_COUNT];
        for (int id : unlocked) {
            Perk perk = Perk.byId(id);
            if (perk != null) {
                refunds[perk.stat.index] += perk.tier.cost;
            }
        }

        int totalRefund = 0;
        for (int i = 0; i < refunds.length; i++) {
            if (refunds[i] > 0) {
                data.addPerkPointsForStat(i, refunds[i]);
                totalRefund += refunds[i];
            }
        }

        data.clearUnlockedPerks();
        player.sendSystemMessage(Component.literal("All perks reset! " + totalRefund + " points refunded."));
        player.getItemInHand(hand).shrink(1);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
