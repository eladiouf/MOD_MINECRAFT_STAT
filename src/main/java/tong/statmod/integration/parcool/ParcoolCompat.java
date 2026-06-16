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
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

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
    public static void onParkourStart(ParCoolActionEvent.Start.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;

        String name = event.getAction().getClass().getSimpleName().toLowerCase(Locale.ROOT);
        Map<StatType, Integer> rewards = xpRewardsForAction(name);
        long now = System.currentTimeMillis();
        PerkState.recordComboHit(player.getUUID(), now, 3000L);
        if (PerkState.getComboCount(player.getUUID(), now, 3000L) >= 5) {
            rewards = withComboBonus(rewards);
        }

        boolean leveled = awardRewards(player, rewards);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onParkourTick(ParCoolActionEvent.Tick.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        String name = event.getAction().getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (name.equals("wallrun") || name.equals("clingtocliff")
                || name.equals("hangdown") || name.equals("wallslide")) {
            boolean leveled = addXp(player, StatType.PHYSICAL_ENDURANCE, 1);
            if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
            SyncHelper.syncStats((ServerPlayer) player);
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

    static Map<StatType, Integer> withComboBonus(Map<StatType, Integer> rewards) {
        EnumMap<StatType, Integer> boosted = new EnumMap<>(StatType.class);
        rewards.forEach((stat, amount) -> boosted.put(stat, amount + Math.max(1, Math.round(amount * 0.5f))));
        return Map.copyOf(boosted);
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
