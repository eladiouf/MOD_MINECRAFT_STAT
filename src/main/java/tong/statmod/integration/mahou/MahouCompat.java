package tong.statmod.integration.mahou;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MahouCompat {
    private static boolean loaded = false;
    private static Class<?> spellScrollClass = null;
    private static Object getPlayerMahouMethod = null;
    private static java.lang.reflect.Method manaGetter = null;
    private static final Map<UUID, Integer> lastManaMap = new HashMap<>();
    private static final Map<UUID, String> lastElementMap = new HashMap<>();
    private static final Map<UUID, Integer> lastElementTickMap = new HashMap<>();

    private MahouCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("mahoutsukai");
        if (!loaded) {
            STATMod.LOGGER.info("Mahou Tsukai not detected, skipping MahouCompat");
            return;
        }
        try {
            spellScrollClass = Class.forName("stepsword.mahoutsukai.item.spells.SpellScroll");
            Class<?> utils = Class.forName("stepsword.mahoutsukai.util.Utils");
            getPlayerMahouMethod = utils.getMethod("getPlayerMahou", Player.class);
            Class<?> iMahou = Class.forName("stepsword.mahoutsukai.dataattachments.mahou.IMahou");
            manaGetter = iMahou.getMethod("getStoredMana");
        } catch (Exception e) {
            STATMod.LOGGER.warn("Mahou reflection failed: {}", e.getMessage());
        }
        NeoForge.EVENT_BUS.register(MahouCompat.class);
        STATMod.LOGGER.info("Mahou Tsukai integration loaded");
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        ItemStack stack = event.getItemStack();
        if (!isSpellScroll(stack)) return;

        int arcanePower = RaceEffectApplier.getEffectiveLevel(player, StatType.ARCANE_POWER.index);
        int requiredArcane = MahouSpellTier.requiredArcanePower(stack);
        if (!MahouSpellTier.canCast(stack, arcanePower)) {
            player.sendSystemMessage(Component.literal(
                    "Arcane Power " + requiredArcane + " required for this spell scroll."
            ));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        String element = MahouElementMapper.elementFor(stack);
        String itemId = itemId(stack);
        lastElementMap.put(player.getUUID(), element);
        lastElementTickMap.put(player.getUUID(), player.tickCount);
        int baseXp = 3 * MahouSpellTier.xpMultiplier(stack);
        boolean leveled = addXp(player, StatType.ARCANE_POWER, baseXp);
        for (StatType stat : MahouPerkMap.statsForSpellId(itemId)) {
            leveled |= addXp(player, stat, Math.max(1, baseXp / 2));
        }
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 40 != 0) return;

        boolean leveled = false;

        int currentMana = getMana(player);
        if (currentMana < 0) return;

        UUID id = player.getUUID();
        int lastMana = lastManaMap.getOrDefault(id, -1);

        if (lastMana >= 0 && currentMana < lastMana) {
            int consumed = lastMana - currentMana;
            if (consumed > 0) {
                leveled = addXp(player, StatType.MANA_POOL, Math.min(consumed, 10));
                leveled |= addXp(player, StatType.ARCANE_POWER, Math.min(consumed / 2, 5));
            }
        }

        lastManaMap.put(id, currentMana);

        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }

    private static boolean isSpellScroll(ItemStack stack) {
        if (spellScrollClass == null) return false;
        return spellScrollClass.isInstance(stack.getItem());
    }

    private static int getMana(Player player) {
        try {
            if (getPlayerMahouMethod == null) return -1;
            Object mahou = ((java.lang.reflect.Method) getPlayerMahouMethod).invoke(null, player);
            if (mahou == null || manaGetter == null) return -1;
            return (int) manaGetter.invoke(mahou);
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean addXp(Player player, StatType stat, int amount) {
        return RaceEffectApplier.addScaledXp(player, stat.index, amount,
                player.getData(ModAttachments.STATS));
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    public static String recentElement(Player player) {
        if (player == null) {
            return null;
        }
        return lastElementMap.get(player.getUUID());
    }

    public static int recentElementTick(Player player) {
        if (player == null) {
            return Integer.MIN_VALUE;
        }
        return lastElementTickMap.getOrDefault(player.getUUID(), Integer.MIN_VALUE);
    }

    public static boolean hasRecentElement(Player player, String element) {
        if (player == null || element == null) {
            return false;
        }
        String recent = lastElementMap.get(player.getUUID());
        Integer tick = lastElementTickMap.get(player.getUUID());
        if (recent == null || tick == null) {
            return false;
        }
        return recent.equals(element) && player.tickCount - tick <= 40;
    }

    public static float earthDamageMultiplier(int earthAffinity, String element, int castTick, int nowTick) {
        if (!"earth".equals(element) || nowTick - castTick > 40) {
            return 1.0f;
        }
        return 1.0f + Math.max(0, earthAffinity) * 0.01f;
    }

    public static float magicReflectionChance(int willpower, int magicResistance) {
        return Math.min(0.3f, willpower * 0.002f + magicResistance * 0.003f);
    }
}
