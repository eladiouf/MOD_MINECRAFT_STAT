package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chambre-forte ultra-secrète (2026-07-09) — verrouille l'élection des étages (pur, sans Bootstrap).
 */
public class DungeonUltraVaultTest {

    @Test
    void vaultFloorsAreNeverBossOrTreasureFloors() {
        for (int f = 1; f <= 300; f++) {
            if (DungeonUltraVault.isVaultFloor(f)) {
                assertTrue(f % 5 != 0 && f % 10 != 0,
                        "étage " + f + " : la chambre-forte n'existe que sur les étages de combat");
            }
        }
    }

    @Test
    void roughlyOneCombatFloorInSevenHasAVault() {
        int combat = 0, vaults = 0;
        for (int f = 1; f <= 700; f++) {
            if (!DungeonTeleportHandler.isCombatFloor(f)) continue;
            combat++;
            if (DungeonUltraVault.isVaultFloor(f)) vaults++;
        }
        assertTrue(vaults > 0, "il doit exister des étages à chambre-forte");
        double ratio = (double) vaults / combat;
        assertTrue(ratio > 0.05 && ratio < 0.35,
                "fréquence ~1/7 attendue, obtenu " + vaults + "/" + combat);
    }

    @Test
    void electionIsDeterministic() {
        for (int f = 1; f <= 100; f++) {
            assertEquals(DungeonUltraVault.isVaultFloor(f), DungeonUltraVault.isVaultFloor(f));
        }
        assertFalse(DungeonUltraVault.isVaultFloor(0));
        assertFalse(DungeonUltraVault.isVaultFloor(-7));
    }
}
