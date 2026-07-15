package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import tong.statmod.stats.StatType;

public record StatProgressNoticeMessage(
        StatType stat, int awardedXp, int newLevel, int levelsGained) {
    public static final int MAX_AWARDED_XP = 1_000_000;

    public StatProgressNoticeMessage {
        awardedXp = clamp(awardedXp, 0, MAX_AWARDED_XP);
        newLevel = clamp(newLevel, 0, 100);
        levelsGained = clamp(levelsGained, 0, 100);
    }

    public boolean valid() {
        return stat != null && awardedXp > 0;
    }

    public static void encode(StatProgressNoticeMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.stat == null ? "" : message.stat.id(), 64);
        buffer.writeVarInt(message.awardedXp);
        buffer.writeVarInt(message.newLevel);
        buffer.writeVarInt(message.levelsGained);
    }

    public static StatProgressNoticeMessage decode(FriendlyByteBuf buffer) {
        StatType stat = StatType.fromId(buffer.readUtf(64)).orElse(null);
        return new StatProgressNoticeMessage(
                stat, buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
