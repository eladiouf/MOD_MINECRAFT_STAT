package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;

/**
 * Mission M6 — Phase β.
 *
 * <p>Serialization NBT des champs Trial Dungeon de {@link PlayerStatData} :
 * {@code dungeonFloorReached}, {@code lastOverworldDimensionId}, {@code lastOverworldPosPacked}.
 *
 * <p>Fait le pendant de {@link MagicStateSerializer} — isole les concerns, garde
 * {@code StatSerializer} lisible.
 */
public final class DungeonStateSerializer {

    static final String KEY_FLOOR = "DungeonFloorReached";
    static final String KEY_DIM = "DungeonLastOverworldDim";
    static final String KEY_POS = "DungeonLastOverworldPos";
    static final String KEY_POINTS = "DungeonPoints";
    static final String KEY_BEST_COMBO = "DungeonBestCombo";
    static final String KEY_BEST_CLEAR = "DungeonBestClearTicks";
    static final String KEY_BOSS_COOLDOWNS = "BossCooldowns";

    private DungeonStateSerializer() {}

    public static CompoundTag serialize(PlayerStatData data) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_FLOOR, data.getDungeonFloorReached());
        if (data.getLastOverworldDimensionId() != null) {
            tag.putString(KEY_DIM, data.getLastOverworldDimensionId());
        }
        if (data.hasLastOverworldPos()) {
            tag.putLong(KEY_POS, data.getLastOverworldPosPacked());
        }
        tag.putInt(KEY_POINTS, data.getDungeonPoints());
        tag.putInt(KEY_BEST_COMBO, data.getDungeonBestCombo());
        tag.putInt(KEY_BEST_CLEAR, data.getDungeonBestClearTicks());
        if (!data.getBossCooldowns().isEmpty()) {
            var cdTag = new net.minecraft.nbt.CompoundTag();
            for (var e : data.getBossCooldowns().entrySet()) {
                cdTag.putLong(String.valueOf(e.getKey()), e.getValue());
            }
            tag.put(KEY_BOSS_COOLDOWNS, cdTag);
        }
        return tag;
    }

    public static void deserialize(CompoundTag tag, PlayerStatData data) {
        if (tag.contains(KEY_FLOOR)) {
            data.setDungeonFloorReached(tag.getInt(KEY_FLOOR));
        }
        if (tag.contains(KEY_DIM)) {
            data.setLastOverworldDimensionId(tag.getString(KEY_DIM));
        }
        if (tag.contains(KEY_POS)) {
            data.setLastOverworldPos(tag.getLong(KEY_POS));
        }
        if (tag.contains(KEY_POINTS)) {
            data.setDungeonPoints(tag.getInt(KEY_POINTS));
        }
        if (tag.contains(KEY_BEST_COMBO)) {
            data.setDungeonBestCombo(tag.getInt(KEY_BEST_COMBO));
        }
        if (tag.contains(KEY_BEST_CLEAR)) {
            data.setDungeonBestClearTicks(tag.getInt(KEY_BEST_CLEAR));
        }
        if (tag.contains(KEY_BOSS_COOLDOWNS)) {
            java.util.Map<Integer, Long> map = new java.util.HashMap<>();
            var cdTag = tag.getCompound(KEY_BOSS_COOLDOWNS);
            for (var k : cdTag.getAllKeys()) {
                try {
                    map.put(Integer.parseInt(k), cdTag.getLong(k));
                } catch (NumberFormatException ignored) { }
            }
            data.setBossCooldowns(map);
        }
    }
}
