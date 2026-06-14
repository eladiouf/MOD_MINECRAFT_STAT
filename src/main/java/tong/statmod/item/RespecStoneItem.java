package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import tong.statmod.network.BatchSyncPacket;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.SyncPerksPacket;
import tong.statmod.perks.PerkProvider;
import tong.statmod.stats.StatEffectApplier;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class RespecStoneItem extends Item {
    public RespecStoneItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            CapabilityHelper.withStats(sp, stats -> {
                for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                    stats.setLevel(i, 0);
                    stats.setXp(i, 0);
                }
            });

            sp.getCapability(PerkProvider.PERKS).ifPresent(pm -> {
                pm.resetPerks();
            });

            int[] emptyLevels = new int[PlayerStats.STAT_COUNT];
            int[] emptyXp = new int[PlayerStats.STAT_COUNT];

            NetworkHandler.sendToPlayer(new BatchSyncPacket(
                emptyLevels, emptyXp,
                0, 750, 100, 0,
                new int[0], new int[23]
            ), sp);

            if (!sp.isCreative()) {
                player.getItemInHand(hand).shrink(1);
            }

            player.displayClientMessage(
                Component.literal("§6§l✨ Toutes tes stats et perks ont été réinitialisées !"), true);

            StatEffectApplier.applyAllBonuses(sp);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§8Réinitialise toutes tes stats et perks."));
        tooltip.add(Component.literal("§7Drop semi-rare (5% des mobs)."));
    }
}
