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
    private static final Path TENSURA_SPELL_GATE_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "integration", "tensura", "TensuraSpellGate.java");
    private static final Path PERK_TO_SKILL_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "integration", "tensura", "PerkToSkillMapper.java");
    private static final Path EPIC_FIGHT_GATE_SOURCE = Path.of("src", "main", "java",
            "tong", "statmod", "perks", "EpicFightPerkGate.java");

    @Test
    void perkManagerRevokePathAlsoAttemptsExternalRewardCleanup() throws Exception {
        String source = Files.readString(PERK_MANAGER_SOURCE);

        assertTrue(source.contains("public boolean revoke(Perk perk, boolean refundPoints, Player player)"),
                "PerkManager needs a player-aware revoke path to clean up external rewards");
        assertTrue(source.contains("revokeRewards(player, perk);"),
                "revoking a perk should also revoke any external reward tied to that perk");
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
        String tensuraSpellGate = Files.readString(TENSURA_SPELL_GATE_SOURCE);
        String perkToSkill = Files.readString(PERK_TO_SKILL_SOURCE);
        String epicFightGate = Files.readString(EPIC_FIGHT_GATE_SOURCE);

        assertTrue(tensuraSpellGate.contains("public static boolean revokeReward(Player player, Perk perk)"),
                "Tensura spell rewards need an explicit revoke helper");
        assertTrue(perkToSkill.contains("public static boolean revokeReward(Player player, Perk perk)"),
                "Tensura transcendence skill rewards need an explicit revoke helper");
        assertTrue(epicFightGate.contains("public static boolean revokeReward(Player player, Perk perk)"),
                "Epic Fight rewards need an explicit revoke helper");
    }
}
