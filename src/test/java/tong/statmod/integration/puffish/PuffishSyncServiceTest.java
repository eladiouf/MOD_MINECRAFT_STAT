package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishSyncServiceTest {
    @Test
    void mirrorsUnlockedPerksIntoTheUnifiedCategory() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 3);
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("category:statmod:statmod_perks"));
        assertTrue(gateway.operations.contains("unlock:statmod:statmod_perks:brute_force__brute_core"));
    }

    @Test
    void mirrorsTotalAvailablePerkPointsAcrossFamilies() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 3);
        data.setPerkPoints(Perk.WILL_CORE.stat.index, 2);
        data.setPerkPoints(Perk.FORGE_CORE.stat.index, 4);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("points:statmod:statmod_perks:9"));
    }

    @Test
    void stillLocksPerksThatAreNotCanonicallyUnlocked() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 1);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("lock:statmod:statmod_perks:blade_technique__blade_core"));
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
