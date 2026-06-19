package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tong.statmod.perks.PerkPointAllocator;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public class PerkTomeItem extends Item {
    public PerkTomeItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return super.use(level, player, hand);
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkPointAllocator.grantPointsToAllFamilies(data, 1);
        player.sendSystemMessage(Component.literal("+1 Perk Point to all families!"));
        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
