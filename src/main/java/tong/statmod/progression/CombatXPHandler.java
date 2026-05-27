package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStats;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;
import tong.statmod.stats.StatType;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class CombatXPHandler {
    private static final Map<UUID, HitTracker> hitTrackers = new HashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        awardWeaponXp(player);
        trackRapidHit(player);

        if (event.getAmount() > 0) {
            double critBonus = event.getAmount() - Math.floor(event.getAmount());
            if (critBonus > 0.5) {
                player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                    stats.addXp(StatType.PRECISION.index, 3);
                    sendUpdate(player, StatType.PRECISION.index, stats);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().getEntity() == null) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            stats.addXp(StatType.PHYSICAL_RESISTANCE.index, 2);
            sendUpdate(player, StatType.PHYSICAL_RESISTANCE.index, stats);
        });

        if (player.isBlocking()) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                stats.addXp(StatType.PHYSICAL_ENDURANCE.index, 3);
                sendUpdate(player, StatType.PHYSICAL_ENDURANCE.index, stats);
            });
        }

        if (player.getHealth() / player.getMaxHealth() < 0.3f) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                stats.addXp(StatType.WILLPOWER.index, 3);
                sendUpdate(player, StatType.WILLPOWER.index, stats);
            });
        }
    }

    private static void awardWeaponXp(ServerPlayer player) {
        StatType primaryStat = determinePrimaryStat(player);
        if (primaryStat == null) return;

        int xp = 5 + player.getRandom().nextInt(6);
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            stats.addXp(primaryStat.index, xp);
            sendUpdate(player, primaryStat.index, stats);
        });
    }

    private static void trackRapidHit(ServerPlayer player) {
        long now = System.currentTimeMillis();
        HitTracker tracker = hitTrackers.computeIfAbsent(player.getUUID(), k -> new HitTracker());
        tracker.addHit(now);

        if (tracker.getHitCount(2000) >= 5) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                stats.addXp(StatType.RAPIDITE.index, 2);
                sendUpdate(player, StatType.RAPIDITE.index, stats);
            });
            tracker.reset();
        }
    }

    private static StatType determinePrimaryStat(ServerPlayer player) {
        var cap = player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
        if (cap.isPresent() && cap.resolve().isPresent()) {
            Object patch = cap.resolve().get();
            if (patch instanceof ServerPlayerPatch playerPatch) {
                CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                if (itemCap != null && !itemCap.isEmpty()) {
                    WeaponCategory cat = itemCap.getWeaponCategory();
                    if (cat == CapabilityItem.WeaponCategories.AXE || cat == CapabilityItem.WeaponCategories.GREATSWORD) return StatType.BRUTE_FORCE;
                    if (cat == CapabilityItem.WeaponCategories.SWORD || cat == CapabilityItem.WeaponCategories.DAGGER
                        || cat == CapabilityItem.WeaponCategories.UCHIGATANA || cat == CapabilityItem.WeaponCategories.TACHI
                        || cat == CapabilityItem.WeaponCategories.TRIDENT || cat == CapabilityItem.WeaponCategories.LONGSWORD) return StatType.BLADE_TECHNIQUE;
                    if (cat == CapabilityItem.WeaponCategories.BOW || cat == CapabilityItem.WeaponCategories.CROSSBOW) return StatType.PRECISION;
                    if (cat == CapabilityItem.WeaponCategories.SPEAR) return StatType.AGILITY;
                    if (cat == CapabilityItem.WeaponCategories.SHIELD) return StatType.PHYSICAL_ENDURANCE;
                }
            }
        }
        return StatType.BRUTE_FORCE;
    }

    private static void sendUpdate(ServerPlayer player, int index, PlayerStats stats) {
        NetworkHandler.sendToPlayer(
            new StatUpdatePacket(index, stats.getLevel(index), stats.getXp(index)), player);
    }

    private static class HitTracker {
        private final long[] hits = new long[20];
        private int index = 0;

        void addHit(long time) {
            hits[index % hits.length] = time;
            index++;
        }

        int getHitCount(long windowMs) {
            long threshold = System.currentTimeMillis() - windowMs;
            int count = 0;
            for (long h : hits) {
                if (h >= threshold) count++;
            }
            return count;
        }

        void reset() {
            for (int i = 0; i < hits.length; i++) hits[i] = 0;
        }
    }
}
