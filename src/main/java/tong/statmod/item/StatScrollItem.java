package tong.statmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

import java.util.List;

public class StatScrollItem extends Item {
    public StatScrollItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.statmod.stat_scroll").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.statmod.stat_scroll.desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        CapabilityHelper.withStats(sp, stats -> {
            int lowestIdx = 0;
            int lowestLevel = stats.getLevel(0);
            for (int i = 1; i < StatType.values().length; i++) {
                int lvl = stats.getLevel(i);
                if (lvl < lowestLevel) { lowestLevel = lvl; lowestIdx = i; }
            }
            StatType targetStat = StatType.values()[lowestIdx];
            int before = stats.getLevel(targetStat.index);
            stats.addXp(targetStat.index, stats.getXpForNextLevel(before) * 5);
            int after = stats.getLevel(targetStat.index);
            NetworkHandler.sendToPlayer(new StatUpdatePacket(targetStat.index, after, stats.getXp(targetStat.index)), sp);
            sp.sendSystemMessage(Component.literal("\u00a7a+" + (after - before) + " niveaux en " + targetStat.displayName + " \u00a78(stat la plus faible)"));
        });

        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
