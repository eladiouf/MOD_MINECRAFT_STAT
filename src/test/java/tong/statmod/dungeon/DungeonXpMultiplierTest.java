package tong.statmod.dungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase γ — vérifie le calcul de multiplier d'XP basé sur la position XZ.
 *
 * <p>Note : les tests utilisent les valeurs fallback de {@link tong.statmod.config.Config}
 * quand la spec n'est pas encore chargée (env unit test). Base = 1.0, perFloor = 0.05.
 */
public class DungeonXpMultiplierTest {

    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));

    @Test
    void multiplierIsOneInOverworld() {
        assertEquals(1.0, DungeonXpMultiplier.multiplierFor(OVERWORLD, 0, 0), 0.001);
    }

    @Test
    void multiplierIsOneForNullDimension() {
        assertEquals(1.0, DungeonXpMultiplier.multiplierFor(null, 0, 0), 0.001);
    }

    @Test
    void multiplierAtFloor1IsBasePlusOnePerFloor() {
        double m = DungeonXpMultiplier.multiplierFor(DungeonDimensions.TRIAL_DUNGEON, 0, 0);
        assertEquals(1.05, m, 0.001);
    }

    @Test
    void multiplierAtFloor50IsBigger() {
        int s = DungeonTeleportHandler.FLOOR_SPACING;
        int x50 = s * ((50 - 1) % DungeonTeleportHandler.GRID_COLS);
        int z50 = s * ((50 - 1) / DungeonTeleportHandler.GRID_COLS);
        double m = DungeonXpMultiplier.multiplierFor(DungeonDimensions.TRIAL_DUNGEON, x50, z50);
        assertEquals(3.5, m, 0.001);
    }

    @Test
    void applyToXpRoundsProperly() {
        int xp = DungeonXpMultiplier.applyToXp(10, DungeonDimensions.TRIAL_DUNGEON, 0, 0);
        assertTrue(xp == 10 || xp == 11, "XP arrondi doit etre 10 ou 11, got " + xp);
    }

    @Test
    void applyToXpAlwaysReturnsAtLeastOne() {
        int xp = DungeonXpMultiplier.applyToXp(1, OVERWORLD, 0, 0);
        assertTrue(xp >= 1);
    }
}
