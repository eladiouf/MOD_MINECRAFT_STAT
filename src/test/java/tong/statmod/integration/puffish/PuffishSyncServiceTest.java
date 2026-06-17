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

        assertTrue(gateway.operations.contains("unlock:statmod:brute_force:brute_core"));
        assertTrue(gateway.operations.contains("points:statmod:brute_force:4"));
    }

    @Test
    void mirrorsCategoryTotalsUsingRealPerkCosts() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_SYNERGY.stat.index, 1);
        data.addUnlockedPerk(Perk.BRUTE_SYNERGY.id);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("points:statmod:brute_force:3"));
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
