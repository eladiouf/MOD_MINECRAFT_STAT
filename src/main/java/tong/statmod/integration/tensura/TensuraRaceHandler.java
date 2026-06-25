package tong.statmod.integration.tensura;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.RaceModifierRegistry;
import tong.statmod.integration.TensuraEventSubscriber;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import tong.statmod.network.SyncHelper;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Set;
import java.util.stream.Collectors;

public final class TensuraRaceHandler {
    private static boolean loaded;

    private TensuraRaceHandler() {}

    public static void init() {
        loaded = ModList.get().isLoaded("tensura");
        if (!loaded) {
            STATMod.LOGGER.info("Tensura not detected, skipping TensuraRaceHandler");
            return;
        }
        RaceEvents.SET_RACE.register(TensuraRaceHandler::onSetRace);
        NeoForge.EVENT_BUS.register(TensuraRaceHandler.class);
        STATMod.LOGGER.info("Tensura race integration loaded");
    }

    public static String getRaceName(Player player) {
        return normalizeRaceId(PlayerDataBridge.getRaceId(player));
    }

    public static String normalizeRaceId(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return "tensura:human";
        }

        String normalized = raceId.trim().toLowerCase();
        if (!normalized.contains(":")) {
            return "tensura:" + normalized;
        }
        return normalized;
    }

    public static void applyRaceBonuses(Player player) {
        applyRaceBonuses(player, true);
    }

    static void applyRaceBonuses(Player player, boolean sync) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        String raceId = getRaceName(player);
        RacePhysicalEffects.apply(player, raceId);

        if (!RaceModifierRegistry.hasRaceData(raceId)) {
            return;
        }

        // Bonuses are evaluated through RaceEffectApplier at read time;
        // refreshing sync here keeps the client aligned after race changes.
        for (int i = 0; i < 23; i++) {
            RaceEffectApplier.getEffectiveLevel(player, i);
        }

        if (sync && player instanceof ServerPlayer serverPlayer) {
            SyncHelper.syncStats(serverPlayer);
        }
    }

    private static EventResult onSetRace(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace, boolean forced, Changeable<Boolean> cancel, Changeable<MutableComponent> message) {
        if (entity instanceof Player player) {
            PlayerStatData data = player.getData(ModAttachments.STATS);
            String newRaceId = newRace != null ? normalizeRaceId(newRace.getRaceId().toString()) : "tensura:human";
            int refunded = autoRespecRacePerks(data, newRaceId);
            Set<String> oldIntrinsicSkills = oldRace == null ? Set.of() : oldRace.getIntrinsicSkills(player).stream()
                    .map(skill -> skill.getRegistryName().toString())
                    .collect(Collectors.toSet());
            Set<String> intrinsicSkills = newRace == null ? Set.of() : newRace.getIntrinsicSkills(player).stream()
                    .map(skill -> skill.getRegistryName().toString())
                    .collect(Collectors.toSet());

            reconcileIntrinsicPerks(data, oldIntrinsicSkills, intrinsicSkills);
            TensuraEventSubscriber.unlockIntrinsicPerks(player, intrinsicSkills, false);
            applyRaceBonuses(player, false);
            syncMagicRaceFromTensura(data, newRaceId);

            if (player instanceof ServerPlayer serverPlayer) {
                if (refunded > 0) {
                    player.sendSystemMessage(Component.literal(
                            "Race evolution respec: " + refunded + " perk points refunded."
                    ));
                }
                SyncHelper.syncAll(serverPlayer);
            }
        }
        return EventResult.pass();
    }

    static int autoRespecRacePerks(PlayerStatData data, String newRaceId) {
        if (data == null) {
            return 0;
        }

        String normalizedRaceId = normalizeRaceId(newRaceId);
        PerkManager perks = new PerkManager(data);
        int refunded = 0;

        for (int perkId : data.getUnlockedPerks()) {
            String requiredRace = tong.statmod.integration.tensura.TensuraSkillGate.requiredRace(perkId);
            if (requiredRace == null || normalizeRaceId(requiredRace).equals(normalizedRaceId)) {
                continue;
            }

            Perk perk = Perk.byId(perkId);
            if (perk != null) {
                boolean refundable = !data.isPerkFreeGranted(perk.id);
                if (perks.revoke(perk, true) && refundable) {
                    refunded += perk.tier.cost;
                }
            }
        }

        return refunded;
    }

    static int reconcileIntrinsicPerks(PlayerStatData data, Set<String> oldIntrinsicSkills, Set<String> newIntrinsicSkills) {
        if (data == null) {
            return 0;
        }

        PerkManager perks = new PerkManager(data);
        Set<Integer> oldPerkIds = TensuraEventSubscriber.intrinsicPerkIdsForSkills(oldIntrinsicSkills);
        Set<Integer> newPerkIds = TensuraEventSubscriber.intrinsicPerkIdsForSkills(newIntrinsicSkills);
        int changed = 0;

        for (int perkId : oldPerkIds) {
            if (newPerkIds.contains(perkId)) {
                continue;
            }

            Perk perk = Perk.byId(perkId);
            if (perk != null && data.isPerkFreeGranted(perk.id) && perks.revoke(perk, true)) {
                changed++;
            }
        }

        for (int perkId : newPerkIds) {
            if (oldPerkIds.contains(perkId)) {
                continue;
            }

            Perk perk = Perk.byId(perkId);
            if (perk != null && perks.grant(perk)) {
                changed++;
            }
        }

        return changed;
    }
    public static int getFlatBonus(Player player, int statIndex) {
        String raceId = getRaceName(player);
        return RaceModifierRegistry.get(raceId).modifiers().stream()
                .filter(modifier -> modifier.statIndex() == statIndex)
                .mapToInt(modifier -> modifier.flatBonus())
                .findFirst()
                .orElse(0);
    }

    public static double getXpMultiplier(Player player, int statIndex) {
        String raceId = getRaceName(player);
        return RaceModifierRegistry.get(raceId).modifiers().stream()
                .filter(modifier -> modifier.statIndex() == statIndex)
                .mapToDouble(modifier -> modifier.xpMultiplier())
                .findFirst()
                .orElse(1.0d);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        applyRaceBonuses(player);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerStatData data = player.getData(ModAttachments.STATS);
            // Sous Mission N : re-sync inconditionnel au login. La race Tensura est
            // autoritative ; si le joueur a reincarne, le STAT MOD's magic race doit suivre.
            if (syncMagicRaceFromTensura(data, getRaceName(player))) {
                SyncHelper.syncMagic(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        applyRaceBonuses(player);
        // Au respawn (mort/dimension change), re-sync la magic race aussi.
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerStatData data = player.getData(ModAttachments.STATS);
            if (syncMagicRaceFromTensura(data, getRaceName(player))) {
                SyncHelper.syncMagic(serverPlayer);
            }
        }
    }

    /**
     * Polling toutes les 5 secondes pour détecter les reincarnations Tensura mid-game (qui
     * n'émettent pas d'event clone/login). Compare la race Tensura actuelle à la magic race
     * dérivée ; si discordance, re-sync. Coût : 1 lookup map toutes les 100 ticks.
     */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 100 != 0) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        MagicRace currentMagicRace = data.getMagicRace();
        MagicRace expectedMagicRace = TensuraToMagicRaceMapper.fromTensuraRaceId(getRaceName(player));
        if (currentMagicRace != expectedMagicRace) {
            if (syncMagicRaceFromTensura(data, getRaceName(player))) {
                applyRaceBonuses(player);
                SyncHelper.syncMagic(serverPlayer);
                SyncHelper.syncStats(serverPlayer);
            }
        }
    }

    static boolean syncMagicRaceFromTensura(PlayerStatData data, String tensuraRaceId) {
        if (data == null) {
            return false;
        }
        MagicRace previous = data.getMagicRace();
        MagicRace derived = TensuraToMagicRaceMapper.fromTensuraRaceId(tensuraRaceId);
        boolean changed = previous != derived;
        data.setMagicRace(derived);

        MagicBranch currentStart = data.getChosenStartBranch();
        if (currentStart == null || !derived.canChooseStartBranch(currentStart)) {
            MagicBranch defaultStart = TensuraToMagicRaceMapper.defaultStartBranch(derived);
            data.setChosenStartBranch(defaultStart);
            changed = changed || currentStart != defaultStart;
        }
        return changed;
    }
}
