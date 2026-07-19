package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

class StatsSnapshotMessageTest {
    @Test
    void roundTripsByStableStringId() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.MAGIC_RESISTANCE, 17);
        stats.addXp(StatType.MAGIC_RESISTANCE, 30);
        stats.learnedSpells().learn("irons_spellbooks:fireball", 4);
        stats.learnedSpells().learn("addon:wind_blade", 2);
        StatsSnapshotMessage original = StatsSnapshotMessage.from(stats);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        StatsSnapshotMessage.encode(original, buffer);
        StatsSnapshotMessage decoded = StatsSnapshotMessage.decode(buffer);

        assertEquals(original.values(), decoded.values());
        assertEquals(original.activePerkIds(), decoded.activePerkIds());
        assertEquals(java.util.List.of(
                new LearnedSpellEntry("addon:wind_blade", 2),
                new LearnedSpellEntry("irons_spellbooks:fireball", 4)),
                decoded.learnedSpells());
    }

    @Test
    void roundTripsOnlyCanonicalKnownPerkIds() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.RAPIDITE, 50);
        StatsSnapshotMessage original = StatsSnapshotMessage.from(stats);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        StatsSnapshotMessage.encode(original, buffer);
        StatsSnapshotMessage decoded = StatsSnapshotMessage.decode(buffer);

        assertEquals(45, StatsSnapshotMessage.MAX_PERKS);
        assertEquals(java.util.List.of(
                "statmod:rapidite_25", "statmod:rapidite_50"),
                decoded.activePerkIds());
    }

    @Test
    void roundTripsTheCompleteCanonicalPerkCatalogInStableOrder() {
        PlayerStats stats = new PlayerStats();
        for (StatType type : new StatType[] {StatType.RAPIDITE, StatType.AGILITY,
                StatType.PHYSICAL_ENDURANCE, StatType.ARCANE_POWER,
                StatType.CASTING_SPEED, StatType.MANA_POOL,
                StatType.MAGIC_RESISTANCE, StatType.BRUTE_FORCE,
                StatType.BLADE_TECHNIQUE, StatType.PRECISION,
                StatType.PHYSICAL_RESISTANCE, StatType.WILLPOWER,
                StatType.INTIMIDATION, StatType.TRACKING,
                StatType.KEEN_SENSES}) {
            stats.setLevel(type, 75);
        }
        StatsSnapshotMessage original = StatsSnapshotMessage.from(stats);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        StatsSnapshotMessage.encode(original, buffer);
        StatsSnapshotMessage decoded = StatsSnapshotMessage.decode(buffer);

        assertEquals(45, decoded.activePerkIds().size());
        assertEquals("statmod:rapidite_25", decoded.activePerkIds().get(0));
        assertEquals("statmod:keen_senses_75",
                decoded.activePerkIds().get(44));
    }

    @Test
    void rejectsOversizedPerkPayloadBeforeAllocation() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(0);
        buffer.writeVarInt(StatsSnapshotMessage.MAX_PERKS + 1);
        buffer.writeVarInt(0);
        buffer.writeVarInt(1);

        assertThrows(IllegalArgumentException.class,
                () -> StatsSnapshotMessage.decode(buffer));
    }

    @Test
    void rejectsOversizedLearnedSpellPayloadBeforeAllocation() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeVarInt(1);
        buffer.writeVarInt(513);

        assertThrows(IllegalArgumentException.class,
                () -> StatsSnapshotMessage.decode(buffer));
    }
}
