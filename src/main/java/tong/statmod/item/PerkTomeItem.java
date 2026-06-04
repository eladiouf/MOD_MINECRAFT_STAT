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
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.SyncPerksPacket;

public class PerkTomeItem extends Item {
    public PerkTomeItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        CapabilityHelper.withPerks(sp, perks -> {
            perks.addPoints(1);
            int[] ids = perks.getUnlockedPerks().stream().mapToInt(i -> i).toArray();
            NetworkHandler.sendToPlayer(new SyncPerksPacket(ids, perks.getAvailablePoints()), sp);
            sp.sendSystemMessage(Component.literal("\u00a7a+1 perk point"));
        });

        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
