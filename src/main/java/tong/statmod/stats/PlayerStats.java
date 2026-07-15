package tong.statmod.stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import tong.statmod.StatModRuntime;

public final class PlayerStats {
    private static final String SCHEMA_KEY = "schema";
    private static final String STATS_KEY = "stats";
    private static final String LEVEL_KEY = "level";
    private static final String XP_KEY = "xp";

    private final EnumMap<StatType, StatProgress> values = new EnumMap<>(StatType.class);

    private int dungeonFloorReached = 1;
    private String lastOverworldDimensionId = "";
    private long lastOverworldPosPacked;
    private boolean hasLastOverworldPos;
    private int dungeonPoints = 0;
    private int dungeonBestCombo = 0;
    private int dungeonBestClearTicks = 0;
    private final java.util.Map<Integer, Long> bossCooldowns = new java.util.HashMap<>();

    public PlayerStats() {
        for (StatType type : StatType.values()) {
            values.put(type, new StatProgress());
        }
    }

    public StatValue get(StatType type) {
        return values.get(type).value();
    }

    public void setLevel(StatType type, int level) {
        values.get(type).setLevel(level);
    }

    public void addXp(StatType type, long amount) {
        values.get(type).addXp(amount);
    }

    public Map<StatType, StatValue> snapshot() {
        EnumMap<StatType, StatValue> copy = new EnumMap<>(StatType.class);
        values.forEach((type, progress) -> copy.put(type, progress.value()));
        return Collections.unmodifiableMap(copy);
    }

    public void copyFrom(PlayerStats source) {
        for (StatType type : StatType.values()) {
            values.get(type).copyFrom(source.values.get(type));
        }
        this.dungeonFloorReached = source.dungeonFloorReached;
        this.lastOverworldDimensionId = source.lastOverworldDimensionId;
        this.lastOverworldPosPacked = source.lastOverworldPosPacked;
        this.hasLastOverworldPos = source.hasLastOverworldPos;
        this.dungeonPoints = source.dungeonPoints;
        this.dungeonBestCombo = source.dungeonBestCombo;
        this.dungeonBestClearTicks = source.dungeonBestClearTicks;
        this.bossCooldowns.clear();
        this.bossCooldowns.putAll(source.bossCooldowns);
    }

    public CompoundTag serializeNbt() {
        CompoundTag root = new CompoundTag();
        root.putInt(SCHEMA_KEY, StatModRuntime.PLAYER_STATS_SCHEMA);
        CompoundTag entries = new CompoundTag();
        for (StatType type : StatType.values()) {
            StatValue value = get(type);
            CompoundTag entry = new CompoundTag();
            entry.putInt(LEVEL_KEY, value.level());
            entry.putInt(XP_KEY, value.xp());
            entries.put(type.id(), entry);
        }
        root.put("stats", entries);
        root.putInt("dungeonFloorReached", dungeonFloorReached);
        root.putString("lastOverworldDimensionId", lastOverworldDimensionId);
        root.putLong("lastOverworldPosPacked", lastOverworldPosPacked);
        root.putBoolean("hasLastOverworldPos", hasLastOverworldPos);
        root.putInt("dungeonPoints", dungeonPoints);
        root.putInt("dungeonBestCombo", dungeonBestCombo);
        root.putInt("dungeonBestClearTicks", dungeonBestClearTicks);

        CompoundTag cooldownsTag = new CompoundTag();
        bossCooldowns.forEach((floor, cooldown) -> cooldownsTag.putLong(String.valueOf(floor), cooldown));
        root.put("bossCooldowns", cooldownsTag);

        return root;
    }

    static int serializedSchema(CompoundTag root) {
        return root.contains(SCHEMA_KEY, Tag.TAG_INT) ? root.getInt(SCHEMA_KEY) : 0;
    }

    public void deserializeNbt(CompoundTag root) {
        if (!root.contains(STATS_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag entries = root.getCompound(STATS_KEY);
        for (StatType type : StatType.values()) {
            if (!entries.contains(type.id(), Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag entry = entries.getCompound(type.id());
            load(type, entry.getInt(LEVEL_KEY), entry.getInt(XP_KEY));
        }
        if (root.contains("dungeonFloorReached")) {
            dungeonFloorReached = root.getInt("dungeonFloorReached");
        }
        if (root.contains("lastOverworldDimensionId")) {
            lastOverworldDimensionId = root.getString("lastOverworldDimensionId");
        }
        if (root.contains("lastOverworldPosPacked")) {
            lastOverworldPosPacked = root.getLong("lastOverworldPosPacked");
        }
        if (root.contains("hasLastOverworldPos")) {
            hasLastOverworldPos = root.getBoolean("hasLastOverworldPos");
        }
        if (root.contains("dungeonPoints")) {
            dungeonPoints = root.getInt("dungeonPoints");
        }
        if (root.contains("dungeonBestCombo")) {
            dungeonBestCombo = root.getInt("dungeonBestCombo");
        }
        if (root.contains("dungeonBestClearTicks")) {
            dungeonBestClearTicks = root.getInt("dungeonBestClearTicks");
        }

        bossCooldowns.clear();
        if (root.contains("bossCooldowns", Tag.TAG_COMPOUND)) {
            CompoundTag cooldownsTag = root.getCompound("bossCooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                try {
                    int floor = java.lang.Integer.parseInt(key);
                    long cooldown = cooldownsTag.getLong(key);
                    bossCooldowns.put(floor, cooldown);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    void load(StatType type, int level, int xp) {
        values.get(type).load(level, xp);
    }

    public int getDungeonFloorReached() {
        return dungeonFloorReached;
    }

    public void setDungeonFloorReached(int floor) {
        this.dungeonFloorReached = floor;
    }

    public String getLastOverworldDimensionId() {
        return lastOverworldDimensionId;
    }

    public void setLastOverworldDimensionId(String id) {
        this.lastOverworldDimensionId = id;
    }

    public long getLastOverworldPosPacked() {
        return lastOverworldPosPacked;
    }

    public void setLastOverworldPos(long packedPos) {
        this.lastOverworldPosPacked = packedPos;
        this.hasLastOverworldPos = true;
    }

    public boolean hasLastOverworldPos() {
        return hasLastOverworldPos;
    }

    public int getDungeonPoints() {
        return dungeonPoints;
    }

    public void setDungeonPoints(int points) {
        this.dungeonPoints = Math.max(0, points);
    }

    public long getBossCooldown(int floor) {
        return bossCooldowns.getOrDefault(floor, 0L);
    }

    public void setBossCooldown(int floor, long gameTime) {
        bossCooldowns.put(floor, gameTime);
    }

    public int addDungeonPoints(int amount) {
        setDungeonPoints(this.dungeonPoints + amount);
        return this.dungeonPoints;
    }

    public void unlockDungeonFloor(int floor) {
        if (floor > this.dungeonFloorReached) {
            setDungeonFloorReached(floor);
        }
    }

    public int getDungeonBestCombo() {
        return dungeonBestCombo;
    }

    public void setDungeonBestCombo(int combo) {
        this.dungeonBestCombo = combo;
    }

    public int getDungeonBestClearTicks() {
        return dungeonBestClearTicks;
    }

    public void setDungeonBestClearTicks(int clearTicks) {
        this.dungeonBestClearTicks = clearTicks;
    }
}
