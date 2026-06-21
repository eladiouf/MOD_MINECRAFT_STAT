package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishMagicSyncServiceTest {
    @Test
    void mirrorsMagicCategoriesPointsAndUnlockedNodes() {
        PlayerStatData data = new PlayerStatData();
        data.setArcanePoints(4);
        data.setSchoolPoints(MagicBranch.FIRE, 2);
        data.addMagicNode("common/foundation/arcane_focus");
        data.addMagicNode("fire/opener/ignition");

        FakeGateway gateway = new FakeGateway();
        PuffishMagicSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("category:statmod:statmod_magic_common"));
        assertTrue(gateway.operations.contains("category:statmod:statmod_magic_fire"));
        assertTrue(gateway.operations.contains("category:statmod:statmod_magic_locked"));

        assertTrue(gateway.operations.contains("points:statmod:statmod_magic_common:4"));
        assertTrue(gateway.operations.contains("points:statmod:statmod_magic_fire:2"));
        assertTrue(gateway.operations.contains("points:statmod:statmod_magic_locked:0"));

        assertTrue(gateway.operations.contains(
                "unlock:statmod:statmod_magic_common:common.foundation.arcane_focus"));
        assertTrue(gateway.operations.contains(
                "unlock:statmod:statmod_magic_fire:fire.opener.ignition"));
        assertTrue(gateway.operations.contains(
                "lock:statmod:statmod_magic_common:common.foundation.mana_well"));
        assertTrue(gateway.operations.contains(
                "lock:statmod:statmod_magic_locked:blood.locked.anchor"));
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
