package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

@EventBusSubscriber(modid = STATMod.MODID)
public class NonCombatXPHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof Player player) || player.level().isClientSide) return;
        if (!isOre(event.getState().getBlock())) return;

        award(player, StatType.FORGING, 5);
    }

    @SubscribeEvent
    public static void onItemCrafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        award(player, StatType.FORGING, Math.max(1, event.getCrafting().getCount()));
    }

    @SubscribeEvent
    public static void onItemSmelted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ItemStack result = event.getSmelting();
        StatType stat = result.getItem().getFoodProperties(result, player) != null
                ? StatType.COOKING
                : StatType.FORGING;
        award(player, stat, Math.max(1, result.getCount()));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.BREWING_STAND)) return;
        if (event.getItemStack().isEmpty()) return;

        award(player, StatType.ALCHEMY, 2);
    }

    private static boolean isOre(net.minecraft.world.level.block.Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.NETHER_QUARTZ_ORE || block == Blocks.NETHER_GOLD_ORE
                || block == Blocks.ANCIENT_DEBRIS;
    }

    private static void award(Player player, StatType stat, int amount) {
        boolean leveled = RaceEffectApplier.addScaledXp(player, stat.index, amount, player.getData(ModAttachments.STATS));
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }
}
