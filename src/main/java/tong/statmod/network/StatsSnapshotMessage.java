package tong.statmod.network;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import tong.statmod.perks.AutomaticPerkCatalog;
import tong.statmod.perks.AutomaticPerkDefinition;
import tong.statmod.perks.AutomaticPerkResolver;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public record StatsSnapshotMessage(
        Map<StatType, StatValue> values, List<String> activePerkIds,
        int dungeonPoints, int dungeonFloorReached,
        List<LearnedSpellEntry> learnedSpells) {
    public static final int MAX_PERKS = 45;
    public static final int MAX_LEARNED_SPELLS = 512;

    public StatsSnapshotMessage {
        values = Collections.unmodifiableMap(new EnumMap<>(values));
        Set<String> requested = new HashSet<>(activePerkIds == null ? List.of() : activePerkIds);
        activePerkIds = AutomaticPerkCatalog.definitions().stream()
                .map(AutomaticPerkDefinition::id)
                .filter(requested::contains)
                .limit(MAX_PERKS)
                .toList();
        java.util.TreeMap<String, Integer> highestLevels = new java.util.TreeMap<>();
        for (LearnedSpellEntry entry : learnedSpells == null ? List.<LearnedSpellEntry>of() : learnedSpells) {
            highestLevels.merge(entry.id(), entry.level(), Math::max);
        }
        if (highestLevels.size() > MAX_LEARNED_SPELLS) {
            throw new IllegalArgumentException("too many learned spells");
        }
        learnedSpells = highestLevels.entrySet().stream()
                .map(entry -> new LearnedSpellEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public StatsSnapshotMessage(Map<StatType, StatValue> values) {
        this(values, List.of(), 0, 1, List.of());
    }

    public StatsSnapshotMessage(Map<StatType, StatValue> values, List<String> activePerkIds) {
        this(values, activePerkIds, 0, 1, List.of());
    }

    public StatsSnapshotMessage(Map<StatType, StatValue> values, List<String> activePerkIds,
            int dungeonPoints, int dungeonFloorReached) {
        this(values, activePerkIds, dungeonPoints, dungeonFloorReached, List.of());
    }

    public static StatsSnapshotMessage from(PlayerStats stats) {
        EnumMap<StatType, Integer> levels = new EnumMap<>(StatType.class);
        stats.snapshot().forEach((type, value) -> levels.put(type, value.level()));
        List<String> perkIds = AutomaticPerkResolver.active(levels).stream()
                .map(AutomaticPerkDefinition::id)
                .toList();
        List<LearnedSpellEntry> learned = stats.learnedSpells().snapshot().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new LearnedSpellEntry(entry.getKey(), entry.getValue()))
                .toList();
        return new StatsSnapshotMessage(stats.snapshot(), perkIds, stats.getDungeonPoints(),
                stats.getDungeonFloorReached(), learned);
    }

    public static void encode(StatsSnapshotMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.values.size());
        message.values.forEach((type, value) -> {
            buffer.writeUtf(type.id());
            buffer.writeVarInt(value.level());
            buffer.writeVarInt(value.xp());
        });
        buffer.writeVarInt(message.activePerkIds.size());
        message.activePerkIds.forEach(id -> buffer.writeUtf(id, 64));
        buffer.writeVarInt(message.dungeonPoints);
        buffer.writeVarInt(message.dungeonFloorReached);
        buffer.writeVarInt(message.learnedSpells.size());
        for (LearnedSpellEntry entry : message.learnedSpells) {
            buffer.writeUtf(entry.id(), tong.statmod.magic.LearnedSpellState.MAX_ID_LENGTH);
            buffer.writeVarInt(entry.level());
        }
    }

    public static StatsSnapshotMessage decode(FriendlyByteBuf buffer) {
        EnumMap<StatType, StatValue> values = new EnumMap<>(StatType.class);
        int count = Math.min(buffer.readVarInt(), StatType.values().length);
        for (int index = 0; index < count; index++) {
            String id = buffer.readUtf(64);
            int level = buffer.readVarInt();
            int xp = buffer.readVarInt();
            StatType.fromId(id).ifPresent(type -> values.put(type, new StatValue(level, xp)));
        }
        for (StatType type : StatType.values()) {
            values.putIfAbsent(type, new StatValue(0, 0));
        }
        int perkCount = buffer.readVarInt();
        if (perkCount < 0 || perkCount > MAX_PERKS) {
            throw new IllegalArgumentException("invalid active perk count: " + perkCount);
        }
        java.util.ArrayList<String> perkIds = new java.util.ArrayList<>(perkCount);
        for (int index = 0; index < perkCount; index++) {
            String id = buffer.readUtf(64);
            if (AutomaticPerkCatalog.byId(id).isPresent()) {
                perkIds.add(id);
            }
        }
        int dungeonPoints = buffer.readVarInt();
        int dungeonFloorReached = buffer.readVarInt();
        int learnedCount = buffer.readVarInt();
        if (learnedCount < 0 || learnedCount > MAX_LEARNED_SPELLS) {
            throw new IllegalArgumentException("invalid learned spell count: " + learnedCount);
        }
        java.util.ArrayList<LearnedSpellEntry> learned = new java.util.ArrayList<>(learnedCount);
        for (int index = 0; index < learnedCount; index++) {
            learned.add(new LearnedSpellEntry(
                    buffer.readUtf(tong.statmod.magic.LearnedSpellState.MAX_ID_LENGTH),
                    buffer.readVarInt()));
        }
        return new StatsSnapshotMessage(values, perkIds, dungeonPoints, dungeonFloorReached, learned);
    }
}
