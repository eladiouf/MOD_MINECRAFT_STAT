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
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.SyncPerksPacket;
import tong.statmod.stats.StatType;

import java.util.List;

public class PerkTomeItem extends Item {
    public PerkTomeItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.statmod.perk_tome").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.statmod.perk_tome.desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        CapabilityHelper.withPerks(sp, perks -> {
            for (StatType stat : StatType.values()) {
                perks.addPointsForStat(stat, 1);
            }
            int[] ids = perks.getUnlockedPerks().stream().mapToInt(i -> i).toArray();
            NetworkHandler.sendToPlayer(new SyncPerksPacket(ids, perks.getPerStatPoints()), sp);
            sp.sendSystemMessage(Component.literal("\u00a7a+1 perk point pour chaque stat!"));
        });

        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
