package tong.statmod.magic;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class LearnedSpellState {
    public static final int MAX_ENTRIES = 512;
    public static final int MAX_ID_LENGTH = 128;
    public static final String NBT_KEY = "learnedSpells";

    private static final String ID_KEY = "id";
    private static final String LEVEL_KEY = "level";

    private final LinkedHashMap<String, Integer> levels = new LinkedHashMap<>();

    public enum LearnResult {
        NEW,
        UPGRADED,
        DUPLICATE,
        FULL,
        INVALID
    }

    public LearnResult learn(String rawId, int level) {
        String canonical = canonicalId(rawId);
        if (canonical == null || level <= 0) {
            return LearnResult.INVALID;
        }
        Integer previous = levels.get(canonical);
        if (previous != null && previous >= level) {
            return LearnResult.DUPLICATE;
        }
        if (previous == null && levels.size() >= MAX_ENTRIES) {
            return LearnResult.FULL;
        }
        levels.put(canonical, level);
        return previous == null ? LearnResult.NEW : LearnResult.UPGRADED;
    }

    public int level(String rawId) {
        String canonical = canonicalId(rawId);
        return canonical == null ? 0 : levels.getOrDefault(canonical, 0);
    }

    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(levels));
    }

    public void copyFrom(LearnedSpellState source) {
        levels.clear();
        if (source != null) {
            source.levels.forEach(this::learn);
        }
    }

    public ListTag save() {
        ListTag list = new ListTag();
        levels.forEach((id, level) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(ID_KEY, id);
            entry.putInt(LEVEL_KEY, level);
            list.add(entry);
        });
        return list;
    }

    public void load(CompoundTag root) {
        levels.clear();
        if (root == null || !root.contains(NBT_KEY, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = root.getList(NBT_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size() && levels.size() < MAX_ENTRIES; index++) {
            CompoundTag entry = list.getCompound(index);
            learn(entry.getString(ID_KEY), entry.getInt(LEVEL_KEY));
        }
    }

    private static String canonicalId(String rawId) {
        if (rawId == null || rawId.isBlank()
                || rawId.getBytes(StandardCharsets.UTF_8).length > MAX_ID_LENGTH) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        return id == null ? null : id.toString();
    }
}
