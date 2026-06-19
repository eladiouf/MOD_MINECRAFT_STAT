package tong.statmod.integration.elementals;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.network.SyncHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

public final class ElementalsCompat {
    private static final ElementalsRuntimeBridge RUNTIME = new ElementalsRuntimeBridge();
    private static boolean loaded;

    private ElementalsCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("elementals");
        if (!loaded) {
            STATMod.LOGGER.info("Elementals not detected, skipping ElementalsCompat");
            return;
        }
        NeoForge.EVENT_BUS.register(ElementalsCompat.class);
        NeoForge.EVENT_BUS.register(ElementalsCombatScalingHandler.class);
        STATMod.LOGGER.info("Elementals integration loaded");
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!loaded || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        reconcilePlayer(player);
        applyRuntimePenalties(player);
    }

    public static void reconcilePlayer(ServerPlayer player) {
        PlayerStatData statData = player.getData(ModAttachments.STATS);
        ElementalsMageData mageData = player.getData(ModAttachments.ELEMENTALS_MAGE);
        MageRaceProfile profile = ElementalsRaceAffinity.resolve(PlayerDataBridge.getRaceId(player));
        Set<Integer> unlockedPerks = unlockedPerks(statData);
        EnumSet<ElementalBranch> previousAllowed = mageData.unlockedBranches();
        EnumSet<ElementalBranch> runtimeAllowed = RUNTIME.branches(player);
        boolean previousAwakened = mageData.mageAwakened();
        EnumSet<ElementalBranch> allowed = reconcileMageState(
                profile,
                player.getUUID(),
                statData::getLevel,
                unlockedPerks,
                mageData,
                null);
        boolean runtimeChanged = !runtimeAllowed.equals(allowed);
        if (runtimeChanged) {
            RUNTIME.setAllowedBranches(player, allowed);
        }
        if (runtimeChanged || previousAwakened != mageData.mageAwakened() || !previousAllowed.equals(mageData.unlockedBranches())) {
            SyncHelper.syncStats(player);
            SyncHelper.syncPerks(player);
        }
    }

    static EnumSet<ElementalBranch> reconcileMageState(MageRaceProfile profile,
                                                       UUID playerId,
                                                       IntUnaryOperator levels,
                                                       Set<Integer> unlockedPerks,
                                                       ElementalsMageData data,
                                                       ElementalsRuntimePort runtime) {
        if (profile == null || !profile.supported()) {
            data.setMageAwakened(false);
            data.setStarterBranches(EnumSet.noneOf(ElementalBranch.class));
            data.setUnlockedBranches(EnumSet.noneOf(ElementalBranch.class));
            if (runtime != null) {
                runtime.setAllowedBranches(EnumSet.noneOf(ElementalBranch.class));
            }
            return EnumSet.noneOf(ElementalBranch.class);
        }

        if (!data.mageAwakened() && ElementalsMageRules.canAwaken(profile, levels)) {
            data.setMageAwakened(true);
            data.setStarterBranches(ElementalsRaceAffinity.starterBranches(profile, playerId));
        }

        EnumSet<ElementalBranch> allowed = data.mageAwakened()
                ? data.starterBranches()
                : EnumSet.noneOf(ElementalBranch.class);

        int masteredBaseCount = 0;
        for (ElementalBranch branch : allowed) {
            if (branch.isBaseBranch()
                    && ElementalsMageRules.stateForBaseBranch(branch, levels, unlockedPerks) == ElementState.MASTERED) {
                masteredBaseCount++;
            }
        }

        for (ElementalBranch branch : ElementalBranch.values()) {
            if (!branch.isBaseBranch() || allowed.contains(branch)) {
                continue;
            }
            if (ElementalsMageRules.canUnlockThirdBase(profile, branch, levels, unlockedPerks, masteredBaseCount)
                    || ElementalsMageRules.canUnlockFourthBase(profile, branch, levels, unlockedPerks, masteredBaseCount)) {
                allowed.add(branch);
            }
        }

        allowed.addAll(data.rewardedRareBranches());
        data.setUnlockedBranches(allowed);
        if (runtime != null) {
            runtime.setAllowedBranches(allowed);
        }
        return allowed;
    }

    static ElementalBranch activeBranch(Player player) {
        return player instanceof ServerPlayer serverPlayer ? RUNTIME.activeBranch(serverPlayer) : null;
    }

    static ElementState stateFor(Player player, ElementalBranch branch) {
        if (branch == null) {
            return ElementState.LOCKED;
        }
        Set<Integer> unlockedPerks = unlockedPerks(player.getData(ModAttachments.STATS));
        ElementalsMageData mageData = player.getData(ModAttachments.ELEMENTALS_MAGE);
        if (mageData.unlockedBranches().contains(branch) && branch.isBaseBranch()) {
            return ElementalsMageRules.stateForBaseBranch(branch, player.getData(ModAttachments.STATS)::getLevel, unlockedPerks);
        }
        return mageData.unlockedBranches().contains(branch) ? ElementState.AWAKENED : ElementState.LOCKED;
    }

    private static void applyRuntimePenalties(ServerPlayer player) {
        ElementalsMageData mageData = player.getData(ModAttachments.ELEMENTALS_MAGE);
        MageRaceProfile profile = ElementalsRaceAffinity.resolve(PlayerDataBridge.getRaceId(player));
        ElementalBranch activeBranch = RUNTIME.activeBranch(player);
        float currentChi = RUNTIME.chi(player);
        float currentXp = RUNTIME.xp(player);
        int currentLevel = RUNTIME.level(player);

        if (profile.supported()
                && mageData.mageAwakened()
                && activeBranch != null
                && mageData.unlockedBranches().contains(activeBranch)) {
            ElementState state = stateFor(player, activeBranch);
            boolean rareBranch = !activeBranch.isBaseBranch();
            float adjustedChi = ElementalsPenaltyModel.adjustedChiAfterSpend(
                    mageData.lastSeenChi(),
                    currentChi,
                    state,
                    rareBranch,
                    mageData.rewardedRareBranches().size());
            if (Float.compare(adjustedChi, currentChi) != 0) {
                RUNTIME.setChi(player, adjustedChi);
                currentChi = adjustedChi;
            }

            if (mageData.lastSeenLevel() == currentLevel) {
                float adjustedXp = ElementalsPenaltyModel.adjustedXpAfterGain(
                        mageData.lastSeenXp(),
                        currentXp,
                        profile.beastfolk(),
                        activeBranch,
                        mageData.rewardedRareBranches());
                if (Float.compare(adjustedXp, currentXp) != 0) {
                    RUNTIME.setXp(player, adjustedXp);
                    currentXp = adjustedXp;
                }
            }
        }

        mageData.setLastSeenChi(currentChi);
        mageData.setLastSeenXp(currentXp);
        mageData.setLastSeenLevel(currentLevel);
    }

    private static Set<Integer> unlockedPerks(PlayerStatData statData) {
        Set<Integer> unlocked = new HashSet<>();
        for (int id : statData.getUnlockedPerks()) {
            unlocked.add(id);
        }
        return unlocked;
    }
}
