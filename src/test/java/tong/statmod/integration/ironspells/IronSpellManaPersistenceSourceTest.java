package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSpellManaPersistenceSourceTest {
    private static final Path MAIN_JAVA = Path.of("src", "main", "java", "tong", "statmod");

    @Test
    void sharedManaBridgePersistsStoredManaBeforeSyncingClients() throws IOException {
        Path sourcePath = MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellManaSyncBridge.java"));

        assertTrue(Files.exists(sourcePath),
                "direct Iron's mana writes should go through a dedicated shared bridge");

        String source = Files.readString(sourcePath);
        assertTrue(source.contains("setStoredMana"),
                "the shared bridge must persist the attachment snapshot immediately");
        assertTrue(source.contains("SyncManaPacket"),
                "the shared bridge must still sync the live Iron's mana HUD");
    }

    @Test
    void genericSleepRecoveryStaysFocusedOnStaminaWhenDedicatedIronWakeHookExists() throws IOException {
        String source = Files.readString(MAIN_JAVA.resolve(Path.of(
                "time", "SleepRecoveryHandler.java")));

        assertTrue(!source.contains("IronSpellManaSyncBridge"),
                "generic wake recovery should not duplicate Iron's mana restoration");
        assertTrue(!source.contains("irons_spellbooks"),
                "generic wake recovery should stay decoupled from Iron's-specific mana logic");
    }

    @Test
    void potionAndPerkRefundPathsUseSharedManaBridge() throws IOException {
        String potionBridge = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellTensuraPotionBridge.java")));
        String eventBridge = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellEventBridge.java")));

        assertTrue(potionBridge.contains("IronSpellManaSyncBridge.addMana(player"),
                "Tensura arcane potions should persist and sync mana through the shared bridge");
        assertTrue(eventBridge.contains("IronSpellManaSyncBridge.addMana(player"),
                "advanced mana refunds should persist and sync mana through the shared bridge");
    }

    @Test
    void attributeAndRestHooksUseSharedManaBridgeInsteadOfManualPacketWrites() throws IOException {
        String attributeBridge = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellAttributeBridge.java")));
        String restRecovery = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellRestRecoveryHandler.java")));

        assertTrue(attributeBridge.contains("IronSpellManaSyncBridge.syncMana(player,"),
                "login/respawn/max-mana rescaling should persist mana through the shared bridge");
        assertTrue(restRecovery.contains("IronSpellManaSyncBridge.restoreToMax(player)"),
                "the dedicated Iron sleep hook should use the same shared restore path");
    }

    @Test
    void attributeBridgeDoesNotSkipSharedPersistenceWhenLiveManaAlreadyMatchesTarget() throws IOException {
        String attributeBridge = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellAttributeBridge.java")));

        assertTrue(!attributeBridge.contains("if (Math.abs(desiredMana - magicData.getMana()) < 0.01f)"),
                "stored mana should still be refreshed even when the live mana value already matches");
    }

    @Test
    void loginAndRespawnCaptureStoredManaBeforeAttributeApply() throws IOException {
        String attributeBridge = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellAttributeBridge.java")));

        assertTrue(attributeBridge.contains("float storedMana = player.getData(ModAttachments.STATS).getStoredMana();"),
                "login/respawn should snapshot persisted mana before attribute rescaling can overwrite it");
        assertTrue(attributeBridge.contains("restoreMana(player, storedMana);"),
                "login/respawn should restore from the pre-apply snapshot instead of the just-rescaled live value");
    }

    @Test
    void loginRestoreForcesAManaPacketEvenWhenServerStateAlreadyMatches() throws IOException {
        String manaBridge = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellManaSyncBridge.java")));
        String attributeBridge = Files.readString(MAIN_JAVA.resolve(Path.of(
                "integration", "ironspells", "IronSpellAttributeBridge.java")));

        assertTrue(manaBridge.contains("boolean forceClientSync"),
                "the shared mana bridge should expose an explicit forced client sync path");
        assertTrue(manaBridge.contains("boolean shouldBroadcast = shouldBroadcastClientSync(current, targetMana, forceClientSync);"),
                "the shared mana bridge should decide packet broadcasts through the dedicated forced-sync helper");
        assertTrue(manaBridge.contains("if (!shouldBroadcast) return;"),
                "redundant packet suppression should happen after the bridge has accounted for forced client refreshes");
        assertTrue(attributeBridge.contains("syncMana(player, resolveRestoredMana(storedMana, maxMana), true)"),
                "login/respawn restore should force a SyncManaPacket so the client HUD cannot stay stale at zero");
    }
}
