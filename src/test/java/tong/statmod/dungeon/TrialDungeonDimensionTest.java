package tong.statmod.dungeon;

import net.minecraft.core.registries.Registries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase α — smoke-test que les ResourceKeys de la dimension sont bien câblées avec le bon
 * namespace et la bonne registry cible. Sert de canari : si un rename casse le mapping
 * Java/JSON, ce test tombe et on sait que la dimension ne se chargera pas au boot serveur.
 */
public class TrialDungeonDimensionTest {

    @Test
    void trialDungeonKeyHasStatmodNamespace() {
        assertNotNull(DungeonDimensions.TRIAL_DUNGEON);
        assertEquals("statmod", DungeonDimensions.TRIAL_DUNGEON.location().getNamespace());
        assertEquals("trial_dungeon", DungeonDimensions.TRIAL_DUNGEON.location().getPath());
    }

    @Test
    void trialDungeonKeyTargetsDimensionRegistry() {
        assertEquals(Registries.DIMENSION.location(), DungeonDimensions.TRIAL_DUNGEON.registry());
    }

    @Test
    void trialDungeonTypeKeyHasStatmodNamespace() {
        assertNotNull(DungeonDimensions.TRIAL_DUNGEON_TYPE);
        assertEquals("statmod", DungeonDimensions.TRIAL_DUNGEON_TYPE.location().getNamespace());
        assertEquals("trial_dungeon", DungeonDimensions.TRIAL_DUNGEON_TYPE.location().getPath());
    }

    @Test
    void trialDungeonTypeKeyTargetsDimensionTypeRegistry() {
        assertEquals(Registries.DIMENSION_TYPE.location(), DungeonDimensions.TRIAL_DUNGEON_TYPE.registry());
    }

    @Test
    void trialDungeonAndTypeShareSamePath() {
        assertEquals(
                DungeonDimensions.TRIAL_DUNGEON.location(),
                DungeonDimensions.TRIAL_DUNGEON_TYPE.location(),
                "La dimension et son type doivent partager le même id — sinon le datapack "
                        + "ne linkera pas dimension.json → dimension_type.json.");
    }
}
