package tong.statmod.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalTensuraPerkRuntimeSourceTest {
    private static final Path PLAYER_DATA_BRIDGE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "PlayerDataBridge.java");
    private static final Path PLAYER_DATA_TENSURA_HOOK = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "tensura", "PlayerDataTensuraHook.java");
    private static final Path PERK_MANAGER = Path.of(
            "src", "main", "java", "tong", "statmod", "perks", "PerkManager.java");

    @Test
    void playerDataBridgeStaysSafeWhenTensuraIsMissing() throws IOException {
        String bridge = Files.readString(PLAYER_DATA_BRIDGE);

        assertTrue(bridge.contains("ModList.get().isLoaded(\"tensura\")"),
                "PlayerDataBridge must guard Tensura access behind a loaded check");
        assertTrue(bridge.contains("PlayerDataTensuraHook.getSoulLevel(player)"),
                "safe bridge should delegate soul lookups through a Tensura-only hook");
        assertTrue(bridge.contains("PlayerDataTensuraHook.getOptionalRaceId(player)"),
                "safe bridge should delegate race lookups through a Tensura-only hook");
        assertTrue(bridge.contains("PlayerDataTensuraHook.hasSkill(player, skillId)"),
                "safe bridge should delegate skill lookups through a Tensura-only hook");
        assertFalse(bridge.contains("ExistenceStorage"),
                "safe bridge must not import Tensura runtime storage classes directly");
        assertFalse(bridge.contains("RaceStorage"),
                "safe bridge must not import Tensura race storage classes directly");
        assertFalse(bridge.contains("SkillStorage"),
                "safe bridge must not import Tensura skill storage classes directly");
        assertFalse(bridge.contains("StorageManager"),
                "safe bridge must not import Tensura storage managers directly");
    }

    @Test
    void tensuraRuntimeHookKeepsOptionalApiTypesOutOfSafeBridge() throws IOException {
        String hook = Files.readString(PLAYER_DATA_TENSURA_HOOK);

        assertTrue(hook.contains("ExistenceStorage"));
        assertTrue(hook.contains("RaceStorage"));
        assertTrue(hook.contains("SkillStorage"));
        assertTrue(hook.contains("StorageManager"));
    }

    @Test
    void perkRewardGrantingTouchesTensuraBridgeOnlyWhenModIsLoaded() throws IOException {
        String source = Files.readString(PERK_MANAGER);

        int tensuraGuard = source.indexOf("if (ModList.get().isLoaded(\"tensura\"))");
        int tensuraGrant = source.indexOf("TensuraSpellGate.grantReward(player, perk)");

        assertTrue(tensuraGuard >= 0, "PerkManager must guard Tensura reward grants");
        assertTrue(tensuraGrant > tensuraGuard,
                "PerkManager must only touch Tensura reward bridges after the mod-loaded guard");
    }
}
