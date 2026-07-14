package tong.statmod.network;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public record StatsSnapshotMessage(Map<StatType, StatValue> values) {
    public StatsSnapshotMessage {
        values = Collections.unmodifiableMap(new EnumMap<>(values));
    }

    public static StatsSnapshotMessage from(PlayerStats stats) {
        return new StatsSnapshotMessage(stats.snapshot());
    }

    public static void encode(StatsSnapshotMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.values.size());
        message.values.forEach((type, value) -> {
            buffer.writeUtf(type.id());
            buffer.writeVarInt(value.level());
            buffer.writeVarInt(value.xp());
        });
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
        return new StatsSnapshotMessage(values);
    }
}
