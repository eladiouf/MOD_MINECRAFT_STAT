package tong.statmod.progression;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

import java.util.Set;

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
    public static void onItemPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;

        // First spell book pickup awards Erudition Lv1
        if (player.getData(ModAttachments.STATS).getLevel(StatType.ERUDITION.index) == 0
                && isIronSpellBook(event.getItemEntity().getItem())) {
            award(player, StatType.ERUDITION, 10);
        }
    }

    static boolean isIronSpellBook(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!ModList.get().isLoaded("irons_spellbooks")) return false;
        try {
            return stack.getItem() instanceof io.redspace.ironsspellbooks.item.SpellBook;
        } catch (NoClassDefFoundError ignored) {
            return false;
        }
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
    public static void onPlayerBrewedPotion(PlayerBrewedPotionEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!isAlchemyOutput(event.getStack())) return;

        award(player, StatType.ALCHEMY, 2);
    }

    static boolean isOre(Block block) {
        if (block == null) return false;
        return isOrePath(BuiltInRegistries.BLOCK.getKey(block).getPath());
    }

    static boolean isOrePath(String path) {
        return path != null && ORE_PATHS.contains(path);
    }

    static boolean isAlchemyOutput(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

    private static final Set<String> ORE_PATHS = Set.of(
            "coal_ore", "deepslate_coal_ore",
            "iron_ore", "deepslate_iron_ore",
            "gold_ore", "deepslate_gold_ore",
            "diamond_ore", "deepslate_diamond_ore",
            "emerald_ore", "deepslate_emerald_ore",
            "lapis_ore", "deepslate_lapis_ore",
            "redstone_ore", "deepslate_redstone_ore",
            "copper_ore", "deepslate_copper_ore",
            "nether_quartz_ore", "nether_gold_ore",
            "ancient_debris"
    );

    private static void award(Player player, StatType stat, int amount) {
        boolean leveled = RaceEffectApplier.addScaledXp(player, stat.index, amount, player.getData(ModAttachments.STATS), false);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }
}
