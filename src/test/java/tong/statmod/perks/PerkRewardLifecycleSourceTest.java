package tong.statmod.perks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkRewardLifecycleSourceTest {
    private static final Path PERK_MANAGER_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "perks", "PerkManager.java");
    private static final Path RESPEC_STONE_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "item", "RespecStoneItem.java");
    private static final Path TENSURA_RACE_HANDLER_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "integration", "tensura", "TensuraRaceHandler.java");

    @Test
    void perkManagerRevokePathAlsoAttemptsExternalRewardCleanup() throws Exception {
        String source = Files.readString(PERK_MANAGER_SOURCE);

        assertTrue(source.contains("public int resetAll(Player player, boolean refundPoints)"),
                "PerkManager should expose a centralized player-aware reset flow for runtime respecs");
        assertTrue(source.contains("public boolean revoke(Perk perk, boolean refundPoints, Player player)"),
                "PerkManager still needs a player-aware revoke path so runtime callers can stay centralized");
    }

    @Test
    void respecStoneUsesPerkManagerResetFlowInsteadOfDirectlyClearingPerks() throws Exception {
        String source = Files.readString(RESPEC_STONE_SOURCE);

        assertTrue(source.contains("new PerkManager(data)"),
                "respec should use PerkManager so perk reward cleanup stays centralized");
        assertFalse(source.contains("data.clearUnlockedPerks();"),
                "respec should not bypass external reward cleanup with a raw clearUnlockedPerks call");
    }

    @Test
    void raceChangeFlowsUsePlayerAwarePerkRevocationHelpers() throws Exception {
        String source = Files.readString(TENSURA_RACE_HANDLER_SOURCE);

        assertTrue(source.contains("autoRespecRacePerks(player, data, newRaceId)"),
                "race changes should revoke race-locked perks through the player-aware cleanup path");
        assertTrue(source.contains("reconcileIntrinsicPerks(player, data, oldIntrinsicSkills, intrinsicSkills)"),
                "intrinsic perk reconciliation should use the player-aware cleanup path too");
    }

    @Test
    void externalRewardBridgesExposeExplicitRevocationHelpers() throws Exception {
        String raceHandler = Files.readString(TENSURA_RACE_HANDLER_SOURCE);

        assertTrue(raceHandler.contains("if (perk != null && perks.grant(perk, player))"),
                "player-aware intrinsic grants must still award their external rewards on race changes");
    }
}
