package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

class PlayerXpStateTest {
    @Test
    void enforcesGlobalAndPerOpponentRollingLimitsAtomically() {
        PlayerXpState state = new PlayerXpState();
        assertEquals(200, state.acceptXp(StatType.BRUTE_FORCE, 250, 100, null));
        assertEquals(0, state.acceptXp(StatType.BRUTE_FORCE, 1, 101, null));
        assertEquals(1, state.acceptXp(StatType.BRUTE_FORCE, 1, 1300, null));

        UUID opponent = UUID.randomUUID();
        PlayerXpState fresh = new PlayerXpState();
        assertEquals(25, fresh.acceptXp(StatType.BLADE_TECHNIQUE, 40, 0, opponent));
        assertEquals(0, fresh.acceptXp(StatType.BLADE_TECHNIQUE, 1, 1, opponent));

        PlayerXpState atomic = new PlayerXpState();
        assertEquals(200, atomic.acceptXp(StatType.PRECISION, 200, 0, null));
        assertEquals(0, atomic.acceptXp(StatType.PRECISION, 10, 1, opponent));
        assertEquals(10, atomic.acceptXp(StatType.PRECISION, 10, 1200, opponent));
    }

    @Test
    void tracksCombosAndCooldownsAtTheirExactBoundaries() {
        PlayerXpState state = new PlayerXpState();
        assertEquals(1, state.recordMeleeHit(10));
        assertEquals(2, state.recordMeleeHit(50));
        assertEquals(1, state.recordMeleeHit(91));

        assertTrue(state.tryWillpower(400));
        assertFalse(state.tryWillpower(799));
        assertTrue(state.tryWillpower(800));
        assertTrue(state.tryAgility(100));
        assertFalse(state.tryAgility(199));
        assertTrue(state.tryAgility(200));
    }

    @Test
    void returnsOnlySafeFallPeaksAndAlwaysClearsTheFall() {
        PlayerXpState state = new PlayerXpState();
        state.observeAirborne(4.9F);
        assertTrue(state.finishLanding().isEmpty());

        state.observeAirborne(6F);
        state.observeAirborne(9.5F);
        assertEquals(OptionalDouble.of(9.5), state.finishLanding());
        assertTrue(state.finishLanding().isEmpty());

        state.observeAirborne(12F);
        state.markFallDamage();
        assertTrue(state.finishLanding().isEmpty());
    }

    @Test
    void persistsAndCopiesOnlyDiscoveredBiomes() {
        ResourceLocation plains = ResourceLocation.fromNamespaceAndPath("minecraft", "plains");
        PlayerXpState source = new PlayerXpState();
        assertTrue(source.discoverBiome(plains));
        assertFalse(source.discoverBiome(plains));
        source.recordMeleeHit(10);
        source.tryWillpower(400);
        source.acceptXp(StatType.BRUTE_FORCE, 200, 0, null);

        CompoundTag serialized = source.serializeNbt();
        PlayerXpState loaded = new PlayerXpState();
        loaded.deserializeNbt(serialized);

        assertTrue(loaded.hasDiscoveredBiome(plains));
        assertEquals(1, loaded.recordMeleeHit(20));
        assertTrue(loaded.tryWillpower(401));
        assertEquals(1, loaded.acceptXp(StatType.BRUTE_FORCE, 1, 1, null));

        PlayerXpState copied = new PlayerXpState();
        copied.copyPersistentFrom(source);
        assertTrue(copied.hasDiscoveredBiome(plains));
        assertEquals(1, copied.recordMeleeHit(20));
        assertTrue(copied.tryWillpower(401));
        assertEquals(1, copied.acceptXp(StatType.BRUTE_FORCE, 1, 1, null));
    }
}
