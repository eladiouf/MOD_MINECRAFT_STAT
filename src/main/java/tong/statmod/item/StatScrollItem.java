package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

public class StatScrollItem extends Item {
    public StatScrollItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        CapabilityHelper.withStats(sp, stats -> {
            StatType randomStat = StatType.values()[sp.getRandom().nextInt(StatType.values().length)];
            int before = stats.getLevel(randomStat.index);
            stats.addXp(randomStat.index, stats.getXpForNextLevel(before) * 5);
            int after = stats.getLevel(randomStat.index);
            NetworkHandler.sendToPlayer(new StatUpdatePacket(randomStat.index, after, stats.getXp(randomStat.index)), sp);
            sp.sendSystemMessage(Component.literal("\u00a7a+" + (after - before) + " levels in " + randomStat.displayName));
        });

        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
