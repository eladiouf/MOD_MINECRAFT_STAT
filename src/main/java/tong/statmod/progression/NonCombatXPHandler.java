package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Set;

@EventBusSubscriber(modid = STATMod.MODID)
public class NonCombatXPHandler {
    private static final Set<Block> ORES = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    private static final Set<Block> CROPS = Set.of(
            Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES,
            Blocks.BEETROOTS, Blocks.NETHER_WART, Blocks.MELON,
            Blocks.PUMPKIN, Blocks.SUGAR_CANE, Blocks.CACTUS,
            Blocks.BAMBOO
    );

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        Block block = event.getState().getBlock();
        boolean leveled = false;

        if (ORES.contains(block)) {
            leveled = RaceEffectApplier.addScaledXp(player, StatType.FORGING.index, 5, data);
        } else if (CROPS.contains(block)) {
            leveled = RaceEffectApplier.addScaledXp(player, StatType.COOKING.index, 2, data);
        } else {
            leveled = RaceEffectApplier.addScaledXp(player, StatType.FORGING.index, 1, data);
        }

        SyncHelper.syncStats((ServerPlayer) player);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int count = event.getCrafting().getCount();
        boolean leveled = RaceEffectApplier.addScaledXp(player, StatType.FORGING.index, count, data);
        SyncHelper.syncStats((ServerPlayer) player);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        boolean leveled = RaceEffectApplier.addScaledXp(player, StatType.FORGING.index, event.getSmelting().getCount(), data);
        SyncHelper.syncStats((ServerPlayer) player);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
    }
}
