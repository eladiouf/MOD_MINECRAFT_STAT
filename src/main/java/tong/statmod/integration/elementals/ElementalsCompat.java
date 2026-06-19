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

        EnumSet<ElementalBranch> allowed = EnumSet.noneOf(ElementalBranch.class);
        if (data.mageAwakened()) {
            allowed.addAll(data.starterBranches());
            for (ElementalBranch branch : data.unlockedBranches()) {
                if (branch.isBaseBranch()) {
                    allowed.add(branch);
                }
            }
        }

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
        ElementState activeState = activeBranch == null ? ElementState.LOCKED : stateFor(player, activeBranch);
        applyRuntimePenalties(profile, mageData, activeState, new ElementalsRuntimePort() {
            @Override
            public void setAllowedBranches(EnumSet<ElementalBranch> branches) {
                throw new UnsupportedOperationException();
            }

            @Override
            public float chi() {
                return RUNTIME.chi(player);
            }

            @Override
            public float xp() {
                return RUNTIME.xp(player);
            }

            @Override
            public int level() {
                return RUNTIME.level(player);
            }

            @Override
            public void setXp(float value) {
                RUNTIME.setXp(player, value);
            }

            @Override
            public void setChi(float value) {
                RUNTIME.setChi(player, value);
            }

            @Override
            public ElementalBranch activeBranch() {
                return RUNTIME.activeBranch(player);
            }

            @Override
            public float maxXpForLevel(int level) {
                return RUNTIME.maxXpForLevel(level);
            }

            @Override
            public void setLevelAndXp(int level, float xp) {
                RUNTIME.setLevelAndXp(player, level, xp);
            }
        });
    }

    static void applyRuntimePenalties(MageRaceProfile profile,
                                      ElementalsMageData mageData,
                                      ElementState activeState,
                                      ElementalsRuntimePort runtime) {
        ElementalBranch activeBranch = runtime.activeBranch();
        float currentChi = runtime.chi();
        float currentXp = runtime.xp();
        int currentLevel = runtime.level();

        if (profile.supported()
                && mageData.mageAwakened()
                && activeBranch != null
                && mageData.unlockedBranches().contains(activeBranch)) {
            boolean rareBranch = !activeBranch.isBaseBranch();
            float adjustedChi = ElementalsPenaltyModel.adjustedChiAfterSpend(
                    mageData.lastSeenChi(),
                    currentChi,
                    activeState,
                    rareBranch,
                    mageData.rewardedRareBranches().size());
            if (Float.compare(adjustedChi, currentChi) != 0) {
                runtime.setChi(adjustedChi);
                currentChi = adjustedChi;
            }

            ProgressSnapshot adjustedProgress = adjustProgressSnapshot(
                    mageData.lastSeenLevel(),
                    mageData.lastSeenXp(),
                    currentLevel,
                    currentXp,
                    profile.beastfolk(),
                    activeBranch,
                    mageData.rewardedRareBranches(),
                    runtime);
            if (adjustedProgress != null
                    && (adjustedProgress.level() != currentLevel || Float.compare(adjustedProgress.xp(), currentXp) != 0)) {
                runtime.setLevelAndXp(adjustedProgress.level(), adjustedProgress.xp());
                currentLevel = adjustedProgress.level();
                currentXp = adjustedProgress.xp();
            }
        }

        mageData.setLastSeenChi(currentChi);
        mageData.setLastSeenXp(currentXp);
        mageData.setLastSeenLevel(currentLevel);
    }

    private static ProgressSnapshot adjustProgressSnapshot(int previousLevel,
                                                           float previousXp,
                                                           int currentLevel,
                                                           float currentXp,
                                                           boolean beastfolk,
                                                           ElementalBranch activeBranch,
                                                           EnumSet<ElementalBranch> rareOwned,
                                                           ElementalsRuntimePort runtime) {
        if (currentLevel < previousLevel || (currentLevel == previousLevel && currentXp <= previousXp)) {
            return null;
        }

        float gained = gainedProgress(previousLevel, previousXp, currentLevel, currentXp, runtime);
        float kept = gained * ElementalsPenaltyModel.progressionMultiplier(beastfolk, activeBranch, rareOwned);
        int level = previousLevel;
        float xp = previousXp + kept;
        while (runtime.maxXpForLevel(level) > 0.0f && xp >= runtime.maxXpForLevel(level)) {
            xp -= runtime.maxXpForLevel(level);
            level++;
        }
        return new ProgressSnapshot(level, xp);
    }

    private static float gainedProgress(int previousLevel,
                                        float previousXp,
                                        int currentLevel,
                                        float currentXp,
                                        ElementalsRuntimePort runtime) {
        if (currentLevel == previousLevel) {
            return currentXp - previousXp;
        }

        float gained = Math.max(0.0f, runtime.maxXpForLevel(previousLevel) - previousXp);
        for (int level = previousLevel + 1; level < currentLevel; level++) {
            gained += Math.max(0.0f, runtime.maxXpForLevel(level));
        }
        return gained + Math.max(0.0f, currentXp);
    }

    private static Set<Integer> unlockedPerks(PlayerStatData statData) {
        Set<Integer> unlocked = new HashSet<>();
        for (int id : statData.getUnlockedPerks()) {
            unlocked.add(id);
        }
        return unlocked;
    }

    private record ProgressSnapshot(int level, float xp) {}
}
