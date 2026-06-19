package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishSyncServiceTest {
    @Test
    void mirrorsUnlockedPerksAndPerStatPoints() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 3);
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("unlock:statmod:frontline_physical_combat:brute_force__brute_core"));
        assertTrue(gateway.operations.contains("points:statmod:frontline_physical_combat:3"));
    }

    @Test
    void mirrorsRemainingPointsAcrossTheWholeFamily() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_SYNERGY.stat.index, 1);
        data.setPerkPoints(Perk.BLADE_CORE.stat.index, 2);
        data.setPerkPoints(Perk.BRUTE_SYNERGY.stat.index, 1);
        data.addUnlockedPerk(Perk.BRUTE_SYNERGY.id);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("points:statmod:frontline_physical_combat:3"));
    }

    @Test
    void mirrorsOnlyRemainingSpendableFamilyPoints() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 0);
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("points:statmod:frontline_physical_combat:0"));
    }

    static final class FakeGateway implements PuffishMirrorGateway {
        final List<String> operations = new ArrayList<>();

        @Override
        public void ensureCategoryUnlocked(String categoryId) {
            operations.add("category:" + categoryId);
        }

        @Override
        public void setPoints(String categoryId, int points) {
            operations.add("points:" + categoryId + ":" + points);
        }

        @Override
        public void unlock(String categoryId, String skillId) {
            operations.add("unlock:" + categoryId + ":" + skillId);
        }

        @Override
        public void lock(String categoryId, String skillId) {
            operations.add("lock:" + categoryId + ":" + skillId);
        }
    }
}
