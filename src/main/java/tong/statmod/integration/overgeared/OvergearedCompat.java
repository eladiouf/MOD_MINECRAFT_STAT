package tong.statmod.integration.overgeared;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.stirdrem.overgeared.ForgingQuality;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

import java.lang.reflect.Method;
import java.util.Locale;

public final class OvergearedCompat {
    private static boolean loaded;

    private OvergearedCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("overgeared");
        if (!loaded) {
            STATMod.LOGGER.info("Overgeared not detected, skipping OvergearedCompat");
            return;
        }
        NeoForge.EVENT_BUS.register(OvergearedCompat.class);
        STATMod.LOGGER.info("Overgeared integration loaded");
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        if (blockId == null || !isOvergearedBlockId(blockId.toString())) return;

        StatType stat = statForBlockId(blockId.toString());
        int xp = baseXpForBlock(blockId.toString());
        xp = OvergearedForgingBonus.scaledInteractionXp(xp, resolveForgingQuality(event.getItemStack()));

        award(player, stat, xp);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) return;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (blockId == null || !isOvergearedBlockId(blockId.toString())) return;

        award(player, StatType.FORGING, 2);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!OvergearedMaterialGate.isOvergearedItem(player.getMainHandItem())) return;

        int forging = RaceEffectApplier.getEffectiveLevel(player, StatType.FORGING.index);
        event.setNewSpeed(event.getNewSpeed() * OvergearedForgingBonus.breakSpeedMultiplier(forging));
    }

    public static boolean isOvergearedBlockId(String blockId) {
        if (blockId == null) return false;
        return blockId.startsWith("overgeared:");
    }

    public static StatType statForBlockId(String blockId) {
        String path = path(blockId);
        if (path.contains("smithing_anvil") || path.contains("drafting_table")) {
            return StatType.FORGING;
        }
        if (path.contains("casting_furnace")) {
            return StatType.ALCHEMY;
        }
        if (path.contains("alloy_furnace") || path.contains("nether_alloy_furnace")) {
            return StatType.COOKING;
        }
        return StatType.FORGING;
    }

    public static ForgingQuality resolveForgingQuality(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !loaded) {
            return ForgingQuality.NONE;
        }

        try {
            for (Method method : stack.getClass().getMethods()) {
                if (method.getParameterCount() != 0) continue;
                String name = method.getName().toLowerCase(Locale.ROOT);
                if (!name.contains("quality")) continue;
                Object value = method.invoke(stack);
                if (value instanceof ForgingQuality quality) {
                    return quality;
                }
            }
        } catch (Exception ignored) {
            // Best-effort reflection only.
        }

        return ForgingQuality.NONE;
    }

    public static int baseXpForBlock(String blockId) {
        String path = path(blockId);
        if (path.contains("smithing_anvil")) return 4;
        if (path.contains("casting_furnace")) return 3;
        if (path.contains("alloy_furnace")) return 2;
        return 1;
    }

    private static String path(String blockId) {
        if (blockId == null) return "";
        int idx = blockId.indexOf(':');
        return idx >= 0 ? blockId.substring(idx + 1) : blockId;
    }

    private static void award(Player player, StatType stat, int amount) {
        if (amount <= 0) return;
        boolean leveled = RaceEffectApplier.addScaledXp(player, stat.index, amount, player.getData(ModAttachments.STATS));
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }
}
