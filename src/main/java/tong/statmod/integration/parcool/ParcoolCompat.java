package tong.statmod.integration.parcool;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.perks.PerkState;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stamina.StaminaManager;
import tong.statmod.stamina.StaminaRules;
import tong.statmod.stamina.StaminaThreshold;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ParcoolCompat {
    private static boolean loaded = false;

    private ParcoolCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("parcool");
        if (!loaded) {
            STATMod.LOGGER.info("ParCool not detected, skipping ParcoolCompat");
            return;
        }
        NeoForge.EVENT_BUS.register(ParcoolCompat.class);
        NeoForge.EVENT_BUS.register(ParcoolAttributeHandler.class);
        STATMod.LOGGER.info("ParCool integration loaded");
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onParkourTryToStart(ParCoolActionEvent.TryToStart event) {
        handleBurstStamina(event);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onParkourStart(ParCoolActionEvent.Start.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        String name = event.getAction().getClass().getSimpleName().toLowerCase(Locale.ROOT);
        Map<StatType, Integer> rewards = xpRewardsForAction(name);
        long now = System.currentTimeMillis();
        PerkState.recordComboHit(player.getUUID(), now, 3000L);
        if (PerkState.getComboCount(player.getUUID(), now, 3000L) >= 5) {
            rewards = withComboBonus(rewards);
        }

        boolean leveled = awardRewards(player, rewards);
        if (leveled) SoundHelper.playLevelUp(serverPlayer);
        SyncHelper.syncStats(serverPlayer);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onParkourTickPre(ParCoolActionEvent.Tick.Pre event) {
        Player player = extractPlayer(event);
        if (player == null || player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.tickCount % 20 != 0) return;

        String name = extractActionName(event);
        float tickCost = staminaTickDrainForAction(name);
        if (tickCost <= 0.0f) return;

        if (!tryConsumeActionStamina(player, name, tickCost)) {
            tryCancelEvent(event);
        }
        SyncHelper.syncStamina(serverPlayer);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onParkourTick(ParCoolActionEvent.Tick.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        String name = event.getAction().getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (name.equals("wallrun") || name.equals("clingtocliff")
                || name.equals("hangdown") || name.equals("wallslide")) {
            boolean leveled = addXp(player, StatType.PHYSICAL_ENDURANCE, 1);
            if (leveled) SoundHelper.playLevelUp(serverPlayer);
            SyncHelper.syncStats(serverPlayer);
        }
    }

    static Map<StatType, Integer> xpRewardsForAction(String actionName) {
        String name = actionName == null ? "" : actionName.toLowerCase(Locale.ROOT);
        EnumMap<StatType, Integer> rewards = new EnumMap<>(StatType.class);

        switch (name) {
            case "vault" -> rewards.put(StatType.AGILITY, 3);
            case "walljump", "chargejump", "catleap" -> {
                rewards.put(StatType.AGILITY, 3);
                rewards.put(StatType.BRUTE_FORCE, 2);
            }
            case "dodge", "roll", "quickturn", "flipping", "slide" -> {
                rewards.put(StatType.AGILITY, 2);
                rewards.put(StatType.RAPIDITE, 2);
            }
            case "wallrun", "horizontalwallrun", "verticalwallrun",
                 "crawl", "climbpoles", "climbup", "clingtocliff",
                 "hangdown", "wallslide" -> {
                rewards.put(StatType.AGILITY, 3);
                rewards.put(StatType.PHYSICAL_ENDURANCE, 2);
            }
            case "fastrun", "fastswim", "dive", "skydive" -> {
                rewards.put(StatType.AGILITY, 2);
                rewards.put(StatType.PHYSICAL_ENDURANCE, 1);
            }
            default -> rewards.put(StatType.AGILITY, 1);
        }

        return Map.copyOf(rewards);
    }

    static float staminaCostForAction(String actionName) {
        String name = normalizeActionName(actionName);
        return switch (name) {
            case "vault" -> 4.0f;
            case "walljump", "chargejump", "catleap" -> 7.0f;
            case "dodge", "roll", "quickturn", "flipping", "slide" -> 8.0f;
            case "wallrun", "horizontalwallrun", "verticalwallrun",
                 "crawl", "climbpoles", "climbup", "clingtocliff",
                 "hangdown", "wallslide" -> 3.0f;
            case "fastrun", "fastswim", "dive", "skydive" -> 5.0f;
            default -> 2.0f;
        };
    }

    static float staminaTickDrainForAction(String actionName) {
        String name = normalizeActionName(actionName);
        return switch (name) {
            case "wallrun", "horizontalwallrun", "verticalwallrun",
                 "crawl", "climbpoles", "climbup", "clingtocliff",
                 "hangdown", "wallslide", "fastrun", "fastswim",
                 "dive", "skydive" -> 2.0f;
            default -> 0.0f;
        };
    }

    static boolean canUseAction(StaminaThreshold threshold, float currentStamina, String actionName) {
        return canUseAction(threshold, currentStamina, staminaCostForAction(actionName));
    }

    static boolean canUseAction(StaminaThreshold threshold, float currentStamina, float cost) {
        if (cost <= 0.0f) {
            return true;
        }
        if (threshold == StaminaThreshold.CRITICAL) {
            return false;
        }
        return currentStamina >= cost;
    }

    static Map<StatType, Integer> withComboBonus(Map<StatType, Integer> rewards) {
        EnumMap<StatType, Integer> boosted = new EnumMap<>(StatType.class);
        rewards.forEach((stat, amount) -> boosted.put(stat, amount + Math.max(1, Math.round(amount * 0.5f))));
        return Map.copyOf(boosted);
    }

    private static void handleBurstStamina(Object event) {
        Player player = extractPlayer(event);
        if (player == null || player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        String name = extractActionName(event);
        if (!tryConsumeActionStamina(player, name, staminaCostForAction(name))) {
            tryCancelEvent(event);
        }
        SyncHelper.syncStamina(serverPlayer);
    }

    private static boolean tryConsumeActionStamina(Player player, String actionName, float cost) {
        if (cost <= 0.0f) {
            return true;
        }

        var stamina = player.getData(ModAttachments.STAMINA);
        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        float maxStamina = StaminaRules.maxStamina(endurance);
        StaminaThreshold threshold = StaminaRules.threshold(stamina.currentStamina(), maxStamina);
        if (!canUseAction(threshold, stamina.currentStamina(), cost)) {
            return false;
        }
        return StaminaManager.consume(stamina, cost);
    }

    private static Player extractPlayer(Object event) {
        Object value = invokeNoArg(event, "getPlayer");
        return value instanceof Player player ? player : null;
    }

    private static String extractActionName(Object event) {
        Object action = invokeNoArg(event, "getAction");
        return action == null ? "" : action.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void tryCancelEvent(Object event) {
        if (tryInvokeBoolean(event, "setCanceled")) return;
        if (tryInvokeBoolean(event, "setCancelled")) return;
        tryInvokeVoid(event, "cancel");
    }

    private static boolean tryInvokeBoolean(Object target, String methodName) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            method.invoke(target, true);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void tryInvokeVoid(Object target, String methodName) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static String normalizeActionName(String actionName) {
        return actionName == null ? "" : actionName.toLowerCase(Locale.ROOT);
    }

    private static boolean addXp(Player player, StatType stat, int amount) {
        return RaceEffectApplier.addScaledXp(player, stat.index, amount,
                player.getData(tong.statmod.storage.ModAttachments.STATS));
    }

    private static boolean awardRewards(Player player, Map<StatType, Integer> rewards) {
        boolean leveled = false;
        for (Map.Entry<StatType, Integer> entry : rewards.entrySet()) {
            int amount = entry.getValue();
            if (amount > 0) {
                leveled |= addXp(player, entry.getKey(), amount);
            }
        }
        return leveled;
    }
}
