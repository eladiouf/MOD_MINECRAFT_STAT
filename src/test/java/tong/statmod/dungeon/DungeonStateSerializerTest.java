package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tong.statmod.storage.DungeonStateSerializer;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase β — vérifie que le round-trip NBT des champs Trial Dungeon fonctionne.
 *
 * <p>Cas couverts :
 * <ul>
 *   <li>Un joueur neuf a un floorReached = 1, aucune position sauvegardée.</li>
 *   <li>Écrire un floor + position, deserialize dans une nouvelle instance retourne les mêmes valeurs.</li>
 *   <li>unlockDungeonFloor ne régresse jamais.</li>
 * </ul>
 */
public class DungeonStateSerializerTest {

    @Test
    void freshPlayerHasDefaultValues() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(1, data.getDungeonFloorReached());
        assertFalse(data.hasLastOverworldPos());
        assertNull(data.getLastOverworldDimensionId());
    }

    @Test
    void roundTripFloorAndPosition() {
        PlayerStatData original = new PlayerStatData();
        original.setDungeonFloorReached(42);
        original.setLastOverworldDimensionId("minecraft:overworld");
        original.setLastOverworldPos(new BlockPos(100, 64, -200).asLong());

        CompoundTag tag = DungeonStateSerializer.serialize(original);
        PlayerStatData restored = new PlayerStatData();
        DungeonStateSerializer.deserialize(tag, restored);

        assertEquals(42, restored.getDungeonFloorReached());
        assertEquals("minecraft:overworld", restored.getLastOverworldDimensionId());
        assertTrue(restored.hasLastOverworldPos());
        BlockPos pos = BlockPos.of(restored.getLastOverworldPosPacked());
        assertEquals(100, pos.getX());
        assertEquals(64, pos.getY());
        assertEquals(-200, pos.getZ());
    }

    @Test
    void unlockDungeonFloorNeverRegresses() {
        PlayerStatData data = new PlayerStatData();
        data.setDungeonFloorReached(10);
        data.unlockDungeonFloor(5);
        assertEquals(10, data.getDungeonFloorReached(), "un unlock < reached doit être ignoré");

        data.unlockDungeonFloor(20);
        assertEquals(20, data.getDungeonFloorReached(), "un unlock > reached doit progresser");
    }

    @Test
    void deserializeEmptyTagKeepsDefaults() {
        PlayerStatData data = new PlayerStatData();
        DungeonStateSerializer.deserialize(new CompoundTag(), data);
        assertEquals(1, data.getDungeonFloorReached());
        assertNull(data.getLastOverworldDimensionId());
        assertFalse(data.hasLastOverworldPos());
    }
}
